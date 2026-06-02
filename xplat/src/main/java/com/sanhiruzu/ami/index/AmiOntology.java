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

    public static final Category UTILITY;
    public static final Category SOCIAL;
    public static final Category COBBLEMON;
    public static final Category CREATE;
    public static final Category AE2;
    public static final Category MEKANISM;
    public static final Category GREGTECH;
    public static final Category MINECOLONIES;
    public static final Category APOTHEOSIS;
    public static final Category BOTANIA;
    public static final Category SOPHISTICATED;
    public static final Category MAPPING;
    public static final Category MODULAR_GEAR;

    // ── Singleton category constants ──────────────────────────────────────────
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
                        new SubCategory("medical", "ami.subcategory.utility.medical"),
                        new SubCategory("currency", "ami.subcategory.utility.currency"),
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

        COBBLEMON = new Category(
                "cobblemon", "ami.category.cobblemon", "Cobblemon",
                "cobblemon:poke_ball", 0xFFE95B5B,
                List.of(
                        new SubCategory("species", "ami.subcategory.cobblemon.species"),
                        new SubCategory("poke_balls", "ami.subcategory.cobblemon.poke_balls"),
                        new SubCategory("medicine", "ami.subcategory.cobblemon.medicine"),
                        new SubCategory("berries", "ami.subcategory.cobblemon.berries"),
                        new SubCategory("apricorns", "ami.subcategory.cobblemon.apricorns"),
                        new SubCategory("evolution", "ami.subcategory.cobblemon.evolution"),
                        new SubCategory("fossils", "ami.subcategory.cobblemon.fossils"),
                        new SubCategory("machines", "ami.subcategory.cobblemon.machines"),
                        new SubCategory("decor", "ami.subcategory.cobblemon.decor"),
                        new SubCategory("transport", "ami.subcategory.cobblemon.transport"),
                        new SubCategory("held_items", "ami.subcategory.cobblemon.held_items"),
                        new SubCategory("utility", "ami.subcategory.cobblemon.utility"),
                        new SubCategory("consumables", "ami.subcategory.cobblemon.consumables"),
                        new SubCategory("agriculture", "ami.subcategory.cobblemon.agriculture"),
                        new SubCategory("building", "ami.subcategory.cobblemon.building"),
                        new SubCategory("archaeology", "ami.subcategory.cobblemon.archaeology"),
                        new SubCategory("misc", "ami.subcategory.cobblemon.misc")
                ),
                List.of()
        );

        CREATE = new Category(
                "create", "ami.category.create", "Create",
                "create:cogwheel", 0xFFD79B5D,
                List.of(
                        new SubCategory("kinetics", "ami.subcategory.create.kinetics"),
                        new SubCategory("machines", "ami.subcategory.create.machines"),
                        new SubCategory("logistics", "ami.subcategory.create.logistics"),
                        new SubCategory("trains", "ami.subcategory.create.trains"),
                        new SubCategory("contraptions", "ami.subcategory.create.contraptions"),
                        new SubCategory("fluids", "ami.subcategory.create.fluids"),
                        new SubCategory("tools", "ami.subcategory.create.tools"),
                        new SubCategory("materials", "ami.subcategory.create.materials"),
                        new SubCategory("building", "ami.subcategory.create.building"),
                        new SubCategory("misc", "ami.subcategory.create.misc")
                ),
                List.of()
        );

        AE2 = new Category(
                "ae2", "ami.category.ae2", "AE2",
                "ae2:controller", 0xFF79B7E8,
                List.of(
                        new SubCategory("network", "ami.subcategory.ae2.network"),
                        new SubCategory("storage", "ami.subcategory.ae2.storage"),
                        new SubCategory("terminals", "ami.subcategory.ae2.terminals"),
                        new SubCategory("crafting", "ami.subcategory.ae2.crafting"),
                        new SubCategory("channels", "ami.subcategory.ae2.channels"),
                        new SubCategory("spatial", "ami.subcategory.ae2.spatial"),
                        new SubCategory("materials", "ami.subcategory.ae2.materials"),
                        new SubCategory("misc", "ami.subcategory.ae2.misc")
                ),
                List.of()
        );

        MEKANISM = new Category(
                "mekanism", "ami.category.mekanism", "Mekanism",
                "mekanism:metallurgic_infuser", 0xFF48B7A7,
                List.of(
                        new SubCategory("machines", "ami.subcategory.mekanism.machines"),
                        new SubCategory("energy", "ami.subcategory.mekanism.energy"),
                        new SubCategory("chemicals", "ami.subcategory.mekanism.chemicals"),
                        new SubCategory("logistics", "ami.subcategory.mekanism.logistics"),
                        new SubCategory("upgrades", "ami.subcategory.mekanism.upgrades"),
                        new SubCategory("tools", "ami.subcategory.mekanism.tools"),
                        new SubCategory("materials", "ami.subcategory.mekanism.materials"),
                        new SubCategory("misc", "ami.subcategory.mekanism.misc")
                ),
                List.of()
        );

        GREGTECH = new Category(
                "gregtech", "ami.category.gregtech", "GregTech",
                "gtceu:steam_solid_boiler", 0xFF8FA0AA,
                List.of(
                        new SubCategory("machines", "ami.subcategory.gregtech.machines"),
                        new SubCategory("multiblocks", "ami.subcategory.gregtech.multiblocks"),
                        new SubCategory("power", "ami.subcategory.gregtech.power"),
                        new SubCategory("circuits", "ami.subcategory.gregtech.circuits"),
                        new SubCategory("materials", "ami.subcategory.gregtech.materials"),
                        new SubCategory("tools", "ami.subcategory.gregtech.tools"),
                        new SubCategory("covers", "ami.subcategory.gregtech.covers"),
                        new SubCategory("misc", "ami.subcategory.gregtech.misc")
                ),
                List.of()
        );

        MINECOLONIES = new Category(
                "minecolonies", "ami.category.minecolonies", "MineColonies",
                "minecolonies:buildingtool", 0xFFB68B5B,
                List.of(
                        new SubCategory("buildings", "ami.subcategory.minecolonies.buildings"),
                        new SubCategory("workorders", "ami.subcategory.minecolonies.workorders"),
                        new SubCategory("workers", "ami.subcategory.minecolonies.workers"),
                        new SubCategory("research", "ami.subcategory.minecolonies.research"),
                        new SubCategory("supply", "ami.subcategory.minecolonies.supply"),
                        new SubCategory("decor", "ami.subcategory.minecolonies.decor"),
                        new SubCategory("misc", "ami.subcategory.minecolonies.misc")
                ),
                List.of()
        );

        APOTHEOSIS = new Category(
                "apotheosis", "ami.category.apotheosis", "Apotheosis",
                "apotheosis:hellshelf", 0xFFD05BC6,
                List.of(
                        new SubCategory("affixes", "ami.subcategory.apotheosis.affixes"),
                        new SubCategory("gems", "ami.subcategory.apotheosis.gems"),
                        new SubCategory("sockets", "ami.subcategory.apotheosis.sockets"),
                        new SubCategory("enchanting", "ami.subcategory.apotheosis.enchanting"),
                        new SubCategory("spawners", "ami.subcategory.apotheosis.spawners"),
                        new SubCategory("bosses", "ami.subcategory.apotheosis.bosses"),
                        new SubCategory("misc", "ami.subcategory.apotheosis.misc")
                ),
                List.of()
        );

        BOTANIA = new Category(
                "botania", "ami.category.botania", "Botania",
                "botania:lexicon", 0xFF65C66F,
                List.of(
                        new SubCategory("mana", "ami.subcategory.botania.mana"),
                        new SubCategory("generating_flowers", "ami.subcategory.botania.generating_flowers"),
                        new SubCategory("functional_flowers", "ami.subcategory.botania.functional_flowers"),
                        new SubCategory("runes", "ami.subcategory.botania.runes"),
                        new SubCategory("baubles", "ami.subcategory.botania.baubles"),
                        new SubCategory("tools", "ami.subcategory.botania.tools"),
                        new SubCategory("materials", "ami.subcategory.botania.materials"),
                        new SubCategory("misc", "ami.subcategory.botania.misc")
                ),
                List.of()
        );

        SOPHISTICATED = new Category(
                "sophisticated", "ami.category.sophisticated", "Sophisticated",
                "sophisticatedbackpacks:backpack", 0xFFB78F62,
                List.of(
                        new SubCategory("backpacks", "ami.subcategory.sophisticated.backpacks"),
                        new SubCategory("storage", "ami.subcategory.sophisticated.storage"),
                        new SubCategory("upgrades", "ami.subcategory.sophisticated.upgrades"),
                        new SubCategory("filters", "ami.subcategory.sophisticated.filters"),
                        new SubCategory("tools", "ami.subcategory.sophisticated.tools"),
                        new SubCategory("misc", "ami.subcategory.sophisticated.misc")
                ),
                List.of()
        );

        MAPPING = new Category(
                "mapping", "ami.category.mapping", "Mapping",
                "minecraft:filled_map", 0xFF5FA8A6,
                List.of(
                        new SubCategory("waypoints", "ami.subcategory.mapping.waypoints"),
                        new SubCategory("markers", "ami.subcategory.mapping.markers"),
                        new SubCategory("claims", "ami.subcategory.mapping.claims"),
                        new SubCategory("death_points", "ami.subcategory.mapping.death_points"),
                        new SubCategory("dimensions", "ami.subcategory.mapping.dimensions"),
                        new SubCategory("sharing", "ami.subcategory.mapping.sharing"),
                        new SubCategory("misc", "ami.subcategory.mapping.misc")
                ),
                List.of()
        );

        MODULAR_GEAR = new Category(
                "modular_gear", "ami.category.modular_gear", "Modular Gear",
                "minecraft:smithing_table", 0xFF9D7AE2,
                List.of(
                        new SubCategory("tools", "ami.subcategory.modular_gear.tools"),
                        new SubCategory("weapons", "ami.subcategory.modular_gear.weapons"),
                        new SubCategory("armor", "ami.subcategory.modular_gear.armor"),
                        new SubCategory("parts", "ami.subcategory.modular_gear.parts"),
                        new SubCategory("materials", "ami.subcategory.modular_gear.materials"),
                        new SubCategory("modifiers", "ami.subcategory.modular_gear.modifiers"),
                        new SubCategory("stations", "ami.subcategory.modular_gear.stations"),
                        new SubCategory("blueprints", "ami.subcategory.modular_gear.blueprints"),
                        new SubCategory("misc", "ami.subcategory.modular_gear.misc")
                ),
                List.of()
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
                        new SubCategory("ammo", "ami.subcategory.tools.ammo"),
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
                        new SubCategory("cables", "ami.subcategory.tech.cables"),
                        new SubCategory("upgrades", "ami.subcategory.tech.upgrades"),
                        new SubCategory("templates", "ami.subcategory.tech.templates"),
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
                "masonry", "ami.category.masonry", "Building",
                "minecraft:oak_planks", 0xFF888888,
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
                COBBLEMON, CREATE, AE2, MEKANISM, GREGTECH, MINECOLONIES, APOTHEOSIS, BOTANIA, SOPHISTICATED, MAPPING,
                MODULAR_GEAR, UTILITY, BESTIARY, MAGIC, ARMOR, TOOLS, TECH, NATURE, INGREDIENTS, DECORATION, ENVIRONMENT, SOCIAL,
                GEOLOGY, MASONRY, MISC
        );
    }

    private AmiOntology() {
    }

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
            return dynamicCategory(precomputed);
        }

        return switch (node.type()) {
            case BIOME, STRUCTURE, DIMENSION -> ENVIRONMENT;
            case ENTITY -> BESTIARY;
            case PLAYER -> SOCIAL;
            case ITEM -> classifyItem(node);
        };
    }

    // ── Classification ────────────────────────────────────────────────────────

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

    public static Category categoryForId(String categoryId) {
        if (categoryId == null || categoryId.isBlank()) return MISC;
        String normalized = categoryId.trim().toLowerCase(Locale.ROOT);
        for (Category category : CATEGORIES) {
            if (category.id.equals(normalized)) return category;
        }
        return dynamicCategory(normalized);
    }

    private static Category dynamicCategory(String id) {
        String normalized = id == null || id.isBlank() ? "custom" : id.trim().toLowerCase(Locale.ROOT);
        return new Category(
                normalized,
                "",
                titleCase(normalized.replace(':', ' ').replace('/', ' ').replace('_', ' ')),
                "minecraft:paper",
                0xFF888888,
                List.of(),
                List.of()
        );
    }

    private static String titleCase(String value) {
        String[] words = value.trim().split("\\s+");
        StringBuilder out = new StringBuilder(value.length());
        for (String word : words) {
            if (word.isBlank()) continue;
            if (!out.isEmpty()) out.append(' ');
            out.append(Character.toUpperCase(word.charAt(0)));
            if (word.length() > 1) {
                out.append(word.substring(1));
            }
        }
        return out.isEmpty() ? "Custom" : out.toString();
    }

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
            if (translationKey == null || translationKey.isBlank()) {
                return Component.literal(shortName);
            }
            return Component.translatable(translationKey);
        }
    }
}
