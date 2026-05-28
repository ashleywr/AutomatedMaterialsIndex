package com.sanhiruzu.ami.mixin;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class MixinsJsonTest {

    private static final Path JSON_PATH = Paths.get("../neoforge/src/main/resources/ami.mixins.json");

    @Test
    void mixinsJsonExists() {
        assertTrue(Files.exists(JSON_PATH), "ami.mixins.json must exist");
    }

    @Test
    void containsJeiSuppressionMixinsInClientArray() throws Exception {
        String content = Files.readString(JSON_PATH);
        assertTrue(content.contains("\"JeiGuiEventHandlerMixin\""),
                "ami.mixins.json must contain JeiGuiEventHandlerMixin in client array");
        assertTrue(content.contains("\"JeiClientInputHandlerMixin\""),
                "ami.mixins.json must contain JeiClientInputHandlerMixin in client array");
    }

    @Test
    void doesNotContainDeletedJeiMixinClasses() throws Exception {
        String content = Files.readString(JSON_PATH);
        String[] deletedMixins = {
                "JeiIngredientListOverlayMixin",
                "JeiBookmarkOverlayMixin",
                "JeiScreenPropertiesCacheMixin",
                "RecipeViewerMixinSupport",
                "JeiPluginCallerMixin",
                "JeiNeoForgeGuiPluginMixin",
                "JeiGuiStarterMixin"
        };
        for (String deleted : deletedMixins) {
            assertFalse(content.contains("\"" + deleted + "\""),
                    "ami.mixins.json must not contain deleted mixin: " + deleted);
        }
    }

    @Test
    void hasRequiredTrue() throws Exception {
        String content = Files.readString(JSON_PATH);
        assertTrue(content.contains("\"required\": true"),
                "ami.mixins.json must have 'required': true");
    }

    @Test
    void hasCorrectPackageAndPlugin() throws Exception {
        String content = Files.readString(JSON_PATH);
        assertTrue(content.contains("\"package\": \"com.sanhiruzu.ami.mixin\""),
                "Mixin package must be com.sanhiruzu.ami.mixin");
        assertTrue(content.contains("\"plugin\": \"com.sanhiruzu.ami.mixin.AmiMixinConfigPlugin\""),
                "Mixin plugin must be AmiMixinConfigPlugin");
    }

    @Test
    void hasCompatibilityLevelJava17() throws Exception {
        String content = Files.readString(JSON_PATH);
        assertTrue(content.contains("\"compatibilityLevel\": \"JAVA_21\""),
                "Mixin compatibilityLevel must be JAVA_21");
    }

    @Test
    void mixinsArrayExistsAndIsEmpty() throws Exception {
        String content = Files.readString(JSON_PATH);
        assertTrue(content.contains("\"mixins\": []"),
                "Common mixins array must be empty (all our mixins are client-only)");
    }

    @Test
    void clientArrayDoesNotContainDeletedClasses() throws Exception {
        List<String> deletedFiles = List.of(
                "AtlasGridWidget.java",
                "FacetBar.java"
        );
        String content = Files.readString(JSON_PATH);
        for (String deleted : deletedFiles) {
            String baseName = deleted.replace(".java", "");
            assertFalse(content.contains("\"" + baseName + "\""),
                    "ami.mixins.json must not reference deleted class: " + baseName);
        }
    }
}
