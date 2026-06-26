package com.sanhiruzu.ami.index;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Loads an optional modpack-author override file from {@code <gamedir>/config/ami/overrides.json}
 * and merges it on top of the bundled mod defaults. Never throws — the index must keep running
 * even if the pack file is missing or malformed.
 */
public final class PackOverrideLoader {

    public static final String RELATIVE_PATH = "ami/overrides.json";

    public record LoadResult(boolean fileFound, boolean parseOk, int bytesRead, String errorMessage) {}

    private PackOverrideLoader() {}

    public static LoadResult loadFrom(Path configDir) {
        Path file = configDir.resolve(RELATIVE_PATH);
        if (!Files.exists(file)) {
            return new LoadResult(false, true, 0, null);
        }
        String body;
        try {
            body = Files.readString(file, StandardCharsets.UTF_8);
        } catch (IOException e) {
            return new LoadResult(true, false, 0, e.getMessage());
        }
        try {
            ClassificationOverrides.mergeAndInstall(body);
            return new LoadResult(true, true, body.length(), null);
        } catch (RuntimeException e) {
            // mergeAndInstall already swallows parse errors, but guard the call site too.
            return new LoadResult(true, false, body.length(), e.getMessage());
        }
    }
}
