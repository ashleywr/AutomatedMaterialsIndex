package com.sanhiruzu.ami.compat;

import com.sanhiruzu.ami.index.SearchNodeKeys;
import com.sanhiruzu.ami.index.PathTokens;
import com.sanhiruzu.ami.platform.Services;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.StringJoiner;

public final class ModularGearCompat {
    private static final Set<String> TOOL_TOKENS = Set.of(
            "pickaxe", "axe", "hatchet", "shovel", "spade", "hoe", "hammer", "excavator",
            "mattock", "kama", "scythe", "paxel", "sickle", "saw", "shears", "fishing_rod",
            "prospector", "chisel", "wrench"
    );
    private static final Set<String> WEAPON_TOKENS = Set.of(
            "sword", "dagger", "cleaver", "katana", "rapier", "broadsword", "longsword",
            "mace", "spear", "bow", "crossbow", "slingshot", "arrow", "bolt"
    );
    private static final Set<String> ARMOR_TOKENS = Set.of(
            "helmet", "chestplate", "leggings", "boots", "shield", "elytra"
    );
    private static final Set<String> PART_TOKENS = Set.of(
            "head", "blade", "binding", "handle", "rod", "tool_rod", "tough_handle",
            "plate", "large_plate", "guard", "grip", "limb", "bow_limb", "bowstring",
            "fletching", "tip", "cord", "lining", "trim", "chain", "mail", "plating",
            "coating", "repair_kit", "upgrade_base", "part"
    );
    private static final Set<String> MATERIAL_TOKENS = Set.of(
            "ingot", "nugget", "gem", "dust", "ore", "raw", "alloy", "slime", "crystal",
            "shard", "string", "cloth", "fiber", "leather", "hide", "wood", "metal",
            "compound", "blend"
    );
    private static final Set<String> STATION_TOKENS = Set.of(
            "station", "builder", "table", "anvil", "forge", "melter", "basin",
            "faucet", "drain", "controller", "tank", "heater", "grader",
            "refabricator", "salvager", "starlight_charger"
    );
    private static final Set<String> BLUEPRINT_TOKENS = Set.of(
            "blueprint", "template", "pattern", "schematic", "stencil"
    );
    private static final Set<String> MODIFIER_TOKENS = Set.of(
            "modifier", "trait", "ability", "upgrade", "augment", "embellishment", "gilding",
            "reinforcement"
    );
    private static final Set<String> KNOWN_MATERIALS = Set.of(
            "wood", "stone", "flint", "bone", "copper", "iron", "gold", "diamond", "emerald",
            "netherite", "amethyst", "obsidian", "steel", "bronze", "brass", "invar", "electrum",
            "constantan", "crimson_iron", "azure_silver", "blaze_gold", "tyrian_steel",
            "manyullyn", "cobalt", "ardite", "slimesteel", "amethyst_bronze", "hepatizon",
            "queens_slime", "rose_gold", "nahuatl", "pig_iron", "knightslime"
    );
    private static final Set<String> RUNTIME_MATERIAL_LABELS = Set.of(
            "material", "materials", "main material", "head material", "rod material",
            "handle material", "binding material", "part material"
    );
    private static final Set<String> RUNTIME_TRAIT_LABELS = Set.of(
            "trait", "traits", "ability", "abilities", "modifier", "modifiers",
            "upgrade", "upgrades", "effect", "effects"
    );
    private static final Set<String> RUNTIME_STAT_LABELS = Set.of(
            "durability", "mining speed", "harvest speed", "attack damage", "attack speed",
            "armor", "armor toughness", "toughness", "draw speed", "draw time",
            "projectile damage", "ranged damage", "velocity", "enchantability",
            "repair efficiency", "magic capacity", "chargeability", "rarity"
    );
    private static final int MAX_RUNTIME_MATERIALS = 8;
    private static final int MAX_RUNTIME_TRAITS = 12;
    private static final int MAX_RUNTIME_STATS = 8;
    private static final int MAX_RUNTIME_STAT_LENGTH = 48;

    private ModularGearCompat() {
    }

