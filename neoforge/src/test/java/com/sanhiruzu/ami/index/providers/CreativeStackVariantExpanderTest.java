package com.sanhiruzu.ami.index.providers;

import com.sanhiruzu.ami.config.AmiConfig;
import com.sanhiruzu.ami.index.FacetProfile;
import com.sanhiruzu.ami.index.ItemFilter;
import com.sanhiruzu.ami.index.ItemFacet;
import com.sanhiruzu.ami.index.SearchNodeKeys;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.EnumSet;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class CreativeStackVariantExpanderTest {
    @BeforeEach
    void resetConfigBeforeEach() {
        AmiConfig.resetToDefaults();
    }

    @AfterEach
    void resetConfigAfterEach() {
        AmiConfig.resetToDefaults();
    }

    @Test
    void normalModeSkipsVisiblyIdenticalCreativeStacksWithDifferentHiddenComponents() {
        Item charm = new Item("Charm of Shrinking");
        ItemFilter.CreativeTabInfo relics = new ItemFilter.CreativeTabInfo("relics:relics", "Relics");

        List<SubtypeExpander.SubtypeEntry> entries = CreativeStackVariantExpander.expand(
                new ResourceLocation("artifacts", "charm_of_shrinking"),
                List.of(
                        new ItemFilter.CreativeStackInfo(new ItemStack(charm).withComponentSignature("compat-a"), relics),
                        new ItemFilter.CreativeStackInfo(new ItemStack(charm).withComponentSignature("compat-b"), relics)
                ),
                null
        );

        assertEquals(1, entries.size());
        assertFalse(entries.get(0).extraMeta().containsKey(SearchNodeKeys.ACCESS_LEVEL));
    }

    @Test
    void cheatModeSkipsVisiblyIdenticalCreativeStacksWithDifferentHiddenComponents() {
        AmiConfig.cheatMode = true;
        AmiConfig.devMode = true;
        Item charm = new Item("Charm of Shrinking");
        ItemFilter.CreativeTabInfo relics = new ItemFilter.CreativeTabInfo("relics:relics", "Relics");

        List<SubtypeExpander.SubtypeEntry> entries = CreativeStackVariantExpander.expand(
                new ResourceLocation("artifacts", "charm_of_shrinking"),
                List.of(
                        new ItemFilter.CreativeStackInfo(new ItemStack(charm).withComponentSignature("compat-a"), relics),
                        new ItemFilter.CreativeStackInfo(new ItemStack(charm).withComponentSignature("compat-b"), relics)
                ),
                null
        );

        assertEquals(1, entries.size());
        assertFalse(entries.get(0).extraMeta().containsKey(SearchNodeKeys.ACCESS_LEVEL));
    }

    @Test
    void distinctCreativeStackNamesStillExpand() {
        Item barrel = new Item("Barrel");
        ItemFilter.CreativeTabInfo storage = new ItemFilter.CreativeTabInfo("test:storage", "Storage");

        List<SubtypeExpander.SubtypeEntry> entries = CreativeStackVariantExpander.expand(
                new ResourceLocation("test", "barrel"),
                List.of(
                        new ItemFilter.CreativeStackInfo(new ItemStack(barrel)
                                .withComponentSignature("oak")
                                .withHoverName("Oak Barrel"), storage),
                        new ItemFilter.CreativeStackInfo(new ItemStack(barrel)
                                .withComponentSignature("spruce")
                                .withHoverName("Spruce Barrel"), storage)
                ),
                null
        );

        assertEquals(2, entries.size());
    }

    @Test
    void creativeVariantIdsAreStableWhenSourceOrderChanges() {
        Item barrel = new Item("Barrel");
        ItemFilter.CreativeTabInfo storage = new ItemFilter.CreativeTabInfo("test:storage", "Storage");
        ItemFilter.CreativeStackInfo oak = new ItemFilter.CreativeStackInfo(new ItemStack(barrel)
                .withComponentSignature("oak")
                .withHoverName("Oak Barrel"), storage);
        ItemFilter.CreativeStackInfo spruce = new ItemFilter.CreativeStackInfo(new ItemStack(barrel)
                .withComponentSignature("spruce")
                .withHoverName("Spruce Barrel"), storage);

        Set<ResourceLocation> firstOrder = CreativeStackVariantExpander.expand(
                        new ResourceLocation("test", "barrel"),
                        List.of(oak, spruce),
                        null
                )
                .stream()
                .map(SubtypeExpander.SubtypeEntry::id)
                .collect(Collectors.toSet());
        Set<ResourceLocation> secondOrder = CreativeStackVariantExpander.expand(
                        new ResourceLocation("test", "barrel"),
                        List.of(spruce, oak),
                        null
                )
                .stream()
                .map(SubtypeExpander.SubtypeEntry::id)
                .collect(Collectors.toSet());

        assertEquals(firstOrder, secondOrder);
        assertTrue(firstOrder.stream().allMatch(id -> id.getPath().matches("barrel/variant/[a-z_]+_[0-9a-f]{12}")));
    }

    @Test
    void ae2FacadesDoNotExpandCreativeTabVariantsByDefault() {
        Item facade = new Item("Cable Facade");
        ItemFilter.CreativeTabInfo facades = new ItemFilter.CreativeTabInfo("ae2:facades", "AE2 Facades");

        List<SubtypeExpander.SubtypeEntry> entries = CreativeStackVariantExpander.expand(
                new ResourceLocation("ae2", "facade"),
                List.of(
                        new ItemFilter.CreativeStackInfo(new ItemStack(facade)
                                .withComponentSignature("minecraft:oak_log")
                                .withHoverName("Cable Facade - Oak Log"), facades),
                        new ItemFilter.CreativeStackInfo(new ItemStack(facade)
                                .withComponentSignature("minecraft:stone")
                                .withHoverName("Cable Facade - Stone"), facades)
                ),
                null
        );

        assertEquals(0, entries.size());
    }

    @Test
    void suppressedFacadeVariantsCanBeExplainedOnBaseNode() {
        Map<String, String> meta = new java.util.HashMap<>();

        ItemProvider.applySuppressedCreativeVariantMeta(meta,
                new ItemProvider.SuppressedCreativeVariants("facade_variants_hidden_by_default", 926));

        assertEquals("facade_variants_hidden_by_default", meta.get(SearchNodeKeys.VARIANT_SUPPRESSION_REASON));
        assertEquals("926", meta.get(SearchNodeKeys.VARIANT_SUPPRESSED_COUNT));
    }

    @Test
    void subtypeCategoryAssignmentUsesMetricAddedFacets() {
        Item copperCan = new Item("Copper Can");
        Map<String, String> meta = new java.util.HashMap<>();
        meta.put(SearchNodeKeys.MOD_ID, "tconstruct");
        meta.put(SearchNodeKeys.FACETS, ItemFacet.FLUID_CONTAINER.id());

        ItemProvider.applyPrimaryCategoryMeta(
                new ResourceLocation("tconstruct", "copper_can"),
                copperCan,
                new FacetProfile(EnumSet.noneOf(ItemFacet.class), Map.of()),
                meta
        );

        assertEquals("utility", meta.get(SearchNodeKeys.ONTOLOGY_CATEGORY));
        assertEquals("misc", meta.get(SearchNodeKeys.ONTOLOGY_SUBCATEGORY));
    }

    @Test
    void devModeCanInspectAe2FacadeCreativeTabVariants() {
        AmiConfig.devMode = true;
        Item facade = new Item("Cable Facade");
        ItemFilter.CreativeTabInfo facades = new ItemFilter.CreativeTabInfo("ae2:facades", "AE2 Facades");

        List<SubtypeExpander.SubtypeEntry> entries = CreativeStackVariantExpander.expand(
                new ResourceLocation("ae2", "facade"),
                List.of(
                        new ItemFilter.CreativeStackInfo(new ItemStack(facade)
                                .withComponentSignature("minecraft:oak_log")
                                .withHoverName("Cable Facade - Oak Log"), facades),
                        new ItemFilter.CreativeStackInfo(new ItemStack(facade)
                                .withComponentSignature("minecraft:stone")
                                .withHoverName("Cable Facade - Stone"), facades)
                ),
                null
        );

        assertEquals(2, entries.size());
    }

    @Test
    void tooltipResourceDetectionReadsStoredAmountsNotItemNames() {
        assertTrue(CreativeStackVariantExpander.tooltipIndicatesPositiveStoredResource(List.of(
                "Energy: 80,000 / 80,000 FE"
        )));
        assertTrue(CreativeStackVariantExpander.tooltipIndicatesPositiveStoredResource(List.of(
                "Fluid: Lava 1000 mB"
        )));

        assertFalse(CreativeStackVariantExpander.tooltipIndicatesPositiveStoredResource(List.of(
                "Energy: 0 / 80,000 FE"
        )));
        assertFalse(CreativeStackVariantExpander.tooltipIndicatesPositiveStoredResource(List.of(
                "Capacity: 80,000 FE"
        )));
        assertFalse(CreativeStackVariantExpander.tooltipIndicatesPositiveStoredResource(List.of(
                "Advanced Energy Cube"
        )));
    }
}
