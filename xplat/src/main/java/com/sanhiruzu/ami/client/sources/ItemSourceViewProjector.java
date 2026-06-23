package com.sanhiruzu.ami.client.sources;

import java.util.ArrayList;
import java.util.List;

public final class ItemSourceViewProjector {
    static final int ROW_H = 18;
    static final int CARD_H = 42;
    static final int CARD_H_BIOMES = 52;
    static final int CARD_GAP = 3;
    static final int ICON_BOX = 24;
    static final int ROUTE_ICON_SIZE = 12;
    static final int BIOME_CHIP_ICON_SIZE = 8;
    static final int CHIP_H = 12;
    static final int BIOME_CHIP_MAX_W = 126;
    static final int CHIP_GAP = 3;
    static final int PAD_X = 5;

    private ItemSourceViewProjector() {
    }

    public static Projection project(ItemSourceReport report, TextMeasurer text, int x, int y, int width, int height,
                                     int groupHeaderHeight) {
        ItemSourceReport safeReport = report == null
                ? new ItemSourceReport(net.minecraft.network.chat.Component.empty(), List.of())
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
            if (drawY < bottom) {
                rows.add(Row.text(RowKind.LOADING, x + PAD_X, drawY, textW, ROW_H, "ami.sources.loading"));
            }
            drawY += ROW_H;
        }

        if (safeReport.groupOrder().isEmpty()) {
            if (safeReport.loading()) {
                return new Projection(List.copyOf(rows), List.copyOf(targets));
            }
            if (drawY < bottom) {
                rows.add(Row.text(RowKind.EMPTY, x + PAD_X, drawY, textW, ROW_H, "ami.sources.empty"));
            }
            drawY += ROW_H;
            drawY = projectDiagnostics(safeReport, safeText, rows, x, drawY, bottom, textW);
            return new Projection(List.copyOf(rows), List.copyOf(targets));
        }

        drawY = projectDiagnostics(safeReport, safeText, rows, x, drawY, bottom, textW);

        for (ItemSourceType type : safeReport.groupOrder()) {
            if (drawY >= bottom) break;
            rows.add(Row.groupHeader(x, drawY, safeWidth, groupHeaderHeight, textW, type));
            drawY += groupHeaderHeight;

            for (ItemSourceRow row : safeReport.rows(type)) {
                if (drawY >= bottom) break;
                int cardH = rowHeight(row);
                if (drawY + cardH > bottom) {
                    return new Projection(List.copyOf(rows), List.copyOf(targets));
                }
                CardLayout card = cardLayout(row, x, safeWidth, drawY, cardH, safeText);
                rows.add(Row.card(card));
                addHitTargets(row, card, targets);
                drawY += cardH + CARD_GAP;
            }
        }

        return new Projection(List.copyOf(rows), List.copyOf(targets));
    }

    private static int projectDiagnostics(ItemSourceReport report, TextMeasurer text, List<Row> rows,
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

    private static CardLayout cardLayout(ItemSourceRow row, int x, int width, int cardY, int cardH, TextMeasurer text) {
        int cardX = x + 1;
        int cardW = Math.max(0, width - 2);
        int iconX = cardX + 5;
        int iconY = cardY + 7;
        int textX = iconX + ICON_BOX + 5;
        int textRight = cardX + cardW - 5;
        int lineY = cardY + 18;
        Rect outputBounds = row.routeOutputLink() == null || row.routeOutputLink().node() == null
                ? null
                : new Rect(textX, cardY + 16, Math.max(0, textRight - textX), 18);
        List<ChipLayout> chips = biomeChips(row, text, textX, cardY + 32, textRight);
        return new CardLayout(row, new Rect(cardX, cardY, cardW, cardH), iconX, iconY, textX, textRight, lineY,
                outputBounds, chips);
    }

    private static List<ChipLayout> biomeChips(ItemSourceRow row, TextMeasurer text, int startX, int chipY, int right) {
        List<ChipLayout> chips = new ArrayList<>();
        int chipX = startX;
        for (ItemSourceLink link : row.biomeLinks()) {
            if (link == null || link.node() == null) continue;
            int chipW = chipWidth(text, link.label());
            if (chipX + chipW > right) break;
            chips.add(new ChipLayout(link, new Rect(chipX, chipY, chipW, CHIP_H)));
            chipX += chipW + CHIP_GAP;
        }
        return List.copyOf(chips);
    }

    private static void addHitTargets(ItemSourceRow row, CardLayout card, List<HitTarget> targets) {
        for (ChipLayout chip : card.biomeChips()) {
            targets.add(new HitTarget(chip.link(), row, TargetKind.BIOME, chip.bounds()));
        }
        ItemSourceLink output = row.routeOutputLink();
        if (output != null && output.node() != null && card.outputBounds() != null) {
            targets.add(new HitTarget(output, row, TargetKind.OUTPUT, card.outputBounds()));
        }
        if (row.primaryLink() != null && row.primaryLink().node() != null) {
            targets.add(new HitTarget(row.primaryLink(), row, TargetKind.PRIMARY, card.bounds()));
        }
    }

    static int rowHeight(ItemSourceRow row) {
        return row != null && !row.biomeLinks().isEmpty() ? CARD_H_BIOMES : CARD_H;
    }

    static int chipWidth(TextMeasurer text, String label) {
        return Math.max(42, Math.min(BIOME_CHIP_MAX_W,
                text.width(label == null ? "" : label) + BIOME_CHIP_ICON_SIZE + 14));
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
                if (target.bounds().contains(mouseX, mouseY)) {
                    return target;
                }
            }
            return null;
        }
    }

    public enum RowKind {
        LOADING,
        EMPTY,
        DIAGNOSTIC,
        GROUP_HEADER,
        CARD
    }

    public enum TargetKind {
        PRIMARY,
        OUTPUT,
        BIOME
    }

    public record Row(RowKind kind, Rect bounds, String text, int textWidth, ItemSourceType type, CardLayout card) {
        static Row text(RowKind kind, int x, int y, int width, int height, String text) {
            return new Row(kind, new Rect(x, y, width, height), text, width, null, null);
        }

        static Row groupHeader(int x, int y, int width, int height, int textWidth, ItemSourceType type) {
            return new Row(RowKind.GROUP_HEADER, new Rect(x, y, width, height), null, textWidth, type, null);
        }

        static Row card(CardLayout card) {
            return new Row(RowKind.CARD, card.bounds(), null, 0, null, card);
        }
    }

    public record CardLayout(
            ItemSourceRow source,
            Rect bounds,
            int iconX,
            int iconY,
            int textX,
            int textRight,
            int lineY,
            Rect outputBounds,
            List<ChipLayout> biomeChips
    ) {
        public CardLayout {
            biomeChips = biomeChips == null ? List.of() : List.copyOf(biomeChips);
        }
    }

    public record ChipLayout(ItemSourceLink link, Rect bounds) {
    }

    public record HitTarget(ItemSourceLink link, ItemSourceRow row, TargetKind kind, Rect bounds) {
    }

    public record Rect(int x, int y, int width, int height) {
        boolean contains(int pointX, int pointY) {
            return pointX >= x && pointX < x + width && pointY >= y && pointY < y + height;
        }
    }
}
