package com.sanhiruzu.ami.compat;

import com.sanhiruzu.ami.index.SearchNode;
import com.sanhiruzu.ami.index.providers.RegistryUtils;

import java.util.Locale;
import java.util.Map;

public final class CompatDisplayNames {
    private static final Map<String, Alias> MOD_ALIASES = Map.of(
            "gtceu", new Alias("gregtech", "GregTech"),
            "gregtech", new Alias("gregtech", "GregTech"),
            "appliedenergistics2", new Alias("ae2", "Applied Energistics 2"),
            "ae2", new Alias("ae2", "Applied Energistics 2"),
            "tconstruct", new Alias("tinkers", "Tinkers' Construct"),
            "tinkers", new Alias("tinkers", "Tinkers' Construct"),
            "silentgear", new Alias("silent_gear", "Silent Gear"),
            "silent_gear", new Alias("silent_gear", "Silent Gear")
    );

    private CompatDisplayNames() {
    }

    public static String displayModName(SearchNode node) {
        if (node == null || node.id() == null) {
            return "";
        }
        return displayModName(node.id().getNamespace());
    }

    public static String displayModName(String namespace) {
        String normalized = normalize(namespace);
        Alias alias = MOD_ALIASES.get(normalized);
        if (alias != null) {
            return alias.displayName();
        }
        return RegistryUtils.modDisplayName(namespace == null ? "" : namespace);
    }

    public static String canonicalModSuggestionToken(String token) {
        String normalized = normalize(token);
        Alias alias = MOD_ALIASES.get(normalized);
        return alias == null ? normalized : alias.canonicalToken();
    }

    public static String displayModSuggestionToken(String token) {
        String normalized = normalize(token);
        Alias alias = MOD_ALIASES.get(normalized);
        return alias == null ? token : alias.displayName();
    }

    public static boolean isModSuggestionAlias(String alias, String canonicalToken) {
        String normalizedAlias = normalize(alias);
        String normalizedCanonical = normalize(canonicalToken);
        Alias mapped = MOD_ALIASES.get(normalizedAlias);
        return mapped != null && mapped.canonicalToken().equals(normalizedCanonical)
                && !normalizedAlias.equals(normalizedCanonical);
    }

    private static String normalize(String value) {
        return value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
    }

    private record Alias(String canonicalToken, String displayName) {
    }
}
