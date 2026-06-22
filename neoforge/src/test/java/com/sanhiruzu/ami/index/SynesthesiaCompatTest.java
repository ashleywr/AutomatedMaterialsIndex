package com.sanhiruzu.ami.index;

import com.sanhiruzu.ami.compat.DatanessenceCompat;
import com.sanhiruzu.ami.compat.DoggyTalentsCompat;
import com.sanhiruzu.ami.compat.EnigmaticLegacyPlusCompat;
import com.sanhiruzu.ami.compat.EternalStarlightCompat;
import com.sanhiruzu.ami.compat.ForbiddenArcanusCompat;
import com.sanhiruzu.ami.compat.GeneratedVariantCollapseCompat;
import com.sanhiruzu.ami.compat.HexaliaCompat;
import com.sanhiruzu.ami.compat.HexereiCompat;
import com.sanhiruzu.ami.compat.MalumCompat;
import com.sanhiruzu.ami.compat.PastelCompat;
import com.sanhiruzu.ami.compat.PowerGridCompat;
import com.sanhiruzu.ami.compat.SilentGemsCompat;
import com.sanhiruzu.ami.compat.SwemCompat;
import com.sanhiruzu.ami.compat.TideCompat;
import com.sanhiruzu.ami.compat.WitcheryCompat;
import com.sanhiruzu.ami.compat.ZenColonyCompat;
import net.minecraft.resources.ResourceLocation;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import java.util.EnumSet;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SynesthesiaCompatTest {
    @Test
    void swemMetalComponentsNoLongerFallBackUnknown() {
        Map<String, String> meta = meta("swem", "net.minecraft.world.item.Item");

        SwemCompat.enrichItem(new ResourceLocation("swem", "rivet_copper"), meta);
        CategoryAssignment assignment = resolve("swem:rivet_copper", meta);

        assertEquals("components", meta.get("swemItemKind"));
        assertTrue(meta.getOrDefault(SearchNodeKeys.FACETS, "").contains(ItemFacet.TECH_COMPONENT.id()));
        assertEquals("ingredients", assignment.categoryId());
        assertEquals("mineral", assignment.subcategoryId());
    }

    @Test
    void swemHorseSpecificItemsPreferSwemHeaderOverGenericRoutes() {
        Map<String, String> armor = meta("swem", "com.alaharranhonor.swem.item.tack.TackItem");
        armor.put(SearchNodeKeys.FACETS, "armor_animal");
        SwemCompat.enrichItem(new ResourceLocation("swem", "horse_armor_cloth"), armor);
        CategoryAssignment armorAssignment = resolve("swem:horse_armor_cloth", armor);

        Map<String, String> ridingHelmet = meta("swem", "com.alaharranhonor.swem.item.armor.RidingHelmet");
        ridingHelmet.put(SearchNodeKeys.FACETS, "equippable,armor_head");
        SwemCompat.enrichItem(new ResourceLocation("swem", "helmet_riding"), ridingHelmet);
        CategoryAssignment helmetAssignment = resolve("swem:helmet_riding", ridingHelmet);

        Map<String, String> feeder = meta("swem", "net.minecraft.world.item.BlockItem");
        feeder.put(SearchNodeKeys.BLOCK_CLASS, "com.alaharranhonor.swem.block.GrainBinBlock");
        feeder.put(SearchNodeKeys.FACETS, "placeable,has_block_entity");
        feeder.put(SearchNodeKeys.TAGS, "swem:grain_bins");
        SwemCompat.enrichItem(new ResourceLocation("swem", "bin_grain_white"), feeder);
        CategoryAssignment feederAssignment = resolve("swem:bin_grain_white", feeder);

        Map<String, String> jump = meta("swem", "com.alaharranhonor.swem.item.EggJumpItem");
        SwemCompat.enrichItem(new ResourceLocation("swem", "jump_xc_bronze"), jump);
        CategoryAssignment jumpAssignment = resolve("swem:jump_xc_bronze", jump);

        assertEquals("horse_armor", armor.get("swemItemKind"));
        assertEquals("swem", armorAssignment.categoryId());
        assertEquals("horse_armor", armorAssignment.subcategoryId());
        assertEquals("swem", helmetAssignment.categoryId());
        assertEquals("riding_gear", helmetAssignment.subcategoryId());
        assertEquals("swem", feederAssignment.categoryId());
        assertEquals("feed", feederAssignment.subcategoryId());
        assertEquals("swem", jumpAssignment.categoryId());
        assertEquals("stable", jumpAssignment.subcategoryId());
    }

    @Test
    void malumGeasRoutesToMagicAndCollapses() {
        Map<String, String> meta = meta("malum", "com.sammy.malum.common.item.GeasItem");
        meta.put(SearchNodeKeys.SUBTYPE_OF, "malum:geas");

        MalumCompat.enrichItem(new ResourceLocation("malum", "geas/variant/geas_b28b0afe76f1"), meta);
        CategoryAssignment assignment = resolve("malum:geas/variant/geas_b28b0afe76f1", meta);

        assertEquals("geasa", meta.get("malumItemKind"));
        assertEquals("malum:geas", meta.get(SearchNodeKeys.COLLAPSE_FAMILY));
        assertEquals("malum", assignment.categoryId());
        assertEquals("geasa", assignment.subcategoryId());
    }

    @Test
    void pastelStructurePlacersRouteToUtility() {
        Map<String, String> meta = meta("pastel", "earth.terrarium.pastel.items.StructurePlacerItem");

        PastelCompat.enrichItem(new ResourceLocation("pastel", "fusion_shrine_structure_placer"), meta);
        CategoryAssignment assignment = resolve("pastel:fusion_shrine_structure_placer", meta);

        assertEquals("structure_placers", meta.get("pastelItemKind"));
        assertEquals("pastel", assignment.categoryId());
        assertEquals("structures", assignment.subcategoryId());
    }

    @Test
    void pastelDecayBottlesRouteToPastelMagic() {
        Map<String, String> meta = meta("pastel", "earth.terrarium.pastel.items.DecayPlacerItem");
        meta.put(SearchNodeKeys.BLOCK_TAGS, "pastel:decay/decay,pastel:decay/decay_away");

        PastelCompat.enrichItem(new ResourceLocation("pastel", "bottle_of_fading"), meta);
        CategoryAssignment assignment = resolve("pastel:bottle_of_fading", meta);

        assertEquals("decay_magic", meta.get("pastelItemKind"));
        assertTrue(meta.getOrDefault(SearchNodeKeys.FACETS, "").contains(ItemFacet.MAGIC_ARTIFACT.id()));
        assertEquals("pastel", assignment.categoryId());
        assertEquals("magic", assignment.subcategoryId());
    }

    @Test
    @Disabled("intended routing pending override layer; see docs/superpowers/specs/2026-06-22-classification-override-and-curation-design.md")
    void doggyTalentsRepeatedUnknownFamiliesRouteSemantically() {
        Map<String, String> treat = meta("doggytalents", "doggytalents.common.item.TreatItem");
        treat.put(SearchNodeKeys.TAGS, "doggytalents:treats");
        DoggyTalentsCompat.enrichItem(new ResourceLocation("doggytalents", "training_treat"), treat);
        CategoryAssignment treatAssignment = resolve("doggytalents:training_treat", treat);

        Map<String, String> tracker = meta("doggytalents", "doggytalents.common.item.CanineTrackerItem");
        DoggyTalentsCompat.enrichItem(new ResourceLocation("doggytalents", "canine_tracker"), tracker);
        CategoryAssignment trackerAssignment = resolve("doggytalents:canine_tracker", tracker);

        Map<String, String> toy = meta("doggytalents", "doggytalents.common.item.FrisbeeItem");
        DoggyTalentsCompat.enrichItem(new ResourceLocation("doggytalents", "frisbee"), toy);
        CategoryAssignment toyAssignment = resolve("doggytalents:frisbee", toy);

        Map<String, String> decor = meta("doggytalents", "doggytalents.common.item.PianoItem");
        DoggyTalentsCompat.enrichItem(new ResourceLocation("doggytalents", "grand_piano_white_item"), decor);
        CategoryAssignment decorAssignment = resolve("doggytalents:grand_piano_white_item", decor);

        Map<String, String> accessory = meta("doggytalents", "doggytalents.common.entity.accessory.FlatCap$FlatCapItem");
        DoggyTalentsCompat.enrichItem(new ResourceLocation("doggytalents", "flatcap"), accessory);
        CategoryAssignment accessoryAssignment = resolve("doggytalents:flatcap", accessory);

        assertEquals("treat", treat.get("doggyTalentsItemKind"));
        assertEquals("nature", treatAssignment.categoryId());
        assertEquals("snacks", treatAssignment.subcategoryId());
        assertEquals("tracker", tracker.get("doggyTalentsItemKind"));
        assertEquals("utility", trackerAssignment.categoryId());
        assertEquals("navigation", trackerAssignment.subcategoryId());
        assertEquals("toy_tool", toy.get("doggyTalentsItemKind"));
        assertEquals("tools", toyAssignment.categoryId());
        assertEquals("utility", toyAssignment.subcategoryId());
        assertEquals("decor", decor.get("doggyTalentsItemKind"));
        assertEquals("decoration", decorAssignment.categoryId());
        assertEquals("furniture", decorAssignment.subcategoryId());
        assertEquals("pet_accessory", accessory.get("doggyTalentsItemKind"));
        assertTrue(accessory.getOrDefault(SearchNodeKeys.FACETS, "").contains(ItemFacet.CURIO.id()));
        assertEquals("armor", accessoryAssignment.categoryId());
        assertEquals("curios", accessoryAssignment.subcategoryId());
    }

    @Test
    void hexereiUnknownFamiliesGainSemanticFacts() {
        Map<String, String> broom = meta("hexerei", "net.joefoxe.hexerei.item.ModItems$4");
        HexereiCompat.enrichItem(new ResourceLocation("hexerei", "mahogany_broom"), broom);
        CategoryAssignment broomAssignment = resolve("hexerei:mahogany_broom", broom);

        Map<String, String> herb = meta("hexerei", "net.minecraft.world.item.Item");
        herb.put(SearchNodeKeys.TAGS, "hexerei:herbs");
        HexereiCompat.enrichItem(new ResourceLocation("hexerei", "sage"), herb);
        CategoryAssignment herbAssignment = resolve("hexerei:sage", herb);

        Map<String, String> sigil = meta("hexerei", "net.minecraft.world.item.Item");
        sigil.put(SearchNodeKeys.TAGS, "hexerei:sigils");
        HexereiCompat.enrichItem(new ResourceLocation("hexerei", "blood_sigil"), sigil);
        CategoryAssignment sigilAssignment = resolve("hexerei:blood_sigil", sigil);

        Map<String, String> rod = meta("hexerei", "net.joefoxe.hexerei.item.custom.DowsingRodItem");
        HexereiCompat.enrichItem(new ResourceLocation("hexerei", "dowsing_rod"), rod);
        CategoryAssignment rodAssignment = resolve("hexerei:dowsing_rod", rod);

        assertEquals("broom_tool", broom.get("hexereiItemKind"));
        assertEquals("tech", broomAssignment.categoryId());
        assertEquals("transport", broomAssignment.subcategoryId());
        assertEquals("organic_reagent", herb.get("hexereiItemKind"));
        assertEquals("ingredients", herbAssignment.categoryId());
        assertEquals("organic", herbAssignment.subcategoryId());
        assertEquals("magic_artifact", sigil.get("hexereiItemKind"));
        assertEquals("magic", sigilAssignment.categoryId());
        assertEquals("artifacts", sigilAssignment.subcategoryId());
        assertEquals("navigation_tool", rod.get("hexereiItemKind"));
        assertEquals("utility", rodAssignment.categoryId());
        assertEquals("navigation", rodAssignment.subcategoryId());
    }

    @Test
    @Disabled("intended routing pending override layer; see docs/superpowers/specs/2026-06-22-classification-override-and-curation-design.md")
    void hexaliaUnknownFamiliesGainSemanticFacts() {
        Map<String, String> focus = meta("hexalia", "net.astralya.hexalia.item.custom.HexFocusItem");
        focus.put(SearchNodeKeys.TAGS, "hexalia:offhand_equipment");
        HexaliaCompat.enrichItem(new ResourceLocation("hexalia", "hex_focus"), focus);
        CategoryAssignment focusAssignment = resolve("hexalia:hex_focus", focus);

        Map<String, String> sac = meta("hexalia", "net.astralya.hexalia.item.custom.ThrownSacItem");
        HexaliaCompat.enrichItem(new ResourceLocation("hexalia", "foul_sac"), sac);
        CategoryAssignment sacAssignment = resolve("hexalia:foul_sac", sac);

        Map<String, String> athame = meta("hexalia", "net.astralya.hexalia.item.custom.AthameItem");
        HexaliaCompat.enrichItem(new ResourceLocation("hexalia", "athame"), athame);
        CategoryAssignment athameAssignment = resolve("hexalia:athame", athame);

        Map<String, String> powder = meta("hexalia", "net.minecraft.world.item.Item");
        powder.put(SearchNodeKeys.TAGS, "hexalia:crushed_herbs");
        HexaliaCompat.enrichItem(new ResourceLocation("hexalia", "spirit_powder"), powder);
        CategoryAssignment powderAssignment = resolve("hexalia:spirit_powder", powder);

        assertEquals("magic_artifact", focus.get("hexaliaItemKind"));
        assertEquals("magic", focusAssignment.categoryId());
        assertEquals("artifacts", focusAssignment.subcategoryId());
        assertEquals("projectile_sac", sac.get("hexaliaItemKind"));
        assertEquals("tools", sacAssignment.categoryId());
        assertEquals("ammo", sacAssignment.subcategoryId());
        assertEquals("melee_tool", athame.get("hexaliaItemKind"));
        assertEquals("tools", athameAssignment.categoryId());
        assertEquals("melee", athameAssignment.subcategoryId());
        assertEquals("magic_reagent", powder.get("hexaliaItemKind"));
        assertEquals("magic", powderAssignment.categoryId());
        assertEquals("reagents", powderAssignment.subcategoryId());
    }

    @Test
    void witcheryUnknownFamiliesGainSemanticFacts() {
        Map<String, String> broom = meta("witchery", "dev.sterner.witchery.content.item.BroomItem");
        WitcheryCompat.enrichItem(new ResourceLocation("witchery", "broom/variant/broom_a22400b68762"), broom);
        CategoryAssignment broomAssignment = resolve("witchery:broom/variant/broom_a22400b68762", broom);

        Map<String, String> mutandis = meta("witchery", "dev.sterner.witchery.content.item.MutandisItem");
        WitcheryCompat.enrichItem(new ResourceLocation("witchery", "mutandis"), mutandis);
        CategoryAssignment mutandisAssignment = resolve("witchery:mutandis", mutandis);

        Map<String, String> stone = meta("witchery", "dev.sterner.witchery.content.item.SeerStoneItem");
        WitcheryCompat.enrichItem(new ResourceLocation("witchery", "seer_stone"), stone);
        CategoryAssignment stoneAssignment = resolve("witchery:seer_stone", stone);

        Map<String, String> stake = meta("witchery", "dev.sterner.witchery.content.item.WoodenStakeItem");
        WitcheryCompat.enrichItem(new ResourceLocation("witchery", "wooden_oak_stake"), stake);
        CategoryAssignment stakeAssignment = resolve("witchery:wooden_oak_stake", stake);

        assertEquals("broom_tool", broom.get("witcheryItemKind"));
        assertEquals("tech", broomAssignment.categoryId());
        assertEquals("transport", broomAssignment.subcategoryId());
        assertEquals("magic_reagent", mutandis.get("witcheryItemKind"));
        assertEquals("magic", mutandisAssignment.categoryId());
        assertEquals("reagents", mutandisAssignment.subcategoryId());
        assertEquals("magic_artifact", stone.get("witcheryItemKind"));
        assertEquals("magic", stoneAssignment.categoryId());
        assertEquals("artifacts", stoneAssignment.subcategoryId());
        assertEquals("melee_tool", stake.get("witcheryItemKind"));
        assertEquals("tools", stakeAssignment.categoryId());
        assertEquals("melee", stakeAssignment.subcategoryId());
    }

    @Test
    void silentGemsTreatsGemNamesAsColorAxisForGeneratedBlocks() {
        Map<String, String> roseQuartz = meta("silentgems", "net.silentchaos512.gems.item.GemBlockItem");
        Map<String, String> ammolite = meta("silentgems", "net.silentchaos512.gems.item.GemBlockItem");

        SilentGemsCompat.enrichItem(new ResourceLocation("silentgems", "smooth_rose_quartz"), roseQuartz);
        SilentGemsCompat.enrichItem(new ResourceLocation("silentgems", "smooth_ammolite"), ammolite);

        assertEquals("silentgems:smooth_stone", roseQuartz.get(SearchNodeKeys.COLLAPSE_FAMILY));
        assertEquals("silentgems:smooth_stone", ammolite.get(SearchNodeKeys.COLLAPSE_FAMILY));
        assertEquals("Smooth Gem Stones", roseQuartz.get(SearchNodeKeys.COLLAPSE_LABEL));
        assertEquals("rose_quartz", roseQuartz.get(SearchNodeKeys.COLOR_BUCKET));
        assertEquals("ammolite", ammolite.get(SearchNodeKeys.COLOR_BUCKET));
        assertEquals("default_collapsed", roseQuartz.get(SearchNodeKeys.VARIANT_COLLAPSE_MODE));
    }

    @Test
    void datanessenceRoutesHalcyonBlocksToDedicatedCategory() {
        Map<String, String> meta = meta("datanessence", "net.minecraft.world.item.BlockItem");
        meta.put(SearchNodeKeys.FACETS, "placeable,has_block_entity");
        meta.put(SearchNodeKeys.ONTOLOGY_CATEGORY, "masonry");
        meta.put(SearchNodeKeys.ONTOLOGY_SUBCATEGORY, "full_block");

        DatanessenceCompat.enrichItem(new ResourceLocation("datanessence", "item_filter"), meta);
        CategoryAssignment assignment = resolve("datanessence:item_filter", meta);

        assertEquals("machines", meta.get("halcyonItemKind"));
        assertEquals("halcyon", assignment.categoryId());
        assertEquals("machines", assignment.subcategoryId());
    }

    @Test
    void datanessenceFocusedRoutesBeatGenericHardIdentityAndScoring() {
        Map<String, String> tool = meta("datanessence", "EsetKalenko.Halcyon.item.Locator");
        tool.put(SearchNodeKeys.FACETS, "utility_tool");
        DatanessenceCompat.enrichItem(new ResourceLocation("datanessence", "locator"), tool);
        CategoryAssignment toolAssignment = resolve("datanessence:locator", tool);

        Map<String, String> template = meta("datanessence", "net.minecraft.world.item.Item");
        template.put(SearchNodeKeys.FACETS, "template");
        DatanessenceCompat.enrichItem(new ResourceLocation("datanessence", "rod_mold"), template);
        CategoryAssignment templateAssignment = resolve("datanessence:rod_mold", template);

        assertEquals("halcyon", toolAssignment.categoryId());
        assertEquals("tools", toolAssignment.subcategoryId());
        assertEquals("halcyon", templateAssignment.categoryId());
        assertEquals("templates", templateAssignment.subcategoryId());
    }

    @Test
    void datanessenceClassFactsProduceHalcyonEquipmentAndEssenceFacets() {
        Map<String, String> sword = meta("datanessence", "EsetKalenko.Halcyon.item.equipment.EssenceSword");
        DatanessenceCompat.enrichItem(new ResourceLocation("datanessence", "essence_sword"), sword);
        CategoryAssignment swordAssignment = resolve("datanessence:essence_sword", sword);

        Map<String, String> bomb = meta("datanessence", "EsetKalenko.Halcyon.item.equipment.EssenceBombItem");
        DatanessenceCompat.enrichItem(new ResourceLocation("datanessence", "essence_bomb"), bomb);
        CategoryAssignment bombAssignment = resolve("datanessence:essence_bomb", bomb);

        Map<String, String> shard = meta("datanessence", "EsetKalenko.Halcyon.api.item.EssenceShard");
        DatanessenceCompat.enrichItem(new ResourceLocation("datanessence", "essence_shard"), shard);
        CategoryAssignment shardAssignment = resolve("datanessence:essence_shard", shard);

        Map<String, String> drive = meta("datanessence", "EsetKalenko.Halcyon.item.DataDrive");
        DatanessenceCompat.enrichItem(new ResourceLocation("datanessence", "data_drive"), drive);
        CategoryAssignment driveAssignment = resolve("datanessence:data_drive", drive);

        assertTrue(sword.get(SearchNodeKeys.FACETS).contains(ItemFacet.MELEE_WEAPON.id()));
        assertEquals("halcyon", swordAssignment.categoryId());
        assertEquals("equipment", swordAssignment.subcategoryId());
        assertTrue(bomb.get(SearchNodeKeys.FACETS).contains(ItemFacet.PROJECTILE.id()));
        assertTrue(bomb.get(SearchNodeKeys.FACETS).contains(ItemFacet.MAGIC_ARTIFACT.id()));
        assertEquals("equipment", bombAssignment.subcategoryId());
        assertTrue(shard.get(SearchNodeKeys.FACETS).contains(ItemFacet.MAGIC_REAGENT.id()));
        assertEquals("essence", shardAssignment.subcategoryId());
        assertTrue(drive.get(SearchNodeKeys.FACETS).contains(ItemFacet.STORAGE.id()));
        assertEquals("machines", driveAssignment.subcategoryId());
    }

    @Test
    void datanessenceKeepsHalcyonBooksSearchableAsGuideBooks() {
        Map<String, String> meta = meta("datanessence", "net.minecraft.world.item.Item");
        meta.put(SearchNodeKeys.ONTOLOGY_CATEGORY, "utility");
        meta.put(SearchNodeKeys.ONTOLOGY_SUBCATEGORY, "books");
        meta.put(SearchNodeKeys.FACETS, "book");

        DatanessenceCompat.enrichItem(new ResourceLocation("datanessence", "sprite_book_flora"), meta);
        CategoryAssignment assignment = resolve("datanessence:sprite_book_flora", meta);

        assertEquals("books", meta.get("halcyonItemKind"));
        assertEquals("true", meta.get(SearchNodeKeys.GUIDE_BOOK_CANDIDATE));
        assertTrue(meta.get(SearchNodeKeys.FACETS).contains(ItemFacet.GUIDE_BOOK.id()));
        assertEquals("utility", assignment.categoryId());
        assertEquals("books", assignment.subcategoryId());
    }

    @Test
    void tideFishingGadgetsRouteSemantically() {
        Map<String, String> hook = meta("tide", "com.li64.tide.registries.items.FishingHookItem");
        TideCompat.enrichItem(new ResourceLocation("tide", "fishing_hook"), hook);
        CategoryAssignment hookAssignment = resolve("tide:fishing_hook", hook);

        Map<String, String> meter = meta("tide", "com.li64.tide.registries.items.DepthMeterItem");
        TideCompat.enrichItem(new ResourceLocation("tide", "depth_meter"), meter);
        CategoryAssignment meterAssignment = resolve("tide:depth_meter", meter);

        Map<String, String> journal = meta("tide", "com.li64.tide.registries.items.FishingJournalItem");
        TideCompat.enrichItem(new ResourceLocation("tide", "fishing_journal"), journal);
        CategoryAssignment journalAssignment = resolve("tide:fishing_journal", journal);

        assertEquals("fishing_tackle", hook.get("tideItemKind"));
        assertEquals("tools", hookAssignment.categoryId());
        assertEquals("utility", hookAssignment.subcategoryId());
        assertEquals("navigation_gadget", meter.get("tideItemKind"));
        assertEquals("utility", meterAssignment.categoryId());
        assertEquals("navigation", meterAssignment.subcategoryId());
        assertEquals("reference", journal.get("tideItemKind"));
        assertEquals("utility", journalAssignment.categoryId());
        assertEquals("books", journalAssignment.subcategoryId());
    }

    @Test
    void forbiddenArcanusUnknownFamiliesGainSemanticFacts() {
        Map<String, String> catcher = meta("forbidden_arcanus", "com.stal111.forbidden_arcanus.common.item.QuantumCatcherItem");
        ForbiddenArcanusCompat.enrichItem(new ResourceLocation("forbidden_arcanus", "blue_quantum_catcher"), catcher);
        CategoryAssignment catcherAssignment = resolve("forbidden_arcanus:blue_quantum_catcher", catcher);

        Map<String, String> prism = meta("forbidden_arcanus", "com.stal111.forbidden_arcanus.common.item.WhirlwindPrismItem");
        ForbiddenArcanusCompat.enrichItem(new ResourceLocation("forbidden_arcanus", "whirlwind_prism"), prism);
        CategoryAssignment prismAssignment = resolve("forbidden_arcanus:whirlwind_prism", prism);

        Map<String, String> soul = meta("forbidden_arcanus", "net.minecraft.world.item.Item");
        soul.put(SearchNodeKeys.TAGS, "forbidden_arcanus:clibano/creates_soul_fire");
        ForbiddenArcanusCompat.enrichItem(new ResourceLocation("forbidden_arcanus", "enchanted_soul"), soul);
        CategoryAssignment soulAssignment = resolve("forbidden_arcanus:enchanted_soul", soul);

        Map<String, String> tank = meta("forbidden_arcanus", "com.stal111.forbidden_arcanus.common.item.AurealTankItem");
        ForbiddenArcanusCompat.enrichItem(new ResourceLocation("forbidden_arcanus", "aureal_tank"), tank);
        CategoryAssignment tankAssignment = resolve("forbidden_arcanus:aureal_tank", tank);

        assertEquals("capture_tool", catcher.get("forbiddenArcanusItemKind"));
        assertEquals("tools", catcherAssignment.categoryId());
        assertEquals("utility", catcherAssignment.subcategoryId());
        assertEquals("magic_artifact", prism.get("forbiddenArcanusItemKind"));
        assertEquals("magic", prismAssignment.categoryId());
        assertEquals("artifacts", prismAssignment.subcategoryId());
        assertEquals("soul_reagent", soul.get("forbiddenArcanusItemKind"));
        assertEquals("magic", soulAssignment.categoryId());
        assertEquals("reagents", soulAssignment.subcategoryId());
        assertEquals("storage_vessel", tank.get("forbiddenArcanusItemKind"));
        assertEquals("utility", tankAssignment.categoryId());
        assertEquals("misc", tankAssignment.subcategoryId());
    }

    @Test
    @Disabled("intended routing pending override layer; see docs/superpowers/specs/2026-06-22-classification-override-and-curation-design.md")
    void eternalStarlightUnknownFamiliesGainSemanticFacts() {
        Map<String, String> painting = meta("eternal_starlight", "cn.leolezury.eternalstarlight.common.item.misc.ESPaintingItem");
        painting.put(SearchNodeKeys.SUBTYPE_OF, "eternal_starlight:starlit_painting");
        EternalStarlightCompat.enrichItem(new ResourceLocation("eternal_starlight", "starlit_painting/variant/starlit_painting_27b7fa141554"), painting);
        CategoryAssignment paintingAssignment = resolve("eternal_starlight:starlit_painting/variant/starlit_painting_27b7fa141554", painting);

        Map<String, String> pendant = meta("eternal_starlight", "net.minecraft.world.item.Item");
        pendant.put(SearchNodeKeys.TAGS, "eternal_starlight:accessories");
        EternalStarlightCompat.enrichItem(new ResourceLocation("eternal_starlight", "battleaxe_pendant"), pendant);
        CategoryAssignment pendantAssignment = resolve("eternal_starlight:battleaxe_pendant", pendant);

        Map<String, String> soulDew = meta("eternal_starlight", "net.minecraft.world.item.Item");
        EternalStarlightCompat.enrichItem(new ResourceLocation("eternal_starlight", "soul_dew"), soulDew);
        CategoryAssignment soulDewAssignment = resolve("eternal_starlight:soul_dew", soulDew);

        Map<String, String> brick = meta("eternal_starlight", "net.minecraft.world.item.Item");
        EternalStarlightCompat.enrichItem(new ResourceLocation("eternal_starlight", "cinder_brick"), brick);
        CategoryAssignment brickAssignment = resolve("eternal_starlight:cinder_brick", brick);

        assertEquals("paintings", painting.get("eternalStarlightItemKind"));
        assertEquals("eternal_starlight:starlit_painting", painting.get(SearchNodeKeys.COLLAPSE_FAMILY));
        assertEquals("eternal_starlight", paintingAssignment.categoryId());
        assertEquals("paintings", paintingAssignment.subcategoryId());
        assertEquals("accessories", pendant.get("eternalStarlightItemKind"));
        assertEquals("eternal_starlight", pendantAssignment.categoryId());
        assertEquals("accessories", pendantAssignment.subcategoryId());
        assertEquals("reagents", soulDew.get("eternalStarlightItemKind"));
        assertEquals("eternal_starlight", soulDewAssignment.categoryId());
        assertEquals("reagents", soulDewAssignment.subcategoryId());
        assertEquals("materials", brick.get("eternalStarlightItemKind"));
        assertEquals("eternal_starlight", brickAssignment.categoryId());
        assertEquals("materials", brickAssignment.subcategoryId());
    }

    @Test
    @Disabled("intended routing pending override layer; see docs/superpowers/specs/2026-06-22-classification-override-and-curation-design.md")
    void malumUnknownFamiliesExtendExistingCompatRoutes() {
        Map<String, String> sapball = meta("malum", "net.minecraft.world.item.Item");
        sapball.put(SearchNodeKeys.TAGS, "malum:sapballs");
        MalumCompat.enrichItem(new ResourceLocation("malum", "runic_sapball"), sapball);
        CategoryAssignment sapballAssignment = resolve("malum:runic_sapball", sapball);

        Map<String, String> lobber = meta("malum", "com.sammy.malum.common.item.curiosities.tools.CatalystLobberItem");
        MalumCompat.enrichItem(new ResourceLocation("malum", "catalyst_lobber"), lobber);
        CategoryAssignment lobberAssignment = resolve("malum:catalyst_lobber", lobber);

        Map<String, String> weave = meta("malum", "net.minecraft.world.item.Item");
        MalumCompat.enrichItem(new ResourceLocation("malum", "ancient_weave"), weave);
        CategoryAssignment weaveAssignment = resolve("malum:ancient_weave", weave);

        Map<String, String> nucleus = meta("malum", "com.sammy.malum.common.item.WindNucleusItem");
        MalumCompat.enrichItem(new ResourceLocation("malum", "wind_nucleus"), nucleus);
        CategoryAssignment nucleusAssignment = resolve("malum:wind_nucleus", nucleus);

        assertEquals("organic_materials", sapball.get("malumItemKind"));
        assertEquals("malum", sapballAssignment.categoryId());
        assertEquals("materials", sapballAssignment.subcategoryId());
        assertEquals("tools", lobber.get("malumItemKind"));
        assertEquals("malum", lobberAssignment.categoryId());
        assertEquals("equipment", lobberAssignment.subcategoryId());
        assertEquals("textiles", weave.get("malumItemKind"));
        assertEquals("malum", weaveAssignment.categoryId());
        assertEquals("weaves", weaveAssignment.subcategoryId());
        assertEquals("artifacts", nucleus.get("malumItemKind"));
        assertEquals("malum", nucleusAssignment.categoryId());
        assertEquals("artifacts", nucleusAssignment.subcategoryId());
    }

    @Test
    @Disabled("intended routing pending override layer; see docs/superpowers/specs/2026-06-22-classification-override-and-curation-design.md")
    void powerGridUnknownFamiliesGainTechComponentFacts() {
        Map<String, String> resistor = meta("powergrid", "net.minecraft.world.item.Item");
        resistor.put(SearchNodeKeys.TAGS, "powergrid:circuit_component");
        PowerGridCompat.enrichItem(new ResourceLocation("powergrid", "resistor"), resistor);
        CategoryAssignment resistorAssignment = resolve("powergrid:resistor", resistor);

        Map<String, String> meter = meta("powergrid", "org.patryk3211.powergrid.equipment.multimeter.MultimeterItem");
        PowerGridCompat.enrichItem(new ResourceLocation("powergrid", "multimeter"), meter);
        CategoryAssignment meterAssignment = resolve("powergrid:multimeter", meter);

        Map<String, String> card = meta("powergrid", "org.patryk3211.powergrid.kinetics.punchcard.PunchCardItem");
        PowerGridCompat.enrichItem(new ResourceLocation("powergrid", "punch_card"), card);
        CategoryAssignment cardAssignment = resolve("powergrid:punch_card", card);

        assertEquals("components", resistor.get("powerGridItemKind"));
        assertEquals("tech", resistorAssignment.categoryId());
        assertEquals("components", resistorAssignment.subcategoryId());
        assertEquals("tools", meter.get("powerGridItemKind"));
        assertEquals("tools", meterAssignment.categoryId());
        assertEquals("utility", meterAssignment.subcategoryId());
        assertEquals("programming", card.get("powerGridItemKind"));
        assertEquals("tech", cardAssignment.categoryId());
        assertEquals("components", cardAssignment.subcategoryId());
    }

    @Test
    @Disabled("intended routing pending override layer; see docs/superpowers/specs/2026-06-22-classification-override-and-curation-design.md")
    void enigmaticLegacyPlusUnknownFamiliesGainMagicAndStorageFacts() {
        Map<String, String> eye = meta("enigmaticlegacyplus", "auviotre.enigmatic.legacy.contents.item.misc.ExtradimensionalEye");
        EnigmaticLegacyPlusCompat.enrichItem(new ResourceLocation("enigmaticlegacyplus", "extradimensional_eye"), eye);
        CategoryAssignment eyeAssignment = resolve("enigmaticlegacyplus:extradimensional_eye", eye);

        Map<String, String> bag = meta("enigmaticlegacyplus", "auviotre.enigmatic.legacy.contents.item.legacy.AntiqueBag");
        EnigmaticLegacyPlusCompat.enrichItem(new ResourceLocation("enigmaticlegacyplus", "antique_bag"), bag);
        CategoryAssignment bagAssignment = resolve("enigmaticlegacyplus:antique_bag", bag);

        Map<String, String> mixture = meta("enigmaticlegacyplus", "auviotre.enigmatic.legacy.contents.item.potions.MendingMixture");
        EnigmaticLegacyPlusCompat.enrichItem(new ResourceLocation("enigmaticlegacyplus", "mending_mixture"), mixture);
        CategoryAssignment mixtureAssignment = resolve("enigmaticlegacyplus:mending_mixture", mixture);

        Map<String, String> ring = meta("enigmaticlegacyplus", "auviotre.enigmatic.legacy.contents.item.BaseItem$1");
        EnigmaticLegacyPlusCompat.enrichItem(new ResourceLocation("enigmaticlegacyplus", "extra_ring"), ring);
        CategoryAssignment ringAssignment = resolve("enigmaticlegacyplus:extra_ring", ring);

        assertEquals("artifact", eye.get("enigmaticLegacyPlusItemKind"));
        assertEquals("magic", eyeAssignment.categoryId());
        assertEquals("artifacts", eyeAssignment.subcategoryId());
        assertEquals("storage", bag.get("enigmaticLegacyPlusItemKind"));
        assertEquals("utility", bagAssignment.categoryId());
        assertEquals("misc", bagAssignment.subcategoryId());
        assertEquals("reagent", mixture.get("enigmaticLegacyPlusItemKind"));
        assertEquals("magic", mixtureAssignment.categoryId());
        assertEquals("reagents", mixtureAssignment.subcategoryId());
        assertEquals("accessory", ring.get("enigmaticLegacyPlusItemKind"));
        assertEquals("armor", ringAssignment.categoryId());
        assertEquals("curios", ringAssignment.subcategoryId());
    }

    @Test
    @Disabled("intended routing pending override layer; see docs/superpowers/specs/2026-06-22-classification-override-and-curation-design.md")
    void zenColonyUnknownFamiliesGainSupplyPackFacts() {
        Map<String, String> pack = meta("zen_colony", "net.minecraft.world.item.Item");
        pack.put(SearchNodeKeys.TAGS, "zen_colony:basic_supply_packs,zen_colony:supply_packs");
        ZenColonyCompat.enrichItem(new ResourceLocation("zen_colony", "raw_lumber_pack"), pack);
        CategoryAssignment packAssignment = resolve("zen_colony:raw_lumber_pack", pack);

        Map<String, String> focus = meta("zen_colony", "net.minecraft.world.item.Item");
        ZenColonyCompat.enrichItem(new ResourceLocation("zen_colony", "astral_focus"), focus);
        CategoryAssignment focusAssignment = resolve("zen_colony:astral_focus", focus);

        assertEquals("supply_pack", pack.get("zenColonyItemKind"));
        assertEquals("minecolonies", packAssignment.categoryId());
        assertEquals("supply_packs", packAssignment.subcategoryId());
        assertEquals("focus", focus.get("zenColonyItemKind"));
        assertEquals("magic", focusAssignment.categoryId());
        assertEquals("artifacts", focusAssignment.subcategoryId());
    }

    @Test
    void swemUnknownFamiliesExtendHorseCareFacts() {
        Map<String, String> tracker = meta("swem", "com.alaharranhonor.swem.item.TrackerItem");
        SwemCompat.enrichItem(new ResourceLocation("swem", "tracker"), tracker);
        CategoryAssignment trackerAssignment = resolve("swem:tracker", tracker);

        Map<String, String> mortar = meta("swem", "com.alaharranhonor.swem.item.PestleMortarItem");
        mortar.put(SearchNodeKeys.TAGS, "swem:pestle_mortar");
        SwemCompat.enrichItem(new ResourceLocation("swem", "pestle_mortar"), mortar);
        CategoryAssignment mortarAssignment = resolve("swem:pestle_mortar", mortar);

        assertEquals("horse_care", tracker.get("swemItemKind"));
        assertEquals("swem", trackerAssignment.categoryId());
        assertEquals("care", trackerAssignment.subcategoryId());
        assertEquals("horse_care", mortar.get("swemItemKind"));
        assertEquals("swem", mortarAssignment.categoryId());
        assertEquals("care", mortarAssignment.subcategoryId());
    }

    @Test
    void genericGeneratedVariantsCollapseBySubtype() {
        Map<String, String> meta = meta("domum_ornamentum", "com.ldtteam.domumornamentum.item.decoration.PanelBlockItem");
        meta.put(SearchNodeKeys.SUBTYPE_OF, "domum_ornamentum:panel");

        GeneratedVariantCollapseCompat.enrichItem(
                new ResourceLocation("domum_ornamentum", "panel/variant/stripped_oak_wood_panel_11720a6e1c90"),
                meta);

        assertEquals("domum_ornamentum:panel", meta.get(SearchNodeKeys.COLLAPSE_FAMILY));
        assertEquals("Panel", meta.get(SearchNodeKeys.COLLAPSE_LABEL));
        assertEquals("default_collapsed", meta.get(SearchNodeKeys.VARIANT_COLLAPSE_MODE));
    }

    private static Map<String, String> meta(String modId, String itemClass) {
        Map<String, String> meta = new HashMap<>();
        meta.put(SearchNodeKeys.MOD_ID, modId);
        meta.put(SearchNodeKeys.ITEM_CLASS, itemClass);
        return meta;
    }

    private static CategoryAssignment resolve(String id, Map<String, String> meta, ItemFacet... facets) {
        return PrimaryCategoryResolver.resolve(
                new ResourceLocation(id),
                facetsFrom(meta, facets),
                meta
        );
    }

    private static EnumSet<ItemFacet> facetsFrom(Map<String, String> meta, ItemFacet... explicitFacets) {
        EnumSet<ItemFacet> resolved = EnumSet.noneOf(ItemFacet.class);
        String encoded = meta.getOrDefault(SearchNodeKeys.FACETS, "");
        if (!encoded.isBlank()) {
            for (String token : encoded.split(",")) {
                String normalized = token.trim();
                if (normalized.isBlank()) {
                    continue;
                }
                for (ItemFacet facet : ItemFacet.values()) {
                    if (facet.id().equals(normalized)) {
                        resolved.add(facet);
                        break;
                    }
                }
            }
        }
        for (ItemFacet facet : explicitFacets) {
            resolved.add(facet);
        }
        return resolved;
    }
}
