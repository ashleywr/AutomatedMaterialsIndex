package net.minecraft.world.level.levelgen.structure;

import com.google.common.base.MoreObjects;
import com.mojang.logging.LogUtils;
import com.mojang.serialization.Codec;
import java.util.Iterator;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.stream.IntStream;
import java.util.stream.Stream;
import net.minecraft.SharedConstants;
import net.minecraft.Util;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.SectionPos;
import net.minecraft.core.Vec3i;
import net.minecraft.world.level.ChunkPos;
import org.slf4j.Logger;

/**
 * A simple three-dimensional mutable integer bounding box.
 * Note that this box is both mutable, and has an implementation of {@code hashCode()} and {@code equals()}.
 * This can be used as {@code HashMap} keys for example, if the user can ensure the instances themselves are not modified.
 */
public class BoundingBox {
    private static final Logger LOGGER = LogUtils.getLogger();
    public static final Codec<BoundingBox> CODEC = Codec.INT_STREAM
        .<BoundingBox>comapFlatMap(
            p_338100_ -> Util.fixedSize(p_338100_, 6)
                    .map(p_162385_ -> new BoundingBox(p_162385_[0], p_162385_[1], p_162385_[2], p_162385_[3], p_162385_[4], p_162385_[5])),
            p_162391_ -> IntStream.of(p_162391_.minX, p_162391_.minY, p_162391_.minZ, p_162391_.maxX, p_162391_.maxY, p_162391_.maxZ)
        )
        .stable();
    private int minX;
    private int minY;
    private int minZ;
    private int maxX;
    private int maxY;
    private int maxZ;

    public BoundingBox(BlockPos pos) {
        this(pos.getX(), pos.getY(), pos.getZ(), pos.getX(), pos.getY(), pos.getZ());
    }

    public BoundingBox(int minX, int minY, int minZ, int maxX, int maxY, int maxZ) {
        this.minX = minX;
        this.minY = minY;
        this.minZ = minZ;
        this.maxX = maxX;
        this.maxY = maxY;
        this.maxZ = maxZ;
        if (maxX < minX || maxY < minY || maxZ < minZ) {
            String s = "Invalid bounding box data, inverted bounds for: " + this;
            if (SharedConstants.IS_RUNNING_IN_IDE) {
                throw new IllegalStateException(s);
            }

            LOGGER.error(s);
            this.minX = Math.min(minX, maxX);
            this.minY = Math.min(minY, maxY);
            this.minZ = Math.min(minZ, maxZ);
            this.maxX = Math.max(minX, maxX);
            this.maxY = Math.max(minY, maxY);
            this.maxZ = Math.max(minZ, maxZ);
        }
    }

    public static BoundingBox fromCorners(Vec3i first, Vec3i second) {
        return new BoundingBox(
            Math.min(first.getX(), second.getX()),
            Math.min(first.getY(), second.getY()),
            Math.min(first.getZ(), second.getZ()),
            Math.max(first.getX(), second.getX()),
            Math.max(first.getY(), second.getY()),
            Math.max(first.getZ(), second.getZ())
        );
    }

    public static BoundingBox infinite() {
        return new BoundingBox(Integer.MIN_VALUE, Integer.MIN_VALUE, Integer.MIN_VALUE, Integer.MAX_VALUE, Integer.MAX_VALUE, Integer.MAX_VALUE);
    }

    /**
     * Create a bounding box with the specified dimensions and rotate it. Used to project a possible new component Bounding Box - to check if it would cut anything already spawned.
     */
    public static BoundingBox orientBox(
        int structureMinX, int structureMinY, int structureMinZ, int xMin, int yMin, int zMin, int xMax, int yMax, int zMax, Direction facing
    ) {
        switch (facing) {
            case SOUTH:
            default:
                return new BoundingBox(
                    structureMinX + xMin,
                    structureMinY + yMin,
                    structureMinZ + zMin,
                    structureMinX + xMax - 1 + xMin,
                    structureMinY + yMax - 1 + yMin,
                    structureMinZ + zMax - 1 + zMin
                );
            case NORTH:
                return new BoundingBox(
                    structureMinX + xMin,
                    structureMinY + yMin,
                    structureMinZ - zMax + 1 + zMin,
                    structureMinX + xMax - 1 + xMin,
                    structureMinY + yMax - 1 + yMin,
                    structureMinZ + zMin
                );
            case WEST:
                return new BoundingBox(
                    structureMinX - zMax + 1 + zMin,
                    structureMinY + yMin,
                    structureMinZ + xMin,
                    structureMinX + zMin,
                    structureMinY + yMax - 1 + yMin,
                    structureMinZ + xMax - 1 + xMin
                );
            case EAST:
                return new BoundingBox(
                    structureMinX + zMin,
                    structureMinY + yMin,
                    structureMinZ + xMin,
                    structureMinX + zMax - 1 + zMin,
                    structureMinY + yMax - 1 + yMin,
                    structureMinZ + xMax - 1 + xMin
                );
        }
    }

