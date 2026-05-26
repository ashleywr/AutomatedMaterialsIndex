package com.sanhiruzu.ami.neoforge;

import com.sanhiruzu.ami.platform.IPlatformHelper;
import net.neoforged.fml.ModList;
import net.neoforged.fml.loading.FMLLoader;

import java.util.Optional;

public class NeoForgePlatformHelper implements IPlatformHelper {
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
}
