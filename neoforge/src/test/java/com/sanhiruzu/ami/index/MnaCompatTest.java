package com.sanhiruzu.ami.index;

import com.sanhiruzu.ami.compat.CompatFamilyDetector;
import com.sanhiruzu.ami.compat.MnaCompat;
import net.minecraft.resources.Identifier;
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

        CompatFamilyDetector.detect(new Identifier("mna", "ritual_focus_lesser"), meta);

        assertEquals("mna", meta.get(SearchNodeKeys.PRIMARY_COMPAT_FAMILY));
    }

    @Test
    void constructPartClassesRouteToTechPartsAndCollapseTogether() {
        Map<String, String> meta = meta("com.mna.items.constructs.parts.arms.ConstructPartManaCannonLeft");

        MnaCompat.enrichItem(new Identifier("mna", "construct_mana_cannon_left"), meta);
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

        MnaCompat.enrichItem(new Identifier("mna", "greater_mote_earth"), meta);
        CategoryAssignment assignment = resolve("mna:greater_mote_earth", meta);

        assertEquals("motes", meta.get(SearchNodeKeys.MNA_ITEM_KIND));
        assertTrue(meta.getOrDefault(SearchNodeKeys.FACETS, "").contains(ItemFacet.MAGIC_REAGENT.id()));
        assertEquals("magic", assignment.categoryId());
        assertEquals("reagents", assignment.subcategoryId());
    }

    @Test
    void practitionerPatchesRouteToMagicArtifactsAndCollapseTogether() {
        Map<String, String> meta = meta("com.mna.items.ritual.PractitionersPatch");

        MnaCompat.enrichItem(new Identifier("mna", "patch_collection"), meta);
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

        MnaCompat.enrichItem(new Identifier("mna", "stone_rune_fire"), meta);
        CategoryAssignment assignment = resolve("mna:stone_rune_fire", meta);

        assertEquals("runes", meta.get(SearchNodeKeys.MNA_ITEM_KIND));
        assertTrue(meta.getOrDefault(SearchNodeKeys.FACETS, "").contains(ItemFacet.MAGIC_REAGENT.id()));
        assertEquals("magic", assignment.categoryId());
        assertEquals("reagents", assignment.subcategoryId());
    }

    @Test
    void mnaSorceryReagentsRouteToMagicReagents() {
        Map<String, String> meta = meta("com.mna.items.sorcery.ItemManaGem");

        MnaCompat.enrichItem(new Identifier("mna", "minor_mana_gem"), meta);
        CategoryAssignment assignment = resolve("mna:minor_mana_gem", meta);

        assertEquals("reagents", meta.get(SearchNodeKeys.MNA_ITEM_KIND));
        assertTrue(meta.getOrDefault(SearchNodeKeys.FACETS, "").contains(ItemFacet.MAGIC_REAGENT.id()));
        assertEquals("magic", assignment.categoryId());
        assertEquals("reagents", assignment.subcategoryId());
    }

    @Test
    void factionHornsRouteToMagicArtifacts() {
        Map<String, String> meta = meta("com.mna.items.artifice.ItemFactionHorn");

        MnaCompat.enrichItem(new Identifier("mna", "faction_horn_fey"), meta);
        CategoryAssignment assignment = resolve("mna:faction_horn_fey", meta);

        assertEquals("artifacts", meta.get(SearchNodeKeys.MNA_ITEM_KIND));
        assertTrue(meta.getOrDefault(SearchNodeKeys.FACETS, "").contains(ItemFacet.MAGIC_ARTIFACT.id()));
        assertEquals("magic", assignment.categoryId());
        assertEquals("artifacts", assignment.subcategoryId());
    }

    @Test
    void tieredMnaMaterialsRouteToIngredients() {
        Map<String, String> meta = meta("com.mna.api.items.TieredItem");

        MnaCompat.enrichItem(new Identifier("mna", "superheated_vinteum_ingot"), meta);
        CategoryAssignment assignment = resolve("mna:superheated_vinteum_ingot", meta);

        assertEquals("materials", meta.get(SearchNodeKeys.MNA_ITEM_KIND));
        assertTrue(meta.getOrDefault(SearchNodeKeys.FACETS, "").contains(ItemFacet.INGREDIENT_MINERAL.id()));
        assertEquals("ingredients", assignment.categoryId());
        assertEquals("mineral", assignment.subcategoryId());
    }

    @Test
    void tieredSachetsRouteToMagicArtifacts() {
        Map<String, String> meta = meta("com.mna.api.items.TieredItem");

        MnaCompat.enrichItem(new Identifier("mna", "sachet_air"), meta);
        CategoryAssignment assignment = resolve("mna:sachet_air", meta);

        assertEquals("artifacts", meta.get(SearchNodeKeys.MNA_ITEM_KIND));
        assertEquals("magic", assignment.categoryId());
        assertEquals("artifacts", assignment.subcategoryId());
    }

    @Test
    void stavesAndWandsRouteToRangedToolsFromClassAndTags() {
        Map<String, String> meta = meta("com.mna.items.sorcery.MagicStaff");
        meta.put(SearchNodeKeys.TAGS, "mna:staves,mna:generated_spell_items");

        MnaCompat.enrichItem(new Identifier("mna", "eldrin_staff"), meta);
        CategoryAssignment assignment = resolve("mna:eldrin_staff", meta);

        assertEquals("ranged_weapons", meta.get(SearchNodeKeys.MNA_ITEM_KIND));
        assertTrue(meta.getOrDefault(SearchNodeKeys.MNA_FACTS, "").contains("ranged_weapon"));
        assertTrue(meta.getOrDefault(SearchNodeKeys.FACETS, "").contains(ItemFacet.RANGED_WEAPON.id()));
        assertEquals("tools", assignment.categoryId());
        assertEquals("ranged", assignment.subcategoryId());
    }

    @Test
    void relicMeleeWeaponsRouteToMeleeBeforeArtifactFallback() {
        Map<String, String> meta = meta("com.mna.items.relic.AstroBlade");
        meta.put(SearchNodeKeys.TAGS, "mna:relics");

        MnaCompat.enrichItem(new Identifier("mna", "astro_blade"), meta);
        CategoryAssignment assignment = resolve("mna:astro_blade", meta);

        assertEquals("weapons", meta.get(SearchNodeKeys.MNA_ITEM_KIND));
        assertTrue(meta.getOrDefault(SearchNodeKeys.MNA_FACTS, "").contains("weapon"));
        assertEquals("tools", assignment.categoryId());
        assertEquals("melee", assignment.subcategoryId());
    }

    @Test
    void ritualUtilityItemsRouteToMagicArtifacts() {
        Map<String, String> meta = meta("net.minecraft.world.item.Item");

        MnaCompat.enrichItem(new Identifier("mna", "animated_quill"), meta);
        CategoryAssignment assignment = resolve("mna:animated_quill", meta);

        assertEquals("artifacts", meta.get(SearchNodeKeys.MNA_ITEM_KIND));
        assertEquals("magic", assignment.categoryId());
        assertEquals("artifacts", assignment.subcategoryId());
    }

    @Test
    void hudBadgesRouteToUtilityMiscInsteadOfUnknown() {
        Map<String, String> meta = meta("net.minecraft.world.item.Item");

        MnaCompat.enrichItem(new Identifier("mna", "council_hud_badge_item"), meta);
        CategoryAssignment assignment = resolve("mna:council_hud_badge_item", meta);

        assertEquals("utility", meta.get(SearchNodeKeys.MNA_ITEM_KIND));
        assertTrue(meta.getOrDefault(SearchNodeKeys.FACETS, "").contains(ItemFacet.UTILITY_MISC.id()));
        assertEquals("utility", assignment.categoryId());
        assertEquals("misc", assignment.subcategoryId());
    }

    @Test
    void mnaDustTagsRouteToMagicReagents() {
        Map<String, String> meta = meta("net.minecraft.world.item.Item");
        meta.put(SearchNodeKeys.TAGS, "mna:dusts/arcane_compound");

        MnaCompat.enrichItem(new Identifier("mna", "arcane_compound"), meta);
        CategoryAssignment assignment = resolve("mna:arcane_compound", meta);

        assertEquals("reagents", meta.get(SearchNodeKeys.MNA_ITEM_KIND));
        assertEquals("magic", assignment.categoryId());
        assertEquals("reagents", assignment.subcategoryId());
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
                new Identifier(id),
                facets.length == 0 ? EnumSet.noneOf(ItemFacet.class) : EnumSet.of(facets[0], facets),
                meta
        );
    }
}
