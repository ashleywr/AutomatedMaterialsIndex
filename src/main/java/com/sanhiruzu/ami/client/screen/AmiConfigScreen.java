package com.sanhiruzu.ami.client.screen;

import com.sanhiruzu.ami.client.AMITheme;
import com.sanhiruzu.ami.config.AmiConfig;
import com.sanhiruzu.ami.config.ConfigValue;
import com.sanhiruzu.ami.config.ConfigGroup;
import com.sanhiruzu.ami.client.widget.AmiWidgetFactory;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.ObjectSelectionList;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Tooltip;
import net.neoforged.fml.ModList;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * Custom configuration screen for AMI with EMI-inspired layout and dynamic feedback.
 */
public class AmiConfigScreen extends Screen {
    private final Screen parent;
    private EditBox searchBox;
    private ConfigList list;
    private final Map<Field, Object> originalValues = new HashMap<>();
    
    private Button revertBtn;
    private Button defaultsBtn;

    public AmiConfigScreen(Screen parent) {
        super(Component.translatable("ami.config.title"));
        this.parent = parent;
        captureOriginalValues();
    }

    private void captureOriginalValues() {
        for (Field field : AmiConfig.class.getFields()) {
            try {
                if (java.lang.reflect.Modifier.isStatic(field.getModifiers())) {
                    originalValues.put(field, field.get(null));
                }
            } catch (Exception e) {}
        }
    }

    @Override
    protected void init() {
        super.init();
        
        // Search Box at the top
        searchBox = new EditBox(this.font, width / 2 - 50, 10, 150, 20, Component.translatable("ami.config.search_placeholder"));
        searchBox.setResponder(this::onSearchChanged);
        this.addRenderableWidget(searchBox);

        // EMI config shortcut — use reflection to avoid a hard class reference when EMI is absent
        if (ModList.get().isLoaded("emi")) {
            this.addRenderableWidget(Button.builder(Component.translatable("ami.config.emi_config"), b -> {
                try {
                    Class<?> cls = Class.forName("dev.emi.emi.screen.ConfigScreen");
                    this.minecraft.setScreen((Screen) cls.getConstructor(Screen.class).newInstance(this));
                } catch (Exception ignored) {}
            }).bounds(width - 110, 10, 100, 20).build());
        }

        // Scrolling List - 66% width
        list = new ConfigList(this.minecraft, width, height - 70, 40, 25);
        this.addRenderableWidget(list);

        buildConfigUI();

        // Bottom Buttons
        int btnW = 100;
        int spacing = 10;
        int totalW = (btnW * 3) + (spacing * 2);
        int startX = (width - totalW) / 2;
        int btnY = height - 25;

        defaultsBtn = this.addRenderableWidget(Button.builder(Component.translatable("ami.config.defaults"), b -> resetToDefaults())
                .bounds(startX, btnY, btnW, 20).build());
        
        revertBtn = this.addRenderableWidget(Button.builder(getRevertLabel(), b -> revertChanges())
                .bounds(startX + btnW + spacing, btnY, btnW, 20).build());

        this.addRenderableWidget(Button.builder(Component.translatable("ami.config.done"), b -> this.onClose())
                .bounds(startX + (btnW + spacing) * 2, btnY, btnW, 20).build());
        
        updateButtonStates();
    }

    private Component getRevertLabel() {
        int changes = countChanges();
        return changes > 0 ? Component.translatable("ami.config.revert_count", String.valueOf(changes)) : Component.translatable("ami.config.revert");
    }

    private int countChanges() {
        int count = 0;
        for (var entry : originalValues.entrySet()) {
            try {
                if (!Objects.equals(entry.getKey().get(null), entry.getValue())) {
                    count++;
                }
            } catch (Exception e) {}
        }
        return count;
    }

    private void updateButtonStates() {
        if (revertBtn != null) {
            revertBtn.active = countChanges() > 0;
            revertBtn.setMessage(getRevertLabel());
        }
        if (defaultsBtn != null) {
            // Check if current state differs from the system defaults
            // We use a temporary reset to compare or just check if it's dirty
            defaultsBtn.active = true; // For now, keep it active as a "factory reset"
        }
    }

    private void onSearchChanged(String query) {
        buildConfigUI();
    }

    private void resetToDefaults() {
        AmiConfig.resetToDefaults();
        buildConfigUI();
        updateButtonStates();
    }

