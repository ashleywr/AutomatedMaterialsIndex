package com.sanhiruzu.ami.index;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

/**
 * Classifies BlockItem instances into AmiOntology categories at index time.
 *
 * Returns a two-element array {categoryId, subcategoryId}, or null when the item
 * should be left for the runtime heuristics in AmiOntology.classifyItem().
 *
 * Decision order (first match wins):
 *   1. Redstone  – checked before nature so sculk sensors win
 *   2. Nature    – logs, leaves, saplings, flowers, crops, fungi, etc. → null (runtime → "nature")
 *   3. Ores      – all ore block types → null (runtime → "tech")
 *   4. Tech storage – compressed-material blocks → null (runtime → "tech")
 *   5. Functional – beds, doors, block entities, crafting stations, etc.
 *   6. Decorative – carpets, candles, banners, signs, torches, etc.
 *   7. Building   – everything else that is a BlockItem
 */
public final class OntologyClassifier {

    /**
     * @return {categoryId, subcategoryId} or null to use runtime classification.
     */
    @Nullable
    public static String[] classifyItem(Item item, ResourceLocation id) {
        if (!(item instanceof BlockItem bi)) return null;

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

        // ── 7. Building — default for all remaining BlockItems ────────────────
        return blocks("building");
    }

    // ── Helper predicates ─────────────────────────────────────────────────────

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

    private static String[] blocks(String subcategory) {
        return new String[]{"blocks", subcategory};
    }

    private OntologyClassifier() {}
}
