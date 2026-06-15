package com.sanhiruzu.ami.fabric.compat;

import com.sanhiruzu.ami.client.InventoryOverlayHandler;
import com.sanhiruzu.ami.client.overlay.OverlayWidgetManager;
import com.sanhiruzu.ami.client.overlay.WidgetBounds;
import me.shedaniel.math.Rectangle;
import me.shedaniel.rei.api.client.plugins.REIClientPlugin;
import me.shedaniel.rei.api.client.registry.screen.ExclusionZones;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * REI client plugin. REI exists only on Fabric, so this plugin lives in the Fabric module and is
 * discovered via the {@code rei_client} Fabric entrypoint (lazy-loaded, so it is harmless when REI
 * is absent — same as the {@code jei_mod_plugin} entry).
 *
 * <p>Mirrors {@code AmiEmiPlugin}: it reserves AMI's panel bounds as exclusion zones so REI does not
 * render its item list into them (and, because {@code ExclusionZones extends OverlayDecider}, REI also
 * stops handling input over those areas). We intentionally do NOT register a screen suppressor — that
 * would block the recipe view from appearing when the user clicks an AMI item.
 */
@Environment(EnvType.CLIENT)
public class AmiReiPlugin implements REIClientPlugin {

    @Override
    public void registerExclusionZones(ExclusionZones zones) {
        // Register over the container screen (and any subclass) so AMI's overlay over inventory-like
        // screens is excluded from REI's overlay; mirrors the AbstractContainerScreen guard in AmiEmiPlugin.
        zones.register(AbstractContainerScreen.class, screen -> amiExclusionZones((Screen) screen));
    }

    private static Collection<Rectangle> amiExclusionZones(Screen screen) {
        if (!(screen instanceof AbstractContainerScreen<?>)) {
            return List.of();
        }
        if (!InventoryOverlayHandler.isAmiEnabled()) {
            return List.of();
        }

        OverlayWidgetManager manager = InventoryOverlayHandler.getManager();
        if (manager == null || !manager.isPanelVisible()) {
            return List.of();
        }

        List<WidgetBounds> amiBounds = manager.getExclusionBounds();
        List<Rectangle> result = new ArrayList<>(amiBounds.size());
        for (WidgetBounds b : amiBounds) {
            result.add(new Rectangle(b.x(), b.y(), b.width(), b.height()));
        }
        return result;
    }
}
