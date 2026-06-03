package com.sanhiruzu.ami.client;

import com.google.gson.JsonParser;
import net.minecraft.resources.ResourceLocation;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class ThemeResourceLoaderTest {
    @Test
    void appliesNestedResourceThemeValuesByFieldName() {
        int oldBand = AMITheme.GRID_GROUP_BAND;
        int oldPadding = AMITheme.GLOBAL_PADDING;
        try {
            boolean applied = ThemeResourceStyles.apply(new ResourceLocation("ami:test"), JsonParser.parseString("""
                    {
                      "layout": {
                        "global-padding": 9
                      },
                      "grid": {
                        "group": {
                          "band": "#03000000"
                        }
                      }
                    }
                    """));

            assertTrue(applied);
            assertEquals(9, AMITheme.GLOBAL_PADDING);
            assertEquals(0x03000000, AMITheme.GRID_GROUP_BAND);
        } finally {
            AMITheme.GRID_GROUP_BAND = oldBand;
            AMITheme.GLOBAL_PADDING = oldPadding;
        }
    }

    @Test
    void ignoresMalformedThemeValuesWithoutChangingExistingValues() {
        int oldBand = AMITheme.GRID_GROUP_BAND;
        try {
            AMITheme.GRID_GROUP_BAND = 0x12345678;
            boolean applied = ThemeResourceStyles.apply(new ResourceLocation("ami:test"), JsonParser.parseString("""
                    {
                      "grid": {
                        "group": {
                          "band": "definitely-not-a-color"
                        }
                      }
                    }
                    """));

            assertFalse(applied);
            assertEquals(0x12345678, AMITheme.GRID_GROUP_BAND);
        } finally {
            AMITheme.GRID_GROUP_BAND = oldBand;
        }
    }

    @Test
    void appliesNestedSearchAndAccentThemeValues() {
        int oldAccent = AMITheme.ACCENT_BLUE;
        int oldSearchBg = AMITheme.SEARCH_BAR_BG;
        int oldSearchBorder = AMITheme.SEARCH_BAR_BORDER;
        try {
            boolean applied = ThemeResourceStyles.apply(new ResourceLocation("ami:test"), JsonParser.parseString("""
                    {
                      "accent-blue": "#A02020",
                      "search": {
                        "bar": {
                          "bg": "#000000",
                          "border": "#E0E0E0"
                        }
                      }
                    }
                    """));

            assertTrue(applied);
            assertEquals(0xFFA02020, AMITheme.ACCENT_BLUE);
            assertEquals(0xFF000000, AMITheme.SEARCH_BAR_BG);
            assertEquals(0xFFE0E0E0, AMITheme.SEARCH_BAR_BORDER);
        } finally {
            AMITheme.ACCENT_BLUE = oldAccent;
            AMITheme.SEARCH_BAR_BG = oldSearchBg;
            AMITheme.SEARCH_BAR_BORDER = oldSearchBorder;
        }
    }
}
