package com.sanhiruzu.ami.compat;

import com.sanhiruzu.ami.index.SearchNodeKeys;
import net.minecraft.resources.Identifier;

import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.StringJoiner;

public final class CreateCompat {
    private static final String MOD_ID = "create";
    private static final Set<String> CREATE_PROCESSING_RECIPES = Set.of(
            "pressing", "crushing", "mixing", "milling", "cutting", "sawing",
            "filling", "emptying", "compacting", "sandpaper_polishing",
            "sequenced_assembly", "mechanical_crafting", "deploying",
            "item_application", "splashing", "haunting", "smoking", "blasting"
    );

    private CreateCompat() {
    }

    public static void enrichItem(Identifier id, Map<String, String> meta) {
        if (id == null || meta == null) {
            return;
        }
        if (meta.getOrDefault(SearchNodeKeys.COMPAT_FAMILIES, "").isBlank()) {
            CompatFamilyDetector.detect(id, meta);
        }
        if (!isCreateFamilyItem(id, meta)) {
            return;
        }

        Context context = new Context(id, meta);
        LinkedHashSet<String> facts = new LinkedHashSet<>();
        LinkedHashSet<String> recipeRoles = new LinkedHashSet<>();

        addClassFacts(context, facts);
        addPathFacts(context, facts);
        addTagFacts(context, facts);
        addRecipeFacts(context, facts, recipeRoles);
        addMetricFacts(context, facts);
        addPonderFact(context, facts);

        String kind = classifyKind(context, facts, recipeRoles);
        if (!kind.isBlank()) {
            meta.put(SearchNodeKeys.CREATE_ITEM_KIND, kind);
        }
        String stressRole = classifyStressRole(facts);
        if (!stressRole.isBlank()) {
            meta.put(SearchNodeKeys.CREATE_STRESS_ROLE, stressRole);
        }
        String kineticRole = classifyKineticRole(context, facts);
        if (!kineticRole.isBlank()) {
            meta.put(SearchNodeKeys.CREATE_KINETIC_ROLE, kineticRole);
        }
        if (!facts.isEmpty()) {
            meta.put(SearchNodeKeys.CREATE_FACTS, join(facts));
            if (!kind.isBlank()) {
                addSearchToken(meta, "create_" + kind);
            }
            for (String fact : facts) {
                addSearchToken(meta, fact);
            }
        }
        if (!recipeRoles.isEmpty()) {
            meta.put(SearchNodeKeys.CREATE_RECIPE_ROLES, join(recipeRoles));
            for (String role : recipeRoles) {
                addSearchToken(meta, role);
            }
        }
    }

    private static boolean isCreateFamilyItem(Identifier id, Map<String, String> meta) {
        return MOD_ID.equals(id.getNamespace())
                || CompatFamilyDetector.hasFamily(meta, CompatFamilyDetector.CREATE);
    }

    private static void addClassFacts(Context context, Set<String> facts) {
        if (isAssignableTo(context.blockClass, "com.simibubi.create.content.kinetics.base.IRotate")
                || containsAny(context.blockClass, ".content.kinetics.", ".blocks.customs.custom", ".blocks.shafts.", ".blocks.cogwheels.")) {
            facts.add("uses_su");
            facts.add("kinetic");
        }
        if (containsAny(context.blockClass, "waterwheel", "windmillbearing", "creativemotor", "steamengine")
                || containsAny(context.itemClass, "portableengine")) {
            facts.add("generates_su");
        }
        if (containsAny(context.blockClass, ".content.logistics.", "packager", "packageport", "postbox", "stocklink")
                || containsAny(context.itemClass, "packageitem", "packageportitem")) {
            facts.add("logistics");
        }
        if (containsAny(context.blockClass, ".content.trains.", "trackblock", "stationblock", "signalblock")
                || containsAny(context.itemClass, ".content.trains.", "tracktargetingblockitem")) {
            facts.add("train");
        }
        if (containsAny(context.blockClass, ".content.contraptions.", "contraption", "bearingblock", "gantry", "chassis")
                || containsAny(context.itemClass, ".content.contraptions.", "contraption")) {
            facts.add("contraption");
        }
        if (containsAny(context.blockClass, ".content.fluids.", "fluidtank", "fluidpipe", "pumpblock", "spoutblock", "drainblock")) {
            facts.add("fluid_handling");
        }
        if (containsAny(context.blockClass, ".content.decoration.", "copycat", "scaffolding", "casingblock", "windowblock")
                || containsAny(context.itemClass, "catwalk", "railing", "coinstack")) {
            facts.add("decoration");
        }
        if (containsAny(context.itemClass, "wrench", "sandpaper", "filteritem", "schematic", "clipboard", "goggles")) {
            facts.add("create_tool");
        }
        if (containsAny(context.itemClass, "lighteritem", "sprayeritem", "scanneritem", "finderitem", "atlasitem",
                "ramroditem", "wormitem", "plungerlauncheritem", "portabledrillitem", "flamethroweritem",
                "blockpickeritem", "magnetitem")) {
            facts.add("create_tool");
        }
        if (containsAny(context.itemClass, "mechanismitem", "componentitem", "tireitem", "wirespool",
                "springitem", "quickfiringmechanismitem")) {
            facts.add("create_component");
        }
        if (containsAny(context.itemClass, "chromaticcompounditem", "shadowsteelitem", "refinedradianceitem")) {
            facts.add("create_material");
        }
    }

