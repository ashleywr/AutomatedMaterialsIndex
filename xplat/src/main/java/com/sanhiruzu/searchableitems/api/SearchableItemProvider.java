package com.sanhiruzu.searchableitems.api;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Map;

/**
 * Service-provider entry point for mods that want compatible viewers to index
 * mod-owned item metadata or representative generated stacks.
 * <p>
 * Implementations can be discovered through:
 * {@code META-INF/services/com.sanhiruzu.searchableitems.api.SearchableItemProvider}
 */
public interface SearchableItemProvider {
    String id();

    /**
     * Return representative stacks for generated or modular content that should
     * be searchable without exhaustively enumerating every possible variant.
     */
    default List<ItemStack> getRepresentativeItems(@Nullable Level level) {
        return List.of();
    }

    /**
     * Add structured metadata for a concrete indexed item stack.
     */
    default void enrichItemMetadata(ResourceLocation id,
                                    ItemStack stack,
                                    @Nullable Level level,
                                    Map<String, String> metadata) {
    }
}
