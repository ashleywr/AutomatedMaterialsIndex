package com.sanhiruzu.ami.index;

import net.minecraft.resources.ResourceLocation;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.EnumSet;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Proves bundled minecolonies override data reproduces MinecoloniesCompat's class-based
 * facet tagging WITHOUT referencing MinecoloniesCompat -- survives the plugin's deletion.
 */
class MinecoloniesOverrideMigrationTest {

    @BeforeEach
    void installBundled() {
        ClassificationOverrides.loadBundledDefaults();
    }

    @AfterEach
    void reset() {
        ClassificationOverrides.clear();
    }

    private static Map<String, String> meta(String modId, String itemClass) {
        Map<String, String> m = new HashMap<>();
        m.put(SearchNodeKeys.MOD_ID, modId);
        m.put(SearchNodeKeys.ITEM_CLASS, itemClass);
        return m;
    }

    private static CategoryAssignment resolveBare(String id, Map<String, String> meta) {
        return PrimaryCategoryResolver.resolve(
                new ResourceLocation(id), EnumSet.noneOf(ItemFacet.class), meta);
    }

    private static boolean hasFacet(CategoryAssignment a, ItemFacet facet) {
        return a.attributes().getOrDefault(SearchNodeKeys.FACETS, "").contains(facet.id());
    }

    @Test
    void supplyDeployerRoutesToSettlementsCategory() {
        CategoryAssignment a = resolveBare("minecolonies:supplychestdeployer",
                meta("minecolonies", "com.minecolonies.core.items.ItemSupplyChestDeployer"));
        assertEquals("minecolonies", a.categoryId());
        assertEquals("settlements", a.subcategoryId());
    }

    @Test
    void colonyToolsGainUtilityToolFacet() {
        CategoryAssignment a = resolveBare("minecolonies:scepterpermission",
                meta("minecolonies", "com.minecolonies.core.items.ItemScepterGuard"));
        assertTrue(hasFacet(a, ItemFacet.UTILITY_TOOL));
    }

    @Test
    void fireArrowGainsProjectileFacet() {
        CategoryAssignment a = resolveBare("minecolonies:firearrow",
                meta("minecolonies", "com.minecolonies.core.items.ItemFireArrow"));
        assertTrue(hasFacet(a, ItemFacet.PROJECTILE));
    }

    @Test
    void compostGainsOrganicFacet() {
        CategoryAssignment a = resolveBare("minecolonies:compost",
                meta("minecolonies", "com.minecolonies.core.items.ItemCompost"));
        assertTrue(hasFacet(a, ItemFacet.INGREDIENT_ORGANIC));
    }

    @Test
    void hutBlocksExposeSettlementWorksiteVerb() {
        Map<String, String> meta = meta("minecolonies", "com.minecolonies.core.items.ItemBlockHut");

        CategoryAssignment assignment = PrimaryCategoryResolver.resolve(
                new ResourceLocation("minecolonies:blockhutbuilder"),
                EnumSet.of(ItemFacet.PLACEABLE, ItemFacet.HAS_BLOCK_ENTITY),
                meta);

        assertTrue(SemanticVerbCodec.has(assignment.attributes(), SemanticVerb.SETTLEMENT_WORKSITE));
        assertEquals("utility", assignment.categoryId());
        assertEquals("workstations", assignment.subcategoryId());
    }

    @Test
    void hutWorksiteVerbRequiresBlockhutPathAndHutClass() {
        CategoryAssignment pathOnly = PrimaryCategoryResolver.resolve(
                new ResourceLocation("minecolonies:blockhutbuilder"),
                EnumSet.of(ItemFacet.PLACEABLE, ItemFacet.HAS_BLOCK_ENTITY),
                meta("minecolonies", "net.minecraft.world.item.BlockItem"));
        assertFalse(SemanticVerbCodec.has(pathOnly.attributes(), SemanticVerb.SETTLEMENT_WORKSITE));

        CategoryAssignment classOnly = PrimaryCategoryResolver.resolve(
                new ResourceLocation("minecolonies:builder_hut"),
                EnumSet.of(ItemFacet.PLACEABLE, ItemFacet.HAS_BLOCK_ENTITY),
                meta("minecolonies", "com.minecolonies.core.items.ItemBlockHut"));
        assertFalse(SemanticVerbCodec.has(classOnly.attributes(), SemanticVerb.SETTLEMENT_WORKSITE));
    }

    @Test
    void unmatchedMinecoloniesItemsGainNoOverrideFacets() {
        CategoryAssignment a = resolveBare("minecolonies:stone_sword",
                meta("minecolonies", "net.minecraft.world.item.Item"));
        assertFalse(hasFacet(a, ItemFacet.UTILITY_TOOL));
        assertFalse(hasFacet(a, ItemFacet.PROJECTILE));
        assertFalse(hasFacet(a, ItemFacet.INGREDIENT_ORGANIC));
    }
}
