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
}
