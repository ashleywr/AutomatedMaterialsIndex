package com.sanhiruzu.ami.api;

import net.minecraft.client.gui.screens.Screen;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Public API for interacting with AMI from other mods.
 * Currently provides screen suppression for mods that want to hide AMI's overlay.
 */
public class AmiApi {
    private static final Logger LOGGER = Logger.getLogger(AmiApi.class.getName());
    private static final List<AmiScreenSuppressor> SUPPRESSORS = new ArrayList<>();

    /**
     * Register a screen suppressor. When the suppressor's predicate returns true,
     * AMI will hide its overlay and ignore input for that screen.
     *
     * @param suppressor the predicate that determines if AMI should be suppressed
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
     * Source failures are isolated so one broken guide provider does not break
     * AMI indexing.
     */
    public static void registerGuideSource(AmiGuideSource source) {
        AmiGuideRegistry.registerSource(source);
    }

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
     * Replace all rich searchable quest documents from one source.
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
}