    public Stream<ChunkPos> intersectingChunks() {
        int i = SectionPos.blockToSectionCoord(this.minX());
        int j = SectionPos.blockToSectionCoord(this.minZ());
        int k = SectionPos.blockToSectionCoord(this.maxX());
        int l = SectionPos.blockToSectionCoord(this.maxZ());
        return ChunkPos.rangeClosed(new ChunkPos(i, j), new ChunkPos(k, l));
    }

    /**
     * @return {@code true} if {@code box} intersects this box.
     */
    public boolean intersects(BoundingBox box) {
        return this.maxX >= box.minX
            && this.minX <= box.maxX
            && this.maxZ >= box.minZ
            && this.minZ <= box.maxZ
            && this.maxY >= box.minY
            && this.minY <= box.maxY;
    }

    /**
     * @return {@code true} if this bounding box intersects the horizontal x/z region described by the min and max parameters.
     */
    public boolean intersects(int minX, int minZ, int maxX, int maxZ) {
        return this.maxX >= minX && this.minX <= maxX && this.maxZ >= minZ && this.minZ <= maxZ;
    }

    public static Optional<BoundingBox> encapsulatingPositions(Iterable<BlockPos> positions) {
        Iterator<BlockPos> iterator = positions.iterator();
        if (!iterator.hasNext()) {
            return Optional.empty();
        } else {
            BoundingBox boundingbox = new BoundingBox(iterator.next());
            iterator.forEachRemaining(boundingbox::encapsulate);
            return Optional.of(boundingbox);
        }
    }

    public static Optional<BoundingBox> encapsulatingBoxes(Iterable<BoundingBox> boxes) {
        Iterator<BoundingBox> iterator = boxes.iterator();
        if (!iterator.hasNext()) {
            return Optional.empty();
        } else {
            BoundingBox boundingbox = iterator.next();
            BoundingBox boundingbox1 = new BoundingBox(
                boundingbox.minX, boundingbox.minY, boundingbox.minZ, boundingbox.maxX, boundingbox.maxY, boundingbox.maxZ
            );
            iterator.forEachRemaining(boundingbox1::encapsulate);
            return Optional.of(boundingbox1);
        }
    }

    /**
     * Expands this box to be at least large enough to contain {@code box}.
     */
    @Deprecated
    public BoundingBox encapsulate(BoundingBox box) {
        this.minX = Math.min(this.minX, box.minX);
        this.minY = Math.min(this.minY, box.minY);
        this.minZ = Math.min(this.minZ, box.minZ);
        this.maxX = Math.max(this.maxX, box.maxX);
        this.maxY = Math.max(this.maxY, box.maxY);
        this.maxZ = Math.max(this.maxZ, box.maxZ);
        return this;
    }

    /**
     * Expands this box to be at least large enough to contain {@code pos}.
     */
    @Deprecated
    public BoundingBox encapsulate(BlockPos pos) {
        this.minX = Math.min(this.minX, pos.getX());
        this.minY = Math.min(this.minY, pos.getY());
        this.minZ = Math.min(this.minZ, pos.getZ());
        this.maxX = Math.max(this.maxX, pos.getX());
        this.maxY = Math.max(this.maxY, pos.getY());
        this.maxZ = Math.max(this.maxZ, pos.getZ());
        return this;
    }

    /**
     * Translates this box by the given coordinates, modifying the current box.
     */
    @Deprecated
    public BoundingBox move(int x, int y, int z) {
        this.minX += x;
        this.minY += y;
        this.minZ += z;
        this.maxX += x;
        this.maxY += y;
        this.maxZ += z;
        return this;
    }

