package com.sanhiruzu.ami.api;

import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.renderer.Rect2i;
import net.minecraft.world.item.ItemStack;
import java.util.List;

/**
 * Interface for mods to provide AMI with layout-specific data.
 *
 * Mods that generate infinite modular item variants (Silent Gear, Apotheosis, etc.)
 * should NOT be enumerated by AMI — instead implement {@link #getHeroItems()} to
 * hand back a curated list of representative stacks (e.g. all-diamond pick, all-wood
 * pick) that stand in for the full item space.
 */
public interface IAmiPlugin {

    /**
     * Return a list of screen bounds where AMI should not render its overlay.
     */
    default List<Rect2i> getExclusionZones(Screen screen) {
        return List.of();
    }

    /**
     * Return a curated set of "Hero Items" to be indexed in place of full enumeration.
     *
     * Called once during index build. Use this for mods with procedurally generated
     * item variants where exhaustive enumeration is impractical or undesirable.
     * AMI will add each returned stack as a subtype node alongside regular items.
     *
     * @return representative ItemStacks, or an empty list if not applicable
     */
    default List<ItemStack> getHeroItems() {
        return List.of();
    }
}
