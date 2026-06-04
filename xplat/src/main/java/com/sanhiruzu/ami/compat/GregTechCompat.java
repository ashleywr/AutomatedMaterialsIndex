package com.sanhiruzu.ami.compat;

import com.sanhiruzu.ami.index.SearchNodeKeys;
import net.minecraft.resources.ResourceLocation;

import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.StringJoiner;

public final class GregTechCompat {
    private static final Map<String, Long> GT_VOLTAGES = Map.ofEntries(
            Map.entry("ulv", 8L),
            Map.entry("lv", 32L),
            Map.entry("mv", 128L),
            Map.entry("hv", 512L),
            Map.entry("ev", 2048L),
            Map.entry("iv", 8192L),
            Map.entry("luv", 32768L),
            Map.entry("zpm", 131072L),
            Map.entry("uv", 524288L),
            Map.entry("uhv", 2097152L),
            Map.entry("uev", 8388608L),
            Map.entry("uiv", 33554432L),
            Map.entry("uxv", 134217728L),
            Map.entry("opv", 536870912L),
            Map.entry("max", 2147483648L)
    );
    private static final Map<String, String> GT_CIRCUIT_TIERS = Map.ofEntries(
            Map.entry("basic_electronic_circuit", "lv"),
            Map.entry("basic_integrated_circuit", "lv"),
            Map.entry("microchip_processor", "lv"),
            Map.entry("good_electronic_circuit", "mv"),
            Map.entry("good_integrated_circuit", "mv"),
            Map.entry("micro_processor", "mv"),
            Map.entry("advanced_integrated_circuit", "hv"),
            Map.entry("micro_processor_assembly", "hv"),
            Map.entry("nano_processor", "hv"),
            Map.entry("micro_processor_computer", "ev"),
            Map.entry("nano_processor_assembly", "ev"),
            Map.entry("quantum_processor", "ev"),
            Map.entry("micro_processor_mainframe", "iv"),
            Map.entry("nano_processor_computer", "iv"),
            Map.entry("quantum_processor_assembly", "iv"),
            Map.entry("crystal_processor", "iv"),
            Map.entry("nano_processor_mainframe", "luv"),
            Map.entry("quantum_processor_computer", "luv"),
            Map.entry("crystal_processor_assembly", "luv"),
            Map.entry("wetware_processor", "luv"),
            Map.entry("quantum_processor_mainframe", "zpm"),
            Map.entry("crystal_processor_computer", "zpm"),
            Map.entry("wetware_processor_assembly", "zpm"),
            Map.entry("crystal_processor_mainframe", "uv"),
            Map.entry("wetware_processor_computer", "uv"),
            Map.entry("wetware_processor_mainframe", "uhv")
    );

    private GregTechCompat() {
    }

    public static void enrichItem(ResourceLocation id, Map<String, String> meta) {
        if (id == null || meta == null) {
            return;
        }
        if (meta.getOrDefault(SearchNodeKeys.COMPAT_FAMILIES, "").isBlank()) {
            CompatFamilyDetector.detect(id, meta);
        }
        if (!isGregTechFamilyItem(id, meta)) {
            return;
        }

        Context context = new Context(id, meta);
        LinkedHashSet<String> facts = new LinkedHashSet<>();
        addClassFacts(context, facts);
        addPathFacts(context, facts);
        addTagFacts(context, facts);
        addMetricFacts(context, facts);

        String kind = classifyKind(context, facts);
        if (!kind.isBlank()) {
            meta.put(SearchNodeKeys.GREGTECH_ITEM_KIND, kind);
            addSearchToken(meta, "gregtech_" + kind);
        }

        String tier = classifyTier(context);
        if (!tier.isBlank()) {
            meta.put(SearchNodeKeys.GREGTECH_TIER, tier);
            addSearchToken(meta, "gregtech_" + tier);
            addSearchToken(meta, tier + "_tier");
        }

        String circuitGrade = classifyCircuitGrade(context, kind);
        if (!circuitGrade.isBlank()) {
            meta.put(SearchNodeKeys.GREGTECH_CIRCUIT_GRADE, circuitGrade);
            addSearchToken(meta, "gregtech_" + circuitGrade);
            addSearchToken(meta, "gregtech_circuit_" + circuitGrade);
            addSearchToken(meta, circuitGrade + "_circuit");
        }

        addEnergyFacts(context, facts, kind, tier);

        if (!facts.isEmpty()) {
            meta.put(SearchNodeKeys.GREGTECH_FACTS, join(facts));
            for (String fact : facts) {
                addSearchToken(meta, fact);
                addSearchToken(meta, "gregtech_" + fact);
            }
        }
    }

