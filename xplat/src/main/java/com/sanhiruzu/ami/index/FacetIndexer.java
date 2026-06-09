package com.sanhiruzu.ami.index;

import com.sanhiruzu.ami.platform.Services;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.*;
import net.minecraft.world.level.block.ComposterBlock;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.state.BlockState;

import java.util.*;
import java.util.stream.Collectors;

public final class FacetIndexer {
    private static final Set<String> DRINK_PATH_TOKENS = Set.of(
            "water", "juice", "juicebox", "soda", "beer", "vodka", "wodka",
            "whisky", "whiskey", "wine", "coffee", "tea", "cider", "canteen",
            "milk", "porpsi", "coke", "drink"
    );
    private static final Set<String> MEAL_PATH_TOKENS = Set.of(
            "sandwich", "burger", "pizza", "pie", "salad", "noodle", "noodles",
            "pasta", "rice", "roll", "rolls", "dumpling", "dumplings", "kebab",
            "mre", "canned", "macandcheese", "bowl", "plate", "meal"
    );
    private static final Set<String> PROTEIN_PATH_TOKENS = Set.of(
            "beef", "chicken", "porkchop", "pork", "cod", "salmon", "rabbit",
            "mutton", "fish", "meat", "bacon", "ham", "sausage", "ribs",
            "rat", "cockroach", "flesh", "duck", "venison",
            "lizard", "catfish", "bass", "koi"
    );
    private static final List<String> DEFAULT_COMPONENT_FACT_FIELDS = List.of(
            "POTION_CONTENTS",
            "TOOL",
            "MAX_DAMAGE",
            "CONTAINER",
            "BUNDLE_CONTENTS",
            "CUSTOM_DATA",
            "ENTITY_DATA",
            "BUCKET_ENTITY_DATA",
            "BLOCK_ENTITY_DATA"
    );
    private static final List<String> STACK_COMPONENT_FACT_FIELDS = List.of(
            "TOOL",
            "DAMAGE"
    );

    private FacetIndexer() {
    }

    public static FacetProfile index(Item item, ResourceLocation id, ItemStack stack) {
        EnumSet<ItemFacet> facets = EnumSet.noneOf(ItemFacet.class);
        Map<String, String> attributes = new LinkedHashMap<>();
        String path = id.getPath().toLowerCase(Locale.ROOT);
        List<String> tags = collectTags(item);
        attributes.put(SearchNodeKeys.ITEM_CLASS, item.getClass().getName());

        boolean hasFood = Services.PLATFORM.hasFood(stack);
        if (hasFood) {
            facets.add(ItemFacet.EDIBLE);
        }
        if (stack.getUseAnimation() == UseAnim.DRINK) {
            facets.add(ItemFacet.FOOD_DRINK);
        }
        if (ComposterBlock.COMPOSTABLES.containsKey(item)) {
            facets.add(ItemFacet.COMPOSTABLE);
        }
        applyComponentFacts(stack, hasFood, facets, attributes);
        applyItemClassFacts(item, id, facets, attributes);
        applyPathFacts(id, path, facets, attributes);
        applyTypeFacts(item, path, facets);
        applyEquipmentFacts(item, stack, path, facets, attributes);
        applyComponentPromotions(attributes, facets);
        applyTagFacts(tags, facets);

        if (item instanceof BlockItem blockItem) {
            applyBlockFacts(blockItem, stack, path, facets, attributes);
        }

        return new FacetProfile(facets, attributes);
    }

    private static void applyComponentFacts(ItemStack stack, boolean hasFood,
                                            EnumSet<ItemFacet> facets, Map<String, String> attributes) {
        List<String> facts = new ArrayList<>();
        Item item = stack.getItem();
        Set<String> defaultComponents = Services.PLATFORM.getDefaultItemComponentNames(item, DEFAULT_COMPONENT_FACT_FIELDS);
        Set<String> stackComponents = Services.PLATFORM.getStackComponentNames(stack, STACK_COMPONENT_FACT_FIELDS);
        if (hasFood) {
            facts.add("food");
            facets.add(ItemFacet.EDIBLE);
        }
        if (defaultComponents.contains("POTION_CONTENTS")) {
            facts.add("potion_contents");
            facets.add(ItemFacet.POTION);
        }
        if (defaultComponents.contains("TOOL") || stackComponents.contains("TOOL")) {
            facts.add("tool");
        }
        if (defaultComponents.contains("MAX_DAMAGE")) {
            facts.add("max_damage");
        }
        if (stackComponents.contains("DAMAGE")) {
            facts.add("damage");
        }
        if (defaultComponents.contains("CONTAINER")) {
            facts.add("container");
            facets.add(ItemFacet.STORAGE);
        }
        if (defaultComponents.contains("BUNDLE_CONTENTS")) {
            facts.add("bundle_contents");
            facets.add(ItemFacet.STORAGE);
        }
        if (defaultComponents.contains("CUSTOM_DATA")) {
            facts.add("custom_data");
        }
        if (defaultComponents.contains("ENTITY_DATA")) {
            facts.add("entity_data");
        }
        if (defaultComponents.contains("BUCKET_ENTITY_DATA")) {
            facts.add("bucket_entity_data");
        }
        if (defaultComponents.contains("BLOCK_ENTITY_DATA")) {
            facts.add("block_entity_data");
        }
        if (!facts.isEmpty()) {
            attributes.put(SearchNodeKeys.COMPONENT_FACTS, String.join(",", facts));
        }
    }

