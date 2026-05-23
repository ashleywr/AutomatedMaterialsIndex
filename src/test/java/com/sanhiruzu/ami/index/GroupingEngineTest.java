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
        BuiltInRegistries.itemRegistry().register(ResourceLocation.parse("minecraft:apple"), apple);

        assertEquals("food", GroupingEngine.classifyShape(new ItemStack(apple)));
    }

    @Test
    void keepsVanillaUnknownItemsAsItem() {
        Item musicDisc = new Item("music_disc_cat");
        BuiltInRegistries.itemRegistry().register(ResourceLocation.parse("minecraft:music_disc_cat"), musicDisc);

        assertEquals("item", GroupingEngine.classifyShape(new ItemStack(musicDisc)));
    }

    @Test
    void supportsDynamicModShapeFromCommonTag() {
        Item copperWire = new Item("copper_wire")
                .withTag(TagKey.create(null, ResourceLocation.parse("c:shapes/wire")));
        BuiltInRegistries.itemRegistry().register(ResourceLocation.parse("techreborn:copper_wire"), copperWire);

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
            ResourceLocation id = ResourceLocation.parse(mod + ":insulated_" + i + "_cable");
            BuiltInRegistries.itemRegistry().register(id, new Item("insulated_" + i + "_cable"));
            ids.add(id);
        }
        GroupingEngine.rebuildDynamicShapeCandidatesFromIds(ids);

        Item insulatedCable = new Item("insulated_cable");
        BuiltInRegistries.itemRegistry().register(ResourceLocation.parse("powah:insulated_cable"), insulatedCable);
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
            ResourceLocation id = ResourceLocation.parse(mod + ":material_" + i + "_sail");
            BuiltInRegistries.itemRegistry().register(id, new Item("material_" + i + "_sail"));
            ids.add(id);
        }
        GroupingEngine.rebuildDynamicShapeCandidatesFromIds(ids);

        Item oakSail = new Item("oak_sail");
        BuiltInRegistries.itemRegistry().register(ResourceLocation.parse("ships:oak_sail"), oakSail);
        assertEquals("sail", GroupingEngine.classifyShape(new ItemStack(oakSail)));
    }

    @Test
    void doesNotPromoteSingleModNoiseToken() {
        List<ResourceLocation> ids = new ArrayList<>();
        for (int i = 0; i < 100; i++) {
            ResourceLocation id = ResourceLocation.parse("bibliocraft:oak_" + i + "_back");
            BuiltInRegistries.itemRegistry().register(id, new Item("oak_" + i + "_back"));
            ids.add(id);
        }
        GroupingEngine.rebuildDynamicShapeCandidatesFromIds(ids);

        Item chairBack = new Item("oak_back");
        BuiltInRegistries.itemRegistry().register(ResourceLocation.parse("bibliocraft:oak_back"), chairBack);
        assertEquals("item", GroupingEngine.classifyShape(new ItemStack(chairBack)));
    }
}
