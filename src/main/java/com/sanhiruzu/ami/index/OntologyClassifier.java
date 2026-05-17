package com.sanhiruzu.ami.index;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

import java.util.Locale;
import java.util.stream.Collectors;

/**
 * Classifies Item instances into AmiOntology categories at index time.
 *
 * Returns a two-element array {categoryId, subcategoryId}, or null when the item
 * should be left for the runtime heuristics in AmiOntology.classifyItem().
 *
 * BlockItem decision order (first match wins):
 *   1. Redstone  – checked before nature so sculk sensors win
 *   2. Nature    – logs, leaves, saplings, flowers, crops, fungi, etc. → null (runtime → "nature")
 *   3. Ores      – all ore block types → null (runtime → "tech")
 *   4. Tech storage – compressed-material blocks → null (runtime → "tech")
 *   5. Functional – beds, doors, block entities, crafting stations, etc.
 *   6. Decorative – carpets, candles, banners, signs, torches, etc.
 *   7. Building   – everything else that is a BlockItem
 *
 * Non-BlockItem decision order uses item tags + path heuristics:
 *   Armor → Weapons/Tools → Magic → Food → Tech → Nature → Social → Entities
 */
public final class OntologyClassifier {

    /**
     * @return {categoryId, subcategoryId} or null to use runtime classification.
     */
    @Nullable
    public static String[] classifyItem(Item item, ResourceLocation id) {
        if (!(item instanceof BlockItem bi)) {
            return classifyNonBlockItem(item, id);
        }

        Block block = bi.getBlock();
        BlockState state = block.defaultBlockState();
        String path = id.getPath();

        // ── 1. Redstone ───────────────────────────────────────────────────────
        if (state.is(BlockTags.BUTTONS)
                || state.is(BlockTags.PRESSURE_PLATES)
                || state.is(BlockTags.RAILS)) {
            return blocks("redstone");
        }
        if (isRedstoneBlock(path)) return blocks("redstone");

        // ── 2. Nature → delegate to runtime ("nature") ───────────────────────
        if (state.is(BlockTags.LOGS)
                || state.is(BlockTags.BAMBOO_BLOCKS)
                || state.is(BlockTags.LEAVES)
                || state.is(BlockTags.SAPLINGS)
                || state.is(BlockTags.FLOWERS)
                || state.is(BlockTags.CROPS)
                || state.is(BlockTags.CORAL_BLOCKS)
                || state.is(BlockTags.CORALS)
                || state.is(BlockTags.CORAL_PLANTS)
                || state.is(BlockTags.NYLIUM)) {
            return null;
        }
        if (isNaturePath(path)) return null;

        // ── 3. Ores → delegate to runtime ("tech") ───────────────────────────
        if (state.is(BlockTags.GOLD_ORES)
                || state.is(BlockTags.IRON_ORES)
                || state.is(BlockTags.DIAMOND_ORES)
                || state.is(BlockTags.REDSTONE_ORES)
                || state.is(BlockTags.LAPIS_ORES)
                || state.is(BlockTags.COAL_ORES)
                || state.is(BlockTags.EMERALD_ORES)
                || state.is(BlockTags.COPPER_ORES)
                || path.contains("_ore")   // catches nether_quartz_ore + any mod ore
                || path.equals("ancient_debris")) {
            return null;
        }

        // ── 4. Tech storage blocks → delegate to runtime ("tech") ────────────
        if (isMaterialStorageBlock(path)) return null;

        // ── 5. Functional ─────────────────────────────────────────────────────
        if (state.is(BlockTags.BEDS)
                || state.is(BlockTags.DOORS)
                || state.is(BlockTags.TRAPDOORS)
                || state.is(BlockTags.FENCE_GATES)
                || state.is(BlockTags.SHULKER_BOXES)
                || state.is(BlockTags.CAMPFIRES)
                || state.is(BlockTags.CAULDRONS)
                || state.is(BlockTags.ANVIL)
                || state.is(BlockTags.BEEHIVES)) {
            return blocks("functional");
        }
        if (block instanceof BaseEntityBlock) return blocks("functional");
        if (isFunctionalBlock(path)) return blocks("functional");

        // ── 6. Decorative ─────────────────────────────────────────────────────
        if (state.is(BlockTags.WOOL_CARPETS)
                || state.is(BlockTags.CANDLES)
                || state.is(BlockTags.BANNERS)
                || state.is(BlockTags.ALL_SIGNS)
                || state.is(BlockTags.FLOWER_POTS)) {
            return blocks("decorative");
        }
        if (isDecorativeBlock(path)) return blocks("decorative");

        // ── 7. Building — split by shape (subcategory) and material (3rd element) ──
        return blocks(classifyBlockShape(path), classifyBlockMaterial(state, path));
    }