    private void revertChanges() {
        for (var entry : originalValues.entrySet()) {
            try {
                entry.getKey().set(null, entry.getValue());
            } catch (Exception e) {}
        }
        buildConfigUI();
        updateButtonStates();
    }

    private void buildConfigUI() {
        list.clearEntries();
        String query = searchBox.getValue().toLowerCase();

        for (Field field : AmiConfig.class.getFields()) {
            ConfigGroup group = field.getAnnotation(ConfigGroup.class);
            if (group != null) {
                String groupKey = "ami.config.group." + group.value();
                Component groupText = Component.translatable(groupKey);
                if (query.isEmpty() || groupText.getString().toLowerCase().contains(query)) {
                    list.addEntry(list.new HeaderEntry(groupText));
                }
                if ("sidepanels".equals(group.value())) {
                    if (query.isEmpty() || sidePanelEditorMatches(query)) {
                        addSidePanelEditorEntries();
                    }
                }
            }

            ConfigValue value = field.getAnnotation(ConfigValue.class);
            if (value != null && !isSidePanelField(field)) {
                String valueKey = "ami.config.value." + value.value();
                Component valueText = Component.translatable(valueKey);
                if (query.isEmpty() || valueText.getString().toLowerCase().contains(query)) {
                    list.addEntry(list.new SettingEntry(valueText, field));
                }
            }
        }

        // Add keybinds section
        Component bindsHeader = Component.translatable("ami.config.group.binds");
        if (query.isEmpty() || bindsHeader.getString().toLowerCase().contains(query)) {
            list.addEntry(list.new HeaderEntry(bindsHeader));
        }

        try {
            for (Field field : com.sanhiruzu.ami.client.AMIKeyMappings.class.getFields()) {
                if (java.lang.reflect.Modifier.isStatic(field.getModifiers()) && net.minecraft.client.KeyMapping.class.isAssignableFrom(field.getType())) {
                    net.minecraft.client.KeyMapping keyMapping = (net.minecraft.client.KeyMapping) field.get(null);
                    String keybindName = keyMapping.getName(); // e.g., "key.ami.favorite"
                    Component keyLabel = Component.translatable(keybindName);
                    if (query.isEmpty() || keyLabel.getString().toLowerCase().contains(query)) {
                        list.addEntry(list.new KeybindEntry(keyLabel, keyMapping));
                    }
                }
            }
        } catch (Exception e) {
            com.sanhiruzu.ami.AMI.LOGGER.error("Failed to load keybinds in config screen", e);
        }
    }

    private void addSidePanelEditorEntries() {
        list.addEntry(list.new PanelTitleEntry(Component.translatable("ami.config.panel.left")));
        list.addEntry(list.new SidePanelWidthEntry(Component.translatable("ami.config.panel.width"), field("leftPanelWidth")));
        list.addEntry(list.new PanelSubheaderEntry(Component.translatable("ami.config.panel.normal_view")));
        addSlotListEntries(field("leftPanelSlots"));
        list.addEntry(list.new PanelSubheaderEntry(Component.translatable("ami.config.panel.toggled_view")));
        addSlotListEntries(field("leftPanelAlternateSlots"));

        list.addEntry(list.new PanelTitleEntry(Component.translatable("ami.config.panel.right")));
        list.addEntry(list.new SidePanelWidthEntry(Component.translatable("ami.config.panel.width"), field("rightPanelWidth")));
        list.addEntry(list.new PanelSubheaderEntry(Component.translatable("ami.config.panel.normal_view")));
        addSlotListEntries(field("rightPanelSlots"));
        list.addEntry(list.new PanelSubheaderEntry(Component.translatable("ami.config.panel.toggled_view")));
        addSlotListEntries(field("rightPanelAlternateSlots"));
    }

    private void addSlotListEntries(Field slotsField) {
        List<AmiConfig.PanelContent> slots = readSlots(slotsField);
        if (slots.isEmpty()) {
            list.addEntry(list.new PanelEmptySlotsEntry(Component.translatable("ami.config.panel.no_slots")));
        }
        for (int i = 0; i < slots.size(); i++) {
            list.addEntry(list.new PanelSlotEntry(Component.translatable("ami.config.panel.slot", i + 1), slotsField, i));
        }
        list.addEntry(list.new PanelAddSlotEntry(Component.translatable("ami.config.panel.add_slot"), slotsField));
    }

    private static List<AmiConfig.PanelContent> readSlots(Field field) {
        try {
            return new ArrayList<>(AmiConfig.parsePanelSlots((String) field.get(null)));
        } catch (Exception e) {
            return new ArrayList<>();
        }
    }

