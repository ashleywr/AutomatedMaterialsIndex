package com.sanhiruzu.ami.fabric.client;

import com.sanhiruzu.ami.client.AmiClientCommands;
import com.sanhiruzu.ami.client.InventoryOverlayHandler;
import com.sanhiruzu.ami.config.AmiConfig;
import com.sanhiruzu.ami.index.GlobalIndex;
import net.fabricmc.fabric.api.client.item.v1.ItemTooltipCallback;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents;
import net.fabricmc.fabric.api.client.screen.v1.ScreenKeyboardEvents;
import net.fabricmc.fabric.api.client.screen.v1.ScreenMouseEvents;
import net.fabricmc.fabric.api.event.Event;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

import java.util.List;

/**
 * Fabric-specific client hooks (Milestone D).
 *
 * Wires Fabric API screen and lifecycle events into AMI's xplat overlay handlers:
 *   - ScreenEvents.AFTER_INIT   → InventoryOverlayHandler.onScreenInit()
 *   - ScreenEvents.BEFORE_INIT  → register per-screen mouse/keyboard events
 *   - ScreenEvents.afterRender  → render overlay (base + top in screen-space)
 *   - ScreenKeyboardEvents      → OverlayInputController.keyPressed / charTyped
 *   - ScreenMouseEvents         → OverlayInputController.mouseButtonPressed / Released / Scrolled / Dragged
 *   - ClientPlayConnectionEvents.JOIN → InventoryOverlayHandler.onPlayerLoggingIn()
 *   - ItemTooltipCallback       → AmiDevModeHandler tooltip augmentation
 *   - ClientCommandRegistrationCallback → /ami client commands
 *
 * Called once from AmiFabricClient.onInitializeClient().
 */
public final class AmiFabricClientHooks {

    // JEI (and other recipe viewers) register their own allow-input listeners on these same per-screen
    // Fabric events in the default phase. Fabric's array-backed "allow" events short-circuit on the first
    // listener that returns false, so without an explicit phase, whichever mod's listener happens to be
    // registered first (an artifact of mod load order) silently wins the input for that frame — AMI's own
    // handling of a hovered AMI element was sometimes pre-empted by JEI in large modpacks. Running AMI's
    // listeners in a phase ordered before the default phase keeps AMI authoritative over its own panels.
    private static final net.minecraft.resources.ResourceLocation AMI_INPUT_PHASE =
            net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("ami", "overlay_input");

    private AmiFabricClientHooks() {}

    public static void register() {
        registerScreenEvents();
        registerWorldJoinEvents();
        registerTooltipEvents();
        registerClientCommands();
    }

    // -------------------------------------------------------------------------
    // Screen events
    // -------------------------------------------------------------------------

    private static void registerScreenEvents() {
        // AFTER_INIT: screen layout is complete — run our screen-init logic.
        ScreenEvents.AFTER_INIT.register((client, screen, scaledWidth, scaledHeight) -> {
            InventoryOverlayHandler.onScreenInit(screen);
        });

        // BEFORE_INIT: register per-screen input/render events.
        // Must be done here (before the screen is ready) so the per-screen event objects
        // exist when the screen's own init() runs. Pattern taken from JEI's EventRegistration.
        ScreenEvents.BEFORE_INIT.register((client, screen, scaledWidth, scaledHeight) -> {
            if (!InventoryOverlayHandler.isAmiScreen(screen)) return;
            registerPerScreenEvents(screen);
        });
    }