    // ── Non-BlockItem classification ──────────────────────────────────────────

    @Nullable
    private static String[] classifyNonBlockItem(Item item, ResourceLocation id) {
        String path = id.getPath();
        String tags = item.builtInRegistryHolder().tags()
                .map(tag -> tag.location().toString().toLowerCase(Locale.ROOT))
                .collect(Collectors.joining(","));
        // Single combined string so each pattern is checked against both at once.
        String combined = tags + "," + path;

        // ── Armor (check slot-specific tags first for precision) ──────────────
        if (combined.contains("head_armor") || path.endsWith("_helmet") || path.equals("turtle_helmet")) {
            return armor("head");
        }
        if (combined.contains("chest_armor") || path.endsWith("_chestplate")) {
            return armor("chest");
        }
        if (combined.contains("leg_armor") || path.endsWith("_leggings")) {
            return armor("legs");
        }
        if (combined.contains("foot_armor") || path.endsWith("_boots")) {
            return armor("feet");
        }
        if (path.equals("elytra")) return armor("curios");
        if (combined.contains(":armor")) return armor("chest"); // generic armor fallback

        // ── Weapons & Tools ───────────────────────────────────────────────────
        if (combined.contains(":swords") || path.endsWith("_sword")
                || path.equals("trident") || path.equals("mace")) {
            return weapons("melee");
        }
        if (combined.contains(":bows") || path.equals("bow") || path.equals("crossbow")) {
            return weapons("ranged");
        }
        if (combined.contains(":arrows") || path.endsWith("_arrow") || path.equals("arrow")) {
            return weapons("ranged");
        }
        if (combined.contains(":pickaxes") || path.endsWith("_pickaxe")) return weapons("harvest");
        if (combined.contains(":shovels")  || path.endsWith("_shovel"))  return weapons("harvest");
        if (combined.contains(":hoes")     || path.endsWith("_hoe"))     return weapons("harvest");
        if (combined.contains(":axes")     || path.endsWith("_axe"))     return weapons("harvest");
        if (path.equals("fishing_rod") || path.equals("shears")
                || path.equals("flint_and_steel") || path.equals("brush")) {
            return weapons("utility");
        }
        if (combined.contains(":tools")) return weapons("utility"); // generic tool fallback

        // ── Magic ─────────────────────────────────────────────────────────────
        if (path.contains("potion")) return magic("potions");
        if (path.equals("enchanted_book")) return magic("books");
        if (path.equals("totem_of_undying") || path.equals("nether_star")) return magic("artifacts");
        if (isMagicReagent(path)) return magic("reagents");

        // ── Food ──────────────────────────────────────────────────────────────
        if (path.startsWith("cooked_")) return food("meals");
        if (isRawProtein(path)) return food("proteins");
        if (path.equals("cod") || path.equals("salmon")
                || path.equals("tropical_fish") || path.equals("pufferfish")) {
            return food("proteins");
        }
        if (path.equals("mushroom_stew") || path.equals("beetroot_soup") || path.equals("rabbit_stew")
                || path.equals("suspicious_stew") || path.equals("honey_bottle")
                || path.equals("milk_bucket")) {
            return food("drinks");
        }
        if (combined.contains(":foods") || combined.contains(":food")) return food("snacks");

        // ── Tech (non-block materials) ────────────────────────────────────────
        if (combined.contains(":ingots") || path.endsWith("_ingot")) return tech("ingots");
        if (combined.contains(":gems")
                || path.equals("diamond") || path.equals("emerald") || path.equals("amethyst_shard")
                || path.equals("quartz") || path.equals("lapis_lazuli")
                || path.equals("prismarine_crystals")) {
            return tech("ingots");
        }
        if (combined.contains(":nuggets") || path.endsWith("_nugget")) return tech("ingots");
        if (isRawOre(path)) return tech("ingots");
        if (combined.contains(":dusts") || path.equals("redstone") || path.equals("glowstone_dust")
                || path.equals("blaze_rod") || path.equals("coal") || path.equals("charcoal")
                || path.equals("nether_brick") || path.equals("bone_meal")) {
            return tech("dusts");
        }

        // ── Nature (non-block) ────────────────────────────────────────────────
        if (combined.contains(":seeds") || path.endsWith("_seeds")) return nature("seeds");
        if (isHarvestCrop(path)) return nature("crops");
        if (path.equals("red_mushroom") || path.equals("brown_mushroom")) return nature("fungi");
        if (path.equals("bamboo") || path.equals("stick")) return nature("wood");

        // ── Navigation ───────────────────────────────────────────────────────
        if (path.equals("compass") || path.equals("recovery_compass")
                || path.equals("filled_map") || path.equals("clock") || path.equals("spyglass")) {
            return navigation("instruments");
        }

        // ── Social ───────────────────────────────────────────────────────────
        if (path.equals("player_head")) return social("players");
        if (path.equals("lodestone"))   return social("claims");

        // ── Entities (spawn eggs only) ────────────────────────────────────────
        if (path.endsWith("_spawn_egg")) return entities(classifySpawnEgg(path));

        // ── Environment (transport items) ──────────────────────────────────────
        if (path.contains("minecart") || path.endsWith("_boat") || path.endsWith("_chest_boat")) {
            return environment("transport");
        }

        return null; // leave remaining items to AmiOntology runtime heuristics → BLOCKS
    }

