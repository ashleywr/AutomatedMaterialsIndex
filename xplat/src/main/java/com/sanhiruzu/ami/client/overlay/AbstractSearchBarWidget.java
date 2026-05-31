package com.sanhiruzu.ami.client.overlay;

import com.sanhiruzu.ami.client.AMITheme;
import com.sanhiruzu.ami.client.results.SearchQueryHistory;
import com.sanhiruzu.ami.config.AmiConfig;
import com.sanhiruzu.ami.index.GlobalIndex;
import com.sanhiruzu.ami.index.query.SearchSuggestions;
import com.sanhiruzu.ami.index.query.SearchSyntax;
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

public abstract class AbstractSearchBarWidget extends EditBox {
    private static final Component PLACEHOLDER_HINT = Component.translatable("ami.gui.search.placeholder_hint");
    private static final Component TYPING_HINT = Component.translatable("ami.gui.search.typing");
    private static final int MAX_SUGGESTIONS = 24;
    private static final int MAX_VISIBLE_SUGGESTIONS = 8;
    private static final int SUGGESTION_ROW_HEIGHT = 14;
    private static final int SUGGESTION_MARGIN = 2;
    private static final int HELP_BUTTON_W = 14;
    private static final int CLEAR_BUTTON_W = 14;
    private static final int HELP_MARGIN = 9;
    private static final int HELP_ROW_HEIGHT = 15;
    private static final int HELP_SECTION_GAP = 9;
    private static final int HELP_MIN_W = 300;
    private static final int HELP_MAX_W = 360;
    private static final int HELP_GAP = 6;
    private final Listener listener;
    private final Deque<String> undoStack = new ArrayDeque<>();
    private final SearchQueryHistory queryHistory = new SearchQueryHistory();
    private String lastValue = "";
    private long lastClickTime = 0;
    private boolean silentUpdate = false;
    private boolean undoing = false;
    private int cursorPos = 0;
    private int highlightPos = 0;
    private boolean navigatingHistory = false;

    private List<TokenColorizer.ColorSpan> colorSpans = List.of();
    private List<SearchSuggestions.Suggestion> suggestions = List.of();
    private int selectedSuggestion = 0;
    private int suggestionScrollOffset = 0;
    private boolean helpOpen = false;

    protected AbstractSearchBarWidget(Listener listener) {
        super(Minecraft.getInstance().font, 0, 0, 160, 14, Component.empty());
        this.listener = listener;
        setMaxLength(256);
        setResponder(this::onTextChanged);
        setBordered(false);
        setFilter(s -> s.chars().allMatch(c -> c >= 32 && c < 127));
    }

    protected abstract void doMoveCursorTo(int pos, boolean select);

    // ── Platform bridge ───────────────────────────────────────────────────────

    protected abstract void doMoveCursorToEnd();

    public void updateBounds(WidgetBounds bounds) {
        setX(bounds.x());
        setY(bounds.y());
        this.width = bounds.width();
        this.height = bounds.height();
    }

    // ── Layout ────────────────────────────────────────────────────────────────

    public WidgetBounds getBounds() {
        return new WidgetBounds(getX(), getY(), width, height);
    }

    public List<WidgetBounds> getPredictiveBounds() {
        java.util.ArrayList<WidgetBounds> bounds = new java.util.ArrayList<>();
        bounds.add(getBounds());
        if (hasVisibleSuggestions()) {
            int popupY = suggestionPopupY();
            int popupH = visibleSuggestionRows() * SUGGESTION_ROW_HEIGHT + SUGGESTION_MARGIN * 2;
            bounds.add(new WidgetBounds(getX(), popupY, width, popupH));
        }
        if (helpOpen) {
            bounds.add(new WidgetBounds(helpPopupX(), helpPopupY(), helpPopupW(), helpPopupH()));
        }
        return List.copyOf(bounds);
    }

    @Override
    public void setFocused(boolean focused) {
        super.setFocused(focused);
        if (!focused) {
            lastClickTime = 0;
            suggestions = List.of();
            suggestionScrollOffset = 0;
        } else {
            updateSuggestions();
        }
    }

    // ── Focus ─────────────────────────────────────────────────────────────────

