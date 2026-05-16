package com.sanhiruzu.ami.client.results;

import com.sanhiruzu.ami.index.SearchNode;
import com.sanhiruzu.ami.index.SearchNodeKeys;

/**
 * Fields that can appear on the subtitle line of a list-view row.
 * Ordinal order is the display order when multiple fields are enabled.
 */
public enum RowField {

    MOD_NAME("Mod") {
        @Override public String extract(SearchNode node) {
            return node.id().getNamespace();
        }
    },

    STORAGE_CAPACITY("Storage") {
        @Override public String extract(SearchNode node) {
            String cap = node.meta(SearchNodeKeys.ESM_CAPACITY, "");
            return cap.isEmpty() ? "" : cap + " items";
        }
    },

    DPS("DPS") {
        @Override public String extract(SearchNode node) {
            return node.meta(SearchNodeKeys.DPS, "");
        }
    };

    public final String displayName;

    RowField(String displayName) {
        this.displayName = displayName;
    }

    /** Returns the display string for this field from the given node, or "" if not applicable. */
    public abstract String extract(SearchNode node);
}
