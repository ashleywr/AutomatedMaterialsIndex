package com.sanhiruzu.ami.index;

import net.minecraft.network.chat.Component;

import java.util.List;
import java.util.Locale;

/**
 * Static 2-level ontology for AMI.
 * Categories are ordered by classification priority — first match wins when
 * classifying an ITEM node by its tag/path strings.
 */
public final class AmiOntology {

    public record SubCategory(String id, String translationKey) {
        public Component displayName() {
            return Component.translatable(translationKey);
        }
    }

    public static final class Category {
        public final String id;
        public final String translationKey;
        public final String shortName;
        public final String iconItemId;   // e.g. "minecraft:compass"
        public final int color;           // ARGB
        public final List<SubCategory> subCategories;
        // Substrings checked against (lowercased tags + "," + lowercased path).
        // First category whose any pattern matches wins.
        public final List<String> matchPatterns;

        public Category(String id, String translationKey, String shortName,
                        String iconItemId, int color,
                        List<SubCategory> subCategories,
                        List<String> matchPatterns) {
            this.id = id;
            this.translationKey = translationKey;
            this.shortName = shortName;
            this.iconItemId = iconItemId;
            this.color = color;
            this.subCategories = subCategories;
            this.matchPatterns = matchPatterns;
        }

        public Component displayName() {
            return Component.translatable(translationKey);
        }
    }

    // ── Singleton category constants ──────────────────────────────────────────

    public static final Category NAVIGATION;
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
        NAVIGATION = new Category(
            "navigation", "ami.category.navigation", "Nav",
            "minecraft:compass", 0xFF4499EE,
            List.of(
                new SubCategory("instruments", "ami.subcategory.navigation.instruments"),
                new SubCategory("waypoints",   "ami.subcategory.navigation.waypoints")
            ),
            List.of("compass", "recovery_compass", "spyglass", "clock", ":map")
        );

        SOCIAL = new Category(
            "social", "ami.category.social", "Social",
            "minecraft:player_head", 0xFF66CCFF,
            List.of(
                new SubCategory("players",   "ami.subcategory.social.players"),
                new SubCategory("teams",     "ami.subcategory.social.teams"),
                new SubCategory("claims",    "ami.subcategory.social.claims")
            ),
            List.of("player_head")
        );

        ENTITIES = new Category(
            "entities", "ami.category.entities", "Mobs",
            "minecraft:zombie_head", 0xFFAA3322,
            List.of(
                new SubCategory("passive",  "ami.subcategory.entities.passive"),
                new SubCategory("hostile",  "ami.subcategory.entities.hostile"),
                new SubCategory("neutral",  "ami.subcategory.entities.neutral"),
                new SubCategory("vehicles", "ami.subcategory.entities.vehicles")
            ),
            List.of("spawn_egg")
        );

        MAGIC = new Category(
            "magic", "ami.category.magic", "Magic",
            "minecraft:enchanting_table", 0xFF9933CC,
            List.of(
                new SubCategory("potions",   "ami.subcategory.magic.potions"),
                new SubCategory("books",     "ami.subcategory.magic.books"),
                new SubCategory("artifacts", "ami.subcategory.magic.artifacts"),
                new SubCategory("reagents",  "ami.subcategory.magic.reagents")
            ),
            List.of("potion", "enchant", "blaze", "ender_pearl", "ender_eye",
                    "nether_star", "totem", "dragon_breath", "experience_bottle",
                    "fermented_spider", "ghast_tear", "phantom_membrane",
                    "rabbit_foot", "spider_eye", "glistering_melon")
        );

        ARMOR = new Category(
            "armor", "ami.category.armor", "Armor",
            "minecraft:iron_chestplate", 0xFF4488CC,
            List.of(
                new SubCategory("head",   "ami.subcategory.armor.head"),
                new SubCategory("chest",  "ami.subcategory.armor.chest"),
                new SubCategory("legs",   "ami.subcategory.armor.legs"),
                new SubCategory("feet",   "ami.subcategory.armor.feet"),
                new SubCategory("curios", "ami.subcategory.armor.curios")
            ),
            List.of("head_armor", "chest_armor", "leg_armor", "foot_armor",
                    "helmet", "chestplate", "leggings", "boots", "elytra",
                    "turtle_helmet", ":armor")
        );

        WEAPONS = new Category(
            "weapons", "ami.category.weapons", "Weapons",
            "minecraft:iron_sword", 0xFFCC4444,
            List.of(
                new SubCategory("melee",   "ami.subcategory.weapons.melee"),
                new SubCategory("ranged",  "ami.subcategory.weapons.ranged"),
                new SubCategory("harvest", "ami.subcategory.weapons.harvest"),
                new SubCategory("utility", "ami.subcategory.weapons.utility")
            ),
            List.of("swords", "sword", "bows", "bow", "crossbow", "trident",
                    "arrows", ":arrow",
                    "pickaxes", "pickaxe", "shovels", "shovel",
                    ":hoes", ":hoe", ":axes", ":axe",
                    "fishing_rod", "shears", "flint_and_steel",
                    ":tools", "weapons")
        );

