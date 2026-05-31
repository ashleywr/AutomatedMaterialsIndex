package com.sanhiruzu.ami.neoforge;

import com.sanhiruzu.ami.client.InventoryOverlayHandler;
import com.sanhiruzu.ami.client.ItemIconCache;
import com.sanhiruzu.ami.client.ThemeResourceLoader;
import com.sanhiruzu.ami.client.icon.RendererRegistry;
import com.sanhiruzu.ami.client.results.ItemGridView;
import com.sanhiruzu.ami.client.tooltip.AmiTooltipRenderer;
import com.sanhiruzu.ami.client.tooltip.CompositeTooltipComponent;
import com.sanhiruzu.ami.client.tooltip.HeartBarTooltipComponent;
import com.sanhiruzu.ami.client.tooltip.NeoForgeTooltipHooks;
import com.sanhiruzu.ami.client.tooltip.StatIconRowTooltipComponent;
import com.sanhiruzu.ami.config.AmiConfigStore;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.ModList;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.RegisterClientReloadListenersEvent;
import net.neoforged.neoforge.client.event.RegisterClientTooltipComponentFactoriesEvent;
import net.neoforged.neoforge.client.gui.ConfigurationScreen;
import net.neoforged.neoforge.client.gui.IConfigScreenFactory;

@Mod(value = AMI.MODID, dist = Dist.CLIENT)
@EventBusSubscriber(modid = AMI.MODID, value = Dist.CLIENT)
public class AMIClient {

    public AMIClient(ModContainer container, net.neoforged.bus.api.IEventBus modEventBus) {
        AmiTooltipRenderer.setHooks(new NeoForgeTooltipHooks());
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

        if (jeiLoaded) {
            AMI.LOGGER.debug("JEI detected - plugin will integrate when ready");
        }
        if (emiLoaded) {
            AMI.LOGGER.debug("EMI detected - plugin will integrate when ready");
        }
        if (!jeiLoaded && !emiLoaded) {
            AMI.LOGGER.debug("No recipe UI detected - AMI shell UI will be used");
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

    static void onRegisterReloadListeners(RegisterClientReloadListenersEvent event) {
        event.registerReloadListener(ThemeResourceLoader.INSTANCE);
    }

    @SubscribeEvent
    static void onPlayerLogout(ClientPlayerNetworkEvent.LoggingOut event) {
        ItemIconCache.invalidate();
        ItemGridView.clearStackCache();
        RendererRegistry.invalidateAll();
        InventoryOverlayHandler.resetSessionState();
        com.sanhiruzu.ami.network.AmiNetworkState.onServer = false;
    }

    @SubscribeEvent
    static void onItemTooltip(net.neoforged.neoforge.event.entity.player.ItemTooltipEvent event) {
        com.sanhiruzu.ami.client.AmiTooltipHandler.appendTooltip(event.getItemStack(), event.getToolTip());
    }
}
