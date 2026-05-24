package com.sanhiruzu.ami.client.overlay;

import com.sanhiruzu.ami.client.AMITheme;
import com.sanhiruzu.ami.config.AmiConfig;
import com.sanhiruzu.ami.index.query.TokenColorizer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.List;

public class SearchBarWidget extends EditBox {
    private final Listener listener;
    private final Deque<String> undoStack = new ArrayDeque<>();
    private String liveQuery = "";
    private String lastValue = "";
    private long lastClickTime = 0;
    private boolean silentUpdate = false;
    private boolean undoing = false;
    private int cursorPos = 0;
    private int highlightPos = 0;

    private static final Component PLACEHOLDER_HINT = Component.translatable("ami.gui.search.placeholder_hint");
    private static final Component TYPING_HINT = Component.translatable("ami.gui.search.typing");

    private List<TokenColorizer.ColorSpan> colorSpans = List.of();

    public interface Listener {
        void onQueryChanged(String query);
    }

    public SearchBarWidget(Listener listener) {
        super(Minecraft.getInstance().font, 0, 0, 160, 14, Component.empty());
        this.listener = listener;
        setMaxLength(256);
        setResponder(this::onTextChanged);
        setBordered(false);
        setFilter(s -> s.chars().allMatch(c -> c >= 32 && c < 127));
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
        super.setFocused(focused);
        if (!focused) {
            liveQuery = "";
            lastClickTime = 0;
        }
    }

