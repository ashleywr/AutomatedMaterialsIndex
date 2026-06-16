package com.sanhiruzu.ami.index.providers;

import com.sanhiruzu.ami.AmiCore;
import net.minecraft.resources.Identifier;
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

    static Identifier syntheticId(String modId, @Nullable Identifier Identifier, String typeUid, Object uniqueUid) {
        if (Identifier != null && uniqueUidMatchesResource(Identifier, String.valueOf(uniqueUid))) {
            return Identifier;
        }
        String namespace = normalizeNamespace(Identifier != null ? Identifier.getNamespace() : modId);
        String basePath = Identifier != null ? Identifier.getPath() : normalizePathSegment(typeUid);
        String suffix = shortHash(typeUid + "|" + uniqueUid);
        return Identifier.fromNamespaceAndPath(namespace, basePath + "/rv/" + suffix);
    }

    private static boolean uniqueUidMatchesResource(Identifier Identifier, String uniqueUid) {
        if (uniqueUid == null || uniqueUid.isBlank()) {
            return false;
        }
        String normalizedUid = uniqueUid.trim().toLowerCase(Locale.ROOT);
        String asId = Identifier.toString().toLowerCase(Locale.ROOT);
        return normalizedUid.equals(asId)
                || normalizedUid.equals(Identifier.getNamespace().toLowerCase(Locale.ROOT) + "/" + Identifier.getPath().toLowerCase(Locale.ROOT));
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
