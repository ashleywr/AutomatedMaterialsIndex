package com.sanhiruzu.ami.index;

import net.minecraft.resources.ResourceLocation;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

class PackOverrideLoaderTest {

    @AfterEach
    void cleanUp() { ClassificationOverrides.clear(); }

    @Test
    void missingFile_returnsFileFoundFalse_noError(@TempDir Path tmp) throws Exception {
        PackOverrideLoader.LoadResult r = PackOverrideLoader.loadFrom(tmp);
        assertFalse(r.fileFound());
        assertTrue(r.parseOk());
        assertNull(r.errorMessage());
    }

    @Test
    void validFile_installedOnTopOfBundled(@TempDir Path tmp) throws Exception {
        ClassificationOverrides.parseAndInstall(
                """
                { "items": { "modid:keep": { "category": "kept" } } }
                """);
        Path amiDir = tmp.resolve("ami");
        Files.createDirectories(amiDir);
        Files.writeString(amiDir.resolve("overrides.json"),
                """
                { "items": { "modid:new": { "category": "added" } } }
                """);

        PackOverrideLoader.LoadResult r = PackOverrideLoader.loadFrom(tmp);
        assertTrue(r.fileFound());
        assertTrue(r.parseOk());
        assertEquals("kept",
                ClassificationOverrides.forItem(ResourceLocation.parse("modid:keep")).orElseThrow().forceCategory());
        assertEquals("added",
                ClassificationOverrides.forItem(ResourceLocation.parse("modid:new")).orElseThrow().forceCategory());
    }

    @Test
    void malformedJson_doesNotThrow_preservesBundled(@TempDir Path tmp) throws Exception {
        ClassificationOverrides.parseAndInstall(
                """
                { "items": { "modid:keep": { "category": "kept" } } }
                """);
        Path amiDir = tmp.resolve("ami");
        Files.createDirectories(amiDir);
        Files.writeString(amiDir.resolve("overrides.json"), "{ not valid json");

        PackOverrideLoader.LoadResult r = PackOverrideLoader.loadFrom(tmp);
        assertTrue(r.fileFound());
        // Bundled entry must still be present even though pack JSON was junk.
        assertEquals("kept",
                ClassificationOverrides.forItem(ResourceLocation.parse("modid:keep")).orElseThrow().forceCategory());
    }
}