    @Override
    public void renderWidget(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        if (width <= 0 || height <= 0) return;
        Font font = Minecraft.getInstance().font;

        int x = getX(), y = getY(), w = width, h = height;
        boolean focused = isFocused();

        // Use themed colors
        int bgColor = AmiConfig.searchBarBg;
        int border = AmiConfig.searchBarBorder;

        if (!focused) {
            // Desaturate border slightly when not focused
            int alpha = (border >> 24) & 0xFF;
            border = (alpha / 2 << 24) | (border & 0x00FFFFFF);
        }

        g.fill(x, y, x + w, y + h, bgColor);

        // Draw 1px border
        g.fill(x, y, x + w, y + 1, border);
        g.fill(x, y + h - 1, x + w, y + h, border);
        g.fill(x, y + 1, x + 1, y + h - 1, border);
        g.fill(x + w - 1, y + 1, x + w, y + h - 1, border);

        int textX = x + 5;
        int textY = y + (h - font.lineHeight) / 2 + 1;
        String value = getValue();
        int maxTextWidth = w - 10 - (value.isEmpty() ? 0 : 12); // Reserve space for 'x'

        int displayStart = computeDisplayStart(font, maxTextWidth);

        if (value.isEmpty()) {
            Component hint = focused ? TYPING_HINT : PLACEHOLDER_HINT;
            g.enableScissor(textX, textY - 1, textX + maxTextWidth, textY + font.lineHeight + 1);
            g.drawString(font, hint, textX, textY, AMITheme.SEARCH_PLACEHOLDER, false);
            g.disableScissor();
        } else {
            String visibleText = value.substring(displayStart);
            renderSelection(g, font, textX, textY, visibleText, displayStart, maxTextWidth);
            drawColorizedText(g, font, textX, textY, visibleText, displayStart, maxTextWidth);

            // Draw 'x' clear button
            int clearX = x + w - 14;
            int clearY = y + (h - 10) / 2;
            boolean hoveredX = mouseX >= clearX && mouseX < clearX + 12 && mouseY >= clearY && mouseY < clearY + 10;
            g.drawString(font, Component.translatable("ami.gui.search.clear"), clearX + 3, clearY, hoveredX ? AMITheme.SEARCH_CLEAR_TEXT_HOVER : AMITheme.SEARCH_CLEAR_TEXT, false);
        }

        if (focused && (System.currentTimeMillis() % 1000) < 500) {
            int cursorInVisible = Math.max(0, Math.min(getCursorPosition() - displayStart, value.length() - displayStart));
            int cursorX = textX + font.width(value.substring(displayStart, displayStart + cursorInVisible)) + 1;
            g.fill(cursorX, textY - 1, cursorX + 1, textY + font.lineHeight, AMITheme.SEARCH_CURSOR);
        }
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (!isMouseOver(mouseX, mouseY)) return false;

        String value = getValue();
        if (button == 0) {
            // Check clear button click
            if (!value.isEmpty()) {
                int clearX = getX() + width - 14;
                int clearY = getY() + (height - 10) / 2;
                if (mouseX >= clearX && mouseX < clearX + 12 && mouseY >= clearY && mouseY < clearY + 10) {
                    clearAndFocus();
                    return true;
                }
            }

            long now = System.currentTimeMillis();
            int clickedPos = cursorPositionFromMouse(mouseX);
            if (now - lastClickTime < 500) {
                selectTokenAt(clickedPos);
                lastClickTime = 0;
                return true;
            }
            lastClickTime = now;
        } else if (button == 1) {
            clearAndFocus();
            return true;
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public void onClick(double mouseX, double mouseY) {
        moveCursorTo(cursorPositionFromMouse(mouseX), Screen.hasShiftDown());
    }

    @Override
    protected void onDrag(double mouseX, double mouseY, double dragX, double dragY) {
        moveCursorTo(cursorPositionFromMouse(mouseX), true);
    }

    @Override
    public void setCursorPosition(int pos) {
        super.setCursorPosition(pos);
        cursorPos = getCursorPosition();
    }

    @Override
    public void setHighlightPos(int position) {
        super.setHighlightPos(position);
        highlightPos = Mth.clamp(position, 0, getValue().length());
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (!isFocused()) return false;

        if (keyCode == GLFW.GLFW_KEY_ESCAPE) {
            setFocused(false);
            return true;   // consume so the screen doesn't close
        }
        if (keyCode == GLFW.GLFW_KEY_TAB) {
            return false;   // propagate to screen-level keybinds
        }
        if (keyCode == GLFW.GLFW_KEY_ENTER) {
            setFocused(false);
            return true;
        }
        if (Screen.hasControlDown() && keyCode == GLFW.GLFW_KEY_Z) {
            undoLastEdit();
            return true;
        }

        // Token-aware movement
        if (Screen.hasControlDown()) {
            if (keyCode == GLFW.GLFW_KEY_LEFT) {
                moveCursorTokenWise(-1, Screen.hasShiftDown());
                return true;
            } else if (keyCode == GLFW.GLFW_KEY_RIGHT) {
                moveCursorTokenWise(1, Screen.hasShiftDown());
                return true;
            }
        }

        super.keyPressed(keyCode, scanCode, modifiers);
        return true;
    }

    private void moveCursorTokenWise(int direction, boolean select) {
        String value = getValue();
        int pos = getCursorPosition();

        if (direction < 0) { // Left
            if (pos <= 0) return;
            // Skip trailing spaces
            while (pos > 0 && value.charAt(pos - 1) == ' ') pos--;
            // Find start of token
            while (pos > 0 && value.charAt(pos - 1) != ' ') pos--;
        } else { // Right
            if (pos >= value.length()) return;
            // Skip leading spaces
            while (pos < value.length() && value.charAt(pos) == ' ') pos++;
            // Find end of token
            while (pos < value.length() && value.charAt(pos) != ' ') pos++;
        }

        moveCursorTo(pos, select);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        return false;
    }

    public String getQuery() {
        return getValue();
    }

    /**
     * Sets the displayed query without firing the listener. Used for external sync (e.g. EMI).
     */
    public void setQuery(String q) {
        silentUpdate = true;
        setValue(q == null ? "" : q);
        silentUpdate = false;
        lastValue = getValue();
    }

    public void clear() {
        setValue("");
        setFocused(false);
    }

    public void clearAndFocus() {
        setValue("");
        setFocused(true);
        moveCursorToEnd(false);
    }

    public void toggleToken(String token) {
        setValue(com.sanhiruzu.ami.index.query.QueryUtils.toggleToken(getValue(), token));
        setFocused(true);
    }

    private void onTextChanged(String newValue) {
        updateColorSpans();
        if (!silentUpdate && !undoing && !newValue.equals(lastValue)) {
            undoStack.push(lastValue);
            if (undoStack.size() > 50) {
                undoStack.removeLast();
            }
        }
        lastValue = newValue;

        if (!silentUpdate && listener != null) listener.onQueryChanged(newValue);
    }

    private void undoLastEdit() {
        if (undoStack.isEmpty()) return;
        undoing = true;
        setValue(undoStack.pop());
        setCursorPosition(getValue().length());
        setHighlightPos(getCursorPosition());
        undoing = false;
        lastValue = getValue();
    }

    private void updateColorSpans() {
        colorSpans = TokenColorizer.colorize(getValue());
    }

    private int computeDisplayStart(Font font, int maxTextWidth) {
        String value = getValue();
        int cursor = getCursorPosition();
        if (font.width(value) <= maxTextWidth) return 0;
        // Walk backward from cursor until adding one more character would overflow.
        int displayPos = cursor;
        while (displayPos > 0 && font.width(value.substring(displayPos - 1, cursor)) <= maxTextWidth) {
            displayPos--;
        }
        return displayPos;
    }

    private int cursorPositionFromMouse(double mouseX) {
        Font font = Minecraft.getInstance().font;
        int textX = getX() + 5;
        int maxTextWidth = width - 10;
        String value = getValue();
        if (value.isEmpty()) return 0;

        if (mouseX <= textX) return 0;
        if (mouseX >= textX + maxTextWidth) return value.length();

        int displayStart = computeDisplayStart(font, maxTextWidth);
        String visibleText = font.plainSubstrByWidth(value.substring(displayStart), maxTextWidth);
        int relativeX = Mth.floor(mouseX) - textX;
        return displayStart + font.plainSubstrByWidth(visibleText, relativeX).length();
    }

    private void selectTokenAt(int cursorIndex) {
        String value = getValue();
        if (value.isEmpty()) {
            return;
        }

        int pos = Mth.clamp(cursorIndex, 0, value.length());
        if (pos >= value.length() && pos > 0) {
            pos--;
        }
        if (pos < 0 || pos >= value.length()) {
            return;
        }

        if (Character.isWhitespace(value.charAt(pos))) {
            setCursorPosition(pos);
            setHighlightPos(pos);
            return;
        }

        int start = pos;
        while (start > 0 && !Character.isWhitespace(value.charAt(start - 1))) {
            start--;
        }

        int end = pos;
        while (end < value.length() && !Character.isWhitespace(value.charAt(end))) {
            end++;
        }

        setCursorPosition(end);
        setHighlightPos(start);
    }

    private void renderSelection(GuiGraphics g, Font font, int textX, int textY, String visibleText, int displayStart, int maxTextWidth) {
        int selectionStart = Math.min(cursorPos, highlightPos);
        int selectionEnd = Math.max(cursorPos, highlightPos);
        if (selectionStart == selectionEnd || visibleText.isEmpty()) return;

        int visibleLength = font.plainSubstrByWidth(visibleText, maxTextWidth).length();
        int visibleStart = displayStart;
        int visibleEnd = displayStart + visibleLength;
        int clippedStart = Mth.clamp(selectionStart, visibleStart, visibleEnd);
        int clippedEnd = Mth.clamp(selectionEnd, visibleStart, visibleEnd);
        if (clippedStart == clippedEnd) return;

        int startX = textX + font.width(visibleText.substring(0, clippedStart - displayStart));
        int endX = textX + font.width(visibleText.substring(0, clippedEnd - displayStart));
        g.fill(RenderType.guiTextHighlight(), startX, textY - 1, endX, textY + font.lineHeight, AMITheme.SEARCH_SELECTION);
    }

    private void drawColorizedText(GuiGraphics g, Font font, int startX, int startY, String visibleText, int scrollStart, int maxTextWidth) {
        if (visibleText.isEmpty()) return;

        g.enableScissor(startX, startY - 1, startX + maxTextWidth, startY + font.lineHeight + 1);
        try {
            if (colorSpans.isEmpty()) {
                g.drawString(font, visibleText, startX, startY, AMITheme.SEARCH_DEFAULT_TEXT, false);
            } else {
                int currentX = startX;
                int coveredUntil = 0;
                for (TokenColorizer.ColorSpan span : colorSpans) {
                    int sStart = Math.max(span.startIndex() - scrollStart, 0);
                    int sEnd = Math.min(span.endIndex() - scrollStart, visibleText.length());
                    if (sEnd <= sStart || sStart >= visibleText.length()) continue;
                    if (sStart > coveredUntil) {
                        String gap = visibleText.substring(coveredUntil, sStart);
                        g.drawString(font, gap, currentX, startY, AMITheme.SEARCH_DEFAULT_TEXT, false);
                        currentX += font.width(gap);
                    }
                    String spanText = visibleText.substring(sStart, sEnd);
                    if (!spanText.isEmpty()) {
                        g.drawString(font, spanText, currentX, startY, span.argbColor(), false);
                        currentX += font.width(spanText);
                    }
                    coveredUntil = sEnd;
                }
                if (coveredUntil < visibleText.length()) {
                    g.drawString(font, visibleText.substring(coveredUntil), currentX, startY, AMITheme.SEARCH_DEFAULT_TEXT, false);
                }
            }
        } finally {
            g.disableScissor();
        }
    }
}
