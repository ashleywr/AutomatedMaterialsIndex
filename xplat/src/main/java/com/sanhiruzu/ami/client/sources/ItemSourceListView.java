package com.sanhiruzu.ami.client.sources;

import com.sanhiruzu.ami.client.AMITheme;
import com.sanhiruzu.ami.client.icon.RendererRegistry;
import com.sanhiruzu.ami.client.sources.ItemSourceViewProjector.HitTarget;
import com.sanhiruzu.ami.client.sources.ItemSourceViewProjector.TargetKind;
import com.sanhiruzu.ami.index.SearchNode;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;

import java.util.List;
import java.util.function.Consumer;
import java.util.function.Predicate;

public final class ItemSourceListView {
    private int x;
    private int y;
    private int width;
    private int height;
    private ItemSourceReport report = new ItemSourceReport(Component.empty(), List.of());
    private SourceActionHandler actionHandler;
    private Predicate<SearchNode> entityInfoAvailable = node -> false;

    public enum SourceAction {
        OPEN_LINK,
        OPEN_OUTPUT_RECIPES,
        OPEN_OUTPUT_CONTEXT,
        LOCATE_BIOME,
        OPEN_BIOME_CONTEXT,
        OPEN_ENTITY_INFO
    }

    @FunctionalInterface
    public interface SourceActionHandler {
        boolean handle(SearchNode node, SourceAction action, int mouseX, int mouseY);
    }

    public ItemSourceListView(int x, int y, int width, int height) {
        updateLayout(x, y, width, height);
    }

    public void updateLayout(int x, int y, int width, int height) {
        this.x = x;
        this.y = y;
        this.width = Math.max(0, width);
        this.height = Math.max(0, height);
    }

    public void setReport(ItemSourceReport report) {
        this.report = report == null ? new ItemSourceReport(Component.empty(), List.of()) : report;
    }

    public void setActionHandler(SourceActionHandler handler) {
        this.actionHandler = handler;
    }

    public void setEntityInfoAvailable(Predicate<SearchNode> predicate) {
        this.entityInfoAvailable = predicate == null ? node -> false : predicate;
    }

    public void setLinkClickCallback(Consumer<SearchNode> callback) {
        this.actionHandler = callback == null
                ? null
                : (node, action, mouseX, mouseY) -> {
                    if (action != SourceAction.OPEN_LINK) {
                        return false;
                    }
                    callback.accept(node);
                    return true;
                };
    }

    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if ((button != 0 && button != 1) || mouseX < x || mouseX >= x + width || mouseY < y || mouseY >= y + height) {
            return false;
        }

        HitTarget target = projection(Minecraft.getInstance().font).hitTargetAt((int) mouseX, (int) mouseY);
        if (target == null || target.link() == null || target.link().node() == null || actionHandler == null) {
            return false;
        }
        if (target.kind() == TargetKind.PRIMARY && !isPrimaryCardClickable(target.row())) {
            return false;
        }

