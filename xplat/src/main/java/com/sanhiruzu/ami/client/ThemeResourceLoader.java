package com.sanhiruzu.ami.client;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.sanhiruzu.ami.AmiCore;
import com.sanhiruzu.ami.config.AmiConfig;
import com.sanhiruzu.ami.platform.Services;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public final class ThemeResourceLoader extends SimpleJsonResourceReloadListener {
    private static final Gson GSON = new Gson();
    private static final String DIRECTORY = "themes";
    private static final Map<ResourceLocation, ThemeResourceStyles.ThemeDefinition> THEMES = new HashMap<>();
    public static final ThemeResourceLoader INSTANCE = new ThemeResourceLoader();

    private ThemeResourceLoader() {
        super(GSON, DIRECTORY);
    }

    public static void applyCurrentTheme() {
        ResourceLocation id = selectedThemeId();
        ThemeResourceStyles.ThemeDefinition theme = THEMES.get(id);
        if (theme == null) {
            return;
        }
        theme.apply();
    }

    public static ResourceLocation selectedThemeId() {
        return Services.PLATFORM.rl(AmiCore.MODID, AmiConfig.theme.name().toLowerCase(Locale.ROOT));
    }

    @Override
    protected void apply(Map<ResourceLocation, JsonElement> resources, ResourceManager resourceManager, ProfilerFiller profiler) {
        Map<ResourceLocation, ThemeResourceStyles.ThemeDefinition> parsed = new HashMap<>();
        for (Map.Entry<ResourceLocation, JsonElement> entry : resources.entrySet()) {
            ThemeResourceStyles.ThemeDefinition theme = ThemeResourceStyles.parse(entry.getKey(), entry.getValue());
            if (!theme.isEmpty()) {
                parsed.put(entry.getKey(), theme);
            }
        }

        THEMES.clear();
        THEMES.putAll(parsed);
        AMITheme.sync();
        AmiCore.LOGGER.info("Loaded {} AMI resource theme(s)", THEMES.size());
    }
}
