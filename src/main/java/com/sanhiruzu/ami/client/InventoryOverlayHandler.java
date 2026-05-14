package com.sanhiruzu.ami.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ScreenEvent;
import net.neoforged.neoforge.client.event.InputEvent;

import com.sanhiruzu.ami.AMI;
import com.sanhiruzu.ami.AMIConfig;
import com.sanhiruzu.ami.index.AMIIndex;
import com.sanhiruzu.ami.index.IndexCategory;
import com.sanhiruzu.ami.index.MaterialEntry;
import com.sanhiruzu.ami.index.WorldAtlasIndex;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@EventBusSubscriber(modid = AMI.MODID, value = Dist.CLIENT)
public class InventoryOverlayHandler {
    private static AtlasGridWidget gridWidget;
    private static WorldAtlasIndex.AtlasType currentType = null; // null for items

    @SubscribeEvent
    static void onScreenRenderPost(ScreenEvent.Render.Post event) {
        if (!AMIConfig.ENABLE_AUTO_INDEXING.get()) return;

        if (!(event.getScreen() instanceof AbstractContainerScreen<?> containerScreen)) return;

        // Calculate available space on the right
        int guiLeft = containerScreen.getGuiLeft();
        int guiTop = containerScreen.getGuiTop();
        int xSize = containerScreen.getXSize();
        int ySize = containerScreen.getYSize();
        int screenWidth = event.getScreen().width;

        int margin = 10;
        int panelX = guiLeft + xSize + 5;
        int panelY = guiTop;
        int panelWidth = screenWidth - panelX - margin;
        int panelHeight = ySize;

        // Ensure minimum width for the panel
        if (panelWidth < 60) return;

        if (gridWidget == null) {
            gridWidget = new AtlasGridWidget(panelX, panelY, panelWidth, panelHeight);
            refreshEntries();
        }

        gridWidget.updateLayout(panelX, panelY, panelWidth, panelHeight);
        
        // Push pose to ensure correct Z-level
        event.getGuiGraphics().pose().pushPose();
        event.getGuiGraphics().pose().translate(0, 0, 100);
        gridWidget.render(event.getGuiGraphics(), event.getMouseX(), event.getMouseY(), event.getPartialTick());
        event.getGuiGraphics().pose().popPose();
    }

    private static void refreshEntries() {
        if (gridWidget == null) return;
        
        try {
            if (currentType == null) {
                List<ItemStack> items = new ArrayList<>();
                Map<String, List<MaterialEntry>> categoryIndex = AMIIndex.getInstance().getCategoryIndex(IndexCategory.BY_MOD);
                if (categoryIndex != null) {
                    // Use a copy to avoid CME during indexing
                    List<List<MaterialEntry>> values = new ArrayList<>(categoryIndex.values());
                    for (List<MaterialEntry> entries : values) {
                        for (MaterialEntry entry : entries) {
                            if (entry != null && entry.item() != null) {
                                items.add(new ItemStack(entry.item()));
                            }
                        }
                    }
                }
                gridWidget.setEntries(items);
            } else {
                List<WorldAtlasIndex.AtlasEntry> entries = WorldAtlasIndex.getInstance().getEntries(currentType);
                if (entries != null) {
                    gridWidget.setEntries(new ArrayList<>(entries));
                }
            }
        } catch (Exception e) {
            AMI.LOGGER.error("Error refreshing AMI entries", e);
        }
    }

    @SubscribeEvent
    static void onKeyInput(InputEvent.Key event) {
        // Only consume keys if we are in a container screen
        if (Minecraft.getInstance().screen instanceof AbstractContainerScreen<?>) {
            if (AMIKeyMappings.CYCLE_ATLAS.consumeClick()) {
                cycleType();
                refreshEntries();
            }
        }
    }

    private static void cycleType() {
        if (currentType == null) currentType = WorldAtlasIndex.AtlasType.BIOME;
        else if (currentType == WorldAtlasIndex.AtlasType.BIOME) currentType = WorldAtlasIndex.AtlasType.STRUCTURE;
        else if (currentType == WorldAtlasIndex.AtlasType.STRUCTURE) currentType = WorldAtlasIndex.AtlasType.ENTITY;
        else currentType = null;
    }

    @SubscribeEvent
    static void onMouseScroll(InputEvent.MouseScrollingEvent event) {
        if (!AMIConfig.ENABLE_AUTO_INDEXING.get() || gridWidget == null) return;
        
        // Only scroll if we are in a container screen
        if (!(Minecraft.getInstance().screen instanceof AbstractContainerScreen<?>)) return;

        double mouseX = event.getMouseX();
        double mouseY = event.getMouseY();

        if (gridWidget.isMouseOver(mouseX, mouseY)) {
            if (gridWidget.mouseScrolled(mouseX, mouseY, event.getScrollDeltaY())) {
                event.setCanceled(true);
            }
        }
    }
}
