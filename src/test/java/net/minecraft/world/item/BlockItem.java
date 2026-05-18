package net.minecraft.world.item;

import net.minecraft.world.level.block.Block;

public class BlockItem extends Item {
    private final Block block;

    public BlockItem(String name, Block block) {
        super(name);
        this.block = block;
    }

    public Block getBlock() {
        return block;
    }
}
