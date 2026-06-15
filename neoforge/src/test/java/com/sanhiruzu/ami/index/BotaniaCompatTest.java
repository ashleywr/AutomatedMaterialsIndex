package com.sanhiruzu.ami.index;

import com.sanhiruzu.ami.compat.CompatFamilyDetector;
import com.sanhiruzu.ami.config.AmiConfig;
import net.minecraft.resources.ResourceLocation;
import org.junit.jupiter.api.Test;

import java.util.EnumSet;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

class BotaniaCompatTest {
    @Test
    void botaniaManaAndRunesUseFocusedBuckets() {
        Map<String, String> pool = meta("botania", "net.minecraft.world.item.BlockItem");
        Map<String, String> rune = meta("botania", "net.minecraft.world.item.Item");

        CompatFamilyDetector.detect(new ResourceLocation("botania", "mana_pool"), pool);
        CompatFamilyDetector.detect(new ResourceLocation("botania", "rune_water"), rune);

        assertEquals("botania", pool.get(SearchNodeKeys.PRIMARY_COMPAT_FAMILY));
        assertEquals("mana", resolve("botania:mana_pool", pool, ItemFacet.PLACEABLE).subcategoryId());
        assertEquals("runes", resolve("botania:rune_water", rune).subcategoryId());
    }

    @Test
    void botaniaFlowersSeparateGeneratingAndFunctional() {
        Map<String, String> endoflame = meta("botania", "net.minecraft.world.item.BlockItem");
        Map<String, String> hopperhock = meta("botania", "net.minecraft.world.item.BlockItem");

        CompatFamilyDetector.detect(new ResourceLocation("botania", "endoflame"), endoflame);
        CompatFamilyDetector.detect(new ResourceLocation("botania", "hopperhock"), hopperhock);

        assertEquals("generating_flowers", resolve("botania:endoflame", endoflame, ItemFacet.PLACEABLE).subcategoryId());
        assertEquals("functional_flowers", resolve("botania:hopperhock", hopperhock, ItemFacet.PLACEABLE).subcategoryId());
    }

    @Test
    void botaniaBaublesToolsAndMaterialsUseFocusedBuckets() {
        Map<String, String> ring = meta("botania", "vazkii.botania.common.item.equipment.bauble.BaubleItem");
        Map<String, String> wand = meta("botania", "vazkii.botania.common.item.WandOfTheForestItem");
        Map<String, String> ingot = meta("botania", "net.minecraft.world.item.Item");

        CompatFamilyDetector.detect(new ResourceLocation("botania", "aura_ring"), ring);
        CompatFamilyDetector.detect(new ResourceLocation("botania", "twig_wand"), wand);
        CompatFamilyDetector.detect(new ResourceLocation("botania", "manasteel_ingot"), ingot);

        assertEquals("baubles", resolve("botania:aura_ring", ring, ItemFacet.CURIO).subcategoryId());
        assertEquals("tools", resolve("botania:twig_wand", wand, ItemFacet.UTILITY_TOOL).subcategoryId());
        assertEquals("materials", resolve("botania:manasteel_ingot", ingot, ItemFacet.INGOT).subcategoryId());
    }

    @Test
    void knownBotaniaAddonsJoinBotaniaFamily() {
        Map<String, String> alfsteel = meta("mythicbotany", "net.minecraft.world.item.Item");

        CompatFamilyDetector.detect(new ResourceLocation("mythicbotany", "alfsteel_ingot"), alfsteel);
        CategoryAssignment assignment = resolve("mythicbotany:alfsteel_ingot", alfsteel, ItemFacet.INGOT);

        assertEquals("botania", alfsteel.get(SearchNodeKeys.PRIMARY_COMPAT_FAMILY));
        assertEquals("botania", assignment.categoryId());
        assertEquals("materials", assignment.subcategoryId());
    }

    @Test
    void semanticBotaniaPolicyCanStillOptOut() {
        AmiConfig.CompatCategoryPolicy oldPolicy = AmiConfig.botaniaCategoryPolicy;
        try {
            AmiConfig.botaniaCategoryPolicy = AmiConfig.CompatCategoryPolicy.SEMANTIC;
            Map<String, String> meta = meta("botania", "net.minecraft.world.item.Item");
            CompatFamilyDetector.detect(new ResourceLocation("botania", "manasteel_ingot"), meta);

            CategoryAssignment assignment = resolve("botania:manasteel_ingot", meta, ItemFacet.INGOT);

            assertNotEquals("botania", assignment.categoryId());
            assertEquals("semantic", assignment.attributes().get(SearchNodeKeys.COMPAT_CATEGORY_POLICY));
        } finally {
            AmiConfig.botaniaCategoryPolicy = oldPolicy;
        }
    }

