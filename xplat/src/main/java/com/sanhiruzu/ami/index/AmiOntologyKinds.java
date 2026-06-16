package com.sanhiruzu.ami.index;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.function.Predicate;

/**
 * Curated third-level ontology buckets for Category view.
 * Runtime metadata is only used to place nodes into these known buckets.
 */
public final class AmiOntologyKinds {
    private static final List<Rule> RULES = List.of(
            rule("masonry", "functional", "doors", "Doors", c -> c.hasFacet(ItemFacet.DOOR) || c.shape("door") || c.variant("doors") || c.pathToken("door")),
            rule("masonry", "functional", "trapdoors", "Trapdoors", c -> c.hasFacet(ItemFacet.TRAPDOOR) || c.shape("trapdoor") || c.variant("trapdoors") || c.pathToken("trapdoor")),
            rule("masonry", "functional", "fence_gates", "Fence Gates", c -> c.hasFacet(ItemFacet.FENCE_GATE) || c.shape("fence_gate") || c.variant("fence_gates")),
            rule("masonry", "functional", "mats", "Mats & Grates", c -> c.pathToken("doormat", "mat", "grate")),
            rule("masonry", "functional", "ladders", "Ladders", c -> c.pathToken("ladder")),
            rule("masonry", "functional", "scaffolding", "Scaffolding", c -> c.pathToken("scaffold", "scaffolding")),
            rule("masonry", "functional", "work_blocks", "Work Blocks", c -> c.pathToken("crafting_table", "stonecutter", "loom", "smithing_table", "cartography_table")),
            rule("masonry", "functional", "containers", "Containers", c -> c.pathToken("barrel", "chest", "crate", "shelf", "drawer")),

            rule("masonry", "full_block", "planks", "Planks", c -> c.pathToken("planks")),
            rule("masonry", "full_block", "bricks", "Bricks", c -> c.pathToken("bricks", "brick")),
            rule("masonry", "full_block", "tiles", "Tiles", c -> c.pathToken("tiles", "tile")),
            rule("masonry", "full_block", "shingles", "Shingles", c -> c.pathToken("shingle", "shingles")),
            rule("masonry", "full_block", "pillars", "Pillars", c -> c.pathToken("pillar", "column")),
            rule("masonry", "full_block", "beams", "Beams & Supports", c -> c.pathToken("beam", "support", "baseboard", "platform")),
            rule("masonry", "full_block", "flooring", "Flooring & Paths", c -> c.pathToken("floorboard", "path", "paving")),
            rule("masonry", "full_block", "patterns", "Patterns & Hazard Stripes", c -> c.pathToken("stripe", "stripes", "chevron", "pattern")),
            rule("masonry", "full_block", "chiseled", "Chiseled Blocks", c -> c.pathToken("chiseled", "carved")),
            rule("masonry", "full_block", "polished", "Polished Blocks", c -> c.pathToken("polished", "smooth", "cut")),
            rule("masonry", "full_block", "resource_blocks", "Resource Blocks", c -> c.pathEndsWith("_block") && c.pathToken("iron", "gold", "copper", "diamond", "emerald", "lapis", "redstone", "coal", "netherite", "zinc", "lead", "silver", "tin", "nickel", "uranium")),

            rule("masonry", "redstone", "buttons", "Buttons", c -> c.pathToken("button")),
            rule("masonry", "redstone", "pressure_plates", "Pressure Plates", c -> c.pathToken("pressure_plate")),
            rule("masonry", "redstone", "levers", "Levers", c -> c.pathToken("lever")),
            rule("masonry", "redstone", "sensors", "Sensors", c -> c.pathToken("sensor", "detector")),
            rule("masonry", "redstone", "logic_blocks", "Logic Blocks", c -> c.pathToken("repeater", "comparator", "observer", "piston", "dispenser", "dropper", "hopper")),
            rule("masonry", "redstone", "lamps", "Lamps", c -> c.pathToken("lamp", "light")),

            rule("decoration", "furniture", "chairs", "Chairs & Seating", c -> c.pathToken("chair", "seat", "stool", "bench", "sofa", "couch")),
            rule("decoration", "furniture", "tables", "Tables & Desks", c -> c.pathToken("table", "desk")),
            rule("decoration", "furniture", "cabinets", "Cabinets & Drawers", c -> c.pathToken("cabinet", "drawer", "dresser", "nightstand", "wardrobe", "cupboard")),
            rule("decoration", "furniture", "shelves", "Shelves & Racks", c -> c.pathToken("shelf", "shelves", "rack", "bookcase", "bookshelf")),
            rule("decoration", "furniture", "counters", "Counters", c -> c.pathToken("counter", "kitchen_counter")),
            rule("decoration", "furniture", "kitchen", "Kitchen Fixtures", c -> c.pathToken("sink", "basin", "cutting_board", "jar", "stove", "oven")),
            rule("decoration", "furniture", "bathroom", "Bathroom Fixtures", c -> c.pathToken("bath", "toilet")),
            rule("decoration", "furniture", "mailboxes", "Mailboxes", c -> c.pathToken("mailbox", "mail_box")),
            rule("decoration", "furniture", "crates", "Crates", c -> c.pathToken("crate")),
            rule("decoration", "furniture", "beds", "Beds", c -> c.pathToken("bed")),
            rule("decoration", "furniture", "signs", "Signs & Plaques", c -> c.pathToken("sign", "plaque")),
            rule("decoration", "furniture", "stands", "Stands & Displays", c -> c.pathToken("stand", "display", "board", "dartboard")),
            rule("decoration", "furniture", "mats", "Mats & Carpets", c -> c.pathToken("mat", "carpet", "leaf_carpet")),
            rule("decoration", "furniture", "plushies", "Plushies & Figurines", c -> c.pathToken("plush", "plushie", "statue", "figurine", "trophy", "friend", "_on_head")),

            rule("decoration", "lighting", "torches", "Torches", c -> c.pathToken("torch")),
            rule("decoration", "lighting", "lanterns", "Lanterns", c -> c.pathToken("lantern")),
            rule("decoration", "lighting", "lamps", "Lamps", c -> c.pathToken("lamp", "light", "night_light", "pendant")),
            rule("decoration", "lighting", "candles", "Candles", c -> c.pathToken("candle", "candelabra")),
            rule("decoration", "lighting", "braziers", "Braziers", c -> c.pathToken("brazier", "sconce", "chandelier")),
            rule("decoration", "lighting", "chimes", "Chimes", c -> c.pathToken("chime", "chimes")),
            rule("decoration", "lighting", "beacons", "Beacons", c -> c.pathToken("beacon")),
            rule("decoration", "lighting", "glowing_blocks", "Glowing Blocks", c -> c.pathToken("glass", "bifrost", "cluster")),

            rule("decoration", "textiles", "banners", "Banners", c -> c.pathToken("banner")),
            rule("decoration", "textiles", "carpets", "Carpets & Rugs", c -> c.pathToken("carpet", "rug")),
            rule("decoration", "textiles", "curtains", "Curtains & Blinds", c -> c.pathToken("curtain", "curtains", "blind", "blinds", "shutter", "shutters")),
            rule("decoration", "textiles", "cushions", "Cushions & Pillows", c -> c.pathToken("cushion", "pillow")),
            rule("decoration", "textiles", "wallpaper", "Wallpaper", c -> c.pathToken("wallpaper")),

            rule("tech", "machines", "generators", "Generators", c -> c.pathToken("generator", "dynamo", "alternator", "engine")),
            rule("tech", "machines", "gearboxes", "Gearboxes", c -> c.pathToken("gearbox", "gearcube", "gearshift")),
            rule("tech", "machines", "processors", "Processors", c -> c.pathToken("crusher", "grinder", "pulverizer", "processor", "press", "mixer", "sawmill", "centrifuge")),
            rule("tech", "machines", "crafters", "Crafters & Assemblers", c -> c.pathToken("crafter", "assembler", "fabricator", "workbench")),
            rule("tech", "machines", "fluid_machines", "Fluid Machines", c -> c.pathToken("pump", "tank", "drain", "spout", "fluid", "pipe")),
            rule("tech", "machines", "storage_machines", "Storage Machines", c -> c.pathToken("storage", "drawer", "controller", "interface", "cabinet", "chest", "barrel", "crate")),
            rule("tech", "machines", "displays", "Displays", c -> c.pathToken("display", "board")),
            rule("tech", "machines", "switches", "Switches", c -> c.pathToken("switch")),
            rule("tech", "machines", "jukeboxes", "Jukeboxes", c -> c.pathToken("jukebox")),
            rule("tech", "machines", "furniture_machines", "Furniture Machines", c -> c.pathToken("shelf", "bureau", "desk")),
            rule("tech", "machines", "chargers", "Chargers & Batteries", c -> c.pathToken("charger", "battery", "accumulator", "capacitor")),

            rule("tech", "parts", "gears", "Gears & Cogwheels", c -> c.pathToken("gear", "gearbox", "cogwheel")),
            rule("tech", "parts", "shafts", "Shafts & Rods", c -> c.pathToken("shaft", "rod")),
            rule("tech", "parts", "plates", "Plates & Sheets", c -> c.pathToken("plate", "sheet", "pressing")),
            rule("tech", "parts", "pipes", "Pipes & Tubes", c -> c.pathToken("pipe", "tube", "conduit", "duct")),
            rule("tech", "parts", "belts", "Belts", c -> c.pathToken("belt")),
            rule("tech", "parts", "wires", "Wires & Connectors", c -> c.pathToken("wire", "connector", "coil", "electrode")),
            rule("tech", "parts", "casings", "Casings & Frames", c -> c.pathToken("casing", "frame", "chassis", "wrapped", "locometal", "smokebox", "buffer", "cowcatcher", "boiler")),
            rule("tech", "parts", "tiles", "Tiles", c -> c.pathToken("tile", "tiles")),
            rule("tech", "parts", "resource_blocks", "Resource Blocks", c -> c.pathEndsWith("_block")),
            rule("tech", "parts", "bars_ladders", "Bars, Ladders & Scaffolding", c -> c.pathToken("bars", "ladder", "scaffolding", "cardboard")),

            rule("tech", "redstone", "buttons", "Buttons", c -> c.pathToken("button")),
            rule("tech", "redstone", "pressure_plates", "Pressure Plates", c -> c.pathToken("pressure_plate")),
            rule("tech", "redstone", "levers", "Levers", c -> c.pathToken("lever")),
            rule("tech", "redstone", "rails", "Rails", c -> c.pathToken("rail")),
            rule("tech", "redstone", "depositors", "Depositors", c -> c.pathToken("depositor")),
            rule("tech", "redstone", "components", "Components", c -> c.pathToken("redstone", "repeater", "comparator", "observer", "piston", "torch")),
            rule("tech", "redstone", "sensors", "Sensors", c -> c.pathToken("sensor", "detector")),
            rule("tech", "redstone", "storage_redstone", "Storage & Drawers", c -> c.pathToken("drawer", "trapped_chest", "chest")),
            rule("tech", "redstone", "screens", "Screens & Displays", c -> c.pathToken("tv", "screen", "display")),
            rule("tech", "transport", "rails", "Rails & Tracks", c -> c.hasFacet(ItemFacet.RAIL) || c.pathToken("rail", "track")),
            rule("tech", "transport", "carts", "Carts & Vehicles", c -> c.pathToken("minecart", "cart", "handcar")),
            rule("tech", "transport", "boats", "Boats & Ships", c -> c.pathToken("boat", "ship", "brigg", "cog", "drakkar", "galley")),

            rule("nature", "seeds", "saplings", "Saplings", c -> c.pathToken("sapling")),
            rule("nature", "seeds", "seeds", "Seeds", c -> c.hasFacet(ItemFacet.SEED) || c.pathToken("seed", "seeds")),
            rule("nature", "crops", "crop_seeds", "Crop Seeds", c -> c.hasFacet(ItemFacet.SEED) || c.pathToken("seed", "seeds")),
            rule("nature", "crops", "fruits", "Fruits", c -> c.pathToken("apple", "berry", "berries", "melon", "grape", "tomato", "ancient_fruit", "fruit")),
            rule("nature", "crops", "vegetables", "Vegetables", c -> c.pathToken("carrot", "potato", "beetroot", "onion", "cabbage", "lettuce", "pepper", "broccoli", "cauliflower", "corn", "cucumber", "barley")),
            rule("nature", "flora", "flowers", "Flowers", c -> c.hasFacet(ItemFacet.FLOWER) || c.pathToken("flower", "rose", "tulip", "orchid")),
            rule("nature", "flora", "leaves", "Leaves", c -> c.hasFacet(ItemFacet.LEAVES) || c.pathToken("leaves", "leaf")),
            rule("nature", "flora", "succulents", "Succulents", c -> c.pathToken("agave", "aloe", "cactus")),
            rule("nature", "flora", "coral", "Coral", c -> c.pathToken("coral")),
            rule("nature", "flora", "berries", "Berries", c -> c.pathToken("berry", "berries")),
            rule("nature", "flora", "grass", "Grass & Groundcover", c -> c.pathToken("grass", "fern", "moss", "ivy", "vine", "sprout", "sprouts", "kernels")),
            rule("nature", "wood", "logs", "Logs", c -> c.hasFacet(ItemFacet.LOG) || c.pathToken("log", "stem", "hyphae")),
            rule("nature", "wood", "wood_blocks", "Wood Blocks", c -> c.hasFacet(ItemFacet.WOOD_BLOCK) || c.pathToken("wood", "bamboo")),

            rule("nature", "meals", "prepared_meals", "Prepared Meals", c -> c.pathToken("meal", "stew", "soup", "sandwich", "burger", "roll", "salad", "plate", "bowl", "pasta", "pizza", "kebab", "cooked")),
            rule("nature", "meals", "cakes", "Cakes & Pies", c -> c.pathToken("cake", "pie")),
            rule("nature", "snacks", "baked_goods", "Baked Goods", c -> c.pathToken("bread", "cake", "cookie", "muffin", "pie", "pastry")),
            rule("nature", "snacks", "nuts", "Nuts", c -> c.pathToken("nut", "acorn")),
            rule("nature", "snacks", "roe", "Roe", c -> c.pathToken("roe")),
            rule("nature", "snacks", "preserves", "Preserves & Mash", c -> c.pathToken("preserves", "mash")),
            rule("nature", "snacks", "fruit", "Fruit", c -> c.pathToken("apple", "berry", "berries", "fruit", "algae")),
            rule("nature", "snacks", "dairy", "Dairy", c -> c.pathToken("cheese", "milk")),
            rule("nature", "snacks", "sweets", "Sweets", c -> c.pathToken("candy", "chocolate", "sweet", "honey")),
            rule("nature", "drinks", "beverages", "Beverages", c -> c.pathToken("juice", "tea", "coffee", "wine", "beer", "bottle", "milk", "cider", "cocktail", "whiskey", "eggnog", "chai", "espresso", "drink", "cocoa", "shake", "flask", "liquid")),
            rule("nature", "proteins", "meat", "Meat", c -> c.pathToken("beef", "pork", "chicken", "mutton", "rabbit", "meat", "bacon")),
            rule("nature", "proteins", "fish", "Fish", c -> c.pathToken("fish", "cod", "salmon", "halibut", "herring")),

            rule("ingredients", "organic", "wallpaper", "Wallpaper", c -> c.pathToken("wallpaper")),
            rule("ingredients", "organic", "eggs", "Eggs", c -> c.pathToken("egg", "eggs")),
            rule("ingredients", "organic", "paper", "Paper & Lantern Materials", c -> c.pathToken("paper", "lantern")),
            rule("ingredients", "organic", "mob_drops", "Mob Drops", c -> c.pathToken("bone", "feather", "leather", "hide", "shell", "string", "wool", "scute")),
            rule("ingredients", "organic", "plant_parts", "Plant Parts", c -> c.pathToken("stick", "fiber", "straw", "root", "petal")),
            rule("ingredients", "mineral", "shards", "Shards & Crystals", c -> c.pathToken("shard", "crystal", "powder")),
            rule("ingredients", "mineral", "gems", "Gems", c -> c.hasFacet(ItemFacet.GEM) || c.pathToken("gem")),
            rule("ingredients", "dyes", "dyes", "Dyes", c -> c.pathToken("dye")),

            rule("armor", "curios", "backpacks", "Backpacks & Bags", c -> c.pathToken("backpack", "satchel", "pouch", "bag")),
            rule("armor", "curios", "rings", "Rings", c -> c.pathToken("ring")),
            rule("armor", "curios", "necklaces", "Necklaces & Amulets", c -> c.pathToken("necklace", "amulet", "charm", "pendant")),

            rule("magic", "potions", "potions", "Potions", c -> c.pathToken("potion")),
            rule("magic", "books", "enchanted_books", "Enchanted Books", c -> c.hasFacet(ItemFacet.ENCHANTED_BOOK) || c.pathToken("book")),
            rule("magic", "reagents", "essences", "Essences", c -> c.pathToken("essence", "rune", "crystal", "shard", "powder", "dragon_breath", "experience_bottle")),
            rule("magic", "artifacts", "wands", "Wands", c -> c.pathToken("wand")),
            rule("magic", "artifacts", "scrolls", "Scrolls", c -> c.pathToken("scroll")),
            rule("magic", "artifacts", "trinkets", "Trinkets", c -> c.pathToken("totem", "artifact", "relic", "crystal", "hook")),

            rule("tools", "harvest", "pickaxes", "Pickaxes", c -> c.pathToken("pickaxe")),
            rule("tools", "harvest", "axes", "Axes", c -> c.pathToken("axe") && !c.pathToken("pickaxe")),
            rule("tools", "harvest", "shovels", "Shovels", c -> c.pathToken("shovel")),
            rule("tools", "harvest", "hoes", "Hoes", c -> c.pathToken("hoe")),
            rule("tools", "harvest", "scythes", "Scythes & Sickles", c -> c.pathToken("scythe", "sickle")),
            rule("tools", "harvest", "hammers", "Hammers", c -> c.pathToken("hammer")),
            rule("tools", "harvest", "knives", "Knives", c -> c.pathToken("knife", "needle")),
            rule("tools", "melee", "swords", "Swords", c -> c.pathToken("sword")),
            rule("tools", "melee", "axes", "Axes", c -> c.pathToken("axe")),
            rule("tools", "melee", "knives", "Knives & Scythes", c -> c.pathToken("knife", "scythe")),
            rule("tools", "ranged", "bows", "Bows", c -> c.pathToken("bow")),
            rule("tools", "ammo", "arrows", "Arrows", c -> c.pathToken("arrow")),
            rule("tools", "ammo", "rounds", "Rounds & Shells", c -> c.pathToken("round", "shell", "bolt"))
    );
    private static final Map<ScopeKey, List<Rule>> RULES_BY_SCOPE = buildRulesByScope();
    private static final Map<ScopeKey, List<Kind>> KINDS_BY_SCOPE = buildKindsByScope();
    private static final ConcurrentMap<ClassifyKey, Optional<Kind>> CLASSIFICATION_CACHE = new ConcurrentHashMap<>();

