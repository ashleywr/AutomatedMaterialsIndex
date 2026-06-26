package com.sanhiruzu.ami.forge;

import com.sanhiruzu.ami.client.InventoryOverlayHandler;
import com.sanhiruzu.ami.client.AmiClientTelemetry;
import com.sanhiruzu.ami.client.ThemeResourceLoader;
import com.sanhiruzu.ami.client.icon.EntityIconRenderer;
import com.sanhiruzu.ami.client.icon.RendererRegistry;
import com.sanhiruzu.ami.client.results.ItemGridView;
import com.sanhiruzu.ami.client.tooltip.CompositeTooltipComponent;
import com.sanhiruzu.ami.client.tooltip.HeartBarTooltipComponent;
import com.sanhiruzu.ami.client.tooltip.StatIconRowTooltipComponent;
import com.sanhiruzu.ami.compat.FtbQuestsRuntimeCompat;
import com.sanhiruzu.ami.config.AmiConfigStore;
import com.sanhiruzu.ami.forge.client.AMIKeyMappings;
import com.sanhiruzu.ami.util.AmiWorldTooltipComposer;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent;
import net.minecraftforge.client.event.RegisterClientReloadListenersEvent;
import net.minecraftforge.client.event.RegisterClientTooltipComponentFactoriesEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;

public class AMIClient {

    public static void init(IEventBus modEventBus) {
        modEventBus.addListener(AMIClient::onClientSetup);
        modEventBus.addListener(AMIClient::onRegisterTooltipFactories);
        modEventBus.addListener(AMIClient::onRegisterReloadListeners);
        modEventBus.addListener(AMIKeyMappings::registerKeyMappings);
    }

    @SubscribeEvent
    public static void onClientSetup(FMLClientSetupEvent event) {
        AMI.LOGGER.debug("================================");
        AMI.LOGGER.debug("AMI client setup initialized");
        AMI.LOGGER.debug("================================");

        AmiConfigStore.load();
        com.sanhiruzu.ami.client.AMITheme.sync();

        boolean jeiLoaded = ModList.get().isLoaded("jei");
        boolean emiLoaded = ModList.get().isLoaded("emi");
        boolean ftbQuestsLoaded = ModList.get().isLoaded("ftbquests");

        if (jeiLoaded) {
            AMI.LOGGER.debug("✓ JEI detected - plugin will integrate when ready");
        }
        if (emiLoaded) {
            AMI.LOGGER.debug("✓ EMI detected - plugin will integrate when ready");
        }
        if (ftbQuestsLoaded) {
            AMI.LOGGER.debug("✓ FTB Quests detected - runtime quest mirror enabled");
        }
        if (!jeiLoaded && !emiLoaded) {
            AMI.LOGGER.debug("✓ No recipe UI detected - AMI shell UI will be used");
        }
        FtbQuestsRuntimeCompat.setModLoaded(ftbQuestsLoaded);
    }

    @SubscribeEvent
    public static void onRegisterTooltipFactories(RegisterClientTooltipComponentFactoriesEvent event) {
        event.register(CompositeTooltipComponent.class, c -> c);
        event.register(HeartBarTooltipComponent.class, c -> c);
        event.register(StatIconRowTooltipComponent.class, c -> c);
        event.register(com.sanhiruzu.ami.client.tooltip.PokemonTypeBadgesComponent.class, c -> c);
        event.register(com.sanhiruzu.ami.client.tooltip.PokemonStatBarsComponent.class, c -> c);
    }

    public static void onRegisterReloadListeners(RegisterClientReloadListenersEvent event) {
        event.registerReloadListener(ThemeResourceLoader.INSTANCE);
    }

    @Mod.EventBusSubscriber(modid = AMI.MODID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.FORGE)
    public static class ForgeEvents {
        @SubscribeEvent
        public static void onPlayerLogout(ClientPlayerNetworkEvent.LoggingOut event) {
            ItemGridView.clearStackCache();
            RendererRegistry.invalidateAll();
            AmiWorldTooltipComposer.invalidateCache();
            InventoryOverlayHandler.resetSessionState();
            com.sanhiruzu.ami.network.AmiNetworkState.onServer = false;
            FtbQuestsRuntimeCompat.clear();
        }

        @SubscribeEvent
        public static void onClientTick(TickEvent.ClientTickEvent event) {
            if (event.phase == TickEvent.Phase.END) {
                AmiClientTelemetry.beginClientTick();
                try {
                    FtbQuestsRuntimeCompat.clientTick();
                    InventoryOverlayHandler.tickAutoIndexBootstrap();
                    EntityIconRenderer.tickAtlasWarmup();
                } finally {
                    AmiClientTelemetry.endClientTick();
                }
            }
        }

        @SubscribeEvent(priority = EventPriority.LOWEST)
        public static void onItemTooltip(net.minecraftforge.event.entity.player.ItemTooltipEvent event) {
            com.sanhiruzu.ami.client.AmiTooltipHandler.appendTooltip(event.getItemStack(), event.getToolTip());
            com.sanhiruzu.ami.client.ClassificationOverrideTooltipAppender.appendTo(event.getItemStack(), event.getToolTip());
        }
    }
}
