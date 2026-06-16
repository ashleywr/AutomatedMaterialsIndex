package com.sanhiruzu.ami.client;

import net.minecraft.resources.Identifier;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class EntityIconPersistentStoreTest {
    @Test
    void manifestParserKeepsOnlyValidIconRows() {
        String zombieFile = EntityIconCacheKey.iconFileName("minecraft:zombie");
        String skeletonFile = EntityIconCacheKey.iconFileName("minecraft:skeleton");

        Map<Identifier, String> manifest = EntityIconPersistentStore.parseManifestLines(List.of(
                "minecraft:zombie\t" + zombieFile,
                "minecraft:skeleton\t" + skeletonFile,
                "minecraft:creeper\t../creeper.png",
                "minecraft:enderman\tminecraft:enderman.png",
                "bad-row-without-tab",
                "",
                "minecraft:pig\t" + zombieFile + "\textra"));

        assertEquals(2, manifest.size());
        assertEquals(zombieFile, manifest.get(Identifier.parse("minecraft:zombie")));
        assertEquals(skeletonFile, manifest.get(Identifier.parse("minecraft:skeleton")));
        assertFalse(manifest.containsKey(Identifier.parse("minecraft:creeper")));
        assertFalse(manifest.containsKey(Identifier.parse("minecraft:enderman")));
        assertFalse(manifest.containsKey(Identifier.parse("minecraft:pig")));
    }
}
