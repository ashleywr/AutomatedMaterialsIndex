package com.sanhiruzu.ami.compat;

import com.sanhiruzu.ami.config.AmiConfig;
import com.sanhiruzu.ami.client.InventoryOverlayHandler;
import com.sanhiruzu.ami.client.overlay.WidgetBounds;
import dev.emi.emi.api.EmiEntrypoint;
import dev.emi.emi.api.EmiPlugin;
import dev.emi.emi.api.EmiRegistry;
import dev.emi.emi.api.widget.Bounds;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;

@EmiEntrypoint
public class AmiEmiPlugin implements EmiPlugin {

    @Override
    public void register(EmiRegistry registry) {
        if (AmiConfig.suppressRecipeViewers) {
            registry.addGenericExclusionArea((screen, consumer) -> {
                if (!(screen instanceof AbstractContainerScreen<?>)) {
                    return;
                }

                if (!InventoryOverlayHandler.isAmiEnabled()) {
                    return;
                }

                var manager = InventoryOverlayHandler.getManager();
                if (!manager.isPanelVisible()) return;

                // Results Panel
                WidgetBounds bounds = manager.getResultsBounds();
                if (bounds != null && bounds.width() > 0) {
                    consumer.accept(new Bounds(bounds.x(), bounds.y(), bounds.width(), bounds.height()));
                }

                // Left Sidebars
                addPanelBounds(manager.getLeftPanel(), consumer);
                addPanelBounds(manager.getLeftPanelSecondary(), consumer);

                // Right Sidebars
                addPanelBounds(manager.getRightPanelPrimary(), consumer);
                addPanelBounds(manager.getRightPanelSecondary(), consumer);

                // Search Bar
                var searchBar = manager.getSearchBar();
                if (searchBar != null && searchBar.visible) {
                    var b = searchBar.getBounds();
                    if (b != null) {
                        consumer.accept(new Bounds(b.x(), b.y(), b.width(), b.height()));
                    }
                }
            });
        }
    }

    private void addPanelBounds(com.sanhiruzu.ami.client.overlay.SidebarPanelWidget panel, java.util.function.Consumer<Bounds> consumer) {
        if (panel != null && panel.visible) {
            consumer.accept(new Bounds(panel.getX(), panel.getY(), panel.getWidth(), panel.getHeight()));
        }
    }
}