    private static boolean isGregTechFamilyItem(ResourceLocation id, Map<String, String> meta) {
        return "gtceu".equals(id.getNamespace())
                || "gregtech".equals(id.getNamespace())
                || CompatFamilyDetector.hasFamily(meta, CompatFamilyDetector.GREGTECH);
    }

    private static void addClassFacts(Context context, Set<String> facts) {
        if (containsAny(context.itemClass, "MetaMachineItem")
                || containsAny(context.blockClass, "MetaMachineBlock")) {
            facts.add("machine");
        }
        if (containsAny(context.itemClass, "GTBucketItem", "SurfaceRockBlockItem")) {
            facts.add("material");
        }
        if (containsAny(context.itemClass, "Cover", "CoverItem")) {
            facts.add("cover");
        }
        if (containsAny(context.itemClass, "Tool", "Wrench", "Screwdriver", "Prospector", "Scanner")) {
            facts.add("tool");
        }
        if (containsAny(context.blockClass, "Multiblock")) {
            facts.add("multiblock");
        }
    }

    private static void addPathFacts(Context context, Set<String> facts) {
        String path = context.path;
        if (containsAny(path, "multiblock") || containsPathToken(path, "controller")) facts.add("multiblock");
        if (containsPathToken(path, "cover") || containsAny(path, "_cover_", "cover_")) facts.add("cover");
        if (containsAny(path, "circuit", "processor", "chip", "soc", "ram", "ulpic", "ilc")) facts.add("circuit");
        if (containsAny(path, "battery", "capacitor", "cable", "wire", "energy", "power", "generator",
                "dynamo", "transformer", "converter", "diode", "solar_panel", "voltage_coil")) {
            facts.add("power");
        }
        if (containsPathToken(path, "combustion")) facts.add("power");
        if (containsAny(path, "pipe", "duct", "conveyor", "robot_arm", "pump", "fluid", "gas", "item_filter")) {
            facts.add("logistics");
        }
        if (containsAny(path, "hatch", "bus", "muffler", "maintenance")) {
            facts.add("machine");
        }
        if (containsAny(path, "emitter", "sensor", "regulator", "motor", "piston", "field_generator",
                "electric_pump", "conveyor_module", "robot_arm")) {
            facts.add("component");
        }
        if (containsAny(path, "assembler", "macerator", "centrifuge", "electrolyzer", "compressor", "extractor",
                "furnace", "mixer", "canner", "lathe", "bender", "wiremill", "polarizer", "smelter",
                "reactor", "collector", "boiler", "crusher", "autoclave", "bath", "cutter", "distillery",
                "extruder", "solidifier", "press", "packer", "turbine", "miner", "brewery", "separator",
                "fermenter", "heater", "engraver", "sifter", "accelerator", "fisher", "scrubber", "breaker",
                "buffer")) {
            facts.add("machine");
        }
        if (containsAny(path, "wrench", "screwdriver", "soft_mallet", "hard_hammer", "prospector", "scanner",
                "drill", "saw", "chainsaw", "buzzsaw", "plunger", "wire_cutter", "wire_cutters", "mortar")) {
            facts.add("tool");
        }
        if (containsAny(path, "ingot", "nugget", "dust", "plate", "rod", "bolt", "screw", "ring", "foil",
                "wire", "gear", "spring", "rotor", "gem", "ore", "crushed", "purified", "impure", "raw",
                "tiny", "small", "bucket", "indicator", "blade", "head", "tip", "lens", "wafer", "mold",
                "casing", "frame", "sheet", "studs", "dye", "can", "boule", "round")) {
            facts.add("material");
        }
    }

    private static void addTagFacts(Context context, Set<String> facts) {
        if (containsAny(context.tags, "c:circuits", "gtceu:circuits")) facts.add("circuit");
        if (containsAny(context.tags, "c:ingots", "c:nuggets", "c:dusts", "c:gems", "c:ores", "c:plates",
                "c:rods", "c:wires", "gtceu:materials")) {
            facts.add("material");
        }
        if (containsAny(context.tags, "gtceu:tools", "c:tools")) facts.add("tool");
    }

    private static void addMetricFacts(Context context, Set<String> facts) {
        if (hasValue(context.meta, SearchNodeKeys.ENERGY_CAPACITY)
                || hasValue(context.meta, SearchNodeKeys.ENERGY_GENERATION)
                || hasValue(context.meta, SearchNodeKeys.ENERGY_CONSUMPTION)) {
            facts.add("power");
        }
        if (hasValue(context.meta, SearchNodeKeys.FLUID_CAPACITY)) {
            facts.add("fluid");
        }
    }

