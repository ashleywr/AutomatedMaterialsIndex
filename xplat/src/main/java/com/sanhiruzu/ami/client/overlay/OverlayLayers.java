package com.sanhiruzu.ami.client.overlay;

/**
 * Named z translations for screen rendering layers.
 *
 * Z-order is ascending: higher values render on top (closer to camera).
 * Promise: all Z values defined here; no magic numbers in rendering code.
 * Do not fix z-order bugs with ad hoc literals in individual widgets.
 */
public final class OverlayLayers {
    public static final int SCREEN = 0;              // Vanilla screen/menu base
    public static final int AMI_BASE = 200;          // AMI overlay base (panels, etc.)
    public static final int PANEL = AMI_BASE;        // Alias for compatibility
    public static final int SEARCH_BAR = 201;        // Search bar (just above panels)
    public static final int LAYOUT_MODE = 210;       // Layout mode UI (above panels)
    public static final int DROPDOWN = 400;          // Dropdowns on top of panels
    public static final int CONTEXT_MENU = 500;      // Context menus
    public static final int VANILLA_TOOLTIP = 1000;  // Vanilla Minecraft tooltip baseline
    public static final int TRANSIENT_TOOLTIP = 1100; // AMI's own tooltips, above item/result rendering
    public static final int DEBUG = 1200;            // Debug overlays

    private OverlayLayers() {
    }
}
