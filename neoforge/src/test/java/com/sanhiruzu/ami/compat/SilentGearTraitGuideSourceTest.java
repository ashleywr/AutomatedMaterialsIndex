package com.sanhiruzu.ami.compat;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.sanhiruzu.ami.api.AmiGuideDocument;
import com.sanhiruzu.ami.index.AmiGuideSearchIndex;
import net.minecraft.resources.ResourceLocation;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SilentGearTraitGuideSourceTest {
    @Test
    void traitDocumentsExplainAndMatchSemanticTraitQueries() {
        AmiGuideDocument document = SilentGearTraitGuideSource.traitDocument(
                new ResourceLocation("silentgear", "silentgear_traits/malleable.json"),
                malleableJson());

        assertEquals("Malleable Trait", document.title());
        assertTrue(document.summaryText().contains("Max level: 5"));
        assertTrue(document.summaryText().contains("Durability"));
        assertTrue(document.tags().contains("gear_trait_malleable_v"));
        assertFalse(document.canOpen());
        assertNull(document.bookId());

        AmiGuideSearchIndex index = new AmiGuideSearchIndex(List.of(document),
                AmiGuideSearchIndex.GuideIndexingMode.SUMMARY);
        assertEquals(List.of(document), index.search("?trait:malleable"));
        assertEquals(List.of(document), index.search("?gear_trait_malleable_v"));
    }

    private static JsonObject malleableJson() {
        return JsonParser.parseString("""
                {
                  "description": { "text": "Materials with this trait take durability loss instead of breaking." },
                  "effects": [
                    {
                      "type": "silentgear:durability",
                      "activation_chance": 0.1,
                      "effect_scale": -1
                    }
                  ],
                  "max_level": 5,
                  "name": { "text": "Malleable" }
                }
                """).getAsJsonObject();
    }
}
