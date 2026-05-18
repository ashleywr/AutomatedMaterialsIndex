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
                new FacetProfile(EnumSet.of(ItemFacet.EDIBLE, ItemFacet.PLACEABLE, ItemFacet.PLACEABLE_FOOD, ItemFacet.COMPOSTABLE), Map.of())
        );

        assertEquals("nature", assignment.categoryId());
        assertEquals("meals", assignment.subcategoryId());
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
    void interactiveBlocksResolveToTechMachinesBeforeMasonry() {
        CategoryAssignment assignment = PrimaryCategoryResolver.resolve(
                ResourceLocation.parse("examplemod:crafting_terminal"),
                new FacetProfile(EnumSet.of(ItemFacet.PLACEABLE, ItemFacet.INTERACTIVE_BLOCK), Map.of())
        );

        assertEquals("tech", assignment.categoryId());
        assertEquals("machines", assignment.subcategoryId());
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

    @Test
    void createFamilyPriorsBiasAmbiguousBlocksTowardTechAndDecoration() {
        CategoryAssignment machineLikeAssignment = PrimaryCategoryResolver.resolve(
                ResourceLocation.parse("create:andesite_casing"),
                new FacetProfile(
                        EnumSet.of(ItemFacet.PLACEABLE),
                        Map.of(SearchNodeKeys.BLOCKS_MATERIAL, "other_building")
                )
        );
        CategoryAssignment decorativeAssignment = PrimaryCategoryResolver.resolve(
                ResourceLocation.parse("displaydelight:oak_display_board"),
                new FacetProfile(
                        EnumSet.of(ItemFacet.PLACEABLE, ItemFacet.DECORATIVE_BLOCK),
                        Map.of()
                )
        );
        CategoryAssignment jarAssignment = PrimaryCategoryResolver.resolve(
                ResourceLocation.parse("supplementaries:tater_in_a_jar"),
                new FacetProfile(
                        EnumSet.of(ItemFacet.PLACEABLE, ItemFacet.DECORATIVE_BLOCK),
                        Map.of(SearchNodeKeys.OBTAINABILITY, "no_recipe")
                )
        );
        CategoryAssignment partsAssignment = PrimaryCategoryResolver.resolve(
                ResourceLocation.parse("create:belt_connector"),
                new FacetProfile(
                        EnumSet.of(ItemFacet.PLACEABLE),
                        Map.of(SearchNodeKeys.BLOCKS_MATERIAL, "other_building")
                )
        );
        CategoryAssignment machineAssignment = PrimaryCategoryResolver.resolve(
                ResourceLocation.parse("create:mechanical_press"),
                new FacetProfile(
                        EnumSet.of(ItemFacet.PLACEABLE),
                        Map.of(SearchNodeKeys.BLOCKS_MATERIAL, "other_building")
                )
        );
        CategoryAssignment stagedMachineAssignment = PrimaryCategoryResolver.resolve(
                ResourceLocation.parse("create:blaze_burner"),
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
                ResourceLocation.parse("farmersdelight:tomato_crate"),
                new FacetProfile(
                        EnumSet.of(ItemFacet.PLACEABLE),
                        Map.of(SearchNodeKeys.BLOCKS_MATERIAL, "other_building")
                )
        );

        assertEquals("nature", assignment.categoryId());
        assertEquals("snacks", assignment.subcategoryId());
    }

    @Test
    void storageAndDecorFamilyPriorsHandleCommonModpackFamilies() {
        CategoryAssignment ae2ChestAssignment = PrimaryCategoryResolver.resolve(
                ResourceLocation.parse("ae2:chest"),
                new FacetProfile(
                        EnumSet.of(ItemFacet.PLACEABLE),
                        Map.of(SearchNodeKeys.BLOCKS_MATERIAL, "other_building")
                )
        );
        CategoryAssignment backpackAssignment = PrimaryCategoryResolver.resolve(
                ResourceLocation.parse("sophisticatedbackpacks:backpack"),
                new FacetProfile(EnumSet.noneOf(ItemFacet.class), Map.of())
        );
        CategoryAssignment furnitureAssignment = PrimaryCategoryResolver.resolve(
                ResourceLocation.parse("mcwfurnitures:oak_chair"),
                new FacetProfile(
                        EnumSet.of(ItemFacet.PLACEABLE),
                        Map.of(SearchNodeKeys.BLOCKS_MATERIAL, "other_building")
                )
        );
        CategoryAssignment drawerAssignment = PrimaryCategoryResolver.resolve(
                ResourceLocation.parse("storagedrawers:oak_full_drawers_1"),
                new FacetProfile(
                        EnumSet.of(ItemFacet.PLACEABLE),
                        Map.of(SearchNodeKeys.BLOCKS_MATERIAL, "wood")
                )
        );
        CategoryAssignment holidayAssignment = PrimaryCategoryResolver.resolve(
                ResourceLocation.parse("mcwholidays:gingerbread_chair"),
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
                ResourceLocation.parse("ae2:silicon"),
                new FacetProfile(EnumSet.noneOf(ItemFacet.class), Map.of())
        );
        CategoryAssignment printedAssignment = PrimaryCategoryResolver.resolve(
                ResourceLocation.parse("ae2:printed_logic_processor"),
                new FacetProfile(EnumSet.noneOf(ItemFacet.class), Map.of())
        );
        CategoryAssignment processorAssignment = PrimaryCategoryResolver.resolve(
                ResourceLocation.parse("ae2:logic_processor"),
                new FacetProfile(EnumSet.noneOf(ItemFacet.class), Map.of())
        );
        CategoryAssignment partsAssignment = PrimaryCategoryResolver.resolve(
                ResourceLocation.parse("ae2:me_drive"),
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
    }

    @Test
    void refinedStorageAndAutomationFamiliesResolveTechnicalIntermediates() {
        CategoryAssignment rsProcessorAssignment = PrimaryCategoryResolver.resolve(
                ResourceLocation.parse("refinedstorage:raw_basic_processor"),
                new FacetProfile(EnumSet.noneOf(ItemFacet.class), Map.of())
        );
        CategoryAssignment pneumaticCircuitAssignment = PrimaryCategoryResolver.resolve(
                ResourceLocation.parse("pneumaticcraft:printed_circuit_board"),
                new FacetProfile(EnumSet.noneOf(ItemFacet.class), Map.of())
        );
        CategoryAssignment pneumaticPartAssignment = PrimaryCategoryResolver.resolve(
                ResourceLocation.parse("pneumaticcraft:pressure_tube"),
                new FacetProfile(EnumSet.noneOf(ItemFacet.class), Map.of())
        );
        CategoryAssignment pneumaticMachineAssignment = PrimaryCategoryResolver.resolve(
                ResourceLocation.parse("pneumaticcraft:charging_station"),
                new FacetProfile(EnumSet.of(ItemFacet.PLACEABLE), Map.of())
        );

        assertEquals("tech", rsProcessorAssignment.categoryId());
        assertEquals("circuits", rsProcessorAssignment.subcategoryId());
        assertEquals("tech", pneumaticCircuitAssignment.categoryId());
        assertEquals("circuits", pneumaticCircuitAssignment.subcategoryId());
        assertEquals("tech", pneumaticPartAssignment.categoryId());
        assertEquals("parts", pneumaticPartAssignment.subcategoryId());
        assertEquals("tech", pneumaticMachineAssignment.categoryId());
        assertEquals("machines", pneumaticMachineAssignment.subcategoryId());
    }

    @Test
    void createFamilyHandheldsResolveOutOfMagic() {
        CategoryAssignment wrenchAssignment = PrimaryCategoryResolver.resolve(
                ResourceLocation.parse("create:wrench"),
                new FacetProfile(EnumSet.noneOf(ItemFacet.class), Map.of())
        );
        CategoryAssignment cannonAssignment = PrimaryCategoryResolver.resolve(
                ResourceLocation.parse("create:potato_cannon"),
                new FacetProfile(EnumSet.noneOf(ItemFacet.class), Map.of())
        );
        CategoryAssignment filterAssignment = PrimaryCategoryResolver.resolve(
                ResourceLocation.parse("create:attribute_filter"),
                new FacetProfile(EnumSet.noneOf(ItemFacet.class), Map.of())
        );
        CategoryAssignment schematicAssignment = PrimaryCategoryResolver.resolve(
                ResourceLocation.parse("create:schematic_and_quill"),
                new FacetProfile(EnumSet.noneOf(ItemFacet.class), Map.of())
        );
        CategoryAssignment symmetryAssignment = PrimaryCategoryResolver.resolve(
                ResourceLocation.parse("create:wand_of_symmetry"),
                new FacetProfile(EnumSet.of(ItemFacet.MAGIC_ARTIFACT), Map.of())
        );

        assertEquals("tools", wrenchAssignment.categoryId());
        assertEquals("utility", wrenchAssignment.subcategoryId());
        assertEquals("tools", cannonAssignment.categoryId());
        assertEquals("ranged", cannonAssignment.subcategoryId());
        assertEquals("utility", filterAssignment.categoryId());
        assertEquals("misc", filterAssignment.subcategoryId());
        assertEquals("utility", schematicAssignment.categoryId());
        assertEquals("misc", schematicAssignment.subcategoryId());
        assertEquals("tools", symmetryAssignment.categoryId());
        assertEquals("utility", symmetryAssignment.subcategoryId());
    }

    @Test
    void createAddonsBiasTransportAndPowerFamilies() {
        CategoryAssignment trackAssignment = PrimaryCategoryResolver.resolve(
                ResourceLocation.parse("railways:redstone_track"),
                new FacetProfile(
                        EnumSet.of(ItemFacet.PLACEABLE),
                        Map.of(SearchNodeKeys.BLOCKS_MATERIAL, "other_building")
                )
        );
        CategoryAssignment handcarAssignment = PrimaryCategoryResolver.resolve(
                ResourceLocation.parse("railways:handcar"),
                new FacetProfile(
                        EnumSet.of(ItemFacet.PLACEABLE),
                        Map.of(SearchNodeKeys.BLOCKS_MATERIAL, "other_building")
                )
        );
        CategoryAssignment connectorAssignment = PrimaryCategoryResolver.resolve(
                ResourceLocation.parse("railways:oak_storage_connector"),
                new FacetProfile(
                        EnumSet.of(ItemFacet.PLACEABLE),
                        Map.of(SearchNodeKeys.BLOCKS_MATERIAL, "wood")
                )
        );
        CategoryAssignment generatorAssignment = PrimaryCategoryResolver.resolve(
                ResourceLocation.parse("createaddition:alternator"),
                new FacetProfile(
                        EnumSet.of(ItemFacet.PLACEABLE),
                        Map.of(SearchNodeKeys.BLOCKS_MATERIAL, "other_building")
                )
        );
        CategoryAssignment wireAssignment = PrimaryCategoryResolver.resolve(
                ResourceLocation.parse("createaddition:copper_wire"),
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
        assertEquals("parts", wireAssignment.subcategoryId());
    }

    @Test
    void createAddonDomainPriorsHandleWineryEnchantingAndOreExtraction() {
        CategoryAssignment wineryMachineAssignment = PrimaryCategoryResolver.resolve(
                ResourceLocation.parse("create_winery:fermentation_vat"),
                new FacetProfile(
                        EnumSet.of(ItemFacet.PLACEABLE),
                        Map.of(SearchNodeKeys.BLOCKS_MATERIAL, "other_building")
                )
        );
        CategoryAssignment oreMachineAssignment = PrimaryCategoryResolver.resolve(
                ResourceLocation.parse("createoreexcavation:ore_drill"),
                new FacetProfile(
                        EnumSet.of(ItemFacet.PLACEABLE),
                        Map.of(SearchNodeKeys.BLOCKS_MATERIAL, "other_building")
                )
        );
        CategoryAssignment orePartAssignment = PrimaryCategoryResolver.resolve(
                ResourceLocation.parse("createoreexcavation:vein_finder_core"),
                new FacetProfile(
                        EnumSet.of(ItemFacet.PLACEABLE),
                        Map.of(SearchNodeKeys.BLOCKS_MATERIAL, "other_building")
                )
        );
        CategoryAssignment experienceAssignment = PrimaryCategoryResolver.resolve(
                ResourceLocation.parse("create_enchantment_industry:nugget_of_super_experience"),
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
                ResourceLocation.parse("createfood:bread_crumbs"),
                new FacetProfile(EnumSet.noneOf(ItemFacet.class), Map.of())
        );
        CategoryAssignment puddingAssignment = PrimaryCategoryResolver.resolve(
                ResourceLocation.parse("createfood:rice_pudding_bowl"),
                new FacetProfile(EnumSet.noneOf(ItemFacet.class), Map.of())
        );
        CategoryAssignment parmesanAssignment = PrimaryCategoryResolver.resolve(
                ResourceLocation.parse("displaydelight:ctd_eggplant_parmesan"),
                new FacetProfile(EnumSet.of(ItemFacet.PLACEABLE), Map.of())
        );
        CategoryAssignment stickFoodAssignment = PrimaryCategoryResolver.resolve(
                ResourceLocation.parse("displaydelight:od_plated_baked_tentacle_on_a_stick"),
                new FacetProfile(EnumSet.of(ItemFacet.PLACEABLE), Map.of())
        );
        CategoryAssignment pieAssignment = PrimaryCategoryResolver.resolve(
                ResourceLocation.parse("bountifulfares:lemon_pie"),
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
