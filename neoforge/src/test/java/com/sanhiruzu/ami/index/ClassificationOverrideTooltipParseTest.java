package com.sanhiruzu.ami.index;

import net.minecraft.resources.ResourceLocation;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

class ClassificationOverrideTooltipParseTest {

    @AfterEach
    void cleanUp() {
        ClassificationOverrides.clear();
    }

    @Test
    void parses_tooltipLines_onPerItemEntry() {
        String json = """
        { "items": {
            "modid:thing": {
              "tooltipLines": ["Custom note", "Second line"]
            }
        }}""";
        ClassificationOverrides.parseAndInstall(json);
        Optional<ClassificationOverride> ov = ClassificationOverrides.forItem(
                ResourceLocation.parse("modid:thing"));
        assertTrue(ov.isPresent());
        assertEquals(List.of("Custom note", "Second line"), ov.get().tooltipLines());
    }

    @Test
    void absent_tooltipLines_defaultsToEmpty() {
        String json = """
                { "items": { "modid:thing": { "category": "x" } } }
                """;
        ClassificationOverrides.parseAndInstall(json);
        Optional<ClassificationOverride> ov = ClassificationOverrides.forItem(
                ResourceLocation.parse("modid:thing"));
        assertTrue(ov.isPresent());
        assertEquals(List.of(), ov.get().tooltipLines());
    }
}
