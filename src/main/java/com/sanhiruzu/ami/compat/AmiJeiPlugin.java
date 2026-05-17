package com.sanhiruzu.ami.compat;

import com.sanhiruzu.ami.AMI;
import com.sanhiruzu.ami.client.InventoryOverlayHandler;
import com.sanhiruzu.ami.client.overlay.WidgetBounds;
import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.runtime.IJeiRuntime;
import net.minecraft.resources.ResourceLocation;

@JeiPlugin
public class AmiJeiPlugin implements IModPlugin {
    private static final ResourceLocation UID = ResourceLocation.fromNamespaceAndPath(AMI.MODID, "plugin");

    @Override
    public ResourceLocation getPluginUid() {
        return UID;
    }

    @Override
    public void registerGuiHandlers(mezz.jei.api.registration.IGuiHandlerRegistration registration) {
        registration.addGlobalGuiHandler(new mezz.jei.api.gui.handlers.IGlobalGuiHandler() {
            @Override
            public java.util.Collection<net.minecraft.client.renderer.Rect2i> getGuiExtraAreas() {
                if (!com.sanhiruzu.ami.config.AmiConfig.suppressRecipeViewers) return java.util.Collections.emptyList();
                if (!InventoryOverlayHandler.isAmiEnabled()) return java.util.Collections.emptyList();
                
                var manager = InventoryOverlayHandler.getManager();
                if (!manager.isPanelVisible()) return java.util.Collections.emptyList();

                java.util.List<net.minecraft.client.renderer.Rect2i> areas = new java.util.ArrayList<>();
                
                // Results Panel
                WidgetBounds bounds = manager.getResultsBounds();
                if (bounds != null && bounds.width() > 0) {
                    areas.add(new net.minecraft.client.renderer.Rect2i(bounds.x(), bounds.y(), bounds.width(), bounds.height()));
                }

                // Sidebars
                addPanelArea(manager.getLeftPanel(), areas);
                addPanelArea(manager.getLeftPanelSecondary(), areas);
                addPanelArea(manager.getRightPanelPrimary(), areas);
                addPanelArea(manager.getRightPanelSecondary(), areas);

                // Search Bar
                var searchBar = manager.getSearchBar();
                if (searchBar != null && searchBar.visible) {
                    var b = searchBar.getBounds();
                    if (b != null) areas.add(new net.minecraft.client.renderer.Rect2i(b.x(), b.y(), b.width(), b.height()));
                }

                return areas;
            }

            private void addPanelArea(com.sanhiruzu.ami.client.overlay.SidebarPanelWidget panel, java.util.List<net.minecraft.client.renderer.Rect2i> areas) {
                if (panel != null && panel.visible) {
                    areas.add(new net.minecraft.client.renderer.Rect2i(panel.getX(), panel.getY(), panel.getWidth(), panel.getHeight()));
                }
            }
        });
    }

    @Override
    public void onRuntimeAvailable(mezz.jei.api.runtime.IJeiRuntime jeiRuntime) {
        JeiRuntimeAccessor.setRuntime(jeiRuntime);
    }
}
