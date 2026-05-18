package com.sanhiruzu.ami.index;

import net.minecraft.resources.ResourceLocation;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

public final class PrimaryCategoryResolver {
    private enum ModFamily {
        GENERIC,
        CREATE,
        FOOD,
        STORAGE,
        AUTOMATION,
        PORTABLE_STORAGE,
        DECOR
    }

    private static final Set<String> HOSTILE_SPAWN_EGGS = Set.of(
            "blaze", "bogged", "breeze", "creeper", "drowned", "elder_guardian",
            "ender_dragon", "endermite", "evoker", "ghast", "guardian", "hoglin",
            "husk", "magma_cube", "phantom", "piglin_brute", "pillager", "ravager",
            "shulker", "silverfish", "skeleton", "slime", "spider", "stray",
            "vex", "vindicator", "warden", "witch", "wither", "wither_skeleton",
            "zoglin", "zombie", "zombie_villager"
    );

    private static final Set<String> NEUTRAL_SPAWN_EGGS = Set.of(
            "bee", "cave_spider", "dolphin", "enderman", "goat", "iron_golem",
            "llama", "panda", "piglin", "polar_bear", "spider", "trader_llama",
            "wolf", "zombified_piglin"
    );

    private PrimaryCategoryResolver() {}

    public static CategoryAssignment resolve(ResourceLocation id, FacetProfile profile) {
        if (id == null) {
            return fallback();
        }

        String modId = id.getNamespace().toLowerCase(Locale.ROOT);
        String path = id.getPath().toLowerCase(Locale.ROOT);
        var facets = profile.facets();
        var attributes = new HashMap<>(profile.attributes());
        ModFamily modFamily = classifyModFamily(modId);

        if (shouldBiasCreateFamilyHandheldToTools(modFamily, path)) {
            return assignment("tools", classifyCreateFamilyToolSubcategory(path), attributes);
        }
        if (shouldBiasCreateFamilyHandheldToUtility(modFamily, path)) {
            return assignment("utility", "misc", attributes);
        }
        if (shouldBiasCreateEnchantingFamilyToMagic(modId, path)) {
            return assignment("magic", "reagents", attributes);
        }
        if (hasAny(facets, ItemFacet.UTILITY_NAVIGATION, ItemFacet.UTILITY_MISC)) {
            return assignment("utility", classifyUtilitySubcategory(facets), attributes);
        }
        if (hasAny(facets, ItemFacet.SPAWN_EGG, ItemFacet.MOB_BUCKET)) {
            return assignment("bestiary", classifyBestiarySubcategory(path), attributes);
        }
        if (hasAny(facets, ItemFacet.POTION, ItemFacet.ENCHANTED_BOOK, ItemFacet.MAGIC_ARTIFACT, ItemFacet.MAGIC_REAGENT)) {
            return assignment("magic", classifyMagicSubcategory(facets), attributes);
        }
        if (hasAny(facets, ItemFacet.ARMOR_HEAD, ItemFacet.ARMOR_CHEST, ItemFacet.ARMOR_LEGS, ItemFacet.ARMOR_FEET)) {
            return assignment("armor", classifyArmorSubcategory(facets), attributes);
        }
        if (isThrowableIngredient(facets)) {
            return assignment("ingredients", classifyIngredientSubcategory(facets), attributes);
        }
        if (hasAny(facets, ItemFacet.MELEE_WEAPON, ItemFacet.RANGED_WEAPON, ItemFacet.PROJECTILE, ItemFacet.HARVEST_TOOL, ItemFacet.UTILITY_TOOL)) {
            return assignment("tools", classifyWeaponSubcategory(facets), attributes);
        }
        if (hasAny(facets,
                ItemFacet.HAS_ENERGY,
                ItemFacet.STORAGE,
                ItemFacet.INTERACTIVE_BLOCK,
                ItemFacet.REDSTONE_LOGIC,
                ItemFacet.REDSTONE_SIGNAL,
                ItemFacet.TRANSPORT,
                ItemFacet.MACHINE,
                ItemFacet.INGOT,
                ItemFacet.GEM,
                ItemFacet.NUGGET,
                ItemFacet.RAW_MATERIAL,
                ItemFacet.DUST)) {
            return assignment("tech", classifyTechSubcategory(facets), attributes);
        }
        if (hasAny(facets, ItemFacet.EDIBLE, ItemFacet.PLACEABLE_FOOD, ItemFacet.COMPOSTABLE, ItemFacet.SEED, ItemFacet.CROP, ItemFacet.NATURE_MISC, ItemFacet.FUNGI, ItemFacet.LOG, ItemFacet.LEAVES, ItemFacet.FLOWER)) {
            return assignment("nature", classifyNatureSubcategory(facets), attributes);
        }
        if (hasAny(facets, ItemFacet.INGREDIENT_ORGANIC, ItemFacet.INGREDIENT_MINERAL, ItemFacet.INGREDIENT_DYE)) {
            return assignment("ingredients", classifyIngredientSubcategory(facets), attributes);
        }
        if (hasAny(facets, ItemFacet.LIGHT_SOURCE, ItemFacet.DECORATIVE_BLOCK)) {
            return assignment("decoration", classifyDecorationSubcategory(facets), attributes);
        }
        if (hasAny(facets, ItemFacet.SOCIAL_PLAYERS, ItemFacet.SOCIAL_CLAIMS)) {
            return assignment("social", classifySocialSubcategory(facets), attributes);
        }
        if (shouldBiasFoodFamilyToIngredients(modFamily, facets, path)) {
            return assignment("ingredients", classifyFoodFamilyIngredientSubcategory(path, facets), attributes);
        }
        if (shouldBiasFoodFamilyToPreparedFood(modFamily, facets, path)) {
            return assignment("nature", classifyFoodFamilyPreparedSubcategory(path, facets), attributes);
        }
        if (shouldBiasDecorFamilyToDecoration(modFamily, facets, path, attributes)) {
            return assignment("decoration", classifyDecorationSubcategory(facets), attributes);
        }
        if (shouldBiasPortableStorageFamilyToArmor(modFamily, path)) {
            return assignment("armor", "curios", attributes);
        }
        if (shouldBiasStorageFamilyToTech(modFamily, facets, path, attributes)) {
            return assignment("tech", classifyStorageSubcategory(path, facets), attributes);
        }
        if (shouldBiasAutomationFamilyToTech(modFamily, facets, path, attributes)) {
            return assignment("tech", classifyAutomationSubcategory(path, facets), attributes);
        }
        if (shouldBiasCreateFamilyToDecoration(modFamily, facets)) {
            return assignment("decoration", classifyDecorationSubcategory(facets), attributes);
        }
        if (shouldBiasCreateFamilyToTech(modFamily, facets, path, attributes)) {
            return assignment("tech", classifyCreateFamilyTechSubcategory(modId, path, facets), attributes);
        }
        if (shouldBiasFoodFamilyToNature(modFamily, facets, path, attributes)) {
            return assignment("nature", classifyNatureSubcategory(facets), attributes);
        }
        if (shouldBeGeology(facets, path)) {
            return assignment("geology", facets.contains(ItemFacet.SOIL_BLOCK) ? "terrain" : "stone", attributes);
        }
        if (shouldBiasUncraftableFullBlockToTerrain(facets, attributes)) {
            return assignment("geology", classifyUncraftableTerrainSubcategory(facets, attributes), attributes);
        }
        if (facets.contains(ItemFacet.PLACEABLE)) {
            return assignment("masonry", classifyMasonrySubcategory(facets, attributes), attributes);
        }
        return fallback();
    }

