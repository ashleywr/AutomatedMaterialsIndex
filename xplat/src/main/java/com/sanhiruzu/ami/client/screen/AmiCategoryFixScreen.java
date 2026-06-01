package com.sanhiruzu.ami.client.screen;

import com.sanhiruzu.ami.client.AMITheme;
import com.sanhiruzu.ami.config.AmiDataFixes;
import com.sanhiruzu.ami.index.AmiIndexerService;
import com.sanhiruzu.ami.index.AmiOntology;
import com.sanhiruzu.ami.index.SearchNode;
import com.sanhiruzu.ami.index.SearchNodeKeys;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class AmiCategoryFixScreen extends Screen {
    private static final int PANEL_WIDTH = 320;
    private static final int ROW_HEIGHT = 14;
    private static final int MAX_SUGGESTIONS = 6;

    private final Screen parent;
    private final SearchNode node;
    private EditBox categoryBox;
    private EditBox subcategoryBox;

    public AmiCategoryFixScreen(Screen parent, SearchNode node) {
        super(Component.translatable("ami.fix_category.title"));
        this.parent = parent;
        this.node = node;
    }

    @Override
    protected void init() {
        super.init();
        int left = (width - PANEL_WIDTH) / 2;
        int top = Math.max(24, height / 2 - 76);
        int fieldWidth = PANEL_WIDTH - 24;

        String category = node.meta(SearchNodeKeys.ONTOLOGY_CATEGORY, "");
        if (category.isBlank()) {
            category = AmiOntology.classifyNode(node).id;
        }
        String subcategory = node.meta(SearchNodeKeys.ONTOLOGY_SUBCATEGORY, "");

        categoryBox = new EditBox(font, left + 12, top + 44, fieldWidth, 20, Component.translatable("ami.fix_category.category"));
        categoryBox.setValue(category);
        categoryBox.setResponder(ignored -> {});
        addRenderableWidget(categoryBox);

        subcategoryBox = new EditBox(font, left + 12, top + 82, fieldWidth, 20, Component.translatable("ami.fix_category.subcategory"));
        subcategoryBox.setValue(subcategory);
        subcategoryBox.setResponder(ignored -> {});
        addRenderableWidget(subcategoryBox);

        addRenderableWidget(Button.builder(Component.translatable("ami.fix_category.save"), button -> apply())
                .bounds(left + 12, top + 124, 86, 20)
                .build());
        addRenderableWidget(Button.builder(Component.translatable("ami.fix_category.clear"), button -> clear())
                .bounds(left + 104, top + 124, 86, 20)
                .build());
        addRenderableWidget(Button.builder(Component.translatable("gui.cancel"), button -> onClose())
                .bounds(left + PANEL_WIDTH - 98, top + 124, 86, 20)
                .build());

        setInitialFocus(categoryBox);
    }

    private void renderPanel(GuiGraphics g) {
        int left = (width - PANEL_WIDTH) / 2;
        int top = Math.max(24, height / 2 - 76);
        int panelHeight = 158;
        g.fill(left, top, left + PANEL_WIDTH, top + panelHeight, AMITheme.DROPDOWN_LIST_BG);
        g.fill(left, top, left + PANEL_WIDTH, top + 1, AMITheme.SECTION_SEP);
        g.fill(left, top + panelHeight - 1, left + PANEL_WIDTH, top + panelHeight, AMITheme.SECTION_SEP);
        g.fill(left, top, left + 1, top + panelHeight, AMITheme.SECTION_SEP);
        g.fill(left + PANEL_WIDTH - 1, top, left + PANEL_WIDTH, top + panelHeight, AMITheme.SECTION_SEP);
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        g.fill(0, 0, width, height, 0x88000000);
        renderPanel(g);
        super.render(g, mouseX, mouseY, partialTick);
        int left = (width - PANEL_WIDTH) / 2;
        int top = Math.max(24, height / 2 - 76);

        g.drawString(font, title, left + 12, top + 10, AMITheme.TEXT_HEADER, false);
        g.drawString(font, Component.literal(node.displayName()), left + 12, top + 24, AMITheme.TEXT_SUBTLE, false);
        g.drawString(font, Component.translatable("ami.fix_category.category"), left + 12, top + 34, AMITheme.TEXT_SUBTLE, false);
        g.drawString(font, Component.translatable("ami.fix_category.subcategory"), left + 12, top + 72, AMITheme.TEXT_SUBTLE, false);
        renderSuggestions(g, mouseX, mouseY);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0 && acceptSuggestionAt((int) mouseX, (int) mouseY)) {
            return true;
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (keyCode == GLFW.GLFW_KEY_ENTER || keyCode == GLFW.GLFW_KEY_KP_ENTER) {
            apply();
            return true;
        }
        if (keyCode == GLFW.GLFW_KEY_TAB && acceptFirstSuggestion()) {
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    private void apply() {
        String category = normalizePart(categoryBox.getValue(), false);
        if (category.isBlank()) return;

        String subcategory = normalizePart(subcategoryBox.getValue(), true);
        AmiDataFixes.putUserMetadataFix(node.id(), node.type(), Map.of(
                SearchNodeKeys.ONTOLOGY_CATEGORY, category,
                SearchNodeKeys.ONTOLOGY_SUBCATEGORY, subcategory
        ));
        AmiIndexerService.getInstance().rebuild();
        onClose();
    }

    private void clear() {
        AmiDataFixes.removeUserFix(node.id(), node.type());
        AmiIndexerService.getInstance().rebuild();
        onClose();
    }

    private static String normalizePart(String raw, boolean allowBlank) {
        if (raw == null) return "";
        String normalized = raw.trim().toLowerCase(Locale.ROOT)
                .replace(' ', '_')
                .replace('-', '_')
                .replace('/', '_');
        StringBuilder out = new StringBuilder(normalized.length());
        for (int i = 0; i < normalized.length(); i++) {
            char c = normalized.charAt(i);
            if ((c >= 'a' && c <= 'z') || (c >= '0' && c <= '9') || c == '_' || c == ':') {
                out.append(c);
            }
        }
        return out.isEmpty() && !allowBlank ? "custom" : out.toString();
    }

    private void renderSuggestions(GuiGraphics g, int mouseX, int mouseY) {
        EditBox focused = focusedBox();
        if (focused == null) return;

        List<Suggestion> suggestions = suggestionsFor(focused);
        if (suggestions.isEmpty()) return;

        int x = focused.getX();
        int y = focused.getY() + focused.getHeight() + 2;
        int width = focused.getWidth();
        int rows = Math.min(MAX_SUGGESTIONS, suggestions.size());
        g.fill(x, y, x + width, y + rows * ROW_HEIGHT + 2, AMITheme.DROPDOWN_LIST_BG);
        g.fill(x, y, x + width, y + 1, AMITheme.SECTION_SEP);

        for (int i = 0; i < rows; i++) {
            int rowY = y + 1 + i * ROW_HEIGHT;
            boolean hovered = mouseX >= x && mouseX < x + width && mouseY >= rowY && mouseY < rowY + ROW_HEIGHT;
            if (hovered) {
                g.fill(x + 1, rowY, x + width - 1, rowY + ROW_HEIGHT, AMITheme.DROPDOWN_BG_ACTIVE);
            }
            Suggestion suggestion = suggestions.get(i);
            String label = font.plainSubstrByWidth(suggestion.label(), width - 8);
            g.drawString(font, label, x + 4, rowY + 3, AMITheme.TEXT_HEADER, false);
        }
    }

    private boolean acceptSuggestionAt(int mouseX, int mouseY) {
        EditBox focused = focusedBox();
        if (focused == null) return false;
        List<Suggestion> suggestions = suggestionsFor(focused);
        if (suggestions.isEmpty()) return false;

        int x = focused.getX();
        int y = focused.getY() + focused.getHeight() + 3;
        int rows = Math.min(MAX_SUGGESTIONS, suggestions.size());
        if (mouseX < x || mouseX >= x + focused.getWidth() || mouseY < y || mouseY >= y + rows * ROW_HEIGHT) {
            return false;
        }
        int row = (mouseY - y) / ROW_HEIGHT;
        applySuggestion(focused, suggestions.get(row));
        return true;
    }

    private boolean acceptFirstSuggestion() {
        EditBox focused = focusedBox();
        if (focused == null) return false;
        List<Suggestion> suggestions = suggestionsFor(focused);
        if (suggestions.isEmpty()) return false;
        applySuggestion(focused, suggestions.get(0));
        return true;
    }

    private void applySuggestion(EditBox focused, Suggestion suggestion) {
        focused.setValue(suggestion.id());
        focused.setCursorPosition(suggestion.id().length());
    }

    private EditBox focusedBox() {
        if (categoryBox != null && categoryBox.isFocused()) return categoryBox;
        if (subcategoryBox != null && subcategoryBox.isFocused()) return subcategoryBox;
        return null;
    }

    private List<Suggestion> suggestionsFor(EditBox focused) {
        String filter = normalizePart(focused.getValue(), true);
        if (focused == categoryBox) {
            return categorySuggestions(filter);
        }
        return subcategorySuggestions(filter);
    }

    private List<Suggestion> categorySuggestions(String filter) {
        List<Suggestion> suggestions = new ArrayList<>();
        for (AmiOntology.Category category : AmiOntology.CATEGORIES) {
            if (matches(category.id, category.shortName, filter)) {
                suggestions.add(new Suggestion(category.id, category.shortName + " (" + category.id + ")"));
            }
        }
        addCurrentCustomSuggestion(suggestions, categoryBox.getValue(), filter);
        return suggestions;
    }

    private List<Suggestion> subcategorySuggestions(String filter) {
        AmiOntology.Category category = AmiOntology.categoryForId(normalizePart(categoryBox.getValue(), false));
        List<Suggestion> suggestions = new ArrayList<>();
        for (AmiOntology.SubCategory subcategory : category.subCategories) {
            String label = subcategory.displayName().getString();
            if (matches(subcategory.id(), label, filter)) {
                suggestions.add(new Suggestion(subcategory.id(), label + " (" + subcategory.id() + ")"));
            }
        }
        addCurrentCustomSuggestion(suggestions, subcategoryBox.getValue(), filter);
        return suggestions;
    }

    private void addCurrentCustomSuggestion(List<Suggestion> suggestions, String value, String filter) {
        String normalized = normalizePart(value, true);
        if (normalized.isBlank()) return;
        boolean alreadyKnown = suggestions.stream().anyMatch(suggestion -> suggestion.id().equals(normalized));
        if (!alreadyKnown && normalized.contains(filter)) {
            suggestions.add(new Suggestion(normalized, normalized));
        }
    }

    private static boolean matches(String id, String label, String filter) {
        if (filter == null || filter.isBlank()) return true;
        return id.toLowerCase(Locale.ROOT).contains(filter)
                || label.toLowerCase(Locale.ROOT).contains(filter);
    }

    @Override
    public void onClose() {
        minecraft.setScreen(parent);
    }

    private record Suggestion(String id, String label) {
    }
}
