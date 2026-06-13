package com.sanhiruzu.ami.client;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class EntityIconCacheKeyTest {
    @Test
    void fingerprintIsStableRegardlessOfInputOrder() {
        String first = EntityIconCacheKey.fingerprint(List.of(
                EntityIconCacheKey.CACHE_VERSION,
                "mod=minecraft@1.21.1",
                "pack=vanilla"));
        String second = EntityIconCacheKey.fingerprint(List.of(
                "pack=vanilla",
                "mod=minecraft@1.21.1",
                EntityIconCacheKey.CACHE_VERSION));

        assertEquals(first, second);
        assertEquals(16, first.length());
    }

    @Test
    void fingerprintChangesWhenCacheVersionChanges() {
        String first = EntityIconCacheKey.fingerprint(List.of("entity-icons-v3", "mod=minecraft@1.21.1"));
        String second = EntityIconCacheKey.fingerprint(List.of("entity-icons-v4", "mod=minecraft@1.21.1"));

        assertNotEquals(first, second);
    }

    @Test
    void iconFileNamesAreHashedPngNamesOnly() {
        String fileName = EntityIconCacheKey.iconFileName("minecraft:zombie");

        assertTrue(EntityIconCacheKey.isIconFileName(fileName));
        assertFalse(EntityIconCacheKey.isIconFileName("../zombie.png"));
        assertFalse(EntityIconCacheKey.isIconFileName("minecraft:zombie.png"));
        assertFalse(EntityIconCacheKey.isIconFileName(fileName.toUpperCase()));
    }
}
