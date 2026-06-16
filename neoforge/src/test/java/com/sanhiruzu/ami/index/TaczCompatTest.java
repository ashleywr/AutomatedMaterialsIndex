package com.sanhiruzu.ami.index;

import com.sanhiruzu.ami.compat.CompatFamilyDetector;
import com.sanhiruzu.ami.compat.TaczCompat;
import net.minecraft.resources.Identifier;
import org.junit.jupiter.api.Test;

import java.util.EnumSet;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TaczCompatTest {
    @Test
    void taczNamespaceGetsFamilyPolicy() {
        Map<String, String> meta = meta("com.tacz.guns.item.ModernKineticGunItem");

        CompatFamilyDetector.detect(new Identifier("tacz", "modern_kinetic_gun"), meta);

        assertEquals("tacz", meta.get(SearchNodeKeys.PRIMARY_COMPAT_FAMILY));
    }

    @Test
    void generatedAttachmentsRouteToTaczAttachmentsAndCollapseTogether() {
        Map<String, String> meta = meta("com.tacz.guns.item.AttachmentItem");
        meta.put(SearchNodeKeys.SUBTYPE_OF, "tacz:attachment");
        meta.put(SearchNodeKeys.MATERIAL_GROUP, "tacz:attachment");

        TaczCompat.enrichItem(
                new Identifier("tacz", "attachment/variant/sro_mini_red_dot_8d3fad27f0ad"),
                meta);
        CategoryAssignment assignment = resolve(
                "tacz:attachment/variant/sro_mini_red_dot_8d3fad27f0ad",
                meta);

        assertEquals("attachments", meta.get(SearchNodeKeys.TACZ_ITEM_KIND));
        assertTrue(meta.getOrDefault(SearchNodeKeys.TACZ_FACTS, "").contains("attachment"));
        assertTrue(meta.getOrDefault(SearchNodeKeys.FACETS, "").contains(ItemFacet.UPGRADE.id()));
        assertEquals("tacz:attachment", meta.get(SearchNodeKeys.COLLAPSE_FAMILY));
        assertEquals("Attachments", meta.get(SearchNodeKeys.COLLAPSE_LABEL));
        assertEquals("default_collapsed", meta.get(SearchNodeKeys.VARIANT_COLLAPSE_MODE));
        assertEquals("tacz", assignment.categoryId());
        assertEquals("attachments", assignment.subcategoryId());
        assertEquals("hard_identity", assignment.attributes().get(SearchNodeKeys.CLASSIFICATION_ROUTE_PHASE));
        assertEquals("identity", assignment.attributes().get(SearchNodeKeys.CLASSIFICATION_ROUTE_RULE));
    }

    @Test
    void taczApiClassesMarkAddonNamespacesAsTaczFamily() {
        Map<String, String> meta = meta("com.tacz.guns.item.AttachmentItem");
        meta.put(SearchNodeKeys.MOD_ID, "example_tacz_addon");

        CompatFamilyDetector.detect(new Identifier("example_tacz_addon", "custom_scope"), meta);
        TaczCompat.enrichItem(new Identifier("example_tacz_addon", "custom_scope"), meta);
        CategoryAssignment assignment = resolve("example_tacz_addon:custom_scope", meta);

        assertEquals("tacz", meta.get(SearchNodeKeys.PRIMARY_COMPAT_FAMILY));
        assertEquals("attachments", meta.get(SearchNodeKeys.TACZ_ITEM_KIND));
        assertEquals("tacz", assignment.categoryId());
        assertEquals("attachments", assignment.subcategoryId());
    }

    @Test
    void gunsAndAmmoRouteToTaczTopLevelGroup() {
        Map<String, String> gun = meta("com.tacz.guns.item.ModernKineticGunItem");
        gun.put(SearchNodeKeys.FACETS, FacetCodec.encode(EnumSet.of(ItemFacet.RANGED_WEAPON)));
        TaczCompat.enrichItem(new Identifier("tacz", "modern_kinetic_gun/variant/glock_17"), gun);
        CategoryAssignment gunAssignment = resolve("tacz:modern_kinetic_gun/variant/glock_17", gun,
                ItemFacet.RANGED_WEAPON);

        Map<String, String> ammo = meta("com.tacz.guns.item.AmmoItem");
        ammo.put(SearchNodeKeys.FACETS, FacetCodec.encode(EnumSet.of(ItemFacet.PROJECTILE)));
        TaczCompat.enrichItem(new Identifier("tacz", "ammo/variant/9mm"), ammo);
        CategoryAssignment ammoAssignment = resolve("tacz:ammo/variant/9mm", ammo, ItemFacet.PROJECTILE);

        assertEquals("guns", gun.get(SearchNodeKeys.TACZ_ITEM_KIND));
        assertEquals("tacz", gunAssignment.categoryId());
        assertEquals("guns", gunAssignment.subcategoryId());
        assertFalse(gun.containsKey(SearchNodeKeys.COLLAPSE_FAMILY));
        assertEquals("ammo", ammo.get(SearchNodeKeys.TACZ_ITEM_KIND));
        assertEquals("tacz", ammoAssignment.categoryId());
        assertEquals("ammo", ammoAssignment.subcategoryId());
    }

    @Test
    void workstationsRouteToTaczWorkstations() {
        Map<String, String> meta = meta("com.tacz.guns.item.GunSmithTableItem");

        TaczCompat.enrichItem(new Identifier("tacz", "gun_smith_table"), meta);
        CategoryAssignment assignment = resolve("tacz:gun_smith_table", meta);

        assertEquals("workstations", meta.get(SearchNodeKeys.TACZ_ITEM_KIND));
        assertEquals("tacz", assignment.categoryId());
        assertEquals("workstations", assignment.subcategoryId());
    }

    private static Map<String, String> meta(String itemClass) {
        Map<String, String> meta = new HashMap<>();
        meta.put(SearchNodeKeys.MOD_ID, "tacz");
        meta.put(SearchNodeKeys.CREATIVE_TAB_LABEL, "Timeless and Classics");
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
