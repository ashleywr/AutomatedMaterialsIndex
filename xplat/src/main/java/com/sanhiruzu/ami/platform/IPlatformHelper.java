package com.sanhiruzu.ami.platform;

import com.sanhiruzu.ami.util.AmiRecipeHolder;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeType;

import java.util.List;
import java.util.Optional;

public interface IPlatformHelper {
    boolean isClient();

    Optional<String> getModName(String modId);

    default boolean isModLoaded(String modId) {
        return getModName(modId).isPresent();
    }

    ResourceLocation rl(String namespace, String path);

    IAmiKeyMappings keyMappings();

    default ResourceLocation rl(String namespaceAndPath) {
        int colon = namespaceAndPath.indexOf(':');
        return colon >= 0
                ? rl(namespaceAndPath.substring(0, colon), namespaceAndPath.substring(colon + 1))
                : rl("minecraft", namespaceAndPath);
    }

    boolean isRecipeIndexBuilt();
    List<AmiRecipeHolder<?>> getRecipesFor(ItemStack target);
    List<AmiRecipeHolder<?>> getUsesFor(ItemStack target);
    List<AmiRecipeHolder<?>> getAllRecipesOfType(RecipeType<?> type);

    default boolean tryLoadGlobalIndexCache() {
        return false;
    }

    default void saveGlobalIndexCache() {
    }
}
