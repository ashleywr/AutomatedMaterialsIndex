package com.sanhiruzu.ami.forge.recipe.special;

import com.sanhiruzu.ami.recipe.special.AmiSpecialRecipe;
import com.sanhiruzu.ami.recipe.special.CompostingRecipeView;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.Level;

import java.util.List;

public class CompostingRecipe extends AmiSpecialRecipe implements Recipe<net.minecraft.world.Container>, CompostingRecipeView {
    private final ResourceLocation id;
    private final ItemStack stack;
    private final float chance;

    public CompostingRecipe(ResourceLocation id, ItemStack stack, float chance, RecipeType<?> type) {
        super(new ItemStack(Items.BONE_MEAL), type, List.of(Ingredient.of(stack)));
        this.id = id;
        this.stack = stack;
        this.chance = chance;
    }

    public ResourceLocation getId() {
        return id;
    }

    public ItemStack getStack() {
        return stack;
    }

    public float getChance() {
        return chance;
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