    public static void enrichItem(Identifier id, Map<String, String> meta) {
        if (id == null || meta == null) {
            return;
        }
        if (meta.getOrDefault(SearchNodeKeys.COMPAT_FAMILIES, "").isBlank()) {
            CompatFamilyDetector.detect(id, meta);
        }
        String family = family(id, meta);
        if (family.isBlank()) {
            return;
        }

        addCompatFamily(meta, CompatFamilyDetector.MODULAR_GEAR);
        addCompatFamily(meta, family);
        meta.put(SearchNodeKeys.MODULAR_GEAR_FAMILY, family);
        addSearchToken(meta, CompatFamilyDetector.MODULAR_GEAR);
        addSearchToken(meta, family);

        Context context = new Context(id, meta);
        LinkedHashSet<String> facts = new LinkedHashSet<>();
        addClassFacts(context, facts);
        addPathFacts(context, facts);
        addTagFacts(context, facts);

        String kind = classifyKind(context, facts);
        if (!kind.isBlank()) {
            meta.put(SearchNodeKeys.MODULAR_GEAR_ITEM_KIND, kind);
            addSearchToken(meta, "modular_gear_" + kind);
            addSearchToken(meta, kind);
        }
        String material = classifyMaterial(context.path);
        if (!material.isBlank()) {
            meta.put(SearchNodeKeys.MODULAR_GEAR_MATERIAL, material);
            meta.put(SearchNodeKeys.MODULAR_GEAR_TIER, material);
            addSearchToken(meta, material);
            addSearchToken(meta, "gear_material_" + material);
        }
        String part = classifyPart(context.path, facts);
        if (!part.isBlank()) {
            meta.put(SearchNodeKeys.MODULAR_GEAR_PART, part);
            addSearchToken(meta, part);
            addSearchToken(meta, "gear_part_" + part);
        }
        if (!facts.isEmpty()) {
            meta.put(SearchNodeKeys.MODULAR_GEAR_FACTS, join(facts));
            for (String fact : facts) {
                addSearchToken(meta, fact);
            }
        }
    }

    public static void enrichRuntimeStack(Identifier id, ItemStack stack, @Nullable Level level, Map<String, String> meta) {
        if (id == null || stack == null || stack.isEmpty() || meta == null) {
            return;
        }
        String family = family(id, meta);
        if (family.isBlank()) {
            return;
        }

        List<String> tooltipLines = Services.PLATFORM.getTooltipLines(stack, level).stream()
                .map(Component::getString)
                .toList();
        RuntimeFacts facts = extractRuntimeFacts(tooltipLines);
        if (!facts.hasAny()) {
            return;
        }

        if (!facts.materials().isEmpty()) {
            meta.put(SearchNodeKeys.MODULAR_GEAR_RUNTIME_MATERIALS, join(facts.materials()));
            for (String material : facts.materials()) {
                addSearchToken(meta, material);
                addSearchToken(meta, "gear_material_" + material);
            }
        }
        if (!facts.traits().isEmpty()) {
            meta.put(SearchNodeKeys.MODULAR_GEAR_RUNTIME_TRAITS, join(facts.traits()));
            for (String trait : facts.traits()) {
                addSearchToken(meta, trait);
                addSearchToken(meta, "gear_trait_" + trait);
            }
        }
        if (!facts.stats().isEmpty()) {
            meta.put(SearchNodeKeys.MODULAR_GEAR_RUNTIME_STATS, String.join(" | ", facts.stats()));
        }
    }

    public static RuntimeFacts extractRuntimeFacts(Collection<String> tooltipLines) {
        if (tooltipLines == null || tooltipLines.isEmpty()) {
            return RuntimeFacts.empty();
        }

        LinkedHashSet<String> materials = new LinkedHashSet<>();
        LinkedHashSet<String> traits = new LinkedHashSet<>();
        List<String> stats = new ArrayList<>();

        boolean firstLine = true;
        for (String rawLine : tooltipLines) {
            String line = cleanTooltipLine(rawLine);
            if (line.isBlank()) {
                continue;
            }
            if (firstLine) {
                firstLine = false;
                if (!line.contains(":")) {
                    continue;
                }
            }

            LabelValue labelValue = splitLabelValue(line);
            if (labelValue == null) {
                continue;
            }
            String label = labelValue.label();
            String value = labelValue.value();
            if (value.isBlank()) {
                continue;
            }

            if (RUNTIME_MATERIAL_LABELS.contains(label)) {
                addNormalizedTokens(materials, value, MAX_RUNTIME_MATERIALS);
            } else if (RUNTIME_TRAIT_LABELS.contains(label)) {
                addNormalizedTokens(traits, value, MAX_RUNTIME_TRAITS);
            } else if (RUNTIME_STAT_LABELS.contains(label) && stats.size() < MAX_RUNTIME_STATS) {
                stats.add(capitalizeWords(label) + ": " + abbreviate(value, MAX_RUNTIME_STAT_LENGTH));
            }
        }

        return new RuntimeFacts(List.copyOf(materials), List.copyOf(traits), List.copyOf(stats));
    }

