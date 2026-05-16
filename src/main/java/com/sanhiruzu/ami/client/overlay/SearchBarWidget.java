package com.sanhiruzu.ami.client.overlay;

import com.sanhiruzu.ami.index.query.TokenColorizer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.network.chat.Component;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.List;

public class SearchBarWidget extends AbstractWidget {
    private String query = "";
    private final Listener listener;
    private final List<String> history = new ArrayList<>();
    private int historyIndex = -1;
    private String liveQuery = "";
    private long lastClickTime = 0;
    private boolean highlight = false;

    private static final String[] PLACEHOLDER_HINTS = {
        "Search items, biomes, or players...",
        "Try #storage or #ore for tag search...",
        "Try &nether to filter by environment...",
        "Type a player name to see their location..."
    };
    private static final long PLACEHOLDER_CYCLE_MS = 3000;
    private int placeholderIndex = 0;
    private long lastPlaceholderSwap = 0;

    private List<TokenColorizer.ColorSpan> colorSpans = List.of();

    public interface Listener {
        void onQueryChanged(String query);
    }

    public SearchBarWidget(Listener listener) {
        super(0, 0, 160, 14, Component.empty());
        this.listener = listener;
    }

    public void updateBounds(WidgetBounds bounds) {
        setX(bounds.x());
        setY(bounds.y());
        this.width = bounds.width();
        this.height = bounds.height();
    }

    public WidgetBounds getBounds() {
        return new WidgetBounds(getX(), getY(), width, height);
    }

    @Override
    public void setFocused(boolean focused) {
        boolean wasFocused = isFocused();
        super.setFocused(focused);
        if (wasFocused && !focused && !query.isEmpty()) {
            addToHistory(query);
        }
        if (!focused) {
            historyIndex = -1;
            liveQuery = "";
            highlight = false;
        }
    }

    @Override
    protected void renderWidget(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        if (width <= 0 || height <= 0) return;
        var font = Minecraft.getInstance().font;

        int x = getX(), y = getY(), w = width, h = height;
        boolean focused = isFocused();

        g.fill(x, y, x + w, y + h, focused ? 0xFF2E2E2E : 0xFF1A1A1A);
        int border = focused ? 0xFFFFFFFF : 0xFF555555;
        g.fill(x, y, x + w, y + 1, border);
        g.fill(x, y + h - 1, x + w, y + h, border);
        g.fill(x, y, x + 1, y + h, border);
        g.fill(x + w - 1, y, x + w, y + h, border);

        if (highlight) {
            int highlightBorder = 0xFFEEEE00;
            g.fill(x - 1, y - 1, x + w + 1, y, highlightBorder);
            g.fill(x - 1, y + h, x + w + 1, y + h + 1, highlightBorder);
            g.fill(x - 1, y - 1, x, y + h + 1, highlightBorder);
            g.fill(x + w, y - 1, x + w + 1, y + h + 1, highlightBorder);
        }

        int textX = x + 5;
        int textY = y + (h - font.lineHeight) / 2 + 1;
        int maxTextWidth = w - 10;

        if (query.isEmpty() && !focused) {
            long now = System.currentTimeMillis();
            if (now - lastPlaceholderSwap >= PLACEHOLDER_CYCLE_MS) {
                placeholderIndex = (placeholderIndex + 1) % PLACEHOLDER_HINTS.length;
                lastPlaceholderSwap = now;
            }
            g.enableScissor(textX, textY - 1, textX + maxTextWidth, textY + font.lineHeight + 1);
            g.drawString(font, PLACEHOLDER_HINTS[placeholderIndex], textX, textY, 0xFF666666, false);
            g.disableScissor();
        } else {
            String visibleText = computeVisibleText(font, maxTextWidth);
            int scrollStart = query.length() - visibleText.length();
            drawColorizedText(g, font, textX, textY, visibleText, scrollStart, maxTextWidth);

            if (focused && (System.currentTimeMillis() % 1000) < 500) {
                int cursorX = textX + font.width(visibleText) + 1;
                g.fill(cursorX, textY - 1, cursorX + 1, textY + font.lineHeight, 0xFFCCCCCC);
            }
        }
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (!isMouseOver(mouseX, mouseY)) return false;

        if (button == 0) {
            long now = System.currentTimeMillis();
            if (now - lastClickTime < 500) {
                highlight = !highlight;
                lastClickTime = 0;
            } else {
                lastClickTime = now;
            }
            setFocused(true);
            return true;
        } else if (button == 1) {
            clear();
            setFocused(true);
            return true;
        }
        return false;
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (!isFocused()) return false;

        if (keyCode == GLFW.GLFW_KEY_BACKSPACE) {
            deleteChar();
            updateColorSpans();
            if (listener != null) listener.onQueryChanged(query);
            historyIndex = -1;
            return true;
        } else if (keyCode == GLFW.GLFW_KEY_ESCAPE) {
            setFocused(false);
            return false;  // let Escape propagate so the screen can close
        } else if (keyCode == GLFW.GLFW_KEY_ENTER) {
            setFocused(false);
            return true;
        } else if (keyCode == GLFW.GLFW_KEY_UP) {
            if (!history.isEmpty()) {
                if (historyIndex < 0) {
                    liveQuery = query;
                    historyIndex = 0;
                } else if (historyIndex < history.size() - 1) {
                    historyIndex++;
                }
                query = history.get(historyIndex);
                updateColorSpans();
                if (listener != null) listener.onQueryChanged(query);
            }
            return true;
        } else if (keyCode == GLFW.GLFW_KEY_DOWN) {
            if (!history.isEmpty()) {
                if (historyIndex > 0) {
                    historyIndex--;
                    query = history.get(historyIndex);
                } else if (historyIndex == 0) {
                    historyIndex = -1;
                    query = liveQuery;
                    liveQuery = "";
                }
                updateColorSpans();
                if (listener != null) listener.onQueryChanged(query);
            }
            return true;
        } else {
            return false;
        }
    }

