package com.sanhiruzu.ami.util;

import java.util.Locale;

public final class StorageDisplayFormatter {
    private static final long VANILLA_CHEST_ITEMS = 27L * 64L;

    private StorageDisplayFormatter() {
    }

    public static String formatChestEquivalent(String rawCapacity) {
        return formatChestEquivalent(rawCapacity, false);
    }

    public static String formatChestEquivalent(String rawCapacity, boolean includeItems) {
        if (rawCapacity == null || rawCapacity.isBlank()) return "";
        try {
            long capacity = Long.parseLong(rawCapacity.trim());
            if (capacity <= 0) return "";

            double chests = (double) capacity / VANILLA_CHEST_ITEMS;
            String equivalent = trimDecimal(chests) + "x chest";
            if (!includeItems) return equivalent;

            return equivalent + " (" + String.format(Locale.ROOT, "%,d items", capacity) + ")";
        } catch (NumberFormatException ignored) {
            return rawCapacity;
        }
    }

    private static String trimDecimal(double value) {
        if (Math.abs(value - Math.rint(value)) < 0.0001D) {
            return Long.toString(Math.round(value));
        }
        return String.format(Locale.ROOT, "%.1f", value)
                .replaceAll("\\.0$", "");
    }
}
