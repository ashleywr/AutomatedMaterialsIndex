package com.sanhiruzu.ami.index;

import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.ComposterBlock;
import org.jetbrains.annotations.Nullable;

/**
 * Classifies Item instances into AmiOntology categories at index time.
 *
 * Returns a two-element array {categoryId, subcategoryId}, or null when the item
 * should be left for the runtime heuristics in AmiOntology.classifyNode().
 *
 * Non-block items are classified by Java type, DataComponents, and c: tags first.
 * Path string matching is used only where it genuinely describes a name-based family
 * (pottery sherds, copper bulb oxidation variants, coral fan/plant variants).
 *
 * BlockItem decision order (waterfall: first match wins):
 *   1. Redstone  – checked before nature so sculk sensors win
 *   2. Nature    – logs, leaves, saplings, flowers, crops, fungi, etc. → null (runtime → "nature")
 *   3. Furniture – beds, skulls, heads (decoration)
 *   4. Tech machines – block entities, crafting stations, cauldrons, anvils
 *   5. Decorative – carpets, candles, banners, signs, torches, etc.
 *   6. Functional – doors, trapdoors, campfires, beehives, etc.
 *   7. Building   – everything else that is a BlockItem
 */
public final class OntologyClassifier {

    // ── Common (c:) item tag keys ─────────────────────────────────────────────

    private static final TagKey<Item> TAG_INGOTS             = cTag("ingots");
    private static final TagKey<Item> TAG_GEMS               = cTag("gems");
    private static final TagKey<Item> TAG_NUGGETS            = cTag("nuggets");
    private static final TagKey<Item> TAG_RAW_MATERIALS      = cTag("raw_materials");
    private static final TagKey<Item> TAG_DUSTS              = cTag("dusts");
    private static final TagKey<Item> TAG_SEEDS              = cTag("seeds");
    private static final TagKey<Item> TAG_CROPS              = cTag("crops");
    private static final TagKey<Item> TAG_EGGS               = cTag("eggs");
    private static final TagKey<Item> TAG_FEATHERS           = cTag("feathers");
    private static final TagKey<Item> TAG_STRING             = cTag("string");
    private static final TagKey<Item> TAG_LEATHERS           = cTag("leathers");
    private static final TagKey<Item> TAG_BONES              = cTag("bones");
    private static final TagKey<Item> TAG_SPAWN_CREATURES    = cTag("spawn_creatures");
    private static final TagKey<Item> TAG_DUSTS_REDSTONE     = cTag("dusts/redstone");
    private static final TagKey<Item> TAG_FOODS_COOKED_MEAT  = cTag("foods/cooked_meat");
    private static final TagKey<Item> TAG_FOODS_COOKED_FISH  = cTag("foods/cooked_fish");
    private static final TagKey<Item> TAG_FOODS_MEAT         = cTag("foods/meat");
    private static final TagKey<Item> TAG_FOODS_FISH         = cTag("foods/fish");
    private static final TagKey<Item> TAG_FOODS_VEGETABLE    = cTag("foods/vegetable");
    private static final TagKey<Item> TAG_FOODS_FRUIT        = cTag("foods/fruit");
    private static final TagKey<Item> TAG_FOODS_DRINK        = cTag("foods/drink");
    private static final TagKey<Item> TAG_FOODS_PLACED       = cTag("foods/edible_when_placed");
    private static final TagKey<Item> TAG_DRINKS_MAGIC       = cTag("drinks/magic");

    private static TagKey<Item> cTag(String path) {
        return TagKey.create(Registries.ITEM, ResourceLocation.fromNamespaceAndPath("c", path));
    }

    private static boolean is(Item item, TagKey<Item> tag) {
        return item.builtInRegistryHolder().is(tag);
    }

    // Check if a block state has a specific property (semantic, mod-agnostic)
    private static boolean hasProperty(BlockState state, String propertyName) {
        return state.getProperties().stream()
            .anyMatch(prop -> prop.getName().equals(propertyName));
    }

    // ─────────────────────────────────────────────────────────────────────────

