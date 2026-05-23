package dev.emi.emi.api.stack;

import java.util.List;
import com.google.common.collect.Lists;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.world.inventory.tooltip.TooltipComponent;

public class ListEmiIngredient implements EmiIngredient {
    private final List<? extends EmiIngredient> ingredients;
    private final List<EmiStack> fullList;
    private long amount;
    private float chance = 1;

    public ListEmiIngredient(List<? extends EmiIngredient> ingredients, long amount) {
        this.ingredients = ingredients;
        this.fullList = ingredients.stream().flatMap(i -> i.getEmiStacks().stream()).toList();
        this.amount = amount;
    }

    @Override
    public EmiIngredient copy() {
        EmiIngredient copy = new ListEmiIngredient(ingredients, amount);
        copy.setChance(chance);
        return copy;
    }

    @Override
    public List<EmiStack> getEmiStacks() { return fullList; }

    @Override
    public long getAmount() { return amount; }

    @Override
    public EmiIngredient setAmount(long amount) { this.amount = amount; return this; }

    @Override
    public float getChance() { return chance; }

    @Override
    public EmiIngredient setChance(float chance) { this.chance = chance; return this; }

    @Override
    public void render(GuiGraphics draw, int x, int y, float delta, int flags) {
        int idx = (int) (System.currentTimeMillis() / 1000 % ingredients.size());
        EmiIngredient current = ingredients.get(idx);
        if ((flags & RENDER_ICON) != 0) current.render(draw, x, y, delta, -1 ^ RENDER_AMOUNT);
        if ((flags & RENDER_AMOUNT) != 0) current.copy().setAmount(amount).render(draw, x, y, delta, RENDER_AMOUNT);
    }

    @Override
    public List<TooltipComponent> getTooltip() {
        List<TooltipComponent> tooltip = Lists.newArrayList();
        int idx = (int) (System.currentTimeMillis() / 1000 % ingredients.size());
        tooltip.addAll(ingredients.get(idx).copy().setAmount(amount).getTooltip());
        return tooltip;
    }
}
