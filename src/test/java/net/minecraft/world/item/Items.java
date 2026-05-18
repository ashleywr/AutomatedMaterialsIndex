package net.minecraft.world.item;

import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.ComposterBlock;
import net.minecraft.world.level.block.state.BlockState;

public final class Items {
    public static final Item APPLE = register("apple",
            new Item("Apple").withComponent(DataComponents.FOOD));
    public static final Item CAKE = register("cake",
            new BlockItem("Cake", new Block(new BlockState()))
                    .withComponent(DataComponents.FOOD));
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

    private static Item register(String path, Item item) {
        BuiltInRegistries.itemRegistry().register(ResourceLocation.fromNamespaceAndPath("minecraft", path), item);
        return item;
    }

    private static TagKey<Item> itemTag(String namespace, String path) {
        return TagKey.create(null, ResourceLocation.fromNamespaceAndPath(namespace, path));
    }

    private Items() {}
}
