package net.minecraft.world.item;

import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.ComposterBlock;
import net.minecraft.world.level.block.state.BlockState;

public final class Items {
    public static final Item AIR = register("air", new Item("Air"));
    public static final Item APPLE = register("apple",
            new Item("Apple").withComponent(DataComponents.FOOD));
    public static final Item CAKE = register("cake",
            new BlockItem("Cake", new Block(new BlockState()))
                    .withComponent(DataComponents.FOOD));
    public static final Item CHEST = register("chest", new BlockItem("Chest", new Block(new BlockState())));
    public static final Item TRAPPED_CHEST = register("trapped_chest", new BlockItem("Trapped Chest", new Block(new BlockState())));
    public static final Item BARREL = register("barrel", new BlockItem("Barrel", new Block(new BlockState())));
    public static final Item SHULKER_BOX = register("shulker_box", new BlockItem("Shulker Box", new Block(new BlockState())));
    public static final Item WHITE_SHULKER_BOX = register("white_shulker_box", new BlockItem("White Shulker Box", new Block(new BlockState())));
    public static final Item ORANGE_SHULKER_BOX = register("orange_shulker_box", new BlockItem("Orange Shulker Box", new Block(new BlockState())));
    public static final Item MAGENTA_SHULKER_BOX = register("magenta_shulker_box", new BlockItem("Magenta Shulker Box", new Block(new BlockState())));
    public static final Item LIGHT_BLUE_SHULKER_BOX = register("light_blue_shulker_box", new BlockItem("Light Blue Shulker Box", new Block(new BlockState())));
    public static final Item YELLOW_SHULKER_BOX = register("yellow_shulker_box", new BlockItem("Yellow Shulker Box", new Block(new BlockState())));
    public static final Item LIME_SHULKER_BOX = register("lime_shulker_box", new BlockItem("Lime Shulker Box", new Block(new BlockState())));
    public static final Item PINK_SHULKER_BOX = register("pink_shulker_box", new BlockItem("Pink Shulker Box", new Block(new BlockState())));
    public static final Item GRAY_SHULKER_BOX = register("gray_shulker_box", new BlockItem("Gray Shulker Box", new Block(new BlockState())));
    public static final Item LIGHT_GRAY_SHULKER_BOX = register("light_gray_shulker_box", new BlockItem("Light Gray Shulker Box", new Block(new BlockState())));
    public static final Item CYAN_SHULKER_BOX = register("cyan_shulker_box", new BlockItem("Cyan Shulker Box", new Block(new BlockState())));
    public static final Item PURPLE_SHULKER_BOX = register("purple_shulker_box", new BlockItem("Purple Shulker Box", new Block(new BlockState())));
    public static final Item BLUE_SHULKER_BOX = register("blue_shulker_box", new BlockItem("Blue Shulker Box", new Block(new BlockState())));
    public static final Item BROWN_SHULKER_BOX = register("brown_shulker_box", new BlockItem("Brown Shulker Box", new Block(new BlockState())));
    public static final Item GREEN_SHULKER_BOX = register("green_shulker_box", new BlockItem("Green Shulker Box", new Block(new BlockState())));
    public static final Item RED_SHULKER_BOX = register("red_shulker_box", new BlockItem("Red Shulker Box", new Block(new BlockState())));
    public static final Item BLACK_SHULKER_BOX = register("black_shulker_box", new BlockItem("Black Shulker Box", new Block(new BlockState())));
    public static final Item REDSTONE = register("redstone",
            new Item("Redstone").withTag(itemTag("c", "dusts/redstone")));
    public static final Item RAIL = register("rail",
            new BlockItem("Rail", new Block(new BlockState().withTag(BlockTags.RAILS))));
    public static final Item REINFORCED_DEEPSLATE = register("reinforced_deepslate",
            new BlockItem("Reinforced Deepslate", new Block(new BlockState()
                    .withTag(BlockTags.BASE_STONE_OVERWORLD)
                    .withTag(BlockTags.MINEABLE_WITH_PICKAXE)
                    .withTag(BlockTags.NEEDS_DIAMOND_TOOL))));

    public static final Item DIAMOND_PICKAXE = register("diamond_pickaxe", new PickaxeItem("Diamond Pickaxe"));
    public static final Item IRON_PICKAXE = register("iron_pickaxe", new PickaxeItem("Iron Pickaxe"));
    public static final Item STONE_PICKAXE = register("stone_pickaxe", new PickaxeItem("Stone Pickaxe"));
    public static final Item WOODEN_PICKAXE = register("wooden_pickaxe", new PickaxeItem("Wooden Pickaxe"));
    public static final Item DIAMOND_AXE = register("diamond_axe", new AxeItem("Diamond Axe"));
    public static final Item IRON_AXE = register("iron_axe", new AxeItem("Iron Axe"));
    public static final Item STONE_AXE = register("stone_axe", new AxeItem("Stone Axe"));
    public static final Item WOODEN_AXE = register("wooden_axe", new AxeItem("Wooden Axe"));
    public static final Item DIAMOND_SHOVEL = register("diamond_shovel", new ShovelItem("Diamond Shovel"));
    public static final Item IRON_SHOVEL = register("iron_shovel", new ShovelItem("Iron Shovel"));
    public static final Item STONE_SHOVEL = register("stone_shovel", new ShovelItem("Stone Shovel"));
    public static final Item WOODEN_SHOVEL = register("wooden_shovel", new ShovelItem("Wooden Shovel"));
    public static final Item DIAMOND_HOE = register("diamond_hoe", new HoeItem("Diamond Hoe"));
    public static final Item IRON_HOE = register("iron_hoe", new HoeItem("Iron Hoe"));
    public static final Item STONE_HOE = register("stone_hoe", new HoeItem("Stone Hoe"));
    public static final Item WOODEN_HOE = register("wooden_hoe", new HoeItem("Wooden Hoe"));

    static {
        ComposterBlock.COMPOSTABLES.put(APPLE, 0.65f);
        ComposterBlock.COMPOSTABLES.put(CAKE, 1.0f);
    }

    private Items() {
    }

    private static Item register(String path, Item item) {
        BuiltInRegistries.itemRegistry().register(Identifier.of("minecraft", path), item);
        return item;
    }

    private static TagKey<Item> itemTag(String namespace, String path) {
        return TagKey.create(null, Identifier.of(namespace, path));
    }
}
