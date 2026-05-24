package com.sanhiruzu.ami.client.results;

import com.sanhiruzu.ami.config.AmiConfig;
import com.sanhiruzu.ami.index.SearchNode;
import net.neoforged.fml.ModList;

import java.util.ArrayList;
import java.util.Collection;
import java.util.EnumSet;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Manages which fields are shown on the subtitle line of list-view rows.
 * <p>
 * Persists to AmiConfig as a comma-separated list of RowField names.
 * Resets to the default (MOD_NAME) whenever the installed mod list changes,
 * detected via a stored hashCode of the sorted mod-ID list.
 */
public final class RowFieldConfig {
    private RowFieldConfig() {
    }

    private static volatile boolean initialized = false;

    // ── Init / checksum ───────────────────────────────────────────────────────

    private static synchronized void ensureInitialized() {
        if (initialized) return;
        initialized = true;

        int current = computeChecksum();
        String savedFields = AmiConfig.subtitleFields;
        boolean modlistChanged = current != AmiConfig.subtitleFieldsChecksum;

        if (savedFields == null || savedFields.isBlank() || modlistChanged) {
            AmiConfig.subtitleFields = RowField.MOD_NAME.name();
            AmiConfig.subtitleFieldsChecksum = current;
        }
    }

    /**
     * Java hashCode of the sorted mod-ID list — cheap and sufficient for invalidation.
     */
    private static int computeChecksum() {
        return ModList.get().getMods().stream()
                .map(info -> info.getModId())
                .sorted()
                .collect(Collectors.joining(","))
                .hashCode();
    }

    // ── Read / write ──────────────────────────────────────────────────────────

    /**
     * Returns the active subtitle fields in display order (RowField ordinal).
     * Empty list means no subtitle line should be drawn.
     */
    public static List<RowField> getSubtitleFields() {
        ensureInitialized();
        String raw = AmiConfig.subtitleFields;
        if (raw == null || raw.isBlank()) return List.of();

        EnumSet<RowField> result = EnumSet.noneOf(RowField.class);
        for (String part : raw.split(",")) {
            String trimmed = part.trim();
            if (trimmed.isEmpty()) continue;
            try {
                result.add(RowField.valueOf(trimmed));
            } catch (IllegalArgumentException ignored) {
            }
        }
        return new ArrayList<>(result); // EnumSet iterates in declaration order
    }

    /**
     * Persists a new set of subtitle fields.
     */
    public static void setSubtitleFields(Collection<RowField> fields) {
        EnumSet<RowField> ordered = fields.isEmpty()
                ? EnumSet.noneOf(RowField.class)
                : EnumSet.copyOf(fields);
        AmiConfig.subtitleFields =
                ordered.stream().map(Enum::name).collect(Collectors.joining(","));
    }

    // ── Rendering helper ──────────────────────────────────────────────────────

    /**
     * Builds the joined subtitle string for one node.
     * Fields with no data for this node are silently skipped.
     * Returns "" when nothing should be shown.
     */
    public static String buildSubtitle(SearchNode node) {
        List<String> parts = new ArrayList<>();
        for (RowField field : getSubtitleFields()) {
            String val = field.extract(node);
            if (!val.isEmpty()) parts.add(val);
        }
        return String.join(" · ", parts);
    }
}
