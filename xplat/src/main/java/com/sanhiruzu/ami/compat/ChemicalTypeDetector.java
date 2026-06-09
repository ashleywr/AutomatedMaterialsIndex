package com.sanhiruzu.ami.compat;

import java.util.Locale;

public final class ChemicalTypeDetector {
    private ChemicalTypeDetector() {}

    public static boolean isChemicalType(String typeUid) {
        if (typeUid == null) return false;
        String lower = typeUid.toLowerCase(Locale.ROOT);
        return lower.contains("chemical") || lower.contains("gas") || lower.contains("pigment")
                || lower.contains("slurry") || lower.contains("infuse");
    }
}
