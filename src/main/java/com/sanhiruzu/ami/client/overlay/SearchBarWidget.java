package com.sanhiruzu.ami.client.overlay;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import org.lwjgl.glfw.GLFW;

public class SearchBarWidget implements AmiWidget {
    private String query = "";
    private boolean focused = false;
    private WidgetBounds bounds = new WidgetBounds(0, 0, 160, 14);

    private final Listener listener;

    public interface Listener {
        void onQueryChanged(String query);
    }

    public SearchBarWidget(Listener listener) {
        this.listener = listener;
    }

    public void updateBounds(WidgetBounds bounds) {
        this.bounds = bounds;
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        var font = Minecraft.getInstance().font;

        int x = bounds.x();
        int y = bounds.y();
        int w = bounds.width();
        int h = bounds.height();

        boolean btnHovered = isMouseOver(mouseX, mouseY);

        g.fill(x, y, x + w, y + h, focused ? 0xFF2E2E2E : 0xFF1A1A1A);
        int border = focused ? 0xFFAAAA44 : 0xFF555555;
        g.fill(x, y, x + w, y + 1, border);
        g.fill(x, y + h - 1, x + w, y + h, border);
        g.fill(x, y, x + 1, y + h, border);
        g.fill(x + w - 1, y, x + w, y + h, border);

        int textX = x + 3;
        int textY = y + 3;
        if (query.isEmpty() && !focused) {
            g.drawString(font, Component.translatable("ami.gui.search.placeholder"),
                    textX, textY, 0xFF666666, false);
        } else {
            g.drawString(font, query, textX, textY, 0xFFCCCCCC, false);
        }

        if (focused && (System.currentTimeMillis() % 1000) < 500) {
            int cursorX = textX + font.width(query) + 1;
            g.fill(cursorX, textY, cursorX + 1, textY + font.lineHeight, 0xFFCCCCCC);
        }
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0 && isMouseOver(mouseX, mouseY)) {
            setFocused(true);
            return true;
        }
        return false;
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        return false;
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        return false;
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
        return false;
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (!focused) return false;

        if (keyCode == GLFW.GLFW_KEY_BACKSPACE) {
            deleteChar();
            if (listener != null) listener.onQueryChanged(query);
            return true;
        } else if (keyCode == GLFW.GLFW_KEY_ESCAPE) {
            clear();
            return true;
        } else {
            // Block all other keys when focused (prevents E key from closing inventory)
            return true;
        }
    }

    @Override
    public boolean charTyped(char c, int modifiers) {
        if (!focused) return false;

        if (c >= 32 && c < 127) {
            query += c;
            if (listener != null) listener.onQueryChanged(query);
            return true;
        }
        return false;
    }

    @Override
    public WidgetBounds getBounds() {
        return bounds;
    }

    public String getQuery() {
        return query;
    }

    public boolean isFocused() {
        return focused;
    }

    public void setFocused(boolean focused) {
        this.focused = focused;
    }

    public void clear() {
        query = "";
        focused = false;
    }

    private void deleteChar() {
        if (!query.isEmpty()) {
            query = query.substring(0, query.length() - 1);
        }
    }
}
