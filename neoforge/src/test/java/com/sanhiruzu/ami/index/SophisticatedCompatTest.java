package com.sanhiruzu.ami.index;

import com.sanhiruzu.ami.compat.CompatFamilyDetector;
import com.sanhiruzu.ami.compat.SophisticatedCompat;
import com.sanhiruzu.ami.config.AmiConfig;
import net.minecraft.resources.ResourceLocation;
import org.junit.jupiter.api.Test;

import java.util.EnumSet;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SophisticatedCompatTest {
    @Test
    void sophisticatedNamespacesGetFamilyPolicy() {
        Map<String, String> meta = meta("sophisticatedbackpacks", "Sophisticated Backpacks",
                "net.p3pp3rf1y.sophisticatedbackpacks.backpack.BackpackItem", "");

        CompatFamilyDetector.detect(new ResourceLocation("sophisticatedbackpacks", "backpack"), meta);

        assertEquals("sophisticated", meta.get(SearchNodeKeys.PRIMARY_COMPAT_FAMILY));
    }

    @Test
    void backpacksRouteToSophisticatedBackpacks() {
        Map<String, String> meta = meta("sophisticatedbackpacks", "Sophisticated Backpacks",
                "net.p3pp3rf1y.sophisticatedbackpacks.backpack.BackpackItem", "");
        meta.put(SearchNodeKeys.FACETS, FacetCodec.encode(EnumSet.of(
                ItemFacet.STORAGE,
                ItemFacet.EQUIPPABLE,
                ItemFacet.ARMOR_CHEST,
                ItemFacet.CURIO
        )));

        SophisticatedCompat.enrichItem(new ResourceLocation("sophisticatedbackpacks", "diamond_backpack"), meta);
        CategoryAssignment assignment = resolve("sophisticatedbackpacks:diamond_backpack", meta,
                ItemFacet.STORAGE, ItemFacet.EQUIPPABLE, ItemFacet.ARMOR_CHEST, ItemFacet.CURIO);

        assertEquals("backpacks", meta.get(SearchNodeKeys.SOPHISTICATED_ITEM_KIND));
        assertEquals("diamond", meta.get(SearchNodeKeys.SOPHISTICATED_TIER));
        assertEquals("sophisticated", assignment.categoryId());
        assertEquals("backpacks", assignment.subcategoryId());
    }

    @Test
    void storageBlocksRouteToSophisticatedStorage() {
        Map<String, String> meta = meta("sophisticatedstorage", "Sophisticated Storage",
                "net.p3pp3rf1y.sophisticatedstorage.item.StorageBlockItem",
                "net.p3pp3rf1y.sophisticatedstorage.block.BarrelBlock");
        meta.put(SearchNodeKeys.FACETS, FacetCodec.encode(EnumSet.of(
                ItemFacet.PLACEABLE,
                ItemFacet.HAS_BLOCK_ENTITY,
                ItemFacet.STORAGE
        )));

        SophisticatedCompat.enrichItem(new ResourceLocation("sophisticatedstorage", "oak_barrel"), meta);
        CategoryAssignment assignment = resolve("sophisticatedstorage:oak_barrel", meta,
                ItemFacet.PLACEABLE, ItemFacet.HAS_BLOCK_ENTITY, ItemFacet.STORAGE);

        assertEquals("storage", meta.get(SearchNodeKeys.SOPHISTICATED_ITEM_KIND));
        assertEquals("sophisticated", assignment.categoryId());
        assertEquals("storage", assignment.subcategoryId());
    }

    @Test
    void upgradesRouteToSophisticatedUpgrades() {
        Map<String, String> meta = meta("sophisticatedbackpacks", "Sophisticated Backpacks",
                "net.p3pp3rf1y.sophisticatedcore.upgrades.stack.StackUpgradeItem", "");
        meta.put(SearchNodeKeys.TAGS, "sophisticatedbackpacks:upgrade");

        SophisticatedCompat.enrichItem(new ResourceLocation("sophisticatedbackpacks", "stack_upgrade_omega_tier"), meta);
        CategoryAssignment assignment = resolve("sophisticatedbackpacks:stack_upgrade_omega_tier", meta, ItemFacet.UPGRADE);

        assertEquals("upgrades", meta.get(SearchNodeKeys.SOPHISTICATED_ITEM_KIND));
        assertEquals("omega", meta.get(SearchNodeKeys.SOPHISTICATED_TIER));
        assertTrue(meta.getOrDefault(SearchNodeKeys.SOPHISTICATED_FACTS, "").contains("upgrade"));
        assertEquals("sophisticated", assignment.categoryId());
        assertEquals("upgrades", assignment.subcategoryId());
    }

    @Test
    void filtersRouteToSophisticatedFilters() {
        Map<String, String> meta = meta("sophisticatedstorage", "Sophisticated Storage",
                "net.p3pp3rf1y.sophisticatedcore.upgrades.filter.FilterUpgradeItem", "");
        meta.put(SearchNodeKeys.TAGS, "sophisticatedstorage:upgrade");

        SophisticatedCompat.enrichItem(new ResourceLocation("sophisticatedstorage", "advanced_filter_upgrade"), meta);
        CategoryAssignment assignment = resolve("sophisticatedstorage:advanced_filter_upgrade", meta, ItemFacet.UPGRADE);

        assertEquals("filters", meta.get(SearchNodeKeys.SOPHISTICATED_ITEM_KIND));
        assertEquals("advanced", meta.get(SearchNodeKeys.SOPHISTICATED_TIER));
        assertEquals("sophisticated", assignment.categoryId());
        assertEquals("filters", assignment.subcategoryId());
    }

    @Test
    void utilityItemsStaySemanticUnderHybridPolicy() {
        Map<String, String> meta = meta("sophisticatedcore", "Sophisticated Core",
                "net.minecraft.world.item.BucketItem", "");
        meta.put(SearchNodeKeys.FACETS, FacetCodec.encode(EnumSet.of(ItemFacet.FLUID_CONTAINER, ItemFacet.UTILITY_MISC)));

        SophisticatedCompat.enrichItem(new ResourceLocation("sophisticatedcore", "xp_bucket"), meta);
        CategoryAssignment assignment = resolve("sophisticatedcore:xp_bucket", meta,
                ItemFacet.FLUID_CONTAINER, ItemFacet.UTILITY_MISC);

        assertNotEquals("sophisticated", assignment.categoryId());
    }

    @Test
    void semanticSophisticatedPolicyKeepsBackpackInNormalOntology() {
        AmiConfig.CompatCategoryPolicy oldPolicy = AmiConfig.sophisticatedCategoryPolicy;
        try {
            AmiConfig.sophisticatedCategoryPolicy = AmiConfig.CompatCategoryPolicy.SEMANTIC;
            Map<String, String> meta = meta("sophisticatedbackpacks", "Sophisticated Backpacks",
                    "net.p3pp3rf1y.sophisticatedbackpacks.backpack.BackpackItem", "");

            SophisticatedCompat.enrichItem(new ResourceLocation("sophisticatedbackpacks", "backpack"), meta);
            CategoryAssignment assignment = resolve("sophisticatedbackpacks:backpack", meta,
                    ItemFacet.STORAGE, ItemFacet.EQUIPPABLE, ItemFacet.ARMOR_CHEST, ItemFacet.CURIO);

            assertEquals("backpacks", meta.get(SearchNodeKeys.SOPHISTICATED_ITEM_KIND));
            assertNotEquals("sophisticated", assignment.categoryId());
            assertEquals("semantic", assignment.attributes().get(SearchNodeKeys.COMPAT_CATEGORY_POLICY));
        } finally {
            AmiConfig.sophisticatedCategoryPolicy = oldPolicy;
        }
    }

    private static Map<String, String> meta(String modId, String creativeTab, String itemClass, String blockClass) {
        Map<String, String> meta = new HashMap<>();
        meta.put(SearchNodeKeys.MOD_ID, modId);
        meta.put(SearchNodeKeys.CREATIVE_TAB_LABEL, creativeTab);
        if (!itemClass.isBlank()) {
            meta.put(SearchNodeKeys.ITEM_CLASS, itemClass);
        }
        if (!blockClass.isBlank()) {
            meta.put(SearchNodeKeys.BLOCK_CLASS, blockClass);
        }
        return meta;
    }

    private static CategoryAssignment resolve(String id, Map<String, String> meta, ItemFacet... facets) {
        return PrimaryCategoryResolver.resolve(
                new ResourceLocation(id),
                new FacetProfile(facets.length == 0 ? EnumSet.noneOf(ItemFacet.class) : EnumSet.of(facets[0], facets), meta)
        );
    }
}
