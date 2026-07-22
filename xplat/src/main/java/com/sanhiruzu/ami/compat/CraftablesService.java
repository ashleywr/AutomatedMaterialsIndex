package com.sanhiruzu.ami.compat;

import net.minecraft.world.item.ItemStack;

import java.util.List;

final class CraftablesService {
    private static final CraftablesService DEFAULT = new CraftablesService(List.of(
            new EmiCraftablesProvider(),
            new JeiCraftablesProvider(),
            new VanillaCraftablesProvider()
    ));

    private final List<CraftablesProvider> providers;

    CraftablesService(List<CraftablesProvider> providers) {
        this.providers = providers == null ? List.of() : List.copyOf(providers);
    }

    static List<ItemStack> getCraftables() {
        return DEFAULT.collectCraftables();
    }

    List<ItemStack> collectCraftables() {
        for (CraftablesProvider provider : providers) {
            if (provider == null) {
                continue;
            }
            try {
                CraftablesProvider.Result result = provider.getCraftables();
                if (result != null && result.handled()) {
                    return result.stacks();
                }
            } catch (RuntimeException | LinkageError ignored) {
            }
        }
        return List.of();
    }
}
