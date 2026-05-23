package dev.emi.emi.api.widget;

import dev.emi.emi.api.stack.EmiIngredient;
import net.minecraft.client.gui.GuiGraphics;

public class SlotWidget extends Widget {
    private final EmiIngredient ingredient;
    private final int x, y;
    private final boolean output;

    public SlotWidget(EmiIngredient ingredient, int x, int y) {
        this.ingredient = ingredient;
        this.x = x;
        this.y = y;
        this.output = false;
    }

    public EmiIngredient getStack() { return ingredient; }

    public Bounds getBounds() {
        return output ? new Bounds(x, y, 26, 26) : new Bounds(x, y, 18, 18);
    }

    @Override
    public void render(GuiGraphics draw, int mouseX, int mouseY, float delta) {}
}
