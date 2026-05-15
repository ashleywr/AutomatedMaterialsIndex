package com.sanhiruzu.ami.client.overlay;

import java.util.ArrayList;
import java.util.List;

import com.sanhiruzu.ami.index.query.TokenColorizer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import org.lwjgl.glfw.GLFW;

public class SearchBarWidget implements AmiWidget {
    private String query = "";
    private boolean focused = false;
    private WidgetBounds bounds = new WidgetBounds(0, 0, 160, 14);

    private final Listener listener;
    private final List<String> history = new ArrayList<>();
    private int historyIndex = -1;
    private String liveQuery = "";  // Save current query when entering history nav
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

    private void drawColorizedText(GuiGraphics g, net.minecraft.client.gui.Font font, int startX, int startY, int maxWidth) {
        if (query.isEmpty()) return;

        int maxTextWidth = maxWidth - 6; // Account for padding on both sides
        String displayText = query;

        // Truncate if text is too wide
        while (!displayText.isEmpty() && font.width(displayText) > maxTextWidth) {
            displayText = displayText.substring(0, displayText.length() - 1);
        }

        if (displayText.isEmpty()) return;

        g.enableScissor(startX - 3, startY - 2, startX + maxWidth, startY + 14);
        try {
            if (colorSpans.isEmpty()) {
                g.drawString(font, displayText, startX, startY, 0xFFCCCCCC, false);
            } else {
                int currentX = startX;
                for (TokenColorizer.ColorSpan span : colorSpans) {
                    if (span.endIndex() > displayText.length()) break;

                    String spanText = displayText.substring(span.startIndex(), Math.min(span.endIndex(), displayText.length()));
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

    private void updateColorSpans() {
        colorSpans = TokenColorizer.colorize(query);
    }

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

        if (highlight) {
            int highlightBorder = 0xFFEEEE00;
            g.fill(x - 1, y - 1, x + w + 1, y, highlightBorder);
            g.fill(x - 1, y + h, x + w + 1, y + h + 1, highlightBorder);
            g.fill(x - 1, y - 1, x, y + h + 1, highlightBorder);
            g.fill(x + w, y - 1, x + w + 1, y + h + 1, highlightBorder);
        }

        int textX = x + 5;
        int textY = y + (h - font.lineHeight) / 2 + 1;
        if (query.isEmpty() && !focused) {
            // Cycle placeholder text every 3 seconds
            long now = System.currentTimeMillis();
            if (now - lastPlaceholderSwap >= PLACEHOLDER_CYCLE_MS) {
                placeholderIndex = (placeholderIndex + 1) % PLACEHOLDER_HINTS.length;
                lastPlaceholderSwap = now;
            }
            g.drawString(font, PLACEHOLDER_HINTS[placeholderIndex], textX, textY, 0xFF666666, false);
        } else {
            drawColorizedText(g, font, textX, textY, w);
        }

        if (focused && !query.isEmpty() && (System.currentTimeMillis() % 1000) < 500) {
            int cursorX = textX + font.width(query) + 1;
            g.fill(cursorX, textY - 1, cursorX + 1, textY + font.lineHeight, 0xFFCCCCCC);
        }
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (!isMouseOver(mouseX, mouseY)) return false;

        if (button == 0) {
            long now = System.currentTimeMillis();
            if (now - lastClickTime < 500) {
                // Double-click: toggle highlight
                highlight = !highlight;
                lastClickTime = 0;
            } else {
                lastClickTime = now;
            }
            setFocused(true);
            return true;
        } else if (button == 1) {
            // Right click: clear and activate
            clear();
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
            updateColorSpans();
            if (listener != null) listener.onQueryChanged(query);
            historyIndex = -1;
            return true;
        } else if (keyCode == GLFW.GLFW_KEY_ESCAPE) {
            setFocused(false);
            return true;
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
            return true;
        }
    }

    @Override
    public boolean charTyped(char c, int modifiers) {
        if (!focused) return false;

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
        if (focused == this.focused) return;

        if (!focused && !query.isEmpty()) {
            addToHistory(query);
        }

        this.focused = focused;
        historyIndex = -1;
        liveQuery = "";
        highlight = false;
    }

    public void clear() {
        query = "";
        focused = false;
        historyIndex = -1;
        liveQuery = "";
        highlight = false;
        colorSpans = List.of();
    }

    private void deleteChar() {
        if (!query.isEmpty()) {
            query = query.substring(0, query.length() - 1);
        }
    }

    public void addToHistory(String searchTerm) {
        if (searchTerm == null || searchTerm.isEmpty()) return;

        history.remove(searchTerm);
        history.add(0, searchTerm);

        if (history.size() > 50) {
            history.remove(history.size() - 1);
        }

        historyIndex = -1;
    }
}
