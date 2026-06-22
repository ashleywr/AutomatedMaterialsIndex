package com.sanhiruzu.ami.index;

import com.sanhiruzu.ami.compat.CompatFamilyDetector;
import com.sanhiruzu.ami.config.AmiConfig;

import net.minecraft.resources.ResourceLocation;

import java.util.*;
import java.util.function.Function;
import java.util.function.Predicate;

public final class PrimaryCategoryResolver {
    /*
     * Classification guardrail:
     * - Runtime/API facts beat names: facets, capabilities, components, tags, item/block classes,
     *   explicit compat metadata, and exact registry identities should drive hard routing.
     * - Tokenized path/name evidence is a fallback only. It may help choose among already plausible
     *   buckets, but it must not create ownership, cheat/dev visibility, or broad top-level routes.
     * - Avoid new "isLikely*" gates or arbitrary substring checks. If lexical evidence is unavoidable,
     *   prefer exact tokens/phrases and add a false-positive regression test plus a note in
     *   docs/classification-routing-log.md.
     */
    // TODO(classification): continue shrinking the remaining raw lexical fallbacks below.
    // Prefer facets/facts first, then PathTokens exact token/phrase sets with false-positive tests.
    private static final Set<String> HOSTILE_SPAWN_EGGS = Set.of(
            "blaze", "bogged", "breeze", "creeper", "drowned", "elder_guardian",
            "ender_dragon", "endermite", "evoker", "ghast", "guardian", "hoglin",
            "husk", "magma_cube", "phantom", "piglin_brute", "pillager", "ravager",
            "shulker", "silverfish", "skeleton", "slime", "spider", "cave_spider", "stray",
            "vex", "vindicator", "warden", "witch", "wither", "wither_skeleton",
            "zoglin", "zombie", "zombie_villager"
    );
    private static final Set<String> NEUTRAL_SPAWN_EGGS = Set.of(
            "bee", "dolphin", "enderman", "goat", "iron_golem",
            "llama", "panda", "piglin", "polar_bear", "trader_llama",
            "wolf", "zombified_piglin"
    );
    private static final Set<String> DECOR_TOKENS = Set.of(
            "chair", "stool", "bench", "sofa", "couch", "banister", "railing",
            "shelf", "curtain", "curtains", "blinds", "shutter", "shutters",
            "dresser", "nightstand", "wardrobe", "bookcase", "bookshelf",
            "lamp", "lantern", "chandelier", "sconce", "brazier", "seat",
            "pillow", "cushion", "hammock", "rack", "sign"
    );
    private static final Set<String> LIGHTING_TOKENS = Set.of(
            "lamp", "lantern", "chandelier", "sconce", "brazier", "candelabra"
    );
    private static final Set<String> MICRO_PARTIAL_STATE_HINTS = Set.of(
            "axis", "facing", "connected", "extend", "left", "right", "bottom", "distance",
            "layers", "delay", "lavalogged", "level", "moisture", "particle_radius", "waterlogged",
            "signal_fire", "north", "south", "east", "west", "up", "down"
    );
    private static final Set<String> MICRO_PARTIAL_PATH_HINTS = Set.of(
            "amethyst", "bars", "chorus", "cloud", "clouds", "cluster", "enchanter",
            "end_rod", "firefly", "grate", "hedge", "iron_ladder", "ladder", "matrix",
            "miniature_structure", "pane", "post", "rod", "rope", "scaffolding",
            "thorns"
    );
    private static final Set<String> MICRO_PARTIAL_CLASS_HINTS = Set.of(
            "chain", "cloud", "corundum", "chorus", "hedge", "ladder", "pane", "post", "rod",
            "thorns", "rope", "grate", "forcefield", "miniature", "matrix", "light", "firefly",
            "scaffolding", "campfire", "slider", "wall", "wallpillar", "structurevoid", "cluster", "amethyst"
    );
    private static final Set<String> MICRO_PARTIAL_NATURE_PATH_HINTS = Set.of(
            "chorus", "farmland", "snow", "dirt_path", "moss", "grass", "podzol", "weeds", "twist", "vine"
    );
    private static final Set<String> MICRO_PARTIAL_NATURE_CLASS_HINTS = Set.of(
            "chorus", "hedge", "leaf", "vine", "thorns", "bush", "farm", "snow", "moss", "grass", "twist"
    );
    private static final Set<String> MICRO_PARTIAL_TECH_HINTS = Set.of(
            "cell",
            "magnetized",
            "nodule",
            "portal",
            "volt"
    );
    private static final Set<String> TEXTILE_TOKENS = Set.of(
            "curtain", "curtains", "blinds", "shutter", "shutters", "pillow",
            "cushion", "rug", "carpet", "blanket", "sheet", "banner", "banners",
            "tapestry", "canvas", "streamer", "streamers"
    );
    private static final Set<String> DISPLAY_TOKENS = Set.of(
            "bookcase", "bookshelf", "shelf", "rack", "sign", "plaque"
    );
    private static final Set<String> ARCHITECTURAL_BUILDING_TOKENS = Set.of(
            "stair", "stairs", "roof", "balcony", "bridge", "railing", "banister",
            "parapet", "platform", "path", "paving", "paver", "window", "pier"
    );
    private static final Set<String> WORKSTATION_TOKENS = Set.of(
            "station", "terminal", "controller", "assembler", "fabricator",
            "charger", "press", "mixer", "crusher", "grinder", "smelter",
            "refinery", "processor", "forge", "loom", "workbench", "stove", "oven"
    );
    private static final Set<String> CREATE_HANDHELD_TOOL_TOKENS = Set.of(
            "wrench", "grip", "worldshaper", "cannon"
    );
    private static final Set<String> CREATE_HANDHELD_UTILITY_TOKENS = Set.of(
            "goggle", "filter", "schedule", "shopping", "list", "schematic",
            "quill", "glue", "controller"
    );
    private static final Set<String> CREATE_PART_TOKENS = Set.of(
            "belt", "gearbox", "bracket", "pipe", "valve", "handle", "shaft",
            "cogwheel", "chute", "depot", "ejector", "speedometer", "stressometer",
            "casing"
    );
    private static final Set<String> CREATE_MACHINE_TOKENS = Set.of(
            "press", "burner", "wheel", "millstone", "mixer", "fan", "pump",
            "tank", "backtank", "engine"
    );
    private static final Set<String> RAILWAYS_TRANSPORT_TOKENS = Set.of(
            "track", "coupler", "conductor", "switch", "semaphore", "handcar"
    );
    private static final Set<String> RAILWAYS_PART_TOKENS = Set.of(
            "smokestack", "boiler", "buffer", "cowcatcher", "headstock", "link", "connector"
    );
    private static final Set<String> CREATE_ADDON_MACHINE_TOKENS = Set.of(
            "alternator", "motor", "generator", "charger", "accumulator", "battery"
    );
    private static final Set<String> CREATE_ADDON_PART_TOKENS = Set.of(
            "connector", "cable", "coil", "wire", "electrode"
    );
    private static final Set<String> CREATE_WINERY_MACHINE_TOKENS = Set.of(
            "barrel", "vat", "press", "ferment"
    );
    private static final Set<String> CREATE_ORE_MACHINE_TOKENS = Set.of(
            "drill", "pump", "extract", "boring", "survey"
    );
    private static final Set<String> CREATE_ORE_PART_TOKENS = Set.of(
            "pipe", "vein", "sample", "core"
    );
    private static final Set<String> FOOD_STORAGE_TOKENS = Set.of(
            "crate", "bag", "bale", "sack"
    );
    private static final Set<String> FOOD_COOKING_STATION_TOKENS = Set.of(
            "skillet", "stove", "cooking_pot"
    );
    private static final Set<String> CREATE_ENCHANTING_EXPERIENCE_TOKENS = Set.of(
            "experience", "hyper_experience", "nugget_of_experience", "nugget_of_super_experience"
    );
    private static final Set<String> PORTABLE_STORAGE_ARMOR_TOKENS = Set.of(
            "backpack", "satchel", "pouch"
    );
    private static final Set<String> PORTABLE_STORAGE_TECH_TOKENS = Set.of(
            "upgrade", "stack", "pump", "filter"
    );
    private static final Set<String> STORAGE_FAMILY_ROUTE_TOKENS = Set.of(
            "storage", "chest", "barrel", "drawer", "terminal", "drive", "cell", "disk",
            "interface", "importer", "exporter", "controller", "cable", "bus", "tank",
            "grid", "monitor", "manager", "manipulator", "constructor", "destructor",
            "detector", "relay", "transmitter", "receiver", "network", "silicon",
            "processor", "binding", "printed", "logic", "calculation", "engineering",
            "chip", "card", "module", "core", "quartz_enriched_iron", "upgrade",
            "pattern", "filter", "cover", "remote", "key", "keyring", "template"
    );
    private static final Set<String> STORAGE_CIRCUIT_TOKENS = Set.of(
            "silicon", "processor", "binding", "printed", "logic", "calculation",
            "engineering", "chip", "card", "module", "core"
    );
    private static final Set<String> STORAGE_MEDIA_PART_TOKENS = Set.of(
            "storage_part", "storage_disk", "fluid_storage_part", "fluid_storage_disk",
            "storage_housing", "cover"
    );
    private static final Set<String> STORAGE_NETWORK_PART_TOKENS = Set.of(
            "terminal", "interface", "importer", "exporter", "drive", "disk", "cell",
            "controller", "manager", "manipulator", "constructor", "destructor",
            "detector", "relay", "transmitter", "receiver", "network", "remote"
    );
    private static final Set<String> STORAGE_MACHINE_TOKENS = Set.of(
            "grid", "monitor", "wireless", "storage", "chest", "barrel", "drawer", "tank"
    );
    private static final Set<String> FOOD_FAMILY_INGREDIENT_TOKENS = Set.of(
            "crumb", "crumbs", "sugar", "butter", "chip", "chips", "flour", "cheese",
            "diced", "bean", "beans", "wrapper", "dough", "powder", "puree",
            "frosting", "piping_bag"
    );
    private static final Set<String> AUTOMATION_ROUTE_TOKENS = Set.of(
            "circuit", "transistor", "capacitor", "assembly", "wafer", "etch",
            "photomask", "tube", "valve", "module", "drone", "charger", "charging",
            "compressor", "chamber", "keycard", "codebreaker", "monitor", "modifier",
            "reinforcer", "remover", "changer", "sentry", "taser", "remote", "cable"
    );
    private static final Set<String> AUTOMATION_CIRCUIT_TOKENS = Set.of(
            "circuit", "transistor", "capacitor", "wafer", "etch", "photomask",
            "assembly", "keycard", "modifier", "reinforcer", "changer", "module"
    );
    private static final Set<String> AUTOMATION_PART_TOKENS = Set.of(
            "valve", "drone"
    );
    private static final Set<String> AUTOMATION_MACHINE_TOKENS = Set.of(
            "charger", "charging", "compressor", "chamber"
    );
    private static final Set<String> GREGTECH_CIRCUIT_TOKENS = Set.of(
            "circuit", "processor", "chip"
    );
    private static final Set<String> GREGTECH_MACHINE_TOKENS = Set.of(
            "machine", "hatch", "bus", "conveyor", "robot_arm", "emitter", "sensor",
            "regulator", "pump", "motor", "piston", "assembler", "macerator",
            "centrifuge", "electrolyzer", "compressor", "extractor", "furnace", "mixer",
            "canner", "lathe", "bender", "wiremill", "polarizer", "smelter", "reactor",
            "collector", "boiler", "crusher", "autoclave", "bath", "cutter",
            "distillery", "extruder", "solidifier", "press", "packer", "turbine",
            "miner", "brewery", "separator", "fermenter", "heater", "engraver",
            "sifter", "accelerator", "fisher", "scrubber", "breaker", "buffer"
    );
    private static final Set<String> GREGTECH_POWER_TOKENS = Set.of(
            "battery", "capacitor", "cable", "wire", "energy", "power", "generator",
            "dynamo", "transformer", "converter", "diode", "solar_panel", "voltage_coil"
    );
    private static final Set<String> GREGTECH_MATERIAL_TOKENS = Set.of(
            "ingot", "nugget", "dust", "plate", "rod", "bolt", "screw", "ring", "foil",
            "wire", "gear", "spring", "rotor", "gem", "ore", "crushed", "purified",
            "impure", "raw", "tiny", "small", "bucket", "indicator", "blade", "head",
            "tip", "lens", "wafer", "mold", "casing", "frame", "sheet", "studs", "dye",
            "can", "boule", "round"
    );
    private static final Set<String> APOTHEOSIS_ENCHANTING_TOKENS = Set.of(
            "shelf", "tome", "library", "ender_lead", "infused"
    );
    private static final Set<String> APOTHEOSIS_BOSS_TOKENS = Set.of(
            "boss"
    );
    private static final Set<String> APOTHEOSIS_SPAWNER_TOKENS = Set.of(
            "spawner", "spawn_egg"
    );
    private static final Set<String> APOTHEOSIS_SOCKET_TOKENS = Set.of(
            "socket", "potion_charm"
    );
    private static final Set<String> APOTHEOSIS_GEM_TOKENS = Set.of(
            "gem"
    );
    private static final Set<String> APOTHEOSIS_AFFIX_TOKENS = Set.of(
            "affix", "reforging", "salvaging", "augmenting", "sigil", "material",
            "smithing_template"
    );
    private static final Set<String> BOTANIA_MANA_TOKENS = Set.of(
            "mana", "mana_pool", "spreader", "spark", "alfheim_portal", "gaia_pylon",
            "natura_pylon", "mana_pylon", "mana_tablet", "mana_pearl", "mana_diamond",
            "mana_string", "manaweave", "pool"
    );
    private static final Set<String> BOTANIA_GENERATING_FLOWER_TOKENS = Set.of(
            "endoflame", "hydroangeas", "thermalily", "gourmaryllis", "munchdew",
            "rosa_arcana", "entropinnyum", "kekimurus", "narslimmus", "spectrolus",
            "dandelifeon", "shulk_me_not", "rafflowsia"
    );
    private static final Set<String> BOTANIA_FUNCTIONAL_FLOWER_TOKENS = Set.of(
            "pure_daisy", "agricarnation", "bellethorn", "bergamute", "bubbell",
            "clayconia", "daffomill", "dreadthorn", "exoflame", "fallen_kanade",
            "heisei_dream", "hopperhock", "hyacidus", "jaded_amaranthus", "labellia",
            "loonium", "marimorphosis", "medumone", "pollidisiac", "rannuncarpus",
            "solegnolia", "spectranthemum", "tangleberrie", "tigerseye", "vinculotus",
            "orechid", "orechid_ignem"
    );
    private static final Set<String> BOTANIA_BAUBLE_TOKENS = Set.of(
            "ring", "band", "amulet", "pendant", "belt", "sash", "tiara", "cloak",
            "bauble", "charm", "flugel_eye", "monocle"
    );
    private static final Set<String> BOTANIA_TOOL_TOKENS = Set.of(
            "wand", "rod", "lens", "sword", "bow", "pickaxe", "axe", "shovel", "hoe",
            "terraformer", "horn", "drum", "magnet", "brewer"
    );
    private static final Set<String> BOTANIA_MATERIAL_TOKENS = Set.of(
            "petal", "mystical_flower", "livingwood", "livingrock", "dreamwood",
            "manasteel", "terrasteel", "elementium", "gaia", "pixie_dust", "dragonstone",
            "mana_powder", "mana_dust", "quartz", "shimmerrock", "shimmerwood"
    );
    private static final Set<String> WAYSTONES_REAGENT_TOKENS = Set.of(
            "dust", "shard"
    );
    private static final Set<String> WAYSTONES_ARTIFACT_TOKENS = Set.of(
            "waystone", "portstone", "sharestone", "warp_plate", "warp_stone", "scroll"
    );
    private static final Set<String> GEOLOGY_SURFACE_ORGANIC_TOKENS = Set.of(
            "nylium", "mycelium", "moss", "lichen", "fungi", "fungus", "roots", "stem"
    );
    private static final Set<String> GEOLOGY_FUNGI_TOKENS = Set.of(
            "nylium", "mycelium", "fungi", "fungus"
    );
    private static final Set<String> GEOLOGY_DECORATION_TOKENS = Set.of(
            "window", "desk"
    );
    private static final Set<String> GEOLOGY_MASONRY_TOKENS = Set.of(
            "stairs", "stair", "slab", "wall", "fence", "pane", "door", "trapdoor",
            "brick", "bricks", "tile", "paver", "paving", "beam", "stripe", "stripes", "square",
            "pattern", "dented", "weathered", "plank", "board", "pillar", "column",
            "polished", "chiseled", "carved", "cut", "railing", "banister"
    );
    private static final Set<String> GEOLOGY_STONE_DECORATION_TOKENS = Set.of(
            "brick", "polished", "tile", "glass", "window", "plank", "board", "pillar",
            "column", "paver", "paving", "chiseled", "carved"
    );
    private static final Set<String> GEOLOGY_SOIL_DECORATION_TOKENS = Set.of(
            "terracotta", "concrete", "nylium", "mycelium", "moss", "fungi", "fungus"
    );
    private static final List<PrimaryRule> PRIMARY_RULES = List.of(
            rule("compat route metadata",
                    PrimaryCategoryResolver::shouldUseCompatRouteMetadata,
                    c -> assignment(
                            c.attributes.get(SearchNodeKeys.COMPAT_ROUTE_CATEGORY),
                            c.attributes.get(SearchNodeKeys.COMPAT_ROUTE_SUBCATEGORY),
                            c.attributes)),
            rule("create handheld tools",
                    c -> shouldBiasCreateFamilyHandheldToTools(c.modFamily, c.path),
                    c -> assignment("tools", classifyCreateFamilyToolSubcategory(c.path), c.attributes)),
            rule("create handheld utility",
                    c -> shouldBiasCreateFamilyHandheldToUtility(c.modFamily, c.path),
                    c -> assignment("utility", "misc", c.attributes)),
            rule("create enchanting magic",
                    c -> shouldBiasCreateEnchantingFamilyToMagic(c.modId, c.path),
                    c -> assignment("magic", "reagents", c.attributes)),
            rule("spawn eggs and mob buckets",
                    c -> hasAny(c.facets, ItemFacet.SPAWN_EGG, ItemFacet.MOB_BUCKET),
                    c -> assignment("bestiary", classifyBestiarySubcategory(c.path), c.attributes)),
            rule("saplings",
                    c -> isSapling(c.path, c.attributes),
                    c -> assignment("nature", "seeds", c.attributes)),
            rule("leaves",
                    c -> isLeaves(c.path, c.facets, c.attributes),
                    c -> assignment("nature", "flora", c.attributes)),
            rule("wood blocks",
                    c -> isWoodBlock(c.path, c.facets, c.attributes),
                    c -> assignment("nature", "wood", c.attributes)),
            rule("plant seeds",
                    c -> isPlantSeed(c.path, c.facets, c.attributes),
                    c -> assignment("nature", "seeds", c.attributes)),
            rule("crop-like placeables",
                    c -> isCropLikePlaceable(c.path, c.facets, c.attributes),
                    c -> assignment("nature", "crops", c.attributes)),
            rule("food family placeables",
                    c -> shouldBiasFoodFamilyPlaceableToNature(c.modFamily, c.facets, c.path, c.attributes),
                    c -> assignment("nature", classifyFoodFamilyNatureSubcategory(c.path, c.facets), c.attributes)),
            rule("magic facets",
                    c -> hasAny(c.facets, ItemFacet.POTION, ItemFacet.ENCHANTED_BOOK, ItemFacet.MAGIC_ARTIFACT, ItemFacet.MAGIC_REAGENT),
                    c -> assignment("magic", classifyMagicSubcategory(c.facets), c.attributes)),
            rule("utility facets",
                    c -> hasAny(c.facets, ItemFacet.UTILITY_NAVIGATION, ItemFacet.UTILITY_MEDICAL, ItemFacet.UTILITY_CURRENCY,
                            ItemFacet.BOOK, ItemFacet.GUIDE_BOOK, ItemFacet.UTILITY_MISC, ItemFacet.FLUID_CONTAINER),
                    c -> assignment("utility", classifyUtilitySubcategory(c.facets), c.attributes)),
            rule("clear ingredients before incidental equipment or tech",
                    c -> shouldResolveIngredientBeforeEquipmentTech(c.facets),
                    c -> assignment("ingredients", classifyIngredientSubcategory(c.facets), c.attributes)),
            rule("armor and real curios",
                    c -> shouldResolveAsArmorOrCurio(c.facets),
                    c -> assignment("armor", classifyArmorSubcategory(c.facets, c.attributes), c.attributes)),
            rule("throwable ingredients",
                    c -> isThrowableIngredient(c.facets),
                    c -> assignment("ingredients", classifyIngredientSubcategory(c.facets), c.attributes)),
            rule("tools and weapons",
                    c -> hasAny(c.facets, ItemFacet.MELEE_WEAPON, ItemFacet.RANGED_WEAPON, ItemFacet.HARVEST_TOOL, ItemFacet.UTILITY_TOOL)
                            || (c.facets.contains(ItemFacet.PROJECTILE) && hasProjectileToolContext(c.path, c.attributes)),
                    c -> assignment("tools", classifyWeaponSubcategory(c.facets), c.attributes)),
            rule("food before passive redstone",
                    c -> shouldResolveFoodLikeBeforePassiveRedstone(c.modFamily, c.facets, c.path),
                    c -> assignment("nature", classifyFoodFamilyPreparedSubcategory(c.path, c.facets), c.attributes)),
            rule("decoration before passive redstone",
                    c -> shouldResolveDecorLikeBeforePassiveRedstone(c.modFamily, c.facets, c.attributes),
                    c -> assignment("decoration", classifyDecorationSubcategory(c.facets), c.attributes)),
            rule("natural cable false positives",
                    c -> shouldResolveNaturalBeforeTech(c.facets, c.path),
                    c -> assignment("nature", classifyNatureSubcategory(c.path, c.facets), c.attributes)),
            rule("tech facets",
                    c -> hasAny(c.facets,
                            ItemFacet.HAS_ENERGY,
                            ItemFacet.STORAGE,
                            ItemFacet.INTERACTIVE_BLOCK,
                            ItemFacet.ACTIVE_REDSTONE_LOGIC,
                            ItemFacet.PASSIVE_COMPARATOR_OUTPUT,
                            ItemFacet.REDSTONE_LOGIC,
                            ItemFacet.REDSTONE_SIGNAL,
                            ItemFacet.TRANSPORT,
                            ItemFacet.MACHINE,
                            ItemFacet.WORKSTATION,
                            ItemFacet.CABLE,
                            ItemFacet.UPGRADE,
                            ItemFacet.TEMPLATE,
                            ItemFacet.TECH_COMPONENT,
                            ItemFacet.MECHANICAL_COMPONENT,
                            ItemFacet.INGOT,
                            ItemFacet.GEM,
                            ItemFacet.NUGGET,
                            ItemFacet.RAW_MATERIAL,
                            ItemFacet.DUST),
                    c -> assignment("tech", classifyTechSubcategory(c.path, c.facets), c.attributes)),
            rule("nature facets",
                    c -> hasAny(c.facets, ItemFacet.EDIBLE, ItemFacet.PLACEABLE_FOOD, ItemFacet.FOOD_MEAL, ItemFacet.FOOD_DRINK, ItemFacet.FOOD_PROTEIN, ItemFacet.COMPOSTABLE, ItemFacet.SEED, ItemFacet.CROP, ItemFacet.NATURE_MISC, ItemFacet.FUNGI, ItemFacet.LOG, ItemFacet.LEAVES, ItemFacet.FLOWER),
                    c -> assignment("nature", classifyNatureSubcategory(c.path, c.facets), c.attributes)),
            rule("ingredient facets",
                    c -> hasAny(c.facets, ItemFacet.INGREDIENT_ORGANIC, ItemFacet.INGREDIENT_MINERAL, ItemFacet.INGREDIENT_DYE),
                    c -> assignment("ingredients", classifyIngredientSubcategory(c.facets), c.attributes)),
            rule("structural building shapes",
                    c -> c.facets.contains(ItemFacet.PLACEABLE)
                            && hasAny(c.facets,
                            ItemFacet.STAIRS,
                            ItemFacet.SLAB,
                            ItemFacet.WALL,
                            ItemFacet.FENCE,
                            ItemFacet.FENCE_GATE,
                            ItemFacet.PANE,
                            ItemFacet.DOOR,
                            ItemFacet.TRAPDOOR),
                    c -> assignment("masonry", classifyMasonrySubcategory(c.facets, c.path, c.attributes), c.attributes)),
            rule("decoration facets",
                    c -> shouldResolveDecorationFacetPrimary(c.path, c.facets, c.attributes),
                    c -> assignment("decoration", classifyDecorationSubcategory(c.facets), c.attributes)),
            rule("social facets",
                    c -> hasAny(c.facets, ItemFacet.SOCIAL_PLAYERS, ItemFacet.SOCIAL_CLAIMS),
                    c -> assignment("social", classifySocialSubcategory(c.facets), c.attributes)),
            rule("architectural placeables",
                    c -> shouldBiasArchitecturalPlaceableToBuilding(c.facets, c.path, c.attributes),
                    c -> assignment("masonry", classifyMasonrySubcategory(c.facets, c.path, c.attributes), c.attributes)),
            rule("lexical decoration",
                    c -> shouldBiasLexicalDecoration(c.facets, c.path),
                    c -> assignment("decoration", classifyLexicalDecorationSubcategory(c.path, c.facets), c.attributes)),
            rule("lexical workstation tech",
                    c -> shouldBiasLexicalWorkstationToTech(c.facets, c.path),
                    c -> assignment("tech", classifyLexicalTechSubcategory(c.path, c.facets), c.attributes)),
            rule("food family ingredients",
                    c -> shouldBiasFoodFamilyToIngredients(c.modFamily, c.facets, c.path),
                    c -> assignment("ingredients", classifyFoodFamilyIngredientSubcategory(c.path, c.facets), c.attributes)),
            rule("food family prepared food",
                    c -> shouldBiasFoodFamilyToPreparedFood(c.modFamily, c.facets, c.path),
                    c -> assignment("nature", classifyFoodFamilyPreparedSubcategory(c.path, c.facets), c.attributes)),
            rule("decor family decoration",
                    c -> shouldBiasDecorFamilyToDecoration(c.modFamily, c.facets, c.path, c.attributes),
                    c -> assignment("decoration", classifyDecorationSubcategory(c.facets), c.attributes)),
            rule("portable storage armor",
                    c -> shouldBiasPortableStorageFamilyToArmor(c.modFamily, c.path),
                    c -> assignment("armor", "curios", c.attributes)),
            rule("portable storage upgrades",
                    c -> shouldBiasPortableStorageFamilyToTech(c.modFamily, c.path),
                    c -> assignment("tech", "upgrades", c.attributes)),
            rule("storage family tech",
                    c -> shouldBiasStorageFamilyToTech(c.modFamily, c.facets, c.path, c.attributes),
                    c -> assignment("tech", classifyStorageSubcategory(c.path, c.facets), c.attributes)),
            rule("automation family tech",
                    c -> shouldBiasAutomationFamilyToTech(c.modFamily, c.facets, c.path, c.attributes),
                    c -> assignment("tech", classifyAutomationSubcategory(c.path, c.facets), c.attributes)),
            rule("create family decoration",
                    c -> shouldBiasCreateFamilyToDecoration(c.modFamily, c.facets),
                    c -> assignment("decoration", classifyDecorationSubcategory(c.facets), c.attributes)),
            rule("create family tech",
                    c -> shouldBiasCreateFamilyToTech(c.modFamily, c.facets, c.path, c.attributes),
                    c -> assignment("tech", classifyCreateFamilyTechSubcategory(c.modId, c.path, c.facets), c.attributes)),
            rule("food family nature",
                    c -> shouldBiasFoodFamilyToNature(c.modFamily, c.facets, c.path, c.attributes),
                    c -> assignment("nature", classifyFoodFamilyNatureSubcategory(c.path, c.facets), c.attributes)),
            rule("organic surface blocks",
                    c -> shouldBiasOrganicSurfaceBlockToNature(c.facets, c.path, c.attributes),
                    c -> assignment("nature", classifyOrganicSurfaceBlockSubcategory(c.path, c.facets), c.attributes)),
            rule("geology family decoration",
                    c -> shouldBiasGeologyFamilyToDecoration(c.facets, c.path, c.attributes),
                    c -> assignment("decoration", classifyDecorationSubcategory(c.facets), c.attributes)),
            rule("geology family masonry",
                    c -> shouldBiasGeologyFamilyToMasonry(c.facets, c.path, c.attributes),
                    c -> assignment("masonry", classifyMasonrySubcategory(c.facets, c.path, c.attributes), c.attributes)),
            rule("geology blocks",
                    c -> shouldBeGeology(c.facets, c.path, c.attributes),
                    c -> assignment("geology", c.facets.contains(ItemFacet.SOIL_BLOCK) ? "terrain" : "stone", c.attributes)),
            rule("uncraftable terrain blocks",
                    c -> shouldBiasUncraftableFullBlockToTerrain(c.facets, c.attributes),
                    c -> assignment("geology", classifyUncraftableTerrainSubcategory(c.facets, c.attributes), c.attributes)),
            rule("partial functional micro placeables",
                    c -> isLikelyFunctionalPartialPlaceable(c.facets, c.path, c.attributes),
                    c -> assignment("tech", "machines", c.attributes)),
            rule("partial nature placeables",
                    c -> isLikelyNaturePartialPlaceable(c.facets, c.path, c.attributes),
                    c -> assignment("nature", classifyNatureSubcategory(c.path, c.facets), c.attributes)),
            rule("micro partial placeables",
                    c -> isLikelyDecorativeMicroPlaceable(c.facets, c.path, c.attributes),
                    c -> assignment("decoration", "other_building", c.attributes)),
            rule("partial placeables",
                    c -> isLikelyPartialBuildingPlaceable(c.facets, c.path, c.attributes),
                    c -> assignment("decoration", "other_building", c.attributes)),
            rule("remaining placeables",
                    c -> c.facets.contains(ItemFacet.PLACEABLE) && isLikelyFullMasonryCandidate(c.facets, c.path, c.attributes),
                    c -> assignment("masonry", classifyMasonrySubcategory(c.facets, c.path, c.attributes), c.attributes))
    );

