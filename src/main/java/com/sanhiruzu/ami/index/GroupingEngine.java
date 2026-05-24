package com.sanhiruzu.ami.index;

import com.sanhiruzu.ami.config.AmiConfig;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.*;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.block.state.BlockState;

import java.util.*;

/**
 * Shared item grouping classifiers used by indexers and result processors.
 * <p>
 * Uses a Tag-Lexical Heuristic to identify shapes and materials without
 * hardcoded lists where possible, allowing it to scale to modded items.
 */
public class GroupingEngine {
    public record CollapsedFamily(String key, String label) {
    }

    public enum GroupType {SHAPE, COLOR, MATERIAL}

    private static final List<String> COLOR_BUCKETS = new ArrayList<>(Arrays.asList(
            "light_blue", "light_gray",
            "magenta", "orange", "yellow", "lime", "pink", "cyan", "purple",
            "white", "gray", "blue", "brown", "green", "red", "black"
    ));

    private static final String[] STATE_PREFIXES = {"cooked_", "raw_", "roasted_"};
    private static final String[] FAMILY_PREFIXES = {"stripped_", "waxed_", "exposed_", "weathered_", "oxidized_", "chiseled_", "cut_", "smooth_", "cracked_", "polished_"};
    private static final Set<String> DYNAMIC_SHAPE_STOPWORDS = Set.of(
            "item", "items", "block", "blocks", "ore", "ingot", "dust", "nugget", "plate", "gem",
            "chunk", "shard", "piece", "material", "essence", "seed", "seeds", "part", "parts",
            "blue", "red", "green", "gray", "grey", "light", "dark", "white", "black", "brown",
            "yellow", "purple", "orange", "pink", "cyan", "magenta", "lime"
    );
    private static final Set<String> KNOWN_SHAPE_TERMS = Set.of(
            "stairs", "stair", "slab", "slabs", "wall", "walls", "fence", "fences", "gate", "gates",
            "door", "doors", "trapdoor", "trapdoors", "sign", "signs", "button", "buttons",
            "bucket", "buckets", "boat", "boats", "torch", "torches", "pane", "panes",
            "sapling", "saplings", "carpet", "carpets"
    );
    // Dynamic discovery state
    private static final Map<Item, Item> STONECUTTER_MAP = new HashMap<>();
    private static final Set<String> DYNAMIC_SHAPE_KEYWORDS = new HashSet<>();
    private static final Set<String> APPROVED_DYNAMIC_SHAPES = new HashSet<>();

    /**
     * Order for shape grouping: blocks and logical items first, "unknown" item last.
     */
    public static final List<String> SHAPE_ORDER = List.of(
            "cube", "block", "stairs", "slabs", "walls", "fences", "fence_gates",
            "doors", "trapdoors", "signs", "beds", "buttons", "pressure_plates",
            "torches", "plants", "tools", "armor", "food", "boats", "minecarts"
    );

