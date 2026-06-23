package com.sanhiruzu.ami.client.entitydetails;

import com.sanhiruzu.ami.index.NodeType;

import java.util.ArrayList;
import java.util.List;

public final class EntityDetailsViewProjector {
    static final int ROW_H = 18;
    static final int CARD_H = 30;
    static final int CARD_GAP = 3;
    static final int ICON_SIZE = 16;
    static final int PAD_X = 5;
    static final int STAT_TILE_H = 20;
    static final int STAT_TILE_GAP = 3;
    private static final int STAT_TILE_MIN_W = 110;

    private EntityDetailsViewProjector() {
    }

    public static Projection project(EntityDetailsReport report, TextMeasurer text, int x, int y, int width, int height,
                                     int groupHeaderHeight) {
        EntityDetailsReport safeReport = report == null
                ? new EntityDetailsReport(net.minecraft.network.chat.Component.empty(), List.of())
                : report;
        TextMeasurer safeText = text == null ? TextMeasurer.EMPTY : text;
        int safeWidth = Math.max(0, width);
        int safeHeight = Math.max(0, height);
        int drawY = y;
        int bottom = y + safeHeight;
        int textW = Math.max(0, safeWidth - PAD_X * 2);
        List<Row> rows = new ArrayList<>();
        List<HitTarget> targets = new ArrayList<>();

        if (safeReport.loading()) {
            if (drawY < bottom) rows.add(Row.text(RowKind.LOADING, x + PAD_X, drawY, textW, ROW_H, "ami.entity_details.loading"));
            drawY += ROW_H;
        }

        if (safeReport.groupOrder().isEmpty()) {
            if (safeReport.loading()) return new Projection(List.copyOf(rows), List.copyOf(targets));
            if (drawY < bottom) rows.add(Row.text(RowKind.EMPTY, x + PAD_X, drawY, textW, ROW_H, "ami.entity_details.empty"));
            drawY += ROW_H;
            projectDiagnostics(safeReport, safeText, rows, x, drawY, bottom, textW);
            return new Projection(List.copyOf(rows), List.copyOf(targets));
        }

        drawY = projectDiagnostics(safeReport, safeText, rows, x, drawY, bottom, textW);
        for (EntityDetailsSection section : safeReport.groupOrder()) {
            if (drawY >= bottom) break;
            rows.add(Row.groupHeader(x, drawY, safeWidth, groupHeaderHeight, textW, section));
            drawY += groupHeaderHeight;

            if (section == EntityDetailsSection.STATS) {
                StatGridLayout stats = statGridLayout(safeReport.rows(section), x, safeWidth, drawY);
                if (drawY + stats.bounds().height() > bottom) {
                    return new Projection(List.copyOf(rows), List.copyOf(targets));
                }
                rows.add(Row.statGrid(stats));
                drawY += stats.bounds().height() + CARD_GAP;
                continue;
            }

            for (EntityDetailsRow row : safeReport.rows(section)) {
                if (drawY >= bottom) break;
                if (drawY + CARD_H > bottom) {
                    return new Projection(List.copyOf(rows), List.copyOf(targets));
                }
                CardLayout card = cardLayout(row, x, safeWidth, drawY);
                rows.add(Row.card(card));
                addHitTarget(row, card, targets);
                drawY += CARD_H + CARD_GAP;
            }
        }
        return new Projection(List.copyOf(rows), List.copyOf(targets));
    }

    private static StatGridLayout statGridLayout(List<EntityDetailsRow> rows, int x, int width, int y) {
        List<EntityDetailsRow> safeRows = rows == null ? List.of() : rows;
        int gridX = x + 1;
        int gridW = Math.max(0, width - 2);
        int columns = statColumnCount(gridW, safeRows.size());
        int rowCount = safeRows.isEmpty() ? 0 : (safeRows.size() + columns - 1) / columns;
        int totalGapW = STAT_TILE_GAP * Math.max(0, columns - 1);
        int tileW = columns <= 0 ? gridW : Math.max(0, (gridW - totalGapW) / columns);
        int gridH = rowCount == 0 ? 0 : rowCount * STAT_TILE_H + Math.max(0, rowCount - 1) * STAT_TILE_GAP;
        List<StatTileLayout> tiles = new ArrayList<>();
        for (int i = 0; i < safeRows.size(); i++) {
            int column = i % columns;
            int row = i / columns;
            int tileX = gridX + column * (tileW + STAT_TILE_GAP);
            int tileY = y + row * (STAT_TILE_H + STAT_TILE_GAP);
            int effectiveW = column == columns - 1 ? gridX + gridW - tileX : tileW;
            tiles.add(new StatTileLayout(safeRows.get(i), new Rect(tileX, tileY, Math.max(0, effectiveW), STAT_TILE_H)));
        }
        return new StatGridLayout(new Rect(gridX, y, gridW, gridH), List.copyOf(tiles));
    }

    private static int statColumnCount(int width, int itemCount) {
        if (itemCount <= 0) return 1;
        int columns = Math.max(1, (width + STAT_TILE_GAP) / (STAT_TILE_MIN_W + STAT_TILE_GAP));
        return Math.max(1, Math.min(Math.min(3, itemCount), columns));
    }

