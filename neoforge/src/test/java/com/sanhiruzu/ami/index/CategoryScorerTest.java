package com.sanhiruzu.ami.index;

import net.minecraft.resources.ResourceLocation;
import org.junit.jupiter.api.Test;

import java.util.EnumSet;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CategoryScorerTest {
    @Test
    void componentFactsOutweighGenericPlaceableEvidence() {
        CategoryAssignment assignment = PrimaryCategoryResolver.resolve(
                new ResourceLocation("ami_test:apple_pie_block"),
                new FacetProfile(
                        EnumSet.of(ItemFacet.PLACEABLE),
                        Map.of(SearchNodeKeys.COMPONENT_FACTS, "food")
                )
        );

        assertEquals("food", assignment.categoryId());
        assertEquals("prepared", assignment.subcategoryId());
        assertEquals("hard_identity", assignment.attributes().get("classificationMode"));
        assertTrue(assignment.attributes().getOrDefault("classificationEvidence", "").contains("identity.food"));
    }

    @Test
    void creativeTabEvidenceCanRescueFurnitureFromGenericPlaceable() {
        CategoryAssignment assignment = PrimaryCategoryResolver.resolve(
                new ResourceLocation("ami_test:oak_toilet"),
                new FacetProfile(
                        EnumSet.of(ItemFacet.PLACEABLE),
                        Map.of(SearchNodeKeys.CREATIVE_TAB_LABEL, "Macaw's Furniture")
                )
        );

        assertEquals("decoration", assignment.categoryId());
        assertEquals("furniture", assignment.subcategoryId());
        assertTrue(assignment.attributes().getOrDefault("classificationEvidence", "").contains("creative_tab.decoration"));
    }

    @Test
    void trustedTagsBeatWeakLexicalNoise() {
        CategoryAssignment assignment = PrimaryCategoryResolver.resolve(
                new ResourceLocation("railways:track_incomplete_jungle_narrow"),
                new FacetProfile(
                        EnumSet.of(ItemFacet.PLACEABLE),
                        Map.of(SearchNodeKeys.TAGS, "create:tracks")
                )
        );

        assertEquals("tech", assignment.categoryId());
        assertEquals("transport", assignment.subcategoryId());
        assertTrue(assignment.attributes().getOrDefault("classificationEvidence", "").contains("tag.rails"));
    }

    @Test
    void pathOnlyCableWordsClassifyThroughLexicalEvidenceNotFacets() {
        CategoryAssignment assignment = PrimaryCategoryResolver.resolve(
                new ResourceLocation("immersiveengineering:wire_copper"),
                new FacetProfile(EnumSet.noneOf(ItemFacet.class), Map.of())
        );

        assertEquals("tech", assignment.categoryId());
        assertEquals("cables", assignment.subcategoryId());
        assertTrue(assignment.attributes().getOrDefault("classificationEvidence", "").contains("tech_cables"));
    }

    @Test
    void foodDrinkFacetBeatsGenericBottleContainerEvidence() {
        CategoryAssignment assignment = PrimaryCategoryResolver.resolve(
                new ResourceLocation("farmersdelight:milk_bottle"),
                new FacetProfile(
                        EnumSet.of(ItemFacet.FOOD_DRINK, ItemFacet.UTILITY_MISC),
                        Map.of(SearchNodeKeys.ITEM_CLASS, "vectorwing.farmersdelight.common.item.MilkBottleItem")
                )
        );

        assertEquals("food", assignment.categoryId());
        assertEquals("prepared", assignment.subcategoryId());
        assertTrue(!assignment.attributes().getOrDefault("classificationEvidence", "").contains("utility_container"));
        assertTrue(!assignment.attributes().getOrDefault("classificationEvidence", "").contains("class.bottle_container"));
    }

    @Test
    void magicBottleFacetBeatsGenericBottleContainerEvidence() {
        CategoryAssignment assignment = PrimaryCategoryResolver.resolve(
                new ResourceLocation("minecraft:experience_bottle"),
                new FacetProfile(
                        EnumSet.of(ItemFacet.MAGIC_REAGENT, ItemFacet.UTILITY_MISC),
                        Map.of(SearchNodeKeys.ITEM_CLASS, "net.minecraft.world.item.ExperienceBottleItem")
                )
        );

        assertEquals("magic", assignment.categoryId());
        assertEquals("reagents", assignment.subcategoryId());
    }

    @Test
    void plainBottleStillRoutesToUtilityContainer() {
        CategoryAssignment assignment = PrimaryCategoryResolver.resolve(
                new ResourceLocation("minecraft:glass_bottle"),
                new FacetProfile(
                        EnumSet.of(ItemFacet.UTILITY_MISC, ItemFacet.FLUID_CONTAINER),
                        Map.of(SearchNodeKeys.ITEM_CLASS, "net.minecraft.world.item.BottleItem")
                )
        );

        assertEquals("utility", assignment.categoryId());
        assertEquals("misc", assignment.subcategoryId());
        assertTrue(assignment.attributes().getOrDefault("classificationEvidence", "").contains("class.bottle_container"));
    }

    @Test
    void equipmentArmorEvidenceBeatsStaleUtilityToolFacet() {
        CategoryAssignment assignment = PrimaryCategoryResolver.resolve(
                new ResourceLocation("ami_test:copper_chestplate"),
                new FacetProfile(
                        EnumSet.of(ItemFacet.EQUIPPABLE, ItemFacet.UTILITY_TOOL, ItemFacet.ARMOR_CHEST),
                        Map.of(SearchNodeKeys.EQUIPMENT_SLOT, "chest")
                )
        );

        assertEquals("armor", assignment.categoryId());
        assertEquals("chest", assignment.subcategoryId());
        assertEquals("hard_identity", assignment.attributes().get("classificationMode"));
        assertTrue(assignment.attributes().getOrDefault("classificationEvidence", "").contains("identity.armor"));
        assertTrue(!assignment.attributes().getOrDefault("classificationScores", "").contains("tools/utility"));
    }

    @Test
    void elytraWingsDoNotBecomeProteinFromWingToken() {
        CategoryAssignment assignment = PrimaryCategoryResolver.resolve(
                new ResourceLocation("silentgear:elytra_wings"),
                new FacetProfile(EnumSet.noneOf(ItemFacet.class), Map.of(SearchNodeKeys.COMPONENT_FACTS, "max_damage,damage"))
        );

        assertEquals("armor", assignment.categoryId());
        assertTrue(!assignment.attributes().getOrDefault("classificationScores", "").contains("food/ingredients"));
    }

    @Test
    void passiveComparatorFurnitureDoesNotTieRedstone() {
        CategoryAssignment assignment = PrimaryCategoryResolver.resolve(
                new ResourceLocation("farmersdelight:acacia_cabinet"),
                new FacetProfile(
                        EnumSet.of(
                                ItemFacet.PLACEABLE,
                                ItemFacet.HAS_BLOCK_ENTITY,
                                ItemFacet.PASSIVE_COMPARATOR_OUTPUT,
                                ItemFacet.REDSTONE_SIGNAL
                        ),
                        Map.of()
                )
        );

        assertEquals("decoration", assignment.categoryId());
        assertEquals("furniture", assignment.subcategoryId());
        assertTrue(assignment.attributes().getOrDefault("classificationEvidence", "").contains("decorative_furniture"));
        assertTrue(assignment.attributes().getOrDefault("classificationScores", "").contains("tech/redstone=25"));
    }

    @Test
    void coffeeTablesDoNotBecomeDrinks() {
        CategoryAssignment assignment = PrimaryCategoryResolver.resolve(
                new ResourceLocation("mcwquark:ancient_coffee_table"),
                new FacetProfile(EnumSet.of(ItemFacet.PLACEABLE), Map.of())
        );

        assertEquals("decoration", assignment.categoryId());
        assertEquals("furniture", assignment.subcategoryId());
        assertTrue(!assignment.attributes().getOrDefault("classificationScores", "").contains("food/prepared"));
    }

    @Test
    void activeRedstoneBeatsFurnitureWord() {
        CategoryAssignment assignment = PrimaryCategoryResolver.resolve(
                new ResourceLocation("create:desk_bell"),
                new FacetProfile(
                        EnumSet.of(ItemFacet.PLACEABLE, ItemFacet.ACTIVE_REDSTONE_LOGIC, ItemFacet.REDSTONE_LOGIC),
                        Map.of()
                )
        );

        assertEquals("tech", assignment.categoryId());
        assertEquals("redstone", assignment.subcategoryId());
    }

    @Test
    void arrowTagsBeatLightingWords() {
        CategoryAssignment assignment = PrimaryCategoryResolver.resolve(
                new ResourceLocation("quark:torch_arrow"),
                new FacetProfile(
                        EnumSet.of(ItemFacet.RANGED_WEAPON, ItemFacet.PROJECTILE),
                        Map.of(SearchNodeKeys.TAGS, "minecraft:arrows")
                )
        );

        assertEquals("tools", assignment.categoryId());
        assertEquals("ammo", assignment.subcategoryId());
    }

    @Test
    void foodBlockClassBeatsInkDyeWord() {
        CategoryAssignment assignment = PrimaryCategoryResolver.resolve(
                new ResourceLocation("displaydelight:squid_ink_pasta"),
                new FacetProfile(
                        EnumSet.of(ItemFacet.PLACEABLE),
                        Map.of(
                                SearchNodeKeys.ITEM_CLASS, "com.jkvin114.displaydelight.item.FoodBlockItem",
                                SearchNodeKeys.BLOCK_CLASS, "com.jkvin114.displaydelight.block.WideFoodBlock"
                        )
                )
        );

        assertEquals("food", assignment.categoryId());
        assertEquals("prepared", assignment.subcategoryId());
    }

    @Test
    void techBlockClassBeatsFullBlockFallback() {
        CategoryAssignment assignment = PrimaryCategoryResolver.resolve(
                new ResourceLocation("create:basin"),
                new FacetProfile(
                        EnumSet.of(ItemFacet.PLACEABLE, ItemFacet.HAS_BLOCK_ENTITY, ItemFacet.PASSIVE_COMPARATOR_OUTPUT),
                        Map.of(SearchNodeKeys.BLOCK_CLASS, "com.simibubi.create.content.processing.basin.BasinBlock")
                )
        );

        assertEquals("tech", assignment.categoryId());
        assertEquals("machines", assignment.subcategoryId());
    }

    @Test
    void trellisClassBeatsFullBlockFallback() {
        CategoryAssignment assignment = PrimaryCategoryResolver.resolve(
                new ResourceLocation("natures_spirit:aspen_trellis"),
                new FacetProfile(
                        EnumSet.of(ItemFacet.PLACEABLE),
                        Map.of(SearchNodeKeys.BLOCK_CLASS, "net.hecco.bountifulfares.definition.block.custom.TrellisBlock")
                )
        );

        assertEquals("nature", assignment.categoryId());
        assertEquals("flora", assignment.subcategoryId());
    }

    @Test
    void toolboxTokenBeatsGenericPlaceableFallback() {
        CategoryAssignment assignment = PrimaryCategoryResolver.resolve(
                new ResourceLocation("create:black_toolbox"),
                new FacetProfile(
                        EnumSet.of(ItemFacet.PLACEABLE, ItemFacet.HAS_BLOCK_ENTITY, ItemFacet.PASSIVE_COMPARATOR_OUTPUT),
                        Map.of(SearchNodeKeys.TAGS, "create:toolboxes")
                )
        );

        assertEquals("tools", assignment.categoryId());
        assertEquals("utility", assignment.subcategoryId());
    }

    @Test
    void scienceCreativeTabRescuesCrystalSeedsFromMisc() {
        CategoryAssignment assignment = PrimaryCategoryResolver.resolve(
                new ResourceLocation("ae2cs:fluix_crystal_seed"),
                new FacetProfile(
                        EnumSet.noneOf(ItemFacet.class),
                        Map.of(SearchNodeKeys.CREATIVE_TAB_LABEL, "AE2: Crystal Science")
                )
        );

        assertEquals("tech", assignment.categoryId());
        assertEquals("parts", assignment.subcategoryId());
    }

    @Test
    void utilityCreativeTabRescuesSeedPouchFromSeedNoise() {
        CategoryAssignment assignment = PrimaryCategoryResolver.resolve(
                new ResourceLocation("quark:seed_pouch"),
                new FacetProfile(
                        EnumSet.noneOf(ItemFacet.class),
                        Map.of(SearchNodeKeys.CREATIVE_TAB_LABEL, "Tools & Utilities")
                )
        );

        assertEquals("utility", assignment.categoryId());
        assertEquals("misc", assignment.subcategoryId());
    }

    @Test
    void commonMaterialTagsClassifyAsIngredientsNotTech() {
        CategoryAssignment assignment = PrimaryCategoryResolver.resolve(
                new ResourceLocation("minecraft:amethyst_shard"),
                new FacetProfile(
                        EnumSet.of(ItemFacet.MAGIC_REAGENT),
                        Map.of(SearchNodeKeys.TAGS, "c:gems,c:gems/amethyst")
                )
        );

        assertEquals("ingredients", assignment.categoryId());
        assertEquals("mineral", assignment.subcategoryId());
    }

    @Test
    void ingredientsTabShardEvidenceBeatsStaleMagicFacet() {
        CategoryAssignment assignment = PrimaryCategoryResolver.resolve(
                new ResourceLocation("quark:magenta_shard"),
                new FacetProfile(
                        EnumSet.of(ItemFacet.MAGIC_REAGENT),
                        Map.of(
                                SearchNodeKeys.TAGS, "quark:shards",
                                SearchNodeKeys.CREATIVE_TAB_ID, "minecraft:ingredients",
                                SearchNodeKeys.CREATIVE_TAB_LABEL, "Ingredients",
                                SearchNodeKeys.RECIPE_USE_CATEGORIES, "crafting"
                        )
                )
        );

        assertEquals("ingredients", assignment.categoryId());
        assertEquals("mineral", assignment.subcategoryId());
        assertTrue(assignment.attributes().getOrDefault("classificationEvidence", "").contains("mineral_ingredient"));
        assertTrue(assignment.attributes().getOrDefault("classificationEvidence", "").contains("creative_tab.ingredients"));
    }

    @Test
    void fenceGateFacetBeatsFenceAndWallTie() {
        CategoryAssignment assignment = PrimaryCategoryResolver.resolve(
                new ResourceLocation("mcwfences:acacia_curved_gate"),
                new FacetProfile(EnumSet.of(ItemFacet.PLACEABLE, ItemFacet.FENCE_GATE, ItemFacet.WALL, ItemFacet.FENCE), Map.of())
        );

        assertEquals("masonry", assignment.categoryId());
        assertEquals("functional", assignment.subcategoryId());
    }

    @Test
    void wallNameBreaksFenceWallTie() {
        CategoryAssignment assignment = PrimaryCategoryResolver.resolve(
                new ResourceLocation("mcwfences:andesite_grass_topped_wall"),
                new FacetProfile(EnumSet.of(ItemFacet.PLACEABLE, ItemFacet.WALL, ItemFacet.FENCE), Map.of())
        );

        assertEquals("masonry", assignment.categoryId());
        assertEquals("wall", assignment.subcategoryId());
    }

    @Test
    void exactShapeFacetBeatsDecorativeTabTie() {
        CategoryAssignment assignment = PrimaryCategoryResolver.resolve(
                new ResourceLocation("copycats:copycat_fence"),
                new FacetProfile(
                        EnumSet.of(ItemFacet.PLACEABLE, ItemFacet.HAS_BLOCK_ENTITY, ItemFacet.DECORATIVE_BLOCK, ItemFacet.FENCE),
                        Map.of(SearchNodeKeys.CREATIVE_TAB_LABEL, "Create: Copycats+ | Decorative")
                )
        );

        assertEquals("masonry", assignment.categoryId());
        assertEquals("fence", assignment.subcategoryId());
    }

    @Test
    void createMachineClassBeatsFullBlockFallback() {
        CategoryAssignment assignment = PrimaryCategoryResolver.resolve(
                new ResourceLocation("create_power_loader:brass_chunk_loader"),
                new FacetProfile(
                        EnumSet.of(ItemFacet.PLACEABLE, ItemFacet.HAS_BLOCK_ENTITY, ItemFacet.PASSIVE_COMPARATOR_OUTPUT),
                        Map.of(SearchNodeKeys.BLOCK_CLASS, "com.hlysine.create_power_loader.content.brasschunkloader.BrassChunkLoaderBlock")
                )
        );

        assertEquals("tech", assignment.categoryId());
        assertEquals("machines", assignment.subcategoryId());
    }

    @Test
    void rollTokenClassifiesFoodDisplayBlocksAsMeals() {
        CategoryAssignment assignment = PrimaryCategoryResolver.resolve(
                new ResourceLocation("createfood:cinnamon_sweet_roll_base_plate_block"),
                new FacetProfile(EnumSet.of(ItemFacet.PLACEABLE, ItemFacet.PASSIVE_COMPARATOR_OUTPUT), Map.of())
        );

        assertEquals("food", assignment.categoryId());
        assertEquals("prepared", assignment.subcategoryId());
    }

    @Test
    void compressedMaterialBlockClassBeatsFullBlockFallback() {
        CategoryAssignment assignment = PrimaryCategoryResolver.resolve(
                new ResourceLocation("pneumaticcraft:compressed_iron_block"),
                new FacetProfile(
                        EnumSet.of(ItemFacet.PLACEABLE, ItemFacet.HAS_BLOCK_ENTITY, ItemFacet.PASSIVE_COMPARATOR_OUTPUT),
                        Map.of(SearchNodeKeys.BLOCK_CLASS, "me.desht.pneumaticcraft.common.block.CompressedIronBlock")
                )
        );

        assertEquals("ingredients", assignment.categoryId());
        assertEquals("mineral", assignment.subcategoryId());
    }

    @Test
    void ae2EnergyClassBeatsFullBlockFallback() {
        CategoryAssignment assignment = PrimaryCategoryResolver.resolve(
                new ResourceLocation("ae2:energy_acceptor"),
                new FacetProfile(
                        EnumSet.of(ItemFacet.PLACEABLE, ItemFacet.HAS_BLOCK_ENTITY, ItemFacet.PASSIVE_COMPARATOR_OUTPUT),
                        Map.of(SearchNodeKeys.BLOCK_CLASS, "appeng.block.networking.EnergyAcceptorBlock")
                )
        );

        assertEquals("tech", assignment.categoryId());
        assertEquals("machines", assignment.subcategoryId());
    }

    @Test
    void dishTokenBeatsFullBlockFallback() {
        CategoryAssignment assignment = PrimaryCategoryResolver.resolve(
                new ResourceLocation("bountifulfares:ceramic_dish"),
                new FacetProfile(EnumSet.of(ItemFacet.PLACEABLE), Map.of())
        );

        assertEquals("utility", assignment.categoryId());
        assertEquals("misc", assignment.subcategoryId());
    }

    @Test
    void armorIdentityBeatsOrganicIngredientEvidence() {
        CategoryAssignment assignment = PrimaryCategoryResolver.resolve(
                new ResourceLocation("minecraft:leather_helmet"),
                new FacetProfile(
                        EnumSet.of(ItemFacet.ARMOR_HEAD, ItemFacet.INGREDIENT_ORGANIC),
                        Map.of(SearchNodeKeys.EQUIPMENT_SLOT, "head")
                )
        );

        assertEquals("armor", assignment.categoryId());
        assertEquals("head", assignment.subcategoryId());
        assertEquals("hard_identity", assignment.attributes().get("classificationMode"));
    }

    @Test
    void harvestToolIdentityBeatsStorageAndRedstoneNoise() {
        CategoryAssignment assignment = PrimaryCategoryResolver.resolve(
                new ResourceLocation("ami_test:compressed_pickaxe"),
                new FacetProfile(EnumSet.of(
                        ItemFacet.HARVEST_TOOL,
                        ItemFacet.STORAGE,
                        ItemFacet.PASSIVE_COMPARATOR_OUTPUT,
                        ItemFacet.REDSTONE_SIGNAL
                ), Map.of())
        );

        assertEquals("tools", assignment.categoryId());
        assertEquals("harvest", assignment.subcategoryId());
        assertEquals("hard_identity", assignment.attributes().get("classificationMode"));
    }

    @Test
    void projectileFacetRequiresAmmoContext() {
        CategoryAssignment assignment = PrimaryCategoryResolver.resolve(
                new ResourceLocation("unusualfishmod:clement_shell"),
                new FacetProfile(EnumSet.of(ItemFacet.PROJECTILE), Map.of())
        );

        assertEquals("ingredients", assignment.categoryId());
        assertEquals("organic", assignment.subcategoryId());
        assertTrue(!assignment.attributes().getOrDefault("classificationScores", "").contains("tools/ammo"));
    }

    @Test
    void mortarShellStillClassifiesAsAmmo() {
        CategoryAssignment assignment = PrimaryCategoryResolver.resolve(
                new ResourceLocation("createbigcannons:drop_mortar_shell"),
                new FacetProfile(
                        EnumSet.of(ItemFacet.PROJECTILE, ItemFacet.PLACEABLE),
                        Map.of(SearchNodeKeys.TAGS, "createbigcannons:big_cannon_projectiles")
                )
        );

        assertEquals("tools", assignment.categoryId());
        assertEquals("ammo", assignment.subcategoryId());
    }

    @Test
    void foodDisplayPlatesBeatIngredientTokens() {
        CategoryAssignment assignment = PrimaryCategoryResolver.resolve(
                new ResourceLocation("createfood:fish_taco_kelp_plate_block"),
                new FacetProfile(
                        EnumSet.of(ItemFacet.PLACEABLE, ItemFacet.PASSIVE_COMPARATOR_OUTPUT, ItemFacet.REDSTONE_SIGNAL),
                        Map.of(
                                SearchNodeKeys.BLOCK_CLASS, "dev.averageanime.block.type.plate.PlateBlock",
                                SearchNodeKeys.CREATIVE_TAB_LABEL, "Create: Food - Display"
                        )
                )
        );

        assertEquals("food", assignment.categoryId());
        assertEquals("prepared", assignment.subcategoryId());
        assertTrue(assignment.attributes().getOrDefault("classificationEvidence", "").contains("class.food_plate"));
    }

    @Test
    void foodChipNamesDoNotBecomeCircuits() {
        CategoryAssignment assignment = PrimaryCategoryResolver.resolve(
                new ResourceLocation("createfood:potato_chip_bowl_block"),
                new FacetProfile(
                        EnumSet.of(ItemFacet.PLACEABLE),
                        Map.of(SearchNodeKeys.BLOCK_CLASS, "dev.averageanime.block.type.display.BowlFoodBlock")
                )
        );

        assertEquals("food", assignment.categoryId());
        assertEquals("prepared", assignment.subcategoryId());
        assertTrue(!assignment.attributes().getOrDefault("classificationScores", "").contains("tech/circuits"));
    }

    @Test
    void foodBlockClassBeatsTemplateFacetNoise() {
        CategoryAssignment assignment = PrimaryCategoryResolver.resolve(
                new ResourceLocation("displaydelight:warped_moldy_meat"),
                new FacetProfile(
                        EnumSet.of(ItemFacet.PLACEABLE, ItemFacet.TEMPLATE),
                        Map.of(
                                SearchNodeKeys.ITEM_CLASS, "com.jkvin114.displaydelight.item.FoodBlockItem",
                                SearchNodeKeys.BLOCK_CLASS, "com.jkvin114.displaydelight.block.WideFoodBlock"
                        )
                )
        );

        assertEquals("food", assignment.categoryId());
        assertEquals("prepared", assignment.subcategoryId());
        assertTrue(assignment.attributes().getOrDefault("classificationScores", "").contains("food/prepared=110"));
    }

    @Test
    void rollTablesDoNotBecomeMeals() {
        CategoryAssignment assignment = PrimaryCategoryResolver.resolve(
                new ResourceLocation("dndesires:roll_table"),
                new FacetProfile(EnumSet.of(ItemFacet.PLACEABLE), Map.of())
        );

        assertEquals("decoration", assignment.categoryId());
        assertEquals("furniture", assignment.subcategoryId());
        assertTrue(!assignment.attributes().getOrDefault("classificationScores", "").contains("food/prepared"));
    }

    @Test
    void carpetWithAnimalEquipmentNoiseStaysDecoration() {
        CategoryAssignment assignment = PrimaryCategoryResolver.resolve(
                new ResourceLocation("minecraft:red_carpet"),
                new FacetProfile(
                        EnumSet.of(ItemFacet.PLACEABLE, ItemFacet.DECORATIVE_BLOCK, ItemFacet.EQUIPPABLE, ItemFacet.ARMOR_ANIMAL),
                        Map.of(SearchNodeKeys.EQUIPMENT_SLOT, "body")
                )
        );

        assertEquals("decoration", assignment.categoryId());
        assertEquals("textiles", assignment.subcategoryId());
        assertEquals("hard_identity", assignment.attributes().get("classificationMode"));
    }
}
