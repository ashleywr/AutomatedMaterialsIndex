package net.minecraft.world.level.block;

import net.minecraft.core.Holder;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.block.state.BlockState;

import java.util.stream.Stream;

public class Block {
    private final BlockState defaultState;

    public Block(BlockState defaultState) {
        this.defaultState = defaultState;
        defaultState.setBlock(this);
    }

    public BlockState defaultBlockState() {
        return defaultState;
    }

    public Holder.Reference<Block> builtInRegistryHolder() {
        return new Holder.Reference<>() {
            @Override
            public Stream<TagKey<Block>> tags() {
                return defaultState.getTags();
            }

            @Override
            public boolean is(TagKey<Block> tag) {
                return defaultState.is(tag);
            }
        };
    }
}