    private PrimaryCategoryResolver() {
    }

    public static CategoryAssignment resolve(ResourceLocation id, FacetProfile profile) {
        return resolve(id,
                profile == null ? Set.of() : profile.facets(),
                profile == null ? Map.of() : profile.attributes());
    }

    public static CategoryAssignment resolve(ResourceLocation id, Set<ItemFacet> profileFacets,
                                             Map<String, String> profileAttributes) {
        /*
         * Classification routing rule:
         * - compat family ownership, semantic ontology, and family enrichment are separate decisions.
         * - item path terms and foreign mod tags may refine an already-owned family item, but must not
         *   establish ownership or override semantic category by themselves.
         * - strong semantic identities such as swords, armor, ingots, ores, storage, food, and stone
         *   should remain discoverable in their normal ontology even when owned by a compat family.
         *
         * Keep docs/classification-routing-log.md updated before changing these gates; it records
         * known failed approaches such as omega->mega, Cobblemon tags claiming vanilla shulkers,
         * Create terms claiming AE2 presses/GTCEu casings, and andesite becoming Create-owned.
         */
        if (id == null) {
            return fallback();
        }

        String modId = id.getNamespace().toLowerCase(Locale.ROOT);
        String path = id.getPath().toLowerCase(Locale.ROOT);
        var facets = profileFacets == null || profileFacets.isEmpty()
                ? EnumSet.noneOf(ItemFacet.class)
                : EnumSet.copyOf(profileFacets);
        var attributes = new HashMap<>(profileAttributes == null ? Map.of() : profileAttributes);
        java.util.Optional<ClassificationOverride> itemOverride = ClassificationOverrides.forItem(id);
        if (itemOverride.isPresent()) {
            ClassificationOverride o = itemOverride.get();
            facets.addAll(o.addFacets());
            facets.removeAll(o.removeFacets());
            if (!o.addFacets().isEmpty() || !o.removeFacets().isEmpty()) {
                attributes.put(SearchNodeKeys.FACETS, FacetCodec.encode(facets));
            }
        }
        PrimaryCategoryModFamily modFamily = PrimaryCategoryModFamilies.classify(modId);
        AmiConfig.CompatCategoryPolicy categoryPolicy = CompatCategoryPolicyResolver.resolve(attributes);
        if (hasCompatFamily(attributes)) {
            attributes.put(SearchNodeKeys.COMPAT_CATEGORY_POLICY, categoryPolicy.name().toLowerCase(Locale.ROOT));
        }
        FacetProfile routedProfile = new FacetProfile(facets, attributes);
        String candidateSummary = CategoryScorer.candidateSummary(id, routedProfile);
        if (!candidateSummary.isBlank()) {
            attributes.put(SearchNodeKeys.CLASSIFICATION_CANDIDATES, candidateSummary);
        }
        ResolveContext context = new ResolveContext(id, modId, path, facets, attributes, modFamily, categoryPolicy);
        CategoryRouteTrace route = CategoryRouteTrace.start(id, modFamily.name().toLowerCase(Locale.ROOT), facets, attributes);

        if (itemOverride.isPresent() && itemOverride.get().hasForcedCategory()) {
            ClassificationOverride o = itemOverride.get();
            return route.finish("classification_override", "item_override",
                    new CategoryAssignment(o.forceCategory(), o.subcategoryOrEmpty(), attributes));
        }
        Optional<ModPatternRule> patternRule = ClassificationOverrides.patternFor(modId, path);
        if (patternRule.isPresent()) {
            ModPatternRule r = patternRule.get();
            return route.finish("classification_override", "mod_pattern",
                    new CategoryAssignment(r.category(), r.subcategory(), attributes));
        }
        route.skipped("classification_override", "no override matched");

        if (shouldUseEarlyCompatRouteMetadata(context)) {
            return route.finish("compat_route", "explicit metadata", compatRouteAssignment(context));
        }
        route.skipped("compat_route", "no early explicit compat route");

        Optional<CategoryAssignment> hardIdentity = resolveHardIdentity(context);
        if (hardIdentity.isPresent()) {
            return route.finish("hard_identity", "identity", hardIdentity.get());
        }
        route.skipped("hard_identity", "no hard identity matched");

        Optional<CategoryAssignment> scored = CategoryScorer.resolveStrong(id, routedProfile);
        if (scored.isPresent()) {
            return route.finish("evidence_strong", "category_scorer", scored.get());
        }
        route.skipped("evidence_strong", "no strong evidence winner");

        for (PrimaryRule rule : PRIMARY_RULES) {
            if (rule.matches.test(context)) {
                return route.finish("primary_rule", rule.id(), rule.assignment().apply(context));
            }
            route.skipped("primary_rule:" + rule.id(), "predicate false");
        }
        Optional<CategoryAssignment> fallbackScored = CategoryScorer.resolve(id, routedProfile);
        if (fallbackScored.isPresent()) {
            return route.finish("evidence_fallback", "category_scorer", fallbackScored.get());
        }
        route.skipped("evidence_fallback", "no fallback evidence winner");
        Optional<CategoryAssignment> compatFallback = resolveCompatUnknownFallback(context);
        if (compatFallback.isPresent()) {
            return route.finish("compat_fallback", "recognized_compat_kind", compatFallback.get());
        }
        route.skipped("compat_fallback", "no recognized compat fallback");
        return route.finish("fallback", "unknown", fallback());
    }

    private static Optional<CategoryAssignment> resolveVanillaIdentity(ResolveContext context) {
        // Ore blocks: c:ores is a concrete runtime tag on all ore blocks.
        // Checked before CategoryScorer so redstone_ore (which also has redstone path tokens)
        // and normal ores (blocksMaterial=other_building) both land in geology/stone.
        if (PrimaryCategoryTextMatchers.hasMetadataToken(context.attributes, SearchNodeKeys.TAGS, "c:ores")
                || PrimaryCategoryTextMatchers.hasMetadataToken(context.attributes, SearchNodeKeys.BLOCK_TAGS, "c:ores")) {
            return Optional.of(identityAssignment(
                    "geology", "stone", context.attributes,
                    "identity.ore_block", "c:ores tag"));
        }

        // Magic structure blocks: FacetIndexer sets MAGIC_ARTIFACT on specific block classes.
        // Using the facet lets mod-added magic structures benefit from the same routing.
        if (context.facets.contains(ItemFacet.MAGIC_ARTIFACT) && context.facets.contains(ItemFacet.PLACEABLE)) {
            return Optional.of(identityAssignment("magic", "artifacts", context.attributes,
                    "identity.magic_structure_block", "magic artifact block"));
        }

        if (!"minecraft".equals(context.modId)) {
            return Optional.empty();
        }

        return switch (context.path) {
            case "jukebox" ->
                    Optional.of(identityAssignment("decoration", "furniture", context.attributes,
                            "identity.vanilla.jukebox", "vanilla jukebox"));
            case "spawner" ->
                    Optional.of(identityAssignment("bestiary", "hostile", context.attributes,
                            "identity.vanilla.spawner", "vanilla monster spawner"));
            default -> {
                // Vanilla workstations: machine+workstation+placeable → tech/machines.
                // Excludes lectern (book-stand furniture, not a processing station).
                if (context.facets.contains(ItemFacet.WORKSTATION)
                        && context.facets.contains(ItemFacet.MACHINE)
                        && context.facets.contains(ItemFacet.PLACEABLE)
                        && !context.path.equals("lectern")) {
                    yield Optional.of(identityAssignment("tech", "machines", context.attributes,
                            "identity.vanilla.workstation", "vanilla workstation block"));
                }
                yield Optional.empty();
            }
        };
    }