    private static void applyItemClassFacts(Item item, ResourceLocation id, EnumSet<ItemFacet> facets, Map<String, String> attributes) {
        String itemClass = item.getClass().getName().toLowerCase(Locale.ROOT);
        if (item instanceof BucketItem) {
            attributes.put(SearchNodeKeys.IS_BUCKET_ITEM, "true");
        }
        if (containsAny(itemClass, "bottleitem", "flaskitem")) {
            facets.add(ItemFacet.UTILITY_MISC);
        }
        if ("tconstruct".equals(id.getNamespace())) {
            if (containsAny(itemClass, "coppercanitem")) {
                facets.add(ItemFacet.FLUID_CONTAINER);
            }
            if (containsAny(itemClass, "piggybackpackitem", "glowballitem", "eflnitem")) {
                facets.add(ItemFacet.UTILITY_MISC);
            }
            if (containsAny(itemClass, "crystalshotitem")) {
                facets.add(ItemFacet.RANGED_WEAPON);
                facets.add(ItemFacet.PROJECTILE);
            }
        }
        if (containsAny(itemClass, "malletitem", "displaceritem")) {
            facets.add(ItemFacet.UTILITY_TOOL);
        }
        if (containsAny(itemClass, "crystalitem", "stardustitem")) {
            facets.add(ItemFacet.MAGIC_REAGENT);
        }
        if (containsAny(itemClass, "itemmodbook", "modonomiconitem", "guidebookitem", "guideitem", "guide_book",
                "manualitem", "lexiconitem", "codexitem", "cookbookitem", "materialbookitem",
                "tinkerbookitem", "animaldictionary", "codexarcana", "hexereibookitem")) {
            facets.add(ItemFacet.BOOK);
            facets.add(ItemFacet.GUIDE_BOOK);
            attributes.put(SearchNodeKeys.GUIDE_BOOK_CANDIDATE, "true");
            if (itemClass.contains("patchouli")) {
                attributes.put(SearchNodeKeys.GUIDE_BOOK_SYSTEM, "patchouli");
            } else if (itemClass.contains("guideme")) {
                attributes.put(SearchNodeKeys.GUIDE_BOOK_SYSTEM, "guideme");
            } else if (itemClass.contains("modonomicon") || itemClass.contains("spectrum")) {
                attributes.put(SearchNodeKeys.GUIDE_BOOK_SYSTEM, "modonomicon");
            } else if (itemClass.contains("materialbookitem") && itemClass.contains("silentgear")) {
                attributes.put(SearchNodeKeys.GUIDE_BOOK_SYSTEM, "silentgear_materials");
            } else if (itemClass.contains("tinkerbookitem") || itemClass.contains("tinkers_reforged")) {
                attributes.put(SearchNodeKeys.GUIDE_BOOK_SYSTEM, "mantle_book");
            } else if (itemClass.contains("immersiveengineering") && itemClass.contains("manualitem")) {
                attributes.put(SearchNodeKeys.GUIDE_BOOK_SYSTEM, "immersiveengineering_manual");
            } else if (itemClass.contains("animaldictionary") || itemClass.contains("alexscaves")) {
                attributes.put(SearchNodeKeys.GUIDE_BOOK_SYSTEM, "resource_book");
            } else if (itemClass.contains("codexarcana")) {
                attributes.put(SearchNodeKeys.GUIDE_BOOK_SYSTEM, "mna_guide_json");
            } else if (itemClass.contains("hexereibookitem")) {
                attributes.put(SearchNodeKeys.GUIDE_BOOK_SYSTEM, "hexerei_book");
            }
        }
    }

    private static void applyPathFacts(ResourceLocation id, String path, EnumSet<ItemFacet> facets, Map<String, String> attributes) {
        if (path.contains("spell") || path.contains("wand") || path.contains("staff") || path.contains("scroll")) {
            facets.add(ItemFacet.MAGIC_ARTIFACT);
        }
        if (containsPathToken(path, "rune", "runes")
                || path.contains("essence")) {
            facets.add(ItemFacet.MAGIC_REAGENT);
        }
        if (path.contains("minecart") || path.contains("boat") || path.contains("raft")) {
            facets.add(ItemFacet.TRANSPORT);
        }
        if (isMechanicalComponentPath(path)) {
            facets.add(ItemFacet.MECHANICAL_COMPONENT);
            facets.add(ItemFacet.TECH_COMPONENT);
        }
        if (path.contains("upgrade")) {
            facets.add(ItemFacet.UPGRADE);
        }
        if (isTemplatePath(path)) {
            facets.add(ItemFacet.TEMPLATE);
        }
        if (isMedicalPath(path)) {
            facets.add(ItemFacet.UTILITY_MEDICAL);
        }
        if (isCurrencyPath(path)) {
            facets.add(ItemFacet.UTILITY_CURRENCY);
        }
        if (path.contains("redstone") || path.contains("comparator") || path.contains("repeater")
                || path.contains("observer") || path.contains("piston") || path.contains("lever")) {
            facets.add(ItemFacet.ACTIVE_REDSTONE_LOGIC);
            facets.add(ItemFacet.REDSTONE_LOGIC);
            facets.add(ItemFacet.REDSTONE_SIGNAL);
        }
        if (containsPathToken(path, "compass", "map", "maps", "clock", "spyglass")) {
            facets.add(ItemFacet.UTILITY_NAVIGATION);
        }
        if (containsPathToken(path, "book", "books") || path.endsWith("_book")) {
            facets.add(ItemFacet.BOOK);
        }
        if ("tconstruct".equals(id.getNamespace()) && path.equals("venombone")) {
            facets.add(ItemFacet.INGREDIENT_ORGANIC);
        }
        if (containsPathToken(path, "tome")) {
            facets.add(ItemFacet.BOOK);
        }
        if (isGuideBookPath(path)) {
            facets.add(ItemFacet.BOOK);
            facets.add(ItemFacet.GUIDE_BOOK);
            attributes.put(SearchNodeKeys.GUIDE_BOOK_CANDIDATE, "true");
        }
        if (path.contains("bucket") || path.contains("saddle") || path.contains("name_tag")
                || path.contains("debug_stick") || path.contains("firework") || path.contains("music_disc")
                || path.contains("disc_fragment") || path.contains("echo_shard")
                || path.contains("banner_pattern")
                || path.contains("smithing_template") || path.contains("trial_key")
                || path.equals("bowl") || path.equals("glass_bottle")
                || path.equals("bundle") || path.equals("shield")
                || path.equals("lead")) {
            facets.add(ItemFacet.UTILITY_MISC);
        }
        if (path.equals("fishing_rod") || path.equals("flint_and_steel")
                || path.equals("brush") || path.equals("shears")
                || path.equals("carrot_on_a_stick") || path.equals("warped_fungus_on_a_stick")) {
            facets.add(ItemFacet.UTILITY_TOOL);
        }
        if (path.equals("mace")) {
            facets.add(ItemFacet.MELEE_WEAPON);
        }
        if (path.equals("elytra")) {
            facets.add(ItemFacet.ARMOR_CHEST);
        }
        if (path.equals("wolf_armor") || path.endsWith("_horse_armor")) {
            facets.add(ItemFacet.ARMOR_ANIMAL);
        }
        if (path.equals("player_head")) {
            facets.add(ItemFacet.SOCIAL_PLAYERS);
        }
        if (path.endsWith("_head") || path.endsWith("_skull")) {
            facets.add(ItemFacet.DECORATIVE_BLOCK);
        }
        if (path.equals("lodestone")) {
            facets.add(ItemFacet.UTILITY_NAVIGATION);
        }
        if (path.contains("mushroom") || path.contains("fungus")) {
            facets.add(ItemFacet.FUNGI);
        }
        if (path.equals("bamboo") || path.equals("stick")) {
            facets.add(ItemFacet.LOG);
        }
        if (path.equals("decorated_pot")) {
            facets.add(ItemFacet.DECORATIVE_BLOCK);
        }
        if (path.equals("bone_meal")) {
            facets.add(ItemFacet.FLOWER);
        }
        if (path.equals("frogspawn")) {
            facets.add(ItemFacet.NATURE_MISC);
        }
        if (path.contains("sculk")) {
            facets.add(ItemFacet.NATURE_MISC);
        }
        if (path.contains("dripstone")) {
            facets.add(ItemFacet.STONE_BLOCK);
        }
        if (path.contains("stew") || path.contains("soup")) {
            facets.add(ItemFacet.FOOD_MEAL);
            facets.add(ItemFacet.FOOD_DRINK);
        }
        if (path.contains("honey_bottle")) {
            facets.add(ItemFacet.FOOD_DRINK);
        }
        if (facets.contains(ItemFacet.EDIBLE) && containsPathToken(path, DRINK_PATH_TOKENS)) {
            facets.add(ItemFacet.FOOD_DRINK);
        }
        if (facets.contains(ItemFacet.EDIBLE) && containsPathToken(path, MEAL_PATH_TOKENS)) {
            facets.add(ItemFacet.FOOD_MEAL);
        }
        if (facets.contains(ItemFacet.EDIBLE) && containsPathToken(path, PROTEIN_PATH_TOKENS)) {
            facets.add(ItemFacet.FOOD_PROTEIN);
        }
        if (path.contains("dye") || path.contains("ink_sac")) {
            facets.add(ItemFacet.INGREDIENT_DYE);
        }
        if (isOrganicIngredientPath(path)) {
            facets.add(ItemFacet.INGREDIENT_ORGANIC);
        }
        if (path.contains("flint") || path.contains("clay_ball")
                || path.equals("prismarine_shard") || path.equals("prismarine_crystals")
                || path.contains("nautilus_shell") || path.contains("heart_of_the_sea")
                || path.contains("pottery_sherd") || path.contains("breeze_rod")
                || path.equals("brick") || path.equals("nether_brick")) {
            facets.add(ItemFacet.INGREDIENT_MINERAL);
        }
        if (path.equals("coal") || path.equals("charcoal")) {
            facets.add(ItemFacet.DUST);
        }
        if (path.equals("netherite_scrap")) {
            facets.add(ItemFacet.RAW_MATERIAL);
        }
        if (path.equals("painting") || path.contains("item_frame")
                || path.equals("armor_stand") || path.contains("banner_pattern")) {
            facets.add(ItemFacet.DECORATIVE_BLOCK);
        }
        if (path.contains("nether_star") || path.contains("totem") || path.equals("end_crystal")) {
            facets.add(ItemFacet.MAGIC_ARTIFACT);
        }
        if (path.contains("blaze_powder") || path.contains("ender_pearl") || path.contains("ender_eye")
                || path.contains("fermented_spider_eye") || path.contains("ghast_tear")
                || path.contains("phantom_membrane") || path.contains("rabbit_foot")
                || path.contains("spider_eye") || path.contains("glistering_melon")
                || path.contains("dragon_breath") || path.contains("experience_bottle")
                || path.contains("magma_cream") || path.contains("nether_wart")
                || path.contains("slime_ball")) {
            facets.add(ItemFacet.MAGIC_REAGENT);
        }
    }

