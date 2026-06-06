package com.sanhiruzu.ami.compat;

import com.sanhiruzu.ami.api.AmiGuideDocument;
import net.minecraft.resources.ResourceLocation;
import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PatchouliRuntimeGuideSourceTest {
    @Test
    void buildsOpenableDocumentsFromPatchouliResources() {
        Map<ResourceLocation, String> resources = new LinkedHashMap<>();
        resources.put(new ResourceLocation("botania", "patchouli_books/lexicon/en_us/book.json"), """
                { "name": "Lexicon" }
                """);
        resources.put(new ResourceLocation("botania", "patchouli_books/lexicon/en_us/categories/basics.json"), """
                { "name": "Basics" }
                """);
        resources.put(new ResourceLocation("botania", "patchouli_books/lexicon/en_us/entries/basics/mana_spreader.json"), """
                {
                  "name": "Mana Spreaders",
                  "category": "basics",
                  "pages": [
                    { "type": "patchouli:text", "text": "Mana spreaders move mana." },
                    { "type": "patchouli:spotlight", "item": "botania:mana_spreader" }
                  ]
                }
                """);

        List<AmiGuideDocument> documents = PatchouliRuntimeGuideSource.documentsFromResources(resources, "en_us");

        assertEquals(1, documents.size());
        AmiGuideDocument document = documents.getFirst();
        assertEquals(new ResourceLocation("botania", "lexicon"), document.bookId());
        assertEquals("basics/mana_spreader", document.pageId());
        assertEquals("Mana Spreaders", document.title());
        assertEquals("Basics", document.chapter());
        assertEquals(List.of(new ResourceLocation("botania", "mana_spreader")), document.referencedItems());
        assertTrue(document.summaryText().contains("Mana spreaders move mana."));
        assertTrue(document.canOpen());
    }

    @Test
    void selectedLanguageOverridesDefaultLanguageForSameEntry() {
        Map<ResourceLocation, String> resources = new LinkedHashMap<>();
        resources.put(new ResourceLocation("example", "patchouli_books/manual/en_us/book.json"), """
                { "name": "Manual" }
                """);
        resources.put(new ResourceLocation("example", "patchouli_books/manual/en_us/entries/machines/press.json"), """
                { "name": "Press", "pages": [ { "text": "English body" } ] }
                """);
        resources.put(new ResourceLocation("example", "patchouli_books/manual/de_de/entries/machines/press.json"), """
                { "name": "Localized Press", "pages": [ { "text": "Localized body" } ] }
                """);

        List<AmiGuideDocument> documents = PatchouliRuntimeGuideSource.documentsFromResources(resources, "de_de");

        assertEquals(1, documents.size());
        assertEquals("Localized Press", documents.getFirst().title());
        assertTrue(documents.getFirst().summaryText().contains("Localized body"));
    }

    @Test
    void malformedBookDoesNotBlockOtherBooks() {
        Map<ResourceLocation, String> resources = new LinkedHashMap<>();
        resources.put(new ResourceLocation("bad", "patchouli_books/manual/en_us/entries/broken.json"), "{");
        resources.put(new ResourceLocation("good", "patchouli_books/manual/en_us/entries/working.json"), """
                { "name": "Working" }
                """);

        List<AmiGuideDocument> documents = PatchouliRuntimeGuideSource.documentsFromResources(resources, "en_us");

        assertEquals(1, documents.size());
        assertEquals("Working", documents.getFirst().title());
    }

    @Test
    void cobblepediaStyleRootBookAndLangKeysAreIndexed() {
        Map<ResourceLocation, String> resources = new LinkedHashMap<>();
        resources.put(new ResourceLocation("cobblepedia", "patchouli_books/cobblepedia/book.json"), """
                { "name": "book.cobblepedia" }
                """);
        resources.put(new ResourceLocation("cobblepedia", "patchouli_books/cobblepedia/en_us/categories/getting_started.json"), """
                { "name": "book.cobblepedia.categories.getting_started" }
                """);
        resources.put(new ResourceLocation("cobblepedia", "patchouli_books/cobblepedia/en_us/entries/pokedex.json"), """
                {
                  "name": "book.cobblepedia.entries.pokedex.name",
                  "category": "cobblepedia:getting_started",
                  "icon": "cobblemon:pokedex_red",
                  "pages": [
                    {
                      "type": "patchouli:spotlight",
                      "item": "tag:cobblemon:pokedex",
                      "text": "book.cobblepedia.entries.pokedex.text"
                    }
                  ]
                }
                """);
        Map<ResourceLocation, String> lang = new LinkedHashMap<>();
        lang.put(new ResourceLocation("cobblepedia", "lang/en_us.json"), """
                {
                  "book.cobblepedia": "Cobblepedia",
                  "book.cobblepedia.categories.getting_started": "Getting Started",
                  "book.cobblepedia.entries.pokedex.name": "Pokedex",
                  "book.cobblepedia.entries.pokedex.text": "The Pokedex records Pokemon information."
                }
                """);

        List<AmiGuideDocument> documents = PatchouliRuntimeGuideSource.documentsFromResources(resources, lang, "en_us");

        assertEquals(1, documents.size());
        AmiGuideDocument document = documents.getFirst();
        assertEquals(new ResourceLocation("cobblepedia", "cobblepedia"), document.bookId());
        assertEquals("pokedex", document.pageId());
        assertEquals("Pokedex", document.title());
        assertEquals("Getting Started", document.chapter());
        assertTrue(document.summaryText().contains("Cobblepedia"));
        assertTrue(document.summaryText().contains("The Pokedex records Pokemon information."));
        assertTrue(document.canOpen());
    }

    @Test
    void naturesAuraStyleRootBookAndAssetEntriesAreIndexed() {
        Map<ResourceLocation, String> resources = new LinkedHashMap<>();
        resources.put(new ResourceLocation("naturesaura", "patchouli_books/book/book.json"), """
                {
                  "name": "Book of Natural Aura",
                  "use_resource_pack": true
                }
                """);
        resources.put(new ResourceLocation("naturesaura", "patchouli_books/book/en_us/categories/using.json"), """
                {
                  "name": "Using Aura",
                  "description": "Ways to collect and spend aura."
                }
                """);
        resources.put(new ResourceLocation("naturesaura", "patchouli_books/book/en_us/entries/using/altar.json"), """
                {
                  "name": "The Natural Altar",
                  "icon": "naturesaura:nature_altar",
                  "category": "naturesaura:using",
                  "pages": [
                    {
                      "type": "text",
                      "text": "The Natural Altar collects aura and infuses items."
                    },
                    {
                      "type": "naturesaura:altar",
                      "recipe": "naturesaura:infused_iron",
                      "text": "Creating Infused Iron using aura."
                    }
                  ]
                }
                """);

        List<AmiGuideDocument> documents = PatchouliRuntimeGuideSource.documentsFromResources(resources, "en_us");

        assertEquals(1, documents.size());
        AmiGuideDocument document = documents.getFirst();
        assertEquals(new ResourceLocation("naturesaura", "book"), document.bookId());
        assertEquals("using/altar", document.pageId());
        assertEquals("The Natural Altar", document.title());
        assertEquals("Using Aura", document.chapter());
        assertEquals(List.of(new ResourceLocation("naturesaura", "nature_altar")), document.referencedItems());
        assertTrue(document.summaryText().contains("The Natural Altar collects aura"));
        assertTrue(document.canOpen());
    }
}
