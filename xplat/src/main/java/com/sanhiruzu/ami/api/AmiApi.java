package com.sanhiruzu.ami.api;

import com.sanhiruzu.searchableguides.api.SearchableGuideProvider;
import com.sanhiruzu.searchableguides.api.SearchableGuideProviders;
import com.sanhiruzu.searchableitems.api.SearchableItemActionProvider;
import com.sanhiruzu.searchableitems.api.SearchableItemActionProviders;
import com.sanhiruzu.searchableitems.api.SearchableItemProvider;
import com.sanhiruzu.searchableitems.api.SearchableItemProviders;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Static façade for registering integrations with AMI.
 *
 * <p>See the {@link com.sanhiruzu.ami.api package overview} for a guide on which
 * entry point to use. The short version:
 * <ul>
 *   <li>For item metadata, actions, and guide docs that should also work in other
 *       viewers — use the {@code registerSearchable*} methods below.</li>
 *   <li>For AMI-specific behaviour (exclusion zones, hero items, context menus) —
 *       implement {@link IAmiPlugin} and register via {@link AmiPluginRegistry} or
 *       the ServiceLoader.</li>
 * </ul>
 */
public class AmiApi {
    private static final Logger LOGGER = Logger.getLogger(AmiApi.class.getName());
    private static final List<AmiScreenSuppressor> SUPPRESSORS = new ArrayList<>();

    // -------------------------------------------------------------------------
    // Overlay suppression
    // -------------------------------------------------------------------------

    /**
     * Register a screen suppressor. When the suppressor's predicate returns true,
     * AMI will hide its overlay and ignore input for that screen.
     */
    public static void registerScreenSuppressor(AmiScreenSuppressor suppressor) {
        SUPPRESSORS.add(suppressor);
    }

    /**
     * Check if AMI should be suppressed on the given screen.
     * Called by AMI's render system; not typically called by other mods.
     *
     * @return true if any registered suppressor returns true
     */
    public static boolean shouldSuppressAmi(Screen screen) {
        for (AmiScreenSuppressor suppressor : SUPPRESSORS) {
            if (suppressor.shouldSuppress(screen)) {
                return true;
            }
        }
        return false;
    }

    // -------------------------------------------------------------------------
    // Viewer-neutral item providers (also work in EMI, JEI, etc.)
    // -------------------------------------------------------------------------

    /**
     * Register a UI-agnostic searchable item provider for metadata enrichment
     * and representative generated stacks.
     */
    public static void registerSearchableItemProvider(SearchableItemProvider provider) {
        SearchableItemProviders.register(provider);
    }

    /**
     * Register a UI-agnostic item action provider. AMI adapts these actions into
     * result context menus, while other viewers may present them differently.
     */
    public static void registerSearchableItemActionProvider(SearchableItemActionProvider provider) {
        SearchableItemActionProviders.register(provider);
    }

    // -------------------------------------------------------------------------
    // Guide documents
    // -------------------------------------------------------------------------

    /**
     * Register a searchable guide/tutorial document.
     * <p>
     * AMI indexes guide documents separately from normal item results. Large
     * page bodies should be summarized or capped by the contributor.
     */
    public static void registerGuideDocument(AmiGuideDocument document) {
        AmiGuideRegistry.register(document);
    }

    /**
     * Register a guide source that can contribute multiple documents at once.
     * Source failures are isolated so one broken guide provider does not block
     * AMI indexing.
     */
    public static void registerGuideSource(AmiGuideSource source) {
        AmiGuideRegistry.registerSource(source);
    }

    /**
     * Register a UI-agnostic searchable guide provider.
     * <p>
     * Preferred over {@link #registerGuideSource} when your guide integration
     * may also be consumed by other viewers such as EMI or JEI.
     */
    public static void registerSearchableGuideProvider(SearchableGuideProvider provider) {
        SearchableGuideProviders.register(provider);
    }

    // -------------------------------------------------------------------------
    // Quest / task data
    // -------------------------------------------------------------------------

    /**
     * Register or replace a quest requirement group.
     */
    public static void registerQuestGroup(AmiQuestGroup group) {
        AmiQuestsApi.registerQuestGroup(group);
    }

    /**
     * Register or replace a rich searchable quest document.
     */
    public static void registerQuestDocument(AmiQuestDocument document) {
        AmiQuestsApi.registerQuestDocument(document);
    }

    /**
     * Replace all rich searchable quest documents from one source at once.
     */
    public static void replaceQuestDocumentsFromSource(String sourceId, List<AmiQuestDocument> documents) {
        AmiQuestsApi.replaceQuestDocumentsFromSource(sourceId, documents);
    }

    /**
     * Remove a quest requirement group by id.
     */
    public static void removeQuestGroup(String id) {
        AmiQuestsApi.removeQuestGroup(id);
    }

    /**
     * Remove a rich searchable quest document by id.
     */
    public static void removeQuestDocument(String id) {
        AmiQuestsApi.removeQuestDocument(id);
    }

    /**
     * Remove all quest groups whose ids use the given mod namespace.
     */
    public static void removeQuestGroupsFromMod(String namespace) {
        AmiQuestsApi.removeAllFromMod(namespace);
    }

    // -------------------------------------------------------------------------
    // Cheat / give
    // -------------------------------------------------------------------------

    /**
     * Gives a plugin-created item through AMI's cheat-give path.
     * <p>
     * This preserves components on custom stacks and follows AMI's normal
     * permission/config behavior for cursor-vs-inventory placement.
     */
    public static void cheatGiveItem(ItemStack stack) {
        if (stack == null || stack.isEmpty()) return;

        try {
            Class<?> cheatMode = Class.forName("com.sanhiruzu.ami.client.AMICheatMode");
            cheatMode.getMethod("giveItem", ItemStack.class).invoke(null, stack.copy());
        } catch (ReflectiveOperationException | RuntimeException | LinkageError e) {
            LOGGER.log(Level.WARNING, "AMI: Failed to run cheat-give plugin action", e);
        }
    }

    // -------------------------------------------------------------------------
    // Item deletion
    // -------------------------------------------------------------------------

    /**
     * Notify AMI that an item (waypoint, etc.) has been deleted from a third-party system.
     * AMI will immediately hide the item from search results and favorites while preserving
     * UI state (expanded categories, scroll position, etc.).
     * <p>
     * This is the proper way for plugins to notify AMI of deletions without knowing
     * anything about AMI's internal state or refresh mechanisms.
     *
     * @param nodeId the ResourceLocation ID of the deleted search node
     */
    public static void notifyItemDeleted(ResourceLocation nodeId) {
        if (nodeId == null) return;

        try {
            Class<?> handler = Class.forName("com.sanhiruzu.ami.client.overlay.ItemDeletionHandler");
            handler.getMethod("handleItemDeleted", ResourceLocation.class).invoke(null, nodeId);
        } catch (ReflectiveOperationException | RuntimeException | LinkageError e) {
            LOGGER.log(Level.FINE, "AMI: Failed to notify item deletion", e);
        }
    }
}
