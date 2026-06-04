package com.sanhiruzu.ami.util;

import net.minecraft.network.chat.Component;

import java.util.List;

final class TooltipLineMatcher {
    private TooltipLineMatcher() {
    }

    static boolean containsLine(List<Component> lines, String text) {
        String needle = stripFormatting(text).trim();
        if (needle.isEmpty()) {
            return false;
        }

        for (Component line : lines) {
            if (line != null && needle.equals(stripFormatting(line.getString()).trim())) {
                return true;
            }
        }
        return false;
    }

    private static String stripFormatting(String value) {
        if (value == null || value.indexOf('\u00A7') < 0) {
            return value == null ? "" : value;
        }

        StringBuilder result = new StringBuilder(value.length());
        for (int i = 0; i < value.length(); i++) {
            char current = value.charAt(i);
            if (current == '\u00A7' && i + 1 < value.length() && isFormattingCode(value.charAt(i + 1))) {
                i++;
                continue;
            }
            result.append(current);
        }
        return result.toString();
    }

    private static boolean isFormattingCode(char code) {
        char normalized = Character.toLowerCase(code);
        return (normalized >= '0' && normalized <= '9')
                || (normalized >= 'a' && normalized <= 'f')
                || (normalized >= 'k' && normalized <= 'o')
                || normalized == 'r';
    }
}