    private static ModFamily classifyModFamily(String modId) {
        if (modId == null || modId.isBlank()) {
            return ModFamily.GENERIC;
        }
        if (isPortableStorageFamilyMod(modId)) {
            return ModFamily.PORTABLE_STORAGE;
        }
        if (isStorageFamilyMod(modId)) {
            return ModFamily.STORAGE;
        }
        if (modId.contains("delight")
                || modId.equals("croptopia")
                || modId.equals("createfood")
                || modId.equals("bountifulfares")) {
            return ModFamily.FOOD;
        }
        if (isCreateFamilyMod(modId)) {
            return ModFamily.CREATE;
        }
        if (isAutomationFamilyMod(modId)) {
            return ModFamily.AUTOMATION;
        }
        if (isDecorFamilyMod(modId)) {
            return ModFamily.DECOR;
        }
        return ModFamily.GENERIC;
    }

    private static boolean isCreateFamilyMod(String modId) {
        if (modId.equals("create")
                || modId.startsWith("create")
                || modId.equals("railways")
                || modId.equals("copycats")
                || modId.equals("sliceanddice")
                || modId.equals("bellsandwhistles")) {
            return true;
        }
        return false;
    }

    private static boolean isPortableStorageFamilyMod(String modId) {
        return modId.equals("sophisticatedbackpacks")
                || modId.contains("backpack")
                || modId.contains("satchel")
                || modId.contains("pouch");
    }

