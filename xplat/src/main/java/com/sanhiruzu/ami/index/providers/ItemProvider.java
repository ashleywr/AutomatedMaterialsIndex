package com.sanhiruzu.ami.index.providers;

import com.sanhiruzu.ami.AmiCore;
import com.sanhiruzu.ami.api.AmiPluginRegistry;
import com.sanhiruzu.ami.client.icon.ItemIconRenderer;
import com.sanhiruzu.ami.compat.AE2Compat;
import com.sanhiruzu.ami.compat.CobblemonCompat;
import com.sanhiruzu.ami.compat.CompatFamilyDetector;
import com.sanhiruzu.ami.compat.CreateCompat;
import com.sanhiruzu.ami.compat.MekanismCompat;
import com.sanhiruzu.ami.compat.SophisticatedCompat;
import com.sanhiruzu.ami.config.AmiConfig;
import com.sanhiruzu.ami.index.*;
import com.sanhiruzu.ami.index.metrics.*;
import com.sanhiruzu.ami.index.sniffers.*;
import com.sanhiruzu.ami.platform.Services;
import com.sanhiruzu.ami.recipe.AmiRecipeIndex;
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

import static com.sanhiruzu.ami.index.providers.RecipeProvider.*;

/**
 * Populates the GlobalIndex with all items from BuiltInRegistries.ITEM.
 */
public class ItemProvider implements IAmiDataProvider {
    private final PowerMetricSniffer powerMetricSniffer = new PowerMetricSniffer();
    private final FoodMetricSniffer foodMetricSniffer = new FoodMetricSniffer();
    private final FluidMetricSniffer fluidMetricSniffer = new FluidMetricSniffer();
    private final ToolMetricSniffer toolMetricSniffer = new ToolMetricSniffer();
    private final ArmorMetricSniffer armorMetricSniffer = new ArmorMetricSniffer();

    private static boolean shouldUseLegacyOntologyFallback(FacetProfile facetProfile) {
        return facetProfile.facets().isEmpty();
    }

    private static String formatDps(double value) {
        return String.format(java.util.Locale.ROOT, "%.1f", value);
    }

    private static String formatMetric(double value) {
        if (Math.abs(value - Math.rint(value)) < 0.0001D) {
            return Long.toString(Math.round(value));
        }
        return String.format(java.util.Locale.ROOT, "%.1f", value);
    }

    private static void addPowerStats(Map<String, String> meta, PowerStats stats) {
        if (stats.hasCapacity()) {
            meta.put(SearchNodeKeys.ENERGY_CAPACITY, Integer.toString(stats.capacityFe()));
        }
        if (stats.hasGeneration()) {
            meta.put(SearchNodeKeys.ENERGY_GENERATION, formatMetric(stats.generationFePerTick()));
        }
        if (stats.hasConsumption()) {
            meta.put(SearchNodeKeys.ENERGY_CONSUMPTION, formatMetric(stats.consumptionFePerTick()));
        }
        if (!stats.source().isBlank()) {
            meta.put(SearchNodeKeys.ENERGY_METRIC_SOURCE, stats.source());
        }
        addFacet(meta, ItemFacet.HAS_ENERGY);
        addSearchToken(meta, "has_energy");
        if (stats.hasGeneration()) {
            addSearchToken(meta, "fe_generation");
        }
        if (stats.hasConsumption()) {
            addSearchToken(meta, "fe_consumption");
        }
    }

    private static void addFoodStats(Map<String, String> meta, FoodStats stats) {
        meta.put(SearchNodeKeys.FOOD_NUTRITION, Integer.toString(stats.nutrition()));
        meta.put(SearchNodeKeys.FOOD_SATURATION, formatDps(stats.saturation()));
        addSearchToken(meta, "food_stats");
    }

    private static void addFluidStats(Map<String, String> meta, FluidStats stats) {
        if (!stats.hasAny()) return;
        meta.put(SearchNodeKeys.FLUID_CAPACITY, formatMetric(stats.buckets()));
        if (!stats.source().isBlank()) {
            meta.put(SearchNodeKeys.FLUID_METRIC_SOURCE, stats.source());
        }
        addFacet(meta, ItemFacet.FLUID_CONTAINER);
        addSearchToken(meta, "fluid_container");
    }