    private static void applyTypeFacts(Item item, String path, EnumSet<ItemFacet> facets) {
        if (item instanceof SwordItem || item instanceof AxeItem) {
            facets.add(ItemFacet.MELEE_WEAPON);
        }
        if (item instanceof BowItem || item instanceof CrossbowItem) {
            facets.add(ItemFacet.RANGED_WEAPON);
        }
        if (Services.PLATFORM.isInstanceOf(item, "net.minecraft.world.item.ProjectileItem") || isProjectilePath(path)) {
            facets.add(ItemFacet.PROJECTILE);
        }
        if (item instanceof DiggerItem || item instanceof HoeItem || item instanceof ShovelItem || item instanceof PickaxeItem) {
            facets.add(ItemFacet.HARVEST_TOOL);
        }
        if (item instanceof FishingRodItem
                || item instanceof ShearsItem
                || item instanceof BrushItem
                || item instanceof FlintAndSteelItem
                || item instanceof FoodOnAStickItem) {
            facets.add(ItemFacet.UTILITY_TOOL);
        }
        if (isNonPlayerArmorItem(item, path)) {
            facets.add(ItemFacet.ARMOR_ANIMAL);
        } else if (item instanceof ArmorItem armorItem) {
            EquipmentSlot slot = armorItem.getEquipmentSlot();
            switch (slot) {
                case HEAD -> facets.add(ItemFacet.ARMOR_HEAD);
                case CHEST -> facets.add(ItemFacet.ARMOR_CHEST);
                case LEGS -> facets.add(ItemFacet.ARMOR_LEGS);
                case FEET -> facets.add(ItemFacet.ARMOR_FEET);
                default -> {
                }
            }
        }
        if (item instanceof PotionItem) {
            facets.add(ItemFacet.POTION);
        }
        if (item instanceof EnchantedBookItem) {
            facets.add(ItemFacet.ENCHANTED_BOOK);
            facets.add(ItemFacet.BOOK);
        }
        if (item instanceof SpawnEggItem) {
            facets.add(ItemFacet.SPAWN_EGG);
        }
        if (item instanceof MobBucketItem) {
            facets.add(ItemFacet.MOB_BUCKET);
        }
        if (path.contains("wand") || path.contains("staff") || path.contains("totem")) {
            facets.add(ItemFacet.MAGIC_ARTIFACT);
        }
    }