    private static Optional<CategoryAssignment> resolveHardIdentity(ResolveContext context) {
        Optional<CategoryAssignment> vanilla = resolveVanillaIdentity(context);
        if (vanilla.isPresent()) {
            return vanilla;
        }

        if (hasAny(context.facets, ItemFacet.SPAWN_EGG, ItemFacet.MOB_BUCKET)) {
            return Optional.of(identityAssignment(
                    "bestiary",
                    classifyBestiarySubcategory(context.path),
                    context.attributes,
                    "identity.spawn_egg",
                    "spawn egg or mob bucket facet"
            ));
        }

        Optional<CategoryAssignment> cobblemon = resolveCobblemonIdentity(context);
        if (cobblemon.isPresent()) {
            return cobblemon;
        }

        Optional<CategoryAssignment> create = resolveCreateIdentity(context);
        if (create.isPresent()) {
            return create;
        }

        Optional<CategoryAssignment> ae2 = resolveAe2Identity(context);
        if (ae2.isPresent()) {
            return ae2;
        }

        Optional<CategoryAssignment> mekanism = resolveMekanismIdentity(context);
        if (mekanism.isPresent()) {
            return mekanism;
        }

        Optional<CategoryAssignment> gregTech = resolveGregTechIdentity(context);
        if (gregTech.isPresent()) {
            return gregTech;
        }

        Optional<CategoryAssignment> apotheosis = resolveApotheosisIdentity(context);
        if (apotheosis.isPresent()) {
            return apotheosis;
        }

        Optional<CategoryAssignment> botania = resolveBotaniaIdentity(context);
        if (botania.isPresent()) {
            return botania;
        }

        Optional<CategoryAssignment> sophisticated = resolveSophisticatedIdentity(context);
        if (sophisticated.isPresent()) {
            return sophisticated;
        }

        Optional<CategoryAssignment> storageFamily = resolveStorageFamilyTechIdentity(context);
        if (storageFamily.isPresent()) {
            return storageFamily;
        }

        Optional<CategoryAssignment> modularGear = resolveModularGearIdentity(context);
        if (modularGear.isPresent()) {
            return modularGear;
        }

        Optional<CategoryAssignment> modularGolems = resolveModularGolemsIdentity(context);
        if (modularGolems.isPresent()) {
            return modularGolems;
        }

        Optional<CategoryAssignment> society = resolveSocietyIdentity(context);
        if (society.isPresent()) {
            return society;
        }

        Optional<CategoryAssignment> mna = resolveMnaIdentity(context);
        if (mna.isPresent()) {
            return mna;
        }

        Optional<CategoryAssignment> arsNouveau = resolveArsNouveauIdentity(context);
        if (arsNouveau.isPresent()) {
            return arsNouveau;
        }

        Optional<CategoryAssignment> spectrum = resolveSpectrumIdentity(context);
        if (spectrum.isPresent()) {
            return spectrum;
        }

        Optional<CategoryAssignment> naturesAura = resolveNaturesAuraIdentity(context);
        if (naturesAura.isPresent()) {
            return naturesAura;
        }

        Optional<CategoryAssignment> alexsMobs = resolveAlexsMobsIdentity(context);
        if (alexsMobs.isPresent()) {
            return alexsMobs;
        }

        Optional<CategoryAssignment> alexsCaves = resolveAlexsCavesIdentity(context);
        if (alexsCaves.isPresent()) {
            return alexsCaves;
        }

        Optional<CategoryAssignment> tacz = resolveTaczIdentity(context);
        if (tacz.isPresent()) {
            return tacz;
        }

        Optional<CategoryAssignment> waystones = resolveWaystonesIdentity(context);
        if (waystones.isPresent()) {
            return waystones;
        }

        if (shouldResolveFoodFamilyCookingStation(context)) {
            return Optional.of(identityAssignment(
                    "tech",
                    "machines",
                    context.attributes,
                    "identity.food_cooking_station",
                    "food-family cooking station or vessel"
            ));
        }

        if (context.facets.contains(ItemFacet.DECORATIVE_BLOCK)
                && PrimaryCategoryTextMatchers.containsPathToken(context.path, TEXTILE_TOKENS)) {
            return Optional.of(identityAssignment(
                    "decoration",
                    "textiles",
                    context.attributes,
                    "identity.decorative_textile",
                    "decorative textile block identity"
            ));
        }

        if (shouldResolveAsArmorOrCurio(context.facets) || isNonPlayerArmorClass(context.attributes)) {
            return Optional.of(identityAssignment(
                    "armor",
                    classifyArmorSubcategory(context.facets, context.attributes),
                    context.attributes,
                    "identity.armor",
                    "armor, equipment slot, or curio facet"
            ));
        }

        if (context.facets.contains(ItemFacet.GUIDE_BOOK)) {
            return Optional.of(identityAssignment(
                    "utility",
                    "books",
                    context.attributes,
                    "identity.guide_book",
                    "guide book facet"
            ));
        }

        if (hasHardToolIdentity(context.facets)) {
            return Optional.of(identityAssignment(
                    "tools",
                    classifyWeaponSubcategory(context.facets),
                    context.attributes,
                    "identity.tool",
                    "non-projectile tool or weapon facet"
            ));
        }

        if (shouldResolveEdibleMagicReagentBeforeFood(context)) {
            return Optional.of(identityAssignment(
                    "magic",
                    "reagents",
                    context.attributes,
                    "identity.edible_magic_reagent",
                    "edible brewing reagent identity"
            ));
        }

        if (context.facets.contains(ItemFacet.UTILITY_MISC)
                && PrimaryCategoryTextMatchers.hasMetadataToken(context.attributes, SearchNodeKeys.TAGS, "c:drinks/ominous")) {
            return Optional.of(identityAssignment(
                    "utility",
                    "misc",
                    context.attributes,
                    "identity.ominous_bottle",
                    "bad-omen effect item, not nutritional food"
            ));
        }

        if (hasActualFoodIdentity(context.facets, context.attributes)) {
            return Optional.of(identityAssignment(
                    "nature",
                    classifyNatureSubcategory(context.path, context.facets),
                    context.attributes,
                    "identity.food",
                    "food data component or edible facet"
            ));
        }

        if (isSapling(context.path, context.attributes)) {
            return Optional.of(identityAssignment(
                    "nature",
                    "seeds",
                    context.attributes,
                    "identity.sapling",
                    "sapling block identity"
            ));
        }

        if (isLeaves(context.path, context.facets, context.attributes)) {
            return Optional.of(identityAssignment(
                    "nature",
                    "flora",
                    context.attributes,
                    "identity.flora",
                    "leaves block identity"
            ));
        }

        if (isWoodBlock(context.path, context.facets, context.attributes)) {
            return Optional.of(identityAssignment(
                    "nature",
                    "wood",
                    context.attributes,
                    "identity.wood",
                    "log or wood block identity"
            ));
        }

        if (isPlantSeed(context.path, context.facets, context.attributes)) {
            return Optional.of(identityAssignment(
                    "nature",
                    "seeds",
                    context.attributes,
                    "identity.seed",
                    "plant seed identity"
            ));
        }

        if (isCropLikePlaceable(context.path, context.facets, context.attributes)) {
            return Optional.of(identityAssignment(
                    "nature",
                    "crops",
                    context.attributes,
                    "identity.crop",
                    "crop block identity"
            ));
        }

        if (context.facets.contains(ItemFacet.PLACEABLE)
                && hasAny(context.facets,
                ItemFacet.STAIRS,
                ItemFacet.SLAB,
                ItemFacet.WALL,
                ItemFacet.FENCE,
                ItemFacet.FENCE_GATE,
                ItemFacet.PANE,
                ItemFacet.DOOR,
                ItemFacet.TRAPDOOR)) {
            return Optional.of(identityAssignment(
                    "masonry",
                    classifyMasonrySubcategory(context.facets, context.path, context.attributes),
                    context.attributes,
                    "identity.block_shape",
                    "structural block shape facet"
            ));
        }

        return Optional.empty();
    }

    private static Optional<CategoryAssignment> resolveWaystonesIdentity(ResolveContext context) {
        if (!"waystones".equals(context.modId)
                && !PrimaryCategoryTextMatchers.containsAnyIgnoreCase(context.attributes.getOrDefault(SearchNodeKeys.ITEM_CLASS, ""), "net.blay09.mods.waystones.")
                && !PrimaryCategoryTextMatchers.containsAnyIgnoreCase(context.attributes.getOrDefault(SearchNodeKeys.BLOCK_CLASS, ""), "net.blay09.mods.waystones.")
                && !PrimaryCategoryTextMatchers.hasMetadataToken(context.attributes, SearchNodeKeys.BLOCK_TAGS, "waystones:is_teleport_target")) {
            return Optional.empty();
        }
        PathTokens pathTokens = PathTokens.of(context.path);

        if (pathTokens.containsAny(WAYSTONES_REAGENT_TOKENS)
                || PrimaryCategoryTextMatchers.containsAnyIgnoreCase(context.attributes.getOrDefault(SearchNodeKeys.ITEM_CLASS, ""), "ShardItem", "WarpDustItem")) {
            return Optional.of(identityAssignment(
                    "magic",
                    "reagents",
                    context.attributes,
                    "identity.waystones.reagent",
                    "Waystones teleport reagent"
            ));
        }

        if (PrimaryCategoryTextMatchers.hasMetadataToken(context.attributes, SearchNodeKeys.BLOCK_TAGS, "waystones:is_teleport_target")
                || pathTokens.containsAny(WAYSTONES_ARTIFACT_TOKENS)
                || PrimaryCategoryTextMatchers.containsAnyIgnoreCase(context.attributes.getOrDefault(SearchNodeKeys.ITEM_CLASS, ""), "WarpStoneItem", "ScrollItem")
                || PrimaryCategoryTextMatchers.containsAnyIgnoreCase(context.attributes.getOrDefault(SearchNodeKeys.BLOCK_CLASS, ""), "WaystoneBlock", "PortstoneBlock", "SharestoneBlock", "WarpPlateBlock")) {
            return Optional.of(identityAssignment(
                    "magic",
                    "artifacts",
                    context.attributes,
                    "identity.waystones.teleport",
                    "Waystones teleport target or item"
            ));
        }

        return Optional.empty();
    }

    private static Optional<CategoryAssignment> resolveCobblemonIdentity(ResolveContext context) {
        if (!"cobblemon".equals(context.modId)
                && !CompatFamilyDetector.isPrimaryFamily(context.attributes, CompatFamilyDetector.COBBLEMON)) {
            return Optional.empty();
        }
        if (context.categoryPolicy == AmiConfig.CompatCategoryPolicy.SEMANTIC) {
            return Optional.empty();
        }
        String kind = context.attributes.getOrDefault(SearchNodeKeys.COBBLEMON_ITEM_KIND, "");
        if (kind.isBlank()) {
            return Optional.empty();
        }
        if (context.categoryPolicy == AmiConfig.CompatCategoryPolicy.HYBRID && !isFocusedCobblemonKind(kind)) {
            return Optional.empty();
        }

        return Optional.of(switch (kind) {
            case "poke_ball" -> identityAssignment(
                    "cobblemon", "poke_balls", context.attributes,
                    "identity.cobblemon.poke_ball", "Cobblemon capture item");
            case "medicine", "status_cure", "vitamin", "mint", "mochi" -> identityAssignment(
                    "cobblemon", "medicine", context.attributes,
                    "identity.cobblemon.medicine", "Cobblemon medicine or consumable");
            case "berry" -> identityAssignment(
                    "cobblemon", "berries", context.attributes,
                    "identity.cobblemon.berry", "Cobblemon berry");
            case "apricorn", "apricorn_seed" -> identityAssignment(
                    "cobblemon", "apricorns", context.attributes,
                    "identity.cobblemon.apricorn", "Cobblemon apricorn");
            case "evolution_item" -> identityAssignment(
                    "cobblemon", "evolution", context.attributes,
                    "identity.cobblemon.evolution", "Cobblemon evolution item");
            case "fossil" -> identityAssignment(
                    "cobblemon", "fossils", context.attributes,
                    "identity.cobblemon.fossil", "Cobblemon fossil or archaeology item");
            case "machine" -> identityAssignment(
                    "cobblemon", "machines", context.attributes,
                    "identity.cobblemon.machine", "Cobblemon machine or station");
            case "decor" -> identityAssignment(
                    "cobblemon", "decor", context.attributes,
                    "identity.cobblemon.decor", "Cobblemon display or decor item");
            case "transport" -> identityAssignment(
                    "cobblemon", "transport", context.attributes,
                    "identity.cobblemon.transport", "Cobblemon transport item");
            case "held_item" -> identityAssignment(
                    "cobblemon", "held_items", context.attributes,
                    "identity.cobblemon.held_item", "Cobblemon held item");
            case "utility_item" -> identityAssignment(
                    "cobblemon", "utility", context.attributes,
                    "identity.cobblemon.utility", "Cobblemon utility item");
            case "consumable" -> identityAssignment(
                    "cobblemon", "consumables", context.attributes,
                    "identity.cobblemon.consumable", "Cobblemon consumable");
            case "agriculture" -> identityAssignment(
                    "cobblemon", "agriculture", context.attributes,
                    "identity.cobblemon.agriculture", "Cobblemon agriculture item");
            case "building" -> identityAssignment(
                    "cobblemon", "building", context.attributes,
                    "identity.cobblemon.building", "Cobblemon building block");
            case "archaeology" -> identityAssignment(
                    "cobblemon", "archaeology", context.attributes,
                    "identity.cobblemon.archaeology", "Cobblemon archaeology item");
            case "tm" -> identityAssignment(
                    "cobblemon", "tms", context.attributes,
                    "identity.cobblemon.tm", "Cobblemon TM or TR");
            default -> identityAssignment(
                    "cobblemon", "misc", context.attributes,
                    "identity.cobblemon", "Cobblemon item");
        });
    }

    private static boolean isFocusedCobblemonKind(String kind) {
        return switch (kind) {
            case "poke_ball", "medicine", "status_cure", "vitamin", "mint", "mochi",
                    "berry", "apricorn", "apricorn_seed", "evolution_item", "fossil",
                    "machine", "held_item", "utility_item", "consumable", "agriculture",
                    "archaeology", "tm" -> true;
            default -> false;
        };
    }

    private static Optional<CategoryAssignment> resolveSocietyIdentity(ResolveContext context) {
        if (!"society".equals(context.modId)) {
            return Optional.empty();
        }
        String kind = context.attributes.getOrDefault(SearchNodeKeys.SOCIETY_ITEM_KIND, "");
        if (kind.isBlank()) {
            return Optional.empty();
        }
        return Optional.of(switch (kind) {
            case "artisan_goods" -> identityAssignment(
                    "society", "artisan_goods", context.attributes,
                    "identity.society.artisan_goods", "Society artisan good");
            case "fishing" -> identityAssignment(
                    "society", "fishing", context.attributes,
                    "identity.society.fishing", "Society fishing item");
            case "gem" -> identityAssignment(
                    "society", "gems", context.attributes,
                    "identity.society.gem", "Society gem or mineral");
            case "machine" -> identityAssignment(
                    "society", "machines", context.attributes,
                    "identity.society.machine", "Society machine");
            case "farming" -> identityAssignment(
                    "society", "farming", context.attributes,
                    "identity.society.farming", "Society farming item");
            case "decoration" -> identityAssignment(
                    "society", "decoration", context.attributes,
                    "identity.society.decoration", "Society decoration");
            case "book" -> identityAssignment(
                    "society", "books", context.attributes,
                    "identity.society.book", "Society book");
            default -> identityAssignment(
                    "society", "misc", context.attributes,
                    "identity.society", "Society item");
        });
    }

    private static Optional<CategoryAssignment> resolveCreateIdentity(ResolveContext context) {
        if (!"create".equals(context.modId)
                && !CompatFamilyDetector.isPrimaryFamily(context.attributes, CompatFamilyDetector.CREATE)) {
            return Optional.empty();
        }
        if (context.categoryPolicy == AmiConfig.CompatCategoryPolicy.SEMANTIC) {
            return Optional.empty();
        }
        String kind = context.attributes.getOrDefault(SearchNodeKeys.CREATE_ITEM_KIND, "");
        if (kind.isBlank()) {
            return Optional.empty();
        }
        if (context.categoryPolicy == AmiConfig.CompatCategoryPolicy.HYBRID && !isFocusedCreateKind(kind)) {
            return Optional.empty();
        }
        String subcategory = mapCreateSubcategory(kind);
        return Optional.of(identityAssignment(
                "create",
                subcategory,
                context.attributes,
                "identity.create." + subcategory,
                "Create " + subcategory + " identity"
        ));
    }

    private static boolean isFocusedCreateKind(String kind) {
        return switch (kind) {
            case "kinetics", "machines", "logistics", "trains", "contraptions", "fluids" -> true;
            default -> false;
        };
    }

    private static Optional<CategoryAssignment> resolveCompatUnknownFallback(ResolveContext context) {
        if (context.categoryPolicy == AmiConfig.CompatCategoryPolicy.SEMANTIC) {
            return Optional.empty();
        }
        if ("create".equals(context.modId)
                || CompatFamilyDetector.isPrimaryFamily(context.attributes, CompatFamilyDetector.CREATE)) {
            String kind = context.attributes.getOrDefault(SearchNodeKeys.CREATE_ITEM_KIND, "");
            if (!kind.isBlank()) {
                return Optional.of(compatFallbackAssignment("create", mapCreateSubcategory(kind), context.attributes));
            }
        }
        if ("ae2".equals(context.modId)
                || "appliedenergistics2".equals(context.modId)
                || CompatFamilyDetector.isPrimaryFamily(context.attributes, CompatFamilyDetector.AE2)) {
            String kind = context.attributes.getOrDefault(SearchNodeKeys.AE2_ITEM_KIND, "");
            if (!kind.isBlank()) {
                return Optional.of(compatFallbackAssignment("ae2", mapAe2Subcategory(kind), context.attributes));
            }
        }
        if ("mekanism".equals(context.modId)
                || context.modId.startsWith("mekanism")
                || CompatFamilyDetector.isPrimaryFamily(context.attributes, CompatFamilyDetector.MEKANISM)) {
            String kind = context.attributes.getOrDefault(SearchNodeKeys.MEKANISM_ITEM_KIND, "");
            if (!kind.isBlank()) {
                return Optional.of(compatFallbackAssignment("mekanism", mapMekanismSubcategory(kind), context.attributes));
            }
        }
        if ("sophisticatedbackpacks".equals(context.modId)
                || "sophisticatedstorage".equals(context.modId)
                || "sophisticatedcore".equals(context.modId)
                || CompatFamilyDetector.isPrimaryFamily(context.attributes, CompatFamilyDetector.SOPHISTICATED)) {
            String kind = context.attributes.getOrDefault(SearchNodeKeys.SOPHISTICATED_ITEM_KIND, "");
            if (!kind.isBlank()) {
                return Optional.of(compatFallbackAssignment("sophisticated", mapSophisticatedSubcategory(kind), context.attributes));
            }
        }
        if ("tconstruct".equals(context.modId)
                || "silentgear".equals(context.modId)
                || CompatFamilyDetector.isPrimaryFamily(context.attributes, CompatFamilyDetector.TINKERS)
                || CompatFamilyDetector.isPrimaryFamily(context.attributes, CompatFamilyDetector.SILENT_GEAR)
                || CompatFamilyDetector.hasFamily(context.attributes, CompatFamilyDetector.MODULAR_GEAR)) {
            String kind = context.attributes.getOrDefault(SearchNodeKeys.MODULAR_GEAR_ITEM_KIND, "");
            if (!kind.isBlank()) {
                return Optional.of(compatFallbackAssignment("modular_gear", mapModularGearSubcategory(kind), context.attributes));
            }
        }
        return Optional.empty();
    }

    private static CategoryAssignment compatFallbackAssignment(String categoryId, String subcategoryId, Map<String, String> attributes) {
        return identityAssignment(
                categoryId,
                subcategoryId,
                attributes,
                "fallback." + categoryId + "." + subcategoryId,
                "recognized " + categoryId + " item with no stronger semantic route"
        );
    }

    private static String mapCreateSubcategory(String kind) {
        return switch (kind) {
            case "kinetics" -> "kinetics";
            case "machines" -> "machines";
            case "logistics" -> "logistics";
            case "trains" -> "trains";
            case "contraptions" -> "contraptions";
            case "fluids" -> "fluids";
            case "tools" -> "tools";
            case "materials" -> "materials";
            case "building" -> "building";
            default -> "misc";
        };
    }

    private static String mapAe2Subcategory(String kind) {
        return switch (kind) {
            case "network" -> "network";
            case "storage" -> "storage";
            case "terminals" -> "terminals";
            case "crafting" -> "crafting";
            case "channels" -> "channels";
            case "spatial" -> "spatial";
            case "materials" -> "materials";
            default -> "misc";
        };
    }

    private static String mapMekanismSubcategory(String kind) {
        return switch (kind) {
            case "machines" -> "machines";
            case "energy" -> "energy";
            case "chemicals" -> "chemicals";
            case "logistics" -> "logistics";
            case "upgrades" -> "upgrades";
            case "tools" -> "tools";
            case "materials" -> "materials";
            default -> "misc";
        };
    }

    private static String mapSophisticatedSubcategory(String kind) {
        return switch (kind) {
            case "backpacks" -> "backpacks";
            case "storage" -> "storage";
            case "upgrades" -> "upgrades";
            case "filters" -> "filters";
            case "tools" -> "tools";
            default -> "misc";
        };
    }

    private static String mapModularGearSubcategory(String kind) {
        return switch (kind) {
            case "tools" -> "tools";
            case "weapons" -> "weapons";
            case "armor" -> "armor";
            case "parts" -> "parts";
            case "materials" -> "materials";
            case "modifiers" -> "modifiers";
            case "stations" -> "stations";
            case "blueprints" -> "blueprints";
            default -> "misc";
        };
    }

    private static Optional<CategoryAssignment> resolveStorageFamilyTechIdentity(ResolveContext context) {
        if (!shouldBiasStorageFamilyToTech(context.modFamily, context.facets, context.path, context.attributes)) {
            return Optional.empty();
        }
        String subcategory = classifyStorageSubcategory(context.path, context.facets);
        return Optional.of(identityAssignment(
                "tech",
                subcategory,
                context.attributes,
                "identity.storage_family." + subcategory,
                "storage-family technical item"
        ));
    }