    // ── Non-BlockItem helpers ─────────────────────────────────────────────────

    private static boolean isMagicReagent(String path) {
        return switch (path) {
            case "blaze_powder", "ender_pearl", "ender_eye", "fermented_spider_eye",
                 "ghast_tear", "phantom_membrane", "rabbit_foot", "spider_eye",
                 "glistering_melon_slice", "dragon_breath", "experience_bottle",
                 "magma_cream", "nether_wart", "slime_ball" -> true;
            default -> false;
        };
    }

    private static boolean isRawProtein(String path) {
        return switch (path) {
            case "raw_beef", "raw_chicken", "raw_porkchop",
                 "raw_cod", "raw_salmon", "raw_rabbit", "raw_mutton" -> true;
            default -> false;
        };
    }

    private static boolean isRawOre(String path) {
        return switch (path) {
            case "raw_iron", "raw_gold", "raw_copper" -> true;
            default -> false;
        };
    }

    private static boolean isHarvestCrop(String path) {
        return switch (path) {
            case "wheat", "carrot", "potato", "beetroot",
                 "melon_slice", "pumpkin", "chorus_fruit",
                 "cocoa_beans", "sweet_berries", "glow_berries" -> true;
            default -> false;
        };
    }

    private static String classifySpawnEgg(String path) {
        // Strip the "_spawn_egg" suffix to get the mob name prefix.
        String mob = path.substring(0, path.length() - "_spawn_egg".length());
        return switch (mob) {
            // Passive
            case "pig", "chicken", "cow", "sheep", "horse", "donkey", "mule",
                 "rabbit", "squid", "glow_squid", "bat", "ocelot", "cat",
                 "axolotl", "frog", "tadpole", "parrot", "mooshroom",
                 "strider", "cod", "salmon", "tropical_fish", "pufferfish",
                 "turtle", "sniffer", "allay" -> "passive";
            // Neutral
            case "wolf", "bee", "polar_bear", "dolphin", "panda",
                 "llama", "trader_llama", "goat", "iron_golem",
                 "piglin", "zombified_piglin", "enderman",
                 "spider", "cave_spider" -> "neutral";
            // Default: hostile (covers zombies, skeletons, creepers, most modded mobs)
            default -> "hostile";
        };
    }

