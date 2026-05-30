package com.sanhiruzu.ami.mixin;

import com.sanhiruzu.ami.compat.ForgeEmiSyntheticRecipeIds;
import dev.emi.emi.EmiPort;
import dev.emi.emi.bom.BoM;
import net.minecraft.resources.ResourceLocation;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(value = BoM.class, remap = false)
public class ForgeEmiBoMMixin {
    @Redirect(
            method = "loadAdded",
            at = @At(value = "INVOKE", target = "Ldev/emi/emi/EmiPort;id(Ljava/lang/String;)Lnet/minecraft/resources/ResourceLocation;")
    )
    private static ResourceLocation ami$normalizeAddedDefaults(String id) {
        return ForgeEmiSyntheticRecipeIds.normalize(id);
    }
}
