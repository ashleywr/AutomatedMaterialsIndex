package dev.emi.emi.api;

import dev.emi.emi.api.widget.Bounds;
import net.minecraft.client.gui.screens.Screen;

public interface EmiScreenBoundsProvider<T extends Screen> {
    Bounds getBounds(T screen);
}
