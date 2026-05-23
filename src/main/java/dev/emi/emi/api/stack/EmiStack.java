package dev.emi.emi.api.stack;

import java.util.List;
import net.minecraft.world.inventory.tooltip.TooltipComponent;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.core.component.DataComponentPatch;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;

public abstract class EmiStack implements EmiIngredient {
    public static final EmiStack EMPTY = new EmptyEmiStack();
    protected long amount = 1;
    protected float chance = 1;

    @Override
    public List<EmiStack> getEmiStacks() { return List.of(this); }

    public abstract EmiStack copy();
    public abstract boolean isEmpty();
    public abstract Object getKey();
    public abstract ResourceLocation getId();
    public abstract ItemStack getItemStack();
    public abstract Component getName();

    public long getAmount() { return amount; }
    public EmiStack setAmount(long amount) { this.amount = amount; return this; }
    public float getChance() { return chance; }
    public EmiStack setChance(float chance) { this.chance = chance; return this; }

    public EmiStack getRemainder() { return EMPTY; }
    public EmiStack setRemainder(EmiStack stack) { return this; }

    public DataComponentPatch getComponentChanges() { return DataComponentPatch.EMPTY; }

    public boolean isEqual(EmiStack stack) {
        return getKey().equals(stack.getKey());
    }

    @Override
    public void render(net.minecraft.client.gui.GuiGraphics draw, int x, int y, float delta, int flags) {
        ItemStack is = getItemStack();
        if (!is.isEmpty()) {
            if ((flags & RENDER_ICON) != 0) draw.renderItem(is, x, y);
        }
    }

    public List<TooltipComponent> getTooltip() { return List.of(); }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof EmiStack s) return isEqual(s);
        return false;
    }

    @Override
    public int hashCode() { return getKey().hashCode(); }

    public static EmiStack of(ItemStack stack) {
        if (stack.isEmpty()) return EMPTY;
        return new ItemEmiStack(stack.getItem(), stack.getCount());
    }

    public static EmiStack of(ItemStack stack, long amount) {
        if (stack.isEmpty()) return EMPTY;
        return new ItemEmiStack(stack.getItem(), amount);
    }

    public static EmiStack of(net.minecraft.world.level.ItemLike item) {
        return of(item.asItem().getDefaultInstance(), 1);
    }

    public static EmiStack of(net.minecraft.world.level.ItemLike item, long amount) {
        return of(item.asItem().getDefaultInstance(), amount);
    }

    public static EmiStack of(Fluid fluid) {
        return EMPTY;
    }

    public static EmiStack of(Fluid fluid, long amount) {
        return EMPTY;
    }
}
