package com.sanhiruzu.ami.api;

import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.renderer.Rect2i;
import java.util.List;

/**
 * Interface for mods to provide AMI with layout-specific data,
 * such as screen exclusion zones.
 */
public interface IAmiPlugin {

    /**
     * Return a list of screen bounds where AMI should not render its overlay.
     */
    default List<Rect2i> getExclusionZones(Screen screen) {
        return List.of();
    }
}