    /**
     * Translates this box by the given vector, modifying the current box.
     */
    @Deprecated
    public BoundingBox move(Vec3i vector) {
        return this.move(vector.getX(), vector.getY(), vector.getZ());
    }

    /**
     * @return A new bounding box equal to this box, translated by the given coordinates.
     */
    public BoundingBox moved(int x, int y, int z) {
        return new BoundingBox(
            this.minX + x, this.minY + y, this.minZ + z, this.maxX + x, this.maxY + y, this.maxZ + z
        );
    }

    /**
     * Expands this box by a fixed {@code value} in all directions.
     */
    public BoundingBox inflatedBy(int value) {
        return this.inflatedBy(value, value, value);
    }

    public BoundingBox inflatedBy(int x, int y, int z) {
        return new BoundingBox(
            this.minX() - x,
            this.minY() - y,
            this.minZ() - z,
            this.maxX() + x,
            this.maxY() + y,
            this.maxZ() + z
        );
    }

    /**
     * @return {@code true} if the bounding box contains the {@code vector}.
     */
    public boolean isInside(Vec3i vector) {
        return this.isInside(vector.getX(), vector.getY(), vector.getZ());
    }

    public boolean isInside(int x, int y, int z) {
        return x >= this.minX
            && x <= this.maxX
            && z >= this.minZ
            && z <= this.maxZ
            && y >= this.minY
            && y <= this.maxY;
    }

    public Vec3i getLength() {
        return new Vec3i(this.maxX - this.minX, this.maxY - this.minY, this.maxZ - this.minZ);
    }

    public int getXSpan() {
        return this.maxX - this.minX + 1;
    }

    public int getYSpan() {
        return this.maxY - this.minY + 1;
    }

    public int getZSpan() {
        return this.maxZ - this.minZ + 1;
    }

    public BlockPos getCenter() {
        return new BlockPos(
            this.minX + (this.maxX - this.minX + 1) / 2, this.minY + (this.maxY - this.minY + 1) / 2, this.minZ + (this.maxZ - this.minZ + 1) / 2
        );
    }

    public void forAllCorners(Consumer<BlockPos> pos) {
        BlockPos.MutableBlockPos blockpos$mutableblockpos = new BlockPos.MutableBlockPos();
        pos.accept(blockpos$mutableblockpos.set(this.maxX, this.maxY, this.maxZ));
        pos.accept(blockpos$mutableblockpos.set(this.minX, this.maxY, this.maxZ));
        pos.accept(blockpos$mutableblockpos.set(this.maxX, this.minY, this.maxZ));
        pos.accept(blockpos$mutableblockpos.set(this.minX, this.minY, this.maxZ));
        pos.accept(blockpos$mutableblockpos.set(this.maxX, this.maxY, this.minZ));
        pos.accept(blockpos$mutableblockpos.set(this.minX, this.maxY, this.minZ));
        pos.accept(blockpos$mutableblockpos.set(this.maxX, this.minY, this.minZ));
        pos.accept(blockpos$mutableblockpos.set(this.minX, this.minY, this.minZ));
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
            .add("minX", this.minX)
            .add("minY", this.minY)
            .add("minZ", this.minZ)
            .add("maxX", this.maxX)
            .add("maxY", this.maxY)
            .add("maxZ", this.maxZ)
            .toString();
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        } else {
            return !(other instanceof BoundingBox boundingbox)
                ? false
                : this.minX == boundingbox.minX
                    && this.minY == boundingbox.minY
                    && this.minZ == boundingbox.minZ
                    && this.maxX == boundingbox.maxX
                    && this.maxY == boundingbox.maxY
                    && this.maxZ == boundingbox.maxZ;
        }
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.minX, this.minY, this.minZ, this.maxX, this.maxY, this.maxZ);
    }

    public int minX() {
        return this.minX;
    }

    public int minY() {
        return this.minY;
    }

    public int minZ() {
        return this.minZ;
    }

    public int maxX() {
        return this.maxX;
    }

    public int maxY() {
        return this.maxY;
    }

    public int maxZ() {
        return this.maxZ;
    }
}
