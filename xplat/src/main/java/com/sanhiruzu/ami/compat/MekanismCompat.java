package com.sanhiruzu.ami.compat;

import com.sanhiruzu.ami.index.SearchNodeKeys;
import net.minecraft.resources.ResourceLocation;

import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.StringJoiner;

public final class MekanismCompat {
    private static final String MOD_ID = "mekanism";

    private MekanismCompat() {
    }

    public static void enrichItem(ResourceLocation id, Map<String, String> meta) {
        if (id == null || meta == null) {
            return;
        }
        if (meta.getOrDefault(SearchNodeKeys.COMPAT_FAMILIES, "").isBlank()) {
            CompatFamilyDetector.detect(id, meta);
        }
        if (!isMekanismFamilyItem(id, meta)) {
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
            meta.put(SearchNodeKeys.MEKANISM_ITEM_KIND, kind);
            addSearchToken(meta, "mekanism_" + kind);
        }
        String tier = classifyTier(context.path);
        if (!tier.isBlank()) {
            meta.put(SearchNodeKeys.MEKANISM_TIER, tier);
            addSearchToken(meta, "mekanism_" + tier);
        }
        if (!facts.isEmpty()) {
            meta.put(SearchNodeKeys.MEKANISM_FACTS, join(facts));
            for (String fact : facts) {
                addSearchToken(meta, fact);
            }
        }
    }

    private static boolean isMekanismFamilyItem(ResourceLocation id, Map<String, String> meta) {
        return MOD_ID.equals(id.getNamespace())
                || id.getNamespace().startsWith("mekanism")
                || CompatFamilyDetector.hasFamily(meta, CompatFamilyDetector.MEKANISM);
    }

    private static void addClassFacts(Context context, Set<String> facts) {
        if (containsAny(context.itemClass, "ItemUpgrade", "ItemModule", "ItemTierInstaller")) facts.add("upgrade");
        if (containsAny(context.itemClass, "ItemQIODrive", "QIO", "Transporter", "Pipe", "Tube", "Cable")) facts.add("logistics");
        if (containsAny(context.itemClass, "ItemEnergized", "ItemBlockEnergyCube", "ItemBlockInduction")) facts.add("energy");
        if (containsAny(context.itemClass, "ItemConfigurator", "ItemNetworkReader", "ItemDictionary", "ItemGaugeDropper",
                "ItemGeigerCounter", "ItemDosimeter", "ItemPortableQIODashboard")) facts.add("tool");
        if (containsAny(context.itemClass, "ItemAtomicDisassembler", "ItemMekaTool", "ItemElectricBow", "ItemFlamethrower")) facts.add("weapon_tool");
        if (containsAny(context.itemClass, "ItemMekaSuitArmor", "ItemFreeRunners", "ItemJetpack", "ItemScuba", "ItemHazmat", "ItemHDPEElytra")) facts.add("gear");
        if (containsAny(context.itemClass, "ItemAlloy", "ItemBlockMachine", "MekanismItems$1")) facts.add("material");
        if (containsAny(context.blockClass, "BlockTile", "BlockMachine", "BlockFactory", "BlockDigitalMiner")) facts.add("machine");
    }