    private static void registerPerScreenEvents(Screen screen) {
        // beforeRender: apply any pending reinit and capture partial tick before the screen (and
        // any tooltip mod hooking the normal vanilla tooltip point) renders this frame. Mirrors
        // NeoForge/Forge's ScreenEvent.Render.Pre.
        ScreenEvents.beforeRender(screen).register((s, guiGraphics, mouseX, mouseY, tickDelta) -> {
            InventoryOverlayHandler.consumePendingScreenReinit();
            InventoryOverlayHandler.onScreenBeforeRender(s, tickDelta);
        });

        // afterRender: container screens already rendered their durable body from
        // FabricContainerForegroundMixin (before the screen's own tooltip drew), so only AMI's
        // transient UI (tooltips, dropdowns, context menus) renders here. AMI-owned recipe/custom
        // screens have no container-foreground hook to fire from and no vanilla tooltip to
        // protect, so they still render both layers together here.
        ScreenEvents.afterRender(screen).register((s, guiGraphics, mouseX, mouseY, tickDelta) -> {
            if (InventoryOverlayHandler.isContainerScreen(s)) {
                InventoryOverlayHandler.renderTopLayerForContainerScreen(s, guiGraphics, mouseX, mouseY);
            } else {
                InventoryOverlayHandler.renderOverlayFrame(s, guiGraphics, mouseX, mouseY, tickDelta);
            }
        });

        // Mouse scroll: return false to cancel (consume) the event.
        Event<ScreenMouseEvents.AllowMouseScroll> mouseScrollEvent = ScreenMouseEvents.allowMouseScroll(screen);
        mouseScrollEvent.addPhaseOrdering(AMI_INPUT_PHASE, Event.DEFAULT_PHASE);
        mouseScrollEvent.register(AMI_INPUT_PHASE, (s, mouseX, mouseY, scrollX, scrollY) -> {
            if (!InventoryOverlayHandler.isAmiEnabled()) return true;
            boolean consumed = com.sanhiruzu.ami.client.OverlayInputController.mouseScrolled(
                    s, InventoryOverlayHandler.getManager(), true,
                    mouseX, mouseY, scrollX, scrollY);
            return !consumed;
        });

        // Mouse click: return false to cancel.
        Event<ScreenMouseEvents.AllowMouseClick> mouseClickEvent = ScreenMouseEvents.allowMouseClick(screen);
        mouseClickEvent.addPhaseOrdering(AMI_INPUT_PHASE, Event.DEFAULT_PHASE);
        mouseClickEvent.register(AMI_INPUT_PHASE, (s, mouseX, mouseY, button) -> {
            if (!InventoryOverlayHandler.isAmiEnabled()) return true;
            boolean consumed = com.sanhiruzu.ami.client.OverlayInputController.mouseButtonPressed(
                    s, InventoryOverlayHandler.getManager(), true,
                    mouseX, mouseY, button);
            return !consumed;
        });

        // Mouse release: return false to cancel.
        Event<ScreenMouseEvents.AllowMouseRelease> mouseReleaseEvent = ScreenMouseEvents.allowMouseRelease(screen);
        mouseReleaseEvent.addPhaseOrdering(AMI_INPUT_PHASE, Event.DEFAULT_PHASE);
        mouseReleaseEvent.register(AMI_INPUT_PHASE, (s, mouseX, mouseY, button) -> {
            if (!InventoryOverlayHandler.isAmiEnabled()) return true;
            boolean consumed = com.sanhiruzu.ami.client.OverlayInputController.mouseButtonReleased(
                    s, InventoryOverlayHandler.getManager(), true,
                    mouseX, mouseY, button);
            return !consumed;
        });

        // Key press: return false to cancel. We intentionally do NOT gate on isAmiEnabled() here — the
        // toggle-viewer keybind (and recipe-lookup keys) must still be processed when AMI is hidden behind
        // an external viewer (e.g. REI) so the user can toggle AMI back. OverlayInputController forwards to
        // AmiKeybindHandler, which handles the toggle regardless of the active layer; non-AMI keys return
        // unconsumed and pass through to the viewer/screen.
        Event<ScreenKeyboardEvents.AllowKeyPress> keyPressEvent = ScreenKeyboardEvents.allowKeyPress(screen);
        keyPressEvent.addPhaseOrdering(AMI_INPUT_PHASE, Event.DEFAULT_PHASE);
        keyPressEvent.register(AMI_INPUT_PHASE, (s, key, scancode, modifiers) -> {
            boolean consumed = com.sanhiruzu.ami.client.OverlayInputController.keyPressed(
                    s, InventoryOverlayHandler.getManager(), InventoryOverlayHandler.isAmiEnabled(),
                    key, scancode, modifiers);
            return !consumed;
        });
    }

