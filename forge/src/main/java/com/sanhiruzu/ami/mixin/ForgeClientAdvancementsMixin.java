package com.sanhiruzu.ami.mixin;

import com.sanhiruzu.ami.client.results.AdvancementRuntimeDocuments;
import net.minecraft.client.multiplayer.ClientAdvancements;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Keeps AMI's advancement-search cache aligned with packets from the server. */
@Mixin(value = ClientAdvancements.class, remap = false)
public abstract class ForgeClientAdvancementsMixin {

    // Forge 1.20.1 production jars use m_104399_; dev runs retain the named method.
    @Inject(method = {"update", "m_104399_"}, at = @At("HEAD"), remap = false, require = 1)
    private void ami$invalidateSearchIndex(CallbackInfo ci) {
        AdvancementRuntimeDocuments.invalidateSearchIndex();
    }
}
