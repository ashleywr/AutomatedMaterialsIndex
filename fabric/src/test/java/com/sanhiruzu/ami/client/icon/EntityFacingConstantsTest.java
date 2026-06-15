package com.sanhiruzu.ami.client.icon;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Verifies that the Fabric port uses the same STATIC_ENTITY_Y_ROT value as NeoForge (180.0f).
 * Both loaders use the Mojang-mapped InventoryScreen.renderEntityInInventory() API which
 * takes a model-pose quaternion — so the value must be 180.0f (not 0.0f as in legacy Forge).
 */
class EntityFacingConstantsTest {

    @Test
    void staticEntityYRotIsFacingForward() {
        assertEquals(180.0f, EntityFacingConstants.STATIC_ENTITY_Y_ROT,
                "Fabric must use 180.0f (model-pose quaternion) to match NeoForge facing direction");
    }
}
