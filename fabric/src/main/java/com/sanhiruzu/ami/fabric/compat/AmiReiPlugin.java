package com.sanhiruzu.ami.fabric.compat;

import com.sanhiruzu.ami.client.InventoryOverlayHandler;
import com.sanhiruzu.ami.client.overlay.OverlayWidgetManager;
import com.sanhiruzu.ami.client.overlay.WidgetBounds;
import me.shedaniel.math.Rectangle;
import me.shedaniel.rei.api.client.plugins.REIClientPlugin;
import me.shedaniel.rei.api.client.registry.screen.ExclusionZones;
import me.shedaniel.rei.api.client.gui.screen.DisplayScreen;
import me.shedaniel.rei.api.client.registry.screen.OverlayDecider;
import me.shedaniel.rei.api.client.registry.screen.ScreenRegistry;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.world.InteractionResult;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * REI client plugin. REI exists only on Fabric, so this plugin lives in the Fabric module and is
 * discovered via the {@code rei_client} Fabric entrypoint (lazy-loaded, so it is harmless when REI
 * is absent — same as the {@code jei_mod_plugin} entry).
 *
 * <p>Suppression is done with an {@link OverlayDecider} that denies REI's overlay entirely while AMI is
 * the active layer — the same "fully hide the other viewer" behavior AMI applies to JEI/EMI. Exclusion
 * zones alone only reserve AMI's panel rectangles, which let REI squeeze its item list into whatever
 * screen space is left over (e.g. a sliver in the corner), so they are not sufficient on their own; the
 * decider is the real fix and the exclusion zones remain as a harmless refinement. When AMI is toggled
 * off (REI selected as the active viewer) the decider passes, so REI renders normally — and we never
 * suppress the recipe view itself.
 */
@Environment(EnvType.CLIENT)
public class AmiReiPlugin implements REIClientPlugin {

    @Override
    public void registerScreens(ScreenRegistry registry) {
        registry.registerDecider(new AmiOverlayDecider());
    }

    @Override
    public void registerExclusionZones(ExclusionZones zones) {
        // Register over the container screen (and any subclass) so AMI's overlay over inventory-like
        // screens is excluded from REI's overlay; mirrors the AbstractContainerScreen guard in AmiEmiPlugin.
        zones.register(AbstractContainerScreen.class, screen -> amiExclusionZones((Screen) screen));
    }

    /**
     * Denies REI's overlay on AMI screens while AMI is active (returns {@code FAIL}); otherwise passes so
     * REI decides normally. High priority so it is consulted before REI's own deciders.
     */
    private static final class AmiOverlayDecider implements OverlayDecider {
        @Override
        public <R extends Screen> boolean isHandingScreen(Class<R> screen) {
            // Handle inventory-like screens (AMI's overlay) and REI's own recipe/display screens, so that
            // while AMI is active we suppress REI's item-list overlay there too — leaving the recipe content
            // in the middle and AMI's panels on the sides.
            return AbstractContainerScreen.class.isAssignableFrom(screen)
                    || DisplayScreen.class.isAssignableFrom(screen);
        }

        @Override
        public <R extends Screen> InteractionResult shouldScreenBeOverlaid(R screen) {
            return InventoryOverlayHandler.isAmiEnabled() && InventoryOverlayHandler.isAmiScreen(screen)
                    ? InteractionResult.FAIL
                    : InteractionResult.PASS;
        }

        @Override
        public double getPriority() {
            return 10.0;
        }
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
