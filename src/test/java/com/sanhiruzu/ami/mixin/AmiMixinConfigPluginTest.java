package com.sanhiruzu.ami.mixin;

import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;

import static org.junit.jupiter.api.Assertions.*;

public class AmiMixinConfigPluginTest {

    private static final String CLASS_NAME = "com.sanhiruzu.ami.mixin.AmiMixinConfigPlugin";

    @Test
    void shouldApplyMixinReturnsTrueForJeiClassNames() throws Exception {
        Object plugin = Class.forName(CLASS_NAME).getConstructor().newInstance();
        Method m = plugin.getClass().getMethod("shouldApplyMixin", String.class, String.class);
        assertTrue((boolean) m.invoke(plugin, "mezz.jei.gui.startup.JeiGuiStarter", "JeiGuiStarterMixin"));
        assertTrue((boolean) m.invoke(plugin, "mezz.jei.library.load.PluginCaller", "PluginCallerMixin"));
        assertTrue((boolean) m.invoke(plugin, "mezz.jei.neoforge.plugins.neoforge.NeoForgeGuiPlugin", "NeoForgeGuiPluginMixin"));
    }

    @Test
    void shouldApplyMixinReturnsTrueForNonJeiClassNames() throws Exception {
        Object plugin = Class.forName(CLASS_NAME).getConstructor().newInstance();
        Method m = plugin.getClass().getMethod("shouldApplyMixin", String.class, String.class);
        assertTrue((boolean) m.invoke(plugin, "net.minecraft.client.Minecraft", "SomeMixin"));
        assertTrue((boolean) m.invoke(plugin, "com.example.UnknownClass", "AnotherMixin"));
    }

    @Test
    void shouldApplyMixinReturnsTrueForEmptyStrings() throws Exception {
        Object plugin = Class.forName(CLASS_NAME).getConstructor().newInstance();
        Method m = plugin.getClass().getMethod("shouldApplyMixin", String.class, String.class);
        assertTrue((boolean) m.invoke(plugin, "", ""));
    }

    @Test
    void getRefMapperConfigReturnsNull() throws Exception {
        Object plugin = Class.forName(CLASS_NAME).getConstructor().newInstance();
        Method m = plugin.getClass().getMethod("getRefMapperConfig");
        assertNull(m.invoke(plugin));
    }

    @Test
    void getMixinsReturnsNull() throws Exception {
        Object plugin = Class.forName(CLASS_NAME).getConstructor().newInstance();
        Method m = plugin.getClass().getMethod("getMixins");
        assertNull(m.invoke(plugin));
    }
}
