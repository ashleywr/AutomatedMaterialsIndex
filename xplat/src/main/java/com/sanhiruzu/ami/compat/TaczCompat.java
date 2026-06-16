package com.sanhiruzu.ami.compat;

import com.sanhiruzu.ami.index.ItemFacet;
import com.sanhiruzu.ami.index.SearchNodeKeys;
import net.minecraft.resources.Identifier;

import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.StringJoiner;

public final class TaczCompat {
    private static final String MOD_ID = "tacz";

    private TaczCompat() {
    }

    public static void enrichItem(Identifier id, Map<String, String> meta) {
        if (id == null || meta == null) {
            return;
        }
        if (meta.getOrDefault(SearchNodeKeys.COMPAT_FAMILIES, "").isBlank()) {
            CompatFamilyDetector.detect(id, meta);
        }
        if (!isTaczItem(id, meta)) {
            return;
        }

        Context context = new Context(id, meta);
        LinkedHashSet<String> facts = new LinkedHashSet<>();
        addClassFacts(context, facts);
        addPathFacts(context, facts);

        String kind = classifyKind(context, facts);
        if (!kind.isBlank()) {
            meta.put(SearchNodeKeys.TACZ_ITEM_KIND, kind);
            addSearchToken(meta, "tacz_" + kind);
        }
        if (facts.contains("attachment")) {
            addFacet(meta, ItemFacet.UPGRADE);
            meta.putIfAbsent(SearchNodeKeys.COLLAPSE_FAMILY, MOD_ID + ":attachment");
            meta.putIfAbsent(SearchNodeKeys.COLLAPSE_LABEL, "Attachments");
            meta.putIfAbsent(SearchNodeKeys.VARIANT_COLLAPSE_MODE, "default_collapsed");
            addSearchToken(meta, "attachment");
            addSearchToken(meta, "weapon_attachment");
        }
        if (!facts.isEmpty()) {
            meta.put(SearchNodeKeys.TACZ_FACTS, join(facts));
            for (String fact : facts) {
                addSearchToken(meta, fact);
                addSearchToken(meta, "tacz_" + fact);
            }
        }
    }

    private static boolean isTaczItem(Identifier id, Map<String, String> meta) {
        return MOD_ID.equals(id.getNamespace()) || CompatFamilyDetector.hasFamily(meta, CompatFamilyDetector.TACZ);
    }

    private static void addClassFacts(Context context, Set<String> facts) {
        if (containsAny(context.itemClass, "AttachmentItem")) {
            facts.add("attachment");
        }
        if (containsAny(context.itemClass, "ModernKineticGunItem")) {
            facts.add("gun");
        }
        if (containsAny(context.itemClass, "AmmoItem", "AmmoBoxItem")) {
            facts.add("ammo");
        }
        if (containsAny(context.itemClass, "GunSmithTableItem", "DefaultTableItem")) {
            facts.add("workstation");
        }
    }

    private static void addPathFacts(Context context, Set<String> facts) {
        if (context.path.startsWith("attachment/") || context.path.contains("/attachment/")) {
            facts.add("attachment");
        }
        if (context.path.startsWith("modern_kinetic_gun/")) {
            facts.add("gun");
        }
        if (context.path.startsWith("ammo/") || context.path.startsWith("ammo_box/")) {
            facts.add("ammo");
        }
        if (context.path.contains("gun_smith_table") || context.path.contains("workbench")) {
            facts.add("workstation");
        }
    }

    private static String classifyKind(Context context, Set<String> facts) {
        if (facts.contains("attachment")) return "attachments";
        if (facts.contains("gun") || hasToken(context.facets, "ranged_weapon")) return "guns";
        if (facts.contains("ammo") || hasToken(context.facets, "projectile")) return "ammo";
        if (facts.contains("workstation")) return "workstations";
        return "";
    }

    private static boolean containsAny(String value, String... needles) {
        if (value == null || value.isBlank()) {
            return false;
        }
        String normalized = value.toLowerCase(Locale.ROOT);
        for (String needle : needles) {
            if (normalized.contains(needle.toLowerCase(Locale.ROOT))) {
                return true;
            }
        }
        return false;
    }

    private static boolean hasToken(String csv, String token) {
        for (String value : splitCsv(csv)) {
            if (value.equals(token)) {
                return true;
            }
        }
        return false;
    }

    private static Iterable<String> splitCsv(String csv) {
        if (csv == null || csv.isBlank()) {
            return Set.of();
        }
        LinkedHashSet<String> values = new LinkedHashSet<>();
        for (String value : csv.split(",")) {
            String normalized = value.trim().toLowerCase(Locale.ROOT);
            if (!normalized.isBlank()) {
                values.add(normalized);
            }
        }
        return values;
    }

    private static void addFacet(Map<String, String> meta, ItemFacet facet) {
        String encoded = meta.getOrDefault(SearchNodeKeys.FACETS, "");
        if (encoded.isBlank()) {
            meta.put(SearchNodeKeys.FACETS, facet.id());
            return;
        }
        for (String value : encoded.split(",")) {
            if (facet.id().equals(value.trim())) {
                return;
            }
        }
        meta.put(SearchNodeKeys.FACETS, encoded + "," + facet.id());
    }

    private static String join(Set<String> values) {
        StringJoiner joiner = new StringJoiner(",");
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                joiner.add(value);
            }
        }
        return joiner.toString();
    }

    private static void addSearchToken(Map<String, String> meta, String token) {
        if (token == null || token.isBlank()) {
            return;
        }
        LinkedHashSet<String> values = new LinkedHashSet<>();
        String existing = meta.getOrDefault(SearchNodeKeys.SEARCH_TOKENS, "");
        for (String value : existing.split("\\s+")) {
            if (!value.isBlank()) {
                values.add(value);
            }
        }
        if (values.add(token)) {
            meta.put(SearchNodeKeys.SEARCH_TOKENS, String.join(" ", values));
        }
    }

    private static final class Context {
        final String path;
        final String itemClass;
        final String facets;

        Context(Identifier id, Map<String, String> meta) {
            this.path = id.getPath().toLowerCase(Locale.ROOT);
            this.itemClass = meta.getOrDefault(SearchNodeKeys.ITEM_CLASS, "");
            this.facets = meta.getOrDefault(SearchNodeKeys.FACETS, "").toLowerCase(Locale.ROOT);
        }
    }
}
