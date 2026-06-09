package com.sanhiruzu.ami.index.providers;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class RecipeViewerItemAuditTest {
    @Test
    void compareDatasetSeparatesBaseCoverageFromExactCoverage() {
        List<RecipeViewerItemAudit.AmiItemSnapshot> amiItems = List.of(
                new RecipeViewerItemAudit.AmiItemSnapshot(
                        "minecraft:stone",
                        "minecraft:stone",
                        "Stone",
                        "survival",
                        "",
                        "",
                        "aaa111",
                        "minecraft:stone|aaa111"
                ),
                new RecipeViewerItemAudit.AmiItemSnapshot(
                        "minecraft:potion/variant/awkward_bbb222",
                        "minecraft:potion",
                        "Awkward Potion",
                        "survival",
                        "creative_tab",
                        "minecraft:potion",
                        "bbb222",
                        "minecraft:potion|bbb222"
                )
        );

        RecipeViewerItemAudit.CoverageIndex coverage = RecipeViewerItemAudit.buildCoverageIndex(amiItems);
        List<RecipeViewerItemAudit.ViewerItemSnapshot> viewerItems = List.of(
                new RecipeViewerItemAudit.ViewerItemSnapshot(
                        "emi",
                        "visible",
                        "minecraft:stone",
                        "Stone",
                        "aaa111",
                        "minecraft:stone|aaa111"
                ),
                new RecipeViewerItemAudit.ViewerItemSnapshot(
                        "emi",
                        "visible",
                        "minecraft:potion",
                        "Awkward Potion",
                        "bbb222",
                        "minecraft:potion|bbb222"
                ),
                new RecipeViewerItemAudit.ViewerItemSnapshot(
                        "emi",
                        "visible",
                        "minecraft:potion",
                        "Long Swiftness Potion",
                        "ccc333",
                        "minecraft:potion|ccc333"
                ),
                new RecipeViewerItemAudit.ViewerItemSnapshot(
                        "emi",
                        "visible",
                        "minecraft:diamond",
                        "Diamond",
                        "ddd444",
                        "minecraft:diamond|ddd444"
                )
        );

        RecipeViewerItemAudit.DatasetReport report = RecipeViewerItemAudit.compareDataset(
                "emi",
                "visible",
                viewerItems,
                coverage,
                "recipe_viewer_items_emi_visible.jsonl"
        );

        assertEquals(3, report.uniqueBaseItems());
        assertEquals(1, report.missingBaseItems());
        assertEquals(List.of("minecraft:diamond"), report.missingBaseItemIds());
        assertEquals(2, report.missingExactStacks());
        assertEquals(
                List.of("minecraft:diamond", "minecraft:potion"),
                report.missingExactStackEntries().stream().map(RecipeViewerItemAudit.MissingExactStack::itemId).toList()
        );
    }
}
