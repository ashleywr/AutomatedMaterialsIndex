package com.sanhiruzu.ami.index;

import net.minecraft.resources.Identifier;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

final class EvidenceCollector {
    private EvidenceCollector() {
    }

    static List<ClassificationEvidence> collect(Identifier id, FacetProfile profile) {
        List<ClassificationEvidence> evidence = new ArrayList<>();
        String path = id.getPath().toLowerCase(Locale.ROOT);
        Set<String> tokens = LexicalEvidenceDictionary.tokenize(path);

        EnumSet<ItemFacet> facets = profile.facets();
        Map<String, String> attributes = profile.attributes();
        List<ClassificationEvidence> lexical = LexicalEvidenceDictionary.match(tokens);
        if (isVanillaConduitBlock(attributes)) {
            lexical = lexical.stream()
                    .filter(item -> !"tech_cables".equals(item.id()))
                    .toList();
        }
        if (hasStrongerContainerContext(facets)) {
            lexical = lexical.stream()
                    .filter(item -> !"utility_container".equals(item.id()))
                    .toList();
        }
        evidence.addAll(lexical);

        addComponentEvidence(attributes, evidence);
        addClassEvidence(attributes, facets, evidence);
        addCreativeTabEvidence(attributes, evidence);
        addTrustedTagEvidence(attributes, evidence);
        addRecipeEvidence(attributes, evidence);

        if (hasAny(facets, ItemFacet.SPAWN_EGG, ItemFacet.MOB_BUCKET)) {
            evidence.add(e("facet.spawn_egg", "facet", "bestiary", "", 110, "spawn egg or mob bucket facet"));
        }
        if (facets.contains(ItemFacet.TEMPLATE)) {
            evidence.add(e("facet.template", "facet", "tech", "templates", 100, "template facet"));
        }
        if (facets.contains(ItemFacet.RAIL) || facets.contains(ItemFacet.TRANSPORT)) {
            evidence.add(e("facet.transport", "facet", "tech", "transport", 85, "rail/transport facet"));
        }
        if (facets.contains(ItemFacet.CABLE)) {
            evidence.add(e("facet.cable", "facet", "tech", "cables", 85, "cable facet"));
        }
        if (facets.contains(ItemFacet.MECHANICAL_COMPONENT)) {
            evidence.add(e("facet.mechanical_component", "facet", "tech", "parts", 80, "mechanical component facet"));
        }
        if (facets.contains(ItemFacet.TECH_COMPONENT)) {
            evidence.add(e("facet.tech_component", "facet", "tech", techComponentSubcategory(path), 75, "tech component facet"));
        }
        if (hasAny(facets, ItemFacet.MACHINE, ItemFacet.WORKSTATION, ItemFacet.HAS_ENERGY, ItemFacet.INTERACTIVE_BLOCK)) {
            evidence.add(e("facet.machine", "facet", "tech", "machines", 75, "machine/workstation/energy/interactive facet"));
        }
        if (hasAny(facets, ItemFacet.ACTIVE_REDSTONE_LOGIC, ItemFacet.REDSTONE_LOGIC)) {
            evidence.add(e("facet.redstone", "facet", "tech", "redstone", 95, "active redstone facet"));
        } else if (hasAny(facets, ItemFacet.REDSTONE_SIGNAL, ItemFacet.PASSIVE_COMPARATOR_OUTPUT)) {
            evidence.add(e("facet.passive_redstone", "facet", "tech", "redstone", 25, "passive comparator/signal facet"));
        }
        if (facets.contains(ItemFacet.STORAGE)) {
            evidence.add(e("facet.storage", "facet", "tech", "machines", 65, "storage facet"));
        }
        if (facets.contains(ItemFacet.UPGRADE)) {
            evidence.add(e("facet.upgrade", "facet", "tech", "upgrades", 80, "upgrade facet"));
        }

        if (facets.contains(ItemFacet.SEED)) {
            evidence.add(e("facet.seed", "facet", "nature", "seeds", 80, "seed facet"));
        }
        if (facets.contains(ItemFacet.CROP)) {
            evidence.add(e("facet.crop", "facet", "nature", "crops", 80, "crop facet"));
        }
        if (facets.contains(ItemFacet.LEAVES)) {
            evidence.add(e("facet.leaves", "facet", "nature", "flora", 85, "leaves facet"));
        }
        if (facets.contains(ItemFacet.FLOWER)) {
            evidence.add(e("facet.flower", "facet", "nature", "flora", 80, "flower facet"));
        }
        if (facets.contains(ItemFacet.WOOD_BLOCK)
                || (facets.contains(ItemFacet.LOG) && facets.contains(ItemFacet.PLACEABLE))) {
            evidence.add(e("facet.wood", "facet", "nature", "wood", 85, "log/wood facet"));
        }
        if (facets.contains(ItemFacet.FUNGI)) {
            evidence.add(e("facet.fungi", "facet", "nature", "fungi", 80, "fungi facet"));
        }
        if (hasAny(facets, ItemFacet.EDIBLE, ItemFacet.PLACEABLE_FOOD, ItemFacet.FOOD_MEAL, ItemFacet.FOOD_DRINK, ItemFacet.FOOD_PROTEIN)) {
            evidence.add(e("facet.food", "facet", "nature", foodSubcategory(path, facets), 80, "food facet"));
        }
        if (facets.contains(ItemFacet.NATURE_MISC) || facets.contains(ItemFacet.COMPOSTABLE)) {
            evidence.add(e("facet.nature_misc", "facet", "nature", "flora", 45, "nature misc/compostable facet"));
        }

        if (hasAny(facets, ItemFacet.DOOR, ItemFacet.TRAPDOOR)) {
            evidence.add(e("facet.functional_block", "facet", "masonry", "functional", 95, "door/trapdoor facet"));
        }
        if (facets.contains(ItemFacet.FENCE_GATE)) {
            evidence.add(e("facet.fence_gate", "facet", "masonry", "functional", 110, "fence gate facet"));
        }
        if (facets.contains(ItemFacet.STAIRS)) evidence.add(e("facet.stairs", "facet", "masonry", "stairs", 95, "stairs facet"));
        if (facets.contains(ItemFacet.SLAB)) evidence.add(e("facet.slab", "facet", "masonry", "slab", 95, "slab facet"));
        if (facets.contains(ItemFacet.WALL)) evidence.add(e("facet.wall", "facet", "masonry", "wall", tokens.contains("wall") || tokens.contains("walls") ? 115 : 95, "wall facet"));
        if (facets.contains(ItemFacet.FENCE)) evidence.add(e("facet.fence", "facet", "masonry", "fence", tokens.contains("fence") || tokens.contains("fences") ? 115 : 95, "fence facet"));
        if (facets.contains(ItemFacet.PANE)) evidence.add(e("facet.pane", "facet", "masonry", "pane", 85, "pane facet"));
        addStructuralTokenEvidence(tokens, facets, evidence);
        if (facets.contains(ItemFacet.STONE_BLOCK) || "stone".equals(attributes.get(SearchNodeKeys.BLOCKS_MATERIAL))) {
            evidence.add(e("facet.stone", "facet", "geology", "stone", 55, "stone material facet"));
        }
        if ((tokens.contains("brick") || tokens.contains("bricks") || tokens.contains("tile") || tokens.contains("tiles")
                || "bricks".equals(attributes.get(SearchNodeKeys.VARIANT_GROUP)))
                && facets.contains(ItemFacet.PLACEABLE)) {
            evidence.add(e("shape.masonry_block", "shape", "masonry", "full_block", 60, "masonry block token"));
        }
        if (facets.contains(ItemFacet.SOIL_BLOCK) || "soil".equals(attributes.get(SearchNodeKeys.BLOCKS_MATERIAL))) {
            evidence.add(e("facet.soil", "facet", "geology", "terrain", 55, "soil material facet"));
        }
        if (facets.contains(ItemFacet.PLACEABLE) && !isPartialPlacement(attributes)) {
            evidence.add(e("facet.placeable", "facet", "masonry", "full_block", 25, "placeable fallback"));
        }

        if (facets.contains(ItemFacet.DECORATIVE_BLOCK)) {
            evidence.add(e("facet.decorative", "facet", "decoration", "furniture", 60, "decorative block facet"));
        }
        if (facets.contains(ItemFacet.LIGHT_SOURCE) && hasPrimaryLightingToken(tokens)) {
            evidence.add(e("facet.primary_lighting", "facet", "decoration", "lighting", 85, "light source plus lighting token"));
        }

        if (hasAny(facets, ItemFacet.MELEE_WEAPON, ItemFacet.RANGED_WEAPON, ItemFacet.HARVEST_TOOL)
                || (facets.contains(ItemFacet.PROJECTILE) && hasAmmoProjectileContext(tokens, attributes))
                || (facets.contains(ItemFacet.UTILITY_TOOL) && !hasEquipmentArmorEvidence(facets, attributes))) {
            evidence.add(e("facet.tool", "facet", "tools", toolSubcategory(facets), 80, "tool/weapon facet"));
        }
        if (hasAny(facets, ItemFacet.ARMOR_HEAD, ItemFacet.ARMOR_CHEST, ItemFacet.ARMOR_LEGS, ItemFacet.ARMOR_FEET, ItemFacet.ARMOR_ANIMAL, ItemFacet.CURIO)) {
            int weight = attributes.getOrDefault(SearchNodeKeys.EQUIPMENT_SLOT, "").isBlank() ? 80 : 100;
            evidence.add(e("facet.armor", "facet", "armor", armorSubcategory(facets), weight, "armor/curio facet"));
        }
        if (hasAny(facets, ItemFacet.INGREDIENT_DYE, ItemFacet.INGREDIENT_MINERAL, ItemFacet.INGREDIENT_ORGANIC)) {
            evidence.add(e("facet.ingredient", "facet", "ingredients", ingredientSubcategory(facets), 70, "ingredient facet"));
        }
        if (hasAny(facets, ItemFacet.UTILITY_NAVIGATION, ItemFacet.UTILITY_MEDICAL, ItemFacet.UTILITY_CURRENCY,
                ItemFacet.BOOK, ItemFacet.GUIDE_BOOK, ItemFacet.UTILITY_MISC)) {
            evidence.add(e("facet.utility", "facet", "utility", utilitySubcategory(facets), 65, "utility facet"));
        }
        if (hasAny(facets, ItemFacet.POTION, ItemFacet.ENCHANTED_BOOK, ItemFacet.MAGIC_ARTIFACT, ItemFacet.MAGIC_REAGENT)) {
            evidence.add(e("facet.magic", "facet", "magic", magicSubcategory(facets), 70, "magic facet"));
        }
        if (hasAny(facets, ItemFacet.SOCIAL_PLAYERS, ItemFacet.SOCIAL_CLAIMS)) {
            evidence.add(e("facet.social", "facet", "social", facets.contains(ItemFacet.SOCIAL_CLAIMS) ? "claims" : "players", 80, "social facet"));
        }

        return evidence;
    }

