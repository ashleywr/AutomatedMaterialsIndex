package com.sanhiruzu.ami.index;

import net.minecraft.resources.ResourceLocation;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.EnumSet;
import java.util.HashMap;

import static org.junit.jupiter.api.Assertions.*;

class ClassificationOverridesCollapseFieldTest {

    @BeforeEach
    void setup() {
        ClassificationOverrides.clear();
    }

    @AfterEach
    void teardown() {
        ClassificationOverrides.clear();
    }

    @Test
    void collapseFieldsAreReadFromJson() {
        ClassificationOverrides.parseAndInstall("""
                {
                  "modPatterns": [
                    { "mod": "testmod", "pathTokens": ["painting"],
                      "collapseFamily": "testmod:paintings", "collapseLabel": "Test Paintings",
                      "collapseMode": "default_collapsed",
                      "category": "art", "subcategory": "paintings" }
                  ]
                }
                """);
        var rule = ClassificationOverrides.patternFor("testmod", "fancy_painting", "");
        assertTrue(rule.isPresent());
        assertEquals("testmod:paintings", rule.get().collapseFamily());
        assertEquals("Test Paintings", rule.get().collapseLabel());
        assertEquals("default_collapsed", rule.get().collapseMode());
        assertTrue(rule.get().hasCollapse());
    }

    @Test
    void collapseFieldsAbsentWhenNotInJson() {
        ClassificationOverrides.parseAndInstall("""
                {
                  "modPatterns": [
                    { "mod": "testmod", "pathTokens": ["sword"], "addFacets": ["melee_weapon"] }
                  ]
                }
                """);
        var rule = ClassificationOverrides.patternFor("testmod", "iron_sword", "");
        assertTrue(rule.isPresent());
        assertNull(rule.get().collapseFamily());
        assertFalse(rule.get().hasCollapse());
    }

    @Test
    void collapseFamilyWrittenToAttributesWhenPatternFires() {
        ClassificationOverrides.parseAndInstall("""
                {
                  "modPatterns": [
                    { "mod": "testmod", "pathTokens": ["painting"],
                      "collapseFamily": "testmod:paintings", "collapseLabel": "Paintings",
                      "collapseMode": "default_collapsed" }
                  ]
                }
                """);
        var meta = new HashMap<String, String>();
        meta.put(SearchNodeKeys.MOD_ID, "testmod");
        meta.put(SearchNodeKeys.ITEM_CLASS, "net.minecraft.world.item.Item");
        var result = PrimaryCategoryResolver.resolve(
                new ResourceLocation("testmod", "fancy_painting"),
                EnumSet.noneOf(ItemFacet.class), meta);
        assertEquals("testmod:paintings", result.attributes().get(SearchNodeKeys.COLLAPSE_FAMILY));
        assertEquals("Paintings", result.attributes().get(SearchNodeKeys.COLLAPSE_LABEL));
        assertEquals("default_collapsed", result.attributes().get(SearchNodeKeys.VARIANT_COLLAPSE_MODE));
    }

    @Test
    void collapseNotOverwrittenByPatternIfAlreadySet() {
        ClassificationOverrides.parseAndInstall("""
                {
                  "modPatterns": [
                    { "mod": "testmod", "pathTokens": ["painting"],
                      "collapseFamily": "testmod:paintings", "collapseLabel": "Paintings",
                      "collapseMode": "default_collapsed" }
                  ]
                }
                """);
        var meta = new HashMap<String, String>();
        meta.put(SearchNodeKeys.MOD_ID, "testmod");
        meta.put(SearchNodeKeys.ITEM_CLASS, "net.minecraft.world.item.Item");
        meta.put(SearchNodeKeys.COLLAPSE_FAMILY, "pre:existing");
        var result = PrimaryCategoryResolver.resolve(
                new ResourceLocation("testmod", "fancy_painting"),
                EnumSet.noneOf(ItemFacet.class), meta);
        assertEquals("pre:existing", result.attributes().get(SearchNodeKeys.COLLAPSE_FAMILY));
    }

    @Test
    void collapseLabelDefaultsToCollapseFamilyWhenAbsent() {
        ClassificationOverrides.parseAndInstall("""
                {
                  "modPatterns": [
                    { "mod": "testmod", "pathTokens": ["painting"],
                      "collapseFamily": "testmod:paintings" }
                  ]
                }
                """);
        var meta = new HashMap<String, String>();
        meta.put(SearchNodeKeys.MOD_ID, "testmod");
        meta.put(SearchNodeKeys.ITEM_CLASS, "net.minecraft.world.item.Item");
        var result = PrimaryCategoryResolver.resolve(
                new ResourceLocation("testmod", "fancy_painting"),
                EnumSet.noneOf(ItemFacet.class), meta);
        assertEquals("testmod:paintings", result.attributes().get(SearchNodeKeys.COLLAPSE_FAMILY));
        assertEquals("testmod:paintings", result.attributes().get(SearchNodeKeys.COLLAPSE_LABEL));
        assertNull(result.attributes().get(SearchNodeKeys.VARIANT_COLLAPSE_MODE));
    }
}
