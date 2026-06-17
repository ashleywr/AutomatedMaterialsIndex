package com.sanhiruzu.ami.client.results;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

class ItemGridViewTextureCachePolicyTest {
    @Test
    void itemStackGridRenderingIsNotPropertyGated() throws Exception {
        String source = Files.readString(Path.of("..", "xplat", "src", "main", "java", "com", "sanhiruzu",
                "ami", "client", "results", "ItemGridView.java"));

        assertFalse(source.contains("ami.itemIconCache"));
        assertFalse(source.contains("TEXTURE_ITEM_ICON_CACHE_ENABLED"));
        assertFalse(source.contains("supportsItemIconCache"));
    }

    @Test
    void realItemStacksUseMinecraftItemRenderPathInsteadOfFramebufferThumbnails() throws Exception {
        String source = Files.readString(Path.of("..", "xplat", "src", "main", "java", "com", "sanhiruzu",
                "ami", "client", "results", "ItemGridView.java"));

        assertTrue(source.contains("itemIconBatchRenderer().add(stack, x, y)"));
        assertFalse(source.contains("ItemIconCache"));
        assertFalse(source.contains("primeQueuedIconCache"));
        assertFalse(source.contains("ItemIconCache.primeVisible"));
        assertFalse(source.contains("ItemIconCache.requestVisible"));
        assertFalse(source.contains("ItemIconCache.blit"));
    }

    @Test
    void loaderClientTicksDoNotWarmFramebufferItemThumbnails() throws Exception {
        String forgeClient = Files.readString(Path.of("..", "forge", "src", "main", "java", "com", "sanhiruzu",
                "ami", "forge", "AMIClient.java"));
        String neoForgeClient = Files.readString(Path.of("..", "neoforge", "src", "main", "java", "com", "sanhiruzu",
                "ami", "neoforge", "AMIClient.java"));

        assertFalse(forgeClient.contains("ItemIconCache.tickWarmup();"));
        assertFalse(neoForgeClient.contains("ItemIconCache.tickWarmup();"));
    }
}
