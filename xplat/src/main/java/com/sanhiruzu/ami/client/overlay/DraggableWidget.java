package com.sanhiruzu.ami.client.overlay;

import net.minecraft.client.gui.components.AbstractWidget;

/**
 * Interface for widgets that can be dragged in layout mode.
 */
public interface DraggableWidget {
    /**
     * Unique identifier for this widget (e.g., "search_bar", "left_panel", "right_panel")
     */
    String getId();

    /**
     * Check if a drag handle at (mx, my) was clicked.
     */
    boolean isDragHandleClicked(double mx, double my);

    /**
     * Move the widget to (x, y).
     */
    void setDragPosition(int x, int y);

    /**
     * Get the widget's current X position.
     */
    int getDragX();

    /**
     * Get the widget's current Y position.
     */
    int getDragY();

    /**
     * Get width (for bounds checking).
     */
    int getDragWidth();

    /**
     * Get height (for bounds checking).
     */
    int getDragHeight();

    /**
     * Get the underlying AbstractWidget if this is one.
     */
    AbstractWidget asWidget();
}