    private static void addPathFacts(Context context, Set<String> facts) {
        String path = context.id.getPath().toLowerCase(Locale.ROOT);
        String namespace = context.id.getNamespace().toLowerCase(Locale.ROOT);

        if (containsPathToken(path, Set.of("mechanism", "component", "propeller", "whisk", "hand", "spring",
                "breechblock", "breech", "extractor", "lock", "spool", "capacitor", "tire", "handle",
                "coupling", "drone"))
                || containsAny(path, "quickfiring_mechanism", "precision_mechanism", "fan_component",
                "vault_component", "sliding_breechblock", "screw_lock", "recoil_spring",
                "breech_extractor", "rope_coupling", "brass_drone")) {
            facts.add("create_component");
        }
        if (containsPathToken(path, Set.of("drill", "scanner", "finder", "atlas", "lighter", "sprayer",
                "launcher", "magnet", "flamethrower", "picker", "rod", "worm", "amulet"))
                || containsAny(path, "portable_drill", "oil_scanner", "vein_finder", "vein_atlas",
                "chemical_sprayer", "plunger_launcher", "ram_rod", "electrum_amulet")) {
            facts.add("create_tool");
        }
        if (containsAny(path, "filling_tank", "fueling_tank", "fluid_tank", "chemical_tank")) {
            facts.add("fluid_handling");
        }
        if (containsAny(path, "contraption_diagram")) {
            facts.add("contraption");
        }
        if (containsPathToken(path, Set.of("icon")) || path.endsWith("_icon")) {
            facts.add("create_misc");
        }
        if (containsAny(path, "cinder_flour", "rose_quartz", "chromatic_compound", "shadow_steel",
                "refined_radiance", "overcharged_", "nuclear_fuel", "thorium", "biomass",
                "cocoa_powder", "cocoa_butter", "crushed_cocoa", "gingerdough", "honey_glue",
                "packed_gunpowder", "gunpowder_pinch", "powder_charge", "shot_balls",
                "congealed_nitro", "hardened_nitro", "tracer_tip", "raw_diamond", "raw_emerald",
                "chorium_ingot", "zinc_handle", "heap_of_experience", "incomplete_web", "chains",
                "straw", "pulp", "tree_fertilizer", "end_stone_powder")) {
            facts.add("create_material");
        }
        if (namespace.equals("createoreexcavation") && containsPathToken(path, Set.of("drill"))) {
            facts.add("create_machine_part");
        }
    }

    private static void addTagFacts(Context context, Set<String> facts) {
        if (hasToken(context.tags, "create:contraption_controlled")) facts.add("contraption_controlled");
        if (hasToken(context.blockTags, "create:contraption_controlled")) facts.add("contraption_controlled");
        if (hasToken(context.tags, "create:packages")) facts.add("package");
        if (hasToken(context.tags, "create:postboxes") || hasToken(context.blockTags, "create:postboxes")) facts.add("logistics");
        if (hasToken(context.tags, "create:tracks") || hasToken(context.blockTags, "create:tracks")) facts.add("train");
        if (hasToken(context.tags, "create:valve_handles") || hasToken(context.blockTags, "create:valve_handles")) facts.add("kinetic_control");
        if (hasToken(context.tags, "create:handheld_in_deployer_use")) facts.add("deployer_usable");
        if (hasToken(context.blockTags, "create:windmill_sails")) facts.add("windmill_sail");
        if (hasToken(context.blockTags, "create:safe_nbt")) facts.add("contraption_safe_nbt");
        if (hasToken(context.blockTags, "create:chest_mounted_storage")
                || hasToken(context.blockTags, "create:simple_mounted_storage")) {
            facts.add("mounted_storage");
        }
        if (hasToken(context.blockTags, "create:non_movable")) facts.add("contraption_immovable");
        if (hasToken(context.blockTags, "create:copycat_allow")) facts.add("copycat_material");
        if (hasToken(context.blockTags, "create:copycat_deny")) facts.add("copycat_denied");
        if (hasToken(context.blockTags, "create:casing") || hasToken(context.tags, "create:casing")) facts.add("casing");
        if (hasToken(context.blockTags, "create:fan_transparent")) facts.add("fan_transparent");
    }

