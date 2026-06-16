package com.sanhiruzu.ami.neoforge;

import com.sanhiruzu.ami.client.InventoryOverlayHandler;
import com.sanhiruzu.ami.client.AmiClientTelemetry;
import com.sanhiruzu.ami.client.ItemIconCache;
import com.sanhiruzu.ami.client.ThemeResourceLoader;
import com.sanhiruzu.ami.client.icon.EntityIconRenderer;
import com.sanhiruzu.ami.client.icon.RendererRegistry;
import com.sanhiruzu.ami.client.results.ItemGridView;
import com.sanhiruzu.ami.client.tooltip.CompositeTooltipComponent;
import com.sanhiruzu.ami.client.tooltip.HeartBarTooltipComponent;
import com.sanhiruzu.ami.client.tooltip.StatIconRowTooltipComponent;
import com.sanhiruzu.ami.api.AmiPluginRegistry;
import com.sanhiruzu.ami.compat.FtbQuestsRuntimeCompat;
import com.sanhiruzu.ami.compat.KubeJSAmiPlugin;
import com.sanhiruzu.ami.config.AmiConfigStore;
import com.sanhiruzu.ami.index.AmiIndexerService;
import com.sanhiruzu.ami.index.GlobalIndexCache;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.ModList;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.AddClientReloadListenersEvent;
import net.neoforged.neoforge.client.event.RegisterClientTooltipComponentFactoriesEvent;
import net.neoforged.neoforge.client.event.RenderFrameEvent;
import net.neoforged.neoforge.client.gui.ConfigurationScreen;
import net.neoforged.neoforge.client.gui.IConfigScreenFactory;

@Mod(value = AMI.MODID, dist = Dist.CLIENT)
@EventBusSubscriber(modid = AMI.MODID, value = Dist.CLIENT)
public class AMIClient {

    public AMIClient(ModContainer container, net.neoforged.bus.api.IEventBus modEventBus) {
        container.registerExtensionPoint(IConfigScreenFactory.class, ConfigurationScreen::new);
        modEventBus.addListener(com.sanhiruzu.ami.neoforge.client.AMIKeyMappings::registerKeyMappings);
        modEventBus.addListener(AMIClient::onRegisterReloadListeners);
    }

    @SubscribeEvent
    static void onClientSetup(FMLClientSetupEvent event) {
        AMI.LOGGER.debug("================================");
        AMI.LOGGER.debug("AMI client setup initialized");
        AMI.LOGGER.debug("================================");

        AmiConfigStore.load();
        com.sanhiruzu.ami.client.AMITheme.sync();

        boolean jeiLoaded = ModList.get().isLoaded("jei");
        boolean emiLoaded = ModList.get().isLoaded("emi");
        boolean ftbQuestsLoaded = ModList.get().isLoaded("ftbquests");
        boolean kubeJsLoaded = ModList.get().isLoaded("kubejs");

        if (jeiLoaded) {
            AMI.LOGGER.debug("JEI detected - plugin will integrate when ready");
        }
        if (emiLoaded) {
            AMI.LOGGER.debug("EMI detected - plugin will integrate when ready");
        }
        if (ftbQuestsLoaded) {
            AMI.LOGGER.debug("FTB Quests detected - runtime quest mirror enabled");
        }
        if (!jeiLoaded && !emiLoaded) {
            AMI.LOGGER.debug("No recipe UI detected - AMI shell UI will be used");
        }
        FtbQuestsRuntimeCompat.setModLoaded(ftbQuestsLoaded);
        if (kubeJsLoaded) {
            AmiPluginRegistry.register(new KubeJSAmiPlugin());
        }
    }

    @SubscribeEvent
    static void onRegisterTooltipFactories(RegisterClientTooltipComponentFactoriesEvent event) {
        event.register(CompositeTooltipComponent.class, c -> c);
        event.register(HeartBarTooltipComponent.class, c -> c);
        event.register(StatIconRowTooltipComponent.class, c -> c);
        event.register(com.sanhiruzu.ami.client.tooltip.PokemonTypeBadgesComponent.class, c -> c);
        event.register(com.sanhiruzu.ami.client.tooltip.PokemonStatBarsComponent.class, c -> c);
    }

    static void onRegisterReloadListeners(AddClientReloadListenersEvent event) {
        event.addListener(net.minecraft.resources.Identifier.fromNamespaceAndPath("ami", "theme_resource_loader"), ThemeResourceLoader.INSTANCE);
    }

    @SubscribeEvent
    static void onPlayerLogout(ClientPlayerNetworkEvent.LoggingOut event) {
        ItemIconCache.invalidate();
        ItemGridView.clearStackCache();
        RendererRegistry.invalidateAll();
        InventoryOverlayHandler.resetSessionState();
        com.sanhiruzu.ami.network.AmiNetworkState.onServer = false;
        FtbQuestsRuntimeCompat.clear();
    }

    private static boolean cachePreloadTriggered = false;

    @SubscribeEvent
    static void onClientTick(ClientTickEvent.Post event) {
        AmiClientTelemetry.beginClientTick();
        try {
            if (!cachePreloadTriggered) {
                cachePreloadTriggered = true;
                GlobalIndexCache.preloadAsync();
            }
            FtbQuestsRuntimeCompat.clientTick();
            com.sanhiruzu.ami.client.discovery.AmiDiscoveryState.getInstance().clientTick();
            AmiIndexerService.getInstance().ensurePendingRecipeIndexBuild();
            InventoryOverlayHandler.tickAutoIndexBootstrap();
            EntityIconRenderer.tickAtlasWarmup();
        } finally {
            AmiClientTelemetry.endClientTick();
        }
    }

    @SubscribeEvent
    static void onRenderFrame(RenderFrameEvent.Post event) {
        AmiClientTelemetry.recordFrame();
    }

    @SubscribeEvent
    static void onItemTooltip(net.neoforged.neoforge.event.entity.player.ItemTooltipEvent event) {
        com.sanhiruzu.ami.client.AmiTooltipHandler.appendTooltip(event.getItemStack(), event.getToolTip());
    }

    @SubscribeEvent
    static void onItemUseFinish(net.neoforged.neoforge.event.entity.living.LivingEntityUseItemEvent.Finish event) {
        if (event.getEntity() == net.minecraft.client.Minecraft.getInstance().player) {
            com.sanhiruzu.ami.client.discovery.AmiDiscoveryState.getInstance().markFoodTasted(event.getItem());
        }
    }
}