    // ── Category return helpers ───────────────────────────────────────────────

    private static String[] armor(String sub)      { return new String[]{"armor",       sub}; }
    private static String[] weapons(String sub)    { return new String[]{"weapons",     sub}; }
    private static String[] magic(String sub)      { return new String[]{"magic",       sub}; }
    private static String[] food(String sub)       { return new String[]{"food",        sub}; }
    private static String[] tech(String sub)       { return new String[]{"tech",        sub}; }
    private static String[] nature(String sub)     { return new String[]{"nature",      sub}; }
    private static String[] social(String sub)     { return new String[]{"social",      sub}; }
    private static String[] navigation(String sub) { return new String[]{"navigation",  sub}; }
    private static String[] entities(String sub)   { return new String[]{"entities",    sub}; }
    private static String[] environment(String sub) { return new String[]{"environment", sub}; }
    /** Two-element return for non-building block subcategories (functional/redstone/decorative). */
    private static String[] blocks(String sub)   { return new String[]{"blocks",   sub}; }
    /** Three-element return for building blocks: [category, shapeSubcategory, materialSubcategory]. */
    private static String[] blocks(String shape, String material) { return new String[]{"blocks", shape, material}; }

    // ── Building block shape & material classifiers ───────────────────────────

    private static String classifyBlockShape(String path) {
        if (path.endsWith("_stairs"))     return "stairs";
        if (path.endsWith("_slab"))       return "slab";
        if (path.endsWith("_wall"))       return "wall";
        if (path.endsWith("_fence_gate")) return "fence";
        if (path.endsWith("_fence"))      return "fence";
        if (path.endsWith("_pane"))       return "pane";
        return "full_block";
    }

    private static String classifyBlockMaterial(BlockState state, String path) {
        // Glass — check first, "glass" is unambiguous
        if (path.contains("glass")) return "glass";

        // Wood — use tags for precision across all wood types (vanilla + mods)
        if (state.is(BlockTags.PLANKS)
                || state.is(BlockTags.WOODEN_SLABS)
                || state.is(BlockTags.WOODEN_STAIRS)
                || state.is(BlockTags.WOODEN_FENCES)
                || path.endsWith("_wood")
                || path.equals("bamboo_block")
                || path.equals("bamboo_mosaic")) {
            return "wood";
        }

        // Soil & Terrain — check concrete_powder before concrete to avoid mismatch
        if (path.contains("concrete_powder")
                || containsAny(path, "dirt", "gravel", "clay", "mud", "podzol", "mycelium",
                               "snow", "ice", "terracotta", "soul_sand", "soul_soil")
                || path.equals("sand") || path.endsWith("_sand")) {
            return "soil";
        }

        // Stone & Masonry — broad catch for stone-like and mineral building materials
        if (state.is(BlockTags.STONE_BRICKS)
                || containsAny(path, "stone", "cobble", "brick", "sandstone",
                               "andesite", "diorite", "granite", "deepslate", "tuff",
                               "basalt", "calcite", "dripstone", "blackstone",
                               "netherrack", "end_stone", "obsidian",
                               "prismarine", "purpur", "magma", "concrete")) {
            return "stone";
        }

        return "other_building";
    }

