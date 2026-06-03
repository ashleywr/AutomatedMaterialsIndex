package com.sanhiruzu.ami.config;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;

import static org.junit.jupiter.api.Assertions.*;

public class AmiConfigTest {

    @BeforeEach
    void setup() {
        AmiConfig.resetToDefaults();
    }

    @Test
    void testResetToDefaults() {
        // Change some values
        AmiConfig.cheatMode = true;
        AmiConfig.accentColor = 0x000000;
        AmiConfig.leftPanelWidth = 500;

        AmiConfig.resetToDefaults();

        // Verify they are back to original hardcoded defaults
        assertFalse(AmiConfig.cheatMode);
        assertEquals(0xFF5555, AmiConfig.accentColor);
        assertEquals(140, AmiConfig.leftPanelWidth);
        assertEquals("LIST", AmiConfig.rightPanelAlternateSlots);
        assertEquals(10, AmiConfig.listScrollRows);
        assertEquals(AmiConfig.GuideIndexingMode.TITLES, AmiConfig.guideIndexingMode);
        assertEquals(4096, AmiConfig.guideSummaryTextCap);
        assertFalse(AmiConfig.showTooltipTags);
        assertFalse(AmiConfig.startHidden);
    }

    @Test
    void defaultAlternateRightPanelExposesListView() {
        assertEquals(
                java.util.List.of(AmiConfig.PanelContent.LIST),
                AmiConfig.parsePanelSlots(AmiConfig.rightPanelAlternateSlots)
        );
    }

    @Test
    void testAnnotationsPresent() throws NoSuchFieldException {
        Field modeField = AmiConfig.class.getField("mode");
        assertTrue(modeField.isAnnotationPresent(ConfigValue.class));
        assertTrue(modeField.isAnnotationPresent(ConfigGroup.class));
        assertEquals("general", modeField.getAnnotation(ConfigGroup.class).value());

        Field accentField = AmiConfig.class.getField("accentColor");
        assertTrue(accentField.isAnnotationPresent(ConfigColor.class));
        assertTrue(accentField.isAnnotationPresent(ConfigValue.class));
    }

    @Test
    void testAllConfigValuesHaveKeys() {
        for (Field field : AmiConfig.class.getFields()) {
            ConfigValue annotation = field.getAnnotation(ConfigValue.class);
            if (annotation != null) {
                assertFalse(annotation.value().isEmpty(), "Field " + field.getName() + " has empty config key");
                assertTrue(annotation.value().contains("."), "Field " + field.getName() + " key should follow group.key format");
            }
        }
    }
}
