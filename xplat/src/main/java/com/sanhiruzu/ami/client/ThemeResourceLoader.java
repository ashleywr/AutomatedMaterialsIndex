package com.sanhiruzu.ami.client;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.sanhiruzu.ami.AmiCore;
import com.sanhiruzu.ami.config.AmiConfig;
import com.sanhiruzu.ami.platform.Services;
import net.minecraft.resources.FileToIdConverter;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimplePreparableReloadListener;
import net.minecraft.util.GsonHelper;
import net.minecraft.util.profiling.ProfilerFiller;

import java.io.IOException;
import java.io.Reader;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public final class ThemeResourceLoader extends SimplePreparableReloadListener<Map<Identifier, JsonElement>> {
    private static final Gson GSON = new Gson();
    private static final String DIRECTORY = "themes";
    private static final FileToIdConverter LISTER = FileToIdConverter.json(DIRECTORY);
    private static final Map<Identifier, ThemeResourceStyles.ThemeDefinition> THEMES = new HashMap<>();
    public static final ThemeResourceLoader INSTANCE = new ThemeResourceLoader();

    private ThemeResourceLoader() {}

    public static void applyCurrentTheme() {
        Identifier id = selectedThemeId();
        ThemeResourceStyles.ThemeDefinition theme = THEMES.get(id);
        if (theme == null) {
            return;
        }
        theme.apply();
    }

    public static Identifier selectedThemeId() {
        return Services.PLATFORM.rl(AmiCore.MODID, AmiConfig.theme.name().toLowerCase(Locale.ROOT));
    }

    @Override
    protected Map<Identifier, JsonElement> prepare(ResourceManager manager, ProfilerFiller profiler) {
        Map<Identifier, JsonElement> result = new HashMap<>();
        for (Map.Entry<Identifier, Resource> entry : LISTER.listMatchingResources(manager).entrySet()) {
            Identifier fileId = entry.getKey();
            Identifier themeId = LISTER.fileToId(fileId);
            try (Reader reader = entry.getValue().openAsReader()) {
                JsonElement json = GsonHelper.fromJson(GSON, reader, JsonElement.class);
                if (json != null) {
                    result.put(themeId, json);
                }
            } catch (IOException | com.google.gson.JsonParseException e) {
                AmiCore.LOGGER.error("Failed to load AMI theme {}", fileId, e);
            }
        }
        return result;
    }

    @Override
    protected void apply(Map<Identifier, JsonElement> resources, ResourceManager resourceManager, ProfilerFiller profiler) {
        Map<Identifier, ThemeResourceStyles.ThemeDefinition> parsed = new HashMap<>();
        for (Map.Entry<Identifier, JsonElement> entry : resources.entrySet()) {
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
