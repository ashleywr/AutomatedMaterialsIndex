package com.sanhiruzu.ami.fabric;

import com.sanhiruzu.ami.client.AMITheme;
import com.sanhiruzu.ami.client.AmiClientTelemetry;
import com.sanhiruzu.ami.client.InventoryOverlayHandler;
import com.sanhiruzu.ami.client.ThemeResourceLoader;
import com.sanhiruzu.ami.client.discovery.AmiDiscoveryState;
import com.sanhiruzu.ami.client.icon.EntityIconRenderer;
import com.sanhiruzu.ami.client.icon.RendererRegistry;
import com.sanhiruzu.ami.client.results.ItemGridView;
import com.sanhiruzu.ami.client.tooltip.CompositeTooltipComponent;
import com.sanhiruzu.ami.client.tooltip.HeartBarTooltipComponent;
import com.sanhiruzu.ami.client.tooltip.PokemonStatBarsComponent;
import com.sanhiruzu.ami.client.tooltip.PokemonTypeBadgesComponent;
import com.sanhiruzu.ami.client.tooltip.StatIconRowTooltipComponent;
import com.sanhiruzu.ami.compat.FtbQuestsRuntimeCompat;
import com.sanhiruzu.ami.config.AmiConfigStore;
import com.sanhiruzu.ami.fabric.client.AmiFabricClientHooks;
import com.sanhiruzu.ami.fabric.client.FabricAmiKeyMappings;
import com.sanhiruzu.ami.index.AmiIndexerService;
import com.sanhiruzu.ami.index.GlobalIndexCache;
import com.sanhiruzu.ami.network.AmiNetworkState;
import com.sanhiruzu.ami.network.AmiServerPingPacket;
import com.sanhiruzu.ami.platform.Services;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.rendering.v1.TooltipComponentCallback;
import net.fabricmc.fabric.api.resource.IdentifiableResourceReloadListener;
import net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipComponent;
import net.fabricmc.fabric.api.resource.ResourceManagerHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.util.profiling.ProfilerFiller;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

/**
 * AMI client entrypoint for Fabric.
 * <p>
 * Responsibilities:
 * - Register the AMI key bindings that are supported on Fabric via Fabric's KeyBindingHelper.
 * - Load config and sync the theme palette on client startup.
 * - Register the ThemeResourceLoader as a client reload listener.
 * - Register the client-side receiver for AmiServerPingPacket (S2C).
 * - Drive the per-tick index build (GlobalIndexCache pre-load, AmiIndexerService,
 *   AmiDiscoveryState) mirroring NeoForge AMIClient's onClientTick handler.
 * - Reset session state (caches, network flag) on player logout.
 * <p>
 * What is NOT here (later milestones):
 * - Screen mixins for JEI/EMI integration (Milestone F).
 * - Recipe index integration (Milestone E).
 */
@Environment(EnvType.CLIENT)
public class AmiFabricClient implements ClientModInitializer {

    private static boolean cachePreloadTriggered = false;

    @Override
    public void onInitializeClient() {
        AmiFabric.LOGGER.debug("================================");
        AmiFabric.LOGGER.debug("AMI client setup initialized (Fabric)");
        AmiFabric.LOGGER.debug("================================");

        registerKeyBindings();
        registerTooltipComponentFactories();
        registerResourceReloadListeners();
        registerNetworkReceivers();
        registerClientLifecycleEvents();
        registerClientTickEvents();
        registerPlayerLogoutEvents();
        AmiFabricClientHooks.register();

        // Config + theme load: mirror NeoForge's onClientSetup
        AmiConfigStore.load();
        AMITheme.sync();

        boolean ftbQuestsLoaded = Services.PLATFORM.isModLoaded("ftbquests");
        boolean jeiLoaded = Services.PLATFORM.isModLoaded("jei");
        boolean emiLoaded = Services.PLATFORM.isModLoaded("emi");

        if (jeiLoaded) {
            AmiFabric.LOGGER.debug("JEI detected - plugin will integrate when ready");
        }
        if (emiLoaded) {
            AmiFabric.LOGGER.debug("EMI detected - plugin will integrate when ready");
        }
        if (ftbQuestsLoaded) {
            AmiFabric.LOGGER.debug("FTB Quests detected - runtime quest mirror enabled");
        }
        if (!jeiLoaded && !emiLoaded) {
            AmiFabric.LOGGER.debug("No recipe UI detected - AMI shell UI will be used");
        }
        FtbQuestsRuntimeCompat.setModLoaded(ftbQuestsLoaded);
    }

    // -------------------------------------------------------------------------
    // Key bindings
    // -------------------------------------------------------------------------

