package com.sanhiruzu.ami.index;

import com.sanhiruzu.ami.AmiCore;
import com.sanhiruzu.ami.config.AmiConfig;
import com.sanhiruzu.ami.platform.Services;
import com.sanhiruzu.ami.recipe.AmiRecipeIndex;
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
    /**
     * Order for shape grouping: blocks and logical items first, "unknown" item last.
     */
    public static final List<String> SHAPE_ORDER = List.of(
            "cube", "block", "stairs", "slabs", "walls", "fences", "fence_gates",
            "doors", "trapdoors", "signs", "beds", "buttons", "pressure_plates",
            "torches", "plants", "tools", "armor", "food", "boats", "minecarts"
    );
    /**
     * Order for topology grouping: foundational items first.
     */
    public static final List<String> TOPOLOGY_ORDER = List.of(
            "minecraft:cobblestone", "minecraft:stone", "minecraft:dirt",
            "minecraft:oak_log", "minecraft:sand", "minecraft:gravel",
            "minecraft:iron_ore", "minecraft:raw_iron", "minecraft:copper_ore", "minecraft:raw_copper",
            "minecraft:gold_ore", "minecraft:raw_gold", "minecraft:diamond_ore", "minecraft:coal_ore"
    );
    public static final List<String> BEHAVIOR_ORDER = List.of(
            "behavior:energy_generation",
            "behavior:energy_storage",
            "behavior:energy_usage",
            "behavior:fluid_storage",
            "behavior:fluid_transport",
            "behavior:chemical_handling",
            "behavior:kinetic_power",
            "behavior:create_processing",
            "behavior:logistics",
            "behavior:network",
            "behavior:terminal",
            "behavior:machine",
            "behavior:upgrade",
            "behavior:storage",
            "behavior:portable_storage",
            "behavior:tool",
            "behavior:material"
    );
    private static final List<String> COLOR_BUCKETS = new ArrayList<>(Arrays.asList(
            "light_blue", "light_gray",
            "magenta", "orange", "yellow", "lime", "pink", "cyan", "purple",
            "white", "gray", "blue", "brown", "green", "red", "black"
    ));

    private static final String[] STATE_PREFIXES = {"cooked_", "raw_", "roasted_"};
    private static final String[] FAMILY_PREFIXES = {"stripped_", "waxed_", "exposed_", "weathered_", "oxidized_", "chiseled_", "cut_", "smooth_", "cracked_", "polished_", "mossy_", "infested_", "tiled_", "reinforced_", "stained_", "glowing_"};
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
    private static final Set<String> TINTABLE_FAMILY_SHAPES = Set.of(
            "stairs", "slabs", "walls", "fences", "fence_gates",
            "doors", "trapdoors", "buttons", "pressure_plates"
    );
    private static final String[] ENERGY_TERMS = {"energy", "power", "fe", "rf"};
    private static final String[] FLUID_TERMS = {"fluid", "fluids", "liquid"};
    // Dynamic discovery state
    private static final Map<Item, Item> STONECUTTER_MAP = new HashMap<>();
    private static final Set<String> DYNAMIC_SHAPE_KEYWORDS = new HashSet<>();
    private static final Set<String> APPROVED_DYNAMIC_SHAPES = new HashSet<>();

    private static final Set<String> UNKNOWN_GROUP_MARKERS = Set.of(
            "", "item", "minecraft:item", "block"
    );
    private static final Map<Item, String> TOPOLOGY_CACHE = new HashMap<>();
    private static final Map<Item, String> SIMILARITY_CACHE = new HashMap<>();
    private static final Map<Item, String> PROPERTY_CACHE = new HashMap<>();
    private static final Map<Item, Item> TOPOLOGY_DSU_MAP = new HashMap<>();
    private static boolean topologyBuilt = false;
    private static boolean similarityBuilt = false;
    private static boolean propertyBuilt = false;

    /**
     * Build heuristics from the live world state (recipes, families, tags).
     */
    public static void initialize(net.minecraft.world.level.Level level) {
        STONECUTTER_MAP.clear();
        DYNAMIC_SHAPE_KEYWORDS.clear();
        APPROVED_DYNAMIC_SHAPES.clear();
        resetTopology();

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
                    // In category/material tags, the FIRST part is usually the shape keyword (e.g. "ingots" in "ingots/iron")
                    String shape = path.substring(0, path.indexOf('/'));
                    // Trim plurals for better keyword matching
                    if (shape.endsWith("s")) shape = shape.substring(0, shape.length() - 1);
                    if (shape.length() > 3) DYNAMIC_SHAPE_KEYWORDS.add(shape);
                }
            });
        } catch (Exception ignored) {
        }

        DYNAMIC_SHAPE_KEYWORDS.addAll(Arrays.asList(
                "stairs", "slab", "wall", "fence", "gate", "door", "trapdoor", "button", "plate",
                "sign", "bed", "boat", "minecart", "pickaxe", "axe", "shovel", "hoe", "sword",
                "helmet", "chestplate", "leggings", "boots", "sapling", "leaves", "log", "wood", "planks",
                "chest_boat", "hanging_sign", "pressure_plate", "bucket", "spawn_egg", "bricks", "block", "ball",
                "ingot", "nugget", "dust", "gem", "ore", "shard", "crystal", "clump", "dirty_dust", "fragment", "cream",
                "rod", "powder", "chunk", "piece", "essence"
        ));

        if (level == null) return;

        // 3. Stonecutter Heuristics
        try {
            for (var holder : level.getRecipeManager().getAllRecipesFor(RecipeType.STONECUTTING)) {
                ItemStack result = Services.PLATFORM.getRecipeResultItem(holder, level.registryAccess());
                if (result.isEmpty()) continue;
                var ingredients = Services.PLATFORM.getRecipeIngredients(holder);
                if (ingredients.isEmpty()) continue;
                for (ItemStack input : ingredients.get(0).getItems()) {
                    if (!input.isEmpty()) {
                        STONECUTTER_MAP.put(result.getItem(), input.getItem());
                        break;
                    }
                }
            }
        } catch (Exception e) {
            AmiCore.LOGGER.warn("GroupingEngine: Failed to build Stonecutter heuristics", e);
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
        if (Services.PLATFORM.hasFood(stack)) return "food";

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
                if (isSafeCollisionShapeProbe(item)
                        && s.isCollisionShapeFullBlock(net.minecraft.world.level.EmptyBlockGetter.INSTANCE, net.minecraft.core.BlockPos.ZERO)) {
                    return "cube";
                }
            } catch (Throwable ignored) {
                // Some modded blocks load helper classes or dynamic caches from shape methods.
                // Shape grouping should never make background indexing fail or force async class loading.
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

    private static boolean isSafeCollisionShapeProbe(Item item) {
        ResourceLocation id = BuiltInRegistries.ITEM.getKey(item);
        return id != null && "minecraft".equals(id.getNamespace());
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
        if (path.contains("pottery_sherd")) return "minecraft:pottery_sherd";
        if (path.startsWith("magma_")) return "minecraft:magma";

        String stripped = stripDynamicShapes(stripVariantSuffix(path));
        String noState = stripStatePrefix(stripped);
        String noFamily = stripFamilyPrefix(noState);
        return id.getNamespace() + ":" + stripColorPrefix(noFamily);
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
        if (path.endsWith("_banner")) return Optional.of(new CollapsedFamily("banners", "Banners"));
        if (path.endsWith("_banner_pattern"))
            return Optional.of(new CollapsedFamily("banner_patterns", "Banner Patterns"));
        // goat_horn: SubtypeExpander uses synthetic IDs without item tags, so it needs explicit handling
        if (path.equals("goat_horn")) return Optional.of(new CollapsedFamily("goat_horns", "Goat Horns"));
        if (path.startsWith("music_disc_")) return Optional.of(new CollapsedFamily("music_discs", "Music Discs"));
        return Optional.empty();
    }

    public static Optional<CollapsedFamily> classifyTintableGeneratedFamily(ResourceLocation id, String shape,
                                                                            String colorBucket, String tags) {
        if (id == null || shape == null || shape.isBlank()) {
            return Optional.empty();
        }
        String normalizedShape = shape.toLowerCase(Locale.ROOT);
        if (!TINTABLE_FAMILY_SHAPES.contains(normalizedShape)) {
            return Optional.empty();
        }

        String color = tintableTagColor(tags);
        if (color.isBlank()) {
            return Optional.empty();
        }
        color = color.toLowerCase(Locale.ROOT);

        String key = id.getNamespace() + ":tintable/" + color + "/" + normalizedShape;
        return Optional.of(new CollapsedFamily(key, title(color) + " " + title(normalizedShape)));
    }

    public static Optional<CollapsedFamily> classifyLexicalGeneratedFamily(ResourceLocation id, String colorBucket) {
        if (id == null || colorBucket == null || colorBucket.isBlank()) {
            return Optional.empty();
        }
        String path = id.getPath().toLowerCase(Locale.ROOT);
        String color = colorBucket.toLowerCase(Locale.ROOT);
        if (path.startsWith(color + "_linguistic_glyph_")) {
            String key = id.getNamespace() + ":linguistic_glyph/" + color;
            return Optional.of(new CollapsedFamily(key, title(color) + " Linguistic Glyphs"));
        }
        return Optional.empty();
    }

    public static Optional<CollapsedFamily> classifyColorizedGeneratedFamily(ResourceLocation id, String displayName,
                                                                             String colorBucket, String tags,
                                                                             String materialGroup) {
        if (id == null) {
            return Optional.empty();
        }
        if (colorBucket == null || colorBucket.isBlank()) {
            // Root of a colorized family: the item has no color itself but acts as the base for colored
            // variants (e.g. minecraft:candle among white_candle…black_candle). Detect by: materialGroup
            // points back to this item AND the item has a plural form of its own path as a tag.
            if (materialGroup != null && materialGroup.equals(id.toString())) {
                String path = id.getPath().toLowerCase(Locale.ROOT);
                String pluralTag = id.getNamespace() + ":" + path + "s";
                if (hasCsvTagToken(tags, pluralTag)) {
                    return Optional.of(new CollapsedFamily(id.toString(), pluralize(title(path))));
                }
            }
            return Optional.empty();
        }
        String familyKey = "";
        if (materialGroup != null
                && !materialGroup.isBlank()
                && !materialGroup.equals(id.toString())
                && !isColorOnlyMaterialGroup(id, colorBucket, materialGroup)) {
            familyKey = materialGroup;
        } else {
            familyKey = matchingColorStrippedTag(id, colorBucket, tags);
        }
        if (familyKey.isBlank()) {
            return Optional.empty();
        }

        String label = stripColorPrefixFromDisplayName(displayName, colorBucket);
        if (label.isBlank()) {
            ResourceLocation familyId = ResourceLocation.tryParse(familyKey);
            label = familyId == null ? familyKey : title(familyId.getPath());
        }
        return Optional.of(new CollapsedFamily(familyKey, pluralize(label)));
    }

    private static boolean isColorOnlyMaterialGroup(ResourceLocation id, String colorBucket, String materialGroup) {
        ResourceLocation familyId = ResourceLocation.tryParse(materialGroup);
        if (familyId == null || !familyId.getNamespace().equals(id.getNamespace())) {
            return false;
        }
        return familyId.getPath().equals(colorBucket.toLowerCase(Locale.ROOT));
    }

    public static Optional<CollapsedFamily> classifyCompressedBlockFamily(ResourceLocation id) {
        if (id == null || !"compressedblocks".equals(id.getNamespace())) {
            return Optional.empty();
        }
        String path = id.getPath().toLowerCase(Locale.ROOT);
        if (!path.matches("c\\d+_.+")) {
            return Optional.empty();
        }
        String base = path.substring(path.indexOf('_') + 1);
        return Optional.of(new CollapsedFamily(
                id.getNamespace() + ":compressed/" + base,
                "Compressed " + title(base)
        ));
    }

    private static boolean hasCsvTagToken(String tags, String expected) {
        if (tags == null || tags.isBlank()) return false;
        for (String t : tags.split(",")) {
            if (expected.equals(t.trim().toLowerCase(Locale.ROOT))) return true;
        }
        return false;
    }

    private static String matchingColorStrippedTag(ResourceLocation id, String colorBucket, String tags) {
        if (tags == null || tags.isBlank()) {
            return "";
        }
        String strippedPath = stripColorToken(id.getPath(), colorBucket);
        if (strippedPath.equals(id.getPath())) {
            return "";
        }
        String expected = id.getNamespace() + ":" + strippedPath;
        String pluralExpected = expected.endsWith("s") ? expected : expected + "s";
        for (String rawTag : tags.split(",")) {
            String tag = rawTag.trim().toLowerCase(Locale.ROOT);
            if (tag.equals(expected) || tag.equals(pluralExpected)) {
                return tag;
            }
        }
        return "";
    }

    private static String stripColorToken(String path, String colorBucket) {
        String color = colorBucket.toLowerCase(Locale.ROOT);
        if (path.startsWith(color + "_")) {
            return path.substring(color.length() + 1);
        }
        if (path.endsWith("_" + color)) {
            return path.substring(0, path.length() - color.length() - 1);
        }
        return path;
    }

    private static String stripColorPrefixFromDisplayName(String displayName, String colorBucket) {
        if (displayName == null || displayName.isBlank()) {
            return "";
        }
        String colorWords = title(colorBucket);
        if (displayName.regionMatches(true, 0, colorWords, 0, colorWords.length())
                && displayName.length() > colorWords.length()
                && Character.isWhitespace(displayName.charAt(colorWords.length()))) {
            return displayName.substring(colorWords.length()).trim();
        }
        return displayName.trim();
    }

    private static String pluralize(String label) {
        if (label.isBlank() || label.endsWith("s")) return label;
        String lower = label.toLowerCase(Locale.ROOT);
        if (lower.endsWith("glass") || lower.endsWith("quartz") || lower.endsWith("wool")) {
            return label;
        }
        if (label.endsWith("y") && label.length() > 1) {
            char beforeY = Character.toLowerCase(label.charAt(label.length() - 2));
            if ("aeiou".indexOf(beforeY) < 0) {
                return label.substring(0, label.length() - 1) + "ies";
            }
        }
        return label + "s";
    }

    private static String tintableTagColor(String tags) {
        if (tags == null || tags.isBlank()) {
            return "";
        }
        for (String rawTag : tags.split(",")) {
            String tag = rawTag.trim().toLowerCase(Locale.ROOT);
            int tintable = tag.indexOf(":tintable/");
            if (tintable < 0) {
                continue;
            }
            String color = tag.substring(tintable + ":tintable/".length()).trim();
            if (!color.isBlank()) {
                return color;
            }
        }
        return "";
    }

    private static String title(String value) {
        String[] words = value.replace('_', ' ').split("\\s+");
        StringBuilder out = new StringBuilder(value.length());
        for (String word : words) {
            if (word.isEmpty()) continue;
            if (out.length() > 0) out.append(' ');
            out.append(Character.toUpperCase(word.charAt(0)));
            if (word.length() > 1) out.append(word.substring(1));
        }
        return out.toString();
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

    private static String stripVariantSuffix(String path) {
        if (path.endsWith("_connecting")) {
            return path.substring(0, path.length() - "_connecting".length());
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
                .filter(t -> t.contains("ingots/") || t.contains("gems/") || t.contains("planks/")
                        || t.contains("nuggets/") || t.contains("dusts/") || t.contains("ores/")
                        || t.contains("raw_materials/") || t.contains("storage_blocks/")
                        || t.contains("seeds/") || t.contains("crops/") || t.contains("pottery_sherds/"))
                .findFirst()
                .map(t -> {
                    if (t.contains("pottery_sherds/")) return "minecraft:pottery_sherd";
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

    public static String classifyTopologyRoot(ItemStack stack) {
        if (!topologyBuilt) buildTopology();
        Item item = stack.getItem();
        Item dsuRoot = TOPOLOGY_DSU_MAP.get(item);

        // Hybrid Fallback Pipeline:
        // 1. If connected in recipe graph -> use DSU root
        if (dsuRoot != null && dsuRoot != item) {
            ResourceLocation id = BuiltInRegistries.ITEM.getKey(dsuRoot);
            if (id != null) return id.toString();
        }

        // 2. Fall back to Material Root (Name/Tag heuristics)
        String matRoot = classifyMaterialRoot(stack);
        if (!isUnknownGroup(matRoot)) return matRoot;

        // 3. Fall back to Shape (to at least group all "Chests" or "Tools" together)
        return "shape:" + classifyShape(item);
    }

    public static String classifyPropertyRoot(SearchNode node) {
        String bucket = node.meta(SearchNodeKeys.COLOR_BUCKET, "");
        String category = node.meta(SearchNodeKeys.ONTOLOGY_CATEGORY, "misc");

        if (bucket.isEmpty()) return "prop:" + category;
        return "prop:" + bucket + "_" + category;
    }

    public static String classifyBehaviorRoot(SearchNode node) {
        if (hasMetadata(node, SearchNodeKeys.ENERGY_GENERATION)
                || hasMetadata(node, SearchNodeKeys.GREGTECH_EU_GENERATION)
                || hasMetadata(node, SearchNodeKeys.GREGTECH_EU_OUTPUT)
                || factHasResourceAction(node, ENERGY_TERMS, "generate", "generates", "generating")) {
            return "behavior:energy_generation";
        }
        if (hasMetadata(node, SearchNodeKeys.ENERGY_CAPACITY)
                || tokenAny(node, SearchNodeKeys.FACETS, "has_energy")
                || factHasResourceAction(node, ENERGY_TERMS, "store", "stores", "storage")) {
            return "behavior:energy_storage";
        }
        if (hasMetadata(node, SearchNodeKeys.ENERGY_CONSUMPTION)
                || hasMetadata(node, SearchNodeKeys.GREGTECH_EU_CONSUMPTION)
                || hasMetadata(node, SearchNodeKeys.GREGTECH_EU_INPUT)
                || factHasResourceAction(node, ENERGY_TERMS, "use", "uses", "consume", "consumes", "consumption")) {
            return "behavior:energy_usage";
        }
        if (factHasAnyComponent(node, "chemical", "gas", "slurry", "infusion", "pigment")) {
            return "behavior:chemical_handling";
        }
        if (hasMetadata(node, SearchNodeKeys.FLUID_CAPACITY) || tokenAny(node, SearchNodeKeys.FACETS, "fluid_container")) {
            return "behavior:fluid_storage";
        }
        if (factHasAnyComponent(node, FLUID_TERMS)) {
            return "behavior:fluid_transport";
        }
        if (hasConventionMetadata(node, "stressrole", "kineticrole")
                || factHasAnyComponent(node, "su", "stress", "kinetic")) {
            return "behavior:kinetic_power";
        }
        if (hasConventionMetadata(node, "roles")
                || factHasAnyComponent(node, "processing", "process")) {
            return "behavior:create_processing";
        }
        if (conventionHasAnyToken(node, "package", "packager", "logistics")
                || tokenAny(node, SearchNodeKeys.ONTOLOGY_SUBCATEGORY, "logistics")) {
            return "behavior:logistics";
        }
        if (conventionHasAnyToken(node, "network", "cable", "channel")) {
            return "behavior:network";
        }
        if (conventionHasAnyToken(node, "terminal", "terminals")) {
            return "behavior:terminal";
        }
        if (conventionHasAnyToken(node, "upgrade", "upgrades")
                || tokenAny(node, SearchNodeKeys.ONTOLOGY_SUBCATEGORY, "upgrades")) {
            return "behavior:upgrade";
        }
        if (conventionHasAnyToken(node, "backpack", "backpacks", "portable_storage", "portable")) {
            return "behavior:portable_storage";
        }
        if (hasMetadata(node, SearchNodeKeys.STORAGE_ITEM_KIND)
                || tokenAny(node, SearchNodeKeys.STORAGE_FACTS, "storage")) {
            return "behavior:storage";
        }
        if (tokenAny(node, SearchNodeKeys.FACETS, "machine", "interactive_block")
                || conventionHasAnyToken(node, "machine", "machines", "machine_part")
                || tokenAny(node, SearchNodeKeys.ONTOLOGY_SUBCATEGORY, "machines")) {
            return "behavior:machine";
        }
        if (tokenAny(node, SearchNodeKeys.FACETS, "harvest_tool", "utility_tool", "melee_weapon", "ranged_weapon")
                || tokenAny(node, SearchNodeKeys.ONTOLOGY_SUBCATEGORY, "tools")) {
            return "behavior:tool";
        }
        if (tokenAny(node, SearchNodeKeys.ONTOLOGY_SUBCATEGORY, "materials")
                || conventionHasAnyToken(node, "material", "materials")) {
            return "behavior:material";
        }
        return "";
    }

    public static String classifySimilarityRoot(ItemStack stack) {
        if (!similarityBuilt) buildSimilarity();
        Item item = stack.getItem();
        String root = SIMILARITY_CACHE.get(item);
        if (root != null) return root;
        ResourceLocation id = BuiltInRegistries.ITEM.getKey(item);
        return id != null ? id.toString() : "";
    }

    public static String classifyPropertyRoot(ItemStack stack) {
        if (!propertyBuilt) buildProperties();
        Item item = stack.getItem();
        String root = PROPERTY_CACHE.get(item);
        if (root != null) return root;
        ResourceLocation id = BuiltInRegistries.ITEM.getKey(item);
        return id != null ? id.toString() : "";
    }

    private static int getTopologyPriority(Item item) {
        ResourceLocation id = BuiltInRegistries.ITEM.getKey(item);
        if (id == null) return 0;
        if (!id.getNamespace().equals("minecraft")) return 0;
        String path = id.getPath();
        return switch (path) {
            case "cobblestone" -> 1000;
            case "stone" -> 950;
            case "dirt" -> 900;
            case "oak_log", "spruce_log", "birch_log", "jungle_log", "acacia_log", "dark_oak_log", "mangrove_log",
                 "cherry_log", "crimson_stem", "warped_stem" -> 850;
            case "sand" -> 800;
            case "gravel" -> 750;
            case "iron_ore", "deepslate_iron_ore", "raw_iron" -> 700;
            case "copper_ore", "deepslate_copper_ore", "raw_copper" -> 650;
            case "gold_ore", "deepslate_gold_ore", "raw_gold" -> 600;
            case "diamond_ore", "deepslate_diamond_ore" -> 550;
            case "coal_ore", "deepslate_coal_ore" -> 500;
            default -> {
                if (path.contains("netherite")) yield 2000;
                if (path.contains("_ingot")) yield 1500;
                if (path.contains("_gem") || path.contains("diamond") || path.contains("emerald")) yield 1400;
                if (path.contains("_log")) yield 840;
                if (path.contains("_ore")) yield 400;
                if (path.contains("raw_")) yield 350;
                yield 0;
            }
        };
    }

    private static void buildTopology() {
        TOPOLOGY_DSU_MAP.clear();
        AmiRecipeIndex index = AmiRecipeIndex.getInstance();
        if (!index.isBuilt()) return;

        Set<Item> allItems = new HashSet<>();
        BuiltInRegistries.ITEM.forEach(allItems::add);

        // Disjoint Set Union (DSU) using Item as keys
        Map<Item, Item> parent = new HashMap<>();
        java.util.function.Function<Item, Item> find = new java.util.function.Function<>() {
            @Override
            public Item apply(Item i) {
                Item p = parent.getOrDefault(i, i);
                if (p == i) return i;
                Item root = apply(p);
                parent.put(i, root);
                return root;
            }
        };

        java.util.function.BiConsumer<Item, Item> union = (a, b) -> {
            Item rootA = find.apply(a);
            Item rootB = find.apply(b);
            if (rootA != rootB) {
                if (getTopologyPriority(rootA) >= getTopologyPriority(rootB)) {
                    parent.put(rootB, rootA);
                } else {
                    parent.put(rootA, rootB);
                }
            }
        };

        Set<Item> blacklist = new HashSet<>(Arrays.asList(
                Items.STICK, Items.COAL, Items.CHARCOAL, Items.GUNPOWDER, Items.STRING, Items.FEATHER,
                Items.BONE_MEAL, Items.REDSTONE, Items.GLOWSTONE_DUST, Items.LAPIS_LAZULI, Items.QUARTZ,
                Items.BUCKET, Items.WATER_BUCKET, Items.LAVA_BUCKET, Items.BOWL, Items.GLASS_BOTTLE,
                Items.PAPER, Items.SUGAR, Items.LEATHER, Items.EGG, Items.WHEAT, Items.WHEAT_SEEDS,
                Items.CLAY_BALL, Items.BRICK, Items.NETHER_BRICK, Items.FLINT, Items.BLAZE_ROD,
                Items.ENCHANTED_BOOK, Items.BOOK, Items.WRITABLE_BOOK, Items.WRITTEN_BOOK,
                Items.STRIPPED_CRIMSON_HYPHAE, Items.STRIPPED_WARPED_HYPHAE, Items.DRIED_KELP, Items.DRIED_KELP_BLOCK,
                Items.NETHERITE_UPGRADE_SMITHING_TEMPLATE, Items.NETHER_STAR, Items.DRAGON_BREATH,
                Items.IRON_NUGGET, Items.GOLD_NUGGET, Items.COPPER_INGOT, Items.IRON_INGOT, Items.GOLD_INGOT,
                Items.PUMPKIN, Items.CARVED_PUMPKIN, Items.JACK_O_LANTERN, Items.WHITE_DYE, Items.ORANGE_DYE,
                Items.MAGENTA_DYE, Items.LIGHT_BLUE_DYE, Items.YELLOW_DYE, Items.LIME_DYE, Items.PINK_DYE,
                Items.GRAY_DYE, Items.LIGHT_GRAY_DYE, Items.CYAN_DYE, Items.PURPLE_DYE, Items.BLUE_DYE,
                Items.BROWN_DYE, Items.GREEN_DYE, Items.RED_DYE, Items.BLACK_DYE,
                Items.SAND, Items.RED_SAND, Items.GRAVEL, Items.DIRT, Items.COARSE_DIRT, Items.ROOTED_DIRT, Items.MUD,
                Items.GLASS, Items.GLASS_PANE, Items.WHITE_STAINED_GLASS, Items.ORANGE_STAINED_GLASS,
                Items.MAGENTA_STAINED_GLASS, Items.LIGHT_BLUE_STAINED_GLASS, Items.YELLOW_STAINED_GLASS,
                Items.LIME_STAINED_GLASS, Items.PINK_STAINED_GLASS, Items.GRAY_STAINED_GLASS,
                Items.LIGHT_GRAY_STAINED_GLASS, Items.CYAN_STAINED_GLASS, Items.PURPLE_STAINED_GLASS,
                Items.BLUE_STAINED_GLASS, Items.BROWN_STAINED_GLASS, Items.GREEN_STAINED_GLASS,
                Items.RED_STAINED_GLASS, Items.BLACK_STAINED_GLASS,
                Items.POTATO, Items.BAKED_POTATO, Items.POISONOUS_POTATO, Items.CARROT, Items.GOLDEN_CARROT,
                Items.WHEAT, Items.WHEAT_SEEDS, Items.MELON_SLICE, Items.GLISTERING_MELON_SLICE, Items.APPLE, Items.GOLDEN_APPLE,
                Items.ENCHANTED_GOLDEN_APPLE, Items.SWEET_BERRIES, Items.GLOW_BERRIES, Items.CHORUS_FRUIT,
                Items.BREAD, Items.EGG, Items.SUGAR, Items.HONEY_BOTTLE, Items.MILK_BUCKET,
                Items.BEEF, Items.COOKED_BEEF, Items.PORKCHOP, Items.COOKED_PORKCHOP, Items.MUTTON, Items.COOKED_MUTTON,
                Items.CHICKEN, Items.COOKED_CHICKEN, Items.RABBIT, Items.COOKED_RABBIT, Items.SALMON, Items.COOKED_SALMON,
                Items.COD, Items.COOKED_COD, Items.PUFFERFISH, Items.TROPICAL_FISH,
                Items.KELP, Items.DRIED_KELP, Items.DRIED_KELP_BLOCK, Items.BAMBOO, Items.SUGAR_CANE,
                Items.CACTUS, Items.VINE, Items.GLOW_LICHEN, Items.MOSS_BLOCK, Items.MOSS_CARPET,
                Items.POPPY, Items.DANDELION, Items.BLUE_ORCHID, Items.ALLIUM, Items.AZURE_BLUET,
                Items.RED_TULIP, Items.ORANGE_TULIP, Items.WHITE_TULIP, Items.PINK_TULIP, Items.OXEYE_DAISY,
                Items.CORNFLOWER, Items.LILY_OF_THE_VALLEY, Items.WITHER_ROSE, Items.TORCHFLOWER,
                Items.SUNFLOWER, Items.LILAC, Items.ROSE_BUSH, Items.PEONY, Items.PITCHER_PLANT,
                Items.COCOA_BEANS, Items.INK_SAC, Items.GLOW_INK_SAC, Items.LAPIS_LAZULI, Items.BONE_MEAL,
                Items.BONE, Items.GUNPOWDER, Items.GLOWSTONE_DUST, Items.REDSTONE, Items.QUARTZ,
                Items.AMETHYST_SHARD, Items.NETHERITE_SCRAP, Items.NETHERITE_INGOT, Items.NETHER_STAR,
                Items.DRAGON_BREATH, Items.CHORUS_FRUIT, Items.POPPED_CHORUS_FRUIT,
                Items.BLAZE_ROD, Items.BLAZE_POWDER, Items.GHAST_TEAR, Items.MAGMA_CREAM,
                Items.SLIME_BALL, Items.SLIME_BLOCK, Items.SPIDER_EYE, Items.FERMENTED_SPIDER_EYE,
                Items.RABBIT_FOOT, Items.RABBIT_HIDE, Items.LEATHER, Items.FEATHER, Items.STRING,
                Items.FLINT, Items.CLAY_BALL, Items.BRICK, Items.NETHER_BRICK, Items.PRISMARINE_SHARD,
                Items.PRISMARINE_CRYSTALS, turtleScuteItem(), // Items.ARMADILLO_SCUTE,
                Items.HONEYCOMB, Items.HONEYCOMB_BLOCK, Items.HONEY_BOTTLE, Items.HONEY_BLOCK,
                Items.SNOWBALL, Items.SNOW_BLOCK, Items.ICE, Items.PACKED_ICE, Items.BLUE_ICE
        ));

        // Blacklist colors and common patterns
        for (Item item : allItems) {
            ResourceLocation id = BuiltInRegistries.ITEM.getKey(item);
            if (id == null) continue;
            String path = id.getPath();
            if (path.contains("dye") || path.contains("smithing_template") || path.contains("pattern")) {
                blacklist.add(item);
            }
        }

        // Pass 1: Strong Material Anchors (Smelting, Blasting, 9-to-1)
        for (Item item : allItems) {
            for (var holder : index.getRecipesFor(new ItemStack(item))) {
                var ingredients = holder.value().getIngredients();
                if (ingredients.size() == 1) {
                    for (ItemStack is : ingredients.get(0).getItems()) {
                        if (!is.isEmpty() && !blacklist.contains(is.getItem()) && !blacklist.contains(item)) {
                            union.accept(is.getItem(), item);
                        }
                    }
                }
            }
        }

        // Pass 2: Recursive Evacuation
        // Items that are still "Misc" (roots of themselves) try to find a parent among their ingredients
        boolean changed = true;
        int maxPasses = 3;
        while (changed && maxPasses-- > 0) {
            changed = false;
            for (Item item : allItems) {
                if (find.apply(item) == item) { // Still a root
                    Map<Item, Integer> candidates = new HashMap<>();
                    for (var holder : index.getRecipesFor(new ItemStack(item))) {
                        for (var ing : holder.value().getIngredients()) {
                            for (ItemStack is : ing.getItems()) {
                                if (!is.isEmpty()) {
                                    Item root = find.apply(is.getItem());
                                    if (!blacklist.contains(root)) {
                                        candidates.merge(root, 1, Integer::sum);
                                    }
                                }
                            }
                        }
                    }
                    if (!candidates.isEmpty()) {
                        Item best = candidates.entrySet().stream()
                                .max(Comparator.comparingInt(Map.Entry::getValue))
                                .get().getKey();
                        union.accept(best, item);
                        changed = true;
                    }
                }
            }
        }

        // Final Pass: Lexical Affinity (Last resort for items with no recipes)
        for (Item item : allItems) {
            if (find.apply(item) == item) {
                String mat = classifyMaterialRoot(new ItemStack(item));
                if (!isUnknownGroup(mat)) {
                    ResourceLocation matLoc = ResourceLocation.tryParse(mat);
                    if (matLoc != null) {
                        Item matItem = BuiltInRegistries.ITEM.get(matLoc);
                        if (matItem != Items.AIR) {
                            union.accept(matItem, item);
                        }
                    }
                }
            }
        }

        // Map DSU results
        for (Item item : allItems) {
            TOPOLOGY_DSU_MAP.put(item, find.apply(item));
        }
        topologyBuilt = true;
    }

    private static void buildSimilarity() {
        SIMILARITY_CACHE.clear();
        Map<Item, Set<ResourceLocation>> itemTags = new HashMap<>();
        for (Item item : BuiltInRegistries.ITEM) {
            Set<ResourceLocation> tags = new HashSet<>();
            item.getDefaultInstance().getTags().forEach(t -> tags.add(t.location()));
            if (!tags.isEmpty()) itemTags.put(item, tags);
        }

        Map<Item, Item> parent = new HashMap<>();
        java.util.function.Function<Item, Item> find = new java.util.function.Function<>() {
            @Override
            public Item apply(Item i) {
                Item p = parent.getOrDefault(i, i);
                if (p == i) return i;
                Item r = apply(p);
                parent.put(i, r);
                return r;
            }
        };

        List<Item> items = new ArrayList<>(itemTags.keySet());
        for (int i = 0; i < items.size(); i++) {
            Item a = items.get(i);
            Set<ResourceLocation> tagsA = itemTags.get(a);
            for (int j = i + 1; j < items.size(); j++) {
                Item b = items.get(j);
                Set<ResourceLocation> tagsB = itemTags.get(b);

                // Jaccard Similarity: Intersection / Union
                int intersection = 0;
                for (ResourceLocation t : tagsA) if (tagsB.contains(t)) intersection++;
                double similarity = (double) intersection / (tagsA.size() + tagsB.size() - intersection);

                if (similarity >= 0.85) { // High threshold for "basically the same item"
                    Item rootA = find.apply(a);
                    Item rootB = find.apply(b);
                    if (rootA != rootB) parent.put(rootA, rootB);
                }
            }
        }

        for (Item item : BuiltInRegistries.ITEM) {
            Item root = find.apply(item);
            SIMILARITY_CACHE.put(item, BuiltInRegistries.ITEM.getKey(root).toString());
        }
        similarityBuilt = true;
    }

    @SuppressWarnings("deprecation")
    private static void buildProperties() {
        PROPERTY_CACHE.clear();
        for (Item item : BuiltInRegistries.ITEM) {
            PROPERTY_CACHE.put(item, BuiltInRegistries.ITEM.getKey(item).toString());
        }
        propertyBuilt = true;
    }

    public static void resetTopology() {
        topologyBuilt = false;
        TOPOLOGY_CACHE.clear();
        similarityBuilt = false;
        SIMILARITY_CACHE.clear();
        propertyBuilt = false;
        PROPERTY_CACHE.clear();
    }

    public static boolean isUnknownGroup(String key) {
        return UNKNOWN_GROUP_MARKERS.contains(key) || key.toLowerCase(Locale.ROOT).contains("unknown");
    }

    private static boolean hasMetadata(SearchNode node, String key) {
        return !node.meta(key, "").isBlank();
    }

    private static boolean tokenAny(SearchNode node, String metadataKey, String... tokens) {
        String raw = node.meta(metadataKey, "");
        if (raw.isBlank()) {
            return false;
        }
        Set<String> present = new HashSet<>();
        for (String part : raw.split("[,\\s]+")) {
            String normalized = normalizeMetadataToken(part);
            if (!normalized.isBlank()) {
                present.add(normalized);
            }
        }
        for (String token : tokens) {
            if (present.contains(normalizeMetadataToken(token))) {
                return true;
            }
        }
        return false;
    }

    private static boolean hasConventionMetadata(SearchNode node, String... normalizedKeySuffixes) {
        for (String key : node.metadata().keySet()) {
            String normalizedKey = normalizeMetadataToken(key);
            for (String suffix : normalizedKeySuffixes) {
                if (normalizedKey.endsWith(normalizeMetadataToken(suffix))
                        && !node.meta(key, "").isBlank()) {
                    return true;
                }
            }
        }
        return false;
    }

    private static boolean conventionHasAnyToken(SearchNode node, String... tokens) {
        for (var entry : node.metadata().entrySet()) {
            if (!isConventionBehaviorKey(entry.getKey())) {
                continue;
            }
            for (String token : splitMetadataTokens(entry.getValue())) {
                Set<String> parts = metadataTokenParts(token);
                for (String candidate : tokens) {
                    if (parts.contains(normalizeMetadataToken(candidate))) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    private static boolean factHasAnyComponent(SearchNode node, String... concepts) {
        for (String token : conventionFactTokens(node)) {
            Set<String> parts = metadataTokenParts(token);
            for (String concept : concepts) {
                if (parts.contains(normalizeMetadataToken(concept))) {
                    return true;
                }
            }
        }
        return false;
    }

    private static boolean factHasResourceAction(SearchNode node, String[] resourceTerms, String... actionTerms) {
        for (String token : conventionFactTokens(node)) {
            Set<String> parts = metadataTokenParts(token);
            boolean hasResource = false;
            boolean hasAction = false;
            for (String resourceTerm : resourceTerms) {
                if (parts.contains(normalizeMetadataToken(resourceTerm))) {
                    hasResource = true;
                    break;
                }
            }
            for (String actionTerm : actionTerms) {
                if (parts.contains(normalizeMetadataToken(actionTerm))) {
                    hasAction = true;
                    break;
                }
            }
            if (hasResource && hasAction) {
                return true;
            }
        }
        return false;
    }

    private static List<String> conventionFactTokens(SearchNode node) {
        List<String> tokens = new ArrayList<>();
        for (var entry : node.metadata().entrySet()) {
            String normalizedKey = normalizeMetadataToken(entry.getKey());
            if (normalizedKey.endsWith("facts") || SearchNodeKeys.SEARCH_TOKENS.equals(entry.getKey())) {
                tokens.addAll(splitMetadataTokens(entry.getValue()));
            }
        }
        return tokens;
    }

    private static boolean isConventionBehaviorKey(String key) {
        String normalizedKey = normalizeMetadataToken(key);
        return normalizedKey.endsWith("facts")
                || normalizedKey.endsWith("itemkind")
                || normalizedKey.endsWith("role")
                || normalizedKey.endsWith("roles")
                || SearchNodeKeys.FACETS.equals(key)
                || SearchNodeKeys.COMPONENT_FACTS.equals(key)
                || SearchNodeKeys.SEARCH_TOKENS.equals(key);
    }

    private static List<String> splitMetadataTokens(String raw) {
        if (raw == null || raw.isBlank()) {
            return List.of();
        }
        List<String> tokens = new ArrayList<>();
        for (String part : raw.split("[,\\s]+")) {
            if (!part.isBlank()) {
                tokens.add(part.trim());
            }
        }
        return tokens;
    }

    private static Set<String> metadataTokenParts(String token) {
        Set<String> parts = new HashSet<>();
        String normalized = normalizeMetadataToken(token);
        if (!normalized.isBlank()) {
            parts.add(normalized);
        }
        for (String part : token.split("[_\\-:/]+")) {
            String normalizedPart = normalizeMetadataToken(part);
            if (!normalizedPart.isBlank()) {
                parts.add(normalizedPart);
            }
        }
        return parts;
    }

    private static String normalizeMetadataToken(String value) {
        return value == null ? "" : value.toLowerCase(Locale.ROOT).replace("_", "").replace("-", "").trim();
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
        return TagKey.create(Registries.ITEM, Services.PLATFORM.rl(namespace, path));
    }

    private static Item turtleScuteItem() {
        return BuiltInRegistries.ITEM.getOptional(Services.PLATFORM.rl("minecraft", "turtle_scute"))
                .or(() -> BuiltInRegistries.ITEM.getOptional(Services.PLATFORM.rl("minecraft", "scute")))
                .orElse(Items.AIR);
    }

    public enum GroupType {SHAPE, COLOR, MATERIAL}

    public record CollapsedFamily(String key, String label) {
    }
}
