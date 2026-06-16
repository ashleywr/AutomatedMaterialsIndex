package com.sanhiruzu.ami.client.overlay;

import com.sanhiruzu.ami.client.UniversalResultsPanel;
import com.sanhiruzu.ami.config.AmiConfig;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.input.CharacterEvent;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;

public class ResultsPanelWidget extends AbstractWidget {
    private UniversalResultsPanel panel;
    private java.util.function.Consumer<String> modClickCallback;
    private Runnable resetCallback;
    private java.util.function.Consumer<String> tokenInjectCallback;
    private Runnable modeToggleCallback;
    private java.util.function.BooleanSupplier modeToggleActive;
    private AmiConfig.PanelContent contentType = AmiConfig.PanelContent.GRID;

    public ResultsPanelWidget() {
        super(0, 0, 0, 0, Component.empty());
    }

    public void setOnModClick(java.util.function.Consumer<String> callback) {
        this.modClickCallback = callback;
        if (panel != null) panel.setOnModClick(callback);
    }

    public void setOnReset(Runnable callback) {
        this.resetCallback = callback;
        if (panel != null) panel.setOnReset(callback);
    }

    public void setOnTokenInject(java.util.function.Consumer<String> callback) {
        this.tokenInjectCallback = callback;
        if (panel != null) panel.setOnTokenInject(callback);
    }

    public void setOnModeToggle(Runnable callback, java.util.function.BooleanSupplier activeSupplier) {
        this.modeToggleCallback = callback;
        this.modeToggleActive = activeSupplier;
        if (panel != null) panel.setOnModeToggle(callback, activeSupplier);
    }

    public void setContentType(AmiConfig.PanelContent contentType) {
        this.contentType = contentType;
        if (panel != null) panel.configureView(contentType);
    }

    public void updateBounds(WidgetBounds bounds) {
        setX(bounds.x());
        setY(bounds.y());
        this.width = bounds.width();
        this.height = bounds.height();

        if (panel == null) {
            panel = new UniversalResultsPanel(bounds.x(), bounds.y(), bounds.width(), bounds.height());
            if (modClickCallback != null) panel.setOnModClick(modClickCallback);
            if (resetCallback != null) panel.setOnReset(resetCallback);
            if (tokenInjectCallback != null) panel.setOnTokenInject(tokenInjectCallback);
            if (modeToggleCallback != null) panel.setOnModeToggle(modeToggleCallback, modeToggleActive);
            panel.configureView(contentType);
        } else {
            panel.updateLayout(bounds.x(), bounds.y(), bounds.width(), bounds.height());
            panel.configureView(contentType);
        }
    }

    public WidgetBounds getBounds() {
        return new WidgetBounds(getX(), getY(), width, height);
    }

    @Override
    protected void extractWidgetRenderState(GuiGraphicsExtractor g, int mouseX, int mouseY, float partialTick) {
        if (panel == null || width <= 0 || height <= 0) return;
        panel.render(g, mouseX, mouseY, partialTick);
    }

    /**
     * Renders dropdowns and tooltips that must draw above everything else.
     */
    public void renderOverlay(GuiGraphicsExtractor g, int mouseX, int mouseY) {
        if (panel == null) return;
        panel.renderOverlay(g, mouseX, mouseY);
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        if (panel == null || !this.visible) return false;
        double mouseX = event.x();
        double mouseY = event.y();
        int button = event.button();
        if (!isMouseOver(mouseX, mouseY) && !panel.isContextMenuOpen()) return false;
        if (isMouseOver(mouseX, mouseY)) {
            panel.mouseClickedScrollbar(mouseX, mouseY, button);
        }
        panel.mouseClicked(mouseX, mouseY, button);
        // Always return true for in-bounds clicks so the screen focuses this widget,
        // enabling mouseDragged (scrollbar) and mouseReleased to route here correctly.
        return true;
    }

    public boolean isContextMenuOpen() {
        return panel != null && panel.isContextMenuOpen();
    }

    @Override
    public boolean mouseDragged(MouseButtonEvent event, double dragX, double dragY) {
        if (panel == null) return false;
        return panel.mouseDragged(event.x(), event.y(), event.button(), dragX, dragY);
    }

    @Override
    public boolean mouseReleased(MouseButtonEvent event) {
        if (panel == null) return false;
        panel.mouseReleased(event.x(), event.y(), event.button());
        return false;
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        if (panel == null) return false;
        return panel.mouseScrolled(mouseX, mouseY, scrollY);
    }

    @Override
    public boolean keyPressed(KeyEvent event) {
        if (panel == null) return false;
        return panel.keyPressed(event.key(), event.scancode(), event.modifiers());
    }

    @Override
    public boolean charTyped(CharacterEvent event) {
        if (panel == null) return false;
        return panel.charTyped((char) event.codepoint(), 0);
    }

    @Override
    protected void updateWidgetNarration(NarrationElementOutput output) {
    }

    public UniversalResultsPanel getInnerPanel() {
        return panel;
    }
}