    private static void writeSlots(Field field, List<AmiConfig.PanelContent> slots) {
        try {
            field.set(null, AmiConfig.encodePanelSlots(slots));
        } catch (Exception ignored) {
        }
    }

    private Field field(String name) {
        try {
            return AmiConfig.class.getField(name);
        } catch (NoSuchFieldException e) {
            throw new IllegalStateException("Missing AmiConfig field " + name, e);
        }
    }

    private boolean isSidePanelField(Field field) {
        ConfigValue value = field.getAnnotation(ConfigValue.class);
        return value != null && value.value().startsWith("sidepanels.");
    }

    private boolean sidePanelEditorMatches(String query) {
        if ("side panels left right normal toggled toggle slot favorites craftables results grid list compact empty quests history width".contains(query)) {
            return true;
        }
        for (AmiConfig.PanelContent content : AmiConfig.PanelContent.values()) {
            if (panelContentLabel(content).toLowerCase(Locale.ROOT).contains(query)) {
                return true;
            }
        }
        return false;
    }

    private static String panelContentLabel(AmiConfig.PanelContent content) {
        String key = switch (content) {
            case GRID -> "ami.config.panel.results_grid";
            case LIST -> "ami.config.panel.results_list";
            case COMPACT -> "ami.config.panel.results_compact";
            case FAVORITES -> "ami.gui.favorites";
            case LOOKUP_HISTORY -> "ami.gui.sidebar.lookup_history";
            case CRAFTING_HISTORY -> "ami.gui.sidebar.crafting_history";
            case CRAFTABLE -> "ami.gui.sidebar.craftable";
            case QUESTS -> "ami.gui.sidebar.quests";
            case EMPTY -> "ami.gui.sidebar.empty";
            default -> null;
        };
        if (key != null) return Component.translatable(key).getString();
        String raw = content.name().toLowerCase(Locale.ROOT).replace('_', ' ');
        StringBuilder out = new StringBuilder(raw.length());
        boolean cap = true;
        for (int i = 0; i < raw.length(); i++) {
            char c = raw.charAt(i);
            out.append(cap ? Character.toUpperCase(c) : c);
            cap = c == ' ';
        }
        return out.toString();
    }

    private static AmiConfig.PanelContent[] selectablePanelContents() {
        return new AmiConfig.PanelContent[]{
                AmiConfig.PanelContent.EMPTY,
                AmiConfig.PanelContent.FAVORITES,
                AmiConfig.PanelContent.GRID,
                AmiConfig.PanelContent.LIST,
                AmiConfig.PanelContent.COMPACT,
                AmiConfig.PanelContent.LOOKUP_HISTORY,
                AmiConfig.PanelContent.CRAFTING_HISTORY,
                AmiConfig.PanelContent.CRAFTABLE,
                AmiConfig.PanelContent.QUESTS
        };
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        super.render(g, mouseX, mouseY, partialTick);
        g.drawString(this.font, Component.translatable("ami.config.brand_top"), 10, 8, AMITheme.CONFIG_BRAND_GOLD);
        g.drawString(this.font, Component.translatable("ami.config.brand_bottom"), 10, 18, AMITheme.CONFIG_TEXT_PRIMARY);
    }

    @Override
    public void onClose() {
        this.minecraft.setScreen(parent);
    }

    class ConfigList extends ObjectSelectionList<ConfigList.ConfigEntry> {
        public ConfigList(net.minecraft.client.Minecraft mc, int width, int height, int top, int itemHeight) {
            super(mc, width, height, top, itemHeight);
        }

        @Override
        public int getRowWidth() {
            return (int)(this.width * 0.66);
        }

        @Override
        protected int getScrollbarPosition() {
            return (this.width / 2) + (getRowWidth() / 2) + 5;
        }

        public void clearEntries() {
            super.clearEntries();
        }

        public int addEntry(ConfigEntry entry) {
            return super.addEntry(entry);
        }

        abstract class ConfigEntry extends ObjectSelectionList.Entry<ConfigEntry> {}

        class HeaderEntry extends ConfigEntry {
            private final Component text;

            HeaderEntry(Component text) {
                this.text = text;
            }

            @Override
            public void render(GuiGraphics g, int index, int y, int x, int width, int height, int mouseX, int mouseY, boolean isSelected, float partialTick) {
                g.drawCenteredString(AmiConfigScreen.this.font, text, x + width / 2, y + 5, AMITheme.CONFIG_HEADER_GOLD);
            }