    private static void applyEquipmentFacts(Item item, ItemStack stack, String path, EnumSet<ItemFacet> facets, Map<String, String> attributes) {
        Services.PLATFORM.getEquipmentSlotName(stack).ifPresent(slot -> {
            attributes.put(SearchNodeKeys.EQUIPMENT_SLOT, slot);
            facets.add(ItemFacet.EQUIPPABLE);
            if (isNonPlayerArmorItem(item, path)) {
                facets.add(ItemFacet.ARMOR_ANIMAL);
                return;
            }
            switch (slot) {
                case "head" -> facets.add(ItemFacet.ARMOR_HEAD);
                case "chest" -> facets.add(ItemFacet.ARMOR_CHEST);
                case "legs" -> facets.add(ItemFacet.ARMOR_LEGS);
                case "feet" -> facets.add(ItemFacet.ARMOR_FEET);
                case "body" -> {
                    if (isAnimalArmorPath(path)) {
                        facets.add(ItemFacet.ARMOR_ANIMAL);
                    }
                }
                case "offhand" -> facets.add(ItemFacet.UTILITY_MISC);
                default -> {
                }
            }
        });
    }

    private static boolean isAnimalArmorPath(String path) {
        return path.equals("wolf_armor")
                || path.endsWith("_horse_armor")
                || path.endsWith("_hamster_armor")
                || path.endsWith("_dragonfly_armor")
                || path.contains("_animal_armor");
    }

    private static boolean isNonPlayerArmorItem(Item item, String path) {
        if (Services.PLATFORM.isInstanceOf(item, "net.minecraft.world.item.AnimalArmorItem")) {
            return true;
        }
        String itemClass = item.getClass().getName().toLowerCase(Locale.ROOT);
        if (containsAny(itemClass,
                "animalarmoritem",
                "horsearmoritem",
                "wolfarmoritem",
                "dogarmoritem",
                "golemarmoritem",
                "saddleitem")) {
            return true;
        }
        return isAnimalArmorPath(path);
    }

    private static void applyComponentPromotions(Map<String, String> attributes, EnumSet<ItemFacet> facets) {
        if (!hasComponentFact(attributes, "tool")) {
            return;
        }
        if (!hasAny(facets,
                ItemFacet.HARVEST_TOOL,
                ItemFacet.MELEE_WEAPON,
                ItemFacet.RANGED_WEAPON,
                ItemFacet.PROJECTILE,
                ItemFacet.ARMOR_HEAD,
                ItemFacet.ARMOR_CHEST,
                ItemFacet.ARMOR_LEGS,
                ItemFacet.ARMOR_FEET,
                ItemFacet.ARMOR_ANIMAL)) {
            facets.add(ItemFacet.UTILITY_TOOL);
        }
    }

    private static void applyTagFacts(List<String> tags, EnumSet<ItemFacet> facets) {
        for (String tag : tags) {
            if (tag.equals("forge:books/guide") || tag.equals("c:books/guide") || tag.endsWith(":guides")) {
                facets.add(ItemFacet.BOOK);
                facets.add(ItemFacet.GUIDE_BOOK);
            }
            if (isCommonTagFamily(tag, "ingots")) facets.add(ItemFacet.INGOT);
            if (isCommonTagFamily(tag, "gems")) facets.add(ItemFacet.GEM);
            if (isCommonTagFamily(tag, "nuggets")) facets.add(ItemFacet.NUGGET);
            if (isCommonTagFamily(tag, "raw_materials")) facets.add(ItemFacet.RAW_MATERIAL);
            if (isCommonTagFamily(tag, "dusts")) facets.add(ItemFacet.DUST);
            if (isCommonTagFamily(tag, "gears")) {
                facets.add(ItemFacet.MECHANICAL_COMPONENT);
            }
            if (isCommonTagFamily(tag, "plates")
                    || isCommonTagFamily(tag, "rods")
                    || isCommonTagFamily(tag, "gears")
                    || isCannonComponentTag(tag)
                    || tag.contains(":circuits/")
                    || tag.contains("/circuits/")
                    || tag.contains("_circuits/")
                    || tag.endsWith(":circuits")
                    || tag.endsWith("/circuits")
                    || tag.endsWith("_circuits")
                    || tag.endsWith("_circuit")) {
                facets.add(ItemFacet.TECH_COMPONENT);
            }
            if (tag.startsWith("create:crushed_raw_materials")) {
                facets.add(ItemFacet.DUST);
            }
            if (isCommonTagFamily(tag, "wires")
                    || tag.endsWith(":toolbox/wiring")
                    || tag.endsWith("/toolbox/wiring")
                    || tag.endsWith(":spools")
                    || tag.endsWith("/spools")) {
                facets.add(ItemFacet.CABLE);
                facets.add(ItemFacet.TECH_COMPONENT);
            }
            if (tag.endsWith(":power_bottles") || tag.endsWith("/power_bottles")) {
                facets.add(ItemFacet.MAGIC_ARTIFACT);
            }
            if (tag.endsWith(":upgrades")
                    || tag.endsWith("/upgrades")
                    || tag.contains(":upgrades/")
                    || tag.contains("/upgrades/")) {
                facets.add(ItemFacet.UPGRADE);
            }
            if (isAmmoTag(tag)) {
                facets.add(ItemFacet.PROJECTILE);
            }
            if (tag.equals("tconstruct:throwable")) {
                facets.add(ItemFacet.PROJECTILE);
                facets.add(ItemFacet.UTILITY_MISC);
            }
            if (isCommonTagFamily(tag, "dusts/redstone")) {
                facets.add(ItemFacet.ACTIVE_REDSTONE_LOGIC);
                facets.add(ItemFacet.REDSTONE_LOGIC);
                facets.add(ItemFacet.REDSTONE_SIGNAL);
            }
            if (tag.startsWith("c:seeds")) facets.add(ItemFacet.SEED);
            if (tag.startsWith("c:crops")) facets.add(ItemFacet.CROP);
            if (isProduceFoodTag(tag)
                    || (tag.equals("diet:vegetables") && facets.contains(ItemFacet.EDIBLE))) {
                facets.add(ItemFacet.CROP);
            }
            if (tag.startsWith("c:eggs") || tag.startsWith("c:feathers") || tag.startsWith("c:string")
                    || tag.startsWith("c:leathers") || tag.startsWith("c:bones")
                    || tag.equals("spore:body_parts")
                    || tag.equals("spore:inf_parts")) {
                facets.add(ItemFacet.INGREDIENT_ORGANIC);
            }
            if (tag.startsWith("c:foods/drink") || tag.endsWith(":drinks") || tag.endsWith("/drinks")) {
                facets.add(ItemFacet.FOOD_DRINK);
            }
            if (tag.startsWith("c:foods/cooked_meat") || tag.startsWith("c:foods/cooked_fish")
                    || tag.contains(":cooked_meat") || tag.contains(":cooked_fish")
                    || tag.contains("/cooked_meat") || tag.contains("/cooked_fish")
                    || tag.endsWith(":meals") || tag.endsWith("/meals")
                    || tag.equals("forge:mre/main_course")) {
                facets.add(ItemFacet.FOOD_MEAL);
            }
            if (tag.startsWith("c:foods/meat") || tag.startsWith("c:foods/fish")
                    || tag.contains(":raw_meat") || tag.contains(":raw_fish")
                    || tag.contains("/raw_meat") || tag.contains("/raw_fish")
                    || tag.equals("minecraft:is_meat")
                    || (tag.equals("diet:proteins") && facets.contains(ItemFacet.EDIBLE))) {
                facets.add(ItemFacet.FOOD_PROTEIN);
            }
            if (isCurioTag(tag)) {
                facets.add(ItemFacet.CURIO);
            }
            if (tag.endsWith(":melee_weapons") || tag.endsWith("/melee_weapons")) {
                facets.add(ItemFacet.MELEE_WEAPON);
            }
            if (isMeleeToolTag(tag)) {
                facets.add(ItemFacet.MELEE_WEAPON);
            }
            if (isUtilityToolTag(tag)) {
                facets.add(ItemFacet.UTILITY_TOOL);
            }
            if (isHarvestToolTag(tag)) {
                facets.add(ItemFacet.HARVEST_TOOL);
            }
            if (isRangedWeaponTag(tag)) {
                facets.add(ItemFacet.RANGED_WEAPON);
            }
            if (tag.startsWith("c:armors/helmets")) facets.add(ItemFacet.ARMOR_HEAD);
            if (tag.startsWith("c:armors/chestplates")) facets.add(ItemFacet.ARMOR_CHEST);
            if (tag.startsWith("c:armors/leggings")) facets.add(ItemFacet.ARMOR_LEGS);
            if (tag.startsWith("c:armors/boots")) facets.add(ItemFacet.ARMOR_FEET);
            if (tag.equals("minecraft:music_discs")
                    || tag.endsWith(":film_rolls")
                    || tag.endsWith(":developed_film_rolls")
                    || tag.endsWith("/film_rolls")
                    || tag.endsWith("/developed_film_rolls")) {
                facets.add(ItemFacet.UTILITY_MISC);
            }
            if (tag.equals("minecraft:bookshelf_books")
                    || tag.equals("minecraft:lectern_books")) {
                facets.add(ItemFacet.BOOK);
            }
            if (isFurnitureTag(tag)) {
                facets.add(ItemFacet.DECORATIVE_BLOCK);
            }
            if (isCommonTagFamily(tag, "storage_blocks")) {
                if (tag.contains("/raw_")) {
                    facets.add(ItemFacet.RAW_MATERIAL);
                } else if (tag.contains("gem") || tag.contains("diamond") || tag.contains("emerald")
                        || tag.contains("lapis") || tag.contains("quartz") || tag.contains("amethyst")) {
                    facets.add(ItemFacet.GEM);
                } else if (tag.contains("nugget")) {
                    facets.add(ItemFacet.NUGGET);
                } else {
                    facets.add(ItemFacet.INGOT);
                }
            }
        }
    }

