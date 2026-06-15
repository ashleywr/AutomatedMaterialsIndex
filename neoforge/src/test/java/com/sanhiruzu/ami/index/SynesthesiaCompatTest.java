package com.sanhiruzu.ami.index;

import com.sanhiruzu.ami.compat.BornInChaosCompat;
import com.sanhiruzu.ami.compat.CataclysmCompat;
import com.sanhiruzu.ami.compat.DatanessenceCompat;
import com.sanhiruzu.ami.compat.GeneratedVariantCollapseCompat;
import com.sanhiruzu.ami.compat.MalumCompat;
import com.sanhiruzu.ami.compat.PastelCompat;
import com.sanhiruzu.ami.compat.SilentGemsCompat;
import com.sanhiruzu.ami.compat.SwemCompat;
import net.minecraft.resources.ResourceLocation;
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
    void bornInChaosMaterialsAndCharmsRouteSemantically() {
        Map<String, String> material = meta("born_in_chaos_v1", "net.mcreator.borninchaosv.item.DarkMetalIngotItem");
        BornInChaosCompat.enrichItem(new ResourceLocation("born_in_chaos_v1", "dark_metal_ingot"), material);
        CategoryAssignment materialAssignment = resolve("born_in_chaos_v1:dark_metal_ingot", material);

        Map<String, String> charm = meta("born_in_chaos_v1", "net.mcreator.borninchaosv.item.CharmofPowerItem");
        BornInChaosCompat.enrichItem(new ResourceLocation("born_in_chaos_v1", "charmof_power"), charm);
        CategoryAssignment charmAssignment = resolve("born_in_chaos_v1:charmof_power", charm);

        assertEquals("materials", material.get("bornInChaosItemKind"));
        assertEquals("ingredients", materialAssignment.categoryId());
        assertEquals("mineral", materialAssignment.subcategoryId());
        assertEquals("artifacts", charm.get("bornInChaosItemKind"));
        assertEquals("magic", charmAssignment.categoryId());
        assertEquals("artifacts", charmAssignment.subcategoryId());
    }

    @Test
    void cataclysmDungeonEyesAndIngotsRouteSemantically() {
        Map<String, String> eye = meta("cataclysm", "com.github.L_Ender.cataclysm.items.DungeonEyeItem");
        CataclysmCompat.enrichItem(new ResourceLocation("cataclysm", "mech_eye"), eye);
        CategoryAssignment eyeAssignment = resolve("cataclysm:mech_eye", eye);

        Map<String, String> ingot = meta("cataclysm", "net.minecraft.world.item.Item");
        CataclysmCompat.enrichItem(new ResourceLocation("cataclysm", "witherite_ingot"), ingot);
        CategoryAssignment ingotAssignment = resolve("cataclysm:witherite_ingot", ingot);

        assertEquals("dungeon_eyes", eye.get("cataclysmItemKind"));
        assertEquals("cataclysm", eyeAssignment.categoryId());
        assertEquals("dungeon_eyes", eyeAssignment.subcategoryId());
        assertEquals("materials", ingot.get("cataclysmItemKind"));
        assertEquals("ingredients", ingotAssignment.categoryId());
        assertEquals("mineral", ingotAssignment.subcategoryId());
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
