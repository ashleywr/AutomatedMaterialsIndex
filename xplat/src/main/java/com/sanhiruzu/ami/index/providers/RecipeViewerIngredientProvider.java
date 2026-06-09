package com.sanhiruzu.ami.index.providers;

import com.sanhiruzu.ami.AmiCore;
import com.sanhiruzu.ami.client.icon.RecipeViewerIngredientRenderer;
import com.sanhiruzu.ami.compat.JeiRuntimeAccessor;
import com.sanhiruzu.ami.index.AmiOntology;
import com.sanhiruzu.ami.index.GlobalIndex;
import com.sanhiruzu.ami.index.IAmiDataProvider;
import com.sanhiruzu.ami.index.NodeType;
import com.sanhiruzu.ami.index.SearchNode;
import com.sanhiruzu.ami.index.SearchNodeKeys;
import com.sanhiruzu.ami.platform.Services;
import mezz.jei.api.ingredients.IIngredientHelper;
import mezz.jei.api.ingredients.IIngredientType;
import mezz.jei.api.ingredients.subtypes.UidContext;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

import java.text.Normalizer;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

public class RecipeViewerIngredientProvider implements IAmiDataProvider {
    public static final String TYPE_UID_KEY = "recipeViewerTypeUid";
    public static final String INGREDIENT_UID_KEY = "recipeViewerIngredientUid";
    public static final String DISPLAY_MOD_ID_KEY = "recipeViewerDisplayModId";

    @Override
    public void populate(GlobalIndex index, @Nullable Level level) {
        long start = System.currentTimeMillis();
        int nativeCount = indexNativeIngredients(index);
        int jeiCount = indexJeiIngredients(index);
        long elapsed = System.currentTimeMillis() - start;
        AmiCore.LOGGER.info("AMI indexing: RecipeViewerIngredientProvider indexed {} native + {} JEI ingredients in {}ms",
                nativeCount, jeiCount, elapsed);
    }

    private static int indexNativeIngredients(GlobalIndex index) {
        RecipeViewerIngredientRenderer.clearPersistent();
        int[] count = {0};
        IAmiIngredientPlugin.IngredientRegistry registry = new IngredientRegistry(index, count);
        IngredientPluginRegistry.registerAllIngredients(registry);
        return count[0];
    }

    private static int indexJeiIngredients(GlobalIndex index) {
        RecipeViewerIngredientRenderer.clearPersistent();
        int[] count = {0};
        JeiRuntimeAccessor.withRuntime(runtime -> {
            indexJeiIngredientsInternal(index, runtime, count);
        });
        return count[0];
    }

    private static void indexJeiIngredientsInternal(GlobalIndex index, mezz.jei.api.runtime.IJeiRuntime runtime, int[] count) {
        var ingredientManager = runtime.getIngredientManager();
        var visibility = runtime.getJeiHelpers().getIngredientVisibility();
        Set<String> seenKeys = new LinkedHashSet<>();
        for (mezz.jei.api.ingredients.IIngredientType<?> type : ingredientManager.getRegisteredIngredientTypes()) {
            String typeUid = safeTypeUid(type);
            if (RecipeViewerIngredientIds.shouldSkipTypeUid(typeUid)) {
                continue;
            }
            count[0] += addJeiType(index, ingredientManager, visibility, type, typeUid, seenKeys);
        }
    }

    public static void rebuildRuntimeHandles(GlobalIndex index) {
        RecipeViewerIngredientRenderer.clearPersistent();
        JeiRuntimeAccessor.withRuntime(runtime -> {
            var ingredientManager = runtime.getIngredientManager();
            var visibility = runtime.getJeiHelpers().getIngredientVisibility();
            Set<String> seenKeys = new LinkedHashSet<>();
            for (mezz.jei.api.ingredients.IIngredientType<?> type : ingredientManager.getRegisteredIngredientTypes()) {
                String typeUid = safeTypeUid(type);
                if (RecipeViewerIngredientIds.shouldSkipTypeUid(typeUid)) {
                    continue;
                }
                addJeiType(index, ingredientManager, visibility, type, typeUid, seenKeys);
            }
        });
    }

