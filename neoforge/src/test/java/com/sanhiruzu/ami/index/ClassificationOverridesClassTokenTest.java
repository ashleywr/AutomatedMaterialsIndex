package com.sanhiruzu.ami.index;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ClassificationOverridesClassTokenTest {

    @BeforeEach
    void setup() {
        ClassificationOverrides.clear();
        // Install a synthetic rule: mod "testmod", classToken "SwordItem" → MELEE_WEAPON facet
        ClassificationOverrides.parseAndInstall("""
                {
                  "modPatterns": [
                    { "mod": "testmod", "classTokens": ["sworditem"], "addFacets": ["melee_weapon"] }
                  ]
                }
                """);
    }

    @AfterEach
    void teardown() { ClassificationOverrides.clear(); }

    @Test
    void classTokenSubstringMatchFiringProducesFacet() {
        var rule = ClassificationOverrides.patternFor("testmod", "iron_sword", "com.example.testmod.SwordItem");
        assertTrue(rule.isPresent());
        assertTrue(rule.get().addFacets().contains(ItemFacet.MELEE_WEAPON));
    }

    @Test
    void classTokenMatchIsCaseInsensitive() {
        var rule = ClassificationOverrides.patternFor("testmod", "iron_sword", "COM.EXAMPLE.TESTMOD.SWORDITEM");
        assertTrue(rule.isPresent());
    }

    @Test
    void nonMatchingClassProducesEmpty() {
        var rule = ClassificationOverrides.patternFor("testmod", "iron_sword", "com.example.testmod.AxeItem");
        assertFalse(rule.isPresent());
    }

    @Test
    void pathTokensAndClassTokensAreIndependent() {
        // Install a rule with both pathTokens and classTokens
        ClassificationOverrides.parseAndInstall("""
                {
                  "modPatterns": [
                    { "mod": "testmod", "pathTokens": ["bow"], "classTokens": ["arrowitem"],
                      "addFacets": ["projectile"] }
                  ]
                }
                """);
        // Fires via path token
        var byPath = ClassificationOverrides.patternFor("testmod", "testmod_bow", "com.example.GenericItem");
        assertTrue(byPath.isPresent());
        // Fires via class token
        var byClass = ClassificationOverrides.patternFor("testmod", "wooden_stick", "com.example.testmod.ArrowItem");
        assertTrue(byClass.isPresent());
    }
}