        FOOD = new Category(
            "food", "ami.category.food", "Food",
            "minecraft:apple", 0xFF44AA44,
            List.of(
                new SubCategory("meals",    "ami.subcategory.food.meals"),
                new SubCategory("snacks",   "ami.subcategory.food.snacks"),
                new SubCategory("drinks",   "ami.subcategory.food.drinks"),
                new SubCategory("proteins", "ami.subcategory.food.proteins")
            ),
            List.of("foods", "food", "cooked_", "stew", "soup", "apple",
                    "sweet_berries", "glow_berries", "cake", "cookie", "bread",
                    "honey_bottle", "dried_kelp", "chorus_fruit",
                    "raw_beef", "raw_chicken", "raw_porkchop", "raw_cod",
                    "raw_salmon", "raw_rabbit", "raw_mutton")
        );

        TECH = new Category(
            "tech", "ami.category.tech", "Tech",
            "minecraft:iron_ingot", 0xFFCC8833,
            List.of(
                new SubCategory("ingots",   "ami.subcategory.tech.ingots"),
                new SubCategory("dusts",    "ami.subcategory.tech.dusts"),
                new SubCategory("parts",    "ami.subcategory.tech.parts"),
                new SubCategory("circuits", "ami.subcategory.tech.circuits")
            ),
            List.of("ingots", "ingot", "gems", ":gem", "dusts", "dust",
                    "nuggets", "nugget", ":ores", ":ore",
                    "raw_iron", "raw_gold", "raw_copper",
                    "netherite", "redstone", "amethyst", "quartz",
                    "diamond", "emerald", "piston", "comparator", "lodestone")
        );

        NATURE = new Category(
            "nature", "ami.category.nature", "Nature",
            "minecraft:wheat", 0xFF66CC44,
            List.of(
                new SubCategory("seeds", "ami.subcategory.nature.seeds"),
                new SubCategory("crops", "ami.subcategory.nature.crops"),
                new SubCategory("flora", "ami.subcategory.nature.flora"),
                new SubCategory("fungi", "ami.subcategory.nature.fungi"),
                new SubCategory("wood",  "ami.subcategory.nature.wood")
            ),
            List.of("saplings", "sapling", "seeds", ":seed",
                    "flowers", ":flower", "mushroom", "leaves", ":log", "logs",
                    "kelp", "seagrass", "bamboo", "vine",
                    "wheat", "carrot", "potato", "beetroot",
                    "pumpkin", "melon", "cocoa", "cactus", "sugar_cane")
        );

        ENVIRONMENT = new Category(
            "environment", "ami.category.environment", "World",
            "minecraft:grass_block", 0xFF339966,
            List.of(
                new SubCategory("biomes",     "ami.subcategory.environment.biomes"),
                new SubCategory("dimensions", "ami.subcategory.environment.dimensions"),
                new SubCategory("structures", "ami.subcategory.environment.structures"),
                new SubCategory("transport",  "ami.subcategory.environment.transport")
            ),
            List.of("minecart", "_boat", "chest_boat")
        );

        BLOCKS = new Category(
            "blocks", "ami.category.blocks", "Blocks",
            "minecraft:bricks", 0xFF888888,
            List.of(
                // Always-visible structural subcategories
                new SubCategory("functional",     "ami.subcategory.blocks.functional"),
                new SubCategory("redstone",       "ami.subcategory.blocks.redstone"),
                new SubCategory("decorative",     "ami.subcategory.blocks.decorative"),
                // Shape subcategories (shown when blockSubgroup = SHAPE)
                new SubCategory("full_block",     "ami.subcategory.blocks.full_block"),
                new SubCategory("stairs",         "ami.subcategory.blocks.stairs"),
                new SubCategory("slab",           "ami.subcategory.blocks.slab"),
                new SubCategory("wall",           "ami.subcategory.blocks.wall"),
                new SubCategory("fence",          "ami.subcategory.blocks.fence"),
                new SubCategory("pane",           "ami.subcategory.blocks.pane"),
                // Material subcategories (shown when blockSubgroup = MATERIAL)
                new SubCategory("stone",          "ami.subcategory.blocks.stone"),
                new SubCategory("wood",           "ami.subcategory.blocks.wood"),
                new SubCategory("soil",           "ami.subcategory.blocks.soil"),
                new SubCategory("glass",          "ami.subcategory.blocks.glass"),
                new SubCategory("other_building", "ami.subcategory.blocks.other_building")
            ),
            List.of("banner") // banner patterns etc catch-all
        );

        // Priority order: most-specific first, BLOCKS last as default.
        CATEGORIES = List.of(
            NAVIGATION, ENTITIES, MAGIC, ARMOR, WEAPONS, FOOD, TECH, NATURE, ENVIRONMENT, SOCIAL, BLOCKS
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
