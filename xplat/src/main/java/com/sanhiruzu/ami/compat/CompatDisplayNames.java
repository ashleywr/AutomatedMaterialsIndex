package com.sanhiruzu.ami.compat;

import com.sanhiruzu.ami.index.SearchNode;
import com.sanhiruzu.ami.index.providers.RegistryUtils;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

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

    public static List<String> modSuggestionAliases(String canonicalToken) {
        String normalizedCanonical = normalize(canonicalToken);
        if (normalizedCanonical.isBlank()) {
            return List.of();
        }
        Set<String> aliases = new LinkedHashSet<>();
        addIfAlias(aliases, RegistryUtils.modDisplayName(normalizedCanonical), normalizedCanonical);
        for (var entry : MOD_ALIASES.entrySet()) {
            Alias alias = entry.getValue();
            if (!alias.canonicalToken().equals(normalizedCanonical)) {
                continue;
            }
            addIfAlias(aliases, entry.getKey(), normalizedCanonical);
            addIfAlias(aliases, alias.displayName(), normalizedCanonical);
        }
        return List.copyOf(new ArrayList<>(aliases));
    }

    private static void addIfAlias(Set<String> aliases, String alias, String normalizedCanonical) {
        String normalizedAlias = normalize(alias);
        if (normalizedAlias.isBlank() || normalizedAlias.equals(normalizedCanonical)) {
            return;
        }
        aliases.add(alias);
    }

    private static String normalize(String value) {
        return value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
    }

    private record Alias(String canonicalToken, String displayName) {
    }
}