    private static <V> int addJeiType(
            GlobalIndex index,
            mezz.jei.api.runtime.IIngredientManager ingredientManager,
            mezz.jei.api.runtime.IIngredientVisibility visibility,
            mezz.jei.api.ingredients.IIngredientType<V> type,
            String typeUid,
            Set<String> seenKeys
    ) {
        int added = 0;
        Collection<V> ingredients = ingredientManager.getAllIngredients(type);
        IIngredientHelper<V> helper = ingredientManager.getIngredientHelper(type);
        var renderer = ingredientManager.getIngredientRenderer(type);
        Map<String, String> modNameCache = new HashMap<>();
        for (V ingredient : ingredients) {
            if (ingredient == null || !visibility.isIngredientVisible(type, ingredient)) {
                continue;
            }
            Object uniqueUid = safeUid(helper, ingredient);
            if (uniqueUid == null) {
                continue;
            }
            String seenKey = typeUid + "|" + uniqueUid;
            if (!seenKeys.add(seenKey)) {
                continue;
            }

            ResourceLocation resourceLocation = safeResourceLocation(helper, ingredient);
            String displayName = safeDisplayName(helper, ingredient);
            if (displayName.isBlank()) {
                displayName = resourceLocation != null ? resourceLocation.toString() : safeTypeLabel(typeUid);
            }
            String modId = safeDisplayModId(helper, ingredient, resourceLocation);
            ResourceLocation nodeId = RecipeViewerIngredientIds.syntheticId(modId, resourceLocation, typeUid, uniqueUid);

            Map<String, String> meta = new LinkedHashMap<>();
            meta.put(SearchNodeKeys.MOD_ID, modId);
            meta.put(TYPE_UID_KEY, typeUid);
            meta.put(INGREDIENT_UID_KEY, String.valueOf(uniqueUid));
            meta.put(DISPLAY_MOD_ID_KEY, modId);
            maybeApplyKnownOntology(meta, modId, typeUid);

            String modName = modNameCache.computeIfAbsent(modId, ns -> Services.PLATFORM.getModName(ns).orElse(ns));
            addPlainSearchTokens(meta, modName);
            addPlainSearchTokens(meta, safeTypeLabel(typeUid));
            addPlainSearchTokens(meta, displayName);

            List<Component> fullTooltip = safeTooltip(renderer, ingredient);
            List<Component> bodyTooltip = trimTooltipBody(displayName, modName, fullTooltip);
            index.addNode(new SearchNode(nodeId, NodeType.INGREDIENT, displayName, 0xFFFFFFFF, 0, meta));
            RecipeViewerIngredientRenderer.register(
                    nodeId,
                    RecipeViewerIngredientRenderer.RenderHandle.jei(renderer, ingredient, bodyTooltip)
            );
            added++;
        }
        return added;
    }

    private static String safeTypeUid(IIngredientType<?> type) {
        try {
            return type.getUid();
        } catch (RuntimeException | LinkageError e) {
            return "";
        }
    }

    private static <V> Object safeUid(IIngredientHelper<V> helper, V ingredient) {
        try {
            return helper.getUniqueId(ingredient, UidContext.Ingredient);
        } catch (RuntimeException | LinkageError e) {
            return null;
        }
    }

    private static <V> ResourceLocation safeResourceLocation(IIngredientHelper<V> helper, V ingredient) {
        try {
            return helper.getResourceLocation(ingredient);
        } catch (RuntimeException | LinkageError e) {
            return null;
        }
    }

