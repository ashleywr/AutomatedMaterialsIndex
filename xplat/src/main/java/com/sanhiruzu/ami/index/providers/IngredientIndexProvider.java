package com.sanhiruzu.ami.index.providers;

import com.sanhiruzu.ami.AmiCore;
import com.sanhiruzu.ami.client.icon.RecipeViewerIngredientRenderer;
import com.sanhiruzu.ami.index.GlobalIndex;
import com.sanhiruzu.ami.index.IAmiDataProvider;
import com.sanhiruzu.ami.index.NodeType;
import com.sanhiruzu.ami.index.SearchNode;
import com.sanhiruzu.ami.index.SearchNodeKeys;
import com.sanhiruzu.ami.platform.Services;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

import java.text.Normalizer;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * Core ingredient discovery via native plugins.
 * Recipe viewers (JEI, EMI, etc.) can optionally enhance this via IngredientPluginRegistry,
 * but are not required and not referenced in core code.
 */
public class IngredientIndexProvider implements IAmiDataProvider {
    public static final String TYPE_UID_KEY = "ingredientTypeUid";
    public static final String INGREDIENT_UID_KEY = "ingredientUid";
    public static final String DISPLAY_MOD_ID_KEY = "displayModId";

    @Override
    public void populate(GlobalIndex index, @Nullable Level level) {
        long start = System.currentTimeMillis();
        RecipeViewerIngredientRenderer.clearPersistent();
        int count = indexNativeIngredients(index);
        long elapsed = System.currentTimeMillis() - start;
        AmiCore.LOGGER.info("AMI indexing: IngredientIndexProvider indexed {} ingredients in {}ms", count, elapsed);
    }

    private static int indexNativeIngredients(GlobalIndex index) {
        int[] count = {0};
        IAmiIngredientPlugin.IngredientRegistry registry = new IngredientRegistry(index, count);
        IngredientPluginRegistry.registerAllIngredients(registry);
        return count[0];
    }

    public static void rebuildRuntimeHandles(GlobalIndex index) {
        RecipeViewerIngredientRenderer.clearPersistent();
        indexNativeIngredients(index);
    }

    private static class IngredientRegistry implements IAmiIngredientPlugin.IngredientRegistry {
        private final GlobalIndex index;
        private final int[] count;
        private final Set<String> seenKeys;

        IngredientRegistry(GlobalIndex index, int[] count) {
            this.index = index;
            this.count = count;
            this.seenKeys = new LinkedHashSet<>();
        }

        @Override
        public void addIngredient(ResourceLocation id, String displayName, String typeUid, Map<String, String> metadata) {
            if (id == null || displayName == null || displayName.isBlank()) {
                return;
            }
            String seenKey = typeUid + "|" + id;
            if (!seenKeys.add(seenKey)) {
                return;
            }

            Map<String, String> meta = new LinkedHashMap<>(metadata);
            meta.putIfAbsent(SearchNodeKeys.MOD_ID, id.getNamespace());
            meta.put(TYPE_UID_KEY, typeUid);
            meta.putIfAbsent(DISPLAY_MOD_ID_KEY, id.getNamespace());

            String modName = Services.PLATFORM.getModName(id.getNamespace()).orElse(id.getNamespace());
            addPlainSearchTokens(meta, modName);
            addPlainSearchTokens(meta, displayName);

            index.addNode(new SearchNode(id, NodeType.INGREDIENT, displayName, 0xFFFFFFFF, 0, meta));
            count[0]++;
        }
    }

    private static void addPlainSearchTokens(Map<String, String> meta, String rawValue) {
        if (rawValue == null || rawValue.isBlank()) {
            return;
        }
        String normalized = Normalizer.normalize(
                        rawValue.replaceAll("([a-z])([A-Z])", "$1 $2"),
                        Normalizer.Form.NFD)
                .replaceAll("\\p{M}+", "")
                .toLowerCase(Locale.ROOT);
        for (String part : normalized.split("[^a-z0-9]+")) {
            if (part.length() >= 2) {
                meta.merge(SearchNodeKeys.PLAIN_SEARCH_TOKENS, part, IngredientIndexProvider::mergeTokens);
            }
        }
    }

    private static String mergeTokens(String existing, String added) {
        List<String> parts = List.of(existing.split("\\s+"));
        return parts.contains(added) ? existing : existing + " " + added;
    }
}
