package com.sanhiruzu.ami.index;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.tags.BlockTags;

import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Server-safe facade for constructing AMI's searchable item index from the live registry.
 */
public final class AmiIndexerService {
    private static final AmiIndexerService INSTANCE = new AmiIndexerService();

    private volatile SearchService searchService;
    private volatile int indexedItemCount;

    private AmiIndexerService() {
    }

    public static AmiIndexerService getInstance() {
        return INSTANCE;
    }

    public synchronized SearchService getOrBuildSearchService() {
        GlobalIndex index = GlobalIndex.getInstance();
        if (searchService == null || !index.isIndexReady() || indexedItemCount == 0) {
            rebuild();
        }
        return searchService;
    }

    public synchronized void rebuild() {
        long started = System.nanoTime();
        GlobalIndex index = GlobalIndex.getInstance();
        index.clear();

        for (Item item : BuiltInRegistries.ITEM) {
            ResourceLocation id = BuiltInRegistries.ITEM.getKey(item);
            if (id == null || "air".equals(id.getPath())) {
                continue;
            }

            Map<String, String> meta = new HashMap<>();
            meta.put(SearchNodeKeys.MOD_ID, id.getNamespace());
            meta.put(SearchNodeKeys.VARIANT_GROUP, variantGroup(id));
            meta.put(SearchNodeKeys.COLOR_BUCKET, colorBucket(id));

            String tags = collectTags(item);
            if (!tags.isBlank()) {
                meta.put(SearchNodeKeys.TAGS, tags);
            }

            String requiredTool = requiredTool(item);
            if (requiredTool != null) {
                meta.put(SearchNodeKeys.REQUIRED_TOOL, requiredTool);
            }

            String[] ontology = OntologyClassifier.classifyItem(item, id);
            if (ontology != null) {
                meta.put(SearchNodeKeys.ONTOLOGY_CATEGORY, ontology[0]);
                if (ontology.length > 1) {
                    meta.put(SearchNodeKeys.ONTOLOGY_SUBCATEGORY, ontology[1]);
                }
            }

            index.addNode(new SearchNode(id, NodeType.ITEM, displayName(item), 0xFFFFFF, 0, meta));
        }

        index.markIndexReady();
        index.setIndexBuildTime((System.nanoTime() - started) / 1_000_000L);
        indexedItemCount = index.getNodes(NodeType.ITEM).size();
        searchService = SearchService.buildFrom(index, false);
    }

    public int indexedItemCount() {
        return indexedItemCount;
    }

    private static String displayName(Item item) {
        try {
            return item.getName(new ItemStack(item)).getString();
        } catch (RuntimeException e) {
            ResourceLocation id = BuiltInRegistries.ITEM.getKey(item);
            return id == null ? item.toString() : id.toString();
        }
    }

    private static String collectTags(Item item) {
        return item.builtInRegistryHolder().tags()
                .map(tag -> tag.location().toString().toLowerCase())
                .collect(Collectors.joining(","));
    }

    private static String variantGroup(ResourceLocation id) {
        String path = id.getPath();
        if (path.contains("_stair")) return "stair";
        if (path.contains("_slab")) return "slab";
        if (path.contains("_wall")) return "wall";
        if (path.contains("_door")) return "door";
        if (path.contains("_trapdoor")) return "trapdoor";
        if (path.contains("_fence_gate")) return "fence_gate";
        if (path.contains("_fence")) return "fence";
        if (path.contains("_button")) return "button";
        if (path.contains("_pressure_plate")) return "pressure_plate";
        if (path.contains("_sword")) return "sword";
        if (path.contains("_pickaxe")) return "pickaxe";
        if (path.contains("_axe")) return "axe";
        if (path.contains("_shovel")) return "shovel";
        if (path.contains("_hoe")) return "hoe";
        return "item";
    }

    private static final String[] COLOR_KEYWORDS = {
            "light_blue", "light_gray", "magenta", "orange", "yellow", "purple",
            "white", "black", "brown", "cyan", "green", "lime", "pink", "blue", "gray", "red"
    };

    private static String colorBucket(ResourceLocation id) {
        String path = id.getPath();
        for (String color : COLOR_KEYWORDS) {
            if (hasToken(path, color)) {
                return color;
            }
        }
        return "";
    }

    private static boolean hasToken(String path, String token) {
        int idx = path.indexOf(token);
        while (idx >= 0) {
            boolean beforeOk = idx == 0 || path.charAt(idx - 1) == '_';
            boolean afterOk = idx + token.length() == path.length() || path.charAt(idx + token.length()) == '_';
            if (beforeOk && afterOk) {
                return true;
            }
            idx = path.indexOf(token, idx + 1);
        }
        return false;
    }

    private static String requiredTool(Item item) {
        if (!(item instanceof BlockItem blockItem)) {
            return null;
        }

        BlockState state = blockItem.getBlock().defaultBlockState();
        if (state.is(BlockTags.MINEABLE_WITH_PICKAXE)) {
            if (state.is(BlockTags.NEEDS_DIAMOND_TOOL)) return "minecraft:diamond_pickaxe";
            if (state.is(BlockTags.NEEDS_IRON_TOOL)) return "minecraft:iron_pickaxe";
            if (state.is(BlockTags.NEEDS_STONE_TOOL)) return "minecraft:stone_pickaxe";
            return "minecraft:wooden_pickaxe";
        }
        if (state.is(BlockTags.MINEABLE_WITH_AXE)) {
            if (state.is(BlockTags.NEEDS_DIAMOND_TOOL)) return "minecraft:diamond_axe";
            if (state.is(BlockTags.NEEDS_IRON_TOOL)) return "minecraft:iron_axe";
            if (state.is(BlockTags.NEEDS_STONE_TOOL)) return "minecraft:stone_axe";
            return "minecraft:wooden_axe";
        }
        if (state.is(BlockTags.MINEABLE_WITH_SHOVEL)) {
            if (state.is(BlockTags.NEEDS_DIAMOND_TOOL)) return "minecraft:diamond_shovel";
            if (state.is(BlockTags.NEEDS_IRON_TOOL)) return "minecraft:iron_shovel";
            if (state.is(BlockTags.NEEDS_STONE_TOOL)) return "minecraft:stone_shovel";
            return "minecraft:wooden_shovel";
        }
        if (state.is(BlockTags.MINEABLE_WITH_HOE)) {
            if (state.is(BlockTags.NEEDS_DIAMOND_TOOL)) return "minecraft:diamond_hoe";
            if (state.is(BlockTags.NEEDS_IRON_TOOL)) return "minecraft:iron_hoe";
            if (state.is(BlockTags.NEEDS_STONE_TOOL)) return "minecraft:stone_hoe";
            return "minecraft:wooden_hoe";
        }
        return null;
    }
}
