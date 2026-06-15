package com.sanhiruzu.ami.compat;

import com.sanhiruzu.ami.index.SearchNodeKeys;
import net.minecraft.resources.ResourceLocation;

import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Predicate;

/**
 * Focused identity routing for the Society pack mod. Most Society items are KubeJS
 * custom items ({@code BasicItemJS}) with no tags, facets, or creative-tab signals,
 * so classification relies on path-token patterns derived from the pack's naming
 * conventions.
 */
public final class SocietyCompat {
    private static final String MOD_ID = "society";

    private static final List<ItemRule> ITEM_RULES = List.of(
            rule("artisan_goods", SocietyCompat::isArtisanGood, context -> {
                kind(context, "artisan_goods");
                tokens(context, "artisan aged smoked dried pickled preserved wine cheese");
                collapse(context, "society:artisan_goods", "Artisan Goods");
            }),
            rule("fishing", SocietyCompat::isFishing, context -> {
                kind(context, "fishing");
                tokens(context, "fishing bait roe bobber fish");
                collapse(context, "society:fishing", "Fishing");
            }),
            rule("gems", SocietyCompat::isGem, context -> {
                kind(context, "gem");
                tokens(context, "gem mineral geode crystal pristine stone");
                collapse(context, "society:gems", "Gems & Minerals");
            }),
            rule("machines", SocietyCompat::isSocietyMachine, context -> {
                kind(context, "machine");
                tokens(context, "machine station press maker keg jar recycler");
                collapse(context, "society:machines", "Machines");
            }),
            rule("farming", SocietyCompat::isFarming, context -> {
                kind(context, "farming");
                tokens(context, "farming seed crop animal milk egg feed fertilizer");
                collapse(context, "society:farming", "Farming");
            }),
            rule("decoration", SocietyCompat::isSocietyDecor, context -> {
                kind(context, "decoration");
                tokens(context, "decoration sheet wallpaper baseboard plushie furniture");
                collapse(context, "society:decoration", "Decoration");
            }),
            rule("books", SocietyCompat::isSocietyBook, context -> {
                kind(context, "book");
                tokens(context, "book guide tome manual");
                collapse(context, "society:books", "Books");
            }),
            rule("misc", context -> true, context -> {
                kind(context, "misc");
                tokens(context, "society");
            })
    );

    private SocietyCompat() {
    }

    public static void enrichItem(ResourceLocation id, Map<String, String> meta) {
        if (id == null || !MOD_ID.equals(id.getNamespace())) {
            return;
        }
        ItemContext context = new ItemContext(
                id,
                id.getPath().toLowerCase(Locale.ROOT),
                meta.getOrDefault(SearchNodeKeys.CREATIVE_TAB_LABEL, "").toLowerCase(Locale.ROOT),
                meta.getOrDefault(SearchNodeKeys.ITEM_CLASS, "").toLowerCase(Locale.ROOT),
                meta.getOrDefault(SearchNodeKeys.TAGS, "").toLowerCase(Locale.ROOT),
                meta
        );
        tokens(context, "society pack custom");

        for (ItemRule rule : ITEM_RULES) {
            if (rule.matches.test(context)) {
                rule.enrich.accept(context);
                return;
            }
        }
    }

    private static boolean isArtisanGood(ItemContext context) {
        return context.path.startsWith("aged_") || context.path.startsWith("double_aged_")
                || context.path.startsWith("smoked_") || context.path.startsWith("dried_")
                || context.path.startsWith("pickled_") || context.path.contains("preserves")
                || context.path.endsWith("_wine") || context.path.contains("_cheese_")
                || context.path.endsWith("_cheese") || context.path.contains("juice")
                || context.path.contains("_roe") && context.path.contains("_aged");
    }

    private static boolean isFishing(ItemContext context) {
        return context.path.endsWith("_bait") || (context.path.endsWith("_roe") && !isArtisanGood(context))
                || context.path.contains("bobber") || context.path.contains("fishing")
                || context.path.equals("bait_maker") || context.path.equals("fish_smoker")
                || context.path.equals("fish_radar") || context.path.equals("fish_pond_basket")
                || context.path.equals("fish_pond") || context.path.equals("roe_recycler")
                || context.path.equals("charging_rod") || context.path.contains("_bobber");
    }

