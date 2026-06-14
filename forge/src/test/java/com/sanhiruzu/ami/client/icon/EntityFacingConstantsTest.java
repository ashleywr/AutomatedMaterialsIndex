package com.sanhiruzu.ami.client.icon;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class EntityFacingConstantsTest {

    @Test
    void staticEntityYRotIsZeroForForge() {
        /*
         * Forge's InventoryScreen.renderEntityInInventory applies rotateZ(PI) as
         * the *camera orientation* quaternion. This orients the camera to look in the
         * +Z direction. An entity with yBodyRot=0 faces the +Z axis and therefore
         * faces the camera — which is correct.
         *
         * If this value were 180f the entity would face -Z, i.e. away from the camera.
         * This was the bug that has been reintroduced multiple times. DO NOT change
         * this to 180f. Fix the bug here and you will see this test fail.
         *
         * The NeoForge module has its own EntityFacingConstants with 180f — that is
         * correct for NeoForge's different API where rotateZ(PI) is a model pose
         * transform rather than a camera orientation.
         */
        assertEquals(0f, EntityFacingConstants.STATIC_ENTITY_Y_ROT,
                "Forge entity icons must use yBodyRot=0: Forge's renderEntityInInventory " +
                "uses rotateZ(PI) as camera orientation (not model pose), so yBodyRot=0 " +
                "faces the camera. Using 180f renders entities backwards.");
    }
}
