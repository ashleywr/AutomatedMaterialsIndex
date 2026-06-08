package com.sanhiruzu.ami.index.resolvers;

import com.google.gson.*;
import com.sanhiruzu.ami.AmiCore;
import com.sanhiruzu.ami.platform.Services;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.regex.Pattern;

public final class PlayerHeadHistory {
    private static final int MAX_ENTRIES = 50;
    private static final String FILE_NAME = "player_head_history.json";
    private static final Pattern VALID_NAME = Pattern.compile("[A-Za-z0-9_]{1,16}");
    private static final Gson GSON = new GsonBuilder().create();
    private static volatile Path historyFileOverrideForTests;

    private PlayerHeadHistory() {
    }

    public static boolean isValidName(String name) {
        return name != null && VALID_NAME.matcher(name).matches();
    }

    public static List<String> load() {
        Path file = resolveFile();
        if (file == null || !Files.exists(file)) return List.of();
        try {
            JsonArray arr = JsonParser.parseString(Files.readString(file, StandardCharsets.UTF_8)).getAsJsonArray();
            List<String> result = new ArrayList<>();
            Set<String> seen = new LinkedHashSet<>();
            for (JsonElement element : arr) {
                String name = element.getAsString();
                if (!isValidName(name)) continue;
                String key = name.toLowerCase(Locale.ROOT);
                if (seen.add(key)) {
                    result.add(name);
                }
                if (result.size() >= MAX_ENTRIES) break;
            }
            return List.copyOf(result);
        } catch (RuntimeException | IOException e) {
            AmiCore.LOGGER.warn("AMI: Failed to load player head history: {}", e.getMessage());
            return List.of();
        }
    }

    public static void record(String playerName) {
        if (!isValidName(playerName)) return;
        Path file = resolveFile();
        if (file == null) return;

        Set<String> names = new LinkedHashSet<>();
        names.add(playerName);
        for (String existing : load()) {
            if (names.size() >= MAX_ENTRIES) break;
            if (!existing.equalsIgnoreCase(playerName)) {
                names.add(existing);
            }
        }

        JsonArray arr = new JsonArray();
        for (String name : names) {
            arr.add(name);
        }
        try {
            Files.createDirectories(file.getParent());
            Files.writeString(file, GSON.toJson(arr), StandardCharsets.UTF_8);
        } catch (IOException e) {
            AmiCore.LOGGER.warn("AMI: Failed to save player head history: {}", e.getMessage());
        }
    }

    public static void setHistoryFileOverrideForTests(Path file) {
        historyFileOverrideForTests = file;
    }

    public static void clearHistoryFileOverrideForTests() {
        historyFileOverrideForTests = null;
    }

    private static Path resolveFile() {
        Path override = historyFileOverrideForTests;
        if (override != null) {
            return override;
        }
        try {
            return Services.PLATFORM.getConfigDir().resolve("ami").resolve(FILE_NAME);
        } catch (RuntimeException | LinkageError e) {
            return null;
        }
    }
}
