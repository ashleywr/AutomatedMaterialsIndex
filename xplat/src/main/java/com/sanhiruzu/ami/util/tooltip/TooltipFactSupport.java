package com.sanhiruzu.ami.util.tooltip;

import com.sanhiruzu.ami.client.AMITheme;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;

import java.util.List;
import java.util.Locale;

public final class TooltipFactSupport {
    private TooltipFactSupport() {
    }

    public static List<Component> line(String key, String value) {
        return line(key, value, AMITheme.TEXT_PRIMARY); // Default values to bright white/primary
    }

    public static List<Component> line(String key, String value, int valueColorArgb) {
        if (value == null || value.isBlank()) return List.of();
        
        // Formats the injected %s value with the bright color, while the base translation string remains subtle
        return List.of(Component.translatable(key, Component.literal(value).withStyle(s -> s.withColor(valueColorArgb)))
                .withStyle(s -> s.withColor(AMITheme.TEXT_SUBTLE)));
    }

    public static List<Component> line(String key, String value, ChatFormatting formatting) {
        if (value == null || value.isBlank()) return List.of();
        
        return List.of(Component.translatable(key, Component.literal(value).withStyle(formatting))
                .withStyle(s -> s.withColor(AMITheme.TEXT_SUBTLE)));
    }

    public static List<Component> message(String key) {
        return List.of(Component.translatable(key).withStyle(s -> s.withColor(AMITheme.TEXT_SUBTLE)));
    }

    public static String formatNumber(String raw, String suffix) {
        if (raw == null || raw.isBlank()) return "";
        try {
            long value = Long.parseLong(raw.trim());
            return String.format(Locale.ROOT, "%,d%s", value, suffix);
        } catch (NumberFormatException ignored) {
            return raw + suffix;
        }
    }

    public static long parseLong(String raw, long fallback) {
        if (raw == null || raw.isBlank()) return fallback;
        try {
            return Long.parseLong(raw.trim());
        } catch (NumberFormatException ignored) {
            return fallback;
        }
    }

    public static String firstNonBlank(String first, String second) {
        return first == null || first.isBlank() ? second : first;
    }
}