    private static boolean isGem(ItemContext context) {
        return context.path.startsWith("pristine_") || context.path.contains("_geode")
                || context.path.endsWith("_geode") || context.path.contains("_crystal_")
                || context.path.endsWith("_crystal") || context.path.endsWith("_gem")
                || context.path.endsWith("_shard") || context.path.contains("prismatic")
                || context.path.equals("sparkstone") || context.path.equals("sparkstone_block")
                || context.path.equals("sparkstone_ore") || context.path.equals("sparkstone_recycler")
                || context.path.equals("geode_buster") || context.path.equals("geode_node")
                || context.path.equals("geode") || context.path.equals("iridium_ore")
                || context.path.equals("deepslate_iridium_ore") || context.path.equals("deepslate_sparkstone_ore")
                || context.path.equals("iridium_clock") || context.path.equals("golden_clock")
                || context.path.equals("broken_clock") || context.path.equals("crystalarium");
    }

    private static boolean isSocietyMachine(ItemContext context) {
        return context.path.contains("_machine") || context.path.contains("_maker")
                || context.path.contains("_press") || context.path.endsWith("_keg")
                || context.path.endsWith("_jar") || context.path.contains("recycler")
                || context.path.contains("dehydrator") || context.path.contains("_inserter")
                || context.path.contains("_hopper") || context.path.contains("_cask")
                || context.path.equals("tapper") || context.path.equals("loom")
                || context.path.equals("seed_maker") || context.path.equals("oil_maker")
                || context.path.equals("snow_melter") || context.path.contains("_machine");
    }

    private static boolean isFarming(ItemContext context) {
        return context.path.endsWith("_seed") || context.path.contains("_crop")
                || context.path.endsWith("_milk") || context.path.endsWith("_egg")
                || context.path.contains("_feed") || context.path.contains("fertilizer")
                || context.path.endsWith("_wool") || context.path.endsWith("_manure")
                || context.path.contains("animal_") || context.path.contains("_animal")
                || context.path.equals("milk_pail") || context.path.equals("milk_bucket")
                || context.path.equals("milk") || context.path.contains("mayonnaise")
                || context.path.contains("_milk") || context.path.equals("growth_obelisk")
                || context.path.equals("growth_obelisk_upper") || context.path.equals("spark_gro")
                || context.path.contains("canvas") || context.path.contains("_resin")
                || context.path.contains("_syrup") || context.path.contains("_tar")
                || context.path.equals("sap") || context.path.equals("rubber");
    }

    private static boolean isSocietyDecor(ItemContext context) {
        return context.path.endsWith("_sheet") || context.path.contains("wallpaper")
                || context.path.contains("baseboard") || context.path.contains("plushie")
                || context.path.contains("furniture") || context.path.contains("catalog")
                || context.path.contains("gnome") || context.path.contains("lantern")
                || context.path.contains("bouquet");
    }

    private static boolean isSocietyBook(ItemContext context) {
        return context.path.startsWith("book_") || context.path.contains("_guide")
                || context.path.contains("_digest") || context.path.contains("catalog");
    }

    private static ItemRule rule(String id, Predicate<ItemContext> matches, Consumer<ItemContext> enrich) {
        return new ItemRule(id, matches, enrich);
    }

    private static void kind(ItemContext context, String kind) {
        context.meta.put(SearchNodeKeys.SOCIETY_ITEM_KIND, kind);
    }

    private static void collapse(ItemContext context, String key, String label) {
        context.meta.put(SearchNodeKeys.COLLAPSE_FAMILY, key);
        context.meta.put(SearchNodeKeys.COLLAPSE_LABEL, label);
    }

    private static void tokens(ItemContext context, String tokens) {
        String existing = context.meta.getOrDefault(SearchNodeKeys.SEARCH_TOKENS, "");
        if (existing.isBlank()) {
            context.meta.put(SearchNodeKeys.SEARCH_TOKENS, tokens);
            return;
        }
        String merged = existing;
        for (String token : tokens.split("\\s+")) {
            if (!containsToken(merged, token)) {
                merged += " " + token;
            }
        }
        context.meta.put(SearchNodeKeys.SEARCH_TOKENS, merged);
    }

    private static boolean containsToken(String encoded, String token) {
        for (String part : encoded.split("\\s+")) {
            if (part.equals(token)) {
                return true;
            }
        }
        return false;
    }

    private record ItemContext(ResourceLocation id, String path, String creativeTab, String itemClass, String tags,
                               Map<String, String> meta) {
        private boolean inTab(String text) {
            return creativeTab.contains(text);
        }

        private boolean hasTag(String tag) {
            return tags.contains(tag);
        }
    }

    private record ItemRule(String id, Predicate<ItemContext> matches, Consumer<ItemContext> enrich) {
    }
}
