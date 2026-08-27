package com.sanhiruzu.ami.compat;

import com.sanhiruzu.ami.AmiCore;
import com.sanhiruzu.ami.index.GlobalIndex;
import com.sanhiruzu.ami.index.AmiIndexerService;
import com.sanhiruzu.ami.index.providers.IngredientIndexProvider;
import com.sanhiruzu.ami.index.providers.IngredientPluginRegistry;
import com.sanhiruzu.ami.platform.Services;
import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import net.minecraft.Util;
import net.minecraft.resources.ResourceLocation;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

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
    private static final long JEI_BRIDGE_REINDEX_DELAY_MS =
            Long.getLong("ami.jeiBridgeReindexDelayMs", 15_000L);
    private static final long JEI_BRIDGE_REINDEX_RETRY_MS =
            Long.getLong("ami.jeiBridgeReindexRetryMs", 5_000L);
    private static final AtomicInteger BRIDGE_REINDEX_GENERATION = new AtomicInteger();
    private static final AtomicBoolean BRIDGE_REINDEX_RUNNING = new AtomicBoolean(false);
    private static boolean bridgeRegistered = false;
    private static volatile boolean runtimeAvailable = false;

    @Override
    public ResourceLocation getPluginUid() {
        return UID;
    }

    @Override
    public void onRuntimeAvailable(mezz.jei.api.runtime.IJeiRuntime jeiRuntime) {
        JeiRuntimeAccessor.setRuntime(jeiRuntime);
        JeiRuntimeListenerBridge.register(jeiRuntime);
        JeiIngredientBridge.setManager(jeiRuntime.getIngredientManager());
        runtimeAvailable = true;
        if (!bridgeRegistered) {
            IngredientPluginRegistry.register(INGREDIENT_BRIDGE);
            bridgeRegistered = true;
        }
        scheduleBridgeReindex(BRIDGE_REINDEX_GENERATION.incrementAndGet(), JEI_BRIDGE_REINDEX_DELAY_MS);
        RecipeViewerStateSync.recipesChanged();
    }

    @Override
    public void onRuntimeUnavailable() {
        runtimeAvailable = false;
        BRIDGE_REINDEX_GENERATION.incrementAndGet();
        JeiRuntimeAccessor.clearRuntime();
        JeiIngredientBridge.clearManager();
        RecipeViewerStateSync.recipesChanged();
    }

    private static void scheduleBridgeReindex(int generation, long delayMs) {
        CompletableFuture.runAsync(
                () -> runBridgeReindexIfReady(generation),
                CompletableFuture.delayedExecutor(delayMs, TimeUnit.MILLISECONDS, Util.backgroundExecutor()));
    }

    private static void runBridgeReindexIfReady(int generation) {
        if (generation != BRIDGE_REINDEX_GENERATION.get() || !runtimeAvailable) {
            return;
        }
        if (AmiIndexerService.getInstance().isBusy()) {
            AmiCore.LOGGER.debug("AMI: delaying JEI ingredient bridge re-index until AMI indexing is idle");
            scheduleBridgeReindex(generation, JEI_BRIDGE_REINDEX_RETRY_MS);
            return;
        }
        if (!BRIDGE_REINDEX_RUNNING.compareAndSet(false, true)) {
            scheduleBridgeReindex(generation, JEI_BRIDGE_REINDEX_RETRY_MS);
            return;
        }
        try {
            if (generation != BRIDGE_REINDEX_GENERATION.get() || !runtimeAvailable) {
                return;
            }
            long started = System.currentTimeMillis();
            IngredientIndexProvider.rebuildRuntimeHandles(GlobalIndex.getInstance());
            AmiCore.LOGGER.info("AMI: JEI ingredient bridge re-index complete in {}ms",
                    System.currentTimeMillis() - started);
        } catch (Throwable t) {
            AmiCore.LOGGER.warn("AMI: JEI ingredient bridge re-index failed", t);
        } finally {
            BRIDGE_REINDEX_RUNNING.set(false);
        }
    }
}
