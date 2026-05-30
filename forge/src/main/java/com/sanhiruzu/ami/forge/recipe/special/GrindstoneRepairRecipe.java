package com.sanhiruzu.ami.forge.recipe.special;

import com.sanhiruzu.ami.recipe.special.AmiSpecialRecipe;
import com.sanhiruzu.ami.recipe.special.GrindstoneRepairRecipeView;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.Level;

import java.util.List;

public class GrindstoneRepairRecipe extends AmiSpecialRecipe implements Recipe<net.minecraft.world.Container>, GrindstoneRepairRecipeView {
    private final ResourceLocation id;
    private final ItemStack tool1;
    private final ItemStack tool2;

    public GrindstoneRepairRecipe(ResourceLocation id, ItemStack tool1, ItemStack tool2, ItemStack output, RecipeType<?> type) {
        super(output, type, List.of(Ingredient.of(tool1), Ingredient.of(tool2)));
        this.id = id;
        this.tool1 = tool1;
        this.tool2 = tool2;
    }

    public ResourceLocation getId() {
        return id;
    }

    public ItemStack getTool1() {
        return tool1;
    }

    public ItemStack getTool2() {
        return tool2;
    }

    public ItemStack getOutput() {
        return result();
    }

    @Override
    public boolean matches(net.minecraft.world.Container input, Level level) {
        return false;
    }

    @Override
    public ItemStack assemble(net.minecraft.world.Container container, net.minecraft.core.RegistryAccess registryAccess) {
        return resultCopy();
    }

    @Override
    public ItemStack getResultItem(net.minecraft.core.RegistryAccess registryAccess) {
        return result();
    }
}
