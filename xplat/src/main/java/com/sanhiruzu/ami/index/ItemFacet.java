package com.sanhiruzu.ami.index;

import java.util.Arrays;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

public enum ItemFacet {
    EDIBLE("edible"),
    PLACEABLE_FOOD("placeable_food"),
    COMPOSTABLE("compostable"),
    FOOD_MEAL("food_meal"),
    FOOD_DRINK("food_drink"),
    FOOD_PROTEIN("food_protein"),
    PLACEABLE("placeable"),
    HAS_BLOCK_ENTITY("has_block_entity"),
    INTERACTIVE_BLOCK("interactive_block"),
    HAS_ENERGY("has_energy"),
    STORAGE("storage"),
    MELEE_WEAPON("melee_weapon"),
    RANGED_WEAPON("ranged_weapon"),
    PROJECTILE("projectile"),
    HARVEST_TOOL("harvest_tool"),
    UTILITY_TOOL("utility_tool"),
    ARMOR_HEAD("armor_head"),
    ARMOR_CHEST("armor_chest"),
    ARMOR_LEGS("armor_legs"),
    ARMOR_FEET("armor_feet"),
    ARMOR_ANIMAL("armor_animal"),
    POTION("potion"),
    ENCHANTED_BOOK("enchanted_book"),
    MAGIC_REAGENT("magic_reagent"),
    MAGIC_ARTIFACT("magic_artifact"),
    SPAWN_EGG("spawn_egg"),
    MOB_BUCKET("mob_bucket"),
    REDSTONE_LOGIC("redstone_logic"),
    REDSTONE_SIGNAL("redstone_signal"),
    TRANSPORT("transport"),
    MACHINE("machine"),
    INGOT("ingot"),
    GEM("gem"),
    NUGGET("nugget"),
    RAW_MATERIAL("raw_material"),
    DUST("dust"),
    SEED("seed"),
    CROP("crop"),
    NATURE_MISC("nature_misc"),
    FUNGI("fungi"),
    LOG("log"),
    LEAVES("leaves"),
    FLOWER("flower"),
    WOOD_BLOCK("wood_block"),
    STONE_BLOCK("stone_block"),
    SOIL_BLOCK("soil_block"),
    GLASS_BLOCK("glass_block"),
    INGREDIENT_ORGANIC("ingredient_organic"),
    INGREDIENT_MINERAL("ingredient_mineral"),
    INGREDIENT_DYE("ingredient_dye"),
    UTILITY_NAVIGATION("utility_navigation"),
    UTILITY_MISC("utility_misc"),
    SOCIAL_PLAYERS("social_players"),
    SOCIAL_CLAIMS("social_claims"),
    LIGHT_SOURCE("light_source"),
    DECORATIVE_BLOCK("decorative_block"),
    DOOR("door"),
    TRAPDOOR("trapdoor"),
    FENCE_GATE("fence_gate"),
    RAIL("rail"),
    STAIRS("stairs"),
    SLAB("slab"),
    WALL("wall"),
    FENCE("fence"),
    PANE("pane");

    private static final Map<String, ItemFacet> BY_ID = Arrays.stream(values())
            .collect(Collectors.toUnmodifiableMap(ItemFacet::id, Function.identity()));

    private final String id;

    ItemFacet(String id) {
        this.id = id;
    }

    public String id() {
        return id;
    }

    public static ItemFacet byId(String id) {
        return BY_ID.get(id);
    }
}
