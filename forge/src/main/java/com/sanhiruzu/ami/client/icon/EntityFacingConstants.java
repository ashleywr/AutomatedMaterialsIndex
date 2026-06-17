package com.sanhiruzu.ami.client.icon;

public final class EntityFacingConstants {
    /*
     * Forge's InventoryScreen.renderEntityInInventory(GuiGraphics, int, int, int,
     * Quaternionf cameraOrientation, Quaternionf entityOrientation, LivingEntity)
     * passes rotateZ(PI) as the *camera* quaternion, making the camera look in the
     * +Z direction. An entity with yBodyRot=0 faces +Z and therefore faces the camera.
     *
     * DO NOT change this to 180f. That renders entities facing away from the player
     * and has been reverted multiple times. The test EntityFacingConstantsTest locks
     * this value and will fail if it is changed.
     *
     * NeoForge uses a different API signature where rotateZ(PI) is a *model pose*
     * transform, not a camera orientation; its EntityFacingConstants uses 180f.
     */
    public static final float STATIC_ENTITY_Y_ROT = 0f;

    private EntityFacingConstants() {}
}
