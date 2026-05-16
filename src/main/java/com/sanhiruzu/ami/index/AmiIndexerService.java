package com.sanhiruzu.ami.index;

import com.sanhiruzu.ami.index.metrics.DpsMetricSniffer;
import com.sanhiruzu.ami.index.metrics.StorageMetricSniffer;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.tags.BlockTags;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.OptionalDouble;
import java.util.OptionalLong;
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
            String accessLevel = ItemFilter.classifyAccessLevel(id, true);
            if (!ItemFilter.shouldShowAccessLevel(accessLevel)) {
                continue;
            }

            Map<String, String> meta = new HashMap<>();
            ItemStack stack = new ItemStack(item);
            OptionalDouble dps = DpsMetricSniffer.estimate(stack);
            OptionalLong esmCapacity = StorageMetricSniffer.estimate(stack, id);
            meta.put(SearchNodeKeys.MOD_ID, id.getNamespace());
            meta.put(SearchNodeKeys.VARIANT_GROUP, GroupingEngine.classifyShape(item));
            meta.put(SearchNodeKeys.COLOR_BUCKET, GroupingEngine.classifyColor(stack));
            meta.put(SearchNodeKeys.MATERIAL_GROUP, GroupingEngine.classifyMaterialRoot(stack));
            meta.put(SearchNodeKeys.ACCESS_LEVEL, accessLevel);
            dps.ifPresent(value -> meta.put(SearchNodeKeys.DPS, formatDps(value)));
            esmCapacity.ifPresent(value -> meta.put(SearchNodeKeys.ESM_CAPACITY, Long.toString(value)));

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

    private static String formatDps(double value) {
        return String.format(Locale.ROOT, "%.1f", value);
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
