package com.sanhiruzu.ami.mixin;

import org.objectweb.asm.tree.ClassNode;
import org.spongepowered.asm.mixin.extensibility.IMixinConfigPlugin;
import org.spongepowered.asm.mixin.extensibility.IMixinInfo;

import java.util.List;
import java.util.Set;

public class AmiMixinConfigPlugin implements IMixinConfigPlugin {

    private static Boolean isModLoaded(String loadingModListClassName, String modId) {
        try {
            Class<?> loadingModListClass = Class.forName(loadingModListClassName);
            Object list = loadingModListClass.getMethod("get").invoke(null);
            if (list == null) return true; // Fallback for unit tests
            Object modFile = list.getClass().getMethod("getModFileById", String.class).invoke(list, modId);
            return modFile != null;
        } catch (ClassNotFoundException ignored) {
            return null;
        } catch (Throwable ignored) {
            return true; // Fallback for unit tests where FMLLoader is not active
        }
    }

    @Override
    public void onLoad(String mixinPackage) {
    }

    @Override
    public String getRefMapperConfig() {
        return null;
    }

    @Override
    public boolean shouldApplyMixin(String targetClassName, String mixinClassName) {
        if (mixinClassName.contains("Emi")) {
            return isModLoaded("emi");
        }
        if (mixinClassName.contains("Jei")) {
            return isModLoaded("jei");
        }
        return true;
    }

    private static Boolean isFabricModLoaded(String modId) {
        try {
            Class<?> fabricLoaderClass = Class.forName("net.fabricmc.loader.api.FabricLoader");
            Object instance = fabricLoaderClass.getMethod("getInstance").invoke(null);
            Object loaded = instance.getClass().getMethod("isModLoaded", String.class).invoke(instance, modId);
            return loaded instanceof Boolean result ? result : null;
        } catch (ClassNotFoundException ignored) {
            return null;
        } catch (Throwable ignored) {
            return true; // Fallback for unit tests where FabricLoader is not active
        }
    }

    private boolean isModLoaded(String modId) {
        Boolean neoForgeLoaded = isModLoaded("net.neoforged.fml.loading.LoadingModList", modId);
        if (neoForgeLoaded != null) return neoForgeLoaded;

        Boolean forgeLoaded = isModLoaded("net.minecraftforge.fml.loading.LoadingModList", modId);
        if (forgeLoaded != null) return forgeLoaded;

        Boolean fabricLoaded = isFabricModLoaded(modId);
        return fabricLoaded == null || fabricLoaded;
    }

    @Override
    public void acceptTargets(Set<String> myTargets, Set<String> otherTargets) {
    }

    @Override
    public List<String> getMixins() {
        return null;
    }

    @Override
    public void preApply(String targetClassName, ClassNode targetClass, String mixinClassName, IMixinInfo mixinInfo) {
    }

    @Override
    public void postApply(String targetClassName, ClassNode targetClass, String mixinClassName, IMixinInfo mixinInfo) {
    }
}