    private static void addComponentEvidence(Map<String, String> attributes, List<ClassificationEvidence> evidence) {
        String componentFacts = attributes.getOrDefault(SearchNodeKeys.COMPONENT_FACTS, "");
        if (hasCsvToken(componentFacts, "food")) {
            evidence.add(e("component.food", "component", "nature", "snacks", 95, "FOOD data component"));
        }
        if (hasCsvToken(componentFacts, "potion_contents")) {
            evidence.add(e("component.potion_contents", "component", "magic", "potions", 100, "POTION_CONTENTS data component"));
        }
        if (hasCsvToken(componentFacts, "container") || hasCsvToken(componentFacts, "bundle_contents")) {
            evidence.add(e("component.container", "component", "tech", "machines", 60, "container/bundle data component"));
        }
        if (hasCsvToken(componentFacts, "max_damage")) {
            evidence.add(e("component.max_damage", "component", "tools", "utility", 35, "MAX_DAMAGE data component"));
        }
        if (hasCsvToken(componentFacts, "tool")) {
            evidence.add(e("component.tool", "component", "tools", "utility", 35, "TOOL data component"));
        }
        if (hasCsvToken(componentFacts, "bucket_entity_data")) {
            evidence.add(e("component.bucket_entity_data", "component", "bestiary", "", 80, "BUCKET_ENTITY_DATA data component"));
        }
        if (hasCsvToken(componentFacts, "block_entity_data")) {
            evidence.add(e("component.block_entity_data", "component", "tech", "machines", 35, "BLOCK_ENTITY_DATA data component"));
        }
    }

