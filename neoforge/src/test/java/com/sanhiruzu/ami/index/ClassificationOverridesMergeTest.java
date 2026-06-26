package com.sanhiruzu.ami.index;

import net.minecraft.resources.ResourceLocation;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

class ClassificationOverridesMergeTest {

    @AfterEach
    void cleanUp() {
        ClassificationOverrides.clear();
    }

    @Test
    void packEntryOverridesBundledEntryForSameItem() {
        String bundled = """
        { "items": { "modid:thing": { "category": "old" } } }""";
        String pack = """
        { "items": { "modid:thing": { "category": "new" } } }""";

        ClassificationOverrides.parseAndInstall(bundled);
        ClassificationOverrides.mergeAndInstall(pack);

        Optional<ClassificationOverride> ov = ClassificationOverrides.forItem(
                ResourceLocation.parse("modid:thing"));
        assertTrue(ov.isPresent());
        assertEquals("new", ov.get().forceCategory());
    }

    @Test
    void packPatternMatchesBeforeBundledPattern() {
        String bundled = """
        { "modPatterns": [
            { "mod": "m", "pathTokens": ["x"], "category": "bundled_cat" }
        ]}""";
        String pack = """
        { "modPatterns": [
            { "mod": "m", "pathTokens": ["x"], "category": "pack_cat" }
        ]}""";
        ClassificationOverrides.parseAndInstall(bundled);
        ClassificationOverrides.mergeAndInstall(pack);

        Optional<ModPatternRule> rule = ClassificationOverrides.patternFor("m", "x");
        assertTrue(rule.isPresent());
        assertEquals("pack_cat", rule.get().category());
    }

    @Test
    void bundledItemSurvivesWhenPackOverridesDifferentItem() {
        ClassificationOverrides.parseAndInstall("""
                { "items": { "modid:a": { "category": "A" } } }
                """);
        ClassificationOverrides.mergeAndInstall("""
                { "items": { "modid:b": { "category": "B" } } }
                """);

        assertEquals("A", ClassificationOverrides.forItem(ResourceLocation.parse("modid:a")).orElseThrow().forceCategory());
        assertEquals("B", ClassificationOverrides.forItem(ResourceLocation.parse("modid:b")).orElseThrow().forceCategory());
    }
}
