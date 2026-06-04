package com.sanhiruzu.ami.client.input;

import org.junit.jupiter.api.Test;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TextInputFilterTest {
    private static final String ASSET_INDEX_RESOURCE_PATH = "ami/minecraft_locales.txt";
    private static final Pattern ASSET_INDEX_LANG_PATTERN = Pattern.compile("\"minecraft/lang/([^\".]+)\\.json\"");
    private static final int ASSET_INDEX_SEARCH_DEPTH = 8;

    private static final Map<String, String> LOCALE_SAMPLE_OVERRIDES = Map.ofEntries(
            Map.entry("af_za", "steen"),
            Map.entry("ar_sa", "حجر"),
            Map.entry("be_by", "камень"),
            Map.entry("be_latn", "kamen"),
            Map.entry("bg_bg", "камък"),
            Map.entry("es_ar", "piedra"),
            Map.entry("es_cl", "piedra"),
            Map.entry("es_ec", "piedra"),
            Map.entry("es_es", "piedra"),
            Map.entry("es_mx", "piedra"),
            Map.entry("es_uy", "piedra"),
            Map.entry("es_ve", "piedra"),
            Map.entry("esan", "stène"),
            Map.entry("eu_es", "harri"),
            Map.entry("fa_ir", "سنگ"),
            Map.entry("fi_fi", "kivi"),
            Map.entry("fr_ca", "pierre"),
            Map.entry("fr_ch", "pierre"),
            Map.entry("fr_fr", "pierre"),
            Map.entry("fra_de", "Stein"),
            Map.entry("fy_nl", "stien"),
            Map.entry("ga_ie", "cloch"),
            Map.entry("gd_gb", "clach"),
            Map.entry("gl_es", "pedra"),
            Map.entry("go_fr", "pierre"),
            Map.entry("he_il", "אבן"),
            Map.entry("hi_in", "पत्थर"),
            Map.entry("hy_am", "քար"),
            Map.entry("id_id", "batu"),
            Map.entry("io_en", "ŝtono"),
            Map.entry("it_it", "pietra"),
            Map.entry("ja_jp", "ダイヤモンド"),
            Map.entry("jbo_en", "stone"),
            Map.entry("ka_ge", "ქვა"),
            Map.entry("kk_kz", "тас"),
            Map.entry("kn_in", "ಕಲ್ಲು"),
            Map.entry("ko_kr", "돌"),
            Map.entry("ky_kg", "таш"),
            Map.entry("la_la", "lapis"),
            Map.entry("lol_us", "stõn"),
            Map.entry("lmo", "petra"),
            Map.entry("lo_la", "ຫີນ"),
            Map.entry("lt_lt", "akmuo"),
            Map.entry("lv_lv", "akmens"),
            Map.entry("lzh", "石"),
            Map.entry("mk_mk", "камен"),
            Map.entry("mn_mn", "чулуу"),
            Map.entry("ms_my", "batu"),
            Map.entry("mt_mt", "ġebel"),
            Map.entry("nds_de", "stain"),
            Map.entry("nl_be", "steen"),
            Map.entry("nl_nl", "steen"),
            Map.entry("nn_no", "stein"),
            Map.entry("no_no", "stein"),
            Map.entry("oc_fr", "pèira"),
            Map.entry("ovd", "kámen"),
            Map.entry("pl_pl", "kamień"),
            Map.entry("pls", "stun"),
            Map.entry("pt_br", "pedra"),
            Map.entry("pt_pt", "pedra"),
            Map.entry("qcb_es", "piedra"),
            Map.entry("qid", "stone"),
            Map.entry("qya_aa", "kamen"),
            Map.entry("ro_ro", "piatra"),
            Map.entry("ru_ru", "камень"),
            Map.entry("rpr", "kamen"),
            Map.entry("ry_ua", "камінь"),
            Map.entry("sah_sah", "тоҕут"),
            Map.entry("se_no", "sijes"),
            Map.entry("sk_sk", "kameň"),
            Map.entry("sl_si", "kamen"),
            Map.entry("so_so", "dhagax"),
            Map.entry("sq_al", "gur"),
            Map.entry("sr_cs", "камен"),
            Map.entry("sr_sp", "камень"),
            Map.entry("sv_se", "sten"),
            Map.entry("sxu", "qapi"),
            Map.entry("szl", "kamyń"),
            Map.entry("ta_in", "கல்"),
            Map.entry("th_th", "หิน"),
            Map.entry("tl_ph", "bato"),
            Map.entry("tlh_aa", "ghItlh"),
            Map.entry("tok", "sitelen"),
            Map.entry("tr_tr", "taş"),
            Map.entry("tt_ru", "таш"),
            Map.entry("tzo_mx", "piedra"),
            Map.entry("uk_ua", "камінь"),
            Map.entry("uz_uz", "tosh"),
            Map.entry("val_es", "pedra"),
            Map.entry("vec_it", "piera"),
            Map.entry("vi_vn", "đá"),
            Map.entry("vp_vl", "piedra"),
            Map.entry("vro", "piatră"),
            Map.entry("yi_de", "שטיין"),
            Map.entry("yo_ng", "okuta"),
            Map.entry("zh_cn", "石"),
            Map.entry("zh_hk", "石"),
            Map.entry("zh_tw", "石"),
            Map.entry("zlm_arab", "بته")
    );

    @Test
    void acceptsAsciiWordsAndSymbols() {
        assertTrue(TextInputFilter.isAllowedInput("diamond sword"));
        assertTrue(TextInputFilter.isAllowedInput("modid:my_item"));
        assertTrue(TextInputFilter.isAllowedInput("tag=#minecraft"));
    }

    @Test
    void acceptsUnicodeScripts() {
        assertTrue(TextInputFilter.isAllowedInput("кирпич"));
        assertTrue(TextInputFilter.isAllowedInput("钻石"));
        assertTrue(TextInputFilter.isAllowedInput("木工台"));
        assertTrue(TextInputFilter.isAllowedInput("ダイヤモンド"));
        assertTrue(TextInputFilter.isAllowedInput("\uFF76\uFF80\uFF76\uFF85"));
    }

    @Test
    void acceptsHalfAndFullWidthVariants() {
        assertTrue(TextInputFilter.isAllowedInput("ｽﾄｰﾝ")); // full-width + half-width kana form
        assertTrue(TextInputFilter.isAllowedInput("ｔｈａｎｋｓ"));
        assertTrue(TextInputFilter.isAllowedInput("ﾀｲｱﾓﾝﾄﾞ"));
    }

    @Test
    void acceptsSampleForEveryMinecraftLocale() throws IOException {
        List<String> locales = discoverMinecraftLocales();
        assertFalse(locales.isEmpty(), "Minecraft locale fixture should provide at least one locale");

        for (String locale : locales) {
            String sample = sampleForLocale(locale);
            assertFalse(sample.isBlank(), "Locale sample must not be empty for " + locale);
            assertTrue(TextInputFilter.isAllowedInput(sample), () -> "Locale " + locale + " sample should be accepted: " + sample);
        }
    }

    @Test
    void rejectsControlCharacters() {
        assertFalse(TextInputFilter.isAllowedInput("\n"));
        assertFalse(TextInputFilter.isAllowedInput("\u0000"));
        assertFalse(TextInputFilter.isAllowedInput("\u007F"));
    }

    private static List<String> discoverMinecraftLocales() throws IOException {
        List<String> fromAssetIndex = discoverLocalesFromGradleCache();
        if (!fromAssetIndex.isEmpty()) {
            return fromAssetIndex;
        }
        return readFallbackLocaleList();
    }

    private static List<String> discoverLocalesFromGradleCache() throws IOException {
        java.util.Optional<Path> path = findAssetIndexPath();
        if (path.isEmpty()) {
            return List.of();
        }
        return parseLocalesFromAssetIndex(path.get());
    }

    private static List<String> parseLocalesFromAssetIndex(Path path) throws IOException {
        String json = Files.readString(path, StandardCharsets.UTF_8);
        Matcher matcher = ASSET_INDEX_LANG_PATTERN.matcher(json);
        LinkedHashSet<String> locales = new LinkedHashSet<>();
        while (matcher.find()) {
            locales.add(matcher.group(1));
        }
        return locales.stream().toList();
    }

    private static List<String> readFallbackLocaleList() throws IOException {
        try (InputStream in = TextInputFilterTest.class.getClassLoader().getResourceAsStream(ASSET_INDEX_RESOURCE_PATH)) {
            assertNotNull(in, "Missing test resource: " + ASSET_INDEX_RESOURCE_PATH);
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8))) {
                return reader.lines()
                        .map(String::trim)
                        .filter(line -> !line.isBlank() && !line.startsWith("#"))
                        .toList();
            }
        }
    }

    private static java.util.Optional<Path> findAssetIndexPath() {
        Path home = Paths.get(System.getProperty("user.home"));
        Path primary = home.resolve(".gradle/caches/minecraft/assets/indexes/asset-index.json");
        if (Files.isRegularFile(primary)) {
            return java.util.Optional.of(primary);
        }
        return scanForAssetIndex(home.resolve(".gradle/caches/ng_execute"));
    }

    private static java.util.Optional<Path> scanForAssetIndex(Path root) {
        if (!Files.isDirectory(root)) {
            return java.util.Optional.empty();
        }
        try (Stream<Path> paths = Files.find(root, ASSET_INDEX_SEARCH_DEPTH, (p, attrs) ->
                attrs.isRegularFile() && p.getFileName().toString().equals("asset-index.json"))) {
            return paths.filter(Files::isRegularFile)
                    .findFirst();
        } catch (IOException e) {
            return java.util.Optional.empty();
        }
    }

    private static String sampleForLocale(String locale) {
        String exact = LOCALE_SAMPLE_OVERRIDES.get(locale);
        if (exact != null) {
            return exact;
        }

        String language = locale.split("_", 2)[0].toLowerCase(Locale.ROOT);
        return switch (language) {
            case "ru", "be", "bg", "mk", "sr", "kk", "ky", "tt", "uz", "sah", "uk", "mn" -> "камень";
            case "ar", "fa", "zlm" -> "حجر";
            case "he", "yi" -> "אבן";
            case "ja", "zh", "lzh" -> "石";
            case "ko" -> "돌";
            case "th" -> "หิน";
            case "hi", "ta", "kn" -> "பாறை";
            case "lo" -> "ຫີນ";
            case "el" -> "πέτρα";
            case "hy", "ka" -> "ქვა";
            case "en" -> "stone";
            default -> "stone";
        };
    }
}
