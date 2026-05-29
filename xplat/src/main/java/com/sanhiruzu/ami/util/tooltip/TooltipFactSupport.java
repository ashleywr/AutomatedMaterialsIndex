package com.sanhiruzu.ami.util.tooltip;

import com.sanhiruzu.ami.client.AMITheme;
import net.minecraft.network.chat.Component;

import java.util.List;
import java.util.Locale;

final class TooltipFactSupport {
    private TooltipFactSupport() {
    }

    static List<Component> line(String key, String value) {
        if (value == null || value.isBlank()) return List.of();
        return List.of(Component.translatable(key, value).withStyle(s -> s.withColor(AMITheme.TEXT_SUBTLE)));
    }

    static List<Component> message(String key) {
        return List.of(Component.translatable(key).withStyle(s -> s.withColor(AMITheme.TEXT_SUBTLE)));
    }

    static String formatNumber(String raw, String suffix) {
        if (raw == null || raw.isBlank()) return "";
        try {
            long value = Long.parseLong(raw.trim());
            return String.format(Locale.ROOT, "%,d%s", value, suffix);
        } catch (NumberFormatException ignored) {
            return raw + suffix;
        }
    }

    static long parseLong(String raw, long fallback) {
        if (raw == null || raw.isBlank()) return fallback;
        try {
            return Long.parseLong(raw.trim());
        } catch (NumberFormatException ignored) {
            return fallback;
        }
    }

    static String firstNonBlank(String first, String second) {
        return first == null || first.isBlank() ? second : first;
    }
}
