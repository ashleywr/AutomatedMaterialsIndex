package com.sanhiruzu.ami.index;

import com.sanhiruzu.ami.AmiCore;
import com.sanhiruzu.ami.client.icon.ItemIconRenderer;
import com.sanhiruzu.ami.compat.CobblemonSpeciesProvider;
import com.sanhiruzu.ami.index.providers.*;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * Registry of all data providers.
 * Orchestrates the population of GlobalIndex.
 */
public final class ProviderRegistry {
    /**
     * All providers run on first inventory open, except StructureProvider (deferred).
     */
    private static final List<IAmiDataProvider> PROVIDERS = List.of(
            new RecipeProvider(),
            new ItemProvider(),
            new FluidProvider(),
            new IngredientIndexProvider(),
            new BiomeProvider(),
            new EntityProvider(),
            new CobblemonSpeciesProvider(),
            new DimensionProvider(),
            new LootTableProvider(),
            new SpawnProvider()
    );

    private ProviderRegistry() {
    }

    /**
     * Index all data types except STRUCTURE and DIMENSION (which are deferred).
     */
    public static void indexAll(Level level) {
        AmiCore.LOGGER.debug("Starting GlobalIndex population...");
        long start = System.currentTimeMillis();
        GlobalIndex index = GlobalIndex.getInstance();
        AmiIndexerService progress = AmiIndexerService.getInstance();
        index.clear();
        ItemIconRenderer.clearPersistent();
        com.sanhiruzu.ami.client.icon.RecipeViewerIngredientRenderer.clearPersistent();

        // Mark deferred types as loading
        index.setLoading(NodeType.STRUCTURE, true);
        index.setLoading(NodeType.DIMENSION, true);

        for (int i = 0; i < PROVIDERS.size(); i++) {
            IAmiDataProvider provider = PROVIDERS.get(i);
            long providerStart = System.currentTimeMillis();
            progress.beginProgress("Indexing " + providerName(provider), (i + 1) + "/" + PROVIDERS.size(), 0);
            try {
                provider.populate(index, level);
            } catch (Exception e) {
                AmiCore.LOGGER.error("Provider {} failed", provider.getClass().getSimpleName(), e);
            } finally {
                AmiCore.LOGGER.info("AMI indexing: {} provider finished in {}ms",
                        providerName(provider), System.currentTimeMillis() - providerStart);
            }
        }

        index.setIndexBuildTime(System.currentTimeMillis() - start);
        AmiCore.LOGGER.info("AMI indexing: GlobalIndex populated in {}ms", index.getIndexBuildTimeMs());
    }

    /**
     * Re-registers ItemStack instances for all subtype nodes after a cache load.
     * ItemProvider.populate() is skipped on cache hits, so persistentStacks is otherwise
     * empty — causing synthetic node IDs (potions, enchanted books, etc.) to resolve to
     * ItemStack.EMPTY and render as fallback icons.
     */
    public static void rehydrateSubtypeStacks(@Nullable Level level) {
        long start = System.currentTimeMillis();
        ItemIconRenderer.clearPersistent();
        RegistryAccess registryAccess = level != null ? level.registryAccess() : null;
        AmiIndexerService progress = AmiIndexerService.getInstance();
        progress.beginProgress("Reading creative tabs");
        long creativeStart = System.currentTimeMillis();
        var creativeStackMap = com.sanhiruzu.ami.index.ItemFilter.buildCreativeStackMap(level);
        long creativeMs = System.currentTimeMillis() - creativeStart;
        int total = BuiltInRegistries.ITEM.size();
        progress.beginProgress("Restoring cached item icons", "", total);
        int current = 0;
        int registered = 0;
        for (Item item : orderedItemsForIndexing()) {
            current++;
            if ((current & 31) == 0 || current == total) {
                ResourceLocation itemId = BuiltInRegistries.ITEM.getKey(item);
                progress.updateProgress(current);
                if (itemId != null) {
                    progress.updateProgressDetail(itemId.toString());
                }
            }
            ResourceLocation id = BuiltInRegistries.ITEM.getKey(item);
            if (id == null) continue;
            ItemFilter.firstCreativeStack(item, creativeStackMap).ifPresent(stack -> ItemIconRenderer.registerStack(id, stack));
            List<SubtypeExpander.SubtypeEntry> entries = SubtypeExpander.expand(id, registryAccess);
            if ((entries.isEmpty() || SubtypeExpander.shouldPreferCreativeStackParity(id))
                    && hasMultipleCreativeStacks(creativeStackMap.get(item))) {
                entries = CreativeStackVariantExpander.expand(id, creativeStackMap.get(item), level);
            }
            for (SubtypeExpander.SubtypeEntry entry : entries) {
                ItemIconRenderer.registerStack(entry.id(), entry.stack());
                registered++;
            }
        }
        for (Fluid fluid : BuiltInRegistries.FLUID) {
            if (fluid == Fluids.EMPTY || !fluid.isSource(fluid.defaultFluidState())) continue;
            ResourceLocation fluidId = BuiltInRegistries.FLUID.getKey(fluid);
            if (fluidId != null) FluidProvider.registerBucketIcon(fluidId, fluid);
        }
        IngredientIndexProvider.rebuildRuntimeHandles(GlobalIndex.getInstance());
        AmiCore.LOGGER.info("AMI indexing: restored cached item icons in {}ms (creativeTabs={}ms, subtypeStacks={})",
                System.currentTimeMillis() - start, creativeMs, registered);
    }

