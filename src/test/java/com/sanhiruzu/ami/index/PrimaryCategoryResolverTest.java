package com.sanhiruzu.ami.index;

import net.minecraft.resources.ResourceLocation;
import org.junit.jupiter.api.Test;

import java.util.EnumSet;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

class PrimaryCategoryResolverTest {

    @Test
    void multiFacetItemResolvesToOnePrimaryCategoryByPriority() {
        CategoryAssignment assignment = PrimaryCategoryResolver.resolve(
                ResourceLocation.parse("minecraft:cake"),
                new FacetProfile(EnumSet.of(ItemFacet.EDIBLE, ItemFacet.PLACEABLE, ItemFacet.COMPOSTABLE), Map.of())
        );

        assertEquals("nature", assignment.categoryId());
        assertEquals("snacks", assignment.subcategoryId());
    }

    @Test
    void navigationItemsResolveToUtilityBeforeOtherBuckets() {
        CategoryAssignment assignment = PrimaryCategoryResolver.resolve(
                ResourceLocation.parse("minecraft:compass"),
                new FacetProfile(EnumSet.of(ItemFacet.UTILITY_NAVIGATION), Map.of())
        );

        assertEquals("utility", assignment.categoryId());
        assertEquals("navigation", assignment.subcategoryId());
    }

    @Test
    void flowerResolvesToNatureBeforeDecorationOrMasonry() {
        CategoryAssignment assignment = PrimaryCategoryResolver.resolve(
                ResourceLocation.parse("minecraft:dandelion"),
                new FacetProfile(EnumSet.of(ItemFacet.PLACEABLE, ItemFacet.FLOWER, ItemFacet.DECORATIVE_BLOCK), Map.of())
        );

        assertEquals("nature", assignment.categoryId());
        assertEquals("flora", assignment.subcategoryId());
    }

    @Test
    void drinksAndMealsPreserveNatureSubcategories() {
        CategoryAssignment drinkAssignment = PrimaryCategoryResolver.resolve(
                ResourceLocation.parse("minecraft:honey_bottle"),
                new FacetProfile(EnumSet.of(ItemFacet.EDIBLE, ItemFacet.FOOD_DRINK), Map.of())
        );
        CategoryAssignment mealAssignment = PrimaryCategoryResolver.resolve(
                ResourceLocation.parse("minecraft:mushroom_stew"),
                new FacetProfile(EnumSet.of(ItemFacet.EDIBLE, ItemFacet.FOOD_MEAL, ItemFacet.FOOD_DRINK, ItemFacet.FUNGI), Map.of())
        );

        assertEquals("nature", drinkAssignment.categoryId());
        assertEquals("drinks", drinkAssignment.subcategoryId());
        assertEquals("nature", mealAssignment.categoryId());
        assertEquals("drinks", mealAssignment.subcategoryId());
    }

    @Test
    void armorResolvesToArmor() {
        CategoryAssignment assignment = PrimaryCategoryResolver.resolve(
                ResourceLocation.parse("minecraft:iron_helmet"),
                new FacetProfile(EnumSet.of(ItemFacet.ARMOR_HEAD), Map.of())
        );

        assertEquals("armor", assignment.categoryId());
        assertEquals("head", assignment.subcategoryId());
    }

    @Test
    void chestArmorAndMaceResolveWithoutLegacyClassifier() {
        CategoryAssignment armorAssignment = PrimaryCategoryResolver.resolve(
                ResourceLocation.parse("minecraft:elytra"),
                new FacetProfile(EnumSet.of(ItemFacet.ARMOR_CHEST), Map.of())
        );
        CategoryAssignment weaponAssignment = PrimaryCategoryResolver.resolve(
                ResourceLocation.parse("minecraft:mace"),
                new FacetProfile(EnumSet.of(ItemFacet.MELEE_WEAPON), Map.of())
        );

        assertEquals("armor", armorAssignment.categoryId());
        assertEquals("chest", armorAssignment.subcategoryId());
        assertEquals("tools", weaponAssignment.categoryId());
        assertEquals("melee", weaponAssignment.subcategoryId());
    }

    @Test
    void utilityToolsResolveToToolsUtility() {
        CategoryAssignment assignment = PrimaryCategoryResolver.resolve(
                ResourceLocation.parse("minecraft:fishing_rod"),
                new FacetProfile(EnumSet.of(ItemFacet.UTILITY_TOOL), Map.of())
        );

        assertEquals("tools", assignment.categoryId());
        assertEquals("utility", assignment.subcategoryId());
    }

