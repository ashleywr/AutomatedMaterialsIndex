package com.sanhiruzu.ami.mixin;

import org.objectweb.asm.tree.ClassNode;
import org.spongepowered.asm.mixin.extensibility.IMixinConfigPlugin;
import org.spongepowered.asm.mixin.extensibility.IMixinInfo;

import java.util.List;
import java.util.Set;

public class AmiMixinConfigPlugin implements IMixinConfigPlugin {
    private static final String EMI_SCREEN_MANAGER = "dev.emi.emi.screen.EmiScreenManager";
    private static final String JEI_INGREDIENT_LIST_OVERLAY = "mezz.jei.gui.overlay.IngredientListOverlay";
    private static final String JEI_BOOKMARK_OVERLAY = "mezz.jei.gui.overlay.bookmarks.BookmarkOverlay";

    @Override
    public void onLoad(String mixinPackage) {
    }

    @Override
    public String getRefMapperConfig() {
        return null;
    }

    @Override
    public boolean shouldApplyMixin(String targetClassName, String mixinClassName) {
        if (mixinClassName.endsWith(".EmiScreenManagerMixin")) {
            return isClassPresent(EMI_SCREEN_MANAGER);
        }
        if (mixinClassName.endsWith(".JeiIngredientListOverlayMixin")) {
            return isClassPresent(JEI_INGREDIENT_LIST_OVERLAY);
        }
        if (mixinClassName.endsWith(".JeiBookmarkOverlayMixin")) {
            return isClassPresent(JEI_BOOKMARK_OVERLAY);
        }
        return true;
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

    private static boolean isClassPresent(String className) {
        try {
            Class.forName(className, false, AmiMixinConfigPlugin.class.getClassLoader());
            return true;
        } catch (ClassNotFoundException | NoClassDefFoundError ignored) {
            return false;
        }
    }
}