    /**
     * Deferred retry for STRUCTURE and DIMENSION data.
     * Matches WorldAtlasIndexer.indexStructuresFromConnection() contract.
     */
    public static void indexStructuresDeferred(Level level) {
        try {
            AmiIndexerService.getInstance().beginProgress("Indexing structures");
            new StructureProvider().populate(GlobalIndex.getInstance(), level);
        } catch (Exception e) {
            AmiCore.LOGGER.error("Deferred StructureProvider failed", e);
        }
        try {
            AmiIndexerService.getInstance().beginProgress("Indexing dimensions");
            new DimensionProvider().populate(GlobalIndex.getInstance(), level);
        } catch (Exception e) {
            AmiCore.LOGGER.error("Deferred DimensionProvider failed", e);
        }
    }

    public static int indexDeferredItems(Level level) {
        if (!IndexingHotItemPolicy.hasDeferredIndexNamespaces()) {
            return 0;
        }
        GlobalIndex index = GlobalIndex.getInstance();
        int before = index.getNodes(NodeType.ITEM).size();
        long started = System.currentTimeMillis();
        AmiIndexerService.getInstance().beginProgress(
                "Indexing deferred items",
                IndexingHotItemPolicy.deferredIndexNamespacesForLog(),
                BuiltInRegistries.ITEM.size()
        );
        new ItemProvider().populateDeferredNamespaces(index, level);
        int added = Math.max(0, index.getNodes(NodeType.ITEM).size() - before);
        AmiCore.LOGGER.info("AMI indexing: Deferred item provider finished in {}ms; added {} nodes from namespaces={}",
                System.currentTimeMillis() - started,
                added,
                IndexingHotItemPolicy.deferredIndexNamespacesForLog());
        return added;
    }

    private static String providerName(IAmiDataProvider provider) {
        String name = provider.getClass().getSimpleName();
        return name.endsWith("Provider") ? name.substring(0, name.length() - "Provider".length()) : name;
    }

    private static boolean hasMultipleCreativeStacks(@Nullable List<ItemFilter.CreativeStackInfo> stacks) {
        return stacks != null && stacks.size() > 1;
    }

    private static List<Item> orderedItemsForIndexing() {
        List<Item> regular = new java.util.ArrayList<>();
        List<Item> deferred = new java.util.ArrayList<>();
        for (Item item : BuiltInRegistries.ITEM) {
            ResourceLocation id = BuiltInRegistries.ITEM.getKey(item);
            if (IndexingHotItemPolicy.shouldDeferUntilTail(id)) {
                deferred.add(item);
            } else {
                regular.add(item);
            }
        }
        regular.addAll(deferred);
        return regular;
    }
}
