package com.sanhiruzu.ami.api;

import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.renderer.Rect2i;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

/**
 * AMI-specific plugin interface. Implement this for features that only AMI provides.
 * <p>
 * <b>Registration:</b> The easiest way to register is the Java ServiceLoader — create
 * {@code META-INF/services/com.sanhiruzu.ami.api.IAmiPlugin} in your jar containing
 * your implementation's fully-qualified class name. AMI discovers it automatically.
 * You can also call {@link AmiPluginRegistry#register} during client initialisation.
 * <p>
 * <b>Prefer viewer-neutral APIs</b> when the feature is not AMI-specific. The APIs in
 * {@code com.sanhiruzu.searchableitems.api} and {@code com.sanhiruzu.searchableguides.api}
 * (registered via {@link AmiApi}) also work in EMI, JEI, and other viewers that adopt
 * the shared contracts. Use this interface only for:
 * <ul>
 *   <li>Overlay exclusion zones ({@link #getExclusionZones})</li>
 *   <li>Hero / representative stacks for modular-item mods ({@link #getHeroItems})</li>
 *   <li>AMI result context-menu actions ({@link #addItemContextMenuActions})</li>
 * </ul>
 * <p>
 * <b>Modular-item mods</b> (Silent Gear, Apotheosis, etc.) should NOT let AMI enumerate
 * every generated variant. Implement {@link #getHeroItems} to return a curated set of
 * representative stacks (e.g. all-diamond pick, all-wood pick) that stand in for the
 * full item space.
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
     * Allows a mod to add indexing metadata for the given item stack.
     * <p>
     * This hook is called during item-index builds. Implementations must be defensive:
     * failures are isolated per-plugin, but throwing code can still prevent useful
     * metadata for the current item. Keep logic cheap and avoid blocking work.
     * <p>
     * <b>Prefer</b> {@code SearchableItemProvider.enrichItemMetadata} (registered via
     * {@link AmiApi#registerSearchableItemProvider}) if your enrichment should also
     * be visible to other item viewers.
     *
     * @param id       item id for the base registry entry
     * @param stack    concrete stack (including variant context if available)
     * @param level    world/level context (optional, may be null)
     * @param metadata mutable metadata map to enrich in-place
     */
    default void enrichItemMeta(Identifier id, ItemStack stack, Level level, Map<String, String> metadata) {
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
     * <p>
     * <b>Prefer</b> {@code SearchableGuideProvider} (registered via
     * {@link AmiApi#registerSearchableGuideProvider}) if your guide integration
     * should also be visible to other item viewers.
     */
    default void addGuideDocuments(Consumer<AmiGuideDocument> documents) {
    }
}
