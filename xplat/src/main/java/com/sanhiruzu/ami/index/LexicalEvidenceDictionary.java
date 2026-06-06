package com.sanhiruzu.ami.index;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

final class LexicalEvidenceDictionary {
    private static final List<Entry> ENTRIES = List.of(
            entry("decorative_furniture", "decoration", "furniture", 85,
                    tokens("chair", "table", "desk", "couch", "sofa", "bench", "stool", "cabinet", "cabinetry", "shelf", "shelves", "rack", "counter", "nightstand", "wardrobe", "dresser", "bookcase", "bookshelf", "bookshelves", "postbox", "toilet")),
            entry("decorative_lighting", "decoration", "lighting", 85,
                    tokens("lamp", "lantern", "chandelier", "sconce", "brazier", "candelabra", "candle", "torch", "glowstone", "shroomlight", "froglight", "beacon")),
            entry("decorative_textiles", "decoration", "textiles", 65,
                    tokens("curtain", "curtains", "blinds", "shutter", "shutters", "rug", "carpet", "pillow", "cushion", "blanket", "sheet", "banner", "tapestry", "canvas", "streamer", "streamers")),
            entry("rail_transport", "tech", "transport", 90,
                    tokens("rail", "rails", "track", "tracks", "train", "tram", "monorail", "coupler", "conductor", "semaphore", "handcar", "locomotive")),
            entry("tech_machine", "tech", "machines", 65,
                    tokens("machine", "generator", "crusher", "mixer", "press", "millstone", "pump", "engine", "motor", "charger", "accumulator", "crafter", "assembler", "fabricator", "refinery", "compressor", "chamber", "stove")),
            entry("tech_redstone", "tech", "redstone", 85,
                    tokens("redstone", "piston", "observer", "comparator", "repeater", "button", "lever", "detector", "relay", "transmitter", "receiver", "sensor", "target", "gauge", "gauges")),
            entry("tech_cables", "tech", "cables", 85,
                    tokens("cable", "cables", "wire", "wires", "pipe", "pipes", "tube", "tubes", "conduit", "duct"),
                    tokens("coral", "cobweb", "bush", "leaf", "leaves", "roots")),
            entry("tech_circuits", "tech", "circuits", 90,
                    tokens("circuit", "circuits", "processor", "chip", "chipset", "logic", "calculation", "engineering"),
                    tokens("bowl", "plate", "bottle", "milkshake", "chips")),
            entry("mechanical_component", "tech", "parts", 85,
                    tokens("gear", "gears", "gearbox", "cog", "cogs", "cogwheel", "cogwheels", "shaft", "shafts", "belt", "belts", "flywheel")),
            entry("template", "tech", "templates", 95,
                    tokens("blueprint", "schematic", "template", "mold")),
            entry("wood_block", "nature", "wood", 80,
                    tokens("log", "logs", "planks", "stem", "stems", "hyphae")),
            entry("flora", "nature", "flora", 75,
                    tokens("leaves", "flower", "flowers", "bush", "shrub", "sprouts", "roots", "vine", "vines", "moss", "coral", "seagrass", "kelp", "cactus", "fern", "clover")),
            entry("flora_leaf_weak", "nature", "flora", 40,
                    tokens("leaf")),
            entry("seeds", "nature", "seeds", 70,
                    tokens("seed", "seeds", "sapling", "saplings"),
                    tokens("pouch", "bucket", "oil", "crystal", "machine", "maker")),
            entry("crops", "nature", "crops", 65,
                    tokens("crop", "crops", "wheat", "carrot", "potato", "beetroot", "tomato", "cabbage", "onion", "pepper", "cucumber", "grape", "berry", "berries", "rice")),
            entry("fungi", "nature", "fungi", 75,
                    tokens("mushroom", "mushrooms", "fungus", "fungi", "mycelium", "nylium")),
            entry("food_meal", "nature", "meals", 80,
                    tokens("soup", "stew", "sandwich", "burger", "cheeseburger", "pizza", "pasta", "noodle", "noodles", "rice", "roll", "rolls", "kebab", "salad", "dumpling", "dumplings", "casserole", "lasagna", "quiche", "meatball", "meatballs", "taco", "tacos", "wrap", "wraps", "burrito", "burritos", "meal"),
                    tokens("table", "tables", "sauce", "sauces")),
            entry("food_snack", "nature", "snacks", 65,
                    tokens("cake", "pie", "cookie", "bread", "tart", "pudding", "icecream", "preserve", "preserves", "jam", "jelly", "candy", "chocolate", "berry", "berries", "fruit", "apple")),
            entry("food_drink", "nature", "drinks", 70,
                    tokens("juice", "soda", "beer", "wine", "coffee", "tea", "cider", "milk", "drink", "smoothie"),
                    tokens("chair", "table", "desk", "cabinet", "shelf", "rack", "counter", "drawer", "drawers", "wardrobe", "dresser", "storage")),
            entry("food_protein", "nature", "proteins", 75,
                    tokens("beef", "chicken", "pork", "porkchop", "cod", "salmon", "rabbit", "mutton", "fish", "meat", "bacon", "ham", "sausage", "ribs", "venison")),
            entry("organic_ingredient", "ingredients", "organic", 65,
                    tokens("string", "feather", "feathers", "bone", "bones", "leather", "hide", "scute", "honeycomb", "wool", "egg", "eggs", "crumb", "crumbs", "scale", "scales", "sprig", "thread")),
            entry("mineral_ingredient", "ingredients", "mineral", 55,
                    tokens("flint", "clay", "shard", "shards", "prismarine", "pottery", "sherd", "quartz", "crystal", "crystals", "salt", "salts", "rock")),
            entry("dyes", "ingredients", "dyes", 80,
                    tokens("dye", "dyes", "pigment", "pigments", "ink")),
            entry("melee_weapon", "tools", "melee", 75,
                    tokens("sword", "swords", "dagger", "daggers", "spear", "spears", "mace", "club", "katana")),
            entry("ranged_weapon", "tools", "ranged", 75,
                    tokens("bow", "bows", "crossbow", "crossbows", "gun", "rifle", "pistol", "musket", "cannon")),
            entry("ammo_projectile", "tools", "ammo", 85,
                    tokens("arrow", "arrows", "bolt", "bolts", "bullet", "bullets", "round", "rounds", "cartridge", "cartridges", "grenade", "rocket")),
            entry("harvest_tool", "tools", "harvest", 85,
                    tokens("pickaxe", "pickaxes", "shovel", "shovels", "axe", "axes", "hoe", "hoes", "sickle", "scythe", "shears")),
            entry("utility_tool", "tools", "utility", 85,
                    tokens("wrench", "hammer", "mallet", "brush", "toolbox")),
            entry("utility_container", "utility", "misc", 85,
                    tokens("pouch", "satchel", "dish", "bottle", "bottles", "flask")),
            entry("armor", "armor", "", 75,
                    tokens("helmet", "helmets", "chestplate", "chestplates", "leggings", "boots", "armor", "elytra")),
            entry("navigation", "utility", "navigation", 85,
                    tokens("compass", "map", "maps", "clock", "spyglass")),
            entry("medical", "utility", "medical", 80,
                    tokens("bandage", "medkit", "syringe", "morphine", "adrenaline", "splint")),
            entry("currency", "utility", "currency", 70,
                    tokens("coin", "coins", "cash", "money")),
            entry("magic_artifact", "magic", "artifacts", 65,
                    tokens("spell", "wand", "staff", "scroll", "totem", "charm", "relic")),
            entry("magic_reagent", "magic", "reagents", 60,
                    tokens("rune", "runes", "essence", "pearl", "blaze", "ghast", "phantom", "membrane", "wart", "slime", "stardust"))
    );

