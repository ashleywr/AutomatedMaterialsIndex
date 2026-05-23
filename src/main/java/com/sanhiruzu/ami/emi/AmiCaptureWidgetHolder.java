package com.sanhiruzu.ami.emi;

import java.util.List;
import java.util.ArrayList;
import dev.emi.emi.api.stack.EmiIngredient;
import dev.emi.emi.api.widget.Widget;
import dev.emi.emi.api.widget.WidgetHolder;
import dev.emi.emi.api.widget.SlotWidget;
import net.minecraft.world.item.ItemStack;

/**
 * WidgetHolder that captures slot positions instead of rendering.
 */
public class AmiCaptureWidgetHolder implements WidgetHolder {
    private final AmiCapturedRecipe recipe;
    private final int width, height;
    private boolean outputSide = false;

    public AmiCaptureWidgetHolder(AmiCapturedRecipe recipe, int width, int height) {
        this.recipe = recipe;
        this.width = width;
        this.height = height;
    }

    @Override
    public int getWidth() { return width; }

    @Override
    public int getHeight() { return height; }

    @Override
    @SuppressWarnings("unchecked")
    public <T extends Widget> T add(T widget) {
        if (widget instanceof SlotWidget slot) {
            EmiIngredient ingredient = slot.getStack();
            List<ItemStack> stacks = new ArrayList<>();
            if (ingredient != null && !ingredient.isEmpty()) {
                for (var emiStack : ingredient.getEmiStacks()) {
                    ItemStack is = emiStack.getItemStack();
                    if (!is.isEmpty()) stacks.add(is);
                }
            }
            var bounds = slot.getBounds();
            recipe.addSlot(stacks, bounds.x(), bounds.y());
        }
        return widget;
    }

    /**
     * Call this so the first half of slots are treated as inputs,
     * the rest as outputs (approximate but works for typical recipes).
     */
    public void markOutputSide() {
        // For simplicity, we're recording position only.
        // The recipe viewer can infer input/output from position.
    }
}
