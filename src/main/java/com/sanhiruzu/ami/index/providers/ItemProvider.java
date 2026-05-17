package com.sanhiruzu.ami.index.providers;

import com.sanhiruzu.ami.AMIConfig;
import com.sanhiruzu.ami.api.AmiPluginRegistry;
import com.sanhiruzu.ami.client.icon.ItemIconRenderer;
import com.sanhiruzu.ami.index.*;
import com.sanhiruzu.ami.index.metrics.DpsMetricSniffer;
import com.sanhiruzu.ami.index.metrics.StorageMetricSniffer;
import com.sanhiruzu.ami.index.sniffers.EnergyCapacitySniffer;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.tags.BlockTags;
import org.jetbrains.annotations.Nullable;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.OptionalDouble;
import java.util.OptionalLong;
import java.util.stream.Collectors;

/**
 * Populates the GlobalIndex with all items from BuiltInRegistries.ITEM.
 * Port of Indexer.java logic.
 */
public class ItemProvider implements IAmiDataProvider {
    private final EnergyCapacitySniffer energyCapacitySniffer = new EnergyCapacitySniffer();

    @Override
    public void populate(GlobalIndex index, @Nullable ClientLevel level) {
        boolean strictSurvival  = AMIConfig.STRICT_SURVIVAL_MODE.get();

        Set<Item> creativeItems = ItemFilter.buildCreativeItemSet(level);
        Set<Item> recipeOutputs = strictSurvival
            ? ItemFilter.buildRecipeOutputSet(level)
            : Collections.emptySet();

        boolean hasCreativeData = !creativeItems.isEmpty();
        boolean hasRecipeData   = !recipeOutputs.isEmpty();

        RegistryAccess registryAccess = level != null ? level.registryAccess() : null;

        for (Item item : BuiltInRegistries.ITEM) {
            ResourceLocation id = BuiltInRegistries.ITEM.getKey(item);
            if (id == null || id.getNamespace().equals("air") || id.getPath().equals("air")) continue;

            // Layer 2: creative-tab membership
            boolean inCreative = !hasCreativeData || creativeItems.contains(item);
            String accessLevel = ItemFilter.classifyAccessLevel(id, inCreative);

            // Layer 3: recipe availability (only evaluated when strictSurvival is on)
            boolean hasRecipe = !hasRecipeData || recipeOutputs.contains(item);

            // Generated subtypes should not be suppressed just because the dummy base item is dev-only.
            List<SubtypeExpander.SubtypeEntry> subtypes =
                    SubtypeExpander.expand(id, registryAccess);
            if (!subtypes.isEmpty()) {
                String tags = collectTags(item);
                for (SubtypeExpander.SubtypeEntry entry : subtypes) {
                    ItemIconRenderer.registerStack(entry.id(), entry.stack());
                    Map<String, String> meta = buildSubtypeMeta(id, extractColorBucket(entry.id()));
                    if (!entry.extraMeta().isEmpty()) meta.putAll(entry.extraMeta());
                    if (!tags.isEmpty()) meta.put(SearchNodeKeys.TAGS, tags);
                    energyCapacitySniffer.sniff(entry.stack()).ifPresent(capacity -> addEnergyCapacity(meta, capacity));
                    index.addNode(new SearchNode(entry.id(), NodeType.ITEM,
                            entry.displayName(), 0xFFFFFF, 0, meta));
                }
                // Skip the plain base node — its subtypes represent the full item space.
                continue;
            }

            if (!ItemFilter.shouldShowAccessLevel(accessLevel)) continue;
            if (!hasRecipe && strictSurvival) continue;

            String modId        = id.getNamespace();
            String displayName  = item.getName(new ItemStack(item)).getString();
            ItemStack defaultStack = new ItemStack(item);
            String variantGroup = GroupingEngine.classifyShape(item);
            String colorBucket  = GroupingEngine.classifyColor(defaultStack);
            String materialGroup = GroupingEngine.classifyMaterialRoot(defaultStack);
            int color           = 0xFFFFFF;
            String tags         = collectTags(item);
            String requiredTool = determineRequiredTool(item);
            OptionalDouble dps = DpsMetricSniffer.estimate(defaultStack);
            OptionalLong esmCapacity = StorageMetricSniffer.estimate(defaultStack, id);
            Optional<Integer> energyCapacity = energyCapacitySniffer.sniff(defaultStack);

            Map<String, String> meta = new HashMap<>();
            meta.put(SearchNodeKeys.MOD_ID, modId);
            meta.put(SearchNodeKeys.VARIANT_GROUP, variantGroup);
            meta.put(SearchNodeKeys.COLOR_BUCKET, colorBucket);
            meta.put(SearchNodeKeys.MATERIAL_GROUP, materialGroup);
            meta.put(SearchNodeKeys.ACCESS_LEVEL, accessLevel);
            if (!tags.isEmpty()) {
                meta.put(SearchNodeKeys.TAGS, tags);
            }
            if (requiredTool != null) {
                meta.put(SearchNodeKeys.REQUIRED_TOOL, requiredTool);
            }
            dps.ifPresent(value -> meta.put(SearchNodeKeys.DPS, formatDps(value)));
            esmCapacity.ifPresent(value -> meta.put(SearchNodeKeys.ESM_CAPACITY, Long.toString(value)));
            energyCapacity.ifPresent(capacity -> addEnergyCapacity(meta, capacity));
            if (!inCreative) {
                meta.put(SearchNodeKeys.VISIBILITY, "hidden");
            }
            if (!hasRecipe) {
                meta.put(SearchNodeKeys.OBTAINABILITY, "no_recipe");
            }

            // Pre-compute ontology category during indexing for accuracy and performance.
            String[] ontology = OntologyClassifier.classifyItem(item, id);
            if (ontology != null) {
                meta.put(SearchNodeKeys.ONTOLOGY_CATEGORY, ontology[0]);
                if (ontology.length > 1) {
                    meta.put(SearchNodeKeys.ONTOLOGY_SUBCATEGORY, ontology[1]);
                }
            }

            index.addNode(new SearchNode(id, NodeType.ITEM, displayName, color, 0, meta));
        }

        // Collect hero items from registered plugins (mods with infinite modular variants).
        indexHeroItems(index, registryAccess);
    }

