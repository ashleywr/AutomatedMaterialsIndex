package com.sanhiruzu.ami.index;

import java.util.List;
import java.util.Locale;

/**
 * Static 2-level ontology for AMI.
 * Categories are ordered by classification priority — first match wins when
 * classifying an ITEM node by its tag/path strings.
 */
public final class AmiOntology {

    public record SubCategory(String id, String displayName) {}

    public static final class Category {
        public final String id;
        public final String displayName;
        public final String shortName;
        public final String iconItemId;   // e.g. "minecraft:compass"
        public final int color;           // ARGB
        public final List<SubCategory> subCategories;
        // Substrings checked against (lowercased tags + "," + lowercased path).
        // First category whose any pattern matches wins.
        public final List<String> matchPatterns;

        public Category(String id, String displayName, String shortName,
                        String iconItemId, int color,
                        List<SubCategory> subCategories,
                        List<String> matchPatterns) {
            this.id = id;
            this.displayName = displayName;
            this.shortName = shortName;
            this.iconItemId = iconItemId;
            this.color = color;
            this.subCategories = subCategories;
            this.matchPatterns = matchPatterns;
        }
    }

    // ── Singleton category constants ──────────────────────────────────────────

    public static final Category SOCIAL;
    public static final Category ENTITIES;
    public static final Category MAGIC;
    public static final Category ARMOR;
    public static final Category WEAPONS;
    public static final Category FOOD;
    public static final Category TECH;
    public static final Category NATURE;
    public static final Category ENVIRONMENT;
    public static final Category BLOCKS;

    /** All categories in classification-priority order. */
    public static final List<Category> CATEGORIES;

    static {
        SOCIAL = new Category(
            "social", "Social & Navigation", "Social",
            "minecraft:compass", 0xFF4499EE,
            List.of(
                new SubCategory("players",   "Online Players"),
                new SubCategory("waypoints", "Saved Waypoints"),
                new SubCategory("teams",     "Team Members"),
                new SubCategory("claims",    "Public Server Claims")
            ),
            List.of("compass", "player_head", "banner", "lodestone",
                    "recovery_compass", "spyglass", "clock", ":map")
        );

        ENTITIES = new Category(
            "entities", "Entities", "Mobs",
            "minecraft:zombie_head", 0xFFAA3322,
            List.of(
                new SubCategory("passive",  "Passive Mobs"),
                new SubCategory("hostile",  "Hostile Mobs"),
                new SubCategory("neutral",  "Neutral Mobs"),
                new SubCategory("vehicles", "Vehicles & Utility")
            ),
            List.of("spawn_egg", "minecart", "chest_boat", "_boat")
        );

        MAGIC = new Category(
            "magic", "Magic & Alchemy", "Magic",
            "minecraft:enchanting_table", 0xFF9933CC,
            List.of(
                new SubCategory("potions",   "Potions & Flasks"),
                new SubCategory("books",     "Enchanted Books"),
                new SubCategory("artifacts", "Magical Artifacts"),
                new SubCategory("reagents",  "Alchemy Reagents")
            ),
            List.of("potion", "enchant", "blaze", "ender_pearl", "ender_eye",
                    "nether_star", "totem", "dragon_breath", "experience_bottle",
                    "fermented_spider", "ghast_tear", "phantom_membrane",
                    "rabbit_foot", "spider_eye", "glistering_melon")
        );

        ARMOR = new Category(
            "armor", "Armor & Wearables", "Armor",
            "minecraft:iron_chestplate", 0xFF4488CC,
            List.of(
                new SubCategory("head",   "Headwear"),
                new SubCategory("chest",  "Chestwear"),
                new SubCategory("legs",   "Legwear"),
                new SubCategory("feet",   "Footwear"),
                new SubCategory("curios", "Curios & Accessories")
            ),
            List.of("head_armor", "chest_armor", "leg_armor", "foot_armor",
                    "helmet", "chestplate", "leggings", "boots", "elytra",
                    "turtle_helmet", ":armor")
        );

        WEAPONS = new Category(
            "weapons", "Weapons & Tools", "Weapons",
            "minecraft:iron_sword", 0xFFCC4444,
            List.of(
                new SubCategory("melee",   "Melee Weapons"),
                new SubCategory("ranged",  "Ranged Weapons"),
                new SubCategory("harvest", "Harvesting Tools"),
                new SubCategory("utility", "Utility Tools")
            ),
            List.of("swords", "sword", "bows", "bow", "crossbow", "trident",
                    "arrows", ":arrow",
                    "pickaxes", "pickaxe", "shovels", "shovel",
                    ":hoes", ":hoe", ":axes", ":axe",
                    "fishing_rod", "shears", "flint_and_steel",
                    ":tools", "weapons")
        );

        FOOD = new Category(
            "food", "Food & Drinks", "Food",
            "minecraft:apple", 0xFF44AA44,
            List.of(
                new SubCategory("meals",    "Meals"),
                new SubCategory("snacks",   "Snacks"),
                new SubCategory("drinks",   "Beverages & Soups"),
                new SubCategory("proteins", "Raw Proteins")
            ),
            List.of("foods", "food", "cooked_", "stew", "soup",
                    "sweet_berries", "glow_berries", "cake", "cookie", "bread",
                    "honey_bottle", "dried_kelp", "chorus_fruit",
                    "raw_beef", "raw_chicken", "raw_porkchop", "raw_cod",
                    "raw_salmon", "raw_rabbit", "raw_mutton")
        );

        TECH = new Category(
            "tech", "Tech & Materials", "Tech",
            "minecraft:iron_ingot", 0xFFCC8833,
            List.of(
                new SubCategory("ingots",   "Ingots & Gems"),
                new SubCategory("dusts",    "Dusts & Powders"),
                new SubCategory("parts",    "Machine Parts"),
                new SubCategory("circuits", "Circuits & Wires")
            ),
            List.of("ingots", "ingot", "gems", ":gem", "dusts", "dust",
                    "nuggets", "nugget", ":ores", ":ore",
                    "raw_iron", "raw_gold", "raw_copper",
                    "netherite", "redstone", "amethyst", "quartz",
                    "diamond", "emerald", "piston", "comparator")
        );

        NATURE = new Category(
            "nature", "Nature & Farming", "Nature",
            "minecraft:wheat", 0xFF66CC44,
            List.of(
                new SubCategory("seeds", "Seeds & Saplings"),
                new SubCategory("crops", "Raw Crops"),
                new SubCategory("flora", "Flora & Foliage"),
                new SubCategory("fungi", "Fungi & Forage"),
                new SubCategory("wood",  "Wood & Logs")
            ),
            List.of("saplings", "sapling", "seeds", ":seed",
                    "flowers", ":flower", "mushroom", "leaves", ":log", "logs",
                    "kelp", "seagrass", "bamboo", "vine",
                    "wheat", "carrot", "potato", "beetroot",
                    "pumpkin", "melon", "cocoa", "cactus", "sugar_cane")
        );

        ENVIRONMENT = new Category(
            "environment", "Environment", "World",
            "minecraft:grass_block", 0xFF339966,
            List.of(
                new SubCategory("biomes",     "Biomes"),
                new SubCategory("dimensions", "Dimensions"),
                new SubCategory("structures", "Structures")
            ),
            List.of() // handled by NodeType in classifyNode()
        );

        BLOCKS = new Category(
            "blocks", "Blocks", "Blocks",
            "minecraft:bricks", 0xFF888888,
            List.of(
                new SubCategory("building",   "Building Blocks"),
                new SubCategory("functional", "Functional & Interactive"),
                new SubCategory("redstone",   "Redstone & Logic"),
                new SubCategory("decorative", "Decorative")
            ),
            List.of() // catch-all
        );

        // Priority order: most-specific first, BLOCKS last as default.
        CATEGORIES = List.of(
            SOCIAL, ENTITIES, MAGIC, ARMOR, WEAPONS, FOOD, TECH, NATURE, ENVIRONMENT, BLOCKS
        );
    }

