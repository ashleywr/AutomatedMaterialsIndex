package com.sanhiruzu.ami.fabric.client;

import com.mojang.brigadier.CommandDispatcher;
import com.sanhiruzu.ami.client.EntityIconCache;
import com.sanhiruzu.ami.client.InventoryOverlayHandler;
import com.sanhiruzu.ami.client.ItemIconCache;
import com.sanhiruzu.ami.client.icon.RendererRegistry;
import com.sanhiruzu.ami.config.AmiConfig;
import com.sanhiruzu.ami.fabric.AmiFabric;
import com.sanhiruzu.ami.index.AmiIndexerService;
import com.sanhiruzu.ami.index.GlobalIndex;
import com.sanhiruzu.ami.index.GlobalIndexCache;
import com.sanhiruzu.ami.index.SearchNode;
import com.sanhiruzu.ami.index.SearchNodeMirrorDump;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.fabricmc.fabric.api.client.item.v1.ItemTooltipCallback;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents;
import net.fabricmc.fabric.api.client.screen.v1.ScreenKeyboardEvents;
import net.fabricmc.fabric.api.client.screen.v1.ScreenMouseEvents;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

import java.nio.file.Files;
import java.nio.file.Path;
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
        // afterRender: both base and top layer in screen-space coordinates.
        // On Fabric there is no container-foreground hook, so AMI renders after vanilla.
        ScreenEvents.afterRender(screen).register((s, guiGraphics, mouseX, mouseY, tickDelta) -> {
            InventoryOverlayHandler.renderOverlayFrame(s, guiGraphics, mouseX, mouseY, tickDelta);
        });

        // Mouse scroll: return false to cancel (consume) the event.
        ScreenMouseEvents.allowMouseScroll(screen).register((s, mouseX, mouseY, scrollX, scrollY) -> {
            if (!InventoryOverlayHandler.isAmiEnabled()) return true;
            boolean consumed = com.sanhiruzu.ami.client.OverlayInputController.mouseScrolled(
                    s, InventoryOverlayHandler.getManager(), true,
                    mouseX, mouseY, scrollX, scrollY);
            return !consumed;
        });

        // Mouse click: return false to cancel.
        ScreenMouseEvents.allowMouseClick(screen).register((s, mouseX, mouseY, button) -> {
            if (!InventoryOverlayHandler.isAmiEnabled()) return true;
            boolean consumed = com.sanhiruzu.ami.client.OverlayInputController.mouseButtonPressed(
                    s, InventoryOverlayHandler.getManager(), true,
                    mouseX, mouseY, button);
            return !consumed;
        });

        // Mouse release: return false to cancel.
        ScreenMouseEvents.allowMouseRelease(screen).register((s, mouseX, mouseY, button) -> {
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
        ScreenKeyboardEvents.allowKeyPress(screen).register((s, key, scancode, modifiers) -> {
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
            if (!AmiConfig.devMode && !AmiConfig.showTooltipTags) return;
            if (stack.isEmpty()) return;

            // 1. Registry Name
            ResourceLocation itemId = BuiltInRegistries.ITEM.getKey(stack.getItem());
            if (AmiConfig.devMode) {
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
                components.add(compId.toString());
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
    // Mirrors NeoForge AmiClientCommands (dump commands requiring RecipeDumpWriters skipped)
    // -------------------------------------------------------------------------

    private static void registerClientCommands() {
        ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) ->
                registerAmiCommand(dispatcher));
    }

    private static void registerAmiCommand(
            CommandDispatcher<FabricClientCommandSource> dispatcher) {
        var cmd = net.fabricmc.fabric.api.client.command.v2.ClientCommandManager.literal("ami")
                .then(net.fabricmc.fabric.api.client.command.v2.ClientCommandManager.literal("dump-search-nodes")
                        .executes(context -> {
                            exportSearchNodes(context.getSource());
                            return 1;
                        })
                )
                .then(net.fabricmc.fabric.api.client.command.v2.ClientCommandManager.literal("reindex")
                        .executes(context -> {
                            invalidateAndRebuildIndex(context.getSource());
                            return 1;
                        })
                );

        dispatcher.register(cmd);
    }

    private static void exportSearchNodes(FabricClientCommandSource source) {
        Path dumpDir = dumpDir("search");
        try {
            Files.createDirectories(dumpDir);
            Path out = dumpDir.resolve("search_nodes.jsonl");
            Path meta = dumpDir.resolve("search_nodes.meta.json");
            List<SearchNode> nodes = SearchNodeMirrorDump.runtimeAtlasNodes();
            int count = SearchNodeMirrorDump.writeJsonl(out, nodes);
            SearchNodeMirrorDump.writeMeta(meta);
            source.sendFeedback(Component.literal(
                    "AMI search node mirror written to " + out.toAbsolutePath()
                            + " (" + count + " nodes) + metadata at " + meta.toAbsolutePath())
                    .withStyle(ChatFormatting.GREEN));
        } catch (Exception e) {
            AmiFabric.LOGGER.error("Failed to export search node mirror", e);
            source.sendFeedback(Component.literal(
                    "Failed to export AMI search node mirror: " + e.getMessage())
                    .withStyle(ChatFormatting.RED));
        }
    }

    private static void invalidateAndRebuildIndex(FabricClientCommandSource source) {
        try {
            boolean deleted = GlobalIndexCache.invalidateCurrent();
            boolean accepted = AmiIndexerService.getInstance().rebuild(true);
            if (accepted) {
                ItemIconCache.invalidate();
                RendererRegistry.invalidateAll();
                EntityIconCache.invalidateAndPurgePersistentCache();
                source.sendFeedback(Component.literal("AMI index cache "
                        + (deleted ? "deleted" : "was already absent")
                        + "; forced reindex and icon cache reset started")
                        .withStyle(ChatFormatting.GREEN));
            } else {
                source.sendFeedback(Component.literal("AMI index cache "
                        + (deleted ? "deleted" : "was already absent")
                        + ", but a reindex is already running")
                        .withStyle(ChatFormatting.YELLOW));
            }
        } catch (Exception e) {
            AmiFabric.LOGGER.error("Failed to invalidate AMI index cache", e);
            source.sendFeedback(Component.literal(
                    "Failed to invalidate AMI index cache: " + e.getMessage())
                    .withStyle(ChatFormatting.RED));
        }
    }

    private static Path dumpDir(String category) {
        return FabricLoader.getInstance().getGameDir().resolve("ami_dumps").resolve(category);
    }
}
