package com.sanhiruzu.ami.util;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.crafting.Recipe;

public record AmiRecipeHolder<T extends Recipe<?>>(ResourceLocation id, T value) {
    public ResourceLocation getId() {
        return id;
    }

    public T value() {
        return value;
    }
}
