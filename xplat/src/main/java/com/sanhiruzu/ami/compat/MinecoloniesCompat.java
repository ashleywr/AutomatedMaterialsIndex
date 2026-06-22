package com.sanhiruzu.ami.compat;

import com.sanhiruzu.ami.index.ItemFacet;
import com.sanhiruzu.ami.index.SearchNodeKeys;
import net.minecraft.resources.ResourceLocation;

import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

public final class MinecoloniesCompat {
    private static final String MOD_ID = "minecolonies";

    private MinecoloniesCompat() {
    }

    public static void enrichItem(ResourceLocation id, Map<String, String> meta) {
        if (id == null || meta == null || !MOD_ID.equals(id.getNamespace())) {
            return;
        }
        meta.putIfAbsent(SearchNodeKeys.PRIMARY_COMPAT_FAMILY, MOD_ID);
        meta.putIfAbsent(SearchNodeKeys.COMPAT_FAMILIES, MOD_ID);
        Context context = new Context(id, meta);
        LinkedHashSet<String> facts = new LinkedHashSet<>();
        if (CompatMetaUtil.containsAny(context.itemClass, "ItemSupplyChestDeployer", "ItemSupplyCampDeployer")) facts.add("deployer");
        if (CompatMetaUtil.containsAny(context.itemClass, "ItemScanAnalyzer", "ItemScepterPermission", "ItemScepterGuard",
                "ItemScepterLumberjack", "ItemScepterBeekeeper", "ItemAssistantHammer")) facts.add("colony_tool");
        if (CompatMetaUtil.containsAny(context.itemClass, "ItemFireArrow")) facts.add("ammo");
        if (CompatMetaUtil.containsAny(context.itemClass, "ItemMagicPotion")) facts.add("potion");
        if (CompatMetaUtil.containsAny(context.itemClass, "ItemCompost", "ItemMistletoe")) facts.add("organic_material");
        if (CompatMetaUtil.containsAny(context.itemClass, "ItemSifterMesh")) facts.add("component");
        if (CompatMetaUtil.containsAny(context.itemClass, "ItemAdventureToken")) facts.add("token");
        String kind = classifyKind(facts);
        if (!kind.isBlank()) {
            meta.put("minecoloniesItemKind", kind);
            CompatMetaUtil.addSearchToken(meta, "minecolonies_" + kind);
        }
        switch (kind) {
            case "deployer" -> {
                meta.put(SearchNodeKeys.COMPAT_ROUTE_CATEGORY, MOD_ID);
                meta.put(SearchNodeKeys.COMPAT_ROUTE_SUBCATEGORY, "settlements");
            }
            case "colony_tool" -> CompatMetaUtil.addFacet(meta, ItemFacet.UTILITY_TOOL);
            case "ammo" -> CompatMetaUtil.addFacet(meta, ItemFacet.PROJECTILE);
            case "potion" -> CompatMetaUtil.addFacet(meta, ItemFacet.POTION);
            case "organic_material" -> CompatMetaUtil.addFacet(meta, ItemFacet.INGREDIENT_ORGANIC);
            case "component" -> CompatMetaUtil.addFacet(meta, ItemFacet.TECH_COMPONENT);
            case "token" -> CompatMetaUtil.addFacet(meta, ItemFacet.UTILITY_MISC);
            default -> {
            }
        }
    }

    private static String classifyKind(Set<String> facts) {
        if (facts.contains("deployer")) return "deployer";
        if (facts.contains("colony_tool")) return "colony_tool";
        if (facts.contains("ammo")) return "ammo";
        if (facts.contains("potion")) return "potion";
        if (facts.contains("organic_material")) return "organic_material";
        if (facts.contains("component")) return "component";
        if (facts.contains("token")) return "token";
        return "";
    }

    private static final class Context {
        final String itemClass;
        final String path;

        Context(ResourceLocation id, Map<String, String> meta) {
            this.itemClass = meta.getOrDefault(SearchNodeKeys.ITEM_CLASS, "").toLowerCase(Locale.ROOT);
            this.path = id.getPath().toLowerCase(Locale.ROOT);
        }
    }
}
