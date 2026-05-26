package com.sanhiruzu.ami.platform;

import java.util.Optional;

public interface IPlatformHelper {
    boolean isClient();

    /**
     * Gets the human-readable display name of a mod, or Optional.empty() if not found/not applicable.
     */
    Optional<String> getModName(String modId);
}
