package com.sanhiruzu.ami.client.overlay;

/**
 * Named z translations for AMI's painter-order overlay pass.
 *
 * Promise: render panel bodies first, then each higher layer in ascending order.
 * Do not fix z-order bugs with ad hoc literals in individual widgets.
 */
public final class OverlayLayers {
    public static final int PANEL = 100;
    public static final int DROPDOWN = 400;
    public static final int CONTEXT_MENU = 500;
    public static final int TRANSIENT_TOOLTIP = 600;
    public static final int DEBUG = 700;

    private OverlayLayers() {
    }
}
