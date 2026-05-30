package com.sanhiruzu.ami.util;

import com.sanhiruzu.ami.client.AMICheatMode;
import com.sanhiruzu.ami.client.AMITheme;
import com.sanhiruzu.ami.client.AmiKeybindHandler;
import com.sanhiruzu.ami.client.icon.ItemIconRenderer;
import com.sanhiruzu.ami.client.icon.RendererRegistry;
import com.sanhiruzu.ami.client.results.DebugTooltip;
import com.sanhiruzu.ami.compat.RecipeViewerBridge;
import com.sanhiruzu.ami.config.AmiConfig;
import com.sanhiruzu.ami.index.NodeType;
import com.sanhiruzu.ami.index.SearchNode;
import com.sanhiruzu.ami.index.SearchNodeKeys;
import com.sanhiruzu.ami.index.providers.RegistryUtils;
import com.sanhiruzu.ami.platform.Services;
import com.sanhiruzu.ami.util.tooltip.AmiTooltipFacts;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.inventory.tooltip.TooltipComponent;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public final class AmiTooltipComposer {
    private AmiTooltipComposer() {
    }

    public static List<Component> buildTooltip(SearchNode entry) {
        if (AmiConfig.devMode && AmiKeybindHandler.isDebugTooltipsActive()) {
            return DebugTooltip.build(entry);
        }

        List<Component> lines = new ArrayList<>();

        // 1. Initial Content (Native tooltips or Specialized renderer body)
        if (entry.type() == NodeType.ITEM) {
            ItemStack stack = ItemIconRenderer.resolveStack(entry.id());
            if (!stack.isEmpty()) {
                lines.addAll(Screen.getTooltipFromItem(Minecraft.getInstance(), stack));
            } else {
                lines.add(Component.literal(entry.displayName()));
            }
        } else {
            // Header: Name and Type Label
            lines.add(Component.literal(entry.displayName()));
            lines.add(Component.translatable(entry.type().tooltipKey())
                    .withStyle(s -> s.withColor(AMITheme.TEXT_SUBTLE)));

            // Body: Delegate to specialized renderer
            var renderer = RendererRegistry.get(entry.type());
            if (renderer != null) {
                List<Component> body = renderer.getTooltip(entry);
                if (body != null) lines.addAll(body);
            }
        }

        // 2. AMI Details Section (Common metadata: Storage, FE, DPS, etc.)
        List<Component> amiDetails = buildAmiDetails(entry);
        if (!amiDetails.isEmpty()) {
            lines.add(Component.empty());
            lines.add(Component.translatable("ami.tooltip.ami_section").withStyle(ChatFormatting.DARK_AQUA));
            lines.addAll(amiDetails);
        }

        // 3. Branding & Identification (Unified mod name highlight)
        if (entry.type() != NodeType.ITEM) {
            // Item nodes already get mod branding via vanilla logic.
            // For all other types, we provide it here to match.
            String modId = entry.meta(SearchNodeKeys.MOD_ID, entry.id().getNamespace());
            String modName = RegistryUtils.modDisplayName(modId);
            lines.add(Component.literal(modName).withStyle(ChatFormatting.BLUE, ChatFormatting.ITALIC));
        }

        // 4. Interaction Hints (Footer)
        appendHints(lines, entry);

        return lines;
    }

    /**
     * Backwards-compatibility bridge for older code expecting buildItemTooltip.
     */
    public static List<Component> buildItemTooltip(SearchNode entry, ItemStack stack) {
        return buildTooltip(entry);
    }

    public static Optional<TooltipComponent> getTooltipImage(SearchNode node) {
        if (node.type() == NodeType.ITEM) {
            ItemStack stack = ItemIconRenderer.resolveStack(node.id());
            return stack.isEmpty() ? Optional.empty() : stack.getTooltipImage();
        } else {
            var renderer = RendererRegistry.get(node.type());
            return renderer != null ? renderer.getTooltipImage(node) : Optional.empty();
        }
    }

    /**
     * Legacy bridge for older code expecting getItemTooltipImage(ItemStack).
     */
    public static Optional<TooltipComponent> getItemTooltipImage(ItemStack stack) {
        return stack.isEmpty() ? Optional.empty() : stack.getTooltipImage();
    }

    private static List<Component> buildAmiDetails(SearchNode entry) {
        return AmiTooltipFacts.build(entry);
    }

    private static void appendHints(List<Component> lines, SearchNode entry) {
        lines.add(Component.empty());
        if (entry.type() == NodeType.ITEM) {
            appendCheatGiveHints(lines);
            ItemStack stack = ItemIconRenderer.resolveStack(entry.id());
            boolean canTransfer = RecipeViewerBridge.canTransferStack(stack);
            if (canTransfer && Screen.hasShiftDown()) {
                lines.add(Component.translatable("ami.gui.transfer_max_hint").withStyle(ChatFormatting.GRAY));
                lines.add(Component.translatable("ami.gui.uses_hint").withStyle(ChatFormatting.DARK_GRAY));
            } else if (canTransfer && Screen.hasControlDown()) {
                lines.add(Component.translatable("ami.gui.transfer_one_modified_hint").withStyle(ChatFormatting.GRAY));
                lines.add(Component.translatable("ami.gui.uses_hint").withStyle(ChatFormatting.DARK_GRAY));
            } else if (canTransfer) {
                lines.add(Component.translatable("ami.gui.transfer_one_hint").withStyle(ChatFormatting.GRAY));
                lines.add(Component.translatable("ami.gui.uses_hint").withStyle(ChatFormatting.DARK_GRAY));
            } else {
                lines.add(Component.translatable("ami.gui.recipes_hint").withStyle(ChatFormatting.DARK_GRAY));
                lines.add(Component.translatable("ami.gui.uses_hint").withStyle(ChatFormatting.DARK_GRAY));
                lines.add(Component.translatable("ami.gui.mod_filter_hint").withStyle(ChatFormatting.DARK_GRAY));
            }
        } else if (entry.type() == NodeType.ENTITY) {
            appendCheatGiveHints(lines);
        } else if ((entry.type() == NodeType.BIOME || entry.type() == NodeType.STRUCTURE)
                && AMICheatMode.isEnabled()) {
            lines.add(Component.translatable("ami.tooltip.cheat_locate").withStyle(ChatFormatting.GOLD));
        }

        if (AmiConfig.devMode) {
            String keybindName = Services.PLATFORM.keyMappings().debugTooltips().getTranslatedKeyMessage().getString();
            String hintKey = AmiKeybindHandler.isDebugTooltipsActive()
                    ? "ami.gui.debug_hint_active" : "ami.gui.debug_hint";
            lines.add(Component.translatable(hintKey, keybindName).withStyle(ChatFormatting.DARK_GRAY));
        }
    }

    private static void appendCheatGiveHints(List<Component> lines) {
        if (!AMICheatMode.isEnabled()) {
            return;
        }
        var keys = Services.PLATFORM.keyMappings();
        String giveOne = keys.cheatGiveOne().getTranslatedKeyMessage().getString();
        String giveStack = keys.cheatGiveStack().getTranslatedKeyMessage().getString();
        lines.add(Component.translatable("ami.tooltip.cheat_give_one", giveOne).withStyle(ChatFormatting.GOLD));
        lines.add(Component.translatable("ami.tooltip.cheat_give_stack", giveStack).withStyle(ChatFormatting.GOLD));
    }

}
