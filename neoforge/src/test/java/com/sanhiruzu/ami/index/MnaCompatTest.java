package com.sanhiruzu.ami.index;

import com.sanhiruzu.ami.compat.CompatFamilyDetector;
import com.sanhiruzu.ami.compat.MnaCompat;
import net.minecraft.resources.ResourceLocation;
import org.junit.jupiter.api.Test;

import java.util.EnumSet;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MnaCompatTest {
    @Test
    void mnaNamespaceGetsFamilyPolicy() {
        Map<String, String> meta = meta("com.mna.api.items.TieredItem");

        CompatFamilyDetector.detect(new ResourceLocation("mna", "ritual_focus_lesser"), meta);

        assertEquals("mna", meta.get(SearchNodeKeys.PRIMARY_COMPAT_FAMILY));
    }

    @Test
    void constructPartClassesRouteToTechPartsAndCollapseTogether() {
        Map<String, String> meta = meta("com.mna.items.constructs.parts.arms.ConstructPartManaCannonLeft");

        MnaCompat.enrichItem(new ResourceLocation("mna", "construct_mana_cannon_left"), meta);
        CategoryAssignment assignment = resolve("mna:construct_mana_cannon_left", meta);

        assertEquals("construct_parts", meta.get(SearchNodeKeys.MNA_ITEM_KIND));
        assertTrue(meta.getOrDefault(SearchNodeKeys.MNA_FACTS, "").contains("construct_part"));
        assertTrue(meta.getOrDefault(SearchNodeKeys.FACETS, "").contains(ItemFacet.TECH_COMPONENT.id()));
        assertEquals("mna:construct_parts", meta.get(SearchNodeKeys.COLLAPSE_FAMILY));
        assertEquals("Construct Parts", meta.get(SearchNodeKeys.COLLAPSE_LABEL));
        assertEquals("default_collapsed", meta.get(SearchNodeKeys.VARIANT_COLLAPSE_MODE));
        assertEquals("tech", assignment.categoryId());
        assertEquals("parts", assignment.subcategoryId());
    }

    @Test
    void motesRouteToMagicReagents() {
        Map<String, String> meta = meta("com.mna.items.ritual.Mote");
        meta.put(SearchNodeKeys.TAGS, "mna:greater_motes");

        MnaCompat.enrichItem(new ResourceLocation("mna", "greater_mote_earth"), meta);
        CategoryAssignment assignment = resolve("mna:greater_mote_earth", meta);

        assertEquals("motes", meta.get(SearchNodeKeys.MNA_ITEM_KIND));
        assertTrue(meta.getOrDefault(SearchNodeKeys.FACETS, "").contains(ItemFacet.MAGIC_REAGENT.id()));
        assertEquals("magic", assignment.categoryId());
        assertEquals("reagents", assignment.subcategoryId());
    }

    @Test
    void practitionerPatchesRouteToMagicArtifactsAndCollapseTogether() {
        Map<String, String> meta = meta("com.mna.items.ritual.PractitionersPatch");

        MnaCompat.enrichItem(new ResourceLocation("mna", "patch_collection"), meta);
        CategoryAssignment assignment = resolve("mna:patch_collection", meta);

        assertEquals("ritual_patches", meta.get(SearchNodeKeys.MNA_ITEM_KIND));
        assertTrue(meta.getOrDefault(SearchNodeKeys.FACETS, "").contains(ItemFacet.MAGIC_ARTIFACT.id()));
        assertTrue(meta.getOrDefault(SearchNodeKeys.FACETS, "").contains(ItemFacet.UPGRADE.id()));
        assertEquals("mna:ritual_patches", meta.get(SearchNodeKeys.COLLAPSE_FAMILY));
        assertEquals("Practitioner Patches", meta.get(SearchNodeKeys.COLLAPSE_LABEL));
        assertEquals("magic", assignment.categoryId());
        assertEquals("artifacts", assignment.subcategoryId());
    }

    @Test
    void runesRouteToMagicReagentsFromTags() {
        Map<String, String> meta = meta("com.mna.items.runes.StoneRune");
        meta.put(SearchNodeKeys.TAGS, "mna:stone_runes,mna:runes");

        MnaCompat.enrichItem(new ResourceLocation("mna", "stone_rune_fire"), meta);
        CategoryAssignment assignment = resolve("mna:stone_rune_fire", meta);

        assertEquals("runes", meta.get(SearchNodeKeys.MNA_ITEM_KIND));
        assertTrue(meta.getOrDefault(SearchNodeKeys.FACETS, "").contains(ItemFacet.MAGIC_REAGENT.id()));
        assertEquals("magic", assignment.categoryId());
        assertEquals("reagents", assignment.subcategoryId());
    }

    @Test
    void mnaSorceryReagentsRouteToMagicReagents() {
        Map<String, String> meta = meta("com.mna.items.sorcery.ItemManaGem");

        MnaCompat.enrichItem(new ResourceLocation("mna", "minor_mana_gem"), meta);
        CategoryAssignment assignment = resolve("mna:minor_mana_gem", meta);

        assertEquals("reagents", meta.get(SearchNodeKeys.MNA_ITEM_KIND));
        assertTrue(meta.getOrDefault(SearchNodeKeys.FACETS, "").contains(ItemFacet.MAGIC_REAGENT.id()));
        assertEquals("magic", assignment.categoryId());
        assertEquals("reagents", assignment.subcategoryId());
    }

    @Test
    void factionHornsRouteToMagicArtifacts() {
        Map<String, String> meta = meta("com.mna.items.artifice.ItemFactionHorn");

        MnaCompat.enrichItem(new ResourceLocation("mna", "faction_horn_fey"), meta);
        CategoryAssignment assignment = resolve("mna:faction_horn_fey", meta);

        assertEquals("artifacts", meta.get(SearchNodeKeys.MNA_ITEM_KIND));
        assertTrue(meta.getOrDefault(SearchNodeKeys.FACETS, "").contains(ItemFacet.MAGIC_ARTIFACT.id()));
        assertEquals("magic", assignment.categoryId());
        assertEquals("artifacts", assignment.subcategoryId());
    }

    @Test
    void tieredMnaMaterialsRouteToIngredients() {
        Map<String, String> meta = meta("com.mna.api.items.TieredItem");

        MnaCompat.enrichItem(new ResourceLocation("mna", "superheated_vinteum_ingot"), meta);
        CategoryAssignment assignment = resolve("mna:superheated_vinteum_ingot", meta);

        assertEquals("materials", meta.get(SearchNodeKeys.MNA_ITEM_KIND));
        assertTrue(meta.getOrDefault(SearchNodeKeys.FACETS, "").contains(ItemFacet.INGREDIENT_MINERAL.id()));
        assertEquals("ingredients", assignment.categoryId());
        assertEquals("mineral", assignment.subcategoryId());
    }

    @Test
    void tieredSachetsRouteToMagicArtifacts() {
        Map<String, String> meta = meta("com.mna.api.items.TieredItem");

        MnaCompat.enrichItem(new ResourceLocation("mna", "sachet_air"), meta);
        CategoryAssignment assignment = resolve("mna:sachet_air", meta);

        assertEquals("artifacts", meta.get(SearchNodeKeys.MNA_ITEM_KIND));
        assertEquals("magic", assignment.categoryId());
        assertEquals("artifacts", assignment.subcategoryId());
    }

    private static Map<String, String> meta(String itemClass) {
        Map<String, String> meta = new HashMap<>();
        meta.put(SearchNodeKeys.MOD_ID, "mna");
        meta.put(SearchNodeKeys.CREATIVE_TAB_LABEL, "Mana and Artifice");
        meta.put(SearchNodeKeys.ITEM_CLASS, itemClass);
        return meta;
    }

    private static CategoryAssignment resolve(String id, Map<String, String> meta, ItemFacet... facets) {
        return PrimaryCategoryResolver.resolve(
                new ResourceLocation(id),
                facets.length == 0 ? EnumSet.noneOf(ItemFacet.class) : EnumSet.of(facets[0], facets),
                meta
        );
    }
}
