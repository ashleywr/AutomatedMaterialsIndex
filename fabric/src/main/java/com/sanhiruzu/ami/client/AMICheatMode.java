package com.sanhiruzu.ami.client;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

/**
 * Fabric stub for AMICheatMode.
 * Full implementation (networking, packet dispatch) is deferred to a later milestone.
 * Provides the public API surface required by xplat so the project compiles.
 */
public final class AMICheatMode {
    private AMICheatMode() {
    }

    // TODO(Milestone C+): implement Fabric cheat mode using Fabric networking

    public static boolean isEnabled() {
        return false;
    }

    public static boolean isAllowed() {
        return false;
    }

    public static boolean hasCarriedItem() {
        return false;
    }

    public static void giveItem(ResourceLocation itemId) {
    }

    public static void giveItem(ItemStack source) {
    }

    public static void giveStack(ResourceLocation itemId) {
    }

    public static void giveStack(ItemStack source) {
    }

    public static void deleteCarried() {
    }

    public static void locateBiome(ResourceLocation biomeId) {
    }

    public static void locateStructure(ResourceLocation structureId) {
    }

    public static void runCommand(String command) {
    }

    public static void giveEntityAsSpawnEgg(ResourceLocation entityId) {
    }

    public static void giveEntityStackAsSpawnEgg(ResourceLocation entityId) {
    }

    public static void spawnPokemon(ResourceLocation entityId) {
    }

    public static void pokemonToParty(ResourceLocation entityId) {
    }
}