    private static String family(Identifier id, Map<String, String> meta) {
        String namespace = id.getNamespace().toLowerCase(Locale.ROOT);
        if ("tconstruct".equals(namespace)) {
            return CompatFamilyDetector.TINKERS;
        }
        if ("silentgear".equals(namespace)) {
            return CompatFamilyDetector.SILENT_GEAR;
        }
        if (CompatFamilyDetector.hasFamily(meta, CompatFamilyDetector.TINKERS)) {
            return CompatFamilyDetector.TINKERS;
        }
        if (CompatFamilyDetector.hasFamily(meta, CompatFamilyDetector.SILENT_GEAR)) {
            return CompatFamilyDetector.SILENT_GEAR;
        }
        if (CompatFamilyDetector.hasFamily(meta, CompatFamilyDetector.MODULAR_GEAR)) {
            return CompatFamilyDetector.MODULAR_GEAR;
        }
        return "";
    }

    private static void addClassFacts(Context context, Set<String> facts) {
        PathTokens itemClass = PathTokens.of(context.itemClass);
        PathTokens blockClass = PathTokens.of(context.blockClass);
        if (itemClass.containsAny("tool", "gear_item", "pickaxe", "axe", "shovel", "hoe")) facts.add("tool");
        if (itemClass.containsAny("sword", "weapon", "bow", "projectile")) facts.add("weapon");
        if (itemClass.containsAny("armor", "shield")) facts.add("armor");
        if (itemClass.containsAny("part", "material", "ingredient")) facts.add("part");
        if (itemClass.containsAny("blueprint", "template", "pattern", "stencil")) facts.add("blueprint");
        if (itemClass.containsAny("modifier", "trait", "upgrade")) facts.add("modifier");
        if (CompatFamilyDetector.TINKERS.equals(context.family)
                && context.itemClass.toLowerCase(Locale.ROOT).contains("creativeslotitem")) {
            facts.add("modifier");
        }
        if (blockClass.containsAny("station", "table", "anvil", "melter", "grader")) {
            facts.add("station");
        }
    }

    private static void addPathFacts(Context context, Set<String> facts) {
        PathTokens path = context.pathTokens;
        if (path.containsAny(BLUEPRINT_TOKENS)) facts.add("blueprint");
        if (path.containsAny(MODIFIER_TOKENS)) facts.add("modifier");
        if (isStationPath(context)) facts.add("station");
        if (isPartPath(context, facts)) facts.add("part");
        if (path.containsAny(MATERIAL_TOKENS)) facts.add("material");
        if (path.containsAny(TOOL_TOKENS)) facts.add("tool");
        if (path.containsAny(WEAPON_TOKENS)) facts.add("weapon");
        if (path.containsAny(ARMOR_TOKENS)) facts.add("armor");
        if (path.contains("repair")) facts.add("repair");
        if (path.contains("cast")) facts.add("casting");
        if (path.containsAny("guide", "book")) facts.add("guide");
    }

    private static void addTagFacts(Context context, Set<String> facts) {
        if (hasAnyTag(context.tags, "tconstruct:modifiable", "silentgear:tools")) facts.add("tool");
        if (hasAnyTag(context.tags, "silentgear:blueprints", "silentgear:templates")) facts.add("blueprint");
        if (hasAnyTag(context.tags, "tconstruct:modifiable/armor", "silentgear:armor")) facts.add("armor");
        if (hasAnyTag(context.tags, "tconstruct:parts", "silentgear:parts")) facts.add("part");
        if (hasAnyTag(context.tags, "tconstruct:modifiers", "silentgear:upgrades")) facts.add("modifier");
        if (hasAnyToken(context.facets, "harvest_tool", "utility_tool")) facts.add("tool");
        if (hasAnyToken(context.facets, "melee_weapon", "ranged_weapon")) facts.add("weapon");
        if (hasAnyToken(context.facets, "armor_head", "armor_chest", "armor_legs", "armor_feet")) facts.add("armor");
        if (hasAnyToken(context.facets, "template")) facts.add("blueprint");
        if (hasAnyToken(context.facets, "upgrade")) facts.add("modifier");
        if (hasAnyToken(context.facets, "ingot", "nugget", "dust", "gem", "raw_material", "tech_component")) facts.add("material");
    }