    @Test
    void throwableIngredientsResolveToIngredientsBeforeTools() {
        CategoryAssignment assignment = PrimaryCategoryResolver.resolve(
                ResourceLocation.parse("minecraft:egg"),
                new FacetProfile(EnumSet.of(ItemFacet.PROJECTILE, ItemFacet.INGREDIENT_ORGANIC), Map.of())
        );

        assertEquals("ingredients", assignment.categoryId());
        assertEquals("organic", assignment.subcategoryId());
    }

    @Test
    void redstoneDoorResolvesAccordingToPriorityRules() {
        CategoryAssignment assignment = PrimaryCategoryResolver.resolve(
                ResourceLocation.parse("minecraft:iron_door"),
                new FacetProfile(
                        EnumSet.of(ItemFacet.PLACEABLE, ItemFacet.DOOR, ItemFacet.REDSTONE_SIGNAL, ItemFacet.DECORATIVE_BLOCK),
                        Map.of("blockShape", "door")
                )
        );

        assertEquals("tech", assignment.categoryId());
        assertEquals("redstone", assignment.subcategoryId());
    }

    @Test
    void naturalStoneFallsBackToGeologyBeforeMasonry() {
        CategoryAssignment assignment = PrimaryCategoryResolver.resolve(
                ResourceLocation.parse("minecraft:reinforced_deepslate"),
                new FacetProfile(
                        EnumSet.of(ItemFacet.PLACEABLE, ItemFacet.STONE_BLOCK),
                        Map.of(SearchNodeKeys.BLOCKS_MATERIAL, "stone")
                )
        );

        assertEquals("geology", assignment.categoryId());
        assertEquals("stone", assignment.subcategoryId());
    }

    @Test
    void ingredientAndSocialSlicesResolveWithoutLegacyClassifier() {
        CategoryAssignment ingredientAssignment = PrimaryCategoryResolver.resolve(
                ResourceLocation.parse("minecraft:ink_sac"),
                new FacetProfile(EnumSet.of(ItemFacet.INGREDIENT_DYE), Map.of())
        );
        CategoryAssignment socialAssignment = PrimaryCategoryResolver.resolve(
                ResourceLocation.parse("minecraft:player_head"),
                new FacetProfile(EnumSet.of(ItemFacet.SOCIAL_PLAYERS), Map.of())
        );

        assertEquals("ingredients", ingredientAssignment.categoryId());
        assertEquals("dyes", ingredientAssignment.subcategoryId());
        assertEquals("social", socialAssignment.categoryId());
        assertEquals("players", socialAssignment.subcategoryId());
    }

    @Test
    void bedsAndWorkstationsResolveOutOfMasonry() {
        CategoryAssignment bedAssignment = PrimaryCategoryResolver.resolve(
                ResourceLocation.parse("minecraft:light_gray_bed"),
                new FacetProfile(EnumSet.of(ItemFacet.PLACEABLE, ItemFacet.HAS_BLOCK_ENTITY, ItemFacet.DECORATIVE_BLOCK), Map.of())
        );
        CategoryAssignment workstationAssignment = PrimaryCategoryResolver.resolve(
                ResourceLocation.parse("minecraft:composter"),
                new FacetProfile(EnumSet.of(ItemFacet.PLACEABLE, ItemFacet.MACHINE), Map.of())
        );

        assertEquals("decoration", bedAssignment.categoryId());
        assertEquals("furniture", bedAssignment.subcategoryId());
        assertEquals("tech", workstationAssignment.categoryId());
        assertEquals("machines", workstationAssignment.subcategoryId());
    }