    private LexicalEvidenceDictionary() {
    }

    static List<ClassificationEvidence> match(Set<String> tokens) {
        List<ClassificationEvidence> evidence = new ArrayList<>();
        for (Entry entry : ENTRIES) {
            if (!entry.blockers.isEmpty() && intersects(tokens, entry.blockers)) {
                continue;
            }
            for (String token : entry.tokens) {
                if (tokens.contains(token)) {
                    evidence.add(new ClassificationEvidence(
                            entry.id,
                            "lexical",
                            entry.categoryId,
                            entry.subcategoryId,
                            entry.weight,
                            "token=" + token
                    ));
                    break;
                }
            }
        }
        if (tokens.contains("shell") || tokens.contains("shells")) {
            boolean ammoContext = intersects(tokens, tokens("ammo", "gun", "shotgun", "cannon", "autocannon", "artillery", "mortar", "munition", "munitions"));
            evidence.add(new ClassificationEvidence(
                    ammoContext ? "ammo_shell" : "natural_shell",
                    "lexical",
                    ammoContext ? "tools" : "ingredients",
                    ammoContext ? "ammo" : "organic",
                    ammoContext ? 85 : 45,
                    ammoContext ? "shell with ammo context" : "shell without ammo context"
            ));
        }
        if (tokens.contains("spawn") && tokens.contains("egg")) {
            evidence.add(new ClassificationEvidence("spawn_egg", "lexical", "bestiary", "", 80, "tokens=spawn+egg"));
        }
        return evidence;
    }

    static Set<String> tokenize(String value) {
        Set<String> tokens = new HashSet<>();
        if (value == null || value.isBlank()) {
            return tokens;
        }
        for (String raw : value.toLowerCase(Locale.ROOT).split("[^a-z0-9]+")) {
            if (raw.length() >= 2 && !raw.chars().allMatch(Character::isDigit)) {
                tokens.add(raw);
            }
        }
        return tokens;
    }

    private static Entry entry(String id, String categoryId, String subcategoryId, int weight, Set<String> tokens) {
        return entry(id, categoryId, subcategoryId, weight, tokens, Set.of());
    }

    private static Entry entry(String id, String categoryId, String subcategoryId, int weight, Set<String> tokens, Set<String> blockers) {
        return new Entry(id, categoryId, subcategoryId, weight, tokens, blockers);
    }

    private static Set<String> tokens(String... tokens) {
        return Set.of(tokens);
    }

    private static boolean intersects(Set<String> a, Set<String> b) {
        for (String token : b) {
            if (a.contains(token)) {
                return true;
            }
        }
        return false;
    }

    private record Entry(
            String id,
            String categoryId,
            String subcategoryId,
            int weight,
            Set<String> tokens,
            Set<String> blockers
    ) {
    }
}
