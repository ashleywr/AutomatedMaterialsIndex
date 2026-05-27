package com.sanhiruzu.ami.platform;

import java.util.ServiceLoader;

public class Services {
    public static final IPlatformHelper PLATFORM = load();

    private static IPlatformHelper load() {
        return ServiceLoader.load(IPlatformHelper.class, Services.class.getClassLoader())
                .findFirst()
                .orElseThrow(() -> new RuntimeException("No IPlatformHelper found!"));
    }
}
