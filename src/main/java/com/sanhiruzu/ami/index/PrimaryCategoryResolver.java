package com.sanhiruzu.ami.index;

import net.minecraft.resources.ResourceLocation;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

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

    private PrimaryCategoryResolver() {}

    public static CategoryAssignment resolve(ResourceLocation id, FacetProfile profile) {
        if (id == null) {
            return fallback();
        }

        String path = id.getPath().toLowerCase(Locale.ROOT);
        var facets = profile.facets();
        var attributes = new HashMap<>(profile.attributes());

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
        if (hasAny(facets, ItemFacet.EDIBLE, ItemFacet.COMPOSTABLE, ItemFacet.SEED, ItemFacet.CROP, ItemFacet.NATURE_MISC, ItemFacet.FUNGI, ItemFacet.LOG, ItemFacet.LEAVES, ItemFacet.FLOWER)) {
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

    private static boolean isThrowableIngredient(Set<ItemFacet> facets) {
        return facets.contains(ItemFacet.PROJECTILE)
                && hasAny(facets, ItemFacet.INGREDIENT_ORGANIC, ItemFacet.INGREDIENT_MINERAL, ItemFacet.INGREDIENT_DYE)
                && !hasAny(facets, ItemFacet.MELEE_WEAPON, ItemFacet.RANGED_WEAPON, ItemFacet.HARVEST_TOOL, ItemFacet.UTILITY_TOOL);
    }

    private static String classifyTechSubcategory(Set<ItemFacet> facets) {
        if (facets.contains(ItemFacet.MACHINE) || facets.contains(ItemFacet.HAS_ENERGY) || facets.contains(ItemFacet.STORAGE)) {
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
