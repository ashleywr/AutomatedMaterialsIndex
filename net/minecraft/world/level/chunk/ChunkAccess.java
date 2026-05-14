package net.minecraft.world.level.chunk;

import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.mojang.logging.LogUtils;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import it.unimi.dsi.fastutil.longs.LongSet;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.shorts.ShortArrayList;
import it.unimi.dsi.fastutil.shorts.ShortList;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumSet;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;
import javax.annotation.Nullable;
import net.minecraft.CrashReport;
import net.minecraft.CrashReportCategory;
import net.minecraft.ReportedException;
import net.minecraft.SharedConstants;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.QuartPos;
import net.minecraft.core.Registry;
import net.minecraft.core.SectionPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.LevelHeightAccessor;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.BiomeGenerationSettings;
import net.minecraft.world.level.biome.BiomeManager;
import net.minecraft.world.level.biome.BiomeResolver;
import net.minecraft.world.level.biome.Climate;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.status.ChunkStatus;
import net.minecraft.world.level.gameevent.GameEventListenerRegistry;
import net.minecraft.world.level.levelgen.BelowZeroRetrogen;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.NoiseChunk;
import net.minecraft.world.level.levelgen.blending.BlendingData;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructureStart;
import net.minecraft.world.level.lighting.ChunkSkyLightSources;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.ticks.SerializableTickContainer;
import net.minecraft.world.ticks.TickContainerAccess;
import org.slf4j.Logger;

public abstract class ChunkAccess implements BlockGetter, BiomeManager.NoiseBiomeSource, LightChunk, StructureAccess, net.neoforged.neoforge.attachment.IAttachmentHolder {
    public static final int NO_FILLED_SECTION = -1;
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final LongSet EMPTY_REFERENCE_SET = new LongOpenHashSet();
    protected final ShortList[] postProcessing;
    protected volatile boolean unsaved;
    private volatile boolean isLightCorrect;
    protected final ChunkPos chunkPos;
    private long inhabitedTime;
    @Nullable
    @Deprecated
    private BiomeGenerationSettings carverBiomeSettings;
    @Nullable
    protected NoiseChunk noiseChunk;
    protected final UpgradeData upgradeData;
    @Nullable
    protected BlendingData blendingData;
    protected final Map<Heightmap.Types, Heightmap> heightmaps = Maps.newEnumMap(Heightmap.Types.class);
    protected ChunkSkyLightSources skyLightSources;
    private final Map<Structure, StructureStart> structureStarts = Maps.newHashMap();
    private final Map<Structure, LongSet> structuresRefences = Maps.newHashMap();
    protected final Map<BlockPos, CompoundTag> pendingBlockEntities = Maps.newHashMap();
    protected final Map<BlockPos, BlockEntity> blockEntities = new Object2ObjectOpenHashMap<>();
    protected final LevelHeightAccessor levelHeightAccessor;
    protected final LevelChunkSection[] sections;

    public ChunkAccess(
        ChunkPos chunkPos,
        UpgradeData upgradeData,
        LevelHeightAccessor levelHeightAccessor,
        Registry<Biome> biomeRegistry,
        long inhabitedTime,
        @Nullable LevelChunkSection[] sections,
        @Nullable BlendingData blendingData
    ) {
        this.chunkPos = chunkPos;
        this.upgradeData = upgradeData;
        this.levelHeightAccessor = levelHeightAccessor;
        this.sections = new LevelChunkSection[levelHeightAccessor.getSectionsCount()];
        this.inhabitedTime = inhabitedTime;
        this.postProcessing = new ShortList[levelHeightAccessor.getSectionsCount()];
        this.blendingData = blendingData;
        this.skyLightSources = new ChunkSkyLightSources(levelHeightAccessor);
        if (sections != null) {
            if (this.sections.length == sections.length) {
                System.arraycopy(sections, 0, this.sections, 0, this.sections.length);
            } else {
                LOGGER.warn("Could not set level chunk sections, array length is {} instead of {}", sections.length, this.sections.length);
            }
        }

        replaceMissingSections(biomeRegistry, this.sections);
    }

