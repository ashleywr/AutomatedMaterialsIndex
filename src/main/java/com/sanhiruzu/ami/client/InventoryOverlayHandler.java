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
    private static WorldAtlasIndex.AtlasType currentType = null; // null means items mode
    private static int lastKnownItemCount = -1;

    @SubscribeEvent
    static void onScreenRenderPost(ScreenEvent.Render.Post event) {
        if (!AMIConfig.ENABLE_AUTO_INDEXING.get()) return;
        if (!(event.getScreen() instanceof AbstractContainerScreen<?> containerScreen)) return;

        int guiLeft = containerScreen.getGuiLeft();
        int guiTop = containerScreen.getGuiTop();
        int xSize = containerScreen.getXSize();
        int ySize = containerScreen.getYSize();
        int screenWidth = event.getScreen().width;

        int margin = 6;
        int panelX = guiLeft + xSize + margin;
        int panelY = guiTop;
        int panelWidth = screenWidth - panelX - margin;
        int panelHeight = ySize;

        if (panelWidth < 60) return;

        if (gridWidget == null) {
            gridWidget = new AtlasGridWidget(panelX, panelY, panelWidth, panelHeight);
            gridWidget.setModeLabel(currentModeLabel());
            refreshEntries();
        }

        // Refresh if index populated after widget was created
        checkAndRefreshIfStale();

        gridWidget.updateLayout(panelX, panelY, panelWidth, panelHeight);

        event.getGuiGraphics().pose().pushPose();
        event.getGuiGraphics().pose().translate(0, 0, 100);
        gridWidget.render(event.getGuiGraphics(), event.getMouseX(), event.getMouseY(), event.getPartialTick());
        event.getGuiGraphics().pose().popPose();
    }

    private static void checkAndRefreshIfStale() {
        if (currentType == null) {
            int currentItemCount = AMIIndex.getInstance().getTotalItemsIndexed();
            if (currentItemCount != lastKnownItemCount) {
                lastKnownItemCount = currentItemCount;
                refreshEntries();
            }
        } else if (gridWidget.getEntryCount() == 0) {
            refreshEntries();
        }
    }

    private static void refreshEntries() {
        if (gridWidget == null) return;

        try {
            if (currentType == null) {
                List<ItemStack> items = new ArrayList<>();
                Map<String, List<MaterialEntry>> categoryIndex = AMIIndex.getInstance().getCategoryIndex(IndexCategory.BY_MOD);
                if (categoryIndex != null) {
                    for (List<MaterialEntry> entries : new ArrayList<>(categoryIndex.values())) {
                        for (MaterialEntry entry : entries) {
                            if (entry != null && entry.item() != null) {
                                items.add(new ItemStack(entry.item()));
                            }
                        }
                    }
                }
                gridWidget.setEntries(items);
                lastKnownItemCount = AMIIndex.getInstance().getTotalItemsIndexed();
            } else {
                List<WorldAtlasIndex.AtlasEntry> entries = WorldAtlasIndex.getInstance().getEntries(currentType);
                gridWidget.setEntries(entries != null ? new ArrayList<>(entries) : new ArrayList<>());
            }

            gridWidget.setModeLabel(currentModeLabel());
            AMI.LOGGER.debug("AMI overlay refreshed: {} {} entries", gridWidget.getEntryCount(), currentModeLabel());
        } catch (Exception e) {
            AMI.LOGGER.error("Error refreshing AMI entries", e);
        }
    }

    private static String currentModeLabel() {
        if (currentType == null) return "Items";
        return switch (currentType) {
            case BIOME -> "Biomes";
            case STRUCTURE -> "Structures";
            case ENTITY -> "Entities";
        };
    }

    @SubscribeEvent
    static void onKeyInput(InputEvent.Key event) {
        if (!(Minecraft.getInstance().screen instanceof AbstractContainerScreen<?>)) return;

        if (AMIKeyMappings.CYCLE_ATLAS.consumeClick()) {
            cycleType();
            refreshEntries();
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
        if (!(Minecraft.getInstance().screen instanceof AbstractContainerScreen<?>)) return;

        if (gridWidget.isMouseOver(event.getMouseX(), event.getMouseY())) {
            if (gridWidget.mouseScrolled(event.getMouseX(), event.getMouseY(), event.getScrollDeltaY())) {
                event.setCanceled(true);
            }
        }
    }
}
