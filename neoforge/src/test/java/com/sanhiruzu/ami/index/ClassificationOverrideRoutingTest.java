package com.sanhiruzu.ami.index;

import net.minecraft.resources.ResourceLocation;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ClassificationOverrideRoutingTest {

    @AfterEach
    void reset() {
        ClassificationOverrides.clear();
    }

    @Test
    void perItemForceCategoryWinsOverRuntime() {
        ClassificationOverrides.install(
                Map.of("examplemod:confusing", new ClassificationOverride(
                        EnumSet.noneOf(ItemFacet.class), EnumSet.noneOf(ItemFacet.class), "decoration", "furniture")),
                Map.of());

        CategoryAssignment a = PrimaryCategoryResolver.resolve(
                new ResourceLocation("examplemod:confusing"),
                new FacetProfile(EnumSet.of(ItemFacet.MELEE_WEAPON), Map.of()));

        assertEquals("decoration", a.categoryId());
        assertEquals("furniture", a.subcategoryId());
        assertEquals("classification_override", a.attributes().get(SearchNodeKeys.CLASSIFICATION_ROUTE_PHASE));
    }

    @Test
    void perItemAddFacetChangesRouting() {
        ClassificationOverrides.install(
                Map.of("examplemod:mystery", new ClassificationOverride(
                        EnumSet.of(ItemFacet.MELEE_WEAPON), EnumSet.noneOf(ItemFacet.class), null, null)),
                Map.of());

        CategoryAssignment a = PrimaryCategoryResolver.resolve(
                new ResourceLocation("examplemod:mystery"),
                new FacetProfile(EnumSet.noneOf(ItemFacet.class), Map.of()));

        assertEquals("tools", a.categoryId());
    }

    @Test
    void modPatternRoutesByPathToken() {
        ClassificationOverrides.install(
                Map.of(),
                Map.of("botania", List.of(new ModPatternRule(
                        "botania", Set.of("spreader"), "magic", "reagents"))));

        CategoryAssignment a = PrimaryCategoryResolver.resolve(
                new ResourceLocation("botania:mana_spreader"),
                new FacetProfile(EnumSet.noneOf(ItemFacet.class), Map.of()));

        assertEquals("magic", a.categoryId());
        assertEquals("reagents", a.subcategoryId());
    }

    @Test
    void facetOnlyModPatternAddsFacetAndFallsThrough() {
        ClassificationOverrides.install(
                Map.of(),
                Map.of("cnc", List.of(new ModPatternRule(
                        "cnc", Set.of("potofmouse"),
                        EnumSet.of(ItemFacet.MAGIC_ARTIFACT), EnumSet.noneOf(ItemFacet.class),
                        null, null))));

        CategoryAssignment a = PrimaryCategoryResolver.resolve(
                new ResourceLocation("cnc:potofmouse"),
                new FacetProfile(EnumSet.noneOf(ItemFacet.class), Map.of()));

        // facet applied and visible to downstream scoring
        assertTrue(a.attributes().getOrDefault(SearchNodeKeys.FACETS, "").contains(ItemFacet.MAGIC_ARTIFACT.id()));
        // facet-only pattern must NOT terminate the route as a mod_pattern category
        assertEquals("magic", a.categoryId());
        assertEquals("artifacts", a.subcategoryId());
    }

    @Test
    void categoryBearingModPatternStillTerminates() {
        ClassificationOverrides.install(
                Map.of(),
                Map.of("examplemod", List.of(new ModPatternRule(
                        "examplemod", Set.of("widget"),
                        EnumSet.noneOf(ItemFacet.class), EnumSet.noneOf(ItemFacet.class),
                        "decoration", "furniture"))));

        CategoryAssignment a = PrimaryCategoryResolver.resolve(
                new ResourceLocation("examplemod:widget"),
                new FacetProfile(EnumSet.of(ItemFacet.MELEE_WEAPON), Map.of()));

        assertEquals("decoration", a.categoryId());
        assertEquals("furniture", a.subcategoryId());
        assertEquals("classification_override", a.attributes().get(SearchNodeKeys.CLASSIFICATION_ROUTE_PHASE));
    }
}