    /**
     * Registers the AMI key bindings that Fabric actually supports.
     * The KeyMapping instances live in FabricPlatformHelper.KEY_MAPPINGS (a singleton
     * FabricAmiKeyMappings), so the same objects are both registered here and returned
     * by Services.PLATFORM.keyMappings().
     */
    private void registerKeyBindings() {
        FabricAmiKeyMappings keyMappings = (FabricAmiKeyMappings) Services.PLATFORM.keyMappings();
        int registeredCount = 0;
        for (var km : keyMappings.all()) {
            if ("key.ami.debug_tooltips".equals(km.getName()) && !Services.PLATFORM.supportsDebugTooltipToggle()) {
                continue;
            }
            KeyBindingHelper.registerKeyBinding(km);
            registeredCount++;
        }
        AmiFabric.LOGGER.debug("AMI: registered {} key bindings", registeredCount);
    }

    // -------------------------------------------------------------------------
    // Tooltip component factories
    // -------------------------------------------------------------------------

    /**
     * Fabric equivalent of NeoForge's RegisterClientTooltipComponentFactoriesEvent.
     * AMI's tooltip components implement both {@code TooltipComponent} (data) and
     * {@code ClientTooltipComponent} (render), so each maps to itself. Without this,
     * vanilla {@code ClientTooltipComponent.create()} throws when AMI passes one of
     * its custom components to {@code GuiGraphics.renderTooltip(...)}.
     */
    private void registerTooltipComponentFactories() {
        TooltipComponentCallback.EVENT.register(data -> {
            if (data instanceof CompositeTooltipComponent
                    || data instanceof HeartBarTooltipComponent
                    || data instanceof StatIconRowTooltipComponent
                    || data instanceof PokemonTypeBadgesComponent
                    || data instanceof PokemonStatBarsComponent) {
                return (ClientTooltipComponent) data;
            }
            return null;
        });
    }

    // -------------------------------------------------------------------------
    // Resource reload listeners
    // -------------------------------------------------------------------------

    /**
     * Fabric requires reload listeners to implement {@link IdentifiableResourceReloadListener}
     * (to provide a stable {@link ResourceLocation} ID). ThemeResourceLoader is xplat and extends
     * vanilla's SimpleJsonResourceReloadListener — we cannot modify it. This thin wrapper
     * delegates all work to ThemeResourceLoader.INSTANCE while satisfying the Fabric interface.
     */
    private void registerResourceReloadListeners() {
        ResourceManagerHelper.get(PackType.CLIENT_RESOURCES)
                .registerReloadListener(new IdentifiableResourceReloadListener() {
                    private static final ResourceLocation ID =
                            ResourceLocation.fromNamespaceAndPath("ami", "theme_loader");

                    @Override
                    public ResourceLocation getFabricId() {
                        return ID;
                    }

                    @Override
                    public CompletableFuture<Void> reload(
                            PreparationBarrier barrier,
                            ResourceManager manager,
                            ProfilerFiller prepareProfiler,
                            ProfilerFiller applyProfiler,
                            Executor prepareExecutor,
                            Executor applyExecutor) {
                        return ThemeResourceLoader.INSTANCE.reload(
                                barrier, manager, prepareProfiler, applyProfiler,
                                prepareExecutor, applyExecutor);
                    }
                });
    }

    // -------------------------------------------------------------------------
    // Networking — client-side receiver for the S2C server-ping packet
    // -------------------------------------------------------------------------

    private void registerNetworkReceivers() {
        ClientPlayNetworking.registerGlobalReceiver(AmiServerPingPacket.TYPE,
                (payload, context) -> {
                    // handleOnClient() only writes a boolean; safe to call on network thread
                    payload.handleOnClient();
                });
    }

    // -------------------------------------------------------------------------
    // Client lifecycle
    // -------------------------------------------------------------------------

    private void registerClientLifecycleEvents() {
        // Nothing needed at CLIENT_STARTED for this milestone.
        // The index build is driven by the tick handler (one-shot trigger on first tick).
        ClientLifecycleEvents.CLIENT_STARTED.register(client ->
                AmiFabric.LOGGER.debug("AMI: Minecraft client started"));
    }

    // -------------------------------------------------------------------------
    // Client tick — mirrors NeoForge AMIClient.onClientTick
    // -------------------------------------------------------------------------

    private void registerClientTickEvents() {
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            AmiClientTelemetry.beginClientTick();
            try {
                if (!cachePreloadTriggered) {
                    cachePreloadTriggered = true;
                    GlobalIndexCache.preloadAsync();
                }
                FtbQuestsRuntimeCompat.clientTick();
                AmiDiscoveryState.getInstance().clientTick();
                AmiIndexerService.getInstance().ensurePendingRecipeIndexBuild();
                InventoryOverlayHandler.tickAutoIndexBootstrap();
                EntityIconRenderer.tickAtlasWarmup();
            } finally {
                AmiClientTelemetry.endClientTick();
            }
        });
    }

    // -------------------------------------------------------------------------
    // Player logout — mirrors NeoForge AMIClient.onPlayerLogout
    // -------------------------------------------------------------------------

    private void registerPlayerLogoutEvents() {
        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> {
            ItemGridView.clearStackCache();
            RendererRegistry.invalidateAll();
            InventoryOverlayHandler.resetSessionState();
            AmiNetworkState.onServer = false;
            FtbQuestsRuntimeCompat.clear();
        });
    }
}