    private static String classifyKind(Context context, Set<String> facts) {
        if (facts.contains("multiblock")) return "multiblocks";
        if (facts.contains("cover")) return "covers";
        if (facts.contains("circuit")) return "circuits";
        if (isTool(context) || facts.contains("tool")) return "tools";
        if (facts.contains("machine")) return "machines";
        if (facts.contains("power")) return "power";
        if (facts.contains("material") || facts.contains("component") || isMaterial(context)) return "materials";
        if (facts.contains("logistics") || facts.contains("fluid")) return "machines";
        return "";
    }

    private static boolean isTool(Context context) {
        return hasToken(context.facets, "melee_weapon")
                || hasToken(context.facets, "ranged_weapon")
                || hasToken(context.facets, "harvest_tool")
                || hasToken(context.facets, "utility_tool");
    }

    private static boolean isMaterial(Context context) {
        return hasToken(context.facets, "ingot")
                || hasToken(context.facets, "nugget")
                || hasToken(context.facets, "dust")
                || hasToken(context.facets, "gem")
                || hasToken(context.facets, "raw_material")
                || hasToken(context.facets, "tech_component")
                || hasToken(context.facets, "mechanical_component")
                || hasToken(context.facets, "ingredient_mineral");
    }

    private static String classifyTier(Context context) {
        for (String tier : new String[]{
                "steam", "ulv", "lv", "mv", "hv", "ev", "iv", "luv", "zpm", "uv", "uhv", "uev", "uiv", "uxv", "opv", "max"
        }) {
            if (hasCircuitTierTag(context.tags, tier)) {
                return tier;
            }
        }
        String circuitTier = GT_CIRCUIT_TIERS.getOrDefault(context.path, "");
        if (!circuitTier.isBlank()) {
            return circuitTier;
        }
        for (String tier : new String[]{
                "steam", "ulv", "lv", "mv", "hv", "ev", "iv", "luv", "zpm", "uv", "uhv", "uev", "uiv", "uxv", "opv", "max"
        }) {
            if (context.path.equals(tier)
                    || context.path.startsWith(tier + "_")
                    || context.path.contains("_" + tier + "_")
                    || context.path.endsWith("_" + tier)) {
                return tier;
            }
        }
        if (context.path.startsWith("bronze_") || context.path.contains("_bronze_") || context.path.endsWith("_bronze")) {
            return "steam";
        }
        return "";
    }

    private static String classifyCircuitGrade(Context context, String kind) {
        if (!"circuits".equals(kind) && !hasCircuitTierTag(context.tags)) {
            return "";
        }
        String path = context.path;
        if (path.startsWith("basic_")) return "basic";
        if (path.startsWith("good_")) return "good";
        if (path.startsWith("advanced_")) return "advanced";
        if (path.startsWith("microchip_")) return "microchip";
        if (path.startsWith("micro_processor")) return "micro";
        if (path.startsWith("nano_processor")) return "nano";
        if (path.startsWith("quantum_processor")) return "quantum";
        if (path.startsWith("crystal_processor")) return "crystal";
        if (path.startsWith("wetware_processor")) return "wetware";
        if (path.equals("vacuum_tube") || path.equals("glass_tube")) return "tube";
        if (path.endsWith("_chip")) return "chip";
        return "";
    }