    private static <V> String safeDisplayName(IIngredientHelper<V> helper, V ingredient) {
        try {
            String displayName = helper.getDisplayName(ingredient);
            return displayName == null ? "" : displayName.trim();
        } catch (RuntimeException | LinkageError e) {
            return "";
        }
    }

    private static <V> String safeDisplayModId(IIngredientHelper<V> helper, V ingredient, @Nullable ResourceLocation fallback) {
        try {
            String modId = helper.getDisplayModId(ingredient);
            if (modId != null && !modId.isBlank()) {
                return modId;
            }
        } catch (RuntimeException | LinkageError ignored) {
        }
        return fallback != null ? fallback.getNamespace() : AmiCore.MODID;
    }

    private static String safeTypeLabel(String typeUid) {
        String raw = typeUid == null ? "" : typeUid.trim();
        if (raw.isBlank()) return "ingredient";
        int slash = Math.max(raw.lastIndexOf('/'), raw.lastIndexOf(':'));
        String tail = slash >= 0 ? raw.substring(slash + 1) : raw;
        return tail.replace('_', ' ').replace('-', ' ');
    }

    private static <V> List<Component> safeTooltip(mezz.jei.api.ingredients.IIngredientRenderer<V> renderer, V ingredient) {
        try {
            List<Component> tooltip = renderer.getTooltip(ingredient, TooltipFlag.Default.NORMAL);
            return tooltip == null ? List.of() : List.copyOf(tooltip);
        } catch (RuntimeException | LinkageError e) {
            return List.of();
        }
    }

    private static List<Component> trimTooltipBody(String displayName, String modName, List<Component> fullTooltip) {
        if (fullTooltip.isEmpty()) {
            return List.of();
        }
        List<Component> trimmed = new ArrayList<>(fullTooltip);
        if (!trimmed.isEmpty() && normalizedText(trimmed.get(0)).equals(normalizedText(displayName))) {
            trimmed.remove(0);
        }
        if (!trimmed.isEmpty() && normalizedText(trimmed.get(trimmed.size() - 1)).equals(normalizedText(modName))) {
            trimmed.remove(trimmed.size() - 1);
        }
        return List.copyOf(trimmed);
    }

    private static String normalizedText(@Nullable Component component) {
        return component == null ? "" : normalizedText(component.getString());
    }

    private static String normalizedText(@Nullable String text) {
        return text == null ? "" : text.trim().toLowerCase(Locale.ROOT);
    }

    private static void maybeApplyKnownOntology(Map<String, String> meta, String modId, String typeUid) {
        if (!AmiOntology.isDefinedCategoryId(modId)) {
            return;
        }
        meta.put(SearchNodeKeys.ONTOLOGY_CATEGORY, modId);
        if (isChemicalType(typeUid)) {
            meta.put(SearchNodeKeys.ONTOLOGY_SUBCATEGORY, "chemicals");
        }
    }

    private static boolean isChemicalType(String typeUid) {
        if (typeUid == null) return false;
        String lower = typeUid.toLowerCase(java.util.Locale.ROOT);
        return lower.contains("chemical") || lower.contains("gas") || lower.contains("pigment")
                || lower.contains("slurry") || lower.contains("infuse");
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
                meta.merge(SearchNodeKeys.PLAIN_SEARCH_TOKENS, part, RecipeViewerIngredientProvider::mergeTokens);
            }
        }
    }

    private static String mergeTokens(String existing, String added) {
        List<String> parts = List.of(existing.split("\\s+"));
        return parts.contains(added) ? existing : existing + " " + added;
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
            maybeApplyKnownOntology(meta, id.getNamespace(), typeUid);

            String modName = Services.PLATFORM.getModName(id.getNamespace()).orElse(id.getNamespace());
            addPlainSearchTokens(meta, modName);
            addPlainSearchTokens(meta, displayName);

            index.addNode(new SearchNode(id, NodeType.INGREDIENT, displayName, 0xFFFFFFFF, 0, meta));
            count[0]++;
        }
    }

}
