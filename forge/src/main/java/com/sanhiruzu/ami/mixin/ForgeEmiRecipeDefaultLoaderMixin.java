package com.sanhiruzu.ami.mixin;

import com.sanhiruzu.ami.compat.ForgeEmiSyntheticRecipeIds;
import dev.emi.emi.EmiPort;
import dev.emi.emi.data.RecipeDefaultLoader;
import net.minecraft.resources.ResourceLocation;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(value = RecipeDefaultLoader.class, remap = false)
public class ForgeEmiRecipeDefaultLoaderMixin {
    @Redirect(
            method = "loadDefaults",
            at = @At(value = "INVOKE", target = "Ldev/emi/emi/EmiPort;id(Ljava/lang/String;)Lnet/minecraft/resources/ResourceLocation;")
    )
    private static ResourceLocation ami$normalizeDataPackDefaults(String id) {
        return ForgeEmiSyntheticRecipeIds.normalize(id);
    }
}
