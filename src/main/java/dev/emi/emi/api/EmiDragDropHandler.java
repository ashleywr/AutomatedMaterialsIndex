package dev.emi.emi.api;

import net.minecraft.client.gui.screens.Screen;

public interface EmiDragDropHandler<T extends Screen> {
    boolean dropStack(T screen, dev.emi.emi.api.stack.EmiIngredient stack, int x, int y);
}
