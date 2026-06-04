package com.sanhiruzu.ami.index;

import com.sanhiruzu.ami.compat.CompatFamilyDetector;
import com.sanhiruzu.ami.compat.ModularGolemsCompat;
import net.minecraft.resources.ResourceLocation;
import org.junit.jupiter.api.Test;

import java.util.EnumSet;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

class ModularGolemsCompatTest {
    @Test
    void namespaceGetsModularGolemsCompatFamily() {
        Map<String, String> meta = meta("", "");

        CompatFamilyDetector.detect(new ResourceLocation("modulargolems", "golem_workbench"), meta);

        assertEquals("modular_golems", meta.get(SearchNodeKeys.PRIMARY_COMPAT_FAMILY));
    }

    @Test
    void golemArmorDoesNotRouteToPlayerArmorSlots() {
        Map<String, String> meta = meta("dev.xkmc.modulargolems.content.item.equipments.MetalGolemArmorItem", "");
        meta.put(SearchNodeKeys.FACETS, FacetCodec.encode(EnumSet.of(ItemFacet.EQUIPPABLE, ItemFacet.ARMOR_CHEST)));
        meta.put(SearchNodeKeys.EQUIPMENT_SLOT, "chest");

        ModularGolemsCompat.enrichItem(new ResourceLocation("modulargolems", "roman_guard_chestplate"), meta);
        CategoryAssignment assignment = resolve("modulargolems:roman_guard_chestplate", meta,
                ItemFacet.EQUIPPABLE, ItemFacet.ARMOR_CHEST);

        assertEquals("golem_armor", meta.get(SearchNodeKeys.MODULAR_GOLEMS_ITEM_KIND));
        assertEquals("armor", assignment.categoryId());
        assertEquals("animal", assignment.subcategoryId());
        assertNotEquals("chest", assignment.subcategoryId());
    }

    @Test
    void specialGolemBootsDoNotRouteToPlayerFeetArmor() {
        Map<String, String> meta = meta("dev.xkmc.modulargolems.content.item.equipments.MetalGolemBeaconItem",
                "modulargolems:tough_item");
        meta.put(SearchNodeKeys.FACETS, FacetCodec.encode(EnumSet.of(ItemFacet.EQUIPPABLE, ItemFacet.ARMOR_FEET)));
        meta.put(SearchNodeKeys.EQUIPMENT_SLOT, "feet");

        ModularGolemsCompat.enrichItem(new ResourceLocation("modulargolems", "beacon_boots"), meta);
        CategoryAssignment assignment = resolve("modulargolems:beacon_boots", meta,
                ItemFacet.EQUIPPABLE, ItemFacet.ARMOR_FEET);

        assertEquals("golem_armor", meta.get(SearchNodeKeys.MODULAR_GOLEMS_ITEM_KIND));
        assertEquals("armor", assignment.categoryId());
        assertEquals("animal", assignment.subcategoryId());
    }

    @Test
    void golemFacadeCurioTagDoesNotRouteToPlayerCurios() {
        Map<String, String> meta = meta("dev.xkmc.modulargolems.content.item.golem.GolemFacade", "curios:golem_skin");
        meta.put(SearchNodeKeys.FACETS, FacetCodec.encode(EnumSet.of(ItemFacet.CURIO)));
        meta.put(SearchNodeKeys.SUBTYPE_OF, "modulargolems:golem_facade");

        ModularGolemsCompat.enrichItem(
                new ResourceLocation("modulargolems", "golem_facade/variant/golem_facade_0"),
                meta);
        CategoryAssignment assignment = resolve("modulargolems:golem_facade/variant/golem_facade_0", meta, ItemFacet.CURIO);

        assertEquals("facades", meta.get(SearchNodeKeys.MODULAR_GOLEMS_ITEM_KIND));
        assertEquals("modulargolems:golem_facade", meta.get(SearchNodeKeys.COLLAPSE_FAMILY));
        assertEquals("default_collapsed", meta.get(SearchNodeKeys.VARIANT_COLLAPSE_MODE));
        assertEquals("tech", assignment.categoryId());
        assertEquals("parts", assignment.subcategoryId());
    }

    @Test
    void routeCardCurioTagDoesNotRouteToPlayerCurios() {
        Map<String, String> meta = meta("dev.xkmc.modulargolems.content.item.card.PathRecordCard",
                "curios:golem_route,modulargolems:golem_interact");
        meta.put(SearchNodeKeys.FACETS, FacetCodec.encode(EnumSet.of(ItemFacet.CURIO)));

        ModularGolemsCompat.enrichItem(new ResourceLocation("modulargolems", "patrol_path_recorder"), meta);
        CategoryAssignment assignment = resolve("modulargolems:patrol_path_recorder", meta, ItemFacet.CURIO);

        assertEquals("route_cards", meta.get(SearchNodeKeys.MODULAR_GOLEMS_ITEM_KIND));
        assertEquals("tech", assignment.categoryId());
        assertEquals("redstone", assignment.subcategoryId());
    }