    private static int projectDiagnostics(EntityDetailsReport report, TextMeasurer text, List<Row> rows,
                                          int x, int drawY, int bottom, int textW) {
        for (var diagnostic : report.diagnostics()) {
            for (String line : wrapLines(text, diagnostic.getString(), textW)) {
                if (drawY >= bottom) return drawY;
                rows.add(Row.text(RowKind.DIAGNOSTIC, x + PAD_X, drawY, textW, ROW_H, line));
                drawY += ROW_H;
            }
        }
        return drawY;
    }

    private static CardLayout cardLayout(EntityDetailsRow row, int x, int width, int cardY) {
        int cardX = x + 1;
        int cardW = Math.max(0, width - 2);
        int iconX = cardX + 6;
        int iconY = cardY + 7;
        int textX = row.link() == null || row.link().node() == null ? cardX + PAD_X : iconX + ICON_SIZE + 6;
        int textRight = cardX + cardW - 5;
        return new CardLayout(row, new Rect(cardX, cardY, cardW, CARD_H), iconX, iconY, textX, textRight);
    }

    private static void addHitTarget(EntityDetailsRow row, CardLayout card, List<HitTarget> targets) {
        if (row.link() == null || row.link().node() == null) return;
        TargetKind kind = switch (row.section()) {
            case SPAWNS -> TargetKind.BIOME;
            case DROPS -> TargetKind.ITEM_DROP;
            case EXTERNAL_INFO -> TargetKind.EXTERNAL_INFO;
            case STATS -> null;
        };
        if (kind != null) targets.add(new HitTarget(row.link(), row, kind, card.bounds()));
    }

    static List<String> wrapLines(TextMeasurer text, String value, int maxW) {
        if (value == null || value.isBlank()) return List.of("");
        if (maxW <= 0) return List.of("");
        List<String> lines = new ArrayList<>();
        String[] words = value.trim().split("\\s+");
        String current = "";
        for (String word : words) {
            if (word.isEmpty()) continue;
            if (current.isEmpty()) {
                if (text.width(word) <= maxW) {
                    current = word;
                } else {
                    splitLongWord(text, word, maxW, lines);
                }
                continue;
            }

            String candidate = current + " " + word;
            if (text.width(candidate) <= maxW) {
                current = candidate;
            } else {
                lines.add(current);
                if (text.width(word) <= maxW) {
                    current = word;
                } else {
                    current = "";
                    splitLongWord(text, word, maxW, lines);
                }
            }
        }
        if (!current.isEmpty()) lines.add(current);
        return lines.isEmpty() ? List.of("") : List.copyOf(lines);
    }

    private static void splitLongWord(TextMeasurer text, String word, int maxW, List<String> lines) {
        String remaining = word;
        while (!remaining.isEmpty()) {
            String part = text.plainSubstrByWidth(remaining, maxW);
            if (part.isEmpty()) {
                lines.add(remaining.substring(0, 1));
                remaining = remaining.substring(1);
            } else {
                lines.add(part);
                remaining = remaining.substring(part.length());
            }
        }
    }

    public interface TextMeasurer {
        TextMeasurer EMPTY = new TextMeasurer() {
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

        int width(String text);

        String plainSubstrByWidth(String text, int width);
    }

    public record Projection(List<Row> rows, List<HitTarget> hitTargets) {
        public Projection {
            rows = rows == null ? List.of() : List.copyOf(rows);
            hitTargets = hitTargets == null ? List.of() : List.copyOf(hitTargets);
        }

        public HitTarget hitTargetAt(int mouseX, int mouseY) {
            for (HitTarget target : hitTargets) {
                if (target.bounds().contains(mouseX, mouseY)) return target;
            }
            return null;
        }
    }

    public enum RowKind {
        LOADING,
        EMPTY,
        DIAGNOSTIC,
        GROUP_HEADER,
        STAT_GRID,
        CARD
    }

    public enum TargetKind {
        BIOME,
        ITEM_DROP,
        EXTERNAL_INFO
    }

    public record Row(RowKind kind, Rect bounds, String text, int textWidth, EntityDetailsSection section,
                      CardLayout card, StatGridLayout statGrid) {
        static Row text(RowKind kind, int x, int y, int width, int height, String text) {
            return new Row(kind, new Rect(x, y, width, height), text, width, null, null, null);
        }

        static Row groupHeader(int x, int y, int width, int height, int textWidth, EntityDetailsSection section) {
            return new Row(RowKind.GROUP_HEADER, new Rect(x, y, width, height), null, textWidth, section, null, null);
        }

        static Row statGrid(StatGridLayout grid) {
            return new Row(RowKind.STAT_GRID, grid.bounds(), null, 0, EntityDetailsSection.STATS, null, grid);
        }

        static Row card(CardLayout card) {
            return new Row(RowKind.CARD, card.bounds(), null, 0, null, card, null);
        }
    }

    public record CardLayout(EntityDetailsRow row, Rect bounds, int iconX, int iconY, int textX, int textRight) {
    }

    public record StatGridLayout(Rect bounds, List<StatTileLayout> tiles) {
        public StatGridLayout {
            tiles = tiles == null ? List.of() : List.copyOf(tiles);
        }
    }

    public record StatTileLayout(EntityDetailsRow row, Rect bounds) {
    }

    public record HitTarget(EntityDetailsLink link, EntityDetailsRow row, TargetKind kind, Rect bounds) {
    }

    public record Rect(int x, int y, int width, int height) {
        boolean contains(int pointX, int pointY) {
            return pointX >= x && pointX < x + width && pointY >= y && pointY < y + height;
        }
    }
}
