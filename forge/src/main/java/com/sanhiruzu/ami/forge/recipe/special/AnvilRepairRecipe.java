package com.sanhiruzu.ami.forge.recipe.special;

import com.sanhiruzu.ami.recipe.special.AmiSpecialRecipe;
import com.sanhiruzu.ami.recipe.special.AnvilRepairRecipeView;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.Level;

import java.util.List;

public class AnvilRepairRecipe extends AmiSpecialRecipe implements Recipe<net.minecraft.world.Container>, AnvilRepairRecipeView {
    private final ResourceLocation id;
    private final ItemStack tool;
    private final Ingredient material;

    public AnvilRepairRecipe(ResourceLocation id, ItemStack tool, Ingredient material, ItemStack output, RecipeType<?> type) {
        super(output, type, List.of(Ingredient.of(tool), material));
        this.id = id;
        this.tool = tool;
        this.material = material;
    }

    public ResourceLocation getId() {
        return id;
    }

    public ItemStack getTool() {
        return tool;
    }

    public Ingredient getMaterial() {
        return material;
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
