package com.sanhiruzu.ami.client.icon;

final class EntityFacingConstants {
    /*
     * Fabric's InventoryScreen.renderEntityInInventory(GuiGraphics, float, float,
     * int, Vector3f translate, Quaternionf poseQuat, Quaternionf cameraQuat, LivingEntity)
     * uses the same Mojang-mapped method signature as NeoForge 1.21.1: it passes
     * rotateZ(PI) as the *model pose* quaternion (poseQuat), which rotates the
     * entity's local frame. An entity with yBodyRot=180 faces -Z in world space;
     * after the PI pose rotation its model is flipped to face +Z toward the viewer.
     *
     * DO NOT change this to 0f. That renders entities facing away from the player.
     * The test EntityFacingConstantsTest locks this value and will fail if it is changed.
     *
     * Forge uses a different API signature where rotateZ(PI) is a *camera* orientation,
     * not a model pose; its EntityFacingConstants uses 0f.
     */
    static final float STATIC_ENTITY_Y_ROT = 180.0f;

    private EntityFacingConstants() {}
}
