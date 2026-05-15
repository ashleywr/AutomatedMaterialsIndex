package com.sanhiruzu.ami.compat;

import dev.emi.emi.api.EmiEntrypoint;
import dev.emi.emi.api.EmiPlugin;
import dev.emi.emi.api.EmiRegistry;
import dev.emi.emi.api.EmiExclusionArea;
import dev.emi.emi.api.widget.Bounds;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;

import com.sanhiruzu.ami.AMI;
import com.sanhiruzu.ami.AMIConfig;
import com.sanhiruzu.ami.client.InventoryOverlayHandler;
import com.sanhiruzu.ami.client.overlay.WidgetBounds;

@EmiEntrypoint
public class AmiEmiPlugin implements EmiPlugin {

    @Override
    public void register(EmiRegistry registry) {
        AMI.LOGGER.info("AmiEmiPlugin.register() called - EMI integration active");

        if (AMIConfig.SUPPRESS_RECIPE_VIEWERS.get()) {
            // Register exclusion areas where AMI renders
            registry.addGenericExclusionArea((screen, consumer) -> {
                if (!(screen instanceof AbstractContainerScreen<?>)) {
                    return;
                }

                WidgetBounds bounds = InventoryOverlayHandler.getManager().getResultsBounds();

                // Use actual panel bounds if available, otherwise use fallback right-side coverage
                if (bounds != null && bounds.width() > 0) {
                    consumer.accept(new Bounds(bounds.x(), bounds.y(), bounds.width(), bounds.height()));
                } else {
                    // Fallback: exclude entire right half of screen if panel isn't ready
                    int screenMid = screen.width / 2;
                    consumer.accept(new Bounds(screenMid, 0, screen.width - screenMid, screen.height));
                }
            });
        }
    }
}
