package com.sanhiruzu.ami.compat;

import com.sanhiruzu.ami.AMI;
import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.runtime.IJeiRuntime;
import net.minecraft.resources.ResourceLocation;

@JeiPlugin
public class AmiJeiPlugin implements IModPlugin {
    private static final ResourceLocation UID = ResourceLocation.fromNamespaceAndPath(AMI.MODID, "plugin");

    @Override
    public ResourceLocation getPluginUid() {
        return UID;
    }

    @Override
    public void onRuntimeAvailable(IJeiRuntime jeiRuntime) {
        JeiRuntimeAccessor.setRuntime(jeiRuntime);
    }
}