    private static void addPathFacts(Context context, Set<String> facts) {
        String path = context.path;
        if (startsWithAny(path, "upgrade_", "module_") || path.endsWith("_tier_installer")) facts.add("upgrade");
        if (containsAny(path, "qio_", "logistical_transporter", "restrictive_transporter", "diversion_transporter",
                "universal_cable", "mechanical_pipe", "pressurized_tube", "thermodynamic_conductor", "logistical_sorter")) {
            facts.add("logistics");
        }
        if (containsAny(path, "energy_tablet", "energy_cube", "induction_cell", "induction_provider", "induction_port",
                "induction_casing", "solar_generator", "wind_generator", "heat_generator", "bio_generator",
                "gas_burning_generator", "turbine", "reactor", "resistive_heater")) {
            facts.add("energy");
        }
        if (containsAny(path, "chemical_tank", "chemical_", "infuse_type", "pigment", "slurry", "gas_",
                "fissile", "plutonium", "polonium", "antimatter", "yellow_cake", "hdpe", "substrate",
                "pellet_", "reprocessed_fissile", "fluorite")) {
            facts.add("chemical");
        }
        if (containsAny(path, "fluid_tank", "dynamic_tank", "thermal_evaporation", "boiler", "fuelwood_heater")) {
            facts.add("fluid_or_heat");
        }
        if (containsAny(path, "metallurgic_infuser", "crusher", "enrichment_chamber", "purification_chamber",
                "chemical_injection_chamber", "osmium_compressor", "combiner", "precision_sawmill",
                "energized_smelter", "digital_miner", "rotary_condensentrator", "electrolytic_separator",
                "chemical_oxidizer", "chemical_infuser", "chemical_dissolution_chamber", "chemical_washer",
                "chemical_crystallizer", "isotopic_centrifuge", "pressurized_reaction_chamber", "formulaic_assemblicator",
                "oredictionificator", "modification_station", "security_desk", "personal_chest")) {
            facts.add("machine");
        }
        if (containsAny(path, "configurator", "network_reader", "dictionary", "gauge_dropper", "geiger_counter",
                "dosimeter", "portable_qio_dashboard", "portable_teleporter")) {
            facts.add("tool");
        }
        if (containsAny(path, "atomic_disassembler", "meka_tool", "electric_bow", "flamethrower")) facts.add("weapon_tool");
        if (containsAny(path, "mekasuit", "free_runners", "jetpack", "scuba", "hazmat", "elytra")) facts.add("gear");
        if (containsAny(path, "dust_", "ingot_", "nugget_", "block_", "raw_", "alloy_", "enriched_", "shard_",
                "crystal_", "clump_", "dirty_dust_", "dirty_", "bio_fuel", "control_circuit",
                "electrolytic_core", "teleportation_core")) {
            facts.add("material");
        }
    }

    private static void addTagFacts(Context context, Set<String> facts) {
        if (containsAny(context.tags, "mekanism:unit")) facts.add("upgrade");
        if (containsAny(context.tags, "mekanism:alloys", "mekanism:enriched", "c:circuits", "c:dusts", "c:ingots", "c:nuggets")) facts.add("material");
        if (containsAny(context.tags, "mekanism:configurators")) facts.add("tool");
    }

    private static void addMetricFacts(Context context, Set<String> facts) {
        if (hasValue(context.meta, SearchNodeKeys.ENERGY_CAPACITY)
                || hasValue(context.meta, SearchNodeKeys.ENERGY_GENERATION)
                || hasValue(context.meta, SearchNodeKeys.ENERGY_CONSUMPTION)) {
            facts.add("energy");
        }
        if (hasValue(context.meta, SearchNodeKeys.FLUID_CAPACITY)) {
            facts.add("fluid_or_heat");
        }
    }

    private static String classifyKind(Context context, Set<String> facts) {
        if (facts.contains("upgrade")) return "upgrades";
        if (facts.contains("logistics")) return "logistics";
        if (facts.contains("machine") && !isStrongSemanticEquipment(context)) return "machines";
        if (facts.contains("chemical") && !isStrongSemanticEquipment(context)) return "chemicals";
        if (facts.contains("energy") && !isStrongSemanticEquipment(context) && !facts.contains("weapon_tool")) return "energy";
        if (facts.contains("weapon_tool") || facts.contains("tool") || isTool(context)) return "tools";
        if (facts.contains("gear")) return "gear";
        if (facts.contains("material") || isMaterial(context)) return "materials";
        if (facts.contains("fluid_or_heat")) return "machines";
        return "";
    }

    private static boolean isStrongSemanticEquipment(Context context) {
        return hasToken(context.facets, "melee_weapon")
                || hasToken(context.facets, "ranged_weapon")
                || hasToken(context.facets, "harvest_tool")
                || hasToken(context.facets, "utility_tool")
                || hasToken(context.facets, "armor_head")
                || hasToken(context.facets, "armor_chest")
                || hasToken(context.facets, "armor_legs")
                || hasToken(context.facets, "armor_feet");
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
                || hasToken(context.facets, "tech_component");
    }

    private static String classifyTier(String path) {
        for (String tier : new String[]{"creative", "ultimate", "elite", "advanced", "basic"}) {
            if (path.startsWith(tier + "_") || path.contains("_" + tier + "_") || path.endsWith("_" + tier)) {
                return tier;
            }
        }
        return "";
    }

    private static boolean hasValue(Map<String, String> meta, String key) {
        String value = meta.get(key);
        return value != null && !value.isBlank();
    }

    private static boolean startsWithAny(String value, String... prefixes) {
        for (String prefix : prefixes) {
            if (value.startsWith(prefix)) {
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
