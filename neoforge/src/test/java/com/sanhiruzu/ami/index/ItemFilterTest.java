package com.sanhiruzu.ami.index;

import com.sanhiruzu.ami.config.AmiConfig;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.SpawnEggItem;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class ItemFilterTest {

    @BeforeEach
    void setup() {
        AmiConfig.resetToDefaults();
    }

    @Test
    void testClassifyAccessLevel() {
        // Survival items
        assertEquals(ItemFilter.ACCESS_SURVIVAL, ItemFilter.classifyAccessLevel(new ResourceLocation("minecraft:iron_ingot"), true));

        // Creative/Cheat items
        assertEquals(ItemFilter.ACCESS_CREATIVE, ItemFilter.classifyAccessLevel(new ResourceLocation("minecraft:zombie_spawn_egg"), true));
        assertEquals(ItemFilter.ACCESS_CREATIVE, ItemFilter.classifyAccessLevel(
                new ResourceLocation("alexsmobs:spawn_egg_snow_leopard"), new SpawnEggItem("Snow Leopard Spawn Egg"), true));
        assertEquals(ItemFilter.ACCESS_CREATIVE, ItemFilter.classifyAccessLevel(
                new ResourceLocation("mod:nonstandard_entity_token"), new SpawnEggItem("Spawn Egg"), true));
        assertEquals(ItemFilter.ACCESS_SURVIVAL, ItemFilter.classifyAccessLevel(
                new ResourceLocation("mod:regular_egg"), new Item("Regular Egg"), true));
        assertEquals(ItemFilter.ACCESS_CHEAT, ItemFilter.classifyAccessLevel(new ResourceLocation("minecraft:command_block"), true));
        assertEquals(ItemFilter.ACCESS_CREATIVE, ItemFilter.classifyAccessLevel(new ResourceLocation("mekanism:creative_fluid_tank"), true));
        assertEquals(ItemFilter.ACCESS_CREATIVE, ItemFilter.classifyAccessLevel(new ResourceLocation("example:infinite_creative_battery"), true));

        // Hidden items (not in creative)
        assertEquals(ItemFilter.ACCESS_DEV, ItemFilter.classifyAccessLevel(new ResourceLocation("minecraft:iron_ingot"), false));
    }

    @Test
    void testShouldShowAccessLevel() {
        // Default survival should always show
        assertTrue(ItemFilter.shouldShowAccessLevel(ItemFilter.ACCESS_SURVIVAL));

        // Cheat mode off - shouldn't show cheat items
        assertFalse(ItemFilter.shouldShowAccessLevel(ItemFilter.ACCESS_CHEAT));

        // Creative-only items are indexed so the result UI can filter them instantly.
        assertTrue(ItemFilter.shouldShowAccessLevel(ItemFilter.ACCESS_CREATIVE));

        // Cheat mode on
        AmiConfig.cheatMode = true;
        assertTrue(ItemFilter.shouldShowAccessLevel(ItemFilter.ACCESS_CHEAT));
        assertTrue(ItemFilter.shouldShowAccessLevel(ItemFilter.ACCESS_CREATIVE));

        // Dev mode on
        AmiConfig.cheatMode = false;
        AmiConfig.devMode = true;
        assertTrue(ItemFilter.shouldShowAccessLevel(ItemFilter.ACCESS_DEV));
    }
}
