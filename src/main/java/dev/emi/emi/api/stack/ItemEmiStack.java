package dev.emi.emi.api.stack;

import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.core.component.DataComponentPatch;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;

public class ItemEmiStack extends EmiStack {
    private final Item item;

    public ItemEmiStack(ItemStack stack) {
        this(stack.getItem(), stack.getCount());
    }

    public ItemEmiStack(ItemStack stack, long amount) {
        this(stack.getItem(), amount);
    }

    public ItemEmiStack(Item item, long amount) {
        this.item = item;
        this.amount = amount;
    }

    @Override
    public ItemStack getItemStack() {
        return new ItemStack(item, (int) amount);
    }

    @Override
    public EmiStack copy() {
        return new ItemEmiStack(item, amount);
    }

    @Override
    public boolean isEmpty() {
        return amount == 0 || item == Items.AIR;
    }

    @Override
    public Object getKey() {
        return item;
    }

    @Override
    public ResourceLocation getId() {
        return BuiltInRegistries.ITEM.getKey(item);
    }

    @Override
    public Component getName() {
        return getItemStack().getHoverName();
    }

    @Override
    public DataComponentPatch getComponentChanges() {
        return DataComponentPatch.EMPTY;
    }
}
