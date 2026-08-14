package com.sanhiruzu.ami.index.providers;

import com.sanhiruzu.ami.index.ClassificationOverrides;
import com.sanhiruzu.ami.index.ItemFilter;
import com.sanhiruzu.ami.index.SearchNodeKeys;
import net.minecraft.resources.ResourceLocation;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

class ItemProviderPackVisibilityOverrideTest {

    @BeforeEach
    void setup() {
        ClassificationOverrides.clear();
    }

    @AfterEach
    void teardown() {
        ClassificationOverrides.clear();
    }

    @Test
    void modWideHiddenRuleAppliesToItemMetadata() {
        ClassificationOverrides.parseAndInstall("""
                {
                  "modPatterns": [
                    { "mod": "examplemod", "match": "all", "visibility": "hidden" }
                  ]
                }
                """);
        Map<String, String> meta = survivalMeta();

        ItemProvider.applyPackVisibilityOverrides(
                ResourceLocation.parse("examplemod:any_item"), null, "", meta);

        assertEquals("hidden", meta.get(SearchNodeKeys.VISIBILITY));
    }

    @Test
    void perItemVisibleOverrideWinsOverModWideHiddenRule() {
        ClassificationOverrides.parseAndInstall("""
                {
                  "items": {
                    "examplemod:kept_item": { "visibility": "visible" }
                  },
                  "modPatterns": [
                    { "mod": "examplemod", "match": "all", "visibility": "hidden" }
                  ]
                }
                """);
        Map<String, String> meta = survivalMeta();

        ItemProvider.applyPackVisibilityOverrides(
                ResourceLocation.parse("examplemod:kept_item"), null, "", meta);

        assertFalse(meta.containsKey(SearchNodeKeys.VISIBILITY));
    }

    @Test
    void baseItemVisibilityOverrideAppliesToGeneratedVariant() {
        ClassificationOverrides.parseAndInstall("""
                {
                  "items": {
                    "examplemod:wand": { "visibility": "hidden" }
                  }
                }
                """);
        Map<String, String> meta = survivalMeta();

        ItemProvider.applyPackVisibilityOverrides(
                ResourceLocation.parse("examplemod:wand/variant/red"),
                ResourceLocation.parse("examplemod:wand"),
                "",
                meta);

        assertEquals("hidden", meta.get(SearchNodeKeys.VISIBILITY));
    }

    @Test
    void perItemAccessLevelWinsOverModWideAccessLevelRule() {
        ClassificationOverrides.parseAndInstall("""
                {
                  "items": {
                    "examplemod:normal_item": { "accessLevel": "survival" }
                  },
                  "modPatterns": [
                    { "mod": "examplemod", "match": "all", "accessLevel": "dev" }
                  ]
                }
                """);

        String accessLevel = ItemProvider.applyPackAccessLevelOverride(
                ResourceLocation.parse("examplemod:normal_item"),
                null,
                "",
                ItemFilter.ACCESS_SURVIVAL);

        assertEquals(ItemFilter.ACCESS_SURVIVAL, accessLevel);
    }

    private static Map<String, String> survivalMeta() {
        Map<String, String> meta = new HashMap<>();
        meta.put(SearchNodeKeys.ACCESS_LEVEL, ItemFilter.ACCESS_SURVIVAL);
        return meta;
    }
}