    private static void replaceMissingSections(Registry<Biome> biomeRegistry, LevelChunkSection[] sections) {
        for (int i = 0; i < sections.length; i++) {
            if (sections[i] == null) {
                sections[i] = new LevelChunkSection(biomeRegistry);
            }
        }
    }

    public GameEventListenerRegistry getListenerRegistry(int sectionY) {
        return GameEventListenerRegistry.NOOP;
    }

    @Nullable
    public abstract BlockState setBlockState(BlockPos pos, BlockState state, boolean isMoving);

    public abstract void setBlockEntity(BlockEntity blockEntity);

    public abstract void addEntity(Entity entity);

    public int getHighestFilledSectionIndex() {
        LevelChunkSection[] alevelchunksection = this.getSections();

        for (int i = alevelchunksection.length - 1; i >= 0; i--) {
            LevelChunkSection levelchunksection = alevelchunksection[i];
            if (!levelchunksection.hasOnlyAir()) {
                return i;
            }
        }

        return -1;
    }

    @Deprecated(
        forRemoval = true
    )
    public int getHighestSectionPosition() {
        int i = this.getHighestFilledSectionIndex();
        return i == -1 ? this.getMinBuildHeight() : SectionPos.sectionToBlockCoord(this.getSectionYFromSectionIndex(i));
    }

    public Set<BlockPos> getBlockEntitiesPos() {
        Set<BlockPos> set = Sets.newHashSet(this.pendingBlockEntities.keySet());
        set.addAll(this.blockEntities.keySet());
        return set;
    }

    public LevelChunkSection[] getSections() {
        return this.sections;
    }

    public LevelChunkSection getSection(int index) {
        return this.getSections()[index];
    }

    public Collection<Entry<Heightmap.Types, Heightmap>> getHeightmaps() {
        return Collections.unmodifiableSet(this.heightmaps.entrySet());
    }

    public void setHeightmap(Heightmap.Types type, long[] data) {
        this.getOrCreateHeightmapUnprimed(type).setRawData(this, type, data);
    }

    public Heightmap getOrCreateHeightmapUnprimed(Heightmap.Types type) {
        return this.heightmaps.computeIfAbsent(type, p_187665_ -> new Heightmap(this, p_187665_));
    }

    public boolean hasPrimedHeightmap(Heightmap.Types type) {
        return this.heightmaps.get(type) != null;
    }

    public int getHeight(Heightmap.Types type, int x, int z) {
        Heightmap heightmap = this.heightmaps.get(type);
        if (heightmap == null) {
            if (SharedConstants.IS_RUNNING_IN_IDE && this instanceof LevelChunk) {
                LOGGER.error("Unprimed heightmap: " + type + " " + x + " " + z);
            }

            Heightmap.primeHeightmaps(this, EnumSet.of(type));
            heightmap = this.heightmaps.get(type);
        }

        return heightmap.getFirstAvailable(x & 15, z & 15) - 1;
    }

    public ChunkPos getPos() {
        return this.chunkPos;
    }

    @Nullable
    @Override
    public StructureStart getStartForStructure(Structure structure) {
        return this.structureStarts.get(structure);
    }

    @Override
    public void setStartForStructure(Structure structure, StructureStart structureStart) {
        this.structureStarts.put(structure, structureStart);
        this.unsaved = true;
    }

    public Map<Structure, StructureStart> getAllStarts() {
        return Collections.unmodifiableMap(this.structureStarts);
    }

    public void setAllStarts(Map<Structure, StructureStart> structureStarts) {
        this.structureStarts.clear();
        this.structureStarts.putAll(structureStarts);
        this.unsaved = true;
    }

    @Override
    public LongSet getReferencesForStructure(Structure structure) {
        return this.structuresRefences.getOrDefault(structure, EMPTY_REFERENCE_SET);
    }

    @Override
    public void addReferenceForStructure(Structure structure, long reference) {
        this.structuresRefences.computeIfAbsent(structure, p_223019_ -> new LongOpenHashSet()).add(reference);
        this.unsaved = true;
    }

