package com.sanhiruzu.ami.index.metrics;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.items.IItemHandler;

import java.util.Locale;
import java.util.OptionalLong;

final class VanillaStorageMetricAdapter implements StorageMetricAdapter {
    private static final long DEFAULT_STACK_SIZE = 64L;
    private static final int SHULKER_SLOTS = 27;

    @Override
    public OptionalLong estimate(ItemStack stack, ResourceLocation id) {
        OptionalLong componentCapacity = estimateContainerComponent(stack);
        if (componentCapacity.isPresent()) return componentCapacity;

        OptionalLong capabilityCapacity = estimateItemHandler(stack);
        if (capabilityCapacity.isPresent()) return capabilityCapacity;

        return estimateVanillaContainer(stack);
    }

    private static OptionalLong estimateContainerComponent(ItemStack stack) {
        net.minecraft.nbt.CompoundTag tag = stack.getTagElement("BlockEntityTag");
        if (tag == null || !tag.contains("Items", 9)) return OptionalLong.empty();
        int slots = SHULKER_SLOTS; // assume 27 for shulker-like
        return OptionalLong.of((long) slots * DEFAULT_STACK_SIZE);
    }

    private static OptionalLong estimateItemHandler(ItemStack stack) {
        IItemHandler handler = stack.getCapability(ForgeCapabilities.ITEM_HANDLER).orElse(null);
        if (handler == null || handler.getSlots() <= 0) {
            return OptionalLong.empty();
        }

        long capacity = 0L;
        for (int slot = 0; slot < handler.getSlots(); slot++) {
            capacity += Math.max(0, handler.getSlotLimit(slot));
        }
        return capacity > 0 ? OptionalLong.of(capacity) : OptionalLong.empty();
    }

    private static OptionalLong estimateVanillaContainer(ItemStack stack) {
        if (stack.is(Items.SHULKER_BOX)
                || stack.is(Items.WHITE_SHULKER_BOX)
                || stack.is(Items.ORANGE_SHULKER_BOX)
                || stack.is(Items.MAGENTA_SHULKER_BOX)
                || stack.is(Items.LIGHT_BLUE_SHULKER_BOX)
                || stack.is(Items.YELLOW_SHULKER_BOX)
                || stack.is(Items.LIME_SHULKER_BOX)
                || stack.is(Items.PINK_SHULKER_BOX)
                || stack.is(Items.GRAY_SHULKER_BOX)
                || stack.is(Items.LIGHT_GRAY_SHULKER_BOX)
                || stack.is(Items.CYAN_SHULKER_BOX)
                || stack.is(Items.PURPLE_SHULKER_BOX)
                || stack.is(Items.BLUE_SHULKER_BOX)
                || stack.is(Items.BROWN_SHULKER_BOX)
                || stack.is(Items.GREEN_SHULKER_BOX)
                || stack.is(Items.RED_SHULKER_BOX)
                || stack.is(Items.BLACK_SHULKER_BOX)) {
            return OptionalLong.of(SHULKER_SLOTS * DEFAULT_STACK_SIZE);
        }

        if (stack.getItem() instanceof BlockItem blockItem) {
            String blockName = blockItem.getBlock().getDescriptionId().toLowerCase(Locale.ROOT);
            if (blockName.endsWith(".chest") || blockName.endsWith(".barrel")) {
                return OptionalLong.of(27L * DEFAULT_STACK_SIZE);
            }
        }

        return OptionalLong.empty();
    }
}
