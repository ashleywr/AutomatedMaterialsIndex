package com.sanhiruzu.ami.compat;

import com.sanhiruzu.ami.platform.Services;
import net.minecraft.client.ClientRecipeBook;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.recipebook.RecipeCollection;
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

    static void accountMenuContents(StackedContents stackedContents, AbstractContainerMenu menu) {
        if (menu == null) {
            return;
        }
        if (CraftablesScopePolicy.shouldAccountMenuContents(menu instanceof RecipeBookMenu)
                && menu instanceof RecipeBookMenu recipeBookMenu) {
            recipeBookMenu.fillCraftSlotsStackedContents(stackedContents);
        }
    }

    private static long menuContentsSignature(AbstractContainerMenu menu) {
        long signature = 0xcbf29ce484222325L;
        if (menu == null) {
            return signature;
        }
        signature = mix(signature, menu.containerId);
        if (!CraftablesScopePolicy.shouldAccountMenuContents(menu instanceof RecipeBookMenu)) {
            return mix(signature, 0);
        }
        signature = mix(signature, 1);
        for (Slot slot : menu.slots) {
            if (!shouldAccountRecipeBookSlot(slot)) {
                signature = mix(signature, 0);
                continue;
            }
            signature = mix(signature, 1);
            ItemStack stack = slot.getItem();
            signature = mix(signature, System.identityHashCode(stack.getItem()));
            signature = mix(signature, stack.getCount());
        }
        return signature;
    }

    private static boolean shouldAccountRecipeBookSlot(Slot slot) {
        if (slot == null) {
            return false;
        }
        ItemStack stack = slot.getItem();
        return CraftablesScopePolicy.shouldAccountRecipeBookSlot(slot.isActive(), stack.isEmpty(), slot.mayPlace(stack));
    }

    private static long mix(long signature, int value) {
        signature ^= value;
        return signature * 0x100000001b3L;
    }
}
