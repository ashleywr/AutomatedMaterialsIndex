package com.sanhiruzu.ami.client.results;

import com.sanhiruzu.ami.index.SearchNode;
import com.sanhiruzu.ami.index.SearchNodeKeys;
import net.minecraft.network.chat.Component;

/**
 * Fields that can appear on the subtitle line of a list-view row.
 * Ordinal order is the display order when multiple fields are enabled.
 */
public enum RowField {

    MOD_NAME(Component.translatable("ami.row_field.mod")) {
        @Override
        public String extract(SearchNode node) {
            return node.id().getNamespace();
        }
    },

    ID(Component.translatable("ami.row_field.id")) {
        @Override
        public String extract(SearchNode node) {
            return node.id().toString();
        }
    },

    TYPE(Component.translatable("ami.row_field.type")) {
        @Override
        public String extract(SearchNode node) {
            return node.type().displayName().getString();
        }
    },

    STORAGE_CAPACITY(Component.translatable("ami.row_field.storage")) {
        @Override
        public String extract(SearchNode node) {
            String cap = node.meta(SearchNodeKeys.ESM_CAPACITY, "");
            return cap.isEmpty() ? "" : Component.translatable("ami.row_field.storage_capacity", cap).getString();
        }
    },

    DPS(Component.translatable("ami.row_field.dps")) {
        @Override
        public String extract(SearchNode node) {
            String dps = node.meta(SearchNodeKeys.DPS, "");
            return dps.isEmpty() ? "" : Component.translatable("ami.row_field.dps_value", dps).getString();
        }
    };

    public final Component displayName;

    RowField(Component displayName) {
        this.displayName = displayName;
    }

    /**
     * Returns the display string for this field from the given node, or "" if not applicable.
     */
    public abstract String extract(SearchNode node);
}