    private static Optional<CategoryAssignment> resolveAe2Identity(ResolveContext context) {
        if (!"ae2".equals(context.modId)
                && !"appliedenergistics2".equals(context.modId)
                && !CompatFamilyDetector.isPrimaryFamily(context.attributes, CompatFamilyDetector.AE2)) {
            return Optional.empty();
        }
        if (context.categoryPolicy == AmiConfig.CompatCategoryPolicy.SEMANTIC) {
            return Optional.empty();
        }
        String kind = context.attributes.getOrDefault(SearchNodeKeys.AE2_ITEM_KIND, "");
        if (kind.isBlank()) {
            return Optional.empty();
        }
        if (context.categoryPolicy == AmiConfig.CompatCategoryPolicy.HYBRID && !isFocusedAe2Kind(kind)) {
            return Optional.empty();
        }
        String subcategory = mapAe2Subcategory(kind);
        return Optional.of(identityAssignment(
                "ae2",
                subcategory,
                context.attributes,
                "identity.ae2." + subcategory,
                "AE2 " + subcategory + " identity"
        ));
    }

    private static boolean isFocusedAe2Kind(String kind) {
        return switch (kind) {
            case "network", "storage", "terminals", "crafting", "channels", "spatial" -> true;
            default -> false;
        };
    }

    private static Optional<CategoryAssignment> resolveMekanismIdentity(ResolveContext context) {
        if (!"mekanism".equals(context.modId)
                && !context.modId.startsWith("mekanism")
                && !CompatFamilyDetector.isPrimaryFamily(context.attributes, CompatFamilyDetector.MEKANISM)) {
            return Optional.empty();
        }
        if (context.categoryPolicy == AmiConfig.CompatCategoryPolicy.SEMANTIC) {
            return Optional.empty();
        }
        String kind = context.attributes.getOrDefault(SearchNodeKeys.MEKANISM_ITEM_KIND, "");
        if (kind.isBlank()) {
            return Optional.empty();
        }
        if (context.categoryPolicy == AmiConfig.CompatCategoryPolicy.HYBRID
                && !isFocusedMekanismKind(kind, context)) {
            return Optional.empty();
        }
        String subcategory = mapMekanismSubcategory(kind);
        return Optional.of(identityAssignment(
                "mekanism",
                subcategory,
                context.attributes,
                "identity.mekanism." + subcategory,
                "Mekanism " + subcategory + " identity"
        ));
    }

    private static boolean isFocusedMekanismKind(String kind, ResolveContext context) {
        return switch (kind) {
            case "machines", "energy", "chemicals", "logistics", "upgrades" -> true;
            case "tools" -> !shouldLetMekanismUseObviousSemanticCategory(context);
            default -> false;
        };
    }

    private static boolean shouldLetMekanismUseObviousSemanticCategory(ResolveContext context) {
        return hasActualFoodIdentity(context.facets, context.attributes)
                || shouldResolveAsArmorOrCurio(context.facets)
                || hasAny(context.facets, ItemFacet.HARVEST_TOOL, ItemFacet.MELEE_WEAPON,
                ItemFacet.RANGED_WEAPON, ItemFacet.UTILITY_TOOL);
    }

    private static Optional<CategoryAssignment> resolveGregTechIdentity(ResolveContext context) {
        if (!"gtceu".equals(context.modId)
                && !"gregtech".equals(context.modId)
                && !CompatFamilyDetector.isPrimaryFamily(context.attributes, CompatFamilyDetector.GREGTECH)) {
            return Optional.empty();
        }
        if (context.categoryPolicy == AmiConfig.CompatCategoryPolicy.SEMANTIC) {
            return Optional.empty();
        }
        if (shouldLetGregTechUseObviousSemanticCategory(context)) {
            return Optional.empty();
        }
        String subcategory = classifyGregTechSubcategory(context);
        return Optional.of(identityAssignment(
                "gregtech",
                subcategory,
                context.attributes,
                "identity.gregtech." + subcategory,
                "GregTech family isolation"
        ));
    }

    private static boolean shouldLetGregTechUseObviousSemanticCategory(ResolveContext context) {
        return hasActualFoodIdentity(context.facets, context.attributes)
                || shouldResolveAsArmorOrCurio(context.facets)
                || hasAny(context.facets, ItemFacet.HARVEST_TOOL, ItemFacet.MELEE_WEAPON, ItemFacet.RANGED_WEAPON);
    }

    private static String classifyGregTechSubcategory(ResolveContext context) {
        String kind = context.attributes.getOrDefault(SearchNodeKeys.GREGTECH_ITEM_KIND, "");
        if (!kind.isBlank()) {
            return mapGregTechSubcategory(kind);
        }
        PathTokens pathTokens = PathTokens.of(context.path);
        String itemClass = context.attributes.getOrDefault(SearchNodeKeys.ITEM_CLASS, "");
        String blockClass = context.attributes.getOrDefault(SearchNodeKeys.BLOCK_CLASS, "");
        if (pathTokens.containsAny("multiblock", "multiblocks")
                || PrimaryCategoryTextMatchers.containsAnyIgnoreCase(itemClass, "multiblock")
                || PrimaryCategoryTextMatchers.containsAnyIgnoreCase(blockClass, "multiblock")) {
            return "multiblocks";
        }
        if (pathTokens.containsAny("cover", "covers")) {
            return "covers";
        }
        if (pathTokens.containsAny(GREGTECH_CIRCUIT_TOKENS)) {
            return "circuits";
        }
        if (hasAny(context.facets, ItemFacet.MELEE_WEAPON, ItemFacet.RANGED_WEAPON,
                ItemFacet.HARVEST_TOOL, ItemFacet.UTILITY_TOOL)) {
            return "tools";
        }
        if (PrimaryCategoryTextMatchers.containsAnyIgnoreCase(itemClass, ".MetaMachineItem")
                || PrimaryCategoryTextMatchers.containsAnyIgnoreCase(blockClass, ".MetaMachineBlock")) {
            return "machines";
        }
        if (hasAny(context.facets, ItemFacet.MACHINE, ItemFacet.WORKSTATION)
                || pathTokens.containsAny(GREGTECH_MACHINE_TOKENS)) {
            return "machines";
        }
        if (hasAny(context.facets, ItemFacet.HAS_ENERGY, ItemFacet.CABLE)
                || pathTokens.containsAny(GREGTECH_POWER_TOKENS)) {
            return "power";
        }
        if (PrimaryCategoryTextMatchers.containsAnyIgnoreCase(itemClass, ".GTBucketItem", ".SurfaceRockBlockItem")) {
            return "materials";
        }
        if (hasAny(context.facets, ItemFacet.INGOT, ItemFacet.NUGGET, ItemFacet.DUST,
                ItemFacet.GEM, ItemFacet.RAW_MATERIAL, ItemFacet.TECH_COMPONENT,
                ItemFacet.MECHANICAL_COMPONENT, ItemFacet.INGREDIENT_MINERAL)
                || pathTokens.containsAny(GREGTECH_MATERIAL_TOKENS)) {
            return "materials";
        }
        return "misc";
    }

    private static String mapGregTechSubcategory(String kind) {
        return switch (kind) {
            case "machines" -> "machines";
            case "multiblocks" -> "multiblocks";
            case "power" -> "power";
            case "circuits" -> "circuits";
            case "materials" -> "materials";
            case "tools" -> "tools";
            case "covers" -> "covers";
            default -> "misc";
        };
    }

    private static Optional<CategoryAssignment> resolveApotheosisIdentity(ResolveContext context) {
        if (!isApotheosisFamily(context)) {
            return Optional.empty();
        }
        if (context.categoryPolicy == AmiConfig.CompatCategoryPolicy.SEMANTIC) {
            return Optional.empty();
        }
        String subcategory = classifyApotheosisSubcategory(context);
        return Optional.of(identityAssignment(
                "apotheosis",
                subcategory,
                context.attributes,
                "identity.apotheosis." + subcategory,
                "Apotheosis family identity"
        ));
    }

    private static boolean isApotheosisFamily(ResolveContext context) {
        return "apotheosis".equals(context.modId)
                || "apothic_attributes".equals(context.modId)
                || "apothic_enchanting".equals(context.modId)
                || "apothic_spawners".equals(context.modId)
                || CompatFamilyDetector.isPrimaryFamily(context.attributes, CompatFamilyDetector.APOTHEOSIS);
    }

    private static String classifyApotheosisSubcategory(ResolveContext context) {
        PathTokens pathTokens = PathTokens.of(context.path);
        String itemClass = context.attributes.getOrDefault(SearchNodeKeys.ITEM_CLASS, "");
        String blockClass = context.attributes.getOrDefault(SearchNodeKeys.BLOCK_CLASS, "");
        String tags = context.attributes.getOrDefault(SearchNodeKeys.TAGS, "");

        if ("apothic_enchanting".equals(context.modId)
                || pathTokens.containsAny(APOTHEOSIS_ENCHANTING_TOKENS)
                || PrimaryCategoryTextMatchers.containsAnyIgnoreCase(itemClass, "tomeitem", "shelf", "enchlibrary", "enderlead")
                || PrimaryCategoryTextMatchers.containsAnyIgnoreCase(blockClass, "shelf", "enchlibrary")) {
            return "enchanting";
        }
        if (pathTokens.containsAny(APOTHEOSIS_BOSS_TOKENS)
                || PrimaryCategoryTextMatchers.containsAnyIgnoreCase(tags, "boss_music_discs")
                || PrimaryCategoryTextMatchers.containsAnyIgnoreCase(itemClass, "bosssummoner")) {
            return "bosses";
        }
        if (pathTokens.containsAny(APOTHEOSIS_SPAWNER_TOKENS)
                || PrimaryCategoryTextMatchers.containsAnyIgnoreCase(itemClass, "spawner")
                || PrimaryCategoryTextMatchers.containsAnyIgnoreCase(blockClass, "spawner")) {
            return "spawners";
        }
        if (pathTokens.containsAny(APOTHEOSIS_SOCKET_TOKENS)
                || PrimaryCategoryTextMatchers.containsAnyIgnoreCase(itemClass, "potioncharm")
                || PrimaryCategoryTextMatchers.hasMetadataToken(context.attributes, SearchNodeKeys.TAGS, "curios:charm")) {
            return "sockets";
        }
        if (pathTokens.containsAny(APOTHEOSIS_GEM_TOKENS)
                || PrimaryCategoryTextMatchers.containsAnyIgnoreCase(itemClass, "gemitem")
                || PrimaryCategoryTextMatchers.containsAnyIgnoreCase(blockClass, "gem")) {
            return "gems";
        }
        if (pathTokens.containsAny(APOTHEOSIS_AFFIX_TOKENS)
                || PrimaryCategoryTextMatchers.containsAnyIgnoreCase(itemClass, "salvageitem", "tooltipitem")
                || PrimaryCategoryTextMatchers.containsAnyIgnoreCase(blockClass, "reforging", "salvaging", "augmenting")
                || PrimaryCategoryTextMatchers.containsAnyIgnoreCase(tags, "rarity_materials")) {
            return "affixes";
        }
        return "misc";
    }

    private static Optional<CategoryAssignment> resolveBotaniaIdentity(ResolveContext context) {
        if (!isBotaniaFamily(context)) {
            return Optional.empty();
        }
        if (context.categoryPolicy == AmiConfig.CompatCategoryPolicy.SEMANTIC) {
            return Optional.empty();
        }
        String subcategory = classifyBotaniaSubcategory(context);
        return Optional.of(identityAssignment(
                "botania",
                subcategory,
                context.attributes,
                "identity.botania." + subcategory,
                "Botania family identity"
        ));
    }

    private static boolean isBotaniaFamily(ResolveContext context) {
        return "botania".equals(context.modId)
                || "mythicbotany".equals(context.modId)
                || "botanicalmachinery".equals(context.modId)
                || "extrabotany".equals(context.modId)
                || CompatFamilyDetector.isPrimaryFamily(context.attributes, CompatFamilyDetector.BOTANIA);
    }

    private static String classifyBotaniaSubcategory(ResolveContext context) {
        PathTokens pathTokens = PathTokens.of(context.path);
        if (pathTokens.containsAny("rune", "runes")) {
            return "runes";
        }
        if (pathTokens.containsAny(BOTANIA_MANA_TOKENS)
                || PrimaryCategoryTextMatchers.containsAnyIgnoreCase(context.attributes.getOrDefault(SearchNodeKeys.ITEM_CLASS, ""), "mana")) {
            return "mana";
        }
        if (isBotaniaGeneratingFlower(context.path)) {
            return "generating_flowers";
        }
        if (isBotaniaFunctionalFlower(context.path)) {
            return "functional_flowers";
        }
        if (pathTokens.containsAny("brew", "vial", "flask", "incense", "incense_stick")) {
            return "brews";
        }
        if (hasAny(context.facets, ItemFacet.CURIO, ItemFacet.EQUIPPABLE)
                || pathTokens.containsAny(BOTANIA_BAUBLE_TOKENS)) {
            return "baubles";
        }
        if (hasAny(context.facets, ItemFacet.MELEE_WEAPON, ItemFacet.RANGED_WEAPON,
                ItemFacet.HARVEST_TOOL, ItemFacet.UTILITY_TOOL)
                || pathTokens.containsAny(BOTANIA_TOOL_TOKENS)) {
            return "tools";
        }
        if (hasAny(context.facets, ItemFacet.DECORATIVE_BLOCK, ItemFacet.GLASS_BLOCK,
                ItemFacet.STONE_BLOCK, ItemFacet.FLOWER, ItemFacet.FUNGI)
                || (hasAny(context.facets, ItemFacet.PLACEABLE)
                && PrimaryCategoryTextMatchers.containsAnyIgnoreCase(context.attributes.getOrDefault(SearchNodeKeys.ITEM_CLASS, ""), "blockitem"))) {
            return "decoration";
        }
        if (hasAny(context.facets, ItemFacet.INGOT, ItemFacet.NUGGET, ItemFacet.DUST,
                ItemFacet.GEM, ItemFacet.RAW_MATERIAL, ItemFacet.TECH_COMPONENT,
                ItemFacet.INGREDIENT_ORGANIC, ItemFacet.INGREDIENT_MINERAL,
                ItemFacet.TEMPLATE)
                || pathTokens.containsAny(BOTANIA_MATERIAL_TOKENS)
                || pathTokens.containsAny("seed", "seeds", "shard")
                || PrimaryCategoryTextMatchers.containsAnyIgnoreCase(context.attributes.getOrDefault(SearchNodeKeys.ITEM_CLASS, ""),
                "grassseeds", "laputashard")) {
            return "materials";
        }
        return "misc";
    }

    private static boolean isBotaniaGeneratingFlower(String path) {
        return PrimaryCategoryTextMatchers.containsPathToken(path, BOTANIA_GENERATING_FLOWER_TOKENS);
    }

    private static boolean isBotaniaFunctionalFlower(String path) {
        return PrimaryCategoryTextMatchers.containsPathToken(path, BOTANIA_FUNCTIONAL_FLOWER_TOKENS);
    }

    private static Optional<CategoryAssignment> resolveSophisticatedIdentity(ResolveContext context) {
        if (!"sophisticatedbackpacks".equals(context.modId)
                && !"sophisticatedstorage".equals(context.modId)
                && !"sophisticatedcore".equals(context.modId)
                && !CompatFamilyDetector.isPrimaryFamily(context.attributes, CompatFamilyDetector.SOPHISTICATED)) {
            return Optional.empty();
        }
        if (context.categoryPolicy == AmiConfig.CompatCategoryPolicy.SEMANTIC) {
            return Optional.empty();
        }
        String kind = context.attributes.getOrDefault(SearchNodeKeys.SOPHISTICATED_ITEM_KIND, "");
        if (kind.isBlank()) {
            return Optional.empty();
        }
        if (context.categoryPolicy == AmiConfig.CompatCategoryPolicy.HYBRID && !isFocusedSophisticatedKind(kind)) {
            return Optional.empty();
        }
        String subcategory = mapSophisticatedSubcategory(kind);
        return Optional.of(identityAssignment(
                "sophisticated",
                subcategory,
                context.attributes,
                "identity.sophisticated." + subcategory,
                "Sophisticated " + subcategory + " identity"
        ));
    }

    private static boolean isFocusedSophisticatedKind(String kind) {
        return switch (kind) {
            case "backpacks", "storage", "upgrades", "filters" -> true;
            default -> false;
        };
    }

    private static Optional<CategoryAssignment> resolveModularGearIdentity(ResolveContext context) {
        if (!"tconstruct".equals(context.modId)
                && !"silentgear".equals(context.modId)
                && !CompatFamilyDetector.isPrimaryFamily(context.attributes, CompatFamilyDetector.TINKERS)
                && !CompatFamilyDetector.isPrimaryFamily(context.attributes, CompatFamilyDetector.SILENT_GEAR)
                && !CompatFamilyDetector.hasFamily(context.attributes, CompatFamilyDetector.MODULAR_GEAR)) {
            return Optional.empty();
        }
        if (context.categoryPolicy == AmiConfig.CompatCategoryPolicy.SEMANTIC) {
            return Optional.empty();
        }
        String kind = context.attributes.getOrDefault(SearchNodeKeys.MODULAR_GEAR_ITEM_KIND, "");
        if (kind.isBlank()) {
            return Optional.empty();
        }
        if (context.categoryPolicy == AmiConfig.CompatCategoryPolicy.HYBRID && !isFocusedModularGearKind(kind)) {
            return Optional.empty();
        }
        String subcategory = mapModularGearSubcategory(kind);
        return Optional.of(identityAssignment(
                "modular_gear",
                subcategory,
                context.attributes,
                "identity.modular_gear." + subcategory,
                "Modular gear " + subcategory + " identity"
        ));
    }

    private static boolean isFocusedModularGearKind(String kind) {
        return switch (kind) {
            case "parts", "modifiers", "stations", "blueprints" -> true;
            default -> false;
        };
    }

    private static Optional<CategoryAssignment> resolveModularGolemsIdentity(ResolveContext context) {
        if (!"modulargolems".equals(context.modId)
                && !CompatFamilyDetector.hasFamily(context.attributes, CompatFamilyDetector.MODULAR_GOLEMS)) {
            return Optional.empty();
        }
        String kind = context.attributes.getOrDefault(SearchNodeKeys.MODULAR_GOLEMS_ITEM_KIND, "");
        if (kind.isBlank()) {
            return Optional.empty();
        }
        String category;
        String subcategory;
        switch (kind) {
            case "golem_armor" -> {
                category = "armor";
                subcategory = "animal";
            }
            case "ranged_weapons" -> {
                category = "tools";
                subcategory = "ranged";
            }
            case "harvest_tools" -> {
                category = "tools";
                subcategory = "harvest";
            }
            case "weapons" -> {
                category = "tools";
                subcategory = "melee";
            }
            case "upgrades" -> {
                category = "tech";
                subcategory = "upgrades";
            }
            case "templates" -> {
                category = "tech";
                subcategory = "templates";
            }
            case "cards", "route_cards" -> {
                category = "tech";
                subcategory = "redstone";
            }
            case "parts", "holders", "facades" -> {
                category = "tech";
                subcategory = "parts";
            }
            case "workstations" -> {
                category = "tech";
                subcategory = "machines";
            }
            case "wands" -> {
                category = "magic";
                subcategory = "artifacts";
            }
            default -> {
                return Optional.empty();
            }
        }
        return Optional.of(identityAssignment(
                category,
                subcategory,
                context.attributes,
                "identity.modular_golems." + kind,
                "Modular Golems " + kind + " identity"
        ));
    }

    private static Optional<CategoryAssignment> resolveTaczIdentity(ResolveContext context) {
        if (!"tacz".equals(context.modId)
                && !CompatFamilyDetector.hasFamily(context.attributes, CompatFamilyDetector.TACZ)) {
            return Optional.empty();
        }
        String kind = context.attributes.getOrDefault(SearchNodeKeys.TACZ_ITEM_KIND, "");
        String subcategory = switch (kind) {
            case "guns" -> "guns";
            case "ammo" -> "ammo";
            case "attachments" -> "attachments";
            case "workstations" -> "workstations";
            default -> "";
        };
        if (subcategory.isBlank()) {
            return Optional.empty();
        }
        return Optional.of(identityAssignment(
                "tacz",
                subcategory,
                context.attributes,
                "identity.tacz." + kind,
                "TacZ " + kind + " identity"
        ));
    }

