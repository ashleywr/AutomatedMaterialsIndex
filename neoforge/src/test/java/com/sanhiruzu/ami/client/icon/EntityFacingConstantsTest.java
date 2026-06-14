package com.sanhiruzu.ami.client.icon;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class EntityFacingConstantsTest {

    @Test
    void staticEntityYRotIs180ForNeoForge() {
        /*
         * NeoForge's InventoryScreen.renderEntityInInventory applies rotateZ(PI) as
         * the *model pose* quaternion (poseQuat parameter). This rotates the entity's
         * local coordinate frame. An entity with yBodyRot=180 faces -Z in world space;
         * after the PI pose rotation its model is flipped to face +Z toward the viewer.
         *
         * If this value were 0f the entity would face +Z before the pose flip, ending
         * up facing -Z away from the viewer — rendering backwards.
         *
         * The Forge module has its own EntityFacingConstants with 0f — that is correct
         * for Forge's different API where rotateZ(PI) is a camera orientation rather
         * than a model pose transform.
         */
        assertEquals(180.0f, EntityFacingConstants.STATIC_ENTITY_Y_ROT,
                "NeoForge entity icons must use yBodyRot=180: NeoForge's renderEntityInInventory " +
                "uses rotateZ(PI) as a model pose transform (not camera orientation), so " +
                "yBodyRot=180 faces the camera after the pose flip. Using 0f renders backwards.");
    }
}
