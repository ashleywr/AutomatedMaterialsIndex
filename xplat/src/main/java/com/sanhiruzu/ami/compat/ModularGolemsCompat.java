package com.sanhiruzu.ami.compat;

import com.sanhiruzu.ami.index.SearchNodeKeys;
import net.minecraft.resources.ResourceLocation;

import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.StringJoiner;

public final class ModularGolemsCompat {
    public static final String MOD_ID = "modulargolems";

    private ModularGolemsCompat() {
    }

    public static void enrichItem(ResourceLocation id, Map<String, String> meta) {
        if (id == null || meta == null) {
            return;
        }
        if (meta.getOrDefault(SearchNodeKeys.COMPAT_FAMILIES, "").isBlank()) {
            CompatFamilyDetector.detect(id, meta);
        }
        if (!isModularGolemsItem(id, meta)) {
            return;
        }

        addCompatFamily(meta, CompatFamilyDetector.MODULAR_GOLEMS);
        addSearchToken(meta, CompatFamilyDetector.MODULAR_GOLEMS);

        Context context = new Context(id, meta);
        LinkedHashSet<String> facts = new LinkedHashSet<>();
        addClassFacts(context, facts);
        addTagFacts(context, facts);
        addPathFacts(context, facts);

        String kind = classifyKind(context, facts);
        if (!kind.isBlank()) {
            meta.put(SearchNodeKeys.MODULAR_GOLEMS_ITEM_KIND, kind);
            addSearchToken(meta, "modular_golems_" + kind);
            addSearchToken(meta, kind);
        }

        String golemType = classifyGolemType(context.path);
        if (!golemType.isBlank()) {
            meta.put(SearchNodeKeys.MODULAR_GOLEMS_GOLEM_TYPE, golemType);
            addSearchToken(meta, golemType);
        }

        String part = classifyPart(context, facts);
        if (!part.isBlank()) {
            meta.put(SearchNodeKeys.MODULAR_GOLEMS_PART, part);
            addSearchToken(meta, part);
        }

        if (!facts.isEmpty()) {
            meta.put(SearchNodeKeys.MODULAR_GOLEMS_FACTS, join(facts));
            for (String fact : facts) {
                addSearchToken(meta, fact);
            }
        }

        applyGeneratedVariantCollapse(id, context, kind, meta);
    }

    private static boolean isModularGolemsItem(ResourceLocation id, Map<String, String> meta) {
        return MOD_ID.equals(id.getNamespace())
                || CompatFamilyDetector.hasFamily(meta, CompatFamilyDetector.MODULAR_GOLEMS);
    }

    private static void addClassFacts(Context context, Set<String> facts) {
        if (containsAny(context.itemClass, "GolemPart")) facts.add("part");
        if (containsAny(context.itemClass, "GolemHolder")) facts.add("holder");
        if (containsAny(context.itemClass, "GolemFacade")) facts.add("facade");
        if (containsAny(context.itemClass, "ConfigCard")) facts.add("config_card");
        if (containsAny(context.itemClass, "NameFilterCard", "EntityTypeFilterCard", "UuidFilterCard", "DefaultFilterCard")) {
            facts.add("filter_card");
        }
        if (containsAny(context.itemClass, "PathRecordCard")) facts.add("route_card");
        if (containsAny(context.itemClass, "SimpleUpgradeItem")) facts.add("upgrade");
        if (containsAny(context.itemClass, "AddSlotTemplate")) facts.add("template");
        if (isKnownGolemArmor(context)) facts.add("golem_armor");
        if (containsAny(context.itemClass, "MetalGolemBowItem", "MetalGolemMechaBowItem", "SonicCannonItem",
                "BeaconCannonItem", "FlameThrowerItem")) {
            facts.add("ranged_weapon");
        }
        if (containsAny(context.itemClass, "MetalGolemWeaponItem", "TFMetalGolemWeaponItem",
                "KnightmetalMetalGolemWeaponItem", "FieryMetalGolemWeaponItem")) {
            facts.add("weapon");
        }
        if (containsAny(context.itemClass, "SlicingAxe")) facts.add("harvest_tool");
        if (containsAny(context.itemClass, "WandItem", "DispenseWand")) facts.add("wand");
        if (containsAny(context.blockClass, "GolemWorkbench")) facts.add("workstation");
    }

    private static void addTagFacts(Context context, Set<String> facts) {
        if (containsAny(context.tags, "modulargolems:parts")) facts.add("part");
        if (containsAny(context.tags, "modulargolems:holders")) facts.add("holder");
        if (containsAny(context.tags, "modulargolems:config_card")) facts.add("config_card");
        if (containsAny(context.tags, "modulargolems:upgrades")) facts.add("upgrade");
        if (containsAny(context.tags, "modulargolems:golem_interact")) facts.add("golem_interact");
        if (containsAny(context.tags, "modulargolems:tough_item")) facts.add("tough_item");
        if (containsAny(context.tags, "modulargolems:shield_breaker_weapons")) facts.add("weapon");
        if (containsAny(context.tags, "curios:golem_skin")) facts.add("facade");
        if (containsAny(context.tags, "curios:golem_route")) facts.add("route_card");
    }

    private static void addPathFacts(Context context, Set<String> facts) {
        String path = context.path;
        if (path.endsWith("_template")) facts.add("template");
        if (path.contains("config_card")) facts.add("config_card");
        if (path.contains("target_filter")) facts.add("filter_card");
        if (path.contains("path_record") || path.contains("patrol_path")) facts.add("route_card");
        if (path.contains("golem_facade")) facts.add("facade");
        if (path.contains("golem_holder")) facts.add("holder");
        if (path.contains("golem_body") || path.contains("golem_arm") || path.contains("golem_legs")) facts.add("part");
        if (path.contains("dog_golem_armor")) facts.add("golem_armor");
        if (path.contains("cannon") || path.contains("flame_thrower") || path.contains("mecha_bow")) facts.add("ranged_weapon");
        if (path.contains("golem_workbench")) facts.add("workstation");
    }