    @Override
    public Map<Structure, LongSet> getAllReferences() {
        return Collections.unmodifiableMap(this.structuresRefences);
    }

    @Override
    public void setAllReferences(Map<Structure, LongSet> structureReferencesMap) {
        this.structuresRefences.clear();
        this.structuresRefences.putAll(structureReferencesMap);
        this.unsaved = true;
    }

    public boolean isYSpaceEmpty(int startY, int endY) {
        if (startY < this.getMinBuildHeight()) {
            startY = this.getMinBuildHeight();
        }

        if (endY >= this.getMaxBuildHeight()) {
            endY = this.getMaxBuildHeight() - 1;
        }

        for (int i = startY; i <= endY; i += 16) {
            if (!this.getSection(this.getSectionIndex(i)).hasOnlyAir()) {
                return false;
            }
        }

        return true;
    }

    public boolean isSectionEmpty(int y) {
        return this.getSection(this.getSectionIndexFromSectionY(y)).hasOnlyAir();
    }

    public void setUnsaved(boolean unsaved) {
        this.unsaved = unsaved;
    }

    public boolean isUnsaved() {
        return this.unsaved;
    }

    public abstract ChunkStatus getPersistedStatus();

    public ChunkStatus getHighestGeneratedStatus() {
        ChunkStatus chunkstatus = this.getPersistedStatus();
        BelowZeroRetrogen belowzeroretrogen = this.getBelowZeroRetrogen();
        if (belowzeroretrogen != null) {
            ChunkStatus chunkstatus1 = belowzeroretrogen.targetStatus();
            return ChunkStatus.max(chunkstatus1, chunkstatus);
        } else {
            return chunkstatus;
        }
    }

    public abstract void removeBlockEntity(BlockPos pos);

    public void markPosForPostprocessing(BlockPos pos) {
        LOGGER.warn("Trying to mark a block for PostProcessing @ {}, but this operation is not supported.", pos);
    }

    public ShortList[] getPostProcessing() {
        return this.postProcessing;
    }

    public void addPackedPostProcess(short packedPosition, int index) {
        getOrCreateOffsetList(this.getPostProcessing(), index).add(packedPosition);
    }

    public void setBlockEntityNbt(CompoundTag tag) {
        this.pendingBlockEntities.put(BlockEntity.getPosFromTag(tag), tag);
    }

    @Nullable
    public CompoundTag getBlockEntityNbt(BlockPos pos) {
        return this.pendingBlockEntities.get(pos);
    }

    @Nullable
    public abstract CompoundTag getBlockEntityNbtForSaving(BlockPos pos, HolderLookup.Provider registries);

    @Override
    public final void findBlockLightSources(BiConsumer<BlockPos, BlockState> output) {
        this.findBlocks(p_284897_ -> p_284897_.hasDynamicLightEmission() || p_284897_.getLightEmission(net.minecraft.world.level.EmptyBlockGetter.INSTANCE, BlockPos.ZERO) != 0, (p_284897_, pos) -> p_284897_.getLightEmission(this, pos) != 0, output);
    }

    public void findBlocks(Predicate<BlockState> predicate, BiConsumer<BlockPos, BlockState> output) {
        findBlocks(predicate, (state, pos) -> predicate.test(state), output);
    }

    @Deprecated(forRemoval = true)
    public void findBlocks(java.util.function.BiPredicate<BlockState, BlockPos> p_285343_, BiConsumer<BlockPos, BlockState> p_285030_) {
        findBlocks(state -> p_285343_.test(state, BlockPos.ZERO), p_285343_, p_285030_);
    }