    private static void addClassEvidence(Map<String, String> attributes, EnumSet<ItemFacet> facets, List<ClassificationEvidence> evidence) {
        String itemClass = attributes.getOrDefault(SearchNodeKeys.ITEM_CLASS, "").toLowerCase(Locale.ROOT);
        String blockClass = attributes.getOrDefault(SearchNodeKeys.BLOCK_CLASS, "").toLowerCase(Locale.ROOT);
        String combined = itemClass + " " + blockClass;
        String tabId = attributes.getOrDefault(SearchNodeKeys.CREATIVE_TAB_ID, "").toLowerCase(Locale.ROOT);
        String tabLabel = attributes.getOrDefault(SearchNodeKeys.CREATIVE_TAB_LABEL, "").toLowerCase(Locale.ROOT);
        String creativeContext = tabId + " " + tabLabel;
        if (combined.contains("foodblock") || combined.contains("platefood")
                || combined.contains("bowlfood") || combined.contains("bottlefood")
                || combined.contains("simpleplatedfood") || combined.contains("smallplatedfood")) {
            evidence.add(e("class.food_block", "class", "nature", "meals", 110, "food block class"));
        } else if (combined.contains("plateblock")
                && containsAny(creativeContext, "food", "delight", "cuisine", "farm", "crop")) {
            evidence.add(e("class.food_plate", "class", "nature", "meals", 95, "food display plate class"));
        }
        if (combined.contains("trellis")) {
            evidence.add(e("class.trellis", "class", "nature", "flora", 80, "trellis class"));
        }
        if (combined.contains("compressedironblock")) {
            evidence.add(e("class.material_block", "class", "ingredients", "mineral", 80, "compressed material block class"));
        }
        if (containsAny(combined, "machine", "processor", "processing", "basinblock", "controllerblock", "foundry",
                "chunkloader", "depotblock", "portablestorageinterface", "clutchblock", "kineticbattery",
                "backtankblock", "fluidtank", "fluidvessel", "energycell", "energyacceptor", "diskdrive",
                "datadial")) {
            evidence.add(e("class.tech_block", "class", "tech", "machines", 75, "tech-like block class"));
        }
        if (isVanillaConduitBlock(attributes)) {
            evidence.add(e("class.vanilla_conduit", "class", "utility", "misc", 130, "vanilla ConduitBlock class"));
        }
        if (containsAny(combined, "powerbottleitem", "powerbottleblock")) {
            evidence.add(e("class.power_bottle", "class", "magic", "artifacts", 105, "power bottle class"));
        }
        if (containsAny(combined, "saddleitem")) {
            evidence.add(e("class.saddle_item", "class", "armor", "animal", 100, "saddle item class"));
        }
        if (containsAny(combined, "flaskitem", "bottleitem") && !hasStrongerContainerContext(facets)) {
            evidence.add(e("class.bottle_container", "class", "utility", "misc", 75, "bottle/flask item class"));
        }
        if (containsAny(combined, "crucibleblock", "plinthblock")) {
            evidence.add(e("class.workstation_block", "class", "tech", "machines", 85, "workstation-like block class"));
        }
        if (containsAny(combined, "symbolblock", "symbolitem")) {
            evidence.add(e("class.symbol", "class", "magic", "artifacts", 85, "symbol class"));
        }
    }