    private static Optional<CategoryAssignment> resolveMnaIdentity(ResolveContext context) {
        if (!"mna".equals(context.modId)
                && !CompatFamilyDetector.hasFamily(context.attributes, CompatFamilyDetector.MNA)) {
            return Optional.empty();
        }
        String kind = context.attributes.getOrDefault(SearchNodeKeys.MNA_ITEM_KIND, "");
        String category;
        String subcategory;
        switch (kind) {
            case "construct_parts" -> {
                category = "tech";
                subcategory = "parts";
            }
            case "ritual_patches" -> {
                category = "magic";
                subcategory = "artifacts";
            }
            case "motes", "runes" -> {
                category = "magic";
                subcategory = "reagents";
            }
            case "reagents" -> {
                category = "magic";
                subcategory = "reagents";
            }
            case "artifacts" -> {
                category = "magic";
                subcategory = "artifacts";
            }
            case "materials" -> {
                category = "ingredients";
                subcategory = "mineral";
            }
            case "tools" -> {
                category = "tools";
                subcategory = "utility";
            }
            case "ranged_weapons" -> {
                category = "tools";
                subcategory = "ranged";
            }
            case "weapons" -> {
                category = "tools";
                subcategory = "melee";
            }
            case "utility" -> {
                category = "utility";
                subcategory = "misc";
            }
            case "transport" -> {
                category = "tech";
                subcategory = "transport";
            }
            default -> {
                return Optional.empty();
            }
        }
        return Optional.of(identityAssignment(
                category,
                subcategory,
                context.attributes,
                "identity.mna." + kind,
                "Mana and Artifice " + kind + " identity"
        ));
    }

    private static Optional<CategoryAssignment> resolveArsNouveauIdentity(ResolveContext context) {
        if (!"ars_nouveau".equals(context.modId)
                && !CompatFamilyDetector.hasFamily(context.attributes, CompatFamilyDetector.ARS_NOUVEAU)) {
            return Optional.empty();
        }
        if (context.categoryPolicy == AmiConfig.CompatCategoryPolicy.SEMANTIC) {
            return Optional.empty();
        }
        String kind = context.attributes.getOrDefault(SearchNodeKeys.ARS_NOUVEAU_ITEM_KIND, "");
        String subcategory = classifyArsNouveauSubcategory(context, kind);
        return Optional.of(identityAssignment(
                "ars_nouveau",
                subcategory,
                context.attributes,
                "identity.ars_nouveau." + kind,
                "Ars Nouveau " + kind + " identity"
        ));
    }

    private static String classifyArsNouveauSubcategory(ResolveContext context, String kind) {
        String mapped = switch (kind) {
            case "glyphs" -> "glyphs";
            case "ritual_tablets" -> "rituals";
            case "spellcasting" -> "spellcasting";
            case "source" -> "source";
            case "automation" -> "automation";
            case "familiars" -> "familiars";
            case "equipment" -> "equipment";
            case "materials" -> "materials";
            case "building" -> "building";
            default -> "";
        };
        if (!mapped.isBlank()) {
            return mapped;
        }
        PathTokens pathTokens = PathTokens.of(context.path);
        if (pathTokens.containsAny("glyph", "glyphs")) {
            return "glyphs";
        }
        if (pathTokens.containsAny("ritual", "rituals")) {
            return "rituals";
        }
        if (pathTokens.containsAny("spell", "book", "wand", "parchment", "chalk")) {
            return "spellcasting";
        }
        if (pathTokens.containsAny("source", "relay", "jar")) {
            return "source";
        }
        if (pathTokens.containsAny("apparatus", "imbuement", "scribes", "repository", "lectern", "pedestal", "turret", "sensor")) {
            return "automation";
        }
        if (pathTokens.containsAny("familiar", "starbuncle", "drygmy", "wixie", "whirlisprig", "bookwyrm", "charm")) {
            return "familiars";
        }
        if (hasAny(context.facets, ItemFacet.EQUIPPABLE, ItemFacet.CURIO)
                || pathTokens.containsAny("robe", "hood", "leggings", "boots", "ring", "belt", "amulet", "trinket")) {
            return "equipment";
        }
        if (hasAny(context.facets, ItemFacet.INGOT, ItemFacet.NUGGET, ItemFacet.DUST,
                ItemFacet.GEM, ItemFacet.RAW_MATERIAL, ItemFacet.INGREDIENT_ORGANIC, ItemFacet.INGREDIENT_MINERAL,
                ItemFacet.MAGIC_REAGENT)
                || pathTokens.containsAny("magebloom", "archwood", "wilden", "essence", "gem", "fiber")) {
            return "materials";
        }
        if (hasAny(context.facets, ItemFacet.DECORATIVE_BLOCK, ItemFacet.SLAB, ItemFacet.STAIRS, ItemFacet.WALL, ItemFacet.FENCE)
                || pathTokens.containsAny("sourcestone", "weave", "decor", "planks", "leaves", "sapling")) {
            return "building";
        }
        return "misc";
    }

    private static Optional<CategoryAssignment> resolveSpectrumIdentity(ResolveContext context) {
        if (!"spectrum".equals(context.modId)
                && !CompatFamilyDetector.hasFamily(context.attributes, CompatFamilyDetector.SPECTRUM)) {
            return Optional.empty();
        }
        String kind = context.attributes.getOrDefault(SearchNodeKeys.SPECTRUM_ITEM_KIND, "");
        String category;
        String subcategory;
        switch (kind) {
            case "structure_placers" -> {
                category = "utility";
                subcategory = "misc";
            }
            case "reagents" -> {
                category = "magic";
                subcategory = "reagents";
            }
            case "upgrades" -> {
                category = "tech";
                subcategory = "upgrades";
            }
            case "materials" -> {
                category = "ingredients";
                subcategory = "mineral";
            }
            default -> {
                return Optional.empty();
            }
        }
        return Optional.of(identityAssignment(
                category,
                subcategory,
                context.attributes,
                "identity.spectrum." + kind,
                "Spectrum " + kind + " identity"
        ));
    }

    private static Optional<CategoryAssignment> resolveNaturesAuraIdentity(ResolveContext context) {
        if (!"naturesaura".equals(context.modId)
                && !CompatFamilyDetector.hasFamily(context.attributes, CompatFamilyDetector.NATURES_AURA)) {
            return Optional.empty();
        }
        String kind = context.attributes.getOrDefault(SearchNodeKeys.NATURES_AURA_ITEM_KIND, "");
        String category;
        String subcategory;
        switch (kind) {
            case "effect_powders", "tokens", "spirits" -> {
                category = "magic";
                subcategory = "reagents";
            }
            case "artifacts" -> {
                category = "magic";
                subcategory = "artifacts";
            }
            case "structure_finders", "staff_finders" -> {
                category = "utility";
                subcategory = "navigation";
            }
            case "transport" -> {
                category = "tech";
                subcategory = "transport";
            }
            case "materials" -> {
                category = "ingredients";
                subcategory = "mineral";
            }
            case "templates" -> {
                category = "tech";
                subcategory = "parts";
            }
            case "utility" -> {
                category = "utility";
                subcategory = "misc";
            }
            default -> {
                return Optional.empty();
            }
        }
        return Optional.of(identityAssignment(
                category,
                subcategory,
                context.attributes,
                "identity.naturesaura." + kind,
                "Nature's Aura " + kind + " identity"
        ));
    }

    private static Optional<CategoryAssignment> resolveAlexsMobsIdentity(ResolveContext context) {
        if (!"alexsmobs".equals(context.modId)
                && !CompatFamilyDetector.hasFamily(context.attributes, CompatFamilyDetector.ALEXS_MOBS)) {
            return Optional.empty();
        }
        String kind = context.attributes.getOrDefault(SearchNodeKeys.ALEXS_MOBS_ITEM_KIND, "");
        String category;
        String subcategory;
        switch (kind) {
            case "organic_drops" -> {
                category = "ingredients";
                subcategory = "organic";
            }
            case "protein_foods" -> {
                category = "nature";
                subcategory = "proteins";
            }
            case "navigation_tools" -> {
                category = "utility";
                subcategory = "navigation";
            }
            case "utility_items", "internal_items" -> {
                category = "utility";
                subcategory = "misc";
            }
            case "ranged_weapons" -> {
                category = "tools";
                subcategory = "ranged";
            }
            case "projectiles" -> {
                category = "tools";
                subcategory = "ammo";
            }
            case "transport" -> {
                category = "tech";
                subcategory = "transport";
            }
            case "feet_armor" -> {
                category = "armor";
                subcategory = "feet";
            }
            default -> {
                return Optional.empty();
            }
        }
        return Optional.of(identityAssignment(
                category,
                subcategory,
                context.attributes,
                "identity.alexsmobs." + kind,
                "Alex's Mobs " + kind + " identity"
        ));
    }

    private static Optional<CategoryAssignment> resolveAlexsCavesIdentity(ResolveContext context) {
        if (!"alexscaves".equals(context.modId)
                && !CompatFamilyDetector.hasFamily(context.attributes, CompatFamilyDetector.ALEXS_CAVES)) {
            return Optional.empty();
        }
        String kind = context.attributes.getOrDefault(SearchNodeKeys.ALEXS_CAVES_ITEM_KIND, "");
        String category;
        String subcategory;
        switch (kind) {
            case "guide_items" -> {
                category = "utility";
                subcategory = "books";
            }
            case "materials" -> {
                category = "tech";
                subcategory = "ingots";
            }
            case "tech_parts" -> {
                category = "tech";
                subcategory = "parts";
            }
            case "magic_reagents" -> {
                category = "magic";
                subcategory = "reagents";
            }
            case "protein_foods" -> {
                category = "nature";
                subcategory = "proteins";
            }
            case "snacks" -> {
                category = "nature";
                subcategory = "snacks";
            }
            case "organic_drops", "ingredients" -> {
                category = "ingredients";
                subcategory = "organic";
            }
            case "utility_items", "internal_items" -> {
                category = "utility";
                subcategory = "misc";
            }
            case "ranged_weapons" -> {
                category = "tools";
                subcategory = "ranged";
            }
            case "weapons" -> {
                category = "tools";
                subcategory = "melee";
            }
            case "harvest_tools" -> {
                category = "tools";
                subcategory = "harvest";
            }
            case "projectiles" -> {
                category = "tools";
                subcategory = "ammo";
            }
            case "transport" -> {
                category = "tech";
                subcategory = "transport";
            }
            default -> {
                return Optional.empty();
            }
        }
        return Optional.of(identityAssignment(
                category,
                subcategory,
                context.attributes,
                "identity.alexscaves." + kind,
                "Alex's Caves " + kind + " identity"
        ));
    }

    private static CategoryAssignment identityAssignment(
            String categoryId,
            String subcategoryId,
            Map<String, String> attributes,
            String evidenceId,
            String reason
    ) {
        Map<String, String> diagnosticAttributes = new LinkedHashMap<>(attributes);
        diagnosticAttributes.put("classificationMode", "hard_identity");
        diagnosticAttributes.put("classificationScore", "1000");
        diagnosticAttributes.put("classificationEvidence", "+1000 " + evidenceId + "[" + reason + "]");
        diagnosticAttributes.put("classificationScores", categoryId + "/" + subcategoryId + "=1000");
        return assignment(categoryId, subcategoryId, diagnosticAttributes);
    }

    private static boolean hasHardToolIdentity(Set<ItemFacet> facets) {
        return hasAny(facets,
                ItemFacet.MELEE_WEAPON,
                ItemFacet.HARVEST_TOOL,
                ItemFacet.UTILITY_TOOL)
                || (facets.contains(ItemFacet.RANGED_WEAPON) && !facets.contains(ItemFacet.PROJECTILE));
    }

    private static boolean shouldResolveFoodFamilyCookingStation(ResolveContext context) {
        if (context.modFamily != PrimaryCategoryModFamily.FOOD
                || !context.facets.contains(ItemFacet.PLACEABLE)
                || !context.facets.contains(ItemFacet.HAS_BLOCK_ENTITY)) {
            return false;
        }
        if (hasAny(context.facets,
                ItemFacet.EDIBLE,
                ItemFacet.PLACEABLE_FOOD,
                ItemFacet.FOOD_MEAL,
                ItemFacet.FOOD_DRINK,
                ItemFacet.FOOD_PROTEIN,
                ItemFacet.SEED,
                ItemFacet.CROP,
                ItemFacet.NATURE_MISC,
                ItemFacet.FUNGI,
                ItemFacet.LEAVES,
                ItemFacet.FLOWER,
                ItemFacet.DECORATIVE_BLOCK)) {
            return false;
        }

        String itemClass = context.attributes.getOrDefault(SearchNodeKeys.ITEM_CLASS, "");
        String blockClass = context.attributes.getOrDefault(SearchNodeKeys.BLOCK_CLASS, "");
        return PathTokens.of(context.path).containsAny(FOOD_COOKING_STATION_TOKENS)
                || PrimaryCategoryTextMatchers.containsAnyIgnoreCase(itemClass, "SkilletItem", "CookingPotItem", "StoveItem")
                || PrimaryCategoryTextMatchers.containsAnyIgnoreCase(blockClass, "SkilletBlock", "CookingPotBlock", "StoveBlock");
    }

    private static boolean shouldResolveEdibleMagicReagentBeforeFood(ResolveContext context) {
        if (!context.facets.contains(ItemFacet.EDIBLE)
                || !context.facets.contains(ItemFacet.MAGIC_REAGENT)) {
            return false;
        }
        return "spider_eye".equals(context.path)
                || PrimaryCategoryTextMatchers.containsPathToken(context.path, Set.of("spider_eye"))
                || PrimaryCategoryTextMatchers.hasMetadataToken(context.attributes, SearchNodeKeys.RECIPE_USE_CATEGORIES, "ami:brewing")
                || PrimaryCategoryTextMatchers.hasMetadataToken(context.attributes, SearchNodeKeys.RECIPE_USE_CATEGORIES, "potion_workshop_brewing");
    }

    private static boolean hasProjectileToolContext(String path, Map<String, String> attributes) {
        String itemClass = attributes.getOrDefault(SearchNodeKeys.ITEM_CLASS, "");
        return PrimaryCategoryTextMatchers.containsPathToken(path, Set.of(
                "arrow", "arrows", "bolt", "bolts", "bullet", "bullets", "round", "rounds",
                "cartridge", "cartridges", "rocket", "ammo", "gun", "shotgun", "cannon",
                "autocannon", "artillery", "mortar", "munition", "munitions"))
                || PrimaryCategoryTextMatchers.hasMetadataToken(attributes, SearchNodeKeys.TAGS, "minecraft:arrows")
                || PrimaryCategoryTextMatchers.hasMetadataToken(attributes, SearchNodeKeys.TAGS, "createbigcannons:big_cannon_projectiles")
                || PrimaryCategoryTextMatchers.hasMetadataToken(attributes, SearchNodeKeys.BLOCK_TAGS, "createbigcannons:big_cannon_projectiles")
                || PrimaryCategoryTextMatchers.containsAnyIgnoreCase(itemClass, "SnowballItem", "BombItem");
    }

    private static boolean hasActualFoodIdentity(Set<ItemFacet> facets, Map<String, String> attributes) {
        return hasAny(facets, ItemFacet.EDIBLE, ItemFacet.PLACEABLE_FOOD)
                || PrimaryCategoryTextMatchers.hasCsvToken(attributes.getOrDefault(SearchNodeKeys.COMPONENT_FACTS, ""), "food");
    }

    private static CategoryAssignment fallback() {
        return new CategoryAssignment("misc", "unknown", Map.of());
    }

    private static boolean hasCompatFamily(Map<String, String> attributes) {
        return attributes != null
                && (!attributes.getOrDefault(SearchNodeKeys.COMPAT_FAMILIES, "").isBlank()
                || !attributes.getOrDefault(SearchNodeKeys.PRIMARY_COMPAT_FAMILY, "").isBlank()
                || !attributes.getOrDefault(SearchNodeKeys.COMPAT_FAMILY, "").isBlank());
    }

    private static CategoryAssignment assignment(String categoryId, String subcategoryId, Map<String, String> attributes) {
        return new CategoryAssignment(categoryId, subcategoryId, attributes);
    }

    private static PrimaryRule rule(String id,
                                    Predicate<ResolveContext> matches,
                                    Function<ResolveContext, CategoryAssignment> assignment) {
        return new PrimaryRule(id, matches, assignment);
    }

    private static boolean hasAny(Set<ItemFacet> facets, ItemFacet... expected) {
        for (ItemFacet facet : expected) {
            if (facets.contains(facet)) {
                return true;
            }
        }
        return false;
    }

    private static boolean hasStructuralBuildingShape(Set<ItemFacet> facets) {
        return facets.contains(ItemFacet.PLACEABLE)
                && hasAny(facets,
                ItemFacet.STAIRS,
                ItemFacet.SLAB,
                ItemFacet.WALL,
                ItemFacet.FENCE,
                ItemFacet.FENCE_GATE,
                ItemFacet.PANE,
                ItemFacet.DOOR,
                ItemFacet.TRAPDOOR);
    }

    private static String classifyBestiarySubcategory(String path) {
        if (!path.endsWith("_spawn_egg")) {
            return "passive";
        }

        String mob = path.substring(0, path.length() - "_spawn_egg".length());
        if (HOSTILE_SPAWN_EGGS.contains(mob)) return "hostile";
        if (NEUTRAL_SPAWN_EGGS.contains(mob)) return "neutral";
        return "passive";
    }

    private static String classifyUtilitySubcategory(Set<ItemFacet> facets) {
        if (facets.contains(ItemFacet.UTILITY_NAVIGATION)) return "navigation";
        if (facets.contains(ItemFacet.UTILITY_MEDICAL)) return "medical";
        if (facets.contains(ItemFacet.UTILITY_CURRENCY)) return "currency";
        if (facets.contains(ItemFacet.GUIDE_BOOK) || facets.contains(ItemFacet.BOOK)) return "books";
        return "misc";
    }

    private static String classifyMagicSubcategory(Set<ItemFacet> facets) {
        if (facets.contains(ItemFacet.POTION)) return "potions";
        if (facets.contains(ItemFacet.ENCHANTED_BOOK)) return "books";
        if (facets.contains(ItemFacet.MAGIC_ARTIFACT)) return "artifacts";
        if (facets.contains(ItemFacet.MAGIC_REAGENT)) return "reagents";
        return "artifacts";
    }

    private static String classifyArmorSubcategory(Set<ItemFacet> facets, Map<String, String> attributes) {
        if (isNonPlayerArmorClass(attributes)) return "animal";
        if (facets.contains(ItemFacet.ARMOR_HEAD)) return "head";
        if (facets.contains(ItemFacet.ARMOR_CHEST)) return "chest";
        if (facets.contains(ItemFacet.ARMOR_LEGS)) return "legs";
        if (facets.contains(ItemFacet.ARMOR_FEET)) return "feet";
        if (facets.contains(ItemFacet.ARMOR_ANIMAL)) return "animal";
        if (facets.contains(ItemFacet.CURIO)) return "curios";
        return "curios";
    }

    private static boolean isNonPlayerArmorClass(Map<String, String> attributes) {
        String itemClass = attributes.getOrDefault(SearchNodeKeys.ITEM_CLASS, "").toLowerCase(Locale.ROOT);
        return PrimaryCategoryTextMatchers.containsAnyIgnoreCase(itemClass,
                "animalarmoritem",
                "horsearmoritem",
                "wolfarmoritem",
                "dogarmoritem",
                "golemarmoritem",
                "saddleitem");
    }

    private static String classifyWeaponSubcategory(Set<ItemFacet> facets) {
        if (facets.contains(ItemFacet.MELEE_WEAPON)) return "melee";
        if (facets.contains(ItemFacet.RANGED_WEAPON)) return "ranged";
        if (facets.contains(ItemFacet.PROJECTILE)) return "ammo";
        if (facets.contains(ItemFacet.HARVEST_TOOL)) return "harvest";
        if (facets.contains(ItemFacet.UTILITY_TOOL)) return "utility";
        return "utility";
    }

    private static String classifyCreateFamilyToolSubcategory(String path) {
        if (PathTokens.of(path).containsAny("cannon", "gun")) return "ranged";
        return "utility";
    }

    private static boolean isThrowableIngredient(Set<ItemFacet> facets) {
        return facets.contains(ItemFacet.PROJECTILE)
                && hasAny(facets, ItemFacet.INGREDIENT_ORGANIC, ItemFacet.INGREDIENT_MINERAL, ItemFacet.INGREDIENT_DYE)
                && !hasAny(facets, ItemFacet.MELEE_WEAPON, ItemFacet.RANGED_WEAPON, ItemFacet.HARVEST_TOOL, ItemFacet.UTILITY_TOOL);
    }

