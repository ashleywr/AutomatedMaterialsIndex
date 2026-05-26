package com.sanhiruzu.ami.index;

import net.minecraft.resources.ResourceLocation;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class EntityQuerySemanticsTest {

    @AfterEach
    public void cleanup() {
        GlobalIndex.getInstance().clear();
    }

    @Test
    public void entityNumericFiltersCanSeedAndRefineResults() {
        GlobalIndex index = GlobalIndex.getInstance();

        SearchNode wolf = new SearchNode(
                new ResourceLocation("minecraft:wolf"),
                NodeType.ENTITY,
                "Wolf",
                0,
                0,
                Map.of(SearchNodeKeys.ENTITY_HEALTH, "8", SearchNodeKeys.ENTITY_ATTACK_DAMAGE, "4")
        );
        SearchNode warden = new SearchNode(
                new ResourceLocation("minecraft:warden"),
                NodeType.ENTITY,
                "Warden",
                0,
                0,
                Map.of(SearchNodeKeys.ENTITY_HEALTH, "500", SearchNodeKeys.ENTITY_ATTACK_DAMAGE, "30")
        );

        index.addNode(wolf);
        index.addNode(warden);
        SearchService service = SearchService.buildFrom(index, false);

        List<SearchNode> highHealth = service.query(">health:100").get(NodeType.ENTITY);
        assertTrue(highHealth.contains(warden));
        assertFalse(highHealth.contains(wolf));

        List<SearchNode> refinedAttack = service.query("wolf >attack:3").get(NodeType.ENTITY);
        assertTrue(refinedAttack.contains(wolf));
        assertFalse(refinedAttack.contains(warden));
    }

    @Test
    public void tagAndPropertyFiltersRefineLiteralResults() {
        GlobalIndex index = GlobalIndex.getInstance();

        SearchNode wolf = new SearchNode(
                new ResourceLocation("minecraft:wolf"),
                NodeType.ENTITY,
                "Wolf",
                0,
                0,
                Map.of(
                        SearchNodeKeys.TAGS, "ami:tamable",
                        SearchNodeKeys.ENTITY_TRAITS, "tamable pet",
                        SearchNodeKeys.SEARCH_TOKENS, "tamable pet"
                )
        );
        SearchNode cat = new SearchNode(
                new ResourceLocation("minecraft:cat"),
                NodeType.ENTITY,
                "Cat",
                0,
                0,
                Map.of(
                        SearchNodeKeys.TAGS, "ami:tamable",
                        SearchNodeKeys.ENTITY_TRAITS, "tamable pet",
                        SearchNodeKeys.SEARCH_TOKENS, "tamable pet"
                )
        );
        SearchNode wolfArmor = new SearchNode(
                new ResourceLocation("example:wolf_armor"),
                NodeType.ITEM,
                "Wolf Armor",
                0,
                0,
                Map.of()
        );

        index.addNode(wolf);
        index.addNode(cat);
        index.addNode(wolfArmor);
        SearchService service = SearchService.buildFrom(index, false);

        List<SearchNode> tamableWolves = service.query("wolf #tamable").get(NodeType.ENTITY);
        assertTrue(tamableWolves.contains(wolf));
        assertFalse(tamableWolves.contains(cat));
        assertFalse(service.query("wolf #tamable").getOrDefault(NodeType.ITEM, List.of()).contains(wolfArmor));
        assertTrue(service.query("missing #tamable").isEmpty());

        List<SearchNode> propertyPets = service.query("?tamable").get(NodeType.ENTITY);
        assertTrue(propertyPets.contains(wolf));
        assertTrue(propertyPets.contains(cat));
    }

    @Test
    public void tagExclusionsUseTheTagResolver() {
        GlobalIndex index = GlobalIndex.getInstance();

        SearchNode wolf = new SearchNode(
                new ResourceLocation("minecraft:wolf"),
                NodeType.ENTITY,
                "Wolf",
                0,
                0,
                Map.of(SearchNodeKeys.TAGS, "ami:tamable", SearchNodeKeys.SEARCH_TOKENS, "tamable")
        );
        SearchNode creeper = new SearchNode(
                new ResourceLocation("minecraft:creeper"),
                NodeType.ENTITY,
                "Creeper",
                0,
                0,
                Map.of()
        );

        index.addNode(wolf);
        index.addNode(creeper);
        SearchService service = SearchService.buildFrom(index, false);

        List<SearchNode> entities = service.query("minecraft -#tamable").get(NodeType.ENTITY);
        assertFalse(entities.contains(wolf));
        assertTrue(entities.contains(creeper));
    }
}