    private static String classifyKind(Context context, Set<String> facts) {
        if (facts.contains("golem_armor")) return "golem_armor";
        if (facts.contains("ranged_weapon")) return "ranged_weapons";
        if (facts.contains("harvest_tool")) return "harvest_tools";
        if (facts.contains("weapon")) return "weapons";
        if (facts.contains("route_card")) return "route_cards";
        if (facts.contains("filter_card") || facts.contains("config_card")) return "cards";
        if (facts.contains("template")) return "templates";
        if (facts.contains("upgrade")) return "upgrades";
        if (facts.contains("facade")) return "facades";
        if (facts.contains("holder")) return "holders";
        if (facts.contains("part")) return "parts";
        if (facts.contains("workstation") || context.path.contains("workbench")) return "workstations";
        if (facts.contains("wand")) return "wands";
        return "";
    }

    private static boolean isKnownGolemArmor(Context context) {
        if (!containsAny(context.itemClass, ".equipments.")) {
            return false;
        }
        if (containsAny(context.itemClass,
                "DogGolemArmorItem",
                "MetalGolemArmorItem",
                "MetalGolemBeaconItem",
                "NetheriteBootItem")) {
            return true;
        }
        return isCompatMaterialGolemArmor(context);
    }

    private static boolean isCompatMaterialGolemArmor(Context context) {
        if (!containsAny(context.itemClass, "compat.materials.")) {
            return false;
        }
        if (!containsAny(context.itemClass, "ArmorItem")) {
            return false;
        }
        return isArmorSlotPath(context.path);
    }

    private static boolean isArmorSlotPath(String path) {
        return containsAny(path, "_helmet", "_chestplate", "_shinguard", "_boots");
    }

    private static String classifyGolemType(String path) {
        String root = rootPath(path);
        if (root.contains("dog_golem")) return "dog_golem";
        if (root.contains("metal_golem")) return "metal_golem";
        if (root.contains("humanoid_golem")) return "humanoid_golem";
        return "";
    }

    private static String classifyPart(Context context, Set<String> facts) {
        if (facts.contains("holder")) return "holder";
        if (facts.contains("facade")) return "facade";
        if (facts.contains("route_card")) return "route_card";
        if (facts.contains("filter_card")) return "filter_card";
        if (facts.contains("config_card")) return "config_card";
        String root = rootPath(context.path);
        if (root.contains("body")) return "body";
        if (root.contains("arm")) return "arm";
        if (root.contains("legs")) return "legs";
        if (root.contains("helmet")) return "helmet";
        if (root.contains("chestplate")) return "chestplate";
        if (root.contains("shinguard")) return "shinguard";
        if (root.contains("boots")) return "boots";
        if (facts.contains("part")) return "part";
        return "";
    }

    private static void applyGeneratedVariantCollapse(ResourceLocation id, Context context, String kind, Map<String, String> meta) {
        if (!context.path.contains("/variant/")) {
            return;
        }
        if (!Set.of("parts", "holders", "facades").contains(kind)) {
            return;
        }
        String family = meta.getOrDefault(SearchNodeKeys.SUBTYPE_OF, "");
        if (family.isBlank()) {
            family = id.getNamespace() + ":" + rootPath(context.path);
        }
        meta.putIfAbsent(SearchNodeKeys.COLLAPSE_FAMILY, family);
        meta.putIfAbsent(SearchNodeKeys.COLLAPSE_LABEL, titleCase(rootPath(context.path)));
        meta.putIfAbsent(SearchNodeKeys.VARIANT_COLLAPSE_MODE, "default_collapsed");
    }

    private static String rootPath(String path) {
        int variantIndex = path.indexOf("/variant/");
        return variantIndex >= 0 ? path.substring(0, variantIndex) : path;
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

    private static String join(Set<String> values) {
        StringJoiner joiner = new StringJoiner(",");
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                joiner.add(value);
            }
        }
        return joiner.toString();
    }

    private static String titleCase(String value) {
        StringJoiner joiner = new StringJoiner(" ");
        for (String token : value.split("[_/]+")) {
            if (token.isBlank()) {
                continue;
            }
            joiner.add(token.substring(0, 1).toUpperCase(Locale.ROOT) + token.substring(1));
        }
        return joiner.toString();
    }

    private static void addCompatFamily(Map<String, String> meta, String family) {
        if (family == null || family.isBlank()) {
            return;
        }
        LinkedHashSet<String> values = new LinkedHashSet<>();
        String existing = meta.getOrDefault(SearchNodeKeys.COMPAT_FAMILIES, "");
        for (String value : existing.split(",")) {
            if (!value.isBlank()) {
                values.add(value.trim());
            }
        }
        if (values.add(family)) {
            meta.put(SearchNodeKeys.COMPAT_FAMILIES, String.join(",", values));
        }
        meta.putIfAbsent(SearchNodeKeys.PRIMARY_COMPAT_FAMILY, family);
        meta.putIfAbsent(SearchNodeKeys.COMPAT_FAMILY, family);
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

        Context(ResourceLocation id, Map<String, String> meta) {
            this.path = id.getPath().toLowerCase(Locale.ROOT);
            this.itemClass = meta.getOrDefault(SearchNodeKeys.ITEM_CLASS, "");
            this.blockClass = meta.getOrDefault(SearchNodeKeys.BLOCK_CLASS, "");
            this.tags = meta.getOrDefault(SearchNodeKeys.TAGS, "").toLowerCase(Locale.ROOT);
        }
    }
}
