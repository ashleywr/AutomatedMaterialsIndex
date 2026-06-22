package com.sanhiruzu.ami.index;

import net.minecraft.resources.ResourceLocation;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ClassificationOverridesTest {

    @AfterEach
    void reset() {
        ClassificationOverrides.clear();
    }

    @Test
    void forItemReturnsInstalledOverride() {
        ClassificationOverrides.install(
                Map.of("examplemod:widget", new ClassificationOverride(
                        EnumSet.of(ItemFacet.MAGIC_REAGENT), EnumSet.noneOf(ItemFacet.class), "magic", "reagents")),
                Map.of());

        Optional<ClassificationOverride> found = ClassificationOverrides.forItem(new ResourceLocation("examplemod:widget"));
        assertTrue(found.isPresent());
        assertEquals("magic", found.get().forceCategory());
        assertTrue(found.get().addFacets().contains(ItemFacet.MAGIC_REAGENT));
        assertTrue(ClassificationOverrides.forItem(new ResourceLocation("examplemod:other")).isEmpty());
    }

    @Test
    void patternForMatchesAnyPathToken() {
        ClassificationOverrides.install(
                Map.of(),
                Map.of("botania", List.of(new ModPatternRule(
                        "botania", Set.of("mana", "spreader"), "magic", "reagents"))));

        Optional<ModPatternRule> hit = ClassificationOverrides.patternFor("botania", "mana_spreader");
        assertTrue(hit.isPresent());
        assertEquals("magic", hit.get().category());
        assertFalse(ClassificationOverrides.patternFor("botania", "petal_apothecary").isPresent());
        assertFalse(ClassificationOverrides.patternFor("create", "mana_spreader").isPresent());
    }
}
