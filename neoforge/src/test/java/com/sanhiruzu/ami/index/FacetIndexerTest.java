package com.sanhiruzu.ami.index;

import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.*;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.Property;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class FacetIndexerTest {

    private static FacetProfile index(Item item) {
        return FacetIndexer.index(
                item,
                BuiltInRegistries.ITEM.getKey(item),
                new ItemStack(item)
        );
    }

    private static Item register(String path, Item item) {
        BuiltInRegistries.itemRegistry().register(new ResourceLocation("ami_test", path), item);
        return item;
    }

    @Test
    void appleHasEdibleAndCompostable() {
        FacetProfile profile = index(Items.APPLE);

        assertTrue(profile.facets().contains(ItemFacet.EDIBLE));
        assertTrue(profile.facets().contains(ItemFacet.COMPOSTABLE));
    }

    @Test
    void cakeHasEdiblePlaceableAndCompostable() {
        FacetProfile profile = index(Items.CAKE);

        assertTrue(profile.facets().contains(ItemFacet.EDIBLE));
        assertTrue(profile.facets().contains(ItemFacet.PLACEABLE_FOOD));
        assertTrue(profile.facets().contains(ItemFacet.PLACEABLE));
        assertTrue(profile.facets().contains(ItemFacet.COMPOSTABLE));
    }

    @Test
    void drinkAnimationAndBroadFoodTagsProduceFoodSubfacets() {
        Item drink = register("plain_drink", new DrinkItem("Plain Drink")
                .withComponent(DataComponents.FOOD));
        Item potionLike = register("potion_like", new Item("Potion Like")
                .withComponent(DataComponents.POTION_CONTENTS));
        Item containerLike = register("container_like", new Item("Container Like")
                .withComponent(DataComponents.CONTAINER));
        Item toolComponentLike = register("component_tool_like", new Item("Component Tool Like")
                .withComponent(DataComponents.TOOL)
                .withComponent(DataComponents.MAX_DAMAGE)
                .withComponent(DataComponents.DAMAGE));
        Item cookedMeat = register("cooked_duck", new Item("Cooked Duck")
                .withComponent(DataComponents.FOOD)
                .withTag(TagKey.create(null, new ResourceLocation("c", "cooked_meat"))));
        Item vegetable = register("canned_carrot", new Item("Canned Carrot")
                .withComponent(DataComponents.FOOD)
                .withTag(TagKey.create(null, new ResourceLocation("diet", "vegetables"))));
        Item meal = register("fried_rice", new Item("Fried Rice")
                .withComponent(DataComponents.FOOD)
                .withTag(TagKey.create(null, new ResourceLocation("farmersdelight", "meals"))));

        assertTrue(index(drink).facets().contains(ItemFacet.FOOD_DRINK));
        assertTrue(index(drink).attributes().getOrDefault(SearchNodeKeys.COMPONENT_FACTS, "").contains("food"));
        assertTrue(index(potionLike).facets().contains(ItemFacet.POTION));
        assertTrue(index(potionLike).attributes().getOrDefault(SearchNodeKeys.COMPONENT_FACTS, "").contains("potion_contents"));
        assertTrue(index(containerLike).facets().contains(ItemFacet.STORAGE));
        assertTrue(index(containerLike).attributes().getOrDefault(SearchNodeKeys.COMPONENT_FACTS, "").contains("container"));
        assertTrue(index(toolComponentLike).facets().contains(ItemFacet.UTILITY_TOOL));
        assertTrue(index(toolComponentLike).attributes().getOrDefault(SearchNodeKeys.COMPONENT_FACTS, "").contains("tool"));
        assertTrue(index(cookedMeat).facets().contains(ItemFacet.FOOD_MEAL));
        assertTrue(index(cookedMeat).facets().contains(ItemFacet.FOOD_PROTEIN));
        assertTrue(index(vegetable).facets().contains(ItemFacet.CROP));
        assertTrue(index(meal).facets().contains(ItemFacet.FOOD_MEAL));
    }

    @Test
    void reactiveStyleClassesTagsAndStatePropertiesProduceConcreteFacets() {
        Item powerBottle = register("reactive_power_bottle", new PowerBottleItem(
                "Reactive Power Bottle",
                new PowerBottleBlock(new BlockState().withProperty(new Property<>("bottles")))
        ).withTag(TagKey.create(null, new ResourceLocation("reactive", "power_bottles"))));
        Item crucible = register("reactive_crucible", new BlockItem(
                "Reactive Crucible",
                new CrucibleBlock(new BlockState())
        ));
        Item poweredBlock = register("reactive_powered_state", new BlockItem(
                "Reactive Powered State",
                new Block(new BlockState().withProperty(new Property<>("powered")))
        ));

        FacetProfile bottleProfile = index(powerBottle);
        FacetProfile crucibleProfile = index(crucible);
        FacetProfile poweredProfile = index(poweredBlock);

        assertTrue(bottleProfile.facets().contains(ItemFacet.MAGIC_ARTIFACT));
        assertTrue(bottleProfile.facets().contains(ItemFacet.UTILITY_MISC));
        assertTrue(crucibleProfile.facets().contains(ItemFacet.HAS_BLOCK_ENTITY));
        assertTrue(crucibleProfile.facets().contains(ItemFacet.WORKSTATION));
        assertTrue(crucibleProfile.facets().contains(ItemFacet.MACHINE));
        assertTrue(poweredProfile.facets().contains(ItemFacet.REDSTONE_LOGIC));
        assertTrue(poweredProfile.facets().contains(ItemFacet.REDSTONE_SIGNAL));
    }

    @Test
    void redstoneDustHasRedstoneFacet() {
        FacetProfile profile = index(Items.REDSTONE);

        assertTrue(profile.facets().contains(ItemFacet.REDSTONE_LOGIC));
        assertTrue(profile.facets().contains(ItemFacet.ACTIVE_REDSTONE_LOGIC));
        assertTrue(profile.facets().contains(ItemFacet.REDSTONE_SIGNAL));
    }

    @Test
    void railHasPlaceableRailAndTransportFacts() {
        FacetProfile profile = index(Items.RAIL);

        assertTrue(profile.facets().contains(ItemFacet.PLACEABLE));
        assertTrue(profile.facets().contains(ItemFacet.RAIL));
        assertTrue(profile.facets().contains(ItemFacet.TRANSPORT));
        assertTrue(profile.facets().contains(ItemFacet.REDSTONE_LOGIC));
        assertEquals("rail", profile.attributes().get("blockShape"));
    }

    @Test
    void reinforcedDeepslateHasPlaceableAndStoneFacts() {
        FacetProfile profile = index(Items.REINFORCED_DEEPSLATE);

        assertTrue(profile.facets().contains(ItemFacet.PLACEABLE));
        assertTrue(profile.facets().contains(ItemFacet.STONE_BLOCK));
        assertEquals("stone", profile.attributes().get(SearchNodeKeys.BLOCKS_MATERIAL));
        assertEquals("minecraft:diamond_pickaxe", profile.attributes().get(SearchNodeKeys.REQUIRED_TOOL));
    }

    @Test
    void compassGetsUtilityNavigationFacet() {
        Item compass = register("test_compass", new Item("Test Compass"));

        FacetProfile profile = index(compass);

        assertTrue(profile.facets().contains(ItemFacet.UTILITY_NAVIGATION));
    }

    @Test
    void guideBookPathsAndClassesProduceGuideBookCandidateFacet() {
        Item plainBook = register("plain_reference_book", new Item("Plain Reference Book"));
        Item fieldGuide = register("ami_field_guide", new Item("AMI Field Guide"));
        Item classGuide = register("class_named_manual", new TestGuideBookItem("Class Named Manual"));

        FacetProfile plainProfile = index(plainBook);
        FacetProfile fieldProfile = index(fieldGuide);
        FacetProfile classProfile = index(classGuide);

        assertTrue(plainProfile.facets().contains(ItemFacet.BOOK));
        assertFalse(plainProfile.facets().contains(ItemFacet.GUIDE_BOOK));
        assertFalse(plainProfile.facets().contains(ItemFacet.UTILITY_MISC));
        assertTrue(fieldProfile.facets().contains(ItemFacet.GUIDE_BOOK));
        assertFalse(fieldProfile.facets().contains(ItemFacet.UTILITY_MISC));
        assertEquals("true", fieldProfile.attributes().get(SearchNodeKeys.GUIDE_BOOK_CANDIDATE));
        assertTrue(classProfile.facets().contains(ItemFacet.GUIDE_BOOK));
        assertFalse(classProfile.facets().contains(ItemFacet.UTILITY_MISC));
        assertEquals("true", classProfile.attributes().get(SearchNodeKeys.GUIDE_BOOK_CANDIDATE));
    }

    @Test
    void bookshelvesAreDecorativePlaceablesNotUtilityMisc() {
        Item bookshelf = register("oak_bookshelf", new BlockItem("Oak Bookshelf", new Block(new BlockState())));

        FacetProfile profile = index(bookshelf);
        CategoryAssignment assignment = PrimaryCategoryResolver.resolve(
                new ResourceLocation("ami_test:oak_bookshelf"),
                profile
        );

        assertTrue(profile.facets().contains(ItemFacet.PLACEABLE));
        assertTrue(profile.facets().contains(ItemFacet.DECORATIVE_BLOCK));
        assertFalse(profile.facets().contains(ItemFacet.UTILITY_MISC));
        assertEquals("decoration", assignment.categoryId());
        assertEquals("furniture", assignment.subcategoryId());
    }

    @Test
    void taggedBoneGetsOrganicIngredientFacet() {
        Item boneMeal = register("test_bone", new Item("Test Bone")
                .withTag(TagKey.create(null, new ResourceLocation("c", "bones"))));

        FacetProfile profile = index(boneMeal);

        assertTrue(profile.facets().contains(ItemFacet.INGREDIENT_ORGANIC));
    }

    @Test
    void playerHeadGetsSocialFacet() {
        Item playerHead = register("player_head", new Item("Player Head"));

        FacetProfile profile = index(playerHead);

        assertTrue(profile.facets().contains(ItemFacet.SOCIAL_PLAYERS));
    }

    @Test
    void paperAndNetherStarGetLegacyCoverageFacets() {
        Item paper = register("paper", new Item("Paper"));
        Item netherStar = register("nether_star", new Item("Nether Star"));
        Item glassShard = register("magenta_shard", new Item("Magenta Glass Shard"));

        FacetProfile paperProfile = index(paper);
        FacetProfile starProfile = index(netherStar);
        FacetProfile shardProfile = index(glassShard);

        assertTrue(paperProfile.facets().contains(ItemFacet.INGREDIENT_ORGANIC));
        assertTrue(starProfile.facets().contains(ItemFacet.MAGIC_ARTIFACT));
        assertFalse(shardProfile.facets().contains(ItemFacet.MAGIC_REAGENT));
    }

    @Test
    void fishingRodGetsUtilityToolFacetNotProtein() {
        Item fishingRod = register("fishing_rod", new Item("Fishing Rod"));
        Item moddedFishingRod = register("aqua_fishing_rod", new TestFishingRodItem("Aqua Fishing Rod"));

        FacetProfile profile = index(fishingRod);
        FacetProfile moddedProfile = index(moddedFishingRod);

        assertTrue(profile.facets().contains(ItemFacet.UTILITY_TOOL));
        assertTrue(!profile.facets().contains(ItemFacet.FOOD_PROTEIN));
        assertTrue(moddedProfile.facets().contains(ItemFacet.UTILITY_TOOL));
        assertFalse(moddedProfile.facets().contains(ItemFacet.HARVEST_TOOL));
    }

    @Test
    void onlyVanillaControlSticksGetUtilityToolFacet() {
        Item carrotOnAStick = register("carrot_on_a_stick", new Item("Carrot on a Stick"));
        Item platedTentacle = register("od_plated_baked_tentacle_on_a_stick", new Item("Plated Tentacle on a Stick"));

        FacetProfile controlProfile = index(carrotOnAStick);
        FacetProfile foodProfile = index(platedTentacle);

        assertTrue(controlProfile.facets().contains(ItemFacet.UTILITY_TOOL));
        assertFalse(foodProfile.facets().contains(ItemFacet.UTILITY_TOOL));
    }

    @Test
    void legacyMagicBooksFamiliesNowEmitConcreteFacets() {
        Item raft = register("bamboo_raft", new Item("Bamboo Raft"));
        Item smithingTemplate = register("sentry_armor_trim_smithing_template", new Item("Smithing Template"));
        Item painting = register("painting", new Item("Painting"));
        Item scute = register("turtle_scute", new Item("Turtle Scute"));
        Item coal = register("coal", new Item("Coal"));
        Item endCrystal = register("end_crystal", new Item("End Crystal"));
        Item poppedChorusFruit = register("popped_chorus_fruit", new Item("Popped Chorus Fruit"));

        FacetProfile raftProfile = index(raft);
        FacetProfile templateProfile = index(smithingTemplate);
        FacetProfile paintingProfile = index(painting);
        FacetProfile scuteProfile = index(scute);
        FacetProfile coalProfile = index(coal);
        FacetProfile endCrystalProfile = index(endCrystal);
        FacetProfile poppedChorusFruitProfile = index(poppedChorusFruit);

        assertTrue(raftProfile.facets().contains(ItemFacet.TRANSPORT));
        assertTrue(templateProfile.facets().contains(ItemFacet.UTILITY_MISC));
        assertTrue(paintingProfile.facets().contains(ItemFacet.DECORATIVE_BLOCK));
        assertTrue(scuteProfile.facets().contains(ItemFacet.INGREDIENT_ORGANIC));
        assertTrue(coalProfile.facets().contains(ItemFacet.DUST));
        assertTrue(endCrystalProfile.facets().contains(ItemFacet.MAGIC_ARTIFACT));
        assertTrue(poppedChorusFruitProfile.facets().contains(ItemFacet.INGREDIENT_ORGANIC));
    }

    @Test
    void residualArmorAndWeaponItemsNowEmitConcreteFacets() {
        Item elytra = register("elytra", new Item("Elytra"));
        Item wolfArmor = register("wolf_armor", new Item("Wolf Armor"));
        Item horseArmor = register("diamond_horse_armor", new Item("Diamond Horse Armor"));
        Item saddle = register("saddle", new TestSaddleItem("Saddle"));
        Item mace = register("mace", new Item("Mace"));

        FacetProfile elytraProfile = index(elytra);
        FacetProfile wolfArmorProfile = index(wolfArmor);
        FacetProfile horseArmorProfile = index(horseArmor);
        FacetProfile saddleProfile = index(saddle);
        FacetProfile maceProfile = index(mace);

        assertTrue(elytraProfile.facets().contains(ItemFacet.ARMOR_CHEST));
        assertTrue(wolfArmorProfile.facets().contains(ItemFacet.ARMOR_ANIMAL));
        assertTrue(horseArmorProfile.facets().contains(ItemFacet.ARMOR_ANIMAL));
        assertTrue(saddleProfile.facets().contains(ItemFacet.ARMOR_ANIMAL));
        assertTrue(maceProfile.facets().contains(ItemFacet.MELEE_WEAPON));
    }

    @Test
    void prismarineBlockIsNotClassifiedAsMineralIngredient() {
        Item prismarine = register("prismarine", new BlockItem("Prismarine", new Block(new BlockState())));

        FacetProfile profile = index(prismarine);

        assertTrue(profile.facets().contains(ItemFacet.PLACEABLE));
        assertFalse(profile.facets().contains(ItemFacet.INGREDIENT_MINERAL));
    }

    @Test
    void bedIsDecorativeButNotStorage() {
        Item bed = register("light_gray_bed", new BlockItem("Light Gray Bed",
                new TestEntityBlock(new BlockState().withTag(net.minecraft.tags.BlockTags.BEDS))));

        FacetProfile profile = index(bed);

        assertTrue(profile.facets().contains(ItemFacet.PLACEABLE));
        assertTrue(profile.facets().contains(ItemFacet.HAS_BLOCK_ENTITY));
        assertTrue(profile.facets().contains(ItemFacet.DECORATIVE_BLOCK));
        assertFalse(profile.facets().contains(ItemFacet.STORAGE));
    }

    @Test
    void composterGetsMachineFacet() {
        Item composter = register("composter", new BlockItem("Composter", new Block(new BlockState())));

        FacetProfile profile = index(composter);

        assertTrue(profile.facets().contains(ItemFacet.PLACEABLE));
        assertTrue(profile.facets().contains(ItemFacet.MACHINE));
    }

    @Test
    void functionalAndDecorativePlaceablesGetNonMasonryFacets() {
        Item target = register("target", new BlockItem("Target", new Block(new BlockState())));
        Item lectern = register("lectern", new BlockItem("Lectern", new InteractiveBlock(new BlockState())));
        Item carpet = register("red_carpet", new BlockItem("Red Carpet", new Block(new BlockState())));
        Item flowerPot = register("flower_pot", new BlockItem("Flower Pot", new Block(new BlockState())));
        Item frogspawn = register("frogspawn", new BlockItem("Frogspawn", new Block(new BlockState())));
        Item lemonPie = register("lemon_pie", new BlockItem("Lemon Pie", new Block(new BlockState())));

        FacetProfile targetProfile = index(target);
        FacetProfile lecternProfile = index(lectern);
        FacetProfile carpetProfile = index(carpet);
        FacetProfile flowerPotProfile = index(flowerPot);
        FacetProfile frogspawnProfile = index(frogspawn);
        FacetProfile lemonPieProfile = index(lemonPie);

        assertTrue(targetProfile.facets().contains(ItemFacet.REDSTONE_LOGIC));
        assertTrue(targetProfile.facets().contains(ItemFacet.ACTIVE_REDSTONE_LOGIC));
        assertTrue(lecternProfile.facets().contains(ItemFacet.INTERACTIVE_BLOCK));
        assertTrue(lecternProfile.facets().contains(ItemFacet.WORKSTATION));
        assertTrue(lecternProfile.facets().contains(ItemFacet.MACHINE));
        assertTrue(carpetProfile.facets().contains(ItemFacet.DECORATIVE_BLOCK));
        assertTrue(flowerPotProfile.facets().contains(ItemFacet.DECORATIVE_BLOCK));
        assertTrue(frogspawnProfile.facets().contains(ItemFacet.NATURE_MISC));
        assertTrue(lemonPieProfile.facets().contains(ItemFacet.PLACEABLE_FOOD));
    }

    @Test
    void redstoneBehaviorAndRelayPathsProduceRedstoneFacets() {
        Item pulseRelay = register("pulse_relay",
                new BlockItem("Pulse Redstone Relay", new Block(new BlockState().withSignalSource(true))));
        Item comparatorLike = register("analog_reader",
                new BlockItem("Analog Reader", new Block(new BlockState().withAnalogOutputSignal(true))));

        FacetProfile relayProfile = index(pulseRelay);
        FacetProfile analogProfile = index(comparatorLike);

        assertTrue(relayProfile.facets().contains(ItemFacet.REDSTONE_LOGIC));
        assertTrue(relayProfile.facets().contains(ItemFacet.ACTIVE_REDSTONE_LOGIC));
        assertTrue(relayProfile.facets().contains(ItemFacet.REDSTONE_SIGNAL));
        assertTrue(analogProfile.facets().contains(ItemFacet.REDSTONE_SIGNAL));
        assertTrue(analogProfile.facets().contains(ItemFacet.PASSIVE_COMPARATOR_OUTPUT));
        assertFalse(analogProfile.facets().contains(ItemFacet.ACTIVE_REDSTONE_LOGIC));
    }

    @Test
    void furnitureTagsProduceDecorationFacetForPassiveSignalBlocks() {
        Item toilet = register("yellow_toilet", new BlockItem("Yellow Toilet",
                new TestEntityBlock(new BlockState().withAnalogOutputSignal(true)))
                .withTag(TagKey.create(null, new ResourceLocation("refurbished_furniture", "bathroom"))));

        FacetProfile profile = index(toilet);

        assertTrue(profile.facets().contains(ItemFacet.HAS_BLOCK_ENTITY));
        assertTrue(profile.facets().contains(ItemFacet.REDSTONE_SIGNAL));
        assertTrue(profile.facets().contains(ItemFacet.PASSIVE_COMPARATOR_OUTPUT));
        assertFalse(profile.facets().contains(ItemFacet.REDSTONE_LOGIC));
        assertFalse(profile.facets().contains(ItemFacet.ACTIVE_REDSTONE_LOGIC));
        assertTrue(profile.facets().contains(ItemFacet.DECORATIVE_BLOCK));
    }

    @Test
    void placedBlockEvidenceIsRecordedAsMetadata() {
        Item planks = register("metadata_planks", new BlockItem("Metadata Planks",
                new Block(new BlockState()
                        .withTag(net.minecraft.tags.BlockTags.PLANKS)
                        .withProperty(new Property<>("Facing")))));

        FacetProfile profile = index(planks);

        assertEquals("net.minecraft.world.level.block.Block", profile.attributes().get(SearchNodeKeys.BLOCK_CLASS));
        assertEquals("minecraft:planks", profile.attributes().get(SearchNodeKeys.BLOCK_TAGS));
        assertEquals("facing", profile.attributes().get(SearchNodeKeys.BLOCK_STATE_PROPERTIES));
    }

    @Test
    void decorativeNaturalAndGeologicPlaceablesGetExpectedFacets() {
        Item candle = register("white_candle", new BlockItem("White Candle", new Block(new BlockState().withLightEmission(3))));
        Item zombieHead = register("zombie_head", new BlockItem("Zombie Head", new Block(new BlockState())));
        Item pointedDripstone = register("pointed_dripstone", new BlockItem("Pointed Dripstone", new Block(new BlockState())));
        Item tubeCoral = register("tube_coral", new BlockItem("Tube Coral", new Block(new BlockState())));
        Item oakSign = register("oak_sign", new BlockItem("Oak Sign", new Block(new BlockState())));
        Item waySign = register("biomesoplenty/way_sign_jacaranda", new BlockItem("Jacaranda Way Sign", new Block(new BlockState())));
        Item taterInAJar = register("tater_in_a_jar", new BlockItem("Tater in a Jar", new Block(new BlockState())));
        Item cobweb = register("cobweb", new BlockItem("Cobweb", new Block(new BlockState())));
        Item deadBush = register("dead_bush", new BlockItem("Dead Bush", new Block(new BlockState())));
        Item sculk = register("sculk", new BlockItem("Sculk", new Block(new BlockState())));

        FacetProfile candleProfile = index(candle);
        FacetProfile zombieHeadProfile = index(zombieHead);
        FacetProfile pointedDripstoneProfile = index(pointedDripstone);
        FacetProfile tubeCoralProfile = index(tubeCoral);
        FacetProfile oakSignProfile = index(oakSign);
        FacetProfile waySignProfile = index(waySign);
        FacetProfile taterInAJarProfile = index(taterInAJar);
        FacetProfile cobwebProfile = index(cobweb);
        FacetProfile deadBushProfile = index(deadBush);
        FacetProfile sculkProfile = index(sculk);

        assertTrue(candleProfile.facets().contains(ItemFacet.DECORATIVE_BLOCK));
        assertTrue(candleProfile.facets().contains(ItemFacet.LIGHT_SOURCE));
        assertTrue(zombieHeadProfile.facets().contains(ItemFacet.DECORATIVE_BLOCK));
        assertFalse(zombieHeadProfile.facets().contains(ItemFacet.SOCIAL_PLAYERS));
        assertTrue(pointedDripstoneProfile.facets().contains(ItemFacet.STONE_BLOCK));
        assertTrue(tubeCoralProfile.facets().contains(ItemFacet.NATURE_MISC));
        assertFalse(tubeCoralProfile.facets().contains(ItemFacet.CABLE));
        assertFalse(tubeCoralProfile.facets().contains(ItemFacet.TECH_COMPONENT));
        assertTrue(oakSignProfile.facets().contains(ItemFacet.DECORATIVE_BLOCK));
        assertTrue(waySignProfile.facets().contains(ItemFacet.DECORATIVE_BLOCK));
        assertTrue(taterInAJarProfile.facets().contains(ItemFacet.DECORATIVE_BLOCK));
        assertTrue(cobwebProfile.facets().contains(ItemFacet.NATURE_MISC));
        assertTrue(deadBushProfile.facets().contains(ItemFacet.NATURE_MISC));
        assertTrue(sculkProfile.facets().contains(ItemFacet.NATURE_MISC));
    }

    @Test
    void storageBlocksGetMaterialFacetsButNotStorageFacet() {
        Item ironBlock = register("iron_block", new Item("Iron Block")
                .withTag(TagKey.create(null, new ResourceLocation("c", "storage_blocks/iron"))));
        Item rawIronBlock = register("raw_iron_block", new Item("Raw Iron Block")
                .withTag(TagKey.create(null, new ResourceLocation("c", "storage_blocks/raw_iron"))));
        Item diamondBlock = register("diamond_block", new Item("Diamond Block")
                .withTag(TagKey.create(null, new ResourceLocation("c", "storage_blocks/diamond"))));

        FacetProfile ironProfile = index(ironBlock);
        FacetProfile rawIronProfile = index(rawIronBlock);
        FacetProfile diamondProfile = index(diamondBlock);

        assertTrue(ironProfile.facets().contains(ItemFacet.INGOT));
        assertFalse(ironProfile.facets().contains(ItemFacet.STORAGE));

        assertTrue(rawIronProfile.facets().contains(ItemFacet.RAW_MATERIAL));
        assertFalse(rawIronProfile.facets().contains(ItemFacet.STORAGE));

        assertTrue(diamondProfile.facets().contains(ItemFacet.GEM));
        assertFalse(diamondProfile.facets().contains(ItemFacet.STORAGE));
    }

    @Test
    void forgeMaterialTagsProduceTechMaterialFacets() {
        Item inaniteIngot = register("inanite_ingot", new Item("Inanite Ingot")
                .withTag(TagKey.create(null, new ResourceLocation("forge", "ingots/inanite"))));
        Item cyaniteDust = register("cyanite_dust", new Item("Cyanite Dust")
                .withTag(TagKey.create(null, new ResourceLocation("forge", "dusts/cyanite"))));
        Item yelloriumBlock = register("yellorium_block", new Item("Yellorium Block")
                .withTag(TagKey.create(null, new ResourceLocation("forge", "storage_blocks/yellorium"))));

        assertTrue(index(inaniteIngot).facets().contains(ItemFacet.INGOT));
        assertTrue(index(cyaniteDust).facets().contains(ItemFacet.DUST));
        assertTrue(index(yelloriumBlock).facets().contains(ItemFacet.INGOT));
    }

    @Test
    void broadDietTagsOnlyCreateFoodFacetsForEdibleItems() {
        Item leaves = register("diet_leaves", new BlockItem("Diet Leaves", new Block(new BlockState()))
                .withTag(TagKey.create(null, new ResourceLocation("diet", "vegetables"))));
        Item slimeball = register("diet_slimeball", new Item("Diet Slimeball")
                .withTag(TagKey.create(null, new ResourceLocation("diet", "proteins"))));
        Item tomato = register("diet_tomato", new Item("Diet Tomato")
                .withComponent(DataComponents.FOOD)
                .withTag(TagKey.create(null, new ResourceLocation("diet", "vegetables"))));

        assertFalse(index(leaves).facets().contains(ItemFacet.CROP));
        assertFalse(index(slimeball).facets().contains(ItemFacet.FOOD_PROTEIN));
        assertTrue(index(tomato).facets().contains(ItemFacet.CROP));
    }

    @Test
    void cropLikeBlockClassesAndPathsProduceCropFacetEvenWithBlockEntity() {
        Item grapeBush = register("red_grape_bush_stage_2",
                new BlockItem("Red Grape Bush", new TestGrapeBushStage2Block(new BlockState())));

        FacetProfile profile = index(grapeBush);

        assertTrue(profile.facets().contains(ItemFacet.PLACEABLE));
        assertTrue(profile.facets().contains(ItemFacet.HAS_BLOCK_ENTITY));
        assertTrue(profile.facets().contains(ItemFacet.CROP));
    }

    @Test
    void materialLeadDoesNotBecomeUtilityLeash() {
        Item leadIngot = register("ingot_lead", new Item("Lead Ingot")
                .withTag(TagKey.create(null, new ResourceLocation("forge", "ingots/lead"))));
        Item leash = register("lead", new Item("Lead"));

        assertTrue(index(leadIngot).facets().contains(ItemFacet.INGOT));
        assertFalse(index(leadIngot).facets().contains(ItemFacet.UTILITY_MISC));
        assertTrue(index(leash).facets().contains(ItemFacet.UTILITY_MISC));
    }

    @Test
    void curiosTagsProduceCurioFacet() {
        Item trinket = register("trinket", new Item("Trinket")
                .withTag(TagKey.create(null, new ResourceLocation("curios", "charm"))));

        assertTrue(index(trinket).facets().contains(ItemFacet.CURIO));
    }

    @Test
    void componentAndToolTagsProduceConcreteFacets() {
        Item wire = register("wire_copper", new Item("Copper Wire")
                .withTag(TagKey.create(null, new ResourceLocation("c", "wires/copper"))));
        Item circuit = register("integrated_circuit", new Item("Integrated Circuit")
                .withTag(TagKey.create(null, new ResourceLocation("ccbr", "integrated_circuits"))));
        Item cogwheel = register("small_cogwheel", new Item("Small Cogwheel"));
        Item hammer = register("hammer", new Item("Hammer")
                .withTag(TagKey.create(null, new ResourceLocation("immersiveengineering", "tools/hammers"))));
        Item disc = register("music_disc", new Item("Music Disc")
                .withTag(TagKey.create(null, new ResourceLocation("minecraft", "music_discs"))));
        Item round = register("autocannon_round", new Item("Autocannon Round")
                .withTag(TagKey.create(null, new ResourceLocation("createbigcannons", "autocannon_rounds"))));
        Item narrowTrack = register("track_incomplete_jungle_narrow", new Item("Incomplete Narrow Jungle Track"));
        Item rockyShell = register("rocky_shell", new Item("Rocky Shell"));
        Item mapleDoor = register("maple_japanese_door", new BlockItem("Maple Shoji Door", new Block(new BlockState())));
        Item upgrade = register("speed_upgrade", new Item("Speed Upgrade"));
        Item blueprint = register("blueprint", new Item("Blueprint"));
        Item leggingsBlueprint = register("leggings_blueprint", new Item("Leggings Blueprint")
                .withTag(TagKey.create(null, new ResourceLocation("silentgear", "blueprints/leggings")))
                .withTag(TagKey.create(null, new ResourceLocation("silentgear", "blueprints")))
                .withTag(TagKey.create(null, new ResourceLocation("c", "blueprint_override"))));
        Item bandage = register("bandage", new Item("Bandage"));
        Item coin = register("gold_coin", new Item("Gold Coin"));
        Item wrench = register("modded_wrench", new Item("Modded Wrench")
                .withTag(TagKey.create(null, new ResourceLocation("c", "tools/wrenches"))));
        Item taggedFishingRod = register("tagged_rod", new Item("Tagged Rod")
                .withTag(TagKey.create(null, new ResourceLocation("c", "tools/fishing_rods"))));
        Item enchantableHelmet = register("enchantable_helmet", new Item("Enchantable Helmet")
                .withTag(TagKey.create(null, new ResourceLocation("minecraft", "enchantable/head_armor"))));

        assertTrue(index(wire).facets().contains(ItemFacet.TECH_COMPONENT));
        assertTrue(index(wire).facets().contains(ItemFacet.CABLE));
        assertTrue(index(circuit).facets().contains(ItemFacet.TECH_COMPONENT));
        assertTrue(index(cogwheel).facets().contains(ItemFacet.MECHANICAL_COMPONENT));
        assertTrue(index(cogwheel).facets().contains(ItemFacet.TECH_COMPONENT));
        assertTrue(index(hammer).facets().contains(ItemFacet.UTILITY_TOOL));
        assertTrue(index(disc).facets().contains(ItemFacet.UTILITY_MISC));
        assertTrue(index(round).facets().contains(ItemFacet.PROJECTILE));
        assertFalse(index(narrowTrack).facets().contains(ItemFacet.PROJECTILE));
        assertFalse(index(rockyShell).facets().contains(ItemFacet.PROJECTILE));
        assertTrue(index(rockyShell).facets().contains(ItemFacet.INGREDIENT_ORGANIC));
        assertFalse(index(mapleDoor).facets().contains(ItemFacet.UTILITY_NAVIGATION));
        assertTrue(index(upgrade).facets().contains(ItemFacet.UPGRADE));
        assertTrue(index(blueprint).facets().contains(ItemFacet.TEMPLATE));
        assertTrue(index(leggingsBlueprint).facets().contains(ItemFacet.TEMPLATE));
        assertFalse(index(leggingsBlueprint).facets().contains(ItemFacet.INGREDIENT_ORGANIC));
        assertTrue(index(bandage).facets().contains(ItemFacet.UTILITY_MEDICAL));
        assertTrue(index(coin).facets().contains(ItemFacet.UTILITY_CURRENCY));
        assertTrue(index(wrench).facets().contains(ItemFacet.UTILITY_TOOL));
        assertTrue(index(taggedFishingRod).facets().contains(ItemFacet.UTILITY_TOOL));
        assertFalse(index(taggedFishingRod).facets().contains(ItemFacet.HARVEST_TOOL));
        assertFalse(index(enchantableHelmet).facets().contains(ItemFacet.UTILITY_TOOL));
    }

    @Test
    void splinterspawnPathDoesNotLookMedical() {
        Item splinterspawn = register("splinterspawn_infested_pyrite",
                new BlockItem("Splinterspawn Infested Pyrite", new Block(new BlockState())));

        FacetProfile profile = index(splinterspawn);

        assertTrue(profile.facets().contains(ItemFacet.PLACEABLE));
        assertFalse(profile.facets().contains(ItemFacet.UTILITY_MEDICAL));
    }

    @Test
    void equipmentSlotFactsProduceEquippableAndArmorFacets() {
        Item helmet = register("test_helmet", new ArmorItem("Test Helmet", EquipmentSlot.HEAD));

        FacetProfile profile = index(helmet);

        assertTrue(profile.facets().contains(ItemFacet.EQUIPPABLE));
        assertTrue(profile.facets().contains(ItemFacet.ARMOR_HEAD));
        assertEquals("head", profile.attributes().get(SearchNodeKeys.EQUIPMENT_SLOT));
        assertEquals("net.minecraft.world.item.ArmorItem", profile.attributes().get(SearchNodeKeys.ITEM_CLASS));
    }

    @Test
    void entityArmorClassDoesNotBecomePlayerChestArmor() {
        Item golemArmor = register("iron_dog_golem_armor", new DogGolemArmorItem("Iron Dog Golem Armor"));

        FacetProfile profile = index(golemArmor);

        assertTrue(profile.facets().contains(ItemFacet.EQUIPPABLE));
        assertTrue(profile.facets().contains(ItemFacet.ARMOR_ANIMAL));
        assertFalse(profile.facets().contains(ItemFacet.ARMOR_CHEST));
        assertEquals("chest", profile.attributes().get(SearchNodeKeys.EQUIPMENT_SLOT));
    }

    private static final class TestEntityBlock extends Block implements EntityBlock {
        private TestEntityBlock(BlockState defaultState) {
            super(defaultState);
        }
    }

    private static final class TestGrapeBushStage2Block extends Block implements EntityBlock {
        private TestGrapeBushStage2Block(BlockState defaultState) {
            super(defaultState);
        }
    }

    private static final class InteractiveBlock extends Block implements MenuProvider {
        private InteractiveBlock(BlockState defaultState) {
            super(defaultState);
        }
    }

    private static final class DrinkItem extends Item {
        private DrinkItem(String name) {
            super(name);
        }

        @Override
        public UseAnim getUseAnimation(ItemStack stack) {
            return UseAnim.DRINK;
        }
    }

    private static final class TestFishingRodItem extends FishingRodItem {
        private TestFishingRodItem(String name) {
            super(name);
        }
    }

    private static final class TestGuideBookItem extends Item {
        private TestGuideBookItem(String name) {
            super(name);
        }
    }

    private static final class TestSaddleItem extends Item {
        private TestSaddleItem(String name) {
            super(name);
        }
    }

    private static final class PowerBottleBlock extends Block {
        private PowerBottleBlock(BlockState defaultState) {
            super(defaultState);
        }
    }

    private static final class PowerBottleItem extends BlockItem {
        private PowerBottleItem(String name, Block block) {
            super(name, block);
        }
    }

    private static final class DogGolemArmorItem extends ArmorItem {
        private DogGolemArmorItem(String name) {
            super(name, EquipmentSlot.CHEST);
        }
    }

    private static final class CrucibleBlock extends Block implements EntityBlock {
        private CrucibleBlock(BlockState defaultState) {
            super(defaultState);
        }
    }
}
