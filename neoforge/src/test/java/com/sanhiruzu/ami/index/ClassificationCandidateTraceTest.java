package com.sanhiruzu.ami.index;

import net.minecraft.resources.ResourceLocation;
import org.junit.jupiter.api.Test;

import java.util.EnumSet;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ClassificationCandidateTraceTest {

    @Test
    void candidatesRecordedEvenWhenIdentityOrRuleWins() {
        CategoryAssignment assignment = PrimaryCategoryResolver.resolve(
                new ResourceLocation("minecraft:cake"),
                new FacetProfile(EnumSet.of(ItemFacet.EDIBLE, ItemFacet.PLACEABLE, ItemFacet.PLACEABLE_FOOD), Map.of()));

        String candidates = assignment.attributes().get(SearchNodeKeys.CLASSIFICATION_CANDIDATES);
        assertNotNull(candidates);
        assertTrue(candidates.contains("food/"), "expected a food/* candidate, got: " + candidates);
    }
}
