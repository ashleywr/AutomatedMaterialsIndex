package com.sanhiruzu.ami.compat;

import com.sanhiruzu.ami.AmiCore;
import com.sanhiruzu.ami.platform.Services;
import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import net.minecraft.resources.ResourceLocation;

/**
 * Minimal JEI plugin that stores the runtime reference for search-sync
 * and recipe bridges. JEI's visual overlay is prevented from ever being
 * created by {@code JeiPluginCallerMixin}, which blocks
 * {@code NeoForgeGuiPlugin.registerRuntime} from calling
 * {@code JeiGuiStarter.start()}.
 */
@JeiPlugin
public class AmiJeiPlugin implements IModPlugin {
    private static final ResourceLocation UID = Services.PLATFORM.rl(AmiCore.MODID, "plugin");

    @Override
    public ResourceLocation getPluginUid() {
        return UID;
    }

    @Override
    public void onRuntimeAvailable(mezz.jei.api.runtime.IJeiRuntime jeiRuntime) {
        JeiRuntimeAccessor.setRuntime(jeiRuntime);
        JeiRuntimeListenerBridge.register(jeiRuntime);
        RecipeViewerStateSync.recipesChanged();
    }

    @Override
    public void onRuntimeUnavailable() {
        JeiRuntimeAccessor.clearRuntime();
        RecipeViewerStateSync.recipesChanged();
    }
}
