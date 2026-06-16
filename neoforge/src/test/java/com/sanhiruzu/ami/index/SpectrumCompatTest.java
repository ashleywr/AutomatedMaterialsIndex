package com.sanhiruzu.ami.index;

import com.sanhiruzu.ami.compat.CompatFamilyDetector;
import com.sanhiruzu.ami.compat.SpectrumCompat;
import net.minecraft.resources.Identifier;
import org.junit.jupiter.api.Test;

import java.util.EnumSet;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SpectrumCompatTest {
    @Test
    void spectrumNamespaceGetsFamilyPolicy() {
        Map<String, String> meta = meta("de.dafuqs.spectrum.items.conditional.CloakedItem");

        CompatFamilyDetector.detect(new Identifier("spectrum", "bismuth_flake"), meta);

        assertEquals("spectrum", meta.get(SearchNodeKeys.PRIMARY_COMPAT_FAMILY));
    }

    @Test
    void structurePlacersRouteToUtilityAndCollapseTogether() {
        Map<String, String> meta = meta("de.dafuqs.spectrum.items.StructurePlacerItem");

        SpectrumCompat.enrichItem(new Identifier("spectrum", "fusion_shrine_structure_placer"), meta);
        CategoryAssignment assignment = resolve("spectrum:fusion_shrine_structure_placer", meta);

        assertEquals("structure_placers", meta.get(SearchNodeKeys.SPECTRUM_ITEM_KIND));
        assertTrue(meta.getOrDefault(SearchNodeKeys.SPECTRUM_FACTS, "").contains("structure_placer"));
        assertEquals("spectrum:structure_placers", meta.get(SearchNodeKeys.COLLAPSE_FAMILY));
        assertEquals("Structure Placers", meta.get(SearchNodeKeys.COLLAPSE_LABEL));
        assertEquals("utility", assignment.categoryId());
        assertEquals("misc", assignment.subcategoryId());
    }

    @Test
    void cloakedReagentTagsRouteToMagicReagents() {
        Map<String, String> meta = meta("de.dafuqs.spectrum.items.conditional.CloakedItemWithLoomPattern");
        meta.put(SearchNodeKeys.TAGS, "spectrum:reagent/reagents,spectrum:reagent/complex,spectrum:memory_bonding_agents");

        SpectrumCompat.enrichItem(new Identifier("spectrum", "neolith"), meta);
        CategoryAssignment assignment = resolve("spectrum:neolith", meta);

        assertEquals("reagents", meta.get(SearchNodeKeys.SPECTRUM_ITEM_KIND));
        assertTrue(meta.getOrDefault(SearchNodeKeys.FACETS, "").contains(ItemFacet.MAGIC_REAGENT.id()));
        assertEquals("magic", assignment.categoryId());
        assertEquals("reagents", assignment.subcategoryId());
    }

    @Test
    void pureResourcesRouteToIngredients() {
        Map<String, String> meta = meta("net.minecraft.world.item.Item");
        meta.put(SearchNodeKeys.TAGS, "spectrum:pure_resources,minecraft:beacon_payment_items");

        SpectrumCompat.enrichItem(new Identifier("spectrum", "pure_iron"), meta);
        CategoryAssignment assignment = resolve("spectrum:pure_iron", meta);

        assertEquals("materials", meta.get(SearchNodeKeys.SPECTRUM_ITEM_KIND));
        assertTrue(meta.getOrDefault(SearchNodeKeys.FACETS, "").contains(ItemFacet.INGREDIENT_MINERAL.id()));
        assertEquals("ingredients", assignment.categoryId());
        assertEquals("mineral", assignment.subcategoryId());
    }

    @Test
    void pastelNodeUpgradesWithoutReagentEvidenceRouteToTechUpgrades() {
        Map<String, String> meta = meta("net.minecraft.world.item.Item");
        meta.put(SearchNodeKeys.TAGS, "spectrum:pastel_node_upgrades");

        SpectrumCompat.enrichItem(new Identifier("spectrum", "pure_malachite"), meta);
        CategoryAssignment assignment = resolve("spectrum:pure_malachite", meta);

        assertEquals("upgrades", meta.get(SearchNodeKeys.SPECTRUM_ITEM_KIND));
        assertTrue(meta.getOrDefault(SearchNodeKeys.FACETS, "").contains(ItemFacet.UPGRADE.id()));
        assertEquals("tech", assignment.categoryId());
        assertEquals("upgrades", assignment.subcategoryId());
    }

    private static Map<String, String> meta(String itemClass) {
        Map<String, String> meta = new HashMap<>();
        meta.put(SearchNodeKeys.MOD_ID, "spectrum");
        meta.put(SearchNodeKeys.CREATIVE_TAB_LABEL, "Spectrum");
        meta.put(SearchNodeKeys.ITEM_CLASS, itemClass);
        return meta;
    }

    private static CategoryAssignment resolve(String id, Map<String, String> meta, ItemFacet... facets) {
        return PrimaryCategoryResolver.resolve(
                new Identifier(id),
                new FacetProfile(facets.length == 0 ? EnumSet.noneOf(ItemFacet.class) : EnumSet.of(facets[0], facets), meta)
        );
    }
}