    private static void addToolStats(Map<String, String> meta, ToolStats stats) {
        if (!stats.hasAny()) return;
        if (stats.speed() > 0.0D) {
            meta.put(SearchNodeKeys.TOOL_SPEED, formatMetric(stats.speed()));
        }
        if (stats.uses() > 0) {
            meta.put(SearchNodeKeys.TOOL_USES, Integer.toString(stats.uses()));
        }
        if (stats.attackBonus() > 0.0D) {
            meta.put(SearchNodeKeys.TOOL_ATTACK_BONUS, formatMetric(stats.attackBonus()));
        }
    }

    private static void addArmorStats(Map<String, String> meta, ArmorStats stats) {
        if (!stats.hasAny()) return;
        if (stats.defense() > 0) {
            meta.put(SearchNodeKeys.ARMOR_DEFENSE, Integer.toString(stats.defense()));
        }
        if (stats.toughness() > 0.0D) {
            meta.put(SearchNodeKeys.ARMOR_TOUGHNESS, formatMetric(stats.toughness()));
        }
    }

    private static void addDurability(Map<String, String> meta, ItemStack stack) {
        int maxDamage = stack.getMaxDamage();
        if (maxDamage <= 0) return;
        meta.put(SearchNodeKeys.MAX_DURABILITY, Integer.toString(maxDamage));
    }

    private static void addSearchToken(Map<String, String> meta, String token) {
        meta.merge(SearchNodeKeys.SEARCH_TOKENS, token, (existing, added) ->
                existing.contains(added) ? existing : existing + " " + added);
    }

    private static void addFacet(Map<String, String> meta, ItemFacet facet) {
        String encoded = meta.getOrDefault(SearchNodeKeys.FACETS, "");
        if (encoded.isBlank()) {
            meta.put(SearchNodeKeys.FACETS, facet.id());
            return;
        }
        for (String part : encoded.split(",")) {
            if (facet.id().equals(part.trim())) {
                return;
            }
        }
        meta.put(SearchNodeKeys.FACETS, encoded + "," + facet.id());
    }

    private static FacetProfile profileWithMetadata(FacetProfile profile, Map<String, String> meta) {
        Map<String, String> attributes = new HashMap<>(profile.attributes());
        attributes.putAll(meta);
        return new FacetProfile(profile.facets(), attributes);
    }

    private static void inferAmmoType(ResourceLocation id, Map<String, String> meta) {
        if (meta.containsKey(SearchNodeKeys.AMMO_TYPE)) {
            return;
        }
        String path = id.getPath().toLowerCase(java.util.Locale.ROOT);
        if (path.contains("gunpowder") || path.contains("bulletproof")) {
            return;
        }
        Set<String> pathTokens = pathTokens(path);
        String facets = meta.getOrDefault(SearchNodeKeys.FACETS, "");

        String ammoType = null;
        if (pathTokens.contains("bullet") || pathTokens.contains("bullets") || pathTokens.contains("ammo") || path.contains("cartridge")) {
            ammoType = "bullets";
        } else if (pathTokens.contains("shell") || pathTokens.contains("shells")) {
            ammoType = "shells";
        } else if (pathTokens.contains("rocket") || pathTokens.contains("rockets") || path.contains("missile")) {
            ammoType = "rockets";
        } else if (pathTokens.contains("bolt") || pathTokens.contains("bolts")) {
            ammoType = "bolts";
        } else if (pathTokens.contains("arrow") || pathTokens.contains("arrows") || path.contains("bow")) {
            ammoType = "arrows";
        } else if (path.contains("gun") || path.contains("rifle") || path.contains("pistol") || path.contains("shotgun")) {
            ammoType = "bullets";
        } else if (path.contains("cannon") || path.contains("launcher")) {
            ammoType = "shells";
        }

        if (ammoType != null && (facets.contains("ranged_weapon") || facets.contains("projectile")
                || path.contains("gun") || path.contains("rifle") || path.contains("pistol") || path.contains("cannon"))) {
            meta.put(SearchNodeKeys.AMMO_TYPE, ammoType);
            addSearchToken(meta, ammoType);
        }
    }