    public void findBlocks(Predicate<BlockState> p_285343_, java.util.function.BiPredicate<BlockState, BlockPos> fineFilter, BiConsumer<BlockPos, BlockState> p_285030_) {
        BlockPos.MutableBlockPos blockpos$mutableblockpos = new BlockPos.MutableBlockPos();

        for (int i = this.getMinSection(); i < this.getMaxSection(); i++) {
            LevelChunkSection levelchunksection = this.getSection(this.getSectionIndexFromSectionY(i));
            if (levelchunksection.maybeHas(p_285343_)) {
                BlockPos blockpos = SectionPos.of(this.chunkPos, i).origin();

                for (int j = 0; j < 16; j++) {
                    for (int k = 0; k < 16; k++) {
                        for (int l = 0; l < 16; l++) {
                            BlockState blockstate = levelchunksection.getBlockState(l, j, k);
                            blockpos$mutableblockpos.setWithOffset(blockpos, l, j, k);
                            if (fineFilter.test(blockstate, blockpos$mutableblockpos)) {
                                p_285030_.accept(blockpos$mutableblockpos, blockstate);
                            }
                        }
                    }
                }
            }
        }
    }

    public abstract TickContainerAccess<Block> getBlockTicks();

    public abstract TickContainerAccess<Fluid> getFluidTicks();

    public abstract ChunkAccess.TicksToSave getTicksForSerialization();

    public UpgradeData getUpgradeData() {
        return this.upgradeData;
    }

    public boolean isOldNoiseGeneration() {
        return this.blendingData != null;
    }

    @Nullable
    public BlendingData getBlendingData() {
        return this.blendingData;
    }

    public void setBlendingData(BlendingData blendingData) {
        this.blendingData = blendingData;
    }

    public long getInhabitedTime() {
        return this.inhabitedTime;
    }

    public void incrementInhabitedTime(long amount) {
        this.inhabitedTime += amount;
    }

    public void setInhabitedTime(long inhabitedTime) {
        this.inhabitedTime = inhabitedTime;
    }

    public static ShortList getOrCreateOffsetList(ShortList[] packedPositions, int index) {
        if (packedPositions[index] == null) {
            packedPositions[index] = new ShortArrayList();
        }

        return packedPositions[index];
    }

    public boolean isLightCorrect() {
        return this.isLightCorrect;
    }

    public void setLightCorrect(boolean lightCorrect) {
        this.isLightCorrect = lightCorrect;
        this.setUnsaved(true);
    }

    @Override
    public int getMinBuildHeight() {
        return this.levelHeightAccessor.getMinBuildHeight();
    }

    @Override
    public int getHeight() {
        return this.levelHeightAccessor.getHeight();
    }

    public NoiseChunk getOrCreateNoiseChunk(Function<ChunkAccess, NoiseChunk> noiseChunkCreator) {
        if (this.noiseChunk == null) {
            this.noiseChunk = noiseChunkCreator.apply(this);
        }

        return this.noiseChunk;
    }

    @Deprecated
    public BiomeGenerationSettings carverBiome(Supplier<BiomeGenerationSettings> caverBiomeSettingsSupplier) {
        if (this.carverBiomeSettings == null) {
            this.carverBiomeSettings = caverBiomeSettingsSupplier.get();
        }

        return this.carverBiomeSettings;
    }

    /**
     * Gets the biome at the given quart positions.
     * Note that the coordinates passed into this method are 1/4 the scale of block coordinates.
     */
    @Override
    public Holder<Biome> getNoiseBiome(int x, int y, int z) {
        try {
            int i = QuartPos.fromBlock(this.getMinBuildHeight());
            int k = i + QuartPos.fromBlock(this.getHeight()) - 1;
            int l = Mth.clamp(y, i, k);
            int j = this.getSectionIndex(QuartPos.toBlock(l));
            return this.sections[j].getNoiseBiome(x & 3, l & 3, z & 3);
        } catch (Throwable throwable) {
            CrashReport crashreport = CrashReport.forThrowable(throwable, "Getting biome");
            CrashReportCategory crashreportcategory = crashreport.addCategory("Biome being got");
            crashreportcategory.setDetail("Location", () -> CrashReportCategory.formatLocation(this, x, y, z));
            throw new ReportedException(crashreport);
        }
    }