    private static String classifyKind(Context context, Set<String> facts) {
        if (facts.contains("guide") && hasAnyToken(context.facets, "guide_book")) return "";
        if (facts.contains("blueprint")) return "blueprints";
        if (facts.contains("station") || hasAnyToken(context.facets, "workstation")) return "stations";
        if (facts.contains("modifier")) return "modifiers";
        if (facts.contains("casting")) return "parts";
        if (facts.contains("part")) return "parts";
        if (facts.contains("weapon")) return "weapons";
        if (facts.contains("tool")) return "tools";
        if (facts.contains("armor")) return "armor";
        if (facts.contains("material")) return "materials";
        if (facts.contains("guide")) return "misc";
        return "";
    }

    private static String classifyMaterial(String path) {
        PathTokens tokens = PathTokens.of(path);
        for (String material : KNOWN_MATERIALS) {
            if (tokens.contains(material)) {
                return material;
            }
        }
        for (String suffix : MATERIAL_TOKENS) {
            String marker = "_" + suffix;
            if (path.endsWith(marker) && path.length() > marker.length()) {
                return path.substring(0, path.length() - marker.length());
            }
        }
        return "";
    }

    private static String classifyPart(String path, Set<String> facts) {
        if (!facts.contains("part") && !facts.contains("blueprint") && !facts.contains("casting")) {
            return "";
        }
        PathTokens tokens = PathTokens.of(path);
        for (String part : PART_TOKENS) {
            if (tokens.contains(part)) {
                return part;
            }
        }
        for (String blueprint : BLUEPRINT_TOKENS) {
            if (tokens.endsWith(blueprint) && path.length() > blueprint.length() + 1) {
                return path.substring(0, path.length() - blueprint.length() - 1);
            }
        }
        return "";
    }

    private static boolean isStationPath(Context context) {
        if (!CompatFamilyDetector.TINKERS.equals(context.family)) {
            return context.pathTokens.containsAny(STATION_TOKENS);
        }
        PathTokens path = context.pathTokens;
        if (path.containsAny("station", "builder", "anvil", "worktable", "controller",
                "melter", "heater", "alloyer", "basin", "table", "faucet", "channel",
                "drain", "duct", "chute")) {
            return true;
        }
        if (path.containsAny("fuel tank", "fuel gauge", "ingot tank", "ingot gauge")) {
            return true;
        }
        return path.contains("tank") && !path.containsAny("tinker", "proxy");
    }

    private static boolean isPartPath(Context context, Set<String> facts) {
        if (!CompatFamilyDetector.TINKERS.equals(context.family)) {
            return context.pathTokens.containsAny(PART_TOKENS);
        }
        PathTokens path = context.pathTokens;
        if (hasAnyTag(context.tags, "tconstruct:parts")) {
            return true;
        }
        if (path.containsAny("binding", "handle", "tough handle", "tool handle", "bowstring",
                "bow limb", "bow grip", "fletching", "plating", "maille", "repair kit",
                "shield core", "large plate", "plate shield", "part")) {
            return true;
        }
        if (path.containsAny("pick head", "hammer head", "arrow head", "adze head",
                "small axe head", "small blade", "broad blade", "broad axe head")) {
            return true;
        }
        return facts.contains("casting") && path.containsAny("head", "blade", "binding",
                "handle", "plate", "plating", "shield", "limb", "grip", "rod", "fletching");
    }

    private static boolean hasAnyTag(String csv, String... tags) {
        Set<String> expected = Set.of(tags);
        for (String value : splitCsv(csv)) {
            if (expected.contains(value)) {
                return true;
            }
        }
        return false;
    }

    private static boolean hasAnyToken(String csv, String... tokens) {
        Set<String> expected = Set.of(tokens);
        for (String value : splitCsv(csv)) {
            if (expected.contains(value)) {
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

    private static String join(List<String> values) {
        StringJoiner joiner = new StringJoiner(",");
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                joiner.add(value);
            }
        }
        return joiner.toString();
    }

    private static String cleanTooltipLine(String raw) {
        if (raw == null) {
            return "";
        }
        return raw.replaceAll("(?i)§[0-9A-FK-OR]", "")
                .replaceAll("\\s+", " ")
                .trim();
    }

    private static LabelValue splitLabelValue(String line) {
        int colon = line.indexOf(':');
        if (colon <= 0 || colon >= line.length() - 1) {
            return null;
        }
        String label = normalizeLabel(line.substring(0, colon));
        String value = line.substring(colon + 1).trim();
        return label.isBlank() ? null : new LabelValue(label, value);
    }

    private static String normalizeLabel(String label) {
        return label == null ? "" : label.toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z0-9 ]", " ")
                .replaceAll("\\s+", " ")
                .trim();
    }

