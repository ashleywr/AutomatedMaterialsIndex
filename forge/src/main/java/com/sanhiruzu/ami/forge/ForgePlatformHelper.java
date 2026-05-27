package com.sanhiruzu.ami.forge;

import com.sanhiruzu.ami.platform.IPlatformHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.loading.FMLLoader;

import java.util.Optional;

public class ForgePlatformHelper implements IPlatformHelper {
    @Override
    public boolean isClient() {
        return FMLLoader.getDist().isClient();
    }

    @Override
    public Optional<String> getModName(String modId) {
        if (ModList.get() != null) {
            return ModList.get().getModContainerById(modId)
                    .map(mc -> mc.getModInfo().getDisplayName());
        }
        return Optional.empty();
    }

    @Override
    public ResourceLocation rl(String namespace, String path) {
        return new ResourceLocation(namespace, path);
    }
}
