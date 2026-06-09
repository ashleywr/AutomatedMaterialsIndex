package com.sanhiruzu.ami.compat;

import com.sanhiruzu.ami.AmiCore;
import com.sanhiruzu.ami.index.GlobalIndex;
import com.sanhiruzu.ami.index.providers.IngredientIndexProvider;
import com.sanhiruzu.ami.index.providers.IngredientPluginRegistry;
import com.sanhiruzu.ami.platform.Services;
import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import net.minecraft.Util;
import net.minecraft.resources.ResourceLocation;

import java.util.concurrent.CompletableFuture;

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
    private static final JeiIngredientBridge INGREDIENT_BRIDGE = new JeiIngredientBridge();
    private static boolean bridgeRegistered = false;

    @Override
    public ResourceLocation getPluginUid() {
        return UID;
    }

    @Override
    public void onRuntimeAvailable(mezz.jei.api.runtime.IJeiRuntime jeiRuntime) {
        JeiRuntimeAccessor.setRuntime(jeiRuntime);
        JeiRuntimeListenerBridge.register(jeiRuntime);
        JeiIngredientBridge.setManager(jeiRuntime.getIngredientManager());
        if (!bridgeRegistered) {
            IngredientPluginRegistry.register(INGREDIENT_BRIDGE);
            bridgeRegistered = true;
        }
        // Re-run ingredient discovery now that JEI's manager is populated.
        // Uses the targeted rebuildRuntimeHandles path — not a full re-index.
        CompletableFuture.runAsync(() -> {
            try {
                IngredientIndexProvider.rebuildRuntimeHandles(GlobalIndex.getInstance());
                AmiCore.LOGGER.info("AMI: JEI ingredient bridge re-index complete");
            } catch (Throwable t) {
                AmiCore.LOGGER.warn("AMI: JEI ingredient bridge re-index failed", t);
            }
        }, Util.backgroundExecutor());
        RecipeViewerStateSync.recipesChanged();
    }

    @Override
    public void onRuntimeUnavailable() {
        JeiRuntimeAccessor.clearRuntime();
        JeiIngredientBridge.clearManager();
        RecipeViewerStateSync.recipesChanged();
    }
}
