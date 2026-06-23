package com.sanhiruzu.ami.index.providers;

import com.sanhiruzu.ami.AmiCore;
import com.sanhiruzu.ami.index.EdgeType;
import com.sanhiruzu.ami.index.GlobalIndex;
import com.sanhiruzu.ami.index.IAmiDataProvider;
import com.sanhiruzu.ami.index.NodeType;
import com.sanhiruzu.ami.index.SearchNode;
import com.sanhiruzu.ami.platform.Services;
import net.minecraft.client.Minecraft;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.MobSpawnSettings;
import org.jetbrains.annotations.Nullable;

import java.util.HashSet;
import java.util.Set;

/**
 * Scans biome spawn settings and records entity->biome spawn relations.
 */
public class SpawnProvider implements IAmiDataProvider {

    @Override
    public void populate(GlobalIndex index, @Nullable Level level) {
        if (level == null) return;

        AmiCore.LOGGER.debug("SpawnProvider: scanning biome spawns...");

        try {
            Set<String> seenEdges = new HashSet<>();
            int[] counts = new int[3];
            scanBiomeRegistry(index, level, seenEdges, counts);
            if (counts[1] == 0) {
                Level serverLevel = matchingSingleplayerServerLevel(level);
                if (serverLevel != null) {
                    AmiCore.LOGGER.info("AMI indexing: SpawnProvider client biome registry had no spawn entries; retrying server biome registry.");
                    counts[0] = 0;
                    scanBiomeRegistry(index, serverLevel, seenEdges, counts);
                }
            }
            AmiCore.LOGGER.info("AMI indexing: SpawnProvider scanned {} biomes, {} spawn entries, recorded {} entity->biome spawn edges.",
                    counts[0], counts[1], counts[2]);
        } catch (Exception e) {
            AmiCore.LOGGER.warn("SpawnProvider skipped due to: {}", e.toString());
        }
    }

    private static void scanBiomeRegistry(GlobalIndex index, Level level, Set<String> seenEdges, int[] counts) {
        level.registryAccess().registry(Registries.BIOME).ifPresent(reg ->
                reg.holders().forEach(holder -> {
                    counts[0]++;
                    recordBiomeSpawns(index, holder.key().location(),
                            Services.PLATFORM.getBiomeMobSpawnSettings(holder.value()), seenEdges, counts);
                })
        );
    }

    @Nullable
    private static Level matchingSingleplayerServerLevel(Level clientLevel) {
        try {
            Minecraft minecraft = Minecraft.getInstance();
            if (minecraft == null || minecraft.getSingleplayerServer() == null || clientLevel == null) {
                return null;
            }
            return minecraft.getSingleplayerServer().getLevel(clientLevel.dimension());
        } catch (RuntimeException ignored) {
            return null;
        }
    }

    private static void recordBiomeSpawns(GlobalIndex index, ResourceLocation biomeId, MobSpawnSettings settings,
                                          Set<String> seenEdges, int[] counts) {
        if (biomeId == null || settings == null) return;
        SearchNode biomeNode = index.getNode(biomeId, NodeType.BIOME).orElse(null);
        if (biomeNode == null) return;

        for (MobCategory category : MobCategory.values()) {
            for (MobSpawnSettings.SpawnerData spawnerData : settings.getMobs(category).unwrap()) {
                counts[1]++;
                ResourceLocation entityId = BuiltInRegistries.ENTITY_TYPE.getKey(spawnerData.type);
                if (entityId == null) continue;

                String edgeKey = entityId + "->" + biomeId;
                if (!seenEdges.add(edgeKey)) continue;

                index.getNode(entityId, NodeType.ENTITY)
                        .ifPresent(entityNode -> {
                            entityNode.addUnresolvedEdge(EdgeType.SPAWNS_IN, biomeId);
                            entityNode.addResolvedEdge(EdgeType.SPAWNS_IN, biomeNode);
                            counts[2]++;
                        });
            }
        }
    }
}
