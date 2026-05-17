package com.sanhiruzu.ami.client.overlay;

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
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;

public class SearchBarWidget extends EditBox {
    private final Listener listener;
    private final List<String> history = new ArrayList<>();
    private final Deque<String> undoStack = new ArrayDeque<>();
    private int historyIndex = -1;
    private String liveQuery = "";
    private String lastValue = "";
    private long lastClickTime = 0;
    private boolean silentUpdate = false;
    private boolean undoing = false;
    private int cursorPos = 0;
    private int highlightPos = 0;

    private static final Component PLACEHOLDER_HINT = Component.translatable("ami.gui.search.placeholder_hint");

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
        boolean wasFocused = isFocused();
        super.setFocused(focused);
        if (wasFocused && !focused && !getValue().isEmpty()) {
            addToHistory(getValue());
        }
        if (!focused) {
            historyIndex = -1;
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

        g.fill(x, y, x + w, y + h, focused ? 0xFF2E2E2E : 0xFF1A1A1A);
        int border = focused ? 0xFFFFFFFF : 0xFF555555;
        g.fill(x, y, x + w, y + 1, border);
        g.fill(x, y + h - 1, x + w, y + h, border);
        g.fill(x, y, x + 1, y + h, border);
        g.fill(x + w - 1, y, x + w, y + h, border);

        int textX = x + 5;
        int textY = y + (h - font.lineHeight) / 2 + 1;
        int maxTextWidth = w - 10;

        String value = getValue();
        if (value.isEmpty() && !focused) {
            g.enableScissor(textX, textY - 1, textX + maxTextWidth, textY + font.lineHeight + 1);
            g.drawString(font, PLACEHOLDER_HINT, textX, textY, 0xFF666666, false);
            g.disableScissor();
        } else {
            int displayStart = computeDisplayStart(font, maxTextWidth);
            String visibleText = value.substring(displayStart);
            renderSelection(g, font, textX, textY, visibleText, displayStart, maxTextWidth);
            drawColorizedText(g, font, textX, textY, visibleText, displayStart, maxTextWidth);

            if (focused && (System.currentTimeMillis() % 1000) < 500) {
                int cursorInVisible = Math.max(0, Math.min(getCursorPosition() - displayStart, visibleText.length()));
                int cursorX = textX + font.width(visibleText.substring(0, cursorInVisible)) + 1;
                g.fill(cursorX, textY - 1, cursorX + 1, textY + font.lineHeight, 0xFFCCCCCC);
            }
        }
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (!isMouseOver(mouseX, mouseY)) return false;

        if (button == 0) {
            long now = System.currentTimeMillis();
            int clickedPos = cursorPositionFromMouse(mouseX);
            if (now - lastClickTime < 500) {
                selectTokenAt(clickedPos);
                lastClickTime = 0;
                return true;
            }
            lastClickTime = now;
        } else if (button == 1) {
            clear();
            setFocused(true);
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
            return false;   // propagate for atlas-cycling keybind
        }
        if (keyCode == GLFW.GLFW_KEY_ENTER) {
            setFocused(false);
            return true;
        }
        if (keyCode == GLFW.GLFW_KEY_UP) {
            navigateHistory(+1);
            return true;
        }
        if (keyCode == GLFW.GLFW_KEY_DOWN) {
            navigateHistory(-1);
            return true;
        }
        if (Screen.hasControlDown() && keyCode == GLFW.GLFW_KEY_Z) {
            undoLastEdit();
            return true;
        }

        // Delegate to EditBox for Backspace, Delete, Ctrl+A, Ctrl+V, Ctrl+X, Ctrl+C,
        // Ctrl+Backspace, arrows, Home, End. Ctrl+Z is handled above because EditBox
        // does not provide undo. Always return true when focused so the screen
        // doesn't also act on the key (e.g. 'E' closing inventory).
        super.keyPressed(keyCode, scanCode, modifiers);
        return true;
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        return false;
    }

    public String getQuery() {
        return getValue();
    }

    /** Sets the displayed query without firing the listener. Used for external sync (e.g. EMI). */
    public void setQuery(String q) {
        silentUpdate = true;
        setValue(q == null ? "" : q);
        silentUpdate = false;
        lastValue = getValue();
    }

    public void clear() {
        silentUpdate = true;
        setValue("");
        silentUpdate = false;
        super.setFocused(false);
        historyIndex = -1;
        liveQuery = "";
        lastValue = "";
    }

    public void appendQuery(String text) {
        String current = getValue();
        if (current.contains(text)) return;

        if (!current.isEmpty() && !current.endsWith(" ")) {
            current += " ";
        }
        setValue(current + text);
        setFocused(true);
    }

    public void addToHistory(String searchTerm) {
        if (searchTerm == null || searchTerm.isEmpty()) return;
        history.remove(searchTerm);
        history.add(0, searchTerm);
        if (history.size() > 50) history.remove(history.size() - 1);
        historyIndex = -1;
    }

    private void onTextChanged(String newValue) {
        historyIndex = -1;
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

    private void navigateHistory(int direction) {
        if (history.isEmpty()) return;
        int newIndex = historyIndex;
        String newValue;
        if (direction > 0) {
            if (newIndex < 0) {
                liveQuery = getValue();
                newIndex = 0;
            } else if (newIndex < history.size() - 1) {
                newIndex++;
            } else {
                return;
            }
            newValue = history.get(newIndex);
        } else {
            if (newIndex > 0) {
                newIndex--;
                newValue = history.get(newIndex);
            } else if (newIndex == 0) {
                newIndex = -1;
                newValue = liveQuery;
                liveQuery = "";
            } else {
                return;
            }
        }
        setValue(newValue); // fires onTextChanged → resets historyIndex to -1
        historyIndex = newIndex; // restore after responder reset it
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
        g.fill(RenderType.guiTextHighlight(), startX, textY - 1, endX, textY + font.lineHeight, 0xFF0000FF);
    }

    private void drawColorizedText(GuiGraphics g, Font font, int startX, int startY, String visibleText, int scrollStart, int maxTextWidth) {
        if (visibleText.isEmpty()) return;

        g.enableScissor(startX, startY - 1, startX + maxTextWidth, startY + font.lineHeight + 1);
        try {
            if (colorSpans.isEmpty()) {
                g.drawString(font, visibleText, startX, startY, 0xFFCCCCCC, false);
            } else {
                int currentX = startX;
                int coveredUntil = 0;
                for (TokenColorizer.ColorSpan span : colorSpans) {
                    int sStart = Math.max(span.startIndex() - scrollStart, 0);
                    int sEnd = Math.min(span.endIndex() - scrollStart, visibleText.length());
                    if (sEnd <= sStart || sStart >= visibleText.length()) continue;
                    if (sStart > coveredUntil) {
                        String gap = visibleText.substring(coveredUntil, sStart);
                        g.drawString(font, gap, currentX, startY, 0xFFCCCCCC, false);
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
                    g.drawString(font, visibleText.substring(coveredUntil), currentX, startY, 0xFFCCCCCC, false);
                }
            }
        } finally {
            g.disableScissor();
        }
    }
}
