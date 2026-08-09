package com.sanhiruzu.ami.client.entitydetails;

import com.sanhiruzu.ami.index.EdgeType;
import com.sanhiruzu.ami.index.GlobalIndex;
import com.sanhiruzu.ami.index.NodeType;
import com.sanhiruzu.ami.index.SearchNode;
import com.sanhiruzu.ami.index.SearchNodeKeys;
import net.minecraft.resources.ResourceLocation;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class EntityDetailsResolverTest {
    @BeforeEach
    void clearGlobalIndex() {
        GlobalIndex.getInstance().clear();
    }

    @Test
    void resolvesStatsSpawnBiomesAndKnownDrops() {
        SearchNode cow = entity("cow", "Cow", Map.of(
                SearchNodeKeys.ENTITY_CATEGORY, "CREATURE",
                SearchNodeKeys.ENTITY_HEALTH, "10",
                SearchNodeKeys.ENTITY_ATTACK_DAMAGE, "2",
                SearchNodeKeys.FIRE_IMMUNE, "false",
                SearchNodeKeys.ENTITY_TRAITS, "mountable"
        ));
        SearchNode plains = biome("plains", "Plains Biome");
        SearchNode meadow = biome("meadow", "Meadow");
        SearchNode leather = item("leather", "Leather");
        SearchNode beef = item("beef", "Raw Beef");
        cow.addResolvedEdge(EdgeType.SPAWNS_IN, plains);
        cow.addResolvedEdge(EdgeType.SPAWNS_IN, meadow);
        cow.addResolvedEdge(EdgeType.DROPS, leather);
        cow.addResolvedEdge(EdgeType.DROPS, beef);

        EntityDetailsReport report = resolver(cow, plains, meadow, leather, beef).resolve(cow);

        assertEquals("ami.entity_details.title.named", report.title().getString());
        assertEquals(List.of(
                EntityDetailsSection.STATS,
                EntityDetailsSection.SPAWNS,
                EntityDetailsSection.DROPS
        ), report.groupOrder());
        assertEquals(List.of("5 hearts", "2 damage", "Mountable"),
                report.rows(EntityDetailsSection.STATS).stream().map(EntityDetailsRow::text).toList());
        assertEquals(List.of(
                        EntityDetailsStatKind.HEALTH,
                        EntityDetailsStatKind.DAMAGE,
                        EntityDetailsStatKind.TRAIT
                ),
                report.rows(EntityDetailsSection.STATS).stream().map(EntityDetailsRow::statKind).toList());
        assertEquals(List.of("Plains", "Meadow"),
                report.rows(EntityDetailsSection.SPAWNS).stream().map(EntityDetailsRow::text).toList());
        assertEquals(List.of("Leather", "Raw Beef"),
                report.rows(EntityDetailsSection.DROPS).stream().map(EntityDetailsRow::text).toList());
        assertTrue(report.rows(EntityDetailsSection.DROPS).stream().allMatch(row -> row.detail().isBlank()));
    }

    @Test
    void resolveUsesCanonicalIndexedNodeWhenClickedNodeHasNoEdges() {
        SearchNode indexedCow = entity("cow", "Cow", Map.of(SearchNodeKeys.ENTITY_HEALTH, "10"));
        SearchNode clickedCow = entity("cow", "Cow", Map.of());
        SearchNode leather = item("leather", "Leather");
        indexedCow.addResolvedEdge(EdgeType.DROPS, leather);

        EntityDetailsReport report = resolver(indexedCow, clickedCow, leather).resolve(clickedCow);

        assertEquals(List.of("Leather"),
                report.rows(EntityDetailsSection.DROPS).stream().map(EntityDetailsRow::text).toList());
        assertEquals(List.of("5 hearts"),
                report.rows(EntityDetailsSection.STATS).stream().map(EntityDetailsRow::text).toList());
    }

    @Test
    void resolvesUnresolvedEdgesFromIndexedNodes() {
        SearchNode cow = entity("cow", "Cow", Map.of());
        SearchNode savanna = biome("savanna", "Savanna Biome");
        SearchNode leather = item("leather", "Leather");
        cow.addUnresolvedEdge(EdgeType.SPAWNS_IN, savanna.id());
        cow.addUnresolvedEdge(EdgeType.DROPS, leather.id());

        EntityDetailsReport report = resolver(cow, savanna, leather).resolve(cow);

        assertEquals(List.of("Savanna"),
                report.rows(EntityDetailsSection.SPAWNS).stream().map(EntityDetailsRow::text).toList());
        assertEquals(List.of("Leather"),
                report.rows(EntityDetailsSection.DROPS).stream().map(EntityDetailsRow::text).toList());
    }

    @Test
    void deduplicatesResolvedAndUnresolvedEdgesInStableOrder() {
        SearchNode cow = entity("cow", "Cow", Map.of());
        SearchNode plains = biome("plains", "Plains Biome");
        SearchNode meadow = biome("meadow", "Meadow");
        SearchNode leather = item("leather", "Leather");
        SearchNode beef = item("beef", "Raw Beef");
        cow.addResolvedEdge(EdgeType.SPAWNS_IN, plains);
        cow.addResolvedEdge(EdgeType.SPAWNS_IN, plains);
        cow.addUnresolvedEdge(EdgeType.SPAWNS_IN, plains.id());
        cow.addResolvedEdge(EdgeType.SPAWNS_IN, meadow);
        cow.addResolvedEdge(EdgeType.DROPS, leather);
        cow.addUnresolvedEdge(EdgeType.DROPS, leather.id());
        cow.addResolvedEdge(EdgeType.DROPS, beef);

        EntityDetailsReport report = resolver(cow, plains, meadow, leather, beef).resolve(cow);

        assertEquals(List.of("Plains", "Meadow"),
                report.rows(EntityDetailsSection.SPAWNS).stream().map(EntityDetailsRow::text).toList());
        assertEquals(List.of("Leather", "Raw Beef"),
                report.rows(EntityDetailsSection.DROPS).stream().map(EntityDetailsRow::text).toList());
    }

    @Test
    void dropRowsAreClickableItemRowsWithoutChancePlaceholderText() {
        SearchNode cow = entity("cow", "Cow", Map.of());
        SearchNode leather = item("leather", "Leather");
        cow.addResolvedEdge(EdgeType.DROPS, leather);

        EntityDetailsReport report = resolver(cow, leather).resolve(cow);
        EntityDetailsRow drop = report.rows(EntityDetailsSection.DROPS).getFirst();

        assertFalse(drop.detail().matches(".*\\d+\\s*%.*"));
        assertTrue(drop.detail().isBlank());
    }

    @Test
    void fireImmuneUsesEffectStatKind() {
        SearchNode blaze = entity("blaze", "Blaze", Map.of(SearchNodeKeys.FIRE_IMMUNE, "true"));

        EntityDetailsReport report = resolver(blaze).resolve(blaze);
        EntityDetailsRow stat = report.rows(EntityDetailsSection.STATS).getFirst();

        assertEquals("Fire immune", stat.text());
        assertEquals(EntityDetailsStatKind.EFFECT, stat.statKind());
    }

    private static EntityDetailsResolver resolver(SearchNode... nodes) {
        return new EntityDetailsResolver(List.of(nodes));
    }

    private static SearchNode entity(String path, String name, Map<String, String> metadata) {
        return new SearchNode(new ResourceLocation("minecraft:" + path), NodeType.ENTITY, name, 0, 0, metadata);
    }

    private static SearchNode item(String path, String name) {
        return new SearchNode(new ResourceLocation("minecraft:" + path), NodeType.ITEM, name, 0, 0, Map.of());
    }

    private static SearchNode biome(String path, String name) {
        return new SearchNode(new ResourceLocation("minecraft:" + path), NodeType.BIOME, name, 0, 0, Map.of());
    }
}
