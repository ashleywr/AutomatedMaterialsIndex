package com.sanhiruzu.ami.client.results;

import com.sanhiruzu.ami.api.AmiAdvancementDocument;
import com.sanhiruzu.ami.client.AMITheme;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;

/**
 * Generated sprites for document-result row icons.
 */
public final class DocumentRowIconSprites {
    private static final int DOC_SIZE = 12;
    private static final int STATUS_SIZE = 10;

    private static final GeneratedGuiSprite GUIDE_HEADER = documentSprite("guide_header", DocumentRowIconSprites::paintGuide, () -> AMITheme.TEXT_HEADER);
    private static final GeneratedGuiSprite GUIDE_ACCENT = documentSprite("guide_accent", DocumentRowIconSprites::paintGuide, () -> AMITheme.ACCENT_BLUE);
    private static final GeneratedGuiSprite QUEST_HEADER = documentSprite("quest_header", DocumentRowIconSprites::paintQuest, () -> AMITheme.TEXT_HEADER);
    private static final GeneratedGuiSprite QUEST_ACCENT = documentSprite("quest_accent", DocumentRowIconSprites::paintQuest, () -> AMITheme.ACCENT_BLUE);
    private static final GeneratedGuiSprite ADVANCEMENT_HEADER = documentSprite("advancement_header", DocumentRowIconSprites::paintAdvancement, () -> AMITheme.TEXT_HEADER);
    private static final GeneratedGuiSprite ADVANCEMENT_ACCENT = documentSprite("advancement_accent", DocumentRowIconSprites::paintAdvancement, () -> AMITheme.ACCENT_BLUE);

    private static final GeneratedGuiSprite STATUS_COMPLETED = statusSprite("completed", () -> 0xFF55DD77, DocumentRowIconSprites::paintCompleted);
    private static final GeneratedGuiSprite STATUS_IN_PROGRESS = statusSprite("in_progress", () -> 0xFFFFC857, DocumentRowIconSprites::paintInProgress);
    private static final GeneratedGuiSprite STATUS_NOT_STARTED = statusSprite("not_started", () -> AMITheme.TEXT_SUBTLE, DocumentRowIconSprites::paintNotStarted);
    private static final GeneratedGuiSprite STATUS_UNKNOWN = statusSprite("unknown", () -> AMITheme.TEXT_SUBTLE, DocumentRowIconSprites::paintUnknown);

    private DocumentRowIconSprites() {
    }

    public static void guide(GuiGraphics g, int x, int y, boolean header) {
        (header ? GUIDE_HEADER : GUIDE_ACCENT).blit(g, x, y);
    }

    public static void quest(GuiGraphics g, int x, int y, boolean header) {
        (header ? QUEST_HEADER : QUEST_ACCENT).blit(g, x, y);
    }

    public static void advancement(GuiGraphics g, int x, int y, boolean header) {
        (header ? ADVANCEMENT_HEADER : ADVANCEMENT_ACCENT).blit(g, x, y);
    }

    public static void advancementStatus(GuiGraphics g, AmiAdvancementDocument.ProgressStatus status, int x, int y) {
        switch (status == null ? AmiAdvancementDocument.ProgressStatus.UNKNOWN : status) {
            case COMPLETED -> STATUS_COMPLETED.blit(g, x, y);
            case IN_PROGRESS -> STATUS_IN_PROGRESS.blit(g, x, y);
            case NOT_STARTED -> STATUS_NOT_STARTED.blit(g, x, y);
            case UNKNOWN -> STATUS_UNKNOWN.blit(g, x, y);
        }
    }

    private static GeneratedGuiSprite documentSprite(String name, DocumentPainter painter, java.util.function.IntSupplier colorSupplier) {
        return new GeneratedGuiSprite(
                ResourceLocation.fromNamespaceAndPath("ami", "generated/document_row_" + name),
                DOC_SIZE,
                DOC_SIZE,
                colorSupplier,
                canvas -> painter.paint(canvas, colorSupplier.getAsInt())
        );
    }

