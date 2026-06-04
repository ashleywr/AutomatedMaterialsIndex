package com.sanhiruzu.ami.index;

import org.junit.jupiter.api.Test;

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
import static org.junit.jupiter.api.Assertions.assertNotNull;

class GlobalIndexCacheTest {
    private static final String ASSET_LOCALES_RESOURCE = "ami/minecraft_locales.txt";
    private static final Pattern ASSET_INDEX_LANGUAGE_PATTERN = Pattern.compile("\"minecraft/lang/([^\".]+)\\.json\"");
    private static final int ASSET_INDEX_SEARCH_DEPTH = 8;

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
