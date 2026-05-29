package com.sanhiruzu.ami.index;

import net.minecraft.network.chat.Component;

import java.util.List;
import java.util.Locale;

import com.sanhiruzu.ami.AmiCore;
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

    public static final Category UTILITY;
    public static final Category SOCIAL;
    public static final Category BESTIARY;
    public static final Category MAGIC;
    public static final Category ARMOR;
    public static final Category TOOLS;
    public static final Category TECH;
    public static final Category NATURE;
    public static final Category INGREDIENTS;
    public static final Category DECORATION;
    public static final Category ENVIRONMENT;
    public static final Category GEOLOGY;
    public static final Category MASONRY;
    /**
     * Terminal catch-all — items that fell through every other classifier.
     */
    public static final Category MISC;

    /**
     * All categories in classification-priority order.
     */
    public static final List<Category> CATEGORIES;

    static {
        UTILITY = new Category(
                "utility", "ami.category.utility", "Utility",
                "minecraft:compass", 0xFF4499EE,
                List.of(
                        new SubCategory("navigation", "ami.subcategory.utility.navigation"),
                        new SubCategory("tools", "ami.subcategory.utility.tools"),
                        new SubCategory("misc", "ami.subcategory.utility.misc")
                ),
                List.of("compass", "recovery_compass", "spyglass", "clock", ":map",
                        "saddle", "bucket", "name_tag", "lead", "debug_stick", "firework", "music_disc", "disc_fragment", "echo_shard")
        );

        SOCIAL = new Category(
                "social", "ami.category.social", "Social",
                "minecraft:player_head", 0xFF66CCFF,
                List.of(
                        new SubCategory("players", "ami.subcategory.social.players"),
                        new SubCategory("teams", "ami.subcategory.social.teams"),
                        new SubCategory("claims", "ami.subcategory.social.claims")
                ),
                List.of("player_head")
        );

        BESTIARY = new Category(
                "bestiary", "ami.category.bestiary", "Bestiary",
                "minecraft:zombie_head", 0xFFAA3322,
                List.of(
                        new SubCategory("passive", "ami.subcategory.bestiary.passive"),
                        new SubCategory("hostile", "ami.subcategory.bestiary.hostile"),
                        new SubCategory("neutral", "ami.subcategory.bestiary.neutral"),
                        new SubCategory("vehicles", "ami.subcategory.bestiary.vehicles")
                ),
                List.of("spawn_egg")
        );

        MAGIC = new Category(
                "magic", "ami.category.magic", "Magic",
                "minecraft:enchanting_table", 0xFF9933CC,
                List.of(
                        new SubCategory("potions", "ami.subcategory.magic.potions"),
                        new SubCategory("books", "ami.subcategory.magic.books"),
                        new SubCategory("artifacts", "ami.subcategory.magic.artifacts"),
                        new SubCategory("reagents", "ami.subcategory.magic.reagents")
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
                        new SubCategory("head", "ami.subcategory.armor.head"),
                        new SubCategory("chest", "ami.subcategory.armor.chest"),
                        new SubCategory("legs", "ami.subcategory.armor.legs"),
                        new SubCategory("feet", "ami.subcategory.armor.feet"),
                        new SubCategory("animal", "ami.subcategory.armor.animal"),
                        new SubCategory("curios", "ami.subcategory.armor.curios")
                ),
                List.of("head_armor", "chest_armor", "leg_armor", "foot_armor",
                        "helmet", "chestplate", "leggings", "boots", "elytra",
                        "turtle_helmet", ":armor", "horse_armor")
        );

        TOOLS = new Category(
                "tools", "ami.category.tools", "Tools",
                "minecraft:iron_sword", 0xFFCC4444,
                List.of(
                        new SubCategory("melee", "ami.subcategory.tools.melee"),
                        new SubCategory("ranged", "ami.subcategory.tools.ranged"),
                        new SubCategory("harvest", "ami.subcategory.tools.harvest"),
                        new SubCategory("utility", "ami.subcategory.tools.utility")
                ),
                List.of("swords", "sword", "bows", "bow", "crossbow", "trident",
                        "arrows", ":arrow",
                        "pickaxes", "pickaxe", "shovels", "shovel",
                        ":hoes", ":hoe", ":axes", ":axe",
                        "fishing_rod", "shears", "flint_and_steel",
                        ":tools", "tools", "weapons")
        );

        TECH = new Category(
                "tech", "ami.category.tech", "Tech",
                "minecraft:redstone", 0xFFCC8833,
                List.of(
                        new SubCategory("machines", "ami.subcategory.tech.machines"),
                        new SubCategory("redstone", "ami.subcategory.tech.redstone"),
                        new SubCategory("transport", "ami.subcategory.tech.transport"),
                        new SubCategory("ingots", "ami.subcategory.tech.ingots"),
                        new SubCategory("dusts", "ami.subcategory.tech.dusts"),
                        new SubCategory("parts", "ami.subcategory.tech.parts"),
                        new SubCategory("circuits", "ami.subcategory.tech.circuits")
                ),
                List.of(":ingot", ":gem", ":dust", ":nugget", ":raw_materials",
                        "raw_iron", "raw_gold", "raw_copper", "netherite_ingot", "netherite_scrap",
                        "redstone", "piston", "rail", "minecart", "observer", "sensor", "target",
                        "repeater", "comparator", "dispenser", "dropper", "hopper", "_boat", "chest_boat",
                        "daylight_detector", "note_block", "lightning_rod", "jigsaw", "structure_block")
        );

        NATURE = new Category(
                "nature", "ami.category.nature", "Nature",
                "minecraft:apple", 0xFF66CC44,
                List.of(
                        new SubCategory("meals", "ami.subcategory.food.meals"),
                        new SubCategory("snacks", "ami.subcategory.food.snacks"),
                        new SubCategory("drinks", "ami.subcategory.food.drinks"),
                        new SubCategory("proteins", "ami.subcategory.food.proteins"),
                        new SubCategory("seeds", "ami.subcategory.nature.seeds"),
                        new SubCategory("crops", "ami.subcategory.nature.crops"),
                        new SubCategory("flora", "ami.subcategory.nature.flora"),
                        new SubCategory("fungi", "ami.subcategory.nature.fungi"),
                        new SubCategory("wood", "ami.subcategory.nature.wood")
                ),
                List.of("saplings", "sapling", "seeds", ":seed",
                        "flowers", ":flower", "mushroom", "leaves", ":log", "logs",
                        "kelp", "seagrass", "bamboo", "vine",
                        "wheat", "carrot", "potato", "beetroot",
                        "pumpkin", "melon", "cocoa", "cactus", "sugar_cane",
                        ":foods", ":food", "cooked_", "stew", "soup", "apple",
                        "sweet_berries", "glow_berries", "cake", "cookie", "bread",
                        "honey_bottle", "dried_kelp", "chorus_fruit",
                        "raw_beef", "raw_chicken", "raw_porkchop", "raw_cod",
                        "raw_salmon", "raw_rabbit", "raw_mutton",
                        "coral", "fungus", "fern", "dead_bush", "moss", "honey", "slime",
                        "short_grass", "tall_grass", "dripleaf", "lily_pad", "chorus_plant", "mycelium", "roots", "stem")
        );

        INGREDIENTS = new Category(
                "ingredients", "ami.category.ingredients", "Parts",
                "minecraft:string", 0xFFDDDDDD,
                List.of(
                        new SubCategory("organic", "ami.subcategory.ingredients.organic"),
                        new SubCategory("mineral", "ami.subcategory.ingredients.mineral"),
                        new SubCategory("dyes", "ami.subcategory.ingredients.dyes")
                ),
                List.of("string", "feather", "flint", "dye", "leather", "clay_ball", "scute", "honeycomb",
                        "prismarine_shard", "prismarine_crystals", "bone", "paper", "ink_sac", "glow_ink_sac",
                        "rabbit_hide", "shulker_shell", "nautilus_shell", "heart_of_the_sea", "pottery_sherd", "egg")
        );

        DECORATION = new Category(
                "decoration", "ami.category.decoration", "Decor",
                "minecraft:painting", 0xFFEEAA44,
                List.of(
                        new SubCategory("furniture", "ami.subcategory.decoration.furniture"),
                        new SubCategory("lighting", "ami.subcategory.decoration.lighting"),
                        new SubCategory("textiles", "ami.subcategory.decoration.textiles")
                ),
                List.of("carpet", "bed", "torch", "lantern", "candle", "froglight", "banner", "item_frame",
                        "painting", "head", "skull", "glass_pane", "iron_bars", "chain", "flower_pot",
                        "glowstone", "sea_lantern", "shroomlight")
        );

        ENVIRONMENT = new Category(
                "environment", "ami.category.environment", "World",
                "minecraft:grass_block", 0xFF339966,
                List.of(
                        new SubCategory("biomes", "ami.subcategory.environment.biomes"),
                        new SubCategory("dimensions", "ami.subcategory.environment.dimensions"),
                        new SubCategory("structures", "ami.subcategory.environment.structures")
                ),
                List.of()
        );

        GEOLOGY = new Category(
                "geology", "ami.category.geology", "Geology",
                "minecraft:stone", 0xFF887755,
                List.of(
                        new SubCategory("terrain", "ami.subcategory.geology.terrain"),
                        new SubCategory("stone", "ami.subcategory.geology.stone")
                ),
                List.of()
        );

        MASONRY = new Category(
                "masonry", "ami.category.masonry", "Masonry",
                "minecraft:bricks", 0xFF888888,
                List.of(
                        new SubCategory("functional", "ami.subcategory.masonry.functional"),
                        new SubCategory("redstone", "ami.subcategory.masonry.redstone"),
                        new SubCategory("decorative", "ami.subcategory.masonry.decorative"),
                        // Shape subcategories (shown when blockSubgroup = SHAPE)
                        new SubCategory("full_block", "ami.subcategory.masonry.full_block"),
                        new SubCategory("stairs", "ami.subcategory.masonry.stairs"),
                        new SubCategory("slab", "ami.subcategory.masonry.slab"),
                        new SubCategory("wall", "ami.subcategory.masonry.wall"),
                        new SubCategory("fence", "ami.subcategory.masonry.fence"),
                        new SubCategory("pane", "ami.subcategory.masonry.pane"),
                        // Material subcategories (shown when blockSubgroup = MATERIAL)
                        new SubCategory("stone", "ami.subcategory.masonry.stone"),
                        new SubCategory("wood", "ami.subcategory.masonry.wood"),
                        new SubCategory("soil", "ami.subcategory.masonry.soil"),
                        new SubCategory("glass", "ami.subcategory.masonry.glass"),
                        new SubCategory("other_building", "ami.subcategory.masonry.other_building")
                ),
                List.of()
        );

        MISC = new Category(
                "misc", "ami.category.misc", "Misc",
                "minecraft:paper", 0xFF888888,
                List.of(
                        new SubCategory("unknown", "ami.subcategory.misc.unknown")
                ),
                List.of()
        );

        // Priority order: most-specific first, GEOLOGY/MASONRY second-to-last, MISC as terminal fallback.
        CATEGORIES = List.of(
                UTILITY, BESTIARY, MAGIC, ARMOR, TOOLS, TECH, NATURE, INGREDIENTS, DECORATION, ENVIRONMENT, SOCIAL, GEOLOGY, MASONRY, MISC
        );
    }

    // ── Classification ────────────────────────────────────────────────────────

    /**
     * Classifies a SearchNode into the best-matching ontology category.
     * <p>
     * Priority:
     * 1. Pre-computed ONTOLOGY_CATEGORY metadata (set by OntologyClassifier during indexing)
     * 2. NodeType mapping for atlas nodes (BIOME → ENVIRONMENT, ENTITY → ENTITIES, PLAYER → SOCIAL)
     * 3. Runtime tag/path heuristics for ITEM nodes without pre-computed data
     */
    public static Category classifyNode(SearchNode node) {
        String precomputed = node.meta(SearchNodeKeys.ONTOLOGY_CATEGORY, "");
        if ("weapons".equals(precomputed)) {
            precomputed = "tools";
        }
        if (!precomputed.isEmpty()) {
            for (Category cat : CATEGORIES) {
                if (cat.id.equals(precomputed)) return cat;
            }
        }

        return switch (node.type()) {
            case BIOME, STRUCTURE, DIMENSION -> ENVIRONMENT;
            case ENTITY -> BESTIARY;
            case PLAYER -> SOCIAL;
            case ITEM -> classifyItem(node);
        };
    }

    private static Category classifyItem(SearchNode node) {
        String tags = node.meta(SearchNodeKeys.TAGS, "").toLowerCase(Locale.ROOT);
        String path = node.id().getPath().toLowerCase(Locale.ROOT);
        // Single combined string so each pattern is checked against both at once.
        String combined = tags + "," + path;

        for (Category cat : CATEGORIES) {
            if (cat == ENVIRONMENT || cat == GEOLOGY || cat == MASONRY || cat == MISC) continue;
            for (String pattern : cat.matchPatterns) {
                if (combined.contains(pattern)) return cat;
            }
        }

        return MISC;
    }

    private AmiOntology() {
    }
}
