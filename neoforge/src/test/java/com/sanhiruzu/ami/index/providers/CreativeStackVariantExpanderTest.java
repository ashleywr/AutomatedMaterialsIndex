package com.sanhiruzu.ami.index.providers;

import com.sanhiruzu.ami.config.AmiConfig;
import com.sanhiruzu.ami.index.ItemFilter;
import com.sanhiruzu.ami.index.SearchNodeKeys;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

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
    void cheatModeMarksVisiblyIdenticalCreativeStacksWithDifferentHiddenComponents() {
        AmiConfig.cheatMode = true;
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

        assertEquals(2, entries.size());
        assertEquals(ItemFilter.ACCESS_CHEAT, entries.get(1).extraMeta().get(SearchNodeKeys.ACCESS_LEVEL));
        assertEquals("hidden_component_duplicate", entries.get(1).extraMeta().get("variantAccessReason"));
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
