package com.sanhiruzu.ami.client.entitydetails;

import com.sanhiruzu.ami.client.AMITheme;
import com.sanhiruzu.ami.client.entitydetails.EntityDetailsViewProjector.HitTarget;
import com.sanhiruzu.ami.client.entitydetails.EntityDetailsViewProjector.TargetKind;
import com.sanhiruzu.ami.client.icon.RendererRegistry;
import com.sanhiruzu.ami.index.SearchNode;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;

import java.util.List;

public final class EntityDetailsListView {
    private static final int STAT_ICON_BOX = 16;

    private int x;
    private int y;
    private int width;
    private int height;
    private EntityDetailsReport report = new EntityDetailsReport(Component.empty(), List.of());
    private EntityActionHandler actionHandler;

    public enum EntityAction {
        LOCATE_BIOME,
        OPEN_CONTEXT,
        OPEN_ITEM_RECIPES,
        OPEN_EXTERNAL_INFO
    }

    @FunctionalInterface
    public interface EntityActionHandler {
        boolean handle(SearchNode node, EntityAction action, int mouseX, int mouseY);
    }

    public EntityDetailsListView(int x, int y, int width, int height) {
        updateLayout(x, y, width, height);
    }

    public void updateLayout(int x, int y, int width, int height) {
        this.x = x;
        this.y = y;
        this.width = Math.max(0, width);
        this.height = Math.max(0, height);
    }

    public void setReport(EntityDetailsReport report) {
        this.report = report == null ? new EntityDetailsReport(Component.empty(), List.of()) : report;
    }

