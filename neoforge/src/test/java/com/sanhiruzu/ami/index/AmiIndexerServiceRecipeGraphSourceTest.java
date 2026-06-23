package com.sanhiruzu.ami.index;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertTrue;

class AmiIndexerServiceRecipeGraphSourceTest {
    @Test
    void cacheRestorePathRebuildsRecipeGraphEdges() throws Exception {
        String service = Files.readString(Path.of("../xplat/src/main/java/com/sanhiruzu/ami/index/AmiIndexerService.java"));
        String registry = Files.readString(Path.of("../xplat/src/main/java/com/sanhiruzu/ami/index/ProviderRegistry.java"));

        int cacheLoad = service.indexOf("Services.PLATFORM.tryLoadGlobalIndexCache()");
        int ensureRecipeIndex = service.indexOf("ensureRecipeIndexBuilt(level)", cacheLoad);
        int ensureMethod = service.indexOf("private void ensureRecipeIndexBuilt");
        int alreadyBuilt = service.indexOf("if (recipeIndex.isBuilt())", ensureMethod);
        int restoreRecipeGraph = service.indexOf("ProviderRegistry.indexRecipeGraphDeferred(level)", alreadyBuilt);
        assertTrue(cacheLoad >= 0);
        assertTrue(ensureRecipeIndex > cacheLoad);
        assertTrue(restoreRecipeGraph > alreadyBuilt,
                "Cached GlobalIndex restores must rebuild non-serialized recipe graph edges for Sources.");

        int recipeRebuild = service.indexOf("recipeIndex.rebuild(level)");
        int graphAfterRebuild = service.indexOf("ProviderRegistry.indexRecipeGraphDeferred(level)", recipeRebuild);
        assertTrue(graphAfterRebuild > recipeRebuild,
                "Deferred recipe-index rebuilds must also refresh recipe graph edges.");

        assertTrue(registry.contains("indexRecipeGraphDeferred"));
        assertTrue(registry.contains("new RecipeGraphProvider().populate"));
    }
}
