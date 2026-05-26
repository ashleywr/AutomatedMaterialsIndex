package com.sanhiruzu.ami.index;

import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class GroupingEngineTest {

    @Test
    void keepsVanillaFoodHandlingUnchanged() {
        Item apple = new Item("apple").withComponent(DataComponents.FOOD);
        BuiltInRegistries.itemRegistry().register(new ResourceLocation("minecraft:apple"), apple);

        assertEquals("food", GroupingEngine.classifyShape(new ItemStack(apple)));
    }

    @Test
    void keepsVanillaUnknownItemsAsItem() {
        Item musicDisc = new Item("music_disc_cat");
        BuiltInRegistries.itemRegistry().register(new ResourceLocation("minecraft:music_disc_cat"), musicDisc);

        assertEquals("item", GroupingEngine.classifyShape(new ItemStack(musicDisc)));
    }

    @Test
    void supportsDynamicModShapeFromCommonTag() {
        Item copperWire = new Item("copper_wire")
                .withTag(TagKey.create(null, new ResourceLocation("c:shapes/wire")));
        BuiltInRegistries.itemRegistry().register(new ResourceLocation("techreborn:copper_wire"), copperWire);

        assertEquals("wire", GroupingEngine.classifyShape(new ItemStack(copperWire)));
    }

    @Test
    void supportsDynamicModShapeFromIdTokenFallback() {
        List<ResourceLocation> ids = new ArrayList<>();
        for (int i = 0; i < 45; i++) {
            String mod = switch (i % 3) {
                case 0 -> "powah";
                case 1 -> "ae2";
                default -> "mekanism";
            };
            ResourceLocation id = new ResourceLocation(mod + ":insulated_" + i + "_cable");
            BuiltInRegistries.itemRegistry().register(id, new Item("insulated_" + i + "_cable"));
            ids.add(id);
        }
        GroupingEngine.rebuildDynamicShapeCandidatesFromIds(ids);

        Item insulatedCable = new Item("insulated_cable");
        BuiltInRegistries.itemRegistry().register(new ResourceLocation("powah:insulated_cable"), insulatedCable);
        assertEquals("cable", GroupingEngine.classifyShape(new ItemStack(insulatedCable)));
    }

    @Test
    void supportsUnknownModFamiliesWithoutHardcodedDictionary() {
        List<ResourceLocation> ids = new ArrayList<>();
        for (int i = 0; i < 60; i++) {
            String mod = switch (i % 3) {
                case 0 -> "ships";
                case 1 -> "smallships";
                default -> "valkyrienskies";
            };
            ResourceLocation id = new ResourceLocation(mod + ":material_" + i + "_sail");
            BuiltInRegistries.itemRegistry().register(id, new Item("material_" + i + "_sail"));
            ids.add(id);
        }
        GroupingEngine.rebuildDynamicShapeCandidatesFromIds(ids);

        Item oakSail = new Item("oak_sail");
        BuiltInRegistries.itemRegistry().register(new ResourceLocation("ships:oak_sail"), oakSail);
        assertEquals("sail", GroupingEngine.classifyShape(new ItemStack(oakSail)));
    }

    @Test
    void doesNotPromoteSingleModNoiseToken() {
        List<ResourceLocation> ids = new ArrayList<>();
        for (int i = 0; i < 100; i++) {
            ResourceLocation id = new ResourceLocation("bibliocraft:oak_" + i + "_back");
            BuiltInRegistries.itemRegistry().register(id, new Item("oak_" + i + "_back"));
            ids.add(id);
        }
        GroupingEngine.rebuildDynamicShapeCandidatesFromIds(ids);

        Item chairBack = new Item("oak_back");
        BuiltInRegistries.itemRegistry().register(new ResourceLocation("bibliocraft:oak_back"), chairBack);
        assertEquals("item", GroupingEngine.classifyShape(new ItemStack(chairBack)));
    }

    @Test
    void classifyMaterialRootUsesCommonTags() {
        Item ironNugget = new Item("iron_nugget")
                .withTag(TagKey.create(null, new ResourceLocation("c:nuggets/iron")));
        BuiltInRegistries.itemRegistry().register(new ResourceLocation("ami_test:iron_nugget"), ironNugget);

        Item goldDust = new Item("gold_dust")
                .withTag(TagKey.create(null, new ResourceLocation("c:dusts/gold")));
        BuiltInRegistries.itemRegistry().register(new ResourceLocation("ami_test:gold_dust"), goldDust);

        Item copperOre = new Item("copper_ore")
                .withTag(TagKey.create(null, new ResourceLocation("c:ores/copper")));
        BuiltInRegistries.itemRegistry().register(new ResourceLocation("ami_test:copper_ore"), copperOre);

        Item rawTin = new Item("raw_tin")
                .withTag(TagKey.create(null, new ResourceLocation("c:raw_materials/tin")));
        BuiltInRegistries.itemRegistry().register(new ResourceLocation("ami_test:raw_tin"), rawTin);

        Item silverBlock = new Item("silver_block")
                .withTag(TagKey.create(null, new ResourceLocation("c:storage_blocks/silver")));
        BuiltInRegistries.itemRegistry().register(new ResourceLocation("ami_test:silver_block"), silverBlock);

        assertEquals("minecraft:iron", GroupingEngine.classifyMaterialRoot(new ItemStack(ironNugget)));
        assertEquals("minecraft:gold", GroupingEngine.classifyMaterialRoot(new ItemStack(goldDust)));
        assertEquals("minecraft:copper", GroupingEngine.classifyMaterialRoot(new ItemStack(copperOre)));
        assertEquals("minecraft:tin", GroupingEngine.classifyMaterialRoot(new ItemStack(rawTin)));
        assertEquals("minecraft:silver", GroupingEngine.classifyMaterialRoot(new ItemStack(silverBlock)));
    }
}
