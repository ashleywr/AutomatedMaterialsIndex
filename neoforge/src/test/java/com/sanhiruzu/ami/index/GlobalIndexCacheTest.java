package com.sanhiruzu.ami.index;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GlobalIndexCacheTest {
    private static final String ASSET_LOCALES_RESOURCE = "ami/minecraft_locales.txt";
    private static final Pattern ASSET_INDEX_LANGUAGE_PATTERN = Pattern.compile("\"minecraft/lang/([^\".]+)\\.json\"");
    private static final int ASSET_INDEX_SEARCH_DEPTH = 8;

    // ---- dynamicScriptHash ----

    @Test
    void dynamicScriptHashIsEmptyWhenNoDynamicDirsExist(@TempDir Path gameDir) {
        assertEquals("", GlobalIndexCache.dynamicScriptHash(gameDir));
    }

    @Test
    void dynamicScriptHashIsNonEmptyWhenKubeJsDirExists(@TempDir Path gameDir) throws IOException {
        Files.createDirectories(gameDir.resolve("kubejs/server_scripts"));
        Files.writeString(gameDir.resolve("kubejs/server_scripts/recipes.js"), "// hello");

        String hash = GlobalIndexCache.dynamicScriptHash(gameDir);
        assertFalse(hash.isEmpty(), "Expected a non-empty hash when kubejs scripts are present");
        assertTrue(hash.matches("[0-9a-f]{64}"), "Hash should be a 64-char hex string: " + hash);
    }

    @Test
    void dynamicScriptHashChangesWhenFileIsAdded(@TempDir Path gameDir) throws IOException {
        Path dir = gameDir.resolve("kubejs/server_scripts");
        Files.createDirectories(dir);
        Files.writeString(dir.resolve("a.js"), "ServerEvents.recipes(e => {})");

        String before = GlobalIndexCache.dynamicScriptHash(gameDir);
        Files.writeString(dir.resolve("b.js"), "ServerEvents.recipes(e => { e.remove({}) })");
        String after = GlobalIndexCache.dynamicScriptHash(gameDir);

        assertNotEquals(before, after, "Hash must change when a file is added");
    }

    @Test
    void dynamicScriptHashChangesWhenFileIsRemoved(@TempDir Path gameDir) throws IOException {
        Path dir = gameDir.resolve("kubejs/server_scripts");
        Files.createDirectories(dir);
        Path file = dir.resolve("recipes.js");
        Files.writeString(file, "ServerEvents.recipes(e => {})");

        String before = GlobalIndexCache.dynamicScriptHash(gameDir);
        Files.delete(file);
        String after = GlobalIndexCache.dynamicScriptHash(gameDir);

        assertNotEquals(before, after, "Hash must change when a file is removed");
    }

    @Test
    void dynamicScriptHashChangesWhenFileSizeChanges(@TempDir Path gameDir) throws IOException {
        Path dir = gameDir.resolve("scripts");
        Files.createDirectories(dir);
        Path file = dir.resolve("recipes.zs");
        Files.writeString(file, "// CraftTweaker script");

        String before = GlobalIndexCache.dynamicScriptHash(gameDir);
        Files.writeString(file, "// CraftTweaker script with more content added");
        String after = GlobalIndexCache.dynamicScriptHash(gameDir);

        assertNotEquals(before, after, "Hash must change when file content (size) changes");
    }

    @Test
    void dynamicScriptHashIsStableForUnchangedFiles(@TempDir Path gameDir) throws IOException {
        Path dir = gameDir.resolve("groovy");
        Files.createDirectories(dir);
        Files.writeString(dir.resolve("postInit.groovy"), "// groovyscript");

        String first = GlobalIndexCache.dynamicScriptHash(gameDir);
        String second = GlobalIndexCache.dynamicScriptHash(gameDir);

        assertEquals(first, second, "Hash must be stable across repeated calls with no changes");
    }

    @Test
    void dynamicScriptHashCoversAllKnownDynamicDirs(@TempDir Path gameDir) throws IOException {
        // All declared dirs should contribute to the hash — verify each one independently
        for (String rel : GlobalIndexCache.DYNAMIC_SCRIPT_DIRS) {
            Path dir = gameDir.resolve(rel);
            Files.createDirectories(dir);
            Files.writeString(dir.resolve("test.txt"), rel); // unique content per dir

            String hash = GlobalIndexCache.dynamicScriptHash(gameDir);
            assertFalse(hash.isEmpty(), "Expected non-empty hash for dir: " + rel);

            // Remove the dir for the next iteration so dirs don't accumulate
            Files.delete(dir.resolve("test.txt"));
            Files.delete(dir);
            // also clean up parent if it was created and is now empty
            Path parent = dir.getParent();
            if (!parent.equals(gameDir) && Files.isDirectory(parent)) {
                try (var s = Files.list(parent)) {
                    if (s.findAny().isEmpty()) Files.delete(parent);
                }
            }
        }
    }

    @Test
    void dynamicScriptHashesAreDifferentAcrossDirs(@TempDir Path gameDir) throws IOException {
        // A file in kubejs/server_scripts and a file in scripts should produce different hashes
        // because the relative path is part of the hash input
        Path kubejsDir = gameDir.resolve("kubejs/server_scripts");
        Path ctDir = gameDir.resolve("scripts");
        Files.createDirectories(kubejsDir);
        Files.createDirectories(ctDir);

        Files.writeString(kubejsDir.resolve("file.js"), "same content");
        String kubeHash = GlobalIndexCache.dynamicScriptHash(gameDir);

        Files.delete(kubejsDir.resolve("file.js"));
        Files.writeString(ctDir.resolve("file.js"), "same content");
        String ctHash = GlobalIndexCache.dynamicScriptHash(gameDir);

        assertNotEquals(kubeHash, ctHash, "Same filename with same content in different dirs must hash differently");
    }

    // ---- normalizeLanguageCodeForCache ----

    @Test
    void normalizeLanguageCodeForCacheRewritesCommonFormats() {
        assertEquals("en_us", GlobalIndexCache.normalizeLanguageCodeForCache("en_US"));
        assertEquals("en_us", GlobalIndexCache.normalizeLanguageCodeForCache("en-US"));
    }

    @Test
    void normalizeLanguageCodeForCacheCleansUnsafeCharacters() {
        assertEquals("zh_cn", GlobalIndexCache.normalizeLanguageCodeForCache("zh#cn"));
        assertEquals("ru_ru", GlobalIndexCache.normalizeLanguageCodeForCache(" ru ru "));
    }

    @Test
    void normalizeLanguageCodeForCacheFallsBackForBlankInput() {
        assertEquals("en_us", GlobalIndexCache.normalizeLanguageCodeForCache(null));
        assertEquals("en_us", GlobalIndexCache.normalizeLanguageCodeForCache("   "));
    }

    @Test
    void normalizeLanguageCodeForCacheAcceptsMinecraftLocaleCodes() throws IOException {
        for (String locale : discoverMinecraftLocales()) {
            String normalized = GlobalIndexCache.normalizeLanguageCodeForCache(locale);
            assertFalse(normalized.isBlank(), "Locale should normalize to non-blank: " + locale);
            assertEquals(locale.trim().toLowerCase(Locale.ROOT).replace('-', '_'), normalized);
        }
    }

    private static List<String> discoverMinecraftLocales() throws IOException {
        List<String> fromAssetIndex = discoverLocalesFromGradleCache();
        if (!fromAssetIndex.isEmpty()) {
            return fromAssetIndex;
        }
        return readFallbackLocaleList();
    }

    private static List<String> discoverLocalesFromGradleCache() throws IOException {
        Optional<Path> path = findAssetIndexPath();
        if (path.isEmpty()) {
            return List.of();
        }
        return parseLocalesFromAssetIndex(path.get());
    }

    private static List<String> parseLocalesFromAssetIndex(Path path) throws IOException {
        String json = Files.readString(path, StandardCharsets.UTF_8);
        Matcher matcher = ASSET_INDEX_LANGUAGE_PATTERN.matcher(json);
        return matcher.results()
                .map(match -> match.group(1))
                .distinct()
                .toList();
    }

    private static List<String> readFallbackLocaleList() throws IOException {
        try (InputStream in = GlobalIndexCacheTest.class.getClassLoader().getResourceAsStream(ASSET_LOCALES_RESOURCE)) {
            assertNotNull(in, "Missing test resource: " + ASSET_LOCALES_RESOURCE);
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8))) {
                return reader.lines()
                        .map(String::trim)
                        .filter(line -> !line.isBlank() && !line.startsWith("#"))
                        .toList();
            }
        }
    }

    private static Optional<Path> findAssetIndexPath() {
        Path home = Paths.get(System.getProperty("user.home"));
        Path primary = home.resolve(".gradle/caches/minecraft/assets/indexes/asset-index.json");
        if (Files.isRegularFile(primary)) {
            return Optional.of(primary);
        }
        return scanForAssetIndex(home.resolve(".gradle/caches/ng_execute"));
    }

    private static Optional<Path> scanForAssetIndex(Path root) {
        if (!Files.isDirectory(root)) {
            return Optional.empty();
        }
        try (Stream<Path> paths = Files.find(root, ASSET_INDEX_SEARCH_DEPTH, (p, attrs) ->
                attrs.isRegularFile() && p.getFileName().toString().equals("asset-index.json"))) {
            return paths.filter(Files::isRegularFile)
                    .findFirst();
        } catch (IOException e) {
            return Optional.empty();
        }
    }
}