    private void indexHeroItems(GlobalIndex index, @Nullable RegistryAccess registryAccess) {
        for (var plugin : AmiPluginRegistry.getPlugins()) {
            List<ItemStack> heroItems;
            try {
                heroItems = plugin.getHeroItems();
            } catch (Exception e) {
                com.sanhiruzu.ami.AMI.LOGGER.warn(
                        "IAmiPlugin.getHeroItems() threw from {}", plugin.getClass().getName(), e);
                continue;
            }
            if (heroItems.isEmpty()) continue;

            // Use full class name to avoid collisions between two plugins in the same package.
            String pluginKey = plugin.getClass().getName().replace('.', '_').toLowerCase();
            int count = 0;
            for (ItemStack stack : heroItems) {
                if (stack == null || stack.isEmpty()) continue;
                if (count >= SubtypeExpander.HARD_CAP) {
                    com.sanhiruzu.ami.AMI.LOGGER.warn(
                            "IAmiPlugin.getHeroItems() from {} exceeded HARD_CAP; truncating",
                            plugin.getClass().getName());
                    break;
                }
                ResourceLocation baseId = BuiltInRegistries.ITEM.getKey(stack.getItem());
                if (baseId == null) continue;

                ResourceLocation syntheticId = ResourceLocation.fromNamespaceAndPath("ami",
                        "hero/" + pluginKey + "/" + count);
                ItemIconRenderer.registerStack(syntheticId, stack);

                Map<String, String> meta = buildSubtypeMeta(baseId, extractColorBucket(baseId));
                energyCapacitySniffer.sniff(stack).ifPresent(capacity -> addEnergyCapacity(meta, capacity));
                index.addNode(new SearchNode(syntheticId, NodeType.ITEM,
                        stack.getHoverName().getString(), 0xFFFFFF, 0, meta));
                count++;
            }
        }
    }

