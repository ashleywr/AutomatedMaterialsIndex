package com.sanhiruzu.ami.index;

import net.minecraft.resources.ResourceLocation;
import org.junit.jupiter.api.Test;

import java.util.EnumSet;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

class EnchantableTagEvidenceTest {

    @Test
    void enchantableSharpWeaponRoutesToToolsMelee() {
        CategoryAssignment assignment = PrimaryCategoryResolver.resolve(
                new ResourceLocation("examplemod:mystery_edge"),
                new FacetProfile(EnumSet.noneOf(ItemFacet.class),
                        Map.of(SearchNodeKeys.TAGS, "minecraft:enchantable/sharp_weapon")));

        assertEquals("tools", assignment.categoryId());
        assertEquals("melee", assignment.subcategoryId());
    }

    @Test
    void enchantableMiningRoutesToToolsHarvest() {
        CategoryAssignment assignment = PrimaryCategoryResolver.resolve(
                new ResourceLocation("examplemod:mystery_digger"),
                new FacetProfile(EnumSet.noneOf(ItemFacet.class),
                        Map.of(SearchNodeKeys.TAGS, "minecraft:enchantable/mining")));

        assertEquals("tools", assignment.categoryId());
        assertEquals("harvest", assignment.subcategoryId());
    }
}