    /**
     * @return {categoryId, subcategoryId} or null to use runtime classification.
     */
    @Nullable
    public static String[] classifyItem(Item item, ResourceLocation id) {
        String path = id.getPath();

        // ── Pass 1: Semantic identity — wins regardless of game object type ───
        // Magical drinks before food classification
        if (is(item, TAG_DRINKS_MAGIC)) return magic("potions");
        // Catches food blocks (e.g. cake, modded block foods) before the BlockItem gate.
        if (item.components().has(DataComponents.FOOD)) return classifyFood(item);
        if (is(item, TAG_FOODS_PLACED)) return nature("snacks");  // placed/block foods (cake, etc.)
        if (is(item, TAG_SEEDS)) return nature("seeds");
        if (is(item, TAG_CROPS)) return nature("crops");
        if (isCompostable(item)) return nature("flora");

        // ── Pass 2: Non-block items by Java type and tags ─────────────────────
        if (!(item instanceof BlockItem bi)) {
            return classifyNonBlockItem(item, id);
        }

        // ── Pass 3: Block classification ──────────────────────────────────────
        Block block = bi.getBlock();
        BlockState state = block.defaultBlockState();

        // ── 1. Redstone ───────────────────────────────────────────────────────
        // Catch all redstone-logic blocks via "powered" property (semantic, mod-agnostic)
        if (hasProperty(state, "powered")) {
            return masonry("redstone");
        }
        if (state.is(BlockTags.BUTTONS)
                || state.is(BlockTags.PRESSURE_PLATES)
                || state.is(BlockTags.RAILS)) {
            return masonry("redstone");
        }
        if (isRedstoneBlock(item, path)) return masonry("redstone");

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

        // ── 3. Furniture (most specific) ──────────────────────────────────────
        // Beds: any block that implements bed behavior
        if (block instanceof BedBlock) {
            return decoration("furniture");
        }
        // Skulls/heads: decorative furniture
        if (block instanceof SkullBlock) {
            if (item == Items.PLAYER_HEAD) return social("players");
            return decoration("furniture");
        }

        // ── 4. Tech Machines & Crafting ───────────────────────────────────────
        if (state.is(BlockTags.CAULDRONS) || state.is(BlockTags.ANVIL)) {
            return tech("machines");
        }
        if (block instanceof BaseEntityBlock) {
            // Other block entities (chests, furnaces, brewing stands, etc.)
            if (block instanceof SignBlock || block instanceof BannerBlock || block instanceof ShulkerBoxBlock) {
                return masonry("functional");
            }
            return tech("machines");
        }
        if (isMachineBlock(item)) return tech("machines");

        // ── 5. Decorative ─────────────────────────────────────────────────────
        // Carpets: tagged or have "height" property < 2 (very thin, 1/16 block)
        if (state.is(BlockTags.WOOL_CARPETS) || hasProperty(state, "height")) {
            return decoration("decorative");
        }
        // Candles, banners, signs, flower pots — use tags
        if (state.is(BlockTags.CANDLES)) {
            return decoration("decorative");
        }
        if (state.is(BlockTags.BANNERS)) {
            return decoration("decorative");
        }
        if (state.is(BlockTags.ALL_SIGNS)) {
            return decoration("decorative");
        }
        if (state.is(BlockTags.FLOWER_POTS)) {
            return decoration("decorative");
        }
        // Light-emitting decorative blocks: torches, lanterns, etc.
        // Catches any block with light emission > 6 that isn't redstone (checked earlier)
        if (block instanceof TorchBlock || block instanceof WallTorchBlock
                || block instanceof LanternBlock || block instanceof CandleBlock
                || block instanceof AmethystClusterBlock
                || state.getLightEmission() > 6) {
            return decoration("decorative");
        }
        // Coral (decorative forms: corals and coral_plants, not coral_blocks which go to masonry)
        if (state.is(BlockTags.CORALS) || state.is(BlockTags.CORAL_PLANTS)) {
            return decoration("decorative");
        }

        // ── 6. Functional Blocks ──────────────────────────────────────────────
        // Doors, trapdoors, fence gates — all have "open" property (semantic)
        if (state.is(BlockTags.DOORS) || state.is(BlockTags.TRAPDOORS) || state.is(BlockTags.FENCE_GATES)
                || hasProperty(state, "open")) {
            return masonry("functional");
        }
        // Campfires — have "lit" property (can be lit/unlit)
        if (state.is(BlockTags.CAMPFIRES) || hasProperty(state, "lit")) {
            return masonry("functional");
        }
        // Beehives — specific block entity type or tag
        if (state.is(BlockTags.BEEHIVES)) {
            return masonry("functional");
        }
        if (isFunctionalBlock(item)) return masonry("functional");

        // ── 6b. Terrain & Soil Blocks ────────────────────────────────────────
        // Dirt blocks (grass, dirt, mud, etc.) — semantic tag-based
        if (state.is(BlockTags.DIRT)) {
            return geology("terrain");
        }

        // ── 7. Terrain & Geology vs Architecture & Masonry ───────────────────
        String shape    = classifyBlockShape(path);
        String material = classifyBlockMaterial(state, path);
        if (shape.equals("full_block") && material.equals("other_building")) return misc("unknown");
        if (shape.equals("full_block") && isNaturalMaterial(material, path)) {
            return geology(material.equals("soil") ? "terrain" : "stone");
        }
        return masonry(shape, material);
    }

