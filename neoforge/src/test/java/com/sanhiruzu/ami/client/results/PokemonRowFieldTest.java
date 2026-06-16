package com.sanhiruzu.ami.client.results;

import com.sanhiruzu.ami.index.NodeType;
import com.sanhiruzu.ami.index.SearchNode;
import com.sanhiruzu.ami.index.SearchNodeKeys;
import net.minecraft.resources.Identifier;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PokemonRowFieldTest {
    @Test
    void pokemonFieldsExtractExistingMetadata() {
        SearchNode node = new SearchNode(
                Identifier.of("cobblemon", "species/pikachu"),
                NodeType.ENTITY,
                "Pikachu",
                0,
                0,
                Map.of(
                        SearchNodeKeys.POKEMON_TYPE, "electric",
                        SearchNodeKeys.POKEMON_DEX_NUMBER, "25",
                        SearchNodeKeys.POKEMON_BASE_SPEED, "90"
                )
        );

        assertFalse(RowField.POKEMON_TYPE.extract(node).isBlank());
        assertFalse(RowField.POKEMON_DEX.extract(node).isBlank());
        assertFalse(RowField.POKEMON_SPEED.extract(node).isBlank());
        assertTrue(RowField.POKEMON_TYPE.hasValue(node));
    }

    @Test
    void pokemonHealingFieldExtractsMedicineMetadata() {
        SearchNode node = new SearchNode(
                Identifier.of("cobblemon", "hyper_potion"),
                NodeType.ITEM,
                "Hyper Potion",
                0,
                0,
                Map.of(SearchNodeKeys.POKEMON_HEALING, "120")
        );

        assertFalse(RowField.POKEMON_HEALING.extract(node).isBlank());
        assertTrue(RowField.POKEMON_HEALING.hasValue(node));
    }
}
