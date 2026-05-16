package com.sanhiruzu.ami.api;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class AmiPluginRegistry {
    private static final List<IAmiPlugin> plugins = new ArrayList<>();

    public static void register(IAmiPlugin plugin) {
        plugins.add(plugin);
    }

    public static List<IAmiPlugin> getPlugins() {
        return Collections.unmodifiableList(plugins);
    }
}
