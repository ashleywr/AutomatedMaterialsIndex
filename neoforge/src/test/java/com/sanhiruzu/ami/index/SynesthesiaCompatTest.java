package com.sanhiruzu.ami.index;

import com.sanhiruzu.ami.compat.BornInChaosCompat;
import com.sanhiruzu.ami.compat.CataclysmCompat;
import com.sanhiruzu.ami.compat.GeneratedVariantCollapseCompat;
import com.sanhiruzu.ami.compat.MalumCompat;
import com.sanhiruzu.ami.compat.PastelCompat;
import com.sanhiruzu.ami.compat.SwemCompat;
import net.minecraft.resources.ResourceLocation;
import org.junit.jupiter.api.Test;

import java.util.EnumSet;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SynesthesiaCompatTest {
    @Test
    void swemMetalComponentsNoLongerFallBackUnknown() {
        Map<String, String> meta = meta("swem", "net.minecraft.world.item.Item");

        SwemCompat.enrichItem(new ResourceLocation("swem", "rivet_copper"), meta);
        CategoryAssignment assignment = resolve("swem:rivet_copper", meta);

        assertEquals("components", meta.get("swemItemKind"));
        assertTrue(meta.getOrDefault(SearchNodeKeys.FACETS, "").contains(ItemFacet.TECH_COMPONENT.id()));
        assertEquals("ingredients", assignment.categoryId());
        assertEquals("mineral", assignment.subcategoryId());
    }

    @Test
    void malumGeasRoutesToMagicAndCollapses() {
        Map<String, String> meta = meta("malum", "com.sammy.malum.common.item.GeasItem");
        meta.put(SearchNodeKeys.SUBTYPE_OF, "malum:geas");

        MalumCompat.enrichItem(new ResourceLocation("malum", "geas/variant/geas_b28b0afe76f1"), meta);
        CategoryAssignment assignment = resolve("malum:geas/variant/geas_b28b0afe76f1", meta);

        assertEquals("geasa", meta.get("malumItemKind"));
        assertEquals("malum:geas", meta.get(SearchNodeKeys.COLLAPSE_FAMILY));
        assertEquals("malum", assignment.categoryId());
        assertEquals("geasa", assignment.subcategoryId());
    }

    @Test
    void pastelStructurePlacersRouteToUtility() {
        Map<String, String> meta = meta("pastel", "earth.terrarium.pastel.items.StructurePlacerItem");

        PastelCompat.enrichItem(new ResourceLocation("pastel", "fusion_shrine_structure_placer"), meta);
        CategoryAssignment assignment = resolve("pastel:fusion_shrine_structure_placer", meta);

        assertEquals("structure_placers", meta.get("pastelItemKind"));
        assertEquals("pastel", assignment.categoryId());
        assertEquals("structures", assignment.subcategoryId());
    }

    @Test
    void bornInChaosMaterialsAndCharmsRouteSemantically() {
        Map<String, String> material = meta("born_in_chaos_v1", "net.mcreator.borninchaosv.item.DarkMetalIngotItem");
        BornInChaosCompat.enrichItem(new ResourceLocation("born_in_chaos_v1", "dark_metal_ingot"), material);
        CategoryAssignment materialAssignment = resolve("born_in_chaos_v1:dark_metal_ingot", material);

        Map<String, String> charm = meta("born_in_chaos_v1", "net.mcreator.borninchaosv.item.CharmofPowerItem");
        BornInChaosCompat.enrichItem(new ResourceLocation("born_in_chaos_v1", "charmof_power"), charm);
        CategoryAssignment charmAssignment = resolve("born_in_chaos_v1:charmof_power", charm);

        assertEquals("materials", material.get("bornInChaosItemKind"));
        assertEquals("ingredients", materialAssignment.categoryId());
        assertEquals("mineral", materialAssignment.subcategoryId());
        assertEquals("artifacts", charm.get("bornInChaosItemKind"));
        assertEquals("magic", charmAssignment.categoryId());
        assertEquals("artifacts", charmAssignment.subcategoryId());
    }

    @Test
    void cataclysmDungeonEyesAndIngotsRouteSemantically() {
        Map<String, String> eye = meta("cataclysm", "com.github.L_Ender.cataclysm.items.DungeonEyeItem");
        CataclysmCompat.enrichItem(new ResourceLocation("cataclysm", "mech_eye"), eye);
        CategoryAssignment eyeAssignment = resolve("cataclysm:mech_eye", eye);

        Map<String, String> ingot = meta("cataclysm", "net.minecraft.world.item.Item");
        CataclysmCompat.enrichItem(new ResourceLocation("cataclysm", "witherite_ingot"), ingot);
        CategoryAssignment ingotAssignment = resolve("cataclysm:witherite_ingot", ingot);

        assertEquals("dungeon_eyes", eye.get("cataclysmItemKind"));
        assertEquals("cataclysm", eyeAssignment.categoryId());
        assertEquals("dungeon_eyes", eyeAssignment.subcategoryId());
        assertEquals("materials", ingot.get("cataclysmItemKind"));
        assertEquals("ingredients", ingotAssignment.categoryId());
        assertEquals("mineral", ingotAssignment.subcategoryId());
    }

    @Test
    void genericGeneratedVariantsCollapseBySubtype() {
        Map<String, String> meta = meta("domum_ornamentum", "com.ldtteam.domumornamentum.item.decoration.PanelBlockItem");
        meta.put(SearchNodeKeys.SUBTYPE_OF, "domum_ornamentum:panel");

        GeneratedVariantCollapseCompat.enrichItem(
                new ResourceLocation("domum_ornamentum", "panel/variant/stripped_oak_wood_panel_11720a6e1c90"),
                meta);

        assertEquals("domum_ornamentum:panel", meta.get(SearchNodeKeys.COLLAPSE_FAMILY));
        assertEquals("Panel", meta.get(SearchNodeKeys.COLLAPSE_LABEL));
        assertEquals("default_collapsed", meta.get(SearchNodeKeys.VARIANT_COLLAPSE_MODE));
    }

    private static Map<String, String> meta(String modId, String itemClass) {
        Map<String, String> meta = new HashMap<>();
        meta.put(SearchNodeKeys.MOD_ID, modId);
        meta.put(SearchNodeKeys.ITEM_CLASS, itemClass);
        return meta;
    }

    private static CategoryAssignment resolve(String id, Map<String, String> meta, ItemFacet... facets) {
        return PrimaryCategoryResolver.resolve(
                new ResourceLocation(id),
                facetsFrom(meta, facets),
                meta
        );
    }

    private static EnumSet<ItemFacet> facetsFrom(Map<String, String> meta, ItemFacet... explicitFacets) {
        EnumSet<ItemFacet> resolved = EnumSet.noneOf(ItemFacet.class);
        String encoded = meta.getOrDefault(SearchNodeKeys.FACETS, "");
        if (!encoded.isBlank()) {
            for (String token : encoded.split(",")) {
                String normalized = token.trim();
                if (normalized.isBlank()) {
                    continue;
                }
                for (ItemFacet facet : ItemFacet.values()) {
                    if (facet.id().equals(normalized)) {
                        resolved.add(facet);
                        break;
                    }
                }
            }
        }
        for (ItemFacet facet : explicitFacets) {
            resolved.add(facet);
        }
        return resolved;
    }
}
