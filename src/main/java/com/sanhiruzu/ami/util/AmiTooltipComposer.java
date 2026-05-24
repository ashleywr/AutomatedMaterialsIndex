package com.sanhiruzu.ami.util;

import com.sanhiruzu.ami.client.AMIKeyMappings;
import com.sanhiruzu.ami.client.AMITheme;
import com.sanhiruzu.ami.client.AmiKeybindHandler;
import com.sanhiruzu.ami.index.SearchNode;
import com.sanhiruzu.ami.index.SearchNodeKeys;
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

public final class AmiTooltipComposer {
    private AmiTooltipComposer() {
    }

    public static List<Component> buildItemTooltip(SearchNode entry, ItemStack stack) {
        List<Component> lines = new ArrayList<>();
        if (!stack.isEmpty()) {
            lines.addAll(Screen.getTooltipFromItem(Minecraft.getInstance(), stack));
        } else {
            lines.add(Component.literal(entry.displayName()));
        }

        List<Component> amiDetails = buildAmiDetails(entry);
        if (!amiDetails.isEmpty()) {
            lines.add(Component.empty());
            lines.add(Component.translatable("ami.tooltip.ami_section").withStyle(ChatFormatting.DARK_AQUA));
            lines.addAll(amiDetails);
        }

        lines.add(Component.empty());
        lines.add(Component.translatable("ami.gui.recipes_hint").withStyle(ChatFormatting.DARK_GRAY));
        lines.add(Component.translatable("ami.gui.uses_hint").withStyle(ChatFormatting.DARK_GRAY));
        lines.add(Component.translatable("ami.gui.mod_filter_hint").withStyle(ChatFormatting.DARK_GRAY));

        String keybindName = AMIKeyMappings.DEBUG_TOOLTIPS.getTranslatedKeyMessage().getString();
        String hintKey = AmiKeybindHandler.isDebugTooltipsActive()
                ? "ami.gui.debug_hint_active" : "ami.gui.debug_hint";
        lines.add(Component.translatable(hintKey, keybindName).withStyle(ChatFormatting.DARK_GRAY));
        return lines;
    }

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
