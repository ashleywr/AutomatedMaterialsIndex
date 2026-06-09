package com.sanhiruzu.ami.compat.optional;

import com.sanhiruzu.ami.AmiCore;
import com.sanhiruzu.ami.index.providers.IAmiIngredientPlugin;
import mezz.jei.api.ingredients.IIngredientHelper;
import mezz.jei.api.ingredients.IIngredientType;
import mezz.jei.api.ingredients.subtypes.UidContext;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.TooltipFlag;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * Optional integration plugin for JEI ingredient discovery.
 * Only invoked if JEI is available at runtime.
 * Does not introduce JEI dependency into core code.
 */
public class JeiIngredientPlugin implements IAmiIngredientPlugin {
    private final mezz.jei.api.runtime.IJeiRuntime runtime;

    public JeiIngredientPlugin(mezz.jei.api.runtime.IJeiRuntime runtime) {
        this.runtime = runtime;
    }

    @Override
    public String modId() {
        return "jei-integration";
    }

    @Override
    public void registerIngredients(IngredientRegistry registry) {
        if (runtime == null) return;

        try {
            var ingredientManager = runtime.getIngredientManager();
            var visibility = runtime.getJeiHelpers().getIngredientVisibility();
            Set<String> seenKeys = new java.util.LinkedHashSet<>();

            for (IIngredientType<?> type : ingredientManager.getRegisteredIngredientTypes()) {
                String typeUid = safeTypeUid(type);
                if (shouldSkipTypeUid(typeUid)) {
                    continue;
                }
                addJeiType(registry, ingredientManager, visibility, type, typeUid, seenKeys);
            }
        } catch (Exception e) {
            AmiCore.LOGGER.warn("JEI ingredient integration failed", e);
        }
    }