    private AmiOntologyKinds() {
    }

    public static Optional<Kind> classify(SearchNode node, String categoryId, String subcategoryId) {
        List<Rule> rules = RULES_BY_SCOPE.get(new ScopeKey(categoryId, subcategoryId));
        if (rules == null || rules.isEmpty()) {
            return Optional.empty();
        }
        return CLASSIFICATION_CACHE.computeIfAbsent(ClassifyKey.of(node, categoryId, subcategoryId),
                ignored -> classifyUncached(node, rules));
    }

    private static Optional<Kind> classifyUncached(SearchNode node, List<Rule> rules) {
        Context context = new Context(node);
        for (Rule rule : rules) {
            if (rule.predicate.test(context)) {
                return Optional.of(rule.kind);
            }
        }
        return Optional.empty();
    }

    public static List<Kind> kindsFor(String categoryId, String subcategoryId) {
        return KINDS_BY_SCOPE.getOrDefault(new ScopeKey(categoryId, subcategoryId), List.of());
    }

    private static Map<ScopeKey, List<Rule>> buildRulesByScope() {
        Map<ScopeKey, List<Rule>> result = new LinkedHashMap<>();
        for (Rule rule : RULES) {
            result.computeIfAbsent(new ScopeKey(rule.category, rule.subcategory), ignored -> new ArrayList<>()).add(rule);
        }
        result.replaceAll((ignored, rules) -> List.copyOf(rules));
        return Map.copyOf(result);
    }

