package com.sanhiruzu.ami.client.widget;

import com.sanhiruzu.ami.config.ConfigSlider;
import net.minecraft.client.gui.components.AbstractSliderButton;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;

import java.lang.reflect.Field;
import java.util.function.Consumer;

public class AmiSliderWidget extends AbstractSliderButton {
    private final Field field;
    private final ConfigSlider slider;
    private final Consumer<Object> onChange;

    public AmiSliderWidget(Field field, Consumer<Object> onChange) {
        this(field, field.getAnnotation(ConfigSlider.class), onChange);
    }

    private AmiSliderWidget(Field field, ConfigSlider slider, Consumer<Object> onChange) {
        super(0, 0, 110, 18, Component.empty(), normalized(readInt(field), slider));
        this.field = field;
        this.slider = slider;
        this.onChange = onChange;
        updateMessage();
    }

    @Override
    protected void updateMessage() {
        setMessage(Component.literal(valueFromSlider() + "%"));
    }

    @Override
    protected void applyValue() {
        int next = valueFromSlider();
        try {
            field.setInt(null, next);
            onChange.accept(next);
        } catch (Exception ignored) {
        }
        updateMessage();
    }

    private int valueFromSlider() {
        int min = slider.min();
        int max = Math.max(min, slider.max());
        if (max == min) return min;

        int step = Math.max(1, slider.step());
        int raw = min + (int) Math.round(value * (max - min));
        int snapped = min + Math.round((raw - min) / (float) step) * step;
        return Mth.clamp(snapped, min, max);
    }

    private static double normalized(int value, ConfigSlider slider) {
        if (slider == null || slider.max() <= slider.min()) return 0.0;
        return Mth.clamp((value - slider.min()) / (double) (slider.max() - slider.min()), 0.0, 1.0);
    }

    private static int readInt(Field field) {
        try {
            return field.getInt(null);
        } catch (Exception ignored) {
            return 0;
        }
    }
}