    private static boolean shouldResolveIngredientBeforeEquipmentTech(Set<ItemFacet> facets) {
        return hasAny(facets, ItemFacet.INGREDIENT_ORGANIC, ItemFacet.INGREDIENT_MINERAL, ItemFacet.INGREDIENT_DYE)
                && !hasAny(facets,
                ItemFacet.MELEE_WEAPON,
                ItemFacet.RANGED_WEAPON,
                ItemFacet.HARVEST_TOOL,
                ItemFacet.UTILITY_TOOL,
                ItemFacet.ARMOR_HEAD,
                ItemFacet.ARMOR_CHEST,
                ItemFacet.ARMOR_LEGS,
                ItemFacet.ARMOR_FEET,
                ItemFacet.ARMOR_ANIMAL,
                ItemFacet.MACHINE,
                ItemFacet.HAS_ENERGY,
                ItemFacet.STORAGE,
                ItemFacet.REDSTONE_LOGIC,
                ItemFacet.REDSTONE_SIGNAL,
                ItemFacet.TRANSPORT,
                ItemFacet.CABLE,
                ItemFacet.TEMPLATE);
    }

    private static boolean shouldResolveAsArmorOrCurio(Set<ItemFacet> facets) {
        if (hasAny(facets, ItemFacet.ARMOR_HEAD, ItemFacet.ARMOR_CHEST, ItemFacet.ARMOR_LEGS, ItemFacet.ARMOR_FEET, ItemFacet.ARMOR_ANIMAL)) {
            return true;
        }
        return facets.contains(ItemFacet.CURIO)
                && !hasAny(facets,
                ItemFacet.DECORATIVE_BLOCK,
                ItemFacet.LIGHT_SOURCE,
                ItemFacet.NATURE_MISC,
                ItemFacet.FUNGI,
                ItemFacet.FLOWER,
                ItemFacet.LOG,
                ItemFacet.LEAVES,
                ItemFacet.EDIBLE,
                ItemFacet.PLACEABLE_FOOD,
                ItemFacet.INGREDIENT_ORGANIC,
                ItemFacet.INGREDIENT_MINERAL,
                ItemFacet.INGREDIENT_DYE);
    }

    private static String classifyTechSubcategory(String path, Set<ItemFacet> facets) {
        if (facets.contains(ItemFacet.ACTIVE_REDSTONE_LOGIC) || facets.contains(ItemFacet.REDSTONE_LOGIC)) {
            return "redstone";
        }
        if (facets.contains(ItemFacet.MACHINE)
                || facets.contains(ItemFacet.WORKSTATION)
                || facets.contains(ItemFacet.HAS_ENERGY)
                || facets.contains(ItemFacet.STORAGE)
                || facets.contains(ItemFacet.INTERACTIVE_BLOCK)
                || facets.contains(ItemFacet.HAS_BLOCK_ENTITY)) {
            return "machines";
        }
        if (facets.contains(ItemFacet.REDSTONE_SIGNAL)) {
            return "redstone";
        }
        if (facets.contains(ItemFacet.TRANSPORT)) return "transport";
        if (facets.contains(ItemFacet.TEMPLATE)) return "templates";
        if (facets.contains(ItemFacet.UPGRADE)) return "upgrades";
        if (facets.contains(ItemFacet.CABLE)) return "cables";
        if (facets.contains(ItemFacet.MECHANICAL_COMPONENT)) return "parts";
        if (facets.contains(ItemFacet.TECH_COMPONENT)) {
            if (PathTokens.of(path).containsAny("circuit", "processor", "logic", "calculation", "engineering", "chip")) {
                return "circuits";
            }
            return "parts";
        }
        if (facets.contains(ItemFacet.DUST)) return "dusts";
        if (hasAny(facets, ItemFacet.INGOT, ItemFacet.GEM, ItemFacet.NUGGET, ItemFacet.RAW_MATERIAL)) return "ingots";
        return "parts";
    }

    private static String classifyNatureSubcategory(String path, Set<ItemFacet> facets) {
        if (hasPreparedMealPath(path)) return "meals";
        if (facets.contains(ItemFacet.FOOD_MEAL)) return "meals";
        if (facets.contains(ItemFacet.FOOD_DRINK)) return "drinks";
        if (facets.contains(ItemFacet.FOOD_PROTEIN)) return "proteins";
        if (facets.contains(ItemFacet.PLACEABLE_FOOD)) return "meals";
        if (facets.contains(ItemFacet.SEED)) return "seeds";
        if (facets.contains(ItemFacet.CROP)) return "crops";
        if (facets.contains(ItemFacet.EDIBLE)) return "snacks";
        if (facets.contains(ItemFacet.NATURE_MISC)) return "flora";
        if (facets.contains(ItemFacet.FUNGI)) return "fungi";
        if (facets.contains(ItemFacet.FLOWER)) return "flora";
        if (facets.contains(ItemFacet.LOG)) return "wood";
        if (facets.contains(ItemFacet.LEAVES)) return "flora";
        return "flora";
    }

    private static boolean isSapling(String path, Map<String, String> attributes) {
        String blockClass = attributes.getOrDefault(SearchNodeKeys.BLOCK_CLASS, "");
        return PrimaryCategoryTextMatchers.containsPathToken(path, Set.of("sapling", "saplings"))
                || PrimaryCategoryTextMatchers.hasMetadataToken(attributes, SearchNodeKeys.TAGS, "minecraft:saplings")
                || PrimaryCategoryTextMatchers.hasMetadataToken(attributes, SearchNodeKeys.BLOCK_TAGS, "minecraft:saplings")
                || blockClass.endsWith("SaplingBlock")
                || blockClass.contains(".SaplingBlock");
    }

    private static boolean isLeaves(String path, Set<ItemFacet> facets, Map<String, String> attributes) {
        if (facets.contains(ItemFacet.CROP)) {
            return false;
        }
        if (facets.contains(ItemFacet.LEAVES)
                || PrimaryCategoryTextMatchers.hasMetadataToken(attributes, SearchNodeKeys.TAGS, "minecraft:leaves")
                || PrimaryCategoryTextMatchers.hasMetadataToken(attributes, SearchNodeKeys.BLOCK_TAGS, "minecraft:leaves")) {
            return true;
        }
        // Path-based fallback: only apply when blockShape is not explicitly structural.
        // A block shaped as "partial" (hedge, post, lattice) is a building element, not leaves.
        String blockShape = attributes.getOrDefault("blockShape", "");
        if ("partial".equals(blockShape)) {
            return false;
        }
        return PrimaryCategoryTextMatchers.containsPathToken(path, Set.of("leaf", "leaves"));
    }

    private static boolean isWoodBlock(String path, Set<ItemFacet> facets, Map<String, String> attributes) {
        if (isSapling(path, attributes) || isLeaves(path, facets, attributes)) {
            return false;
        }
        return facets.contains(ItemFacet.WOOD_BLOCK)
                || PrimaryCategoryTextMatchers.hasMetadataToken(attributes, SearchNodeKeys.TAGS, "minecraft:planks")
                || PrimaryCategoryTextMatchers.hasMetadataToken(attributes, SearchNodeKeys.TAGS, "minecraft:logs")
                || PrimaryCategoryTextMatchers.hasMetadataToken(attributes, SearchNodeKeys.BLOCK_TAGS, "minecraft:planks")
                || PrimaryCategoryTextMatchers.hasMetadataToken(attributes, SearchNodeKeys.BLOCK_TAGS, "minecraft:logs")
                || PathTokens.of(path).endsWith("planks")
                || PathTokens.of(path).endsWith("log")
                || PathTokens.of(path).endsWith("wood")
                || PathTokens.of(path).endsWith("stem")
                || PathTokens.of(path).endsWith("hyphae");
    }

    private static boolean isPlantSeed(String path, Set<ItemFacet> facets, Map<String, String> attributes) {
        if (isSapling(path, attributes)) {
            return false;
        }
        if (hasAny(facets,
                ItemFacet.INGOT,
                ItemFacet.GEM,
                ItemFacet.NUGGET,
                ItemFacet.RAW_MATERIAL,
                ItemFacet.DUST,
                ItemFacet.TECH_COMPONENT,
                ItemFacet.UTILITY_MISC,
                ItemFacet.FLUID_CONTAINER)) {
            return false;
        }
        if (!facets.contains(ItemFacet.SEED)
                && !PrimaryCategoryTextMatchers.hasMetadataToken(attributes, SearchNodeKeys.TAGS, "c:seeds")
                && !PrimaryCategoryTextMatchers.hasMetadataToken(attributes, SearchNodeKeys.TAGS, "forge:seeds")
                && !PrimaryCategoryTextMatchers.containsPathToken(path, Set.of("seed", "seeds"))) {
            return false;
        }
        PathTokens tokens = PathTokens.of(path);
        return !tokens.containsAny("bag", "bags", "bucket", "oil", "maker", "pouch", "crystal")
                && !tokens.contains("crystal_seed")
                && !tokens.startsWith("roasted")
                && !tokens.startsWith("baked");
    }

    private static boolean isCropLikePlaceable(String path, Set<ItemFacet> facets, Map<String, String> attributes) {
        if (!facets.contains(ItemFacet.PLACEABLE) || isSapling(path, attributes) || isLeaves(path, facets, attributes)) {
            return false;
        }
        if (facets.contains(ItemFacet.FLOWER)) {
            return false;
        }
        String blockClass = attributes.getOrDefault(SearchNodeKeys.BLOCK_CLASS, "").toLowerCase(Locale.ROOT);
        if (path.equals("dead_bush") || blockClass.contains("deadbush")) {
            return false;
        }
        return facets.contains(ItemFacet.CROP)
                || PrimaryCategoryTextMatchers.hasMetadataToken(attributes, SearchNodeKeys.TAGS, "c:crops")
                || PrimaryCategoryTextMatchers.hasMetadataToken(attributes, SearchNodeKeys.TAGS, "forge:crops")
                || PrimaryCategoryTextMatchers.hasMetadataToken(attributes, SearchNodeKeys.BLOCK_TAGS, "c:crops")
                || PrimaryCategoryTextMatchers.hasMetadataToken(attributes, SearchNodeKeys.BLOCK_TAGS, "forge:crops")
                || PathTokens.of(path).containsAny("crop", "bush")
                || blockClass.contains("crop")
                || blockClass.contains("bush");
    }

    private static String classifyIngredientSubcategory(Set<ItemFacet> facets) {
        if (facets.contains(ItemFacet.INGREDIENT_DYE)) return "dyes";
        if (facets.contains(ItemFacet.INGREDIENT_MINERAL)) return "mineral";
        return "organic";
    }

    private static String classifyDecorationSubcategory(Set<ItemFacet> facets) {
        if (facets.contains(ItemFacet.LIGHT_SOURCE)) return "lighting";
        return "furniture";
    }

    private static boolean shouldResolveDecorationFacetPrimary(String path, Set<ItemFacet> facets, Map<String, String> attributes) {
        if (facets.contains(ItemFacet.DECORATIVE_BLOCK)) {
            return true;
        }
        if (facets.contains(ItemFacet.LIGHT_SOURCE)) {
            // Partial-shaped non-functional light sources are decorative fixtures (torches, rods, lanterns, candles).
            // Full-block light sources are handled by isPrimaryLightingPath below.
            String blockShape = attributes.getOrDefault("blockShape", "");
            if ("partial".equals(blockShape)
                    && !facets.contains(ItemFacet.HAS_BLOCK_ENTITY)
                    && !hasAny(facets, ItemFacet.INTERACTIVE_BLOCK, ItemFacet.MACHINE, ItemFacet.WORKSTATION)) {
                return true;
            }
            return isPrimaryLightingPath(path);
        }
        return false;
    }

    private static boolean isPrimaryLightingPath(String path) {
        return PrimaryCategoryTextMatchers.containsPathToken(path, LIGHTING_TOKENS)
                || PrimaryCategoryTextMatchers.containsPathToken(path, Set.of("torch", "torches", "candle", "candles", "glowstone", "shroomlight", "froglight", "beacon"));
    }

    private static String classifyLexicalDecorationSubcategory(String path, Set<ItemFacet> facets) {
        if (facets.contains(ItemFacet.LIGHT_SOURCE) || PrimaryCategoryTextMatchers.containsPathToken(path, LIGHTING_TOKENS)) {
            return "lighting";
        }
        if (PrimaryCategoryTextMatchers.containsPathToken(path, TEXTILE_TOKENS)) {
            return "textiles";
        }
        if (PrimaryCategoryTextMatchers.containsPathToken(path, DISPLAY_TOKENS)) {
            return "furniture";
        }
        return classifyDecorationSubcategory(facets);
    }

    private static String classifySocialSubcategory(Set<ItemFacet> facets) {
        if (facets.contains(ItemFacet.SOCIAL_PLAYERS)) return "players";
        if (facets.contains(ItemFacet.SOCIAL_CLAIMS)) return "claims";
        return "teams";
    }

    private static String classifyLexicalTechSubcategory(String path, Set<ItemFacet> facets) {
        if (facets.contains(ItemFacet.REDSTONE_LOGIC) || facets.contains(ItemFacet.REDSTONE_SIGNAL)) {
            return "redstone";
        }
        if (PrimaryCategoryTextMatchers.containsPathToken(path, Set.of("terminal", "controller", "processor"))) {
            return "parts";
        }
        return classifyTechSubcategory(path, facets);
    }

    private static boolean shouldBiasCreateFamilyToDecoration(PrimaryCategoryModFamily modFamily, Set<ItemFacet> facets) {
        return modFamily == PrimaryCategoryModFamily.CREATE
                && facets.contains(ItemFacet.PLACEABLE)
                && hasAny(facets, ItemFacet.DECORATIVE_BLOCK, ItemFacet.LIGHT_SOURCE)
                && !hasAny(facets,
                ItemFacet.MACHINE,
                ItemFacet.HAS_ENERGY,
                ItemFacet.STORAGE,
                ItemFacet.REDSTONE_LOGIC,
                ItemFacet.REDSTONE_SIGNAL,
                ItemFacet.TRANSPORT);
    }

    private static boolean shouldBiasLexicalDecoration(Set<ItemFacet> facets, String path) {
        return facets.contains(ItemFacet.PLACEABLE)
                && !hasAny(facets,
                ItemFacet.INTERACTIVE_BLOCK,
                ItemFacet.MACHINE,
                ItemFacet.STORAGE,
                ItemFacet.HAS_ENERGY,
                ItemFacet.REDSTONE_LOGIC,
                ItemFacet.REDSTONE_SIGNAL,
                ItemFacet.TRANSPORT,
                ItemFacet.FUNGI,
                ItemFacet.NATURE_MISC,
                ItemFacet.CROP,
                ItemFacet.SEED,
                ItemFacet.LOG,
                ItemFacet.LEAVES,
                ItemFacet.FLOWER)
                && PrimaryCategoryTextMatchers.containsPathToken(path, DECOR_TOKENS);
    }

    private static boolean shouldResolveNaturalBeforeTech(Set<ItemFacet> facets, String path) {
        return facets.contains(ItemFacet.NATURE_MISC)
                && isNaturalCableFalsePositivePath(path);
    }

    private static boolean shouldBiasLexicalWorkstationToTech(Set<ItemFacet> facets, String path) {
        return PrimaryCategoryTextMatchers.containsPathToken(path, WORKSTATION_TOKENS)
                && facets.contains(ItemFacet.PLACEABLE)
                && hasAny(facets,
                ItemFacet.INTERACTIVE_BLOCK,
                ItemFacet.HAS_BLOCK_ENTITY,
                ItemFacet.MACHINE,
                ItemFacet.STORAGE,
                ItemFacet.HAS_ENERGY,
                ItemFacet.REDSTONE_LOGIC,
                ItemFacet.REDSTONE_SIGNAL,
                ItemFacet.TRANSPORT)
                && !hasAny(facets,
                ItemFacet.FUNGI,
                ItemFacet.NATURE_MISC,
                ItemFacet.CROP,
                ItemFacet.SEED,
                ItemFacet.LOG,
                ItemFacet.LEAVES,
                ItemFacet.FLOWER,
                ItemFacet.LIGHT_SOURCE);
    }

    private static boolean shouldBiasCreateFamilyToTech(PrimaryCategoryModFamily modFamily, Set<ItemFacet> facets, String path, Map<String, String> attributes) {
        return modFamily == PrimaryCategoryModFamily.CREATE
                && facets.contains(ItemFacet.PLACEABLE)
                && !hasAny(facets,
                ItemFacet.DECORATIVE_BLOCK,
                ItemFacet.LIGHT_SOURCE,
                ItemFacet.EDIBLE,
                ItemFacet.COMPOSTABLE,
                ItemFacet.SEED,
                ItemFacet.CROP,
                ItemFacet.NATURE_MISC,
                ItemFacet.FUNGI,
                ItemFacet.LOG,
                ItemFacet.LEAVES,
                ItemFacet.FLOWER,
                ItemFacet.INGREDIENT_ORGANIC,
                ItemFacet.INGREDIENT_MINERAL,
                ItemFacet.INGREDIENT_DYE,
                ItemFacet.SOCIAL_PLAYERS,
                ItemFacet.SOCIAL_CLAIMS)
                && (hasAny(facets,
                ItemFacet.RAIL,
                ItemFacet.INTERACTIVE_BLOCK,
                ItemFacet.HAS_BLOCK_ENTITY,
                ItemFacet.MACHINE,
                ItemFacet.STORAGE,
                ItemFacet.HAS_ENERGY,
                ItemFacet.REDSTONE_LOGIC,
                ItemFacet.REDSTONE_SIGNAL,
                ItemFacet.TRANSPORT,
                ItemFacet.CABLE,
                ItemFacet.UPGRADE,
                ItemFacet.TEMPLATE,
                ItemFacet.TECH_COMPONENT,
                ItemFacet.MECHANICAL_COMPONENT)
                || PrimaryCategoryTextMatchers.containsPathToken(path, CREATE_PART_TOKENS)
                || PrimaryCategoryTextMatchers.containsPathToken(path, CREATE_MACHINE_TOKENS)
                || isCreateAddonTechPath(path))
                && !hasAny(facets,
                ItemFacet.STAIRS,
                ItemFacet.SLAB,
                ItemFacet.WALL,
                ItemFacet.FENCE,
                ItemFacet.FENCE_GATE,
                ItemFacet.PANE,
                ItemFacet.DOOR,
                ItemFacet.TRAPDOOR)
                && !shouldBeGeology(facets, path, attributes);
    }

    private static boolean shouldBiasCreateFamilyHandheldToTools(PrimaryCategoryModFamily modFamily, String path) {
        return modFamily == PrimaryCategoryModFamily.CREATE
                && (PrimaryCategoryTextMatchers.containsPathToken(path, CREATE_HANDHELD_TOOL_TOKENS)
                || path.equals("wand_of_symmetry")
                || PrimaryCategoryTextMatchers.endsWithPathToken(path, "gun"));
    }

    private static boolean shouldBiasCreateFamilyHandheldToUtility(PrimaryCategoryModFamily modFamily, String path) {
        return modFamily == PrimaryCategoryModFamily.CREATE
                && (PrimaryCategoryTextMatchers.containsPathToken(path, CREATE_HANDHELD_UTILITY_TOKENS)
                || path.equals("shopping_list"));
    }

    private static boolean shouldBiasCreateEnchantingFamilyToMagic(String modId, String path) {
        return modId.equals("create_enchantment_industry")
                && PrimaryCategoryTextMatchers.containsPathToken(path, CREATE_ENCHANTING_EXPERIENCE_TOKENS);
    }

    private static boolean shouldBiasDecorFamilyToDecoration(PrimaryCategoryModFamily modFamily, Set<ItemFacet> facets, String path, Map<String, String> attributes) {
        return modFamily == PrimaryCategoryModFamily.DECOR
                && facets.contains(ItemFacet.PLACEABLE)
                && !hasAny(facets,
                ItemFacet.MACHINE,
                ItemFacet.HAS_ENERGY,
                ItemFacet.STORAGE,
                ItemFacet.REDSTONE_LOGIC,
                ItemFacet.REDSTONE_SIGNAL,
                ItemFacet.TRANSPORT,
                ItemFacet.NATURE_MISC,
                ItemFacet.FUNGI,
                ItemFacet.FLOWER,
                ItemFacet.LOG,
                ItemFacet.LEAVES)
                && !shouldBeGeology(facets, path, attributes)
                && !shouldBiasUncraftableFullBlockToTerrain(facets, attributes);
    }

