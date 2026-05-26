package com.sanhiruzu.ami.client.widget;

import com.sanhiruzu.ami.config.ConfigColor;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.network.chat.Component;

import java.lang.reflect.Field;
import java.util.function.Consumer;

/**
 * Factory for creating configuration widgets based on field type and annotations.
 */
public class AmiWidgetFactory {

    public static AbstractWidget createWidget(Field field, Consumer<Object> onChange) {
        Class<?> type = field.getType();
        Minecraft mc = Minecraft.getInstance();

        try {
            if (field.isAnnotationPresent(ConfigColor.class)) {
                int color = field.getInt(null) & 0xFFFFFF;
                EditBox eb = new EditBox(mc.font, 0, 0, 72, 18, Component.empty());
                eb.setValue(String.format("#%06X", color));
                eb.setResponder(s -> {
                    if (s.matches("^#[0-9A-Fa-f]{6}$")) {
                        try {
                            int newColor = 0xFF000000 | Integer.parseInt(s.substring(1), 16);
                            field.set(null, newColor);
                            onChange.accept(newColor);
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
            } else if (type == int.class) {
                EditBox eb = new EditBox(mc.font, 0, 0, 72, 18, Component.empty());
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
            } else if (type.isEnum()) {
                return Button.builder(enumLabel(field), b -> {
                    try {
                        Object[] constants = type.getEnumConstants();
                        int idx = 0;
                        for (int i = 0; i < constants.length; i++) {
                            if (constants[i].equals(field.get(null))) {
                                idx = (i + 1) % constants.length;
                                break;
                            }
                        }
                        Object next = constants[idx];
                        field.set(null, next);
                        b.setMessage(enumConstantLabel(next));
                        onChange.accept(next);
                    } catch (Exception ignored) {
                    }
                }).bounds(0, 0, 72, 18).build();
            } else {
                EditBox eb = new EditBox(mc.font, 0, 0, 72, 18, Component.empty());
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

    private static Component boolLabel(boolean val) {
        return Component.translatable(val ? "ami.config.value.boolean.true" : "ami.config.value.boolean.false");
    }

    private static Component enumLabel(Field field) {
        try {
            return enumConstantLabel(field.get(null));
        } catch (Exception e) {
            return Component.literal("?");
        }
    }

    private static Component enumConstantLabel(Object constant) {
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
