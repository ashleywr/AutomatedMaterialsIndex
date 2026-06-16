package com.sanhiruzu.ami.client;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.minecraft.resources.Identifier;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public final class ThemeResourceStyles {
    private static final Map<String, Field> THEME_FIELDS = discoverThemeFields();

    private ThemeResourceStyles() {
    }

    public static boolean apply(Identifier id, JsonElement element) {
        ThemeDefinition theme = ThemeDefinition.parse(id, element);
        if (theme.isEmpty()) {
            return false;
        }
        theme.apply();
        return true;
    }

    static ThemeDefinition parse(Identifier id, JsonElement element) {
        return ThemeDefinition.parse(id, element);
    }

    private static Map<String, Field> discoverThemeFields() {
        Map<String, Field> fields = new HashMap<>();
        for (Field field : AMITheme.class.getFields()) {
            int modifiers = field.getModifiers();
            if (field.getType() != int.class || !Modifier.isStatic(modifiers) || !Modifier.isPublic(modifiers)) {
                continue;
            }
            fields.put(normalizeKey(field.getName()), field);
        }
        return fields;
    }

    private static String normalizeKey(String key) {
        return key.toLowerCase(Locale.ROOT).replace("_", "").replace("-", "").replace(".", "");
    }

    record ThemeDefinition(Identifier id, Map<Field, Integer> values) {
        static ThemeDefinition parse(Identifier id, JsonElement element) {
            Map<Field, Integer> values = new HashMap<>();
            if (!element.isJsonObject()) {
                return new ThemeDefinition(id, values);
            }

            collect(values, id, "", element.getAsJsonObject());
            return new ThemeDefinition(id, values);
        }

        private static void collect(Map<Field, Integer> values, Identifier id, String prefix, JsonObject object) {
            for (Map.Entry<String, JsonElement> entry : object.entrySet()) {
                String key = prefix.isEmpty() ? entry.getKey() : prefix + "." + entry.getKey();
                JsonElement value = entry.getValue();
                if (value.isJsonObject()) {
                    collect(values, id, key, value.getAsJsonObject());
                    continue;
                }

                Field field = THEME_FIELDS.get(normalizeKey(key));
                if (field == null) {
                    int lastDot = key.lastIndexOf('.');
                    if (lastDot >= 0) {
                        field = THEME_FIELDS.get(normalizeKey(key.substring(lastDot + 1)));
                    }
                }
                if (field == null) {
                    continue;
                }

                Integer parsed = parseInt(value);
                if (parsed == null) {
                    continue;
                }
                values.put(field, parsed);
            }
        }

        private static Integer parseInt(JsonElement value) {
            try {
                if (value.isJsonPrimitive() && value.getAsJsonPrimitive().isNumber()) {
                    return value.getAsInt();
                }
                if (!value.isJsonPrimitive() || !value.getAsJsonPrimitive().isString()) {
                    return null;
                }

                String raw = value.getAsString().trim();
                if (raw.equalsIgnoreCase("transparent") || raw.equalsIgnoreCase("none") || raw.equalsIgnoreCase("off")) {
                    return 0;
                }

                String hex = raw;
                if (hex.startsWith("#")) {
                    hex = hex.substring(1);
                } else if (hex.regionMatches(true, 0, "0x", 0, 2)) {
                    hex = hex.substring(2);
                }
                if (hex.length() == 6) {
                    hex = "FF" + hex;
                }
                if (hex.length() == 8 && hex.chars().allMatch(ThemeDefinition::isHexDigit)) {
                    return (int) Long.parseLong(hex, 16);
                }
                return Integer.parseInt(raw);
            } catch (RuntimeException e) {
                return null;
            }
        }

        private static boolean isHexDigit(int c) {
            return (c >= '0' && c <= '9') || (c >= 'a' && c <= 'f') || (c >= 'A' && c <= 'F');
        }

        boolean isEmpty() {
            return values.isEmpty();
        }

        void apply() {
            for (Map.Entry<Field, Integer> entry : values.entrySet()) {
                try {
                    entry.getKey().setInt(null, entry.getValue());
                } catch (IllegalAccessException ignored) {
                }
            }
        }
    }
}