            @Override
            public Component getNarration() { return text; }
        }

        class PanelTitleEntry extends ConfigEntry {
            private final Component text;

            PanelTitleEntry(Component text) {
                this.text = text;
            }

            @Override
            public void render(GuiGraphics g, int index, int y, int x, int width, int height, int mouseX, int mouseY, boolean isSelected, float partialTick) {
                g.drawString(AmiConfigScreen.this.font, text, x + 5, y + 6, AMITheme.CONFIG_PANEL_TITLE, false);
            }

            @Override
            public Component getNarration() { return text; }
        }

        class PanelSubheaderEntry extends ConfigEntry {
            private final Component text;

            PanelSubheaderEntry(Component text) {
                this.text = text;
            }

            @Override
            public void render(GuiGraphics g, int index, int y, int x, int width, int height, int mouseX, int mouseY, boolean isSelected, float partialTick) {
                g.drawString(AmiConfigScreen.this.font, text, x + 14, y + 6, AMITheme.CONFIG_TEXT_SECONDARY, false);
            }

            @Override
            public Component getNarration() { return text; }
        }

        class PanelEmptySlotsEntry extends ConfigEntry {
            private final Component label;

            PanelEmptySlotsEntry(Component label) {
                this.label = label;
            }

            @Override
            public void render(GuiGraphics g, int index, int y, int x, int width, int height, int mouseX, int mouseY, boolean isSelected, float partialTick) {
                g.drawString(AmiConfigScreen.this.font, label, x + 28, y + 6, AMITheme.CONFIG_TEXT_MUTED, false);
            }

            @Override
            public Component getNarration() { return label; }
        }

        class PanelSlotEntry extends ConfigEntry {
            private final Component label;
            private final Field field;
            private final int slotIndex;
            private final Button button;
            private final Button removeButton;

            PanelSlotEntry(Component label, Field field, int slotIndex) {
                this.label = label;
                this.field = field;
                this.slotIndex = slotIndex;
                this.button = Button.builder(currentMessage(), b -> cycle()).bounds(0, 0, 130, 18).build();
                this.removeButton = Button.builder(Component.translatable("ami.config.panel.remove_slot"), b -> remove()).bounds(0, 0, 20, 18).build();
            }

            private Component currentMessage() {
                List<AmiConfig.PanelContent> slots = readSlots(field);
                if (slotIndex < 0 || slotIndex >= slots.size()) return Component.literal("?");
                return Component.literal(panelContentLabel(slots.get(slotIndex)));
            }

            private void cycle() {
                List<AmiConfig.PanelContent> slots = readSlots(field);
                if (slotIndex < 0 || slotIndex >= slots.size()) return;
                AmiConfig.PanelContent current = slots.get(slotIndex);
                AmiConfig.PanelContent[] values = selectablePanelContents();
                int next = 0;
                for (int i = 0; i < values.length; i++) {
                    if (values[i] == current) {
                        next = (i + 1) % values.length;
                        break;
                    }
                }
                slots.set(slotIndex, values[next]);
                writeSlots(field, slots);
                button.setMessage(currentMessage());
                AmiConfigScreen.this.buildConfigUI();
                AmiConfigScreen.this.updateButtonStates();
            }

            private void remove() {
                List<AmiConfig.PanelContent> slots = readSlots(field);
                if (slotIndex >= 0 && slotIndex < slots.size()) {
                    slots.remove(slotIndex);
                    writeSlots(field, slots);
                    AmiConfigScreen.this.buildConfigUI();
                    AmiConfigScreen.this.updateButtonStates();
                }
            }

            @Override
            public void render(GuiGraphics g, int index, int y, int x, int width, int height, int mouseX, int mouseY, boolean isSelected, float partialTick) {
                g.drawString(AmiConfigScreen.this.font, label, x + 28, y + 6, AMITheme.CONFIG_TEXT_PRIMARY, false);
                removeButton.setX(x + width - 22);
                removeButton.setY(y + 3);
                removeButton.setWidth(20);
                removeButton.setHeight(18);
                removeButton.render(g, mouseX, mouseY, partialTick);

                button.setX(x + width - 157);
                button.setY(y + 3);
                button.setWidth(130);
                button.setHeight(18);
                button.setMessage(currentMessage());
                button.render(g, mouseX, mouseY, partialTick);
            }