    private static boolean isStorageFamilyMod(String modId) {
        if (modId.equals("ae2")
                || modId.startsWith("ae2")
                || modId.contains("refinedstorage")
                || modId.equals("merequester")
                || modId.contains("functionalstorage")
                || modId.contains("compatible_storage")
                || modId.contains("compatiblestorage")
                || modId.contains("sophisticatedstorage")
                || modId.contains("storagedrawers")
                || modId.contains("drawer")
                || modId.contains("ironchest")
                || modId.contains("iron_chest")) {
            return true;
        }
        return false;
    }

    private static boolean isDecorFamilyMod(String modId) {
        if (modId.startsWith("mcw")
                || modId.equals("refurbished_furniture")
                || modId.equals("arts_and_crafts")) {
            return true;
        }
        return false;
    }

    private static boolean isAutomationFamilyMod(String modId) {
        return modId.equals("pneumaticcraft")
                || modId.startsWith("pneumaticcraft")
                || modId.contains("projectred")
                || modId.equals("laserio")
                || modId.equals("enderio");
    }

    private static CategoryAssignment fallback() {
        return new CategoryAssignment("misc", "unknown", Map.of());
    }

    private static CategoryAssignment assignment(String categoryId, String subcategoryId, Map<String, String> attributes) {
        return new CategoryAssignment(categoryId, subcategoryId, attributes);
    }

    private static boolean hasAny(Set<ItemFacet> facets, ItemFacet... expected) {
        for (ItemFacet facet : expected) {
            if (facets.contains(facet)) {
                return true;
            }
        }
        return false;
    }

    private static String classifyBestiarySubcategory(String path) {
        if (!path.endsWith("_spawn_egg")) {
            return "passive";
        }

        String mob = path.substring(0, path.length() - "_spawn_egg".length());
        if (HOSTILE_SPAWN_EGGS.contains(mob)) return "hostile";
        if (NEUTRAL_SPAWN_EGGS.contains(mob)) return "neutral";
        return "passive";
    }

    private static String classifyUtilitySubcategory(Set<ItemFacet> facets) {
        if (facets.contains(ItemFacet.UTILITY_NAVIGATION)) return "navigation";
        return "misc";
    }

    private static String classifyMagicSubcategory(Set<ItemFacet> facets) {
        if (facets.contains(ItemFacet.POTION)) return "potions";
        if (facets.contains(ItemFacet.ENCHANTED_BOOK)) return "books";
        if (facets.contains(ItemFacet.MAGIC_ARTIFACT)) return "artifacts";
        if (facets.contains(ItemFacet.MAGIC_REAGENT)) return "reagents";
        return "artifacts";
    }

    private static String classifyArmorSubcategory(Set<ItemFacet> facets) {
        if (facets.contains(ItemFacet.ARMOR_HEAD)) return "head";
        if (facets.contains(ItemFacet.ARMOR_CHEST)) return "chest";
        if (facets.contains(ItemFacet.ARMOR_LEGS)) return "legs";
        if (facets.contains(ItemFacet.ARMOR_FEET)) return "feet";
        return "curios";
    }

    private static String classifyWeaponSubcategory(Set<ItemFacet> facets) {
        if (facets.contains(ItemFacet.MELEE_WEAPON)) return "melee";
        if (facets.contains(ItemFacet.RANGED_WEAPON) || facets.contains(ItemFacet.PROJECTILE)) return "ranged";
        if (facets.contains(ItemFacet.HARVEST_TOOL)) return "harvest";
        if (facets.contains(ItemFacet.UTILITY_TOOL)) return "utility";
        return "utility";
    }

    private static String classifyCreateFamilyToolSubcategory(String path) {
        if (path.contains("cannon") || path.endsWith("_gun")) return "ranged";
        return "utility";
    }

    private static boolean isThrowableIngredient(Set<ItemFacet> facets) {
        return facets.contains(ItemFacet.PROJECTILE)
                && hasAny(facets, ItemFacet.INGREDIENT_ORGANIC, ItemFacet.INGREDIENT_MINERAL, ItemFacet.INGREDIENT_DYE)
                && !hasAny(facets, ItemFacet.MELEE_WEAPON, ItemFacet.RANGED_WEAPON, ItemFacet.HARVEST_TOOL, ItemFacet.UTILITY_TOOL);
    }