    private static void addRecipeFacts(Context context, Set<String> facts, Set<String> recipeRoles) {
        addRecipeRoles(context.recipeCategories, "output", recipeRoles);
        addRecipeRoles(context.recipeUseCategories, "input", recipeRoles);
        if (!recipeRoles.isEmpty()) {
            facts.add("create_processing");
        }
        if (containsRecipeRole(recipeRoles, "sequenced_assembly")) facts.add("sequenced_assembly");
        if (containsRecipeRole(recipeRoles, "mechanical_crafting")) facts.add("mechanical_crafting");
    }

    private static void addMetricFacts(Context context, Set<String> facts) {
        if (hasValue(context.meta, SearchNodeKeys.ENERGY_CAPACITY)) facts.add("stores_fe");
        if (hasValue(context.meta, SearchNodeKeys.ENERGY_GENERATION)) facts.add("generates_fe");
        if (hasValue(context.meta, SearchNodeKeys.ENERGY_CONSUMPTION)) facts.add("uses_fe");
        if (hasValue(context.meta, SearchNodeKeys.FLUID_CAPACITY)) facts.add("fluid_storage");
    }

    private static void addPonderFact(Context context, Set<String> facts) {
        if (!MOD_ID.equals(context.id.getNamespace())) {
            return;
        }
        ClassLoader loader = Thread.currentThread().getContextClassLoader();
        String path = context.id.getPath();
        if (loader.getResource("assets/create/ponder/" + path + ".nbt") != null
                || loader.getResource("assets/create/ponder/" + path + "/") != null) {
            facts.add("has_ponder");
            context.meta.put(SearchNodeKeys.CREATE_HAS_PONDER, "true");
        }
    }

    private static String classifyKind(Context context, Set<String> facts, Set<String> recipeRoles) {
        if (hasAny(facts, "train")) return "trains";
        if (hasAny(facts, "logistics", "package")) return "logistics";
        if (hasAny(facts, "fluid_handling", "fluid_storage")) return "fluids";
        if (hasAny(facts, "contraption", "contraption_controlled", "mounted_storage")) return "contraptions";
        if (hasAny(facts, "kinetic", "uses_su", "generates_su", "kinetic_control")) return "kinetics";
        if (hasAny(facts, "create_machine_part")) return "machines";
        if (hasAny(facts, "create_tool", "deployer_usable")) return "tools";
        if (hasAny(facts, "decoration", "copycat_material", "copycat_denied", "casing", "fan_transparent")) return "building";
        if (hasAny(facts, "create_component", "create_material") || isMaterial(context)) return "materials";
        if (isBuilding(context)) return "building";
        if (hasAny(facts, "uses_fe", "generates_fe", "stores_fe") && isMachineLike(context)) return "machines";
        if (hasAny(facts, "create_misc")) return "misc";
        return "";
    }

    private static boolean isMaterial(Context context) {
        return hasToken(context.facets, "ingot")
                || hasToken(context.facets, "nugget")
                || hasToken(context.facets, "dust")
                || hasToken(context.facets, "gem")
                || hasToken(context.facets, "raw_material")
                || hasToken(context.facets, "tech_component")
                || hasToken(context.facets, "cable");
    }

    private static boolean isBuilding(Context context) {
        return hasToken(context.facets, "placeable")
                && (hasToken(context.facets, "decorative_block")
                || hasToken(context.facets, "stone_block")
                || hasToken(context.facets, "slab")
                || hasToken(context.facets, "stairs")
                || hasToken(context.facets, "wall"));
    }

    private static boolean isMachineLike(Context context) {
        return hasToken(context.facets, "machine")
                || hasToken(context.facets, "has_block_entity")
                || hasToken(context.facets, "placeable");
    }

    private static String classifyStressRole(Set<String> facts) {
        if (facts.contains("generates_su")) return "generates_su";
        if (facts.contains("uses_su")) return "uses_su";
        return "";
    }

    private static String classifyKineticRole(Context context, Set<String> facts) {
        if (!facts.contains("kinetic") && !facts.contains("uses_su") && !facts.contains("generates_su")) {
            return "";
        }
        if (facts.contains("generates_su")) return "source";
        if (containsAny(context.blockClass, "shaft", "cogwheel", "gearbox", "chain", "belt")) return "relay";
        if (containsAny(context.blockClass, "press", "mixer", "saw", "drill", "fan", "deployer", "millstone")) return "appliance";
        return "kinetic";
    }