    private static boolean containsAny(String path, String... tokens) {
        for (String token : tokens) {
            if (path.contains(token)) return true;
        }
        return false;
    }

    // ── BlockItem helper predicates ───────────────────────────────────────────

    private static boolean isRedstoneBlock(String path) {
        return switch (path) {
            case "redstone", "redstone_torch", "redstone_lamp",
                 "repeater", "comparator", "observer",
                 "piston", "sticky_piston",
                 "lever", "daylight_detector", "target", "tnt",
                 "tripwire_hook", "slime_block", "honey_block",
                 "sculk_sensor", "calibrated_sculk_sensor" -> true;
            default -> path.contains("copper_bulb"); // all oxidized / waxed copper bulb variants
        };
    }

    private static boolean isNaturePath(String path) {
        return path.contains("mushroom")           // red/brown mushroom + mushroom_block types
               || path.equals("bamboo")            // the item, not bamboo_block (covered by tag)
               || path.contains("kelp")
               || path.contains("seagrass")
               || path.contains("sugar_cane")
               || path.equals("cactus")
               || path.contains("vine")            // vine, twisting_vines, weeping_vines
               || path.equals("lily_pad")
               || path.contains("nylium")          // covered by NYLIUM tag too; belt-and-suspenders
               || path.contains("wart_block")      // nether_wart_block, warped_wart_block
               || path.equals("shroomlight")
               || path.contains("sculk")           // sculk, sculk_vein, sculk_catalyst, sculk_shrieker
                                                   // (sensors caught by isRedstoneBlock earlier)
               || path.contains("chorus_plant") || path.contains("chorus_flower")
               || path.contains("azalea")          // azalea, flowering_azalea + leaf variants
               || path.contains("dripleaf")
               || path.contains("hanging_roots")
               || path.contains("rooted_dirt")
               || path.contains("pink_petals")
               || path.contains("pitcher")         // pitcher_plant, pitcher_crop
               || path.contains("torchflower")
               || path.equals("spore_blossom")
               || (path.contains("grass")          // short_grass, tall_grass — but NOT grass_block
                   && !path.equals("grass_block") && !path.contains("path"));
    }

    private static boolean isMaterialStorageBlock(String path) {
        return switch (path) {
            case "iron_block", "gold_block", "diamond_block", "emerald_block",
                 "coal_block", "copper_block", "netherite_block", "lapis_block",
                 "redstone_block", "amethyst_block",
                 "raw_iron_block", "raw_gold_block", "raw_copper_block",
                 "quartz_block", "smooth_quartz", "quartz_bricks",
                 "quartz_pillar", "chiseled_quartz_block",
                 "honeycomb_block" -> true;
            default -> false;
        };
    }

    /**
     * Functional blocks not caught by BlockTags or BaseEntityBlock.
     * Covers crafting stations and interactive blocks without block entities.
     */
    private static boolean isFunctionalBlock(String path) {
        return switch (path) {
            case "crafting_table", "stonecutter", "grindstone",
                 "loom", "cartography_table", "fletching_table", "smithing_table",
                 "composter", "ladder", "scaffolding",
                 "bookshelf", "note_block", "lightning_rod" -> true;
            default -> false;
        };
    }

    private static boolean isDecorativeBlock(String path) {
        return switch (path) {
            case "lantern", "soul_lantern", "chain", "glow_lichen",
                 "moss_carpet", "pale_moss_carpet",
                 "amethyst_cluster", "small_amethyst_bud", "medium_amethyst_bud",
                 "large_amethyst_bud", "budding_amethyst" -> true;
            default -> path.contains("torch")  // torch, soul_torch (redstone_torch already returned above)
                       || (path.contains("coral") && !path.contains("coral_block")); // coral fans / plants
        };
    }

    private OntologyClassifier() {}
}
