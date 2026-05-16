package com.sanhiruzu.ami.index;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.data.recipes.packs.VanillaRecipeProvider;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.client.Minecraft;

import java.util.*;
import java.util.stream.Collectors;

public class GroupingEngine {

    public enum GroupType { SHAPE, COLOR, MATERIAL }

    public static Map<String, List<ItemStack>> groupByShape(List<ItemStack> items) {
        Map<String, List<ItemStack>> groups = new HashMap<>();
        for (ItemStack stack : items) {
            String tag = getShapeTag(stack);
            groups.computeIfAbsent(tag, k -> new ArrayList<>()).add(stack);
        }
        return filterSingleItems(groups);
    }

    private static String getShapeTag(ItemStack stack) {
        if (stack.is(net.minecraft.tags.ItemTags.STAIRS)) return "Stairs";
        if (stack.is(net.minecraft.tags.ItemTags.SLABS)) return "Slabs";
        if (stack.is(net.minecraft.tags.ItemTags.WALLS)) return "Walls";
        return "Other";
    }

    public static Map<String, List<ItemStack>> groupByColor(List<ItemStack> items) {
        Map<String, List<ItemStack>> groups = new HashMap<>();
        for (ItemStack stack : items) {
            String color = parseColor(stack);
            groups.computeIfAbsent(color, k -> new ArrayList<>()).add(stack);
        }
        return filterSingleItems(groups);
    }

    private static String parseColor(ItemStack stack) {
        String name = BuiltInRegistries.ITEM.getKey(stack.getItem()).getPath();
        // Simple heuristic: look for color prefixes
        String[] colors = {"red", "blue", "green", "yellow", "white", "black", "orange", "purple"};
        for (String c : colors) {
            if (name.startsWith(c)) return c.substring(0, 1).toUpperCase() + c.substring(1);
        }
        return "Unknown";
    }

    public static Map<String, List<ItemStack>> groupByMaterial(List<ItemStack> items) {
        Map<String, List<ItemStack>> groups = new HashMap<>();
        
        // Phase 1-3 implementation simplified for brevity in logic flow
        for (ItemStack stack : items) {
            String material = "General"; // Placeholder for complex logic
            groups.computeIfAbsent(material, k -> new ArrayList<>()).add(stack);
        }
        return filterSingleItems(groups);
    }

    private static Map<String, List<ItemStack>> filterSingleItems(Map<String, List<ItemStack>> groups) {
        return groups.entrySet().stream()
                .filter(e -> e.getValue().size() > 1)
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
    }
}
