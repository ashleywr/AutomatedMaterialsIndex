package com.sanhiruzu.ami.emi;

import java.util.List;
import java.util.ArrayList;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;

/**
 * Captured slot layout from an EmiRecipe's addWidgets() call.
 */
public class AmiCapturedRecipe {
    public record CapturedSlot(
        List<ItemStack> alternatives,
        int x, int y, int width, int height,
        boolean isOutput
    ) {}

    private final ResourceLocation recipeId;
    private final ResourceLocation categoryId;
    private final String categoryName;
    private final ItemStack categoryIcon;
    private final List<CapturedSlot> slots = new ArrayList<>();
    private int displayWidth = 134;
    private int displayHeight = 72;

    public AmiCapturedRecipe(ResourceLocation recipeId, ResourceLocation categoryId,
                             String categoryName, ItemStack categoryIcon) {
        this.recipeId = recipeId;
        this.categoryId = categoryId;
        this.categoryName = categoryName;
        this.categoryIcon = categoryIcon;
    }

    public void setDisplayDimensions(int w, int h) {
        this.displayWidth = w;
        this.displayHeight = h;
    }

    public void addSlot(List<ItemStack> alternatives, int x, int y) {
        slots.add(new CapturedSlot(alternatives, x, y, 18, 18, false));
    }

    public ResourceLocation recipeId() { return recipeId; }
    public ResourceLocation categoryId() { return categoryId; }
    public String categoryName() { return categoryName; }
    public ItemStack categoryIcon() { return categoryIcon; }
    public List<CapturedSlot> slots() { return slots; }
    public int displayWidth() { return displayWidth; }
    public int displayHeight() { return displayHeight; }
}
