package com.sanhiruzu.ami.compat;

import com.sanhiruzu.ami.api.AmiGuideDocument;
import net.minecraft.resources.ResourceLocation;
import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GuideMeRuntimeGuideSourceTest {
    @Test
    void buildsOpenableDocumentsFromGuideMeMarkdown() {
        Map<ResourceLocation, String> resources = new LinkedHashMap<>();
        resources.put(new ResourceLocation("ae2", "ae2guide/items-blocks-machines/import_bus.md"), """
                ---
                navigation:
                  parent: items-blocks-machines/items-blocks-machines-index.md
                  title: ME Import Bus
                categories:
                - devices
                item_ids:
                - ae2:import_bus
                ---

                # The Import Bus

                The import bus pulls items into [network storage](../ae2-mechanics/import-export-storage.md).
                <ItemLink id="speed_card" />
                <RecipeFor id="import_bus" />
                """);

        List<AmiGuideDocument> documents = GuideMeRuntimeGuideSource.documentsFromResources(resources);

        assertEquals(1, documents.size());
        AmiGuideDocument document = documents.getFirst();
        assertEquals(new ResourceLocation("ae2", "guide"), document.bookId());
        assertEquals("items-blocks-machines/import_bus", document.pageId());
        assertEquals("ME Import Bus", document.title());
        assertEquals("Devices", document.chapter());
        assertTrue(document.referencedItems().contains(new ResourceLocation("ae2", "import_bus")));
        assertTrue(document.referencedItems().contains(new ResourceLocation("ae2", "speed_card")));
        assertTrue(document.tags().contains("devices"));
        assertTrue(document.tags().contains("import_export_storage"));
        assertTrue(document.summaryText().contains("The Import Bus"));
        assertTrue(document.canOpen());
    }

    @Test
    void ignoresNonGuideMeMarkdownPaths() {
        Map<ResourceLocation, String> resources = new LinkedHashMap<>();
        resources.put(new ResourceLocation("example", "otherguide/page.md"), "# Other");

        assertTrue(GuideMeRuntimeGuideSource.documentsFromResources(resources).isEmpty());
    }

    @Test
    void ae2ProcessorPageIndexesSiliconFromFrontMatter() {
        Map<ResourceLocation, String> resources = new LinkedHashMap<>();
        resources.put(new ResourceLocation("ae2", "ae2guide/items-blocks-machines/processors.md"), """
                ---
                navigation:
                  title: Processors
                categories:
                - misc ingredients blocks
                item_ids:
                - ae2:logic_processor
                - ae2:printed_silicon
                - ae2:silicon
                ---

                # Processors

                <RecipeFor id="silicon" />
                """);

        List<AmiGuideDocument> documents = GuideMeRuntimeGuideSource.documentsFromResources(resources);

        assertEquals(1, documents.size());
        AmiGuideDocument document = documents.getFirst();
        assertEquals(new ResourceLocation("ae2", "guide"), document.bookId());
        assertEquals("items-blocks-machines/processors", document.pageId());
        assertTrue(document.referencedItems().contains(new ResourceLocation("ae2", "silicon")));
        assertTrue(document.canOpen());
    }
}