    private static void addEnergyFacts(Context context, Set<String> facts, String kind, String tier) {
        Long voltage = GT_VOLTAGES.get(tier);
        if (voltage == null || voltage <= 0L) {
            return;
        }

        String role = classifyEnergyRole(context, kind);
        if (role.isBlank()) {
            return;
        }

        context.meta.put(SearchNodeKeys.GREGTECH_ENERGY_ROLE, role);
        facts.add(role);
        addSearchToken(context.meta, role);
        addSearchToken(context.meta, "gregtech_" + role);

        int amps = classifyAmperage(context.path);
        if (amps > 1) {
            context.meta.put(SearchNodeKeys.GREGTECH_AMPERAGE, Integer.toString(amps));
            addSearchToken(context.meta, amps + "a");
            addSearchToken(context.meta, "gregtech_" + amps + "a");
        }

        long eu = voltage * Math.max(1, amps);
        switch (role) {
            case "generates_eu" -> {
                context.meta.put(SearchNodeKeys.GREGTECH_EU_GENERATION, Long.toString(eu));
                addSearchToken(context.meta, "eu_generation");
                addSearchToken(context.meta, "eu_produced");
                addSearchToken(context.meta, "eu_production");
                addSearchToken(context.meta, "gregtech_eu_generation");
            }
            case "consumes_eu" -> {
                context.meta.put(SearchNodeKeys.GREGTECH_EU_CONSUMPTION, Long.toString(eu));
                addSearchToken(context.meta, "eu_consumption");
                addSearchToken(context.meta, "eu_consumed");
                addSearchToken(context.meta, "eu_usage");
                addSearchToken(context.meta, "gregtech_eu_consumption");
            }
            case "inputs_eu" -> {
                context.meta.put(SearchNodeKeys.GREGTECH_EU_INPUT, Long.toString(eu));
                addSearchToken(context.meta, "eu_input");
                addSearchToken(context.meta, "gregtech_eu_input");
            }
            case "outputs_eu" -> {
                context.meta.put(SearchNodeKeys.GREGTECH_EU_OUTPUT, Long.toString(eu));
                addSearchToken(context.meta, "eu_output");
                addSearchToken(context.meta, "gregtech_eu_output");
            }
            case "transfers_eu" -> {
                context.meta.put(SearchNodeKeys.GREGTECH_EU_INPUT, Long.toString(eu));
                context.meta.put(SearchNodeKeys.GREGTECH_EU_OUTPUT, Long.toString(eu));
                addSearchToken(context.meta, "eu_transfer");
                addSearchToken(context.meta, "gregtech_eu_transfer");
            }
            default -> {
            }
        }
    }

    private static String classifyEnergyRole(Context context, String kind) {
        String path = context.path;
        if (containsAny(path, "energy_input_hatch", "substation_input_hatch")) {
            return "inputs_eu";
        }
        if (containsAny(path, "energy_output_hatch", "substation_output_hatch")) {
            return "outputs_eu";
        }
        if (containsAny(path, "transformer", "energy_converter")) {
            return "transfers_eu";
        }
        if (containsAny(path, "generator", "turbine", "creative_energy")
                || containsPathToken(path, "combustion")) {
            return "generates_eu";
        }
        if ("machines".equals(kind)
                && !containsAny(path, "battery_buffer", "charger", "diode", "cable", "wire")) {
            return "consumes_eu";
        }
        return "";
    }

    private static int classifyAmperage(String path) {
        if (containsAny(path, "_64a", "64a_")) return 64;
        if (containsAny(path, "_16a", "16a_")) return 16;
        if (containsAny(path, "_8a", "8a_")) return 8;
        if (containsAny(path, "_4a", "4a_")) return 4;
        if (containsAny(path, "_2a", "2a_")) return 2;
        return 1;
    }

    private static boolean hasCircuitTierTag(String tags) {
        return containsAny(tags, "gtceu:circuits/");
    }

    private static boolean hasCircuitTierTag(String tags, String tier) {
        return containsAny(tags, "gtceu:circuits/" + tier);
    }

    private static boolean hasValue(Map<String, String> meta, String key) {
        String value = meta.get(key);
        return value != null && !value.isBlank();
    }

    private static boolean containsPathToken(String path, String token) {
        String normalizedToken = token.toLowerCase(Locale.ROOT);
        for (String part : path.split("[_\\-/]+")) {
            if (part.equals(normalizedToken)) {
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
        LinkedHashSet<String> values = new LinkedHashSet<>();
        String existing = meta.getOrDefault(SearchNodeKeys.SEARCH_TOKENS, "");
        for (String value : existing.split("\\s+")) {
            if (!value.isBlank()) {
                values.add(value);
            }
        }
        if (values.add(token)) {
            meta.put(SearchNodeKeys.SEARCH_TOKENS, String.join(" ", values));
        }
    }

    private static final class Context {
        final String path;
        final String itemClass;
        final String blockClass;
        final String tags;
        final String facets;
        final Map<String, String> meta;

        Context(ResourceLocation id, Map<String, String> meta) {
            this.path = id.getPath().toLowerCase(Locale.ROOT);
            this.itemClass = meta.getOrDefault(SearchNodeKeys.ITEM_CLASS, "");
            this.blockClass = meta.getOrDefault(SearchNodeKeys.BLOCK_CLASS, "");
            this.tags = meta.getOrDefault(SearchNodeKeys.TAGS, "").toLowerCase(Locale.ROOT);
            this.facets = meta.getOrDefault(SearchNodeKeys.FACETS, "").toLowerCase(Locale.ROOT);
            this.meta = meta;
        }
    }
}