    private <V> void addJeiType(
            IngredientRegistry registry,
            mezz.jei.api.runtime.IIngredientManager ingredientManager,
            mezz.jei.api.runtime.IIngredientVisibility visibility,
            IIngredientType<V> type,
            String typeUid,
            Set<String> seenKeys
    ) {
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
            ResourceLocation nodeId = syntheticId(modId, resourceLocation, typeUid, uniqueUid);

            Map<String, String> meta = new LinkedHashMap<>();
            meta.put("modId", modId);
            meta.put("ingredientTypeUid", typeUid);
            meta.put("ingredientUid", String.valueOf(uniqueUid));

            String modName = com.sanhiruzu.ami.platform.Services.PLATFORM.getModName(modId).orElse(modId);
            addPlainSearchTokens(meta, modName);
            addPlainSearchTokens(meta, safeTypeLabel(typeUid));
            addPlainSearchTokens(meta, displayName);

            List<Component> fullTooltip = safeTooltip(renderer, ingredient);
            List<Component> bodyTooltip = trimTooltipBody(displayName, modName, fullTooltip);

            registry.addIngredient(nodeId, displayName, typeUid, meta);
        }
    }

    private String safeTypeUid(IIngredientType<?> type) {
        try {
            return type.getUid();
        } catch (RuntimeException | LinkageError e) {
            return "";
        }
    }

    private <V> Object safeUid(IIngredientHelper<V> helper, V ingredient) {
        try {
            return helper.getUniqueId(ingredient, UidContext.Ingredient);
        } catch (RuntimeException | LinkageError e) {
            return null;
        }
    }

    private <V> ResourceLocation safeResourceLocation(IIngredientHelper<V> helper, V ingredient) {
        try {
            return helper.getResourceLocation(ingredient);
        } catch (RuntimeException | LinkageError e) {
            return null;
        }
    }

    private <V> String safeDisplayName(IIngredientHelper<V> helper, V ingredient) {
        try {
            String displayName = helper.getDisplayName(ingredient);
            return displayName == null ? "" : displayName.trim();
        } catch (RuntimeException | LinkageError e) {
            return "";
        }
    }

    private <V> String safeDisplayModId(IIngredientHelper<V> helper, V ingredient, @Nullable ResourceLocation fallback) {
        try {
            String modId = helper.getDisplayModId(ingredient);
            if (modId != null && !modId.isBlank()) {
                return modId;
            }
        } catch (RuntimeException | LinkageError ignored) {
        }
        return fallback != null ? fallback.getNamespace() : "minecraft";
    }

    private String safeTypeLabel(String typeUid) {
        String raw = typeUid == null ? "" : typeUid.trim();
        if (raw.isBlank()) return "ingredient";
        int slash = Math.max(raw.lastIndexOf('/'), raw.lastIndexOf(':'));
        String tail = slash >= 0 ? raw.substring(slash + 1) : raw;
        return tail.replace('_', ' ').replace('-', ' ');
    }

    private <V> List<Component> safeTooltip(mezz.jei.api.ingredients.IIngredientRenderer<V> renderer, V ingredient) {
        try {
            List<Component> tooltip = renderer.getTooltip(ingredient, TooltipFlag.Default.NORMAL);
            return tooltip == null ? List.of() : List.copyOf(tooltip);
        } catch (RuntimeException | LinkageError e) {
            return List.of();
        }
    }

    private List<Component> trimTooltipBody(String displayName, String modName, List<Component> fullTooltip) {
        if (fullTooltip.isEmpty()) {
            return List.of();
        }
        List<Component> trimmed = new java.util.ArrayList<>(fullTooltip);
        if (!trimmed.isEmpty() && normalizedText(trimmed.get(0)).equals(normalizedText(displayName))) {
            trimmed.remove(0);
        }
        if (!trimmed.isEmpty() && normalizedText(trimmed.get(trimmed.size() - 1)).equals(normalizedText(modName))) {
            trimmed.remove(trimmed.size() - 1);
        }
        return List.copyOf(trimmed);
    }

    private String normalizedText(@Nullable Component component) {
        return component == null ? "" : normalizedText(component.getString());
    }

    private String normalizedText(@Nullable String text) {
        return text == null ? "" : text.trim().toLowerCase(Locale.ROOT);
    }

    private boolean shouldSkipTypeUid(String typeUid) {
        if (typeUid == null || typeUid.isBlank()) return true;
        String lower = typeUid.toLowerCase(Locale.ROOT);
        return lower.contains("item") || lower.contains("fluid");
    }

    private ResourceLocation syntheticId(String modId, ResourceLocation resourceLocation, String typeUid, Object uniqueUid) {
        String typePath = typeUid.toLowerCase(Locale.ROOT).replace(':', '/');
        String idPath = resourceLocation != null ? resourceLocation.getPath() : "unknown";
        return com.sanhiruzu.ami.platform.Services.PLATFORM.rl(
            modId,
            "ingredient/" + typePath + "/" + idPath + "/" + shortHash(String.valueOf(uniqueUid))
        );
    }

    private String shortHash(String input) {
        try {
            java.security.MessageDigest sha1 = java.security.MessageDigest.getInstance("SHA-1");
            byte[] digest = sha1.digest(input.getBytes(java.nio.charset.StandardCharsets.UTF_8));
            StringBuilder hex = new StringBuilder();
            for (int i = 0; i < 6 && i < digest.length; i++) {
                hex.append(String.format("%02x", digest[i]));
            }
            return hex.toString();
        } catch (RuntimeException | java.security.NoSuchAlgorithmException e) {
            return Integer.toHexString(input.hashCode());
        }
    }

    private void addPlainSearchTokens(Map<String, String> meta, String rawValue) {
        if (rawValue == null || rawValue.isBlank()) {
            return;
        }
        String normalized = java.text.Normalizer.normalize(
                        rawValue.replaceAll("([a-z])([A-Z])", "$1 $2"),
                        java.text.Normalizer.Form.NFD)
                .replaceAll("\\p{M}+", "")
                .toLowerCase(Locale.ROOT);
        for (String part : normalized.split("[^a-z0-9]+")) {
            if (part.length() >= 2) {
                meta.merge("plainSearchTokens", part, this::mergeTokens);
            }
        }
    }

    private String mergeTokens(String existing, String added) {
        List<String> parts = List.of(existing.split("\\s+"));
        return parts.contains(added) ? existing : existing + " " + added;
    }
}
