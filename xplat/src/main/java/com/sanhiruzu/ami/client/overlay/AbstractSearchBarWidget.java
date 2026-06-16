package com.sanhiruzu.ami.client.overlay;

import com.sanhiruzu.ami.client.AMITheme;
import com.sanhiruzu.ami.client.AmiGuiIcons;
import com.sanhiruzu.ami.client.input.TextInputFilter;
import com.sanhiruzu.ami.client.results.SearchQueryHistory;
import com.sanhiruzu.ami.config.AmiConfig;
import com.sanhiruzu.ami.index.GlobalIndex;
import com.sanhiruzu.ami.index.query.SearchSuggestions;
import com.sanhiruzu.ami.index.query.SearchSyntax;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.CharacterEvent;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.input.MouseButtonInfo;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import org.lwjgl.glfw.GLFW;

import java.text.Normalizer;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.Locale;

public abstract class AbstractSearchBarWidget extends EditBox {
    private static final Component PLACEHOLDER_HINT = Component.translatable("ami.gui.search.placeholder_hint");
    private static final Component TYPING_HINT = Component.translatable("ami.gui.search.typing");
    private static final int MAX_SUGGESTIONS = 24;
    private static final int MAX_VISIBLE_SUGGESTIONS = 8;
    private static final int SUGGESTION_ROW_HEIGHT = 14;
    private static final int SUGGESTION_MARGIN = 2;
    private static final int LEFT_MOUSE_BUTTON = 0;
    private static final int RIGHT_MOUSE_BUTTON = 1;
    private static final long INVENTORY_FILTER_DOUBLE_CLICK_MS = 500L;
    private static final int CLEAR_BUTTON_W = 14;
    private static final int SEARCH_ICON_BUTTON_W = 16;
    private static final int HELP_MARGIN = 9;
    private static final int HELP_ROW_HEIGHT = 15;
    private static final int HELP_SECTION_GAP = 9;
    private static final int HELP_MIN_W = 420;
    private static final int HELP_MAX_W = 640;
    private static final int HELP_GAP = 6;
    private static final int HELP_COLUMN_GAP = 12;
    private static final int HELP_SCROLLBAR_W = 3;
    private static final int HELP_TWO_COLUMN_MIN_W = 500;
    private static final String STONE_EASTER_EGG_QUERY = "stone";
    private static final String STONE_EASTER_EGG_QUERY_RU = "камень";
    private static final String THANKS_FOR_RUSSIAN_SUPPORT = "Спасибо, Kotemorte!";
    private final Listener listener;
    private final Deque<String> undoStack = new ArrayDeque<>();
    private final SearchQueryHistory queryHistory = new SearchQueryHistory();
    private String lastValue = "";
    private long lastSearchBarClickTimeMs = 0;
    private boolean silentUpdate = false;
    private boolean undoing = false;
    private int cursorPos = 0;
    private int highlightPos = 0;
    private boolean navigatingHistory = false;

    private List<TokenColorizer.ColorSpan> colorSpans = List.of();
    private List<SearchSuggestions.Suggestion> suggestions = List.of();
    private List<HelpClickTarget> helpClickTargets = List.of();
    private HelpLayoutCache helpLayoutCache = HelpLayoutCache.empty();
    private SuggestionCache suggestionCache = SuggestionCache.empty();
    private int selectedSuggestion = 0;
    private int suggestionScrollOffset = 0;
    private int helpScrollOffset = 0;
    private boolean helpOpen = false;
    private boolean inventoryVisualFilterActive = false;

