package com.sanhiruzu.ami.compat;

import com.sanhiruzu.ami.index.SearchNodeKeys;
import net.minecraft.resources.ResourceLocation;

import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

public final class GeneratedPaletteCompat {
    private GeneratedPaletteCompat() {
    }

    public static boolean collapseByBaseTag(Map<String, String> meta, String namespace, Set<String> ignoredSuffixes) {
        String familyTag = baseFamilyTag(meta, namespace, ignoredSuffixes);
        if (familyTag.isBlank()) {
            return false;
        }
        ResourceLocation familyId = ResourceLocation.tryParse(familyTag);
        if (familyId == null) {
            return false;
        }
        markDefaultCollapsed(meta, familyTag, titleCase(familyId.getPath()));
        return true;
    }

    public static boolean collapseByPathRoot(ResourceLocation id, Map<String, String> meta, String rootPath,
                                             boolean pluralizeLabel) {
        if (id == null || rootPath == null || rootPath.isBlank() || rootPath.equals(id.getPath())) {
            return false;
        }
        String label = titleCase(rootPath);
        if (pluralizeLabel) {
            label = pluralize(label);
        }
        markDefaultCollapsed(meta, id.getNamespace() + ":" + rootPath, label);
        return true;
    }

    public static String stripConnectingAndShapeSuffixes(String path) {
        String result = path == null ? "" : path.toLowerCase(Locale.ROOT);
        result = stripSuffix(result, "_connecting");
        result = stripSuffix(result, "_stairs");
        result = stripSuffix(result, "_slab");
        result = stripSuffix(result, "_wall");
        result = stripSuffix(result, "_button");
        result = stripSuffix(result, "_pressure_plate");
        return result;
    }

    public static String titleCase(String path) {
        StringBuilder label = new StringBuilder();
        for (String token : path.split("[_/\\-]+")) {
            if (token.isBlank()) {
                continue;
            }
            if (!label.isEmpty()) {
                label.append(' ');
            }
            label.append(Character.toUpperCase(token.charAt(0)));
            if (token.length() > 1) {
                label.append(token.substring(1));
            }
        }
        return label.toString();
    }

    public static String pluralize(String label) {
        if (label == null || label.isBlank() || label.endsWith("s")) {
            return label;
        }
        if (label.endsWith("y") && label.length() > 1) {
            char beforeY = Character.toLowerCase(label.charAt(label.length() - 2));
            if ("aeiou".indexOf(beforeY) < 0) {
                return label.substring(0, label.length() - 1) + "ies";
            }
        }
        return label + "s";
    }

    private static String baseFamilyTag(Map<String, String> meta, String namespace, Set<String> ignoredSuffixes) {
        LinkedHashSet<String> tags = new LinkedHashSet<>();
        addTags(tags, meta.getOrDefault(SearchNodeKeys.TAGS, ""));
        addTags(tags, meta.getOrDefault(SearchNodeKeys.BLOCK_TAGS, ""));

        for (String tag : tags) {
            ResourceLocation tagId = ResourceLocation.tryParse(tag);
            if (tagId == null || !namespace.equals(tagId.getNamespace())) {
                continue;
            }
            if (endsWithAny(tagId.getPath(), ignoredSuffixes)) {
                continue;
            }
            return tagId.toString();
        }
        return "";
    }

    private static void addTags(Set<String> tags, String csv) {
        if (csv == null || csv.isBlank()) {
            return;
        }
        for (String part : csv.split(",")) {
            String normalized = part.trim().toLowerCase(Locale.ROOT);
            if (!normalized.isBlank()) {
                tags.add(normalized);
            }
        }
    }

    private static boolean endsWithAny(String path, Set<String> suffixes) {
        for (String suffix : suffixes) {
            if (path.endsWith(suffix)) {
                return true;
            }
        }
        return false;
    }

    private static void markDefaultCollapsed(Map<String, String> meta, String familyKey, String label) {
        meta.putIfAbsent(SearchNodeKeys.COLLAPSE_FAMILY, familyKey);
        meta.putIfAbsent(SearchNodeKeys.COLLAPSE_LABEL, label);
        meta.putIfAbsent(SearchNodeKeys.VARIANT_COLLAPSE_MODE, "default_collapsed");
    }

    private static String stripSuffix(String value, String suffix) {
        return value.endsWith(suffix) ? value.substring(0, value.length() - suffix.length()) : value;
    }
}
