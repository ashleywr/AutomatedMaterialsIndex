package com.sanhiruzu.ami.compat;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.sanhiruzu.ami.api.AmiGuideDocument;
import net.minecraft.resources.Identifier;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ApotheosisGuideSourceTest {
    @Test
    void affixDocumentIndexesPlayerFacingAffixTerms() {
        JsonObject json = JsonParser.parseString("""
                {
                  "type": "apotheosis:attribute",
                  "attribute": "apothic_attributes:life_steal",
                  "categories": ["apotheosis:melee_weapon", "apotheosis:trident"],
                  "definition": {
                    "affix_type": "stat",
                    "weights": {"quality": 0.1, "weight": 25}
                  },
                  "operation": "add_value",
                  "values": {
                    "apotheosis:common": {"min": 0.05, "max": 0.08},
                    "apotheosis:mythic": {"min": 0.1, "max": 0.15}
                  }
                }
                """).getAsJsonObject();

        AmiGuideDocument document = ApotheosisGuideSource.affixDocument(
                Identifier.of("apotheosis", "affixes/melee/attribute/vampiric.json"),
                json);

        assertEquals("Vampiric Affix", document.title());
        assertTrue(document.tags().contains("affix"));
        assertTrue(document.tags().contains("affix_vampiric"));
        assertTrue(document.tags().contains("melee_weapon"));
        assertTrue(document.tags().contains("life_steal"));
        assertTrue(document.canOpen());
        assertEquals("adventure/affix_loot/affixes", document.pageId());
        assertTrue(document.summaryText().contains("Attribute: Life Steal"));
        assertTrue(document.summaryText().contains("Mythic 0.1-0.15"));
    }

    @Test
    void enchantingStatsDocumentReferencesShelfAndStats() {
        JsonObject json = JsonParser.parseString("""
                {
                  "block": "apothic_enchanting:hellshelf",
                  "stats": {
                    "maxEterna": 45,
                    "eterna": 3,
                    "quanta": 3,
                    "arcana": 0
                  }
                }
                """).getAsJsonObject();

        AmiGuideDocument document = ApotheosisGuideSource.enchantingStatsDocument(
                Identifier.of("apothic_enchanting", "enchanting_stats/hellshelf.json"),
                json);

        assertEquals("Hellshelf Enchanting Stats", document.title());
        assertEquals(Identifier.of("apothic_enchanting", "hellshelf"),
                document.referencedItems().get(0));
        assertTrue(document.canOpen());
        assertEquals("enchanting/table/stats", document.pageId());
        assertTrue(document.tags().contains("eterna"));
        assertTrue(document.summaryText().contains("Max Eterna: 45"));
        assertTrue(document.summaryText().contains("Arcana: 0"));
    }

    @Test
    void enchantmentDocumentIndexesApothicEnchantmentNames() {
        JsonObject json = JsonParser.parseString("""
                {
                  "description": {"translate": "enchantment.apothic_enchanting.life_mending"},
                  "max_level": 3,
                  "supported_items": "#minecraft:enchantable/durability",
                  "slots": ["any"],
                  "weight": 1
                }
                """).getAsJsonObject();

        AmiGuideDocument document = ApotheosisGuideSource.enchantmentDocument(
                Identifier.of("apothic_enchanting", "enchantment/life_mending.json"),
                json);

        assertTrue(document.title().contains("Enchantment"));
        assertTrue(document.tags().contains("life_mending"));
        assertTrue(document.tags().contains("enchantment_life_mending"));
        assertTrue(document.canOpen());
        assertEquals("enchanting/enchantments/life_mending", document.pageId());
        assertTrue(document.summaryText().contains("Max level: 3"));
        assertTrue(document.summaryText().contains("#minecraft:enchantable/durability"));
    }
}
