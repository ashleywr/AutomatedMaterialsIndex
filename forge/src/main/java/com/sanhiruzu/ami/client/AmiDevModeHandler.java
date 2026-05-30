package com.sanhiruzu.ami.client;

import com.sanhiruzu.ami.config.AmiConfig;
import com.sanhiruzu.ami.forge.AMI;
import com.sanhiruzu.ami.index.GlobalIndex;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.entity.player.ItemTooltipEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = AMI.MODID, value = Dist.CLIENT)
public class AmiDevModeHandler {

    @SubscribeEvent
    public static void onItemTooltip(ItemTooltipEvent event) {
        if (!AmiConfig.devMode) return;

        ItemStack stack = event.getItemStack();
        if (stack.isEmpty()) return;

        // 1. Registry Name
        ResourceLocation itemId = net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(stack.getItem());
        event.getToolTip().add(Component.translatable("ami.dev.id", itemId.toString()).withStyle(ChatFormatting.DARK_GRAY));

        // 2. Tags
        stack.getTags().forEach(tagKey -> {
            event.getToolTip().add(Component.translatable("ami.dev.tag", tagKey.location().toString()).withStyle(ChatFormatting.DARK_GRAY));
        });

        // 3. NBT Tag Keys
        if (stack.hasTag() && stack.getTag() != null) {
            for (String key : stack.getTag().getAllKeys()) {
                event.getToolTip().add(Component.translatable("ami.dev.comp", key).withStyle(ChatFormatting.DARK_GRAY));
            }
        }

        // 4. AMI Group
        String group = Component.translatable("ami.dev.group.none").getString();
        var nodeOpt = GlobalIndex.getInstance().getNode(net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(stack.getItem()));
        if (nodeOpt.isPresent()) {
            group = nodeOpt.get().meta("group", Component.translatable("ami.dev.group.default").getString());
        }
        event.getToolTip().add(Component.translatable("ami.dev.group", group).withStyle(ChatFormatting.DARK_GRAY));
    }
}
