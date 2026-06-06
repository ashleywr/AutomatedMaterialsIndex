package com.sanhiruzu.ami.client.widget;

import com.sanhiruzu.ami.config.ConfigColor;
import com.sanhiruzu.ami.config.ConfigSlider;
import com.sanhiruzu.ami.client.input.TextInputFilter;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.network.chat.Component;

import java.lang.reflect.Method;
import java.lang.reflect.Field;
import java.lang.reflect.Constructor;
import java.util.function.Predicate;
import java.util.function.Consumer;

/**
 * Factory for creating configuration widgets based on field type and annotations.
 */
public class AmiWidgetFactory {

    public static AbstractWidget createWidget(Field field, Consumer<Object> onChange) {
        return createWidget(field, onChange, dropdown -> {
        });
    }

    public static AbstractWidget createWidget(Field field, Consumer<Object> onChange, Consumer<AmiDropdownPopup> onDropdownOpen) {
        Class<?> type = field.getType();
        Minecraft mc = Minecraft.getInstance();

        try {
            if (field.isAnnotationPresent(ConfigColor.class)) {
                int color = field.getInt(null);
                EditBox eb = new EditBox(mc.font, 0, 0, 72, 18, Component.empty());
                configureEditBoxFilter(eb);
                eb.setValue(formatColor(field, color));
                eb.setResponder(s -> {
                    Integer parsed = parseColor(s);
                    if (parsed != null) {
                        try {
                            field.set(null, parsed);
                            onChange.accept(parsed);
                        } catch (Exception ignored) {
                        }
                    }
                });
                return eb;
            } else if (type == boolean.class) {
                boolean initial = field.getBoolean(null);
                return Button.builder(boolLabel(initial), b -> {
                    try {
                        boolean val = !field.getBoolean(null);
                        field.setBoolean(null, val);
                        b.setMessage(boolLabel(val));
                        onChange.accept(val);
                    } catch (Exception ignored) {
                    }
                }).bounds(0, 0, 72, 18).build();
            } else if (type == int.class && field.isAnnotationPresent(ConfigSlider.class)) {
                AbstractWidget slider = createSliderWidget(field, onChange);
                if (slider != null) return slider;
                return createIntEditBox(mc, field, onChange);
            } else if (type == int.class) {
                return createIntEditBox(mc, field, onChange);
            } else if (type.isEnum()) {
                return new AmiEnumDropdownWidget(field, onChange, onDropdownOpen);
            } else {
                EditBox eb = new EditBox(mc.font, 0, 0, 72, 18, Component.empty());
                configureEditBoxFilter(eb);
                Object val = field.get(null);
                eb.setValue(val != null ? val.toString() : "");
                eb.setResponder(s -> {
                    try {
                        field.set(null, s);
                        onChange.accept(s);
                    } catch (Exception ignored) {
                    }
                });
                return eb;
            }
        } catch (Exception e) {
            return null;
        }
    }

    private static AbstractWidget createSliderWidget(Field field, Consumer<Object> onChange) {
        try {
            Class<?> sliderClass = Class.forName("com.sanhiruzu.ami.client.widget.AmiSliderWidget");
            Constructor<?> constructor = sliderClass.getConstructor(Field.class, Consumer.class);
            return (AbstractWidget) constructor.newInstance(field, onChange);
        } catch (ReflectiveOperationException | LinkageError ignored) {
            return null;
        }
    }

    private static EditBox createIntEditBox(Minecraft mc, Field field, Consumer<Object> onChange) throws IllegalAccessException {
        EditBox eb = new EditBox(mc.font, 0, 0, 72, 18, Component.empty());
        configureEditBoxFilter(eb);
        eb.setValue(String.valueOf(field.getInt(null)));
        eb.setResponder(s -> {
            try {
                int val = Integer.parseInt(s);
                field.setInt(null, val);
                onChange.accept(val);
            } catch (Exception ignored) {
            }
        });
        return eb;
    }

    private static String formatColor(Field field, int color) {
        int alpha = (color >>> 24) & 0xFF;
        if (!isAlphaColorField(field) && (alpha == 0xFF || color <= 0x00FFFFFF)) {
            return String.format("#%06X", color & 0x00FFFFFF);
        }
        return String.format("#%08X", color);
    }

    private static boolean isAlphaColorField(Field field) {
        com.sanhiruzu.ami.config.ConfigValue value = field.getAnnotation(com.sanhiruzu.ami.config.ConfigValue.class);
        return value != null && value.value().startsWith("palette.");
    }

    private static Integer parseColor(String raw) {
        if (raw == null) return null;
        String hex = raw.trim();
        if (hex.startsWith("#")) {
            hex = hex.substring(1);
        } else if (hex.regionMatches(true, 0, "0x", 0, 2)) {
            hex = hex.substring(2);
        }
        try {
            if (hex.matches("[0-9A-Fa-f]{6}")) {
                return 0xFF000000 | Integer.parseInt(hex, 16);
            }
            if (hex.matches("[0-9A-Fa-f]{8}")) {
                return (int) Long.parseLong(hex, 16);
            }
        } catch (NumberFormatException ignored) {
        }
        return null;
    }

    private static void configureEditBoxFilter(EditBox editBox) {
        try {
            Method setFilter = EditBox.class.getMethod("setFilter", Predicate.class);
            setFilter.invoke(editBox, (Predicate<String>) TextInputFilter::isAllowedInput);
        } catch (NoSuchMethodException ignored) {
            // Some Minecraft versions/configurations do not expose EditBox#setFilter.
        } catch (Exception ignored) {
            // If reflection fails, fail open. UI should still remain usable.
        }
    }

    private static Component boolLabel(boolean val) {
        return Component.translatable(val ? "ami.config.value.boolean.true" : "ami.config.value.boolean.false");
    }

    static Component enumLabel(Field field) {
        try {
            return enumConstantLabel(field.get(null));
        } catch (Exception e) {
            return Component.literal("?");
        }
    }

    static Component enumConstantLabel(Object constant) {
        try {
            Field dn = constant.getClass().getField("displayName");
            return (Component) dn.get(constant);
        } catch (Exception e) {
            return Component.literal(formatEnumName(constant.toString()));
        }
    }

    private static String formatEnumName(String name) {
        StringBuilder out = new StringBuilder(name.length());
        boolean cap = true;
        for (int i = 0; i < name.length(); i++) {
            char c = name.charAt(i);
            if (c == '_') {
                out.append(' ');
                cap = true;
            } else {
                out.append(cap ? c : Character.toLowerCase(c));
                cap = false;
            }
        }
        return out.toString();
    }
}
