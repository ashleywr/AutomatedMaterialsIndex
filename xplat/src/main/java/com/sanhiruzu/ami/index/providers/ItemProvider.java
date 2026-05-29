package com.sanhiruzu.ami.index.providers;

import com.sanhiruzu.ami.AmiCore;
import com.sanhiruzu.ami.api.AmiPluginRegistry;
import com.sanhiruzu.ami.client.icon.ItemIconRenderer;
import com.sanhiruzu.ami.config.AmiConfig;
import com.sanhiruzu.ami.index.*;
import com.sanhiruzu.ami.platform.Services;
import com.sanhiruzu.ami.index.metrics.DpsMetricSniffer;
import com.sanhiruzu.ami.index.metrics.StorageMetricSniffer;
import com.sanhiruzu.ami.index.sniffers.EnergyCapacitySniffer;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.stream.Collectors;

import static com.sanhiruzu.ami.index.providers.RecipeProvider.computeObtainability;
import static com.sanhiruzu.ami.index.providers.RecipeProvider.computeRecipeCategories;

/**
 * Populates the GlobalIndex with all items from BuiltInRegistries.ITEM.
 */
public class ItemProvider implements IAmiDataProvider {
    private final EnergyCapacitySniffer energyCapacitySniffer = new EnergyCapacitySniffer();

