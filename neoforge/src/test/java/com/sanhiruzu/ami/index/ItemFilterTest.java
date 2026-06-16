package com.sanhiruzu.ami.index;

import com.sanhiruzu.ami.config.AmiConfig;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.SpawnEggItem;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

public class ItemFilterTest {

    @BeforeEach
    void setup() {
        AmiConfig.resetToDefaults();
    }

    @Test
    void testClassifyAccessLevel() {
        // Survival items
        assertEquals(ItemFilter.ACCESS_SURVIVAL, ItemFilter.classifyAccessLevel(new Identifier("minecraft:iron_ingot"), true));

        // Creative/Cheat items
        assertEquals(ItemFilter.ACCESS_CREATIVE, ItemFilter.classifyAccessLevel(new Identifier("minecraft:zombie_spawn_egg"), true));
        assertEquals(ItemFilter.ACCESS_CREATIVE, ItemFilter.classifyAccessLevel(
                new Identifier("alexsmobs:spawn_egg_snow_leopard"), new SpawnEggItem("Snow Leopard Spawn Egg"), true));
        assertEquals(ItemFilter.ACCESS_CREATIVE, ItemFilter.classifyAccessLevel(
                new Identifier("mod:nonstandard_entity_token"), new SpawnEggItem("Spawn Egg"), true));
        assertEquals(ItemFilter.ACCESS_SURVIVAL, ItemFilter.classifyAccessLevel(
                new Identifier("mod:regular_egg"), new Item("Regular Egg"), true));
        assertEquals(ItemFilter.ACCESS_CHEAT, ItemFilter.classifyAccessLevel(new Identifier("minecraft:command_block"), true));
        assertEquals(ItemFilter.ACCESS_CREATIVE, ItemFilter.classifyAccessLevel(new Identifier("mekanism:creative_fluid_tank"), true));
        assertEquals(ItemFilter.ACCESS_CREATIVE, ItemFilter.classifyAccessLevel(new Identifier("example:infinite_creative_battery"), true));

        // Path patterns that were too broad — these are real functional items, not debug items
        assertEquals(ItemFilter.ACCESS_SURVIVAL, ItemFilter.classifyAccessLevel(new Identifier("naturesaura:effect_powder"), true));
        assertEquals(ItemFilter.ACCESS_CREATIVE, ItemFilter.classifyAccessLevel(new Identifier("spectrum:particle_spawner"), true));
        assertEquals(ItemFilter.ACCESS_CREATIVE, ItemFilter.classifyAccessLevel(new Identifier("spectrum:creative_particle_spawner"), true));

        // Debug/test patterns that should still be dev-only
        assertEquals(ItemFilter.ACCESS_DEV, ItemFilter.classifyAccessLevel(new Identifier("ae2:debug_card"), true));
        assertEquals(ItemFilter.ACCESS_DEV, ItemFilter.classifyAccessLevel(new Identifier("mod:test_item_generator"), true));

        // Hidden items (not in creative)
        assertEquals(ItemFilter.ACCESS_DEV, ItemFilter.classifyAccessLevel(new Identifier("minecraft:iron_ingot"), false));
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

    @Test
    void appendCreativeStackKeepsSearchOnlyVariantsAndDedupesOverlap() {
        Item plastic = new Item("Plastic Sheet");
        Item soulVial = new Item("Soul Vial");
        net.minecraft.core.registries.BuiltInRegistries.itemRegistry().register(
                new Identifier("pneumaticcraft", "plastic"), plastic);
        net.minecraft.core.registries.BuiltInRegistries.itemRegistry().register(
                new Identifier("enderio", "soul_vial"), soulVial);

        Map<Item, List<ItemFilter.CreativeStackInfo>> items = new LinkedHashMap<>();
        Map<Item, List<ItemStack>> seen = new LinkedHashMap<>();
        ItemFilter.CreativeTabInfo tab = new ItemFilter.CreativeTabInfo("test:machines", "Machines");

        assertTrue(ItemFilter.appendCreativeStack(items, seen, new ItemStack(soulVial).withComponentSignature("zombie"), tab));
        assertFalse(ItemFilter.appendCreativeStack(items, seen, new ItemStack(soulVial).withComponentSignature("zombie"), tab));
        assertTrue(ItemFilter.appendCreativeStack(items, seen, new ItemStack(soulVial).withComponentSignature("skeleton"), tab));
        assertTrue(ItemFilter.appendCreativeStack(items, seen, new ItemStack(plastic), tab));

        assertEquals(2, items.getOrDefault(soulVial, List.of()).size());
        assertEquals(1, items.getOrDefault(plastic, List.of()).size());
    }
}