    private static boolean isVanillaConduitBlock(Map<String, String> attributes) {
        return "net.minecraft.world.level.block.ConduitBlock"
                .equals(attributes.getOrDefault(SearchNodeKeys.BLOCK_CLASS, ""));
    }

    private static boolean hasStrongerContainerContext(EnumSet<ItemFacet> facets) {
        return hasAny(facets,
                ItemFacet.EDIBLE,
                ItemFacet.FOOD_MEAL,
                ItemFacet.FOOD_DRINK,
                ItemFacet.FOOD_PROTEIN,
                ItemFacet.POTION,
                ItemFacet.MAGIC_ARTIFACT,
                ItemFacet.MAGIC_REAGENT);
    }

    private static void addCreativeTabEvidence(Map<String, String> attributes, List<ClassificationEvidence> evidence) {
        String tabId = attributes.getOrDefault(SearchNodeKeys.CREATIVE_TAB_ID, "").toLowerCase(Locale.ROOT);
        String tabLabel = attributes.getOrDefault(SearchNodeKeys.CREATIVE_TAB_LABEL, "").toLowerCase(Locale.ROOT);
        String combined = tabId + " " + tabLabel;
        if (combined.isBlank()) {
            return;
        }
        if (containsAny(combined, "furniture", "decor", "deco", "decoration")) {
            evidence.add(e("creative_tab.decoration", "creative_tab", "decoration", "furniture", 45, "creative tab=" + tabLabel));
        }
        if (containsAny(combined, "brick", "bricks", "building", "blocks", "masonry")) {
            evidence.add(e("creative_tab.building", "creative_tab", "masonry", "full_block", 35, "creative tab=" + tabLabel));
        }
        if (containsAny(combined, "food", "delight", "cuisine", "farm", "crop")) {
            evidence.add(e("creative_tab.food", "creative_tab", "nature", "snacks", 45, "creative tab=" + tabLabel));
        }
        if (containsAny(combined, "track", "tracks", "rail", "rails", "train")) {
            evidence.add(e("creative_tab.rail", "creative_tab", "tech", "transport", 55, "creative tab=" + tabLabel));
        }
        if (containsAny(combined, "blueprint", "template", "schematic")) {
            evidence.add(e("creative_tab.template", "creative_tab", "tech", "templates", 55, "creative tab=" + tabLabel));
        }
        if (containsAny(combined, "science")) {
            evidence.add(e("creative_tab.science", "creative_tab", "tech", "parts", 60, "creative tab=" + tabLabel));
        }
        if (containsAny(combined, "storage", "machine", "tech", "mechanism", "engineering")) {
            evidence.add(e("creative_tab.tech", "creative_tab", "tech", "machines", 40, "creative tab=" + tabLabel));
        }
        if (containsAny(combined, "tool", "tools", "utility", "utilities")) {
            evidence.add(e("creative_tab.utility", "creative_tab", "utility", "tools", 50, "creative tab=" + tabLabel));
        }
        if (containsAny(combined, "magic", "spell", "occult", "arcane")) {
            evidence.add(e("creative_tab.magic", "creative_tab", "magic", "artifacts", 40, "creative tab=" + tabLabel));
        }
        if (containsAny(combined, "ingredient", "ingredients")) {
            evidence.add(e("creative_tab.ingredients", "creative_tab", "ingredients", "mineral", 35, "creative tab=" + tabLabel));
        }
    }

