package com.sanhiruzu.ami.index;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.state.BlockState;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class FacetIndexerTest {

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
    void redstoneDustHasRedstoneFacet() {
        FacetProfile profile = index(Items.REDSTONE);

        assertTrue(profile.facets().contains(ItemFacet.REDSTONE_LOGIC));
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

        FacetProfile paperProfile = index(paper);
        FacetProfile starProfile = index(netherStar);

        assertTrue(paperProfile.facets().contains(ItemFacet.INGREDIENT_ORGANIC));
        assertTrue(starProfile.facets().contains(ItemFacet.MAGIC_ARTIFACT));
    }

    @Test
    void fishingRodGetsUtilityToolFacetNotProtein() {
        Item fishingRod = register("fishing_rod", new Item("Fishing Rod"));

        FacetProfile profile = index(fishingRod);

        assertTrue(profile.facets().contains(ItemFacet.UTILITY_TOOL));
        assertTrue(!profile.facets().contains(ItemFacet.FOOD_PROTEIN));
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
        Item mace = register("mace", new Item("Mace"));

        FacetProfile elytraProfile = index(elytra);
        FacetProfile wolfArmorProfile = index(wolfArmor);
        FacetProfile horseArmorProfile = index(horseArmor);
        FacetProfile maceProfile = index(mace);

        assertTrue(elytraProfile.facets().contains(ItemFacet.ARMOR_CHEST));
        assertTrue(wolfArmorProfile.facets().contains(ItemFacet.ARMOR_CHEST));
        assertTrue(horseArmorProfile.facets().contains(ItemFacet.ARMOR_CHEST));
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
        assertTrue(lecternProfile.facets().contains(ItemFacet.INTERACTIVE_BLOCK));
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
        assertTrue(relayProfile.facets().contains(ItemFacet.REDSTONE_SIGNAL));
        assertTrue(analogProfile.facets().contains(ItemFacet.REDSTONE_SIGNAL));
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

    private static final class TestEntityBlock extends Block implements EntityBlock {
        private TestEntityBlock(BlockState defaultState) {
            super(defaultState);
        }
    }

    private static final class InteractiveBlock extends Block implements MenuProvider {
        private InteractiveBlock(BlockState defaultState) {
            super(defaultState);
        }
    }
}
