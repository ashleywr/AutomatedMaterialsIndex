package com.sanhiruzu.ami.fabric.compat;

import com.sanhiruzu.ami.fabric.AmiFabric;
import me.shedaniel.rei.api.common.entry.EntryStack;
import me.shedaniel.rei.api.common.util.EntryStacks;
import me.shedaniel.rei.api.client.view.ViewSearchBuilder;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.world.item.ItemStack;

/**
 * Direct REI API calls. REI exists only on Fabric, so this bridge lives in the Fabric module and is
 * only reached behind an {@code isModLoaded("roughlyenoughitems")} guard (via FabricPlatformHelper),
 * so the class is never loaded when REI is absent.
 *
 * <p>Mirrors the robustness of {@code EmiRecipeBridge}/{@code JeiRecipeBridge}: null/empty-safe and
 * wrapped in try/catch so REI internal changes can never crash AMI's click handling.
 */
@Environment(EnvType.CLIENT)
public final class ReiRecipeBridge {

    private ReiRecipeBridge() {
    }

    /**
     * Routes to {@link #openUses(ItemStack)} when {@code uses}, else {@link #openRecipes(ItemStack)}.
     * Public so {@code FabricPlatformHelper} (a different package) can reach it behind its
     * {@code isModLoaded("roughlyenoughitems")} guard.
     */
    public static boolean open(ItemStack stack, boolean uses) {
        return uses ? openUses(stack) : openRecipes(stack);
    }

    /** Open REI's recipe view for what produces {@code stack}. */
    static boolean openRecipes(ItemStack stack) {
        if (stack == null || stack.isEmpty()) {
            return false;
        }
        try {
            EntryStack<ItemStack> entry = EntryStacks.of(stack);
            return ViewSearchBuilder.builder().addRecipesFor(entry).open();
        } catch (RuntimeException | LinkageError e) {
            AmiFabric.LOGGER.warn("REI openRecipes failed", e);
            return false;
        }
    }

    /** Open REI's usage view for what consumes {@code stack}. */
    static boolean openUses(ItemStack stack) {
        if (stack == null || stack.isEmpty()) {
            return false;
        }
        try {
            EntryStack<ItemStack> entry = EntryStacks.of(stack);
            return ViewSearchBuilder.builder().addUsagesFor(entry).open();
        } catch (RuntimeException | LinkageError e) {
            AmiFabric.LOGGER.warn("REI openUses failed", e);
            return false;
        }
    }
}
