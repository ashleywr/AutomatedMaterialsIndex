package com.sanhiruzu.ami.forge;

import com.sanhiruzu.ami.client.InventoryOverlayHandler;
import com.sanhiruzu.ami.client.ItemIconCache;
import com.sanhiruzu.ami.client.ThemeResourceLoader;
import com.sanhiruzu.ami.client.icon.RendererRegistry;
import com.sanhiruzu.ami.client.results.ItemGridView;
import com.sanhiruzu.ami.util.AmiWorldTooltipComposer;
import com.sanhiruzu.ami.client.tooltip.CompositeTooltipComponent;
import com.sanhiruzu.ami.client.tooltip.HeartBarTooltipComponent;
import com.sanhiruzu.ami.client.tooltip.StatIconRowTooltipComponent;
import com.sanhiruzu.ami.forge.client.AMIKeyMappings;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent;
import net.minecraftforge.client.event.RegisterClientReloadListenersEvent;
import net.minecraftforge.client.event.RegisterClientTooltipComponentFactoriesEvent;
import net.minecraftforge.client.ConfigScreenHandler;
import net.minecraft.client.gui.screens.Screen;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;

public class AMIClient {

    public static void init() {
        IEventBus modEventBus = FMLJavaModLoadingContext.get().getModEventBus();
        modEventBus.addListener(AMIClient::onClientSetup);
        modEventBus.addListener(AMIClient::onRegisterTooltipFactories);
        modEventBus.addListener(AMIClient::onRegisterReloadListeners);
        modEventBus.addListener(AMIKeyMappings::registerKeyMappings);
        
        ModList.get().getModContainerById(AMI.MODID).ifPresent(container -> {
            container.registerExtensionPoint(ConfigScreenHandler.ConfigScreenFactory.class, 
                () -> new ConfigScreenHandler.ConfigScreenFactory((mc, lastScreen) -> (Screen)null)); // Placeholder
        });
    }

    @SubscribeEvent
    public static void onClientSetup(FMLClientSetupEvent event) {
        AMI.LOGGER.info("================================");
        AMI.LOGGER.info("AMI client setup initialized");
        AMI.LOGGER.info("================================");

        boolean jeiLoaded = ModList.get().isLoaded("jei");
        boolean emiLoaded = ModList.get().isLoaded("emi");

        if (jeiLoaded) {
            AMI.LOGGER.info("✓ JEI detected - plugin will integrate when ready");
        }
        if (emiLoaded) {
            AMI.LOGGER.info("✓ EMI detected - plugin will integrate when ready");
        }
        if (!jeiLoaded && !emiLoaded) {
            AMI.LOGGER.info("✓ No recipe UI detected - AMI shell UI will be used");
        }
    }

    @SubscribeEvent
    public static void onRegisterTooltipFactories(RegisterClientTooltipComponentFactoriesEvent event) {
        event.register(CompositeTooltipComponent.class, c -> c);
        event.register(HeartBarTooltipComponent.class, c -> c);
        event.register(StatIconRowTooltipComponent.class, c -> c);
    }

    public static void onRegisterReloadListeners(RegisterClientReloadListenersEvent event) {
        event.registerReloadListener(ThemeResourceLoader.INSTANCE);
    }

    @Mod.EventBusSubscriber(modid = AMI.MODID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.FORGE)
    public static class ForgeEvents {
        @SubscribeEvent
        public static void onPlayerLogout(ClientPlayerNetworkEvent.LoggingOut event) {
            ItemIconCache.invalidate();
            ItemGridView.clearStackCache();
            RendererRegistry.invalidateAll();
            AmiWorldTooltipComposer.invalidateCache();
            InventoryOverlayHandler.resetSessionState();
            com.sanhiruzu.ami.network.AmiNetworkState.onServer = false;
        }
    }
}
