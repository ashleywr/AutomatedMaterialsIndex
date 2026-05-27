package com.sanhiruzu.ami.api;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import com.sanhiruzu.ami.AmiCore;
/**
 * Public API for registering quest requirement groups with AMI.
 *
 * <p>External mods (such as FTB Quests) call {@link #registerQuestGroup(AmiQuestGroup)}
 * to add a group of required items to the Quests sidebar panel. Groups are displayed
 * using the same grouping mechanism as the main results panels.</p>
 *
 * <pre>{@code
 * AmiQuestsApi.registerQuestGroup(new AmiQuestGroup(
 *     "ftbquests:basic_circuits",
 *     Component.literal("Basic Circuits"),
 *     List.of(
 *         new AmiQuestEntry(new ResourceLocation("redstone"), 12),
 *         new AmiQuestEntry(new ResourceLocation("iron_ingot"), 6)
 *     )
 * ));
 * }</pre>
 */
public class AmiQuestsApi {
    private static final List<AmiQuestGroup> questGroups = new CopyOnWriteArrayList<>();
    private static Runnable onChange;

    private AmiQuestsApi() {
    }

    /**
     * Register a quest group. If a group with the same ID already exists, it is replaced.
     */
    public static void registerQuestGroup(AmiQuestGroup group) {
        questGroups.removeIf(g -> g.id().equals(group.id()));
        questGroups.add(group);
        questGroups.sort(Comparator.comparingInt(AmiQuestGroup::priority));
        fireChange();
    }

    /**
     * Remove a previously registered quest group by its ID.
     */
    public static void removeQuestGroup(String id) {
        if (questGroups.removeIf(g -> g.id().equals(id))) {
            fireChange();
        }
    }

    /**
     * Remove all quest groups registered by the given mod namespace.
     */
    public static void removeAllFromMod(String namespace) {
        String prefix = namespace + ":";
        if (questGroups.removeIf(g -> g.id().startsWith(prefix))) {
            fireChange();
        }
    }

    /**
     * Returns an unmodifiable view of all registered quest groups, sorted by priority.
     */
    public static List<AmiQuestGroup> getQuestGroups() {
        return Collections.unmodifiableList(questGroups);
    }

    /**
     * Register a callback that fires whenever the quest group list changes.
     * Only one callback is supported; setting a new one replaces the previous.
     */
    public static void setOnChange(Runnable callback) {
        onChange = callback;
    }

    private static void fireChange() {
        if (onChange != null) {
            onChange.run();
        }
    }
}
