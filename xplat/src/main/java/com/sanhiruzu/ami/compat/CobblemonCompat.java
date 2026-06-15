package com.sanhiruzu.ami.compat;

import com.sanhiruzu.ami.index.SearchNodeKeys;
import net.minecraft.resources.ResourceLocation;

import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Predicate;

public final class CobblemonCompat {
    private static final String MOD_ID = "cobblemon";

    private static final Map<String, String> HEALING_BY_ITEM = Map.of(
            "potion", "20",
            "sweet_heart", "20",
            "super_potion", "60",
            "hyper_potion", "120"
    );
    private static final Map<String, String> STATUS_CURE_BY_ITEM = Map.of(
            "antidote", "poison",
            "awakening", "sleep",
            "burn_heal", "burn",
            "ice_heal", "freeze",
            "paralyze_heal", "paralysis",
            "full_heal", "all"
    );
    private static final Set<String> PP_RESTORES = Set.of(
            "ether", "max_ether", "elixir", "max_elixir"
    );
    private static final Set<String> REVIVES = Set.of("revive", "max_revive");
    private static final Set<String> BATTLE_ITEMS = Set.of(
            "x_attack", "x_defense", "x_speed", "x_accuracy", "x_special_attack",
            "x_special_defense", "x_defence", "x_special_defence", "dire_hit", "guard_spec"
    );
    private static final Set<String> VITAMINS = Set.of(
            "protein", "iron", "zinc", "calcium", "carbos", "hp_up"
    );
    private static final Set<String> MACHINES = Set.of(
            "pc", "healing_machine", "fossil_analyzer", "restoration_tank",
            "pasture", "monitor"
    );
    private static final Set<String> POKE_BALL_EXCLUSIONS = Set.of(
            "iron_ball", "smoke_ball", "light_ball", "cell_battery"
    );
    private static final Set<String> APRICORN_ITEMS = Set.of(
            "red_apricorn", "yellow_apricorn", "green_apricorn", "blue_apricorn",
            "pink_apricorn", "black_apricorn", "white_apricorn"
    );
    private static final List<ItemRule> ITEM_RULES = List.of(
            rule("poke_balls", CobblemonCompat::isPokeBall, CobblemonCompat::enrichPokeBall),
            rule("medicine", CobblemonCompat::isMedicine, CobblemonCompat::enrichMedicine),
            rule("tms", CobblemonCompat::isTM, context -> {
                kind(context, "tm");
                tokens(context, "tm tr technical_machine technical_record move_tutor move");
                collapse(context, "cobblemon:tms", "TMs & TRs");
            }),
            rule("held_items", CobblemonCompat::isHeldItem, CobblemonCompat::enrichHeldItem),
            rule("berries", CobblemonCompat::isBerry, context -> {
                kind(context, "berry");
                context.meta.put(SearchNodeKeys.VARIANT_GROUP, "pokemon_berry");
                tokens(context, "berry berries pokemon_berry " + berryFamily(context.path));
                collapse(context, "cobblemon:berries", "Berries");
            }),
            rule("apricorns", CobblemonCompat::isApricorn, context -> {
                kind(context, context.path.endsWith("_seed") || context.path.contains("sapling")
                        ? "apricorn_seed"
                        : "apricorn");
                String color = apricornColor(context.path);
                if (!color.isBlank()) {
                    context.meta.put(SearchNodeKeys.COLOR_BUCKET, color);
                }
                context.meta.put(SearchNodeKeys.VARIANT_GROUP, "apricorn");
                tokens(context, "apricorn apricorns " + color);
                collapse(context, "cobblemon:apricorns", "Apricorns");
            }),
            rule("evolution_items", CobblemonCompat::isEvolutionItem, context -> {
                kind(context, "evolution_item");
                String trigger = evolutionTrigger(context.path);
                context.meta.put(SearchNodeKeys.POKEMON_EVOLUTION_TRIGGER, trigger);
                context.meta.put(SearchNodeKeys.VARIANT_GROUP, "pokemon_evolution_" + trigger);
                tokens(context, "evolution evolution_item evolve");
                collapse(context, "cobblemon:evolution_items", "Evolution Items");
            }),
            rule("fossils", CobblemonCompat::isFossil, context -> {
                kind(context, "fossil");
                tokens(context, "fossil fossils archaeology");
                collapse(context, "cobblemon:fossils", "Fossils");
            }),
            rule("machines", CobblemonCompat::isMachine, context -> {
                kind(context, "machine");
                tokens(context, "pokemon_machine machine station");
                collapse(context, "cobblemon:machines", "Pokemon Machines");
            }),
            rule("decor", CobblemonCompat::isDecor, context -> {
                kind(context, "decor");
                tokens(context, "pokemon_decor decor display");
            }),
            rule("transport", CobblemonCompat::isTransport, context -> {
                kind(context, "transport");
                tokens(context, "pokemon_transport transport boat chest_boat");
                collapse(context, "cobblemon:transport", "Transport");
            }),
            rule("utility", CobblemonCompat::isUtility, context -> {
                kind(context, "utility_item");
                tokens(context, "pokemon_utility utility pokedex poke_rod pokerod fishing_rod");
            }),
            rule("consumables", CobblemonCompat::isConsumable, context -> {
                kind(context, "consumable");
                tokens(context, "pokemon_consumable consumable candy sweet food drink");
            }),
            rule("agriculture", CobblemonCompat::isAgriculture, context -> {
                kind(context, "agriculture");
                tokens(context, "pokemon_agriculture agriculture herb mulch mint");
            }),
            rule("building", CobblemonCompat::isBuilding, context -> {
                kind(context, "building");
                tokens(context, "pokemon_building building block");
            }),
            rule("archaeology", CobblemonCompat::isArchaeology, context -> {
                kind(context, "archaeology");
                tokens(context, "pokemon_archaeology archaeology relic sherd tumblestone");
            }),
            rule("misc", context -> true, context -> {
                kind(context, "misc");
                tokens(context, "pokemon cobblemon");
            })
    );

