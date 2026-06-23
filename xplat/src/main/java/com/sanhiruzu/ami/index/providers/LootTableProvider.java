package com.sanhiruzu.ami.index.providers;

import com.sanhiruzu.ami.AmiCore;
import com.sanhiruzu.ami.index.GlobalIndex;
import com.sanhiruzu.ami.index.IAmiDataProvider;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class LootTableProvider implements IAmiDataProvider {
    @Override
    public void populate(GlobalIndex index, @Nullable Level level) {
        // Keep the primary index pass cheap. Loot table JSON is scanned by indexDeferredDrops().
    }

    public DeferredDropIndexingResult indexDeferredDrops(GlobalIndex index) {
        if (index == null) {
            return DeferredDropIndexingResult.empty();
        }
        Optional<ResourceManager> resourceManager = serverDataResourceManager();
        if (resourceManager.isEmpty()) {
            AmiCore.LOGGER.debug("AMI loot sources: server datapack resource manager unavailable; skipping loot source scan.");
            return DeferredDropIndexingResult.unavailable(EmptyReason.SERVER_DATA_UNAVAILABLE);
        }

        DeferredDropIndexingResult result = DeferredDropIndexingResult.empty();
        result = result.plus(indexRoot(index, resourceManager.get(), "loot_table"));
        result = result.plus(indexRoot(index, resourceManager.get(), "loot_tables"));
        return result;
    }

    private static Optional<ResourceManager> serverDataResourceManager() {
        try {
            Minecraft minecraft = Minecraft.getInstance();
            if (minecraft == null) {
                return Optional.empty();
            }
            var server = minecraft.getSingleplayerServer();
            if (server == null) {
                return Optional.empty();
            }
            ResourceManager resourceManager = server.getResourceManager();
            return resourceManager == null ? Optional.empty() : Optional.of(resourceManager);
        } catch (RuntimeException e) {
            AmiCore.LOGGER.warn("AMI loot sources: server datapack resource manager unavailable: {}", e.getMessage());
            return Optional.empty();
        }
    }

    private static DeferredDropIndexingResult indexRoot(GlobalIndex index, ResourceManager resourceManager, String root) {
        Map<ResourceLocation, Resource> resources;
        try {
            resources = resourceManager.listResources(root, id -> id.getPath().endsWith(".json"));
        } catch (RuntimeException e) {
            AmiCore.LOGGER.warn("AMI loot sources: skipping loot table root '{}' after resource enumeration failed: {}",
                    root, e.getMessage());
            return DeferredDropIndexingResult.empty();
        }
        if (resources.isEmpty()) {
            return new DeferredDropIndexingResult(0, 0, 0, 0);
        }

        DeferredDropIndexingResult result = DeferredDropIndexingResult.empty();
        for (Map.Entry<ResourceLocation, Resource> entry : resources.entrySet()
                .stream()
                .sorted(Comparator.comparing(e -> e.getKey().toString()))
                .toList()) {
            ResourceLocation tableId = normalizeLootTableId(root, entry.getKey());
            Optional<String> json = readResource(entry.getValue());
            if (json.isEmpty()) {
                result = result.plus(new DeferredDropIndexingResult(1, 0, 0, 0));
                continue;
            }
            LootTableDropIndexer.IndexingResult indexed = LootTableDropIndexer.indexEntityLootTables(index, List.of(
                    new LootTableDropIndexer.LootTableResource(tableId, json.get())
            ));
            result = result.plus(new DeferredDropIndexingResult(
                    1,
                    indexed.entityTables(),
                    indexed.itemRefs(),
                    indexed.edgesAdded()
            ));
        }
        return result;
    }

    static ResourceLocation normalizeLootTableId(String root, ResourceLocation resourceId) {
        String path = resourceId.getPath();
        String prefix = root + "/";
        if (path.startsWith(prefix)) {
            path = path.substring(prefix.length());
        }
        if (path.endsWith(".json")) {
            path = path.substring(0, path.length() - ".json".length());
        }
        return ResourceLocation.fromNamespaceAndPath(resourceId.getNamespace(), path);
    }

    private static Optional<String> readResource(Resource resource) {
        try (BufferedReader reader = resource.openAsReader()) {
            StringBuilder out = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                if (!out.isEmpty()) {
                    out.append('\n');
                }
                out.append(line);
            }
            return Optional.of(out.toString());
        } catch (IOException | RuntimeException e) {
            return Optional.empty();
        }
    }

    public enum EmptyReason {
        NONE,
        SERVER_DATA_UNAVAILABLE,
        NO_LOOT_TABLE_RESOURCES,
        NO_ENTITY_LOOT_TABLES,
        NO_INDEXED_DROP_EDGES
    }

    public record DeferredDropIndexingResult(
            int resourcesScanned,
            int entityTables,
            int itemRefs,
            int edgesAdded,
            EmptyReason emptyReason
    ) {
        public static DeferredDropIndexingResult empty() {
            return new DeferredDropIndexingResult(0, 0, 0, 0, EmptyReason.NONE);
        }

        public static DeferredDropIndexingResult unavailable(EmptyReason reason) {
            return new DeferredDropIndexingResult(0, 0, 0, 0, reason == null ? EmptyReason.NONE : reason);
        }

        public DeferredDropIndexingResult(int resourcesScanned, int entityTables, int itemRefs, int edgesAdded) {
            this(resourcesScanned, entityTables, itemRefs, edgesAdded, inferEmptyReason(
                    resourcesScanned,
                    entityTables,
                    itemRefs,
                    edgesAdded
            ));
        }

        public DeferredDropIndexingResult plus(DeferredDropIndexingResult other) {
            if (other == null) {
                return this;
            }
            return new DeferredDropIndexingResult(
                    resourcesScanned + other.resourcesScanned,
                    entityTables + other.entityTables,
                    itemRefs + other.itemRefs,
                    edgesAdded + other.edgesAdded,
                    combineReason(this, other)
            );
        }

        private static EmptyReason combineReason(DeferredDropIndexingResult left, DeferredDropIndexingResult right) {
            int resources = left.resourcesScanned + right.resourcesScanned;
            int entityTables = left.entityTables + right.entityTables;
            int itemRefs = left.itemRefs + right.itemRefs;
            int edges = left.edgesAdded + right.edgesAdded;
            EmptyReason inferred = inferEmptyReason(resources, entityTables, itemRefs, edges);
            if (inferred != EmptyReason.NONE) {
                return inferred;
            }
            if (left.emptyReason != EmptyReason.NONE) {
                return left.emptyReason;
            }
            return right.emptyReason;
        }

        private static EmptyReason inferEmptyReason(int resourcesScanned, int entityTables, int itemRefs, int edgesAdded) {
            if (resourcesScanned <= 0) {
                return EmptyReason.NO_LOOT_TABLE_RESOURCES;
            }
            if (entityTables <= 0) {
                return EmptyReason.NO_ENTITY_LOOT_TABLES;
            }
            if (edgesAdded <= 0 && itemRefs > 0) {
                return EmptyReason.NO_INDEXED_DROP_EDGES;
            }
            return EmptyReason.NONE;
        }
    }
}
