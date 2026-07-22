package com.sanhiruzu.ami.compat;

import net.minecraft.world.item.ItemStack;

import java.util.List;

interface CraftablesProvider {
    Result getCraftables();

    record Result(boolean handled, List<ItemStack> stacks) {
        static Result handled(List<ItemStack> stacks) {
            return new Result(true, stacks == null ? List.of() : List.copyOf(stacks));
        }

        static Result unhandled() {
            return new Result(false, List.of());
        }
    }
}
