package com.sanhiruzu.ami.util.tooltip;

import com.sanhiruzu.ami.client.AMITheme;
import com.sanhiruzu.ami.index.SearchNode;
import com.sanhiruzu.ami.index.SearchNodeKeys;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.Item;

import java.util.List;

public final class RequiredToolTooltipFact implements AmiTooltipFact {
    @Override
    public List<Component> build(SearchNode entry) {
        String reqToolStr = entry.meta(SearchNodeKeys.REQUIRED_TOOL, "");
        if (reqToolStr.isEmpty()) return List.of();

        Identifier toolId = Identifier.tryParse(reqToolStr);
        if (toolId == null) return List.of();

        Item toolItem = BuiltInRegistries.ITEM.getValue(toolId);
        if (toolItem == null || toolItem == net.minecraft.world.item.Items.AIR) return List.of();

        return List.of(Component.translatable("ami.tooltip.required_tool", toolItem.getName(new net.minecraft.world.item.ItemStack(toolItem)))
                .withStyle(s -> s.withColor(AMITheme.TEXT_SUBTLE)));
    }
}