    // ── Classification ────────────────────────────────────────────────────────

    /**
     * Classifies a SearchNode into the best-matching ontology category.
     *
     * Priority:
     *  1. Pre-computed ONTOLOGY_CATEGORY metadata (set by OntologyClassifier during indexing)
     *  2. NodeType mapping for atlas nodes (BIOME → ENVIRONMENT, ENTITY → ENTITIES, PLAYER → SOCIAL)
     *  3. Runtime tag/path heuristics for ITEM nodes without pre-computed data
     */
    public static Category classifyNode(SearchNode node) {
        String precomputed = node.meta(SearchNodeKeys.ONTOLOGY_CATEGORY, "");
        if (!precomputed.isEmpty()) {
            for (Category cat : CATEGORIES) {
                if (cat.id.equals(precomputed)) return cat;
            }
        }

        return switch (node.type()) {
            case BIOME, STRUCTURE, DIMENSION -> ENVIRONMENT;
            case ENTITY                      -> ENTITIES;
            case PLAYER                      -> SOCIAL;
            case ITEM                        -> classifyItem(node);
        };
    }

    private static Category classifyItem(SearchNode node) {
        String tags = node.meta(SearchNodeKeys.TAGS, "").toLowerCase(Locale.ROOT);
        String path = node.id().getPath().toLowerCase(Locale.ROOT);
        // Single combined string so each pattern is checked against both at once.
        String combined = tags + "," + path;

        for (Category cat : CATEGORIES) {
            if (cat == ENVIRONMENT || cat == BLOCKS) continue; // handled specially below
            for (String pattern : cat.matchPatterns) {
                if (combined.contains(pattern)) return cat;
            }
        }

        // Anything that's a block-like item and wasn't caught above defaults to Blocks.
        return BLOCKS;
    }

    private AmiOntology() {}
}
