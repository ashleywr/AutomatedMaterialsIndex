package com.sanhiruzu.ami.neoforge;

import com.sanhiruzu.ami.index.AmiRecipeIndex;
import com.sanhiruzu.ami.neoforge.client.AMIKeyMappings;
import com.sanhiruzu.ami.platform.IAmiKeyMappings;
import com.sanhiruzu.ami.platform.IPlatformHelper;
import com.sanhiruzu.ami.util.AmiRecipeHolder;
import net.minecraft.client.KeyMapping;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeType;
import net.neoforged.fml.ModList;
import net.neoforged.fml.loading.FMLLoader;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class NeoForgePlatformHelper implements IPlatformHelper {
    private static final IAmiKeyMappings KEY_MAPPINGS = new IAmiKeyMappings() {
        @Override public KeyMapping favorite()       { return AMIKeyMappings.FAVORITE; }
        @Override public KeyMapping toggleViewer()   { return AMIKeyMappings.TOGGLE_VIEWER; }
        @Override public KeyMapping showRecipes()    { return AMIKeyMappings.SHOW_RECIPES; }
        @Override public KeyMapping showUses()       { return AMIKeyMappings.SHOW_USES; }
        @Override public KeyMapping cheatGiveStack() { return AMIKeyMappings.CHEAT_GIVE_STACK; }
        @Override public KeyMapping cheatGiveOne()   { return AMIKeyMappings.CHEAT_GIVE_ONE; }
        @Override public KeyMapping debugTooltips()  { return AMIKeyMappings.DEBUG_TOOLTIPS; }
        @Override public KeyMapping recipeBack()     { return AMIKeyMappings.RECIPE_BACK; }
        @Override public KeyMapping[] all()          { return new KeyMapping[]{
            AMIKeyMappings.FAVORITE, AMIKeyMappings.TOGGLE_VIEWER, AMIKeyMappings.SHOW_RECIPES,
            AMIKeyMappings.SHOW_USES, AMIKeyMappings.CHEAT_GIVE_STACK, AMIKeyMappings.CHEAT_GIVE_ONE,
            AMIKeyMappings.DEBUG_TOOLTIPS, AMIKeyMappings.RECIPE_BACK
        }; }
    };

    @Override
    public boolean isClient() {
        return FMLLoader.getDist().isClient();
    }

    @Override
    public Optional<String> getModName(String modId) {
        if (ModList.get() != null) {
            return ModList.get().getModContainerById(modId)
                    .map(mc -> mc.getModInfo().getDisplayName());
        }
        return Optional.empty();
    }

    @Override
    public ResourceLocation rl(String namespace, String path) {
        return ResourceLocation.fromNamespaceAndPath(namespace, path);
    }

    @Override
    public IAmiKeyMappings keyMappings() {
        return KEY_MAPPINGS;
    }

    @Override
    public boolean isRecipeIndexBuilt() {
        return AmiRecipeIndex.getInstance().isBuilt();
    }

    @Override
    public List<AmiRecipeHolder<?>> getRecipesFor(ItemStack target) {
        return wrapAll(AmiRecipeIndex.getInstance().getRecipesFor(target));
    }

    @Override
    public List<AmiRecipeHolder<?>> getUsesFor(ItemStack target) {
        return wrapAll(AmiRecipeIndex.getInstance().getUsesFor(target));
    }

    @Override
    @SuppressWarnings({"unchecked", "rawtypes"})
    public List<AmiRecipeHolder<?>> getAllRecipesOfType(RecipeType<?> type) {
        return wrapAll((List<RecipeHolder<?>>) (List<?>) AmiRecipeIndex.getInstance().getAllRecipesOfType((RecipeType) type));
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private static AmiRecipeHolder<?> wrap(RecipeHolder<?> h) {
        return new AmiRecipeHolder(h.id(), h.value());
    }

    private static List<AmiRecipeHolder<?>> wrapAll(List<RecipeHolder<?>> holders) {
        List<AmiRecipeHolder<?>> result = new ArrayList<>(holders.size());
        for (RecipeHolder<?> h : holders) result.add(wrap(h));
        return result;
    }
}
