package com.sanhiruzu.ami.client;

import com.sanhiruzu.ami.index.GlobalIndex;
import com.sanhiruzu.ami.index.SearchNode;
import com.sanhiruzu.ami.index.SearchNodeKeys;
import net.minecraft.ChatFormatting;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.SpawnEggItem;

import java.util.List;

public final class AmiTooltipHandler {
    private AmiTooltipHandler() {}

    public static void appendTooltip(ItemStack stack, List<Component> lines) {
        if (stack.isEmpty()) return;

        // 1. Entity info for Spawn Eggs
        if (stack.getItem() instanceof SpawnEggItem egg) {
            ResourceLocation entityId = com.sanhiruzu.ami.platform.Services.PLATFORM.getSpawnEggEntityTypeId(egg, stack);
            if (entityId != null) {
                GlobalIndex.getInstance().getNode(entityId).ifPresent(node -> {
                    appendEntityInfo(node, lines);
                });
            }
        }

        // 2. Recipe / Uses counts — shown when the recipe index is ready
        if (com.sanhiruzu.ami.platform.Services.PLATFORM.isRecipeIndexBuilt()) {
            ResourceLocation itemId = BuiltInRegistries.ITEM.getKey(stack.getItem());
            if (itemId != null) {
                GlobalIndex.getInstance().getNode(itemId).ifPresent(node -> {
                    appendRecipeCounts(node, lines);
                });
            }
        }
    }

    private static void appendRecipeCounts(SearchNode node, List<Component> lines) {
        String recipes = node.meta(SearchNodeKeys.RECIPE_OUTPUT_COUNT, "");
        String uses    = node.meta(SearchNodeKeys.RECIPE_USE_COUNT, "");
        int rCount = recipes.isEmpty() ? 0 : parseInt(recipes);
        int uCount = uses.isEmpty()    ? 0 : parseInt(uses);
        if (rCount <= 0 && uCount <= 0) return;

        // Build a single compact hint line: "R: 3  U: 5"
        StringBuilder sb = new StringBuilder();
        if (rCount > 0) sb.append(ChatFormatting.AQUA).append("R: ").append(rCount);
        if (rCount > 0 && uCount > 0) sb.append(ChatFormatting.DARK_GRAY).append("  ");
        if (uCount > 0) sb.append(ChatFormatting.GREEN).append("U: ").append(uCount);
        lines.add(Component.literal(sb.toString()));
    }

    private static int parseInt(String s) {
        try { return Integer.parseInt(s.trim()); } catch (NumberFormatException e) { return 0; }
    }

    private static void appendEntityInfo(SearchNode node, List<Component> lines) {
        String hp = node.meta(SearchNodeKeys.ENTITY_HEALTH, "");
        if (!hp.isEmpty()) {
            lines.add(Component.translatable("ami.tooltip.entity.health", hp)
                    .withStyle(ChatFormatting.GRAY));
        }

        String dmg = node.meta(SearchNodeKeys.ENTITY_ATTACK_DAMAGE, "");
        if (!dmg.isEmpty()) {
            lines.add(Component.translatable("ami.tooltip.entity.damage", dmg)
                    .withStyle(ChatFormatting.GRAY));
        }
    }
}