    @Override
    public boolean charTyped(char c, int modifiers) {
        if (!isFocused()) return false;

        if (c >= 32 && c < 127 && query.length() < 256) {
            query += c;
            historyIndex = -1;
            updateColorSpans();
            if (listener != null) listener.onQueryChanged(query);
            return true;
        }
        return false;
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        return false;
    }

    @Override
    protected void updateWidgetNarration(NarrationElementOutput output) {
    }

    public String getQuery() {
        return query;
    }

    /** Sets the displayed query without firing the listener. Used for external sync (e.g. EMI). */
    public void setQuery(String query) {
        this.query = query == null ? "" : query;
        updateColorSpans();
    }

    public void clear() {
        query = "";
        super.setFocused(false);
        historyIndex = -1;
        liveQuery = "";
        highlight = false;
        colorSpans = List.of();
    }

    public void addToHistory(String searchTerm) {
        if (searchTerm == null || searchTerm.isEmpty()) return;
        history.remove(searchTerm);
        history.add(0, searchTerm);
        if (history.size() > 50) history.remove(history.size() - 1);
        historyIndex = -1;
    }

    private void deleteChar() {
        if (!query.isEmpty()) query = query.substring(0, query.length() - 1);
    }

    private void updateColorSpans() {
        colorSpans = TokenColorizer.colorize(query);
    }

    private String computeVisibleText(net.minecraft.client.gui.Font font, int maxTextWidth) {
        int start = 0;
        while (start < query.length() && font.width(query.substring(start)) > maxTextWidth) start++;
        return query.substring(start);
    }

    private void drawColorizedText(GuiGraphics g, net.minecraft.client.gui.Font font, int startX, int startY, String visibleText, int scrollStart, int maxTextWidth) {
        if (visibleText.isEmpty()) return;

        g.enableScissor(startX, startY - 1, startX + maxTextWidth, startY + font.lineHeight + 1);
        try {
            if (colorSpans.isEmpty() || scrollStart > 0) {
                g.drawString(font, visibleText, startX, startY, 0xFFCCCCCC, false);
            } else {
                int currentX = startX;
                for (TokenColorizer.ColorSpan span : colorSpans) {
                    int sStart = span.startIndex();
                    int sEnd = Math.min(span.endIndex(), visibleText.length());
                    if (sEnd <= sStart || sStart >= visibleText.length()) continue;
                    String spanText = visibleText.substring(sStart, sEnd);
                    if (!spanText.isEmpty()) {
                        g.drawString(font, spanText, currentX, startY, span.argbColor(), false);
                        currentX += font.width(spanText);
                    }
                }
            }
        } finally {
            g.disableScissor();
        }
    }
}
