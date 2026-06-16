package com.sanhiruzu.ami.client.screen;

import com.sanhiruzu.ami.client.input.TextInputFilter;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractSliderButton;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;

import java.lang.reflect.Field;
import java.util.function.Consumer;

/**
 * Interactive color picker for config fields.
 */
public class AmiColorPickerScreen extends Screen {
    private final Screen parent;
    private final Field field;
    private final Consumer<Integer> onApply;

    private int currentColor;
    private EditBox hexInput;
    private Slider rSlider, gSlider, bSlider, aSlider;

    public AmiColorPickerScreen(Screen parent, Field field, int initialColor, Consumer<Integer> onApply) {
        super(Component.literal("Color Picker"));
        this.parent = parent;
        this.field = field;
        this.currentColor = initialColor;
        this.onApply = onApply;
    }

    @Override
    protected void init() {
        super.init();
        int centerX = width / 2;
        int centerY = height / 2;

        int a = (currentColor >> 24) & 0xFF;
        int r = (currentColor >> 16) & 0xFF;
        int g = (currentColor >> 8) & 0xFF;
        int b = currentColor & 0xFF;

        rSlider = addRenderableWidget(new Slider(centerX - 100, centerY - 60, 200, 20, Component.literal("Red"), r / 255.0, v -> updateFromSliders()));
        gSlider = addRenderableWidget(new Slider(centerX - 100, centerY - 35, 200, 20, Component.literal("Green"), g / 255.0, v -> updateFromSliders()));
        bSlider = addRenderableWidget(new Slider(centerX - 100, centerY - 10, 200, 20, Component.literal("Blue"), b / 255.0, v -> updateFromSliders()));
        aSlider = addRenderableWidget(new Slider(centerX - 100, centerY + 15, 200, 20, Component.literal("Alpha"), a / 255.0, v -> updateFromSliders()));

        hexInput = new EditBox(font, centerX - 40, centerY + 45, 80, 20, Component.literal("Hex"));
        hexInput.setFilter(TextInputFilter::isAllowedInput);
        hexInput.setValue(String.format("%08X", currentColor));
        hexInput.setResponder(this::updateFromHex);
        addRenderableWidget(hexInput);

        addRenderableWidget(Button.builder(Component.translatable("gui.done"), b1 -> apply())
                .bounds(centerX - 105, centerY + 75, 100, 20).build());
        addRenderableWidget(Button.builder(Component.translatable("gui.cancel"), b1 -> onClose())
                .bounds(centerX + 5, centerY + 75, 100, 20).build());
    }

    private void updateFromSliders() {
        int a = (int) (aSlider.getValue() * 255);
        int r = (int) (rSlider.getValue() * 255);
        int g = (int) (gSlider.getValue() * 255);
        int b = (int) (bSlider.getValue() * 255);
        currentColor = (a << 24) | (r << 16) | (g << 8) | b;
        hexInput.setResponder(s -> {
        });
        hexInput.setValue(String.format("%08X", currentColor));
        hexInput.setResponder(this::updateFromHex);
    }

    private void updateFromHex(String hex) {
        try {
            currentColor = (int) Long.parseLong(hex, 16);
            int a = (currentColor >> 24) & 0xFF;
            int r = (currentColor >> 16) & 0xFF;
            int g = (currentColor >> 8) & 0xFF;
            int b = currentColor & 0xFF;
            rSlider.setValue(r / 255.0);
            gSlider.setValue(g / 255.0);
            bSlider.setValue(b / 255.0);
            aSlider.setValue(a / 255.0);
        } catch (Exception ignored) {
        }
    }

    private void apply() {
        try {
            field.set(null, currentColor);
            onApply.accept(currentColor);
        } catch (Exception ignored) {
        }
        onClose();
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor g, int mouseX, int mouseY, float partialTick) {
        super.extractRenderState(g, mouseX, mouseY, partialTick);

        int centerX = width / 2;
        int centerY = height / 2;

        // Color preview box
        g.fill(centerX + 110, centerY - 60, centerX + 150, centerY + 35, 0xFFFFFFFF);
        g.fill(centerX + 111, centerY - 59, centerX + 149, centerY + 34, currentColor);
        g.outline(centerX + 110, centerY - 60, centerX + 150, centerY + 35, 0xFF000000);

        String fieldName = field.getName();
        g.text(font, fieldName, centerX - font.width(fieldName) / 2, centerY - 80, 0xFFFFAA00);
    }

    @Override
    public void onClose() {
        minecraft.setScreen(parent);
    }

    private static class Slider extends AbstractSliderButton {
        private final Consumer<Double> onChange;
        private final Component prefix;

        public Slider(int x, int y, int width, int height, Component prefix, double initialValue, Consumer<Double> onChange) {
            super(x, y, width, height, prefix, initialValue);
            this.prefix = prefix;
            this.onChange = onChange;
            updateMessage();
        }

        public double getValue() {
            return value;
        }

        public void setValue(double v) {
            this.value = Mth.clamp(v, 0.0, 1.0);
            updateMessage();
        }

        @Override
        protected void updateMessage() {
            setMessage(prefix.copy().append(": ").append(String.valueOf((int) (value * 255))));
        }

        @Override
        protected void applyValue() {
            onChange.accept(value);
        }
    }
}
