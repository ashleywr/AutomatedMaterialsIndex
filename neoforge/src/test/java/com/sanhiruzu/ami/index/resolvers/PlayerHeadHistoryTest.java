package com.sanhiruzu.ami.index.resolvers;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class PlayerHeadHistoryTest {
    @AfterEach
    void cleanup() {
        PlayerHeadHistory.clearHistoryFileOverrideForTests();
    }

    @Test
    void recordPromotesMostRecentNameAndDeduplicatesIgnoringCase(@TempDir Path tempDir) {
        PlayerHeadHistory.setHistoryFileOverrideForTests(tempDir.resolve("ami").resolve("player_head_history.json"));

        PlayerHeadHistory.record("Steve");
        PlayerHeadHistory.record("Alex");
        PlayerHeadHistory.record("steve");
        PlayerHeadHistory.record("Bad Name");

        assertEquals(List.of("steve", "Alex"), PlayerHeadHistory.load());
    }

    @Test
    void loadSkipsInvalidEntriesAndCapsAtFifty(@TempDir Path tempDir) throws Exception {
        Path file = tempDir.resolve("ami").resolve("player_head_history.json");
        PlayerHeadHistory.setHistoryFileOverrideForTests(file);
        Files.createDirectories(file.getParent());

        StringBuilder json = new StringBuilder("[");
        json.append("\"Steve\",\"Bad Name\",\"STEVE\",");
        for (int i = 0; i < 55; i++) {
            if (i > 0) {
                json.append(',');
            }
            json.append('"').append("Player").append(i).append('"');
        }
        json.append(']');
        Files.writeString(file, json.toString(), StandardCharsets.UTF_8);

        List<String> loaded = PlayerHeadHistory.load();

        assertEquals(50, loaded.size());
        assertEquals("Steve", loaded.get(0));
        assertEquals("Player48", loaded.get(49));
    }
}
