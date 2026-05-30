package com.sanhiruzu.ami.mixin;

import com.sanhiruzu.ami.compat.ForgeEmiSyntheticRecipeIds;
import dev.emi.emi.EmiPort;
import dev.emi.emi.platform.forge.EmiAgnosForge;
import net.minecraft.resources.ResourceLocation;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(value = EmiAgnosForge.class, remap = false)
public class ForgeEmiAgnosForgeMixin {
    @Redirect(
            method = "addBrewingRecipesAgnos",
            at = @At(value = "INVOKE", target = "Ldev/emi/emi/EmiPort;id(Ljava/lang/String;Ljava/lang/String;)Lnet/minecraft/resources/ResourceLocation;")
    )
    private ResourceLocation ami$normalizeSyntheticBrewingIds(String namespace, String path) {
        return EmiPort.id(namespace, ForgeEmiSyntheticRecipeIds.normalizePath(namespace, path));
    }
}
