package com.sanhiruzu.ami.mixin;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;

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
        Map<Path, String> expectedMixins = Map.of(
                Paths.get("../forge/src/main/resources/ami.mixins.json"), "ForgeRecipeBookComponentMixin",
                Paths.get("../neoforge/src/main/resources/ami.mixins.json"), "RecipeBookComponentMixin"
        );
        for (Map.Entry<Path, String> expected : expectedMixins.entrySet()) {
            Path mixinConfigPath = expected.getKey();
            assertTrue(Files.exists(mixinConfigPath), "Mixin config file not found at " + mixinConfigPath);

            String jsonContent = Files.readString(mixinConfigPath, StandardCharsets.UTF_8);
            JsonObject config = JsonParser.parseString(jsonContent).getAsJsonObject();
            JsonArray clientMixins = config.getAsJsonArray("client");

            boolean hasRecipeBookMixin = false;
            for (var element : clientMixins) {
                if (expected.getValue().equals(element.getAsString())) {
                    hasRecipeBookMixin = true;
                    break;
                }
            }

            assertTrue(hasRecipeBookMixin,
                    expected.getValue() + " must be registered in " + mixinConfigPath
                            + " so the vanilla recipe book button can toggle AMI.");
        }
    }

    @Test
    void forgeRecipeBookMixinTargetsDevAndProductionNames() throws Exception {
        Path mixinPath = Paths.get("../forge/src/main/java/com/sanhiruzu/ami/mixin/ForgeRecipeBookComponentMixin.java");
        assertTrue(Files.exists(mixinPath), "Forge recipe book mixin not found at " + mixinPath);

        String source = Files.readString(mixinPath, StandardCharsets.UTF_8);
        assertTrue(source.contains("{\"toggleVisibility\", \"m_100384_\"}"),
                "Forge recipe book toggle injection must target both dev and production names");
        assertTrue(source.contains("@Shadow(aliases = \"m_100385_\") public abstract boolean isVisible();"),
                "Forge recipe book mixin must alias RecipeBookComponent.isVisible() to SRG name m_100385_");
        assertTrue(source.contains("@Shadow(aliases = \"m_100369_\") protected abstract void setVisible(boolean visible);"),
                "Forge recipe book mixin must alias RecipeBookComponent.setVisible(boolean) to SRG name m_100369_");
        assertFalse(source.contains("public abstract boolean m_100385_()"),
                "Forge recipe book mixin should keep readable shadow names in source for dev runs");
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
    void recipeBookMixinPriorityIsLowerThanEmi() throws Exception {
        // EMI's RecipeBookWidgetMixin uses the default Mixin priority of 1000.
        // AMI must declare priority = 500 so its injection fires first and can cancel
        // the method before EMI's handler runs. If the priority ever drifts above 1000,
        // EMI will intercept the button regardless of AMI's recipeBookAction config.
        for (Path mixinPath : new Path[]{
                Paths.get("../xplat/src/main/java/com/sanhiruzu/ami/mixin/RecipeBookComponentMixin.java"),
                Paths.get("../forge/src/main/java/com/sanhiruzu/ami/mixin/ForgeRecipeBookComponentMixin.java")
        }) {
            String source = Files.readString(mixinPath, StandardCharsets.UTF_8);
            assertTrue(source.contains("priority = 500"),
                    "Recipe book mixin in " + mixinPath + " must declare priority = 500 to preempt EMI's " +
                            "RecipeBookWidgetMixin (default priority 1000). Without this, EMI handles the button first.");
        }
    }

    @Test
    void privateGuiGraphicsInvokerIsNotRegistered() throws Exception {
        for (Path mixinConfigPath : new Path[] {
                Paths.get("../forge/src/main/resources/ami.mixins.json"),
                Paths.get("../neoforge/src/main/resources/ami.mixins.json")
        }) {
            String jsonContent = Files.readString(mixinConfigPath, StandardCharsets.UTF_8);
            assertFalse(jsonContent.contains("\"GuiGraphicsInvoker\""),
                    "Private GuiGraphicsExtractor invoker must not be registered in " + mixinConfigPath);
        }
    }
}
