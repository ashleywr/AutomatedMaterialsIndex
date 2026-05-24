package com.sanhiruzu.ami.client;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.sanhiruzu.ami.AMI;
import com.sanhiruzu.ami.client.results.ResultsToolbar;
import net.neoforged.fml.loading.FMLPaths;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

public final class AmiViewPreferences {
    private static final Path FILE = FMLPaths.CONFIGDIR.get().resolve("ami_view_preferences.json");
    private static boolean loaded = false;
    private static StoredView storedView = StoredView.LIST;

    public enum StoredView {
        LIST,
        GRID,
        COMPACT
    }

    private AmiViewPreferences() {
    }

    public static synchronized StoredView loadMainPanelPreference() {
        if (loaded) {
            return storedView;
        }
        loaded = true;

        if (!Files.exists(FILE)) {
            return storedView;
        }

        try {
            JsonObject root = JsonParser.parseString(Files.readString(FILE, StandardCharsets.UTF_8)).getAsJsonObject();
            String preferredView = root.has("preferred_view") ? root.get("preferred_view").getAsString() : "LIST";
            if ("COMPACT".equalsIgnoreCase(preferredView)) {
                storedView = StoredView.COMPACT;
            } else if ("GRID".equalsIgnoreCase(preferredView)) {
                storedView = StoredView.GRID;
            }
        } catch (Exception e) {
            AMI.LOGGER.warn("AMI: Failed to load view preferences: {}", e.getMessage());
        }
        return storedView;
    }

    public static synchronized void saveMainPanelPreference(boolean compactMode, ResultsToolbar.ViewMode viewMode) {
        String preferredView = compactMode ? "COMPACT" : viewMode.name();
        storedView = StoredView.valueOf(preferredView);
        loaded = true;

        try {
            Files.createDirectories(FILE.getParent());
            JsonObject root = new JsonObject();
            root.addProperty("preferred_view", preferredView);
            Files.writeString(FILE, root.toString(), StandardCharsets.UTF_8);
        } catch (Exception e) {
            AMI.LOGGER.warn("AMI: Failed to save view preferences: {}", e.getMessage());
        }
    }
}
