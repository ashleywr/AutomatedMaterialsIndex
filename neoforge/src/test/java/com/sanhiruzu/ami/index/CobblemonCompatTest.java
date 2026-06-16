package com.sanhiruzu.ami.index;

import com.sanhiruzu.ami.compat.CobblemonCompat;
import com.sanhiruzu.ami.compat.CompatFamilyDetector;
import com.sanhiruzu.ami.config.AmiConfig;
import net.minecraft.resources.Identifier;
import org.junit.jupiter.api.Test;

import java.util.EnumSet;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CobblemonCompatTest {
    @Test
    void medicineItemsExposeHealingAndBeatGenericPlaceableEvidence() {
        Map<String, String> meta = enriched("cobblemon:hyper_potion", "Cobblemon: Consumables",
                "com.cobblemon.mod.common.item.PotionItem");

        CategoryAssignment assignment = resolve("cobblemon:hyper_potion", meta, ItemFacet.PLACEABLE);

        assertEquals("medicine", meta.get(SearchNodeKeys.COBBLEMON_ITEM_KIND));
        assertEquals("heal", meta.get(SearchNodeKeys.POKEMON_MEDICINE_KIND));
        assertEquals("120", meta.get(SearchNodeKeys.POKEMON_HEALING));
        assertEquals("cobblemon", assignment.categoryId());
        assertEquals("medicine", assignment.subcategoryId());
        assertTrue(assignment.attributes().getOrDefault("classificationEvidence", "").contains("identity.cobblemon.medicine"));
        assertEquals("focused", assignment.attributes().get(SearchNodeKeys.COMPAT_CATEGORY_POLICY));
        assertEquals("hard_identity", assignment.attributes().get(SearchNodeKeys.CLASSIFICATION_ROUTE_PHASE));
        assertEquals("identity", assignment.attributes().get(SearchNodeKeys.CLASSIFICATION_ROUTE_RULE));
        assertTrue(assignment.attributes().getOrDefault(SearchNodeKeys.CLASSIFICATION_ROUTE, "").contains("compat=cobblemon"));
        assertTrue(assignment.attributes().getOrDefault(SearchNodeKeys.CLASSIFICATION_ROUTE, "").contains("policy=focused"));
        assertTrue(assignment.attributes().getOrDefault(SearchNodeKeys.CLASSIFICATION_ROUTE, "").contains("hard_identity:identity[cobblemon/medicine]"));
    }

    @Test
    void statusCuresExposeTheCuredCondition() {
        Map<String, String> meta = enriched("cobblemon:antidote", "Cobblemon: Consumables",
                "com.cobblemon.mod.common.item.StatusCureItem");

        CategoryAssignment assignment = resolve("cobblemon:antidote", meta);

        assertEquals("medicine", meta.get(SearchNodeKeys.COBBLEMON_ITEM_KIND));
        assertEquals("status_cure", meta.get(SearchNodeKeys.POKEMON_MEDICINE_KIND));
        assertEquals("poison", meta.get(SearchNodeKeys.POKEMON_STATUS_CURE));
        assertEquals("cobblemon", assignment.categoryId());
        assertEquals("medicine", assignment.subcategoryId());
    }

    @Test
    void battleItemsUseCobblemonMedicineEvenWhenTheyArePlaceableStacks() {
        Map<String, String> meta = enriched("cobblemon:x_defence", "Cobblemon: Consumables",
                "com.cobblemon.mod.common.item.battle.XStatItem");

        CategoryAssignment assignment = resolve("cobblemon:x_defence", meta, ItemFacet.PLACEABLE);

        assertEquals("medicine", meta.get(SearchNodeKeys.COBBLEMON_ITEM_KIND));
        assertEquals("battle_stat_boost", meta.get(SearchNodeKeys.POKEMON_MEDICINE_KIND));
        assertEquals("cobblemon", assignment.categoryId());
        assertEquals("medicine", assignment.subcategoryId());
    }

    @Test
    void pokeBallsExposeFamilyAndTier() {
        Map<String, String> meta = enriched("cobblemon:ancient_ultra_ball", "Cobblemon: Poke Balls",
                "com.cobblemon.mod.common.item.PokeBallItem");

        CategoryAssignment assignment = resolve("cobblemon:ancient_ultra_ball", meta);

        assertEquals("poke_ball", meta.get(SearchNodeKeys.COBBLEMON_ITEM_KIND));
        assertEquals("ancient", meta.get(SearchNodeKeys.POKEMON_BALL_FAMILY));
        assertEquals("ultra", meta.get(SearchNodeKeys.POKEMON_BALL_TIER));
        assertEquals("poke_ball", meta.get(SearchNodeKeys.VARIANT_GROUP));
        assertEquals("cobblemon", assignment.categoryId());
        assertEquals("poke_balls", assignment.subcategoryId());
    }

    @Test
    void apricornsAndBerriesUsePokemonNatureSubcategories() {
        Map<String, String> apricorn = enriched("cobblemon:red_apricorn", "Cobblemon: Agriculture",
                "com.cobblemon.mod.common.item.ApricornItem");
        Map<String, String> apricornPlanks = enriched("cobblemon:apricorn_planks", "Cobblemon: Blocks",
                "net.minecraft.world.item.BlockItem");
        Map<String, String> berry = enriched("cobblemon:pomeg_berry", "Cobblemon: Berries",
                "com.cobblemon.mod.common.item.BerryItem");

        CategoryAssignment apricornAssignment = resolve("cobblemon:red_apricorn", apricorn, ItemFacet.PLACEABLE);
        CategoryAssignment apricornPlanksAssignment = resolve("cobblemon:apricorn_planks", apricornPlanks, ItemFacet.PLACEABLE);
        CategoryAssignment berryAssignment = resolve("cobblemon:pomeg_berry", berry, ItemFacet.PLACEABLE);

        assertEquals("apricorn", apricorn.get(SearchNodeKeys.COBBLEMON_ITEM_KIND));
        assertEquals("red", apricorn.get(SearchNodeKeys.COLOR_BUCKET));
        assertEquals("apricorn", apricorn.get(SearchNodeKeys.VARIANT_GROUP));
        assertEquals("cobblemon", apricornAssignment.categoryId());
        assertEquals("apricorns", apricornAssignment.subcategoryId());
        assertEquals("building", apricornPlanks.get(SearchNodeKeys.COBBLEMON_ITEM_KIND));
        assertEquals("cobblemon", apricornPlanksAssignment.categoryId());
        assertEquals("building", apricornPlanksAssignment.subcategoryId());
        assertEquals("berry", berry.get(SearchNodeKeys.COBBLEMON_ITEM_KIND));
        assertEquals("pokemon_berry", berry.get(SearchNodeKeys.VARIANT_GROUP));
        assertEquals("cobblemon", berryAssignment.categoryId());
        assertEquals("berries", berryAssignment.subcategoryId());
    }

    @Test
    void evolutionAndFossilItemsUseFocusedSubcategories() {
        Map<String, String> stone = enriched("cobblemon:dusk_stone", "Cobblemon: Evolution Items", "");
        Map<String, String> fossil = enriched("cobblemon:dome_fossil", "Cobblemon: Archaeology", "");

        CategoryAssignment stoneAssignment = resolve("cobblemon:dusk_stone", stone);
        CategoryAssignment fossilAssignment = resolve("cobblemon:dome_fossil", fossil);

        assertEquals("evolution_item", stone.get(SearchNodeKeys.COBBLEMON_ITEM_KIND));
        assertEquals("stone", stone.get(SearchNodeKeys.POKEMON_EVOLUTION_TRIGGER));
        assertEquals("pokemon_evolution_stone", stone.get(SearchNodeKeys.VARIANT_GROUP));
        assertEquals("cobblemon", stoneAssignment.categoryId());
        assertEquals("evolution", stoneAssignment.subcategoryId());
        assertEquals("fossil", fossil.get(SearchNodeKeys.COBBLEMON_ITEM_KIND));
        assertEquals("cobblemon", fossilAssignment.categoryId());
        assertEquals("fossils", fossilAssignment.subcategoryId());
    }

    @Test
    void machinesDecorAndHeldItemsStaySeparate() {
        Map<String, String> pc = enriched("cobblemon:pc", "Cobblemon: Blocks",
                "net.minecraft.world.item.BlockItem");
        Map<String, String> displayCase = enriched("cobblemon:display_case", "Cobblemon: Colored Blocks",
                "net.minecraft.world.item.BlockItem");
        Map<String, String> powerWeight = enriched("cobblemon:power_weight", "Cobblemon: Held Items", "");

        CategoryAssignment pcAssignment = resolve("cobblemon:pc", pc, ItemFacet.PLACEABLE, ItemFacet.HAS_BLOCK_ENTITY);
        CategoryAssignment displayAssignment = resolve("cobblemon:display_case", displayCase, ItemFacet.PLACEABLE);
        CategoryAssignment heldAssignment = resolve("cobblemon:power_weight", powerWeight);

        assertEquals("machine", pc.get(SearchNodeKeys.COBBLEMON_ITEM_KIND));
        assertEquals("cobblemon", pcAssignment.categoryId());
        assertEquals("machines", pcAssignment.subcategoryId());
        assertEquals("decor", displayCase.get(SearchNodeKeys.COBBLEMON_ITEM_KIND));
        assertEquals("cobblemon", displayAssignment.categoryId());
        assertEquals("decor", displayAssignment.subcategoryId());
        assertEquals("held_item", powerWeight.get(SearchNodeKeys.COBBLEMON_ITEM_KIND));
        assertEquals("training", powerWeight.get(SearchNodeKeys.POKEMON_HELD_ITEM_ROLE));
        assertEquals("cobblemon", heldAssignment.categoryId());
        assertEquals("held_items", heldAssignment.subcategoryId());
    }

    @Test
    void utilityConsumableAgricultureAndTaggedHeldItemsUseCobblemonBuckets() {
        Map<String, String> pokedex = enriched("cobblemon:pokedex_black", "Cobblemon: Utility Items",
                "com.cobblemon.mod.common.item.PokedexItem");
        Map<String, String> hyperTrainingCandy = enriched("cobblemon:mighty_candy", "Cobblemon: Consumables",
                "com.cobblemon.mod.common.item.interactive.HyperTrainingItem");
        Map<String, String> mulch = enriched("cobblemon:growth_mulch", "Cobblemon: Agriculture", "");
        Map<String, String> gem = enriched("cobblemon:psychic_gem", "Cobblemon: Archaeology",
                "com.cobblemon.mod.common.item.CobblemonItem",
                Map.of(SearchNodeKeys.TAGS, "cobblemon:held/is_held_item,cobblemon:type_gems,c:gems"));

        CategoryAssignment pokedexAssignment = resolve("cobblemon:pokedex_black", pokedex);
        CategoryAssignment hyperTrainingCandyAssignment = resolve("cobblemon:mighty_candy", hyperTrainingCandy);
        CategoryAssignment mulchAssignment = resolve("cobblemon:growth_mulch", mulch);
        CategoryAssignment gemAssignment = resolve("cobblemon:psychic_gem", gem, ItemFacet.GEM);

        assertEquals("utility_item", pokedex.get(SearchNodeKeys.COBBLEMON_ITEM_KIND));
        assertEquals("utility", pokedexAssignment.subcategoryId());
        assertEquals("medicine", hyperTrainingCandy.get(SearchNodeKeys.COBBLEMON_ITEM_KIND));
        assertEquals("medicine", hyperTrainingCandyAssignment.subcategoryId());
        assertEquals("agriculture", mulch.get(SearchNodeKeys.COBBLEMON_ITEM_KIND));
        assertEquals("agriculture", mulchAssignment.subcategoryId());
        assertEquals("held_item", gem.get(SearchNodeKeys.COBBLEMON_ITEM_KIND));
        assertEquals("type_boost", gem.get(SearchNodeKeys.POKEMON_HELD_ITEM_ROLE));
        assertEquals("psychic", gem.get(SearchNodeKeys.POKEMON_TYPE));
        assertEquals("held_items", gemAssignment.subcategoryId());
    }

    @Test
    void runtimeEdgeCasesStayUnderCobblemonTopLevel() {
        Map<String, String> boat = enriched("cobblemon:apricorn_boat", "Tools & Utilities",
                "com.cobblemon.mod.common.item.CobblemonBoatItem",
                Map.of(SearchNodeKeys.TAGS, "minecraft:boats,cobblemon:boats"));
        Map<String, String> slatheredLog = enriched("cobblemon:saccharine_log_slathered", "",
                "net.minecraft.world.item.BlockItem",
                Map.of(SearchNodeKeys.TAGS, "minecraft:logs,cobblemon:saccharine_logs"));
        Map<String, String> npcEditor = enriched("cobblemon:npc_editor", "Operator Utilities",
                "com.cobblemon.mod.common.item.CobblemonItem");
        Map<String, String> sweetHeart = enriched("cobblemon:sweet_heart", "",
                "com.cobblemon.mod.common.item.CobblemonItem");
        Map<String, String> pokemonModel = enriched("cobblemon:pokemon_model", "",
                "com.cobblemon.mod.common.item.PokemonItem");

        CategoryAssignment boatAssignment = resolve("cobblemon:apricorn_boat", boat, ItemFacet.TRANSPORT);
        CategoryAssignment logAssignment = resolve("cobblemon:saccharine_log_slathered", slatheredLog,
                ItemFacet.PLACEABLE, ItemFacet.LOG, ItemFacet.WOOD_BLOCK);
        CategoryAssignment npcAssignment = resolve("cobblemon:npc_editor", npcEditor, ItemFacet.UTILITY_TOOL);
        CategoryAssignment sweetHeartAssignment = resolve("cobblemon:sweet_heart", sweetHeart);
        CategoryAssignment pokemonModelAssignment = resolve("cobblemon:pokemon_model", pokemonModel);

        assertEquals("transport", boat.get(SearchNodeKeys.COBBLEMON_ITEM_KIND));
        assertEquals("cobblemon", boatAssignment.categoryId());
        assertEquals("transport", boatAssignment.subcategoryId());
        assertEquals("building", slatheredLog.get(SearchNodeKeys.COBBLEMON_ITEM_KIND));
        assertEquals("cobblemon", logAssignment.categoryId());
        assertEquals("building", logAssignment.subcategoryId());
        assertEquals("utility_item", npcEditor.get(SearchNodeKeys.COBBLEMON_ITEM_KIND));
        assertEquals("cobblemon", npcAssignment.categoryId());
        assertEquals("utility", npcAssignment.subcategoryId());
        assertEquals("medicine", sweetHeart.get(SearchNodeKeys.COBBLEMON_ITEM_KIND));
        assertEquals("20", sweetHeart.get(SearchNodeKeys.POKEMON_HEALING));
        assertEquals("cobblemon", sweetHeartAssignment.categoryId());
        assertEquals("medicine", sweetHeartAssignment.subcategoryId());
        assertEquals("misc", pokemonModel.get(SearchNodeKeys.COBBLEMON_ITEM_KIND));
        assertEquals("cobblemon", pokemonModelAssignment.categoryId());
        assertEquals("misc", pokemonModelAssignment.subcategoryId());
    }

    @Test
    void cobblemonAddonContentCanJoinTheCobblemonEcosystemWithoutLosingItsOwnModId() {
        Map<String, String> addonBall = enriched("cobblemonextras:shadow_ball", "Cobblemon Extras: Poke Balls",
                "com.example.cobblemonextras.item.PokeBallItem");
        Map<String, String> cobblefurniesChair = enriched("cobblefurnies:red_chair", "CobbleFurnies",
                "net.minecraft.world.item.BlockItem");
        Map<String, String> furniCrafter = enriched("cobblefurnies:furnicrafter", "CobbleFurnies",
                "net.minecraft.world.item.BlockItem");
        Map<String, String> megaStone = enriched("mega_showdown:absolite", "Mega Evolution",
                "com.github.yajatkaul.mega_showdown.item.custom.mega.MegaStone",
                Map.of(SearchNodeKeys.TAGS, "mega_showdown:mega_stone"));
        Map<String, String> zCrystal = enriched("mega_showdown:normalium_z", "Z Power",
                "com.github.yajatkaul.mega_showdown.item.custom.z.ElementalZCrystal",
                Map.of(SearchNodeKeys.TAGS, "mega_showdown:z_crystal"));
        Map<String, String> badge = enriched("badgebox:fire_badge", "Badges",
                "mod.linguardium.badgebox.common.item.BadgeItem",
                Map.of(SearchNodeKeys.TAGS, "badgebox:badges"));

        CategoryAssignment assignment = resolve("cobblemonextras:shadow_ball", addonBall);
        CategoryAssignment chairAssignment = resolve("cobblefurnies:red_chair", cobblefurniesChair, ItemFacet.PLACEABLE);
        CategoryAssignment crafterAssignment = resolve("cobblefurnies:furnicrafter", furniCrafter, ItemFacet.PLACEABLE, ItemFacet.HAS_BLOCK_ENTITY);
        CategoryAssignment megaAssignment = resolve("mega_showdown:absolite", megaStone);
        CategoryAssignment zAssignment = resolve("mega_showdown:normalium_z", zCrystal);
        CategoryAssignment badgeAssignment = resolve("badgebox:fire_badge", badge);

        assertEquals("cobblemon", addonBall.get(SearchNodeKeys.COMPAT_FAMILY));
        assertEquals("cobblemon", addonBall.get(SearchNodeKeys.PRIMARY_COMPAT_FAMILY));
        assertEquals("cobblemon", addonBall.get(SearchNodeKeys.COMPAT_FAMILIES));
        assertEquals("poke_ball", addonBall.get(SearchNodeKeys.COBBLEMON_ITEM_KIND));
        assertEquals("shadow", addonBall.get(SearchNodeKeys.POKEMON_BALL_TIER));
        assertEquals("cobblemon", assignment.categoryId());
        assertEquals("poke_balls", assignment.subcategoryId());

        assertEquals("cobblemon", cobblefurniesChair.get(SearchNodeKeys.COMPAT_FAMILY));
        assertEquals("decor", cobblefurniesChair.get(SearchNodeKeys.COBBLEMON_ITEM_KIND));
        assertEquals("cobblemon", chairAssignment.categoryId());
        assertEquals("decor", chairAssignment.subcategoryId());

        assertEquals("machine", furniCrafter.get(SearchNodeKeys.COBBLEMON_ITEM_KIND));
        assertEquals("cobblemon", crafterAssignment.categoryId());
        assertEquals("machines", crafterAssignment.subcategoryId());

        assertEquals("evolution_item", megaStone.get(SearchNodeKeys.COBBLEMON_ITEM_KIND));
        assertEquals("cobblemon", megaAssignment.categoryId());
        assertEquals("evolution", megaAssignment.subcategoryId());

        assertEquals("held_item", zCrystal.get(SearchNodeKeys.COBBLEMON_ITEM_KIND));
        assertEquals("z_crystal", zCrystal.get(SearchNodeKeys.POKEMON_HELD_ITEM_ROLE));
        assertEquals("cobblemon", zAssignment.categoryId());
        assertEquals("held_items", zAssignment.subcategoryId());

        assertEquals("utility_item", badge.get(SearchNodeKeys.COBBLEMON_ITEM_KIND));
        assertEquals("cobblemon", badgeAssignment.categoryId());
        assertEquals("utility", badgeAssignment.subcategoryId());
    }

    @Test
    void compatFamilyDetectorCanRepresentHybridCreateCobblemonAddons() {
        Map<String, String> pokeBallPress = new HashMap<>();
        pokeBallPress.put(SearchNodeKeys.CREATIVE_TAB_LABEL, "Create Cobblemon");
        pokeBallPress.put(SearchNodeKeys.ITEM_CLASS, "com.example.createcobblemon.PokeBallPressBlockItem");
        CompatFamilyDetector.detect(new Identifier("createcobblemon", "poke_ball_press"), pokeBallPress);

        assertEquals("cobblemon,create", pokeBallPress.get(SearchNodeKeys.COMPAT_FAMILIES));
        assertEquals("cobblemon", pokeBallPress.get(SearchNodeKeys.PRIMARY_COMPAT_FAMILY));

        Map<String, String> pokemonCasing = new HashMap<>();
        pokemonCasing.put(SearchNodeKeys.CREATIVE_TAB_LABEL, "Create Cobblemon");
        pokemonCasing.put(SearchNodeKeys.ITEM_CLASS, "com.example.createaddon.CreateCasingBlockItem");
        CompatFamilyDetector.detect(new Identifier("createcobblemon", "pokemon_casing"), pokemonCasing);

        assertEquals("create,cobblemon", pokemonCasing.get(SearchNodeKeys.COMPAT_FAMILIES));
        assertEquals("create", pokemonCasing.get(SearchNodeKeys.PRIMARY_COMPAT_FAMILY));
    }

    @Test
    void compatFamilyDetectorDoesNotTreatOmegaAsMegaEvolutionEvidence() {
        Map<String, String> omegaUpgrade = new HashMap<>();
        omegaUpgrade.put(SearchNodeKeys.MOD_ID, "sophisticatedbackpacks");
        omegaUpgrade.put(SearchNodeKeys.CREATIVE_TAB_LABEL, "Sophisticated Backpacks");
        omegaUpgrade.put(SearchNodeKeys.ITEM_CLASS, "net.p3pp3rf1y.sophisticatedcore.upgrades.stack.StackUpgradeItem");
        omegaUpgrade.put(SearchNodeKeys.TAGS, "sophisticatedbackpacks:upgrade");

        CompatFamilyDetector.detect(new Identifier("sophisticatedbackpacks", "stack_upgrade_omega_tier"), omegaUpgrade);

        assertEquals("sophisticated", omegaUpgrade.getOrDefault(SearchNodeKeys.COMPAT_FAMILIES, ""));
        assertEquals("sophisticated", omegaUpgrade.getOrDefault(SearchNodeKeys.PRIMARY_COMPAT_FAMILY, ""));
        assertEquals("sophisticated", omegaUpgrade.getOrDefault(SearchNodeKeys.COMPAT_FAMILY, ""));

        CobblemonCompat.enrichItem(new Identifier("sophisticatedbackpacks", "stack_upgrade_omega_tier"), omegaUpgrade);
        assertEquals("", omegaUpgrade.getOrDefault(SearchNodeKeys.COBBLEMON_ITEM_KIND, ""));
    }

    @Test
    void cobblemonUtilityTagsDoNotClaimVanillaItems() {
        Map<String, String> shulkerBox = new HashMap<>();
        shulkerBox.put(SearchNodeKeys.MOD_ID, "minecraft");
        shulkerBox.put(SearchNodeKeys.CREATIVE_TAB_ID, "minecraft:colored_blocks");
        shulkerBox.put(SearchNodeKeys.CREATIVE_TAB_LABEL, "Colored Blocks");
        shulkerBox.put(SearchNodeKeys.ITEM_CLASS, "net.minecraft.world.item.BlockItem");
        shulkerBox.put(SearchNodeKeys.BLOCK_CLASS, "net.minecraft.world.level.block.ShulkerBoxBlock");
        shulkerBox.put(SearchNodeKeys.TAGS, "c:shulker_boxes,cobblemon:held/blacklisted_items_to_hold,cobblemon:held/container_held_items");

        Identifier id = new Identifier("minecraft", "magenta_shulker_box");
        CompatFamilyDetector.detect(id, shulkerBox);
        CobblemonCompat.enrichItem(id, shulkerBox);

        assertEquals("", shulkerBox.getOrDefault(SearchNodeKeys.COMPAT_FAMILIES, ""));
        assertEquals("", shulkerBox.getOrDefault(SearchNodeKeys.PRIMARY_COMPAT_FAMILY, ""));
        assertEquals("", shulkerBox.getOrDefault(SearchNodeKeys.COBBLEMON_ITEM_KIND, ""));
        assertEquals("", shulkerBox.getOrDefault(SearchNodeKeys.SEARCH_TOKENS, ""));

        CategoryAssignment assignment = resolve(id.toString(), shulkerBox,
                ItemFacet.PLACEABLE,
                ItemFacet.HAS_BLOCK_ENTITY,
                ItemFacet.STORAGE,
                ItemFacet.PASSIVE_COMPARATOR_OUTPUT,
                ItemFacet.REDSTONE_SIGNAL);
        assertNotEquals("cobblemon", assignment.categoryId());
        assertTrue(assignment.attributes().getOrDefault(SearchNodeKeys.CLASSIFICATION_ROUTE, "").contains("input[minecraft:magenta_shulker_box"));
        assertTrue(!assignment.attributes().getOrDefault(SearchNodeKeys.CLASSIFICATION_ROUTE, "").contains("compat=cobblemon"));
    }

    @Test
    void pathOnlyThemedWordsDoNotClaimCompatFamilyOwnership() {
        Map<String, String> andesite = new HashMap<>();
        andesite.put(SearchNodeKeys.MOD_ID, "minecraft");
        andesite.put(SearchNodeKeys.CREATIVE_TAB_LABEL, "Building Blocks");
        andesite.put(SearchNodeKeys.ITEM_CLASS, "net.minecraft.world.item.BlockItem");
        CompatFamilyDetector.detect(new Identifier("minecraft", "andesite"), andesite);
        assertEquals("", andesite.getOrDefault(SearchNodeKeys.COMPAT_FAMILIES, ""));

        Map<String, String> ae2Press = new HashMap<>();
        ae2Press.put(SearchNodeKeys.MOD_ID, "ae2");
        ae2Press.put(SearchNodeKeys.CREATIVE_TAB_LABEL, "Applied Energistics 2");
        ae2Press.put(SearchNodeKeys.ITEM_CLASS, "appeng.items.materials.MaterialItem");
        CompatFamilyDetector.detect(new Identifier("ae2", "calculation_processor_press"), ae2Press);
        assertEquals("ae2", ae2Press.getOrDefault(SearchNodeKeys.COMPAT_FAMILIES, ""));
        assertTrue(!ae2Press.getOrDefault(SearchNodeKeys.COMPAT_FAMILIES, "").contains("create"));

        Map<String, String> ae2NamePress = new HashMap<>();
        ae2NamePress.put(SearchNodeKeys.MOD_ID, "ae2");
        ae2NamePress.put(SearchNodeKeys.CREATIVE_TAB_LABEL, "Applied Energistics 2");
        ae2NamePress.put(SearchNodeKeys.ITEM_CLASS, "appeng.items.materials.NamePressItem");
        CompatFamilyDetector.detect(new Identifier("ae2", "name_press"), ae2NamePress);
        assertEquals("ae2", ae2NamePress.getOrDefault(SearchNodeKeys.COMPAT_FAMILIES, ""));
        assertTrue(!ae2NamePress.getOrDefault(SearchNodeKeys.COMPAT_FAMILIES, "").contains("create"));

        Map<String, String> gtCasing = new HashMap<>();
        gtCasing.put(SearchNodeKeys.MOD_ID, "gtceu");
        gtCasing.put(SearchNodeKeys.CREATIVE_TAB_LABEL, "GTCEu Decoration Blocks");
        gtCasing.put(SearchNodeKeys.ITEM_CLASS, "net.minecraft.world.item.BlockItem");
        CompatFamilyDetector.detect(new Identifier("gtceu", "steam_machine_casing"), gtCasing);
        assertEquals("gregtech", gtCasing.getOrDefault(SearchNodeKeys.COMPAT_FAMILIES, ""));
        assertTrue(!gtCasing.getOrDefault(SearchNodeKeys.COMPAT_FAMILIES, "").contains("create"));

        Map<String, String> sweetBerryCheesecake = new HashMap<>();
        sweetBerryCheesecake.put(SearchNodeKeys.MOD_ID, "farmersdelight");
        sweetBerryCheesecake.put(SearchNodeKeys.CREATIVE_TAB_LABEL, "Farmer's Delight");
        sweetBerryCheesecake.put(SearchNodeKeys.ITEM_CLASS, "vectorwing.farmersdelight.common.item.PlaceableItem");
        CompatFamilyDetector.detect(new Identifier("farmersdelight", "sweet_berry_cheesecake"), sweetBerryCheesecake);
        CobblemonCompat.enrichItem(new Identifier("farmersdelight", "sweet_berry_cheesecake"), sweetBerryCheesecake);
        assertEquals("", sweetBerryCheesecake.getOrDefault(SearchNodeKeys.COMPAT_FAMILIES, ""));
        assertEquals("", sweetBerryCheesecake.getOrDefault(SearchNodeKeys.COBBLEMON_ITEM_KIND, ""));
    }

    @Test
    void ambiguousSingleTermsDoNotClaimFamilyOwnership() {
        for (String term : new String[]{
                "press", "casing", "cell", "drive", "gear", "plate", "core", "module",
                "terminal", "controller", "cable", "pipe", "tank", "berry", "gem",
                "stone", "map", "claim", "waypoint", "badge", "mega", "poke",
                "apricorn", "kinetic", "brass"
        }) {
            Map<String, String> meta = new HashMap<>();
            meta.put(SearchNodeKeys.MOD_ID, "examplemod");
            meta.put(SearchNodeKeys.CREATIVE_TAB_LABEL, "Example " + term);
            meta.put(SearchNodeKeys.ITEM_CLASS, "com.example." + term + ".ExampleItem");

            CompatFamilyDetector.detect(new Identifier("examplemod", term), meta);

            assertEquals("", meta.getOrDefault(SearchNodeKeys.COMPAT_FAMILIES, ""), term);
        }
    }

    @Test
    void observedCobblemonAddonNamespacesStillClaimCobblemonFamily() {
        Map<String, String> manufactory = new HashMap<>();
        manufactory.put(SearchNodeKeys.CREATIVE_TAB_LABEL, "Cobblemon Manufactory");
        CompatFamilyDetector.detect(new Identifier("cobblemon_manufactory", "ancient_black_ball_lid"), manufactory);
        assertTrue(CompatFamilyDetector.hasFamily(manufactory, CompatFamilyDetector.COBBLEMON));

        Map<String, String> cobblemore = new HashMap<>();
        cobblemore.put(SearchNodeKeys.CREATIVE_TAB_LABEL, "Cobblemore Ball Lids");
        CompatFamilyDetector.detect(new Identifier("cobblemore_lib", "ancient_poke_ball_lid"), cobblemore);
        assertTrue(CompatFamilyDetector.hasFamily(cobblemore, CompatFamilyDetector.COBBLEMON));
    }

    @Test
    void cobblemonAddonBallPartsAndApricornPiecesUseFocusedBuckets() {
        Map<String, String> lid = enriched("cobblemon_manufactory:ancient_ultra_ball_lid", "Cobblemon Manufactory",
                "net.minecraft.world.item.Item");
        Map<String, String> tumblestoneLid = enriched("cobblemon_manufactory:tumblestone_lid", "Cobblemon Manufactory",
                "net.minecraft.world.item.Item");
        Map<String, String> base = enriched("createmonballsoverhaul:copper_ball_base", "Create Cobblemon",
                "net.minecraft.world.item.Item");
        Map<String, String> stampedLid = enriched("createmonballsoverhaul:stamped_zinc_nugget_lid", "Create Cobblemon",
                "net.minecraft.world.item.Item");
        Map<String, String> apricornPunch = enriched("createmonballsoverhaul:apricorn_punch", "Create Cobblemon",
                "net.xkcinnay.createmonballsoverhaul.item.ModItems$1");
        Map<String, String> halfApricorn = enriched("createmonballsoverhaul:half_red_apricorn", "Create Cobblemon",
                "net.minecraft.world.item.Item");
        Map<String, String> apricornBits = enriched("createmonballsoverhaul:red_apricorn_bits", "Create Cobblemon",
                "net.minecraft.world.item.Item");

        assertEquals("poke_ball", lid.get(SearchNodeKeys.COBBLEMON_ITEM_KIND));
        assertEquals("ultra", lid.get(SearchNodeKeys.POKEMON_BALL_TIER));
        assertEquals("poke_balls", resolve("cobblemon_manufactory:ancient_ultra_ball_lid", lid).subcategoryId());
        assertEquals("poke_ball", tumblestoneLid.get(SearchNodeKeys.COBBLEMON_ITEM_KIND));
        assertEquals("poke_balls", resolve("cobblemon_manufactory:tumblestone_lid", tumblestoneLid).subcategoryId());
        assertEquals("poke_ball", base.get(SearchNodeKeys.COBBLEMON_ITEM_KIND));
        assertEquals("poke_balls", resolve("createmonballsoverhaul:copper_ball_base", base).subcategoryId());
        assertEquals("poke_ball", stampedLid.get(SearchNodeKeys.COBBLEMON_ITEM_KIND));
        assertEquals("poke_balls", resolve("createmonballsoverhaul:stamped_zinc_nugget_lid", stampedLid).subcategoryId());
        assertEquals("poke_ball", apricornPunch.get(SearchNodeKeys.COBBLEMON_ITEM_KIND));
        assertEquals("poke_balls", resolve("createmonballsoverhaul:apricorn_punch", apricornPunch).subcategoryId());
        assertEquals("apricorn", halfApricorn.get(SearchNodeKeys.COBBLEMON_ITEM_KIND));
        assertEquals("red", halfApricorn.get(SearchNodeKeys.COLOR_BUCKET));
        assertEquals("apricorns", resolve("createmonballsoverhaul:half_red_apricorn", halfApricorn).subcategoryId());
        assertEquals("apricorn", apricornBits.get(SearchNodeKeys.COBBLEMON_ITEM_KIND));
        assertEquals("apricorns", resolve("createmonballsoverhaul:red_apricorn_bits", apricornBits).subcategoryId());
    }

    @Test
    void cobblemonAddonResourceOddballsAvoidMiscWhenTheyHaveClearPokemonMeaning() {
        Map<String, String> tumblestoneDust = enriched("createmonballsoverhaul:tumblestone_dust", "Create Cobblemon",
                "net.minecraft.world.item.Item");
        Map<String, String> coating = enriched("createmonballsoverhaul:standard_tumblestone_coating_bucket", "Create Cobblemon",
                "net.minecraft.world.item.BucketItem");
        Map<String, String> candyOre = enriched("cobblemore_lib:candy_ore", "Cobblemore",
                "com.cobblemore_lib.items.ores.CandyOreBlockItem");
        Map<String, String> expQuartz = enriched("cobblemon_manufactory:exp_quartz_tiles", "Cobblemon Manufactory",
                "net.minecraft.world.item.BlockItem");
        Map<String, String> pedestal = enriched("mega_showdown:pedestal", "Mega Showdown",
                "com.github.yajatkaul.mega_showdown.block.MegaShowdownBlocks$1");

        assertEquals("archaeology", tumblestoneDust.get(SearchNodeKeys.COBBLEMON_ITEM_KIND));
        assertEquals("archaeology", resolve("createmonballsoverhaul:tumblestone_dust", tumblestoneDust).subcategoryId());
        assertEquals("archaeology", coating.get(SearchNodeKeys.COBBLEMON_ITEM_KIND));
        assertEquals("archaeology", resolve("createmonballsoverhaul:standard_tumblestone_coating_bucket", coating).subcategoryId());
        assertEquals("archaeology", candyOre.get(SearchNodeKeys.COBBLEMON_ITEM_KIND));
        assertEquals("archaeology", resolve("cobblemore_lib:candy_ore", candyOre).subcategoryId());
        assertEquals("building", expQuartz.get(SearchNodeKeys.COBBLEMON_ITEM_KIND));
        assertEquals("building", resolve("cobblemon_manufactory:exp_quartz_tiles", expQuartz).subcategoryId());
        assertEquals("decor", pedestal.get(SearchNodeKeys.COBBLEMON_ITEM_KIND));
        assertEquals("decor", resolve("mega_showdown:pedestal", pedestal).subcategoryId());
    }

    @Test
    void cobblemonPotionBucketsUseMedicineBucket() {
        Map<String, String> hyperPotion = enriched("create_cobblemon_potion:hyper_potion_bucket", "Create Cobblemon Potion",
                "net.minecraft.world.item.BucketItem");
        Map<String, String> antidote = enriched("create_cobblemon_potion:antidote_bucket", "Create Cobblemon Potion",
                "net.minecraft.world.item.BucketItem");

        assertEquals("medicine", hyperPotion.get(SearchNodeKeys.COBBLEMON_ITEM_KIND));
        assertEquals("heal", hyperPotion.get(SearchNodeKeys.POKEMON_MEDICINE_KIND));
        assertEquals("120", hyperPotion.get(SearchNodeKeys.POKEMON_HEALING));
        assertEquals("medicine", resolve("create_cobblemon_potion:hyper_potion_bucket", hyperPotion).subcategoryId());
        assertEquals("medicine", antidote.get(SearchNodeKeys.COBBLEMON_ITEM_KIND));
        assertEquals("poison", antidote.get(SearchNodeKeys.POKEMON_STATUS_CURE));
    }

    @Test
    void pathStrongMedicineItemsWorkWithoutConsumablesTab() {
        Map<String, String> candy = enriched("cobblemon:courage_candy", "KubeJS",
                "dev.latvian.mods.kubejs.item.custom.BasicItemJS");
        Map<String, String> mochi = enriched("cobblemon:health_mochi", "KubeJS",
                "dev.latvian.mods.kubejs.item.custom.BasicItemJS");
        Map<String, String> mint = enriched("cobblemon:gentle_mint", "KubeJS",
                "dev.latvian.mods.kubejs.item.custom.BasicItemJS");
        Map<String, String> vitamin = enriched("cobblemon:protein", "KubeJS",
                "dev.latvian.mods.kubejs.item.custom.BasicItemJS");

        CategoryAssignment candyAssignment = resolve("cobblemon:courage_candy", candy);
        CategoryAssignment mochiAssignment = resolve("cobblemon:health_mochi", mochi);
        CategoryAssignment mintAssignment = resolve("cobblemon:gentle_mint", mint);
        CategoryAssignment vitaminAssignment = resolve("cobblemon:protein", vitamin);

        assertEquals("medicine", candy.get(SearchNodeKeys.COBBLEMON_ITEM_KIND));
        assertEquals("cobblemon", candyAssignment.categoryId());
        assertEquals("medicine", candyAssignment.subcategoryId());
        assertEquals("medicine", mochi.get(SearchNodeKeys.COBBLEMON_ITEM_KIND));
        assertEquals("cobblemon", mochiAssignment.categoryId());
        assertEquals("medicine", mochiAssignment.subcategoryId());
        assertEquals("medicine", mint.get(SearchNodeKeys.COBBLEMON_ITEM_KIND));
        assertEquals("cobblemon", mintAssignment.categoryId());
        assertEquals("medicine", mintAssignment.subcategoryId());
        assertEquals("medicine", vitamin.get(SearchNodeKeys.COBBLEMON_ITEM_KIND));
        assertEquals("cobblemon", vitaminAssignment.categoryId());
        assertEquals("medicine", vitaminAssignment.subcategoryId());
    }

    @Test
    void realCandyItemClassCandiesStayInConsumables() {
        Map<String, String> rare = enriched("cobblemon:rare_candy", "Cobblemon: Consumables",
                "com.cobblemon.mod.common.item.interactive.CandyItem");
        Map<String, String> expCandy = enriched("cobblemon:exp_candy_xl", "Cobblemon: Consumables",
                "com.cobblemon.mod.common.item.interactive.CandyItem");

        CategoryAssignment rareAssignment = resolve("cobblemon:rare_candy", rare);
        CategoryAssignment expAssignment = resolve("cobblemon:exp_candy_xl", expCandy);

        assertEquals("consumable", rare.get(SearchNodeKeys.COBBLEMON_ITEM_KIND));
        assertEquals("cobblemon", rareAssignment.categoryId());
        assertEquals("consumables", rareAssignment.subcategoryId());
        assertEquals("consumable", expCandy.get(SearchNodeKeys.COBBLEMON_ITEM_KIND));
        assertEquals("cobblemon", expAssignment.categoryId());
        assertEquals("consumables", expAssignment.subcategoryId());
    }

    @Test
    void megaShowdownUtilitiesConsumablesAndHeldItemsAvoidCobblemonMisc() {
        Map<String, String> zRing = enriched("mega_showdown:z_ring", "Mega Showdown",
                "com.github.yajatkaul.mega_showdown.item.ZRingItem",
                Map.of(SearchNodeKeys.TAGS, "accessories:z_slot,mega_showdown:z_ring"));
        Map<String, String> dynamaxCandy = enriched("mega_showdown:dynamax_candy", "Mega Showdown",
                "com.github.yajatkaul.mega_showdown.item.DynamaxCandyItem");
        Map<String, String> boosterEnergy = enriched("mega_showdown:booster_energy", "Mega Showdown",
                "com.github.yajatkaul.mega_showdown.item.BoosterEnergyItem");

        assertEquals("utility_item", zRing.get(SearchNodeKeys.COBBLEMON_ITEM_KIND));
        assertEquals("utility", resolve("mega_showdown:z_ring", zRing).subcategoryId());
        assertEquals("consumable", dynamaxCandy.get(SearchNodeKeys.COBBLEMON_ITEM_KIND));
        assertEquals("consumables", resolve("mega_showdown:dynamax_candy", dynamaxCandy).subcategoryId());
        assertEquals("held_item", boosterEnergy.get(SearchNodeKeys.COBBLEMON_ITEM_KIND));
        assertEquals("held_items", resolve("mega_showdown:booster_energy", boosterEnergy).subcategoryId());
    }

    @Test
    void knownMapModsUseGeneralMappingFamilyWithoutClaimingVanillaMaps() {
        Map<String, String> journeyMapWaypoint = new HashMap<>();
        journeyMapWaypoint.put(SearchNodeKeys.MOD_ID, "journeymap");
        journeyMapWaypoint.put(SearchNodeKeys.CREATIVE_TAB_LABEL, "JourneyMap");
        CompatFamilyDetector.detect(new Identifier("journeymap", "waypoint_manager"), journeyMapWaypoint);

        assertEquals("mapping", journeyMapWaypoint.get(SearchNodeKeys.COMPAT_FAMILY));
        assertEquals("mapping", journeyMapWaypoint.get(SearchNodeKeys.PRIMARY_COMPAT_FAMILY));
        assertEquals("mapping", journeyMapWaypoint.get(SearchNodeKeys.COMPAT_FAMILIES));

        Map<String, String> vanillaMap = new HashMap<>();
        vanillaMap.put(SearchNodeKeys.MOD_ID, "minecraft");
        vanillaMap.put(SearchNodeKeys.CREATIVE_TAB_LABEL, "Tools & Utilities");
        CompatFamilyDetector.detect(new Identifier("minecraft", "filled_map"), vanillaMap);

        assertEquals("", vanillaMap.getOrDefault(SearchNodeKeys.COMPAT_FAMILIES, ""));

        Map<String, String> ftbQuestBook = new HashMap<>();
        ftbQuestBook.put(SearchNodeKeys.MOD_ID, "ftbquests");
        ftbQuestBook.put(SearchNodeKeys.CREATIVE_TAB_LABEL, "FTB Quests");
        CompatFamilyDetector.detect(new Identifier("ftbquests", "book"), ftbQuestBook);

        assertEquals("", ftbQuestBook.getOrDefault(SearchNodeKeys.COMPAT_FAMILIES, ""));
    }

    @Test
    void semanticCobblemonPolicyLetsStrongItemIdentityWin() {
        AmiConfig.CompatCategoryPolicy oldPolicy = AmiConfig.cobblemonCategoryPolicy;
        try {
            AmiConfig.cobblemonCategoryPolicy = AmiConfig.CompatCategoryPolicy.SEMANTIC;
            Map<String, String> meta = enriched("cobblemon:training_sword", "Cobblemon: Tools",
                    "com.cobblemon.mod.common.item.CobblemonItem");

            CategoryAssignment assignment = resolve("cobblemon:training_sword", meta, ItemFacet.MELEE_WEAPON);

            assertEquals("semantic", assignment.attributes().get(SearchNodeKeys.COMPAT_CATEGORY_POLICY));
            assertEquals("tools", assignment.categoryId());
            assertEquals("melee", assignment.subcategoryId());
            assertTrue(assignment.attributes().getOrDefault(SearchNodeKeys.CLASSIFICATION_ROUTE, "").contains("policy=semantic"));
            assertTrue(assignment.attributes().getOrDefault(SearchNodeKeys.CLASSIFICATION_ROUTE, "").contains("hard_identity:identity[tools/melee]"));
        } finally {
            AmiConfig.cobblemonCategoryPolicy = oldPolicy;
        }
    }

    @Test
    void cobblemonAddonTmsFarmersDexAndEggUseFocusedSubcategories() {
        Map<String, String> tm = enriched("simpletms:tm_thunderbolt", "SimpleTMs: TMs",
                "git.dragomordor.simpletms.forge.item.custom.MoveTutorItem");
        Map<String, String> tr = enriched("simpletms:tr_ice_beam", "SimpleTMs: TRs",
                "git.dragomordor.simpletms.forge.item.custom.MoveTutorItem");
        Map<String, String> blankTm = enriched("simpletms:tm_blank", "SimpleTMs: TMs",
                "git.dragomordor.simpletms.forge.item.custom.BlankTMItem");
        Map<String, String> worker = enriched("cobblemon_farmers:fire_type_worker", "Cobblemon Farmers",
                "net.minecraft.world.item.Item");
        Map<String, String> station = enriched("cobblemon_farmers:gardening_station", "Cobblemon Farmers",
                "net.minecraft.world.item.BlockItem");
        Map<String, String> cobbledex = enriched("cobbledex:cobbledex_item", "Tools & Utilities",
                "com.rafacasari.mod.cobbledex.items.CobbledexItem");
        Map<String, String> pokemonEgg = enriched("cobbreeding:pokemon_egg", "",
                "ludichat.cobbreeding.PokemonEgg");

        CategoryAssignment tmAssignment = resolve("simpletms:tm_thunderbolt", tm);
        CategoryAssignment trAssignment = resolve("simpletms:tr_ice_beam", tr);
        CategoryAssignment blankAssignment = resolve("simpletms:tm_blank", blankTm);
        CategoryAssignment workerAssignment = resolve("cobblemon_farmers:fire_type_worker", worker);
        CategoryAssignment stationAssignment = resolve("cobblemon_farmers:gardening_station", station);
        CategoryAssignment dexAssignment = resolve("cobbledex:cobbledex_item", cobbledex);
        CategoryAssignment eggAssignment = resolve("cobbreeding:pokemon_egg", pokemonEgg);

        assertEquals("tm", tm.get(SearchNodeKeys.COBBLEMON_ITEM_KIND));
        assertEquals("cobblemon", tmAssignment.categoryId());
        assertEquals("tms", tmAssignment.subcategoryId());
        assertEquals("tm", tr.get(SearchNodeKeys.COBBLEMON_ITEM_KIND));
        assertEquals("cobblemon", trAssignment.categoryId());
        assertEquals("tms", trAssignment.subcategoryId());
        assertEquals("tm", blankTm.get(SearchNodeKeys.COBBLEMON_ITEM_KIND));
        assertEquals("tms", blankAssignment.subcategoryId());

        assertEquals("utility_item", worker.get(SearchNodeKeys.COBBLEMON_ITEM_KIND));
        assertEquals("cobblemon", workerAssignment.categoryId());
        assertEquals("utility", workerAssignment.subcategoryId());
        assertEquals("machine", station.get(SearchNodeKeys.COBBLEMON_ITEM_KIND));
        assertEquals("cobblemon", stationAssignment.categoryId());
        assertEquals("machines", stationAssignment.subcategoryId());

        assertEquals("utility_item", cobbledex.get(SearchNodeKeys.COBBLEMON_ITEM_KIND));
        assertEquals("cobblemon", dexAssignment.categoryId());
        assertEquals("utility", dexAssignment.subcategoryId());

        assertEquals("agriculture", pokemonEgg.get(SearchNodeKeys.COBBLEMON_ITEM_KIND));
        assertEquals("cobblemon", eggAssignment.categoryId());
        assertEquals("agriculture", eggAssignment.subcategoryId());
    }

    @Test
    void hybridCobblemonPolicyDoesNotFocusGenericDisplayItems() {
        AmiConfig.CompatCategoryPolicy oldPolicy = AmiConfig.cobblemonCategoryPolicy;
        try {
            AmiConfig.cobblemonCategoryPolicy = AmiConfig.CompatCategoryPolicy.HYBRID;
            Map<String, String> meta = enriched("cobblemon:display_case", "Cobblemon: Colored Blocks",
                    "net.minecraft.world.item.BlockItem");

            CategoryAssignment assignment = resolve("cobblemon:display_case", meta, ItemFacet.PLACEABLE);

            assertEquals("hybrid", assignment.attributes().get(SearchNodeKeys.COMPAT_CATEGORY_POLICY));
            assertEquals("decor", meta.get(SearchNodeKeys.COBBLEMON_ITEM_KIND));
            assertNotEquals("cobblemon", assignment.categoryId());
            assertTrue(assignment.attributes().getOrDefault(SearchNodeKeys.CLASSIFICATION_ROUTE, "").contains("policy=hybrid"));
        } finally {
            AmiConfig.cobblemonCategoryPolicy = oldPolicy;
        }
    }

    private static Map<String, String> enriched(String id, String creativeTab, String itemClass) {
        return enriched(id, creativeTab, itemClass, Map.of());
    }

    private static Map<String, String> enriched(String id, String creativeTab, String itemClass, Map<String, String> extra) {
        Map<String, String> meta = new HashMap<>();
        meta.put(SearchNodeKeys.CREATIVE_TAB_LABEL, creativeTab);
        if (!itemClass.isBlank()) {
            meta.put(SearchNodeKeys.ITEM_CLASS, itemClass);
        }
        meta.putAll(extra);
        CobblemonCompat.enrichItem(new Identifier(id), meta);
        return meta;
    }

    private static CategoryAssignment resolve(String id, Map<String, String> meta, ItemFacet... facets) {
        return PrimaryCategoryResolver.resolve(
                new Identifier(id),
                new FacetProfile(facets.length == 0 ? EnumSet.noneOf(ItemFacet.class) : EnumSet.of(facets[0], facets), meta)
        );
    }
}