    @Override
    public void populate(GlobalIndex index, @Nullable Level level) {
        GroupingEngine.initialize(level);
        GroupingEngine.rebuildDynamicShapeCandidates(BuiltInRegistries.ITEM);
        boolean strictSurvival = AmiConfig.strictSurvivalMode;

        Map<Item, ItemFilter.CreativeTabInfo> creativeTabs = ItemFilter.buildCreativeTabMap(level);
        Set<Item> creativeItems = creativeTabs.keySet();
        AmiRecipeIndex recipeIndex = AmiRecipeIndex.getInstance();
        Set<Item> recipeOutputs = (strictSurvival || AmiConfig.showHiddenModItems)
                ? recipeIndex.getAllOutputItems()
                : Collections.emptySet();

        boolean hasCreativeData = !creativeItems.isEmpty();
        boolean hasRecipeData = !recipeOutputs.isEmpty();

        RegistryAccess registryAccess = level != null ? level.registryAccess() : null;

        for (Item item : BuiltInRegistries.ITEM) {
            ResourceLocation id = BuiltInRegistries.ITEM.getKey(item);
            if (id == null || id.getNamespace().equals("air") || id.getPath().equals("air")) continue;

            // Layer 2: creative-tab membership
            boolean inCreative = !hasCreativeData || creativeItems.contains(item);
            String accessLevel = ItemFilter.classifyAccessLevel(id, inCreative);

            // Layer 3: recipe availability - items with recipes should be shown as SURVIVAL even if not in creative tabs
            boolean hasRecipe = !hasRecipeData || recipeOutputs.contains(item);
            if (hasRecipe && ItemFilter.ACCESS_DEV.equals(accessLevel) && AmiConfig.showHiddenModItems) {
                // Items with recipes that aren't shown in creative tabs should still appear in SURVIVAL mode
                accessLevel = ItemFilter.ACCESS_SURVIVAL;
            }
            // For strict survival mode, only include items with recipes
            if (strictSurvival && !hasRecipe) continue;

            // Generated subtypes should not be suppressed just because the dummy base item is dev-only.
            List<SubtypeExpander.SubtypeEntry> subtypes =
                    SubtypeExpander.expand(id, registryAccess);
            if (!subtypes.isEmpty()) {
                String tags = collectTags(item);
                for (SubtypeExpander.SubtypeEntry entry : subtypes) {
                    ItemIconRenderer.registerStack(entry.id(), entry.stack());
                    Map<String, String> meta = buildSubtypeMeta(id, entry.stack(), extractColorBucket(entry.id()), creativeTabs.get(item));
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

            String modId = id.getNamespace();
            String displayName = item.getName(new ItemStack(item)).getString();
            ItemStack defaultStack = new ItemStack(item);
            String variantGroup = GroupingEngine.classifyShape(item);
            String colorBucket = GroupingEngine.classifyColor(defaultStack);
            String materialGroup = GroupingEngine.classifyMaterialRoot(defaultStack);
            int color = 0xFFFFFF;
            String tags = collectTags(item);
            String requiredTool = determineRequiredTool(item);
            OptionalDouble dps = DpsMetricSniffer.estimate(defaultStack);
            OptionalLong esmCapacity = StorageMetricSniffer.estimate(defaultStack, id);
            Optional<Integer> energyCapacity = energyCapacitySniffer.sniff(defaultStack);
            Optional<GroupingEngine.CollapsedFamily> collapsedFamily = GroupingEngine.classifyCollapsedFamily(id);
            FacetProfile facetProfile = FacetIndexer.index(item, id, defaultStack);
            java.util.EnumSet<ItemFacet> resolvedFacets = facetProfile.facets().isEmpty()
                    ? java.util.EnumSet.noneOf(ItemFacet.class)
                    : java.util.EnumSet.copyOf(facetProfile.facets());
            Map<String, String> facetAttributes = new HashMap<>(facetProfile.attributes());
            if (esmCapacity.isPresent()) {
                resolvedFacets.add(ItemFacet.STORAGE);
            }
            if (energyCapacity.isPresent()) {
                resolvedFacets.add(ItemFacet.HAS_ENERGY);
            }
            facetProfile = new FacetProfile(resolvedFacets, facetAttributes);

            Map<String, String> meta = new HashMap<>();
            meta.put(SearchNodeKeys.MOD_ID, modId);
            meta.put(SearchNodeKeys.VARIANT_GROUP, variantGroup);
            meta.put(SearchNodeKeys.COLOR_BUCKET, colorBucket);
            meta.put(SearchNodeKeys.MATERIAL_GROUP, materialGroup);
            meta.put(SearchNodeKeys.ACCESS_LEVEL, accessLevel);

            if (collapsedFamily.isEmpty() && !materialGroup.isEmpty() && !materialGroup.equals(id.toString())) {
                meta.put(SearchNodeKeys.SUBTYPE_OF, materialGroup);
            }
            applyCreativeTabMeta(meta, creativeTabs.get(item));
            String encodedFacets = FacetCodec.encode(facetProfile.facets());
            if (!encodedFacets.isEmpty()) {
                meta.put(SearchNodeKeys.FACETS, encodedFacets);
            }
            if (!tags.isEmpty()) {
                meta.put(SearchNodeKeys.TAGS, tags);
            }
            if (requiredTool != null) {
                meta.put(SearchNodeKeys.REQUIRED_TOOL, requiredTool);
            }
            collapsedFamily.ifPresent(family -> {
                meta.put(SearchNodeKeys.COLLAPSE_FAMILY, family.key());
                meta.put(SearchNodeKeys.COLLAPSE_LABEL, family.label());
            });
            dps.ifPresent(value -> meta.put(SearchNodeKeys.DPS, formatDps(value)));
            esmCapacity.ifPresent(value -> meta.put(SearchNodeKeys.ESM_CAPACITY, Long.toString(value)));
            energyCapacity.ifPresent(capacity -> addEnergyCapacity(meta, capacity));
            if (!inCreative) {
                meta.put(SearchNodeKeys.VISIBILITY, "hidden");
            }
            String obtainability = computeObtainability(item, recipeIndex);
            meta.put(SearchNodeKeys.OBTAINABILITY, obtainability);
            String recipeCategories = computeRecipeCategories(item, recipeIndex);
            if (!recipeCategories.isEmpty()) {
                meta.put(SearchNodeKeys.RECIPE_CATEGORIES, recipeCategories);
            }

            CategoryAssignment assignment = PrimaryCategoryResolver.resolve(id, facetProfile);
            if (!assignment.attributes().isEmpty()) {
                meta.putAll(assignment.attributes());
            }
            if (!"misc".equals(assignment.categoryId())) {
                meta.put(SearchNodeKeys.ONTOLOGY_CATEGORY, assignment.categoryId());
                meta.put(SearchNodeKeys.ONTOLOGY_SUBCATEGORY, assignment.subcategoryId());
            } else if (shouldUseLegacyOntologyFallback(facetProfile)) {
                // Keep the old classifier as a migration fallback until the facet model
                // fully covers the remaining facetless edge cases.
                String[] ontology = OntologyClassifier.classifyItem(item, id);
                if (ontology != null) {
                    meta.put(SearchNodeKeys.ONTOLOGY_CATEGORY, ontology[0]);
                    if (ontology.length > 1) {
                        meta.put(SearchNodeKeys.ONTOLOGY_SUBCATEGORY, ontology[1]);
                    }
                    if (ontology.length > 2) {
                        meta.put(SearchNodeKeys.BLOCKS_MATERIAL, ontology[2]);
                    }
                }
            }

            index.addNode(new SearchNode(id, NodeType.ITEM, displayName, color, 0, meta));
        }

        // Collect hero items from registered plugins (mods with infinite modular variants).
        indexHeroItems(index, registryAccess, creativeTabs);
    }

    private void indexHeroItems(GlobalIndex index, @Nullable RegistryAccess registryAccess,
                                Map<Item, ItemFilter.CreativeTabInfo> creativeTabs) {
        for (var plugin : AmiPluginRegistry.getPlugins()) {
            List<ItemStack> heroItems;
            try {
                heroItems = plugin.getHeroItems();
            } catch (Exception e) {
                AmiCore.LOGGER.warn(
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
                    AmiCore.LOGGER.warn(
                            "IAmiPlugin.getHeroItems() from {} exceeded HARD_CAP; truncating",
                            plugin.getClass().getName());
                    break;
                }
                ResourceLocation baseId = BuiltInRegistries.ITEM.getKey(stack.getItem());
                if (baseId == null) continue;

                ResourceLocation syntheticId = Services.PLATFORM.rl("ami", "hero/" + pluginKey + "/" + count);
                ItemIconRenderer.registerStack(syntheticId, stack);

                Map<String, String> meta = buildSubtypeMeta(baseId, stack, extractColorBucket(baseId), creativeTabs.get(stack.getItem()));
                energyCapacitySniffer.sniff(stack).ifPresent(capacity -> addEnergyCapacity(meta, capacity));
                index.addNode(new SearchNode(syntheticId, NodeType.ITEM,
                        stack.getHoverName().getString(), 0xFFFFFF, 0, meta));
                count++;
            }
        }
    }

    private static boolean shouldUseLegacyOntologyFallback(FacetProfile facetProfile) {
        return facetProfile.facets().isEmpty();
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
        @SuppressWarnings("deprecation")
        var holder = item.builtInRegistryHolder();
        return holder.tags()
                .map(tag -> tag.location().toString().toLowerCase())
                .collect(Collectors.joining(","));
    }

    private static String formatDps(double value) {
        return String.format(java.util.Locale.ROOT, "%.1f", value);
    }

    private static void addEnergyCapacity(Map<String, String> meta, int capacity) {
        meta.put(SearchNodeKeys.ENERGY_CAPACITY, Integer.toString(capacity));
        addSearchToken(meta, "has_energy");
    }

    private static void addSearchToken(Map<String, String> meta, String token) {
        meta.merge(SearchNodeKeys.SEARCH_TOKENS, token, (existing, added) ->
                existing.contains(added) ? existing : existing + " " + added);
    }

    private static String extractColorBucket(ResourceLocation id) {
        return GroupingEngine.classifyColorFromPath(id.getPath());
    }

    private static Map<String, String> buildSubtypeMeta(ResourceLocation baseId, ItemStack stack, String colorBucket,
                                                        @Nullable ItemFilter.CreativeTabInfo creativeTab) {
        Map<String, String> meta = new HashMap<>();
        meta.put(SearchNodeKeys.MOD_ID, baseId.getNamespace());
        meta.put(SearchNodeKeys.SUBTYPE_OF, baseId.toString());
        meta.put(SearchNodeKeys.COLOR_BUCKET, colorBucket);
        meta.put(SearchNodeKeys.MATERIAL_GROUP, baseId.toString());
        meta.put(SearchNodeKeys.ACCESS_LEVEL, ItemFilter.ACCESS_SURVIVAL);
        applyCreativeTabMeta(meta, creativeTab);
        GroupingEngine.classifyCollapsedFamily(baseId).ifPresent(family -> {
            meta.put(SearchNodeKeys.COLLAPSE_FAMILY, family.key());
            meta.put(SearchNodeKeys.COLLAPSE_LABEL, family.label());
        });

        Item item = stack.getItem();
        FacetProfile facetProfile = FacetIndexer.index(item, baseId, stack);
        if (item != null && item != net.minecraft.world.item.Items.ENCHANTED_BOOK
                && stack.isEnchanted()) {
            // Only explicit subtype / hero stacks should surface as pre-enchanted variants.
            addSearchToken(meta, "enchanted");
            addSearchToken(meta, "pre_enchanted");
        }
        String encodedFacets = FacetCodec.encode(facetProfile.facets());
        if (!encodedFacets.isEmpty()) {
            meta.put(SearchNodeKeys.FACETS, encodedFacets);
        }
        if (!facetProfile.attributes().isEmpty()) {
            meta.putAll(facetProfile.attributes());
        }

        CategoryAssignment assignment = PrimaryCategoryResolver.resolve(baseId, facetProfile);
        if (!assignment.attributes().isEmpty()) {
            meta.putAll(assignment.attributes());
        }
        if (!"misc".equals(assignment.categoryId())) {
            meta.put(SearchNodeKeys.ONTOLOGY_CATEGORY, assignment.categoryId());
            meta.put(SearchNodeKeys.ONTOLOGY_SUBCATEGORY, assignment.subcategoryId());
        } else if (shouldUseLegacyOntologyFallback(facetProfile)) {
            String[] ontology = OntologyClassifier.classifyItem(item, baseId);
            if (ontology != null) {
                meta.put(SearchNodeKeys.ONTOLOGY_CATEGORY, ontology[0]);
                if (ontology.length > 1) {
                    meta.put(SearchNodeKeys.ONTOLOGY_SUBCATEGORY, ontology[1]);
                }
                if (ontology.length > 2) {
                    meta.put(SearchNodeKeys.BLOCKS_MATERIAL, ontology[2]);
                }
            }
        }
        return meta;
    }

    private static void applyCreativeTabMeta(Map<String, String> meta, @Nullable ItemFilter.CreativeTabInfo creativeTab) {
        if (creativeTab == null) {
            return;
        }
        meta.put(SearchNodeKeys.CREATIVE_TAB_ID, creativeTab.id());
        meta.put(SearchNodeKeys.CREATIVE_TAB_LABEL, creativeTab.label());
    }
}
