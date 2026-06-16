package com.sanhiruzu.ami.compat;

import com.sanhiruzu.ami.api.AmiGuideDocument;
import net.minecraft.resources.Identifier;
import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ModonomiconRuntimeGuideSourceTest {
    @Test
    void buildsOpenableDocumentsFromModonomiconResourcesAndLangKeys() {
        Map<Identifier, String> resources = new LinkedHashMap<>();
        resources.put(new Identifier("spectrum", "modonomicon/books/guidebook/book.json"), """
                { "name": "item.spectrum.guidebook" }
                """);
        resources.put(new Identifier("spectrum", "modonomicon/books/guidebook/categories/cuisine.json"), """
                { "name": "book.spectrum.guidebook.category.cuisine.name" }
                """);
        resources.put(new Identifier("spectrum", "modonomicon/books/guidebook/entries/cuisine/cookbooks/brewers_handbook.json"), """
                {
                  "name": "item.spectrum.brewers_handbook",
                  "icon": { "item": "spectrum:brewers_handbook" },
                  "category": "spectrum:cuisine",
                  "pages": [
                    {
                      "type": "modonomicon:text",
                      "title": "book.spectrum.guidebook.brewers_handbook.page0.title",
                      "text": "book.spectrum.guidebook.brewers_handbook.page0.text"
                    },
                    {
                      "type": "modonomicon:spotlight",
                      "item": { "item": "spectrum:potion_workshop" }
                    }
                  ]
                }
                """);
        Map<Identifier, String> lang = new LinkedHashMap<>();
        lang.put(new Identifier("spectrum", "lang/en_us.json"), """
                {
                  "item.spectrum.guidebook": "Guidebook",
                  "book.spectrum.guidebook.category.cuisine.name": "Cuisine",
                  "item.spectrum.brewers_handbook": "Brewer's Handbook",
                  "book.spectrum.guidebook.brewers_handbook.page0.title": "Brewing Basics",
                  "book.spectrum.guidebook.brewers_handbook.page0.text": "Use the Potion Workshop to brew drinks."
                }
                """);

        List<AmiGuideDocument> documents = ModonomiconRuntimeGuideSource.documentsFromResources(resources, lang, "en_us");

        assertEquals(1, documents.size());
        AmiGuideDocument document = documents.getFirst();
        assertEquals(new Identifier("spectrum", "guidebook"), document.bookId());
        assertEquals(new Identifier("spectrum", "brewers_handbook"), document.iconItemId());
        assertEquals("cuisine/cookbooks/brewers_handbook", document.pageId());
        assertEquals("Brewer's Handbook", document.title());
        assertEquals("Cuisine", document.chapter());
        assertEquals(List.of(
                new Identifier("spectrum", "brewers_handbook"),
                new Identifier("spectrum", "potion_workshop")
        ), document.referencedItems());
        assertTrue(document.summaryText().contains("Brewing Basics"));
        assertTrue(document.summaryText().contains("Use the Potion Workshop"));
        assertTrue(document.canOpen());
    }

    @Test
    void selectedLanguageOverridesDefaultTranslations() {
        Map<Identifier, String> resources = new LinkedHashMap<>();
        resources.put(new Identifier("example", "modonomicon/books/manual/entries/basics/press.json"), """
                { "name": "guide.example.press", "pages": [ { "text": "guide.example.body" } ] }
                """);
        Map<Identifier, String> lang = new LinkedHashMap<>();
        lang.put(new Identifier("example", "lang/en_us.json"), """
                { "guide.example.press": "Press", "guide.example.body": "English body" }
                """);
        lang.put(new Identifier("example", "lang/de_de.json"), """
                { "guide.example.press": "Lokalisierte Presse", "guide.example.body": "Lokalisierter Text" }
                """);

        List<AmiGuideDocument> documents = ModonomiconRuntimeGuideSource.documentsFromResources(resources, lang, "de_de");

        assertEquals(1, documents.size());
        assertEquals("Lokalisierte Presse", documents.getFirst().title());
        assertTrue(documents.getFirst().summaryText().contains("Lokalisierter Text"));
    }

    @Test
    void malformedEntryDoesNotBlockOtherEntries() {
        Map<Identifier, String> resources = new LinkedHashMap<>();
        resources.put(new Identifier("example", "modonomicon/books/manual/entries/broken.json"), "{");
        resources.put(new Identifier("example", "modonomicon/books/manual/entries/working.json"), """
                { "name": "Working" }
                """);

        List<AmiGuideDocument> documents = ModonomiconRuntimeGuideSource.documentsFromResources(resources, Map.of(), "en_us");

        assertEquals(1, documents.size());
        assertEquals("Working", documents.getFirst().title());
    }

    @Test
    void unresolvedTranslationKeysDoNotLeakIntoTitles() {
        Map<Identifier, String> resources = new LinkedHashMap<>();
        resources.put(new Identifier("spectrum", "modonomicon/books/guidebook/entries/creating_life/bloodstone.json"), """
                {
                  "name": "book.spectrum.guidebook.bloodstone.name",
                  "category": "spectrum:creating_life",
                  "pages": [
                    {
                      "title": "book.spectrum.guidebook.bloodstone.name",
                      "text": "book.spectrum.guidebook.bloodstone.page0.text"
                    }
                  ]
                }
                """);

        List<AmiGuideDocument> documents = ModonomiconRuntimeGuideSource.documentsFromResources(resources, Map.of(), "en_us");

        assertEquals(1, documents.size());
        assertEquals("Bloodstone", documents.getFirst().title());
        assertTrue(documents.getFirst().summaryText().contains("Bloodstone"));
        assertTrue(!documents.getFirst().summaryText().contains("book.spectrum"));
    }
}