    // ── Non-BlockItem classification ──────────────────────────────────────────

    @Nullable
    private static String[] classifyNonBlockItem(Item item, ResourceLocation id) {
        String path = id.getPath();

        // ── Fluid Buckets ─────────────────────────────────────────────────────
        // Buckets containing mobs → Bestiary (same as the mob)
        if (item instanceof MobBucketItem) {
            return bestiary("creatures");
        }
        // Consumable fluids (have FOOD component like honey) → Nature
        if (item instanceof BucketItem && item.components().has(DataComponents.FOOD)) {
            return nature("drinks");
        }
        // Utility buckets (water, lava, powder snow, milk) → Utility
        if (item instanceof BucketItem) {
            return utility("fluids");
        }

        // ── Armor ─────────────────────────────────────────────────────────────
        if (item instanceof ArmorItem ai) {
            return switch (ai.getEquipmentSlot()) {
                case HEAD  -> armor("head");
                case CHEST -> armor("chest");
                case LEGS  -> armor("legs");
                case FEET  -> armor("feet");
                default    -> armor("chest");
            };
        }
        if (item instanceof AnimalArmorItem) return null; // goes on animals, not Curios slots — defer to runtime
        if (item == Items.ELYTRA)           return armor("curios");

        // ── Weapons & Tools ───────────────────────────────────────────────────
        // Sword-like weapons (melee)
        if (item instanceof SwordItem || item instanceof TridentItem || item instanceof MaceItem) {
            return tools("melee");
        }
        // Ranged weapons & projectiles
        if (item instanceof BowItem || item instanceof CrossbowItem || item instanceof ArrowItem) {
            return tools("ranged");
        }
        // Harvest tools (pickaxe, shovel, axe, hoe)
        if (item instanceof DiggerItem) {
            return tools("harvest");
        }
        // Utility tools (fishing rod, shears, flint & steel, brush)
        if (item instanceof FishingRodItem || item instanceof ShearsItem
                || item instanceof FlintAndSteelItem || item instanceof BrushItem) {
            return tools("utility");
        }

        // ── Magic ─────────────────────────────────────────────────────────────
        // Potions & potion-like items (have potion content component)
        if (item.components().has(DataComponents.POTION_CONTENTS)) {
            return magic("potions");
        }
        // Enchanted books store enchantments on the stack rather than the item type.
        if (item == Items.ENCHANTED_BOOK || item.components().has(DataComponents.STORED_ENCHANTMENTS)) {
            return magic("books");
        }
        // Specific magic artifacts & reagents by semantic identity
        if (isMagicArtifact(item))             return magic("artifacts");
        if (isMagicReagent(item))              return magic("reagents");

        // ── Tech materials (c: tags cover modded ingots/gems/dusts automatically) ──
        if (is(item, TAG_INGOTS) || is(item, TAG_GEMS) || is(item, TAG_NUGGETS)) return tech("ingots");
        if (is(item, TAG_RAW_MATERIALS))                                          return tech("ingots");
        // Redstone dust specifically (logic material, not a generic dust)
        if (is(item, TAG_DUSTS_REDSTONE)) return tech("redstone");
        if (is(item, TAG_DUSTS))                                                  return tech("dusts");
        // Vanilla fuel/material items that typically lack c: dust tags
        if (item == Items.COAL || item == Items.CHARCOAL
                || item == Items.BLAZE_ROD || item == Items.NETHER_BRICK) {
            return tech("dusts");
        }

        // ── Nature (non-block) ────────────────────────────────────────────────
        if (is(item, TAG_SEEDS))                                        return nature("seeds");
        if (is(item, TAG_CROPS))                                        return nature("crops");
        if (item == Items.BAMBOO || item == Items.STICK)                return nature("wood");
        if (item == Items.BONE_MEAL)                                    return nature("flora");
        if (item == Items.RED_MUSHROOM || item == Items.BROWN_MUSHROOM) return nature("fungi");

        // ── Crafting ingredients ───────────────────────────────────────────────
        if (isIngredient(item, path)) return ingredients("organic");

        // ── Utility ───────────────────────────────────────────────────────────
        if (item == Items.COMPASS || item == Items.RECOVERY_COMPASS
                || item == Items.FILLED_MAP || item == Items.CLOCK
                || item == Items.SPYGLASS) {
            return utility("navigation");
        }
        if (item == Items.GOAT_HORN) {
            return utility("misc");
        }

        // ── Social ────────────────────────────────────────────────────────────
        if (item == Items.LODESTONE) return social("claims");

        // ── Entities (spawn eggs & creatures) ─────────────────────────────────
        if (item instanceof SpawnEggItem) return bestiary(classifySpawnEgg(path));
        if (is(item, TAG_SPAWN_CREATURES)) return bestiary("creatures");

        // ── Transport ─────────────────────────────────────────────────────────
        if (item instanceof BoatItem || item instanceof MinecartItem) return tech("transport");

        return null;
    }

