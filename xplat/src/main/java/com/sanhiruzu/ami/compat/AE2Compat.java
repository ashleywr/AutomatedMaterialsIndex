package com.sanhiruzu.ami.compat;

import com.sanhiruzu.ami.index.SearchNodeKeys;
import net.minecraft.resources.ResourceLocation;

import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.StringJoiner;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class AE2Compat {
    private static final String MOD_ID = "ae2";
    private static final Pattern STORAGE_TIER = Pattern.compile("(^|_)(\\d+k|256k|2|16|128)(_|$)");

    private AE2Compat() {
    }

    public static void enrichItem(ResourceLocation id, Map<String, String> meta) {
        if (id == null || meta == null) {
            return;
        }
        if (meta.getOrDefault(SearchNodeKeys.COMPAT_FAMILIES, "").isBlank()) {
            CompatFamilyDetector.detect(id, meta);
        }
        if (!isAe2FamilyItem(id, meta)) {
            return;
        }

        Context context = new Context(id, meta);
        LinkedHashSet<String> facts = new LinkedHashSet<>();

        addGuideBookFacts(id, meta, context, facts);
        addClassFacts(context, facts);
        addPathFacts(context, facts);
        addTagFacts(context, facts);
        addRecipeFacts(context, facts);

        String kind = classifyKind(context, facts);
        if (!kind.isBlank()) {
            meta.put(SearchNodeKeys.AE2_ITEM_KIND, kind);
            addSearchToken(meta, "ae2_" + kind);
        }
        String storageTier = storageTier(context.path);
        if (!storageTier.isBlank()) {
            meta.put(SearchNodeKeys.AE2_STORAGE_TIER, storageTier);
            addSearchToken(meta, "ae2_" + storageTier);
        }
        String storageMedium = storageMedium(context.path);
        if (!storageMedium.isBlank()) {
            meta.put(SearchNodeKeys.AE2_STORAGE_MEDIUM, storageMedium);
            addSearchToken(meta, "ae2_" + storageMedium);
            addSearchToken(meta, storageMedium);
        }
        if (!facts.isEmpty()) {
            meta.put(SearchNodeKeys.AE2_FACTS, join(facts));
            for (String fact : facts) {
                addSearchToken(meta, fact);
            }
        }
    }

    private static boolean isAe2FamilyItem(ResourceLocation id, Map<String, String> meta) {
        return MOD_ID.equals(id.getNamespace())
                || "appmek".equals(id.getNamespace())
                || "appliedenergistics2".equals(id.getNamespace())
                || CompatFamilyDetector.hasFamily(meta, CompatFamilyDetector.AE2);
    }

    private static void addClassFacts(Context context, Set<String> facts) {
        if (containsAny(context.itemClass, "WirelessTerminalItem", "Terminal")
                || containsAny(context.blockClass, "Terminal", "PatternAccessTerminal")) {
            facts.add("terminal");
        }
        if (containsAny(context.itemClass, "PortableCellItem", "StorageCell")
                || containsAny(context.blockClass, "DriveBlock", "ChestBlock")) {
            facts.add("storage");
        }
        if (containsAny(context.itemClass, "ChemicalStorageCell", "ChemicalPortableCellItem")) {
            facts.add("storage");
            facts.add("chemical");
        }
        if (containsAny(context.itemClass, "EncodedPatternItem", "Pattern")
                || containsAny(context.blockClass, ".crafting.", "MolecularAssembler", "Inscriber", "PatternProvider")) {
            facts.add("crafting");
        }
        if (containsAny(context.itemClass, "PartItem", "FacadeItem")
                || containsAny(context.blockClass, "CableBusBlock")) {
            facts.add("network_part");
        }
        if (containsAny(context.blockClass, "ControllerBlock", "EnergyAcceptorBlock", "EnergyCellBlock")) {
            facts.add("network_core");
        }
        if (containsAny(context.itemClass, "Quartz", "Fluix", "MatterCannon", "EntropyManipulator", "ColorApplicator", "NetworkTool")) {
            facts.add("tool_or_material");
        }
        if (containsAny(context.itemClass, "AxeItem", "HoeItem", "SpadeItem", "PickaxeItem", "SwordItem", "WrenchItem", "CuttingKnifeItem", "NetworkToolItem")) {
            facts.add("tool");
        }
    }

    private static void addGuideBookFacts(ResourceLocation id, Map<String, String> meta, Context context, Set<String> facts) {
        if (!"guide".equals(context.path) && !containsAny(context.itemClass, "GuideItem")) {
            return;
        }
        meta.put(SearchNodeKeys.GUIDE_BOOK_CANDIDATE, "true");
        meta.put(SearchNodeKeys.GUIDE_BOOK_SYSTEM, "guideme");
        meta.put(SearchNodeKeys.GUIDE_BOOK_ID, id.getNamespace() + ":guide");
        addSearchToken(meta, "guideme");
        addSearchToken(meta, "guide");
        facts.add("guide");
        facts.add("utility");
    }

    private static void addPathFacts(Context context, Set<String> facts) {
        String path = context.path;
        if (hasAnyPathToken(path, "terminal", "wireless_terminal", "wireless_crafting_terminal")) facts.add("terminal");
        if (containsAny(path, "portable_item_cell", "portable_fluid_cell", "portable_chemical_cell",
                "storage_cell", "chemical_storage_cell", "cell_component", "cell_housing", "chemical_cell_housing")
                || hasAnyPathToken(path, "drive", "chest")) {
            facts.add("storage");
        }
        if (containsAny(path, "chemical_storage_cell", "portable_chemical_cell", "chemical_cell_housing", "chemical_p2p_tunnel")) {
            facts.add("chemical");
        }
        if (containsAny(path, "spatial_cell", "spatial_pylon", "spatial_io", "spatial_anchor")) facts.add("spatial");
        if (containsAny(path, "crafting_unit", "crafting_storage", "crafting_accelerator", "crafting_monitor")
                || containsAny(path, "pattern_provider", "molecular_assembler", "encoded_pattern", "blank_pattern")
                || hasAnyPathToken(path, "inscriber", "pattern")) {
            facts.add("crafting");
        }
        if (containsAny(path, "smart_cable", "covered_cable", "glass_cable", "dense_cable", "p2p_tunnel", "chemical_p2p_tunnel")
                || hasAnyPathToken(path, "bus", "cable", "interface", "annihilation", "formation", "export", "import")) {
            facts.add("channel_network");
        }
        if (containsAny(path, "controller", "energy_acceptor", "energy_cell", "dense_energy_cell", "security_station", "quantum")
                || hasAnyPathToken(path, "controller")) {
            facts.add("network_core");
        }
        if (containsAny(path, "printed_")
                || hasAnyPathToken(path, "processor", "press", "silicon", "calculation", "engineering", "logic")) {
            facts.add("processor_chain");
        }
        if (containsAny(path, "certus", "fluix", "quartz", "sky_stone", "sky_dust", "matter_ball", "singularity")) {
            facts.add("material");
        }
        if (containsAny(path, "wireless_booster")) facts.add("network_core");
        if (containsAny(path, "guide")) facts.add("utility");
        if (containsAny(path, "paint_ball", "lumen_paint_ball", "facade")) facts.add("utility");
    }

    private static void addTagFacts(Context context, Set<String> facts) {
        if (hasToken(context.tags, "ae2:inscriber_presses")) facts.add("processor_chain");
        if (hasToken(context.tags, "ae2:paint_balls") || hasToken(context.tags, "ae2:lumen_paint_balls")) facts.add("utility");
        if (containsAny(context.tags, "ae2:smart_cable", "ae2:covered_cable", "ae2:glass_cable", "ae2:p2p_attunements")) {
            facts.add("channel_network");
        }
    }

    private static void addRecipeFacts(Context context, Set<String> facts) {
        if (hasToken(context.recipeCategories, "inscriber") || hasToken(context.recipeUseCategories, "inscriber")) {
            facts.add("processor_chain");
        }
        if (hasToken(context.recipeCategories, "charger") || hasToken(context.recipeUseCategories, "charger")) {
            facts.add("charged_material");
        }
    }

    private static String classifyKind(Context context, Set<String> facts) {
        if (facts.contains("spatial")) return "spatial";
        if (facts.contains("terminal")) return "terminals";
        if (facts.contains("storage")) return "storage";
        if (facts.contains("crafting")) return "crafting";
        if (facts.contains("channel_network") || facts.contains("network_part")) return "channels";
        if (facts.contains("network_core")) return "network";
        if (facts.contains("tool") || isTool(context)) return "tools";
        if (isBuilding(context)) return "building";
        if (facts.contains("material") || facts.contains("processor_chain") || isMaterial(context)) return "materials";
        if (facts.contains("utility")) return "utility";
        return "";
    }

    private static boolean isTool(Context context) {
        return hasToken(context.facets, "melee_weapon")
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

    private static boolean isBuilding(Context context) {
        return hasToken(context.facets, "placeable")
                && (hasToken(context.facets, "stone_block")
                || hasToken(context.facets, "decorative_block")
                || hasToken(context.facets, "slab")
                || hasToken(context.facets, "stairs")
                || hasToken(context.facets, "wall"));
    }

    private static String storageTier(String path) {
        Matcher matcher = STORAGE_TIER.matcher(path);
        if (!matcher.find()) {
            return "";
        }
        return matcher.group(2);
    }

    private static String storageMedium(String path) {
        if (path.contains("fluid_cell")) return "fluid";
        if (path.contains("chemical_storage_cell") || path.contains("portable_chemical_cell")) return "chemical";
        if (path.contains("item_cell")) return "item";
        if (path.contains("spatial_cell")) return "spatial";
        return "";
    }

    private static boolean hasAnyPathToken(String path, String... expected) {
        for (String token : path.split("[_/]")) {
            for (String value : expected) {
                if (token.equals(value)) {
                    return true;
                }
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
        final String recipeCategories;
        final String recipeUseCategories;

        Context(ResourceLocation id, Map<String, String> meta) {
            this.path = id.getPath().toLowerCase(Locale.ROOT);
            this.itemClass = meta.getOrDefault(SearchNodeKeys.ITEM_CLASS, "");
            this.blockClass = meta.getOrDefault(SearchNodeKeys.BLOCK_CLASS, "");
            this.tags = meta.getOrDefault(SearchNodeKeys.TAGS, "").toLowerCase(Locale.ROOT);
            this.facets = meta.getOrDefault(SearchNodeKeys.FACETS, "").toLowerCase(Locale.ROOT);
            this.recipeCategories = meta.getOrDefault(SearchNodeKeys.RECIPE_CATEGORIES, "").toLowerCase(Locale.ROOT);
            this.recipeUseCategories = meta.getOrDefault(SearchNodeKeys.RECIPE_USE_CATEGORIES, "").toLowerCase(Locale.ROOT);
        }
    }
}
