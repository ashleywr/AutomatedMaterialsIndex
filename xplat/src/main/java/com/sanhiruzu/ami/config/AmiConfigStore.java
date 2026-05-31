package com.sanhiruzu.ami.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.sanhiruzu.ami.platform.Services;

import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

public final class AmiConfigStore {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final String FILE_NAME = "ami-client.json";

    private AmiConfigStore() {
    }

    public static Path file() {
        return Services.PLATFORM.getConfigDir().resolve(FILE_NAME);
    }

    public static void load() {
        Path file = file();
        if (!Files.isRegularFile(file)) {
            save();
            return;
        }
        try {
            JsonObject root = JsonParser.parseString(Files.readString(file, StandardCharsets.UTF_8)).getAsJsonObject();
            for (Field field : AmiConfig.class.getFields()) {
                ConfigValue annotation = field.getAnnotation(ConfigValue.class);
                if (annotation == null || !Modifier.isStatic(field.getModifiers())) {
                    continue;
                }
                JsonElement element = root.get(annotation.value());
                if (element == null || element.isJsonNull()) {
                    continue;
                }
                apply(field, element);
            }
        } catch (IOException | RuntimeException e) {
            System.err.println("AMI: failed to load config " + file + ": " + e.getMessage());
        }
    }

    public static void save() {
        Path file = file();
        JsonObject root = new JsonObject();
        try {
            for (Field field : AmiConfig.class.getFields()) {
                ConfigValue annotation = field.getAnnotation(ConfigValue.class);
                if (annotation == null || !Modifier.isStatic(field.getModifiers())) {
                    continue;
                }
                Object value = field.get(null);
                if (value instanceof Enum<?> enumValue) {
                    root.addProperty(annotation.value(), enumValue.name());
                } else if (value instanceof Boolean boolValue) {
                    root.addProperty(annotation.value(), boolValue);
                } else if (value instanceof Number numberValue) {
                    root.addProperty(annotation.value(), numberValue);
                } else if (value instanceof String stringValue) {
                    root.addProperty(annotation.value(), stringValue);
                }
            }
            Files.createDirectories(file.getParent());
            Files.writeString(file, GSON.toJson(root), StandardCharsets.UTF_8);
        } catch (IOException | IllegalAccessException | RuntimeException e) {
            System.err.println("AMI: failed to save config " + file + ": " + e.getMessage());
        }
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private static void apply(Field field, JsonElement element) {
        try {
            Class<?> type = field.getType();
            if (type == boolean.class || type == Boolean.class) {
                field.setBoolean(null, element.getAsBoolean());
            } else if (type == int.class || type == Integer.class) {
                field.setInt(null, element.getAsInt());
            } else if (type == String.class) {
                field.set(null, element.getAsString());
            } else if (type.isEnum()) {
                field.set(null, Enum.valueOf((Class<? extends Enum>) type, element.getAsString()));
            }
        } catch (IllegalArgumentException | IllegalAccessException ignored) {
        }
    }
}
