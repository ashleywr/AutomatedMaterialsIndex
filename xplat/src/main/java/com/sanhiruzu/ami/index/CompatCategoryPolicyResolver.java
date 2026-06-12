package com.sanhiruzu.ami.index;

import com.sanhiruzu.ami.compat.CompatFamilyDetector;
import com.sanhiruzu.ami.config.AmiConfig;

import java.util.Locale;
import java.util.Map;

final class CompatCategoryPolicyResolver {
    private CompatCategoryPolicyResolver() {
    }

    static AmiConfig.CompatCategoryPolicy resolve(Map<String, String> attributes) {
        if (attributes == null || attributes.isEmpty()) {
            return AmiConfig.CompatCategoryPolicy.SEMANTIC;
        }

        AmiConfig.CompatCategoryPolicy override = parse(attributes.get(SearchNodeKeys.COMPAT_CATEGORY_POLICY));
        if (override != null) {
            return override;
        }

        String family = attributes.getOrDefault(SearchNodeKeys.PRIMARY_COMPAT_FAMILY,
                attributes.getOrDefault(SearchNodeKeys.COMPAT_FAMILY, ""));
        return switch (normalize(family)) {
            case CompatFamilyDetector.COBBLEMON -> AmiConfig.cobblemonCategoryPolicy;
            case CompatFamilyDetector.CREATE -> AmiConfig.createCategoryPolicy;
            case "ae2", "appliedenergistics2" -> AmiConfig.ae2CategoryPolicy;
            case "mekanism" -> AmiConfig.mekanismCategoryPolicy;
            case "gregtech", "gtceu" -> AmiConfig.gregtechCategoryPolicy;
            case "sophisticated", "sophisticatedbackpacks", "sophisticatedstorage" -> AmiConfig.sophisticatedCategoryPolicy;
            case CompatFamilyDetector.MODULAR_GEAR, CompatFamilyDetector.TINKERS, CompatFamilyDetector.SILENT_GEAR,
                 "tconstruct", "silentgear" -> AmiConfig.modularGearCategoryPolicy;
            case "minecolonies" -> AmiConfig.minecoloniesCategoryPolicy;
            case "apotheosis" -> AmiConfig.apotheosisCategoryPolicy;
            case "botania" -> AmiConfig.botaniaCategoryPolicy;
            case CompatFamilyDetector.ARS_NOUVEAU, "arsnouveau" -> AmiConfig.arsNouveauCategoryPolicy;
            case "pastel" -> AmiConfig.pastelCategoryPolicy;
            case "malum" -> AmiConfig.malumCategoryPolicy;
            case "swem" -> AmiConfig.swemCategoryPolicy;
            case "cataclysm" -> AmiConfig.cataclysmCategoryPolicy;
            case CompatFamilyDetector.MAPPING, "map", "maps", "xaero", "journeymap", "ftb" -> AmiConfig.mapUtilityCategoryPolicy;
            default -> AmiConfig.CompatCategoryPolicy.SEMANTIC;
        };
    }

    private static AmiConfig.CompatCategoryPolicy parse(String value) {
        String normalized = normalize(value).replace('-', '_').replace(' ', '_');
        if (normalized.isBlank()) {
            return null;
        }
        try {
            return AmiConfig.CompatCategoryPolicy.valueOf(normalized.toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ignored) {
            return null;
        }
    }

    private static String normalize(String value) {
        return value == null ? "" : value.toLowerCase(Locale.ROOT).trim();
    }
}
