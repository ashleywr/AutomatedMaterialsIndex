package com.sanhiruzu.ami.compat;

import com.sanhiruzu.ami.api.AmiGuideDocument;
import com.sanhiruzu.ami.index.AmiGuideSearchIndex;
import net.minecraft.resources.ResourceLocation;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PatchouliGuideFixtureAdapterTest {
    @Test
    void parsesEntryIntoGuideDocument() {
        List<AmiGuideDocument> documents = PatchouliGuideFixtureAdapter.parse(
                new ResourceLocation("botania", "lexicon"),
                bookJson(),
                Map.of("en_us/categories/basics.json", """
                        {
                          "name": "Basics"
                        }
                        """),
                Map.of("en_us/entries/basics/mana_spreader.json", """
                        {
                          "name": "Mana Spreaders",
                          "category": "botania:basics",
                          "pages": [
                            {
                              "type": "patchouli:text",
                              "title": "Moving Mana",
                              "text": "$(item)Mana Spreaders$() move mana into pools."
                            },
                            {
                              "type": "patchouli:spotlight",
                              "item": "botania:mana_spreader"
                            }
                          ]
                        }
                        """));

        assertEquals(1, documents.size());
        AmiGuideDocument document = documents.getFirst();
        assertEquals(new ResourceLocation("ami", "guide/patchouli/botania/lexicon/basics/mana_spreader"),
                document.id());
        assertEquals("patchouli", document.sourceType());
        assertEquals("botania", document.modId());
        assertEquals(new ResourceLocation("botania", "lexicon"), document.bookId());
        assertEquals("basics/mana_spreader", document.pageId());
        assertEquals("Mana Spreaders", document.title());
        assertEquals("Basics", document.chapter());
        assertEquals(List.of(new ResourceLocation("botania", "mana_spreader")), document.referencedItems());
        assertEquals(List.of("patchouli", "basics"), document.tags());
        assertTrue(document.summaryText().contains("Moving Mana"));
        assertTrue(document.summaryText().contains("Mana Spreaders move mana into pools."));
        assertFalse(document.canOpen());
    }

    @Test
    void supportsArrayAndObjectItemReferences() {
        List<AmiGuideDocument> documents = PatchouliGuideFixtureAdapter.parse(
                new ResourceLocation("example", "manual"),
                "",
                Map.of(),
                Map.of("machines.json", """
                        {
                          "name": "Machines",
                          "pages": [
                            {
                              "item": { "item": "example:press" }
                            },
                            {
                              "items": [
                                "example:cutter{display:{Name:'ignored'}}",
                                { "id": "example:lathe" },
                                "#example:machine_parts"
                              ]
                            }
                          ]
                        }
                        """));

        assertEquals(List.of(
                new ResourceLocation("example", "press"),
                new ResourceLocation("example", "cutter"),
                new ResourceLocation("example", "lathe")
        ), documents.getFirst().referencedItems());
    }

    @Test
    void documentsWorkWithGuideIndexModes() {
        List<AmiGuideDocument> documents = PatchouliGuideFixtureAdapter.parse(
                new ResourceLocation("ae2", "guide"),
                bookJson(),
                Map.of("channels.json", "{ \"name\": \"Channels\" }"),
                Map.of("networks/controller.json", """
                        {
                          "name": "Controller",
                          "category": "channels",
                          "pages": [
                            {
                              "text": "A dense cable network can carry many channels."
                            },
                            {
                              "item": "ae2:controller"
                            }
                          ]
                        }
                        """));

        AmiGuideSearchIndex titles = new AmiGuideSearchIndex(documents, AmiGuideSearchIndex.GuideIndexingMode.TITLES);
        assertEquals(documents, titles.search("controller"));
        assertEquals(documents, titles.search("ae2 controller"));
        assertTrue(titles.search("dense cable").isEmpty());

        AmiGuideSearchIndex summary = new AmiGuideSearchIndex(documents, AmiGuideSearchIndex.GuideIndexingMode.SUMMARY);
        assertEquals(documents, summary.search("dense cable"));
    }

    @Test
    void untranslatedKeysFallBackToReadableLabels() {
        List<AmiGuideDocument> documents = PatchouliGuideFixtureAdapter.parse(
                new ResourceLocation("cobblepedia", "cobblepedia"),
                """
                        { "name": "book.cobblepedia" }
                        """,
                Map.of("items.json", """
                        { "name": "book.cobblepedia.categories.items" }
                        """),
                Map.of("master_ball.json", """
                        {
                          "name": "item.cobblemon.master_ball",
                          "category": "cobblepedia:items",
                          "pages": [
                            {
                              "title": "book.cobblepedia.entries.master_ball.name",
                              "text": "book.cobblepedia.entries.master_ball.text"
                            }
                          ]
                        }
                        """));

        assertEquals("Master Ball", documents.getFirst().title());
        assertEquals("Items", documents.getFirst().chapter());
        assertTrue(documents.getFirst().summaryText().contains("Master Ball"));
        assertFalse(documents.getFirst().summaryText().contains("item.cobblemon.master_ball"));
    }

    private static String bookJson() {
        return """
                {
                  "name": "Lexicon"
                }
                """;
    }
}