            @Override
            public boolean mouseClicked(double mouseX, double mouseY, int button) {
                if (this.removeButton.mouseClicked(mouseX, mouseY, button)) return true;
                if (this.button.mouseClicked(mouseX, mouseY, button)) return true;
                return super.mouseClicked(mouseX, mouseY, button);
            }

            @Override
            public Component getNarration() { return label.copy().append(": ").append(currentMessage()); }
        }

        class PanelAddSlotEntry extends ConfigEntry {
            private final Component label;
            private final Field field;
            private final Button button;

            PanelAddSlotEntry(Component label, Field field) {
                this.label = label;
                this.field = field;
                this.button = Button.builder(label, b -> addSlot()).bounds(0, 0, 130, 18).build();
            }

            private void addSlot() {
                List<AmiConfig.PanelContent> slots = readSlots(field);
                slots.add(AmiConfig.PanelContent.EMPTY);
                writeSlots(field, slots);
                AmiConfigScreen.this.buildConfigUI();
                AmiConfigScreen.this.updateButtonStates();
            }

            @Override
            public void render(GuiGraphics g, int index, int y, int x, int width, int height, int mouseX, int mouseY, boolean isSelected, float partialTick) {
                button.setX(x + 28);
                button.setY(y + 3);
                button.setWidth(130);
                button.setHeight(18);
                button.render(g, mouseX, mouseY, partialTick);
            }

            @Override
            public boolean mouseClicked(double mouseX, double mouseY, int button) {
                if (this.button.mouseClicked(mouseX, mouseY, button)) return true;
                return super.mouseClicked(mouseX, mouseY, button);
            }

            @Override
            public Component getNarration() { return label; }
        }

        class SidePanelWidthEntry extends ConfigEntry {
            private final Component label;
            private final Field field;
            private final AbstractWidget widget;

            SidePanelWidthEntry(Component label, Field field) {
                this.label = label;
                this.field = field;
                this.widget = AmiWidgetFactory.createWidget(field, o -> AmiConfigScreen.this.updateButtonStates());
            }

            @Override
            public void render(GuiGraphics g, int index, int y, int x, int width, int height, int mouseX, int mouseY, boolean isSelected, float partialTick) {
                g.drawString(AmiConfigScreen.this.font, label, x + 14, y + 6, AMITheme.CONFIG_TEXT_PRIMARY, false);
                if (widget != null) {
                    widget.setX(x + width - 77);
                    widget.setY(y + 3);
                    widget.render(g, mouseX, mouseY, partialTick);
                }
            }

            @Override
            public boolean mouseClicked(double mouseX, double mouseY, int button) {
                if (widget != null && widget.mouseClicked(mouseX, mouseY, button)) return true;
                return super.mouseClicked(mouseX, mouseY, button);
            }

            @Override
            public boolean charTyped(char codePoint, int modifiers) {
                if (widget != null && widget.charTyped(codePoint, modifiers)) return true;
                return super.charTyped(codePoint, modifiers);
            }

            @Override
            public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
                if (widget != null && widget.keyPressed(keyCode, scanCode, modifiers)) return true;
                return super.keyPressed(keyCode, scanCode, modifiers);
            }

            @Override
            public Component getNarration() { return label; }
        }

        class SettingEntry extends ConfigEntry {
            private final Component label;
            private final Field field;
            private final Tooltip tooltip;
            private final AbstractWidget widget;

            SettingEntry(Component label, Field field) {
                this.label = label;
                this.field = field;
                String tooltipKey = "ami.config.tooltip." + field.getAnnotation(ConfigValue.class).value();
                this.tooltip = Tooltip.create(Component.translatable(tooltipKey));
                this.widget = AmiWidgetFactory.createWidget(field, o -> AmiConfigScreen.this.updateButtonStates());
            }