    // ── Food sub-classification ───────────────────────────────────────────────

    private static String[] classifyFood(Item item) {
        // Stews/soups first — they use a bowl-return consume pattern, not typical snacks
        if (item == Items.MUSHROOM_STEW || item == Items.BEETROOT_SOUP
                || item == Items.RABBIT_STEW || item == Items.SUSPICIOUS_STEW
                || item == Items.HONEY_BOTTLE || is(item, TAG_FOODS_DRINK)) {
            return nature("drinks");
        }
        if (is(item, TAG_FOODS_COOKED_MEAT) || is(item, TAG_FOODS_COOKED_FISH)) return nature("meals");
        if (is(item, TAG_FOODS_MEAT)         || is(item, TAG_FOODS_FISH))        return nature("proteins");
        if (is(item, TAG_FOODS_VEGETABLE)    || is(item, TAG_FOODS_FRUIT))       return nature("crops");
        return nature("snacks");
    }

    // ── Magic helpers ─────────────────────────────────────────────────────────

    private static boolean isMagicArtifact(Item item) {
        return item == Items.TOTEM_OF_UNDYING || item == Items.NETHER_STAR;
    }

    private static boolean isMagicReagent(Item item) {
        return item == Items.BLAZE_POWDER           || item == Items.ENDER_PEARL
            || item == Items.ENDER_EYE              || item == Items.FERMENTED_SPIDER_EYE
            || item == Items.GHAST_TEAR             || item == Items.PHANTOM_MEMBRANE
            || item == Items.RABBIT_FOOT            || item == Items.SPIDER_EYE
            || item == Items.GLISTERING_MELON_SLICE || item == Items.DRAGON_BREATH
            || item == Items.EXPERIENCE_BOTTLE      || item == Items.MAGMA_CREAM
            || item == Items.NETHER_WART            || item == Items.SLIME_BALL;
    }

    // ── Ingredient helper ─────────────────────────────────────────────────────