    private static Map<ScopeKey, List<Kind>> buildKindsByScope() {
        Map<ScopeKey, List<Kind>> result = new LinkedHashMap<>();
        Map<ScopeKey, Set<String>> seenByScope = new LinkedHashMap<>();
        for (Rule rule : RULES) {
            ScopeKey scope = new ScopeKey(rule.category, rule.subcategory);
            Set<String> seen = seenByScope.computeIfAbsent(scope, ignored -> new LinkedHashSet<>());
            if (seen.add(rule.kind.id())) {
                result.computeIfAbsent(scope, ignored -> new ArrayList<>()).add(rule.kind);
            }
        }
        result.replaceAll((ignored, kinds) -> List.copyOf(kinds));
        return Map.copyOf(result);
    }

    private static int relevantMetadataHash(SearchNode node) {
        int hash = 1;
        hash = 31 * hash + node.meta(SearchNodeKeys.FACETS, "").hashCode();
        hash = 31 * hash + node.meta(SearchNodeKeys.TAGS, "").hashCode();
        hash = 31 * hash + node.meta(SearchNodeKeys.BLOCK_TAGS, "").hashCode();
        hash = 31 * hash + node.meta(SearchNodeKeys.VARIANT_GROUP, "").hashCode();
        hash = 31 * hash + node.meta("blockShape", "").hashCode();
        return hash;
    }

