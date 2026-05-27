package com.sanhiruzu.ami.platform;

import net.minecraft.resources.ResourceLocation;

import java.util.Optional;

public interface IPlatformHelper {
    boolean isClient();

    /**
     * Gets the human-readable display name of a mod, or Optional.empty() if not found/not applicable.
     */
    Optional<String> getModName(String modId);

    ResourceLocation rl(String namespace, String path);

    default ResourceLocation rl(String namespaceAndPath) {
        int colon = namespaceAndPath.indexOf(':');
        return colon >= 0
                ? rl(namespaceAndPath.substring(0, colon), namespaceAndPath.substring(colon + 1))
                : rl("minecraft", namespaceAndPath);
    }
}
