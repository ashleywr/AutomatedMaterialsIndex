package com.sanhiruzu.ami.index;

/**
 * String constants for SearchNode.metadata() keys.
 */
public final class SearchNodeKeys {
    private SearchNodeKeys() {
    }

    // Shared
    public static final String MOD_ID = "modId";

    // Item-specific
    public static final String ONTOLOGY_CATEGORY = "ontologyCategory";    // AmiOntology.Category.id
    public static final String ONTOLOGY_SUBCATEGORY = "ontologySubcategory"; // AmiOntology.SubCategory.id
    public static final String VARIANT_GROUP = "variantGroup";
    public static final String COLOR_BUCKET = "colorBucket";
    public static final String MATERIAL_GROUP = "materialGroup";
    public static final String TAGS = "tags";  // Comma-separated tag paths
    public static final String FACETS = "facets"; // Comma-separated stable facet ids
    public static final String ESM_CAPACITY = "emsCapacity";  // Equivalent Stack Metric
    public static final String ENERGY_CAPACITY = "energy_capacity"; // Maximum FE/RF capacity
    public static final String REQUIRED_TOOL = "requiredTool";
    public static final String DPS = "dps";          // Damage per second (Assembly Lab)
    public static final String SEARCH_TOKENS = "searchTokens";  // Space-separated synthetic search tokens
    public static final String ACCESS_LEVEL = "accessLevel";  // "survival", "creative", "cheat", or "dev"
    public static final String VISIBILITY = "visibility";    // "hidden" = not in any creative tab
    public static final String OBTAINABILITY = "obtainability"; // "no_recipe" = no recipe output
    public static final String RECIPE_CATEGORIES = "recipeCategories"; // comma-separated recipe type IDs
    public static final String CREATIVE_TAB_ID = "creativeTabId";
    public static final String CREATIVE_TAB_LABEL = "creativeTabLabel";
    public static final String SUBTYPE_OF = "subtypeOf";     // base item id for subtype nodes
    public static final String POTION_EFFECT = "potionEffect";  // full ResourceLocation of the potion effect (e.g. "minecraft:fire_resistance")
    public static final String COLLAPSE_FAMILY = "collapseFamily"; // stable UI grouping key for explicit family collapse
    public static final String COLLAPSE_LABEL = "collapseLabel";  // user-facing label for explicit family collapse
    public static final String BLOCKS_MATERIAL = "blocksMaterial"; // material family for block subcategory (shape/material toggle)

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
}
