package com.sanhiruzu.ami.api;

import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.renderer.Rect2i;
import net.minecraft.world.item.ItemStack;

import java.util.List;
import java.util.function.Consumer;

/**
 * Interface for mods to provide AMI with layout-specific data.
 * <p>
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
     * <p>
     * Called once during index build. Use this for mods with procedurally generated
     * item variants where exhaustive enumeration is impractical or undesirable.
     * AMI will add each returned stack as a subtype node alongside regular items.
     * This is also the supported path for curated pre-enchanted non-book items.
     *
     * @return representative ItemStacks, or an empty list if not applicable
     */
    default List<ItemStack> getHeroItems() {
        return List.of();
    }

    /**
     * Allows a mod to append actions to AMI's item result context menu.
     * <p>
     * Called client-side when the player opens the right-click menu for an item
     * result. Actions should do their own permission/config checks before
     * registering. Cheat actions can use {@code context.cheatEnabled()} to match
     * AMI's built-in cheat-mode visibility.
     */
    default void addItemContextMenuActions(AmiItemContext context, Consumer<AmiContextMenuAction> actions) {
    }

    /**
     * Allows a mod to contribute searchable guide/tutorial documents.
     * <p>
     * Documents should be lightweight and stable. Large guide bodies should be
     * summarized or provided through a bounded text field; the owning mod should
     * keep responsibility for opening the actual guide UI through each document's
     * optional open action.
     */
    default void addGuideDocuments(Consumer<AmiGuideDocument> documents) {
    }
}
