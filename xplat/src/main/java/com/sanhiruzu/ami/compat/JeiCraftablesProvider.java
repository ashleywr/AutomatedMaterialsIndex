package com.sanhiruzu.ami.compat;

import com.sanhiruzu.ami.platform.Services;
import com.sanhiruzu.ami.util.AmiRecipeHolder;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;

final class JeiCraftablesProvider implements CraftablesProvider {
    private List<ItemStack> cached = List.of();
    private int lastInventoryVersion = -1;
    private long lastMenuSignature = Long.MIN_VALUE;
    private Screen lastScreen = null;

    @Override
    public Result getCraftables() {
        if (!RecipeViewerBridge.isJeiSelectedExternalViewer()) {
            return Result.unhandled();
        }

        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null) {
            return Result.handled(List.of());
        }

        Screen screen = mc.screen;
        if (!(screen instanceof AbstractContainerScreen<?>)) {
            return Result.handled(List.of());
        }
        if (!Services.PLATFORM.isRecipeIndexBuilt()) {
            return Result.unhandled();
        }

        AbstractContainerMenu menu = mc.player.containerMenu;
        int version = mc.player.getInventory().getTimesChanged();
        long menuSignature = fullMenuSignature(menu);
        if (screen == lastScreen && version == lastInventoryVersion && menuSignature == lastMenuSignature) {
            return Result.handled(cached);
        }

        List<ItemStack> result = new ArrayList<>();
        for (AmiRecipeHolder<?> recipe : Services.PLATFORM.getAllRecipes()) {
            if (!JeiRecipeBridge.canTransfer(recipe, screen)) {
                continue;
            }
            ItemStack output = Services.PLATFORM.getRecipeResultItem(recipe, mc.level.registryAccess());
            if (output.isEmpty()) {
                continue;
            }
            output = output.copy();
            if (!containsEquivalent(result, output)) {
                result.add(output);
            }
        }

        cached = List.copyOf(result);
        lastScreen = screen;
        lastInventoryVersion = version;
        lastMenuSignature = menuSignature;
        return Result.handled(cached);
    }

    private static boolean containsEquivalent(List<ItemStack> stacks, ItemStack candidate) {
        for (ItemStack existing : stacks) {
            if (Services.PLATFORM.sameItemSameComponents(existing, candidate)) {
                return true;
            }
        }
        return false;
    }

    private static long fullMenuSignature(AbstractContainerMenu menu) {
        long signature = 0xcbf29ce484222325L;
        if (menu == null) {
            return signature;
        }
        signature = mix(signature, menu.containerId);
        for (Slot slot : menu.slots) {
            if (slot == null) {
                signature = mix(signature, 0);
                continue;
            }
            signature = mix(signature, slot.isActive() ? 1 : 0);
            ItemStack stack = slot.getItem();
            if (stack == null || stack.isEmpty()) {
                signature = mix(signature, 0);
                continue;
            }
            signature = mix(signature, 1);
            signature = mix(signature, BuiltInRegistries.ITEM.getId(stack.getItem()));
            signature = mix(signature, stack.getCount());
        }
        return signature;
    }

    private static long mix(long signature, int value) {
        signature ^= value;
        return signature * 0x100000001b3L;
    }
}
