package com.sanhiruzu.ami.mixin;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipComponent;
import net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipPositioner;
import net.minecraft.client.gui.Font;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.gen.Invoker;
import net.minecraft.world.item.ItemStack;

import java.util.List;

@Mixin(GuiGraphics.class)
public interface GuiGraphicsInvoker {
    @Accessor("tooltipStack")
    void ami$setTooltipStack(ItemStack stack);

    @Accessor("tooltipStack")
    ItemStack ami$getTooltipStack();

    @Invoker("renderTooltipInternal")
    void ami$renderTooltipInternal(Font font, List<ClientTooltipComponent> components, int x, int y, ClientTooltipPositioner positioner);
}