    public void setActionHandler(EntityActionHandler actionHandler) {
        this.actionHandler = actionHandler;
    }

    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if ((button != 0 && button != 1) || mouseX < x || mouseX >= x + width || mouseY < y || mouseY >= y + height) {
            return false;
        }
        HitTarget target = projection(Minecraft.getInstance().font).hitTargetAt((int) mouseX, (int) mouseY);
        if (target == null || target.link() == null || target.link().node() == null || actionHandler == null) {
            return false;
        }
        EntityAction action = actionFor(target.kind(), button);
        return action != null && actionHandler.handle(target.link().node(), action, (int) mouseX, (int) mouseY);
    }

    public void render(GuiGraphics g, int mouseX, int mouseY) {
        if (g == null || width <= 0 || height <= 0) return;
        g.enableScissor(x, y, x + width, y + height);
        try {
            renderClipped(g, mouseX, mouseY);
        } finally {
            g.disableScissor();
        }
    }

    private void renderClipped(GuiGraphics g, int mouseX, int mouseY) {
        var font = Minecraft.getInstance().font;
        EntityDetailsViewProjector.Projection projection = projection(font);
        for (EntityDetailsViewProjector.Row row : projection.rows()) {
            switch (row.kind()) {
                case LOADING, EMPTY, DIAGNOSTIC -> renderTextRow(g, font, row);
                case GROUP_HEADER -> renderGroupHeader(g, font, row);
                case STAT_GRID -> renderStatGrid(g, font, row.statGrid());
                case CARD -> renderCard(g, font, row.card(), mouseX, mouseY);
            }
        }
    }

    private EntityDetailsViewProjector.Projection projection(net.minecraft.client.gui.Font font) {
        return EntityDetailsViewProjector.project(report, new EntityDetailsViewProjector.TextMeasurer() {
            @Override
            public int width(String text) {
                return font.width(text == null ? "" : text);
            }

            @Override
            public String plainSubstrByWidth(String text, int width) {
                return font.plainSubstrByWidth(text == null ? "" : text, width);
            }
        }, x, y, width, height, groupHeaderHeight());
    }

    private void renderTextRow(GuiGraphics g, net.minecraft.client.gui.Font font, EntityDetailsViewProjector.Row row) {
        String text = switch (row.kind()) {
            case LOADING, EMPTY -> Component.translatable(row.text()).getString();
            default -> row.text();
        };
        g.drawString(font, truncate(font, text, row.textWidth()),
                row.bounds().x(), row.bounds().y() + 3, AMITheme.TEXT_SUBTLE, false);
    }

    private void renderGroupHeader(GuiGraphics g, net.minecraft.client.gui.Font font, EntityDetailsViewProjector.Row row) {
        EntityDetailsViewProjector.Rect bounds = row.bounds();
        int rowRight = bounds.x() + bounds.width();
        g.fill(bounds.x(), bounds.y(), rowRight, bounds.y() + bounds.height(), AMITheme.GRID_GROUP_ROOT_BG);
        g.fill(bounds.x(), bounds.y(), bounds.x() + 2, bounds.y() + bounds.height(), AMITheme.ACCENT_BLUE);
        g.fill(bounds.x() + 2, bounds.y() + bounds.height() - 1, rowRight - 2, bounds.y() + bounds.height(), AMITheme.SECTION_SEP);
        g.drawString(font, truncate(font, row.section().label(), Math.max(0, row.textWidth() - 27)),
                bounds.x() + EntityDetailsViewProjector.PAD_X + 27,
                bounds.y() + Math.max(0, (bounds.height() - font.lineHeight) / 2),
                AMITheme.TEXT_PRIMARY, false);
    }

    private void renderStatGrid(GuiGraphics g, net.minecraft.client.gui.Font font,
                                EntityDetailsViewProjector.StatGridLayout grid) {
        if (grid == null) return;
        for (EntityDetailsViewProjector.StatTileLayout tile : grid.tiles()) {
            EntityDetailsViewProjector.Rect bounds = tile.bounds();
            AMITheme.fillInsetRect(g, bounds.x(), bounds.y(), bounds.width(), bounds.height(), AMITheme.SLOT_BG, false);
            g.fill(bounds.x(), bounds.y(), bounds.x() + 2, bounds.y() + bounds.height(), AMITheme.ACCENT_BLUE);
            renderStatIcon(g, tile.row() == null ? EntityDetailsStatKind.NONE : tile.row().statKind(),
                    bounds.x() + 5, bounds.y() + 2);
            String text = tile.row() == null ? "" : tile.row().text();
            int textX = bounds.x() + 5 + STAT_ICON_BOX + 5;
            g.drawString(font, truncate(font, text, Math.max(0, bounds.x() + bounds.width() - textX - 5)),
                    textX,
                    bounds.y() + Math.max(0, (bounds.height() - font.lineHeight) / 2),
                    AMITheme.TEXT_PRIMARY, false);
        }
    }

    private void renderStatIcon(GuiGraphics g, EntityDetailsStatKind kind, int x, int y) {
        switch (kind) {
            case HEALTH -> renderHeartIcon(g, x, y);
            case DAMAGE -> renderItemIcon(g, Items.IRON_SWORD, x, y);
            case EFFECT -> renderItemIcon(g, Items.MAGMA_CREAM, x, y);
            case TRAIT -> renderItemIcon(g, Items.NAME_TAG, x, y);
            case NONE -> {
            }
        }
    }

    private void renderHeartIcon(GuiGraphics g, int x, int y) {
        var font = Minecraft.getInstance().font;
        g.drawString(font, "\u2665", x + 4, y + 4, AMITheme.HEART_LABEL_COLOR, false);
    }

    private void renderItemIcon(GuiGraphics g, Item item, int x, int y) {
        if (item != null) {
            g.renderItem(item.getDefaultInstance(), x, y);
        }
    }

    private void renderCard(GuiGraphics g, net.minecraft.client.gui.Font font, EntityDetailsViewProjector.CardLayout card,
                            int mouseX, int mouseY) {
        EntityDetailsRow row = card.row();
        EntityDetailsViewProjector.Rect bounds = card.bounds();
        boolean hovered = bounds.contains(mouseX, mouseY) && row.link() != null && row.link().node() != null;
        int fill = hovered ? AMITheme.DROPDOWN_BG_ACTIVE : AMITheme.DROPDOWN_BG;
        AMITheme.fillInsetRect(g, bounds.x(), bounds.y(), bounds.width(), bounds.height(), fill, false);

        SearchNode node = row.link() == null ? null : row.link().node();
        if (node != null) {
            RendererRegistry.get(node.type()).render(g, node, card.iconX(), card.iconY(),
                    EntityDetailsViewProjector.ICON_SIZE, hovered);
        }

        int maxTextW = Math.max(0, card.textRight() - card.textX());
        int textY = bounds.y() + (row.detail().isBlank() ? 10 : 5);
        g.drawString(font, truncate(font, row.text(), maxTextW), card.textX(), textY, AMITheme.TEXT_PRIMARY, false);
        if (!row.detail().isBlank()) {
            g.drawString(font, truncate(font, row.detail(), maxTextW),
                    card.textX(), bounds.y() + 17, AMITheme.TEXT_SUBTLE, false);
        }
    }

    private static int groupHeaderHeight() {
        return AMITheme.ROW_HEIGHT;
    }

    private static EntityAction actionFor(TargetKind kind, int button) {
        if (kind == TargetKind.BIOME) {
            return button == 1 ? EntityAction.OPEN_CONTEXT : EntityAction.LOCATE_BIOME;
        }
        if (kind == TargetKind.ITEM_DROP) {
            return button == 1 ? EntityAction.OPEN_CONTEXT : EntityAction.OPEN_ITEM_RECIPES;
        }
        if (kind == TargetKind.EXTERNAL_INFO) {
            return button == 0 ? EntityAction.OPEN_EXTERNAL_INFO : null;
        }
        return null;
    }

    private static String truncate(net.minecraft.client.gui.Font font, String text, int maxW) {
        if (text == null || text.isEmpty() || maxW <= 0) return "";
        if (font.width(text) <= maxW) return text;
        String ellipsis = "...";
        int ellipsisW = font.width(ellipsis);
        if (maxW <= ellipsisW) return ellipsis;
        return font.plainSubstrByWidth(text, maxW - ellipsisW) + ellipsis;
    }
}