        SourceAction action = actionFor(target, button);
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
        ItemSourceViewProjector.Projection projection = projection(font);
        for (ItemSourceViewProjector.Row row : projection.rows()) {
            switch (row.kind()) {
                case LOADING, EMPTY, DIAGNOSTIC -> renderTextRow(g, font, row);
                case GROUP_HEADER -> renderGroupHeader(g, font, row);
                case CARD -> renderCard(g, font, row.card(), mouseX, mouseY);
            }
        }
    }

    private ItemSourceViewProjector.Projection projection(net.minecraft.client.gui.Font font) {
        return ItemSourceViewProjector.project(report, new ItemSourceViewProjector.TextMeasurer() {
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

    private void renderTextRow(GuiGraphics g, net.minecraft.client.gui.Font font, ItemSourceViewProjector.Row row) {
        String text = switch (row.kind()) {
            case LOADING, EMPTY -> Component.translatable(row.text()).getString();
            default -> row.text();
        };
        g.drawString(font, truncate(font, text, row.textWidth()),
                row.bounds().x(), row.bounds().y() + 3, AMITheme.TEXT_SUBTLE, false);
    }

    private void renderGroupHeader(GuiGraphics g, net.minecraft.client.gui.Font font, ItemSourceViewProjector.Row row) {
        ItemSourceViewProjector.Rect bounds = row.bounds();
        int rowRight = bounds.x() + bounds.width();
        g.fill(bounds.x(), bounds.y(), rowRight, bounds.y() + bounds.height(), AMITheme.GRID_GROUP_ROOT_BG);
        g.fill(bounds.x(), bounds.y(), bounds.x() + 2, bounds.y() + bounds.height(), AMITheme.ACCENT_BLUE);
        g.fill(bounds.x() + 2, bounds.y() + bounds.height() - 1, rowRight - 2, bounds.y() + bounds.height(), AMITheme.SECTION_SEP);
        g.drawString(font, truncate(font, row.type().label(), Math.max(0, row.textWidth() - 27)),
                bounds.x() + ItemSourceViewProjector.PAD_X + 27,
                bounds.y() + Math.max(0, (bounds.height() - font.lineHeight) / 2),
                AMITheme.TEXT_PRIMARY, false);
    }

    private void renderCard(GuiGraphics g, net.minecraft.client.gui.Font font, ItemSourceViewProjector.CardLayout card,
                            int mouseX, int mouseY) {
        ItemSourceRow row = card.source();
        ItemSourceViewProjector.Rect bounds = card.bounds();
        boolean hovered = bounds.contains(mouseX, mouseY);
        int fill = hovered && isPrimaryCardClickable(row) ? AMITheme.DROPDOWN_BG_ACTIVE : AMITheme.DROPDOWN_BG;
        AMITheme.fillInsetRect(g, bounds.x(), bounds.y(), bounds.width(), bounds.height(), fill, false);

        SearchNode primary = row.primaryLink() == null ? null : row.primaryLink().node();
        if (primary != null) {
            RendererRegistry.get(primary.type()).render(g, primary, card.iconX() + 4, card.iconY() + 4, AMITheme.ICON_SIZE, hovered);
        }

        int maxTextW = Math.max(0, card.textRight() - card.textX());
        String name = row.primaryLink() == null || row.primaryLink().label().isBlank()
                ? row.text()
                : row.primaryLink().label();
        g.drawString(font, truncate(font, name, maxTextW), card.textX(), bounds.y() + 6, AMITheme.TEXT_PRIMARY, false);
        renderRouteLine(g, font, card, hovered);

        if (!row.biomeLinks().isEmpty()) {
            renderBiomeChips(g, font, card, mouseX, mouseY);
        }
    }

    private void renderRouteLine(GuiGraphics g, net.minecraft.client.gui.Font font,
                                 ItemSourceViewProjector.CardLayout card, boolean hovered) {
        ItemSourceRow row = card.source();
        ItemSourceLink output = row.routeOutputLink();
        if (output == null || output.node() == null) {
            g.drawString(font, truncate(font, row.routeSummary(), Math.max(0, card.textRight() - card.textX())),
                    card.textX(), card.lineY(),
                    AMITheme.TEXT_SUBTLE, false);
            return;
        }

        String action = row.routeActionLabel();
        int actionW = font.width(action);
        g.drawString(font, action, card.textX(), card.lineY() + 2, AMITheme.TEXT_SUBTLE, false);
        int arrowX = card.textX() + actionW + 4;
        g.drawString(font, ">", arrowX, card.lineY() + 2, AMITheme.TEXT_SUBTLE, false);

        int iconX = arrowX + font.width(">") + 5;
        int iconY = card.lineY() - 1;
        RendererRegistry.get(output.node().type()).render(g, output.node(), iconX, iconY,
                ItemSourceViewProjector.ROUTE_ICON_SIZE, hovered);

        int labelX = iconX + ItemSourceViewProjector.ROUTE_ICON_SIZE + 4;
        int labelW = Math.max(0, card.textRight() - labelX);
        g.drawString(font, truncate(font, output.label(), labelW), labelX, card.lineY() + 2,
                AMITheme.TEXT_SUBTLE, false);
    }

    private void renderBiomeChips(GuiGraphics g, net.minecraft.client.gui.Font font,
                                  ItemSourceViewProjector.CardLayout card, int mouseX, int mouseY) {
        for (ItemSourceViewProjector.ChipLayout chip : card.biomeChips()) {
            ItemSourceLink link = chip.link();
            ItemSourceViewProjector.Rect bounds = chip.bounds();
            boolean hovered = bounds.contains(mouseX, mouseY);
            int fill = hovered ? AMITheme.DROPDOWN_BG_ACTIVE : AMITheme.SLOT_BG;
            AMITheme.fillInsetRect(g, bounds.x(), bounds.y(), bounds.width(), bounds.height(), fill, false);
            RendererRegistry.get(link.node().type()).render(g, link.node(), bounds.x() + 3, bounds.y() + 2,
                    ItemSourceViewProjector.BIOME_CHIP_ICON_SIZE, hovered);
            int labelX = bounds.x() + ItemSourceViewProjector.BIOME_CHIP_ICON_SIZE + 6;
            g.drawString(font, truncate(font, link.label(), bounds.width() - (labelX - bounds.x()) - 3),
                    labelX, bounds.y() + 2,
                    AMITheme.TEXT_SUBTLE, false);
        }
    }

    private static int groupHeaderHeight() {
        return AMITheme.ROW_HEIGHT;
    }

    private boolean isPrimaryCardClickable(ItemSourceRow row) {
        if (row == null || row.primaryLink() == null || row.primaryLink().node() == null) {
            return false;
        }
        if (row.type() == ItemSourceType.MOB_DROP) {
            SearchNode node = row.primaryLink().node();
            return node != null && entityInfoAvailable.test(node);
        }
        return true;
    }

    private static SourceAction actionFor(HitTarget target, int button) {
        if (target.kind() == TargetKind.BIOME) {
            return button == 1 ? SourceAction.OPEN_BIOME_CONTEXT : SourceAction.LOCATE_BIOME;
        }
        if (target.kind() == TargetKind.OUTPUT) {
            if (target.link() == null || target.link().node() == null || target.link().node().type() != com.sanhiruzu.ami.index.NodeType.ITEM) {
                return null;
            }
            return button == 1 ? SourceAction.OPEN_OUTPUT_CONTEXT : SourceAction.OPEN_OUTPUT_RECIPES;
        }
        if (button != 0) {
            return null;
        }
        return target.row() != null && target.row().type() == ItemSourceType.MOB_DROP
                ? SourceAction.OPEN_ENTITY_INFO
                : SourceAction.OPEN_LINK;
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
