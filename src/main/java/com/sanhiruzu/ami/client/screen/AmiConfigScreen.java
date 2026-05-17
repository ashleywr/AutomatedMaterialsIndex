package com.sanhiruzu.ami.client.screen;

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

import java.lang.reflect.Field;
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
            }

            ConfigValue value = field.getAnnotation(ConfigValue.class);
            if (value != null) {
                String valueKey = "ami.config.value." + value.value();
                Component valueText = Component.translatable(valueKey);
                if (query.isEmpty() || valueText.getString().toLowerCase().contains(query)) {
                    list.addEntry(list.new SettingEntry(valueText, field));
                }
            }
        }
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        super.render(g, mouseX, mouseY, partialTick);
        g.drawString(this.font, "AUTOMATED", 10, 8, 0xFFFFAA00);
        g.drawString(this.font, "MATERIALS INDEX", 10, 18, 0xFFFFFFFF);
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
                g.drawCenteredString(AmiConfigScreen.this.font, text, x + width / 2, y + 5, 0xFFFFAA00);
            }

            @Override
            public Component getNarration() { return text; }
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
                g.drawString(AmiConfigScreen.this.font, label, x + 5, y + 5, 0xFFFFFFFF);
                if (widget != null) {
                    widget.setX(x + width - (field.isAnnotationPresent(com.sanhiruzu.ami.config.ConfigColor.class) ? 90 : 65));
                    widget.setY(y + 2);
                    
                    if (field.getType() == boolean.class && widget instanceof Button) {
                        try {
                            boolean val = field.getBoolean(null);
                            g.fill(widget.getX() - 2, widget.getY() - 1, widget.getX() + widget.getWidth() + 2, widget.getY() + widget.getHeight() + 1, val ? 0x8800FF00 : 0x88FF0000);
                        } catch (Exception ignored) {}
                    }

                    widget.render(g, mouseX, mouseY, partialTick);

                    if (field.isAnnotationPresent(com.sanhiruzu.ami.config.ConfigColor.class)) {
                        try {
                            int color = field.getInt(null);
                            g.fill(x + width - 25, y + 2, x + width - 5, y + 18, 0xFF000000 | color);
                            g.renderOutline(x + width - 26, y + 1, 22, 18, 0xFFFFFFFF);
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
    }
}