    private static String classifyTechSubcategory(Set<ItemFacet> facets) {
        if (facets.contains(ItemFacet.MACHINE)
                || facets.contains(ItemFacet.HAS_ENERGY)
                || facets.contains(ItemFacet.STORAGE)
                || facets.contains(ItemFacet.INTERACTIVE_BLOCK)) {
            return "machines";
        }
        if (facets.contains(ItemFacet.REDSTONE_LOGIC) || facets.contains(ItemFacet.REDSTONE_SIGNAL)) {
            return "redstone";
        }
        if (facets.contains(ItemFacet.TRANSPORT)) return "transport";
        if (facets.contains(ItemFacet.DUST)) return "dusts";
        if (hasAny(facets, ItemFacet.INGOT, ItemFacet.GEM, ItemFacet.NUGGET, ItemFacet.RAW_MATERIAL)) return "ingots";
        return "parts";
    }

    private static String classifyNatureSubcategory(Set<ItemFacet> facets) {
        if (facets.contains(ItemFacet.FOOD_DRINK)) return "drinks";
        if (facets.contains(ItemFacet.FOOD_MEAL)) return "meals";
        if (facets.contains(ItemFacet.FOOD_PROTEIN)) return "proteins";
        if (facets.contains(ItemFacet.PLACEABLE_FOOD)) return "meals";
        if (facets.contains(ItemFacet.EDIBLE)) return "snacks";
        if (facets.contains(ItemFacet.SEED)) return "seeds";
        if (facets.contains(ItemFacet.CROP)) return "crops";
        if (facets.contains(ItemFacet.NATURE_MISC)) return "flora";
        if (facets.contains(ItemFacet.FUNGI)) return "fungi";
        if (facets.contains(ItemFacet.FLOWER)) return "flora";
        if (facets.contains(ItemFacet.LOG)) return "wood";
        if (facets.contains(ItemFacet.LEAVES)) return "flora";
        return "flora";
    }

    private static String classifyIngredientSubcategory(Set<ItemFacet> facets) {
        if (facets.contains(ItemFacet.INGREDIENT_DYE)) return "dyes";
        if (facets.contains(ItemFacet.INGREDIENT_MINERAL)) return "mineral";
        return "organic";
    }

    private static String classifyDecorationSubcategory(Set<ItemFacet> facets) {
        if (facets.contains(ItemFacet.LIGHT_SOURCE)) return "lighting";
        return "furniture";
    }

    private static String classifySocialSubcategory(Set<ItemFacet> facets) {
        if (facets.contains(ItemFacet.SOCIAL_PLAYERS)) return "players";
        if (facets.contains(ItemFacet.SOCIAL_CLAIMS)) return "claims";
        return "teams";
    }

    private static boolean shouldBiasCreateFamilyToDecoration(ModFamily modFamily, Set<ItemFacet> facets) {
        return modFamily == ModFamily.CREATE
                && facets.contains(ItemFacet.PLACEABLE)
                && hasAny(facets, ItemFacet.DECORATIVE_BLOCK, ItemFacet.LIGHT_SOURCE)
                && !hasAny(facets,
                ItemFacet.MACHINE,
                ItemFacet.HAS_ENERGY,
                ItemFacet.STORAGE,
                ItemFacet.REDSTONE_LOGIC,
                ItemFacet.REDSTONE_SIGNAL,
                ItemFacet.TRANSPORT);
    }

    private static boolean shouldBiasCreateFamilyToTech(ModFamily modFamily, Set<ItemFacet> facets, String path, Map<String, String> attributes) {
        return modFamily == ModFamily.CREATE
                && facets.contains(ItemFacet.PLACEABLE)
                && !hasAny(facets,
                ItemFacet.DECORATIVE_BLOCK,
                ItemFacet.LIGHT_SOURCE,
                ItemFacet.EDIBLE,
                ItemFacet.COMPOSTABLE,
                ItemFacet.SEED,
                ItemFacet.CROP,
                ItemFacet.NATURE_MISC,
                ItemFacet.FUNGI,
                ItemFacet.LOG,
                ItemFacet.LEAVES,
                ItemFacet.FLOWER,
                ItemFacet.INGREDIENT_ORGANIC,
                ItemFacet.INGREDIENT_MINERAL,
                ItemFacet.INGREDIENT_DYE,
                ItemFacet.SOCIAL_PLAYERS,
                ItemFacet.SOCIAL_CLAIMS)
                && !hasAny(facets,
                ItemFacet.STAIRS,
                ItemFacet.SLAB,
                ItemFacet.WALL,
                ItemFacet.FENCE,
                ItemFacet.FENCE_GATE,
                ItemFacet.PANE,
                ItemFacet.DOOR,
                ItemFacet.TRAPDOOR)
                && !shouldBeGeology(facets, path);
    }

