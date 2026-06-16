package com.sanhiruzu.ami.compat;

import com.sanhiruzu.ami.index.GlobalIndex;
import com.sanhiruzu.ami.index.NodeType;
import com.sanhiruzu.ami.index.SearchNode;
import com.sanhiruzu.ami.index.SearchNodeKeys;
import net.minecraft.resources.Identifier;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CobblemonSpeciesProviderTest {
    @AfterEach
    void clearIndex() {
        GlobalIndex.getInstance().clear();
    }

    @Test
    void indexesLegacyInstanceRegistrySpeciesApi() {
        GlobalIndex index = GlobalIndex.getInstance();
        index.clear();

        new CobblemonSpeciesProvider().populate(index, null);

        SearchNode node = index
                .getNode(Identifier.of("cobblemon", "species/bulbasaur"), NodeType.ENTITY)
                .orElseThrow();
        assertEquals("Bulbasaur", node.displayName());
        assertEquals("cobblemon", node.meta(SearchNodeKeys.ONTOLOGY_CATEGORY, ""));
        assertEquals("species", node.meta(SearchNodeKeys.ONTOLOGY_SUBCATEGORY, ""));
        assertEquals("grass", node.meta(SearchNodeKeys.POKEMON_PRIMARY_TYPE, ""));
        assertEquals("poison", node.meta(SearchNodeKeys.POKEMON_SECONDARY_TYPE, ""));
        assertEquals("1", node.meta(SearchNodeKeys.POKEMON_DEX_NUMBER, ""));
        assertTrue(node.meta(SearchNodeKeys.POKEMON_MOVE, "").contains("tackle"));
        assertTrue(node.meta(SearchNodeKeys.POKEMON_MOVE, "").contains("solar_beam"));
        assertEquals("minecraft:apple,minecraft:redstone", node.meta(SearchNodeKeys.POKEMON_DROP_ITEM, ""));
    }
}
