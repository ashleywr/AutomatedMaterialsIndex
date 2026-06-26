package com.sanhiruzu.ami.index;

import net.minecraft.resources.ResourceLocation;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ClassificationOverrideSchemaVersionTest {

    @AfterEach
    void cleanUp() {
        ClassificationOverrides.clear();
    }

    @Test
    void futureSchemaVersion_doesNotThrow_andParsesKnownFields() {
        String json = """
            {
              "schemaVersion": 2,
              "items": {
                "modid:future": { "category": "still_loaded" }
              }
            }
            """;

        ClassificationOverrides.parseAndInstall(json);

        Optional<ClassificationOverride> ov = ClassificationOverrides.forItem(
                ResourceLocation.parse("modid:future"));
        assertTrue(ov.isPresent(), "known fields must still load from a future-versioned file");
        assertEquals("still_loaded", ov.get().forceCategory());
    }

    @Test
    void supportedSchemaVersion_loadsCleanly() {
        String json = """
            {
              "schemaVersion": 1,
              "items": {
                "modid:current": { "category": "ok" }
              }
            }
            """;

        ClassificationOverrides.parseAndInstall(json);

        assertEquals("ok",
                ClassificationOverrides.forItem(ResourceLocation.parse("modid:current"))
                        .orElseThrow().forceCategory());
    }

    @Test
    void missingSchemaVersion_treatedAsVersion1_noThrow() {
        String json = """
            {
              "items": {
                "modid:legacy": { "category": "legacy_ok" }
              }
            }
            """;

        ClassificationOverrides.parseAndInstall(json);

        assertEquals("legacy_ok",
                ClassificationOverrides.forItem(ResourceLocation.parse("modid:legacy"))
                        .orElseThrow().forceCategory());
    }
}