    // -------------------------------------------------------------------------
    // World join / logout
    // -------------------------------------------------------------------------

    private static void registerWorldJoinEvents() {
        ClientPlayConnectionEvents.JOIN.register((handler, sender, client) -> {
            InventoryOverlayHandler.onPlayerLoggingIn();
        });
    }

    // -------------------------------------------------------------------------
    // Tooltip events — dev-mode tag/id augmentation
    // Mirrors NeoForge AmiDevModeHandler.onItemTooltip
    // -------------------------------------------------------------------------

    private static final int PREVIEW_METADATA_SHOWN = 5;
    private static final int MAX_EXPANDED_METADATA_SHOWN = 32;

    private static void registerTooltipEvents() {
        ItemTooltipCallback.EVENT.register((stack, tooltipContext, tooltipType, lines) -> {
            com.sanhiruzu.ami.client.ClassificationOverrideTooltipAppender.appendTo(stack, lines);
        });

        ItemTooltipCallback.EVENT.register((stack, tooltipContext, tooltipType, lines) -> {
            if (!AmiConfig.devMode && !AmiConfig.showTooltipTags) return;
            if (stack.isEmpty()) return;

            // 1. Registry Name
            ResourceLocation itemId = BuiltInRegistries.ITEM.getKey(stack.getItem());
            if (AmiConfig.devMode && itemId != null) {
                lines.add(Component.translatable("ami.dev.id", itemId.toString())
                        .withStyle(ChatFormatting.DARK_GRAY));
            }

            // 2. Tags
            List<String> tags = stack.getTags()
                    .map(tagKey -> tagKey.location().toString())
                    .sorted()
                    .toList();
            appendMetadataLines(lines, "ami.dev.tag", tags);

            if (!AmiConfig.devMode) return;

            // 3. Data Components
            List<String> components = new java.util.ArrayList<>();
            for (var typed : stack.getComponents()) {
                ResourceLocation compId = BuiltInRegistries.DATA_COMPONENT_TYPE.getKey(typed.type());
                if (compId != null) {
                    components.add(compId.toString());
                }
            }
            appendMetadataLines(lines, "ami.dev.comp", components.stream().sorted().toList());

            // 4. AMI Group
            String group = Component.translatable("ami.dev.group.none").getString();
            var nodeOpt = GlobalIndex.getInstance().getNode(BuiltInRegistries.ITEM.getKey(stack.getItem()));
            if (nodeOpt.isPresent()) {
                group = nodeOpt.get().meta("group", Component.translatable("ami.dev.group.default").getString());
            }
            lines.add(Component.translatable("ami.dev.group", group).withStyle(ChatFormatting.DARK_GRAY));
        });
    }

    private static void appendMetadataLines(List<Component> tooltip, String key, List<String> values) {
        if (values.isEmpty()) return;

        int limit = Screen.hasShiftDown() ? MAX_EXPANDED_METADATA_SHOWN : PREVIEW_METADATA_SHOWN;
        int shown = Math.min(values.size(), limit);
        for (int i = 0; i < shown; i++) {
            tooltip.add(Component.translatable(key, values.get(i)).withStyle(ChatFormatting.DARK_GRAY));
        }
        if (values.size() > shown) {
            String moreKey = Screen.hasShiftDown() ? "ami.tooltip.more_entries" : "ami.tooltip.more_entries_shift";
            tooltip.add(Component.translatable(moreKey, values.size() - shown).withStyle(ChatFormatting.DARK_GRAY));
        }
    }

    // -------------------------------------------------------------------------
    // Client commands — /ami [subcommand]
    // Mirrors NeoForge/Forge AmiClientCommands via Fabric's client-only command dispatcher.
    // -------------------------------------------------------------------------

    private static void registerClientCommands() {
        AmiClientCommands.register();
    }
}
