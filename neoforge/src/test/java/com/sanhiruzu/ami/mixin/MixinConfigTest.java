package com.sanhiruzu.ami.mixin;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class MixinConfigTest {
    /**
     * Validates that critical EMI integration mixins are registered.
     * EmiScreenManagerMixin is essential for suppressing EMI's UI (search bar, buttons)
     * when AMI is active. If it's missing, EMI UI will appear unchecked.
     * <p>
     * This test prevents regression of the issue where EmiScreenManagerMixin was
     * accidentally removed from ami.mixins.json.
     */
    @Test
    void testEmiScreenManagerMixinIsRegistered() throws Exception {
        Path mixinConfigPath = Paths.get("../neoforge/src/main/resources/ami.mixins.json");
        assertTrue(Files.exists(mixinConfigPath), "Mixin config file not found at " + mixinConfigPath);

        String jsonContent = Files.readString(mixinConfigPath, StandardCharsets.UTF_8);
        JsonObject config = JsonParser.parseString(jsonContent).getAsJsonObject();

        assertTrue(config.has("client"), "Mixin config missing 'client' array");
        JsonArray clientMixins = config.getAsJsonArray("client");

        boolean hasEmiScreenManagerMixin = false;
        for (var element : clientMixins) {
            if ("EmiScreenManagerMixin".equals(element.getAsString())) {
                hasEmiScreenManagerMixin = true;
                break;
            }
        }

        assertTrue(hasEmiScreenManagerMixin,
                "EmiScreenManagerMixin must be registered in ami.mixins.json. " +
                        "This mixin suppresses EMI's UI when AMI is active. Without it, EMI buttons and " +
                        "search bar will appear unchecked. Current client mixins: " + clientMixins);
    }

    @Test
    void recipeBookComponentMixinIsRegistered() throws Exception {
        for (Path mixinConfigPath : new Path[] {
                Paths.get("../forge/src/main/resources/ami.mixins.json"),
                Paths.get("../neoforge/src/main/resources/ami.mixins.json")
        }) {
            assertTrue(Files.exists(mixinConfigPath), "Mixin config file not found at " + mixinConfigPath);

            String jsonContent = Files.readString(mixinConfigPath, StandardCharsets.UTF_8);
            JsonObject config = JsonParser.parseString(jsonContent).getAsJsonObject();
            JsonArray clientMixins = config.getAsJsonArray("client");

            boolean hasRecipeBookMixin = false;
            for (var element : clientMixins) {
                if ("RecipeBookComponentMixin".equals(element.getAsString())) {
                    hasRecipeBookMixin = true;
                    break;
                }
            }

            assertTrue(hasRecipeBookMixin,
                    "RecipeBookComponentMixin must be registered in " + mixinConfigPath
                            + " so the vanilla recipe book button can toggle AMI.");
        }
    }

    @Test
    void forgeMixinConfigIsDeclaredForDevRunsAndPackagedRuns() throws Exception {
        Path modsToml = Paths.get("../forge/src/main/resources/META-INF/mods.toml");
        assertTrue(Files.exists(modsToml), "Forge mods.toml file not found at " + modsToml);

        String modsTomlContent = Files.readString(modsToml, StandardCharsets.UTF_8);
        assertTrue(modsTomlContent.contains("[[mixins]]"),
                "Forge mods.toml must declare ami.mixins.json so Forge dev runs load suppression mixins");
        assertTrue(modsTomlContent.contains("config = \"${mod_id}.mixins.json\""),
                "Forge mods.toml must point at the AMI mixin config");

        Path buildFile = Paths.get("../forge/build.gradle");
        assertTrue(Files.exists(buildFile), "Forge build.gradle file not found at " + buildFile);

        String buildFileContent = Files.readString(buildFile, StandardCharsets.UTF_8);
        assertTrue(buildFileContent.contains("programArguments.addAll '--mixin', \"${mod_id}.mixins.json\".toString()"),
                "Forge ModDev exploded-source runs must pass --mixin so ami.mixins.json loads without a jar manifest");
    }

    @Test
    void privateGuiGraphicsInvokerIsNotRegistered() throws Exception {
        for (Path mixinConfigPath : new Path[] {
                Paths.get("../forge/src/main/resources/ami.mixins.json"),
                Paths.get("../neoforge/src/main/resources/ami.mixins.json")
        }) {
            String jsonContent = Files.readString(mixinConfigPath, StandardCharsets.UTF_8);
            assertFalse(jsonContent.contains("\"GuiGraphicsInvoker\""),
                    "Private GuiGraphics invoker must not be registered in " + mixinConfigPath);
        }
    }
}