    @Test
    void functionalDecorativeAndLivingBlocksResolveByNewPriority() {
        CategoryAssignment targetAssignment = PrimaryCategoryResolver.resolve(
                ResourceLocation.parse("minecraft:target"),
                new FacetProfile(EnumSet.of(ItemFacet.PLACEABLE, ItemFacet.REDSTONE_LOGIC, ItemFacet.REDSTONE_SIGNAL), Map.of())
        );
        CategoryAssignment lecternAssignment = PrimaryCategoryResolver.resolve(
                ResourceLocation.parse("minecraft:lectern"),
                new FacetProfile(EnumSet.of(ItemFacet.PLACEABLE, ItemFacet.MACHINE), Map.of())
        );
        CategoryAssignment carpetAssignment = PrimaryCategoryResolver.resolve(
                ResourceLocation.parse("minecraft:red_carpet"),
                new FacetProfile(EnumSet.of(ItemFacet.PLACEABLE, ItemFacet.DECORATIVE_BLOCK), Map.of())
        );
        CategoryAssignment frogspawnAssignment = PrimaryCategoryResolver.resolve(
                ResourceLocation.parse("minecraft:frogspawn"),
                new FacetProfile(EnumSet.of(ItemFacet.PLACEABLE, ItemFacet.NATURE_MISC), Map.of())
        );

        assertEquals("tech", targetAssignment.categoryId());
        assertEquals("redstone", targetAssignment.subcategoryId());
        assertEquals("tech", lecternAssignment.categoryId());
        assertEquals("machines", lecternAssignment.subcategoryId());
        assertEquals("decoration", carpetAssignment.categoryId());
        assertEquals("furniture", carpetAssignment.subcategoryId());
        assertEquals("nature", frogspawnAssignment.categoryId());
        assertEquals("flora", frogspawnAssignment.subcategoryId());
    }

    @Test
    void candlesHeadsCoralsSignsCobwebAndDripstoneResolveIntoExpectedBuckets() {
        CategoryAssignment candleAssignment = PrimaryCategoryResolver.resolve(
                ResourceLocation.parse("minecraft:white_candle"),
                new FacetProfile(EnumSet.of(ItemFacet.PLACEABLE, ItemFacet.DECORATIVE_BLOCK, ItemFacet.LIGHT_SOURCE), Map.of())
        );
        CategoryAssignment headAssignment = PrimaryCategoryResolver.resolve(
                ResourceLocation.parse("minecraft:zombie_head"),
                new FacetProfile(EnumSet.of(ItemFacet.PLACEABLE, ItemFacet.DECORATIVE_BLOCK), Map.of())
        );
        CategoryAssignment dripstoneAssignment = PrimaryCategoryResolver.resolve(
                ResourceLocation.parse("minecraft:pointed_dripstone"),
                new FacetProfile(EnumSet.of(ItemFacet.PLACEABLE, ItemFacet.STONE_BLOCK), Map.of(SearchNodeKeys.BLOCKS_MATERIAL, "stone"))
        );
        CategoryAssignment coralAssignment = PrimaryCategoryResolver.resolve(
                ResourceLocation.parse("minecraft:tube_coral"),
                new FacetProfile(EnumSet.of(ItemFacet.PLACEABLE, ItemFacet.NATURE_MISC), Map.of())
        );
        CategoryAssignment signAssignment = PrimaryCategoryResolver.resolve(
                ResourceLocation.parse("minecraft:oak_sign"),
                new FacetProfile(EnumSet.of(ItemFacet.PLACEABLE, ItemFacet.DECORATIVE_BLOCK), Map.of())
        );
        CategoryAssignment cobwebAssignment = PrimaryCategoryResolver.resolve(
                ResourceLocation.parse("minecraft:cobweb"),
                new FacetProfile(EnumSet.of(ItemFacet.PLACEABLE, ItemFacet.NATURE_MISC), Map.of())
        );
        CategoryAssignment deadBushAssignment = PrimaryCategoryResolver.resolve(
                ResourceLocation.parse("minecraft:dead_bush"),
                new FacetProfile(EnumSet.of(ItemFacet.PLACEABLE, ItemFacet.NATURE_MISC), Map.of())
        );
        CategoryAssignment sculkAssignment = PrimaryCategoryResolver.resolve(
                ResourceLocation.parse("minecraft:sculk"),
                new FacetProfile(EnumSet.of(ItemFacet.PLACEABLE, ItemFacet.NATURE_MISC), Map.of())
        );

        assertEquals("decoration", candleAssignment.categoryId());
        assertEquals("lighting", candleAssignment.subcategoryId());
        assertEquals("decoration", headAssignment.categoryId());
        assertEquals("furniture", headAssignment.subcategoryId());
        assertEquals("geology", dripstoneAssignment.categoryId());
        assertEquals("stone", dripstoneAssignment.subcategoryId());
        assertEquals("nature", coralAssignment.categoryId());
        assertEquals("flora", coralAssignment.subcategoryId());
        assertEquals("decoration", signAssignment.categoryId());
        assertEquals("furniture", signAssignment.subcategoryId());
        assertEquals("nature", cobwebAssignment.categoryId());
        assertEquals("flora", cobwebAssignment.subcategoryId());
        assertEquals("nature", deadBushAssignment.categoryId());
        assertEquals("flora", deadBushAssignment.subcategoryId());
        assertEquals("nature", sculkAssignment.categoryId());
        assertEquals("flora", sculkAssignment.subcategoryId());
    }

