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

    private static final Set<String> DECOR_TOKENS = Set.of(
            "chair", "stool", "bench", "sofa", "couch", "banister", "railing",
            "shelf", "curtain", "curtains", "blinds", "shutter", "shutters",
            "dresser", "nightstand", "wardrobe", "bookcase", "bookshelf",
            "lamp", "lantern", "chandelier", "sconce", "brazier", "seat",
            "pillow", "cushion", "hammock", "rack", "sign"
    );

    private static final Set<String> LIGHTING_TOKENS = Set.of(
            "lamp", "lantern", "chandelier", "sconce", "brazier", "candelabra"
    );

    private static final Set<String> TEXTILE_TOKENS = Set.of(
            "curtain", "curtains", "blinds", "shutter", "shutters", "pillow",
            "cushion", "rug", "carpet", "blanket", "sheet"
    );

    private static final Set<String> DISPLAY_TOKENS = Set.of(
            "bookcase", "bookshelf", "shelf", "rack", "sign", "plaque"
    );

    private static final Set<String> WORKSTATION_TOKENS = Set.of(
            "station", "terminal", "controller", "assembler", "fabricator",
            "charger", "press", "mixer", "crusher", "grinder", "smelter",
            "refinery", "processor", "forge", "loom", "workbench"
    );

    private static final Set<String> CREATE_HANDHELD_TOOL_TOKENS = Set.of(
            "wrench", "grip", "worldshaper", "cannon"
    );

    private static final Set<String> CREATE_HANDHELD_UTILITY_TOKENS = Set.of(
            "goggle", "filter", "schedule", "shopping", "list", "schematic",
            "quill", "glue", "controller"
    );

    private static final Set<String> CREATE_PART_TOKENS = Set.of(
            "belt", "gearbox", "bracket", "pipe", "valve", "handle", "shaft",
            "cogwheel", "chute", "depot", "ejector", "speedometer", "stressometer"
    );

    private static final Set<String> CREATE_MACHINE_TOKENS = Set.of(
            "press", "burner", "wheel", "millstone", "mixer", "fan", "pump",
            "tank", "backtank", "engine"
    );

    private static final Set<String> RAILWAYS_TRANSPORT_TOKENS = Set.of(
            "track", "coupler", "conductor", "switch", "semaphore", "handcar"
    );

    private static final Set<String> RAILWAYS_PART_TOKENS = Set.of(
            "smokestack", "boiler", "buffer", "cowcatcher", "headstock", "link", "connector"
    );

    private static final Set<String> CREATE_ADDON_MACHINE_TOKENS = Set.of(
            "alternator", "motor", "generator", "charger", "accumulator", "battery"
    );

    private static final Set<String> CREATE_ADDON_PART_TOKENS = Set.of(
            "connector", "cable", "coil", "wire", "electrode"
    );

    private static final Set<String> CREATE_WINERY_MACHINE_TOKENS = Set.of(
            "barrel", "vat", "press", "ferment"
    );

    private static final Set<String> CREATE_ORE_MACHINE_TOKENS = Set.of(
            "drill", "pump", "extract", "boring", "survey"
    );

    private static final Set<String> CREATE_ORE_PART_TOKENS = Set.of(
            "pipe", "vein", "sample", "core"
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
        if (hasAny(facets, ItemFacet.SPAWN_EGG, ItemFacet.MOB_BUCKET)) {
            return assignment("bestiary", classifyBestiarySubcategory(path), attributes);
        }
        if (hasAny(facets, ItemFacet.POTION, ItemFacet.ENCHANTED_BOOK, ItemFacet.MAGIC_ARTIFACT, ItemFacet.MAGIC_REAGENT)) {
            return assignment("magic", classifyMagicSubcategory(facets), attributes);
        }
        if (hasAny(facets, ItemFacet.UTILITY_NAVIGATION, ItemFacet.UTILITY_MISC)) {
            return assignment("utility", classifyUtilitySubcategory(facets), attributes);
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
        if (shouldBiasLexicalDecoration(facets, path)) {
            return assignment("decoration", classifyLexicalDecorationSubcategory(path, facets), attributes);
        }
        if (shouldBiasLexicalWorkstationToTech(facets, path)) {
            return assignment("tech", classifyLexicalTechSubcategory(path, facets), attributes);
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
        if (shouldBiasOrganicSurfaceBlockToNature(facets, path, attributes)) {
            return assignment("nature", classifyOrganicSurfaceBlockSubcategory(path, facets), attributes);
        }
        if (shouldBiasGeologyFamilyToDecoration(facets, path, attributes)) {
            return assignment("decoration", classifyDecorationSubcategory(facets), attributes);
        }
        if (shouldBiasGeologyFamilyToMasonry(facets, path, attributes)) {
            return assignment("masonry", classifyMasonrySubcategory(facets, attributes), attributes);
        }
        if (shouldBeGeology(facets, path, attributes)) {
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

    private static String classifyLexicalDecorationSubcategory(String path, Set<ItemFacet> facets) {
        if (facets.contains(ItemFacet.LIGHT_SOURCE) || containsPathToken(path, LIGHTING_TOKENS)) {
            return "lighting";
        }
        if (containsPathToken(path, TEXTILE_TOKENS)) {
            return "textiles";
        }
        if (containsPathToken(path, DISPLAY_TOKENS)) {
            return "furniture";
        }
        return classifyDecorationSubcategory(facets);
    }

    private static String classifySocialSubcategory(Set<ItemFacet> facets) {
        if (facets.contains(ItemFacet.SOCIAL_PLAYERS)) return "players";
        if (facets.contains(ItemFacet.SOCIAL_CLAIMS)) return "claims";
        return "teams";
    }

    private static String classifyLexicalTechSubcategory(String path, Set<ItemFacet> facets) {
        if (facets.contains(ItemFacet.REDSTONE_LOGIC) || facets.contains(ItemFacet.REDSTONE_SIGNAL)) {
            return "redstone";
        }
        if (containsPathToken(path, Set.of("terminal", "controller", "processor"))) {
            return "parts";
        }
        return classifyTechSubcategory(facets);
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

    private static boolean shouldBiasLexicalDecoration(Set<ItemFacet> facets, String path) {
        return facets.contains(ItemFacet.PLACEABLE)
                && !hasAny(facets,
                ItemFacet.INTERACTIVE_BLOCK,
                ItemFacet.MACHINE,
                ItemFacet.STORAGE,
                ItemFacet.HAS_ENERGY,
                ItemFacet.REDSTONE_LOGIC,
                ItemFacet.REDSTONE_SIGNAL,
                ItemFacet.TRANSPORT,
                ItemFacet.FUNGI,
                ItemFacet.NATURE_MISC,
                ItemFacet.CROP,
                ItemFacet.SEED,
                ItemFacet.LOG,
                ItemFacet.LEAVES,
                ItemFacet.FLOWER)
                && containsPathToken(path, DECOR_TOKENS);
    }

    private static boolean shouldBiasLexicalWorkstationToTech(Set<ItemFacet> facets, String path) {
        return containsPathToken(path, WORKSTATION_TOKENS)
                && facets.contains(ItemFacet.PLACEABLE)
                && hasAny(facets,
                ItemFacet.INTERACTIVE_BLOCK,
                ItemFacet.HAS_BLOCK_ENTITY,
                ItemFacet.MACHINE,
                ItemFacet.STORAGE,
                ItemFacet.HAS_ENERGY,
                ItemFacet.REDSTONE_LOGIC,
                ItemFacet.REDSTONE_SIGNAL,
                ItemFacet.TRANSPORT)
                && !hasAny(facets,
                ItemFacet.FUNGI,
                ItemFacet.NATURE_MISC,
                ItemFacet.CROP,
                ItemFacet.SEED,
                ItemFacet.LOG,
                ItemFacet.LEAVES,
                ItemFacet.FLOWER,
                ItemFacet.LIGHT_SOURCE);
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
                && !shouldBeGeology(facets, path, attributes);
    }

    private static boolean shouldBiasCreateFamilyHandheldToTools(ModFamily modFamily, String path) {
        return modFamily == ModFamily.CREATE
                && (containsPathToken(path, CREATE_HANDHELD_TOOL_TOKENS)
                || path.equals("wand_of_symmetry")
                || endsWithPathToken(path, "gun"));
    }

    private static boolean shouldBiasCreateFamilyHandheldToUtility(ModFamily modFamily, String path) {
        return modFamily == ModFamily.CREATE
                && (containsPathToken(path, CREATE_HANDHELD_UTILITY_TOKENS)
                || path.equals("shopping_list"));
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
                && !shouldBeGeology(facets, path, attributes)
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
                && !shouldBeGeology(facets, path, attributes)
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
                && !shouldBeGeology(facets, path, attributes)
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
                && !shouldBeGeology(facets, path, attributes)
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
            if (containsPathToken(path, RAILWAYS_TRANSPORT_TOKENS)) {
                return "transport";
            }
            if (containsPathToken(path, RAILWAYS_PART_TOKENS)) {
                return "parts";
            }
        }
        if (modId.equals("createaddition")
                || modId.equals("create_new_age")
                || modId.equals("new_age")) {
            if (containsPathToken(path, CREATE_ADDON_MACHINE_TOKENS)) {
                return "machines";
            }
            if (containsPathToken(path, CREATE_ADDON_PART_TOKENS)) {
                return "parts";
            }
        }
        if (modId.equals("create_winery")) {
            if (containsPathToken(path, Set.of("bottle"))) {
                return "transport";
            }
            if (containsPathToken(path, CREATE_WINERY_MACHINE_TOKENS)) {
                return "machines";
            }
        }
        if (modId.equals("createoreexcavation")) {
            if (containsPathToken(path, CREATE_ORE_MACHINE_TOKENS)) {
                return "machines";
            }
            if (containsPathToken(path, CREATE_ORE_PART_TOKENS)) {
                return "parts";
            }
        }
        if (facets.contains(ItemFacet.TRANSPORT)) {
            return "transport";
        }
        if (facets.contains(ItemFacet.REDSTONE_LOGIC) || facets.contains(ItemFacet.REDSTONE_SIGNAL)) {
            return "redstone";
        }
        if (containsPathToken(path, CREATE_PART_TOKENS)) {
            return "parts";
        }
        if (containsPathToken(path, CREATE_MACHINE_TOKENS)) {
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

    private static boolean shouldBiasOrganicSurfaceBlockToNature(Set<ItemFacet> facets, String path, Map<String, String> attributes) {
        if (!facets.contains(ItemFacet.PLACEABLE)) {
            return false;
        }
        if (hasAny(facets,
                ItemFacet.STAIRS,
                ItemFacet.SLAB,
                ItemFacet.WALL,
                ItemFacet.FENCE,
                ItemFacet.FENCE_GATE,
                ItemFacet.PANE,
                ItemFacet.DOOR,
                ItemFacet.TRAPDOOR)) {
            return false;
        }
        String blocksMaterial = attributes.getOrDefault(SearchNodeKeys.BLOCKS_MATERIAL, "");
        return facets.contains(ItemFacet.FUNGI)
                || path.contains("nylium")
                || path.contains("mycelium")
                || path.contains("moss")
                || path.contains("lichen")
                || path.contains("fungi")
                || path.contains("fungus")
                || (blocksMaterial.equals("soil") && path.contains("grass"));
    }

    private static String classifyOrganicSurfaceBlockSubcategory(String path, Set<ItemFacet> facets) {
        if (facets.contains(ItemFacet.FUNGI)
                || path.contains("nylium")
                || path.contains("mycelium")
                || path.contains("fungi")
                || path.contains("fungus")) {
            return "fungi";
        }
        return "flora";
    }

    private static boolean shouldBiasGeologyFamilyToDecoration(Set<ItemFacet> facets, String path, Map<String, String> attributes) {
        if (!facets.contains(ItemFacet.PLACEABLE)) {
            return false;
        }
        if (hasAny(facets,
                ItemFacet.INTERACTIVE_BLOCK,
                ItemFacet.HAS_BLOCK_ENTITY,
                ItemFacet.MACHINE,
                ItemFacet.STORAGE,
                ItemFacet.HAS_ENERGY,
                ItemFacet.REDSTONE_LOGIC,
                ItemFacet.REDSTONE_SIGNAL,
                ItemFacet.TRANSPORT,
                ItemFacet.FUNGI,
                ItemFacet.NATURE_MISC,
                ItemFacet.FLOWER,
                ItemFacet.LOG,
                ItemFacet.LEAVES)) {
            return false;
        }
        String blocksMaterial = attributes.getOrDefault(SearchNodeKeys.BLOCKS_MATERIAL, "");
        return path.contains("window")
                || containsPathToken(path, DECOR_TOKENS)
                || path.contains("desk")
                || (facets.contains(ItemFacet.PANE) && (blocksMaterial.equals("glass") || path.contains("glass")));
    }

    private static boolean shouldBiasGeologyFamilyToMasonry(Set<ItemFacet> facets, String path, Map<String, String> attributes) {
        if (!facets.contains(ItemFacet.PLACEABLE)) {
            return false;
        }
        if (hasAny(facets,
                ItemFacet.RAIL,
                ItemFacet.INTERACTIVE_BLOCK,
                ItemFacet.HAS_BLOCK_ENTITY,
                ItemFacet.MACHINE,
                ItemFacet.STORAGE,
                ItemFacet.HAS_ENERGY,
                ItemFacet.REDSTONE_LOGIC,
                ItemFacet.REDSTONE_SIGNAL,
                ItemFacet.TRANSPORT)) {
            return false;
        }
        String blockShape = attributes.getOrDefault("blockShape", "");
        return hasAny(facets,
                ItemFacet.STAIRS,
                ItemFacet.SLAB,
                ItemFacet.WALL,
                ItemFacet.FENCE,
                ItemFacet.FENCE_GATE,
                ItemFacet.PANE,
                ItemFacet.DOOR,
                ItemFacet.TRAPDOOR)
                || blockShape.equals("stairs")
                || blockShape.equals("slab")
                || blockShape.equals("wall")
                || blockShape.equals("fence")
                || blockShape.equals("fence_gate")
                || blockShape.equals("pane")
                || blockShape.equals("door")
                || blockShape.equals("trapdoor")
                || path.contains("brick")
                || path.contains("bricks")
                || path.contains("tile")
                || path.contains("paver")
                || path.contains("paving")
                || path.contains("plank")
                || path.contains("board")
                || path.contains("pillar")
                || path.contains("column")
                || path.contains("polished")
                || path.contains("chiseled")
                || path.contains("carved")
                || path.contains("cut_")
                || path.endsWith("_cut");
    }

    private static boolean containsPathToken(String path, Set<String> expectedTokens) {
        String[] pathTokens = path.split("[_/]");
        for (String pathToken : pathTokens) {
            if (expectedTokens.contains(pathToken)) {
                return true;
            }
        }
        return false;
    }

    private static boolean endsWithPathToken(String path, String token) {
        String[] pathTokens = path.split("[_/]");
        return pathTokens.length > 0 && pathTokens[pathTokens.length - 1].equals(token);
    }

    private static boolean shouldBeGeology(Set<ItemFacet> facets, String path, Map<String, String> attributes) {
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
                    && !path.contains("glass")
                    && !path.contains("window")
                    && !path.contains("plank")
                    && !path.contains("board")
                    && !path.contains("pillar")
                    && !path.contains("column")
                    && !path.contains("paver")
                    && !path.contains("paving")
                    && !path.contains("chiseled")
                    && !path.contains("carved");
        }
        if (facets.contains(ItemFacet.SOIL_BLOCK)) {
            return !path.contains("terracotta")
                    && !path.contains("concrete")
                    && !path.contains("nylium")
                    && !path.contains("mycelium")
                    && !path.contains("moss")
                    && !path.contains("fungi")
                    && !path.contains("fungus");
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