    private CobblemonCompat() {
    }

    public static void enrichItem(ResourceLocation id, Map<String, String> meta) {
        if (id == null) {
            return;
        }
        if (meta.getOrDefault(SearchNodeKeys.COMPAT_FAMILIES, "").isBlank()) {
            CompatFamilyDetector.detect(id, meta);
        }

        ItemContext context = new ItemContext(
                id,
                id.getPath().toLowerCase(Locale.ROOT),
                meta.getOrDefault(SearchNodeKeys.CREATIVE_TAB_LABEL, "").toLowerCase(Locale.ROOT),
                meta.getOrDefault(SearchNodeKeys.ITEM_CLASS, "").toLowerCase(Locale.ROOT),
                meta.getOrDefault(SearchNodeKeys.TAGS, "").toLowerCase(Locale.ROOT),
                meta
        );
        if (!isCobblemonEcosystemItem(context)) {
            return;
        }
        tokens(context, "cobblemon pokemon");

        for (ItemRule rule : ITEM_RULES) {
            if (rule.matches.test(context)) {
                rule.enrich.accept(context);
                return;
            }
        }
    }

    private static boolean isCobblemonEcosystemItem(ItemContext context) {
        return MOD_ID.equals(context.id.getNamespace())
                || CompatFamilyDetector.hasFamily(context.meta, CompatFamilyDetector.COBBLEMON);
    }

    private static boolean isPokeBall(ItemContext context) {
        return context.itemClass.endsWith("pokeballitem")
                || context.path.contains("ball_lid")
                || context.path.contains("ball_base")
                || context.path.contains("ball_mechanism")
                || context.path.endsWith("_lid")
                || context.path.contains("stamp") && context.path.contains("lid")
                || context.path.contains("apricorn_punch")
                || context.itemClass.contains(".items.balls.")
                || (context.path.endsWith("_ball") && !POKE_BALL_EXCLUSIONS.contains(context.path));
    }

    private static void enrichPokeBall(ItemContext context) {
        kind(context, "poke_ball");
        context.meta.put(SearchNodeKeys.POKEMON_BALL_FAMILY, context.path.startsWith("ancient_") ? "ancient" : "standard");
        context.meta.put(SearchNodeKeys.POKEMON_BALL_TIER, ballTier(context.path));
        context.meta.put(SearchNodeKeys.VARIANT_GROUP, "poke_ball");
        tokens(context, "pokeball poke_ball capture ball");
        collapse(context, "cobblemon:poke_balls", "Poke Balls");
    }

    private static String ballTier(String path) {
        path = path.replace("incomplete_", "")
                .replace("_ball_lid", "_ball")
                .replace("_ball_base", "_ball")
                .replace("_ball_mechanism", "_ball");
        if (path.contains("master_ball")) return "master";
        if (path.contains("ultra_ball")) return "ultra";
        if (path.contains("great_ball")) return "great";
        if (path.contains("poke_ball")) return "poke";
        if (path.contains("beast_ball")) return "beast";
        if (path.contains("gigaton_ball")) return "gigaton";
        if (path.contains("heavy_ball")) return "heavy";
        if (path.contains("jet_ball")) return "jet";
        if (path.contains("wing_ball")) return "wing";
        if (path.contains("leaden_ball")) return "leaden";
        if (path.contains("feather_ball")) return "feather";
        return path.replace("ancient_", "").replace("_ball", "");
    }

