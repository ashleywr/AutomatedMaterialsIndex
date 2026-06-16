package com.sanhiruzu.ami.client;

import com.sanhiruzu.ami.index.GlobalIndex;
import com.sanhiruzu.ami.index.SearchNode;
import com.sanhiruzu.ami.index.SearchNodeKeys;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.SpawnEggItem;

import java.util.List;

public final class AmiTooltipHandler {
    private AmiTooltipHandler() {}

    public static void appendTooltip(ItemStack stack, List<Component> lines) {
        if (stack.isEmpty()) return;

        // 1. Entity info for Spawn Eggs
        if (stack.getItem() instanceof SpawnEggItem egg) {
            Identifier entityId = com.sanhiruzu.ami.platform.Services.PLATFORM.getSpawnEggEntityTypeId(egg, stack);
            if (entityId != null) {
                GlobalIndex.getInstance().getNode(entityId).ifPresent(node -> {
                    appendEntityInfo(node, lines);
                });
            }
        }
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
