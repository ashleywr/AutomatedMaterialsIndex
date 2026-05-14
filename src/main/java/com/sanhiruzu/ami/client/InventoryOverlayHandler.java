package com.sanhiruzu.ami.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModList;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.InputEvent;
import net.neoforged.neoforge.client.event.ScreenEvent;

import com.sanhiruzu.ami.AMI;
import com.sanhiruzu.ami.AMIConfig;
import com.sanhiruzu.ami.index.AMIIndex;
import com.sanhiruzu.ami.index.IndexCategory;
import com.sanhiruzu.ami.index.MaterialEntry;
import com.sanhiruzu.ami.index.WorldAtlasIndex;

import java.util.ArrayList;
import java.util.List;

@EventBusSubscriber(modid = AMI.MODID, value = Dist.CLIENT)
public class InventoryOverlayHandler {
    // Checked once at class load — mod list is fixed after startup
    private static final boolean RECIPE_VIEWER_PRESENT =
            ModList.get().isLoaded("emi") || ModList.get().isLoaded("jei");

    private static AtlasGridWidget gridWidget;
    // When a recipe viewer is present, start in atlas mode (items are covered by EMI/JEI)
    private static WorldAtlasIndex.AtlasType atlasType =
            RECIPE_VIEWER_PRESENT ? WorldAtlasIndex.AtlasType.values()[0] : null;
    private static int lastKnownItemCount = -1;

    @SubscribeEvent
    static void onScreenRenderPost(ScreenEvent.Render.Post event) {
        if (!AMIConfig.ENABLE_AUTO_INDEXING.get()) return;
        if (!(event.getScreen() instanceof AbstractContainerScreen<?> containerScreen)) return;

        try {
            int panelY = containerScreen.getGuiTop();
            int panelHeight = containerScreen.getYSize();

            boolean goLeft = switch (AMIConfig.PANEL_SIDE.get()) {
                case LEFT  -> true;
                case RIGHT -> false;
                case AUTO  -> RECIPE_VIEWER_PRESENT;
            };

            int panelX, panelWidth;
            int widthOverride = AMIConfig.PANEL_WIDTH_OVERRIDE.get();
            if (goLeft) {
                int available = containerScreen.getGuiLeft() - 12;
                panelWidth = widthOverride > 0 ? widthOverride : Math.min(120, available);
                panelX = containerScreen.getGuiLeft() - panelWidth - 6;
            } else {
                panelX = containerScreen.getGuiLeft() + containerScreen.getXSize() + 6;
                int available = event.getScreen().width - panelX - 6;
                panelWidth = widthOverride > 0 ? widthOverride : available;
            }

            if (panelWidth < 60) return;

            if (gridWidget == null) {
                gridWidget = new AtlasGridWidget(panelX, panelY, panelWidth, panelHeight);
                refreshEntries();
            }

            checkAndRefreshIfStale();

            gridWidget.updateLayout(panelX, panelY, panelWidth, panelHeight);

            event.getGuiGraphics().pose().pushPose();
            event.getGuiGraphics().pose().translate(0, 0, 100);
            gridWidget.render(event.getGuiGraphics(), event.getMouseX(), event.getMouseY(), event.getPartialTick());
            event.getGuiGraphics().pose().popPose();
        } catch (Exception e) {
            AMI.LOGGER.error("AMI overlay render failed", e);
        }
    }

    @SubscribeEvent
    static void onKeyInput(InputEvent.Key event) {
        if (!(Minecraft.getInstance().screen instanceof AbstractContainerScreen<?>)) return;

        if (AMIKeyMappings.CYCLE_ATLAS.consumeClick()) {
            WorldAtlasIndex.AtlasType[] types = WorldAtlasIndex.AtlasType.values();
            if (RECIPE_VIEWER_PRESENT) {
                // Items mode not available — cycle only through atlas types
                atlasType = atlasType.next();
            } else {
                atlasType = (atlasType == null) ? types[0]
                          : (atlasType.ordinal() == types.length - 1) ? null
                          : atlasType.next();
            }
            refreshEntries();
        }
    }

    @SubscribeEvent
    static void onScreenMouseClick(ScreenEvent.MouseButtonPressed.Pre event) {
        if (!AMIConfig.ENABLE_AUTO_INDEXING.get() || gridWidget == null) return;
        if (!(event.getScreen() instanceof AbstractContainerScreen<?>)) return;
        if (!gridWidget.isMouseOver(event.getMouseX(), event.getMouseY())) return;

        // Scrollbar takes priority over entry clicks
        if (gridWidget.mouseClickedScrollbar(event.getMouseX(), event.getMouseY(), event.getButton())) {
            event.setCanceled(true);
        } else if (gridWidget.mouseClicked(event.getMouseX(), event.getMouseY(), event.getButton())) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    static void onScreenMouseDragged(ScreenEvent.MouseDragged.Pre event) {
        if (gridWidget == null) return;
        if (!(event.getScreen() instanceof AbstractContainerScreen<?>)) return;
        if (gridWidget.mouseDragged(event.getMouseX(), event.getMouseY(),
                event.getMouseButton(), event.getDragX(), event.getDragY())) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    static void onScreenMouseRelease(ScreenEvent.MouseButtonReleased.Pre event) {
        if (gridWidget == null) return;
        if (!(event.getScreen() instanceof AbstractContainerScreen<?>)) return;
        gridWidget.stopScrollbarDrag();
    }

    @SubscribeEvent
    static void onScreenMouseScroll(ScreenEvent.MouseScrolled.Pre event) {
        if (!AMIConfig.ENABLE_AUTO_INDEXING.get() || gridWidget == null) return;
        if (!(event.getScreen() instanceof AbstractContainerScreen<?>)) return;

        if (gridWidget.isMouseOver(event.getMouseX(), event.getMouseY())) {
            gridWidget.mouseScrolled(event.getMouseX(), event.getMouseY(), event.getScrollDeltaY());
            event.setCanceled(true);
        }
    }

    private static void checkAndRefreshIfStale() {
        if (atlasType == null) {
            int currentCount = AMIIndex.getInstance().getTotalItemsIndexed();
            if (currentCount != lastKnownItemCount) {
                refreshEntries();
            }
        } else if (gridWidget.getEntryCount() == 0) {
            refreshEntries();
        }
    }

    private static void refreshEntries() {
        if (gridWidget == null) return;

        if (atlasType == null) {
            List<ItemStack> items = buildItemList();
            gridWidget.setItemEntries(items);
            gridWidget.setItemModeLabel(net.minecraft.network.chat.Component.translatable("ami.gui.items"));
            lastKnownItemCount = AMIIndex.getInstance().getTotalItemsIndexed();
        } else {
            List<WorldAtlasIndex.AtlasEntry> entries = WorldAtlasIndex.getInstance().getEntries(atlasType);
            gridWidget.setAtlasEntries(
                    entries != null ? entries : List.of(),
                    atlasType.displayName()
            );
        }

        AMI.LOGGER.debug("AMI overlay refreshed: {} entries", gridWidget.getEntryCount());
    }

    private static List<ItemStack> buildItemList() {
        List<ItemStack> items = new ArrayList<>();
        var categoryIndex = AMIIndex.getInstance().getCategoryIndex(IndexCategory.BY_MOD);
        if (categoryIndex == null) return items;

        for (List<MaterialEntry> entries : new ArrayList<>(categoryIndex.values())) {
            for (MaterialEntry entry : entries) {
                if (entry != null && entry.item() != null) {
                    items.add(new ItemStack(entry.item()));
                }
            }
        }
        return items;
    }
}
