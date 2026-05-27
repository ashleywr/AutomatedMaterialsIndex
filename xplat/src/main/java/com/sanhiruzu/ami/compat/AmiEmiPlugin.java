package com.sanhiruzu.ami.compat;

import com.sanhiruzu.ami.client.InventoryOverlayHandler;
import com.sanhiruzu.ami.client.overlay.WidgetBounds;
import dev.emi.emi.api.EmiEntrypoint;
import dev.emi.emi.api.EmiPlugin;
import dev.emi.emi.api.EmiRegistry;
import dev.emi.emi.api.widget.Bounds;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;

import java.util.List;

import com.sanhiruzu.ami.AmiCore;
@EmiEntrypoint
public class AmiEmiPlugin implements EmiPlugin {

    @Override
    public void register(EmiRegistry registry) {
        // Reserve AMI panel areas so EMI doesn't render its item list into them.
        // We intentionally do NOT use a screen suppressor — that would also block
        // the recipe view from appearing when the user clicks an item.
        // EMI input is suppressed separately via EmiScreenManagerMixin so visible
        // EMI content in non-excluded areas won't steal clicks.
        registry.addGenericExclusionArea((screen, consumer) -> {
            if (!(screen instanceof AbstractContainerScreen<?>)
                    && !screen.getClass().getName().equals("dev.emi.emi.screen.RecipeScreen")) {
                return;
            }

            if (!InventoryOverlayHandler.isAmiEnabled()) {
                return;
            }

            var manager = InventoryOverlayHandler.getManager();
            if (!manager.isPanelVisible()) return;

            List<WidgetBounds> amiBounds = manager.getExclusionBounds();
            for (WidgetBounds b : amiBounds) {
                consumer.accept(new Bounds(b.x(), b.y(), b.width(), b.height()));
            }
        });
    }
}
