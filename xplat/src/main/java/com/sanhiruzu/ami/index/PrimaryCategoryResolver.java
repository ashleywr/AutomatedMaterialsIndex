package com.sanhiruzu.ami.index;

import com.sanhiruzu.ami.compat.CompatFamilyDetector;
import com.sanhiruzu.ami.config.AmiConfig;

import net.minecraft.resources.ResourceLocation;

import java.util.*;
import java.util.function.Function;
import java.util.function.Predicate;

public final class PrimaryCategoryResolver {
    /*
     * Classification guardrail:
     * - Runtime/API facts beat names: facets, capabilities, components, tags, item/block classes,
     *   explicit compat metadata, and exact registry identities should drive hard routing.
     * - Tokenized path/name evidence is a fallback only. It may help choose among already plausible
     *   buckets, but it must not create ownership, cheat/dev visibility, or broad top-level routes.
     * - Avoid new "isLikely*" gates or arbitrary substring checks. If lexical evidence is unavoidable,
     *   prefer exact tokens/phrases and add a false-positive regression test plus a note in
     *   docs/classification-routing-log.md.
     */
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
    private static final Set<String> PORTABLE_STORAGE_FAMILY_MOD_IDS = Set.of(
            "sophisticatedbackpacks"
    );
    private static final Set<String> STORAGE_FAMILY_MOD_IDS = Set.of(
            "ae2",
            "functionalstorage",
            "ironchest",
            "merequester",
            "refinedstorage",
            "sophisticatedstorage",
            "storagedrawers"
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
        /*
         * Classification routing rule:
         * - compat family ownership, semantic ontology, and family enrichment are separate decisions.
         * - item path terms and foreign mod tags may refine an already-owned family item, but must not
         *   establish ownership or override semantic category by themselves.
         * - strong semantic identities such as swords, armor, ingots, ores, storage, food, and stone
         *   should remain discoverable in their normal ontology even when owned by a compat family.
         *
         * Keep docs/classification-routing-log.md updated before changing these gates; it records
         * known failed approaches such as omega->mega, Cobblemon tags claiming vanilla shulkers,
         * Create terms claiming AE2 presses/GTCEu casings, and andesite becoming Create-owned.
         */
        if (id == null) {
            return fallback();
        }

        String modId = id.getNamespace().toLowerCase(Locale.ROOT);
        String path = id.getPath().toLowerCase(Locale.ROOT);
        var facets = profile.facets();
        var attributes = new HashMap<>(profile.attributes());
        ModFamily modFamily = classifyModFamily(modId);
        AmiConfig.CompatCategoryPolicy categoryPolicy = CompatCategoryPolicyResolver.resolve(attributes);
        if (hasCompatFamily(attributes)) {
            attributes.put(SearchNodeKeys.COMPAT_CATEGORY_POLICY, categoryPolicy.name().toLowerCase(Locale.ROOT));
        }
        FacetProfile routedProfile = new FacetProfile(facets, attributes);
        ResolveContext context = new ResolveContext(id, modId, path, facets, attributes, modFamily, categoryPolicy);
        CategoryRouteTrace route = CategoryRouteTrace.start(id, modFamily.name().toLowerCase(Locale.ROOT), attributes);

        Optional<CategoryAssignment> hardIdentity = resolveHardIdentity(context);
        if (hardIdentity.isPresent()) {
            return route.finish("hard_identity", "identity", hardIdentity.get());
        }

        Optional<CategoryAssignment> scored = CategoryScorer.resolveStrong(id, routedProfile);
        if (scored.isPresent()) {
            return route.finish("evidence_strong", "category_scorer", scored.get());
        }

        for (PrimaryRule rule : PRIMARY_RULES) {
            if (rule.matches.test(context)) {
                return route.finish("primary_rule", rule.id(), rule.assignment().apply(context));
            }
        }
        Optional<CategoryAssignment> fallbackScored = CategoryScorer.resolve(id, routedProfile);
        if (fallbackScored.isPresent()) {
            return route.finish("evidence_fallback", "category_scorer", fallbackScored.get());
        }
        Optional<CategoryAssignment> compatFallback = resolveCompatUnknownFallback(context);
        if (compatFallback.isPresent()) {
            return route.finish("compat_fallback", "recognized_compat_kind", compatFallback.get());
        }
        return route.finish("fallback", "unknown", fallback());
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

        Optional<CategoryAssignment> cobblemon = resolveCobblemonIdentity(context);
        if (cobblemon.isPresent()) {
            return cobblemon;
        }

        Optional<CategoryAssignment> create = resolveCreateIdentity(context);
        if (create.isPresent()) {
            return create;
        }

        Optional<CategoryAssignment> ae2 = resolveAe2Identity(context);
        if (ae2.isPresent()) {
            return ae2;
        }

        Optional<CategoryAssignment> mekanism = resolveMekanismIdentity(context);
        if (mekanism.isPresent()) {
            return mekanism;
        }

        Optional<CategoryAssignment> gregTech = resolveGregTechIdentity(context);
        if (gregTech.isPresent()) {
            return gregTech;
        }

        Optional<CategoryAssignment> apotheosis = resolveApotheosisIdentity(context);
        if (apotheosis.isPresent()) {
            return apotheosis;
        }

        Optional<CategoryAssignment> botania = resolveBotaniaIdentity(context);
        if (botania.isPresent()) {
            return botania;
        }

        Optional<CategoryAssignment> sophisticated = resolveSophisticatedIdentity(context);
        if (sophisticated.isPresent()) {
            return sophisticated;
        }

        Optional<CategoryAssignment> waystones = resolveWaystonesIdentity(context);
        if (waystones.isPresent()) {
            return waystones;
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

    private static Optional<CategoryAssignment> resolveWaystonesIdentity(ResolveContext context) {
        if (!"waystones".equals(context.modId)
                && !containsAny(context.attributes.getOrDefault(SearchNodeKeys.ITEM_CLASS, ""), "net.blay09.mods.waystones.")
                && !containsAny(context.attributes.getOrDefault(SearchNodeKeys.BLOCK_CLASS, ""), "net.blay09.mods.waystones.")
                && !hasMetadataToken(context.attributes, SearchNodeKeys.BLOCK_TAGS, "waystones:is_teleport_target")) {
            return Optional.empty();
        }

        if (containsAny(context.path, "dust", "shard")
                || containsAny(context.attributes.getOrDefault(SearchNodeKeys.ITEM_CLASS, ""), "ShardItem", "WarpDustItem")) {
            return Optional.of(identityAssignment(
                    "magic",
                    "reagents",
                    context.attributes,
                    "identity.waystones.reagent",
                    "Waystones teleport reagent"
            ));
        }

        if (hasMetadataToken(context.attributes, SearchNodeKeys.BLOCK_TAGS, "waystones:is_teleport_target")
                || containsAny(context.path, "waystone", "portstone", "sharestone", "warp_plate", "warp_stone", "scroll")
                || containsAny(context.attributes.getOrDefault(SearchNodeKeys.ITEM_CLASS, ""), "WarpStoneItem", "ScrollItem")
                || containsAny(context.attributes.getOrDefault(SearchNodeKeys.BLOCK_CLASS, ""), "WaystoneBlock", "PortstoneBlock", "SharestoneBlock", "WarpPlateBlock")) {
            return Optional.of(identityAssignment(
                    "magic",
                    "artifacts",
                    context.attributes,
                    "identity.waystones.teleport",
                    "Waystones teleport target or item"
            ));
        }

        return Optional.empty();
    }

    private static Optional<CategoryAssignment> resolveCobblemonIdentity(ResolveContext context) {
        if (!"cobblemon".equals(context.modId)
                && !CompatFamilyDetector.isPrimaryFamily(context.attributes, CompatFamilyDetector.COBBLEMON)) {
            return Optional.empty();
        }
        if (context.categoryPolicy == AmiConfig.CompatCategoryPolicy.SEMANTIC) {
            return Optional.empty();
        }
        String kind = context.attributes.getOrDefault(SearchNodeKeys.COBBLEMON_ITEM_KIND, "");
        if (kind.isBlank()) {
            return Optional.empty();
        }
        if (context.categoryPolicy == AmiConfig.CompatCategoryPolicy.HYBRID && !isFocusedCobblemonKind(kind)) {
            return Optional.empty();
        }

        return Optional.of(switch (kind) {
            case "poke_ball" -> identityAssignment(
                    "cobblemon", "poke_balls", context.attributes,
                    "identity.cobblemon.poke_ball", "Cobblemon capture item");
            case "medicine", "status_cure", "vitamin", "mint", "mochi" -> identityAssignment(
                    "cobblemon", "medicine", context.attributes,
                    "identity.cobblemon.medicine", "Cobblemon medicine or consumable");
            case "berry" -> identityAssignment(
                    "cobblemon", "berries", context.attributes,
                    "identity.cobblemon.berry", "Cobblemon berry");
            case "apricorn", "apricorn_seed" -> identityAssignment(
                    "cobblemon", "apricorns", context.attributes,
                    "identity.cobblemon.apricorn", "Cobblemon apricorn");
            case "evolution_item" -> identityAssignment(
                    "cobblemon", "evolution", context.attributes,
                    "identity.cobblemon.evolution", "Cobblemon evolution item");
            case "fossil" -> identityAssignment(
                    "cobblemon", "fossils", context.attributes,
                    "identity.cobblemon.fossil", "Cobblemon fossil or archaeology item");
            case "machine" -> identityAssignment(
                    "cobblemon", "machines", context.attributes,
                    "identity.cobblemon.machine", "Cobblemon machine or station");
            case "decor" -> identityAssignment(
                    "cobblemon", "decor", context.attributes,
                    "identity.cobblemon.decor", "Cobblemon display or decor item");
            case "transport" -> identityAssignment(
                    "cobblemon", "transport", context.attributes,
                    "identity.cobblemon.transport", "Cobblemon transport item");
            case "held_item" -> identityAssignment(
                    "cobblemon", "held_items", context.attributes,
                    "identity.cobblemon.held_item", "Cobblemon held item");
            case "utility_item" -> identityAssignment(
                    "cobblemon", "utility", context.attributes,
                    "identity.cobblemon.utility", "Cobblemon utility item");
            case "consumable" -> identityAssignment(
                    "cobblemon", "consumables", context.attributes,
                    "identity.cobblemon.consumable", "Cobblemon consumable");
            case "agriculture" -> identityAssignment(
                    "cobblemon", "agriculture", context.attributes,
                    "identity.cobblemon.agriculture", "Cobblemon agriculture item");
            case "building" -> identityAssignment(
                    "cobblemon", "building", context.attributes,
                    "identity.cobblemon.building", "Cobblemon building block");
            case "archaeology" -> identityAssignment(
                    "cobblemon", "archaeology", context.attributes,
                    "identity.cobblemon.archaeology", "Cobblemon archaeology item");
            default -> identityAssignment(
                    "cobblemon", "misc", context.attributes,
                    "identity.cobblemon", "Cobblemon item");
        });
    }

    private static boolean isFocusedCobblemonKind(String kind) {
        return switch (kind) {
            case "poke_ball", "medicine", "status_cure", "vitamin", "mint", "mochi",
                    "berry", "apricorn", "apricorn_seed", "evolution_item", "fossil",
                    "machine", "held_item", "utility_item", "consumable", "agriculture",
                    "archaeology" -> true;
            default -> false;
        };
    }

    private static Optional<CategoryAssignment> resolveCreateIdentity(ResolveContext context) {
        if (!"create".equals(context.modId)
                && !CompatFamilyDetector.isPrimaryFamily(context.attributes, CompatFamilyDetector.CREATE)) {
            return Optional.empty();
        }
        if (context.categoryPolicy == AmiConfig.CompatCategoryPolicy.SEMANTIC) {
            return Optional.empty();
        }
        String kind = context.attributes.getOrDefault(SearchNodeKeys.CREATE_ITEM_KIND, "");
        if (kind.isBlank()) {
            return Optional.empty();
        }
        if (context.categoryPolicy == AmiConfig.CompatCategoryPolicy.HYBRID && !isFocusedCreateKind(kind)) {
            return Optional.empty();
        }
        String subcategory = mapCreateSubcategory(kind);
        return Optional.of(identityAssignment(
                "create",
                subcategory,
                context.attributes,
                "identity.create." + subcategory,
                "Create " + subcategory + " identity"
        ));
    }

    private static boolean isFocusedCreateKind(String kind) {
        return switch (kind) {
            case "kinetics", "machines", "logistics", "trains", "contraptions", "fluids" -> true;
            default -> false;
        };
    }

    private static Optional<CategoryAssignment> resolveCompatUnknownFallback(ResolveContext context) {
        if (context.categoryPolicy == AmiConfig.CompatCategoryPolicy.SEMANTIC) {
            return Optional.empty();
        }
        if ("create".equals(context.modId)
                || CompatFamilyDetector.isPrimaryFamily(context.attributes, CompatFamilyDetector.CREATE)) {
            String kind = context.attributes.getOrDefault(SearchNodeKeys.CREATE_ITEM_KIND, "");
            if (!kind.isBlank()) {
                return Optional.of(compatFallbackAssignment("create", mapCreateSubcategory(kind), context.attributes));
            }
        }
        if ("ae2".equals(context.modId)
                || "appliedenergistics2".equals(context.modId)
                || CompatFamilyDetector.isPrimaryFamily(context.attributes, CompatFamilyDetector.AE2)) {
            String kind = context.attributes.getOrDefault(SearchNodeKeys.AE2_ITEM_KIND, "");
            if (!kind.isBlank()) {
                return Optional.of(compatFallbackAssignment("ae2", mapAe2Subcategory(kind), context.attributes));
            }
        }
        if ("mekanism".equals(context.modId)
                || context.modId.startsWith("mekanism")
                || CompatFamilyDetector.isPrimaryFamily(context.attributes, CompatFamilyDetector.MEKANISM)) {
            String kind = context.attributes.getOrDefault(SearchNodeKeys.MEKANISM_ITEM_KIND, "");
            if (!kind.isBlank()) {
                return Optional.of(compatFallbackAssignment("mekanism", mapMekanismSubcategory(kind), context.attributes));
            }
        }
        if ("sophisticatedbackpacks".equals(context.modId)
                || "sophisticatedstorage".equals(context.modId)
                || "sophisticatedcore".equals(context.modId)
                || CompatFamilyDetector.isPrimaryFamily(context.attributes, CompatFamilyDetector.SOPHISTICATED)) {
            String kind = context.attributes.getOrDefault(SearchNodeKeys.SOPHISTICATED_ITEM_KIND, "");
            if (!kind.isBlank()) {
                return Optional.of(compatFallbackAssignment("sophisticated", mapSophisticatedSubcategory(kind), context.attributes));
            }
        }
        return Optional.empty();
    }

    private static CategoryAssignment compatFallbackAssignment(String categoryId, String subcategoryId, Map<String, String> attributes) {
        return identityAssignment(
                categoryId,
                subcategoryId,
                attributes,
                "fallback." + categoryId + "." + subcategoryId,
                "recognized " + categoryId + " item with no stronger semantic route"
        );
    }

    private static String mapCreateSubcategory(String kind) {
        return switch (kind) {
            case "kinetics" -> "kinetics";
            case "machines" -> "machines";
            case "logistics" -> "logistics";
            case "trains" -> "trains";
            case "contraptions" -> "contraptions";
            case "fluids" -> "fluids";
            case "tools" -> "tools";
            case "materials" -> "materials";
            case "building" -> "building";
            default -> "misc";
        };
    }

    private static String mapAe2Subcategory(String kind) {
        return switch (kind) {
            case "network" -> "network";
            case "storage" -> "storage";
            case "terminals" -> "terminals";
            case "crafting" -> "crafting";
            case "channels" -> "channels";
            case "spatial" -> "spatial";
            case "materials" -> "materials";
            default -> "misc";
        };
    }

    private static String mapMekanismSubcategory(String kind) {
        return switch (kind) {
            case "machines" -> "machines";
            case "energy" -> "energy";
            case "chemicals" -> "chemicals";
            case "logistics" -> "logistics";
            case "upgrades" -> "upgrades";
            case "tools" -> "tools";
            case "materials" -> "materials";
            default -> "misc";
        };
    }

    private static String mapSophisticatedSubcategory(String kind) {
        return switch (kind) {
            case "backpacks" -> "backpacks";
            case "storage" -> "storage";
            case "upgrades" -> "upgrades";
            case "filters" -> "filters";
            case "tools" -> "tools";
            default -> "misc";
        };
    }

    private static Optional<CategoryAssignment> resolveAe2Identity(ResolveContext context) {
        if (!"ae2".equals(context.modId)
                && !"appliedenergistics2".equals(context.modId)
                && !CompatFamilyDetector.isPrimaryFamily(context.attributes, CompatFamilyDetector.AE2)) {
            return Optional.empty();
        }
        if (context.categoryPolicy == AmiConfig.CompatCategoryPolicy.SEMANTIC) {
            return Optional.empty();
        }
        String kind = context.attributes.getOrDefault(SearchNodeKeys.AE2_ITEM_KIND, "");
        if (kind.isBlank()) {
            return Optional.empty();
        }
        if (context.categoryPolicy == AmiConfig.CompatCategoryPolicy.HYBRID && !isFocusedAe2Kind(kind)) {
            return Optional.empty();
        }
        String subcategory = mapAe2Subcategory(kind);
        return Optional.of(identityAssignment(
                "ae2",
                subcategory,
                context.attributes,
                "identity.ae2." + subcategory,
                "AE2 " + subcategory + " identity"
        ));
    }

    private static boolean isFocusedAe2Kind(String kind) {
        return switch (kind) {
            case "network", "storage", "terminals", "crafting", "channels", "spatial" -> true;
            default -> false;
        };
    }

    private static Optional<CategoryAssignment> resolveMekanismIdentity(ResolveContext context) {
        if (!"mekanism".equals(context.modId)
                && !context.modId.startsWith("mekanism")
                && !CompatFamilyDetector.isPrimaryFamily(context.attributes, CompatFamilyDetector.MEKANISM)) {
            return Optional.empty();
        }
        if (context.categoryPolicy == AmiConfig.CompatCategoryPolicy.SEMANTIC) {
            return Optional.empty();
        }
        String kind = context.attributes.getOrDefault(SearchNodeKeys.MEKANISM_ITEM_KIND, "");
        if (kind.isBlank()) {
            return Optional.empty();
        }
        if (context.categoryPolicy == AmiConfig.CompatCategoryPolicy.HYBRID && !isFocusedMekanismKind(kind)) {
            return Optional.empty();
        }
        String subcategory = mapMekanismSubcategory(kind);
        return Optional.of(identityAssignment(
                "mekanism",
                subcategory,
                context.attributes,
                "identity.mekanism." + subcategory,
                "Mekanism " + subcategory + " identity"
        ));
    }

    private static boolean isFocusedMekanismKind(String kind) {
        return switch (kind) {
            case "machines", "energy", "chemicals", "logistics", "upgrades" -> true;
            default -> false;
        };
    }

    private static Optional<CategoryAssignment> resolveGregTechIdentity(ResolveContext context) {
        if (!"gtceu".equals(context.modId)
                && !"gregtech".equals(context.modId)
                && !CompatFamilyDetector.isPrimaryFamily(context.attributes, CompatFamilyDetector.GREGTECH)) {
            return Optional.empty();
        }
        if (context.categoryPolicy == AmiConfig.CompatCategoryPolicy.SEMANTIC) {
            return Optional.empty();
        }
        String subcategory = classifyGregTechSubcategory(context);
        return Optional.of(identityAssignment(
                "gregtech",
                subcategory,
                context.attributes,
                "identity.gregtech." + subcategory,
                "GregTech family isolation"
        ));
    }

    private static String classifyGregTechSubcategory(ResolveContext context) {
        String itemClass = context.attributes.getOrDefault(SearchNodeKeys.ITEM_CLASS, "");
        String blockClass = context.attributes.getOrDefault(SearchNodeKeys.BLOCK_CLASS, "");
        if (containsPathToken(context.path, Set.of("multiblock", "multiblocks"))
                || containsAny(itemClass, "multiblock")
                || containsAny(blockClass, "multiblock")) {
            return "multiblocks";
        }
        if (containsPathToken(context.path, Set.of("cover", "covers"))) {
            return "covers";
        }
        if (containsAny(context.path, "circuit", "processor", "chip")) {
            return "circuits";
        }
        if (hasAny(context.facets, ItemFacet.MELEE_WEAPON, ItemFacet.RANGED_WEAPON,
                ItemFacet.HARVEST_TOOL, ItemFacet.UTILITY_TOOL)) {
            return "tools";
        }
        if (containsAny(itemClass, ".MetaMachineItem")
                || containsAny(blockClass, ".MetaMachineBlock")) {
            return "machines";
        }
        if (hasAny(context.facets, ItemFacet.MACHINE, ItemFacet.WORKSTATION)
                || containsAny(context.path, "machine", "hatch", "bus", "conveyor", "robot_arm",
                "emitter", "sensor", "regulator", "pump", "motor", "piston", "assembler",
                "macerator", "centrifuge", "electrolyzer", "compressor", "extractor", "furnace",
                "mixer", "canner", "lathe", "bender", "wiremill", "polarizer")
                || containsPathToken(context.path, Set.of(
                "smelter", "reactor", "collector", "boiler", "crusher", "autoclave", "bath",
                "cutter", "distillery", "extruder", "solidifier", "press", "packer", "turbine",
                "miner", "brewery", "separator", "fermenter", "heater", "engraver", "sifter",
                "accelerator", "fisher", "scrubber", "breaker", "buffer"))) {
            return "machines";
        }
        if (hasAny(context.facets, ItemFacet.HAS_ENERGY, ItemFacet.CABLE)
                || containsAny(context.path, "battery", "capacitor", "cable", "wire", "energy", "power",
                "generator", "dynamo", "transformer", "converter", "diode", "solar_panel", "voltage_coil")) {
            return "power";
        }
        if (containsAny(itemClass, ".GTBucketItem", ".SurfaceRockBlockItem")) {
            return "materials";
        }
        if (hasAny(context.facets, ItemFacet.INGOT, ItemFacet.NUGGET, ItemFacet.DUST,
                ItemFacet.GEM, ItemFacet.RAW_MATERIAL, ItemFacet.TECH_COMPONENT,
                ItemFacet.MECHANICAL_COMPONENT, ItemFacet.INGREDIENT_MINERAL)
                || containsAny(context.path, "ingot", "nugget", "dust", "plate", "rod", "bolt",
                "screw", "ring", "foil", "wire", "gear", "spring", "rotor", "gem",
                "ore", "crushed", "purified", "impure", "raw", "tiny", "small")
                || containsPathToken(context.path, Set.of(
                "bucket", "indicator", "blade", "head", "tip", "lens", "wafer", "mold",
                "casing", "frame", "sheet", "studs", "dye", "can", "boule", "round"))) {
            return "materials";
        }
        return "misc";
    }

    private static Optional<CategoryAssignment> resolveApotheosisIdentity(ResolveContext context) {
        if (!isApotheosisFamily(context)) {
            return Optional.empty();
        }
        if (context.categoryPolicy == AmiConfig.CompatCategoryPolicy.SEMANTIC) {
            return Optional.empty();
        }
        String subcategory = classifyApotheosisSubcategory(context);
        return Optional.of(identityAssignment(
                "apotheosis",
                subcategory,
                context.attributes,
                "identity.apotheosis." + subcategory,
                "Apotheosis family identity"
        ));
    }

    private static boolean isApotheosisFamily(ResolveContext context) {
        return "apotheosis".equals(context.modId)
                || "apothic_attributes".equals(context.modId)
                || "apothic_enchanting".equals(context.modId)
                || "apothic_spawners".equals(context.modId)
                || CompatFamilyDetector.isPrimaryFamily(context.attributes, CompatFamilyDetector.APOTHEOSIS);
    }

    private static String classifyApotheosisSubcategory(ResolveContext context) {
        String itemClass = context.attributes.getOrDefault(SearchNodeKeys.ITEM_CLASS, "");
        String blockClass = context.attributes.getOrDefault(SearchNodeKeys.BLOCK_CLASS, "");
        String tags = context.attributes.getOrDefault(SearchNodeKeys.TAGS, "");

        if ("apothic_enchanting".equals(context.modId)
                || containsAny(context.path, "shelf", "tome", "library", "ender_lead", "infused_")
                || containsAny(itemClass, "tomeitem", "shelf", "enchlibrary", "enderlead")
                || containsAny(blockClass, "shelf", "enchlibrary")) {
            return "enchanting";
        }
        if (containsAny(context.path, "boss")
                || containsAny(tags, "boss_music_discs")
                || containsAny(itemClass, "bosssummoner")) {
            return "bosses";
        }
        if (containsAny(context.path, "spawner", "spawn_egg")
                || containsAny(itemClass, "spawner")
                || containsAny(blockClass, "spawner")) {
            return "spawners";
        }
        if (containsAny(context.path, "socket", "potion_charm")
                || containsAny(itemClass, "potioncharm")
                || hasMetadataToken(context.attributes, SearchNodeKeys.TAGS, "curios:charm")) {
            return "sockets";
        }
        if (containsAny(context.path, "gem")
                || containsAny(itemClass, "gemitem")
                || containsAny(blockClass, "gem")) {
            return "gems";
        }
        if (containsAny(context.path, "affix", "reforging", "salvaging", "augmenting", "sigil", "material", "smithing_template")
                || containsAny(itemClass, "salvageitem", "tooltipitem")
                || containsAny(blockClass, "reforging", "salvaging", "augmenting")
                || containsAny(tags, "rarity_materials")) {
            return "affixes";
        }
        return "misc";
    }

    private static Optional<CategoryAssignment> resolveBotaniaIdentity(ResolveContext context) {
        if (!isBotaniaFamily(context)) {
            return Optional.empty();
        }
        if (context.categoryPolicy == AmiConfig.CompatCategoryPolicy.SEMANTIC) {
            return Optional.empty();
        }
        String subcategory = classifyBotaniaSubcategory(context);
        return Optional.of(identityAssignment(
                "botania",
                subcategory,
                context.attributes,
                "identity.botania." + subcategory,
                "Botania family identity"
        ));
    }

    private static boolean isBotaniaFamily(ResolveContext context) {
        return "botania".equals(context.modId)
                || "mythicbotany".equals(context.modId)
                || "botanicalmachinery".equals(context.modId)
                || "extrabotany".equals(context.modId)
                || CompatFamilyDetector.isPrimaryFamily(context.attributes, CompatFamilyDetector.BOTANIA);
    }

    private static String classifyBotaniaSubcategory(ResolveContext context) {
        if (containsPathToken(context.path, Set.of("rune", "runes"))) {
            return "runes";
        }
        if (containsAny(context.path, "mana_", "_mana", "mana_pool", "spreader", "spark", "alfheim_portal",
                "gaia_pylon", "natura_pylon", "mana_pylon", "mana_tablet", "mana_pearl", "mana_diamond",
                "mana_string", "manaweave", "pool")
                || containsAny(context.attributes.getOrDefault(SearchNodeKeys.ITEM_CLASS, ""), "mana")) {
            return "mana";
        }
        if (isBotaniaGeneratingFlower(context.path)) {
            return "generating_flowers";
        }
        if (isBotaniaFunctionalFlower(context.path)) {
            return "functional_flowers";
        }
        if (hasAny(context.facets, ItemFacet.CURIO, ItemFacet.EQUIPPABLE)
                || containsAny(context.path, "ring", "band", "amulet", "pendant", "belt", "sash",
                "tiara", "cloak", "bauble", "charm", "flugel_eye", "monocle")) {
            return "baubles";
        }
        if (hasAny(context.facets, ItemFacet.MELEE_WEAPON, ItemFacet.RANGED_WEAPON,
                ItemFacet.HARVEST_TOOL, ItemFacet.UTILITY_TOOL)
                || containsAny(context.path, "wand", "rod", "lens", "sword", "bow", "pickaxe",
                "axe", "shovel", "hoe", "terraformer", "horn", "drum", "magnet", "brewer")) {
            return "tools";
        }
        if (hasAny(context.facets, ItemFacet.INGOT, ItemFacet.NUGGET, ItemFacet.DUST,
                ItemFacet.GEM, ItemFacet.RAW_MATERIAL, ItemFacet.TECH_COMPONENT,
                ItemFacet.INGREDIENT_ORGANIC, ItemFacet.INGREDIENT_MINERAL)
                || containsAny(context.path, "petal", "mystical_flower", "livingwood", "livingrock",
                "dreamwood", "manasteel", "terrasteel", "elementium", "gaia", "pixie_dust",
                "dragonstone", "mana_powder", "mana_dust", "quartz", "shimmerrock", "shimmerwood")) {
            return "materials";
        }
        return "misc";
    }

    private static boolean isBotaniaGeneratingFlower(String path) {
        return containsAny(path,
                "endoflame", "hydroangeas", "thermalily", "gourmaryllis", "munchdew",
                "rosa_arcana", "entropinnyum", "kekimurus", "narslimmus", "spectrolus",
                "dandelifeon", "shulk_me_not", "rafflowsia");
    }

    private static boolean isBotaniaFunctionalFlower(String path) {
        return containsAny(path,
                "pure_daisy", "agricarnation", "bellethorn", "bergamute", "bubbell",
                "clayconia", "daffomill", "dreadthorn", "exoflame", "fallen_kanade",
                "heisei_dream", "hopperhock", "hyacidus", "jaded_amaranthus", "labellia",
                "loonium", "marimorphosis", "medumone", "pollidisiac", "rannuncarpus",
                "solegnolia", "spectranthemum", "tangleberrie", "tigerseye", "vinculotus",
                "orechid", "orechid_ignem");
    }

    private static Optional<CategoryAssignment> resolveSophisticatedIdentity(ResolveContext context) {
        if (!"sophisticatedbackpacks".equals(context.modId)
                && !"sophisticatedstorage".equals(context.modId)
                && !"sophisticatedcore".equals(context.modId)
                && !CompatFamilyDetector.isPrimaryFamily(context.attributes, CompatFamilyDetector.SOPHISTICATED)) {
            return Optional.empty();
        }
        if (context.categoryPolicy == AmiConfig.CompatCategoryPolicy.SEMANTIC) {
            return Optional.empty();
        }
        String kind = context.attributes.getOrDefault(SearchNodeKeys.SOPHISTICATED_ITEM_KIND, "");
        if (kind.isBlank()) {
            return Optional.empty();
        }
        if (context.categoryPolicy == AmiConfig.CompatCategoryPolicy.HYBRID && !isFocusedSophisticatedKind(kind)) {
            return Optional.empty();
        }
        String subcategory = mapSophisticatedSubcategory(kind);
        return Optional.of(identityAssignment(
                "sophisticated",
                subcategory,
                context.attributes,
                "identity.sophisticated." + subcategory,
                "Sophisticated " + subcategory + " identity"
        ));
    }

    private static boolean isFocusedSophisticatedKind(String kind) {
        return switch (kind) {
            case "backpacks", "storage", "upgrades", "filters" -> true;
            default -> false;
        };
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
        return PORTABLE_STORAGE_FAMILY_MOD_IDS.contains(modId);
    }

    private static boolean isStorageFamilyMod(String modId) {
        return STORAGE_FAMILY_MOD_IDS.contains(modId);
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

    private static boolean hasCompatFamily(Map<String, String> attributes) {
        return attributes != null
                && (!attributes.getOrDefault(SearchNodeKeys.COMPAT_FAMILIES, "").isBlank()
                || !attributes.getOrDefault(SearchNodeKeys.PRIMARY_COMPAT_FAMILY, "").isBlank()
                || !attributes.getOrDefault(SearchNodeKeys.COMPAT_FAMILY, "").isBlank());
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

    private static boolean containsAny(String value, String... needles) {
        if (value == null || value.isBlank()) {
            return false;
        }
        String normalized = value.toLowerCase(Locale.ROOT);
        for (String needle : needles) {
            if (normalized.contains(needle.toLowerCase(Locale.ROOT))) {
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
                                  ModFamily modFamily,
                                  AmiConfig.CompatCategoryPolicy categoryPolicy) {
    }

    private record PrimaryRule(String id,
                               Predicate<ResolveContext> matches,
                               Function<ResolveContext, CategoryAssignment> assignment) {
    }
}