    private static boolean shouldBiasCreateFamilyHandheldToTools(ModFamily modFamily, String path) {
        return modFamily == ModFamily.CREATE
                && (path.contains("wrench")
                || path.contains("grip")
                || path.contains("worldshaper")
                || path.contains("wand_of_symmetry")
                || path.contains("cannon")
                || path.endsWith("_gun"));
    }

    private static boolean shouldBiasCreateFamilyHandheldToUtility(ModFamily modFamily, String path) {
        return modFamily == ModFamily.CREATE
                && (path.contains("goggle")
                || path.contains("filter")
                || path.contains("schedule")
                || path.contains("shopping_list")
                || path.contains("schematic")
                || path.contains("quill")
                || path.contains("glue")
                || path.contains("controller"));
    }

    private static boolean shouldBiasCreateEnchantingFamilyToMagic(String modId, String path) {
        return modId.equals("create_enchantment_industry")
                && (path.contains("experience")
                || path.contains("hyper_experience")
                || path.contains("nugget_of_experience")
                || path.contains("nugget_of_super_experience"));
    }

    private static boolean shouldBiasDecorFamilyToDecoration(ModFamily modFamily, Set<ItemFacet> facets, String path, Map<String, String> attributes) {
        return modFamily == ModFamily.DECOR
                && facets.contains(ItemFacet.PLACEABLE)
                && !hasAny(facets,
                ItemFacet.MACHINE,
                ItemFacet.HAS_ENERGY,
                ItemFacet.STORAGE,
                ItemFacet.REDSTONE_LOGIC,
                ItemFacet.REDSTONE_SIGNAL,
                ItemFacet.TRANSPORT,
                ItemFacet.NATURE_MISC,
                ItemFacet.FUNGI,
                ItemFacet.FLOWER,
                ItemFacet.LOG,
                ItemFacet.LEAVES)
                && !shouldBeGeology(facets, path)
                && !shouldBiasUncraftableFullBlockToTerrain(facets, attributes);
    }

    private static boolean shouldBiasPortableStorageFamilyToArmor(ModFamily modFamily, String path) {
        return modFamily == ModFamily.PORTABLE_STORAGE
                && (path.contains("backpack") || path.contains("satchel") || path.contains("pouch"));
    }

    private static boolean shouldBiasStorageFamilyToTech(ModFamily modFamily, Set<ItemFacet> facets, String path, Map<String, String> attributes) {
        return modFamily == ModFamily.STORAGE
                && !hasAny(facets,
                ItemFacet.DECORATIVE_BLOCK,
                ItemFacet.LIGHT_SOURCE,
                ItemFacet.EDIBLE,
                ItemFacet.COMPOSTABLE,
                ItemFacet.SEED,
                ItemFacet.CROP,
                ItemFacet.NATURE_MISC,
                ItemFacet.FUNGI,
                ItemFacet.LOG,
                ItemFacet.LEAVES,
                ItemFacet.FLOWER,
                ItemFacet.INGREDIENT_ORGANIC,
                ItemFacet.INGREDIENT_MINERAL,
                ItemFacet.INGREDIENT_DYE,
                ItemFacet.SOCIAL_PLAYERS,
                ItemFacet.SOCIAL_CLAIMS)
                && (
                facets.contains(ItemFacet.PLACEABLE)
                        || path.contains("storage")
                        || path.contains("chest")
                        || path.contains("barrel")
                        || path.contains("drawer")
                        || path.contains("terminal")
                        || path.contains("drive")
                        || path.contains("cell")
                        || path.contains("disk")
                        || path.contains("interface")
                        || path.contains("importer")
                        || path.contains("exporter")
                        || path.contains("controller")
                        || path.contains("cable")
                        || path.contains("bus")
                        || path.contains("tank")
                        || path.contains("silicon")
                        || path.contains("processor")
                        || path.contains("printed")
                        || path.contains("logic")
                        || path.contains("calculation")
                        || path.contains("engineering")
                        || path.contains("chip")
                        || path.contains("card")
                        || path.contains("module")
        )
                && !shouldBeGeology(facets, path)
                && !shouldBiasUncraftableFullBlockToTerrain(facets, attributes);
    }

