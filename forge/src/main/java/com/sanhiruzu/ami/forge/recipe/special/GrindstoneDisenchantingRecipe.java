package com.sanhiruzu.ami.forge.recipe.special;

import com.sanhiruzu.ami.recipe.special.AmiSpecialRecipe;
import com.sanhiruzu.ami.recipe.special.GrindstoneDisenchantingRecipeView;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.Level;

import java.util.List;

public class GrindstoneDisenchantingRecipe extends AmiSpecialRecipe implements Recipe<net.minecraft.world.Container>, GrindstoneDisenchantingRecipeView {
    private final ResourceLocation id;
    private final ItemStack enchanted;

    public GrindstoneDisenchantingRecipe(ResourceLocation id, ItemStack enchanted, ItemStack normal, RecipeType<?> type) {
        super(normal, type, List.of(Ingredient.of(enchanted)));
        this.id = id;
        this.enchanted = enchanted;
    }

    public ResourceLocation getId() {
        return id;
    }

    public ItemStack getEnchanted() {
        return enchanted;
    }

    public ItemStack getNormal() {
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