    public void fillBiomesFromNoise(BiomeResolver resolver, Climate.Sampler sampler) {
        ChunkPos chunkpos = this.getPos();
        int i = QuartPos.fromBlock(chunkpos.getMinBlockX());
        int j = QuartPos.fromBlock(chunkpos.getMinBlockZ());
        LevelHeightAccessor levelheightaccessor = this.getHeightAccessorForGeneration();

        for (int k = levelheightaccessor.getMinSection(); k < levelheightaccessor.getMaxSection(); k++) {
            LevelChunkSection levelchunksection = this.getSection(this.getSectionIndexFromSectionY(k));
            int l = QuartPos.fromSection(k);
            levelchunksection.fillBiomesFromNoise(resolver, sampler, i, l, j);
        }
    }

    public boolean hasAnyStructureReferences() {
        return !this.getAllReferences().isEmpty();
    }

    @Nullable
    public BelowZeroRetrogen getBelowZeroRetrogen() {
        return null;
    }

    public boolean isUpgrading() {
        return this.getBelowZeroRetrogen() != null;
    }

    public LevelHeightAccessor getHeightAccessorForGeneration() {
        return this;
    }

    public void initializeLightSources() {
        this.skyLightSources.fillFrom(this);
    }

    @Override
    public ChunkSkyLightSources getSkyLightSources() {
        return this.skyLightSources;
    }

    public static record TicksToSave(SerializableTickContainer<Block> blocks, SerializableTickContainer<Fluid> fluids) {
    }

    // Neo: Hook in AttachmentHolder to chunks for data storage and retrieval
    private final net.neoforged.neoforge.attachment.AttachmentHolder.AsField attachmentHolder = new net.neoforged.neoforge.attachment.AttachmentHolder.AsField(this);

    @Override
    public boolean hasAttachments() {
        return getAttachmentHolder().hasAttachments();
    }

    @Override
    public boolean hasData(net.neoforged.neoforge.attachment.AttachmentType<?> type) {
        return getAttachmentHolder().hasData(type);
    }

    @Override
    public <T> T getData(net.neoforged.neoforge.attachment.AttachmentType<T> type) {
        return getAttachmentHolder().getData(type);
    }

    @Override
    @Nullable
    public <T> T getExistingDataOrNull(net.neoforged.neoforge.attachment.AttachmentType<T> type) {
        return getAttachmentHolder().getExistingDataOrNull(type);
    }

    @Override
    @Nullable
    public <T> T setData(net.neoforged.neoforge.attachment.AttachmentType<T> type, T data) {
        setUnsaved(true);
        return getAttachmentHolder().setData(type, data);
    }

    @Override
    @Nullable
    public <T> T removeData(net.neoforged.neoforge.attachment.AttachmentType<T> type) {
        setUnsaved(true);
        return getAttachmentHolder().removeData(type);
    }

    /**
     * <strong>FOR INTERNAL USE ONLY</strong>
     * <p>
     * Only public for use in {@link net.minecraft.world.level.chunk.storage.ChunkSerializer}.
     */
    @org.jetbrains.annotations.ApiStatus.Internal
    @Nullable
    public final CompoundTag writeAttachmentsToNBT(HolderLookup.Provider provider) {
        return getAttachmentHolder().serializeAttachments(provider);
    }

    /**
     * <strong>FOR INTERNAL USE ONLY</strong>
     * <p>
     * Only public for use in {@link net.minecraft.world.level.chunk.storage.ChunkSerializer}.
     *
     */
    @org.jetbrains.annotations.ApiStatus.Internal
    public final void readAttachmentsFromNBT(HolderLookup.Provider provider, CompoundTag tag) {
        getAttachmentHolder().deserializeInternal(provider, tag);
    }

    @org.jetbrains.annotations.ApiStatus.Internal
    public net.neoforged.neoforge.attachment.AttachmentHolder.AsField getAttachmentHolder() {
        return attachmentHolder;
    }

    // Neo: Allow for exposing the Level a chunk is tied to if available
    @Nullable
    public net.minecraft.world.level.Level getLevel() { return null; }
}