            @Override
            public void render(GuiGraphics g, int index, int y, int x, int width, int height, int mouseX, int mouseY, boolean isSelected, float partialTick) {
                g.drawString(AmiConfigScreen.this.font, label, x + 5, y + 5, AMITheme.CONFIG_TEXT_PRIMARY);
                if (widget != null) {
                    widget.setX(x + width - (field.isAnnotationPresent(com.sanhiruzu.ami.config.ConfigColor.class) ? 102 : 77));
                    widget.setY(y + 2);
                    
                    if (field.getType() == boolean.class && widget instanceof Button) {
                        try {
                            boolean val = field.getBoolean(null);
                            g.fill(widget.getX() - 2, widget.getY() - 1, widget.getX() + widget.getWidth() + 2, widget.getY() + widget.getHeight() + 1, val ? AMITheme.CONFIG_BOOL_TRUE : AMITheme.CONFIG_BOOL_FALSE);
                        } catch (Exception ignored) {}
                    }

                    widget.render(g, mouseX, mouseY, partialTick);

                    if (field.isAnnotationPresent(com.sanhiruzu.ami.config.ConfigColor.class)) {
                        try {
                            int color = field.getInt(null);
                            g.fill(x + width - 25, y + 2, x + width - 5, y + 18, 0xFF000000 | color);
                            g.renderOutline(x + width - 26, y + 1, 22, 18, AMITheme.CONFIG_SWATCH_BORDER);

                            String hintKey = "ami.config.hint." + field.getAnnotation(ConfigValue.class).value();
                            Component hint = Component.translatable(hintKey);
                            int hintW = font.width(hint);
                            g.drawString(font, hint, x + width - 28 - hintW, y + 5, AMITheme.CONFIG_TEXT_MUTED, false);
                        } catch (Exception ignored) {}
                    }
                }
                if (mouseX >= x && mouseX <= x + width && mouseY >= y && mouseY <= y + height) {
                    if (widget != null && widget.isMouseOver(mouseX, mouseY)) {
                        widget.setTooltip(this.tooltip);
                    } else {
                        AmiConfigScreen.this.setTooltipForNextRenderPass(this.tooltip, net.minecraft.client.gui.screens.inventory.tooltip.DefaultTooltipPositioner.INSTANCE, true);
                    }
                }
            }

            @Override
            public boolean mouseClicked(double mouseX, double mouseY, int button) {
                if (widget != null && widget.mouseClicked(mouseX, mouseY, button)) return true;
                return super.mouseClicked(mouseX, mouseY, button);
            }

            @Override
            public boolean charTyped(char codePoint, int modifiers) {
                if (widget != null && widget.charTyped(codePoint, modifiers)) return true;
                return super.charTyped(codePoint, modifiers);
            }

            @Override
            public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
                if (widget != null && widget.keyPressed(keyCode, scanCode, modifiers)) return true;
                return super.keyPressed(keyCode, scanCode, modifiers);
            }

            @Override
            public Component getNarration() { return label; }
        }

        class KeybindEntry extends ConfigEntry {
            private final Component label;
            private final net.minecraft.client.KeyMapping keyMapping;
            private final Tooltip tooltip;
            private final Button button;

            KeybindEntry(Component label, net.minecraft.client.KeyMapping keyMapping) {
                this.label = label;
                this.keyMapping = keyMapping;
                String tooltipKey = "ami.config.tooltip." + keyMapping.getName().replace("key.ami.", "");
                this.tooltip = Tooltip.create(Component.translatable(tooltipKey));
                this.button = Button.builder(keyMapping.getTranslatedKeyMessage(), b -> {
                    // Open keybind editor
                    AmiConfigScreen.this.minecraft.setScreen(new net.minecraft.client.gui.screens.options.controls.KeyBindsScreen(AmiConfigScreen.this, AmiConfigScreen.this.minecraft.options));
                }).build();
            }

            @Override
            public void render(GuiGraphics g, int index, int y, int x, int width, int height, int mouseX, int mouseY, boolean isSelected, float partialTick) {
                g.drawString(AmiConfigScreen.this.font, label, x + 5, y + 5, AMITheme.CONFIG_TEXT_PRIMARY);

                button.setX(x + width - 77);
                button.setY(y + 2);
                button.setWidth(72);
                button.setHeight(18);
                button.setMessage(keyMapping.getTranslatedKeyMessage());
                button.render(g, mouseX, mouseY, 0);

                if (mouseX >= x && mouseX <= x + width && mouseY >= y && mouseY <= y + height) {
                    if (button.isMouseOver(mouseX, mouseY)) {
                        button.setTooltip(this.tooltip);
                    } else {
                        AmiConfigScreen.this.setTooltipForNextRenderPass(this.tooltip, net.minecraft.client.gui.screens.inventory.tooltip.DefaultTooltipPositioner.INSTANCE, true);
                    }
                }
            }

            @Override
            public boolean mouseClicked(double mouseX, double mouseY, int button) {
                if (this.button.mouseClicked(mouseX, mouseY, button)) return true;
                return super.mouseClicked(mouseX, mouseY, button);
            }

            @Override
            public Component getNarration() { return label; }
        }
    }
}