    private static void addNormalizedTokens(Set<String> out, String raw, int limit) {
        for (String part : raw.split("[,;/|]+")) {
            if (out.size() >= limit) {
                return;
            }
            String token = normalizeRuntimeToken(part);
            if (!token.isBlank()) {
                out.add(token);
            }
        }
    }

    private static String normalizeRuntimeToken(String raw) {
        if (raw == null) {
            return "";
        }
        String normalized = raw.toLowerCase(Locale.ROOT)
                .replaceAll("\\([^)]*\\)", " ")
                .replaceAll("[^a-z0-9_\\- ]", " ")
                .replace('-', '_')
                .replaceAll("\\s+", "_")
                .replaceAll("_+", "_")
                .replaceAll("^_|_$", "");
        if (normalized.length() < 2 || normalized.matches("\\d+")) {
            return "";
        }
        return normalized;
    }

    private static String capitalizeWords(String raw) {
        StringBuilder out = new StringBuilder();
        for (String word : raw.split("\\s+")) {
            if (word.isBlank()) {
                continue;
            }
            if (out.length() > 0) {
                out.append(' ');
            }
            out.append(word.substring(0, 1).toUpperCase(Locale.ROOT));
            if (word.length() > 1) {
                out.append(word.substring(1).toLowerCase(Locale.ROOT));
            }
        }
        return out.toString();
    }

    private static String abbreviate(String raw, int maxLength) {
        String trimmed = raw.replaceAll("\\s+", " ").trim();
        if (trimmed.length() <= maxLength) {
            return trimmed;
        }
        return trimmed.substring(0, Math.max(0, maxLength - 3)).trim() + "...";
    }

    private static void addCompatFamily(Map<String, String> meta, String family) {
        LinkedHashSet<String> families = new LinkedHashSet<>();
        String existing = meta.getOrDefault(SearchNodeKeys.COMPAT_FAMILIES, "");
        for (String value : existing.split("[,\\s]+")) {
            if (!value.isBlank()) {
                families.add(value.trim());
            }
        }
        if (families.add(family)) {
            meta.put(SearchNodeKeys.COMPAT_FAMILIES, String.join(",", families));
        }
        if (meta.getOrDefault(SearchNodeKeys.PRIMARY_COMPAT_FAMILY, "").isBlank()) {
            meta.put(SearchNodeKeys.PRIMARY_COMPAT_FAMILY, family);
            meta.put(SearchNodeKeys.COMPAT_FAMILY, family);
        }
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
        final String family;
        final PathTokens pathTokens;

        Context(Identifier id, Map<String, String> meta) {
            this.path = id.getPath().toLowerCase(Locale.ROOT);
            this.itemClass = meta.getOrDefault(SearchNodeKeys.ITEM_CLASS, "");
            this.blockClass = meta.getOrDefault(SearchNodeKeys.BLOCK_CLASS, "");
            this.tags = meta.getOrDefault(SearchNodeKeys.TAGS, "").toLowerCase(Locale.ROOT);
            this.facets = meta.getOrDefault(SearchNodeKeys.FACETS, "").toLowerCase(Locale.ROOT);
            this.family = meta.getOrDefault(SearchNodeKeys.MODULAR_GEAR_FAMILY,
                    meta.getOrDefault(SearchNodeKeys.PRIMARY_COMPAT_FAMILY, "")).toLowerCase(Locale.ROOT);
            this.pathTokens = PathTokens.of(this.path);
        }
    }

    private record LabelValue(String label, String value) {
    }

    public record RuntimeFacts(List<String> materials, List<String> traits, List<String> stats) {
        static RuntimeFacts empty() {
            return new RuntimeFacts(List.of(), List.of(), List.of());
        }

        boolean hasAny() {
            return !materials.isEmpty() || !traits.isEmpty() || !stats.isEmpty();
        }
    }
}
