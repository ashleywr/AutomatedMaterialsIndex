package net.minecraft.world.level.block.state;

import net.minecraft.tags.TagKey;
import net.minecraft.world.level.block.Block;

import java.util.LinkedHashSet;
import java.util.Set;

public class BlockState {
    private final Set<TagKey<Block>> tags = new LinkedHashSet<>();
    private int lightEmission;

    public BlockState withTag(TagKey<Block> tag) {
        tags.add(tag);
        return this;
    }

    public BlockState withLightEmission(int lightEmission) {
        this.lightEmission = lightEmission;
        return this;
    }

    public boolean is(TagKey<Block> tag) {
        return tags.contains(tag);
    }

    public int getLightEmission() {
        return lightEmission;
    }
}
