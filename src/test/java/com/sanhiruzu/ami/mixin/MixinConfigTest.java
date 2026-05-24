package com.sanhiruzu.ami.mixin;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

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
        Path mixinConfigPath = Paths.get("src/main/resources/ami.mixins.json");
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
}