    @Override
    public void renderWidget(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        if (width <= 0 || height <= 0) return;
        Font font = Minecraft.getInstance().font;

        int x = getX(), y = getY(), w = width, h = height;
        boolean focused = isFocused();

        int bgColor = AmiConfig.searchBarBg;
        int border = AmiConfig.searchBarBorder;

        if (!focused) {
            int alpha = (border >> 24) & 0xFF;
            border = (alpha / 2 << 24) | (border & 0x00FFFFFF);
        }

        g.fill(x, y, x + w, y + h, bgColor);

        g.fill(x, y, x + w, y + 1, border);
        g.fill(x, y + h - 1, x + w, y + h, border);
        g.fill(x, y + 1, x + 1, y + h - 1, border);
        g.fill(x + w - 1, y + 1, x + w, y + h - 1, border);

        int textX = x + 5;
        int textY = y + (h - font.lineHeight) / 2 + 1;
        String value = getValue();
        int reservedActionW = HELP_BUTTON_W + 4 + (value.isEmpty() ? 0 : CLEAR_BUTTON_W);
        int maxTextWidth = Math.max(20, w - 10 - reservedActionW);

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

            int clearX = clearButtonX();
            int clearY = y + (h - 10) / 2;
            boolean hoveredX = mouseX >= clearX && mouseX < clearX + 12 && mouseY >= clearY && mouseY < clearY + 10;
            g.drawString(font, Component.translatable("ami.gui.search.clear"), clearX + 3, clearY, hoveredX ? AMITheme.SEARCH_CLEAR_TEXT_HOVER : AMITheme.SEARCH_CLEAR_TEXT, false);
        }

        renderHelpButton(g, font, mouseX, mouseY);

        if (focused && (System.currentTimeMillis() % 1000) < 500) {
            int cursorInVisible = Math.max(0, Math.min(getCursorPosition() - displayStart, value.length() - displayStart));
            int cursorX = textX + font.width(value.substring(displayStart, displayStart + cursorInVisible)) + 1;
            g.fill(cursorX, textY - 1, cursorX + 1, textY + font.lineHeight, AMITheme.SEARCH_CURSOR);
        }