    private static boolean isCommonTagFamily(String tag, String familyPath) {
        return tag.equals("c:" + familyPath)
                || tag.startsWith("c:" + familyPath + "/")
                || tag.equals("forge:" + familyPath)
                || tag.startsWith("forge:" + familyPath + "/");
    }

    private static boolean hasComponentFact(Map<String, String> attributes, String expected) {
        String facts = attributes.getOrDefault(SearchNodeKeys.COMPONENT_FACTS, "");
        if (facts.isBlank()) {
            return false;
        }
        for (String fact : facts.split(",")) {
            if (expected.equals(fact.trim())) {
                return true;
            }
        }
        return false;
    }

    private static boolean hasBlockStateProperty(String encoded, String expected) {
        if (encoded == null || encoded.isBlank()) {
            return false;
        }
        for (String property : encoded.split(",")) {
            if (expected.equals(property.trim())) {
                return true;
            }
        }
        return false;
    }

    private static boolean hasAny(EnumSet<ItemFacet> facets, ItemFacet... expected) {
        for (ItemFacet facet : expected) {
            if (facets.contains(facet)) {
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

    private static boolean containsPathToken(String path, String... tokens) {
        String[] pathTokens = path.split("[_/]");
        for (String pathToken : pathTokens) {
            for (String token : tokens) {
                if (pathToken.equals(token)) {
                    return true;
                }
            }
        }
        return false;
    }

    private static boolean containsPathToken(String path, Set<String> tokens) {
        String[] pathTokens = path.split("[_/]");
        for (String pathToken : pathTokens) {
            if (tokens.contains(pathToken)) {
                return true;
            }
        }
        return false;
    }

    private static boolean isProduceFoodTag(String tag) {
        return tag.startsWith("c:foods/vegetable")
                || tag.startsWith("forge:foods/vegetable");
    }

    private static boolean isFurnitureTag(String tag) {
        return tag.endsWith(":bathroom")
                || tag.endsWith("/bathroom")
                || tag.endsWith(":furniture")
                || tag.endsWith("/furniture")
                || tag.endsWith(":furnitures")
                || tag.endsWith("/furnitures");
    }

    private static boolean isCurioTag(String tag) {
        return tag.startsWith("curios:")
                || tag.contains(":trinkets")
                || tag.contains("/trinkets")
                || tag.endsWith(":trinket")
                || tag.endsWith("/trinket");
    }

    private static boolean isMeleeToolTag(String tag) {
        return tag.endsWith(":tool/melee")
                || tag.endsWith("/tool/melee")
                || tag.endsWith(":tools/swords")
                || tag.endsWith("/tools/swords")
                || tag.endsWith(":weapons/swords")
                || tag.endsWith("/weapons/swords");
    }

    private static boolean isUtilityToolTag(String tag) {
        return tag.equals("c:tools")
                || tag.equals("forge:tools")
                || tag.equals("minecraft:tools")
                || isCommonTagFamily(tag, "tools/wrenches")
                || isCommonTagFamily(tag, "tools/wrench")
                || tag.startsWith("c:tools/fishing_rods")
                || tag.startsWith("forge:tools/fishing_rods")
                || tag.endsWith(":tools/hammers")
                || tag.endsWith("/tools/hammers")
                || tag.endsWith(":tools/wirecutters")
                || tag.endsWith("/tools/wirecutters")
                || tag.endsWith(":toolbox/tools")
                || tag.endsWith("/toolbox/tools");
    }

    private static boolean isHarvestToolTag(String tag) {
        return tag.startsWith("c:tools/pickaxes")
                || tag.startsWith("c:tools/axes")
                || tag.startsWith("c:tools/shovels")
                || tag.startsWith("c:tools/hoes")
                || tag.startsWith("c:tools/shears")
                || tag.equals("minecraft:pickaxes")
                || tag.equals("minecraft:axes")
                || tag.equals("minecraft:shovels")
                || tag.equals("minecraft:hoes");
    }

    private static boolean isRangedWeaponTag(String tag) {
        return tag.startsWith("c:tools/bows")
                || tag.startsWith("c:tools/crossbows")
                || tag.startsWith("c:weapons/bows")
                || tag.startsWith("c:weapons/crossbows")
                || tag.equals("minecraft:arrows");
    }

    private static boolean isCannonComponentTag(String tag) {
        return tag.startsWith("createbigcannons:fuzes")
                || tag.startsWith("createbigcannons:spent_autocannon_casings")
                || tag.startsWith("createbigcannons:high_explosive_materials")
                || tag.startsWith("createbigcannons:nitropowder")
                || tag.startsWith("createbigcannons:guncotton");
    }

    private static boolean isAmmoTag(String tag) {
        return tag.startsWith("createbigcannons:autocannon_rounds")
                || tag.startsWith("createbigcannons:autocannon_cartridges");
    }

    private static boolean isProjectilePath(String path) {
        if (path.contains("bulletproof")) {
            return false;
        }
        if (path.contains("crab_shell")
                || path.contains("shulker_shell")
                || path.contains("nautilus_shell")) {
            return false;
        }
        return containsPathToken(path, "arrow", "arrows", "bolt", "bolts", "bullet", "bullets")
                || containsPathToken(path, "round", "ammo", "cartridge", "grenade")
                || isAmmoShellPath(path);
    }

    private static boolean isNaturalShellPath(String path) {
        return path.endsWith("_shell") && !isAmmoShellPath(path);
    }

    private static boolean isAmmoShellPath(String path) {
        return containsPathToken(path, "shell", "shells")
                && containsPathToken(path, "ammo", "cannon", "autocannon", "shotgun", "artillery", "mortar", "munition", "munitions");
    }

    private static boolean isOrganicIngredientPath(String path) {
        return path.equals("paper")
                || path.equals("sugar")
                || path.equals("gunpowder")
                || path.equals("blaze_rod")
                || path.equals("stick")
                || path.equals("shulker_shell")
                || path.equals("popped_chorus_fruit")
                || path.equals("rabbit_hide")
                || path.endsWith("_rabbit_hide")
                || path.endsWith("_honeycomb")
                || path.endsWith("_scute")
                || isNaturalShellPath(path)
                || containsPathToken(path,
                "paper",
                "string",
                "feather",
                "feathers",
                "leather",
                "bone",
                "bones",
                "egg",
                "eggs",
                "honeycomb",
                "scute");
    }

    private static boolean isCablePath(String path) {
        return !isNaturalCableFalsePositivePath(path)
                && (path.contains("cable")
                || path.endsWith("wire")
                || path.contains("_wire")
                || path.contains("wire_")
                || path.contains("wirecoil")
                || containsPathToken(path, "pipe", "tube", "conduit", "duct"))
                && !path.contains("wire_cut");
    }

    private static boolean isMechanicalComponentPath(String path) {
        return containsPathToken(path, "gear", "gears", "gearbox", "cog", "cogs", "cogwheel", "cogwheels", "shaft", "shafts", "belt", "belts");
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

    private static boolean isMedicalPath(String path) {
        return containsPathToken(path, "syringe", "bandage", "medkit", "morphine", "adrenaline", "splint")
                || path.contains("first_aid")
                || path.contains("firstaid");
    }

    private static boolean isCurrencyPath(String path) {
        return path.equals("coin")
                || path.equals("coins")
                || path.contains("coinstack")
                || path.contains("_coin")
                || path.contains("cash")
                || path.contains("money")
                || path.contains("credit_card");
    }

    private static boolean isGuideBookPath(String path) {
        return containsPathToken(path,
                "guidebook",
                "manual",
                "handbook",
                "lexicon",
                "codex",
                "journal",
                "compendium",
                "chronicle")
                || path.equals("guide")
                || path.equals("guides")
                || path.endsWith("_guide")
                || path.endsWith("_guides")
                || path.endsWith("/guide")
                || path.endsWith("/guides")
                || path.endsWith("_guide_book")
                || path.endsWith("_guidebook")
                || path.contains("field_guide");
    }

    private static void applyBlockFacts(
            BlockItem blockItem,
            ItemStack stack,
            String path,
            EnumSet<ItemFacet> facets,
            Map<String, String> attributes
    ) {
        BlockState state = blockItem.getBlock().defaultBlockState();
        facets.add(ItemFacet.PLACEABLE);
        String blockClass = blockItem.getBlock().getClass().getName();
        attributes.put(SearchNodeKeys.BLOCK_CLASS, blockClass);
        String normalizedBlockClass = blockClass.toLowerCase(Locale.ROOT);
        if ("net.minecraft.world.level.block.ConduitBlock".equals(blockClass)) {
            facets.add(ItemFacet.UTILITY_MISC);
        }
        if (containsAny(normalizedBlockClass, "powerbottleblock", "symbolblock", "staffblock")) {
            facets.add(ItemFacet.MAGIC_ARTIFACT);
        }
        if (containsAny(normalizedBlockClass, "crucibleblock", "plinthblock")) {
            facets.add(ItemFacet.WORKSTATION);
            facets.add(ItemFacet.MACHINE);
        }

        String blockTags = state.getTags()
                .map(tag -> tag.location().toString().toLowerCase(Locale.ROOT))
                .sorted()
                .collect(Collectors.joining(","));
        if (!blockTags.isBlank()) {
            attributes.put(SearchNodeKeys.BLOCK_TAGS, blockTags);
        }

        String blockProperties = state.getProperties().stream()
                .map(property -> property.getName().toLowerCase(Locale.ROOT))
                .sorted()
                .collect(Collectors.joining(","));
        if (!blockProperties.isBlank()) {
            attributes.put(SearchNodeKeys.BLOCK_STATE_PROPERTIES, blockProperties);
        }
        if (hasBlockStateProperty(blockProperties, "powered")
                || hasBlockStateProperty(blockProperties, "power")
                || hasBlockStateProperty(blockProperties, "active")
                || hasBlockStateProperty(blockProperties, "charged")
                || hasBlockStateProperty(blockProperties, "enabled")) {
            facets.add(ItemFacet.ACTIVE_REDSTONE_LOGIC);
            facets.add(ItemFacet.REDSTONE_LOGIC);
            facets.add(ItemFacet.REDSTONE_SIGNAL);
        }

        if (!attributes.containsKey("blockShape")) {
            try {
                if (state.isCollisionShapeFullBlock(net.minecraft.world.level.EmptyBlockGetter.INSTANCE, net.minecraft.core.BlockPos.ZERO)) {
                    attributes.put("blockShape", "full_block");
                } else {
                    attributes.put("blockShape", "partial");
                }
            } catch (Throwable ignored) {
                // Shape probing can fail for dynamic modded blocks and must not block indexing.
            }
        }

        if (blockItem.getBlock() instanceof EntityBlock) {
            facets.add(ItemFacet.HAS_BLOCK_ENTITY);
        }
        if (blockItem.getBlock() instanceof MenuProvider) {
            facets.add(ItemFacet.INTERACTIVE_BLOCK);
        }
        if (state.is(BlockTags.BEDS)) {
            facets.add(ItemFacet.DECORATIVE_BLOCK);
        }
        if (isDecorativeNaturePlaceable(path)) {
            facets.add(ItemFacet.NATURE_MISC);
        }
        if (isCropLikePlaceable(path, blockClass)) {
            facets.add(ItemFacet.CROP);
        }
        if (isPlaceableFoodPath(path)) {
            facets.add(ItemFacet.PLACEABLE_FOOD);
        }
        if (isDecorativePlaceable(path)) {
            facets.add(ItemFacet.DECORATIVE_BLOCK);
        }
        if (state.getLightEmission() > 0) {
            facets.add(ItemFacet.LIGHT_SOURCE);
        }
        if (state.isSignalSource()) {
            facets.add(ItemFacet.ACTIVE_REDSTONE_LOGIC);
            facets.add(ItemFacet.REDSTONE_LOGIC);
        }
        if (state.hasAnalogOutputSignal()) {
            facets.add(ItemFacet.REDSTONE_SIGNAL);
            if (!state.isSignalSource()) {
                facets.add(ItemFacet.PASSIVE_COMPARATOR_OUTPUT);
            }
        }
        if (state.is(BlockTags.RAILS)) {
            facets.add(ItemFacet.RAIL);
            facets.add(ItemFacet.TRANSPORT);
            facets.add(ItemFacet.ACTIVE_REDSTONE_LOGIC);
            facets.add(ItemFacet.REDSTONE_LOGIC);
            attributes.put("blockShape", "rail");
        }
        if (state.is(BlockTags.STAIRS)) {
            facets.add(ItemFacet.STAIRS);
            facets.add(ItemFacet.DECORATIVE_BLOCK);
            attributes.put("blockShape", "stairs");
        }
        if (state.is(BlockTags.SLABS)) {
            facets.add(ItemFacet.SLAB);
            facets.add(ItemFacet.DECORATIVE_BLOCK);
            attributes.put("blockShape", "slab");
        }
        if (state.is(BlockTags.WALLS)) {
            facets.add(ItemFacet.WALL);
            facets.add(ItemFacet.DECORATIVE_BLOCK);
            attributes.put("blockShape", "wall");
        }
        if (state.is(BlockTags.FENCES)) {
            facets.add(ItemFacet.FENCE);
            facets.add(ItemFacet.DECORATIVE_BLOCK);
            attributes.put("blockShape", "fence");
        }
        if (state.is(BlockTags.FENCE_GATES)) {
            facets.add(ItemFacet.FENCE_GATE);
            facets.add(ItemFacet.DECORATIVE_BLOCK);
            attributes.put("blockShape", "fence_gate");
        }
        if (state.is(BlockTags.DOORS)) {
            facets.add(ItemFacet.DOOR);
            facets.add(ItemFacet.DECORATIVE_BLOCK);
            attributes.put("blockShape", "door");
        }
        if (state.is(BlockTags.TRAPDOORS)) {
            facets.add(ItemFacet.TRAPDOOR);
            facets.add(ItemFacet.DECORATIVE_BLOCK);
            attributes.put("blockShape", "trapdoor");
        }
        if (state.is(BlockTags.LOGS)) facets.add(ItemFacet.LOG);
        if (state.is(BlockTags.LEAVES)) facets.add(ItemFacet.LEAVES);
        if (state.is(BlockTags.FLOWERS)) facets.add(ItemFacet.FLOWER);
        if (state.is(BlockTags.CROPS)) facets.add(ItemFacet.CROP);
        if (state.is(BlockTags.PLANKS) || state.is(BlockTags.LOGS)) facets.add(ItemFacet.WOOD_BLOCK);
        if (state.is(BlockTags.BASE_STONE_OVERWORLD) || state.is(BlockTags.BASE_STONE_NETHER)) {
            facets.add(ItemFacet.STONE_BLOCK);
        }
        if (state.is(BlockTags.DIRT) || path.contains("sand") || path.contains("gravel")) {
            facets.add(ItemFacet.SOIL_BLOCK);
        }
        if (path.contains("glass") || path.contains("pane")) {
            facets.add(ItemFacet.GLASS_BLOCK);
        }
        if (path.contains("pane")) {
            facets.add(ItemFacet.PANE);
            facets.add(ItemFacet.DECORATIVE_BLOCK);
            attributes.put("blockShape", "pane");
        }
        if (path.contains("redstone") || path.contains("comparator") || path.contains("repeater")
                || path.contains("lever") || path.contains("button") || path.contains("pressure_plate")) {
            facets.add(ItemFacet.ACTIVE_REDSTONE_LOGIC);
            facets.add(ItemFacet.REDSTONE_LOGIC);
            facets.add(ItemFacet.REDSTONE_SIGNAL);
        }
        if (containsPathToken(path, "relay", "transmitter", "receiver", "detector", "trigger")) {
            facets.add(ItemFacet.ACTIVE_REDSTONE_LOGIC);
            facets.add(ItemFacet.REDSTONE_LOGIC);
            facets.add(ItemFacet.REDSTONE_SIGNAL);
        }
        if (path.equals("target") || path.equals("tripwire_hook")
                || path.equals("daylight_detector") || path.equals("lightning_rod")
                || path.equals("note_block") || path.equals("tnt")) {
            facets.add(ItemFacet.ACTIVE_REDSTONE_LOGIC);
            facets.add(ItemFacet.REDSTONE_LOGIC);
            facets.add(ItemFacet.REDSTONE_SIGNAL);
        }
        if (path.contains("machine") || path.contains("generator") || path.contains("factory")) {
            facets.add(ItemFacet.MACHINE);
        }
        if (isWorkstationPath(path)) {
            facets.add(ItemFacet.WORKSTATION);
            facets.add(ItemFacet.MACHINE);
        }
        if (path.equals("honey_block") || path.equals("lectern")
                || path.equals("beehive") || path.equals("bee_nest")
                || path.equals("respawn_anchor")) {
            if (path.equals("lectern")) {
                facets.add(ItemFacet.WORKSTATION);
            }
            facets.add(ItemFacet.MACHINE);
        }
        if (isStoragePath(path)) {
            facets.add(ItemFacet.STORAGE);
        }

        String blocksMaterial = classifyBlockMaterial(state, path);
        if (!blocksMaterial.isBlank()) {
            attributes.put(SearchNodeKeys.BLOCKS_MATERIAL, blocksMaterial);
        }

        String requiredTool = determineRequiredTool(state);
        if (requiredTool != null) {
            attributes.put(SearchNodeKeys.REQUIRED_TOOL, requiredTool);
        }
    }

    private static List<String> collectTags(Item item) {
        List<String> tags = new ArrayList<>();
        item.builtInRegistryHolder().tags().forEach(tag -> tags.add(tag.location().toString().toLowerCase(Locale.ROOT)));
        return tags;
    }

    private static String determineRequiredTool(BlockState state) {
        if (state.is(BlockTags.MINEABLE_WITH_PICKAXE)) {
            if (state.is(BlockTags.NEEDS_DIAMOND_TOOL)) return itemId(Items.DIAMOND_PICKAXE);
            if (state.is(BlockTags.NEEDS_IRON_TOOL)) return itemId(Items.IRON_PICKAXE);
            if (state.is(BlockTags.NEEDS_STONE_TOOL)) return itemId(Items.STONE_PICKAXE);
            return itemId(Items.WOODEN_PICKAXE);
        }
        if (state.is(BlockTags.MINEABLE_WITH_AXE)) {
            if (state.is(BlockTags.NEEDS_DIAMOND_TOOL)) return itemId(Items.DIAMOND_AXE);
            if (state.is(BlockTags.NEEDS_IRON_TOOL)) return itemId(Items.IRON_AXE);
            if (state.is(BlockTags.NEEDS_STONE_TOOL)) return itemId(Items.STONE_AXE);
            return itemId(Items.WOODEN_AXE);
        }
        if (state.is(BlockTags.MINEABLE_WITH_SHOVEL)) {
            if (state.is(BlockTags.NEEDS_DIAMOND_TOOL)) return itemId(Items.DIAMOND_SHOVEL);
            if (state.is(BlockTags.NEEDS_IRON_TOOL)) return itemId(Items.IRON_SHOVEL);
            if (state.is(BlockTags.NEEDS_STONE_TOOL)) return itemId(Items.STONE_SHOVEL);
            return itemId(Items.WOODEN_SHOVEL);
        }
        if (state.is(BlockTags.MINEABLE_WITH_HOE)) {
            if (state.is(BlockTags.NEEDS_DIAMOND_TOOL)) return itemId(Items.DIAMOND_HOE);
            if (state.is(BlockTags.NEEDS_IRON_TOOL)) return itemId(Items.IRON_HOE);
            if (state.is(BlockTags.NEEDS_STONE_TOOL)) return itemId(Items.STONE_HOE);
            return itemId(Items.WOODEN_HOE);
        }
        return null;
    }

    private static boolean isPlaceableFoodPath(String path) {
        return containsPathToken(path,
                "cake",
                "pie",
                "tart",
                "pizza",
                "quiche",
                "cheesecake",
                "flan",
                "cobbler",
                "casserole",
                "lasagna");
    }

    private static String itemId(Item item) {
        ResourceLocation id = net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(item);
        return id != null ? id.toString() : null;
    }

    private static boolean isWorkstationPath(String path) {
        return path.equals("crafting_table")
                || path.equals("stonecutter")
                || path.equals("grindstone")
                || path.equals("loom")
                || path.equals("cartography_table")
                || path.equals("fletching_table")
                || path.equals("smithing_table")
                || path.equals("composter")
                || path.equals("furnace")
                || path.equals("smoker")
                || path.equals("blast_furnace")
                || path.equals("brewing_stand")
                || path.equals("enchanting_table")
                || path.contains("anvil")
                || path.contains("cauldron");
    }

    private static boolean isStoragePath(String path) {
        return path.contains("chest")
                || path.equals("barrel")
                || path.contains("shulker_box")
                || path.equals("hopper")
                || path.equals("dispenser")
                || path.equals("dropper");
    }

    private static boolean isDecorativePlaceable(String path) {
        return path.contains("carpet")
                || path.equals("flower_pot")
                || path.contains("candle")
                || containsPathToken(path, "jar")
                || containsPathToken(path, "banner", "sign", "head", "skull")
                || containsPathToken(path, "bookcase", "bookshelf", "bookshelves", "shelf", "shelves", "rack", "racks");
    }

    private static boolean isDecorativeNaturePlaceable(String path) {
        return path.equals("dead_bush")
                || path.contains("coral")
                || path.equals("cobweb");
    }

    private static boolean isCropLikePlaceable(String path, String blockClass) {
        String className = blockClass.toLowerCase(Locale.ROOT);
        if (path.equals("dead_bush") || className.contains("deadbush")) {
            return false;
        }
        return path.endsWith("_crop")
                || path.contains("_crop_")
                || path.endsWith("_bush")
                || path.contains("_bush_")
                || className.contains("crop")
                || className.contains("bush");
    }

    private static String classifyBlockMaterial(BlockState state, String path) {
        if (path.contains("glass")) return "glass";

        if (state.is(BlockTags.PLANKS) || state.is(BlockTags.LOGS)) {
            return "wood";
        }

        if (state.is(BlockTags.DIRT) || path.contains("sand") || path.contains("gravel")
                || path.contains("terracotta") || path.contains("mud")) {
            return "soil";
        }

        if (state.is(BlockTags.BASE_STONE_OVERWORLD) || state.is(BlockTags.BASE_STONE_NETHER)
                || path.contains("stone") || path.contains("deepslate") || path.contains("brick")
                || path.contains("andesite") || path.contains("diorite") || path.contains("granite")
                || path.contains("tuff") || path.contains("basalt") || path.contains("blackstone")) {
            return "stone";
        }

        return "other_building";
    }
}
