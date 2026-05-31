package com.sanhiruzu.ami.index;

import net.minecraft.resources.ResourceLocation;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PokemonSearchSyntaxTest {
    @AfterEach
    void cleanup() {
        GlobalIndex.getInstance().clear();
    }

    @Test
    void pokemonStylePropertyPrefixesResolveAgainstSpeciesMetadata() {
        GlobalIndex index = GlobalIndex.getInstance();

        SearchNode bulbasaur = species("bulbasaur", "Bulbasaur", Map.of(
                SearchNodeKeys.POKEMON_TYPE, "grass,poison",
                SearchNodeKeys.POKEMON_MOVE, "vine_whip,earthquake",
                SearchNodeKeys.POKEMON_EGG_GROUPS, "monster,grass",
                SearchNodeKeys.POKEMON_ABILITIES, "overgrow,chlorophyll",
                SearchNodeKeys.POKEMON_DEX_NUMBER, "1",
                SearchNodeKeys.POKEMON_BASE_SPEED, "45"
        ));
        SearchNode pikachu = species("pikachu", "Pikachu", Map.of(
                SearchNodeKeys.POKEMON_TYPE, "electric",
                SearchNodeKeys.POKEMON_MOVE, "quick_attack,thunderbolt",
                SearchNodeKeys.POKEMON_EGG_GROUPS, "field,fairy",
                SearchNodeKeys.POKEMON_ABILITIES, "static,lightning_rod",
                SearchNodeKeys.POKEMON_DEX_NUMBER, "25",
                SearchNodeKeys.POKEMON_BASE_SPEED, "90"
        ));

        index.addNode(bulbasaur);
        index.addNode(pikachu);

        SearchService service = SearchService.buildFrom(index, false);

        assertOnlyContains(service.query("@type:grass").get(NodeType.ENTITY), bulbasaur, pikachu);
        assertOnlyContains(service.query("#move:earthquake").get(NodeType.ENTITY), bulbasaur, pikachu);
        assertOnlyContains(service.query("%egg:monster").get(NodeType.ENTITY), bulbasaur, pikachu);
        assertOnlyContains(service.query("?ability:overgrow").get(NodeType.ENTITY), bulbasaur, pikachu);
        assertOnlyContains(service.query("$stat:speed>80").get(NodeType.ENTITY), pikachu, bulbasaur);
        assertOnlyContains(service.query("@type:grass -#move:earthquake").get(NodeType.ENTITY), null, bulbasaur);
        assertOnlyContains(service.query("@type:grass -$stat:speed>40").get(NodeType.ENTITY), null, bulbasaur);
        assertOnlyContains(service.query("=dex:1").get(NodeType.ENTITY), bulbasaur, pikachu);
    }

    @Test
    void pokemonItemMetadataIsSearchableWithExistingNumericAndPropertyFilters() {
        GlobalIndex index = GlobalIndex.getInstance();

        SearchNode potion = item("potion", "Potion", Map.of(
                SearchNodeKeys.COBBLEMON_ITEM_KIND, "medicine",
                SearchNodeKeys.POKEMON_HEALING, "20"
        ));
        SearchNode hyperPotion = item("hyper_potion", "Hyper Potion", Map.of(
                SearchNodeKeys.COBBLEMON_ITEM_KIND, "medicine",
                SearchNodeKeys.POKEMON_HEALING, "120"
        ));
        SearchNode antidote = item("antidote", "Antidote", Map.of(
                SearchNodeKeys.COBBLEMON_ITEM_KIND, "medicine",
                SearchNodeKeys.POKEMON_STATUS_CURE, "poison"
        ));
        SearchNode ultraBall = item("ultra_ball", "Ultra Ball", Map.of(
                SearchNodeKeys.COBBLEMON_ITEM_KIND, "poke_ball",
                SearchNodeKeys.POKEMON_BALL_TIER, "ultra",
                SearchNodeKeys.POKEMON_BALL_FAMILY, "standard"
        ));
        SearchNode psychicGem = item("psychic_gem", "Psychic Gem", Map.of(
                SearchNodeKeys.COBBLEMON_ITEM_KIND, "held_item",
                SearchNodeKeys.POKEMON_HELD_ITEM_ROLE, "type_boost",
                SearchNodeKeys.POKEMON_TYPE, "psychic"
        ));
        SearchNode addonBall = new SearchNode(new ResourceLocation("cobblemonextras", "shadow_ball"), NodeType.ITEM,
                "Shadow Ball", 0, 0, Map.of(
                SearchNodeKeys.COMPAT_FAMILY, "cobblemon",
                SearchNodeKeys.COBBLEMON_ITEM_KIND, "poke_ball",
                SearchNodeKeys.POKEMON_BALL_TIER, "shadow"
        ));

        index.addNode(potion);
        index.addNode(hyperPotion);
        index.addNode(antidote);
        index.addNode(ultraBall);
        index.addNode(psychicGem);
        index.addNode(addonBall);

        SearchService service = SearchService.buildFrom(index, false);

        assertOnlyContains(service.query(">heal:100").get(NodeType.ITEM), hyperPotion, potion);
        assertOnlyContains(service.query("@cobblemon ?medicine >heal:50").get(NodeType.ITEM), hyperPotion, potion);
        assertOnlyContains(service.query("?status:poison").get(NodeType.ITEM), antidote, hyperPotion);
        assertOnlyContains(service.query("?medicine").get(NodeType.ITEM), potion, ultraBall);
        assertOnlyContains(service.query("?pokeball:ultra").get(NodeType.ITEM), ultraBall, potion);
        assertOnlyContains(service.query("?helditem -?type:psychic").get(NodeType.ITEM), null, psychicGem);
        List<SearchNode> ecosystem = service.query("?compat:cobblemon").get(NodeType.ITEM);
        assertTrue(ecosystem.contains(potion));
        assertTrue(ecosystem.contains(addonBall));
    }

    private static SearchNode species(String path, String name, Map<String, String> metadata) {
        return new SearchNode(new ResourceLocation("cobblemon_species", path), NodeType.ENTITY, name, 0, 0, metadata);
    }

    private static SearchNode item(String path, String name, Map<String, String> metadata) {
        Map<String, String> copy = new HashMap<>(metadata);
        copy.put(SearchNodeKeys.MOD_ID, "cobblemon");
        return new SearchNode(new ResourceLocation("cobblemon", path), NodeType.ITEM, name, 0, 0, copy);
    }

    private static void assertOnlyContains(List<SearchNode> results, SearchNode expected, SearchNode unexpected) {
        if (expected == null) {
            assertTrue(results == null || results.isEmpty());
        } else {
            assertTrue(results != null && results.contains(expected));
        }
        if (unexpected != null && results != null) {
            assertFalse(results.contains(unexpected));
        }
    }
}
