package com.sanhiruzu.ami.client.widget;

import com.sanhiruzu.ami.client.AMITheme;
import com.sanhiruzu.ami.client.overlay.OverlayLayers;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.network.chat.Component;

import java.lang.reflect.Field;
import java.util.function.Consumer;

public class AmiEnumDropdownWidget extends AbstractWidget implements AmiDropdownPopup {
    private static final int OPTION_HEIGHT = 18;
    private static final int MAX_VISIBLE_OPTIONS = 6;

    private final Field field;
    private final Consumer<Object> onChange;
    private final Consumer<AmiDropdownPopup> onOpen;
    private final Object[] constants;
    private boolean open;

    public AmiEnumDropdownWidget(Field field, Consumer<Object> onChange, Consumer<AmiDropdownPopup> onOpen) {
        super(0, 0, 96, 18, AmiWidgetFactory.enumLabel(field));
        this.field = field;
        this.onChange = onChange;
        this.onOpen = onOpen;
        this.constants = field.getType().getEnumConstants();
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
        if (!open || constants == null || constants.length == 0) return;

        int listX = getX();
        int listW = width;
        int visible = Math.min(constants.length, MAX_VISIBLE_OPTIONS);
        int listH = visible * OPTION_HEIGHT + 2;
        int listY = dropdownY(listH);

        g.pose().pushPose();
        g.pose().translate(0, 0, OverlayLayers.DROPDOWN);
        AMITheme.fillPixelPopup(g, listX, listY, listW, listH,
                AMITheme.DROPDOWN_LIST_BG, AMITheme.BORDER_LIGHT, AMITheme.CONTROL_SHADOW, 0);

        Object selected = currentValue();
        var font = Minecraft.getInstance().font;
        for (int i = 0; i < visible; i++) {
            int optionY = listY + 1 + i * OPTION_HEIGHT;
            boolean hovered = mouseX >= listX && mouseX <= listX + listW
                    && mouseY >= optionY && mouseY < optionY + OPTION_HEIGHT;
            boolean active = constants[i] != null && constants[i].equals(selected);
            if (hovered || active) {
                g.fill(listX + 1, optionY, listX + listW - 1, optionY + OPTION_HEIGHT,
                        hovered ? AMITheme.DROPDOWN_BG_ACTIVE : AMITheme.DROPDOWN_BG);
            }

            Component label = AmiWidgetFactory.enumConstantLabel(constants[i]);
            String text = label.getString();
            int maxTextW = Math.max(0, listW - 8);
            if (font.width(text) > maxTextW) {
                text = font.plainSubstrByWidth(text, Math.max(0, maxTextW - 8)) + "..";
            }
            g.drawString(font, text, listX + 4, optionY + 5,
                    active ? AMITheme.TEXT_HIGHLIGHT : AMITheme.TEXT_PRIMARY, false);

            if (hovered) {
                Component optionTooltip = AmiWidgetFactory.enumConstantOptionTooltip(constants[i]);
                if (optionTooltip != null) {
                    g.renderTooltip(font, optionTooltip, mouseX, mouseY);
                }
            }
        }
        g.pose().popPose();
    }

    @Override
    protected void renderWidget(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        boolean hovered = active && isMouseOver(mouseX, mouseY);
        int fill = (open || hovered) ? AMITheme.DROPDOWN_BG_ACTIVE : AMITheme.DROPDOWN_BG;
        AMITheme.fillControlChrome(g, getX(), getY(), width, height, fill, open);

        var font = Minecraft.getInstance().font;
        String text = getMessage().getString();
        int arrowW = font.width(open ? "^" : "v");
        int maxTextW = Math.max(0, width - arrowW - 10);
        if (font.width(text) > maxTextW) {
            text = font.plainSubstrByWidth(text, Math.max(0, maxTextW - 8)) + "..";
        }
        int textY = getY() + (height - font.lineHeight) / 2 + 1;
        g.drawString(font, text, getX() + 4, textY, active ? AMITheme.CONFIG_TEXT_PRIMARY : AMITheme.CONFIG_TEXT_MUTED, false);
        g.drawString(font, open ? "^" : "v", getX() + width - arrowW - 4, textY, AMITheme.CONFIG_TEXT_SECONDARY, false);
    }

    /**
     * {@code AmiDropdownPopupController} calls {@link #handlePopupClick} directly while a
     * dropdown is already open, but the initial click that opens a closed dropdown reaches this
     * widget through the normal {@link AbstractWidget} click dispatch (e.g.
     * {@code AmiConfigScreen}'s per-row {@code widget.mouseClicked(...)}), so that path must also
     * route into the same popup-click contract.
     */
    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        return handlePopupClick(mouseX, mouseY, button);
    }

    @Override
    public boolean handlePopupClick(double mouseX, double mouseY, int button) {
        if (!active || button != 0) return false;

        if (open && isMouseOverPopup(mouseX, mouseY)) {
            int listH = Math.min(constants.length, MAX_VISIBLE_OPTIONS) * OPTION_HEIGHT + 2;
            int index = (int) ((mouseY - (dropdownY(listH) + 1)) / OPTION_HEIGHT);
            if (index >= 0 && index < constants.length && index < MAX_VISIBLE_OPTIONS) {
                setValue(constants[index]);
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
        int visible = Math.min(constants.length, MAX_VISIBLE_OPTIONS);
        int listX = getX();
        int listH = visible * OPTION_HEIGHT + 2;
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

    private Object currentValue() {
        try {
            return field.get(null);
        } catch (Exception e) {
            return null;
        }
    }

    private void setValue(Object value) {
        try {
            field.set(null, value);
            setMessage(AmiWidgetFactory.enumConstantLabel(value));
            onChange.accept(value);
        } catch (Exception ignored) {
        }
    }

    @Override
    protected void updateWidgetNarration(NarrationElementOutput output) {
        defaultButtonNarrationText(output);
    }
}