    private static void addTrustedTagEvidence(Map<String, String> attributes, List<ClassificationEvidence> evidence) {
        String tags = attributes.getOrDefault(SearchNodeKeys.TAGS, "");
        String blockTags = attributes.getOrDefault(SearchNodeKeys.BLOCK_TAGS, "");
        if (hasTrustedTag(tags, "c:foods") || hasTrustedTag(tags, "forge:foods")) {
            evidence.add(e("tag.foods", "trusted_tag", "nature", "snacks", 70, "c/forge foods tag"));
        }
        if (hasTrustedTag(tags, "c:drinks/magic")) {
            evidence.add(e("tag.magic_drink", "trusted_tag", "magic", "potions", 120, "c drinks/magic tag"));
        }
        if (hasTrustedTag(tags, "c:seeds") || hasTrustedTag(tags, "forge:seeds")) {
            evidence.add(e("tag.seeds", "trusted_tag", "nature", "seeds", 80, "c/forge seeds tag"));
        }
        if (hasTrustedTag(tags, "c:crops") || hasTrustedTag(tags, "forge:crops")) {
            evidence.add(e("tag.crops", "trusted_tag", "nature", "crops", 80, "c/forge crops tag"));
        }
        if (hasTrustedTag(tags, "c:ingots") || hasTrustedTag(tags, "forge:ingots")
                || hasTrustedTag(tags, "c:gems") || hasTrustedTag(tags, "forge:gems")
                || hasTrustedTag(tags, "c:dusts") || hasTrustedTag(tags, "forge:dusts")) {
            evidence.add(e("tag.materials", "trusted_tag", "ingredients", "mineral", 80, "c/forge material tag"));
        }
        if (hasTrustedTag(tags, "minecraft:fence_gates") || hasTrustedTag(blockTags, "minecraft:fence_gates")) {
            evidence.add(e("tag.fence_gates", "trusted_tag", "masonry", "functional", 120, "minecraft fence gates tag"));
        }
        if (hasTrustedTag(tags, "minecraft:walls")) {
            evidence.add(e("tag.walls", "trusted_tag", "masonry", "wall", 115, "minecraft walls item tag"));
        }
        if (hasTrustedTag(tags, "minecraft:fences")) {
            evidence.add(e("tag.fences", "trusted_tag", "masonry", "fence", 115, "minecraft fences item tag"));
        }
        if (hasTrustedTag(tags, "silentgear:blueprints") || hasTrustedTag(tags, "c:blueprint")) {
            evidence.add(e("tag.blueprints", "trusted_tag", "tech", "templates", 85, "blueprint tag"));
        }
        if (hasTrustedTag(tags, "minecraft:doors") || hasTrustedTag(blockTags, "minecraft:doors")) {
            evidence.add(e("tag.doors", "trusted_tag", "masonry", "functional", 90, "minecraft doors tag"));
        }
        if (hasTrustedTag(tags, "minecraft:saplings") || hasTrustedTag(blockTags, "minecraft:saplings")) {
            evidence.add(e("tag.saplings", "trusted_tag", "nature", "seeds", 90, "minecraft saplings tag"));
        }
        if (hasTrustedTag(tags, "minecraft:leaves") || hasTrustedTag(blockTags, "minecraft:leaves")) {
            evidence.add(e("tag.leaves", "trusted_tag", "nature", "flora", 90, "minecraft leaves tag"));
        }
        if (hasTrustedTag(tags, "minecraft:rails") || hasTrustedTag(blockTags, "minecraft:rails")
                || hasTrustedTag(tags, "create:tracks") || hasTrustedTag(blockTags, "create:tracks")) {
            evidence.add(e("tag.rails", "trusted_tag", "tech", "transport", 90, "rail/track tag"));
        }
        if (hasTrustedTag(tags, "minecraft:arrows")) {
            evidence.add(e("tag.arrows", "trusted_tag", "tools", "ammo", 95, "minecraft arrows tag"));
        }
        if (hasTagEnding(tags, "power_bottles")) {
            evidence.add(e("tag.power_bottles", "trusted_tag", "magic", "artifacts", 110, "power bottles tag"));
        }
    }

