package com.sanhiruzu.ami.compat;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.sanhiruzu.ami.api.AmiGuideDocument;
import com.sanhiruzu.ami.index.AmiGuideSearchIndex;
import net.minecraft.resources.ResourceLocation;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SilentGearMaterialBookGuideSourceTest {
    @Test
    void materialDocumentsAreOpenableMaterialBookPages() {
        AmiGuideDocument document = SilentGearMaterialBookGuideSource.materialDocument(
                new ResourceLocation("silentgear", "silentgear_materials/crimson_steel.json"),
                materialJson());

        assertEquals(new ResourceLocation("silentgear", "material_book"), document.bookId());
        assertEquals(new ResourceLocation("silentgear", "material_book"), document.iconItemId());
        assertEquals("crimson_steel", document.pageId());
        assertEquals("Crimson Steel", document.title());
        assertEquals("Materials", document.chapter());
        assertTrue(document.tags().contains("metal"));
        assertTrue(document.tags().contains("trait_malleable"));
        assertTrue(document.summaryText().contains("Durability 940"));
        assertTrue(document.summaryText().contains("Malleable IV"));
        assertTrue(document.canOpen());

        AmiGuideSearchIndex index = new AmiGuideSearchIndex(List.of(document),
                AmiGuideSearchIndex.GuideIndexingMode.SUMMARY);
        assertEquals(List.of(document), index.search("crimson steel"));
        assertEquals(List.of(document), index.search("malleable"));
    }

    private static JsonObject materialJson() {
        return JsonParser.parseString("""
                {
                  "display": {
                    "name": { "text": "Crimson Steel" }
                  },
                  "crafting": {
                    "categories": [ "metal", "advanced" ],
                    "ingredient": { "item": "silentgear:crimson_steel_ingot" }
                  },
                  "properties": {
                    "silentgear:main": {
                      "durability": 940,
                      "attack_damage": 6,
                      "traits": [
                        { "trait": "silentgear:malleable", "level": 4 }
                      ]
                    }
                  }
                }
                """).getAsJsonObject();
    }
}
