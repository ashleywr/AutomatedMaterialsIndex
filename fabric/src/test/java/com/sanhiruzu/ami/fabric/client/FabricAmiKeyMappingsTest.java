package com.sanhiruzu.ami.fabric.client;

import com.mojang.blaze3d.platform.InputConstants;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

class FabricAmiKeyMappingsTest {
    @Test
    void favoriteDefaultsToVOnFabric() {
        FabricAmiKeyMappings mappings = new FabricAmiKeyMappings();

        assertEquals(
                InputConstants.Type.KEYSYM.getOrCreate(org.lwjgl.glfw.GLFW.GLFW_KEY_V),
                mappings.favorite().getDefaultKey(),
                "Fabric favorites should default to V");
    }

    @Test
    void toggleViewerDefaultsToBOnFabric() {
        FabricAmiKeyMappings mappings = new FabricAmiKeyMappings();

        assertEquals(
                InputConstants.Type.KEYSYM.getOrCreate(org.lwjgl.glfw.GLFW.GLFW_KEY_B),
                mappings.toggleViewer().getDefaultKey(),
                "Fabric toggle viewer should default to B");
    }

    @Test
    void debugTooltipsDoesNotShareFavoritesDefaultKey() {
        FabricAmiKeyMappings mappings = new FabricAmiKeyMappings();

        assertNotEquals(
                mappings.favorite().getDefaultKey(),
                mappings.debugTooltips().getDefaultKey(),
                "Fabric debug tooltips must not reuse the plain favorites key");
    }

    @Test
    void debugTooltipsIsUnboundOnFabric() {
        FabricAmiKeyMappings mappings = new FabricAmiKeyMappings();

        assertEquals(
                InputConstants.UNKNOWN,
                mappings.debugTooltips().getDefaultKey(),
                "Fabric debug tooltips should be unbound");
    }
}
