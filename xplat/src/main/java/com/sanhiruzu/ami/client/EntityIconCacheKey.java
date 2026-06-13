package com.sanhiruzu.ami.client;

import com.sanhiruzu.ami.platform.Services;
import net.minecraft.client.Minecraft;

import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

final class EntityIconCacheKey {
    static final String CACHE_VERSION = "entity-icons-v3";

    private EntityIconCacheKey() {
    }

    static String currentFingerprint() {
        List<String> parts = new ArrayList<>();
        parts.add(CACHE_VERSION);
        for (String entry : Services.PLATFORM.getLoadedModFingerprintEntries()) {
            parts.add("mod=" + entry);
        }
        for (String pack : selectedResourcePacks()) {
            parts.add("pack=" + pack);
        }
        return fingerprint(parts);
    }

    static String fingerprint(List<String> parts) {
        List<String> sorted = new ArrayList<>(parts);
        sorted.sort(Comparator.naturalOrder());
        return sha256(String.join("\n", sorted)).substring(0, 16);
    }

    static String iconFileName(String id) {
        return sha256(id) + ".png";
    }

    static boolean isIconFileName(String fileName) {
        if (fileName.length() != 68 || !fileName.endsWith(".png")) {
            return false;
        }
        for (int i = 0; i < 64; i++) {
            char c = fileName.charAt(i);
            if (!((c >= '0' && c <= '9') || (c >= 'a' && c <= 'f'))) {
                return false;
            }
        }
        return true;
    }

    private static List<String> selectedResourcePacks() {
        try {
            Object repository = Minecraft.getInstance().getResourcePackRepository();
            Method method = repository.getClass().getMethod("getSelectedIds");
            Object selected = method.invoke(repository);
            if (selected instanceof Collection<?> collection) {
                return collection.stream().map(String::valueOf).sorted().toList();
            }
        } catch (ReflectiveOperationException | RuntimeException ignored) {
        }
        return List.of();
    }

    private static String sha256(String input) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(input.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder(hash.length * 2);
            for (byte b : hash) {
                sb.append(String.format(Locale.ROOT, "%02x", b));
            }
            return sb.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 unavailable", e);
        }
    }
}
