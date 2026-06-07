package com.sanhiruzu.ami.config;

import com.sanhiruzu.ami.index.SearchNode;
import com.sanhiruzu.ami.index.NodeType;
import com.sanhiruzu.ami.index.SearchNodeKeys;
import net.minecraft.resources.ResourceLocation;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

class AmiDataFixesTest {
    @TempDir
    Path tempDir;

    @Test
    void readsCategoryAliasesAsMetadataOverrides() throws Exception {
        Path file = tempDir.resolve("fixes.json");
        Files.writeString(file, """
                {
                  "version": 1,
                  "items": {
                    "minecraft:lever": {
                      "category": "masonry",
                      "subcategory": "redstone"
                    }
                  }
                }
                """, StandardCharsets.UTF_8);

        Map<AmiDataFixes.NodeKey, AmiDataFixes.FixEntry> fixes = AmiDataFixes.read(file, "test");

        AmiDataFixes.FixEntry fix = fixes.get(new AmiDataFixes.NodeKey("minecraft:lever", NodeType.ITEM));
        assertEquals("masonry", fix.metadata().get(SearchNodeKeys.ONTOLOGY_CATEGORY));
        assertEquals("redstone", fix.metadata().get(SearchNodeKeys.ONTOLOGY_SUBCATEGORY));
    }

    @Test
    void readsGenericMetadataOverrides() throws Exception {
        Path file = tempDir.resolve("fixes.json");
        Files.writeString(file, """
                {
                  "items": {
                    "example:portable_tank": {
                      "type": "ITEM",
                      "metadata": {
                        "fluid_capacity": "16",
                        "ontologyCategory": "utility"
                      }
                    }
                  }
                }
                """, StandardCharsets.UTF_8);

        Map<AmiDataFixes.NodeKey, AmiDataFixes.FixEntry> fixes = AmiDataFixes.read(file, "test");

        AmiDataFixes.FixEntry fix = fixes.get(new AmiDataFixes.NodeKey("example:portable_tank", NodeType.ITEM));
        assertEquals("16", fix.metadata().get(SearchNodeKeys.FLUID_CAPACITY));
        assertEquals("utility", fix.metadata().get(SearchNodeKeys.ONTOLOGY_CATEGORY));
    }

    @Test
    void runtimeNodeWithoutUserCategoryFixStripsCategoryFields() {
        SearchNode node = new SearchNode(
                new ResourceLocation("minecraft", "lever"),
                NodeType.ITEM,
                "Lever",
                0,
                0,
                Map.of(
                        SearchNodeKeys.ONTOLOGY_CATEGORY, "masonry",
                        SearchNodeKeys.ONTOLOGY_SUBCATEGORY, "redstone",
                        SearchNodeKeys.DATA_FIX_SOURCE, "user",
                        SearchNodeKeys.FACETS, "redstone_component"
                )
        );

        SearchNode restored = AmiDataFixes.runtimeNodeWithoutUserCategoryFix(node);

        assertEquals("", restored.meta(SearchNodeKeys.ONTOLOGY_CATEGORY, ""));
        assertEquals("", restored.meta(SearchNodeKeys.ONTOLOGY_SUBCATEGORY, ""));
        assertEquals("", restored.meta(SearchNodeKeys.DATA_FIX_SOURCE, ""));
        assertEquals("redstone_component", restored.meta(SearchNodeKeys.FACETS, ""));
    }
}