    /**
     * Build heuristics from the live world state (recipes, families, tags).
     */
    public static void initialize(net.minecraft.world.level.Level level) {
        STONECUTTER_MAP.clear();
        DYNAMIC_SHAPE_KEYWORDS.clear();
        APPROVED_DYNAMIC_SHAPES.clear();

        // 1. Dynamic Color Discovery: scan for modded color tags (c:dyes/*)
        Set<String> discoveredColors = new HashSet<>(COLOR_BUCKETS);
        try {
            BuiltInRegistries.ITEM.getTags().forEach(tag -> {
                ResourceLocation loc = tag.getFirst().location();
                if (loc.getNamespace().equals("c") && loc.getPath().startsWith("dyes/")) {
                    discoveredColors.add(loc.getPath().substring(5));
                }
            });
        } catch (Exception ignored) {
        }

        COLOR_BUCKETS.clear();
        COLOR_BUCKETS.addAll(discoveredColors);
        COLOR_BUCKETS.sort((a, b) -> {
            int lenCompare = Integer.compare(b.length(), a.length());
            return lenCompare != 0 ? lenCompare : a.compareTo(b);
        });

        // 2. Dynamic Shape Discovery: Build a "Vocabulary" of shapes from common tags
        try {
            BuiltInRegistries.ITEM.getTags().forEach(tag -> {
                String path = tag.getFirst().location().getPath();
                if (path.contains("/") && !path.startsWith("dyes/")) {
                    String shape = path.substring(path.lastIndexOf('/') + 1);
                    if (shape.length() > 3) DYNAMIC_SHAPE_KEYWORDS.add(shape);
                }
            });
        } catch (Exception ignored) {
        }

        DYNAMIC_SHAPE_KEYWORDS.addAll(Arrays.asList(
                "stairs", "slab", "wall", "fence", "gate", "door", "trapdoor", "button", "plate",
                "sign", "bed", "boat", "minecart", "pickaxe", "axe", "shovel", "hoe", "sword",
                "helmet", "chestplate", "leggings", "boots", "sapling", "leaves", "log", "wood", "planks",
                "chest_boat", "hanging_sign", "pressure_plate", "bucket", "spawn_egg", "bricks", "block", "ball"
        ));

        if (level == null) return;

        // 3. Stonecutter Heuristics
        try {
            for (var holder : level.getRecipeManager().getAllRecipesFor(RecipeType.STONECUTTING)) {
                ItemStack result = holder.value().getResultItem(level.registryAccess());
                if (result.isEmpty()) continue;
                var ingredients = holder.value().getIngredients();
                if (ingredients.isEmpty()) continue;
                for (ItemStack input : ingredients.get(0).getItems()) {
                    if (!input.isEmpty()) {
                        STONECUTTER_MAP.put(result.getItem(), input.getItem());
                        break;
                    }
                }
            }
        } catch (Exception e) {
            com.sanhiruzu.ami.AMI.LOGGER.warn("GroupingEngine: Failed to build Stonecutter heuristics", e);
        }
    }

    public static void rebuildDynamicShapeCandidates(Iterable<Item> items) {
        List<ResourceLocation> ids = new ArrayList<>();
        for (Item item : items) {
            ResourceLocation id = BuiltInRegistries.ITEM.getKey(item);
            if (id != null) ids.add(id);
        }
        rebuildDynamicShapeCandidatesFromIds(ids);
    }

    public static void rebuildDynamicShapeCandidatesFromIds(Iterable<ResourceLocation> ids) {
        Map<String, Integer> tokenCount = new HashMap<>();
        Map<String, Set<String>> tokenMods = new HashMap<>();

        for (ResourceLocation id : ids) {
            if (id == null || "minecraft".equals(id.getNamespace())) continue;
            String token = extractTrailingToken(id.getPath());
            if (token == null) continue;
            if (DYNAMIC_SHAPE_STOPWORDS.contains(token)) continue;
            if (KNOWN_SHAPE_TERMS.contains(token)) continue;

            tokenCount.merge(token, 1, Integer::sum);
            tokenMods.computeIfAbsent(token, ignored -> new HashSet<>()).add(id.getNamespace());
        }

        APPROVED_DYNAMIC_SHAPES.clear();
        for (Map.Entry<String, Integer> entry : tokenCount.entrySet()) {
            String token = entry.getKey();
            int count = entry.getValue();
            int spread = tokenMods.getOrDefault(token, Set.of()).size();
            int minCount = Math.max(1, AmiConfig.dynamicShapeMinCount);
            int minSpread = Math.max(1, AmiConfig.dynamicShapeMinModSpread);
            if (count >= minCount && spread >= minSpread) {
                APPROVED_DYNAMIC_SHAPES.add(token);
            }
        }
    }

