package net.minecraft.world.level.block.state;

import net.minecraft.core.BlockPos;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.properties.Property;

import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.stream.Stream;

public class BlockState {
    private final Set<TagKey<Block>> tags = new LinkedHashSet<>();
    private final Set<Property<?>> properties = new LinkedHashSet<>();
    private int lightEmission;
    private boolean signalSource;
    private boolean analogOutputSignal;

    public BlockState withTag(TagKey<Block> tag) {
        tags.add(tag);
        return this;
    }

    public BlockState withLightEmission(int lightEmission) {
        this.lightEmission = lightEmission;
        return this;
    }

    public BlockState withSignalSource(boolean signalSource) {
        this.signalSource = signalSource;
        return this;
    }

    public BlockState withAnalogOutputSignal(boolean analogOutputSignal) {
        this.analogOutputSignal = analogOutputSignal;
        return this;
    }

    public BlockState withProperty(Property<?> property) {
        properties.add(property);
        return this;
    }

    public boolean is(TagKey<Block> tag) {
        return tags.contains(tag);
    }

    public Stream<TagKey<Block>> getTags() {
        return tags.stream();
    }

    public Collection<Property<?>> getProperties() {
        return properties;
    }

    public int getLightEmission() {
        return lightEmission;
    }

    public boolean isSignalSource() {
        return signalSource;
    }

    public boolean hasAnalogOutputSignal() {
        return analogOutputSignal;
    }

    public boolean isCollisionShapeFullBlock(BlockGetter level, BlockPos pos) {
        return false;
    }
}