    private static void addRecipeEvidence(Map<String, String> attributes, List<ClassificationEvidence> evidence) {
        String categories = attributes.getOrDefault(SearchNodeKeys.RECIPE_CATEGORIES, "");
        if (hasRecipeToken(categories, "transmutation") || hasRecipeToken(categories, "dissolve")) {
            evidence.add(e("recipe.alchemy_output", "recipe", "magic", "reagents", 65, "alchemy/transmutation recipe output"));
        }
        if (hasRecipeToken(categories, "brewing") || hasRecipeToken(categories, "ami:brewing")) {
            evidence.add(e("recipe.brewing_output", "recipe", "magic", "potions", 65, "brewing recipe output"));
        }
    }

    private static ClassificationEvidence e(String id, String source, String category, String subcategory, int weight, String reason) {
        return new ClassificationEvidence(id, source, category, subcategory, weight, reason);
    }

    private static boolean hasAny(EnumSet<ItemFacet> facets, ItemFacet... expected) {
        for (ItemFacet facet : expected) {
            if (facets.contains(facet)) {
                return true;
            }
        }
        return false;
    }

    private static void addStructuralTokenEvidence(Set<String> tokens, EnumSet<ItemFacet> facets, List<ClassificationEvidence> evidence) {
        if (!facets.contains(ItemFacet.PLACEABLE)) {
            return;
        }
        if (tokens.contains("stairs") || tokens.contains("stair")) {
            evidence.add(e("shape.stairs", "shape", "masonry", "stairs", 95, "stairs token"));
        }
        if (tokens.contains("slab") || tokens.contains("slabs")) {
            evidence.add(e("shape.slab", "shape", "masonry", "slab", 95, "slab token"));
        }
        if (tokens.contains("wall") || tokens.contains("walls")) {
            evidence.add(e("shape.wall", "shape", "masonry", "wall", 95, "wall token"));
        }
        if (tokens.contains("fence") || tokens.contains("fences")) {
            evidence.add(e("shape.fence", "shape", "masonry", "fence", 95, "fence token"));
        }
        if (tokens.contains("paving") || tokens.contains("pavement")) {
            evidence.add(e("shape.paving", "shape", "masonry", "full_block", 60, "paving token"));
        }
    }

