package dev.emi.emi.api.recipe;

import java.util.Comparator;
import java.util.List;

import com.google.common.collect.Lists;

import dev.emi.emi.api.render.EmiRenderable;
import dev.emi.emi.api.stack.EmiIngredient;
import dev.emi.emi.api.stack.EmiStack;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.world.inventory.tooltip.TooltipComponent;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

public class EmiRecipeCategory implements EmiRenderable {
    public final ResourceLocation id;
    public final EmiRenderable icon;
    public final EmiRenderable simplified;
    public Comparator<EmiRecipe> sorter;

    public EmiRecipeCategory(ResourceLocation id, EmiRenderable icon) {
        this(id, icon, icon);
    }

    public EmiRecipeCategory(ResourceLocation id, EmiRenderable icon, EmiRenderable simplified) {
        this(id, icon, simplified, EmiRecipeSorting.none());
    }

    public EmiRecipeCategory(ResourceLocation id, EmiRenderable icon, EmiRenderable simplified, Comparator<EmiRecipe> sorter) {
        this.id = id;
        this.icon = icon;
        this.simplified = simplified;
        this.sorter = sorter;
    }

    public ResourceLocation getId() { return id; }

    public Component getName() {
        return Component.translatable("emi.category." + id.getNamespace() + "." + id.getPath());
    }

    @Override
    public void render(GuiGraphics draw, int x, int y, float delta) {
        icon.render(draw, x, y, delta);
    }

    public void renderSimplified(GuiGraphics draw, int x, int y, float delta) {
        simplified.render(draw, x, y, delta);
    }

    public List<TooltipComponent> getTooltip() {
        return List.of();
    }
}
