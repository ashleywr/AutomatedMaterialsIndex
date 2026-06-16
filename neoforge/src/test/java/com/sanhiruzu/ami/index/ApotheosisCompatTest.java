package com.sanhiruzu.ami.index;

import com.sanhiruzu.ami.compat.CompatFamilyDetector;
import com.sanhiruzu.ami.config.AmiConfig;
import net.minecraft.resources.Identifier;
import org.junit.jupiter.api.Test;

import java.util.EnumSet;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

class ApotheosisCompatTest {
    @Test
    void apothicEnchantingJoinsApotheosisFamily() {
        Map<String, String> meta = meta("apothic_enchanting",
                "dev.shadowsoffire.apothic_enchanting.objects.TomeItem",
                "");

        CompatFamilyDetector.detect(new Identifier("apothic_enchanting", "weapon_tome"), meta);
        CategoryAssignment assignment = resolve("apothic_enchanting:weapon_tome", meta);

        assertEquals("apotheosis", meta.get(SearchNodeKeys.PRIMARY_COMPAT_FAMILY));
        assertEquals("apotheosis", assignment.categoryId());
        assertEquals("enchanting", assignment.subcategoryId());
    }

    @Test
    void apotheosisGemAndSocketItemsUseFocusedBuckets() {
        Map<String, String> gem = meta("apotheosis",
                "dev.shadowsoffire.apotheosis.socket.gem.GemItem",
                "");
        Map<String, String> charm = meta("apotheosis",
                "dev.shadowsoffire.apotheosis.item.PotionCharmItem",
                "");
        charm.put(SearchNodeKeys.TAGS, "curios:charm");

        CompatFamilyDetector.detect(new Identifier("apotheosis", "gem"), gem);
        CompatFamilyDetector.detect(new Identifier("apotheosis", "potion_charm"), charm);

        assertEquals("gems", resolve("apotheosis:gem", gem).subcategoryId());
        assertEquals("sockets", resolve("apotheosis:potion_charm", charm, ItemFacet.CURIO).subcategoryId());
    }

    @Test
    void apotheosisAffixTablesAndBossItemsUseFocusedBuckets() {
        Map<String, String> table = meta("apotheosis",
                "net.minecraft.world.item.BlockItem",
                "dev.shadowsoffire.apotheosis.affix.reforging.ReforgingTableBlock");
        Map<String, String> boss = meta("apotheosis",
                "dev.shadowsoffire.apotheosis.item.BossSummonerItem",
                "");

        CompatFamilyDetector.detect(new Identifier("apotheosis", "reforging_table"), table);
        CompatFamilyDetector.detect(new Identifier("apotheosis", "boss_summoner"), boss);

        assertEquals("affixes", resolve("apotheosis:reforging_table", table,
                ItemFacet.PLACEABLE, ItemFacet.HAS_BLOCK_ENTITY).subcategoryId());
        assertEquals("bosses", resolve("apotheosis:boss_summoner", boss).subcategoryId());
    }

    @Test
    void semanticApotheosisPolicyCanStillOptOut() {
        AmiConfig.CompatCategoryPolicy oldPolicy = AmiConfig.apotheosisCategoryPolicy;
        try {
            AmiConfig.apotheosisCategoryPolicy = AmiConfig.CompatCategoryPolicy.SEMANTIC;
            Map<String, String> meta = meta("apothic_enchanting",
                    "dev.shadowsoffire.apothic_enchanting.objects.TomeItem",
                    "");
            CompatFamilyDetector.detect(new Identifier("apothic_enchanting", "weapon_tome"), meta);

            CategoryAssignment assignment = resolve("apothic_enchanting:weapon_tome", meta, ItemFacet.RANGED_WEAPON);

            assertNotEquals("apotheosis", assignment.categoryId());
            assertEquals("semantic", assignment.attributes().get(SearchNodeKeys.COMPAT_CATEGORY_POLICY));
        } finally {
            AmiConfig.apotheosisCategoryPolicy = oldPolicy;
        }
    }

    private static Map<String, String> meta(String modId, String itemClass, String blockClass) {
        Map<String, String> meta = new HashMap<>();
        meta.put(SearchNodeKeys.MOD_ID, modId);
        meta.put(SearchNodeKeys.CREATIVE_TAB_LABEL, modId);
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
                new Identifier(id),
                new FacetProfile(facets.length == 0 ? EnumSet.noneOf(ItemFacet.class) : EnumSet.of(facets[0], facets), meta)
        );
    }
}
