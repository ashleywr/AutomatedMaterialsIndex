package com.sanhiruzu.ami.index;

import net.minecraft.resources.ResourceLocation;

import java.util.*;
import java.util.function.Function;
import java.util.function.Predicate;

public final class PrimaryCategoryResolver {
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
    private static final Set<String> ARCHITECTURAL_BUILDING_TOKENS = Set.of(
            "stair", "stairs", "roof", "balcony", "bridge", "railing", "banister",
            "parapet", "platform", "path", "paving", "paver", "window", "pier"
    );
    private static final Set<String> WORKSTATION_TOKENS = Set.of(
            "station", "terminal", "controller", "assembler", "fabricator",
            "charger", "press", "mixer", "crusher", "grinder", "smelter",
            "refinery", "processor", "forge", "loom", "workbench", "stove", "oven"
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
            "cogwheel", "chute", "depot", "ejector", "speedometer", "stressometer",
            "casing"
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
    private static final Set<String> FOOD_STORAGE_TOKENS = Set.of(
            "crate", "bag", "bale", "sack"
    );
    private static final List<PrimaryRule> PRIMARY_RULES = List.of(
            rule("create handheld tools",
                    c -> shouldBiasCreateFamilyHandheldToTools(c.modFamily, c.path),
                    c -> assignment("tools", classifyCreateFamilyToolSubcategory(c.path), c.attributes)),
            rule("create handheld utility",
                    c -> shouldBiasCreateFamilyHandheldToUtility(c.modFamily, c.path),
                    c -> assignment("utility", "misc", c.attributes)),
            rule("create enchanting magic",
                    c -> shouldBiasCreateEnchantingFamilyToMagic(c.modId, c.path),
                    c -> assignment("magic", "reagents", c.attributes)),
            rule("spawn eggs and mob buckets",
                    c -> hasAny(c.facets, ItemFacet.SPAWN_EGG, ItemFacet.MOB_BUCKET),
                    c -> assignment("bestiary", classifyBestiarySubcategory(c.path), c.attributes)),
            rule("saplings",
                    c -> isSapling(c.path, c.attributes),
                    c -> assignment("nature", "flora", c.attributes)),
            rule("leaves",
                    c -> isLeaves(c.path, c.facets, c.attributes),
                    c -> assignment("nature", "flora", c.attributes)),
            rule("wood blocks",
                    c -> isWoodBlock(c.path, c.facets, c.attributes),
                    c -> assignment("nature", "wood", c.attributes)),
            rule("plant seeds",
                    c -> isPlantSeed(c.path, c.facets, c.attributes),
                    c -> assignment("nature", "seeds", c.attributes)),
            rule("crop-like placeables",
                    c -> isCropLikePlaceable(c.path, c.facets, c.attributes),
                    c -> assignment("nature", "crops", c.attributes)),
            rule("food family placeables",
                    c -> shouldBiasFoodFamilyPlaceableToNature(c.modFamily, c.facets, c.path, c.attributes),
                    c -> assignment("nature", classifyFoodFamilyNatureSubcategory(c.path, c.facets), c.attributes)),
            rule("magic facets",
                    c -> hasAny(c.facets, ItemFacet.POTION, ItemFacet.ENCHANTED_BOOK, ItemFacet.MAGIC_ARTIFACT, ItemFacet.MAGIC_REAGENT),
                    c -> assignment("magic", classifyMagicSubcategory(c.facets), c.attributes)),
            rule("utility facets",
                    c -> hasAny(c.facets, ItemFacet.UTILITY_NAVIGATION, ItemFacet.UTILITY_MEDICAL, ItemFacet.UTILITY_CURRENCY, ItemFacet.UTILITY_MISC),
                    c -> assignment("utility", classifyUtilitySubcategory(c.facets), c.attributes)),
            rule("clear ingredients before incidental equipment or tech",
                    c -> shouldResolveIngredientBeforeEquipmentTech(c.facets),
                    c -> assignment("ingredients", classifyIngredientSubcategory(c.facets), c.attributes)),
            rule("armor and real curios",
                    c -> shouldResolveAsArmorOrCurio(c.facets),
                    c -> assignment("armor", classifyArmorSubcategory(c.facets), c.attributes)),
            rule("throwable ingredients",
                    c -> isThrowableIngredient(c.facets),
                    c -> assignment("ingredients", classifyIngredientSubcategory(c.facets), c.attributes)),
            rule("tools and weapons",
                    c -> hasAny(c.facets, ItemFacet.MELEE_WEAPON, ItemFacet.RANGED_WEAPON, ItemFacet.HARVEST_TOOL, ItemFacet.UTILITY_TOOL)
                            || (c.facets.contains(ItemFacet.PROJECTILE) && hasProjectileToolContext(c.path, c.attributes)),
                    c -> assignment("tools", classifyWeaponSubcategory(c.facets), c.attributes)),
            rule("food before passive redstone",
                    c -> shouldResolveFoodLikeBeforePassiveRedstone(c.modFamily, c.facets, c.path),
                    c -> assignment("nature", classifyFoodFamilyPreparedSubcategory(c.path, c.facets), c.attributes)),
            rule("decoration before passive redstone",
                    c -> shouldResolveDecorLikeBeforePassiveRedstone(c.modFamily, c.facets, c.attributes),
                    c -> assignment("decoration", classifyDecorationSubcategory(c.facets), c.attributes)),
            rule("natural cable false positives",
                    c -> shouldResolveNaturalBeforeTech(c.facets, c.path),
                    c -> assignment("nature", classifyNatureSubcategory(c.path, c.facets), c.attributes)),
            rule("tech facets",
                    c -> hasAny(c.facets,
                            ItemFacet.HAS_ENERGY,
                            ItemFacet.STORAGE,
                            ItemFacet.INTERACTIVE_BLOCK,
                            ItemFacet.ACTIVE_REDSTONE_LOGIC,
                            ItemFacet.PASSIVE_COMPARATOR_OUTPUT,
                            ItemFacet.REDSTONE_LOGIC,
                            ItemFacet.REDSTONE_SIGNAL,
                            ItemFacet.TRANSPORT,
                            ItemFacet.MACHINE,
                            ItemFacet.WORKSTATION,
                            ItemFacet.CABLE,
                            ItemFacet.UPGRADE,
                            ItemFacet.TEMPLATE,
                            ItemFacet.TECH_COMPONENT,
                            ItemFacet.MECHANICAL_COMPONENT,
                            ItemFacet.INGOT,
                            ItemFacet.GEM,
                            ItemFacet.NUGGET,
                            ItemFacet.RAW_MATERIAL,
                            ItemFacet.DUST),
                    c -> assignment("tech", classifyTechSubcategory(c.path, c.facets), c.attributes)),
            rule("nature facets",
                    c -> hasAny(c.facets, ItemFacet.EDIBLE, ItemFacet.PLACEABLE_FOOD, ItemFacet.FOOD_MEAL, ItemFacet.FOOD_DRINK, ItemFacet.FOOD_PROTEIN, ItemFacet.COMPOSTABLE, ItemFacet.SEED, ItemFacet.CROP, ItemFacet.NATURE_MISC, ItemFacet.FUNGI, ItemFacet.LOG, ItemFacet.LEAVES, ItemFacet.FLOWER),
                    c -> assignment("nature", classifyNatureSubcategory(c.path, c.facets), c.attributes)),
            rule("ingredient facets",
                    c -> hasAny(c.facets, ItemFacet.INGREDIENT_ORGANIC, ItemFacet.INGREDIENT_MINERAL, ItemFacet.INGREDIENT_DYE),
                    c -> assignment("ingredients", classifyIngredientSubcategory(c.facets), c.attributes)),
            rule("structural building shapes",
                    c -> c.facets.contains(ItemFacet.PLACEABLE)
                            && hasAny(c.facets,
                            ItemFacet.STAIRS,
                            ItemFacet.SLAB,
                            ItemFacet.WALL,
                            ItemFacet.FENCE,
                            ItemFacet.FENCE_GATE,
                            ItemFacet.PANE,
                            ItemFacet.DOOR,
                            ItemFacet.TRAPDOOR),
                    c -> assignment("masonry", classifyMasonrySubcategory(c.facets, c.path, c.attributes), c.attributes)),
            rule("decoration facets",
                    c -> shouldResolveDecorationFacetPrimary(c.path, c.facets),
                    c -> assignment("decoration", classifyDecorationSubcategory(c.facets), c.attributes)),
            rule("social facets",
                    c -> hasAny(c.facets, ItemFacet.SOCIAL_PLAYERS, ItemFacet.SOCIAL_CLAIMS),
                    c -> assignment("social", classifySocialSubcategory(c.facets), c.attributes)),
            rule("architectural placeables",
                    c -> shouldBiasArchitecturalPlaceableToBuilding(c.facets, c.path, c.attributes),
                    c -> assignment("masonry", classifyMasonrySubcategory(c.facets, c.path, c.attributes), c.attributes)),
            rule("lexical decoration",
                    c -> shouldBiasLexicalDecoration(c.facets, c.path),
                    c -> assignment("decoration", classifyLexicalDecorationSubcategory(c.path, c.facets), c.attributes)),
            rule("lexical workstation tech",
                    c -> shouldBiasLexicalWorkstationToTech(c.facets, c.path),
                    c -> assignment("tech", classifyLexicalTechSubcategory(c.path, c.facets), c.attributes)),
            rule("food family ingredients",
                    c -> shouldBiasFoodFamilyToIngredients(c.modFamily, c.facets, c.path),
                    c -> assignment("ingredients", classifyFoodFamilyIngredientSubcategory(c.path, c.facets), c.attributes)),
            rule("food family prepared food",
                    c -> shouldBiasFoodFamilyToPreparedFood(c.modFamily, c.facets, c.path),
                    c -> assignment("nature", classifyFoodFamilyPreparedSubcategory(c.path, c.facets), c.attributes)),
            rule("decor family decoration",
                    c -> shouldBiasDecorFamilyToDecoration(c.modFamily, c.facets, c.path, c.attributes),
                    c -> assignment("decoration", classifyDecorationSubcategory(c.facets), c.attributes)),
            rule("portable storage armor",
                    c -> shouldBiasPortableStorageFamilyToArmor(c.modFamily, c.path),
                    c -> assignment("armor", "curios", c.attributes)),
            rule("portable storage upgrades",
                    c -> shouldBiasPortableStorageFamilyToTech(c.modFamily, c.path),
                    c -> assignment("tech", "upgrades", c.attributes)),
            rule("storage family tech",
                    c -> shouldBiasStorageFamilyToTech(c.modFamily, c.facets, c.path, c.attributes),
                    c -> assignment("tech", classifyStorageSubcategory(c.path, c.facets), c.attributes)),
            rule("automation family tech",
                    c -> shouldBiasAutomationFamilyToTech(c.modFamily, c.facets, c.path, c.attributes),
                    c -> assignment("tech", classifyAutomationSubcategory(c.path, c.facets), c.attributes)),
            rule("create family decoration",
                    c -> shouldBiasCreateFamilyToDecoration(c.modFamily, c.facets),
                    c -> assignment("decoration", classifyDecorationSubcategory(c.facets), c.attributes)),
            rule("create family tech",
                    c -> shouldBiasCreateFamilyToTech(c.modFamily, c.facets, c.path, c.attributes),
                    c -> assignment("tech", classifyCreateFamilyTechSubcategory(c.modId, c.path, c.facets), c.attributes)),
            rule("food family nature",
                    c -> shouldBiasFoodFamilyToNature(c.modFamily, c.facets, c.path, c.attributes),
                    c -> assignment("nature", classifyFoodFamilyNatureSubcategory(c.path, c.facets), c.attributes)),
            rule("organic surface blocks",
                    c -> shouldBiasOrganicSurfaceBlockToNature(c.facets, c.path, c.attributes),
                    c -> assignment("nature", classifyOrganicSurfaceBlockSubcategory(c.path, c.facets), c.attributes)),
            rule("geology family decoration",
                    c -> shouldBiasGeologyFamilyToDecoration(c.facets, c.path, c.attributes),
                    c -> assignment("decoration", classifyDecorationSubcategory(c.facets), c.attributes)),
            rule("geology family masonry",
                    c -> shouldBiasGeologyFamilyToMasonry(c.facets, c.path, c.attributes),
                    c -> assignment("masonry", classifyMasonrySubcategory(c.facets, c.path, c.attributes), c.attributes)),
            rule("geology blocks",
                    c -> shouldBeGeology(c.facets, c.path, c.attributes),
                    c -> assignment("geology", c.facets.contains(ItemFacet.SOIL_BLOCK) ? "terrain" : "stone", c.attributes)),
            rule("uncraftable terrain blocks",
                    c -> shouldBiasUncraftableFullBlockToTerrain(c.facets, c.attributes),
                    c -> assignment("geology", classifyUncraftableTerrainSubcategory(c.facets, c.attributes), c.attributes)),
            rule("remaining placeables",
                    c -> c.facets.contains(ItemFacet.PLACEABLE),
                    c -> assignment("masonry", classifyMasonrySubcategory(c.facets, c.path, c.attributes), c.attributes))
    );

    private PrimaryCategoryResolver() {
    }

    public static CategoryAssignment resolve(ResourceLocation id, FacetProfile profile) {
        if (id == null) {
            return fallback();
        }

        String modId = id.getNamespace().toLowerCase(Locale.ROOT);
        String path = id.getPath().toLowerCase(Locale.ROOT);
        var facets = profile.facets();
        var attributes = new HashMap<>(profile.attributes());
        ModFamily modFamily = classifyModFamily(modId);
        ResolveContext context = new ResolveContext(id, modId, path, facets, attributes, modFamily);

        Optional<CategoryAssignment> hardIdentity = resolveHardIdentity(context);
        if (hardIdentity.isPresent()) {
            return hardIdentity.get();
        }

        Optional<CategoryAssignment> scored = CategoryScorer.resolveStrong(id, profile);
        if (scored.isPresent()) {
            return scored.get();
        }

        for (PrimaryRule rule : PRIMARY_RULES) {
            if (rule.matches.test(context)) {
                return rule.assignment.apply(context);
            }
        }
        Optional<CategoryAssignment> fallbackScored = CategoryScorer.resolve(id, profile);
        if (fallbackScored.isPresent()) {
            return fallbackScored.get();
        }
        return fallback();
    }

    private static Optional<CategoryAssignment> resolveHardIdentity(ResolveContext context) {
        if (hasAny(context.facets, ItemFacet.SPAWN_EGG, ItemFacet.MOB_BUCKET)) {
            return Optional.of(identityAssignment(
                    "bestiary",
                    classifyBestiarySubcategory(context.path),
                    context.attributes,
                    "identity.spawn_egg",
                    "spawn egg or mob bucket facet"
            ));
        }

        if (context.facets.contains(ItemFacet.DECORATIVE_BLOCK)
                && containsPathToken(context.path, TEXTILE_TOKENS)) {
            return Optional.of(identityAssignment(
                    "decoration",
                    "textiles",
                    context.attributes,
                    "identity.decorative_textile",
                    "decorative textile block identity"
            ));
        }

        if (shouldResolveAsArmorOrCurio(context.facets)) {
            return Optional.of(identityAssignment(
                    "armor",
                    classifyArmorSubcategory(context.facets),
                    context.attributes,
                    "identity.armor",
                    "armor, equipment slot, or curio facet"
            ));
        }

        if (hasHardToolIdentity(context.facets)) {
            return Optional.of(identityAssignment(
                    "tools",
                    classifyWeaponSubcategory(context.facets),
                    context.attributes,
                    "identity.tool",
                    "non-projectile tool or weapon facet"
            ));
        }

        if (hasActualFoodIdentity(context.facets, context.attributes)) {
            return Optional.of(identityAssignment(
                    "nature",
                    classifyNatureSubcategory(context.path, context.facets),
                    context.attributes,
                    "identity.food",
                    "food data component or edible facet"
            ));
        }

        if (isSapling(context.path, context.attributes) || isLeaves(context.path, context.facets, context.attributes)) {
            return Optional.of(identityAssignment(
                    "nature",
                    "flora",
                    context.attributes,
                    "identity.flora",
                    "sapling or leaves block identity"
            ));
        }

        if (isWoodBlock(context.path, context.facets, context.attributes)) {
            return Optional.of(identityAssignment(
                    "nature",
                    "wood",
                    context.attributes,
                    "identity.wood",
                    "log or wood block identity"
            ));
        }

        if (isPlantSeed(context.path, context.facets, context.attributes)) {
            return Optional.of(identityAssignment(
                    "nature",
                    "seeds",
                    context.attributes,
                    "identity.seed",
                    "plant seed identity"
            ));
        }

        if (isCropLikePlaceable(context.path, context.facets, context.attributes)) {
            return Optional.of(identityAssignment(
                    "nature",
                    "crops",
                    context.attributes,
                    "identity.crop",
                    "crop block identity"
            ));
        }

        if (context.facets.contains(ItemFacet.PLACEABLE)
                && hasAny(context.facets,
                ItemFacet.STAIRS,
                ItemFacet.SLAB,
                ItemFacet.WALL,
                ItemFacet.FENCE,
                ItemFacet.FENCE_GATE,
                ItemFacet.PANE,
                ItemFacet.DOOR,
                ItemFacet.TRAPDOOR)) {
            return Optional.of(identityAssignment(
                    "masonry",
                    classifyMasonrySubcategory(context.facets, context.path, context.attributes),
                    context.attributes,
                    "identity.block_shape",
                    "structural block shape facet"
            ));
        }

        return Optional.empty();
    }

    private static CategoryAssignment identityAssignment(
            String categoryId,
            String subcategoryId,
            Map<String, String> attributes,
            String evidenceId,
            String reason
    ) {
        Map<String, String> diagnosticAttributes = new LinkedHashMap<>(attributes);
        diagnosticAttributes.put("classificationMode", "hard_identity");
        diagnosticAttributes.put("classificationScore", "1000");
        diagnosticAttributes.put("classificationEvidence", "+1000 " + evidenceId + "[" + reason + "]");
        diagnosticAttributes.put("classificationScores", categoryId + "/" + subcategoryId + "=1000");
        return assignment(categoryId, subcategoryId, diagnosticAttributes);
    }

    private static boolean hasHardToolIdentity(Set<ItemFacet> facets) {
        return hasAny(facets,
                ItemFacet.MELEE_WEAPON,
                ItemFacet.HARVEST_TOOL,
                ItemFacet.UTILITY_TOOL)
                || (facets.contains(ItemFacet.RANGED_WEAPON) && !facets.contains(ItemFacet.PROJECTILE));
    }

    private static boolean hasProjectileToolContext(String path, Map<String, String> attributes) {
        return containsPathToken(path, Set.of(
                "arrow", "arrows", "bolt", "bolts", "bullet", "bullets", "round", "rounds",
                "cartridge", "cartridges", "rocket", "ammo", "gun", "shotgun", "cannon",
                "autocannon", "artillery", "mortar", "munition", "munitions"))
                || hasMetadataToken(attributes, SearchNodeKeys.TAGS, "minecraft:arrows")
                || hasMetadataToken(attributes, SearchNodeKeys.TAGS, "createbigcannons:big_cannon_projectiles")
                || hasMetadataToken(attributes, SearchNodeKeys.BLOCK_TAGS, "createbigcannons:big_cannon_projectiles");
    }

    private static boolean hasActualFoodIdentity(Set<ItemFacet> facets, Map<String, String> attributes) {
        return hasAny(facets, ItemFacet.EDIBLE, ItemFacet.PLACEABLE_FOOD)
                || hasCsvToken(attributes.getOrDefault(SearchNodeKeys.COMPONENT_FACTS, ""), "food");
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
                || modId.equals("cfm")
                || modId.equals("cfm_wap")
                || modId.equals("redeco")
                || modId.equals("another_furniture")
                || modId.equals("moa_decor_bath")
                || modId.equals("refurbished_furniture")
                || modId.equals("arts_and_crafts")) {
            return true;
        }
        return false;
    }

