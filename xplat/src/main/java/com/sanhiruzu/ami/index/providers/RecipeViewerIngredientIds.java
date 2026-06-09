package com.sanhiruzu.ami.index.providers;

import com.sanhiruzu.ami.AmiCore;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.Nullable;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Locale;

final class RecipeViewerIngredientIds {
    private RecipeViewerIngredientIds() {
    }

    static boolean shouldSkipTypeUid(String typeUid) {
        return typeUid == null
                || typeUid.isBlank()
                || "item_stack".equals(typeUid)
                || "fluid_stack".equals(typeUid);
    }

    static ResourceLocation syntheticId(String modId, @Nullable ResourceLocation resourceLocation, String typeUid, Object uniqueUid) {
        if (resourceLocation != null && uniqueUidMatchesResource(resourceLocation, String.valueOf(uniqueUid))) {
            return resourceLocation;
        }
        String namespace = normalizeNamespace(resourceLocation != null ? resourceLocation.getNamespace() : modId);
        String basePath = resourceLocation != null ? resourceLocation.getPath() : normalizePathSegment(typeUid);
        String suffix = shortHash(typeUid + "|" + uniqueUid);
        return ResourceLocation.fromNamespaceAndPath(namespace, basePath + "/rv/" + suffix);
    }

    private static boolean uniqueUidMatchesResource(ResourceLocation resourceLocation, String uniqueUid) {
        if (uniqueUid == null || uniqueUid.isBlank()) {
            return false;
        }
        String normalizedUid = uniqueUid.trim().toLowerCase(Locale.ROOT);
        String asId = resourceLocation.toString().toLowerCase(Locale.ROOT);
        return normalizedUid.equals(asId)
                || normalizedUid.equals(resourceLocation.getNamespace().toLowerCase(Locale.ROOT) + "/" + resourceLocation.getPath().toLowerCase(Locale.ROOT));
    }

    private static String normalizeNamespace(@Nullable String rawNamespace) {
        String normalized = rawNamespace == null ? "" : rawNamespace.trim().toLowerCase(Locale.ROOT);
        normalized = normalized.replaceAll("[^a-z0-9_.-]", "_");
        if (normalized.isBlank()) {
            return AmiCore.MODID;
        }
        return normalized;
    }

    private static String normalizePathSegment(String rawPath) {
        String normalized = rawPath == null ? "" : rawPath.trim().toLowerCase(Locale.ROOT);
        normalized = normalized
                .replace(":", "/")
                .replaceAll("[^a-z0-9/._-]", "_")
                .replaceAll("_+", "_");
        if (normalized.isBlank()) {
            return "ingredient";
        }
        return normalized;
    }

    private static String shortHash(String value) {
        try {
            MessageDigest sha256 = MessageDigest.getInstance("SHA-256");
            byte[] digest = sha256.digest(value.getBytes(StandardCharsets.UTF_8));
            StringBuilder out = new StringBuilder(12);
            for (int i = 0; i < 6; i++) {
                out.append(String.format("%02x", digest[i]));
            }
            return out.toString();
        } catch (Exception e) {
            return Integer.toHexString(value.hashCode());
        }
    }
}