        if (helpOpen) {
            renderSearchHelp(g, font, mouseX, mouseY);
        } else {
            renderSuggestions(g, font, mouseX, mouseY);
        }
    }

    // ── Rendering ─────────────────────────────────────────────────────────────

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0 && isSuggestionPopupMouseOver(mouseX, mouseY)) {
            int row = suggestionRowAt(mouseY);
            if (row >= 0 && row < suggestions.size()) {
                selectedSuggestion = row;
                acceptSelectedSuggestion();
                return true;
            }
        }
        if (button == 0 && isHelpPopupMouseOver(mouseX, mouseY)) {
            return true;
        }
        if (!isMouseOver(mouseX, mouseY)) return false;

        String value = getValue();
        if (button == 0) {
            if (isHelpButtonMouseOver(mouseX, mouseY)) {
                helpOpen = !helpOpen;
                suggestions = List.of();
                suggestionScrollOffset = 0;
                focusForInput();
                return true;
            }
            if (!value.isEmpty()) {
                int clearX = clearButtonX();
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

    // ── Mouse ─────────────────────────────────────────────────────────────────

    @Override
    public void onClick(double mouseX, double mouseY) {
        doMoveCursorTo(cursorPositionFromMouse(mouseX), Screen.hasShiftDown());
    }

    @Override
    protected void onDrag(double mouseX, double mouseY, double dragX, double dragY) {
        doMoveCursorTo(cursorPositionFromMouse(mouseX), true);
    }

    @Override
    public void setCursorPosition(int pos) {
        super.setCursorPosition(pos);
        cursorPos = getCursorPosition();
    }

    // ── Keyboard ──────────────────────────────────────────────────────────────

    @Override
    public void setHighlightPos(int position) {
        super.setHighlightPos(position);
        highlightPos = Mth.clamp(position, 0, getValue().length());
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (!isFocused()) return false;

        if (keyCode == GLFW.GLFW_KEY_UP) {
            if (hasVisibleSuggestions()) {
                selectedSuggestion = Math.max(0, selectedSuggestion - 1);
                ensureSelectedSuggestionVisible();
                return true;
            }
            String next = queryHistory.navigateUp(getValue());
            navigatingHistory = true;
            setValue(next);
            navigatingHistory = false;
            setCursorPosition(getValue().length());
            return true;
        }
        if (keyCode == GLFW.GLFW_KEY_DOWN) {
            if (hasVisibleSuggestions()) {
                selectedSuggestion = Math.min(suggestions.size() - 1, selectedSuggestion + 1);
                ensureSelectedSuggestionVisible();
                return true;
            }
            String next = queryHistory.navigateDown(getValue());
            navigatingHistory = true;
            setValue(next);
            navigatingHistory = false;
            setCursorPosition(getValue().length());
            return true;
        }
        if (keyCode == GLFW.GLFW_KEY_ESCAPE) {
            if (helpOpen) {
                helpOpen = false;
                return true;
            }
            if (hasVisibleSuggestions()) {
                suggestions = List.of();
                suggestionScrollOffset = 0;
                return true;
            }
            unfocus();
            return true;
        }
        if (keyCode == GLFW.GLFW_KEY_TAB) {
            return acceptSelectedSuggestion();
        }
        if (keyCode == GLFW.GLFW_KEY_ENTER) {
            if (acceptSelectedSuggestion()) {
                return true;
            }
            submitAndUnfocus();
            return true;
        }
        if (Screen.hasControlDown() && keyCode == GLFW.GLFW_KEY_Z) {
            undoLastEdit();
            return true;
        }

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

        if (direction < 0) {
            if (pos <= 0) return;
            while (pos > 0 && value.charAt(pos - 1) == ' ') pos--;
            while (pos > 0 && value.charAt(pos - 1) != ' ') pos--;
        } else {
            if (pos >= value.length()) return;
            while (pos < value.length() && value.charAt(pos) == ' ') pos++;
            while (pos < value.length() && value.charAt(pos) != ' ') pos++;
        }

        doMoveCursorTo(pos, select);
    }

    public String getQuery() {
        return getValue();
    }

    // ── Public API ────────────────────────────────────────────────────────────

    public void setQuery(String q) {
        silentUpdate = true;
        setValue(q == null ? "" : q);
        silentUpdate = false;
        lastValue = getValue();
    }

    public void clear() {
        setValue("");
        unfocus();
    }

    public void submitAndUnfocus() {
        queryHistory.submit(getValue());
        suggestions = List.of();
        suggestionScrollOffset = 0;
        helpOpen = false;
        unfocus();
    }

    public void unfocus() {
        suggestions = List.of();
        suggestionScrollOffset = 0;
        helpOpen = false;
        setFocused(false);
        Screen screen = Minecraft.getInstance().screen;
        if (screen != null) {
            screen.setFocused(null);
        }
    }

    public void focusForInput() {
        setFocused(true);
        Screen screen = Minecraft.getInstance().screen;
        if (screen != null) {
            screen.setFocused(this);
        }
    }

    public void clearAndFocus() {
        setValue("");
        focusForInput();
        doMoveCursorToEnd();
    }

    public void toggleToken(String token) {
        setValue(com.sanhiruzu.ami.index.query.QueryUtils.toggleToken(getValue(), token));
        focusForInput();
    }

    private void onTextChanged(String newValue) {
        updateColorSpans();
        updateSuggestions();
        if (!silentUpdate && !undoing && !newValue.equals(lastValue)) {
            undoStack.push(lastValue);
            if (undoStack.size() > 50) {
                undoStack.removeLast();
            }
        }
        if (!navigatingHistory && !undoing) {
            queryHistory.resetNavigation();
        }
        lastValue = newValue;

        if (!silentUpdate && listener != null) listener.onQueryChanged(newValue);
    }

    // ── Internal ──────────────────────────────────────────────────────────────

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

    private void updateSuggestions() {
        if (!isFocused()) {
            suggestions = List.of();
            selectedSuggestion = 0;
            suggestionScrollOffset = 0;
            return;
        }
        if (helpOpen) {
            suggestions = List.of();
            selectedSuggestion = 0;
            suggestionScrollOffset = 0;
            return;
        }
        suggestions = SearchSuggestions.suggest(GlobalIndex.getInstance(), getValue(), getCursorPosition(), MAX_SUGGESTIONS);
        selectedSuggestion = Mth.clamp(selectedSuggestion, 0, Math.max(0, suggestions.size() - 1));
        suggestionScrollOffset = Mth.clamp(suggestionScrollOffset, 0, maxSuggestionScrollOffset());
        ensureSelectedSuggestionVisible();
    }

    private boolean acceptSelectedSuggestion() {
        if (!hasVisibleSuggestions()) {
            return false;
        }
        SearchSuggestions.Suggestion suggestion = suggestions.get(Mth.clamp(selectedSuggestion, 0, suggestions.size() - 1));
        String next = SearchSuggestions.apply(getValue(), suggestion);
        setValue(next);
        doMoveCursorTo(Mth.clamp(suggestion.cursorAfterApply(), 0, next.length()), false);
        setHighlightPos(getCursorPosition());
        if (suggestion.replacement().endsWith(":")) {
            updateSuggestions();
        } else {
            suggestions = List.of();
            selectedSuggestion = 0;
            suggestionScrollOffset = 0;
        }
        return true;
    }

    public boolean handleSuggestionScroll(double scrollDelta) {
        if (!hasVisibleSuggestions() || suggestions.size() <= visibleSuggestionRows()) {
            return false;
        }
        int direction = scrollDelta > 0 ? -1 : 1;
        suggestionScrollOffset = Mth.clamp(suggestionScrollOffset + direction, 0, maxSuggestionScrollOffset());
        selectedSuggestion = Mth.clamp(selectedSuggestion, suggestionScrollOffset,
                Math.min(suggestions.size() - 1, suggestionScrollOffset + visibleSuggestionRows() - 1));
        return true;
    }

    public boolean isSuggestionPopupMouseOver(double mouseX, double mouseY) {
        if (!hasVisibleSuggestions()) {
            return false;
        }
        int popupX = getX();
        int popupY = suggestionPopupY();
        int popupH = visibleSuggestionRows() * SUGGESTION_ROW_HEIGHT + SUGGESTION_MARGIN * 2;
        return mouseX >= popupX && mouseX < popupX + width && mouseY >= popupY && mouseY < popupY + popupH;
    }

    public boolean isSearchOverlayMouseOver(double mouseX, double mouseY) {
        return isSuggestionPopupMouseOver(mouseX, mouseY) || isHelpPopupMouseOver(mouseX, mouseY);
    }

    private boolean hasVisibleSuggestions() {
        return isFocused() && !suggestions.isEmpty();
    }

    private int suggestionPopupY() {
        int popupH = visibleSuggestionRows() * SUGGESTION_ROW_HEIGHT + SUGGESTION_MARGIN * 2;
        Screen screen = Minecraft.getInstance().screen;
        int screenH = screen == null ? 0 : screen.height;
        int below = getY() + height + 2;
        if (screenH <= 0 || below + popupH <= screenH - 2) {
            return below;
        }
        return getY() - popupH - 2;
    }

    private int suggestionRowAt(double mouseY) {
        int localY = Mth.floor(mouseY) - suggestionPopupY() - SUGGESTION_MARGIN;
        if (localY < 0) {
            return -1;
        }
        int visibleRow = localY / SUGGESTION_ROW_HEIGHT;
        if (visibleRow < 0 || visibleRow >= visibleSuggestionRows()) {
            return -1;
        }
        return suggestionScrollOffset + visibleRow;
    }

    private int visibleSuggestionRows() {
        if (suggestions.isEmpty()) {
            return 0;
        }
        int configuredRows = Math.min(MAX_VISIBLE_SUGGESTIONS, suggestions.size());
        Screen screen = Minecraft.getInstance().screen;
        if (screen == null) {
            return configuredRows;
        }

        int belowSpace = screen.height - 2 - (getY() + height + 2) - SUGGESTION_MARGIN * 2;
        int aboveSpace = getY() - 2 - SUGGESTION_MARGIN * 2;
        int bestSpace = Math.max(belowSpace, aboveSpace);
        int rowsBySpace = bestSpace / SUGGESTION_ROW_HEIGHT;
        return Mth.clamp(rowsBySpace, 1, configuredRows);
    }

    private int maxSuggestionScrollOffset() {
        return Math.max(0, suggestions.size() - MAX_VISIBLE_SUGGESTIONS);
    }

    private void ensureSelectedSuggestionVisible() {
        if (!hasVisibleSuggestions()) {
            suggestionScrollOffset = 0;
            return;
        }
        if (selectedSuggestion < suggestionScrollOffset) {
            suggestionScrollOffset = selectedSuggestion;
        } else if (selectedSuggestion >= suggestionScrollOffset + visibleSuggestionRows()) {
            suggestionScrollOffset = selectedSuggestion - visibleSuggestionRows() + 1;
        }
        suggestionScrollOffset = Mth.clamp(suggestionScrollOffset, 0, maxSuggestionScrollOffset());
    }

    private int computeDisplayStart(Font font, int maxTextWidth) {
        String value = getValue();
        int cursor = getCursorPosition();
        if (font.width(value) <= maxTextWidth) return 0;
        int displayPos = cursor;
        while (displayPos > 0 && font.width(value.substring(displayPos - 1, cursor)) <= maxTextWidth) {
            displayPos--;
        }
        return displayPos;
    }

    private int cursorPositionFromMouse(double mouseX) {
        Font font = Minecraft.getInstance().font;
        int textX = getX() + 5;
        String value = getValue();
        int reservedActionW = HELP_BUTTON_W + 4 + (value.isEmpty() ? 0 : CLEAR_BUTTON_W);
        int maxTextWidth = Math.max(20, width - 10 - reservedActionW);
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
        if (value.isEmpty()) return;

        int pos = Mth.clamp(cursorIndex, 0, value.length());
        if (pos >= value.length() && pos > 0) pos--;
        if (pos < 0 || pos >= value.length()) return;

        if (Character.isWhitespace(value.charAt(pos))) {
            setCursorPosition(pos);
            setHighlightPos(pos);
            return;
        }

        int start = pos;
        while (start > 0 && !Character.isWhitespace(value.charAt(start - 1))) start--;

        int end = pos;
        while (end < value.length() && !Character.isWhitespace(value.charAt(end))) end++;

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

    private void renderSuggestions(GuiGraphics g, Font font, int mouseX, int mouseY) {
        if (!hasVisibleSuggestions()) {
            return;
        }
        int popupX = getX();
        int popupY = suggestionPopupY();
        int popupW = width;
        int visibleRows = visibleSuggestionRows();
        int popupH = visibleRows * SUGGESTION_ROW_HEIGHT + SUGGESTION_MARGIN * 2;

        g.fill(popupX, popupY, popupX + popupW, popupY + popupH, AMITheme.DROPDOWN_LIST_BG);
        g.fill(popupX, popupY, popupX + popupW, popupY + 1, AMITheme.BORDER_LIGHT);
        g.fill(popupX, popupY + popupH - 1, popupX + popupW, popupY + popupH, AMITheme.BORDER_DARK);
        g.fill(popupX, popupY, popupX + 1, popupY + popupH, AMITheme.BORDER_LIGHT);
        g.fill(popupX + popupW - 1, popupY, popupX + popupW, popupY + popupH, AMITheme.BORDER_DARK);

        int hoveredRow = isSuggestionPopupMouseOver(mouseX, mouseY) ? suggestionRowAt(mouseY) : -1;
        for (int visibleIndex = 0; visibleIndex < visibleRows; visibleIndex++) {
            int i = suggestionScrollOffset + visibleIndex;
            SearchSuggestions.Suggestion suggestion = suggestions.get(i);
            int rowY = popupY + SUGGESTION_MARGIN + visibleIndex * SUGGESTION_ROW_HEIGHT;
            boolean active = i == selectedSuggestion || i == hoveredRow;
            if (active) {
                g.fill(popupX + 1, rowY, popupX + popupW - 1, rowY + SUGGESTION_ROW_HEIGHT, AMITheme.DROPDOWN_BG_ACTIVE);
            }

            String detail = popupW >= 96
                    ? suggestion.example() ? "example" : suggestion.detail() == null ? "" : suggestion.detail()
                    : "";
            int detailW = detail.isBlank() ? 0 : font.width(detail);
            int scrollbarReserve = suggestions.size() > visibleRows ? 4 : 0;
            int labelMaxW = Math.max(20, popupW - 8 - detailW - scrollbarReserve - (detailW > 0 ? 8 : 0));
            String label = font.plainSubstrByWidth(suggestion.display(), labelMaxW);
            int textY = rowY + (SUGGESTION_ROW_HEIGHT - font.lineHeight) / 2 + 1;
            g.drawString(font, label, popupX + 4, textY, suggestionColor(suggestion.kind()), false);
            if (!detail.isBlank()) {
                g.drawString(font, detail, popupX + popupW - 4 - detailW, textY, AMITheme.TEXT_SUBTLE, false);
            }
        }

        if (suggestions.size() > visibleRows) {
            int trackX = popupX + popupW - 2;
            int trackY = popupY + SUGGESTION_MARGIN;
            int trackH = visibleRows * SUGGESTION_ROW_HEIGHT;
            int thumbH = Math.max(6, trackH * visibleRows / suggestions.size());
            int thumbY = trackY + (trackH - thumbH) * suggestionScrollOffset / maxSuggestionScrollOffset();
            g.fill(trackX, trackY, trackX + 1, trackY + trackH, AMITheme.SCROLL_TRACK);
            g.fill(trackX, thumbY, trackX + 1, thumbY + thumbH, AMITheme.SCROLL_THUMB_ACTIVE);
        }
    }

    private void renderHelpButton(GuiGraphics g, Font font, int mouseX, int mouseY) {
        int buttonX = helpButtonX();
        int buttonY = getY() + (height - 12) / 2;
        boolean hovered = isHelpButtonMouseOver(mouseX, mouseY);
        int fill = helpOpen ? AMITheme.DROPDOWN_BG_ACTIVE : hovered ? AMITheme.BUTTON_HOVER : 0x00000000;
        if (fill != 0) {
            g.fill(buttonX, buttonY, buttonX + 12, buttonY + 12, fill);
        }
        int color = helpOpen || hovered ? AMITheme.TEXT_HIGHLIGHT : AMITheme.TEXT_SUBTLE;
        g.drawString(font, Component.translatable("ami.gui.search.help_button"), buttonX + 3, buttonY + 2, color, false);
    }

    private void renderSearchHelp(GuiGraphics g, Font font, int mouseX, int mouseY) {
        int popupX = helpPopupX();
        int popupY = helpPopupY();
        int popupW = helpPopupW();
        int popupH = helpPopupH();

        g.pose().pushPose();
        g.pose().translate(0, 0, OverlayLayers.DROPDOWN);
        g.fill(popupX + 2, popupY + 3, popupX + popupW + 2, popupY + popupH + 3, AMITheme.SEARCH_HELP_SHADOW);
        AMITheme.drawRoundedBorder(g, popupX, popupY, popupW, popupH, AMITheme.SEARCH_HELP_BG, AMITheme.SEARCH_HELP_BORDER);
        g.fill(popupX + 2, popupY + 2, popupX + popupW - 2, popupY + 3, AMITheme.ACCENT_BLUE);

        drawHelpSections(g, font, allHelpSections(), popupX + HELP_MARGIN, popupY + HELP_MARGIN, popupW - HELP_MARGIN * 2);
        g.pose().popPose();
    }

    private int drawHelpSections(GuiGraphics g, Font font, List<SearchSyntax.HelpSection> sections, int x, int startY, int width) {
        int y = startY;
        int exampleW = 0;
        for (SearchSyntax.HelpSection section : sections) {
            for (SearchSyntax.Example example : section.examples()) {
                exampleW = Math.max(exampleW, font.width(example.text()));
            }
        }
        exampleW = Mth.clamp(exampleW + 13, 70, Math.max(70, width / 2 - 8));
        for (SearchSyntax.HelpSection section : sections) {
            if (y > startY) {
                y += HELP_SECTION_GAP;
            }
            g.drawString(font, Component.translatable(section.titleKey()), x, y,
                    AMITheme.SEARCH_HELP_TITLE, false);
            y += font.lineHeight + 3;
            g.fill(x, y, x + width, y + 1, AMITheme.SEARCH_HELP_SECTION_LINE);
            y += 4;
            for (SearchSyntax.Example helpExample : section.examples()) {
                int exampleColor = suggestionColor(helpExample.kind());
                String example = font.plainSubstrByWidth(helpExample.text(), Math.max(20, exampleW - 10));
                int descX = x + exampleW + 8;
                int descW = Math.max(40, width - exampleW - 8);
                Component description = Component.translatable(helpExample.descriptionKey());
                String desc = font.plainSubstrByWidth(description.getString(), descW);
                int textY = y + (HELP_ROW_HEIGHT - font.lineHeight) / 2 + 1;
                AMITheme.drawRoundedBorder(g, x, y + 1, exampleW, HELP_ROW_HEIGHT - 2,
                        AMITheme.SEARCH_HELP_CHIP_BG, AMITheme.SEARCH_HELP_CHIP_BORDER);
                g.drawString(font, example, x + 5, textY, exampleColor, false);
                g.drawString(font, desc, descX, textY, AMITheme.SEARCH_HELP_TEXT, false);
                y += HELP_ROW_HEIGHT;
            }
        }
        return y;
    }

    private int helpButtonX() {
        return getX() + width - HELP_BUTTON_W - 2;
    }

    private int clearButtonX() {
        return helpButtonX() - CLEAR_BUTTON_W;
    }

    private boolean isHelpButtonMouseOver(double mouseX, double mouseY) {
        int buttonX = helpButtonX();
        int buttonY = getY() + (height - 12) / 2;
        return mouseX >= buttonX && mouseX < buttonX + 12 && mouseY >= buttonY && mouseY < buttonY + 12;
    }

    private boolean isHelpPopupMouseOver(double mouseX, double mouseY) {
        if (!helpOpen) {
            return false;
        }
        int popupX = helpPopupX();
        int popupY = helpPopupY();
        int popupW = helpPopupW();
        int popupH = helpPopupH();
        return mouseX >= popupX && mouseX < popupX + popupW && mouseY >= popupY && mouseY < popupY + popupH;
    }

    private int helpPopupW() {
        Screen screen = Minecraft.getInstance().screen;
        int screenW = screen == null ? HELP_MAX_W : screen.width;
        int availableW = Math.max(120, screenW - 8);
        int preferredW = Mth.clamp(Math.max(AmiConfig.searchBarWidth, HELP_MIN_W), HELP_MIN_W, HELP_MAX_W);
        return Math.min(preferredW, availableW);
    }

    private int helpPopupH() {
        return HELP_MARGIN * 2 + helpColumnHeight(allHelpSections());
    }

    private int helpColumnHeight(List<SearchSyntax.HelpSection> sections) {
        int height = 0;
        for (SearchSyntax.HelpSection section : sections) {
            height += Minecraft.getInstance().font.lineHeight + 7 + section.examples().size() * HELP_ROW_HEIGHT;
        }
        return height + Math.max(0, sections.size() - 1) * HELP_SECTION_GAP;
    }

    private SearchSyntax.HelpLayout currentHelpLayout() {
        return SearchSuggestions.helpLayout(GlobalIndex.getInstance());
    }

    private List<SearchSyntax.HelpSection> allHelpSections() {
        SearchSyntax.HelpLayout layout = currentHelpLayout();
        java.util.ArrayList<SearchSyntax.HelpSection> sections = new java.util.ArrayList<>(layout.leftSections());
        sections.addAll(layout.rightSections());
        return List.copyOf(sections);
    }

    private int helpPopupX() {
        Screen screen = Minecraft.getInstance().screen;
        int screenW = screen == null ? 0 : screen.width;
        int popupW = helpPopupW();
        int x = helpButtonX() + 12 - popupW;
        if (screenW > 0) {
            x = Mth.clamp(x, 2, Math.max(2, screenW - popupW - 2));
        }
        return x;
    }

    private int helpPopupY() {
        int popupH = helpPopupH();
        Screen screen = Minecraft.getInstance().screen;
        int screenH = screen == null ? 0 : screen.height;
        int above = getY() - popupH - HELP_GAP;
        if (above >= 2 || screenH <= 0) {
            return Math.max(2, above);
        }
        int below = getY() + height + HELP_GAP;
        return Mth.clamp(below, 2, Math.max(2, screenH - popupH - 2));
    }

    private int suggestionColor(SearchSuggestions.Kind kind) {
        return switch (kind) {
            case PROPERTY -> AMITheme.TOKEN_PROP;
            case MOD -> AMITheme.MOD_NAME;
            case TAG -> com.sanhiruzu.ami.util.AmiColors.TAG_COLOR;
            case META -> AMITheme.TOKEN_META;
            case NUMERIC -> AMITheme.TOKEN_ESM;
            case CATEGORY -> AMITheme.TEXT_HIGHLIGHT;
            case ENVIRONMENT -> AMITheme.TOKEN_ENV;
            default -> AMITheme.SEARCH_DEFAULT_TEXT;
        };
    }

    public interface Listener {
        void onQueryChanged(String query);
    }
}