    @Nullable
    private String determineRequiredTool(Item item) {
        if (!(item instanceof BlockItem blockItem)) {
            return null;
        }
        BlockState state = blockItem.getBlock().defaultBlockState();

        String req = null;
        if (state.is(BlockTags.MINEABLE_WITH_PICKAXE)) {
            if (state.is(BlockTags.NEEDS_DIAMOND_TOOL)) req = "minecraft:diamond_pickaxe";
            else if (state.is(BlockTags.NEEDS_IRON_TOOL)) req = "minecraft:iron_pickaxe";
            else if (state.is(BlockTags.NEEDS_STONE_TOOL)) req = "minecraft:stone_pickaxe";
            else req = "minecraft:wooden_pickaxe";
        } else if (state.is(BlockTags.MINEABLE_WITH_AXE)) {
            if (state.is(BlockTags.NEEDS_DIAMOND_TOOL)) req = "minecraft:diamond_axe";
            else if (state.is(BlockTags.NEEDS_IRON_TOOL)) req = "minecraft:iron_axe";
            else if (state.is(BlockTags.NEEDS_STONE_TOOL)) req = "minecraft:stone_axe";
            else req = "minecraft:wooden_axe";
        } else if (state.is(BlockTags.MINEABLE_WITH_SHOVEL)) {
            if (state.is(BlockTags.NEEDS_DIAMOND_TOOL)) req = "minecraft:diamond_shovel";
            else if (state.is(BlockTags.NEEDS_IRON_TOOL)) req = "minecraft:iron_shovel";
            else if (state.is(BlockTags.NEEDS_STONE_TOOL)) req = "minecraft:stone_shovel";
            else req = "minecraft:wooden_shovel";
        } else if (state.is(BlockTags.MINEABLE_WITH_HOE)) {
            if (state.is(BlockTags.NEEDS_DIAMOND_TOOL)) req = "minecraft:diamond_hoe";
            else if (state.is(BlockTags.NEEDS_IRON_TOOL)) req = "minecraft:iron_hoe";
            else if (state.is(BlockTags.NEEDS_STONE_TOOL)) req = "minecraft:stone_hoe";
            else req = "minecraft:wooden_hoe";
        }

        return req;
    }

    private String collectTags(Item item) {
        return item.builtInRegistryHolder().tags()
            .map(tag -> tag.location().toString().toLowerCase())
            .collect(Collectors.joining(","));
    }

    private static String formatDps(double value) {
        return String.format(java.util.Locale.ROOT, "%.1f", value);
    }

    private static void addEnergyCapacity(Map<String, String> meta, int capacity) {
        meta.put(SearchNodeKeys.ENERGY_CAPACITY, Integer.toString(capacity));
        meta.merge(SearchNodeKeys.SEARCH_TOKENS, "has_energy", (existing, token) ->
                existing.contains(token) ? existing : existing + " " + token);
    }

    // Color keywords in longest-first order so "light_blue" wins over "blue".
    private static final String[] COLOR_KEYWORDS = {
        "light_blue", "light_gray",
        "magenta", "orange", "yellow", "purple",
        "white", "black", "brown", "cyan", "green",
        "lime", "pink", "blue", "gray", "red"
    };

    private static String extractColorBucket(ResourceLocation id) {
        String path = id.getPath();
        for (String color : COLOR_KEYWORDS) {
            if (pathHasColorToken(path, color)) return color;
        }
        return "";
    }

    private static boolean pathHasColorToken(String path, String color) {
        int idx = path.indexOf(color);
        while (idx >= 0) {
            boolean beforeOk = idx == 0 || path.charAt(idx - 1) == '_';
            boolean afterOk  = idx + color.length() == path.length() || path.charAt(idx + color.length()) == '_';
            if (beforeOk && afterOk) return true;
            idx = path.indexOf(color, idx + 1);
        }
        return false;
    }

    private static Map<String, String> buildSubtypeMeta(ResourceLocation baseId, String colorBucket) {
        Map<String, String> meta = new HashMap<>();
        meta.put(SearchNodeKeys.MOD_ID, baseId.getNamespace());
        meta.put(SearchNodeKeys.SUBTYPE_OF, baseId.toString());
        meta.put(SearchNodeKeys.COLOR_BUCKET, colorBucket);
        meta.put(SearchNodeKeys.MATERIAL_GROUP, baseId.toString());
        meta.put(SearchNodeKeys.ACCESS_LEVEL, ItemFilter.ACCESS_SURVIVAL);
        return meta;
    }
}
