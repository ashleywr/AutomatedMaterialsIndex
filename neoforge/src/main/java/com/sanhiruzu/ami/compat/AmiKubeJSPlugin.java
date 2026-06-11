package com.sanhiruzu.ami.compat;

import dev.latvian.mods.kubejs.event.EventGroup;
import dev.latvian.mods.kubejs.event.EventGroupRegistry;
import dev.latvian.mods.kubejs.event.EventHandler;
import dev.latvian.mods.kubejs.plugin.KubeJSPlugin;
import dev.latvian.mods.kubejs.script.ScriptManager;
import net.minecraft.resources.ResourceLocation;

import java.util.LinkedHashSet;
import java.util.Set;

public class AmiKubeJSPlugin implements KubeJSPlugin {
    public static final EventGroup GROUP = EventGroup.of("AmiEvents");
    public static final EventHandler MARK_DEV_ONLY = GROUP.startup("markDevOnly", () -> KubeJSMarkDevOnlyEvent.class);

    // Written once at startup-script time; read later from indexing threads.
    private static volatile Set<ResourceLocation> devOnlyItems = Set.of();

    @Override
    public void registerEvents(EventGroupRegistry registry) {
        registry.register(GROUP);
    }

    @Override
    public void afterScriptsLoaded(ScriptManager manager) {
        if (!manager.scriptType.isStartup()) {
            return;
        }
        Set<ResourceLocation> collected = new LinkedHashSet<>();
        MARK_DEV_ONLY.post(new KubeJSMarkDevOnlyEvent(collected));
        devOnlyItems = Set.copyOf(collected);
    }

    public static Set<ResourceLocation> getDevOnlyItems() {
        return devOnlyItems;
    }
}