    private static boolean isAutomationFamilyMod(String modId) {
        return modId.equals("pneumaticcraft")
                || modId.startsWith("pneumaticcraft")
                || modId.equals("securitycraft")
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

    private static PrimaryRule rule(String id,
                                    Predicate<ResolveContext> matches,
                                    Function<ResolveContext, CategoryAssignment> assignment) {
        return new PrimaryRule(id, matches, assignment);
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
        if (facets.contains(ItemFacet.UTILITY_MEDICAL)) return "medical";
        if (facets.contains(ItemFacet.UTILITY_CURRENCY)) return "currency";
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
        if (facets.contains(ItemFacet.ARMOR_ANIMAL)) return "animal";
        if (facets.contains(ItemFacet.CURIO)) return "curios";
        return "curios";
    }

    private static String classifyWeaponSubcategory(Set<ItemFacet> facets) {
        if (facets.contains(ItemFacet.MELEE_WEAPON)) return "melee";
        if (facets.contains(ItemFacet.RANGED_WEAPON)) return "ranged";
        if (facets.contains(ItemFacet.PROJECTILE)) return "ammo";
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

    private static boolean shouldResolveIngredientBeforeEquipmentTech(Set<ItemFacet> facets) {
        return hasAny(facets, ItemFacet.INGREDIENT_ORGANIC, ItemFacet.INGREDIENT_MINERAL, ItemFacet.INGREDIENT_DYE)
                && !hasAny(facets,
                ItemFacet.MELEE_WEAPON,
                ItemFacet.RANGED_WEAPON,
                ItemFacet.HARVEST_TOOL,
                ItemFacet.UTILITY_TOOL,
                ItemFacet.ARMOR_HEAD,
                ItemFacet.ARMOR_CHEST,
                ItemFacet.ARMOR_LEGS,
                ItemFacet.ARMOR_FEET,
                ItemFacet.ARMOR_ANIMAL,
                ItemFacet.MACHINE,
                ItemFacet.HAS_ENERGY,
                ItemFacet.STORAGE,
                ItemFacet.REDSTONE_LOGIC,
                ItemFacet.REDSTONE_SIGNAL,
                ItemFacet.TRANSPORT,
                ItemFacet.CABLE,
                ItemFacet.TEMPLATE);
    }

    private static boolean shouldResolveAsArmorOrCurio(Set<ItemFacet> facets) {
        if (hasAny(facets, ItemFacet.ARMOR_HEAD, ItemFacet.ARMOR_CHEST, ItemFacet.ARMOR_LEGS, ItemFacet.ARMOR_FEET, ItemFacet.ARMOR_ANIMAL)) {
            return true;
        }
        return facets.contains(ItemFacet.CURIO)
                && !hasAny(facets,
                ItemFacet.DECORATIVE_BLOCK,
                ItemFacet.LIGHT_SOURCE,
                ItemFacet.NATURE_MISC,
                ItemFacet.FUNGI,
                ItemFacet.FLOWER,
                ItemFacet.LOG,
                ItemFacet.LEAVES,
                ItemFacet.EDIBLE,
                ItemFacet.PLACEABLE_FOOD,
                ItemFacet.INGREDIENT_ORGANIC,
                ItemFacet.INGREDIENT_MINERAL,
                ItemFacet.INGREDIENT_DYE);
    }

    private static String classifyTechSubcategory(String path, Set<ItemFacet> facets) {
        if (facets.contains(ItemFacet.ACTIVE_REDSTONE_LOGIC) || facets.contains(ItemFacet.REDSTONE_LOGIC)) {
            return "redstone";
        }
        if (facets.contains(ItemFacet.MACHINE)
                || facets.contains(ItemFacet.WORKSTATION)
                || facets.contains(ItemFacet.HAS_ENERGY)
                || facets.contains(ItemFacet.STORAGE)
                || facets.contains(ItemFacet.INTERACTIVE_BLOCK)
                || facets.contains(ItemFacet.HAS_BLOCK_ENTITY)) {
            return "machines";
        }
        if (facets.contains(ItemFacet.REDSTONE_SIGNAL)) {
            return "redstone";
        }
        if (facets.contains(ItemFacet.TRANSPORT)) return "transport";
        if (facets.contains(ItemFacet.TEMPLATE)) return "templates";
        if (facets.contains(ItemFacet.UPGRADE)) return "upgrades";
        if (facets.contains(ItemFacet.CABLE)) return "cables";
        if (facets.contains(ItemFacet.MECHANICAL_COMPONENT)) return "parts";
        if (facets.contains(ItemFacet.TECH_COMPONENT)) {
            if (path.contains("circuit")
                    || path.contains("processor")
                    || path.contains("logic")
                    || path.contains("calculation")
                    || path.contains("engineering")
                    || path.contains("chip")) {
                return "circuits";
            }
            return "parts";
        }
        if (facets.contains(ItemFacet.DUST)) return "dusts";
        if (hasAny(facets, ItemFacet.INGOT, ItemFacet.GEM, ItemFacet.NUGGET, ItemFacet.RAW_MATERIAL)) return "ingots";
        return "parts";
    }

    private static String classifyNatureSubcategory(String path, Set<ItemFacet> facets) {
        if (hasPreparedMealPath(path)) return "meals";
        if (facets.contains(ItemFacet.FOOD_MEAL)) return "meals";
        if (facets.contains(ItemFacet.FOOD_DRINK)) return "drinks";
        if (facets.contains(ItemFacet.FOOD_PROTEIN)) return "proteins";
        if (facets.contains(ItemFacet.PLACEABLE_FOOD)) return "meals";
        if (facets.contains(ItemFacet.SEED)) return "seeds";
        if (facets.contains(ItemFacet.CROP)) return "crops";
        if (facets.contains(ItemFacet.EDIBLE)) return "snacks";
        if (facets.contains(ItemFacet.NATURE_MISC)) return "flora";
        if (facets.contains(ItemFacet.FUNGI)) return "fungi";
        if (facets.contains(ItemFacet.FLOWER)) return "flora";
        if (facets.contains(ItemFacet.LOG)) return "wood";
        if (facets.contains(ItemFacet.LEAVES)) return "flora";
        return "flora";
    }

    private static boolean isSapling(String path, Map<String, String> attributes) {
        String blockClass = attributes.getOrDefault(SearchNodeKeys.BLOCK_CLASS, "");
        return containsPathToken(path, Set.of("sapling", "saplings"))
                || hasMetadataToken(attributes, SearchNodeKeys.TAGS, "minecraft:saplings")
                || hasMetadataToken(attributes, SearchNodeKeys.BLOCK_TAGS, "minecraft:saplings")
                || blockClass.endsWith("SaplingBlock")
                || blockClass.contains(".SaplingBlock");
    }

    private static boolean isLeaves(String path, Set<ItemFacet> facets, Map<String, String> attributes) {
        if (facets.contains(ItemFacet.CROP)) {
            return false;
        }
        return facets.contains(ItemFacet.LEAVES)
                || hasMetadataToken(attributes, SearchNodeKeys.TAGS, "minecraft:leaves")
                || hasMetadataToken(attributes, SearchNodeKeys.BLOCK_TAGS, "minecraft:leaves")
                || containsPathToken(path, Set.of("leaf", "leaves"));
    }

    private static boolean isWoodBlock(String path, Set<ItemFacet> facets, Map<String, String> attributes) {
        if (isSapling(path, attributes) || isLeaves(path, facets, attributes)) {
            return false;
        }
        return facets.contains(ItemFacet.WOOD_BLOCK)
                || hasMetadataToken(attributes, SearchNodeKeys.TAGS, "minecraft:planks")
                || hasMetadataToken(attributes, SearchNodeKeys.TAGS, "minecraft:logs")
                || hasMetadataToken(attributes, SearchNodeKeys.BLOCK_TAGS, "minecraft:planks")
                || hasMetadataToken(attributes, SearchNodeKeys.BLOCK_TAGS, "minecraft:logs")
                || path.endsWith("_planks")
                || path.endsWith("_log")
                || path.endsWith("_wood")
                || path.endsWith("_stem")
                || path.endsWith("_hyphae");
    }

    private static boolean isPlantSeed(String path, Set<ItemFacet> facets, Map<String, String> attributes) {
        if (isSapling(path, attributes)) {
            return false;
        }
        if (hasAny(facets,
                ItemFacet.INGOT,
                ItemFacet.GEM,
                ItemFacet.NUGGET,
                ItemFacet.RAW_MATERIAL,
                ItemFacet.DUST,
                ItemFacet.TECH_COMPONENT,
                ItemFacet.UTILITY_MISC,
                ItemFacet.FLUID_CONTAINER)) {
            return false;
        }
        if (!facets.contains(ItemFacet.SEED)
                && !hasMetadataToken(attributes, SearchNodeKeys.TAGS, "c:seeds")
                && !hasMetadataToken(attributes, SearchNodeKeys.TAGS, "forge:seeds")
                && !containsPathToken(path, Set.of("seed", "seeds"))) {
            return false;
        }
        return !containsPathToken(path, Set.of("bag", "bags", "bucket", "oil", "maker", "pouch", "crystal"))
                && !path.contains("_oil")
                && !path.contains("crystal_seed")
                && !path.contains("_crystal")
                && !path.startsWith("roasted_")
                && !path.startsWith("baked_");
    }

    private static boolean isCropLikePlaceable(String path, Set<ItemFacet> facets, Map<String, String> attributes) {
        if (!facets.contains(ItemFacet.PLACEABLE) || isSapling(path, attributes) || isLeaves(path, facets, attributes)) {
            return false;
        }
        String blockClass = attributes.getOrDefault(SearchNodeKeys.BLOCK_CLASS, "").toLowerCase(Locale.ROOT);
        if (path.equals("dead_bush") || blockClass.contains("deadbush")) {
            return false;
        }
        return facets.contains(ItemFacet.CROP)
                || hasMetadataToken(attributes, SearchNodeKeys.TAGS, "c:crops")
                || hasMetadataToken(attributes, SearchNodeKeys.TAGS, "forge:crops")
                || hasMetadataToken(attributes, SearchNodeKeys.BLOCK_TAGS, "c:crops")
                || hasMetadataToken(attributes, SearchNodeKeys.BLOCK_TAGS, "forge:crops")
                || path.endsWith("_crop")
                || path.contains("_crop_")
                || path.endsWith("_bush")
                || path.contains("_bush_")
                || blockClass.contains("crop")
                || blockClass.contains("bush");
    }

    private static boolean hasMetadataToken(Map<String, String> attributes, String key, String expected) {
        String encoded = attributes.getOrDefault(key, "");
        if (encoded.isBlank()) {
            return false;
        }
        for (String token : encoded.split(",")) {
            if (expected.equals(token.trim())) {
                return true;
            }
        }
        return false;
    }

    private static boolean hasCsvToken(String encoded, String expected) {
        if (encoded == null || encoded.isBlank()) {
            return false;
        }
        for (String token : encoded.split(",")) {
            if (expected.equals(token.trim())) {
                return true;
            }
        }
        return false;
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

    private static boolean shouldResolveDecorationFacetPrimary(String path, Set<ItemFacet> facets) {
        return facets.contains(ItemFacet.DECORATIVE_BLOCK)
                || (facets.contains(ItemFacet.LIGHT_SOURCE) && isPrimaryLightingPath(path));
    }

    private static boolean isPrimaryLightingPath(String path) {
        return containsPathToken(path, LIGHTING_TOKENS)
                || containsPathToken(path, Set.of("torch", "torches", "candle", "candles", "glowstone", "shroomlight", "froglight", "beacon"));
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
        return classifyTechSubcategory(path, facets);
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

    private static boolean shouldResolveNaturalBeforeTech(Set<ItemFacet> facets, String path) {
        return facets.contains(ItemFacet.NATURE_MISC)
                && isNaturalCableFalsePositivePath(path);
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
                && (hasAny(facets,
                ItemFacet.RAIL,
                ItemFacet.INTERACTIVE_BLOCK,
                ItemFacet.HAS_BLOCK_ENTITY,
                ItemFacet.MACHINE,
                ItemFacet.STORAGE,
                ItemFacet.HAS_ENERGY,
                ItemFacet.REDSTONE_LOGIC,
                ItemFacet.REDSTONE_SIGNAL,
                ItemFacet.TRANSPORT,
                ItemFacet.CABLE,
                ItemFacet.UPGRADE,
                ItemFacet.TEMPLATE,
                ItemFacet.TECH_COMPONENT,
                ItemFacet.MECHANICAL_COMPONENT)
                || containsPathToken(path, CREATE_PART_TOKENS)
                || containsPathToken(path, CREATE_MACHINE_TOKENS)
                || isCreateAddonTechPath(path))
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

    private static boolean shouldBiasArchitecturalPlaceableToBuilding(Set<ItemFacet> facets, String path, Map<String, String> attributes) {
        return facets.contains(ItemFacet.PLACEABLE)
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
                ItemFacet.LEAVES,
                ItemFacet.LIGHT_SOURCE)
                && containsPathToken(path, ARCHITECTURAL_BUILDING_TOKENS);
    }

    private static boolean shouldBiasPortableStorageFamilyToArmor(ModFamily modFamily, String path) {
        return modFamily == ModFamily.PORTABLE_STORAGE
                && (path.contains("backpack") || path.contains("satchel") || path.contains("pouch"));
    }

    private static boolean shouldBiasPortableStorageFamilyToTech(ModFamily modFamily, String path) {
        return modFamily == ModFamily.PORTABLE_STORAGE
                && (path.contains("upgrade") || path.contains("stack") || path.contains("pump") || path.contains("filter"));
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
                        || path.contains("core")
                        || path.contains("upgrade")
                        || path.contains("pattern")
                        || path.contains("filter")
                        || path.contains("cover")
                        || path.contains("remote")
                        || path.contains("key")
                        || path.contains("keyring")
                        || path.contains("template")
        )
                && !shouldBeGeology(facets, path, attributes)
                && !shouldBiasUncraftableFullBlockToTerrain(facets, attributes);
    }

    private static boolean shouldBiasFoodFamilyToNature(ModFamily modFamily, Set<ItemFacet> facets, String path, Map<String, String> attributes) {
        return modFamily == ModFamily.FOOD
                && facets.contains(ItemFacet.PLACEABLE)
                && (
                hasAny(facets,
                        ItemFacet.PLACEABLE_FOOD,
                        ItemFacet.EDIBLE,
                        ItemFacet.COMPOSTABLE,
                        ItemFacet.SEED,
                        ItemFacet.CROP,
                        ItemFacet.NATURE_MISC,
                        ItemFacet.FUNGI,
                        ItemFacet.LOG,
                        ItemFacet.LEAVES,
                        ItemFacet.FLOWER)
                        || isFoodStorageBlockPath(path)
        )
                && !hasAny(facets,
                ItemFacet.DECORATIVE_BLOCK,
                ItemFacet.LIGHT_SOURCE,
                ItemFacet.MACHINE,
                ItemFacet.HAS_BLOCK_ENTITY,
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

    private static boolean shouldBiasFoodFamilyPlaceableToNature(ModFamily modFamily, Set<ItemFacet> facets, String path, Map<String, String> attributes) {
        return modFamily == ModFamily.FOOD
                && facets.contains(ItemFacet.PLACEABLE)
                && !hasAny(facets,
                ItemFacet.MACHINE,
                ItemFacet.HAS_BLOCK_ENTITY,
                ItemFacet.HAS_ENERGY,
                ItemFacet.STORAGE,
                ItemFacet.DECORATIVE_BLOCK,
                ItemFacet.MELEE_WEAPON,
                ItemFacet.RANGED_WEAPON,
                ItemFacet.HARVEST_TOOL,
                ItemFacet.UTILITY_TOOL,
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

    private static String classifyFoodFamilyNatureSubcategory(String path, Set<ItemFacet> facets) {
        if (isFoodStorageBlockPath(path)) return "crops";
        return classifyNatureSubcategory(path, facets);
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
                facets.contains(ItemFacet.PLACEABLE_FOOD)
                        || facets.contains(ItemFacet.EDIBLE)
                        || hasPreparedFoodPath(path)
        );
    }

    private static boolean shouldResolveFoodLikeBeforePassiveRedstone(ModFamily modFamily, Set<ItemFacet> facets, String path) {
        if (!isPassiveComparatorOnly(facets)) {
            return false;
        }
        if (hasAny(facets,
                ItemFacet.HAS_ENERGY,
                ItemFacet.STORAGE,
                ItemFacet.INTERACTIVE_BLOCK,
                ItemFacet.TRANSPORT,
                ItemFacet.MACHINE,
                ItemFacet.INGOT,
                ItemFacet.GEM,
                ItemFacet.NUGGET,
                ItemFacet.RAW_MATERIAL,
                ItemFacet.DUST)) {
            return false;
        }
        return hasAny(facets,
                ItemFacet.EDIBLE,
                ItemFacet.PLACEABLE_FOOD,
                ItemFacet.FOOD_MEAL,
                ItemFacet.FOOD_DRINK,
                ItemFacet.FOOD_PROTEIN,
                ItemFacet.COMPOSTABLE)
                || (modFamily == ModFamily.FOOD && hasPreparedFoodPath(path));
    }

    private static boolean shouldResolveDecorLikeBeforePassiveRedstone(ModFamily modFamily, Set<ItemFacet> facets, Map<String, String> attributes) {
        if (!isPassiveComparatorOnly(facets)) {
            return false;
        }
        if (hasAny(facets,
                ItemFacet.HAS_ENERGY,
                ItemFacet.STORAGE,
                ItemFacet.INTERACTIVE_BLOCK,
                ItemFacet.TRANSPORT,
                ItemFacet.MACHINE,
                ItemFacet.INGOT,
                ItemFacet.GEM,
                ItemFacet.NUGGET,
                ItemFacet.RAW_MATERIAL,
                ItemFacet.DUST)) {
            return false;
        }
        if (hasAny(facets,
                ItemFacet.DOOR,
                ItemFacet.TRAPDOOR,
                ItemFacet.FENCE_GATE,
                ItemFacet.RAIL,
                ItemFacet.STAIRS,
                ItemFacet.SLAB,
                ItemFacet.WALL,
                ItemFacet.FENCE,
                ItemFacet.PANE)) {
            return false;
        }
        return facets.contains(ItemFacet.DECORATIVE_BLOCK)
                || (modFamily == ModFamily.DECOR
                && facets.contains(ItemFacet.PLACEABLE)
                && !shouldBeGeology(facets, "", attributes));
    }

    private static boolean isPassiveComparatorOnly(Set<ItemFacet> facets) {
        if (facets.contains(ItemFacet.ACTIVE_REDSTONE_LOGIC) || facets.contains(ItemFacet.REDSTONE_LOGIC)) {
            return false;
        }
        return facets.contains(ItemFacet.PASSIVE_COMPARATOR_OUTPUT)
                || facets.contains(ItemFacet.REDSTONE_SIGNAL);
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
                        || path.contains("keycard")
                        || path.contains("codebreaker")
                        || path.contains("monitor")
                        || path.contains("modifier")
                        || path.contains("reinforcer")
                        || path.contains("remover")
                        || path.contains("changer")
                        || path.contains("module")
                        || path.contains("sentry")
                        || path.contains("taser")
                        || path.contains("remote")
        )
                && !shouldBeGeology(facets, path, attributes)
                && !shouldBiasUncraftableFullBlockToTerrain(facets, attributes);
    }

    private static String classifyStorageSubcategory(String path, Set<ItemFacet> facets) {
        if (facets.contains(ItemFacet.UPGRADE) || path.contains("upgrade")) {
            return "upgrades";
        }
        if (facets.contains(ItemFacet.TEMPLATE) || path.contains("pattern") || path.contains("template")) {
            return "templates";
        }
        if (facets.contains(ItemFacet.CABLE) || path.contains("cable") || path.contains("bus")) {
            return "cables";
        }
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
            if (isCablePath(path) || facets.contains(ItemFacet.CABLE)) {
                return "cables";
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
        if (facets.contains(ItemFacet.UPGRADE) || path.contains("upgrade")) {
            return "upgrades";
        }
        if (facets.contains(ItemFacet.TEMPLATE) || isTemplatePath(path)) {
            return "templates";
        }
        if (facets.contains(ItemFacet.CABLE) || isCablePath(path)) {
            return "cables";
        }
        if (containsPathToken(path, CREATE_PART_TOKENS)) {
            return "parts";
        }
        if (containsPathToken(path, CREATE_MACHINE_TOKENS)) {
            return "machines";
        }
        return classifyTechSubcategory(path, facets);
    }

    private static String classifyFoodFamilyIngredientSubcategory(String path, Set<ItemFacet> facets) {
        if (facets.contains(ItemFacet.INGREDIENT_DYE)) return "dyes";
        if (facets.contains(ItemFacet.INGREDIENT_MINERAL)) return "mineral";
        return "organic";
    }

    private static String classifyFoodFamilyPreparedSubcategory(String path, Set<ItemFacet> facets) {
        if (facets.contains(ItemFacet.FOOD_MEAL)
                || hasPreparedMealPath(path)) {
            return "meals";
        }
        if (facets.contains(ItemFacet.FOOD_DRINK) || path.contains("bottle")) {
            return "drinks";
        }
        if (facets.contains(ItemFacet.FOOD_PROTEIN)) {
            return "proteins";
        }
        return "snacks";
    }

    private static boolean hasPreparedFoodPath(String path) {
        return hasPreparedMealPath(path)
                || path.contains("bottle")
                || path.contains("on_a_stick");
    }

    private static boolean hasPreparedMealPath(String path) {
        return path.contains("plate")
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
                || path.contains("pizza")
                || path.contains("noodle")
                || path.contains("rice")
                || path.contains("kebab")
                || path.contains("canned")
                || path.contains("mre")
                || path.contains("macandcheese")
                || path.contains("soup")
                || path.contains("stew");
    }

    private static boolean isFoodStorageBlockPath(String path) {
        return containsPathToken(path, FOOD_STORAGE_TOKENS);
    }

    private static String classifyAutomationSubcategory(String path, Set<ItemFacet> facets) {
        if (facets.contains(ItemFacet.CABLE) || path.contains("tube") || path.contains("cable")) {
            return "cables";
        }
        if (path.contains("circuit")
                || path.contains("transistor")
                || path.contains("capacitor")
                || path.contains("wafer")
                || path.contains("etch")
                || path.contains("photomask")
                || path.contains("assembly")
                || path.contains("keycard")
                || path.contains("modifier")
                || path.contains("reinforcer")
                || path.contains("changer")
                || path.contains("module")) {
            return "circuits";
        }
        if (path.contains("valve")
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
        if ("stone".equals(blocksMaterial)
                && !hasAny(facets,
                ItemFacet.FUNGI,
                ItemFacet.NATURE_MISC,
                ItemFacet.FLOWER,
                ItemFacet.LEAVES,
                ItemFacet.LOG,
                ItemFacet.COMPOSTABLE,
                ItemFacet.SOIL_BLOCK)) {
            return false;
        }
        return facets.contains(ItemFacet.FUNGI)
                || path.contains("nylium")
                || path.contains("mycelium")
                || path.contains("moss")
                || path.contains("lichen")
                || path.contains("fungi")
                || path.contains("fungus")
                || path.contains("roots")
                || path.contains("stem")
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
                || path.contains("stairs")
                || path.contains("slab")
                || path.contains("wall")
                || path.contains("fence")
                || path.contains("pane")
                || path.contains("door")
                || path.contains("trapdoor")
                || path.contains("brick")
                || path.contains("bricks")
                || path.contains("tile")
                || path.contains("paver")
                || path.contains("paving")
                || path.contains("beam")
                || path.contains("stripe")
                || path.contains("square")
                || path.contains("pattern")
                || path.contains("dented")
                || path.contains("weathered")
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

    private static boolean isCablePath(String path) {
        return !isNaturalCableFalsePositivePath(path)
                && (path.contains("cable")
                || path.endsWith("wire")
                || path.contains("_wire")
                || path.contains("wire_")
                || path.contains("wirecoil")
                || containsPathToken(path, Set.of("pipe", "tube", "conduit", "duct")))
                && !path.contains("wire_cut");
    }

    private static boolean isNaturalCableFalsePositivePath(String path) {
        return path.contains("coral")
                || path.equals("cobweb")
                || path.equals("dead_bush")
                || path.equals("frogspawn")
                || path.contains("sculk");
    }

    private static boolean isTemplatePath(String path) {
        return path.contains("blueprint")
                || path.contains("schematic")
                || path.equals("mold")
                || path.startsWith("mold_")
                || path.contains("_mold")
                || path.contains("pattern")
                || path.contains("template");
    }

    private static boolean isCreateAddonTechPath(String path) {
        return containsPathToken(path, RAILWAYS_TRANSPORT_TOKENS)
                || containsPathToken(path, RAILWAYS_PART_TOKENS)
                || containsPathToken(path, CREATE_ADDON_MACHINE_TOKENS)
                || containsPathToken(path, CREATE_ADDON_PART_TOKENS)
                || containsPathToken(path, CREATE_WINERY_MACHINE_TOKENS)
                || containsPathToken(path, CREATE_ORE_MACHINE_TOKENS)
                || containsPathToken(path, CREATE_ORE_PART_TOKENS);
    }

    private static boolean endsWithPathToken(String path, String token) {
        String[] pathTokens = path.split("[_/]");
        return pathTokens.length > 0 && pathTokens[pathTokens.length - 1].equals(token);
    }

    private static boolean shouldBeGeology(Set<ItemFacet> facets, String path, Map<String, String> attributes) {
        String blocksMaterial = attributes.getOrDefault(SearchNodeKeys.BLOCKS_MATERIAL, "");
        if (facets.contains(ItemFacet.STONE_BLOCK) || "stone".equals(blocksMaterial)) {
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

    private static String classifyMasonrySubcategory(Set<ItemFacet> facets, String path, Map<String, String> attributes) {
        if (hasAny(facets, ItemFacet.DOOR, ItemFacet.TRAPDOOR, ItemFacet.FENCE_GATE)) {
            return "functional";
        }
        if (facets.contains(ItemFacet.REDSTONE_LOGIC) || facets.contains(ItemFacet.REDSTONE_SIGNAL)) {
            return "redstone";
        }
        if (facets.contains(ItemFacet.STAIRS)) return "stairs";
        if (facets.contains(ItemFacet.SLAB)) return "slab";
        if (facets.contains(ItemFacet.WALL)) return "wall";
        if (facets.contains(ItemFacet.FENCE) || facets.contains(ItemFacet.FENCE_GATE)) return "fence";
        if (facets.contains(ItemFacet.PANE)) return "pane";
        if (path.contains("stairs") || path.endsWith("_stair")) return "stairs";
        if (path.contains("slab")) return "slab";
        if (path.contains("wall")) return "wall";
        if (path.contains("fence") || containsPathToken(path, Set.of("railing", "banister"))) return "fence";
        if (path.contains("pane") || containsPathToken(path, Set.of("window"))) return "pane";
        if (path.contains("door") || path.contains("trapdoor")) return "functional";

        String blockShape = attributes.getOrDefault("blockShape", "");
        if (!blockShape.isBlank()) {
            return switch (blockShape) {
                case "stairs", "slab", "wall", "fence", "pane", "door", "trapdoor", "fence_gate" ->
                        blockShape.equals("fence_gate") ? "fence" : blockShape;
                default -> "full_block";
            };
        }
        return "full_block";
    }

    private enum ModFamily {
        GENERIC,
        CREATE,
        FOOD,
        STORAGE,
        AUTOMATION,
        PORTABLE_STORAGE,
        DECOR
    }

    private record ResolveContext(ResourceLocation id,
                                  String modId,
                                  String path,
                                  Set<ItemFacet> facets,
                                  Map<String, String> attributes,
                                  ModFamily modFamily) {
    }

    private record PrimaryRule(String id,
                               Predicate<ResolveContext> matches,
                               Function<ResolveContext, CategoryAssignment> assignment) {
    }
}