    @Test
    void legacyMagicBooksFamiliesResolveIntoConcreteBuckets() {
        CategoryAssignment utilityAssignment = PrimaryCategoryResolver.resolve(
                ResourceLocation.parse("minecraft:written_book"),
                new FacetProfile(EnumSet.of(ItemFacet.UTILITY_MISC), Map.of())
        );
        CategoryAssignment transportAssignment = PrimaryCategoryResolver.resolve(
                ResourceLocation.parse("minecraft:bamboo_raft"),
                new FacetProfile(EnumSet.of(ItemFacet.TRANSPORT), Map.of())
        );
        CategoryAssignment ingredientAssignment = PrimaryCategoryResolver.resolve(
                ResourceLocation.parse("minecraft:turtle_scute"),
                new FacetProfile(EnumSet.of(ItemFacet.INGREDIENT_ORGANIC), Map.of())
        );
        CategoryAssignment decorationAssignment = PrimaryCategoryResolver.resolve(
                ResourceLocation.parse("minecraft:painting"),
                new FacetProfile(EnumSet.of(ItemFacet.DECORATIVE_BLOCK), Map.of())
        );
        CategoryAssignment techAssignment = PrimaryCategoryResolver.resolve(
                ResourceLocation.parse("minecraft:coal"),
                new FacetProfile(EnumSet.of(ItemFacet.DUST), Map.of())
        );

        assertEquals("utility", utilityAssignment.categoryId());
        assertEquals("misc", utilityAssignment.subcategoryId());
        assertEquals("tech", transportAssignment.categoryId());
        assertEquals("transport", transportAssignment.subcategoryId());
        assertEquals("ingredients", ingredientAssignment.categoryId());
        assertEquals("organic", ingredientAssignment.subcategoryId());
        assertEquals("decoration", decorationAssignment.categoryId());
        assertEquals("furniture", decorationAssignment.subcategoryId());
        assertEquals("tech", techAssignment.categoryId());
        assertEquals("dusts", techAssignment.subcategoryId());
    }

    @Test
    void unknownItemsFallBackToMisc() {
        CategoryAssignment assignment = PrimaryCategoryResolver.resolve(
                ResourceLocation.parse("minecraft:paper"),
                new FacetProfile(EnumSet.noneOf(ItemFacet.class), Map.of())
        );

        assertEquals("misc", assignment.categoryId());
        assertEquals("unknown", assignment.subcategoryId());
    }

    @Test
    void uncraftableLowSignalFullBlocksBiasToTerrainInsteadOfBuilding() {
        CategoryAssignment terrainAssignment = PrimaryCategoryResolver.resolve(
                ResourceLocation.parse("minecraft:sculk"),
                new FacetProfile(
                        EnumSet.of(ItemFacet.PLACEABLE),
                        Map.of(
                                SearchNodeKeys.OBTAINABILITY, "no_recipe",
                                SearchNodeKeys.BLOCKS_MATERIAL, "other_building"
                        )
                )
        );
        CategoryAssignment stoneAssignment = PrimaryCategoryResolver.resolve(
                ResourceLocation.parse("minecraft:pointed_dripstone_block"),
                new FacetProfile(
                        EnumSet.of(ItemFacet.PLACEABLE),
                        Map.of(
                                SearchNodeKeys.OBTAINABILITY, "no_recipe",
                                SearchNodeKeys.BLOCKS_MATERIAL, "stone"
                        )
                )
        );

        assertEquals("geology", terrainAssignment.categoryId());
        assertEquals("terrain", terrainAssignment.subcategoryId());
        assertEquals("geology", stoneAssignment.categoryId());
        assertEquals("stone", stoneAssignment.subcategoryId());
    }
}
