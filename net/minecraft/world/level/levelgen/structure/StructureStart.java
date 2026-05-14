package net.minecraft.world.level.levelgen.structure;

import com.mojang.logging.LogUtils;
import java.util.List;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.structure.pieces.PiecesContainer;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceSerializationContext;
import net.minecraft.world.level.levelgen.structure.structures.OceanMonumentStructure;
import org.slf4j.Logger;

public final class StructureStart {
    public static final String INVALID_START_ID = "INVALID";
    public static final StructureStart INVALID_START = new StructureStart(null, new ChunkPos(0, 0), 0, new PiecesContainer(List.of()));
    private static final Logger LOGGER = LogUtils.getLogger();
    private final Structure structure;
    private final PiecesContainer pieceContainer;
    private final ChunkPos chunkPos;
    private int references;
    @Nullable
    private volatile BoundingBox cachedBoundingBox;

    public StructureStart(Structure structure, ChunkPos chunkPos, int references, PiecesContainer pieceContainer) {
        this.structure = structure;
        this.chunkPos = chunkPos;
        this.references = references;
        this.pieceContainer = pieceContainer;
    }

    @Nullable
    public static StructureStart loadStaticStart(StructurePieceSerializationContext context, CompoundTag tag, long seed) {
        String s = tag.getString("id");
        if ("INVALID".equals(s)) {
            return INVALID_START;
        } else {
            Registry<Structure> registry = context.registryAccess().registryOrThrow(Registries.STRUCTURE);
            Structure structure = registry.get(ResourceLocation.parse(s));
            if (structure == null) {
                LOGGER.error("Unknown stucture id: {}", s);
                return null;
            } else {
                ChunkPos chunkpos = new ChunkPos(tag.getInt("ChunkX"), tag.getInt("ChunkZ"));
                int i = tag.getInt("references");
                ListTag listtag = tag.getList("Children", 10);

                try {
                    PiecesContainer piecescontainer = PiecesContainer.load(listtag, context);
                    if (structure instanceof OceanMonumentStructure) {
                        piecescontainer = OceanMonumentStructure.regeneratePiecesAfterLoad(chunkpos, seed, piecescontainer);
                    }

                    return new StructureStart(structure, chunkpos, i, piecescontainer);
                } catch (Exception exception) {
                    LOGGER.error("Failed Start with id {}", s, exception);
                    return null;
                }
            }
        }
    }

    public BoundingBox getBoundingBox() {
        BoundingBox boundingbox = this.cachedBoundingBox;
        if (boundingbox == null) {
            boundingbox = this.structure.adjustBoundingBox(this.pieceContainer.calculateBoundingBox());
            this.cachedBoundingBox = boundingbox;
        }

        return boundingbox;
    }

    public void placeInChunk(
        WorldGenLevel level, StructureManager structureManager, ChunkGenerator generator, RandomSource random, BoundingBox box, ChunkPos chunkPos
    ) {
        List<StructurePiece> list = this.pieceContainer.pieces();
        if (!list.isEmpty()) {
            BoundingBox boundingbox = list.get(0).boundingBox;
            BlockPos blockpos = boundingbox.getCenter();
            BlockPos blockpos1 = new BlockPos(blockpos.getX(), boundingbox.minY(), blockpos.getZ());

            for (StructurePiece structurepiece : list) {
                if (structurepiece.getBoundingBox().intersects(box)) {
                    structurepiece.postProcess(level, structureManager, generator, random, box, chunkPos, blockpos1);
                }
            }

            this.structure.afterPlace(level, structureManager, generator, random, box, chunkPos, this.pieceContainer);
        }
    }

    public CompoundTag createTag(StructurePieceSerializationContext context, ChunkPos chunkPos) {
        CompoundTag compoundtag = new CompoundTag();
        if (this.isValid()) {
            if (context.registryAccess().registryOrThrow(Registries.STRUCTURE).getKey(this.getStructure()) == null) { // FORGE: This is just a more friendly error instead of the 'Null String' below
                throw new RuntimeException("StructureStart \"" + this.getClass().getName() + "\": \"" + this.getStructure() + "\" unregistered, serializing impossible.");
            }
            compoundtag.putString("id", context.registryAccess().registryOrThrow(Registries.STRUCTURE).getKey(this.structure).toString());
            compoundtag.putInt("ChunkX", chunkPos.x);
            compoundtag.putInt("ChunkZ", chunkPos.z);
            compoundtag.putInt("references", this.references);
            compoundtag.put("Children", this.pieceContainer.save(context));
            return compoundtag;
        } else {
            compoundtag.putString("id", "INVALID");
            return compoundtag;
        }
    }

    public boolean isValid() {
        return !this.pieceContainer.isEmpty();
    }

    public ChunkPos getChunkPos() {
        return this.chunkPos;
    }

    public boolean canBeReferenced() {
        return this.references < this.getMaxReferences();
    }

    public void addReference() {
        this.references++;
    }

    public int getReferences() {
        return this.references;
    }

    protected int getMaxReferences() {
        return 1;
    }

    public Structure getStructure() {
        return this.structure;
    }

    public List<StructurePiece> getPieces() {
        return this.pieceContainer.pieces();
    }
}
