package com.sanhiruzu.ami.index;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertTrue;

class ProviderRegistryLootRevisionSourceTest {
    @Test
    void deferredLootEdgesAdvanceIndexRevision() throws Exception {
        String registry = Files.readString(Path.of("../xplat/src/main/java/com/sanhiruzu/ami/index/ProviderRegistry.java"));

        int method = registry.indexOf("indexLootTablesDeferred");
        int edgesCheck = registry.indexOf("edgesAdded() > 0", method);
        int markChanged = registry.indexOf("markGraphChanged()", edgesCheck);
        assertTrue(edgesCheck > method,
                "Deferred loot indexing should only bump the revision when it actually adds source edges.");
        assertTrue(markChanged > edgesCheck,
                "Deferred loot source edges must bump GlobalIndex revision so active source routes can refresh.");
    }
}
