package com.sanhiruzu.ami.index;

import com.sanhiruzu.ami.compat.AE2Compat;
import com.sanhiruzu.ami.compat.CompatFamilyDetector;
import com.sanhiruzu.ami.config.AmiConfig;
import net.minecraft.resources.ResourceLocation;
import org.junit.jupiter.api.Test;

import java.util.EnumSet;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AE2CompatTest {
    @Test
    void ae2NamespaceGetsFamilyPolicy() {
        Map<String, String> meta = meta("ae2", "Applied Energistics 2", "appeng.items.misc.MaterialItem", "");

        CompatFamilyDetector.detect(new ResourceLocation("ae2", "logic_processor"), meta);

        assertEquals("ae2", meta.get(SearchNodeKeys.PRIMARY_COMPAT_FAMILY));
    }

    @Test
    void wirelessTerminalRoutesToAe2Terminals() {
        Map<String, String> meta = meta("ae2", "Applied Energistics 2",
                "appeng.items.tools.powered.WirelessTerminalItem", "");

        AE2Compat.enrichItem(new ResourceLocation("ae2", "wireless_terminal"), meta);
        CategoryAssignment assignment = resolve("ae2:wireless_terminal", meta, ItemFacet.HAS_ENERGY, ItemFacet.CURIO);

        assertEquals("terminals", meta.get(SearchNodeKeys.AE2_ITEM_KIND));
        assertTrue(meta.getOrDefault(SearchNodeKeys.AE2_FACTS, "").contains("terminal"));
        assertEquals("ae2", assignment.categoryId());
        assertEquals("terminals", assignment.subcategoryId());
    }

    @Test
    void portableCellsExposeStorageTierAndRouteToAe2Storage() {
        Map<String, String> meta = meta("ae2", "Applied Energistics 2",
                "appeng.items.tools.powered.PortableCellItem", "");

        AE2Compat.enrichItem(new ResourceLocation("ae2", "portable_item_cell_64k"), meta);
        CategoryAssignment assignment = resolve("ae2:portable_item_cell_64k", meta, ItemFacet.HAS_ENERGY, ItemFacet.CURIO);

        assertEquals("storage", meta.get(SearchNodeKeys.AE2_ITEM_KIND));
        assertEquals("64k", meta.get(SearchNodeKeys.AE2_STORAGE_TIER));
        assertEquals("item", meta.get(SearchNodeKeys.AE2_STORAGE_MEDIUM));
        assertEquals("ae2", assignment.categoryId());
        assertEquals("storage", assignment.subcategoryId());
    }

    @Test
    void cableRoutesToAe2Channels() {
        Map<String, String> meta = meta("ae2", "Applied Energistics 2",
                "appeng.items.parts.PartItem", "");
        meta.put(SearchNodeKeys.TAGS, "ae2:smart_cable,ae2:p2p_attunements/me_p2p_tunnel");

        AE2Compat.enrichItem(new ResourceLocation("ae2", "white_smart_cable"), meta);
        CategoryAssignment assignment = resolve("ae2:white_smart_cable", meta, ItemFacet.CABLE, ItemFacet.TECH_COMPONENT);

        assertEquals("channels", meta.get(SearchNodeKeys.AE2_ITEM_KIND));
        assertEquals("ae2", assignment.categoryId());
        assertEquals("channels", assignment.subcategoryId());
    }

    @Test
    void ae2MaterialsStaySemanticUnderHybridPolicy() {
        Map<String, String> meta = meta("ae2", "Applied Energistics 2",
                "appeng.items.materials.MaterialItem", "");
        meta.put(SearchNodeKeys.TAGS, "ae2:all_certus_quartz,c:gems/certus_quartz,c:gems");
        meta.put(SearchNodeKeys.FACETS, FacetCodec.encode(EnumSet.of(ItemFacet.GEM)));

        AE2Compat.enrichItem(new ResourceLocation("ae2", "certus_quartz_crystal"), meta);
        CategoryAssignment assignment = resolve("ae2:certus_quartz_crystal", meta, ItemFacet.GEM);

        assertEquals("materials", meta.get(SearchNodeKeys.AE2_ITEM_KIND));
        assertTrue(meta.getOrDefault(SearchNodeKeys.AE2_FACTS, "").contains("material"));
        assertNotEquals("ae2", assignment.categoryId());
    }

    @Test
    void ae2ToolsStayToolsUnderHybridPolicy() {
        Map<String, String> meta = meta("ae2", "Applied Energistics 2",
                "appeng.items.tools.quartz.QuartzSwordItem", "");

        AE2Compat.enrichItem(new ResourceLocation("ae2", "certus_quartz_sword"), meta);
        CategoryAssignment assignment = resolve("ae2:certus_quartz_sword", meta,
                ItemFacet.MELEE_WEAPON, ItemFacet.UTILITY_TOOL);

        assertEquals("tools", meta.get(SearchNodeKeys.AE2_ITEM_KIND));
        assertEquals("tools", assignment.categoryId());
        assertEquals("melee", assignment.subcategoryId());
    }

    @Test
    void ae2BuildingBlocksStayMasonryUnderHybridPolicy() {
        Map<String, String> meta = meta("ae2", "Applied Energistics 2",
                "net.minecraft.world.item.BlockItem", "net.minecraft.world.level.block.SlabBlock");
        meta.put(SearchNodeKeys.FACETS, FacetCodec.encode(EnumSet.of(ItemFacet.PLACEABLE, ItemFacet.SLAB)));

        AE2Compat.enrichItem(new ResourceLocation("ae2", "sky_stone_slab"), meta);
        CategoryAssignment assignment = resolve("ae2:sky_stone_slab", meta, ItemFacet.PLACEABLE, ItemFacet.SLAB);

        assertEquals("building", meta.get(SearchNodeKeys.AE2_ITEM_KIND));
        assertEquals("masonry", assignment.categoryId());
        assertEquals("slab", assignment.subcategoryId());
    }

    @Test
    void facetlessAe2MaterialsFallBackToAe2Materials() {
        Map<String, String> meta = meta("ae2", "Applied Energistics 2",
                "appeng.items.materials.MaterialItem", "");

        AE2Compat.enrichItem(new ResourceLocation("ae2", "matter_ball"), meta);
        CategoryAssignment assignment = resolve("ae2:matter_ball", meta);

        assertEquals("materials", meta.get(SearchNodeKeys.AE2_ITEM_KIND));
        assertEquals("ae2", assignment.categoryId());
        assertEquals("materials", assignment.subcategoryId());
        assertEquals("compat_fallback", assignment.attributes().get(SearchNodeKeys.CLASSIFICATION_ROUTE_PHASE));
    }

    @Test
    void ae2PaintBallsFallBackToAe2MiscInsteadOfUnknownMisc() {
        Map<String, String> meta = meta("ae2", "Applied Energistics 2",
                "net.minecraft.world.item.Item", "");

        AE2Compat.enrichItem(new ResourceLocation("ae2", "white_lumen_paint_ball"), meta);
        CategoryAssignment assignment = resolve("ae2:white_lumen_paint_ball", meta);

        assertEquals("utility", meta.get(SearchNodeKeys.AE2_ITEM_KIND));
        assertEquals("ae2", assignment.categoryId());
        assertEquals("misc", assignment.subcategoryId());
    }

    @Test
    void semanticAe2PolicyKeepsTerminalInNormalOntology() {
        AmiConfig.CompatCategoryPolicy oldPolicy = AmiConfig.ae2CategoryPolicy;
        try {
            AmiConfig.ae2CategoryPolicy = AmiConfig.CompatCategoryPolicy.SEMANTIC;
            Map<String, String> meta = meta("ae2", "Applied Energistics 2",
                    "appeng.items.tools.powered.WirelessTerminalItem", "");

            AE2Compat.enrichItem(new ResourceLocation("ae2", "wireless_terminal"), meta);
            CategoryAssignment assignment = resolve("ae2:wireless_terminal", meta, ItemFacet.HAS_ENERGY, ItemFacet.CURIO);

            assertEquals("terminals", meta.get(SearchNodeKeys.AE2_ITEM_KIND));
            assertNotEquals("ae2", assignment.categoryId());
            assertEquals("semantic", assignment.attributes().get(SearchNodeKeys.COMPAT_CATEGORY_POLICY));
        } finally {
            AmiConfig.ae2CategoryPolicy = oldPolicy;
        }
    }

    private static Map<String, String> meta(String modId, String creativeTab, String itemClass, String blockClass) {
        Map<String, String> meta = new HashMap<>();
        meta.put(SearchNodeKeys.MOD_ID, modId);
        meta.put(SearchNodeKeys.CREATIVE_TAB_LABEL, creativeTab);
        if (!itemClass.isBlank()) {
            meta.put(SearchNodeKeys.ITEM_CLASS, itemClass);
        }
        if (!blockClass.isBlank()) {
            meta.put(SearchNodeKeys.BLOCK_CLASS, blockClass);
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
