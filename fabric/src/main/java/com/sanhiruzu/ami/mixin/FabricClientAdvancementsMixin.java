package com.sanhiruzu.ami.mixin;

import com.sanhiruzu.ami.client.results.AdvancementRuntimeDocuments;
import net.minecraft.client.multiplayer.ClientAdvancements;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Keeps AMI's advancement-search cache aligned with packets from the server. */
@Mixin(ClientAdvancements.class)
public abstract class FabricClientAdvancementsMixin {

    @Inject(method = "update", at = @At("HEAD"))
    private void ami$invalidateSearchIndex(CallbackInfo ci) {
        AdvancementRuntimeDocuments.invalidateSearchIndex();
    }
}
