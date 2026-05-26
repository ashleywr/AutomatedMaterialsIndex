package com.sanhiruzu.ami.mixin;

import org.objectweb.asm.tree.ClassNode;
import org.spongepowered.asm.mixin.extensibility.IMixinConfigPlugin;
import org.spongepowered.asm.mixin.extensibility.IMixinInfo;

import java.util.List;
import java.util.Set;

public class AmiMixinConfigPlugin implements IMixinConfigPlugin {

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

    private boolean isModLoaded(String modId) {
        try {
            net.minecraftforge.fml.loading.LoadingModList list = net.minecraftforge.fml.loading.LoadingModList.get();
            if (list == null) return true; // Fallback for unit tests
            return list.getModFileById(modId) != null;
        } catch (Throwable t) {
            return true; // Fallback for unit tests where FMLLoader is not active
        }
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
