package com.sanhiruzu.ami.mixin;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Map;

/**
 * Vanilla {@code KeyMapping.click}/{@code set} resolve the pressed physical key through a
 * single-slot {@code Map<InputConstants.Key, KeyMapping>} that {@code resetMapping()} rebuilds by
 * {@code put()}-ing every registered mapping in {@code ALL} order: whichever mapping is processed
 * last for a given default key silently wins that map slot, and every other mapping bound to the
 * same key never receives a click or down-state again, regardless of whether any screen is open.
 * AMI's own default-bound keys (e.g. show_recipes on R) can therefore silently swallow another
 * mod's identically-keyed action purely due to mod load order, with no conflict UI or error.
 * NeoForge/Forge fix this globally via their own one-to-many {@code KeyMappingLookup}; Fabric has
 * no equivalent, so this mixin restores one-to-many dispatch here: every mapping actually bound to
 * the pressed key is updated, not just whichever mapping most recently claimed the map slot.
 */
@Mixin(KeyMapping.class)
public abstract class KeyMappingConflictMixin {

    @Shadow
    @Final
    private static Map<String, KeyMapping> ALL;

    @Shadow
    private InputConstants.Key key;

    @Shadow
    private int clickCount;

    @Shadow
    public abstract void setDown(boolean isDown);

    @Inject(method = "click", at = @At("HEAD"), cancellable = true)
    private static void ami$clickAllBoundMappings(InputConstants.Key key, CallbackInfo ci) {
        for (KeyMapping mapping : ALL.values()) {
            KeyMappingConflictMixin accessor = (KeyMappingConflictMixin) (Object) mapping;
            if (key.equals(accessor.key)) {
                accessor.clickCount++;
            }
        }
        ci.cancel();
    }

    @Inject(method = "set", at = @At("HEAD"), cancellable = true)
    private static void ami$setAllBoundMappings(InputConstants.Key key, boolean isDown, CallbackInfo ci) {
        for (KeyMapping mapping : ALL.values()) {
            KeyMappingConflictMixin accessor = (KeyMappingConflictMixin) (Object) mapping;
            if (key.equals(accessor.key)) {
                mapping.setDown(isDown);
            }
        }
        ci.cancel();
    }
}
