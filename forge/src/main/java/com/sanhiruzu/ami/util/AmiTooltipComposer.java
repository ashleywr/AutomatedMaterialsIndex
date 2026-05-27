package com.sanhiruzu.ami.util;

import com.sanhiruzu.ami.forge.client.AMIKeyMappings;
import com.sanhiruzu.ami.client.AMICheatMode;
import com.sanhiruzu.ami.client.AMITheme;
import com.sanhiruzu.ami.client.AmiKeybindHandler;
import com.sanhiruzu.ami.client.icon.ItemIconRenderer;
import com.sanhiruzu.ami.client.icon.RendererRegistry;
import com.sanhiruzu.ami.client.results.DebugTooltip;
import com.sanhiruzu.ami.index.NodeType;
import com.sanhiruzu.ami.index.SearchNode;
import com.sanhiruzu.ami.index.SearchNodeKeys;
import com.sanhiruzu.ami.index.providers.RegistryUtils;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.inventory.tooltip.TooltipComponent;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

import com.sanhiruzu.ami.forge.AMI;
/**
 * Master orchestrator for all AMI tooltips.
 * Unifies layout, branding, and styling across all node types (Items, Entities, Biomes, etc.).
 */
public final class AmiTooltipComposer {
    private AmiTooltipComposer() {
    }

    /**
     * Build the full list of components for a tooltip.
     */
    public static List<Component> buildTooltip(SearchNode entry) {
        if (AmiKeybindHandler.isDebugTooltipsActive()) {
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

        // Registry ID (Subtle gray, unified across all types)
        lines.add(Component.literal(entry.id().toString()).withStyle(s -> s.withColor(AMITheme.TEXT_SUBTLE)));

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

    /**
     * Returns the graphical component for the tooltip (e.g. Health hearts for entities).
     */
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
        List<Component> lines = new ArrayList<>();

        addRequiredTool(lines, entry);
        addIfPresent(lines, "ami.tooltip.storage", formatStorage(entry.meta(SearchNodeKeys.ESM_CAPACITY, "")));
        addIfPresent(lines, "ami.tooltip.energy", formatNumber(entry.meta(SearchNodeKeys.ENERGY_CAPACITY, ""), " FE"));
        addIfPresent(lines, "ami.tooltip.dps", suffix(entry.meta(SearchNodeKeys.DPS, ""), " DPS"));

        if (Screen.hasShiftDown()) {
            addIfPresent(lines, "ami.tooltip.mod", entry.meta(SearchNodeKeys.MOD_ID, entry.id().getNamespace()));
            addIfPresent(lines, "ami.tooltip.registry_id", entry.id().toString());
            addIfPresent(lines, "ami.tooltip.creative_tab", entry.meta(SearchNodeKeys.CREATIVE_TAB_LABEL, ""));
            addIfPresent(lines, "ami.tooltip.obtainability", formatObtainability(entry.meta(SearchNodeKeys.OBTAINABILITY, "")));
            addIfPresent(lines, "ami.tooltip.access", formatAccessLevel(entry.meta(SearchNodeKeys.ACCESS_LEVEL, "")));
        } else if (hasShiftOnlyDetails(entry)) {
            lines.add(Component.translatable("ami.tooltip.shift_for_details").withStyle(ChatFormatting.DARK_GRAY));
        }

        return lines;
    }

    private static void appendHints(List<Component> lines, SearchNode entry) {
        lines.add(Component.empty());
        if (entry.type() == NodeType.ITEM) {
            lines.add(Component.translatable("ami.gui.recipes_hint").withStyle(ChatFormatting.DARK_GRAY));
            lines.add(Component.translatable("ami.gui.uses_hint").withStyle(ChatFormatting.DARK_GRAY));
            lines.add(Component.translatable("ami.gui.mod_filter_hint").withStyle(ChatFormatting.DARK_GRAY));
        } else if ((entry.type() == NodeType.BIOME || entry.type() == NodeType.STRUCTURE)
                && AMICheatMode.isEnabled()) {
            lines.add(Component.translatable("ami.tooltip.cheat_locate").withStyle(ChatFormatting.GOLD));
        }

        String keybindName = AMIKeyMappings.DEBUG_TOOLTIPS.getTranslatedKeyMessage().getString();
        String hintKey = AmiKeybindHandler.isDebugTooltipsActive()
                ? "ami.gui.debug_hint_active" : "ami.gui.debug_hint";
        lines.add(Component.translatable(hintKey, keybindName).withStyle(ChatFormatting.DARK_GRAY));
    }

    private static boolean hasShiftOnlyDetails(SearchNode entry) {
        return !entry.meta(SearchNodeKeys.MOD_ID, "").isEmpty()
                || !entry.meta(SearchNodeKeys.CREATIVE_TAB_LABEL, "").isEmpty()
                || !entry.meta(SearchNodeKeys.ACCESS_LEVEL, "").isEmpty()
                || !entry.meta(SearchNodeKeys.OBTAINABILITY, "").isEmpty();
    }

    private static void addRequiredTool(List<Component> lines, SearchNode entry) {
        String reqToolStr = entry.meta(SearchNodeKeys.REQUIRED_TOOL, "");
        if (reqToolStr.isEmpty()) return;

        ResourceLocation toolId = ResourceLocation.tryParse(reqToolStr);
        if (toolId == null) return;

        Item toolItem = BuiltInRegistries.ITEM.get(toolId);
        if (toolItem == null || toolItem == net.minecraft.world.item.Items.AIR) return;

        lines.add(Component.translatable("ami.tooltip.required_tool", toolItem.getDescription())
                .withStyle(s -> s.withColor(AMITheme.TEXT_SUBTLE)));
    }

    private static void addIfPresent(List<Component> lines, String key, String value) {
        if (value == null || value.isBlank()) return;
        lines.add(Component.translatable(key, value).withStyle(s -> s.withColor(AMITheme.TEXT_SUBTLE)));
    }

    private static String formatStorage(String raw) {
        if (raw.isBlank()) return "";
        return formatNumber(raw, " items");
    }

    private static String formatNumber(String raw, String suffix) {
        if (raw.isBlank()) return "";
        try {
            long value = Long.parseLong(raw);
            return String.format(Locale.ROOT, "%,d%s", value, suffix);
        } catch (NumberFormatException ignored) {
            return raw + suffix;
        }
    }

    private static String suffix(String raw, String suffix) {
        return raw.isBlank() ? "" : raw + suffix;
    }

    private static String formatObtainability(String raw) {
        return switch (raw) {
            case "no_recipe" -> Component.translatable("ami.tooltip.obtainability.no_recipe").getString();
            default -> "";
        };
    }

    private static String formatAccessLevel(String raw) {
        return switch (raw) {
            case "survival" -> Component.translatable("ami.tooltip.access.survival").getString();
            case "creative" -> Component.translatable("ami.tooltip.access.creative").getString();
            case "cheat" -> Component.translatable("ami.tooltip.access.cheat").getString();
            case "dev" -> Component.translatable("ami.tooltip.access.dev").getString();
            default -> "";
        };
    }
}