    private static boolean shouldBiasFoodFamilyToNature(ModFamily modFamily, Set<ItemFacet> facets, String path, Map<String, String> attributes) {
        return modFamily == ModFamily.FOOD
                && facets.contains(ItemFacet.PLACEABLE)
                && !hasAny(facets,
                ItemFacet.DECORATIVE_BLOCK,
                ItemFacet.LIGHT_SOURCE,
                ItemFacet.MACHINE,
                ItemFacet.HAS_ENERGY,
                ItemFacet.STORAGE,
                ItemFacet.REDSTONE_LOGIC,
                ItemFacet.REDSTONE_SIGNAL,
                ItemFacet.TRANSPORT,
                ItemFacet.SOCIAL_PLAYERS,
                ItemFacet.SOCIAL_CLAIMS)
                && !hasAny(facets,
                ItemFacet.STAIRS,
                ItemFacet.SLAB,
                ItemFacet.WALL,
                ItemFacet.FENCE,
                ItemFacet.FENCE_GATE,
                ItemFacet.PANE,
                ItemFacet.DOOR,
                ItemFacet.TRAPDOOR)
                && !shouldBeGeology(facets, path)
                && !shouldBiasUncraftableFullBlockToTerrain(facets, attributes);
    }

    private static boolean shouldBiasFoodFamilyToIngredients(ModFamily modFamily, Set<ItemFacet> facets, String path) {
        return modFamily == ModFamily.FOOD
                && !facets.contains(ItemFacet.PLACEABLE)
                && !hasAny(facets,
                ItemFacet.UTILITY_TOOL,
                ItemFacet.MELEE_WEAPON,
                ItemFacet.RANGED_WEAPON,
                ItemFacet.HARVEST_TOOL,
                ItemFacet.PROJECTILE,
                ItemFacet.MAGIC_ARTIFACT,
                ItemFacet.MAGIC_REAGENT)
                && (
                path.contains("crumb")
                        || path.contains("sugar")
                        || path.contains("butter")
                        || path.contains("chip")
                        || path.contains("flour")
                        || path.contains("cheese")
                        || path.contains("diced")
                        || path.contains("bean")
                        || path.contains("wrapper")
                        || path.contains("dough")
                        || path.contains("powder")
                        || path.contains("puree")
                        || path.contains("frosting")
                        || path.contains("piping_bag")
        );
    }

    private static boolean shouldBiasFoodFamilyToPreparedFood(ModFamily modFamily, Set<ItemFacet> facets, String path) {
        return modFamily == ModFamily.FOOD
                && !hasAny(facets,
                ItemFacet.UTILITY_TOOL,
                ItemFacet.MELEE_WEAPON,
                ItemFacet.RANGED_WEAPON,
                ItemFacet.HARVEST_TOOL,
                ItemFacet.PROJECTILE,
                ItemFacet.MAGIC_ARTIFACT,
                ItemFacet.MAGIC_REAGENT)
                && (
                facets.contains(ItemFacet.PLACEABLE)
                        || facets.contains(ItemFacet.EDIBLE)
                        || path.contains("plate")
                        || path.contains("bowl")
                        || path.contains("pie")
                        || path.contains("tart")
                        || path.contains("pudding")
                        || path.contains("calzone")
                        || path.contains("sandwich")
                        || path.contains("parmesan")
                        || path.contains("sausage")
                        || path.contains("burger")
                        || path.contains("pasta")
                        || path.contains("dessert")
                        || path.contains("patty")
                        || path.contains("on_a_stick")
        );
    }

    private static boolean shouldBiasAutomationFamilyToTech(ModFamily modFamily, Set<ItemFacet> facets, String path, Map<String, String> attributes) {
        return modFamily == ModFamily.AUTOMATION
                && !hasAny(facets,
                ItemFacet.DECORATIVE_BLOCK,
                ItemFacet.LIGHT_SOURCE,
                ItemFacet.EDIBLE,
                ItemFacet.COMPOSTABLE,
                ItemFacet.SEED,
                ItemFacet.CROP,
                ItemFacet.NATURE_MISC,
                ItemFacet.FUNGI,
                ItemFacet.LOG,
                ItemFacet.LEAVES,
                ItemFacet.FLOWER,
                ItemFacet.INGREDIENT_ORGANIC,
                ItemFacet.INGREDIENT_MINERAL,
                ItemFacet.INGREDIENT_DYE,
                ItemFacet.SOCIAL_PLAYERS,
                ItemFacet.SOCIAL_CLAIMS)
                && (
                facets.contains(ItemFacet.PLACEABLE)
                        || path.contains("circuit")
                        || path.contains("transistor")
                        || path.contains("capacitor")
                        || path.contains("assembly")
                        || path.contains("wafer")
                        || path.contains("etch")
                        || path.contains("photomask")
                        || path.contains("tube")
                        || path.contains("valve")
                        || path.contains("module")
                        || path.contains("drone")
                        || path.contains("charger")
                        || path.contains("compressor")
                        || path.contains("chamber")
        )
                && !shouldBeGeology(facets, path)
                && !shouldBiasUncraftableFullBlockToTerrain(facets, attributes);
    }

