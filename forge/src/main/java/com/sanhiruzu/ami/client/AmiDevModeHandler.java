package com.sanhiruzu.ami.client;

import com.sanhiruzu.ami.config.AmiConfig;
import com.sanhiruzu.ami.forge.AMI;
import com.sanhiruzu.ami.index.GlobalIndex;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.entity.player.ItemTooltipEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = AMI.MODID, value = Dist.CLIENT)
public class AmiDevModeHandler {
    private static final int PREVIEW_METADATA_SHOWN = 5;
    private static final int MAX_EXPANDED_METADATA_SHOWN = 32;

    @SubscribeEvent
    public static void onItemTooltip(ItemTooltipEvent event) {
        if (!AmiConfig.devMode && !AmiConfig.showTooltipTags) return;

        ItemStack stack = event.getItemStack();
        if (stack.isEmpty()) return;

        // 1. Registry Name
        ResourceLocation itemId = net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(stack.getItem());
        if (AmiConfig.devMode && itemId != null) {
            event.getToolTip().add(Component.translatable("ami.dev.id", itemId.toString()).withStyle(ChatFormatting.DARK_GRAY));
        }

        // 2. Tags
        var tags = stack.getTags()
                .map(tagKey -> tagKey.location().toString())
                .sorted()
                .toList();
        appendMetadataLines(event.getToolTip(), "ami.dev.tag", tags);

        if (!AmiConfig.devMode) return;

        // 3. NBT Tag Keys
        if (stack.hasTag() && stack.getTag() != null) {
            java.util.List<String> keys = new java.util.ArrayList<>(stack.getTag().getAllKeys());
            appendMetadataLines(event.getToolTip(), "ami.dev.comp", keys.stream().sorted().toList());
        }

        // 4. AMI Group
        String group = Component.translatable("ami.dev.group.none").getString();
        var nodeOpt = GlobalIndex.getInstance().getNode(net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(stack.getItem()));
        if (nodeOpt.isPresent()) {
            group = nodeOpt.get().meta("group", Component.translatable("ami.dev.group.default").getString());
        }
        event.getToolTip().add(Component.translatable("ami.dev.group", group).withStyle(ChatFormatting.DARK_GRAY));
    }

    private static void appendMetadataLines(java.util.List<Component> tooltip, String key, java.util.List<String> values) {
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
}