    private static void addRecipeRoles(String csv, String direction, Set<String> recipeRoles) {
        for (String token : splitCsv(csv)) {
            String normalized = normalizeRecipe(token);
            if (CREATE_PROCESSING_RECIPES.contains(normalized)) {
                recipeRoles.add(normalized + "_" + direction);
            }
        }
    }

    private static boolean containsRecipeRole(Set<String> recipeRoles, String recipe) {
        return recipeRoles.contains(recipe + "_input") || recipeRoles.contains(recipe + "_output");
    }

    private static boolean hasValue(Map<String, String> meta, String key) {
        String value = meta.get(key);
        return value != null && !value.isBlank();
    }

    private static boolean isAssignableTo(String className, String targetClassName) {
        if (className == null || className.isBlank()) {
            return false;
        }
        try {
            Class<?> cls = Class.forName(className, false, Thread.currentThread().getContextClassLoader());
            Class<?> target = Class.forName(targetClassName, false, Thread.currentThread().getContextClassLoader());
            return target.isAssignableFrom(cls);
        } catch (RuntimeException | LinkageError | ClassNotFoundException ignored) {
            return false;
        }
    }

    private static boolean hasAny(Set<String> values, String... expected) {
        for (String value : expected) {
            if (values.contains(value)) {
                return true;
            }
        }
        return false;
    }

    private static boolean containsAny(String value, String... needles) {
        if (value == null || value.isBlank()) {
            return false;
        }
        String normalized = value.toLowerCase(Locale.ROOT);
        for (String needle : needles) {
            if (normalized.contains(needle.toLowerCase(Locale.ROOT))) {
                return true;
            }
        }
        return false;
    }

    private static boolean containsPathToken(String path, Set<String> expectedTokens) {
        String[] pathTokens = path.split("[_/\\-]");
        for (String pathToken : pathTokens) {
            if (expectedTokens.contains(pathToken)) {
                return true;
            }
        }
        return false;
    }

    private static boolean hasToken(String csv, String token) {
        for (String value : splitCsv(csv)) {
            if (value.equals(token)) {
                return true;
            }
        }
        return false;
    }

    private static Iterable<String> splitCsv(String csv) {
        if (csv == null || csv.isBlank()) {
            return Set.of();
        }
        LinkedHashSet<String> values = new LinkedHashSet<>();
        for (String value : csv.split(",")) {
            String normalized = value.trim().toLowerCase(Locale.ROOT);
            if (!normalized.isBlank()) {
                values.add(normalized);
            }
        }
        return values;
    }

    private static String normalizeRecipe(String value) {
        int colon = value.indexOf(':');
        String normalized = colon >= 0 ? value.substring(colon + 1) : value;
        int slash = normalized.lastIndexOf('/');
        normalized = slash >= 0 ? normalized.substring(slash + 1) : normalized;
        return normalized.toLowerCase(Locale.ROOT);
    }

    private static String join(Set<String> values) {
        StringJoiner joiner = new StringJoiner(",");
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                joiner.add(value);
            }
        }
        return joiner.toString();
    }

    private static void addSearchToken(Map<String, String> meta, String token) {
        if (token == null || token.isBlank()) {
            return;
        }
        String existing = meta.getOrDefault(SearchNodeKeys.SEARCH_TOKENS, "");
        if (existing.contains(token)) {
            return;
        }
        meta.put(SearchNodeKeys.SEARCH_TOKENS, existing.isBlank() ? token : existing + " " + token);
    }

    private static final class Context {
        final Identifier id;
        final String itemClass;
        final String blockClass;
        final String tags;
        final String blockTags;
        final String facets;
        final String recipeCategories;
        final String recipeUseCategories;
        final Map<String, String> meta;

        Context(Identifier id, Map<String, String> meta) {
            this.id = id;
            this.itemClass = meta.getOrDefault(SearchNodeKeys.ITEM_CLASS, "");
            this.blockClass = meta.getOrDefault(SearchNodeKeys.BLOCK_CLASS, "");
            this.tags = meta.getOrDefault(SearchNodeKeys.TAGS, "").toLowerCase(Locale.ROOT);
            this.blockTags = meta.getOrDefault(SearchNodeKeys.BLOCK_TAGS, "").toLowerCase(Locale.ROOT);
            this.facets = meta.getOrDefault(SearchNodeKeys.FACETS, "").toLowerCase(Locale.ROOT);
            this.recipeCategories = meta.getOrDefault(SearchNodeKeys.RECIPE_CATEGORIES, "").toLowerCase(Locale.ROOT);
            this.recipeUseCategories = meta.getOrDefault(SearchNodeKeys.RECIPE_USE_CATEGORIES, "").toLowerCase(Locale.ROOT);
            this.meta = meta;
        }
    }
}