    @Test
    void generatedGolemPartsGetFactsAndCollapseMetadata() {
        Map<String, String> meta = meta("dev.xkmc.modulargolems.content.item.golem.GolemPart", "modulargolems:parts");
        meta.put(SearchNodeKeys.SUBTYPE_OF, "modulargolems:metal_golem_body");

        ModularGolemsCompat.enrichItem(
                new ResourceLocation("modulargolems", "metal_golem_body/variant/metal_golem_body_0"),
                meta);
        CategoryAssignment assignment = resolve("modulargolems:metal_golem_body/variant/metal_golem_body_0", meta);

        assertEquals("parts", meta.get(SearchNodeKeys.MODULAR_GOLEMS_ITEM_KIND));
        assertEquals("metal_golem", meta.get(SearchNodeKeys.MODULAR_GOLEMS_GOLEM_TYPE));
        assertEquals("body", meta.get(SearchNodeKeys.MODULAR_GOLEMS_PART));
        assertEquals("modulargolems:metal_golem_body", meta.get(SearchNodeKeys.COLLAPSE_FAMILY));
        assertEquals("default_collapsed", meta.get(SearchNodeKeys.VARIANT_COLLAPSE_MODE));
        assertEquals("tech", assignment.categoryId());
        assertEquals("parts", assignment.subcategoryId());
    }

    @Test
    void configCardsRouteToControlCards() {
        Map<String, String> meta = meta("dev.xkmc.modulargolems.content.item.card.ConfigCard",
                "modulargolems:config_card,modulargolems:golem_interact");

        ModularGolemsCompat.enrichItem(new ResourceLocation("modulargolems", "white_config_card"), meta);
        CategoryAssignment assignment = resolve("modulargolems:white_config_card", meta);

        assertEquals("cards", meta.get(SearchNodeKeys.MODULAR_GOLEMS_ITEM_KIND));
        assertEquals("tech", assignment.categoryId());
        assertEquals("redstone", assignment.subcategoryId());
    }

    @Test
    void rangedGolemClassesRouteToRangedTools() {
        Map<String, String> meta = meta("dev.xkmc.modulargolems.content.item.ranged.FlameThrowerItem",
                "modulargolems:tough_item");

        ModularGolemsCompat.enrichItem(new ResourceLocation("modulargolems", "flame_thrower"), meta);
        CategoryAssignment assignment = resolve("modulargolems:flame_thrower", meta);

        assertEquals("ranged_weapons", meta.get(SearchNodeKeys.MODULAR_GOLEMS_ITEM_KIND));
        assertEquals("tools", assignment.categoryId());
        assertEquals("ranged", assignment.subcategoryId());
    }

    @Test
    void compatMaterialArmorClassesResolveAsGolemArmor() {
        Map<String, String> meta = meta(
                "dev.xkmc.modulargolems.compat.materials.twilightforest.equipments.FieryArmorItem",
                "");
        meta.put(SearchNodeKeys.FACETS, FacetCodec.encode(EnumSet.of(ItemFacet.EQUIPPABLE, ItemFacet.ARMOR_CHEST)));

        ModularGolemsCompat.enrichItem(new ResourceLocation("modulargolems", "fiery_chestplate"), meta);
        CategoryAssignment assignment = resolve("modulargolems:fiery_chestplate", meta,
                ItemFacet.EQUIPPABLE, ItemFacet.ARMOR_CHEST);

        assertEquals("golem_armor", meta.get(SearchNodeKeys.MODULAR_GOLEMS_ITEM_KIND));
        assertEquals("armor", assignment.categoryId());
        assertEquals("animal", assignment.subcategoryId());
    }

    private static Map<String, String> meta(String itemClass, String tags) {
        Map<String, String> meta = new HashMap<>();
        meta.put(SearchNodeKeys.MOD_ID, "modulargolems");
        if (!itemClass.isBlank()) {
            meta.put(SearchNodeKeys.ITEM_CLASS, itemClass);
        }
        if (!tags.isBlank()) {
            meta.put(SearchNodeKeys.TAGS, tags);
        }
        return meta;
    }

    private static CategoryAssignment resolve(String id, Map<String, String> meta, ItemFacet... facets) {
        return PrimaryCategoryResolver.resolve(
                new ResourceLocation(id),
                new FacetProfile(facets.length == 0 ? EnumSet.noneOf(ItemFacet.class) : EnumSet.of(facets[0], facets), meta)
        );
    }
}