    private record ScopeKey(String category, String subcategory) {
    }

    private record ClassifyKey(NodeType type,
                               net.minecraft.resources.Identifier id,
                               String category,
                               String subcategory,
                               int metadataHash) {
        static ClassifyKey of(SearchNode node, String category, String subcategory) {
            return new ClassifyKey(node.type(), node.id(), category, subcategory, relevantMetadataHash(node));
        }
    }

    static int cachedClassificationCountForTests() {
        return CLASSIFICATION_CACHE.size();
    }

    static void clearClassificationCacheForTests() {
        CLASSIFICATION_CACHE.clear();
    }

    private static Rule rule(String category, String subcategory, String kindId, String label, Predicate<Context> predicate) {
        return new Rule(category, subcategory, new Kind(kindId, label), predicate);
    }

    public record Kind(String id, String label) {
    }

    private record Rule(String category, String subcategory, Kind kind, Predicate<Context> predicate) {
    }

    private static final class Context {
        private final SearchNode node;
        private final EnumSet<ItemFacet> facets;
        private final String path;
        private final PathTokens pathTokens;
        private final PathTokens tagTokens;
        private final PathTokens blockTagTokens;

        Context(SearchNode node) {
            this.node = node;
            this.facets = FacetCodec.decode(node.meta(SearchNodeKeys.FACETS, ""));
            this.path = node.id().getPath().toLowerCase(Locale.ROOT);
            this.pathTokens = PathTokens.of(path);
            this.tagTokens = PathTokens.of(node.meta(SearchNodeKeys.TAGS, ""));
            this.blockTagTokens = PathTokens.of(node.meta(SearchNodeKeys.BLOCK_TAGS, ""));
        }

        boolean hasFacet(ItemFacet facet) {
            return facets.contains(facet);
        }

        boolean shape(String value) {
            return node.meta("blockShape", "").equals(value);
        }

        boolean variant(String value) {
            return node.meta(SearchNodeKeys.VARIANT_GROUP, "").equals(value);
        }

        boolean pathEndsWith(String suffix) {
            return path.endsWith(suffix);
        }

        boolean pathToken(String... tokens) {
            for (String token : tokens) {
                if (pathTokens.contains(token)) {
                    return true;
                }
            }
            return false;
        }
    }
}