    public static Map<String, List<ItemStack>> groupByShape(List<ItemStack> items) {
        Map<String, List<ItemStack>> groups = new LinkedHashMap<>();
        for (ItemStack stack : items) {
            groups.computeIfAbsent(classifyShape(stack), k -> new ArrayList<>()).add(stack);
        }
        return sortAndFilterGroups(groups, SHAPE_ORDER);
    }

    public static String classifyShape(ItemStack stack) {
        Item item = stack.getItem();
        if (item instanceof SwordItem) return "tools";
        if (item instanceof TieredItem) return "tools";
        if (item instanceof ArmorItem) return "armor";
        if (item instanceof BoatItem) return "boats";
        if (item instanceof MinecartItem) return "minecarts";
        if (stack.has(DataComponents.FOOD)) return "food";

        Optional<BlockState> state = defaultBlockState(item);
        if (state.isPresent()) {
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
            if (s.is(BlockTags.SIGNS) || s.is(BlockTags.ALL_HANGING_SIGNS)) return "signs";
            if (s.is(BlockTags.BEDS)) return "beds";
            if (s.is(BlockTags.RAILS)) return "rails";

            String dynamic = classifyDynamicModShape(stack);
            if (dynamic != null) return dynamic;

            try {
                if (s.isCollisionShapeFullBlock(net.minecraft.world.level.EmptyBlockGetter.INSTANCE, net.minecraft.core.BlockPos.ZERO)) {
                    return "cube";
                }
            } catch (Exception ignored) {
            }
            return "block";
        }
        String dynamic = classifyDynamicModShape(stack);
        if (dynamic != null) return dynamic;
        return "item";
    }

    public static String classifyShape(Item item) {
        return classifyShape(new ItemStack(item));
    }

    public static Map<String, List<ItemStack>> groupByColor(List<ItemStack> items) {
        Map<String, List<ItemStack>> groups = new LinkedHashMap<>();
        for (ItemStack stack : items) {
            groups.computeIfAbsent(classifyColor(stack), k -> new ArrayList<>()).add(stack);
        }
        return sortAndFilterGroups(groups, List.of());
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
        return classifyColorFromPath(id.getPath());
    }

    public static String classifyColorFromPath(String path) {
        for (String color : COLOR_BUCKETS) {
            if (hasToken(path, color)) return color;
        }
        return "";
    }

    public static Map<String, List<ItemStack>> groupByMaterial(List<ItemStack> items) {
        Map<String, List<ItemStack>> groups = new LinkedHashMap<>();
        for (ItemStack stack : items) {
            groups.computeIfAbsent(classifyMaterialRoot(stack), k -> new ArrayList<>()).add(stack);
        }
        return sortAndFilterGroups(groups, List.of());
    }

    public static String classifyMaterialRoot(ItemStack stack) {
        Item item = stack.getItem();
        Item root = STONECUTTER_MAP.get(item);
        if (root != null) {
            ResourceLocation rootId = BuiltInRegistries.ITEM.getKey(root);
            if (rootId != null) return rootId.toString();
        }

        String matFromTags = identifyMaterialFromTags(stack);
        if (matFromTags != null) return matFromTags;

        ResourceLocation id = BuiltInRegistries.ITEM.getKey(item);
        if (id == null) return "";

        String path = id.getPath();
        String stripped = stripDynamicShapes(path);
        String noState = stripStatePrefix(stripped);
        return id.getNamespace() + ":" + stripColorPrefix(noState);
    }

    public static String classifyFamilyRoot(ItemStack stack) {
        String materialRoot = classifyMaterialRoot(stack);
        int colonIdx = materialRoot.indexOf(':');
        if (colonIdx == -1) return materialRoot;

        String namespace = materialRoot.substring(0, colonIdx);
        String path = materialRoot.substring(colonIdx + 1);

        String stripped = stripFamilyPrefix(path);
        return namespace + ":" + stripped;
    }