    private static boolean isMedicine(ItemContext context) {
        String medicinePath = medicinePath(context.path);
        // Path-strong matches that don't need creative-tab evidence.
        // Stat-modifying candies that aren't real CandyItem-class items (rare_candy, exp_candy)
        // are medicine; real CandyItem-class candies stay in consumables.
        boolean pathStrong = medicinePath.equals("sweet_heart")
                || medicinePath.endsWith("_mochi")
                || medicinePath.endsWith("_mint")
                || (medicinePath.endsWith("_candy") && !context.itemClass.contains("candyitem"))
                || VITAMINS.contains(medicinePath);
        if (pathStrong) return true;
        return (context.inTab("consumables")
                && (medicinePath.contains("potion")
                || STATUS_CURE_BY_ITEM.containsKey(medicinePath)
                || PP_RESTORES.contains(medicinePath)
                || REVIVES.contains(medicinePath)
                || BATTLE_ITEMS.contains(medicinePath)
                || medicinePath.equals("remedy")
                || medicinePath.equals("superb_remedy")
                || medicinePath.equals("poke_bait")
                || medicinePath.equals("sweet_heart")
                || context.itemClass.contains("potionitem")
                || context.itemClass.contains("statuscureitem")
                || context.itemClass.contains("etheritem")
                || context.itemClass.contains("elixiritem")
                || context.itemClass.contains("xstatitem")
                || context.itemClass.contains("guardspec")
                || context.itemClass.contains("direhit")))
                || (context.isNamespace("create_cobblemon_potion")
                && (medicinePath.contains("potion")
                || medicinePath.equals("medicinal_brew")
                || medicinePath.equals("full_restore")
                || STATUS_CURE_BY_ITEM.containsKey(medicinePath)
                || PP_RESTORES.contains(medicinePath)));
    }

    private static void enrichMedicine(ItemContext context) {
        kind(context, "medicine");
        classifyMedicine(medicinePath(context.path), context.meta);
        String medicineKind = context.meta.getOrDefault(SearchNodeKeys.POKEMON_MEDICINE_KIND, "");
        String statusCure = context.meta.getOrDefault(SearchNodeKeys.POKEMON_STATUS_CURE, "");
        context.meta.put(SearchNodeKeys.VARIANT_GROUP, "pokemon_medicine");
        tokens(context, "medicine pokemon_medicine heal " + medicineKind + " " + statusCure);
        collapse(context, "cobblemon:medicine", "Pokemon Medicine");
    }

    private static void classifyMedicine(String path, Map<String, String> meta) {
        if (HEALING_BY_ITEM.containsKey(path) || path.contains("potion") || path.equals("full_restore")) {
            meta.put(SearchNodeKeys.POKEMON_MEDICINE_KIND, "heal");
            String healing = HEALING_BY_ITEM.get(path);
            if (healing != null) {
                meta.put(SearchNodeKeys.POKEMON_HEALING, healing);
            }
            if (path.equals("full_restore")) {
                meta.put(SearchNodeKeys.POKEMON_STATUS_CURE, "all");
            }
            return;
        }
        if (STATUS_CURE_BY_ITEM.containsKey(path)) {
            meta.put(SearchNodeKeys.POKEMON_MEDICINE_KIND, "status_cure");
            meta.put(SearchNodeKeys.POKEMON_STATUS_CURE, STATUS_CURE_BY_ITEM.get(path));
        } else if (REVIVES.contains(path)) {
            meta.put(SearchNodeKeys.POKEMON_MEDICINE_KIND, "revive");
        } else if (PP_RESTORES.contains(path)) {
            meta.put(SearchNodeKeys.POKEMON_MEDICINE_KIND, "pp_restore");
        } else if (BATTLE_ITEMS.contains(path)) {
            meta.put(SearchNodeKeys.POKEMON_MEDICINE_KIND, "battle_stat_boost");
        } else if (path.endsWith("_mint")) {
            meta.put(SearchNodeKeys.POKEMON_MEDICINE_KIND, "mint");
        } else if (path.endsWith("_mochi") || VITAMINS.contains(path)) {
            meta.put(SearchNodeKeys.POKEMON_MEDICINE_KIND, "vitamin");
        } else {
            meta.put(SearchNodeKeys.POKEMON_MEDICINE_KIND, "medicine");
        }
    }

    private static String medicinePath(String path) {
        return path.endsWith("_bucket") ? path.substring(0, path.length() - "_bucket".length()) : path;
    }