    private static String classifyStorageSubcategory(String path, Set<ItemFacet> facets) {
        if (path.contains("silicon")
                || path.contains("processor")
                || path.contains("printed_")
                || path.contains("printed")
                || path.contains("logic_")
                || path.contains("logic")
                || path.contains("calculation")
                || path.contains("engineering")
                || path.contains("chip")
                || path.contains("card")
                || path.contains("module")) {
            return "circuits";
        }
        if (facets.contains(ItemFacet.STORAGE)
                || path.contains("storage")
                || path.contains("chest")
                || path.contains("barrel")
                || path.contains("drawer")
                || path.contains("tank")) {
            return "machines";
        }
        if (path.contains("terminal")
                || path.contains("cable")
                || path.contains("bus")
                || path.contains("interface")
                || path.contains("importer")
                || path.contains("exporter")
                || path.contains("drive")
                || path.contains("disk")
                || path.contains("cell")
                || path.contains("controller")) {
            return "parts";
        }
        return "machines";
    }

    private static String classifyCreateFamilyTechSubcategory(String modId, String path, Set<ItemFacet> facets) {
        if (modId.equals("railways")) {
            if (path.contains("track")
                    || path.contains("coupler")
                    || path.contains("conductor")
                    || path.contains("switch")
                    || path.contains("semaphore")
                    || path.contains("handcar")) {
                return "transport";
            }
            if (path.contains("smokestack")
                    || path.contains("boiler")
                    || path.contains("buffer")
                    || path.contains("cowcatcher")
                    || path.contains("headstock")
                    || path.contains("link")
                    || path.contains("connector")) {
                return "parts";
            }
        }
        if (modId.equals("createaddition")
                || modId.equals("create_new_age")
                || modId.equals("new_age")) {
            if (path.contains("alternator")
                    || path.contains("motor")
                    || path.contains("generator")
                    || path.contains("charger")
                    || path.contains("accumulator")
                    || path.contains("battery")) {
                return "machines";
            }
            if (path.contains("connector")
                    || path.contains("cable")
                    || path.contains("coil")
                    || path.contains("wire")
                    || path.contains("electrode")) {
                return "parts";
            }
        }
        if (modId.equals("create_winery")) {
            if (path.contains("bottle")) {
                return "transport";
            }
            if (path.contains("barrel")
                    || path.contains("vat")
                    || path.contains("press")
                    || path.contains("ferment")) {
                return "machines";
            }
        }
        if (modId.equals("createoreexcavation")) {
            if (path.contains("drill")
                    || path.contains("pump")
                    || path.contains("extract")
                    || path.contains("boring")
                    || path.contains("survey")) {
                return "machines";
            }
            if (path.contains("pipe")
                    || path.contains("vein")
                    || path.contains("sample")
                    || path.contains("core")) {
                return "parts";
            }
        }
        if (facets.contains(ItemFacet.TRANSPORT)) {
            return "transport";
        }
        if (facets.contains(ItemFacet.REDSTONE_LOGIC) || facets.contains(ItemFacet.REDSTONE_SIGNAL)) {
            return "redstone";
        }
        if (path.contains("belt")
                || path.contains("gearbox")
                || path.contains("bracket")
                || path.contains("pipe")
                || path.contains("valve")
                || path.contains("handle")
                || path.contains("shaft")
                || path.contains("cog")
                || path.contains("chute")
                || path.contains("depot")
                || path.contains("ejector")
                || path.contains("speedometer")
                || path.contains("stressometer")) {
            return "parts";
        }
        if (path.contains("press")
                || path.contains("burner")
                || path.contains("wheel")
                || path.contains("millstone")
                || path.contains("mixer")
                || path.contains("fan")
                || path.contains("pump")
                || path.contains("tank")
                || path.contains("backtank")
                || path.contains("engine")) {
            return "machines";
        }
        return classifyTechSubcategory(facets);
    }

    private static String classifyFoodFamilyIngredientSubcategory(String path, Set<ItemFacet> facets) {
        if (facets.contains(ItemFacet.INGREDIENT_DYE)) return "dyes";
        if (facets.contains(ItemFacet.INGREDIENT_MINERAL)) return "mineral";
        return "organic";
    }

