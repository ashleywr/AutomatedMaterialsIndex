package com.sanhiruzu.ami.client.entitydetails;

import com.sanhiruzu.ami.index.NodeType;
import com.sanhiruzu.ami.index.SearchNode;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class EntityDetailsListViewTest {
    @Test
    void projectorBuildsRowsAndHitTargetsFromReportData() {
        SearchNode savanna = node("savanna", NodeType.BIOME, "Savanna");
        SearchNode leather = node("leather", NodeType.ITEM, "Leather");
        EntityDetailsReport report = new EntityDetailsReport(
                Component.literal("Mob: Cow"),
                List.of(
                        EntityDetailsRow.stat("5 hearts", EntityDetailsStatKind.HEALTH),
                        new EntityDetailsRow(EntityDetailsSection.SPAWNS, "Savanna", new EntityDetailsLink("Savanna", savanna)),
                        new EntityDetailsRow(EntityDetailsSection.DROPS, "Leather", new EntityDetailsLink("Leather", leather))
                )
        );

        EntityDetailsViewProjector.Projection projection = EntityDetailsViewProjector.project(
                report, onePixelText(), 0, 0, 240, 160, 16);

        assertEquals(List.of(
                        EntityDetailsViewProjector.RowKind.GROUP_HEADER,
                        EntityDetailsViewProjector.RowKind.STAT_GRID,
                        EntityDetailsViewProjector.RowKind.GROUP_HEADER,
                        EntityDetailsViewProjector.RowKind.CARD,
                        EntityDetailsViewProjector.RowKind.GROUP_HEADER,
                        EntityDetailsViewProjector.RowKind.CARD
                ),
                projection.rows().stream().map(EntityDetailsViewProjector.Row::kind).toList());
        assertEquals(EntityDetailsViewProjector.TargetKind.BIOME, projection.hitTargetAt(8, 62).kind());
        assertEquals(EntityDetailsViewProjector.TargetKind.ITEM_DROP, projection.hitTargetAt(8, 111).kind());
    }

    @Test
    void statsProjectAsCompactOverviewTilesWithoutHitTargets() {
        EntityDetailsReport report = new EntityDetailsReport(
                Component.literal("Mob: Blaze"),
                List.of(
                        EntityDetailsRow.stat("10 hearts", EntityDetailsStatKind.HEALTH),
                        EntityDetailsRow.stat("6 damage", EntityDetailsStatKind.DAMAGE),
                        EntityDetailsRow.stat("Fire immune", EntityDetailsStatKind.EFFECT),
                        EntityDetailsRow.stat("Mountable", EntityDetailsStatKind.TRAIT)
                )
        );

        EntityDetailsViewProjector.Projection projection = EntityDetailsViewProjector.project(
                report, onePixelText(), 0, 0, 240, 120, 16);

        EntityDetailsViewProjector.Row statGrid = projection.rows().get(1);
        assertEquals(EntityDetailsViewProjector.RowKind.STAT_GRID, statGrid.kind());
        assertEquals(43, statGrid.bounds().height());
        assertEquals(List.of("10 hearts", "6 damage", "Fire immune", "Mountable"),
                statGrid.statGrid().tiles().stream().map(tile -> tile.row().text()).toList());
        assertEquals(List.of(
                        EntityDetailsStatKind.HEALTH,
                        EntityDetailsStatKind.DAMAGE,
                        EntityDetailsStatKind.EFFECT,
                        EntityDetailsStatKind.TRAIT
                ),
                statGrid.statGrid().tiles().stream().map(tile -> tile.row().statKind()).toList());
        assertTrue(projection.hitTargets().isEmpty());
    }

    @Test
    void biomeAndDropRowsDispatchDistinctActions() {
        SearchNode savanna = node("savanna", NodeType.BIOME, "Savanna");
        SearchNode leather = node("leather", NodeType.ITEM, "Leather");
        EntityDetailsReport report = new EntityDetailsReport(
                Component.literal("Mob: Cow"),
                List.of(
                        new EntityDetailsRow(EntityDetailsSection.SPAWNS, "Savanna", new EntityDetailsLink("Savanna", savanna)),
                        new EntityDetailsRow(EntityDetailsSection.DROPS, "Leather", new EntityDetailsLink("Leather", leather))
                )
        );
        EntityDetailsListView view = new EntityDetailsListView(0, 0, 240, 120);
        AtomicReference<SearchNode> opened = new AtomicReference<>();
        AtomicReference<EntityDetailsListView.EntityAction> action = new AtomicReference<>();
        view.setReport(report);
        view.setActionHandler((node, entityAction, mouseX, mouseY) -> {
            opened.set(node);
            action.set(entityAction);
            return true;
        });

        assertTrue(view.mouseClicked(8, 25, 0));
        assertEquals(savanna, opened.get());
        assertEquals(EntityDetailsListView.EntityAction.LOCATE_BIOME, action.get());

        assertTrue(view.mouseClicked(8, 70, 1));
        assertEquals(leather, opened.get());
        assertEquals(EntityDetailsListView.EntityAction.OPEN_CONTEXT, action.get());
    }

    @Test
    void dropRowsOpenRecipesOnLeftClickAndContextOnRightClick() {
        SearchNode leather = node("leather", NodeType.ITEM, "Leather");
        EntityDetailsReport report = new EntityDetailsReport(
                Component.literal("Mob: Cow"),
                List.of(new EntityDetailsRow(EntityDetailsSection.DROPS, "Leather", new EntityDetailsLink("Leather", leather)))
        );
        EntityDetailsListView view = new EntityDetailsListView(0, 0, 240, 120);
        AtomicReference<EntityDetailsListView.EntityAction> action = new AtomicReference<>();
        view.setReport(report);
        view.setActionHandler((node, entityAction, mouseX, mouseY) -> {
            action.set(entityAction);
            return true;
        });

        assertTrue(view.mouseClicked(8, 25, 0));
        assertEquals(EntityDetailsListView.EntityAction.OPEN_ITEM_RECIPES, action.get());

        assertTrue(view.mouseClicked(8, 25, 1));
        assertEquals(EntityDetailsListView.EntityAction.OPEN_CONTEXT, action.get());
    }

    @Test
    void statRowsDoNotCreateHitTargetsOrDispatchActions() {
        EntityDetailsReport report = new EntityDetailsReport(
                Component.literal("Mob: Cow"),
                List.of(EntityDetailsRow.stat("5 hearts", EntityDetailsStatKind.HEALTH))
        );
        EntityDetailsViewProjector.Projection projection = EntityDetailsViewProjector.project(
                report, onePixelText(), 0, 0, 240, 120, 16);
        EntityDetailsListView view = new EntityDetailsListView(0, 0, 240, 120);
        AtomicBoolean called = new AtomicBoolean();
        view.setReport(report);
        view.setActionHandler((node, action, mouseX, mouseY) -> {
            called.set(true);
            return true;
        });

        assertNull(projection.hitTargetAt(8, 25));
        assertTrue(projection.hitTargets().isEmpty());
        assertFalse(view.mouseClicked(8, 25, 0));
        assertFalse(called.get());
    }

    @Test
    void clippedCardsDoNotCreateHitTargets() {
        SearchNode savanna = node("savanna", NodeType.BIOME, "Savanna");
        EntityDetailsReport report = new EntityDetailsReport(
                Component.literal("Mob: Cow"),
                List.of(new EntityDetailsRow(EntityDetailsSection.SPAWNS, "Savanna", new EntityDetailsLink("Savanna", savanna)))
        );

        EntityDetailsViewProjector.Projection projection = EntityDetailsViewProjector.project(
                report, onePixelText(), 0, 0, 240, 40, 16);

        assertEquals(List.of(EntityDetailsViewProjector.RowKind.GROUP_HEADER),
                projection.rows().stream().map(EntityDetailsViewProjector.Row::kind).toList());
        assertTrue(projection.hitTargets().isEmpty());
    }

    private static SearchNode node(String path, NodeType type, String name) {
        return new SearchNode(new ResourceLocation("minecraft:" + path), type, name, 0, 0, Map.of());
    }

    private static EntityDetailsViewProjector.TextMeasurer onePixelText() {
        return new EntityDetailsViewProjector.TextMeasurer() {
            @Override
            public int width(String text) {
                return text == null ? 0 : text.length();
            }

            @Override
            public String plainSubstrByWidth(String text, int width) {
                if (text == null || width <= 0) return "";
                return text.substring(0, Math.min(text.length(), width));
            }
        };
    }
}
