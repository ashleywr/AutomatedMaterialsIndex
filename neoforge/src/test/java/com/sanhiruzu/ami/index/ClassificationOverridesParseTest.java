package com.sanhiruzu.ami.index;

import net.minecraft.resources.ResourceLocation;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ClassificationOverridesParseTest {

    @AfterEach
    void reset() {
        ClassificationOverrides.clear();
    }

    @Test
    void parsesItemsAndModPatterns() {
        String json = """
            {
              "items": {
                "examplemod:widget": { "category": "magic", "subcategory": "reagents",
                                       "addFacets": ["magic_reagent"], "removeFacets": ["decorative_block"],
                                       "addVerbs": ["stores_items", "unknown_verb"], "removeVerbs": ["sleep_rest"],
                                       "accessLevel": "dev", "visibility": "hidden" }
              },
              "modPatterns": [
                { "mod": "botania", "pathTokens": ["mana", "spreader"], "category": "magic", "subcategory": "reagents",
                  "addVerbs": ["settlement_worksite"], "removeVerbs": ["stores_items", "not_a_verb"],
                  "accessLevel": "survival", "visibility": "visible" }
              ]
            }
            """;

        ClassificationOverrides.parseAndInstall(json);

        ClassificationOverride item = ClassificationOverrides.forItem(new ResourceLocation("examplemod:widget")).orElseThrow();
        assertEquals("magic", item.forceCategory());
        assertEquals("reagents", item.forceSubcategory());
        assertTrue(item.addFacets().contains(ItemFacet.MAGIC_REAGENT));
        assertTrue(item.removeFacets().contains(ItemFacet.DECORATIVE_BLOCK));
        assertTrue(item.addVerbs().contains(SemanticVerb.STORES_ITEMS));
        assertTrue(item.removeVerbs().contains(SemanticVerb.SLEEP_REST));
        assertEquals(1, item.addVerbs().size());
        assertEquals(ItemFilter.ACCESS_DEV, item.accessLevel());
        assertEquals("hidden", item.visibility());

        ModPatternRule rule = ClassificationOverrides.patternFor("botania", "mana_spreader").orElseThrow();
        assertEquals("magic", rule.category());
        assertTrue(rule.addVerbs().contains(SemanticVerb.SETTLEMENT_WORKSITE));
        assertTrue(rule.removeVerbs().contains(SemanticVerb.STORES_ITEMS));
        assertEquals(1, rule.removeVerbs().size());
        assertEquals(ItemFilter.ACCESS_SURVIVAL, rule.accessLevel());
        assertEquals("visible", rule.visibility());
    }

    @Test
    void parsesModPatternFacets() {
        String json = """
            {
              "items": {},
              "modPatterns": [
                { "mod": "cnc", "pathTokens": ["buckskin", "antler"], "addFacets": ["ingredient_organic"] },
                { "mod": "cnc", "pathTokens": ["potofmouse"], "addFacets": ["magic_artifact"],
                  "removeFacets": ["decorative_block"], "category": "magic", "subcategory": "artifacts" }
              ]
            }
            """;

        ClassificationOverrides.parseAndInstall(json);

        ModPatternRule organic = ClassificationOverrides.patternFor("cnc", "buckskin").orElseThrow();
        assertTrue(organic.addFacets().contains(ItemFacet.INGREDIENT_ORGANIC));
        assertTrue(organic.removeFacets().isEmpty());
        assertEquals("", organic.category() == null ? "" : organic.category());
        assertEquals(false, organic.hasCategory());

        ModPatternRule artifact = ClassificationOverrides.patternFor("cnc", "potofmouse").orElseThrow();
        assertTrue(artifact.addFacets().contains(ItemFacet.MAGIC_ARTIFACT));
        assertTrue(artifact.removeFacets().contains(ItemFacet.DECORATIVE_BLOCK));
        assertEquals(true, artifact.hasCategory());
        assertEquals("magic", artifact.category());
    }

    @Test
    void blankOrMalformedJsonInstallsEmpty() {
        ClassificationOverrides.parseAndInstall("not json");
        assertTrue(ClassificationOverrides.forItem(new ResourceLocation("examplemod:widget")).isEmpty());
    }

    @Test
    void emptyCriteriaMatchAllRuleMatchesWholeMod() {
        ClassificationOverrides.parseAndInstall("""
            {
              "modPatterns": [
                { "mod": "examplemod", "match": "all", "visibility": "hidden" }
              ]
            }
            """);

        ModPatternRule rule = ClassificationOverrides.patternFor("examplemod", "anything").orElseThrow();
        assertEquals("hidden", rule.visibility());
        assertTrue(ClassificationOverrides.patternFor("othermod", "anything").isEmpty());
    }

    @Test
    void narrowerRuleWinsOverPrecedingModWideWildcard() {
        ClassificationOverrides.parseAndInstall("""
            {
              "modPatterns": [
                { "mod": "examplemod", "match": "all", "visibility": "hidden" },
                { "mod": "examplemod", "pathTokens": ["ore"], "visibility": "visible" }
              ]
            }
            """);

        ModPatternRule oreRule = ClassificationOverrides.patternFor("examplemod", "raw_ore").orElseThrow();
        assertEquals("visible", oreRule.visibility());

        ModPatternRule fallbackRule = ClassificationOverrides.patternFor("examplemod", "widget").orElseThrow();
        assertEquals("hidden", fallbackRule.visibility());
    }
}
