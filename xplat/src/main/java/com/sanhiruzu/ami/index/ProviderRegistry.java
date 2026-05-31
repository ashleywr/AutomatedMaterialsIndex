package com.sanhiruzu.ami.index;

import com.sanhiruzu.ami.AmiCore;
import com.sanhiruzu.ami.client.icon.ItemIconRenderer;
import com.sanhiruzu.ami.compat.CobblemonSpeciesProvider;
import com.sanhiruzu.ami.index.providers.CreativeStackVariantExpander;
import com.sanhiruzu.ami.index.providers.*;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.Level;
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
        index.clear();
        ItemIconRenderer.clearPersistent();

        // Mark deferred types as loading
        index.setLoading(NodeType.STRUCTURE, true);
        index.setLoading(NodeType.DIMENSION, true);

        for (IAmiDataProvider provider : PROVIDERS) {
            try {
                provider.populate(index, level);
            } catch (Exception e) {
                AmiCore.LOGGER.error("Provider {} failed", provider.getClass().getSimpleName(), e);
            }
        }

        index.setIndexBuildTime(System.currentTimeMillis() - start);
        AmiCore.LOGGER.debug("GlobalIndex populated in {}ms", index.getIndexBuildTimeMs());
    }

    /**
     * Re-registers ItemStack instances for all subtype nodes after a cache load.
     * ItemProvider.populate() is skipped on cache hits, so persistentStacks is otherwise
     * empty — causing synthetic node IDs (potions, enchanted books, etc.) to resolve to
     * ItemStack.EMPTY and render as fallback icons.
     */
    public static void rehydrateSubtypeStacks(@Nullable Level level) {
        ItemIconRenderer.clearPersistent();
        RegistryAccess registryAccess = level != null ? level.registryAccess() : null;
        var creativeStackMap = com.sanhiruzu.ami.index.ItemFilter.buildCreativeStackMap(level);
        for (Item item : BuiltInRegistries.ITEM) {
            ResourceLocation id = BuiltInRegistries.ITEM.getKey(item);
            if (id == null) continue;
            List<SubtypeExpander.SubtypeEntry> entries = SubtypeExpander.expand(id, registryAccess);
            if (entries.isEmpty()) {
                entries = CreativeStackVariantExpander.expand(id, creativeStackMap.get(item), level);
            }
            for (SubtypeExpander.SubtypeEntry entry : entries) {
                ItemIconRenderer.registerStack(entry.id(), entry.stack());
            }
        }
    }

    /**
     * Deferred retry for STRUCTURE and DIMENSION data.
     * Matches WorldAtlasIndexer.indexStructuresFromConnection() contract.
     */
    public static void indexStructuresDeferred(Level level) {
        try {
            new StructureProvider().populate(GlobalIndex.getInstance(), level);
        } catch (Exception e) {
            AmiCore.LOGGER.error("Deferred StructureProvider failed", e);
        }
        try {
            new DimensionProvider().populate(GlobalIndex.getInstance(), level);
        } catch (Exception e) {
            AmiCore.LOGGER.error("Deferred DimensionProvider failed", e);
        }
    }
}
