package com.sanhiruzu.ami.api;

import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class AmiQuestsApiTest {
    @BeforeEach
    void setup() {
        AmiQuestsApi.clearQuestGroups();
        AmiQuestsApi.setOnChange(null);
    }

    @AfterEach
    void tearDown() {
        AmiQuestsApi.clearQuestGroups();
        AmiQuestsApi.setOnChange(null);
    }

    @Test
    void registerReplacesByIdAndSortsByPriorityThenId() {
        AmiQuestsApi.registerQuestGroup(group("ftbquests:late", 20, "minecraft:stone"));
        AmiQuestsApi.registerQuestGroup(group("ftbquests:first_b", 0, "minecraft:apple"));
        AmiQuestsApi.registerQuestGroup(group("ftbquests:first_a", 0, "minecraft:carrot"));
        AmiQuestsApi.registerQuestGroup(group("ftbquests:late", 5, "minecraft:diamond"));

        assertEquals(List.of("ftbquests:first_a", "ftbquests:first_b", "ftbquests:late"),
                AmiQuestsApi.getQuestGroups().stream().map(AmiQuestGroup::id).toList());
        assertEquals(new Identifier("minecraft", "diamond"),
                AmiQuestsApi.getQuestGroups().get(2).entries().getFirst().itemId());
    }

    @Test
    void richQuestDocumentsAreIndexedByItemAndSearchText() {
        AmiQuestDocument document = questDocument(
                "ftbquests:chapter/basic_power",
                "Basic Power",
                "Make a generator and wire it to storage.",
                new Identifier("minecraft", "redstone")
        );

        AmiQuestsApi.registerQuestDocument(document);

        assertEquals(List.of(document), AmiQuestsApi.getQuestDocuments());
        assertEquals("Basic Power",
                AmiQuestsApi.getQuestMatchesForItem(new Identifier("minecraft", "redstone"))
                        .getFirst()
                        .quest()
                        .title());
        assertEquals(List.of(document), AmiQuestsApi.getQuestSearchIndex().search("generator redstone"));
    }

    @Test
    void replaceQuestDocumentsFromSourceRemovesOnlyThatSourceAndFiresOnce() {
        AtomicInteger changes = new AtomicInteger();
        Runnable listener = changes::incrementAndGet;
        AmiQuestsApi.addOnChangeListener(listener);
        AmiQuestDocument ftbOld = questDocument(
                "ftbquests:old",
                "Old",
                "",
                new Identifier("minecraft", "stone")
        );
        AmiQuestDocument other = questDocument(
                "otherquests:kept",
                "otherquests",
                "Kept",
                "",
                new Identifier("minecraft", "apple")
        );
        AmiQuestDocument ftbNew = questDocument(
                "ftbquests:new",
                "New",
                "",
                new Identifier("minecraft", "diamond")
        );

        AmiQuestsApi.registerQuestDocument(ftbOld);
        AmiQuestsApi.registerQuestDocument(other);
        changes.set(0);

        AmiQuestsApi.replaceQuestDocumentsFromSource("ftbquests", List.of(ftbNew));

        assertEquals(1, changes.get());
        assertEquals(List.of("ftbquests:new", "otherquests:kept"),
                AmiQuestsApi.getQuestDocuments().stream().map(AmiQuestDocument::id).toList());
        assertEquals(0, AmiQuestsApi.getQuestMatchesForItem(new Identifier("minecraft", "stone")).size());
        assertEquals("ftbquests:new",
                AmiQuestsApi.getQuestMatchesForItem(new Identifier("minecraft", "diamond"))
                        .getFirst()
                        .quest()
                        .id());
        AmiQuestsApi.removeOnChangeListener(listener);
    }

    @Test
    void simpleQuestGroupsAlsoFeedReverseLookup() {
        AmiQuestsApi.registerQuestGroup(group("ftbquests:starter", 0, "minecraft:apple"));

        assertEquals("ftbquests:starter",
                AmiQuestsApi.getQuestMatchesForItem(new Identifier("minecraft", "apple"))
                        .getFirst()
                        .quest()
                        .id());
    }

    @Test
    void multipleChangeListenersAreSupported() {
        AtomicInteger first = new AtomicInteger();
        AtomicInteger second = new AtomicInteger();
        Runnable firstListener = first::incrementAndGet;
        Runnable secondListener = second::incrementAndGet;

        AmiQuestsApi.addOnChangeListener(firstListener);
        AmiQuestsApi.addOnChangeListener(secondListener);
        AmiQuestsApi.registerQuestGroup(group("ftbquests:test", 0, "minecraft:stone"));
        AmiQuestsApi.removeOnChangeListener(firstListener);
        AmiQuestsApi.removeQuestGroup("ftbquests:test");
        AmiQuestsApi.removeOnChangeListener(secondListener);

        assertEquals(1, first.get());
        assertEquals(2, second.get());
    }

    @Test
    void legacyOnChangeReplacementDoesNotLeakOldCallback() {
        AtomicInteger oldCallback = new AtomicInteger();
        AtomicInteger newCallback = new AtomicInteger();

        AmiQuestsApi.setOnChange(oldCallback::incrementAndGet);
        AmiQuestsApi.setOnChange(newCallback::incrementAndGet);
        AmiQuestsApi.registerQuestGroup(group("ftbquests:test", 0, "minecraft:stone"));

        assertEquals(0, oldCallback.get());
        assertEquals(1, newCallback.get());
    }

    @Test
    void listenerFailureDoesNotBlockLaterListeners() {
        AtomicInteger calls = new AtomicInteger();
        Runnable broken = () -> {
            throw new IllegalStateException("broken");
        };
        Runnable healthy = calls::incrementAndGet;

        AmiQuestsApi.addOnChangeListener(broken);
        AmiQuestsApi.addOnChangeListener(healthy);
        AmiQuestsApi.registerQuestGroup(group("ftbquests:test", 0, "minecraft:stone"));
        AmiQuestsApi.removeOnChangeListener(broken);
        AmiQuestsApi.removeOnChangeListener(healthy);

        assertEquals(1, calls.get());
    }

    @Test
    void groupAndEntryValidateAndCopyInputs() {
        List<AmiQuestEntry> entries = new ArrayList<>();
        entries.add(new AmiQuestEntry(new Identifier("minecraft", "stone"), 1));
        AmiQuestGroup group = new AmiQuestGroup("ftbquests:test", null, entries);
        entries.clear();

        assertEquals("ftbquests:test", group.label().getString());
        assertEquals(1, group.entries().size());
        assertThrows(UnsupportedOperationException.class, () -> group.entries().add(
                new AmiQuestEntry(new Identifier("minecraft", "dirt"), 1)));
        assertThrows(IllegalArgumentException.class, () -> new AmiQuestEntry(null, 1));
        assertThrows(IllegalArgumentException.class,
                () -> new AmiQuestGroup("", Component.literal("Broken"), List.of()));
    }

    @Test
    void amiApiExposesQuestRegistration() {
        AmiApi.registerQuestGroup(group("ftbquests:test", 0, "minecraft:stone"));
        assertEquals(1, AmiQuestsApi.getQuestGroups().size());

        AmiQuestDocument document = questDocument(
                "ftbquests:doc",
                "Document",
                "",
                new Identifier("minecraft", "apple")
        );
        AmiApi.registerQuestDocument(document);
        assertEquals(1, AmiQuestsApi.getQuestDocuments().size());
        AmiApi.removeQuestDocument("ftbquests:doc");
        assertEquals(0, AmiQuestsApi.getQuestDocuments().size());

        AmiApi.removeQuestGroupsFromMod("ftbquests");
        assertEquals(0, AmiQuestsApi.getQuestGroups().size());
    }

    private static AmiQuestGroup group(String id, int priority, String itemId) {
        return new AmiQuestGroup(id, Component.literal(id), List.of(
                new AmiQuestEntry(new Identifier(itemId), 1)
        ), priority);
    }

    private static AmiQuestDocument questDocument(String id, String title, String description, Identifier itemId) {
        return questDocument(id, "ftbquests", title, description, itemId);
    }

    private static AmiQuestDocument questDocument(String id, String sourceId, String title, String description,
                                                  Identifier itemId) {
        return AmiQuestDocument.builder(id, "ftbquests", title)
                .sourceId(sourceId)
                .chapterId("chapter")
                .chapterTitle("Chapter")
                .description(description)
                .task(AmiQuestTaskDocument.builder(id + "/task", id, AmiQuestTaskDocument.Role.REQUIREMENT)
                        .taskType("item")
                        .title(title + " Task")
                        .itemId(itemId)
                        .requiredCount(4)
                        .build())
                .build();
    }
}
