package com.sanhiruzu.ami.api;

import com.sanhiruzu.ami.index.AmiQuestSearchIndex;
import net.minecraft.resources.ResourceLocation;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;

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
    private static final Logger LOGGER = Logger.getLogger(AmiQuestsApi.class.getName());
    private static final List<AmiQuestGroup> questGroups = new CopyOnWriteArrayList<>();
    private static final List<AmiQuestDocument> questDocuments = new CopyOnWriteArrayList<>();
    private static final List<Runnable> changeListeners = new CopyOnWriteArrayList<>();
    private static volatile AmiQuestSearchIndex questSearchIndex = new AmiQuestSearchIndex(List.of());
    private static Runnable legacyOnChange;

    private AmiQuestsApi() {
    }

    /**
     * Register a quest group. If a group with the same ID already exists, it is replaced.
     */
    public static void registerQuestGroup(AmiQuestGroup group) {
        if (group == null) {
            throw new IllegalArgumentException("Quest group must not be null");
        }
        questGroups.removeIf(g -> g.id().equals(group.id()));
        questGroups.add(group);
        questGroups.sort(Comparator.comparingInt(AmiQuestGroup::priority).thenComparing(AmiQuestGroup::id));
        rebuildQuestIndex();
        fireChange();
    }

    /**
     * Remove a previously registered quest group by its ID.
     */
    public static void removeQuestGroup(String id) {
        if (id == null || id.isBlank()) {
            return;
        }
        if (questGroups.removeIf(g -> g.id().equals(id))) {
            rebuildQuestIndex();
            fireChange();
        }
    }

    /**
     * Remove all quest groups registered by the given mod namespace.
     */
    public static void removeAllFromMod(String namespace) {
        if (namespace == null || namespace.isBlank()) {
            return;
        }
        String prefix = namespace + ":";
        boolean removedGroups = questGroups.removeIf(g -> g.id().startsWith(prefix));
        boolean removedDocuments = questDocuments.removeIf(g -> g.id().startsWith(prefix) || g.sourceId().equals(namespace));
        if (removedGroups || removedDocuments) {
            rebuildQuestIndex();
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
     * Register or replace a rich quest document.
     */
    public static void registerQuestDocument(AmiQuestDocument document) {
        if (document == null) {
            throw new IllegalArgumentException("Quest document must not be null");
        }
        questDocuments.removeIf(existing -> existing.id().equals(document.id()));
        questDocuments.add(document);
        questDocuments.sort(Comparator.comparing(AmiQuestDocument::id));
        rebuildQuestIndex();
        fireChange();
    }

    /**
     * Replace all rich quest documents from a source in one index rebuild.
     */
    public static void replaceQuestDocumentsFromSource(String sourceId, List<AmiQuestDocument> documents) {
        if (sourceId == null || sourceId.isBlank()) {
            throw new IllegalArgumentException("Quest source id must not be blank");
        }
        List<AmiQuestDocument> replacements = documents == null ? List.of() : List.copyOf(documents);
        for (AmiQuestDocument document : replacements) {
            if (document == null) {
                throw new IllegalArgumentException("Quest document must not be null");
            }
            if (!document.sourceId().equals(sourceId)) {
                throw new IllegalArgumentException("Quest document sourceId must match replacement source");
            }
        }
        boolean hadSourceDocuments = questDocuments.stream().anyMatch(document -> document.sourceId().equals(sourceId));
        if (!hadSourceDocuments && replacements.isEmpty()) {
            return;
        }
        questDocuments.removeIf(document -> document.sourceId().equals(sourceId));
        questDocuments.addAll(replacements);
        questDocuments.sort(Comparator.comparing(AmiQuestDocument::id));
        rebuildQuestIndex();
        fireChange();
    }

    public static void removeQuestDocument(String id) {
        if (id == null || id.isBlank()) {
            return;
        }
        if (questDocuments.removeIf(document -> document.id().equals(id))) {
            rebuildQuestIndex();
            fireChange();
        }
    }

    public static List<AmiQuestDocument> getQuestDocuments() {
        return Collections.unmodifiableList(questDocuments);
    }

    public static AmiQuestSearchIndex getQuestSearchIndex() {
        return questSearchIndex;
    }

    public static List<AmiQuestItemMatch> getQuestMatchesForItem(ResourceLocation itemId) {
        return questSearchIndex.findItem(itemId);
    }

    /**
     * Remove all quest groups. Useful when a quest provider reloads all data.
     */
    public static void clearQuestGroups() {
        if (!questGroups.isEmpty() || !questDocuments.isEmpty()) {
            questGroups.clear();
            questDocuments.clear();
            rebuildQuestIndex();
            fireChange();
        }
    }

    /**
     * Register a callback that fires whenever the quest group list changes.
     */
    public static void addOnChangeListener(Runnable callback) {
        if (callback != null && !changeListeners.contains(callback)) {
            changeListeners.add(callback);
        }
    }

    public static void removeOnChangeListener(Runnable callback) {
        if (callback != null) {
            changeListeners.remove(callback);
        }
    }

    /**
     * Register a callback that fires whenever the quest group list changes.
     * Only one callback is supported; setting a new one replaces the previous.
     */
    public static void setOnChange(Runnable callback) {
        if (legacyOnChange != null) {
            removeOnChangeListener(legacyOnChange);
        }
        legacyOnChange = callback;
        addOnChangeListener(callback);
    }

    private static void fireChange() {
        for (Runnable listener : changeListeners) {
            try {
                listener.run();
            } catch (RuntimeException e) {
                LOGGER.log(Level.WARNING, "AMI: Quest change listener failed", e);
            }
        }
    }

    private static void rebuildQuestIndex() {
        List<AmiQuestDocument> documents = new ArrayList<>(questDocuments);
        for (AmiQuestGroup group : questGroups) {
            documents.add(documentFromGroup(group));
        }
        questSearchIndex = new AmiQuestSearchIndex(documents);
    }

    private static AmiQuestDocument documentFromGroup(AmiQuestGroup group) {
        String sourceId = sourceId(group.id());
        AmiQuestDocument.Builder builder = AmiQuestDocument.builder(group.id(), "api", group.label().getString())
                .sourceId(sourceId)
                .chapterId(group.id())
                .chapterTitle(group.label().getString());
        int taskIndex = 0;
        for (AmiQuestEntry entry : group.entries()) {
            builder.task(AmiQuestTaskDocument.builder(group.id() + "/entry/" + taskIndex++, group.id(),
                            AmiQuestTaskDocument.Role.REQUIREMENT)
                    .taskType("item")
                    .itemId(entry.itemId())
                    .requiredCount(entry.requiredCount())
                    .build());
        }
        return builder.build();
    }

    private static String sourceId(String questId) {
        if (questId == null) {
            return "";
        }
        int colon = questId.indexOf(':');
        return colon > 0 ? questId.substring(0, colon) : "";
    }
}