    private static boolean shouldBiasArchitecturalPlaceableToBuilding(Set<ItemFacet> facets, String path, Map<String, String> attributes) {
        return facets.contains(ItemFacet.PLACEABLE)
                && !"partial".equals(attributes.getOrDefault("blockShape", ""))
                && !hasAny(facets,
                ItemFacet.MACHINE,
                ItemFacet.HAS_ENERGY,
                ItemFacet.STORAGE,
                ItemFacet.REDSTONE_LOGIC,
                ItemFacet.REDSTONE_SIGNAL,
                ItemFacet.TRANSPORT,
                ItemFacet.NATURE_MISC,
                ItemFacet.FUNGI,
                ItemFacet.FLOWER,
                ItemFacet.LOG,
                ItemFacet.LEAVES,
                ItemFacet.LIGHT_SOURCE)
                && PrimaryCategoryTextMatchers.containsPathToken(path, ARCHITECTURAL_BUILDING_TOKENS);
    }

    private static boolean shouldBiasPortableStorageFamilyToArmor(PrimaryCategoryModFamily modFamily, String path) {
        return modFamily == PrimaryCategoryModFamily.PORTABLE_STORAGE
                && PrimaryCategoryTextMatchers.containsPathToken(path, PORTABLE_STORAGE_ARMOR_TOKENS);
    }

    private static boolean shouldBiasPortableStorageFamilyToTech(PrimaryCategoryModFamily modFamily, String path) {
        return modFamily == PrimaryCategoryModFamily.PORTABLE_STORAGE
                && PrimaryCategoryTextMatchers.containsPathToken(path, PORTABLE_STORAGE_TECH_TOKENS);
    }

    private static boolean shouldBiasStorageFamilyToTech(PrimaryCategoryModFamily modFamily, Set<ItemFacet> facets, String path, Map<String, String> attributes) {
        return modFamily == PrimaryCategoryModFamily.STORAGE
                && !hasStructuralBuildingShape(facets)
                && !hasAny(facets,
                ItemFacet.DECORATIVE_BLOCK,
                ItemFacet.LIGHT_SOURCE,
                ItemFacet.EDIBLE,
                ItemFacet.COMPOSTABLE,
                ItemFacet.SEED,
                ItemFacet.CROP,
                ItemFacet.NATURE_MISC,
                ItemFacet.FUNGI,
                ItemFacet.LOG,
                ItemFacet.LEAVES,
                ItemFacet.FLOWER,
                ItemFacet.INGREDIENT_ORGANIC,
                ItemFacet.INGREDIENT_MINERAL,
                ItemFacet.INGREDIENT_DYE,
                ItemFacet.SOCIAL_PLAYERS,
                ItemFacet.SOCIAL_CLAIMS)
                && (
                facets.contains(ItemFacet.PLACEABLE)
                        || hasAny(facets,
                        ItemFacet.STORAGE,
                        ItemFacet.HAS_BLOCK_ENTITY,
                        ItemFacet.HAS_ENERGY,
                        ItemFacet.INTERACTIVE_BLOCK,
                        ItemFacet.REDSTONE_LOGIC,
                        ItemFacet.REDSTONE_SIGNAL,
                        ItemFacet.TRANSPORT,
                        ItemFacet.CABLE,
                        ItemFacet.UPGRADE,
                        ItemFacet.TEMPLATE,
                        ItemFacet.TECH_COMPONENT,
                        ItemFacet.MECHANICAL_COMPONENT)
                        || PrimaryCategoryTextMatchers.containsPathToken(path, STORAGE_FAMILY_ROUTE_TOKENS)
        )
                && !shouldBeGeology(facets, path, attributes);
    }

    private static boolean shouldBiasFoodFamilyToNature(PrimaryCategoryModFamily modFamily, Set<ItemFacet> facets, String path, Map<String, String> attributes) {
        return modFamily == PrimaryCategoryModFamily.FOOD
                && facets.contains(ItemFacet.PLACEABLE)
                && (
                hasAny(facets,
                        ItemFacet.PLACEABLE_FOOD,
                        ItemFacet.EDIBLE,
                        ItemFacet.COMPOSTABLE,
                        ItemFacet.SEED,
                        ItemFacet.CROP,
                        ItemFacet.NATURE_MISC,
                        ItemFacet.FUNGI,
                        ItemFacet.LOG,
                        ItemFacet.LEAVES,
                        ItemFacet.FLOWER)
                        || isFoodStorageBlockPath(path)
        )
                && !hasAny(facets,
                ItemFacet.DECORATIVE_BLOCK,
                ItemFacet.LIGHT_SOURCE,
                ItemFacet.MACHINE,
                ItemFacet.HAS_BLOCK_ENTITY,
                ItemFacet.HAS_ENERGY,
                ItemFacet.STORAGE,
                ItemFacet.REDSTONE_LOGIC,
                ItemFacet.REDSTONE_SIGNAL,
                ItemFacet.TRANSPORT,
                ItemFacet.SOCIAL_PLAYERS,
                ItemFacet.SOCIAL_CLAIMS)
                && !hasAny(facets,
                ItemFacet.STAIRS,
                ItemFacet.SLAB,
                ItemFacet.WALL,
                ItemFacet.FENCE,
                ItemFacet.FENCE_GATE,
                ItemFacet.PANE,
                ItemFacet.DOOR,
                ItemFacet.TRAPDOOR)
                && !shouldBeGeology(facets, path, attributes)
                && !shouldBiasUncraftableFullBlockToTerrain(facets, attributes);
    }

    private static boolean shouldBiasFoodFamilyPlaceableToNature(PrimaryCategoryModFamily modFamily, Set<ItemFacet> facets, String path, Map<String, String> attributes) {
        return modFamily == PrimaryCategoryModFamily.FOOD
                && facets.contains(ItemFacet.PLACEABLE)
                && !hasAny(facets,
                ItemFacet.MACHINE,
                ItemFacet.HAS_BLOCK_ENTITY,
                ItemFacet.HAS_ENERGY,
                ItemFacet.STORAGE,
                ItemFacet.DECORATIVE_BLOCK,
                ItemFacet.MELEE_WEAPON,
                ItemFacet.RANGED_WEAPON,
                ItemFacet.HARVEST_TOOL,
                ItemFacet.UTILITY_TOOL,
                ItemFacet.REDSTONE_LOGIC,
                ItemFacet.REDSTONE_SIGNAL,
                ItemFacet.TRANSPORT,
                ItemFacet.SOCIAL_PLAYERS,
                ItemFacet.SOCIAL_CLAIMS)
                && !hasAny(facets,
                ItemFacet.STAIRS,
                ItemFacet.SLAB,
                ItemFacet.WALL,
                ItemFacet.FENCE,
                ItemFacet.FENCE_GATE,
                ItemFacet.PANE,
                ItemFacet.DOOR,
                ItemFacet.TRAPDOOR)
                && !shouldBeGeology(facets, path, attributes)
                && !shouldBiasUncraftableFullBlockToTerrain(facets, attributes);
    }

    private static String classifyFoodFamilyNatureSubcategory(String path, Set<ItemFacet> facets) {
        if (isFoodStorageBlockPath(path)) return "crops";
        return classifyNatureSubcategory(path, facets);
    }

    private static boolean shouldBiasFoodFamilyToIngredients(PrimaryCategoryModFamily modFamily, Set<ItemFacet> facets, String path) {
        return modFamily == PrimaryCategoryModFamily.FOOD
                && !facets.contains(ItemFacet.PLACEABLE)
                && !hasAny(facets,
                ItemFacet.UTILITY_TOOL,
                ItemFacet.MELEE_WEAPON,
                ItemFacet.RANGED_WEAPON,
                ItemFacet.HARVEST_TOOL,
                ItemFacet.PROJECTILE,
                ItemFacet.MAGIC_ARTIFACT,
                ItemFacet.MAGIC_REAGENT)
                && (
                PrimaryCategoryTextMatchers.containsPathToken(path, FOOD_FAMILY_INGREDIENT_TOKENS)
        );
    }

    private static boolean shouldBiasFoodFamilyToPreparedFood(PrimaryCategoryModFamily modFamily, Set<ItemFacet> facets, String path) {
        return modFamily == PrimaryCategoryModFamily.FOOD
                && !hasAny(facets,
                ItemFacet.UTILITY_TOOL,
                ItemFacet.MELEE_WEAPON,
                ItemFacet.RANGED_WEAPON,
                ItemFacet.HARVEST_TOOL,
                ItemFacet.PROJECTILE,
                ItemFacet.MAGIC_ARTIFACT,
                ItemFacet.MAGIC_REAGENT)
                && (
                facets.contains(ItemFacet.PLACEABLE_FOOD)
                        || facets.contains(ItemFacet.EDIBLE)
                        || hasPreparedFoodPath(path)
        );
    }

    private static boolean shouldResolveFoodLikeBeforePassiveRedstone(PrimaryCategoryModFamily modFamily, Set<ItemFacet> facets, String path) {
        if (!isPassiveComparatorOnly(facets)) {
            return false;
        }
        if (hasAny(facets,
                ItemFacet.HAS_ENERGY,
                ItemFacet.STORAGE,
                ItemFacet.INTERACTIVE_BLOCK,
                ItemFacet.TRANSPORT,
                ItemFacet.MACHINE,
                ItemFacet.INGOT,
                ItemFacet.GEM,
                ItemFacet.NUGGET,
                ItemFacet.RAW_MATERIAL,
                ItemFacet.DUST)) {
            return false;
        }
        return hasAny(facets,
                ItemFacet.EDIBLE,
                ItemFacet.PLACEABLE_FOOD,
                ItemFacet.FOOD_MEAL,
                ItemFacet.FOOD_DRINK,
                ItemFacet.FOOD_PROTEIN,
                ItemFacet.COMPOSTABLE)
                || (modFamily == PrimaryCategoryModFamily.FOOD && hasPreparedFoodPath(path));
    }

    private static boolean shouldResolveDecorLikeBeforePassiveRedstone(PrimaryCategoryModFamily modFamily, Set<ItemFacet> facets, Map<String, String> attributes) {
        if (!isPassiveComparatorOnly(facets)) {
            return false;
        }
        if (hasAny(facets,
                ItemFacet.HAS_ENERGY,
                ItemFacet.STORAGE,
                ItemFacet.INTERACTIVE_BLOCK,
                ItemFacet.TRANSPORT,
                ItemFacet.MACHINE,
                ItemFacet.INGOT,
                ItemFacet.GEM,
                ItemFacet.NUGGET,
                ItemFacet.RAW_MATERIAL,
                ItemFacet.DUST)) {
            return false;
        }
        if (hasAny(facets,
                ItemFacet.DOOR,
                ItemFacet.TRAPDOOR,
                ItemFacet.FENCE_GATE,
                ItemFacet.RAIL,
                ItemFacet.STAIRS,
                ItemFacet.SLAB,
                ItemFacet.WALL,
                ItemFacet.FENCE,
                ItemFacet.PANE)) {
            return false;
        }
        return facets.contains(ItemFacet.DECORATIVE_BLOCK)
                || (modFamily == PrimaryCategoryModFamily.DECOR
                && facets.contains(ItemFacet.PLACEABLE)
                && !shouldBeGeology(facets, "", attributes));
    }

    private static boolean isPassiveComparatorOnly(Set<ItemFacet> facets) {
        if (facets.contains(ItemFacet.ACTIVE_REDSTONE_LOGIC) || facets.contains(ItemFacet.REDSTONE_LOGIC)) {
            return false;
        }
        return facets.contains(ItemFacet.PASSIVE_COMPARATOR_OUTPUT)
                || facets.contains(ItemFacet.REDSTONE_SIGNAL);
    }

    private static boolean shouldBiasAutomationFamilyToTech(PrimaryCategoryModFamily modFamily, Set<ItemFacet> facets, String path, Map<String, String> attributes) {
        // Exclude plain decorative glass (glass without a block entity means it's not a functional machine component).
        if (facets.contains(ItemFacet.GLASS_BLOCK) && !facets.contains(ItemFacet.HAS_BLOCK_ENTITY)) {
            return false;
        }
        return modFamily == PrimaryCategoryModFamily.AUTOMATION
                && !hasAny(facets,
                ItemFacet.DECORATIVE_BLOCK,
                ItemFacet.LIGHT_SOURCE,
                ItemFacet.EDIBLE,
                ItemFacet.COMPOSTABLE,
                ItemFacet.SEED,
                ItemFacet.CROP,
                ItemFacet.NATURE_MISC,
                ItemFacet.FUNGI,
                ItemFacet.LOG,
                ItemFacet.LEAVES,
                ItemFacet.FLOWER,
                ItemFacet.INGREDIENT_ORGANIC,
                ItemFacet.INGREDIENT_MINERAL,
                ItemFacet.INGREDIENT_DYE,
                ItemFacet.SOCIAL_PLAYERS,
                ItemFacet.SOCIAL_CLAIMS)
                && (
                facets.contains(ItemFacet.PLACEABLE)
                        || hasAny(facets,
                        ItemFacet.STORAGE,
                        ItemFacet.MACHINE,
                        ItemFacet.HAS_BLOCK_ENTITY,
                        ItemFacet.HAS_ENERGY,
                        ItemFacet.CABLE,
                        ItemFacet.TECH_COMPONENT,
                        ItemFacet.MECHANICAL_COMPONENT,
                        ItemFacet.REDSTONE_LOGIC,
                        ItemFacet.REDSTONE_SIGNAL,
                        ItemFacet.TRANSPORT)
                        || PrimaryCategoryTextMatchers.containsPathToken(path, AUTOMATION_ROUTE_TOKENS)
        )
                && !shouldBeGeology(facets, path, attributes)
                && !shouldBiasUncraftableFullBlockToTerrain(facets, attributes);
    }

    private static String classifyStorageSubcategory(String path, Set<ItemFacet> facets) {
        PathTokens tokens = PathTokens.of(path);
        if (facets.contains(ItemFacet.UPGRADE) || tokens.contains("upgrade")) {
            return "upgrades";
        }
        if (facets.contains(ItemFacet.TEMPLATE) || tokens.containsAny("pattern", "template")) {
            return "templates";
        }
        if (facets.contains(ItemFacet.CABLE) || tokens.containsAny("cable", "bus")) {
            return "cables";
        }
        if (tokens.contains("quartz_enriched_iron")) {
            return "ingots";
        }
        if (tokens.containsAny(STORAGE_CIRCUIT_TOKENS)) {
            return "circuits";
        }
        if (tokens.containsAny(STORAGE_MEDIA_PART_TOKENS)) {
            return "parts";
        }
        if (tokens.containsAny("grid", "monitor", "wireless")) {
            return "machines";
        }
        if (facets.contains(ItemFacet.PLACEABLE) || facets.contains(ItemFacet.HAS_BLOCK_ENTITY)) {
            return "machines";
        }
        if (facets.contains(ItemFacet.STORAGE) || tokens.containsAny(STORAGE_MACHINE_TOKENS)) {
            return "machines";
        }
        if (tokens.containsAny(STORAGE_NETWORK_PART_TOKENS)) {
            return "parts";
        }
        return "machines";
    }

    private static String classifyCreateFamilyTechSubcategory(String modId, String path, Set<ItemFacet> facets) {
        if (modId.equals("railways")) {
            if (PrimaryCategoryTextMatchers.containsPathToken(path, RAILWAYS_TRANSPORT_TOKENS)) {
                return "transport";
            }
            if (PrimaryCategoryTextMatchers.containsPathToken(path, RAILWAYS_PART_TOKENS)) {
                return "parts";
            }
        }
        if (modId.equals("createaddition")
                || modId.equals("create_new_age")
                || modId.equals("new_age")) {
            if (PrimaryCategoryTextMatchers.containsPathToken(path, CREATE_ADDON_MACHINE_TOKENS)) {
                return "machines";
            }
            if (facets.contains(ItemFacet.CABLE)) {
                return "cables";
            }
            if (PrimaryCategoryTextMatchers.containsPathToken(path, CREATE_ADDON_PART_TOKENS)) {
                return "parts";
            }
        }
        if (modId.equals("create_winery")) {
            if (PrimaryCategoryTextMatchers.containsPathToken(path, Set.of("bottle"))) {
                return "transport";
            }
            if (PrimaryCategoryTextMatchers.containsPathToken(path, CREATE_WINERY_MACHINE_TOKENS)) {
                return "machines";
            }
        }
        if (modId.equals("createoreexcavation")) {
            if (PrimaryCategoryTextMatchers.containsPathToken(path, CREATE_ORE_MACHINE_TOKENS)) {
                return "machines";
            }
            if (PrimaryCategoryTextMatchers.containsPathToken(path, CREATE_ORE_PART_TOKENS)) {
                return "parts";
            }
        }
        if (facets.contains(ItemFacet.TRANSPORT)) {
            return "transport";
        }
        if (facets.contains(ItemFacet.REDSTONE_LOGIC) || facets.contains(ItemFacet.REDSTONE_SIGNAL)) {
            return "redstone";
        }
        if (facets.contains(ItemFacet.UPGRADE) || PrimaryCategoryTextMatchers.containsPathToken(path, Set.of("upgrade"))) {
            return "upgrades";
        }
        if (facets.contains(ItemFacet.TEMPLATE) || isTemplatePath(path)) {
            return "templates";
        }
        if (facets.contains(ItemFacet.CABLE)) {
            return "cables";
        }
        if (PrimaryCategoryTextMatchers.containsPathToken(path, CREATE_PART_TOKENS)) {
            return "parts";
        }
        if (PrimaryCategoryTextMatchers.containsPathToken(path, CREATE_MACHINE_TOKENS)) {
            return "machines";
        }
        return classifyTechSubcategory(path, facets);
    }

    private static String classifyFoodFamilyIngredientSubcategory(String path, Set<ItemFacet> facets) {
        if (facets.contains(ItemFacet.INGREDIENT_DYE)) return "dyes";
        if (facets.contains(ItemFacet.INGREDIENT_MINERAL)) return "mineral";
        return "organic";
    }

    private static String classifyFoodFamilyPreparedSubcategory(String path, Set<ItemFacet> facets) {
        if (facets.contains(ItemFacet.FOOD_MEAL)
                || hasPreparedMealPath(path)) {
            return "meals";
        }
        if (facets.contains(ItemFacet.FOOD_DRINK) || PrimaryCategoryTextMatchers.containsPathToken(path, Set.of("bottle"))) {
            return "drinks";
        }
        if (facets.contains(ItemFacet.FOOD_PROTEIN)) {
            return "proteins";
        }
        return "snacks";
    }

    private static boolean hasPreparedFoodPath(String path) {
        return hasPreparedMealPath(path)
                || PathTokens.of(path).containsAny("bottle", "on_a_stick");
    }

    private static boolean hasPreparedMealPath(String path) {
        return PathTokens.of(path).containsAny(
                "plate", "plated", "bowl", "pie", "tart", "pudding", "calzone", "sandwich",
                "parmesan", "sausage", "burger", "pasta", "dessert", "patty", "pizza",
                "noodle", "rice", "kebab", "canned", "mre", "macandcheese", "on_a_stick", "soup", "stew");
    }

    private static boolean isFoodStorageBlockPath(String path) {
        return PrimaryCategoryTextMatchers.containsPathToken(path, FOOD_STORAGE_TOKENS);
    }

    private static String classifyAutomationSubcategory(String path, Set<ItemFacet> facets) {
        PathTokens tokens = PathTokens.of(path);
        if (facets.contains(ItemFacet.CABLE) || tokens.containsAny("tube", "cable")) {
            return "cables";
        }
        if (tokens.containsAny(AUTOMATION_CIRCUIT_TOKENS)) {
            return "circuits";
        }
        if (tokens.containsAny(AUTOMATION_PART_TOKENS)) {
            return "parts";
        }
        if (facets.contains(ItemFacet.STORAGE)
                || facets.contains(ItemFacet.MACHINE)
                || tokens.containsAny(AUTOMATION_MACHINE_TOKENS)) {
            return "machines";
        }
        return "parts";
    }

