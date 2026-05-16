package com.sanhiruzu.ami.compat;

import com.sanhiruzu.ami.AMIConfig;
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
        if (AMIConfig.SUPPRESS_RECIPE_VIEWERS.get()) {
            registry.addGenericExclusionArea((screen, consumer) -> {
                if (!(screen instanceof AbstractContainerScreen<?>)) {
                    return;
                }

                if (!InventoryOverlayHandler.isAmiEnabled()) {
                    return;
                }

                WidgetBounds bounds = InventoryOverlayHandler.getManager().getResultsBounds();
                if (bounds != null && bounds.width() > 0) {
                    consumer.accept(new Bounds(bounds.x(), bounds.y(), bounds.width(), bounds.height()));
                }
            });
        }
    }
}
