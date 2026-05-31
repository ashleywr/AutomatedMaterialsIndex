package com.sanhiruzu.ami.client;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

public final class AmiDebugSettings {
    private static final String DEBUG_COMMANDS_PROPERTY = "ami.debugCommands";
    private static final Properties BUILD_PROPERTIES = loadBuildProperties();

    private AmiDebugSettings() {
    }

    public static boolean debugCommandsEnabled() {
        return Boolean.getBoolean(DEBUG_COMMANDS_PROPERTY) || debugBuild();
    }

    public static boolean debugBuild() {
        return Boolean.parseBoolean(BUILD_PROPERTIES.getProperty("ami.debugBuild", "false"));
    }

    public static String versionLabel() {
        return BUILD_PROPERTIES.getProperty("ami.version", "unknown");
    }

    private static Properties loadBuildProperties() {
        Properties properties = new Properties();
        try (InputStream stream = AmiDebugSettings.class.getResourceAsStream("/ami-build.properties")) {
            if (stream != null) {
                properties.load(stream);
            }
        } catch (IOException ignored) {
            // Build metadata is diagnostic-only. Missing or unreadable metadata should not break startup.
        }
        return properties;
    }
}