    private static String classifyFoodFamilyPreparedSubcategory(String path, Set<ItemFacet> facets) {
        if (facets.contains(ItemFacet.FOOD_DRINK) || path.contains("bottle")) {
            return "drinks";
        }
        if (facets.contains(ItemFacet.FOOD_MEAL)
                || path.contains("plate")
                || path.contains("bowl")
                || path.contains("pie")
                || path.contains("tart")
                || path.contains("pudding")
                || path.contains("calzone")
                || path.contains("sandwich")
                || path.contains("parmesan")
                || path.contains("burger")
                || path.contains("pasta")
                || path.contains("dessert")
                || path.contains("patty")) {
            return "meals";
        }
        if (facets.contains(ItemFacet.FOOD_PROTEIN)) {
            return "proteins";
        }
        return "snacks";
    }

    private static String classifyAutomationSubcategory(String path, Set<ItemFacet> facets) {
        if (path.contains("circuit")
                || path.contains("transistor")
                || path.contains("capacitor")
                || path.contains("wafer")
                || path.contains("etch")
                || path.contains("photomask")
                || path.contains("assembly")
                || path.contains("module")) {
            return "circuits";
        }
        if (path.contains("tube")
                || path.contains("valve")
                || path.contains("drone")) {
            return "parts";
        }
        if (facets.contains(ItemFacet.STORAGE)
                || facets.contains(ItemFacet.MACHINE)
                || path.contains("charger")
                || path.contains("charging")
                || path.contains("compressor")
                || path.contains("chamber")) {
            return "machines";
        }
        return "parts";
    }

    private static boolean shouldBeGeology(Set<ItemFacet> facets, String path) {
        if (facets.contains(ItemFacet.STONE_BLOCK)) {
            return !hasAny(facets,
                    ItemFacet.STAIRS,
                    ItemFacet.SLAB,
                    ItemFacet.WALL,
                    ItemFacet.FENCE,
                    ItemFacet.FENCE_GATE,
                    ItemFacet.PANE,
                    ItemFacet.DOOR,
                    ItemFacet.TRAPDOOR)
                    && !path.contains("brick")
                    && !path.contains("polished")
                    && !path.contains("tile")
                    && !path.contains("glass");
        }
        if (facets.contains(ItemFacet.SOIL_BLOCK)) {
            return !path.contains("terracotta") && !path.contains("concrete");
        }
        return false;
    }

    private static boolean shouldBiasUncraftableFullBlockToTerrain(Set<ItemFacet> facets, Map<String, String> attributes) {
        if (!facets.contains(ItemFacet.PLACEABLE)) {
            return false;
        }
        if (!"no_recipe".equals(attributes.getOrDefault(SearchNodeKeys.OBTAINABILITY, ""))) {
            return false;
        }
        String blockShape = attributes.getOrDefault("blockShape", "");
        return blockShape.isBlank() || "full_block".equals(blockShape);
    }

    private static String classifyUncraftableTerrainSubcategory(Set<ItemFacet> facets, Map<String, String> attributes) {
        if (facets.contains(ItemFacet.SOIL_BLOCK)) {
            return "terrain";
        }
        String blocksMaterial = attributes.getOrDefault(SearchNodeKeys.BLOCKS_MATERIAL, "");
        if ("stone".equals(blocksMaterial) || facets.contains(ItemFacet.STONE_BLOCK)) {
            return "stone";
        }
        return "terrain";
    }

    private static String classifyMasonrySubcategory(Set<ItemFacet> facets, Map<String, String> attributes) {
        if (facets.contains(ItemFacet.REDSTONE_LOGIC) || facets.contains(ItemFacet.REDSTONE_SIGNAL)) {
            return "redstone";
        }
        if (hasAny(facets, ItemFacet.DOOR, ItemFacet.TRAPDOOR, ItemFacet.FENCE_GATE)) {
            return "functional";
        }
        if (facets.contains(ItemFacet.STAIRS)) return "stairs";
        if (facets.contains(ItemFacet.SLAB)) return "slab";
        if (facets.contains(ItemFacet.WALL)) return "wall";
        if (facets.contains(ItemFacet.FENCE) || facets.contains(ItemFacet.FENCE_GATE)) return "fence";
        if (facets.contains(ItemFacet.PANE)) return "pane";

        String blockShape = attributes.getOrDefault("blockShape", "");
        if (!blockShape.isBlank()) {
            return switch (blockShape) {
                case "stairs", "slab", "wall", "fence", "pane", "door", "trapdoor", "fence_gate" -> blockShape.equals("fence_gate") ? "fence" : blockShape;
                default -> "full_block";
            };
        }
        return "full_block";
    }
}
