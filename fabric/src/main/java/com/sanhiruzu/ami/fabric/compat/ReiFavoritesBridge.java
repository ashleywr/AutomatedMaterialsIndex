package com.sanhiruzu.ami.fabric.compat;

import com.sanhiruzu.ami.fabric.AmiFabric;
import me.shedaniel.math.Point;
import me.shedaniel.rei.api.client.registry.screen.ScreenRegistry;
import me.shedaniel.rei.api.common.entry.EntryStack;
import me.shedaniel.rei.api.common.entry.type.VanillaEntryTypes;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.Minecraft;
import net.minecraft.client.MouseHandler;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.world.item.ItemStack;

/**
 * Reads the REI {@link EntryStack} currently under the cursor and converts it to a vanilla
 * {@link ItemStack}. REI exists only on Fabric, so this bridge lives in the Fabric module and is only
 * reached behind an {@code isModLoaded("roughlyenoughitems")} guard (via {@code FabricPlatformHelper}),
 * so the {@code me.shedaniel.rei.*} types are never linked when REI is absent.
 *
 * <p>Mirrors {@code EmiFavoritesBridge#removeHoveredFavorite}: wrapped in try/catch so REI internal
 * changes can never crash AMI's favorite-key handling.
 */
@Environment(EnvType.CLIENT)
public final class ReiFavoritesBridge {

    private ReiFavoritesBridge() {
    }

    /**
     * Returns the vanilla {@link ItemStack} under the cursor in REI's window (item list, favorites, or
     * recipe display), or {@link ItemStack#EMPTY} if nothing item-like is hovered.
     *
     * <p>Uses REI's public {@code ScreenRegistry.getFocusedStack(screen, mouse)} — the same hook REI's
     * own focused-stack consumers use — then keeps only {@code VanillaEntryTypes.ITEM} entries (skips
     * fluids/other entry types) and copies the value so the favorites store never aliases REI's stack.
     */
    public static ItemStack getHoveredStack() {
        try {
            Minecraft mc = Minecraft.getInstance();
            Screen screen = mc.screen;
            if (screen == null) {
                return ItemStack.EMPTY;
            }
            ScreenRegistry registry = ScreenRegistry.getInstance();
            if (registry == null) {
                return ItemStack.EMPTY;
            }

            // REI scales the mouse to GUI-space the same way vanilla screens do.
            MouseHandler mouse = mc.mouseHandler;
            double guiScale = mc.getWindow().getGuiScale();
            int mouseX = (int) (mouse.xpos() / guiScale);
            int mouseY = (int) (mouse.ypos() / guiScale);

            EntryStack<?> focused = registry.getFocusedStack(screen, new Point(mouseX, mouseY));
            if (focused == null || focused.isEmpty()) {
                return ItemStack.EMPTY;
            }
            if (!focused.getType().equals(VanillaEntryTypes.ITEM)) {
                return ItemStack.EMPTY;
            }
            Object value = focused.getValue();
            if (value instanceof ItemStack stack && !stack.isEmpty()) {
                return stack.copy();
            }
            return ItemStack.EMPTY;
        } catch (RuntimeException | LinkageError e) {
            AmiFabric.LOGGER.warn("REI hovered-stack lookup failed", e);
            return ItemStack.EMPTY;
        }
    }
}