    private static boolean isIngredient(Item item, String path) {
        // Common c: tags cover vanilla + modded equivalents automatically
        if (is(item, TAG_EGGS) || is(item, TAG_FEATHERS) || is(item, TAG_STRING)
                || is(item, TAG_LEATHERS) || is(item, TAG_BONES)) {
            return true;
        }
        // Vanilla items that lack dedicated c: ingredient tags
        return item == Items.EGG            // throwable egg; c:eggs may not cover it in all envs
            || item == Items.FLINT          || item == Items.CLAY_BALL
            || item == Items.TURTLE_SCUTE   || item == Items.HONEYCOMB
            || item == Items.PRISMARINE_SHARD
            || item == Items.INK_SAC        || item == Items.GLOW_INK_SAC
            || item == Items.RABBIT_HIDE    || item == Items.SHULKER_SHELL
            || item == Items.NAUTILUS_SHELL || item == Items.HEART_OF_THE_SEA
            || item == Items.BREEZE_ROD
            || path.contains("pottery_sherd"); // 20 sherd variants share no single Items constant
    }

    // ── Spawn egg classifier ──────────────────────────────────────────────────

    private static String classifySpawnEgg(String path) {
        String mob = path.substring(0, path.length() - "_spawn_egg".length());
        return switch (mob) {
            case "pig", "chicken", "cow", "sheep", "horse", "donkey", "mule",
                 "rabbit", "squid", "glow_squid", "bat", "ocelot", "cat",
                 "axolotl", "frog", "tadpole", "parrot", "mooshroom",
                 "strider", "cod", "salmon", "tropical_fish", "pufferfish",
                 "turtle", "sniffer", "allay" -> "passive";
            case "wolf", "bee", "polar_bear", "dolphin", "panda",
                 "llama", "trader_llama", "goat", "iron_golem",
                 "piglin", "zombified_piglin", "enderman",
                 "spider", "cave_spider" -> "neutral";
            default -> "hostile";
        };
    }

    // ── Category return helpers ───────────────────────────────────────────────

    private static String[] armor(String sub)       { return new String[]{"armor",       sub}; }
    private static String[] tools(String sub)       { return new String[]{"tools",       sub}; }
    private static String[] magic(String sub)       { return new String[]{"magic",       sub}; }
    private static String[] tech(String sub)        { return new String[]{"tech",        sub}; }
    private static String[] nature(String sub)      { return new String[]{"nature",      sub}; }
    private static String[] social(String sub)      { return new String[]{"social",      sub}; }
    private static String[] utility(String sub)     { return new String[]{"utility",     sub}; }
    private static String[] bestiary(String sub)    { return new String[]{"bestiary",    sub}; }
    private static String[] decoration(String sub)  { return new String[]{"decoration",  sub}; }
    private static String[] ingredients(String sub) { return new String[]{"ingredients", sub}; }
    private static String[] misc(String sub)        { return new String[]{"misc",        sub}; }
    private static String[] geology(String sub)     { return new String[]{"geology",     sub}; }
    private static String[] masonry(String sub)     { return new String[]{"masonry",     sub}; }
    private static String[] masonry(String shape, String material) { return new String[]{"masonry", shape, material}; }

    // ── Building block shape & material classifiers ───────────────────────────

    // Block shape is inherent to the item name/identity, not a semantic classification,
    // so path matching here is acceptable (stairs, slabs, walls, fences are structural variants).
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
        if (path.contains("glass")) return "glass";

        if (state.is(BlockTags.PLANKS)
                || state.is(BlockTags.WOODEN_SLABS)
                || state.is(BlockTags.WOODEN_STAIRS)
                || state.is(BlockTags.WOODEN_FENCES)
                || path.endsWith("_wood")
                || path.equals("bamboo_block")
                || path.equals("bamboo_mosaic")) {
            return "wood";
        }

        if (path.contains("concrete_powder")
                || containsAny(path, "dirt", "gravel", "clay", "mud", "podzol", "mycelium",
                               "snow", "ice", "terracotta", "soul_sand", "soul_soil")
                || path.equals("sand") || path.endsWith("_sand")) {
            return "soil";
        }