    private static boolean shouldBiasOrganicSurfaceBlockToNature(Set<ItemFacet> facets, String path, Map<String, String> attributes) {
        if (!facets.contains(ItemFacet.PLACEABLE)) {
            return false;
        }
        if (hasAny(facets,
                ItemFacet.STAIRS,
                ItemFacet.SLAB,
                ItemFacet.WALL,
                ItemFacet.FENCE,
                ItemFacet.FENCE_GATE,
                ItemFacet.PANE,
                ItemFacet.DOOR,
                ItemFacet.TRAPDOOR)) {
            return false;
        }
        String blocksMaterial = attributes.getOrDefault(SearchNodeKeys.BLOCKS_MATERIAL, "");
        if ("stone".equals(blocksMaterial)
                && !hasAny(facets,
                ItemFacet.FUNGI,
                ItemFacet.NATURE_MISC,
                ItemFacet.FLOWER,
                ItemFacet.LEAVES,
                ItemFacet.LOG,
                ItemFacet.COMPOSTABLE,
                ItemFacet.SOIL_BLOCK)) {
            return false;
        }
        return facets.contains(ItemFacet.FUNGI)
                || PrimaryCategoryTextMatchers.containsPathToken(path, GEOLOGY_SURFACE_ORGANIC_TOKENS)
                || (blocksMaterial.equals("soil") && PrimaryCategoryTextMatchers.containsPathToken(path, Set.of("grass")));
    }

    private static String classifyOrganicSurfaceBlockSubcategory(String path, Set<ItemFacet> facets) {
        if (facets.contains(ItemFacet.FUNGI)
                || PrimaryCategoryTextMatchers.containsPathToken(path, GEOLOGY_FUNGI_TOKENS)) {
            return "fungi";
        }
        return "flora";
    }

    private static boolean shouldBiasGeologyFamilyToDecoration(Set<ItemFacet> facets, String path, Map<String, String> attributes) {
        if (!facets.contains(ItemFacet.PLACEABLE)) {
            return false;
        }
        String blockShape = attributes.getOrDefault("blockShape", "");
        if ("partial".equals(blockShape)) {
            return false;
        }
        if (hasAny(facets,
                ItemFacet.INTERACTIVE_BLOCK,
                ItemFacet.HAS_BLOCK_ENTITY,
                ItemFacet.MACHINE,
                ItemFacet.STORAGE,
                ItemFacet.HAS_ENERGY,
                ItemFacet.REDSTONE_LOGIC,
                ItemFacet.REDSTONE_SIGNAL,
                ItemFacet.TRANSPORT,
                ItemFacet.FUNGI,
                ItemFacet.NATURE_MISC,
                ItemFacet.FLOWER,
                ItemFacet.LOG,
                ItemFacet.LEAVES)) {
            return false;
        }
        String blocksMaterial = attributes.getOrDefault(SearchNodeKeys.BLOCKS_MATERIAL, "");
        return PrimaryCategoryTextMatchers.containsPathToken(path, GEOLOGY_DECORATION_TOKENS)
                || PrimaryCategoryTextMatchers.containsPathToken(path, DECOR_TOKENS)
                || (facets.contains(ItemFacet.PANE) && (blocksMaterial.equals("glass") || PrimaryCategoryTextMatchers.containsPathToken(path, Set.of("glass"))));
    }

    private static boolean shouldBiasGeologyFamilyToMasonry(Set<ItemFacet> facets, String path, Map<String, String> attributes) {
        if (!facets.contains(ItemFacet.PLACEABLE)) {
            return false;
        }
        String blockShape = attributes.getOrDefault("blockShape", "");
        if ("partial".equals(blockShape)) {
            return false;
        }
        if (hasAny(facets,
                ItemFacet.RAIL,
                ItemFacet.INTERACTIVE_BLOCK,
                ItemFacet.HAS_BLOCK_ENTITY,
                ItemFacet.MACHINE,
                ItemFacet.STORAGE,
                ItemFacet.HAS_ENERGY,
                ItemFacet.REDSTONE_LOGIC,
                ItemFacet.REDSTONE_SIGNAL,
                ItemFacet.TRANSPORT)) {
            return false;
        }
        return hasAny(facets,
                ItemFacet.STAIRS,
                ItemFacet.SLAB,
                ItemFacet.WALL,
                ItemFacet.FENCE,
                ItemFacet.FENCE_GATE,
                ItemFacet.PANE,
                ItemFacet.DOOR,
                ItemFacet.TRAPDOOR)
                || blockShape.equals("stairs")
                || blockShape.equals("slab")
                || blockShape.equals("wall")
                || blockShape.equals("fence")
                || blockShape.equals("fence_gate")
                || blockShape.equals("pane")
                || blockShape.equals("door")
                || blockShape.equals("trapdoor")
                || PrimaryCategoryTextMatchers.containsPathToken(path, GEOLOGY_MASONRY_TOKENS);
    }

    private static boolean isLikelyPartialBuildingPlaceable(Set<ItemFacet> facets, String path, Map<String, String> attributes) {
        if (!facets.contains(ItemFacet.PLACEABLE)) {
            return false;
        }
        String blockShape = attributes.getOrDefault("blockShape", "");
        return "partial".equals(blockShape);
    }

    private static boolean isLikelyDecorativeMicroPlaceable(Set<ItemFacet> facets, String path, Map<String, String> attributes) {
        if (!facets.contains(ItemFacet.PLACEABLE)) {
            return false;
        }
        if (hasAny(facets,
                ItemFacet.INTERACTIVE_BLOCK,
                ItemFacet.MACHINE,
                ItemFacet.WORKSTATION,
                ItemFacet.STORAGE,
                ItemFacet.HAS_ENERGY,
                ItemFacet.REDSTONE_LOGIC,
                ItemFacet.REDSTONE_SIGNAL,
                ItemFacet.TRANSPORT,
                ItemFacet.CABLE,
                ItemFacet.TECH_COMPONENT,
                ItemFacet.MECHANICAL_COMPONENT)) {
            return false;
        }
        String blockShape = attributes.getOrDefault("blockShape", "");
        if (!"partial".equals(blockShape)) {
            return false;
        }
        if (facets.contains(ItemFacet.LIGHT_SOURCE)) {
            return false;
        }
        String properties = attributes.getOrDefault(SearchNodeKeys.BLOCK_STATE_PROPERTIES, "");
        if (hasMicroPartialStateHints(properties)) {
            return true;
        }
        return hasMicroPartialPathOrClassSignals(path, attributes.getOrDefault(SearchNodeKeys.BLOCK_CLASS, ""));
    }

    private static boolean isLikelyFunctionalPartialPlaceable(Set<ItemFacet> facets, String path, Map<String, String> attributes) {
        if (!facets.contains(ItemFacet.PLACEABLE)) {
            return false;
        }
        String blockShape = attributes.getOrDefault("blockShape", "");
        if (!"partial".equals(blockShape)) {
            return false;
        }
        if (isLikelyDecorativeMicroPlaceable(facets, path, attributes)) {
            return false;
        }
        return hasMicroPartialTechSignals(path, attributes.getOrDefault(SearchNodeKeys.BLOCK_CLASS, ""));
    }

    private static boolean isLikelyNaturePartialPlaceable(Set<ItemFacet> facets, String path, Map<String, String> attributes) {
        if (!facets.contains(ItemFacet.PLACEABLE)) {
            return false;
        }
        String blockShape = attributes.getOrDefault("blockShape", "");
        if (!"partial".equals(blockShape)) {
            return false;
        }
        if (hasAny(facets, ItemFacet.NATURE_MISC, ItemFacet.LEAVES, ItemFacet.FLOWER, ItemFacet.FUNGI)) {
            return true;
        }
        if (PrimaryCategoryTextMatchers.hasMetadataToken(attributes, SearchNodeKeys.TAGS, "minecraft:leaves")
                || PrimaryCategoryTextMatchers.hasMetadataToken(attributes, SearchNodeKeys.BLOCK_TAGS, "minecraft:leaves")) {
            return true;
        }
        String encodedProperties = attributes.getOrDefault(SearchNodeKeys.BLOCK_STATE_PROPERTIES, "");
        if (PrimaryCategoryTextMatchers.hasCsvToken(encodedProperties, "moisture") || PrimaryCategoryTextMatchers.hasCsvToken(encodedProperties, "layers")) {
            return true;
        }
        String normalizedClass = attributes.getOrDefault(SearchNodeKeys.BLOCK_CLASS, "").toLowerCase(Locale.ROOT);
        return PrimaryCategoryTextMatchers.containsPathToken(path, MICRO_PARTIAL_NATURE_PATH_HINTS)
                || hasMicroPartialNatureClassSignals(normalizedClass);
    }

    private static boolean hasMicroPartialStateHints(String encodedProperties) {
        if (encodedProperties == null || encodedProperties.isBlank()) {
            return false;
        }
        String[] properties = encodedProperties.split(",");
        int meaningfulHints = 0;
        for (String rawProperty : properties) {
            String property = rawProperty.trim();
            if (property.isBlank()) {
                continue;
            }
            if (property.startsWith("connect_")) {
                return true;
            }
            if (MICRO_PARTIAL_STATE_HINTS.contains(property)) {
                meaningfulHints++;
            }
        }
        // Single-property partials like facing-only or axis-only are often edge cases.
        // Keep them masonry unless they also provide stronger class/path hints.
        return meaningfulHints >= 2;
    }

    private static boolean hasMicroPartialNatureClassSignals(String normalizedClass) {
        if (normalizedClass == null || normalizedClass.isBlank()) {
            return false;
        }
        return MICRO_PARTIAL_NATURE_CLASS_HINTS.stream().anyMatch(normalizedClass::contains);
    }

    private static boolean hasMicroPartialPathOrClassSignals(String path, String blockClass) {
        if (PrimaryCategoryTextMatchers.containsPathToken(path, MICRO_PARTIAL_PATH_HINTS)) {
            return true;
        }
        String normalizedClass = blockClass.toLowerCase(Locale.ROOT);
        return MICRO_PARTIAL_CLASS_HINTS.stream().anyMatch(normalizedClass::contains);
    }

    private static boolean hasMicroPartialTechSignals(String path, String blockClass) {
        if (PrimaryCategoryTextMatchers.containsPathToken(path, MICRO_PARTIAL_TECH_HINTS)) {
            return true;
        }
        String normalizedClass = blockClass.toLowerCase(Locale.ROOT);
        return MICRO_PARTIAL_TECH_HINTS.stream().anyMatch(normalizedClass::contains);
    }

    private static boolean isLikelyFullMasonryCandidate(Set<ItemFacet> facets, String path, Map<String, String> attributes) {
        if (!facets.contains(ItemFacet.PLACEABLE)) {
            return false;
        }
        String blockShape = attributes.getOrDefault("blockShape", "");
        return !"partial".equals(blockShape);
    }

    private static boolean isCablePath(String path) {
        PathTokens tokens = PathTokens.of(path);
        return !isNaturalCableFalsePositivePath(path)
                && tokens.containsAny("cable", "wire", "wirecoil", "pipe", "tube", "conduit", "duct")
                && !tokens.contains("wire_cut");
    }

    private static boolean isNaturalCableFalsePositivePath(String path) {
        PathTokens tokens = PathTokens.of(path);
        return tokens.containsAny("coral", "cobweb", "dead_bush", "frogspawn", "sculk");
    }

    private static boolean isTemplatePath(String path) {
        return PathTokens.of(path).containsAny("blueprint", "schematic", "mold", "pattern", "template");
    }

    private static boolean isCreateAddonTechPath(String path) {
        return PrimaryCategoryTextMatchers.containsPathToken(path, RAILWAYS_TRANSPORT_TOKENS)
                || PrimaryCategoryTextMatchers.containsPathToken(path, RAILWAYS_PART_TOKENS)
                || PrimaryCategoryTextMatchers.containsPathToken(path, CREATE_ADDON_MACHINE_TOKENS)
                || PrimaryCategoryTextMatchers.containsPathToken(path, CREATE_ADDON_PART_TOKENS)
                || PrimaryCategoryTextMatchers.containsPathToken(path, CREATE_WINERY_MACHINE_TOKENS)
                || PrimaryCategoryTextMatchers.containsPathToken(path, CREATE_ORE_MACHINE_TOKENS)
                || PrimaryCategoryTextMatchers.containsPathToken(path, CREATE_ORE_PART_TOKENS);
    }

    private static boolean shouldBeGeology(Set<ItemFacet> facets, String path, Map<String, String> attributes) {
        String blocksMaterial = attributes.getOrDefault(SearchNodeKeys.BLOCKS_MATERIAL, "");
        if (facets.contains(ItemFacet.STONE_BLOCK) || "stone".equals(blocksMaterial)) {
            return !hasAny(facets,
                    ItemFacet.STAIRS,
                    ItemFacet.SLAB,
                    ItemFacet.WALL,
                    ItemFacet.FENCE,
                    ItemFacet.FENCE_GATE,
                    ItemFacet.PANE,
                    ItemFacet.DOOR,
                    ItemFacet.TRAPDOOR)
                    && !PrimaryCategoryTextMatchers.containsPathToken(path, GEOLOGY_STONE_DECORATION_TOKENS);
        }
        if (facets.contains(ItemFacet.SOIL_BLOCK)) {
            return !PrimaryCategoryTextMatchers.containsPathToken(path, GEOLOGY_SOIL_DECORATION_TOKENS);
        }
        return false;
    }

    private static boolean shouldBiasUncraftableFullBlockToTerrain(Set<ItemFacet> facets, Map<String, String> attributes) {
        if (!facets.contains(ItemFacet.PLACEABLE)) {
            return false;
        }
        if (!"no_recipe".equals(attributes.getOrDefault(SearchNodeKeys.OBTAINABILITY, ""))) {
            return false;
        }
        if (hasAny(facets,
                ItemFacet.HAS_BLOCK_ENTITY,
                ItemFacet.INTERACTIVE_BLOCK,
                ItemFacet.LIGHT_SOURCE,
                ItemFacet.DECORATIVE_BLOCK,
                ItemFacet.MACHINE,
                ItemFacet.WORKSTATION,
                ItemFacet.STORAGE,
                ItemFacet.HAS_ENERGY,
                ItemFacet.ACTIVE_REDSTONE_LOGIC,
                ItemFacet.PASSIVE_COMPARATOR_OUTPUT,
                ItemFacet.REDSTONE_LOGIC,
                ItemFacet.REDSTONE_SIGNAL,
                ItemFacet.TRANSPORT,
                ItemFacet.CABLE,
                ItemFacet.UPGRADE,
                ItemFacet.TEMPLATE,
                ItemFacet.TECH_COMPONENT,
                ItemFacet.MECHANICAL_COMPONENT,
                ItemFacet.MAGIC_ARTIFACT,
                ItemFacet.MAGIC_REAGENT,
                ItemFacet.UTILITY_NAVIGATION,
                ItemFacet.UTILITY_MEDICAL,
                ItemFacet.UTILITY_CURRENCY,
                ItemFacet.UTILITY_MISC,
                ItemFacet.SEED,
                ItemFacet.CROP,
                ItemFacet.NATURE_MISC,
                ItemFacet.FUNGI,
                ItemFacet.LOG,
                ItemFacet.LEAVES,
                ItemFacet.FLOWER,
                ItemFacet.GLASS_BLOCK)) {
            return false;
        }
        String blockShape = attributes.getOrDefault("blockShape", "");
        if (!blockShape.isBlank() && !"full_block".equals(blockShape)) {
            return false;
        }
        String blocksMaterial = attributes.getOrDefault(SearchNodeKeys.BLOCKS_MATERIAL, "");
        return facets.contains(ItemFacet.SOIL_BLOCK)
                || facets.contains(ItemFacet.STONE_BLOCK)
                || "soil".equals(blocksMaterial)
                || "stone".equals(blocksMaterial);
    }

    private static String classifyUncraftableTerrainSubcategory(Set<ItemFacet> facets, Map<String, String> attributes) {
        if (facets.contains(ItemFacet.SOIL_BLOCK)) {
            return "terrain";
        }
        String blocksMaterial = attributes.getOrDefault(SearchNodeKeys.BLOCKS_MATERIAL, "");
        if ("stone".equals(blocksMaterial) || facets.contains(ItemFacet.STONE_BLOCK)) {
            return "stone";
        }
        return "terrain";
    }

    private static String classifyMasonrySubcategory(Set<ItemFacet> facets, String path, Map<String, String> attributes) {
        if (hasAny(facets, ItemFacet.DOOR, ItemFacet.TRAPDOOR, ItemFacet.FENCE_GATE)) {
            return "functional";
        }
        if (facets.contains(ItemFacet.REDSTONE_LOGIC) || facets.contains(ItemFacet.REDSTONE_SIGNAL)) {
            return "redstone";
        }
        if (facets.contains(ItemFacet.STAIRS)) return "stairs";
        if (facets.contains(ItemFacet.SLAB)) return "slab";
        if (facets.contains(ItemFacet.WALL)) return "wall";
        if (facets.contains(ItemFacet.FENCE) || facets.contains(ItemFacet.FENCE_GATE)) return "fence";
        if (facets.contains(ItemFacet.PANE)) return "pane";
        PathTokens tokens = PathTokens.of(path);
        if (tokens.containsAny("stairs", "stair")) return "stairs";
        if (tokens.contains("slab")) return "slab";
        if (tokens.contains("wall")) return "wall";
        if (tokens.containsAny("fence", "railing", "banister")) return "fence";
        if (tokens.containsAny("pane", "window")) return "pane";
        if (tokens.containsAny("door", "trapdoor")) return "functional";

        String blockShape = attributes.getOrDefault("blockShape", "");
        if (!blockShape.isBlank()) {
            return switch (blockShape) {
                case "stairs", "slab", "wall", "fence", "pane", "door", "trapdoor", "fence_gate" ->
                        blockShape.equals("fence_gate") ? "fence" : blockShape;
                case "partial" -> "other_building";
                default -> "full_block";
            };
        }
        return "full_block";
    }

    private static boolean shouldUseCompatRouteMetadata(ResolveContext context) {
        if (context.categoryPolicy == AmiConfig.CompatCategoryPolicy.SEMANTIC) {
            return false;
        }
        if (shouldUseEarlyCompatRouteMetadata(context)) {
            return false;
        }
        String category = context.attributes.getOrDefault(SearchNodeKeys.COMPAT_ROUTE_CATEGORY, "");
        String subcategory = context.attributes.getOrDefault(SearchNodeKeys.COMPAT_ROUTE_SUBCATEGORY, "");
        return category != null && !category.isBlank()
                && subcategory != null && !subcategory.isBlank();
    }

    private static boolean shouldUseEarlyCompatRouteMetadata(ResolveContext context) {
        if (context.categoryPolicy == AmiConfig.CompatCategoryPolicy.SEMANTIC) {
            return false;
        }
        String category = context.attributes.getOrDefault(SearchNodeKeys.COMPAT_ROUTE_CATEGORY, "");
        String subcategory = context.attributes.getOrDefault(SearchNodeKeys.COMPAT_ROUTE_SUBCATEGORY, "");
        if (category == null || category.isBlank() || subcategory == null || subcategory.isBlank()) {
            return false;
        }
        if (context.facets.contains(ItemFacet.GUIDE_BOOK) || context.facets.contains(ItemFacet.BOOK)) {
            return false;
        }
        return context.categoryPolicy == AmiConfig.CompatCategoryPolicy.FOCUSED
                || "halcyon".equals(category)
                || "swem".equals(category);
    }

    private static CategoryAssignment compatRouteAssignment(ResolveContext context) {
        return assignment(
                context.attributes.get(SearchNodeKeys.COMPAT_ROUTE_CATEGORY),
                context.attributes.get(SearchNodeKeys.COMPAT_ROUTE_SUBCATEGORY),
                context.attributes);
    }

    private record ResolveContext(ResourceLocation id,
                                  String modId,
                                  String path,
                                  Set<ItemFacet> facets,
                                  Map<String, String> attributes,
                                  PrimaryCategoryModFamily modFamily,
                                  AmiConfig.CompatCategoryPolicy categoryPolicy) {
    }

    private record PrimaryRule(String id,
                               Predicate<ResolveContext> matches,
                               Function<ResolveContext, CategoryAssignment> assignment) {
    }
}
