package com.sanhiruzu.ami.compat;

import net.minecraft.resources.Identifier;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FtbQuestsRuntimeCompatTest {
    @Test
    void parsesSmartFilterItemExpressionsWithoutIndexingTheFilterItem() {
        Identifier fallback = new Identifier("ftbfiltersystem", "smart_filter");

        FtbQuestsRuntimeCompat.FilterItems items = FtbQuestsRuntimeCompat.parseFtbFilterString(
                "or(item(minecraft:torch)item(minecraft:soul_torch)item(minecraft:torch))",
                fallback
        );

        assertEquals(List.of(
                new Identifier("minecraft", "torch"),
                new Identifier("minecraft", "soul_torch")
        ), items.itemIds());
        assertFalse(items.highCardinality());
        assertTrue(items.tags().isEmpty());
    }

    @Test
    void marksTagAndCappedSmartFiltersAsHighCardinality() {
        Identifier fallback = new Identifier("ftbfiltersystem", "smart_filter");
        StringBuilder filter = new StringBuilder("or(ftbfiltersystem:item_tag(minecraft:logs)");
        for (int i = 0; i < 40; i++) {
            filter.append("item(example:item_").append(i).append(")");
        }
        filter.append(")");

        FtbQuestsRuntimeCompat.FilterItems items = FtbQuestsRuntimeCompat.parseFtbFilterString(
                filter.toString(),
                fallback
        );

        assertEquals(32, items.itemIds().size());
        assertTrue(items.highCardinality());
        assertTrue(items.tags().contains("ftb_filter_tag:minecraft:logs"));
        assertTrue(items.tags().contains("ftb_filter_capped:40"));
    }
}
