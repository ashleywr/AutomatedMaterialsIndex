package net.minecraft.world.level.block;

import net.minecraft.world.level.block.state.BlockState;

public class Block {
    private final BlockState defaultState;

    public Block(BlockState defaultState) {
        this.defaultState = defaultState;
    }

    public BlockState defaultBlockState() {
        return defaultState;
    }
}
