package com.sanhiruzu.ami.index;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Shared item grouping classifiers used by indexers and result processors.
 */
public class GroupingEngine {

    public enum GroupType { SHAPE, COLOR, MATERIAL }

    private static final String[] COLOR_BUCKETS = {
            "white", "orange", "magenta", "light_blue", "yellow", "lime", "pink", "gray",
            "light_gray", "cyan", "purple", "blue", "brown", "green", "red", "black"
    };

    private static final String[] MATERIAL_SUFFIXES = {
            "_stairs", "_slab", "_wall", "_fence_gate", "_fence", "_door", "_trapdoor",
            "_button", "_pressure_plate", "_carpet", "_bed", "_banner", "_wool",
            "_pickaxe", "_axe", "_shovel", "_hoe", "_sword", "_helmet", "_chestplate",
            "_leggings", "_boots"
    };

    public static Map<String, List<ItemStack>> groupByShape(List<ItemStack> items) {
        Map<String, List<ItemStack>> groups = new LinkedHashMap<>();
        for (ItemStack stack : items) {
            groups.computeIfAbsent(classifyShape(stack), k -> new ArrayList<>()).add(stack);
        }
        return filterSingleItems(groups);
    }

    public static String classifyShape(ItemStack stack) {
        return classifyShape(stack.getItem());
    }

    public static String classifyShape(Item item) {
        Optional<BlockState> state = defaultBlockState(item);
        if (state.isEmpty()) return "item";

        BlockState s = state.get();
        if (s.is(BlockTags.STAIRS)) return "stairs";
        if (s.is(BlockTags.SLABS)) return "slabs";
        if (s.is(BlockTags.WALLS)) return "walls";
        if (s.is(BlockTags.FENCES)) return "fences";
        if (s.is(BlockTags.FENCE_GATES)) return "fence_gates";
        if (s.is(BlockTags.DOORS)) return "doors";
        if (s.is(BlockTags.TRAPDOORS)) return "trapdoors";
        if (s.is(BlockTags.BUTTONS)) return "buttons";
        if (s.is(BlockTags.PRESSURE_PLATES)) return "pressure_plates";
        return "block";
    }

    public static Map<String, List<ItemStack>> groupByColor(List<ItemStack> items) {
        Map<String, List<ItemStack>> groups = new LinkedHashMap<>();
        for (ItemStack stack : items) {
            groups.computeIfAbsent(classifyColor(stack), k -> new ArrayList<>()).add(stack);
        }
        return filterSingleItems(groups);
    }

    public static String classifyColor(ItemStack stack) {
        for (String color : COLOR_BUCKETS) {
            if (stack.is(itemTag("c", "dyes/" + color)) ||
                    stack.is(itemTag("c", color + "_dyes")) ||
                    stack.is(itemTag("minecraft", color + "_wool"))) {
                return color;
            }
        }

        ResourceLocation id = BuiltInRegistries.ITEM.getKey(stack.getItem());
        if (id == null) return "";
        return colorFromPath(id.getPath());
    }

    public static Map<String, List<ItemStack>> groupByMaterial(List<ItemStack> items) {
        Map<String, List<ItemStack>> groups = new LinkedHashMap<>();
        for (ItemStack stack : items) {
            groups.computeIfAbsent(classifyMaterialRoot(stack), k -> new ArrayList<>()).add(stack);
        }
        return filterSingleItems(groups);
    }

    public static String classifyMaterialRoot(ItemStack stack) {
        ResourceLocation id = BuiltInRegistries.ITEM.getKey(stack.getItem());
        if (id == null) return "";

        String path = id.getPath();
        String withoutColor = stripColorPrefix(path);
        String stripped = stripKnownSuffix(withoutColor);
        return id.getNamespace() + ":" + stripped;
    }

    public static int representativeWeight(ItemStack stack) {
        ResourceLocation id = BuiltInRegistries.ITEM.getKey(stack.getItem());
        if (id == null) return 100;

        String path = id.getPath();
        if (path.endsWith("_wool") || path.endsWith("_terracotta") || path.endsWith("_concrete")) return 0;
        if (path.endsWith("_planks") || path.endsWith("_block")) return 1;
        if (path.endsWith("_stairs") || path.endsWith("_slab") || path.endsWith("_wall")) return 20;
        if (path.endsWith("_carpet") || path.endsWith("_bed") || path.endsWith("_banner")) return 40;
        return 10;
    }

    private static Optional<BlockState> defaultBlockState(Item item) {
        if (!(item instanceof BlockItem blockItem)) return Optional.empty();
        return Optional.of(blockItem.getBlock().defaultBlockState());
    }

    private static String colorFromPath(String path) {
        for (String color : COLOR_BUCKETS) {
            if (hasToken(path, color)) return color;
        }
        return "";
    }

    private static String stripColorPrefix(String path) {
        for (String color : COLOR_BUCKETS) {
            String prefix = color + "_";
            if (path.startsWith(prefix)) return path.substring(prefix.length());
        }
        return path;
    }

    private static String stripKnownSuffix(String path) {
        for (String suffix : MATERIAL_SUFFIXES) {
            if (path.endsWith(suffix) && path.length() > suffix.length()) {
                return path.substring(0, path.length() - suffix.length());
            }
        }
        return path;
    }

    private static boolean hasToken(String path, String token) {
        int idx = path.indexOf(token);
        while (idx >= 0) {
            boolean beforeOk = idx == 0 || path.charAt(idx - 1) == '_';
            boolean afterOk = idx + token.length() == path.length() || path.charAt(idx + token.length()) == '_';
            if (beforeOk && afterOk) return true;
            idx = path.indexOf(token, idx + 1);
        }
        return false;
    }

    private static TagKey<Item> itemTag(String namespace, String path) {
        return TagKey.create(Registries.ITEM, ResourceLocation.fromNamespaceAndPath(namespace, path));
    }

    private static Map<String, List<ItemStack>> filterSingleItems(Map<String, List<ItemStack>> groups) {
        return groups.entrySet().stream()
                .filter(e -> !e.getKey().isEmpty())
                .filter(e -> e.getValue().size() > 1)
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        Map.Entry::getValue,
                        (a, b) -> a,
                        LinkedHashMap::new));
    }
}
