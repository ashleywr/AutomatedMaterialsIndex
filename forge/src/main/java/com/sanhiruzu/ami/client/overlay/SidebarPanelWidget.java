package com.sanhiruzu.ami.client.overlay;

import com.sanhiruzu.ami.api.AmiQuestsApi;
import com.sanhiruzu.ami.client.UniversalResultsPanel;
import com.sanhiruzu.ami.config.AmiConfig;
import com.sanhiruzu.ami.index.GlobalIndex;
import com.sanhiruzu.ami.index.SearchNode;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.network.chat.Component;

import java.util.List;

/**
 * A panel that displays various types of content (favorites, history, craftables) in a sidebar.
 * Reuses UniversalResultsPanel for its grid and list views.
 */
public class SidebarPanelWidget extends AbstractWidget {
    private final UniversalResultsPanel panel;
    private AmiConfig.PanelContent contentType;
    private final Runnable questRefreshListener = this::refresh;
    private boolean questListenerRegistered;

    public SidebarPanelWidget(int x, int y, int width, int height, AmiConfig.PanelContent contentType) {
        super(x, y, width, height, Component.translatable("ami.gui.sidebar." + contentType.name().toLowerCase()));
        this.contentType = contentType;
        this.panel = new UniversalResultsPanel(x, y, width, height);
        this.panel.setFavoritesPanel(true); // This tells it to use the smaller icons/sidebar style
        this.panel.setPanelTitle(titleFor(contentType));
        this.panel.setChromeOnly(contentType == AmiConfig.PanelContent.EMPTY);

        if (contentType == AmiConfig.PanelContent.LOOKUP_HISTORY) {
            com.sanhiruzu.ami.client.favorites.AmiHistoryHandler.getInstance().setOnChange(this::refresh);
        }
        updateQuestChangeListener();

        refresh();
    }

    private static Component titleFor(AmiConfig.PanelContent contentType) {
        if (contentType == AmiConfig.PanelContent.EMPTY) {
            return Component.empty();
        }
        return Component.translatable("ami.gui.sidebar." + contentType.name().toLowerCase());
    }

    public void setOnModeToggle(Runnable callback, java.util.function.BooleanSupplier activeSupplier) {
        this.panel.setOnModeToggle(callback, activeSupplier);
    }

    public void setOnCollapse(Runnable callback) {
        this.panel.setOnCollapseSidebar(callback);
    }

    public void refresh() {
        if (contentType == null || contentType == AmiConfig.PanelContent.NONE) {
            panel.setEntries(List.of());
            return;
        }
        if (contentType == AmiConfig.PanelContent.QUESTS) {
            panel.setGroupedEntries(QuestSidebarProjector.project(
                    AmiQuestsApi.getQuestGroups(),
                    AmiQuestsApi.getQuestDocuments(),
                    GlobalIndex.getInstance()::getNode
            ));
            return;
        }
        List<SearchNode> nodes = AmiSidebarSyncHandler.getNodesForContent(contentType);
        panel.setEntries(nodes);
    }

    public void updateLayout(Rect rect) {
        updateLayout(rect.x(), rect.y(), rect.w(), rect.h());
    }

    public void updateLayout(int x, int y, int width, int height) {
        this.setX(x);
        this.setY(y);
        this.width = width;
        this.height = height;
        panel.updateLayout(x, y, width, height);
    }

    public WidgetBounds getBounds() {
        return new WidgetBounds(getX(), getY(), width, height);
    }

    @Override
    protected void renderWidget(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        if (!this.visible) return;
        panel.render(g, mouseX, mouseY, partialTick);
    }

    public void renderOverlay(GuiGraphics g, int mouseX, int mouseY) {
        if (!this.visible) return;
        panel.renderOverlay(g, mouseX, mouseY);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (!this.visible) return false;
        if (!panel.isMouseOver(mouseX, mouseY) && !panel.isContextMenuOpen()) return false;
        if (panel.isMouseOver(mouseX, mouseY)) {
            panel.mouseClickedScrollbar(mouseX, mouseY, button);
        }
        return panel.mouseClicked(mouseX, mouseY, button);
    }

    public boolean isContextMenuOpen() {
        return panel.isContextMenuOpen();
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        if (!this.visible) return false;
        panel.mouseReleased(mouseX, mouseY, button);
        return false;
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        if (!this.visible) return false;
        return panel.mouseDragged(mouseX, mouseY, button, dragX, dragY);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollY) {
        if (!this.visible || !panel.isMouseOver(mouseX, mouseY)) return false;
        return panel.mouseScrolled(mouseX, mouseY, scrollY);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (!this.visible) return false;
        return panel.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean charTyped(char codePoint, int modifiers) {
        if (!this.visible) return false;
        return panel.charTyped(codePoint, modifiers);
    }

    @Override
    protected void updateWidgetNarration(NarrationElementOutput output) {
    }

    public UniversalResultsPanel getInnerPanel() {
        return panel;
    }

    public AmiConfig.PanelContent getContentType() {
        return contentType;
    }

    public void setContentType(AmiConfig.PanelContent contentType) {
        this.contentType = contentType;
        this.panel.setPanelTitle(titleFor(contentType));
        this.panel.setChromeOnly(contentType == AmiConfig.PanelContent.EMPTY);
        updateQuestChangeListener();
        refresh();
    }

    private void updateQuestChangeListener() {
        boolean shouldRegister = contentType == AmiConfig.PanelContent.QUESTS;
        if (shouldRegister && !questListenerRegistered) {
            AmiQuestsApi.addOnChangeListener(questRefreshListener);
            questListenerRegistered = true;
        } else if (!shouldRegister && questListenerRegistered) {
            AmiQuestsApi.removeOnChangeListener(questRefreshListener);
            questListenerRegistered = false;
        }
    }
}
