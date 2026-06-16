package com.sanhiruzu.ami.index;

import com.sanhiruzu.ami.compat.ArsNouveauCompat;
import com.sanhiruzu.ami.compat.CompatFamilyDetector;
import net.minecraft.resources.Identifier;
import org.junit.jupiter.api.Test;

import java.util.EnumSet;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ArsNouveauCompatTest {
    @Test
    void arsNouveauNamespaceGetsFamilyPolicy() {
        Map<String, String> meta = meta("com.hollingsworth.arsnouveau.common.items.Glyph");

        CompatFamilyDetector.detect(new Identifier("ars_nouveau", "glyph_invisibility"), meta);

        assertEquals("ars_nouveau", meta.get(SearchNodeKeys.PRIMARY_COMPAT_FAMILY));
    }

    @Test
    void glyphClassesRouteToArsGlyphsAndCollapseTogether() {
        Map<String, String> meta = meta("com.hollingsworth.arsnouveau.common.items.Glyph");
        meta.put(SearchNodeKeys.RECIPE_CATEGORIES, "glyph");

        ArsNouveauCompat.enrichItem(new Identifier("ars_nouveau", "glyph_cut"), meta);
        CategoryAssignment assignment = resolve("ars_nouveau:glyph_cut", meta);

        assertEquals("glyphs", meta.get(SearchNodeKeys.ARS_NOUVEAU_ITEM_KIND));
        assertTrue(meta.getOrDefault(SearchNodeKeys.ARS_NOUVEAU_FACTS, "").contains("glyph"));
        assertTrue(meta.getOrDefault(SearchNodeKeys.FACETS, "").contains(ItemFacet.MAGIC_REAGENT.id()));
        assertEquals("ars_nouveau:glyphs", meta.get(SearchNodeKeys.COLLAPSE_FAMILY));
        assertEquals("Glyphs", meta.get(SearchNodeKeys.COLLAPSE_LABEL));
        assertEquals("default_collapsed", meta.get(SearchNodeKeys.VARIANT_COLLAPSE_MODE));
        assertEquals("ars_nouveau", assignment.categoryId());
        assertEquals("glyphs", assignment.subcategoryId());
    }

    @Test
    void glyphRecipeCategoryRoutesPathAliasesToArsGlyphs() {
        Map<String, String> meta = meta("net.minecraft.world.item.Item");
        meta.put(SearchNodeKeys.RECIPE_CATEGORIES, "glyph");

        ArsNouveauCompat.enrichItem(new Identifier("ars_nouveau", "reset"), meta);
        CategoryAssignment assignment = resolve("ars_nouveau:reset", meta);

        assertEquals("glyphs", meta.get(SearchNodeKeys.ARS_NOUVEAU_ITEM_KIND));
        assertEquals("ars_nouveau", assignment.categoryId());
        assertEquals("glyphs", assignment.subcategoryId());
    }

    @Test
    void ritualTabletsRouteToArsRitualsAndCollapseTogether() {
        Map<String, String> meta = meta("com.hollingsworth.arsnouveau.common.items.RitualTablet");

        ArsNouveauCompat.enrichItem(new Identifier("ars_nouveau", "ritual_burrowing"), meta);
        CategoryAssignment assignment = resolve("ars_nouveau:ritual_burrowing", meta);

        assertEquals("ritual_tablets", meta.get(SearchNodeKeys.ARS_NOUVEAU_ITEM_KIND));
        assertTrue(meta.getOrDefault(SearchNodeKeys.ARS_NOUVEAU_FACTS, "").contains("ritual_tablet"));
        assertTrue(meta.getOrDefault(SearchNodeKeys.FACETS, "").contains(ItemFacet.MAGIC_ARTIFACT.id()));
        assertEquals("ars_nouveau:ritual_tablets", meta.get(SearchNodeKeys.COLLAPSE_FAMILY));
        assertEquals("Ritual Tablets", meta.get(SearchNodeKeys.COLLAPSE_LABEL));
        assertEquals("ars_nouveau", assignment.categoryId());
        assertEquals("rituals", assignment.subcategoryId());
    }

    @Test
    void arsWorkstationsStayUnderArsHeader() {
        Map<String, String> meta = meta("com.hollingsworth.arsnouveau.common.items.ModBlockItem");

        ArsNouveauCompat.enrichItem(new Identifier("ars_nouveau", "enchanting_apparatus"), meta);
        CategoryAssignment assignment = resolve("ars_nouveau:enchanting_apparatus", meta, ItemFacet.PLACEABLE, ItemFacet.WORKSTATION);

        assertEquals("automation", meta.get(SearchNodeKeys.ARS_NOUVEAU_ITEM_KIND));
        assertEquals("ars_nouveau", assignment.categoryId());
        assertEquals("automation", assignment.subcategoryId());
    }

    @Test
    void semanticPolicyCanOptOutOfArsHeader() {
        Map<String, String> meta = meta("com.hollingsworth.arsnouveau.common.items.Glyph");
        meta.put(SearchNodeKeys.COMPAT_CATEGORY_POLICY, "semantic");

        ArsNouveauCompat.enrichItem(new Identifier("ars_nouveau", "glyph_cut"), meta);
        CategoryAssignment assignment = resolve("ars_nouveau:glyph_cut", meta);

        assertEquals("misc", assignment.categoryId());
        assertEquals("unknown", assignment.subcategoryId());
    }

    private static Map<String, String> meta(String itemClass) {
        Map<String, String> meta = new HashMap<>();
        meta.put(SearchNodeKeys.MOD_ID, "ars_nouveau");
        meta.put(SearchNodeKeys.CREATIVE_TAB_LABEL, "Ars Nouveau");
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