        if (state.is(BlockTags.STONE_BRICKS)
                || containsAny(path, "stone", "cobble", "brick", "sandstone",
                               "andesite", "diorite", "granite", "deepslate", "tuff",
                               "basalt", "calcite", "dripstone", "blackstone",
                               "netherrack", "end_stone", "obsidian",
                               "prismarine", "purpur", "magma", "concrete", "ore", "debris")) {
            return "stone";
        }

        return "other_building";
    }

    private static boolean isNaturalMaterial(String material, String path) {
        if (material.equals("glass") || material.equals("wood")) return false;
        if (material.equals("soil")) {
            return !path.contains("concrete") && !path.contains("terracotta");
        }
        if (material.equals("stone")) {
            return !path.contains("brick") && !path.contains("polished")
                && !path.contains("smooth_") && !path.contains("chiseled")
                && !path.contains("cut_") && !path.contains("tiles")
                && !path.contains("purpur") && !path.contains("concrete");
        }
        return false;
    }

    private static boolean containsAny(String path, String... tokens) {
        for (String token : tokens) {
            if (path.contains(token)) return true;
        }
        return false;
    }

    // ── BlockItem helper predicates ───────────────────────────────────────────

    private static boolean isRedstoneBlock(Item item, String path) {
        return item == Items.REDSTONE_TORCH        || item == Items.REDSTONE_LAMP
            || item == Items.REPEATER              || item == Items.COMPARATOR
            || item == Items.OBSERVER              || item == Items.PISTON
            || item == Items.STICKY_PISTON         || item == Items.LEVER
            || item == Items.DAYLIGHT_DETECTOR     || item == Items.TARGET
            || item == Items.TNT                   || item == Items.TRIPWIRE_HOOK
            || item == Items.SLIME_BLOCK           || item == Items.HONEY_BLOCK
            || item == Items.SCULK_SENSOR          || item == Items.CALIBRATED_SCULK_SENSOR;
    }

    private static boolean isNaturePath(String path) {
        return path.contains("mushroom")
               || path.contains("fungus")
               || path.equals("bamboo")
               || path.contains("kelp")
               || path.contains("seagrass")
               || path.contains("sugar_cane")
               || path.equals("cactus")
               || path.contains("vine")
               || path.equals("lily_pad")
               || path.contains("nylium")
               || path.contains("wart_block")
               || path.equals("shroomlight")
               || path.contains("sculk")
               || path.contains("chorus_plant") || path.contains("chorus_flower")
               || path.contains("azalea")
               || path.contains("dripleaf")
               || path.contains("hanging_roots")
               || path.contains("rooted_dirt")
               || path.contains("pink_petals")
               || path.contains("pitcher")
               || path.contains("torchflower")
               || path.equals("spore_blossom")
               || (path.contains("grass") && !path.equals("grass_block") && !path.contains("path"));
    }

    private static boolean isMachineBlock(Item item) {
        return item == Items.CRAFTING_TABLE    || item == Items.STONECUTTER
            || item == Items.GRINDSTONE        || item == Items.LOOM
            || item == Items.CARTOGRAPHY_TABLE || item == Items.FLETCHING_TABLE
            || item == Items.SMITHING_TABLE    || item == Items.COMPOSTER;
    }

    private static boolean isFunctionalBlock(Item item) {
        return item == Items.LADDER    || item == Items.SCAFFOLDING
            || item == Items.BOOKSHELF || item == Items.NOTE_BLOCK
            || item == Items.LIGHTNING_ROD;
    }

    private static boolean isDecorativeBlock(Item item, String path) {
        // Specific items known to be decorative (no generic tags or properties for these)
        return item == Items.CHAIN       || item == Items.GLOW_LICHEN
            || item == Items.MOSS_CARPET || item == Items.AMETHYST_CLUSTER
            || item == Items.SMALL_AMETHYST_BUD  || item == Items.MEDIUM_AMETHYST_BUD
            || item == Items.LARGE_AMETHYST_BUD || item == Items.BUDDING_AMETHYST;
    }

    private static boolean isCompostable(Item item) {
        return ComposterBlock.COMPOSTABLES.containsKey(item);
    }

    private OntologyClassifier() {}
}
