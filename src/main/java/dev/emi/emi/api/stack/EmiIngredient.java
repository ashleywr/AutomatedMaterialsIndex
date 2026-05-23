package dev.emi.emi.api.stack;

import java.util.List;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.world.inventory.tooltip.TooltipComponent;
import net.minecraft.world.item.crafting.Ingredient;
import dev.emi.emi.api.render.EmiRenderable;

public interface EmiIngredient extends EmiRenderable {
    int RENDER_ICON = 1;
    int RENDER_AMOUNT = 2;
    int RENDER_INGREDIENT = 4;
    int RENDER_REMAINDER = 8;

    List<EmiStack> getEmiStacks();

    default boolean isEmpty() {
        for (EmiStack stack : getEmiStacks()) {
            if (!stack.isEmpty()) return false;
        }
        return true;
    }

    EmiIngredient copy();
    long getAmount();
    EmiIngredient setAmount(long amount);
    float getChance();
    EmiIngredient setChance(float chance);

    @Override
    default void render(GuiGraphics draw, int x, int y, float delta) {
        render(draw, x, y, delta, -1);
    }

    void render(GuiGraphics draw, int x, int y, float delta, int flags);

    List<TooltipComponent> getTooltip();

    static boolean areEqual(EmiIngredient a, EmiIngredient b) {
        List<EmiStack> as = a.getEmiStacks();
        List<EmiStack> bs = b.getEmiStacks();
        if (as.size() != bs.size()) return false;
        for (int i = 0; i < as.size(); i++) {
            if (!as.get(i).isEqual(bs.get(i))) return false;
        }
        return true;
    }

    static EmiIngredient of(Ingredient ingredient) {
        if (ingredient == null || ingredient.isEmpty()) return EmiStack.EMPTY;
        return of(ingredient, 1);
    }

    static EmiIngredient of(Ingredient ingredient, long amount) {
        if (ingredient == null || ingredient.isEmpty()) return EmiStack.EMPTY;
        var stacks = java.util.Arrays.stream(ingredient.getItems()).map(EmiStack::of).toList();
        if (stacks.isEmpty()) return EmiStack.EMPTY;
        if (stacks.size() == 1) return stacks.get(0).copy().setAmount(amount);
        return new ListEmiIngredient(stacks.stream().map(s -> (EmiIngredient) s).toList(), amount);
    }

    static EmiIngredient of(List<? extends EmiIngredient> list) {
        return of(list, 1);
    }

    static EmiIngredient of(List<? extends EmiIngredient> list, long amount) {
        if (list.isEmpty()) return EmiStack.EMPTY;
        if (list.size() == 1) return list.get(0).copy().setAmount(amount);
        return new ListEmiIngredient(list, amount);
    }
}