    private static GeneratedGuiSprite statusSprite(String name, java.util.function.IntSupplier colorSupplier, DocumentPainter painter) {
        return new GeneratedGuiSprite(
                ResourceLocation.fromNamespaceAndPath("ami", "generated/advancement_status_" + name),
                STATUS_SIZE,
                STATUS_SIZE,
                () -> signature(colorSupplier.getAsInt()),
                canvas -> {
                    int color = colorSupplier.getAsInt();
                    canvas.fill(0, 0, STATUS_SIZE, STATUS_SIZE, 0xDD111111);
                    canvas.fill(1, 1, STATUS_SIZE - 1, STATUS_SIZE - 1, AMITheme.DROPDOWN_BG);
                    painter.paint(canvas, color);
                }
        );
    }

    private static int signature(int color) {
        int result = color;
        result = 31 * result + AMITheme.DROPDOWN_BG;
        result = 31 * result + AMITheme.TEXT_SUBTLE;
        return result;
    }

    private static void paintGuide(GeneratedGuiSprite.Canvas canvas, int color) {
        int page = 0x66FFFFFF;
        canvas.fill(2, 0, 10, 1, color);
        canvas.fill(1, 1, 11, 11, color);
        canvas.fill(2, 11, 11, 12, color);
        canvas.fill(3, 2, 10, 10, page);
        canvas.fill(4, 4, 9, 5, color);
        canvas.fill(4, 7, 8, 8, color);
    }

    private static void paintQuest(GeneratedGuiSprite.Canvas canvas, int color) {
        int ink = 0x66000000 | (color & 0x00FFFFFF);
        canvas.fill(2, 0, 9, 1, color);
        canvas.fill(1, 1, 11, 12, color);
        canvas.fill(9, 1, 11, 3, 0x66FFFFFF);
        canvas.fill(3, 3, 5, 5, ink);
        canvas.fill(6, 3, 9, 4, ink);
        canvas.fill(3, 7, 5, 9, ink);
        canvas.fill(6, 7, 9, 8, ink);
    }

    private static void paintAdvancement(GeneratedGuiSprite.Canvas canvas, int color) {
        canvas.fill(3, 1, 9, 6, color);
        canvas.fill(1, 2, 3, 5, color);
        canvas.fill(9, 2, 11, 5, color);
        canvas.fill(5, 6, 7, 9, color);
        canvas.fill(3, 9, 9, 11, color);
        canvas.fill(4, 2, 8, 5, 0x44FFFFFF);
    }

    private static void paintCompleted(GeneratedGuiSprite.Canvas canvas, int color) {
        canvas.fill(1, 5, 3, 7, color);
        canvas.fill(3, 7, 5, 9, color);
        canvas.fill(5, 5, 7, 7, color);
        canvas.fill(7, 3, 9, 5, color);
    }

    private static void paintInProgress(GeneratedGuiSprite.Canvas canvas, int color) {
        canvas.fill(3, 1, 7, 2, color);
        canvas.fill(7, 2, 8, 4, color);
        canvas.fill(6, 4, 9, 6, color);
        canvas.fill(4, 6, 7, 9, color);
        canvas.fill(2, 7, 4, 8, color);
    }

    private static void paintNotStarted(GeneratedGuiSprite.Canvas canvas, int color) {
        canvas.fill(3, 1, 7, 2, color);
        canvas.fill(3, 8, 7, 9, color);
        canvas.fill(1, 3, 2, 7, color);
        canvas.fill(8, 3, 9, 7, color);
        canvas.fill(2, 2, 3, 3, color);
        canvas.fill(7, 2, 8, 3, color);
        canvas.fill(2, 7, 3, 8, color);
        canvas.fill(7, 7, 8, 8, color);
    }

    private static void paintUnknown(GeneratedGuiSprite.Canvas canvas, int color) {
        canvas.fill(3, 0, 7, 1, color);
        canvas.fill(7, 1, 8, 4, color);
        canvas.fill(5, 4, 7, 5, color);
        canvas.fill(4, 5, 6, 7, color);
        canvas.fill(4, 9, 6, 10, color);
    }

    private interface DocumentPainter {
        void paint(GeneratedGuiSprite.Canvas canvas, int color);
    }
}
