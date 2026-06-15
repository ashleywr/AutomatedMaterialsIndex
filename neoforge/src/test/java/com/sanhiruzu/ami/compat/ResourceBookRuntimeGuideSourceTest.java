package com.sanhiruzu.ami.compat;

import com.sanhiruzu.ami.api.AmiGuideDocument;
import net.minecraft.resources.ResourceLocation;
import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ResourceBookRuntimeGuideSourceTest {
    @Test
    void indexesMantleBookPagesFromIndexAndSectionResources() {
        Map<ResourceLocation, String> resources = new LinkedHashMap<>();
        resources.put(new ResourceLocation("tinkers_reforged", "book/reforging_guide/index.json"), """
                [
                  { "name": "ores", "data": "sections/ores.json" }
                ]
                """);
        resources.put(new ResourceLocation("tinkers_reforged", "book/reforging_guide/sections/ores.json"), """
                [
                  { "name": "barium", "type": "mantle:text", "data": "ores/barium.json" }
                ]
                """);
        resources.put(new ResourceLocation("tinkers_reforged", "book/reforging_guide/en_us/ores/barium.json"), """
                {
                  "title": "Barium",
                  "text": [
                    { "text": "Barium is found underground." },
                    { "text": "It reforges tools.", "paragraph": true }
                  ],
                  "effects": [ "Works in the reforging station" ]
                }
                """);

        List<AmiGuideDocument> documents = ResourceBookRuntimeGuideSource.documentsFromResources(resources, Map.of(), "en_us");

        assertEquals(1, documents.size());
        AmiGuideDocument document = documents.getFirst();
        assertEquals(new ResourceLocation("tinkers_reforged", "reforging_guide"), document.bookId());
        assertEquals("barium", document.pageId());
        assertEquals("Barium", document.title());
        assertEquals("Ores", document.chapter());
        assertTrue(document.summaryText().contains("Barium is found underground."));
        assertTrue(document.canOpen());
    }

    @Test
    void indexesMantleLanguageScopedPagesNotExplicitlyReferencedBySections() {
        Map<ResourceLocation, String> resources = new LinkedHashMap<>();
        resources.put(new ResourceLocation("tconstruct", "book/encyclopedia/index.json"), """
                [
                  { "name": "tools", "data": "sections/tools.json" },
                  { "name": "materials_harvest", "data": "no-load" }
                ]
                """);
        resources.put(new ResourceLocation("tconstruct", "book/encyclopedia/sections/tools.json"), """
                [
                  {
                    "name": "group_small",
                    "type": "mantle:text",
                    "data": "tools/small.json",
                    "extraData": {
                      "tconstruct:load_tools": {
                        "tag": "tconstruct:modifiable/small",
                        "path": "tools/small"
                      }
                    }
                  }
                ]
                """);
        resources.put(new ResourceLocation("tconstruct", "book/encyclopedia/en_us/tools/small.json"), """
                {
                  "title": "Small Tools",
                  "text": [
                    { "text": "All small tools can be created in the Tinker Station." }
                  ]
                }
                """);
        resources.put(new ResourceLocation("tconstruct", "book/encyclopedia/en_us/tools/small/tconstruct_pickaxe.json"), """
                {
                  "tool": "tconstruct:pickaxe",
                  "text": [
                    { "text": "The Pickaxe is a precise mining tool, effective on stone, metal, and ores." }
                  ],
                  "properties": [
                    "+0.5 Attack Damage",
                    "1.2 Attack Speed"
                  ]
                }
                """);

        List<AmiGuideDocument> documents = ResourceBookRuntimeGuideSource.documentsFromResources(resources, Map.of(), "en_us");

        assertEquals(2, documents.size());
        AmiGuideDocument generatedTool = documents.stream()
                .filter(document -> "tools/small/tconstruct_pickaxe".equals(document.pageId()))
                .findFirst()
                .orElseThrow();
        assertEquals(new ResourceLocation("tconstruct", "encyclopedia"), generatedTool.bookId());
        assertEquals("Tools Small", generatedTool.chapter());
        assertTrue(generatedTool.summaryText().contains("precise mining tool"));
        assertTrue(generatedTool.summaryText().contains("Attack Damage"));
        assertEquals(List.of(new ResourceLocation("tconstruct", "pickaxe")), generatedTool.referencedItems());
        assertTrue(generatedTool.canOpen());
    }

    @Test
    void indexesAlexStyleJsonBooksWithLocalizedTitlesAndTextFiles() {
        Map<ResourceLocation, String> resources = new LinkedHashMap<>();
        resources.put(new ResourceLocation("alexsmobs", "book/animal_dictionary/alligator_snapping_turtle.json"), """
                {
                  "parent": "root.json",
                  "text": "alligator_snapping_turtle.txt",
                  "title": "entity.alexsmobs.alligator_snapping_turtle",
                  "recipes": [ { "recipe": "alexsmobs:spiked_turtle_shell" } ]
                }
                """);
        resources.put(new ResourceLocation("alexsmobs", "book/animal_dictionary/en_us/alligator_snapping_turtle.txt"), """
                <NEWLINE>
                The Alligator Snapping Turtle is a massive reptile found in swamps.
                It can be bred with raw cod.
                """);
        Map<ResourceLocation, String> lang = Map.of(
                new ResourceLocation("alexsmobs", "lang/en_us.json"),
                "{ \"entity.alexsmobs.alligator_snapping_turtle\": \"Alligator Snapping Turtle\" }"
        );

        List<AmiGuideDocument> documents = ResourceBookRuntimeGuideSource.documentsFromResources(resources, lang, "en_us");

        assertEquals(1, documents.size());
        AmiGuideDocument document = documents.getFirst();
        assertEquals("Alligator Snapping Turtle", document.title());
        assertEquals(new ResourceLocation("alexsmobs", "animal_dictionary"), document.bookId());
        assertEquals("alligator_snapping_turtle", document.pageId());
        assertEquals(List.of(new ResourceLocation("alexsmobs", "spiked_turtle_shell")), document.referencedItems());
        assertTrue(document.summaryText().contains("massive reptile"));
        assertTrue(document.canOpen());
    }

    @Test
    void indexesAlexsCavesCodexPagesWithLocalizedTextFiles() {
        Map<ResourceLocation, String> resources = new LinkedHashMap<>();
        resources.put(new ResourceLocation("alexscaves", "books/primordial/limestone.json"), """
                {
                  "parent": "root.json",
                  "text": "limestone.txt",
                  "title": "item.alexscaves.cave_codex",
                  "items": [ { "item": "alexscaves:limestone" } ]
                }
                """);
        resources.put(new ResourceLocation("alexscaves", "books/en_us/primordial/limestone.txt"), """
                <NEWLINE>
                Limestone is common in Primordial Caves.
                It can be carved into many building blocks.
                """);
        Map<ResourceLocation, String> lang = Map.of(
                new ResourceLocation("alexscaves", "lang/en_us.json"),
                "{ \"item.alexscaves.cave_codex\": \"Cave Codex\" }"
        );

        List<AmiGuideDocument> documents = ResourceBookRuntimeGuideSource.documentsFromResources(resources, lang, "en_us");

        assertEquals(1, documents.size());
        AmiGuideDocument document = documents.getFirst();
        assertEquals("Cave Codex", document.title());
        assertEquals("alexscaves_book", document.sourceType());
        assertEquals(new ResourceLocation("alexscaves", "cave_codex"), document.bookId());
        assertEquals("primordial/limestone", document.pageId());
        assertEquals(List.of(new ResourceLocation("alexscaves", "limestone")), document.referencedItems());
        assertTrue(document.summaryText().contains("Primordial Caves"));
        assertTrue(document.canOpen());
    }

    @Test
    void indexesMnaGuideJsonEntriesFromAddonNamespaces() {
        Map<ResourceLocation, String> resources = new LinkedHashMap<>();
        resources.put(new ResourceLocation("dmnr", "guide/en_us.json"), """
                {
                  "Voidfeather Charm": {
                    "index": "1",
                    "category": "artifice",
                    "sections": [
                      { "type": "title", "value": "Voidfeather Charm" },
                      { "type": "text", "json": [ { "text": "Prevents death from falling into the void." } ] }
                    ],
                    "related_recipes": [
                      { "type": "manaweaving_altar", "location": "dmnr:manaweaving/voidfeather_charm" }
                    ]
                  }
                }
                """);

        List<AmiGuideDocument> documents = ResourceBookRuntimeGuideSource.documentsFromResources(resources, Map.of(), "en_us");

        assertEquals(1, documents.size());
        AmiGuideDocument document = documents.getFirst();
        assertEquals("Voidfeather Charm", document.title());
        assertEquals(new ResourceLocation("mna", "guide_book"), document.bookId());
        assertEquals("Artifice", document.chapter());
        assertTrue(document.summaryText().contains("Prevents death"));
        assertEquals(List.of(new ResourceLocation("dmnr", "manaweaving/voidfeather_charm")), document.referencedItems());
    }

    @Test
    void indexesImmersiveEngineeringManualTextPages() {
        Map<ResourceLocation, String> resources = new LinkedHashMap<>();
        resources.put(new ResourceLocation("immersiveengineering", "manual/en_us/introduction.txt"), """
                Introduction
                Getting into Engineering
                Greetings, fellow engineer, and welcome to Immersive Engineering.
                Wires connect connectors and relays.
                """);

        List<AmiGuideDocument> documents = ResourceBookRuntimeGuideSource.documentsFromResources(resources, Map.of(), "en_us");

        assertEquals(1, documents.size());
        AmiGuideDocument document = documents.getFirst();
        assertEquals(new ResourceLocation("immersiveengineering", "manual"), document.bookId());
        assertEquals("introduction", document.pageId());
        assertEquals("Introduction", document.title());
        assertEquals("Getting into Engineering", document.chapter());
        assertTrue(document.summaryText().contains("Wires connect"));
    }

    @Test
    void indexesHexereiBookOfShadowsPagesFromEntriesAndLangKeys() {
        Map<ResourceLocation, String> resources = new LinkedHashMap<>();
        resources.put(new ResourceLocation("hexerei", "book/book_of_shadows/book_of_shadows.json"), """
                {
                  "chapters": [
                    {
                      "name": "Items",
                      "pages": [
                        { "page_location": "hexerei:book_of_shadows/book_pages/items/items_mixing_cauldron_1" }
                      ]
                    }
                  ]
                }
                """);
        resources.put(new ResourceLocation("hexerei", "book/book_of_shadows/book_pages/items/items_mixing_cauldron_1.json"), """
                {
                  "paragraphs": [
                    { "passage_text": "book.hexerei.items_mixing_cauldron_1.passage_1" },
                    { "passage_text": "book.hexerei.items_mixing_cauldron_1.passage_2" }
                  ],
                  "item_hyperlink": "hexerei:mixing_cauldron",
                  "items_and_fluids": [
                    { "type": "item", "name": "hexerei:mixing_cauldron" }
                  ]
                }
                """);
        Map<ResourceLocation, String> lang = Map.of(
                new ResourceLocation("hexerei", "lang/en_us.json"),
                """
                {
                  "book.hexerei.items_mixing_cauldron_1.passage_1": "Mixing Cauldron",
                  "book.hexerei.items_mixing_cauldron_1.passage_2": "The Mixing Cauldron makes potions and crafts."
                }
                """
        );

        List<AmiGuideDocument> documents = ResourceBookRuntimeGuideSource.documentsFromResources(resources, lang, "en_us");

        assertEquals(1, documents.size());
        AmiGuideDocument document = documents.getFirst();
        assertEquals(new ResourceLocation("hexerei", "book_of_shadows"), document.bookId());
        assertEquals("hexerei_book", document.sourceType());
        assertEquals("hexerei:book_of_shadows/book_pages/items/items_mixing_cauldron_1", document.pageId());
        assertEquals("Items", document.chapter());
        assertEquals("Mixing Cauldron", document.title());
        assertTrue(document.summaryText().contains("potions and crafts"));
        assertEquals(List.of(new ResourceLocation("hexerei", "mixing_cauldron")), document.referencedItems());
    }

    @Test
    void safeResourceListingReturnsEmptyWhenEnumerationThrows() {
        Map<String, String> resources = ResourceBookRuntimeGuideSource.safeResourceListing(
                "",
                () -> {
                    throw new ArrayIndexOutOfBoundsException("Index 1 out of bounds for length 1");
                }
        );

        assertTrue(resources.isEmpty());
    }
}
