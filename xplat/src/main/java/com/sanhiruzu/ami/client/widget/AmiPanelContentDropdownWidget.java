package com.sanhiruzu.ami.client.widget;

import com.sanhiruzu.ami.client.AMITheme;
import com.sanhiruzu.ami.config.AmiConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.network.chat.Component;

import java.util.function.Consumer;

public class AmiPanelContentDropdownWidget extends AbstractWidget implements AmiDropdownPopup {
    private static final int OPTION_HEIGHT = 18;

    private final AmiConfig.PanelContent[] options;
    private final Consumer<AmiConfig.PanelContent> onChange;
    private final Consumer<AmiDropdownPopup> onOpen;
    private AmiConfig.PanelContent selected;
    private boolean open;

    public AmiPanelContentDropdownWidget(
            AmiConfig.PanelContent selected,
            AmiConfig.PanelContent[] options,
            Consumer<AmiConfig.PanelContent> onChange,
            Consumer<AmiDropdownPopup> onOpen
    ) {
        super(0, 0, 118, 18, label(selected));
        this.selected = selected;
        this.options = options;
        this.onChange = onChange;
        this.onOpen = onOpen;
    }

    @Override
    public boolean isOpen() {
        return open;
    }

    @Override
    public void close() {
        open = false;
    }

    @Override
    public void renderDropdownList(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        if (!open || options == null || options.length == 0) return;

        int listX = getX();
        int listW = width;
        int listH = options.length * OPTION_HEIGHT + 2;
        int listY = dropdownY(listH);

        g.pose().pushPose();
        g.pose().translate(0, 0, com.sanhiruzu.ami.client.overlay.OverlayLayers.DROPDOWN);
        AMITheme.fillPixelPopup(g, listX, listY, listW, listH,
                AMITheme.DROPDOWN_LIST_BG, AMITheme.BORDER_LIGHT, AMITheme.CONTROL_SHADOW, 0);

        var font = Minecraft.getInstance().font;
        for (int i = 0; i < options.length; i++) {
            int optionY = listY + 1 + i * OPTION_HEIGHT;
            boolean hovered = mouseX >= listX && mouseX <= listX + listW
                    && mouseY >= optionY && mouseY < optionY + OPTION_HEIGHT;
            boolean active = options[i] == selected;
            if (hovered || active) {
                g.fill(listX + 1, optionY, listX + listW - 1, optionY + OPTION_HEIGHT,
                        hovered ? AMITheme.DROPDOWN_BG_ACTIVE : AMITheme.DROPDOWN_BG);
            }

            String text = clipped(label(options[i]), listW - 8);
            g.drawString(font, text, listX + 4, optionY + 5,
                    active ? AMITheme.TEXT_HIGHLIGHT : AMITheme.TEXT_PRIMARY, false);
        }
        g.pose().popPose();
    }

    @Override
    protected void renderWidget(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        boolean hovered = active && isMouseOver(mouseX, mouseY);
        int fill = (open || hovered) ? AMITheme.DROPDOWN_BG_ACTIVE : AMITheme.DROPDOWN_BG;
        AMITheme.fillControlChrome(g, getX(), getY(), width, height, fill, open);

        var font = Minecraft.getInstance().font;
        String arrow = open ? "^" : "v";
        int arrowW = font.width(arrow);
        String text = clipped(getMessage(), Math.max(0, width - arrowW - 10));
        int textY = getY() + (height - font.lineHeight) / 2 + 1;
        g.drawString(font, text, getX() + 4, textY,
                active ? AMITheme.CONFIG_TEXT_PRIMARY : AMITheme.CONFIG_TEXT_MUTED, false);
        g.drawString(font, arrow, getX() + width - arrowW - 4, textY, AMITheme.CONFIG_TEXT_SECONDARY, false);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (!active || button != 0) return false;

        if (open && isMouseOverPopup(mouseX, mouseY)) {
            int index = (int) ((mouseY - (dropdownY(options.length * OPTION_HEIGHT + 2) + 1)) / OPTION_HEIGHT);
            if (index >= 0 && index < options.length) {
                setValue(options[index]);
                close();
                return true;
            }
        }

        if (isMouseOver(mouseX, mouseY)) {
            open = !open;
            if (open && onOpen != null) {
                onOpen.accept(this);
            }
            return true;
        }

        return false;
    }

    @Override
    public boolean isMouseOverPopup(double mouseX, double mouseY) {
        int listX = getX();
        int listH = options.length * OPTION_HEIGHT + 2;
        int listY = dropdownY(listH);
        return mouseX >= listX && mouseX <= listX + width && mouseY >= listY && mouseY <= listY + listH;
    }

    private int dropdownY(int listH) {
        int belowY = getY() + height + 1;
        int screenH = Minecraft.getInstance().getWindow().getGuiScaledHeight();
        if (belowY + listH > screenH - 4 && getY() - listH - 1 >= 4) {
            return getY() - listH - 1;
        }
        return belowY;
    }

    private void setValue(AmiConfig.PanelContent value) {
        selected = value;
        setMessage(label(value));
        onChange.accept(value);
    }

    private static String clipped(Component label, int maxWidth) {
        var font = Minecraft.getInstance().font;
        String text = label.getString();
        if (font.width(text) > maxWidth) {
            text = font.plainSubstrByWidth(text, Math.max(0, maxWidth - 8)) + "..";
        }
        return text;
    }

    private static Component label(AmiConfig.PanelContent content) {
        if (content == null) return Component.literal("?");
        String key = switch (content) {
            case GRID -> "ami.config.panel.results_grid";
            case LIST -> "ami.config.panel.results_list";
            case COMPACT -> "ami.config.panel.results_compact";
            case FAVORITES -> "ami.gui.favorites";
            case LOOKUP_HISTORY -> "ami.gui.sidebar.lookup_history";
            case CRAFTING_HISTORY -> "ami.gui.sidebar.crafting_history";
            case CRAFTABLE -> "ami.gui.sidebar.craftable";
            case QUESTS -> "ami.gui.sidebar.quests";
            case EMPTY -> "ami.gui.sidebar.empty";
            default -> null;
        };
        return key == null ? Component.literal(content.name()) : Component.translatable(key);
    }

    @Override
    protected void updateWidgetNarration(NarrationElementOutput output) {
        defaultButtonNarrationText(output);
    }
}
