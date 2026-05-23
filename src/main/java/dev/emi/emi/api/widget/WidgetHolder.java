package dev.emi.emi.api.widget;

import java.util.List;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import dev.emi.emi.api.stack.EmiIngredient;
import dev.emi.emi.api.stack.EmiStack;

public interface WidgetHolder {
    int getWidth();
    int getHeight();
    <T extends Widget> T add(T widget);

    default SlotWidget addSlot(EmiIngredient ingredient, int x, int y) {
        return add(new SlotWidget(ingredient, x, y));
    }

    default SlotWidget addSlot(int x, int y) {
        return addSlot(EmiStack.EMPTY, x, y);
    }

    default TextureWidget addTexture(ResourceLocation texture, int x, int y, int width, int height, int u, int v) {
        return add(new TextureWidget(texture, x, y, width, height, u, v));
    }

    default DrawableWidget addDrawable(int x, int y, int width, int height, DrawableWidget.DrawableWidgetConsumer consumer) {
        return add(new DrawableWidget(x, y, width, height, consumer));
    }

    default TextWidget addText(Component text, int x, int y, int color, boolean shadow) {
        return add(new TextWidget(text, x, y, color, shadow));
    }

    default FillingArrowWidget addFillingArrow(int x, int y, int time) {
        return add(new FillingArrowWidget(x, y, time));
    }
}