    private static boolean isBerry(ItemContext context) {
        return context.path.endsWith("_berry") || context.path.contains("_berry_") || context.path.contains("berry_");
    }

    private static String berryFamily(String path) {
        if (path.endsWith("_berry")) {
            return path.substring(0, path.length() - "_berry".length());
        }
        return path.replace("_berry", "").replace("berry_", "");
    }

    private static boolean isApricorn(ItemContext context) {
        return APRICORN_ITEMS.contains(context.path)
                || context.path.endsWith("_apricorn_seed")
                || context.path.contains("_apricorn_bits")
                || context.path.startsWith("half_") && context.path.endsWith("_apricorn")
                || context.itemClass.endsWith("apricornitem");
    }

    private static String apricornColor(String path) {
        for (String color : List.of("red", "yellow", "green", "blue", "pink", "black", "white")) {
            if (path.startsWith(color + "_apricorn") || path.contains("_" + color + "_apricorn")) {
                return color;
            }
        }
        return "";
    }

    private static boolean isEvolutionItem(ItemContext context) {
        return context.inTab("evolution items")
                || (context.isNamespace("mega_showdown") && !isMegaShowdownUtility(context)
                && (context.inTab("mega evolution")
                || context.inTab("form changing")
                || context.itemClass.contains("megastone")
                || context.itemClass.contains("fusion")
                || context.itemClass.contains("formchange")))
                || context.path.endsWith("_stone")
                || context.path.contains("_stone_")
                || context.path.endsWith("_armor")
                || context.path.startsWith("scroll_of_")
                || context.path.endsWith("_pot")
                || context.path.equals("prism_scale")
                || context.path.equals("protector")
                || context.path.equals("magmarizer")
                || context.path.equals("electirizer")
                || context.path.equals("reaper_cloth")
                || context.path.equals("dragon_scale")
                || context.path.equals("sachet")
                || context.path.equals("whipped_dream")
                || context.path.equals("galarica_cuff")
                || context.path.equals("galarica_wreath");
    }

    private static String evolutionTrigger(String path) {
        if (path.endsWith("_stone") || path.contains("_stone_")) return "stone";
        if (path.endsWith("_armor")) return "armor";
        if (path.startsWith("scroll_of_")) return "scroll";
        if (path.endsWith("_pot")) return "pot";
        if (path.contains("fossil")) return "fossil";
        return "held_item";
    }

    private static boolean isFossil(ItemContext context) {
        return context.path.contains("fossil");
    }

    private static boolean isMachine(ItemContext context) {
        return (context.isNamespace("cobblefurnies") && context.path.contains("crafter"))
                || context.inTab("blocks")
                && (MACHINES.contains(context.path)
                || context.path.contains("machine")
                || context.path.contains("analyzer"))
                || (context.isNamespace("cobblemon_farmers")
                && (context.path.contains("station") || context.path.contains("mine")));
    }

    private static boolean isDecor(ItemContext context) {
        return context.isNamespace("cobblefurnies")
                || context.inTab("colored blocks")
                || context.path.contains("pedestal")
                || context.path.contains("plaque")
                || context.path.contains("tatami")
                || context.path.contains("campfire_pot")
                || context.path.contains("display_case");
    }

    private static boolean isTM(ItemContext context) {
        return context.isNamespace("simpletms")
                && (context.itemClass.contains("movetutoritem")
                || context.itemClass.contains("blanktmitem")
                || context.path.startsWith("tm_")
                || context.path.startsWith("tr_"));
    }

    private static boolean isHeldItem(ItemContext context) {
        return context.inTab("held items")
                || context.hasTag("cobblemon:held/is_held_item")
                || (context.isNamespace("mega_showdown")
                && (context.hasTag("mega_showdown:z_crystal")
                || context.hasTag("mega_showdown:tera_shard")
                || context.itemClass.contains("zcrystal")
                || context.itemClass.contains("terashard")
                || context.path.endsWith("_orb")
                || context.path.endsWith("_plate")
                || context.path.equals("soul_dew")
                || context.path.equals("booster_energy")))
                || (context.isNamespace("fightorflight") && context.path.contains("lucky_egg"))
                || context.path.endsWith("_gem");
    }

