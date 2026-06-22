package com.sanhiruzu.ami.index;

import java.util.Set;

final class PrimaryCategoryModFamilies {
    private static final Set<String> PORTABLE_STORAGE_FAMILY_MOD_IDS = Set.of(
            "sophisticatedbackpacks"
    );
    private static final Set<String> STORAGE_FAMILY_MOD_IDS = Set.of(
            "ae2", "functionalstorage", "ironchest", "merequester", "refinedstorage",
            "sophisticatedstorage", "storagedrawers"
    );

    private PrimaryCategoryModFamilies() {
    }

    static PrimaryCategoryModFamily classify(String modId) {
        if (modId == null || modId.isBlank()) {
            return PrimaryCategoryModFamily.GENERIC;
        }
        if (isPortableStorageFamilyMod(modId)) {
            return PrimaryCategoryModFamily.PORTABLE_STORAGE;
        }
        if (isStorageFamilyMod(modId)) {
            return PrimaryCategoryModFamily.STORAGE;
        }
        if (modId.contains("delight")
                || modId.equals("croptopia")
                || modId.equals("createfood")
                || modId.equals("bountifulfares")) {
            return PrimaryCategoryModFamily.FOOD;
        }
        if (isCreateFamilyMod(modId)) {
            return PrimaryCategoryModFamily.CREATE;
        }
        if (isAutomationFamilyMod(modId)) {
            return PrimaryCategoryModFamily.AUTOMATION;
        }
        if (isDecorFamilyMod(modId)) {
            return PrimaryCategoryModFamily.DECOR;
        }
        return PrimaryCategoryModFamily.GENERIC;
    }

    private static boolean isCreateFamilyMod(String modId) {
        return modId.equals("create")
                || modId.startsWith("create")
                || modId.equals("railways")
                || modId.equals("copycats")
                || modId.equals("sliceanddice")
                || modId.equals("bellsandwhistles");
    }

    private static boolean isPortableStorageFamilyMod(String modId) {
        return PORTABLE_STORAGE_FAMILY_MOD_IDS.contains(modId);
    }

    private static boolean isStorageFamilyMod(String modId) {
        return STORAGE_FAMILY_MOD_IDS.contains(modId);
    }

    private static boolean isDecorFamilyMod(String modId) {
        return modId.startsWith("mcw")
                || modId.equals("cfm")
                || modId.equals("cfm_wap")
                || modId.equals("redeco")
                || modId.equals("another_furniture")
                || modId.equals("moa_decor_bath")
                || modId.equals("refurbished_furniture")
                || modId.equals("arts_and_crafts");
    }

    private static boolean isAutomationFamilyMod(String modId) {
        return modId.equals("pneumaticcraft")
                || modId.startsWith("pneumaticcraft")
                || modId.equals("securitycraft")
                || modId.contains("projectred")
                || modId.equals("laserio")
                || modId.equals("enderio");
    }
}