    @Test
    void botaniaBrewsAndIncenseUseBrewsSubcategory() {
        Map<String, String> brew = meta("botania", "vazkii.botania.common.item.brew.BaseBrewItem");
        Map<String, String> vial = meta("botania", "vazkii.botania.common.item.brew.VialItem");
        Map<String, String> incense = meta("botania", "vazkii.botania.common.item.brew.IncenseStickItem");
        Map<String, String> flask = meta("botania", "net.minecraft.world.item.Item");

        CompatFamilyDetector.detect(new ResourceLocation("botania", "brew_vial"), brew);
        CompatFamilyDetector.detect(new ResourceLocation("botania", "vial"), vial);
        CompatFamilyDetector.detect(new ResourceLocation("botania", "incense_stick"), incense);
        CompatFamilyDetector.detect(new ResourceLocation("botania", "flask"), flask);

        assertEquals("botania", brew.get(SearchNodeKeys.PRIMARY_COMPAT_FAMILY));
        assertEquals("brews", resolve("botania:brew_vial", brew, ItemFacet.FOOD_DRINK).subcategoryId());
        assertEquals("brews", resolve("botania:vial", vial).subcategoryId());
        assertEquals("brews", resolve("botania:incense_stick", incense).subcategoryId());
        assertEquals("brews", resolve("botania:flask", flask).subcategoryId());
    }

    @Test
    void botaniaDecorativeBlocksUseDecorationSubcategory() {
        Map<String, String> shimmering = meta("botania", "net.minecraft.world.item.BlockItem");
        Map<String, String> brick = meta("botania", "net.minecraft.world.item.BlockItem");
        Map<String, String> glimmering = meta("botania", "net.minecraft.world.item.BlockItem");

        CompatFamilyDetector.detect(new ResourceLocation("botania", "shimmerrock"), shimmering);
        CompatFamilyDetector.detect(new ResourceLocation("botania", "livingrock_brick"), brick);
        CompatFamilyDetector.detect(new ResourceLocation("botania", "glimmering_white_flower"), glimmering);

        assertEquals("decoration", resolve("botania:shimmerrock", shimmering, ItemFacet.PLACEABLE, ItemFacet.STONE_BLOCK).subcategoryId());
        assertEquals("decoration", resolve("botania:livingrock_brick", brick, ItemFacet.PLACEABLE, ItemFacet.DECORATIVE_BLOCK).subcategoryId());
        assertEquals("decoration", resolve("botania:glimmering_white_flower", glimmering, ItemFacet.PLACEABLE, ItemFacet.FLOWER, ItemFacet.LIGHT_SOURCE).subcategoryId());
    }

    @Test
    void botaniaSeedsShardsAndPatternsUseMaterials() {
        Map<String, String> seeds = meta("botania", "vazkii.botania.common.item.GrassSeedsItem");
        Map<String, String> shard = meta("botania", "vazkii.botania.common.item.LaputaShardItem");
        Map<String, String> pattern = meta("botania", "vazkii.botania.common.item.CraftingPatternItem");

        CompatFamilyDetector.detect(new ResourceLocation("botania", "grass_seeds"), seeds);
        CompatFamilyDetector.detect(new ResourceLocation("botania", "laputa_shard"), shard);
        CompatFamilyDetector.detect(new ResourceLocation("botania", "pattern_1_1"), pattern);

        assertEquals("materials", resolve("botania:grass_seeds", seeds).subcategoryId());
        assertEquals("materials", resolve("botania:laputa_shard", shard).subcategoryId());
        assertEquals("materials", resolve("botania:pattern_1_1", pattern, ItemFacet.TEMPLATE).subcategoryId());
    }

    private static Map<String, String> meta(String modId, String itemClass) {
        Map<String, String> meta = new HashMap<>();
        meta.put(SearchNodeKeys.MOD_ID, modId);
        meta.put(SearchNodeKeys.CREATIVE_TAB_LABEL, modId);
        meta.put(SearchNodeKeys.ITEM_CLASS, itemClass);
        return meta;
    }

    private static CategoryAssignment resolve(String id, Map<String, String> meta, ItemFacet... facets) {
        return PrimaryCategoryResolver.resolve(
                new ResourceLocation(id),
                new FacetProfile(facets.length == 0 ? EnumSet.noneOf(ItemFacet.class) : EnumSet.of(facets[0], facets), meta)
        );
    }
}
