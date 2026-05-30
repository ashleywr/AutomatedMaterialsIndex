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
                new ResourceLocation("minecraft:cake"),
                new FacetProfile(EnumSet.of(ItemFacet.EDIBLE, ItemFacet.PLACEABLE, ItemFacet.PLACEABLE_FOOD, ItemFacet.COMPOSTABLE), Map.of())
        );

        assertEquals("nature", assignment.categoryId());
        assertEquals("meals", assignment.subcategoryId());
    }

    @Test
    void navigationItemsResolveToUtilityBeforeOtherBuckets() {
        CategoryAssignment assignment = PrimaryCategoryResolver.resolve(
                new ResourceLocation("minecraft:compass"),
                new FacetProfile(EnumSet.of(ItemFacet.UTILITY_NAVIGATION), Map.of())
        );

        assertEquals("utility", assignment.categoryId());
        assertEquals("navigation", assignment.subcategoryId());
    }

    @Test
    void flowerResolvesToNatureBeforeDecorationOrMasonry() {
        CategoryAssignment assignment = PrimaryCategoryResolver.resolve(
                new ResourceLocation("minecraft:dandelion"),
                new FacetProfile(EnumSet.of(ItemFacet.PLACEABLE, ItemFacet.FLOWER, ItemFacet.DECORATIVE_BLOCK), Map.of())
        );

        assertEquals("nature", assignment.categoryId());
        assertEquals("flora", assignment.subcategoryId());
    }

    @Test
    void saplingsResolveToFloraBeforeGenericPlaceableOrMagic() {
        CategoryAssignment placeableOnlyAssignment = PrimaryCategoryResolver.resolve(
                new ResourceLocation("quark:blue_blossom_sapling"),
                new FacetProfile(EnumSet.of(ItemFacet.PLACEABLE), Map.of())
        );
        CategoryAssignment magicSaplingAssignment = PrimaryCategoryResolver.resolve(
                new ResourceLocation("malum:runewood_sapling"),
                new FacetProfile(EnumSet.of(ItemFacet.PLACEABLE, ItemFacet.MAGIC_REAGENT), Map.of())
        );
        CategoryAssignment taggedSaplingAssignment = PrimaryCategoryResolver.resolve(
                new ResourceLocation("fruitsdelight:durian_seed"),
                new FacetProfile(
                        EnumSet.of(ItemFacet.PLACEABLE),
                        Map.of(
                                SearchNodeKeys.BLOCK_TAGS, "minecraft:saplings,sereneseasons:summer_crops",
                                SearchNodeKeys.BLOCK_CLASS, "net.minecraft.world.level.block.SaplingBlock"
                        )
                )
        );

        assertEquals("nature", placeableOnlyAssignment.categoryId());
        assertEquals("flora", placeableOnlyAssignment.subcategoryId());
        assertEquals("nature", magicSaplingAssignment.categoryId());
        assertEquals("flora", magicSaplingAssignment.subcategoryId());
        assertEquals("nature", taggedSaplingAssignment.categoryId());
        assertEquals("flora", taggedSaplingAssignment.subcategoryId());
    }

    @Test
    void naturalBlocksResolveBeforeMagicUtilityAndLightingSignals() {
        CategoryAssignment runewoodPlanksAssignment = PrimaryCategoryResolver.resolve(
                new ResourceLocation("malum:runewood_planks"),
                new FacetProfile(
                        EnumSet.of(ItemFacet.PLACEABLE, ItemFacet.WOOD_BLOCK, ItemFacet.MAGIC_REAGENT),
                        Map.of(SearchNodeKeys.BLOCK_TAGS, "minecraft:planks")
                )
        );
        CategoryAssignment runewoodLeavesAssignment = PrimaryCategoryResolver.resolve(
                new ResourceLocation("malum:runewood_leaves"),
                new FacetProfile(
                        EnumSet.of(ItemFacet.PLACEABLE, ItemFacet.LEAVES, ItemFacet.MAGIC_REAGENT),
                        Map.of(SearchNodeKeys.BLOCK_TAGS, "minecraft:leaves")
                )
        );
        CategoryAssignment glowingQuartzAssignment = PrimaryCategoryResolver.resolve(
                new ResourceLocation("example:glowing_quartz"),
                new FacetProfile(EnumSet.of(ItemFacet.PLACEABLE, ItemFacet.GEM, ItemFacet.LIGHT_SOURCE), Map.of())
        );

        assertEquals("nature", runewoodPlanksAssignment.categoryId());
        assertEquals("wood", runewoodPlanksAssignment.subcategoryId());
        assertEquals("nature", runewoodLeavesAssignment.categoryId());
        assertEquals("flora", runewoodLeavesAssignment.subcategoryId());
        assertEquals("tech", glowingQuartzAssignment.categoryId());
        assertEquals("ingots", glowingQuartzAssignment.subcategoryId());
    }

    @Test
    void plantSeedsResolveToSeedsButSeedContainersAndCrystalSeedsDoNot() {
        CategoryAssignment plantSeedAssignment = PrimaryCategoryResolver.resolve(
                new ResourceLocation("fruitsdelight:hamimelon_seeds"),
                new FacetProfile(EnumSet.of(ItemFacet.PLACEABLE, ItemFacet.CROP), Map.of())
        );
        CategoryAssignment seedPouchAssignment = PrimaryCategoryResolver.resolve(
                new ResourceLocation("quark:seed_pouch"),
                new FacetProfile(EnumSet.of(ItemFacet.UTILITY_MISC), Map.of())
        );
        CategoryAssignment seedOilAssignment = PrimaryCategoryResolver.resolve(
                new ResourceLocation("createaddition:seed_oil_bucket"),
                new FacetProfile(EnumSet.of(ItemFacet.FLUID_CONTAINER, ItemFacet.UTILITY_MISC), Map.of())
        );
        CategoryAssignment crystalSeedAssignment = PrimaryCategoryResolver.resolve(
                new ResourceLocation("ae2cs:certus_quartz_seed"),
                new FacetProfile(EnumSet.of(ItemFacet.GEM), Map.of())
        );

        assertEquals("nature", plantSeedAssignment.categoryId());
        assertEquals("seeds", plantSeedAssignment.subcategoryId());
        assertEquals("utility", seedPouchAssignment.categoryId());
        assertEquals("misc", seedPouchAssignment.subcategoryId());
        assertEquals("utility", seedOilAssignment.categoryId());
        assertEquals("misc", seedOilAssignment.subcategoryId());
        assertEquals("tech", crystalSeedAssignment.categoryId());
        assertEquals("ingots", crystalSeedAssignment.subcategoryId());
    }

    @Test
    void integratedCircuitsResolveToTechCircuits() {
        CategoryAssignment assignment = PrimaryCategoryResolver.resolve(
                new ResourceLocation("ccbr:integrated_circuit"),
                new FacetProfile(EnumSet.of(ItemFacet.TECH_COMPONENT), Map.of())
        );

        assertEquals("tech", assignment.categoryId());
        assertEquals("circuits", assignment.subcategoryId());
    }

    @Test
    void cropLikeBlockEntitiesResolveToCropsBeforeCreateTech() {
        CategoryAssignment assignment = PrimaryCategoryResolver.resolve(
                new ResourceLocation("create_winery:red_grape_bush_stage_2"),
                new FacetProfile(
                        EnumSet.of(ItemFacet.PLACEABLE, ItemFacet.HAS_BLOCK_ENTITY),
                        Map.of(SearchNodeKeys.BLOCK_CLASS, "create_winery.block.RedGrapeBushStage2Block")
                )
        );

        assertEquals("nature", assignment.categoryId());
        assertEquals("crops", assignment.subcategoryId());
    }

    @Test
    void createFamilyDecorativeBricksDoNotResolveAsTechParts() {
        CategoryAssignment assignment = PrimaryCategoryResolver.resolve(
                new ResourceLocation("createdeco:short_blue_bricks"),
                new FacetProfile(
                        EnumSet.of(ItemFacet.PLACEABLE),
                        Map.of(
                                SearchNodeKeys.BLOCKS_MATERIAL, "stone",
                                SearchNodeKeys.VARIANT_GROUP, "bricks"
                        )
                )
        );

        assertEquals("masonry", assignment.categoryId());
        assertEquals("full_block", assignment.subcategoryId());
    }

    @Test
    void doorsResolveAsMasonryEvenWhenDecorativeAndPaneLike() {
        CategoryAssignment assignment = PrimaryCategoryResolver.resolve(
                new ResourceLocation("mcwbiomesoplenty:maple_japanese_door"),
                new FacetProfile(
                        EnumSet.of(ItemFacet.PLACEABLE, ItemFacet.GLASS_BLOCK, ItemFacet.DECORATIVE_BLOCK, ItemFacet.DOOR, ItemFacet.PANE),
                        Map.of("blockShape", "pane")
                )
        );

        assertEquals("masonry", assignment.categoryId());
        assertEquals("functional", assignment.subcategoryId());
    }

    @Test
    void drinksAndMealsPreserveNatureSubcategories() {
        CategoryAssignment drinkAssignment = PrimaryCategoryResolver.resolve(
                new ResourceLocation("minecraft:honey_bottle"),
                new FacetProfile(EnumSet.of(ItemFacet.EDIBLE, ItemFacet.FOOD_DRINK), Map.of())
        );
        CategoryAssignment mealAssignment = PrimaryCategoryResolver.resolve(
                new ResourceLocation("minecraft:mushroom_stew"),
                new FacetProfile(EnumSet.of(ItemFacet.EDIBLE, ItemFacet.FOOD_MEAL, ItemFacet.FOOD_DRINK, ItemFacet.FUNGI), Map.of())
        );

        assertEquals("nature", drinkAssignment.categoryId());
        assertEquals("drinks", drinkAssignment.subcategoryId());
        assertEquals("nature", mealAssignment.categoryId());
        assertEquals("meals", mealAssignment.subcategoryId());
    }

    @Test
    void foodSubfacetsResolveToNatureEvenWithoutEdibleComponent() {
        CategoryAssignment drinkAssignment = PrimaryCategoryResolver.resolve(
                new ResourceLocation("legendarysurvivaloverhaul:apple_juice"),
                new FacetProfile(EnumSet.of(ItemFacet.FOOD_DRINK), Map.of())
        );
        CategoryAssignment mealAssignment = PrimaryCategoryResolver.resolve(
                new ResourceLocation("apocalypsenow:canned_soup"),
                new FacetProfile(EnumSet.of(ItemFacet.FOOD_MEAL, ItemFacet.FOOD_DRINK), Map.of())
        );
        CategoryAssignment drinkOnlySoupAssignment = PrimaryCategoryResolver.resolve(
                new ResourceLocation("apocalypsenow:canned_soup"),
                new FacetProfile(EnumSet.of(ItemFacet.FOOD_DRINK), Map.of())
        );

        assertEquals("nature", drinkAssignment.categoryId());
        assertEquals("drinks", drinkAssignment.subcategoryId());
        assertEquals("nature", mealAssignment.categoryId());
        assertEquals("meals", mealAssignment.subcategoryId());
        assertEquals("nature", drinkOnlySoupAssignment.categoryId());
        assertEquals("meals", drinkOnlySoupAssignment.subcategoryId());
    }

    @Test
    void techComponentFacetResolvesToTechParts() {
        CategoryAssignment assignment = PrimaryCategoryResolver.resolve(
                new ResourceLocation("immersiveengineering:wire_copper"),
                new FacetProfile(EnumSet.of(ItemFacet.TECH_COMPONENT), Map.of())
        );

        assertEquals("tech", assignment.categoryId());
        assertEquals("cables", assignment.subcategoryId());
    }

    @Test
    void templatesDoNotResolveAsIngredientsWhenTheyHaveIncidentalOrganicSignals() {
        CategoryAssignment assignment = PrimaryCategoryResolver.resolve(
                new ResourceLocation("silentgear:leggings_blueprint"),
                new FacetProfile(EnumSet.of(ItemFacet.TEMPLATE, ItemFacet.INGREDIENT_ORGANIC), Map.of())
        );

        assertEquals("tech", assignment.categoryId());
        assertEquals("templates", assignment.subcategoryId());
    }

    @Test
    void incidentalCurioAndTechTagsDoNotOverrideClearIngredientOrDecorationRoles() {
        CategoryAssignment dyeAssignment = PrimaryCategoryResolver.resolve(
                new ResourceLocation("minecraft:white_dye"),
                new FacetProfile(EnumSet.of(ItemFacet.CURIO, ItemFacet.INGREDIENT_DYE), Map.of())
        );
        CategoryAssignment lanternAssignment = PrimaryCategoryResolver.resolve(
                new ResourceLocation("minecraft:lantern"),
                new FacetProfile(EnumSet.of(ItemFacet.PLACEABLE, ItemFacet.CURIO, ItemFacet.LIGHT_SOURCE), Map.of())
        );
        CategoryAssignment stickAssignment = PrimaryCategoryResolver.resolve(
                new ResourceLocation("minecraft:stick"),
                new FacetProfile(EnumSet.of(ItemFacet.TECH_COMPONENT, ItemFacet.LOG, ItemFacet.INGREDIENT_ORGANIC), Map.of())
        );

        assertEquals("ingredients", dyeAssignment.categoryId());
        assertEquals("dyes", dyeAssignment.subcategoryId());
        assertEquals("decoration", lanternAssignment.categoryId());
        assertEquals("lighting", lanternAssignment.subcategoryId());
        assertEquals("ingredients", stickAssignment.categoryId());
        assertEquals("organic", stickAssignment.subcategoryId());
    }

    @Test
    void decorativePassiveComparatorBlocksDoNotBecomeMachines() {
        CategoryAssignment decoratedPotAssignment = PrimaryCategoryResolver.resolve(
                new ResourceLocation("minecraft:decorated_pot"),
                new FacetProfile(EnumSet.of(ItemFacet.PLACEABLE, ItemFacet.DECORATIVE_BLOCK, ItemFacet.HAS_BLOCK_ENTITY, ItemFacet.PASSIVE_COMPARATOR_OUTPUT, ItemFacet.REDSTONE_SIGNAL), Map.of())
        );

        assertEquals("decoration", decoratedPotAssignment.categoryId());
        assertEquals("furniture", decoratedPotAssignment.subcategoryId());
    }

    @Test
    void passiveComparatorFoodBlocksStayInFoodGroups() {
        CategoryAssignment pieAssignment = PrimaryCategoryResolver.resolve(
                new ResourceLocation("farmersdelight:apple_pie"),
                new FacetProfile(EnumSet.of(ItemFacet.PLACEABLE, ItemFacet.PLACEABLE_FOOD, ItemFacet.REDSTONE_SIGNAL), Map.of())
        );
        CategoryAssignment displayPlateAssignment = PrimaryCategoryResolver.resolve(
                new ResourceLocation("createfood:beef_meatball_sandwich_plate_block"),
                new FacetProfile(EnumSet.of(ItemFacet.PLACEABLE, ItemFacet.FOOD_PROTEIN, ItemFacet.REDSTONE_SIGNAL), Map.of())
        );
        CategoryAssignment realRedstoneAssignment = PrimaryCategoryResolver.resolve(
                new ResourceLocation("minecraft:target"),
                new FacetProfile(EnumSet.of(ItemFacet.PLACEABLE, ItemFacet.REDSTONE_LOGIC, ItemFacet.REDSTONE_SIGNAL), Map.of())
        );

        assertEquals("nature", pieAssignment.categoryId());
        assertEquals("meals", pieAssignment.subcategoryId());
        assertEquals("nature", displayPlateAssignment.categoryId());
        assertEquals("meals", displayPlateAssignment.subcategoryId());
        assertEquals("tech", realRedstoneAssignment.categoryId());
        assertEquals("redstone", realRedstoneAssignment.subcategoryId());
    }

    @Test
    void armorResolvesToArmor() {
        CategoryAssignment assignment = PrimaryCategoryResolver.resolve(
                new ResourceLocation("minecraft:iron_helmet"),
                new FacetProfile(EnumSet.of(ItemFacet.ARMOR_HEAD), Map.of())
        );

        assertEquals("armor", assignment.categoryId());
        assertEquals("head", assignment.subcategoryId());
    }

    @Test
    void curioFacetResolvesToArmorCurios() {
        CategoryAssignment assignment = PrimaryCategoryResolver.resolve(
                new ResourceLocation("nameless_trinkets:missing_page"),
                new FacetProfile(EnumSet.of(ItemFacet.CURIO), Map.of())
        );

        assertEquals("armor", assignment.categoryId());
        assertEquals("curios", assignment.subcategoryId());
    }

    @Test
    void chestArmorAndMaceResolveWithoutLegacyClassifier() {
        CategoryAssignment armorAssignment = PrimaryCategoryResolver.resolve(
                new ResourceLocation("minecraft:elytra"),
                new FacetProfile(EnumSet.of(ItemFacet.ARMOR_CHEST), Map.of())
        );
        CategoryAssignment animalArmorAssignment = PrimaryCategoryResolver.resolve(
                new ResourceLocation("minecraft:diamond_horse_armor"),
                new FacetProfile(EnumSet.of(ItemFacet.ARMOR_ANIMAL), Map.of())
        );
        CategoryAssignment weaponAssignment = PrimaryCategoryResolver.resolve(
                new ResourceLocation("minecraft:mace"),
                new FacetProfile(EnumSet.of(ItemFacet.MELEE_WEAPON), Map.of())
        );

        assertEquals("armor", armorAssignment.categoryId());
        assertEquals("chest", armorAssignment.subcategoryId());
        assertEquals("armor", animalArmorAssignment.categoryId());
        assertEquals("animal", animalArmorAssignment.subcategoryId());
        assertEquals("tools", weaponAssignment.categoryId());
        assertEquals("melee", weaponAssignment.subcategoryId());
    }

    @Test
    void utilityToolsResolveToToolsUtility() {
        CategoryAssignment assignment = PrimaryCategoryResolver.resolve(
                new ResourceLocation("minecraft:fishing_rod"),
                new FacetProfile(EnumSet.of(ItemFacet.UTILITY_TOOL), Map.of())
        );

        assertEquals("tools", assignment.categoryId());
        assertEquals("utility", assignment.subcategoryId());
    }

    @Test
    void throwableIngredientsResolveToIngredientsBeforeTools() {
        CategoryAssignment assignment = PrimaryCategoryResolver.resolve(
                new ResourceLocation("minecraft:egg"),
                new FacetProfile(EnumSet.of(ItemFacet.PROJECTILE, ItemFacet.INGREDIENT_ORGANIC), Map.of())
        );

        assertEquals("ingredients", assignment.categoryId());
        assertEquals("organic", assignment.subcategoryId());
    }

    @Test
    void redstoneDoorResolvesAccordingToPriorityRules() {
        CategoryAssignment assignment = PrimaryCategoryResolver.resolve(
                new ResourceLocation("minecraft:iron_door"),
                new FacetProfile(
                        EnumSet.of(ItemFacet.PLACEABLE, ItemFacet.DOOR, ItemFacet.REDSTONE_SIGNAL, ItemFacet.DECORATIVE_BLOCK),
                        Map.of("blockShape", "door")
                )
        );

        assertEquals("masonry", assignment.categoryId());
        assertEquals("functional", assignment.subcategoryId());
    }

    @Test
    void passiveSignalFurnitureResolvesToDecorationBeforeRedstone() {
        CategoryAssignment assignment = PrimaryCategoryResolver.resolve(
                new ResourceLocation("refurbished_furniture:yellow_toilet"),
                new FacetProfile(
                        EnumSet.of(
                                ItemFacet.PLACEABLE,
                                ItemFacet.HAS_BLOCK_ENTITY,
                                ItemFacet.REDSTONE_SIGNAL,
                                ItemFacet.DECORATIVE_BLOCK
                        ),
                        Map.of(SearchNodeKeys.BLOCKS_MATERIAL, "other_building")
                )
        );
        CategoryAssignment logicAssignment = PrimaryCategoryResolver.resolve(
                new ResourceLocation("refurbished_furniture:redstone_controller"),
                new FacetProfile(
                        EnumSet.of(
                                ItemFacet.PLACEABLE,
                                ItemFacet.HAS_BLOCK_ENTITY,
                                ItemFacet.REDSTONE_LOGIC,
                                ItemFacet.REDSTONE_SIGNAL,
                                ItemFacet.DECORATIVE_BLOCK
                        ),
                        Map.of(SearchNodeKeys.BLOCKS_MATERIAL, "other_building")
                )
        );
        CategoryAssignment cfmCabinetAssignment = PrimaryCategoryResolver.resolve(
                new ResourceLocation("cfm:oak_cabinet"),
                new FacetProfile(
                        EnumSet.of(
                                ItemFacet.PLACEABLE,
                                ItemFacet.HAS_BLOCK_ENTITY,
                                ItemFacet.REDSTONE_SIGNAL
                        ),
                        Map.of(SearchNodeKeys.BLOCKS_MATERIAL, "other_building")
                )
        );

        assertEquals("decoration", assignment.categoryId());
        assertEquals("furniture", assignment.subcategoryId());
        assertEquals("tech", logicAssignment.categoryId());
        assertEquals("redstone", logicAssignment.subcategoryId());
        assertEquals("decoration", cfmCabinetAssignment.categoryId());
        assertEquals("furniture", cfmCabinetAssignment.subcategoryId());
    }

    @Test
    void structuralVariantsResolveToBuildingBeforeDecoration() {
        CategoryAssignment stairs = PrimaryCategoryResolver.resolve(
                new ResourceLocation("minecraft:oak_stairs"),
                new FacetProfile(
                        EnumSet.of(ItemFacet.PLACEABLE, ItemFacet.STAIRS, ItemFacet.DECORATIVE_BLOCK),
                        Map.of("blockShape", "stairs", SearchNodeKeys.BLOCKS_MATERIAL, "wood")
                )
        );
        CategoryAssignment slab = PrimaryCategoryResolver.resolve(
                new ResourceLocation("minecraft:oak_slab"),
                new FacetProfile(
                        EnumSet.of(ItemFacet.PLACEABLE, ItemFacet.SLAB, ItemFacet.DECORATIVE_BLOCK),
                        Map.of("blockShape", "slab", SearchNodeKeys.BLOCKS_MATERIAL, "wood")
                )
        );
        CategoryAssignment door = PrimaryCategoryResolver.resolve(
                new ResourceLocation("minecraft:oak_door"),
                new FacetProfile(
                        EnumSet.of(ItemFacet.PLACEABLE, ItemFacet.DOOR, ItemFacet.DECORATIVE_BLOCK),
                        Map.of("blockShape", "door", SearchNodeKeys.BLOCKS_MATERIAL, "wood")
                )
        );

        assertEquals("masonry", stairs.categoryId());
        assertEquals("stairs", stairs.subcategoryId());
        assertEquals("masonry", slab.categoryId());
        assertEquals("slab", slab.subcategoryId());
        assertEquals("masonry", door.categoryId());
        assertEquals("functional", door.subcategoryId());
    }

    @Test
    void architecturalDecorModPlaceablesResolveToBuildingBeforeFurniture() {
        CategoryAssignment compactStairs = PrimaryCategoryResolver.resolve(
                new ResourceLocation("mcwstairs:acacia_compact_stairs"),
                new FacetProfile(EnumSet.of(ItemFacet.PLACEABLE), Map.of(SearchNodeKeys.BLOCKS_MATERIAL, "other_building"))
        );
        CategoryAssignment roof = PrimaryCategoryResolver.resolve(
                new ResourceLocation("mcwroofs:acacia_roof"),
                new FacetProfile(EnumSet.of(ItemFacet.PLACEABLE), Map.of(SearchNodeKeys.BLOCKS_MATERIAL, "other_building"))
        );
        CategoryAssignment railing = PrimaryCategoryResolver.resolve(
                new ResourceLocation("mcwstairs:acacia_railing"),
                new FacetProfile(EnumSet.of(ItemFacet.PLACEABLE), Map.of(SearchNodeKeys.BLOCKS_MATERIAL, "other_building"))
        );
        CategoryAssignment paving = PrimaryCategoryResolver.resolve(
                new ResourceLocation("mcwpaths:andesite_basket_weave_paving"),
                new FacetProfile(EnumSet.of(ItemFacet.PLACEABLE), Map.of(SearchNodeKeys.BLOCKS_MATERIAL, "stone"))
        );
        CategoryAssignment chair = PrimaryCategoryResolver.resolve(
                new ResourceLocation("mcwfurnitures:acacia_chair"),
                new FacetProfile(EnumSet.of(ItemFacet.PLACEABLE), Map.of(SearchNodeKeys.BLOCKS_MATERIAL, "other_building"))
        );

        assertEquals("masonry", compactStairs.categoryId());
        assertEquals("stairs", compactStairs.subcategoryId());
        assertEquals("masonry", roof.categoryId());
        assertEquals("full_block", roof.subcategoryId());
        assertEquals("masonry", railing.categoryId());
        assertEquals("fence", railing.subcategoryId());
        assertEquals("masonry", paving.categoryId());
        assertEquals("full_block", paving.subcategoryId());
        assertEquals("decoration", chair.categoryId());
        assertEquals("furniture", chair.subcategoryId());
    }

    @Test
    void naturalStoneFallsBackToGeologyBeforeMasonry() {
        CategoryAssignment assignment = PrimaryCategoryResolver.resolve(
                new ResourceLocation("minecraft:reinforced_deepslate"),
                new FacetProfile(
                        EnumSet.of(ItemFacet.PLACEABLE, ItemFacet.STONE_BLOCK),
                        Map.of(SearchNodeKeys.BLOCKS_MATERIAL, "stone")
                )
        );

        assertEquals("geology", assignment.categoryId());
        assertEquals("stone", assignment.subcategoryId());
    }

    @Test
    void mossyStoneDoesNotResolveAsFlora() {
        CategoryAssignment mossBlockAssignment = PrimaryCategoryResolver.resolve(
                new ResourceLocation("minecraft:moss_block"),
                new FacetProfile(
                        EnumSet.of(ItemFacet.PLACEABLE, ItemFacet.COMPOSTABLE, ItemFacet.SOIL_BLOCK),
                        Map.of(SearchNodeKeys.BLOCKS_MATERIAL, "soil")
                )
        );
        CategoryAssignment mossyCobblestoneAssignment = PrimaryCategoryResolver.resolve(
                new ResourceLocation("minecraft:mossy_cobblestone"),
                new FacetProfile(
                        EnumSet.of(ItemFacet.PLACEABLE),
                        Map.of(SearchNodeKeys.BLOCKS_MATERIAL, "stone")
                )
        );
        CategoryAssignment mossyCobblestoneStairsAssignment = PrimaryCategoryResolver.resolve(
                new ResourceLocation("mcwstairs:mossy_cobblestone_terrace_stairs"),
                new FacetProfile(
                        EnumSet.of(ItemFacet.PLACEABLE),
                        Map.of(SearchNodeKeys.BLOCKS_MATERIAL, "stone")
                )
        );
        CategoryAssignment mossyCobblestoneTextureAssignment = PrimaryCategoryResolver.resolve(
                new ResourceLocation("rechiseled:mossy_cobblestone_stripes"),
                new FacetProfile(
                        EnumSet.of(ItemFacet.PLACEABLE),
                        Map.of(SearchNodeKeys.BLOCKS_MATERIAL, "stone")
                )
        );

        assertEquals("nature", mossBlockAssignment.categoryId());
        assertEquals("flora", mossBlockAssignment.subcategoryId());
        assertEquals("geology", mossyCobblestoneAssignment.categoryId());
        assertEquals("stone", mossyCobblestoneAssignment.subcategoryId());
        assertEquals("masonry", mossyCobblestoneStairsAssignment.categoryId());
        assertEquals("stairs", mossyCobblestoneStairsAssignment.subcategoryId());
        assertEquals("masonry", mossyCobblestoneTextureAssignment.categoryId());
        assertEquals("full_block", mossyCobblestoneTextureAssignment.subcategoryId());
    }

    @Test
    void ingredientAndSocialSlicesResolveWithoutLegacyClassifier() {
        CategoryAssignment ingredientAssignment = PrimaryCategoryResolver.resolve(
                new ResourceLocation("minecraft:ink_sac"),
                new FacetProfile(EnumSet.of(ItemFacet.INGREDIENT_DYE), Map.of())
        );
        CategoryAssignment socialAssignment = PrimaryCategoryResolver.resolve(
                new ResourceLocation("minecraft:player_head"),
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
                new ResourceLocation("minecraft:light_gray_bed"),
                new FacetProfile(EnumSet.of(ItemFacet.PLACEABLE, ItemFacet.HAS_BLOCK_ENTITY, ItemFacet.DECORATIVE_BLOCK), Map.of())
        );
        CategoryAssignment workstationAssignment = PrimaryCategoryResolver.resolve(
                new ResourceLocation("minecraft:composter"),
                new FacetProfile(EnumSet.of(ItemFacet.PLACEABLE, ItemFacet.MACHINE), Map.of())
        );

        assertEquals("decoration", bedAssignment.categoryId());
        assertEquals("furniture", bedAssignment.subcategoryId());
        assertEquals("tech", workstationAssignment.categoryId());
        assertEquals("machines", workstationAssignment.subcategoryId());
    }

    @Test
    void interactiveBlocksResolveToTechMachinesBeforeMasonry() {
        CategoryAssignment assignment = PrimaryCategoryResolver.resolve(
                new ResourceLocation("examplemod:crafting_terminal"),
                new FacetProfile(EnumSet.of(ItemFacet.PLACEABLE, ItemFacet.INTERACTIVE_BLOCK), Map.of())
        );

        assertEquals("tech", assignment.categoryId());
        assertEquals("machines", assignment.subcategoryId());
    }

    @Test
    void functionalDecorativeAndLivingBlocksResolveByNewPriority() {
        CategoryAssignment targetAssignment = PrimaryCategoryResolver.resolve(
                new ResourceLocation("minecraft:target"),
                new FacetProfile(EnumSet.of(ItemFacet.PLACEABLE, ItemFacet.REDSTONE_LOGIC, ItemFacet.REDSTONE_SIGNAL), Map.of())
        );
        CategoryAssignment lecternAssignment = PrimaryCategoryResolver.resolve(
                new ResourceLocation("minecraft:lectern"),
                new FacetProfile(EnumSet.of(ItemFacet.PLACEABLE, ItemFacet.MACHINE), Map.of())
        );
        CategoryAssignment carpetAssignment = PrimaryCategoryResolver.resolve(
                new ResourceLocation("minecraft:red_carpet"),
                new FacetProfile(EnumSet.of(ItemFacet.PLACEABLE, ItemFacet.DECORATIVE_BLOCK), Map.of())
        );
        CategoryAssignment frogspawnAssignment = PrimaryCategoryResolver.resolve(
                new ResourceLocation("minecraft:frogspawn"),
                new FacetProfile(EnumSet.of(ItemFacet.PLACEABLE, ItemFacet.NATURE_MISC), Map.of())
        );

        assertEquals("tech", targetAssignment.categoryId());
        assertEquals("redstone", targetAssignment.subcategoryId());
        assertEquals("tech", lecternAssignment.categoryId());
        assertEquals("machines", lecternAssignment.subcategoryId());
        assertEquals("decoration", carpetAssignment.categoryId());
        assertEquals("textiles", carpetAssignment.subcategoryId());
        assertEquals("nature", frogspawnAssignment.categoryId());
        assertEquals("flora", frogspawnAssignment.subcategoryId());
    }

    @Test
    void candlesHeadsCoralsSignsCobwebAndDripstoneResolveIntoExpectedBuckets() {
        CategoryAssignment candleAssignment = PrimaryCategoryResolver.resolve(
                new ResourceLocation("minecraft:white_candle"),
                new FacetProfile(EnumSet.of(ItemFacet.PLACEABLE, ItemFacet.DECORATIVE_BLOCK, ItemFacet.LIGHT_SOURCE), Map.of())
        );
        CategoryAssignment headAssignment = PrimaryCategoryResolver.resolve(
                new ResourceLocation("minecraft:zombie_head"),
                new FacetProfile(EnumSet.of(ItemFacet.PLACEABLE, ItemFacet.DECORATIVE_BLOCK), Map.of())
        );
        CategoryAssignment dripstoneAssignment = PrimaryCategoryResolver.resolve(
                new ResourceLocation("minecraft:pointed_dripstone"),
                new FacetProfile(EnumSet.of(ItemFacet.PLACEABLE, ItemFacet.STONE_BLOCK), Map.of(SearchNodeKeys.BLOCKS_MATERIAL, "stone"))
        );
        CategoryAssignment coralAssignment = PrimaryCategoryResolver.resolve(
                new ResourceLocation("minecraft:tube_coral"),
                new FacetProfile(EnumSet.of(ItemFacet.PLACEABLE, ItemFacet.NATURE_MISC), Map.of())
        );
        CategoryAssignment deadTubeCoralBlockAssignment = PrimaryCategoryResolver.resolve(
                new ResourceLocation("minecraft:dead_tube_coral_block"),
                new FacetProfile(EnumSet.of(ItemFacet.PLACEABLE, ItemFacet.CABLE, ItemFacet.TECH_COMPONENT, ItemFacet.NATURE_MISC), Map.of())
        );
        CategoryAssignment signAssignment = PrimaryCategoryResolver.resolve(
                new ResourceLocation("minecraft:oak_sign"),
                new FacetProfile(EnumSet.of(ItemFacet.PLACEABLE, ItemFacet.DECORATIVE_BLOCK), Map.of())
        );
        CategoryAssignment cobwebAssignment = PrimaryCategoryResolver.resolve(
                new ResourceLocation("minecraft:cobweb"),
                new FacetProfile(EnumSet.of(ItemFacet.PLACEABLE, ItemFacet.NATURE_MISC), Map.of())
        );
        CategoryAssignment deadBushAssignment = PrimaryCategoryResolver.resolve(
                new ResourceLocation("minecraft:dead_bush"),
                new FacetProfile(EnumSet.of(ItemFacet.PLACEABLE, ItemFacet.NATURE_MISC), Map.of())
        );
        CategoryAssignment sculkAssignment = PrimaryCategoryResolver.resolve(
                new ResourceLocation("minecraft:sculk"),
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
        assertEquals("nature", deadTubeCoralBlockAssignment.categoryId());
        assertEquals("flora", deadTubeCoralBlockAssignment.subcategoryId());
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
                new ResourceLocation("minecraft:written_book"),
                new FacetProfile(EnumSet.of(ItemFacet.UTILITY_MISC), Map.of())
        );
        CategoryAssignment transportAssignment = PrimaryCategoryResolver.resolve(
                new ResourceLocation("minecraft:bamboo_raft"),
                new FacetProfile(EnumSet.of(ItemFacet.TRANSPORT), Map.of())
        );
        CategoryAssignment ingredientAssignment = PrimaryCategoryResolver.resolve(
                new ResourceLocation("minecraft:turtle_scute"),
                new FacetProfile(EnumSet.of(ItemFacet.INGREDIENT_ORGANIC), Map.of())
        );
        CategoryAssignment decorationAssignment = PrimaryCategoryResolver.resolve(
                new ResourceLocation("minecraft:painting"),
                new FacetProfile(EnumSet.of(ItemFacet.DECORATIVE_BLOCK), Map.of())
        );
        CategoryAssignment techAssignment = PrimaryCategoryResolver.resolve(
                new ResourceLocation("minecraft:coal"),
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
                new ResourceLocation("minecraft:paper"),
                new FacetProfile(EnumSet.noneOf(ItemFacet.class), Map.of())
        );

        assertEquals("misc", assignment.categoryId());
        assertEquals("unknown", assignment.subcategoryId());
    }

    @Test
    void uncraftableLowSignalFullBlocksBiasToTerrainInsteadOfBuilding() {
        CategoryAssignment terrainAssignment = PrimaryCategoryResolver.resolve(
                new ResourceLocation("minecraft:sculk"),
                new FacetProfile(
                        EnumSet.of(ItemFacet.PLACEABLE),
                        Map.of(
                                SearchNodeKeys.OBTAINABILITY, "no_recipe",
                                SearchNodeKeys.BLOCKS_MATERIAL, "other_building"
                        )
                )
        );
        CategoryAssignment stoneAssignment = PrimaryCategoryResolver.resolve(
                new ResourceLocation("minecraft:pointed_dripstone_block"),
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

    @Test
    void createFamilyPriorsBiasAmbiguousBlocksTowardTechAndDecoration() {
        CategoryAssignment machineLikeAssignment = PrimaryCategoryResolver.resolve(
                new ResourceLocation("create:andesite_casing"),
                new FacetProfile(
                        EnumSet.of(ItemFacet.PLACEABLE),
                        Map.of(SearchNodeKeys.BLOCKS_MATERIAL, "other_building")
                )
        );
        CategoryAssignment decorativeAssignment = PrimaryCategoryResolver.resolve(
                new ResourceLocation("displaydelight:oak_display_board"),
                new FacetProfile(
                        EnumSet.of(ItemFacet.PLACEABLE, ItemFacet.DECORATIVE_BLOCK),
                        Map.of()
                )
        );
        CategoryAssignment jarAssignment = PrimaryCategoryResolver.resolve(
                new ResourceLocation("supplementaries:tater_in_a_jar"),
                new FacetProfile(
                        EnumSet.of(ItemFacet.PLACEABLE, ItemFacet.DECORATIVE_BLOCK),
                        Map.of(SearchNodeKeys.OBTAINABILITY, "no_recipe")
                )
        );
        CategoryAssignment partsAssignment = PrimaryCategoryResolver.resolve(
                new ResourceLocation("create:belt_connector"),
                new FacetProfile(
                        EnumSet.of(ItemFacet.PLACEABLE),
                        Map.of(SearchNodeKeys.BLOCKS_MATERIAL, "other_building")
                )
        );
        CategoryAssignment machineAssignment = PrimaryCategoryResolver.resolve(
                new ResourceLocation("create:mechanical_press"),
                new FacetProfile(
                        EnumSet.of(ItemFacet.PLACEABLE),
                        Map.of(SearchNodeKeys.BLOCKS_MATERIAL, "other_building")
                )
        );
        CategoryAssignment stagedMachineAssignment = PrimaryCategoryResolver.resolve(
                new ResourceLocation("create:blaze_burner"),
                new FacetProfile(
                        EnumSet.of(ItemFacet.PLACEABLE),
                        Map.of(
                                SearchNodeKeys.BLOCKS_MATERIAL, "other_building",
                                SearchNodeKeys.OBTAINABILITY, "no_recipe"
                        )
                )
        );

        assertEquals("tech", machineLikeAssignment.categoryId());
        assertEquals("parts", machineLikeAssignment.subcategoryId());
        assertEquals("tech", partsAssignment.categoryId());
        assertEquals("parts", partsAssignment.subcategoryId());
        assertEquals("tech", machineAssignment.categoryId());
        assertEquals("machines", machineAssignment.subcategoryId());
        assertEquals("tech", stagedMachineAssignment.categoryId());
        assertEquals("machines", stagedMachineAssignment.subcategoryId());
        assertEquals("decoration", decorativeAssignment.categoryId());
        assertEquals("furniture", decorativeAssignment.subcategoryId());
        assertEquals("decoration", jarAssignment.categoryId());
        assertEquals("furniture", jarAssignment.subcategoryId());
    }

    @Test
    void foodFamilyPriorsBiasAmbiguousPlaceablesTowardNature() {
        CategoryAssignment assignment = PrimaryCategoryResolver.resolve(
                new ResourceLocation("farmersdelight:tomato_crate"),
                new FacetProfile(
                        EnumSet.of(ItemFacet.PLACEABLE),
                        Map.of(SearchNodeKeys.BLOCKS_MATERIAL, "other_building")
                )
        );

        assertEquals("nature", assignment.categoryId());
        assertEquals("crops", assignment.subcategoryId());
    }

    @Test
    void foodFamilyPriorsDoNotSweepUtilityPlaceablesIntoSnacks() {
        CategoryAssignment stoveAssignment = PrimaryCategoryResolver.resolve(
                new ResourceLocation("farmersdelight:stove"),
                new FacetProfile(
                        EnumSet.of(ItemFacet.PLACEABLE, ItemFacet.HAS_BLOCK_ENTITY),
                        Map.of(SearchNodeKeys.BLOCKS_MATERIAL, "other_building")
                )
        );
        CategoryAssignment skilletAssignment = PrimaryCategoryResolver.resolve(
                new ResourceLocation("farmersdelight:skillet"),
                new FacetProfile(
                        EnumSet.of(ItemFacet.PLACEABLE, ItemFacet.HAS_BLOCK_ENTITY, ItemFacet.MELEE_WEAPON),
                        Map.of(SearchNodeKeys.BLOCKS_MATERIAL, "other_building")
                )
        );
        CategoryAssignment rugAssignment = PrimaryCategoryResolver.resolve(
                new ResourceLocation("farmersdelight:canvas_rug"),
                new FacetProfile(
                        EnumSet.of(ItemFacet.PLACEABLE, ItemFacet.DECORATIVE_BLOCK),
                        Map.of(SearchNodeKeys.BLOCKS_MATERIAL, "other_building")
                )
        );

        assertEquals("tech", stoveAssignment.categoryId());
        assertEquals("machines", stoveAssignment.subcategoryId());
        assertEquals("tools", skilletAssignment.categoryId());
        assertEquals("melee", skilletAssignment.subcategoryId());
        assertEquals("decoration", rugAssignment.categoryId());
        assertEquals("textiles", rugAssignment.subcategoryId());
    }

    @Test
    void storageAndDecorFamilyPriorsHandleCommonModpackFamilies() {
        CategoryAssignment ae2ChestAssignment = PrimaryCategoryResolver.resolve(
                new ResourceLocation("ae2:chest"),
                new FacetProfile(
                        EnumSet.of(ItemFacet.PLACEABLE),
                        Map.of(SearchNodeKeys.BLOCKS_MATERIAL, "other_building")
                )
        );
        CategoryAssignment backpackAssignment = PrimaryCategoryResolver.resolve(
                new ResourceLocation("sophisticatedbackpacks:backpack"),
                new FacetProfile(EnumSet.noneOf(ItemFacet.class), Map.of())
        );
        CategoryAssignment furnitureAssignment = PrimaryCategoryResolver.resolve(
                new ResourceLocation("mcwfurnitures:oak_chair"),
                new FacetProfile(
                        EnumSet.of(ItemFacet.PLACEABLE),
                        Map.of(SearchNodeKeys.BLOCKS_MATERIAL, "other_building")
                )
        );
        CategoryAssignment drawerAssignment = PrimaryCategoryResolver.resolve(
                new ResourceLocation("storagedrawers:oak_full_drawers_1"),
                new FacetProfile(
                        EnumSet.of(ItemFacet.PLACEABLE),
                        Map.of(SearchNodeKeys.BLOCKS_MATERIAL, "wood")
                )
        );
        CategoryAssignment holidayAssignment = PrimaryCategoryResolver.resolve(
                new ResourceLocation("mcwholidays:gingerbread_chair"),
                new FacetProfile(
                        EnumSet.of(ItemFacet.PLACEABLE),
                        Map.of(SearchNodeKeys.BLOCKS_MATERIAL, "other_building")
                )
        );

        assertEquals("tech", ae2ChestAssignment.categoryId());
        assertEquals("machines", ae2ChestAssignment.subcategoryId());
        assertEquals("armor", backpackAssignment.categoryId());
        assertEquals("curios", backpackAssignment.subcategoryId());
        assertEquals("tech", drawerAssignment.categoryId());
        assertEquals("machines", drawerAssignment.subcategoryId());
        assertEquals("decoration", furnitureAssignment.categoryId());
        assertEquals("furniture", furnitureAssignment.subcategoryId());
        assertEquals("decoration", holidayAssignment.categoryId());
        assertEquals("furniture", holidayAssignment.subcategoryId());
    }

    @Test
    void storageFamilyTechIntermediatesResolveToCircuits() {
        CategoryAssignment siliconAssignment = PrimaryCategoryResolver.resolve(
                new ResourceLocation("ae2:silicon"),
                new FacetProfile(EnumSet.noneOf(ItemFacet.class), Map.of())
        );
        CategoryAssignment printedAssignment = PrimaryCategoryResolver.resolve(
                new ResourceLocation("ae2:printed_logic_processor"),
                new FacetProfile(EnumSet.noneOf(ItemFacet.class), Map.of())
        );
        CategoryAssignment processorAssignment = PrimaryCategoryResolver.resolve(
                new ResourceLocation("ae2:logic_processor"),
                new FacetProfile(EnumSet.noneOf(ItemFacet.class), Map.of())
        );
        CategoryAssignment partsAssignment = PrimaryCategoryResolver.resolve(
                new ResourceLocation("ae2:me_drive"),
                new FacetProfile(EnumSet.noneOf(ItemFacet.class), Map.of())
        );
        CategoryAssignment refinedStorageUpgradeAssignment = PrimaryCategoryResolver.resolve(
                new ResourceLocation("refinedstorage:speed_upgrade"),
                new FacetProfile(EnumSet.noneOf(ItemFacet.class), Map.of())
        );
        CategoryAssignment storageDrawersKeyAssignment = PrimaryCategoryResolver.resolve(
                new ResourceLocation("storagedrawers:personal_key"),
                new FacetProfile(EnumSet.noneOf(ItemFacet.class), Map.of())
        );
        CategoryAssignment backpackUpgradeAssignment = PrimaryCategoryResolver.resolve(
                new ResourceLocation("sophisticatedbackpacks:stack_upgrade"),
                new FacetProfile(EnumSet.noneOf(ItemFacet.class), Map.of())
        );

        assertEquals("tech", siliconAssignment.categoryId());
        assertEquals("circuits", siliconAssignment.subcategoryId());
        assertEquals("tech", printedAssignment.categoryId());
        assertEquals("circuits", printedAssignment.subcategoryId());
        assertEquals("tech", processorAssignment.categoryId());
        assertEquals("circuits", processorAssignment.subcategoryId());
        assertEquals("tech", partsAssignment.categoryId());
        assertEquals("parts", partsAssignment.subcategoryId());
        assertEquals("tech", refinedStorageUpgradeAssignment.categoryId());
        assertEquals("upgrades", refinedStorageUpgradeAssignment.subcategoryId());
        assertEquals("tech", storageDrawersKeyAssignment.categoryId());
        assertEquals("machines", storageDrawersKeyAssignment.subcategoryId());
        assertEquals("tech", backpackUpgradeAssignment.categoryId());
        assertEquals("upgrades", backpackUpgradeAssignment.subcategoryId());
    }

    @Test
    void refinedStorageAndAutomationFamiliesResolveTechnicalIntermediates() {
        CategoryAssignment rsProcessorAssignment = PrimaryCategoryResolver.resolve(
                new ResourceLocation("refinedstorage:raw_basic_processor"),
                new FacetProfile(EnumSet.noneOf(ItemFacet.class), Map.of())
        );
        CategoryAssignment pneumaticCircuitAssignment = PrimaryCategoryResolver.resolve(
                new ResourceLocation("pneumaticcraft:printed_circuit_board"),
                new FacetProfile(EnumSet.noneOf(ItemFacet.class), Map.of())
        );
        CategoryAssignment pneumaticPartAssignment = PrimaryCategoryResolver.resolve(
                new ResourceLocation("pneumaticcraft:pressure_tube"),
                new FacetProfile(EnumSet.noneOf(ItemFacet.class), Map.of())
        );
        CategoryAssignment pneumaticMachineAssignment = PrimaryCategoryResolver.resolve(
                new ResourceLocation("pneumaticcraft:charging_station"),
                new FacetProfile(EnumSet.of(ItemFacet.PLACEABLE), Map.of())
        );
        CategoryAssignment securityModuleAssignment = PrimaryCategoryResolver.resolve(
                new ResourceLocation("securitycraft:blacklist_module"),
                new FacetProfile(EnumSet.noneOf(ItemFacet.class), Map.of())
        );

        assertEquals("tech", rsProcessorAssignment.categoryId());
        assertEquals("circuits", rsProcessorAssignment.subcategoryId());
        assertEquals("tech", pneumaticCircuitAssignment.categoryId());
        assertEquals("circuits", pneumaticCircuitAssignment.subcategoryId());
        assertEquals("tech", pneumaticPartAssignment.categoryId());
        assertEquals("cables", pneumaticPartAssignment.subcategoryId());
        assertEquals("tech", pneumaticMachineAssignment.categoryId());
        assertEquals("machines", pneumaticMachineAssignment.subcategoryId());
        assertEquals("tech", securityModuleAssignment.categoryId());
        assertEquals("circuits", securityModuleAssignment.subcategoryId());
    }

    @Test
    void createFamilyHandheldsResolveOutOfMagic() {
        CategoryAssignment wrenchAssignment = PrimaryCategoryResolver.resolve(
                new ResourceLocation("create:wrench"),
                new FacetProfile(EnumSet.noneOf(ItemFacet.class), Map.of())
        );
        CategoryAssignment cannonAssignment = PrimaryCategoryResolver.resolve(
                new ResourceLocation("create:potato_cannon"),
                new FacetProfile(EnumSet.noneOf(ItemFacet.class), Map.of())
        );
        CategoryAssignment filterAssignment = PrimaryCategoryResolver.resolve(
                new ResourceLocation("create:attribute_filter"),
                new FacetProfile(EnumSet.noneOf(ItemFacet.class), Map.of())
        );
        CategoryAssignment schematicAssignment = PrimaryCategoryResolver.resolve(
                new ResourceLocation("create:schematic_and_quill"),
                new FacetProfile(EnumSet.noneOf(ItemFacet.class), Map.of())
        );
        CategoryAssignment symmetryAssignment = PrimaryCategoryResolver.resolve(
                new ResourceLocation("create:wand_of_symmetry"),
                new FacetProfile(EnumSet.of(ItemFacet.MAGIC_ARTIFACT), Map.of())
        );

        assertEquals("tools", wrenchAssignment.categoryId());
        assertEquals("utility", wrenchAssignment.subcategoryId());
        assertEquals("tools", cannonAssignment.categoryId());
        assertEquals("ranged", cannonAssignment.subcategoryId());
        assertEquals("utility", filterAssignment.categoryId());
        assertEquals("misc", filterAssignment.subcategoryId());
        assertEquals("tech", schematicAssignment.categoryId());
        assertEquals("templates", schematicAssignment.subcategoryId());
        assertEquals("magic", symmetryAssignment.categoryId());
        assertEquals("artifacts", symmetryAssignment.subcategoryId());
    }

    @Test
    void createAddonsBiasTransportAndPowerFamilies() {
        CategoryAssignment trackAssignment = PrimaryCategoryResolver.resolve(
                new ResourceLocation("railways:redstone_track"),
                new FacetProfile(
                        EnumSet.of(ItemFacet.PLACEABLE),
                        Map.of(SearchNodeKeys.BLOCKS_MATERIAL, "other_building")
                )
        );
        CategoryAssignment handcarAssignment = PrimaryCategoryResolver.resolve(
                new ResourceLocation("railways:handcar"),
                new FacetProfile(
                        EnumSet.of(ItemFacet.PLACEABLE),
                        Map.of(SearchNodeKeys.BLOCKS_MATERIAL, "other_building")
                )
        );
        CategoryAssignment connectorAssignment = PrimaryCategoryResolver.resolve(
                new ResourceLocation("railways:oak_storage_connector"),
                new FacetProfile(
                        EnumSet.of(ItemFacet.PLACEABLE),
                        Map.of(SearchNodeKeys.BLOCKS_MATERIAL, "wood")
                )
        );
        CategoryAssignment generatorAssignment = PrimaryCategoryResolver.resolve(
                new ResourceLocation("createaddition:alternator"),
                new FacetProfile(
                        EnumSet.of(ItemFacet.PLACEABLE),
                        Map.of(SearchNodeKeys.BLOCKS_MATERIAL, "other_building")
                )
        );
        CategoryAssignment wireAssignment = PrimaryCategoryResolver.resolve(
                new ResourceLocation("createaddition:copper_wire"),
                new FacetProfile(
                        EnumSet.of(ItemFacet.PLACEABLE),
                        Map.of(SearchNodeKeys.BLOCKS_MATERIAL, "other_building")
                )
        );

        assertEquals("tech", trackAssignment.categoryId());
        assertEquals("transport", trackAssignment.subcategoryId());
        assertEquals("tech", handcarAssignment.categoryId());
        assertEquals("transport", handcarAssignment.subcategoryId());
        assertEquals("tech", connectorAssignment.categoryId());
        assertEquals("parts", connectorAssignment.subcategoryId());
        assertEquals("tech", generatorAssignment.categoryId());
        assertEquals("machines", generatorAssignment.subcategoryId());
        assertEquals("tech", wireAssignment.categoryId());
        assertEquals("cables", wireAssignment.subcategoryId());
    }

    @Test
    void repeatedModdedFamiliesResolveToSpecificSubcategories() {
        CategoryAssignment cableAssignment = PrimaryCategoryResolver.resolve(
                new ResourceLocation("ami_test:copper_wire"),
                new FacetProfile(EnumSet.of(ItemFacet.CABLE, ItemFacet.TECH_COMPONENT), Map.of())
        );
        CategoryAssignment upgradeAssignment = PrimaryCategoryResolver.resolve(
                new ResourceLocation("ami_test:speed_upgrade"),
                new FacetProfile(EnumSet.of(ItemFacet.UPGRADE), Map.of())
        );
        CategoryAssignment templateAssignment = PrimaryCategoryResolver.resolve(
                new ResourceLocation("ami_test:mold_plate"),
                new FacetProfile(EnumSet.of(ItemFacet.TEMPLATE), Map.of())
        );
        CategoryAssignment ammoAssignment = PrimaryCategoryResolver.resolve(
                new ResourceLocation("ami_test:rifle_round"),
                new FacetProfile(EnumSet.of(ItemFacet.PROJECTILE), Map.of())
        );
        CategoryAssignment medicalAssignment = PrimaryCategoryResolver.resolve(
                new ResourceLocation("ami_test:bandage"),
                new FacetProfile(EnumSet.of(ItemFacet.UTILITY_MEDICAL), Map.of())
        );
        CategoryAssignment currencyAssignment = PrimaryCategoryResolver.resolve(
                new ResourceLocation("ami_test:gold_coin"),
                new FacetProfile(EnumSet.of(ItemFacet.UTILITY_CURRENCY), Map.of())
        );

        assertEquals("tech", cableAssignment.categoryId());
        assertEquals("cables", cableAssignment.subcategoryId());
        assertEquals("tech", upgradeAssignment.categoryId());
        assertEquals("upgrades", upgradeAssignment.subcategoryId());
        assertEquals("tech", templateAssignment.categoryId());
        assertEquals("templates", templateAssignment.subcategoryId());
        assertEquals("tools", ammoAssignment.categoryId());
        assertEquals("ammo", ammoAssignment.subcategoryId());
        assertEquals("utility", medicalAssignment.categoryId());
        assertEquals("medical", medicalAssignment.subcategoryId());
        assertEquals("utility", currencyAssignment.categoryId());
        assertEquals("currency", currencyAssignment.subcategoryId());
    }

    @Test
    void createAddonDomainPriorsHandleWineryEnchantingAndOreExtraction() {
        CategoryAssignment wineryMachineAssignment = PrimaryCategoryResolver.resolve(
                new ResourceLocation("create_winery:fermentation_vat"),
                new FacetProfile(
                        EnumSet.of(ItemFacet.PLACEABLE),
                        Map.of(SearchNodeKeys.BLOCKS_MATERIAL, "other_building")
                )
        );
        CategoryAssignment oreMachineAssignment = PrimaryCategoryResolver.resolve(
                new ResourceLocation("createoreexcavation:ore_drill"),
                new FacetProfile(
                        EnumSet.of(ItemFacet.PLACEABLE),
                        Map.of(SearchNodeKeys.BLOCKS_MATERIAL, "other_building")
                )
        );
        CategoryAssignment orePartAssignment = PrimaryCategoryResolver.resolve(
                new ResourceLocation("createoreexcavation:vein_finder_core"),
                new FacetProfile(
                        EnumSet.of(ItemFacet.PLACEABLE),
                        Map.of(SearchNodeKeys.BLOCKS_MATERIAL, "other_building")
                )
        );
        CategoryAssignment experienceAssignment = PrimaryCategoryResolver.resolve(
                new ResourceLocation("create_enchantment_industry:nugget_of_super_experience"),
                new FacetProfile(EnumSet.noneOf(ItemFacet.class), Map.of())
        );

        assertEquals("tech", wineryMachineAssignment.categoryId());
        assertEquals("machines", wineryMachineAssignment.subcategoryId());
        assertEquals("tech", oreMachineAssignment.categoryId());
        assertEquals("machines", oreMachineAssignment.subcategoryId());
        assertEquals("tech", orePartAssignment.categoryId());
        assertEquals("parts", orePartAssignment.subcategoryId());
        assertEquals("magic", experienceAssignment.categoryId());
        assertEquals("reagents", experienceAssignment.subcategoryId());
    }

    @Test
    void foodFamilyPriorsHandlePreparedMealsAndIngredientIntermediates() {
        CategoryAssignment crumbsAssignment = PrimaryCategoryResolver.resolve(
                new ResourceLocation("createfood:bread_crumbs"),
                new FacetProfile(EnumSet.noneOf(ItemFacet.class), Map.of())
        );
        CategoryAssignment puddingAssignment = PrimaryCategoryResolver.resolve(
                new ResourceLocation("createfood:rice_pudding_bowl"),
                new FacetProfile(EnumSet.noneOf(ItemFacet.class), Map.of())
        );
        CategoryAssignment parmesanAssignment = PrimaryCategoryResolver.resolve(
                new ResourceLocation("displaydelight:ctd_eggplant_parmesan"),
                new FacetProfile(EnumSet.of(ItemFacet.PLACEABLE), Map.of())
        );
        CategoryAssignment stickFoodAssignment = PrimaryCategoryResolver.resolve(
                new ResourceLocation("displaydelight:od_plated_baked_tentacle_on_a_stick"),
                new FacetProfile(EnumSet.of(ItemFacet.PLACEABLE), Map.of())
        );
        CategoryAssignment pieAssignment = PrimaryCategoryResolver.resolve(
                new ResourceLocation("bountifulfares:lemon_pie"),
                new FacetProfile(EnumSet.of(ItemFacet.PLACEABLE, ItemFacet.PLACEABLE_FOOD), Map.of(SearchNodeKeys.BLOCKS_MATERIAL, "other_building"))
        );

        assertEquals("ingredients", crumbsAssignment.categoryId());
        assertEquals("organic", crumbsAssignment.subcategoryId());
        assertEquals("nature", puddingAssignment.categoryId());
        assertEquals("meals", puddingAssignment.subcategoryId());
        assertEquals("nature", parmesanAssignment.categoryId());
        assertEquals("meals", parmesanAssignment.subcategoryId());
        assertEquals("nature", stickFoodAssignment.categoryId());
        assertEquals("meals", stickFoodAssignment.subcategoryId());
        assertEquals("nature", pieAssignment.categoryId());
        assertEquals("meals", pieAssignment.subcategoryId());
    }
}
