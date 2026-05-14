package com.sanhiruzu.ami;

import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.config.ModConfigEvent;
import net.neoforged.neoforge.common.ModConfigSpec;

public class AMIConfig {
    private static final ModConfigSpec.Builder BUILDER = new ModConfigSpec.Builder();

    public static final ModConfigSpec.BooleanValue ENABLE_MATERIAL_ROOT_UI = BUILDER
            .comment("Enable the Material Root UI system")
            .define("enableMaterialRootUI", true);

    public static final ModConfigSpec.BooleanValue ENABLE_GHOST_CRAFTING = BUILDER
            .comment("Enable Ghost Crafting with the Architect's Gauntlet")
            .define("enableGhostCrafting", true);

    public static final ModConfigSpec.BooleanValue ENABLE_AUTO_INDEXING = BUILDER
            .comment("Enable automatic material indexing on world load")
            .define("enableAutoIndexing", true);

    public static final ModConfigSpec.BooleanValue ENABLE_PROGRESSION_GRAPH = BUILDER
            .comment("Enable the progression graph UI")
            .define("enableProgressionGraph", true);

    static final ModConfigSpec SPEC = BUILDER.build();
}