    public static Optional<CollapsedFamily> classifyCollapsedFamily(ResourceLocation id) {
        String path = id.getPath();
        if (path.startsWith("music_disc_")) {
            return Optional.of(new CollapsedFamily("music_discs", "Music Discs"));
        }
        if (path.endsWith("_smithing_template")) {
            return Optional.of(new CollapsedFamily("smithing_templates", "Smithing Templates"));
        }
        if (path.endsWith("_banner") && !path.contains("banner_pattern")) {
            return Optional.of(new CollapsedFamily("banners", "Banners"));
        }
        if (path.equals("goat_horn")) {
            return Optional.of(new CollapsedFamily("goat_horns", "Goat Horns"));
        }
        return Optional.empty();
    }

    private static String stripFamilyPrefix(String path) {
        String result = path;
        boolean changed;
        do {
            changed = false;
            for (String prefix : FAMILY_PREFIXES) {
                if (result.startsWith(prefix)) {
                    result = result.substring(prefix.length());
                    changed = true;
                }
            }
        } while (changed);
        return result;
    }

    private static String stripStatePrefix(String path) {
        for (String prefix : STATE_PREFIXES) {
            if (path.startsWith(prefix)) {
                return path.substring(prefix.length());
            }
        }
        return path;
    }

    private static String stripDynamicShapes(String path) {
        String result = path;
        List<String> sortedKeywords = new ArrayList<>(DYNAMIC_SHAPE_KEYWORDS);
        // CRITICAL: Sort by length descending so "chest_boat" matches before "boat"
        sortedKeywords.sort((a, b) -> Integer.compare(b.length(), a.length()));

        for (String keyword : sortedKeywords) {
            if (hasToken(result, keyword)) {
                result = result.replace("_" + keyword + "_", "_")
                        .replace(keyword + "_", "")
                        .replace("_" + keyword, "");
            }
        }
        // Clean up any double underscores from stripping middle keywords
        while (result.contains("__")) result = result.replace("__", "_");
        if (result.startsWith("_")) result = result.substring(1);
        if (result.endsWith("_")) result = result.substring(0, result.length() - 1);

        return result;
    }

    private static String identifyMaterialFromTags(ItemStack stack) {
        return stack.getTags().map(tag -> tag.location().toString())
                .filter(t -> t.contains("ingots/") || t.contains("gems/") || t.contains("planks/"))
                .findFirst()
                .map(t -> {
                    int lastSlash = t.lastIndexOf('/');
                    return (lastSlash >= 0) ? "minecraft:" + t.substring(lastSlash + 1) : t;
                })
                .orElse(null);
    }

    public static int representativeWeight(ItemStack stack) {
        ResourceLocation id = BuiltInRegistries.ITEM.getKey(stack.getItem());
        if (id == null) return 100;
        String path = id.getPath();
        if (path.endsWith("_wool") || path.endsWith("_terracotta") || path.endsWith("_concrete")) return 0;
        if (path.endsWith("_planks") || path.endsWith("_block")) return 1;
        if (path.endsWith("_stairs") || path.endsWith("_slab") || path.endsWith("_wall")) return 20;
        return 10;
    }

    private static final java.util.Set<String> UNKNOWN_GROUP_MARKERS = java.util.Set.of(
            "", "item", "minecraft:item", "block"
    );

    private static boolean isUnknownGroup(String key) {
        return UNKNOWN_GROUP_MARKERS.contains(key) || key.toLowerCase().contains("unknown");
    }