    protected AbstractSearchBarWidget(Listener listener) {
        super(Minecraft.getInstance().font, 0, 0, 160, 14, Component.empty());
        this.listener = listener;
        setMaxLength(256);
        setResponder(this::onTextChanged);
        setBordered(false);
        setFilter(TextInputFilter::isAllowedInput);
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
            resetInventoryFilterDoubleClick();
            suggestions = List.of();
            suggestionScrollOffset = 0;
        } else {
            updateSuggestions();
        }
    }

    // ── Focus ─────────────────────────────────────────────────────────────────

    @Override
    public void extractWidgetRenderState(GuiGraphicsExtractor g, int mouseX, int mouseY, float partialTick) {
        if (width <= 0 || height <= 0) return;
        Font font = Minecraft.getInstance().font;

        int x = getX(), y = getY(), w = width, h = height;
        boolean focused = isFocused();

        int bgColor = AMITheme.SEARCH_BAR_BG;
        int border = AMITheme.SEARCH_BAR_BORDER;

        if (!focused) {
            int alpha = (border >> 24) & 0xFF;
            border = (alpha / 2 << 24) | (border & 0x00FFFFFF);
        }

        AMITheme.fillControlChrome(g, x, y, w, h, bgColor, focused);
        g.fill(x, y, x + w, y + 1, border);

        int textX = searchTextX();
        int textY = y + (h - font.lineHeight) / 2 + 1;
        renderSearchIconButton(g, mouseX, mouseY);
        String value = getValue();
        int reservedActionW = value.isEmpty() ? 0 : CLEAR_BUTTON_W;
        int maxTextWidth = Math.max(20, w - (textX - x) - reservedActionW - 6);

        int displayStart = computeDisplayStart(font, maxTextWidth);

        if (value.isEmpty()) {
            Component hint = focused ? TYPING_HINT : PLACEHOLDER_HINT;
            g.enableScissor(textX, textY - 1, textX + maxTextWidth, textY + font.lineHeight + 1);
            g.text(font, hint, textX, textY, AMITheme.SEARCH_PLACEHOLDER, false);
            g.disableScissor();
        } else {
            String visibleText = value.substring(displayStart);
            renderSelection(g, font, textX, textY, visibleText, displayStart, maxTextWidth);
            drawColorizedText(g, font, textX, textY, visibleText, displayStart, maxTextWidth);

            int clearX = clearButtonX();
            int clearY = y + (h - 10) / 2;
            boolean hoveredX = mouseX >= clearX && mouseX < clearX + 12 && mouseY >= clearY && mouseY < clearY + 10;
            g.text(font, Component.translatable("ami.gui.search.clear"), clearX + 3, clearY, hoveredX ? AMITheme.SEARCH_CLEAR_TEXT_HOVER : AMITheme.SEARCH_CLEAR_TEXT, false);
        }

        if (focused && (System.currentTimeMillis() % 1000) < 500) {
            int cursorInVisible = Math.max(0, Math.min(getCursorPosition() - displayStart, value.length() - displayStart));
            int cursorX = textX + font.width(value.substring(displayStart, displayStart + cursorInVisible)) + 1;
            g.fill(cursorX, textY - 1, cursorX + 1, textY + font.lineHeight, AMITheme.SEARCH_CURSOR);
        }

        if (shouldRenderKoteEasterEgg()) {
            drawKoteEasterEgg(g, font);
        }

        if (helpOpen) {
            renderSearchHelp(g, font, mouseX, mouseY);
        } else {
            renderSuggestions(g, font, mouseX, mouseY);
        }

        if (inventoryVisualFilterActive) {
            int activeBorder = 0xFFEEEE00;
            g.fill(x - 1, y - 1, x + w + 1, y, activeBorder);
            g.fill(x - 1, y + h, x + w + 1, y + h + 1, activeBorder);
            g.fill(x - 1, y - 1, x, y + h + 1, activeBorder);
            g.fill(x + w, y - 1, x + w + 1, y + h + 1, activeBorder);
        }
    }

    // ── Rendering ─────────────────────────────────────────────────────────────

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        double mouseX = event.x();
        double mouseY = event.y();
        int button = event.button();
        if (handleSearchOverlayClick(mouseX, mouseY, button)) {
            return true;
        }
        if (!isMouseOver(mouseX, mouseY)) return false;

        if (button == LEFT_MOUSE_BUTTON) return handleSearchBarLeftClick(mouseX, mouseY);
        if (button == RIGHT_MOUSE_BUTTON) return handleSearchBarRightClick();

        resetInventoryFilterDoubleClick();
        return super.mouseClicked(event, doubleClick);
    }

    private boolean handleSearchOverlayClick(double mouseX, double mouseY, int button) {
        if (button != LEFT_MOUSE_BUTTON) {
            return false;
        }
        if (isSuggestionPopupMouseOver(mouseX, mouseY)) {
            resetInventoryFilterDoubleClick();
            return acceptSuggestionAt(mouseY);
        }
        if (isHelpPopupMouseOver(mouseX, mouseY)) {
            resetInventoryFilterDoubleClick();
            HelpClickTarget target = helpTargetAt(mouseX, mouseY);
            if (target != null) {
                applyHelpExample(target.example());
            }
            return true;
        }
        return false;
    }

    private boolean handleSearchBarLeftClick(double mouseX, double mouseY) {
        long now = System.currentTimeMillis();
        if (isInventoryFilterDoubleClick(now)) {
            focusForInput();
            listener.onSearchBarDoubleClicked(getValue());
            resetInventoryFilterDoubleClick();
            return true;
        }

        if (isSearchIconMouseOver(mouseX, mouseY)) {
            toggleSearchHelp();
            focusForInput();
            rememberInventoryFilterClick(now);
            return true;
        }
        if (isClearButtonMouseOver(mouseX, mouseY)) {
            clearAndFocus();
            rememberInventoryFilterClick(now);
            return true;
        }

        super.mouseClicked(new MouseButtonEvent(mouseX, mouseY, new MouseButtonInfo(LEFT_MOUSE_BUTTON, 0)), false);
        focusForInput();
        rememberInventoryFilterClick(now);
        return true;
    }

    private boolean handleSearchBarRightClick() {
        resetInventoryFilterDoubleClick();
        clearAndFocus();
        return true;
    }

    private boolean acceptSuggestionAt(double mouseY) {
        int row = suggestionRowAt(mouseY);
        if (row < 0 || row >= suggestions.size()) {
            return true;
        }
        selectedSuggestion = row;
        acceptSelectedSuggestion();
        return true;
    }

    private void toggleSearchHelp() {
        helpOpen = !helpOpen;
        suggestions = List.of();
        suggestionScrollOffset = 0;
        helpScrollOffset = 0;
    }

    private void applyHelpExample(String example) {
        if (example == null || example.isBlank()) {
            return;
        }
        setValue(example);
        doMoveCursorToEnd();
        setHighlightPos(getCursorPosition());
        focusForInput();
        helpOpen = false;
        helpClickTargets = List.of();
        helpScrollOffset = 0;
    }

    private boolean isClearButtonMouseOver(double mouseX, double mouseY) {
        if (getValue().isEmpty()) {
            return false;
        }
        int clearX = clearButtonX();
        int clearY = getY() + (height - 10) / 2;
        return mouseX >= clearX && mouseX < clearX + 12 && mouseY >= clearY && mouseY < clearY + 10;
    }

    private boolean isInventoryFilterDoubleClick(long now) {
        return lastSearchBarClickTimeMs > 0
                && now - lastSearchBarClickTimeMs < INVENTORY_FILTER_DOUBLE_CLICK_MS;
    }

    private void rememberInventoryFilterClick(long now) {
        lastSearchBarClickTimeMs = now;
    }

    private void resetInventoryFilterDoubleClick() {
        lastSearchBarClickTimeMs = 0;
    }

    // ── Mouse ─────────────────────────────────────────────────────────────────

    @Override
    public void onClick(MouseButtonEvent event, boolean doubleClick) {
        doMoveCursorTo(cursorPositionFromMouse(event.x()), Minecraft.getInstance().hasShiftDown());
    }

    @Override
    protected void onDrag(MouseButtonEvent event, double dx, double dy) {
        doMoveCursorTo(cursorPositionFromMouse(event.x()), true);
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
    public boolean keyPressed(KeyEvent event) {
        int keyCode = event.key();
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
        if (Minecraft.getInstance().hasControlDown() && keyCode == GLFW.GLFW_KEY_Z) {
            undoLastEdit();
            return true;
        }

        if (Minecraft.getInstance().hasControlDown()) {
            if (keyCode == GLFW.GLFW_KEY_LEFT) {
                moveCursorTokenWise(-1, Minecraft.getInstance().hasShiftDown());
                return true;
            } else if (keyCode == GLFW.GLFW_KEY_RIGHT) {
                moveCursorTokenWise(1, Minecraft.getInstance().hasShiftDown());
                return true;
            }
        }

        super.keyPressed(event);
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
        listener.onSearchBarCleared();
    }

    public void setInventoryVisualFilterActive(boolean active) {
        inventoryVisualFilterActive = active;
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

        if (!silentUpdate && listener != null) {
            listener.onQueryChanged(newValue);
        }
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
        GlobalIndex index = GlobalIndex.getInstance();
        long revision = index.revision();
        String value = getValue();
        int cursor = getCursorPosition();
        boolean showCreativeItems = shouldShowCreativeItems();
        if (!isFocused()) {
            suggestions = List.of();
            selectedSuggestion = 0;
            suggestionScrollOffset = 0;
            suggestionCache = SuggestionCache.empty();
            return;
        }
        if (helpOpen) {
            suggestions = List.of();
            selectedSuggestion = 0;
            suggestionScrollOffset = 0;
            return;
        }
        if (suggestionCache.matches(
                revision,
                value,
                cursor,
                AmiConfig.cheatMode,
                AmiConfig.devMode,
                AmiConfig.showHiddenModItems,
                showCreativeItems)) {
            suggestions = suggestionCache.suggestions();
        } else {
            suggestions = SearchSuggestions.suggest(index, value, cursor, MAX_SUGGESTIONS, showCreativeItems);
            suggestionCache = new SuggestionCache(revision, value, cursor,
                    AmiConfig.cheatMode, AmiConfig.devMode, AmiConfig.showHiddenModItems, showCreativeItems, suggestions);
        }
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
        if (helpOpen) {
            return handleHelpScroll(scrollDelta);
        }
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
        return Math.max(0, suggestions.size() - visibleSuggestionRows());
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
        int textX = searchTextX();
        String value = getValue();
        int reservedActionW = value.isEmpty() ? 0 : CLEAR_BUTTON_W;
        int maxTextWidth = Math.max(20, width - (textX - getX()) - reservedActionW - 6);
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

    private void renderSelection(GuiGraphicsExtractor g, Font font, int textX, int textY, String visibleText, int displayStart, int maxTextWidth) {
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
        g.textHighlight(startX, textY - 1, endX, textY + font.lineHeight, false);
    }

    private void drawColorizedText(GuiGraphicsExtractor g, Font font, int startX, int startY, String visibleText, int scrollStart, int maxTextWidth) {
        if (visibleText.isEmpty()) return;

        g.enableScissor(startX, startY - 1, startX + maxTextWidth, startY + font.lineHeight + 1);
        try {
            if (colorSpans.isEmpty()) {
                g.text(font, visibleText, startX, startY, AMITheme.SEARCH_DEFAULT_TEXT, false);
            } else {
                int currentX = startX;
                int coveredUntil = 0;
                for (TokenColorizer.ColorSpan span : colorSpans) {
                    int sStart = Math.max(span.startIndex() - scrollStart, 0);
                    int sEnd = Math.min(span.endIndex() - scrollStart, visibleText.length());
                    if (sEnd <= sStart || sStart >= visibleText.length()) continue;
                    if (sStart > coveredUntil) {
                        String gap = visibleText.substring(coveredUntil, sStart);
                        g.text(font, gap, currentX, startY, AMITheme.SEARCH_DEFAULT_TEXT, false);
                        currentX += font.width(gap);
                    }
                    String spanText = visibleText.substring(sStart, sEnd);
                    if (!spanText.isEmpty()) {
                        g.text(font, spanText, currentX, startY, span.argbColor(), false);
                        currentX += font.width(spanText);
                    }
                    coveredUntil = sEnd;
                }
                if (coveredUntil < visibleText.length()) {
                    g.text(font, visibleText.substring(coveredUntil), currentX, startY, AMITheme.SEARCH_DEFAULT_TEXT, false);
                }
            }
        } finally {
            g.disableScissor();
        }
    }

    private boolean shouldRenderKoteEasterEgg() {
        if (isFocused() || !getValue().isBlank()) {
            String normalized = normalizeForEasterEgg(getValue());
            return normalized.contains(STONE_EASTER_EGG_QUERY) || normalized.contains(STONE_EASTER_EGG_QUERY_RU);
        }
        return false;
    }

    private void drawKoteEasterEgg(GuiGraphicsExtractor g, Font font) {
        if (!shouldRenderKoteEasterEgg()) {
            return;
        }

        int noteY = getY() + height + 2;
        int noteX = getX();
        Screen screen = Minecraft.getInstance().screen;
        int screenH = screen == null ? Integer.MAX_VALUE : screen.height;

        if (noteY + font.lineHeight > screenH - 1) {
            noteY = getY() - font.lineHeight - 2;
        }
        if (noteY < 0) {
            return;
        }

        g.text(font, Component.literal(THANKS_FOR_RUSSIAN_SUPPORT), noteX, noteY, AMITheme.TEXT_SUBTLE, false);
    }

    private static String normalizeForEasterEgg(String value) {
        if (value == null) return "";
        return Normalizer.normalize(value, Normalizer.Form.NFKC)
                .toLowerCase(Locale.ROOT)
                .trim();
    }

    private void renderSuggestions(GuiGraphicsExtractor g, Font font, int mouseX, int mouseY) {
        if (!hasVisibleSuggestions()) {
            return;
        }
        g.pose().pushMatrix();
        try {
            int popupX = getX();
            int popupY = suggestionPopupY();
            int popupW = width;
            int visibleRows = visibleSuggestionRows();
            int popupH = visibleRows * SUGGESTION_ROW_HEIGHT + SUGGESTION_MARGIN * 2;

            AMITheme.fillSuggestionPopup(g, popupX, popupY, popupW, popupH);

            int hoveredRow = isSuggestionPopupMouseOver(mouseX, mouseY) ? suggestionRowAt(mouseY) : -1;
            for (int visibleIndex = 0; visibleIndex < visibleRows; visibleIndex++) {
                int i = suggestionScrollOffset + visibleIndex;
                SearchSuggestions.Suggestion suggestion = suggestions.get(i);
                int rowY = popupY + SUGGESTION_MARGIN + visibleIndex * SUGGESTION_ROW_HEIGHT;
                boolean active = i == selectedSuggestion || i == hoveredRow;
                if (active) {
                    g.fill(popupX + 1, rowY, popupX + popupW - 1, rowY + SUGGESTION_ROW_HEIGHT, AMITheme.DROPDOWN_BG_ACTIVE);
                }

                String detail = popupW >= 96 && !suggestion.example() && suggestion.detail() != null
                        ? suggestion.detail()
                        : "";
                int detailW = detail.isBlank() ? 0 : font.width(detail);
                int scrollbarReserve = suggestions.size() > visibleRows ? 4 : 0;
                int labelMaxW = Math.max(20, popupW - 8 - detailW - scrollbarReserve - (detailW > 0 ? 8 : 0));
                String label = font.plainSubstrByWidth(suggestion.display(), labelMaxW);
                int textY = rowY + (SUGGESTION_ROW_HEIGHT - font.lineHeight) / 2 + 1;
                g.text(font, label, popupX + 4, textY, suggestionColor(suggestion.kind()), false);
                if (!detail.isBlank()) {
                    g.text(font, detail, popupX + popupW - 4 - detailW, textY, AMITheme.TEXT_SUBTLE, false);
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
        } finally {
            g.pose().popMatrix();
        }
    }

    private void renderSearchIconButton(GuiGraphicsExtractor g, int mouseX, int mouseY) {
        int buttonX = searchIconButtonX();
        int buttonY = searchIconButtonY();
        boolean hovered = isSearchIconMouseOver(mouseX, mouseY);
        int fill = helpOpen ? AMITheme.DROPDOWN_BG_ACTIVE : hovered ? AMITheme.DROPDOWN_BG_ACTIVE : 0x00000000;
        if (fill != 0) {
            g.fill(buttonX, buttonY, buttonX + SEARCH_ICON_BUTTON_W, buttonY + SEARCH_ICON_BUTTON_W, fill);
        }
        int color = helpOpen || hovered || isFocused() ? AMITheme.TEXT_HEADER : AMITheme.SEARCH_PLACEHOLDER;
        AmiGuiIcons.search(g, buttonX + SEARCH_ICON_BUTTON_W / 2, getY() + height / 2, color);
    }

    private void renderSearchHelp(GuiGraphicsExtractor g, Font font, int mouseX, int mouseY) {
        int popupX = helpPopupX();
        int popupY = helpPopupY();
        int popupW = helpPopupW();
        int popupH = helpPopupH();
        HelpLayoutCache cache = helpLayoutCache();
        SearchSyntax.HelpLayout layout = cache.layout();
        ArrayList<HelpClickTarget> targets = new ArrayList<>();
        int maxScroll = maxHelpScrollOffset(cache, popupW, popupH);
        helpScrollOffset = Mth.clamp(helpScrollOffset, 0, maxScroll);

        g.pose().pushMatrix();
        AMITheme.fillPixelPopup(g, popupX, popupY, popupW, popupH,
                AMITheme.SEARCH_HELP_BG, AMITheme.SEARCH_HELP_BORDER, AMITheme.SEARCH_HELP_SHADOW, AMITheme.ACCENT_BLUE);

        int innerX = popupX + HELP_MARGIN;
        int innerY = popupY + HELP_MARGIN;
        int innerW = popupW - HELP_MARGIN * 2 - (maxScroll > 0 ? HELP_SCROLLBAR_W + 4 : 0);
        int innerH = Math.max(1, popupH - HELP_MARGIN * 2);
        boolean twoColumns = useTwoHelpColumns(popupW);

        g.enableScissor(innerX, innerY, innerX + innerW, innerY + innerH);
        try {
            int contentY = innerY - helpScrollOffset;
            if (twoColumns) {
                int columnW = (innerW - HELP_COLUMN_GAP) / 2;
                drawHelpSections(g, font, layout.leftSections(), cache.leftExampleWidth(), innerX, contentY, columnW, innerY, innerY + innerH, mouseX, mouseY, targets);
                drawHelpSections(g, font, layout.rightSections(), cache.rightExampleWidth(), innerX + columnW + HELP_COLUMN_GAP, contentY, columnW, innerY, innerY + innerH, mouseX, mouseY, targets);
            } else {
                List<SearchSyntax.HelpSection> sections = combinedHelpSections(layout);
                drawHelpSections(g, font, sections, cache.combinedExampleWidth(), innerX, contentY, innerW, innerY, innerY + innerH, mouseX, mouseY, targets);
            }
        } finally {
            g.disableScissor();
        }
        if (maxScroll > 0) {
            drawHelpScrollbar(g, popupX, popupY, popupW, popupH, maxScroll);
        }
        helpClickTargets = List.copyOf(targets);
        g.pose().popMatrix();
    }

    private void drawHelpScrollbar(GuiGraphicsExtractor g, int popupX, int popupY, int popupW, int popupH, int maxScroll) {
        int trackX = popupX + popupW - HELP_MARGIN + 2;
        int trackY = popupY + HELP_MARGIN;
        int trackH = Math.max(1, popupH - HELP_MARGIN * 2);
        int contentH = helpContentH(helpLayoutCache(), popupW);
        int thumbH = Mth.clamp(trackH * trackH / Math.max(trackH, contentH), 8, trackH);
        int thumbY = trackY + (trackH - thumbH) * helpScrollOffset / maxScroll;
        g.fill(trackX, trackY, trackX + 1, trackY + trackH, AMITheme.SCROLL_TRACK);
        g.fill(trackX, thumbY, trackX + HELP_SCROLLBAR_W, thumbY + thumbH, AMITheme.SCROLL_THUMB_ACTIVE);
    }

    private int drawHelpSections(GuiGraphicsExtractor g, Font font, List<SearchSyntax.HelpSection> sections, int exampleW,
                                 int x, int startY, int width, int visibleTop, int visibleBottom, int mouseX, int mouseY,
                                 List<HelpClickTarget> targets) {
        int y = startY;
        exampleW = Mth.clamp(exampleW, 70, Math.max(70, width / 2 - 8));
        for (SearchSyntax.HelpSection section : sections) {
            if (y > startY) {
                y += HELP_SECTION_GAP;
            }
            if (y + font.lineHeight >= visibleTop && y <= visibleBottom) {
                g.text(font, Component.translatable(section.titleKey()), x, y,
                        AMITheme.SEARCH_HELP_TITLE, false);
            }
            y += font.lineHeight + 3;
            if (y >= visibleTop && y <= visibleBottom) {
                g.fill(x, y, x + width, y + 1, AMITheme.SEARCH_HELP_SECTION_LINE);
            }
            y += 4;
            for (SearchSyntax.Example helpExample : section.examples()) {
                boolean visible = y + HELP_ROW_HEIGHT > visibleTop && y < visibleBottom;
                if (!visible) {
                    y += HELP_ROW_HEIGHT;
                    continue;
                }
                int exampleColor = suggestionColor(helpExample.kind());
                String example = font.plainSubstrByWidth(helpExample.text(), Math.max(20, exampleW - 10));
                int descX = x + exampleW + 8;
                int descW = Math.max(40, width - exampleW - 8);
                Component description = Component.translatable(helpExample.descriptionKey());
                String desc = font.plainSubstrByWidth(description.getString(), descW);
                int textY = y + (HELP_ROW_HEIGHT - font.lineHeight) / 2 + 1;
                boolean hovered = mouseX >= x && mouseX < x + exampleW
                        && mouseY >= y + 1 && mouseY < y + HELP_ROW_HEIGHT - 1;
                AMITheme.fillBorderedRect(g, x, y + 1, exampleW, HELP_ROW_HEIGHT - 2,
                        hovered ? AMITheme.DROPDOWN_BG_ACTIVE : AMITheme.SEARCH_HELP_CHIP_BG,
                        hovered ? AMITheme.ACCENT_BLUE : AMITheme.SEARCH_HELP_CHIP_BORDER);
                g.text(font, example, x + 5, textY, exampleColor, false);
                g.text(font, desc, descX, textY, AMITheme.SEARCH_HELP_TEXT, false);
                targets.add(new HelpClickTarget(x, y + 1, exampleW, HELP_ROW_HEIGHT - 2, helpExample.text()));
                y += HELP_ROW_HEIGHT;
            }
        }
        return y;
    }

    private int searchIconButtonX() {
        return getX() + 1;
    }

    private int searchIconButtonY() {
        return getY() + (height - SEARCH_ICON_BUTTON_W) / 2;
    }

    private int searchTextX() {
        return getX() + SEARCH_ICON_BUTTON_W + 3;
    }

    private int clearButtonX() {
        return getX() + width - CLEAR_BUTTON_W - 2;
    }

    private boolean isSearchIconMouseOver(double mouseX, double mouseY) {
        int buttonX = searchIconButtonX();
        int buttonY = searchIconButtonY();
        return mouseX >= buttonX && mouseX < buttonX + SEARCH_ICON_BUTTON_W
                && mouseY >= buttonY && mouseY < buttonY + SEARCH_ICON_BUTTON_W;
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
        int popupW = helpPopupW();
        int naturalH = HELP_MARGIN * 2 + helpContentH(helpLayoutCache(), popupW);
        Screen screen = Minecraft.getInstance().screen;
        int screenH = screen == null ? 0 : screen.height;
        if (screenH <= 0) {
            return naturalH;
        }
        int availableAbove = Math.max(0, getY() - HELP_GAP - 2);
        int availableBelow = Math.max(0, screenH - (getY() + height + HELP_GAP) - 2);
        int available = Math.max(availableAbove, availableBelow);
        return Mth.clamp(naturalH, HELP_MARGIN * 2 + HELP_ROW_HEIGHT * 3, Math.max(HELP_MARGIN * 2 + HELP_ROW_HEIGHT * 3, available));
    }

    private int helpContentH(HelpLayoutCache cache, int popupW) {
        if (useTwoHelpColumns(popupW)) {
            return Math.max(cache.leftHeight(), cache.rightHeight());
        }
        return cache.combinedHeight();
    }

    private int helpColumnHeight(List<SearchSyntax.HelpSection> sections) {
        int height = 0;
        for (SearchSyntax.HelpSection section : sections) {
            height += Minecraft.getInstance().font.lineHeight + 7 + section.examples().size() * HELP_ROW_HEIGHT;
        }
        return height + Math.max(0, sections.size() - 1) * HELP_SECTION_GAP;
    }

    private HelpLayoutCache helpLayoutCache() {
        GlobalIndex index = GlobalIndex.getInstance();
        long revision = index.revision();
        boolean showCreativeItems = shouldShowCreativeItems();
        HelpLayoutCache cached = helpLayoutCache;
        if (cached.matches(
                revision,
                AmiConfig.cheatMode,
                AmiConfig.devMode,
                AmiConfig.showHiddenModItems,
                showCreativeItems)) {
            return cached;
        }
        SearchSyntax.HelpLayout layout = SearchSuggestions.helpLayout(index, showCreativeItems);
        HelpLayoutCache updated = new HelpLayoutCache(
                revision,
                AmiConfig.cheatMode,
                AmiConfig.devMode,
                AmiConfig.showHiddenModItems,
                showCreativeItems,
                layout,
                helpColumnHeight(layout.leftSections()),
                helpColumnHeight(layout.rightSections()),
                helpColumnHeight(combinedHelpSections(layout)),
                helpExampleWidth(layout.leftSections()),
                helpExampleWidth(layout.rightSections()),
                helpExampleWidth(combinedHelpSections(layout))
        );
        helpLayoutCache = updated;
        return updated;
    }

    private int helpExampleWidth(List<SearchSyntax.HelpSection> sections) {
        Font font = Minecraft.getInstance().font;
        int width = 0;
        for (SearchSyntax.HelpSection section : sections) {
            for (SearchSyntax.Example example : section.examples()) {
                width = Math.max(width, font.width(example.text()));
            }
        }
        return width + 13;
    }

    private List<SearchSyntax.HelpSection> combinedHelpSections(SearchSyntax.HelpLayout layout) {
        java.util.ArrayList<SearchSyntax.HelpSection> sections = new java.util.ArrayList<>(layout.leftSections());
        sections.addAll(layout.rightSections());
        return List.copyOf(sections);
    }

    private boolean useTwoHelpColumns(int popupW) {
        return popupW >= HELP_TWO_COLUMN_MIN_W;
    }

    private int maxHelpScrollOffset(HelpLayoutCache cache, int popupW, int popupH) {
        return Math.max(0, helpContentH(cache, popupW) - Math.max(1, popupH - HELP_MARGIN * 2));
    }

    private boolean handleHelpScroll(double scrollDelta) {
        int popupW = helpPopupW();
        int popupH = helpPopupH();
        int maxScroll = maxHelpScrollOffset(helpLayoutCache(), popupW, popupH);
        if (maxScroll <= 0) {
            helpScrollOffset = 0;
            return false;
        }
        int direction = scrollDelta > 0 ? -1 : 1;
        helpScrollOffset = Mth.clamp(helpScrollOffset + direction * HELP_ROW_HEIGHT, 0, maxScroll);
        return true;
    }

    private HelpClickTarget helpTargetAt(double mouseX, double mouseY) {
        for (HelpClickTarget target : helpClickTargets) {
            if (target.contains(mouseX, mouseY)) {
                return target;
            }
        }
        return null;
    }

    private int helpPopupX() {
        Screen screen = Minecraft.getInstance().screen;
        int screenW = screen == null ? 0 : screen.width;
        int popupW = helpPopupW();
        int x = getX();
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

        default void onSearchBarDoubleClicked(String query) {
        }

        default void onSearchBarCleared() {
        }
    }

    private record HelpClickTarget(int x, int y, int width, int height, String example) {
        boolean contains(double mouseX, double mouseY) {
            return mouseX >= x && mouseX < x + width && mouseY >= y && mouseY < y + height;
        }
    }

    private record HelpLayoutCache(long revision, boolean cheatMode, boolean devMode, boolean showHidden,
                                   boolean showCreativeItems,
                                   SearchSyntax.HelpLayout layout,
                                   int leftHeight, int rightHeight, int combinedHeight,
                                   int leftExampleWidth, int rightExampleWidth, int combinedExampleWidth) {
        static HelpLayoutCache empty() {
            return new HelpLayoutCache(Long.MIN_VALUE,
                    false, false, false, false,
                    new SearchSyntax.HelpLayout(List.of(), List.of()),
                    0, 0, 0,
                    70, 70, 70);
        }

        boolean matches(long revision, boolean cheatMode, boolean devMode, boolean showHidden, boolean showCreativeItems) {
            return this.revision == revision
                    && this.cheatMode == cheatMode
                    && this.devMode == devMode
                    && this.showHidden == showHidden
                    && this.showCreativeItems == showCreativeItems;
        }
    }

    private record SuggestionCache(long revision, String value, int cursor, boolean cheatMode, boolean devMode,
                                   boolean showHidden, boolean showCreativeItems, List<SearchSuggestions.Suggestion> suggestions) {
        static SuggestionCache empty() {
            return new SuggestionCache(Long.MIN_VALUE, "", -1, false, false, false, false, List.of());
        }

        boolean matches(long revision, String value, int cursor, boolean cheatMode, boolean devMode, boolean showHidden, boolean showCreativeItems) {
            return this.revision == revision
                    && this.cursor == cursor
                    && this.cheatMode == cheatMode
                    && this.devMode == devMode
                    && this.showHidden == showHidden
                    && this.showCreativeItems == showCreativeItems
                    && this.value.equals(value);
        }
    }

    private static boolean shouldShowCreativeItems() {
        var gameMode = Minecraft.getInstance().gameMode;
        net.minecraft.world.level.GameType playerMode = gameMode == null ? null : gameMode.getPlayerMode();
        return AmiConfig.shouldShowCreativeItems(playerMode);
    }
}
