package com.sanhiruzu.ami.index;

/**
 * String constants for SearchNode.metadata() keys.
 */
public final class SearchNodeKeys {
    // Shared
    public static final String MOD_ID = "modId";
    public static final String COMPAT_FAMILY = "compatFamily"; // Legacy alias for primaryCompatFamily.
    public static final String COMPAT_FAMILIES = "compatFamilies"; // Comma-separated parent ecosystem ids.
    public static final String PRIMARY_COMPAT_FAMILY = "primaryCompatFamily"; // UI/category owner when one family must win.
    public static final String COMPAT_CATEGORY_POLICY = "compatCategoryPolicy"; // focused, semantic, or hybrid.
    public static final String CLASSIFICATION_ROUTE = "classificationRoute"; // Human-readable resolver path.
    public static final String CLASSIFICATION_ROUTE_PHASE = "classificationRoutePhase"; // Final resolver gate.
    public static final String CLASSIFICATION_ROUTE_RULE = "classificationRouteRule"; // Final rule/scorer id.
    // Item-specific
    public static final String ONTOLOGY_CATEGORY = "ontologyCategory";    // AmiOntology.Category.id
    public static final String ONTOLOGY_SUBCATEGORY = "ontologySubcategory"; // AmiOntology.SubCategory.id
    public static final String VARIANT_GROUP = "variantGroup";
    public static final String COLOR_BUCKET = "colorBucket";
    public static final String MATERIAL_GROUP = "materialGroup";
    public static final String TAGS = "tags";  // Comma-separated tag paths
    public static final String FACETS = "facets"; // Comma-separated stable facet ids
    public static final String ITEM_CLASS = "itemClass"; // concrete item class
    public static final String ESM_CAPACITY = "emsCapacity";  // Equivalent Stack Metric
    public static final String ENERGY_CAPACITY = "energy_capacity"; // Maximum FE/RF capacity
    public static final String ENERGY_GENERATION = "energy_generation"; // FE/RF generated per tick
    public static final String ENERGY_CONSUMPTION = "energy_consumption"; // FE/RF consumed per tick
    public static final String ENERGY_METRIC_SOURCE = "energy_metric_source"; // capability/static/tooltip
    public static final String FLUID_CAPACITY = "fluid_capacity"; // Vanilla bucket equivalents
    public static final String FLUID_METRIC_SOURCE = "fluid_metric_source"; // bucket/tooltip
    public static final String TOOL_SPEED = "toolSpeed";
    public static final String TOOL_USES = "toolUses";
    public static final String TOOL_ATTACK_BONUS = "toolAttackBonus";
    public static final String ARMOR_DEFENSE = "armorDefense";
    public static final String ARMOR_TOUGHNESS = "armorToughness";
    public static final String AMMO_TYPE = "ammoType";
    public static final String REQUIRED_TOOL = "requiredTool";
    public static final String MAX_DURABILITY = "maxDurability";
    public static final String FOOD_NUTRITION = "foodNutrition";
    public static final String FOOD_SATURATION = "foodSaturation";
    public static final String ATTACK_DAMAGE = "attackDamage";
    public static final String DPS = "dps";          // Damage per second (Assembly Lab)
    public static final String SEARCH_TOKENS = "searchTokens";  // Space-separated synthetic search tokens
    public static final String ACCESS_LEVEL = "accessLevel";  // "survival", "creative", "cheat", or "dev"
    public static final String VISIBILITY = "visibility";    // "hidden" = not in any creative tab
    public static final String OBTAINABILITY = "obtainability"; // "no_recipe" = no recipe output
    public static final String RECIPE_CATEGORIES = "recipeCategories"; // comma-separated recipe type IDs
    public static final String RECIPE_USE_CATEGORIES = "recipeUseCategories"; // comma-separated recipe type IDs where this item is an input
    public static final String RECIPE_OUTPUT_COUNT = "recipeOutputCount"; // count of indexed recipes producing this item
    public static final String RECIPE_USE_COUNT = "recipeUseCount"; // count of indexed recipes using this item as input
    public static final String CREATIVE_TAB_ID = "creativeTabId";
    public static final String CREATIVE_TAB_LABEL = "creativeTabLabel";
    public static final String SUBTYPE_OF = "subtypeOf";     // base item id for subtype nodes
    public static final String VARIANT_SOURCE = "variantSource"; // source of a representative stack variant, e.g. creative_tab
    public static final String VARIANT_AXES = "variantAxes"; // comma-separated visual/material axes such as color,wood
    public static final String VARIANT_COLLAPSE_MODE = "variantCollapseMode"; // auto, default_collapsed, or never
    public static final String POTION_EFFECT = "potionEffect";  // full ResourceLocation of the potion effect (e.g. "minecraft:fire_resistance")
    public static final String COLLAPSE_FAMILY = "collapseFamily"; // stable UI grouping key for explicit family collapse
    public static final String COLLAPSE_LABEL = "collapseLabel";  // user-facing label for explicit family collapse
    public static final String BLOCKS_MATERIAL = "blocksMaterial"; // material family for block subcategory (shape/material toggle)
    public static final String BLOCK_TAGS = "blockTags"; // comma-separated block tag ids
    public static final String BLOCK_CLASS = "blockClass"; // concrete placed block class
    public static final String BLOCK_STATE_PROPERTIES = "blockStateProperties"; // comma-separated state property names
    public static final String COMPONENT_FACTS = "componentFacts"; // comma-separated normalized DataComponent facts
    public static final String EQUIPMENT_SLOT = "equipmentSlot"; // normalized EquipmentSlot name when known
    // Cobblemon / Pokemon-specific optional metadata. These are plain strings so
    // AMI can index datapack-backed Cobblemon content without a hard dependency.
    public static final String COBBLEMON_ITEM_KIND = "cobblemonItemKind";
    public static final String POKEMON_BALL_FAMILY = "pokemonBallFamily";
    public static final String POKEMON_BALL_TIER = "pokemonBallTier";
    public static final String POKEMON_MEDICINE_KIND = "pokemonMedicineKind";
    public static final String POKEMON_HEALING = "pokemonHealing";
    public static final String POKEMON_STATUS_CURE = "pokemonStatusCure";
    public static final String POKEMON_HELD_ITEM_ROLE = "pokemonHeldItemRole";
    public static final String POKEMON_EVOLUTION_TRIGGER = "pokemonEvolutionTrigger";
    public static final String POKEMON_TYPE = "pokemonType";
    public static final String POKEMON_SPECIES = "pokemonSpecies";
    public static final String POKEMON_DEX_NUMBER = "pokemonDexNumber";
    public static final String POKEMON_PRIMARY_TYPE = "pokemonPrimaryType";
    public static final String POKEMON_SECONDARY_TYPE = "pokemonSecondaryType";
    public static final String POKEMON_GENERATION = "pokemonGeneration";
    public static final String POKEMON_ABILITIES = "pokemonAbilities";
    public static final String POKEMON_EGG_GROUPS = "pokemonEggGroups";
    public static final String POKEMON_MOVE = "pokemonMove";
    public static final String POKEMON_DROP_ITEM = "pokemonDropItem";
    public static final String POKEMON_DROP_CHANCE = "pokemonDropChance";
    public static final String POKEMON_DROP_MIN = "pokemonDropMin";
    public static final String POKEMON_DROP_MAX = "pokemonDropMax";
    public static final String POKEMON_BASE_HP = "pokemonBaseHp";
    public static final String POKEMON_BASE_ATTACK = "pokemonBaseAttack";
    public static final String POKEMON_BASE_DEFENSE = "pokemonBaseDefense";
    public static final String POKEMON_BASE_SPECIAL_ATTACK = "pokemonBaseSpecialAttack";
    public static final String POKEMON_BASE_SPECIAL_DEFENSE = "pokemonBaseSpecialDefense";
    public static final String POKEMON_BASE_SPEED = "pokemonBaseSpeed";
    public static final String POKEMON_HEIGHT = "pokemonHeight";
    public static final String POKEMON_WEIGHT = "pokemonWeight";
    public static final String POKEMON_IMPLEMENTED = "pokemonImplemented";
    public static final String POKEMON_TM_MOVE = "pokemonTmMove";
    public static final String POKEMON_EGG_MOVE = "pokemonEggMove";
    public static final String POKEMON_TUTOR_MOVE = "pokemonTutorMove";
    public static final String POKEMON_LEVEL_UP_MOVE = "pokemonLevelUpMove";
    // Create-specific optional metadata. Ownership is established separately;
    // these facts describe how confirmed Create-family content behaves.
    public static final String CREATE_ITEM_KIND = "createItemKind";
    public static final String CREATE_FACTS = "createFacts"; // Comma-separated behavior facts.
    public static final String CREATE_RECIPE_ROLES = "createRecipeRoles"; // Create processing inputs/outputs.
    public static final String CREATE_STRESS_ROLE = "createStressRole"; // consumes_su, generates_su, transfers_su.
    public static final String CREATE_KINETIC_ROLE = "createKineticRole";
    public static final String CREATE_HAS_PONDER = "createHasPonder";
    // AE2-specific optional metadata.
    public static final String AE2_ITEM_KIND = "ae2ItemKind";
    public static final String AE2_FACTS = "ae2Facts"; // Comma-separated Applied Energistics behavior facts.
    public static final String AE2_STORAGE_TIER = "ae2StorageTier";
    public static final String AE2_STORAGE_MEDIUM = "ae2StorageMedium";
    // Mekanism-specific optional metadata.
    public static final String MEKANISM_ITEM_KIND = "mekanismItemKind";
    public static final String MEKANISM_FACTS = "mekanismFacts"; // Comma-separated Mekanism behavior facts.
    public static final String MEKANISM_TIER = "mekanismTier";
    // Generic storage-family metadata. These intentionally follow the
    // *ItemKind / *Facts / *Tier conventions used by broad metadata search.
    public static final String STORAGE_ITEM_KIND = "storageItemKind";
    public static final String STORAGE_FACTS = "storageFacts";
    public static final String STORAGE_TIER = "storageTier";
    // Sophisticated Storage/Backpacks-specific optional metadata.
    public static final String SOPHISTICATED_ITEM_KIND = "sophisticatedItemKind";
    public static final String SOPHISTICATED_FACTS = "sophisticatedFacts";
    public static final String SOPHISTICATED_TIER = "sophisticatedTier";
    // Atlas-specific
    public static final String DIMENSION = "dimension";
    public static final String ENTITY_CATEGORY = "entityCategory";
    public static final String ENTITY_TRAITS = "entityTraits"; // Space-separated semantic entity traits
    public static final String ENTITY_HEALTH = "entityHealth";
    public static final String ENTITY_ATTACK_DAMAGE = "entityAttackDamage";
    public static final String FIRE_IMMUNE = "fireImmune";
    public static final String TEMPERATURE = "temperature"; // Biome base temperature (float string)
    public static final String DOWNFALL = "downfall";        // Biome humidity/wetness (float string)
    public static final String FOG_COLOR = "fogColor";       // Biome fog color (hex int string)
    public static final String FOLIAGE_COLOR = "foliageColor"; // Biome foliage color override (hex int string)
    public static final String GRASS_COLOR = "grassColor";   // Biome grass color override (hex int string)
    public static final String TEMPERATURE_MODIFIER = "temperatureModifier"; // "frozen" or empty
    // Player-specific (transient, never persisted)
    public static final String PLAYER_UUID = "playerUuid";

    private SearchNodeKeys() {
    }
}
