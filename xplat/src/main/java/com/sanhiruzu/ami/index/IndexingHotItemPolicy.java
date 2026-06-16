package com.sanhiruzu.ami.index;

import com.sanhiruzu.ami.config.AmiConfig;
import net.minecraft.resources.Identifier;

import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Debug-only policy for experimenting with known hot items without changing
 * default indexing behavior for all users.
 */
public final class IndexingHotItemPolicy {
    private static final boolean DEFER_FACADE_ITEMS =
            Boolean.getBoolean("ami.debug.deferFacadeItems");
    private static final boolean FAST_FACADE_INDEX =
            Boolean.getBoolean("ami.debug.fastFacadeIndex");
    private static final boolean CLASSIFICATION_TRACE =
            Boolean.getBoolean("ami.debug.classificationTrace");
    private static final boolean ICON_AUDIT =
            Boolean.getBoolean("ami.debug.iconAudit");
    private static final Set<String> DEFERRED_INDEX_NAMESPACES =
            parseNamespaces(System.getProperty("ami.debug.deferredIndexNamespaces", ""));

    private IndexingHotItemPolicy() {
    }

    public static boolean shouldDeferUntilTail(Identifier id) {
        return DEFER_FACADE_ITEMS && isFacadeLike(id);
    }

    public static boolean shouldCollapseCreativeStacks(Identifier id) {
        return shouldUseFastFacadeIndex(id);
    }

    public static boolean shouldHideComponentBackedVariantsByDefault(Identifier id) {
        return !AmiConfig.devMode && isFacadeLike(id);
    }

    public static String componentBackedVariantSuppressionReason(Identifier id) {
        return shouldHideComponentBackedVariantsByDefault(id)
                ? "facade_variants_hidden_by_default"
                : "";
    }

    public static boolean shouldUseFastFacadeIndex(Identifier id) {
        return FAST_FACADE_INDEX && !AmiConfig.devMode && isFacadeLike(id);
    }

    public static boolean hasDeferredIndexNamespaces() {
        return !DEFERRED_INDEX_NAMESPACES.isEmpty();
    }

    public static boolean shouldDeferFullIndex(Identifier id) {
        return id != null && DEFERRED_INDEX_NAMESPACES.contains(id.getNamespace().toLowerCase(Locale.ROOT));
    }

    public static String deferredIndexNamespacesForLog() {
        return String.join(",", DEFERRED_INDEX_NAMESPACES);
    }

    public static String cacheKeyFragment() {
        return "_deferFacadeItems=" + DEFER_FACADE_ITEMS
                + "_fastFacadeIndex=" + FAST_FACADE_INDEX
                + "_classificationTrace=" + CLASSIFICATION_TRACE
                + "_deferredIndexNamespaces=" + deferredIndexNamespacesForLog();
    }

    public static boolean shouldRecordClassificationTrace() {
        return CLASSIFICATION_TRACE;
    }

    public static boolean shouldAuditIcons() {
        return ICON_AUDIT;
    }

    public static boolean isFacadeLike(Identifier id) {
        if (id == null) return false;
        String namespace = id.getNamespace();
        String path = id.getPath();
        return (("ae2".equals(namespace) || "appliedenergistics2".equals(namespace))
                && "facade".equals(path))
                || path.contains("facade");
    }

    private static Set<String> parseNamespaces(String raw) {
        if (raw == null || raw.isBlank()) {
            return Set.of();
        }
        return Arrays.stream(raw.split("[,;\\s]+"))
                .map(IndexingHotItemPolicy::normalizeDeferredNamespace)
                .filter(value -> !value.isBlank())
                .collect(Collectors.toCollection(LinkedHashSet::new));
    }

    private static String normalizeDeferredNamespace(String raw) {
        String value = raw == null ? "" : raw.trim().toLowerCase(Locale.ROOT);
        while (value.startsWith("@")) {
            value = value.substring(1);
        }
        value = value.replace('-', '_');
        return switch (value) {
            case "everycompat", "every_compat" -> "everycomp";
            default -> value;
        };
    }
}