    private static void enrichHeldItem(ItemContext context) {
        kind(context, "held_item");
        if (context.path.startsWith("choice_")) {
            context.meta.put(SearchNodeKeys.POKEMON_HELD_ITEM_ROLE, "choice");
        } else if (context.path.startsWith("power_") || context.path.contains("training") || context.path.equals("macho_brace")) {
            context.meta.put(SearchNodeKeys.POKEMON_HELD_ITEM_ROLE, "training");
        } else if (context.isNamespace("mega_showdown")) {
            context.meta.put(SearchNodeKeys.POKEMON_HELD_ITEM_ROLE, context.hasTag("mega_showdown:tera_shard") || context.itemClass.contains("terashard")
                    ? "tera"
                    : context.hasTag("mega_showdown:z_crystal") || context.itemClass.contains("zcrystal")
                    ? "z_crystal"
                    : "battle");
        } else if (context.path.endsWith("_gem")) {
            context.meta.put(SearchNodeKeys.POKEMON_HELD_ITEM_ROLE, "type_boost");
            context.meta.put(SearchNodeKeys.POKEMON_TYPE, context.path.substring(0, context.path.length() - "_gem".length()));
        } else {
            context.meta.put(SearchNodeKeys.POKEMON_HELD_ITEM_ROLE, "battle");
        }
        tokens(context, "held_item helditem pokemon_held_item");
        collapse(context, "cobblemon:held_items", "Held Items");
    }

    private static boolean isUtility(ItemContext context) {
        return context.inTab("utility items")
                || context.isNamespace("badgebox")
                || context.isNamespace("cobbled_counter")
                || context.isNamespace("cobbledex")
                || (context.isNamespace("fightorflight") && context.path.contains("staff"))
                || isMegaShowdownUtility(context)
                || context.isNamespace("rctmod")
                || context.path.equals("npc_editor")
                || context.itemClass.contains("pokedexitem")
                || context.itemClass.contains("pokeroditem")
                || context.hasTag("cobblemon:pokedex")
                || context.hasTag("cobblemon:poke_rods")
                || (context.isNamespace("cobblemon_farmers")
                && (context.path.endsWith("_worker") || context.path.equals("retrieve_worker")));
    }

    private static boolean isTransport(ItemContext context) {
        return context.itemClass.contains("cobblemonboatitem")
                || context.hasTag("cobblemon:boats")
                || context.hasTag("cobblemon:chest_boats");
    }

    private static boolean isMegaShowdownUtility(ItemContext context) {
        return context.isNamespace("mega_showdown") && (context.path.contains("bracelet")
                || context.path.contains("debug")
                || context.path.contains("sparkling_stone")
                || context.path.equals("keystone")
                || context.path.contains("ring")
                || context.path.contains("band")
                || context.path.contains("tera_pouch")
                || context.path.equals("tera_orb")
                || context.path.equals("blank_z")
                || context.path.equals("wishing_star")
                || context.path.equals("wishing_star_crystal")
                || context.path.equals("dormant_crystal")
                || context.path.equals("power_spot")
                || context.path.equals("likos_pendant"));
    }

    private static boolean isConsumable(ItemContext context) {
        return context.inTab("consumables")
                || context.creativeTab.equals("food & drinks")
                || context.path.endsWith("_candy")
                || context.itemClass.contains("candyitem")
                || context.itemClass.contains("hypertrainingitem")
                || context.itemClass.contains("aprijuiceitem")
                || context.itemClass.contains("moomoomilk")
                || context.itemClass.contains("regionalfooditem")
                || context.isNamespace("mega_showdown")
                && (context.path.contains("candy")
                || context.path.contains("soup")
                || context.path.contains("honey")
                || context.path.contains("mushroom"));
    }

    private static boolean isAgriculture(ItemContext context) {
        return context.inTab("agriculture")
                || context.itemClass.contains("pokemonegg");
    }

    private static boolean isBuilding(ItemContext context) {
        return context.inTab("blocks")
                || context.path.contains("exp_quartz")
                || (context.itemClass.equals("net.minecraft.world.item.blockitem")
                && (context.hasTag("cobblemon:saccharine_logs")
                || context.hasTag("cobblemon:trees")
                || context.hasTag("minecraft:logs")
                || context.hasTag("minecraft:logs_that_burn")));
    }

    private static boolean isArchaeology(ItemContext context) {
        return context.inTab("archaeology")
                || context.creativeTab.equals("ingredients")
                || context.path.contains("tumblestone")
                || context.path.contains("candy_ore");
    }

    private static ItemRule rule(String id, Predicate<ItemContext> matches, Consumer<ItemContext> enrich) {
        return new ItemRule(id, matches, enrich);
    }

    private static void kind(ItemContext context, String kind) {
        context.meta.put(SearchNodeKeys.COBBLEMON_ITEM_KIND, kind);
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

        private boolean isNamespace(String namespace) {
            return id.getNamespace().equals(namespace);
        }
    }

    private record ItemRule(String id, Predicate<ItemContext> matches, Consumer<ItemContext> enrich) {
    }
}
