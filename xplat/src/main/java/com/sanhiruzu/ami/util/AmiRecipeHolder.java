package com.sanhiruzu.ami.util;

import net.minecraft.resources.Identifier;
import net.minecraft.world.item.crafting.Recipe;

public record AmiRecipeHolder<T extends Recipe<?>>(Identifier id, T value) {
    public Identifier getId() {
        return id;
    }

    public T value() {
        return value;
    }
}
