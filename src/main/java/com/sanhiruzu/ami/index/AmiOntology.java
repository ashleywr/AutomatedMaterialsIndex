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

    public record SubCategory(String id, Component displayName) {}

    public static final class Category {
        public final String id;
        public final Component displayName;
        public final String shortName;
        public final String iconItemId;   // e.g. "minecraft:compass"
        public final int color;           // ARGB
        public final List<SubCategory> subCategories;
        // Substrings checked against (lowercased tags + "," + lowercased path).
        // First category whose any pattern matches wins.
        public final List<String> matchPatterns;

        public Category(String id, Component displayName, String shortName,
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
            "navigation", Component.translatable("ami.category.navigation"), "Nav",
            "minecraft:compass", 0xFF4499EE,
            List.of(
                new SubCategory("instruments", Component.translatable("ami.subcategory.navigation.instruments")),
                new SubCategory("waypoints",   Component.translatable("ami.subcategory.navigation.waypoints"))
            ),
            List.of("compass", "recovery_compass", "spyglass", "clock", ":map")
        );

        SOCIAL = new Category(
            "social", Component.translatable("ami.category.social"), "Social",
            "minecraft:player_head", 0xFF66CCFF,
            List.of(
                new SubCategory("players",   Component.translatable("ami.subcategory.social.players")),
                new SubCategory("teams",     Component.translatable("ami.subcategory.social.teams")),
                new SubCategory("claims",    Component.translatable("ami.subcategory.social.claims"))
            ),
            List.of("player_head")
        );

        ENTITIES = new Category(
            "entities", Component.translatable("ami.category.entities"), "Mobs",
            "minecraft:zombie_head", 0xFFAA3322,
            List.of(
                new SubCategory("passive",  Component.translatable("ami.subcategory.entities.passive")),
                new SubCategory("hostile",  Component.translatable("ami.subcategory.entities.hostile")),
                new SubCategory("neutral",  Component.translatable("ami.subcategory.entities.neutral")),
                new SubCategory("vehicles", Component.translatable("ami.subcategory.entities.vehicles"))
            ),
            List.of("spawn_egg", "minecart", "chest_boat", "_boat")
        );

        MAGIC = new Category(
            "magic", Component.translatable("ami.category.magic"), "Magic",
            "minecraft:enchanting_table", 0xFF9933CC,
            List.of(
                new SubCategory("potions",   Component.translatable("ami.subcategory.magic.potions")),
                new SubCategory("books",     Component.translatable("ami.subcategory.magic.books")),
                new SubCategory("artifacts", Component.translatable("ami.subcategory.magic.artifacts")),
                new SubCategory("reagents",  Component.translatable("ami.subcategory.magic.reagents"))
            ),
            List.of("potion", "enchant", "blaze", "ender_pearl", "ender_eye",
                    "nether_star", "totem", "dragon_breath", "experience_bottle",
                    "fermented_spider", "ghast_tear", "phantom_membrane",
                    "rabbit_foot", "spider_eye", "glistering_melon")
        );

        ARMOR = new Category(
            "armor", Component.translatable("ami.category.armor"), "Armor",
            "minecraft:iron_chestplate", 0xFF4488CC,
            List.of(
                new SubCategory("head",   Component.translatable("ami.subcategory.armor.head")),
                new SubCategory("chest",  Component.translatable("ami.subcategory.armor.chest")),
                new SubCategory("legs",   Component.translatable("ami.subcategory.armor.legs")),
                new SubCategory("feet",   Component.translatable("ami.subcategory.armor.feet")),
                new SubCategory("curios", Component.translatable("ami.subcategory.armor.curios"))
            ),
            List.of("head_armor", "chest_armor", "leg_armor", "foot_armor",
                    "helmet", "chestplate", "leggings", "boots", "elytra",
                    "turtle_helmet", ":armor")
        );

        WEAPONS = new Category(
            "weapons", Component.translatable("ami.category.weapons"), "Weapons",
            "minecraft:iron_sword", 0xFFCC4444,
            List.of(
                new SubCategory("melee",   Component.translatable("ami.subcategory.weapons.melee")),
                new SubCategory("ranged",  Component.translatable("ami.subcategory.weapons.ranged")),
                new SubCategory("harvest", Component.translatable("ami.subcategory.weapons.harvest")),
                new SubCategory("utility", Component.translatable("ami.subcategory.weapons.utility"))
            ),
            List.of("swords", "sword", "bows", "bow", "crossbow", "trident",
                    "arrows", ":arrow",
                    "pickaxes", "pickaxe", "shovels", "shovel",
                    ":hoes", ":hoe", ":axes", ":axe",
                    "fishing_rod", "shears", "flint_and_steel",
                    ":tools", "weapons")
        );

        FOOD = new Category(
            "food", Component.translatable("ami.category.food"), "Food",
            "minecraft:apple", 0xFF44AA44,
            List.of(
                new SubCategory("meals",    Component.translatable("ami.subcategory.food.meals")),
                new SubCategory("snacks",   Component.translatable("ami.subcategory.food.snacks")),
                new SubCategory("drinks",   Component.translatable("ami.subcategory.food.drinks")),
                new SubCategory("proteins", Component.translatable("ami.subcategory.food.proteins"))
            ),
            List.of("foods", "food", "cooked_", "stew", "soup",
                    "sweet_berries", "glow_berries", "cake", "cookie", "bread",
                    "honey_bottle", "dried_kelp", "chorus_fruit",
                    "raw_beef", "raw_chicken", "raw_porkchop", "raw_cod",
                    "raw_salmon", "raw_rabbit", "raw_mutton")
        );

        TECH = new Category(
            "tech", Component.translatable("ami.category.tech"), "Tech",
            "minecraft:iron_ingot", 0xFFCC8833,
            List.of(
                new SubCategory("ingots",   Component.translatable("ami.subcategory.tech.ingots")),
                new SubCategory("dusts",    Component.translatable("ami.subcategory.tech.dusts")),
                new SubCategory("parts",    Component.translatable("ami.subcategory.tech.parts")),
                new SubCategory("circuits", Component.translatable("ami.subcategory.tech.circuits"))
            ),
            List.of("ingots", "ingot", "gems", ":gem", "dusts", "dust",
                    "nuggets", "nugget", ":ores", ":ore",
                    "raw_iron", "raw_gold", "raw_copper",
                    "netherite", "redstone", "amethyst", "quartz",
                    "diamond", "emerald", "piston", "comparator", "lodestone")
        );

        NATURE = new Category(
            "nature", Component.translatable("ami.category.nature"), "Nature",
            "minecraft:wheat", 0xFF66CC44,
            List.of(
                new SubCategory("seeds", Component.translatable("ami.subcategory.nature.seeds")),
                new SubCategory("crops", Component.translatable("ami.subcategory.nature.crops")),
                new SubCategory("flora", Component.translatable("ami.subcategory.nature.flora")),
                new SubCategory("fungi", Component.translatable("ami.subcategory.nature.fungi")),
                new SubCategory("wood",  Component.translatable("ami.subcategory.nature.wood"))
            ),
            List.of("saplings", "sapling", "seeds", ":seed",
                    "flowers", ":flower", "mushroom", "leaves", ":log", "logs",
                    "kelp", "seagrass", "bamboo", "vine",
                    "wheat", "carrot", "potato", "beetroot",
                    "pumpkin", "melon", "cocoa", "cactus", "sugar_cane")
        );

        ENVIRONMENT = new Category(
            "environment", Component.translatable("ami.category.environment"), "World",
            "minecraft:grass_block", 0xFF339966,
            List.of(
                new SubCategory("biomes",     Component.translatable("ami.subcategory.environment.biomes")),
                new SubCategory("dimensions", Component.translatable("ami.subcategory.environment.dimensions")),
                new SubCategory("structures", Component.translatable("ami.subcategory.environment.structures")),
                new SubCategory("transport",  Component.translatable("ami.subcategory.environment.transport"))
            ),
            List.of() // handled by NodeType in classifyNode()
        );

        BLOCKS = new Category(
            "blocks", Component.translatable("ami.category.blocks"), "Blocks",
            "minecraft:bricks", 0xFF888888,
            List.of(
                // Always-visible structural subcategories
                new SubCategory("functional",     Component.translatable("ami.subcategory.blocks.functional")),
                new SubCategory("redstone",       Component.translatable("ami.subcategory.blocks.redstone")),
                new SubCategory("decorative",     Component.translatable("ami.subcategory.blocks.decorative")),
                // Shape subcategories (shown when blockSubgroup = SHAPE)
                new SubCategory("full_block",     Component.translatable("ami.subcategory.blocks.full_block")),
                new SubCategory("stairs",         Component.translatable("ami.subcategory.blocks.stairs")),
                new SubCategory("slab",           Component.translatable("ami.subcategory.blocks.slab")),
                new SubCategory("wall",           Component.translatable("ami.subcategory.blocks.wall")),
                new SubCategory("fence",          Component.translatable("ami.subcategory.blocks.fence")),
                new SubCategory("pane",           Component.translatable("ami.subcategory.blocks.pane")),
                // Material subcategories (shown when blockSubgroup = MATERIAL)
                new SubCategory("stone",          Component.translatable("ami.subcategory.blocks.stone")),
                new SubCategory("wood",           Component.translatable("ami.subcategory.blocks.wood")),
                new SubCategory("soil",           Component.translatable("ami.subcategory.blocks.soil")),
                new SubCategory("glass",          Component.translatable("ami.subcategory.blocks.glass")),
                new SubCategory("other_building", Component.translatable("ami.subcategory.blocks.other_building"))
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