    public static Map<String, List<SearchNode>> sortGroups(Map<String, List<SearchNode>> groups, List<String> order, boolean ascending) {
        java.util.Map<String, Integer> orderMap = new java.util.HashMap<>();
        for (int i = 0; i < order.size(); i++) {
            orderMap.put(order.get(i), i);
        }

        List<Map.Entry<String, List<SearchNode>>> entries = new ArrayList<>(groups.entrySet());
        entries.sort((a, b) -> {
            String k1 = a.getKey();
            String k2 = b.getKey();
            Integer i1 = orderMap.get(k1);
            Integer i2 = orderMap.get(k2);
            int cmp;
            if (i1 != null && i2 != null) cmp = Integer.compare(i1, i2);
            else if (i1 != null) cmp = -1;
            else if (i2 != null) cmp = 1;
            else {
                boolean u1 = isUnknownGroup(k1);
                boolean u2 = isUnknownGroup(k2);
                if (u1 && !u2) cmp = 1;
                else if (!u1 && u2) cmp = -1;
                else cmp = k1.compareTo(k2);
            }
            return ascending ? cmp : -cmp;
        });
        Map<String, List<SearchNode>> result = new LinkedHashMap<>();
        for (var entry : entries) result.put(entry.getKey(), entry.getValue());
        return result;
    }

    private static Map<String, List<ItemStack>> sortAndFilterGroups(Map<String, List<ItemStack>> groups, List<String> order) {
        java.util.Map<String, Integer> orderMap = new java.util.HashMap<>();
        for (int i = 0; i < order.size(); i++) {
            orderMap.put(order.get(i), i);
        }

        List<Map.Entry<String, List<ItemStack>>> entries = new ArrayList<>(groups.entrySet());
        entries.sort((a, b) -> {
            String k1 = a.getKey();
            String k2 = b.getKey();
            Integer i1 = orderMap.get(k1);
            Integer i2 = orderMap.get(k2);
            if (i1 != null && i2 != null) return Integer.compare(i1, i2);
            if (i1 != null) return -1;
            if (i2 != null) return 1;
            boolean u1 = isUnknownGroup(k1);
            boolean u2 = isUnknownGroup(k2);
            if (u1 && !u2) return 1;
            if (!u1 && u2) return -1;
            return k1.compareTo(k2);
        });
        Map<String, List<ItemStack>> result = new LinkedHashMap<>();
        for (var entry : entries) {
            if (entry.getValue().size() > 1) result.put(entry.getKey(), entry.getValue());
        }
        return result;
    }

    private static Optional<BlockState> defaultBlockState(Item item) {
        if (!(item instanceof BlockItem blockItem)) return Optional.empty();
        return Optional.of(blockItem.getBlock().defaultBlockState());
    }

    private static String stripColorPrefix(String path) {
        for (String color : COLOR_BUCKETS) {
            String prefix = color + "_";
            if (path.startsWith(prefix)) return path.substring(prefix.length());
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

    private static String classifyDynamicModShape(ItemStack stack) {
        ResourceLocation id = BuiltInRegistries.ITEM.getKey(stack.getItem());
        if (id == null || "minecraft".equals(id.getNamespace())) return null;

        Optional<String> fromTag = stack.getTags()
                .map(TagKey::location)
                .filter(loc -> "c".equals(loc.getNamespace()))
                .map(ResourceLocation::getPath)
                .filter(path -> path.startsWith("shapes/"))
                .map(path -> path.substring("shapes/".length()))
                .filter(shape -> !shape.isBlank())
                .findFirst();
        if (fromTag.isPresent()) return fromTag.get();

        return classifyDynamicShapeFromPath(id.getPath());
    }

    private static String classifyDynamicShapeFromPath(String path) {
        String trailing = extractTrailingToken(path);
        if (trailing == null) return null;
        return APPROVED_DYNAMIC_SHAPES.contains(trailing) ? trailing : null;
    }

    private static String extractTrailingToken(String path) {
        String normalized = path == null ? "" : path.trim().toLowerCase(Locale.ROOT);
        if (normalized.isEmpty()) return null;
        String[] tokens = normalized.split("_");
        if (tokens.length < 2) return null;
        String trailing = tokens[tokens.length - 1];
        if (trailing.length() < 2) return null;
        if (COLOR_BUCKETS.contains(trailing)) return null;
        return trailing;
    }

    private static TagKey<Item> itemTag(String namespace, String path) {
        return TagKey.create(Registries.ITEM, ResourceLocation.fromNamespaceAndPath(namespace, path));
    }
}
