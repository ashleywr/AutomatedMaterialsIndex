package com.sanhiruzu.ami.client;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ExternalTooltipReplayPolicyTest {
    @Test
    void forgeExternalTooltipReplayStoresMutableElementsForForgeGatherSubscribers() throws Exception {
        assertMutableReplayCopy(repoRoot().resolve(Path.of("forge", "src", "main", "java", "com", "sanhiruzu",
                "ami", "client", "InventoryOverlayHandler.java")));
    }

    @Test
    void neoforgeExternalTooltipReplayStoresMutableElementsForGatherSubscribers() throws Exception {
        assertMutableReplayCopy(repoRoot().resolve(Path.of("neoforge", "src", "main", "java", "com", "sanhiruzu",
                "ami", "client", "InventoryOverlayHandler.java")));
    }

    private static void assertMutableReplayCopy(Path sourcePath) throws Exception {
        String source = Files.readString(sourcePath);

        assertFalse(source.contains("List.copyOf(event.getTooltipElements())"),
                "Replay elements are passed back through tooltip GatherComponents and must remain mutable");
        assertTrue(source.contains("new ArrayList<>(event.getTooltipElements())"),
                "Replay elements should be copied into a mutable list");
    }

    private static Path repoRoot() {
        Path current = Path.of("").toAbsolutePath();
        if (Files.exists(current.resolve("settings.gradle"))) {
            return current;
        }
        return current.getParent();
    }
}
