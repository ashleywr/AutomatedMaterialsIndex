package net.minecraft.tags;

import net.minecraft.resources.Identifier;
import net.minecraft.world.level.block.Block;

public final class BlockTags {
    public static final TagKey<Block> RAILS = tag("rails");
    public static final TagKey<Block> STAIRS = tag("stairs");
    public static final TagKey<Block> SLABS = tag("slabs");
    public static final TagKey<Block> WALLS = tag("walls");
    public static final TagKey<Block> FENCES = tag("fences");
    public static final TagKey<Block> FENCE_GATES = tag("fence_gates");
    public static final TagKey<Block> DOORS = tag("doors");
    public static final TagKey<Block> TRAPDOORS = tag("trapdoors");
    public static final TagKey<Block> LOGS = tag("logs");
    public static final TagKey<Block> LEAVES = tag("leaves");
    public static final TagKey<Block> FLOWERS = tag("flowers");
    public static final TagKey<Block> CROPS = tag("crops");
    public static final TagKey<Block> PLANKS = tag("planks");
    public static final TagKey<Block> BASE_STONE_OVERWORLD = tag("base_stone_overworld");
    public static final TagKey<Block> BASE_STONE_NETHER = tag("base_stone_nether");
    public static final TagKey<Block> DIRT = tag("dirt");
    public static final TagKey<Block> BUTTONS = tag("buttons");
    public static final TagKey<Block> PRESSURE_PLATES = tag("pressure_plates");
    public static final TagKey<Block> SIGNS = tag("signs");
    public static final TagKey<Block> ALL_HANGING_SIGNS = tag("all_hanging_signs");
    public static final TagKey<Block> BEDS = tag("beds");
    public static final TagKey<Block> MINEABLE_WITH_PICKAXE = tag("mineable_with_pickaxe");
    public static final TagKey<Block> NEEDS_DIAMOND_TOOL = tag("needs_diamond_tool");
    public static final TagKey<Block> NEEDS_IRON_TOOL = tag("needs_iron_tool");
    public static final TagKey<Block> NEEDS_STONE_TOOL = tag("needs_stone_tool");
    public static final TagKey<Block> MINEABLE_WITH_AXE = tag("mineable_with_axe");
    public static final TagKey<Block> MINEABLE_WITH_SHOVEL = tag("mineable_with_shovel");
    public static final TagKey<Block> MINEABLE_WITH_HOE = tag("mineable_with_hoe");

    private BlockTags() {
    }

    private static TagKey<Block> tag(String path) {
        return TagKey.create(null, Identifier.of("minecraft", path));
    }
}
