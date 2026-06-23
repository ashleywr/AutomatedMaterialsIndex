package com.sanhiruzu.ami.compat;

import com.sanhiruzu.ami.platform.Services;
import net.minecraft.client.ClientRecipeBook;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.recipebook.RecipeCollection;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.StackedContents;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.RecipeBookMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;

/**
 * Determines craftable items using vanilla Minecraft's {@link ClientRecipeBook}
 * and {@link StackedContents}, with no dependency on EMI or JEI.
 */
public class VanillaCraftablesService {
    private static final int CRAFTING_GRID_WIDTH = 3;
    private static final int CRAFTING_GRID_HEIGHT = 3;

    private static List<ItemStack> cached = List.of();
    private static int lastInventoryVersion = -1;
    private static long lastMenuSignature = Long.MIN_VALUE;

    private VanillaCraftablesService() {
    }

    public static List<ItemStack> getCraftables() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null) return List.of();

        AbstractContainerMenu menu = mc.player.containerMenu;
        int version = mc.player.getInventory().getTimesChanged();
        long menuSignature = menuContentsSignature(menu);
        if (version == lastInventoryVersion && menuSignature == lastMenuSignature) return cached;
        lastInventoryVersion = version;
        lastMenuSignature = menuSignature;

        ClientRecipeBook recipeBook = mc.player.getRecipeBook();
        List<RecipeCollection> collections = recipeBook.getCollections();
        if (collections.isEmpty()) return cached = List.of();

        StackedContents stackedContents = new StackedContents();
        mc.player.getInventory().fillStackedContents(stackedContents);
        accountMenuContents(stackedContents, menu);

        var registryAccess = mc.level.registryAccess();
        List<ItemStack> result = new ArrayList<>();

        for (RecipeCollection collection : collections) {
            collection.canCraft(stackedContents, CRAFTING_GRID_WIDTH, CRAFTING_GRID_HEIGHT, recipeBook);
            if (!collection.hasCraftable()) continue;

            List<?> craftable = collection.getRecipes(true);
            if (craftable.isEmpty()) continue;

            ItemStack output = Services.PLATFORM.getRecipeResultItem(craftable.get(0), registryAccess);
            if (output.isEmpty()) continue;

            result.add(output.copy());
        }

        cached = result;
        return result;
    }

    private static void accountMenuContents(StackedContents stackedContents, AbstractContainerMenu menu) {
        if (menu == null) {
            return;
        }
        if (menu instanceof RecipeBookMenu recipeBookMenu) {
            recipeBookMenu.fillCraftSlotsStackedContents(stackedContents);
            return;
        }
        for (Slot slot : menu.slots) {
            if (shouldAccountOpenContainerSlot(slot)) {
                stackedContents.accountStack(slot.getItem());
            }
        }
    }

    private static long menuContentsSignature(AbstractContainerMenu menu) {
        long signature = 0xcbf29ce484222325L;
        if (menu == null) {
            return signature;
        }
        signature = mix(signature, menu.containerId);
        signature = mix(signature, menu instanceof RecipeBookMenu ? 1 : 0);
        for (Slot slot : menu.slots) {
            ItemStack stack = slot.getItem();
            boolean empty = stack.isEmpty();
            boolean accountable = CraftableSlotPolicy.shouldAccountOpenContainerSlot(
                    slot.isActive(),
                    slot.container instanceof Inventory,
                    empty,
                    !empty && slot.mayPlace(stack)
            );
            signature = mix(signature, accountable ? 1 : 0);
            if (!accountable) {
                continue;
            }
            signature = mix(signature, System.identityHashCode(stack.getItem()));
            signature = mix(signature, stack.getCount());
        }
        return signature;
    }

    private static boolean shouldAccountOpenContainerSlot(Slot slot) {
        if (slot == null) {
            return false;
        }
        ItemStack stack = slot.getItem();
        boolean empty = stack.isEmpty();
        return CraftableSlotPolicy.shouldAccountOpenContainerSlot(
                slot.isActive(),
                slot.container instanceof Inventory,
                empty,
                !empty && slot.mayPlace(stack)
        );
    }

    private static long mix(long signature, int value) {
        signature ^= value;
        return signature * 0x100000001b3L;
    }
}