    private static boolean hasEquipmentArmorEvidence(EnumSet<ItemFacet> facets, Map<String, String> attributes) {
        return !attributes.getOrDefault(SearchNodeKeys.EQUIPMENT_SLOT, "").isBlank()
                || facets.contains(ItemFacet.EQUIPPABLE)
                || hasAny(facets,
                ItemFacet.ARMOR_HEAD,
                ItemFacet.ARMOR_CHEST,
                ItemFacet.ARMOR_LEGS,
                ItemFacet.ARMOR_FEET,
                ItemFacet.ARMOR_ANIMAL);
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

    private static boolean hasTrustedTag(String encoded, String expectedPrefix) {
        if (encoded == null || encoded.isBlank()) {
            return false;
        }
        for (String token : encoded.split(",")) {
            String tag = token.trim();
            if (tag.equals(expectedPrefix) || tag.startsWith(expectedPrefix + "/")) {
                return true;
            }
        }
        return false;
    }

    private static boolean hasTagEnding(String encoded, String expectedSuffix) {
        if (encoded == null || encoded.isBlank()) {
            return false;
        }
        String suffix = expectedSuffix.toLowerCase(Locale.ROOT);
        for (String token : encoded.split(",")) {
            String tag = token.trim().toLowerCase(Locale.ROOT);
            if (tag.endsWith(":" + suffix) || tag.endsWith("/" + suffix)) {
                return true;
            }
        }
        return false;
    }

    private static boolean hasRecipeToken(String encoded, String expected) {
        if (encoded == null || encoded.isBlank()) {
            return false;
        }
        String normalizedExpected = expected.toLowerCase(Locale.ROOT);
        for (String token : encoded.split(",")) {
            String recipe = token.trim().toLowerCase(Locale.ROOT);
            if (recipe.equals(normalizedExpected) || recipe.endsWith(":" + normalizedExpected)) {
                return true;
            }
        }
        return false;
    }

    private static boolean hasAmmoProjectileContext(Set<String> tokens, Map<String, String> attributes) {
        if (intersects(tokens, Set.of(
                "arrow", "arrows", "bolt", "bolts", "bullet", "bullets", "round", "rounds",
                "cartridge", "cartridges", "rocket", "ammo", "gun", "shotgun", "cannon",
                "autocannon", "artillery", "mortar", "munition", "munitions"))) {
            return true;
        }
        String tags = attributes.getOrDefault(SearchNodeKeys.TAGS, "");
        String blockTags = attributes.getOrDefault(SearchNodeKeys.BLOCK_TAGS, "");
        return hasTrustedTag(tags, "minecraft:arrows")
                || hasTrustedTag(tags, "createbigcannons:big_cannon_projectiles")
                || hasTrustedTag(blockTags, "createbigcannons:big_cannon_projectiles");
    }

    private static boolean intersects(Set<String> tokens, Set<String> expected) {
        for (String token : expected) {
            if (tokens.contains(token)) {
                return true;
            }
        }
        return false;
    }

    private static boolean containsAny(String value, String... needles) {
        for (String needle : needles) {
            if (value.contains(needle)) {
                return true;
            }
        }
        return false;
    }

    private static String techComponentSubcategory(String path) {
        return path.contains("circuit")
                || path.contains("processor")
                || path.contains("logic")
                || path.contains("calculation")
                || path.contains("engineering")
                || path.contains("chip")
                ? "circuits"
                : "parts";
    }

    private static String foodSubcategory(String path, EnumSet<ItemFacet> facets) {
        if (path.contains("plate")
                || path.contains("bowl")
                || path.contains("pie")
                || path.contains("tart")
                || path.contains("pudding")
                || path.contains("sandwich")
                || path.contains("meatball")
                || path.contains("soup")
                || path.contains("stew")) {
            return "meals";
        }
        if (facets.contains(ItemFacet.FOOD_MEAL)) return "meals";
        if (facets.contains(ItemFacet.FOOD_DRINK)) return "drinks";
        if (facets.contains(ItemFacet.FOOD_PROTEIN)) return "proteins";
        return "snacks";
    }

    private static String toolSubcategory(EnumSet<ItemFacet> facets) {
        if (facets.contains(ItemFacet.MELEE_WEAPON)) return "melee";
        if (facets.contains(ItemFacet.RANGED_WEAPON)) return "ranged";
        if (facets.contains(ItemFacet.PROJECTILE)) return "ammo";
        if (facets.contains(ItemFacet.HARVEST_TOOL)) return "harvest";
        return "utility";
    }

    private static String armorSubcategory(EnumSet<ItemFacet> facets) {
        if (facets.contains(ItemFacet.ARMOR_HEAD)) return "head";
        if (facets.contains(ItemFacet.ARMOR_CHEST)) return "chest";
        if (facets.contains(ItemFacet.ARMOR_LEGS)) return "legs";
        if (facets.contains(ItemFacet.ARMOR_FEET)) return "feet";
        if (facets.contains(ItemFacet.ARMOR_ANIMAL)) return "animal";
        return "curios";
    }

    private static String ingredientSubcategory(EnumSet<ItemFacet> facets) {
        if (facets.contains(ItemFacet.INGREDIENT_DYE)) return "dyes";
        if (facets.contains(ItemFacet.INGREDIENT_MINERAL)) return "mineral";
        return "organic";
    }

    private static String utilitySubcategory(EnumSet<ItemFacet> facets) {
        if (facets.contains(ItemFacet.UTILITY_NAVIGATION)) return "navigation";
        if (facets.contains(ItemFacet.UTILITY_MEDICAL)) return "medical";
        if (facets.contains(ItemFacet.UTILITY_CURRENCY)) return "currency";
        if (facets.contains(ItemFacet.GUIDE_BOOK) || facets.contains(ItemFacet.BOOK)) return "books";
        return "misc";
    }

    private static String magicSubcategory(EnumSet<ItemFacet> facets) {
        if (facets.contains(ItemFacet.POTION)) return "potions";
        if (facets.contains(ItemFacet.ENCHANTED_BOOK)) return "books";
        if (facets.contains(ItemFacet.MAGIC_ARTIFACT)) return "artifacts";
        return "reagents";
    }

    private static boolean isPartialPlacement(Map<String, String> attributes) {
        return "partial".equals(attributes.getOrDefault("blockShape", ""));
    }

    private static boolean hasPrimaryLightingToken(Set<String> tokens) {
        return tokens.contains("lamp")
                || tokens.contains("lantern")
                || tokens.contains("chandelier")
                || tokens.contains("sconce")
                || tokens.contains("brazier")
                || tokens.contains("candelabra")
                || tokens.contains("candle")
                || tokens.contains("torch")
                || tokens.contains("glowstone")
                || tokens.contains("shroomlight")
                || tokens.contains("froglight")
                || tokens.contains("beacon");
    }
}
