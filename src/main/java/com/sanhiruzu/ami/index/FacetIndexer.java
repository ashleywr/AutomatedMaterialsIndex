package com.sanhiruzu.ami.index;

import net.minecraft.core.component.DataComponents;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.AxeItem;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.BowItem;
import net.minecraft.world.item.CrossbowItem;
import net.minecraft.world.item.DiggerItem;
import net.minecraft.world.item.EnchantedBookItem;
import net.minecraft.world.item.HoeItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.MobBucketItem;
import net.minecraft.world.item.PickaxeItem;
import net.minecraft.world.item.PotionItem;
import net.minecraft.world.item.ProjectileItem;
import net.minecraft.world.item.ShovelItem;
import net.minecraft.world.item.SpawnEggItem;
import net.minecraft.world.item.SwordItem;
import net.minecraft.world.level.block.ComposterBlock;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.state.BlockState;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public final class FacetIndexer {
    private FacetIndexer() {}

    public static FacetProfile index(Item item, ResourceLocation id, ItemStack stack) {
        EnumSet<ItemFacet> facets = EnumSet.noneOf(ItemFacet.class);
        Map<String, String> attributes = new LinkedHashMap<>();
        String path = id.getPath().toLowerCase(Locale.ROOT);
        List<String> tags = collectTags(item);

        if (item.components().has(DataComponents.FOOD)) {
            facets.add(ItemFacet.EDIBLE);
        }
        if (ComposterBlock.COMPOSTABLES.containsKey(item)) {
            facets.add(ItemFacet.COMPOSTABLE);
        }

        applyPathFacts(path, facets);
        applyTypeFacts(item, path, facets);
        applyTagFacts(tags, facets);

        if (item instanceof BlockItem blockItem) {
            applyBlockFacts(blockItem, stack, path, facets, attributes);
        }

        return new FacetProfile(facets, attributes);
    }

    private static void applyPathFacts(String path, EnumSet<ItemFacet> facets) {
        if (path.contains("spell") || path.contains("wand") || path.contains("staff") || path.contains("scroll")) {
            facets.add(ItemFacet.MAGIC_ARTIFACT);
        }
        if (path.contains("rune") || path.contains("essence") || path.contains("shard")) {
            facets.add(ItemFacet.MAGIC_REAGENT);
        }
        if (path.contains("minecart") || path.contains("boat") || path.contains("raft")) {
            facets.add(ItemFacet.TRANSPORT);
        }
        if (path.contains("redstone") || path.contains("comparator") || path.contains("repeater")
                || path.contains("observer") || path.contains("piston") || path.contains("lever")) {
            facets.add(ItemFacet.REDSTONE_LOGIC);
            facets.add(ItemFacet.REDSTONE_SIGNAL);
        }
        if (path.contains("compass") || path.contains("map") || path.contains("clock") || path.contains("spyglass")) {
            facets.add(ItemFacet.UTILITY_NAVIGATION);
        }
        if (path.contains("bucket") || path.contains("saddle") || path.contains("name_tag")
                || path.contains("lead") || path.contains("debug_stick") || path.contains("firework") || path.contains("music_disc")
                || path.contains("disc_fragment") || path.contains("echo_shard")
                || path.contains("book") || path.contains("banner_pattern")
                || path.contains("smithing_template") || path.contains("trial_key")
                || path.equals("bowl") || path.equals("glass_bottle")
                || path.equals("bundle") || path.equals("shield")) {
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
            facets.add(ItemFacet.ARMOR_CHEST);
        }
        if (path.equals("player_head")) {
            facets.add(ItemFacet.SOCIAL_PLAYERS);
        }
        if (path.endsWith("_head") || path.endsWith("_skull")) {
            facets.add(ItemFacet.DECORATIVE_BLOCK);
        }
        if (path.equals("lodestone")) {
            facets.add(ItemFacet.SOCIAL_CLAIMS);
        }
        if (path.contains("mushroom") || path.contains("fungus")) {
            facets.add(ItemFacet.FUNGI);
        }
        if (path.equals("bamboo") || path.equals("stick")) {
            facets.add(ItemFacet.LOG);
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
        if (containsPathToken(path, "beef", "chicken", "porkchop", "cod", "salmon", "rabbit", "mutton", "fish")) {
            facets.add(ItemFacet.FOOD_PROTEIN);
        }
        if (path.contains("dye") || path.contains("ink_sac")) {
            facets.add(ItemFacet.INGREDIENT_DYE);
        }
        if (path.contains("paper") || path.contains("string") || path.contains("feather")
                || path.contains("leather") || path.contains("bone") || path.contains("egg")
                || path.contains("honeycomb") || path.contains("rabbit_hide")
                || path.contains("scute") || path.equals("sugar")
                || path.equals("gunpowder") || path.equals("blaze_rod")
                || path.equals("shulker_shell") || path.equals("popped_chorus_fruit")) {
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
        if (item instanceof ProjectileItem || path.contains("arrow") || path.contains("bolt") || path.contains("bullet")) {
            facets.add(ItemFacet.PROJECTILE);
        }
        if (item instanceof DiggerItem || item instanceof HoeItem || item instanceof ShovelItem || item instanceof PickaxeItem) {
            facets.add(ItemFacet.HARVEST_TOOL);
        }
        if (item instanceof ArmorItem armorItem) {
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

    private static void applyTagFacts(List<String> tags, EnumSet<ItemFacet> facets) {
        for (String tag : tags) {
            if (tag.startsWith("c:ingots")) facets.add(ItemFacet.INGOT);
            if (tag.startsWith("c:gems")) facets.add(ItemFacet.GEM);
            if (tag.startsWith("c:nuggets")) facets.add(ItemFacet.NUGGET);
            if (tag.startsWith("c:raw_materials")) facets.add(ItemFacet.RAW_MATERIAL);
            if (tag.startsWith("c:dusts")) facets.add(ItemFacet.DUST);
            if (tag.startsWith("c:dusts/redstone")) {
                facets.add(ItemFacet.REDSTONE_LOGIC);
                facets.add(ItemFacet.REDSTONE_SIGNAL);
            }
            if (tag.startsWith("c:seeds")) facets.add(ItemFacet.SEED);
            if (tag.startsWith("c:crops")) facets.add(ItemFacet.CROP);
            if (tag.startsWith("c:eggs") || tag.startsWith("c:feathers") || tag.startsWith("c:string")
                    || tag.startsWith("c:leathers") || tag.startsWith("c:bones")) {
                facets.add(ItemFacet.INGREDIENT_ORGANIC);
            }
            if (tag.startsWith("c:foods/drink")) facets.add(ItemFacet.FOOD_DRINK);
            if (tag.startsWith("c:foods/cooked_meat") || tag.startsWith("c:foods/cooked_fish")) {
                facets.add(ItemFacet.FOOD_MEAL);
            }
            if (tag.startsWith("c:foods/meat") || tag.startsWith("c:foods/fish")) {
                facets.add(ItemFacet.FOOD_PROTEIN);
            }
        }
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

    private static void applyBlockFacts(
            BlockItem blockItem,
            ItemStack stack,
            String path,
            EnumSet<ItemFacet> facets,
            Map<String, String> attributes
    ) {
        BlockState state = blockItem.getBlock().defaultBlockState();
        facets.add(ItemFacet.PLACEABLE);

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
            facets.add(ItemFacet.REDSTONE_LOGIC);
        }
        if (state.hasAnalogOutputSignal()) {
            facets.add(ItemFacet.REDSTONE_SIGNAL);
        }
        if (state.is(BlockTags.RAILS)) {
            facets.add(ItemFacet.RAIL);
            facets.add(ItemFacet.TRANSPORT);
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
            facets.add(ItemFacet.REDSTONE_LOGIC);
            facets.add(ItemFacet.REDSTONE_SIGNAL);
        }
        if (containsPathToken(path, "relay", "transmitter", "receiver", "detector", "trigger")) {
            facets.add(ItemFacet.REDSTONE_LOGIC);
            facets.add(ItemFacet.REDSTONE_SIGNAL);
        }
        if (path.equals("target") || path.equals("tripwire_hook")
                || path.equals("daylight_detector") || path.equals("lightning_rod")
                || path.equals("note_block") || path.equals("tnt")) {
            facets.add(ItemFacet.REDSTONE_LOGIC);
            facets.add(ItemFacet.REDSTONE_SIGNAL);
        }
        if (path.contains("machine") || path.contains("generator") || path.contains("factory")) {
            facets.add(ItemFacet.MACHINE);
        }
        if (isWorkstationPath(path)) {
            facets.add(ItemFacet.MACHINE);
        }
        if (path.equals("honey_block") || path.equals("lectern")
                || path.equals("beehive") || path.equals("bee_nest")
                || path.equals("respawn_anchor")) {
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
                || containsPathToken(path, "banner", "sign", "head", "skull");
    }

    private static boolean isDecorativeNaturePlaceable(String path) {
        return path.equals("dead_bush")
                || path.contains("coral")
                || path.equals("cobweb");
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
