package com.sanhiruzu.ami.index;

import net.minecraft.resources.ResourceLocation;
import org.junit.jupiter.api.Test;

import java.util.EnumSet;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

class CreativeTabEvidenceTest {

    @Test
    void combatTabRoutesFeaturelessItemToTools() {
        CategoryAssignment assignment = PrimaryCategoryResolver.resolve(
                new ResourceLocation("examplemod:gizmo"),
                new FacetProfile(EnumSet.noneOf(ItemFacet.class),
                        Map.of(SearchNodeKeys.CREATIVE_TAB_LABEL, "Combat")));

        assertEquals("tools", assignment.categoryId());
    }

    @Test
    void armorTabRoutesFeaturelessItemToArmor() {
        CategoryAssignment assignment = PrimaryCategoryResolver.resolve(
                new ResourceLocation("examplemod:widget"),
                new FacetProfile(EnumSet.noneOf(ItemFacet.class),
                        Map.of(SearchNodeKeys.CREATIVE_TAB_LABEL, "Armor & Clothing")));

        assertEquals("armor", assignment.categoryId());
    }
}
