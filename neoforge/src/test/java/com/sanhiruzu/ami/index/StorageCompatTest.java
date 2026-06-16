package com.sanhiruzu.ami.index;

import com.sanhiruzu.ami.compat.StorageCompat;
import net.minecraft.resources.Identifier;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class StorageCompatTest {
    @Test
    void apiBackedStorageCapacityAddsGenericStorageFacts() {
        Map<String, String> meta = new HashMap<>();
        meta.put(SearchNodeKeys.MOD_ID, "somefuturestorage");
        meta.put(SearchNodeKeys.FACETS, "storage");
        meta.put(SearchNodeKeys.ESM_CAPACITY, "4096");

        StorageCompat.enrichItem(new Identifier("somefuturestorage", "oak_crate"), meta);

        assertEquals("storage", meta.get(SearchNodeKeys.STORAGE_ITEM_KIND));
        assertTrue(meta.getOrDefault(SearchNodeKeys.STORAGE_FACTS, "").contains("capacity_indexed"));
    }

    @Test
    void broadNamespaceWordsAloneDoNotCreateStorageFacts() {
        Map<String, String> meta = new HashMap<>();
        meta.put(SearchNodeKeys.MOD_ID, "chestnutfoods");

        StorageCompat.enrichItem(new Identifier("chestnutfoods", "chestnut_pie"), meta);

        assertFalse(meta.containsKey(SearchNodeKeys.STORAGE_ITEM_KIND));
        assertFalse(meta.containsKey(SearchNodeKeys.STORAGE_FACTS));
    }

    @Test
    void genericInventoryMachinesDoNotBecomeStorageChoices() {
        Map<String, String> meta = new HashMap<>();
        meta.put(SearchNodeKeys.MOD_ID, "minecraft");
        meta.put(SearchNodeKeys.FACETS, "placeable,has_block_entity,storage,machine,workstation");
        meta.put(SearchNodeKeys.ESM_CAPACITY, "297");
        meta.put(SearchNodeKeys.ITEM_CLASS, "net.minecraft.world.item.BlockItem");
        meta.put(SearchNodeKeys.BLOCK_CLASS, "net.minecraft.world.level.block.FurnaceBlock");

        StorageCompat.enrichItem(new Identifier("minecraft", "furnace"), meta);

        assertFalse(meta.containsKey(SearchNodeKeys.STORAGE_ITEM_KIND));
        assertFalse(meta.containsKey(SearchNodeKeys.STORAGE_FACTS));
    }

    @Test
    void reprocessingFinalMetadataClearsEarlierGenericStorageGuess() {
        Map<String, String> meta = new HashMap<>();
        meta.put(SearchNodeKeys.MOD_ID, "minecraft");
        meta.put(SearchNodeKeys.FACETS, "placeable,has_block_entity,storage");
        meta.put(SearchNodeKeys.ESM_CAPACITY, "297");
        meta.put(SearchNodeKeys.STORAGE_ITEM_KIND, "storage");
        meta.put(SearchNodeKeys.STORAGE_FACTS, "storage,capacity_indexed");
        meta.put(SearchNodeKeys.SEARCH_TOKENS, "storage_storage storage capacity_indexed");

        meta.put(SearchNodeKeys.FACETS, "placeable,has_block_entity,storage,machine,workstation");
        meta.put(SearchNodeKeys.BLOCK_CLASS, "net.minecraft.world.level.block.FurnaceBlock");
        StorageCompat.enrichItem(new Identifier("minecraft", "furnace"), meta);

        assertFalse(meta.containsKey(SearchNodeKeys.STORAGE_ITEM_KIND));
        assertFalse(meta.containsKey(SearchNodeKeys.STORAGE_FACTS));
        assertFalse(meta.containsKey(SearchNodeKeys.SEARCH_TOKENS));
    }

    @Test
    void storageIdentityRequiresWholeTokensNotSubstrings() {
        Map<String, String> meta = new HashMap<>();
        meta.put(SearchNodeKeys.MOD_ID, "cobblemon");
        meta.put(SearchNodeKeys.FACETS, "placeable,has_block_entity,storage,crop");

        StorageCompat.enrichItem(new Identifier("cobblemon", "chesto_berry"), meta);

        assertFalse(meta.containsKey(SearchNodeKeys.STORAGE_ITEM_KIND));
        assertFalse(meta.containsKey(SearchNodeKeys.STORAGE_FACTS));
    }

    @Test
    void apiBackedCabinetStorageGetsGenericStorageFacts() {
        Map<String, String> meta = new HashMap<>();
        meta.put(SearchNodeKeys.MOD_ID, "somefuturestorage");
        meta.put(SearchNodeKeys.FACETS, "placeable,has_block_entity,storage");
        meta.put(SearchNodeKeys.ESM_CAPACITY, "3456");

        StorageCompat.enrichItem(new Identifier("somefuturestorage", "oak_cabinet"), meta);

        assertEquals("storage", meta.get(SearchNodeKeys.STORAGE_ITEM_KIND));
        assertTrue(meta.getOrDefault(SearchNodeKeys.STORAGE_FACTS, "").contains("capacity_indexed"));
    }

    @Test
    void storageLookingPathWithoutRuntimeEvidenceDoesNotCreateStorageFacts() {
        Map<String, String> meta = new HashMap<>();
        meta.put(SearchNodeKeys.MOD_ID, "furnituremod");
        meta.put(SearchNodeKeys.FACETS, "placeable,decorative_block");

        StorageCompat.enrichItem(new Identifier("furnituremod", "oak_cabinet"), meta);

        assertFalse(meta.containsKey(SearchNodeKeys.STORAGE_ITEM_KIND));
        assertFalse(meta.containsKey(SearchNodeKeys.STORAGE_FACTS));
    }

    @Test
    void knownStorageNamespacesGetKindAndTierFactsWithoutCapacityGuessing() {
        Map<String, String> meta = new HashMap<>();
        meta.put(SearchNodeKeys.MOD_ID, "ironchest");

        StorageCompat.enrichItem(new Identifier("ironchest", "diamond_chest"), meta);

        assertEquals("chest", meta.get(SearchNodeKeys.STORAGE_ITEM_KIND));
        assertEquals("diamond", meta.get(SearchNodeKeys.STORAGE_TIER));
        assertFalse(meta.containsKey(SearchNodeKeys.ESM_CAPACITY));
    }

    @Test
    void refinedStorageMediaGetsSearchableStorageFacts() {
        Map<String, String> meta = new HashMap<>();
        meta.put(SearchNodeKeys.MOD_ID, "refinedstorage");

        StorageCompat.enrichItem(new Identifier("refinedstorage", "64k_storage_disk"), meta);

        assertEquals("disk", meta.get(SearchNodeKeys.STORAGE_ITEM_KIND));
        assertEquals("64k", meta.get(SearchNodeKeys.STORAGE_TIER));
        assertTrue(meta.getOrDefault(SearchNodeKeys.STORAGE_FACTS, "").contains("storage_disk"));
        assertTrue(meta.getOrDefault(SearchNodeKeys.SEARCH_TOKENS, "").contains("storage_disk"));
    }

    @Test
    void refinedStorageNetworkBlocksGetSearchableStorageFacts() {
        Map<String, String> meta = new HashMap<>();
        meta.put(SearchNodeKeys.MOD_ID, "refinedstorage");
        meta.put(SearchNodeKeys.BLOCK_CLASS, "com.refinedmods.refinedstorage.block.ImporterBlock");

        StorageCompat.enrichItem(new Identifier("refinedstorage", "importer"), meta);

        assertEquals("network_device", meta.get(SearchNodeKeys.STORAGE_ITEM_KIND));
        assertTrue(meta.getOrDefault(SearchNodeKeys.STORAGE_FACTS, "").contains("network_device"));
    }
}