    private static Set<String> pathTokens(String path) {
        return new HashSet<>(Arrays.asList(path.split("[_/\\-]")));
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
        addDurability(meta, stack);

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

        CompatFamilyDetector.detect(baseId, meta);
        CobblemonCompat.enrichItem(baseId, meta);
        CreateCompat.enrichItem(baseId, meta);
        AE2Compat.enrichItem(baseId, meta);
        MekanismCompat.enrichItem(baseId, meta);
        SophisticatedCompat.enrichItem(baseId, meta);
        CategoryAssignment assignment = PrimaryCategoryResolver.resolve(baseId, profileWithMetadata(facetProfile, meta));
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

    @Override
    public void populate(GlobalIndex index, @Nullable Level level) {
        GroupingEngine.initialize(level);
        GroupingEngine.rebuildDynamicShapeCandidates(BuiltInRegistries.ITEM);
        boolean strictSurvival = AmiConfig.strictSurvivalMode;

        Map<Item, List<ItemFilter.CreativeStackInfo>> creativeStackMap = ItemFilter.buildCreativeStackMap(level);
        Map<Item, ItemFilter.CreativeTabInfo> creativeTabs = ItemFilter.firstCreativeTabs(creativeStackMap);
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
            String accessLevel = ItemFilter.classifyAccessLevel(id, item, inCreative);

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
            if (subtypes.isEmpty() && ItemFilter.shouldShowAccessLevel(accessLevel)) {
                subtypes = CreativeStackVariantExpander.expand(id, creativeStackMap.get(item), level);
            }
            if (!subtypes.isEmpty()) {
                String tags = collectTags(item);
                for (SubtypeExpander.SubtypeEntry entry : subtypes) {
                    ItemIconRenderer.registerStack(entry.id(), entry.stack());
                    String colorBucket = entry.extraMeta().getOrDefault(
                            SearchNodeKeys.COLOR_BUCKET,
                            extractColorBucket(entry.id())
                    );
                    Map<String, String> meta = buildSubtypeMeta(id, entry.stack(), colorBucket, creativeTabs.get(item));
                    if (!entry.extraMeta().isEmpty()) meta.putAll(entry.extraMeta());
                    if (!tags.isEmpty()) meta.put(SearchNodeKeys.TAGS, tags);
                    foodMetricSniffer.sniff(entry.stack()).ifPresent(stats -> addFoodStats(meta, stats));
                    powerMetricSniffer.sniff(entry.stack(), entry.id(), level).ifPresent(stats -> addPowerStats(meta, stats));
                    fluidMetricSniffer.sniff(entry.stack(), entry.id(), level).ifPresent(stats -> addFluidStats(meta, stats));
                    toolMetricSniffer.sniff(entry.stack()).ifPresent(stats -> addToolStats(meta, stats));
                    armorMetricSniffer.sniff(entry.stack()).ifPresent(stats -> addArmorStats(meta, stats));
                    inferAmmoType(entry.id(), meta);
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
            OptionalDouble attackDamage = DpsMetricSniffer.estimateDamage(defaultStack);
            OptionalDouble dps = DpsMetricSniffer.estimate(defaultStack);
            OptionalLong esmCapacity = StorageMetricSniffer.estimate(defaultStack, id);
            Optional<PowerStats> powerStats = powerMetricSniffer.sniff(defaultStack, id, level);
            Optional<FoodStats> foodStats = foodMetricSniffer.sniff(defaultStack);
            Optional<FluidStats> fluidStats = fluidMetricSniffer.sniff(defaultStack, id, level);
            Optional<ToolStats> toolStats = toolMetricSniffer.sniff(defaultStack);
            Optional<ArmorStats> armorStats = armorMetricSniffer.sniff(defaultStack);
            Optional<GroupingEngine.CollapsedFamily> collapsedFamily = GroupingEngine.classifyCollapsedFamily(id);
            FacetProfile facetProfile = FacetIndexer.index(item, id, defaultStack);
            java.util.EnumSet<ItemFacet> resolvedFacets = facetProfile.facets().isEmpty()
                    ? java.util.EnumSet.noneOf(ItemFacet.class)
                    : java.util.EnumSet.copyOf(facetProfile.facets());
            Map<String, String> facetAttributes = new HashMap<>(facetProfile.attributes());
            if (esmCapacity.isPresent()) {
                resolvedFacets.add(ItemFacet.STORAGE);
            }
            if (powerStats.map(PowerStats::hasAny).orElse(false)) {
                resolvedFacets.add(ItemFacet.HAS_ENERGY);
            }
            if (fluidStats.map(FluidStats::hasAny).orElse(false)) {
                resolvedFacets.add(ItemFacet.FLUID_CONTAINER);
            }
            facetProfile = new FacetProfile(resolvedFacets, facetAttributes);

            Map<String, String> meta = new HashMap<>();
            meta.put(SearchNodeKeys.MOD_ID, modId);
            meta.put(SearchNodeKeys.VARIANT_GROUP, variantGroup);
            meta.put(SearchNodeKeys.COLOR_BUCKET, colorBucket);
            meta.put(SearchNodeKeys.MATERIAL_GROUP, materialGroup);
            meta.put(SearchNodeKeys.ACCESS_LEVEL, accessLevel);
            if (!facetProfile.attributes().isEmpty()) {
                meta.putAll(facetProfile.attributes());
            }

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
            addDurability(meta, defaultStack);
            attackDamage.ifPresent(value -> meta.put(SearchNodeKeys.ATTACK_DAMAGE, formatDps(value)));
            dps.ifPresent(value -> meta.put(SearchNodeKeys.DPS, formatDps(value)));
            esmCapacity.ifPresent(value -> meta.put(SearchNodeKeys.ESM_CAPACITY, Long.toString(value)));
            powerStats.ifPresent(stats -> addPowerStats(meta, stats));
            foodStats.ifPresent(stats -> addFoodStats(meta, stats));
            fluidStats.ifPresent(stats -> addFluidStats(meta, stats));
            toolStats.ifPresent(stats -> addToolStats(meta, stats));
            armorStats.ifPresent(stats -> addArmorStats(meta, stats));
            inferAmmoType(id, meta);
            CompatFamilyDetector.detect(id, meta);
            CobblemonCompat.enrichItem(id, meta);
            if (!inCreative) {
                meta.put(SearchNodeKeys.VISIBILITY, "hidden");
            }
            String obtainability = computeObtainability(item, recipeIndex);
            meta.put(SearchNodeKeys.OBTAINABILITY, obtainability);
            String recipeCategories = computeRecipeCategories(item, recipeIndex);
            if (!recipeCategories.isEmpty()) {
                meta.put(SearchNodeKeys.RECIPE_CATEGORIES, recipeCategories);
            }
            String recipeUseCategories = computeRecipeUseCategories(item, recipeIndex);
            if (!recipeUseCategories.isEmpty()) {
                meta.put(SearchNodeKeys.RECIPE_USE_CATEGORIES, recipeUseCategories);
            }
            int recipeOutputCount = computeRecipeOutputCount(item, recipeIndex);
            if (recipeOutputCount > 0) {
                meta.put(SearchNodeKeys.RECIPE_OUTPUT_COUNT, Integer.toString(recipeOutputCount));
            }
            int recipeUseCount = computeRecipeUseCount(item, recipeIndex);
            if (recipeUseCount > 0) {
                meta.put(SearchNodeKeys.RECIPE_USE_COUNT, Integer.toString(recipeUseCount));
            }
            CreateCompat.enrichItem(id, meta);
            AE2Compat.enrichItem(id, meta);
            MekanismCompat.enrichItem(id, meta);
            SophisticatedCompat.enrichItem(id, meta);

            CategoryAssignment assignment = PrimaryCategoryResolver.resolve(id, profileWithMetadata(facetProfile, meta));
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
        indexHeroItems(index, registryAccess, creativeTabs, level);
    }

    private void indexHeroItems(GlobalIndex index, @Nullable RegistryAccess registryAccess,
                                Map<Item, ItemFilter.CreativeTabInfo> creativeTabs,
                                @Nullable Level level) {
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
                foodMetricSniffer.sniff(stack).ifPresent(stats -> addFoodStats(meta, stats));
                powerMetricSniffer.sniff(stack, syntheticId, level).ifPresent(stats -> addPowerStats(meta, stats));
                fluidMetricSniffer.sniff(stack, syntheticId, level).ifPresent(stats -> addFluidStats(meta, stats));
                toolMetricSniffer.sniff(stack).ifPresent(stats -> addToolStats(meta, stats));
                armorMetricSniffer.sniff(stack).ifPresent(stats -> addArmorStats(meta, stats));
                inferAmmoType(baseId, meta);
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
        @SuppressWarnings("deprecation")
        var holder = item.builtInRegistryHolder();
        return holder.tags()
                .map(tag -> tag.location().toString().toLowerCase())
                .collect(Collectors.joining(","));
    }
}
