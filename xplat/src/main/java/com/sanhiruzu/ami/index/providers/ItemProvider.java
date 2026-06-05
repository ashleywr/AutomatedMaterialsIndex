package com.sanhiruzu.ami.index.providers;

import com.sanhiruzu.ami.AmiCore;
import com.sanhiruzu.ami.api.AmiPluginRegistry;
import com.sanhiruzu.ami.api.ItemProviderCompatHooks;
import com.sanhiruzu.ami.client.icon.ItemIconRenderer;
import com.sanhiruzu.ami.compat.AE2Compat;
import com.sanhiruzu.ami.compat.ChippedCompat;
import com.sanhiruzu.ami.compat.CobblemonCompat;
import com.sanhiruzu.ami.compat.CompatFamilyDetector;
import com.sanhiruzu.ami.compat.CreateCompat;
import com.sanhiruzu.ami.compat.GregTechCompat;
import com.sanhiruzu.ami.compat.MekanismCompat;
import com.sanhiruzu.ami.compat.ModularGearCompat;
import com.sanhiruzu.ami.compat.ModularGolemsCompat;
import com.sanhiruzu.ami.compat.RechiseledCompat;
import com.sanhiruzu.ami.compat.SophisticatedCompat;
import com.sanhiruzu.ami.compat.StorageCompat;
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

import java.text.Normalizer;
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
    private static final Set<String> PLAIN_SEARCH_STOP_WORDS = Set.of("and", "for", "mod", "the", "with");

    private enum ItemIndexPass {
        PRIMARY("primary"),
        DEFERRED("deferred");

        final String label;

        ItemIndexPass(String label) {
            this.label = label;
        }
    }

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

    private static void addPlainSearchToken(Map<String, String> meta, String token) {
        if (token == null || token.isBlank()) {
            return;
        }
        meta.merge(SearchNodeKeys.PLAIN_SEARCH_TOKENS, token, (existing, added) ->
                containsToken(existing, added) ? existing : existing + " " + added);
    }

    private static void addCheapPlainSearchTokens(Map<String, String> meta, ResourceLocation id,
                                                  Map<String, String> modNameCache) {
        if (id == null) {
            return;
        }
        LinkedHashSet<String> tokens = new LinkedHashSet<>();
        String modName = modNameCache.computeIfAbsent(id.getNamespace(), namespace ->
                Services.PLATFORM.getModName(namespace).orElse(namespace));
        collectPlainSearchTokens(tokens, modName);
        collectPlainSearchTokens(tokens, meta.get(SearchNodeKeys.PRIMARY_COMPAT_FAMILY));
        collectPlainSearchTokens(tokens, meta.get(SearchNodeKeys.COMPAT_FAMILY));
        collectPlainSearchTokens(tokens, meta.get(SearchNodeKeys.COMPAT_FAMILIES));

        if (isCobblemonFamily(id, meta, modName)) {
            tokens.add("pokemon");
            tokens.add("poke");
            tokens.add("cobblemon");
        }

        for (String token : tokens) {
            addPlainSearchToken(meta, token);
        }
    }

    private static boolean isCobblemonFamily(ResourceLocation id, Map<String, String> meta, String modName) {
        return "cobblemon".equals(id.getNamespace())
                || id.getNamespace().contains("cobblemon")
                || CompatFamilyDetector.hasFamily(meta, CompatFamilyDetector.COBBLEMON)
                || normalizedContains(modName, "pokemon")
                || normalizedContains(modName, "poke");
    }

    private static boolean normalizedContains(String raw, String needle) {
        if (raw == null || raw.isBlank()) {
            return false;
        }
        String normalized = Normalizer.normalize(raw, Normalizer.Form.NFD)
                .replaceAll("\\p{M}+", "")
                .toLowerCase(Locale.ROOT);
        return normalized.contains(needle);
    }

    private static void collectPlainSearchTokens(Set<String> tokens, @Nullable String raw) {
        if (raw == null || raw.isBlank()) {
            return;
        }
        String normalized = Normalizer.normalize(raw.replaceAll("([a-z])([A-Z])", "$1 $2"), Normalizer.Form.NFD)
                .replaceAll("\\p{M}+", "")
                .toLowerCase(Locale.ROOT);
        for (String part : normalized.split("[^a-z0-9]+")) {
            if (part.length() >= 3 && !isPlainSearchStopWord(part)) {
                tokens.add(part);
            }
        }
    }

    private static boolean isPlainSearchStopWord(String token) {
        return PLAIN_SEARCH_STOP_WORDS.contains(token);
    }

    private static boolean containsToken(String existing, String added) {
        if (existing == null || existing.isBlank()) {
            return false;
        }
        for (String part : existing.split("\\s+")) {
            if (part.equals(added)) {
                return true;
            }
        }
        return false;
    }

    private static void addTooltipSearchTokens(Map<String, String> meta, ItemStack stack, @Nullable Level level,
                                               ResourceLocation id, String displayName,
                                               Map<String, String> modNameCache) {
        if (!IndexingHotItemPolicy.shouldIndexTooltipSearchTokens()) {
            return;
        }
        try {
            String modName = modNameCache.computeIfAbsent(id.getNamespace(), namespace ->
                    Services.PLATFORM.getModName(namespace).orElse(namespace));
            String tokens = TooltipSearchTokens.extract(
                    Services.PLATFORM.getTooltipLines(stack, level),
                    displayName,
                    id,
                    modName
            );
            if (!tokens.isBlank()) {
                meta.put(SearchNodeKeys.TOOLTIP_SEARCH_TOKENS, tokens);
            }
        } catch (RuntimeException e) {
            AmiCore.LOGGER.debug("Unable to inspect search tooltip for {}", id, e);
        }
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

    private static boolean namespaceIs(ResourceLocation id, String... namespaces) {
        if (id == null) {
            return false;
        }
        String namespace = id.getNamespace();
        for (String candidate : namespaces) {
            if (namespace.equals(candidate)) {
                return true;
            }
        }
        return false;
    }

    private static boolean namespaceStartsWith(ResourceLocation id, String prefix) {
        return id != null && id.getNamespace().startsWith(prefix);
    }

    private static boolean hasCompatFamily(Map<String, String> meta, String family) {
        return CompatFamilyDetector.hasFamily(meta, family);
    }

    private static void runFocusedCompatHooks(ResourceLocation id, ItemStack stack, @Nullable Level level,
                                              Map<String, String> meta, boolean includePluginHooks) {
        if (namespaceIs(id, "cobblemon") || hasCompatFamily(meta, CompatFamilyDetector.COBBLEMON)) {
            ItemProviderCompatHooks.runCompatSafely("CobblemonCompat", () -> CobblemonCompat.enrichItem(id, meta));
        }
        if (namespaceStartsWith(id, "create") || hasCompatFamily(meta, CompatFamilyDetector.CREATE)) {
            ItemProviderCompatHooks.runCompatSafely("CreateCompat", () -> CreateCompat.enrichItem(id, meta));
        }
        if (namespaceIs(id, "ae2", "appliedenergistics2") || hasCompatFamily(meta, CompatFamilyDetector.AE2)) {
            ItemProviderCompatHooks.runCompatSafely("AE2Compat", () -> AE2Compat.enrichItem(id, meta));
        }
        if (namespaceStartsWith(id, "mekanism") || hasCompatFamily(meta, CompatFamilyDetector.MEKANISM)) {
            ItemProviderCompatHooks.runCompatSafely("MekanismCompat", () -> MekanismCompat.enrichItem(id, meta));
        }
        if (namespaceIs(id, "gtceu", "gregtech") || hasCompatFamily(meta, CompatFamilyDetector.GREGTECH)) {
            ItemProviderCompatHooks.runCompatSafely("GregTechCompat", () -> GregTechCompat.enrichItem(id, meta));
        }
        if (namespaceIs(id, "sophisticatedbackpacks", "sophisticatedstorage", "sophisticatedcore")
                || hasCompatFamily(meta, CompatFamilyDetector.SOPHISTICATED)) {
            ItemProviderCompatHooks.runCompatSafely("SophisticatedCompat", () -> SophisticatedCompat.enrichItem(id, meta));
        }
        if (namespaceIs(id, "silentgear", "tconstruct") || hasCompatFamily(meta, CompatFamilyDetector.MODULAR_GEAR)
                || hasCompatFamily(meta, CompatFamilyDetector.TINKERS) || hasCompatFamily(meta, CompatFamilyDetector.SILENT_GEAR)) {
            ItemProviderCompatHooks.runCompatSafely("ModularGearCompat", () -> ModularGearCompat.enrichItem(id, meta));
            ItemProviderCompatHooks.runCompatSafely("ModularGearCompatRuntime", () ->
                    ModularGearCompat.enrichRuntimeStack(id, stack, level, meta));
        }
        if (namespaceIs(id, "modulargolems") || hasCompatFamily(meta, CompatFamilyDetector.MODULAR_GOLEMS)) {
            ItemProviderCompatHooks.runCompatSafely("ModularGolemsCompat", () -> ModularGolemsCompat.enrichItem(id, meta));
        }
        if (namespaceIs(id, "rechiseled", "rechiseledcreate")) {
            ItemProviderCompatHooks.runCompatSafely("RechiseledCompat", () -> RechiseledCompat.enrichItem(id, meta));
        }
        if (namespaceIs(id, "chipped")) {
            ItemProviderCompatHooks.runCompatSafely("ChippedCompat", () -> ChippedCompat.enrichItem(id, meta));
        }
        ItemProviderCompatHooks.runCompatSafely("StorageCompat", () -> StorageCompat.enrichItem(id, meta));
        if (includePluginHooks) {
            ItemProviderCompatHooks.runPluginItemCompatHooks(id, stack, level, meta);
        }
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

    private static boolean shouldDefaultCollapseExplicitFamily(GroupingEngine.CollapsedFamily family) {
        return family != null && Set.of("banner_patterns", "banners", "goat_horns", "music_discs").contains(family.key());
    }

    private static Map<String, String> buildSubtypeMeta(ResourceLocation baseId, ItemStack stack, String colorBucket,
                                                        @Nullable ItemFilter.CreativeTabInfo creativeTab,
                                                        @Nullable Level level,
                                                        Map<String, String> modNameCache) {
        Map<String, String> meta = new HashMap<>(32);
        meta.put(SearchNodeKeys.MOD_ID, baseId.getNamespace());
        meta.put(SearchNodeKeys.SUBTYPE_OF, baseId.toString());
        meta.put(SearchNodeKeys.COLOR_BUCKET, colorBucket);
        meta.put(SearchNodeKeys.MATERIAL_GROUP, baseId.toString());
        meta.put(SearchNodeKeys.ACCESS_LEVEL, ItemFilter.ACCESS_SURVIVAL);
        applyCreativeTabMeta(meta, creativeTab);
        GroupingEngine.classifyCollapsedFamily(baseId).ifPresent(family -> {
            meta.put(SearchNodeKeys.COLLAPSE_FAMILY, family.key());
            meta.put(SearchNodeKeys.COLLAPSE_LABEL, family.label());
            if (shouldDefaultCollapseExplicitFamily(family)) {
                meta.put(SearchNodeKeys.VARIANT_COLLAPSE_MODE, "default_collapsed");
            }
        });
        addDurability(meta, stack);

        Item item = stack.getItem();
        FacetProfile facetProfile = FacetIndexer.index(item, baseId, stack);
        OptionalLong esmCapacity = StorageMetricSniffer.estimate(stack, baseId, level);
        esmCapacity.ifPresent(value -> meta.put(SearchNodeKeys.ESM_CAPACITY, Long.toString(value)));
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

        ItemProviderCompatHooks.runCompatSafely("CompatFamilyDetector", () -> CompatFamilyDetector.detect(baseId, meta));
        runFocusedCompatHooks(baseId, stack, level, meta, true);
        addCheapPlainSearchTokens(meta, baseId, modNameCache);
        CategoryAssignment assignment = PrimaryCategoryResolver.resolve(baseId, facetProfile.facets(), meta);
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
        populateItems(index, level, ItemIndexPass.PRIMARY);
    }

    public void populateDeferredNamespaces(GlobalIndex index, @Nullable Level level) {
        populateItems(index, level, ItemIndexPass.DEFERRED);
    }

    private void populateItems(GlobalIndex index, @Nullable Level level, ItemIndexPass pass) {
        long started = System.currentTimeMillis();
        long setupStart = started;
        ItemProviderCompatHooks.clearDisabledCompatHooks();
        GroupingEngine.initialize(level);
        GroupingEngine.rebuildDynamicShapeCandidates(BuiltInRegistries.ITEM);
        long setupMs = System.currentTimeMillis() - setupStart;
        boolean strictSurvival = AmiConfig.strictSurvivalMode;
        AmiIndexerService progress = AmiIndexerService.getInstance();

        progress.beginProgress("Reading creative tabs");
        long creativeStart = System.currentTimeMillis();
        Map<Item, List<ItemFilter.CreativeStackInfo>> creativeStackMap = ItemFilter.buildCreativeStackMap(level);
        Map<Item, ItemFilter.CreativeTabInfo> creativeTabs = ItemFilter.firstCreativeTabs(creativeStackMap);
        Map<String, String> modNameCache = new HashMap<>();
        Set<Item> creativeItems = creativeTabs.keySet();
        AmiRecipeIndex recipeIndex = AmiRecipeIndex.getInstance();
        Set<Item> recipeOutputs = (strictSurvival || AmiConfig.showHiddenModItems)
                ? recipeIndex.getAllOutputItems()
                : Collections.emptySet();
        long creativeMs = System.currentTimeMillis() - creativeStart;

        boolean hasCreativeData = !creativeItems.isEmpty();
        boolean hasRecipeData = !recipeOutputs.isEmpty();

        RegistryAccess registryAccess = level != null ? level.registryAccess() : null;
        int totalItems = BuiltInRegistries.ITEM.size();
        int scannedItems = 0;
        int baseItemNodes = 0;
        int subtypeNodes = 0;
        int fastFacadeNodes = 0;
        int creativeVariantCandidates = 0;
        int suppressedCreativeVariants = 0;
        int deferredSkipped = 0;
        long subtypeExpandNs = 0L;
        long subtypeNodeNs = 0L;
        long basePreRecipeNs = 0L;
        long baseRecipeNs = 0L;
        long basePostRecipeNs = 0L;
        long tooltipSearchNs = 0L;
        long baseIdentityNs = 0L;
        long baseGroupingNs = 0L;
        long baseTagsToolNs = 0L;
        long baseMetricsNs = 0L;
        long baseFacetNs = 0L;
        long basePreMetaNs = 0L;
        long basePostCompatNs = 0L;
        long basePostCategoryNs = 0L;
        long basePostAddNodeNs = 0L;
        progress.beginProgress(pass == ItemIndexPass.DEFERRED ? "Indexing deferred items" : "Indexing items",
                pass == ItemIndexPass.DEFERRED ? IndexingHotItemPolicy.deferredIndexNamespacesForLog() : "",
                totalItems);
        long itemLoopStart = System.currentTimeMillis();

        for (Item item : orderedItemsForIndexing()) {
            scannedItems++;
            ResourceLocation id = BuiltInRegistries.ITEM.getKey(item);
            if ((scannedItems & 31) == 0 || scannedItems == totalItems) {
                progress.updateProgress(scannedItems);
                if (id != null) {
                    progress.updateProgressDetail(id.toString());
                }
            }
            if (id == null || id.getNamespace().equals("air") || id.getPath().equals("air")) continue;
            boolean deferredNamespace = IndexingHotItemPolicy.shouldDeferFullIndex(id);
            if (pass == ItemIndexPass.PRIMARY && deferredNamespace) {
                deferredSkipped++;
                continue;
            }
            if (pass == ItemIndexPass.DEFERRED && !deferredNamespace) {
                continue;
            }

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

            if (IndexingHotItemPolicy.shouldUseFastFacadeIndex(id)) {
                if (!ItemFilter.shouldShowAccessLevel(accessLevel)) continue;
                indexFastFacadeItem(index, item, id, creativeStackMap, creativeTabs.get(item), accessLevel, inCreative);
                baseItemNodes++;
                fastFacadeNodes++;
                continue;
            }

            // Generated subtypes should not be suppressed just because the dummy base item is dev-only.
            long subtypeExpandStart = System.nanoTime();
            List<SubtypeExpander.SubtypeEntry> subtypes =
                    SubtypeExpander.expand(id, registryAccess);
            List<ItemFilter.CreativeStackInfo> creativeStacks = creativeStackMap.get(item);
            if (subtypes.isEmpty()
                    && ItemFilter.shouldShowAccessLevel(accessLevel)
                    && hasMultipleCreativeStacks(creativeStacks)) {
                creativeVariantCandidates++;
                if (IndexingHotItemPolicy.shouldSuppressCreativeVariantExpansion(id)) {
                    suppressedCreativeVariants++;
                } else {
                    subtypes = CreativeStackVariantExpander.expand(id, creativeStacks, level);
                }
            }
            subtypeExpandNs += System.nanoTime() - subtypeExpandStart;
            if (!subtypes.isEmpty()) {
                long subtypeNodeStart = System.nanoTime();
                String tags = collectTags(item);
                for (SubtypeExpander.SubtypeEntry entry : subtypes) {
                    ItemIconRenderer.registerStack(entry.id(), entry.stack());
                    String colorBucket = entry.extraMeta().getOrDefault(
                            SearchNodeKeys.COLOR_BUCKET,
                            extractColorBucket(entry.id())
                    );
                    Map<String, String> meta = buildSubtypeMeta(id, entry.stack(), colorBucket, creativeTabs.get(item), level, modNameCache);
                    if (!entry.extraMeta().isEmpty()) meta.putAll(entry.extraMeta());
                    if (!tags.isEmpty()) meta.put(SearchNodeKeys.TAGS, tags);
                    foodMetricSniffer.sniff(entry.stack()).ifPresent(stats -> addFoodStats(meta, stats));
                    powerMetricSniffer.sniff(entry.stack(), entry.id(), level).ifPresent(stats -> addPowerStats(meta, stats));
                    fluidMetricSniffer.sniff(entry.stack(), entry.id(), level).ifPresent(stats -> addFluidStats(meta, stats));
                    toolMetricSniffer.sniff(entry.stack()).ifPresent(stats -> addToolStats(meta, stats));
                    armorMetricSniffer.sniff(entry.stack()).ifPresent(stats -> addArmorStats(meta, stats));
                    long tooltipSearchStart = System.nanoTime();
                    addTooltipSearchTokens(meta, entry.stack(), level, entry.id(), entry.displayName(), modNameCache);
                    tooltipSearchNs += System.nanoTime() - tooltipSearchStart;
                    inferAmmoType(entry.id(), meta);
                    markGeneratedModularGearVariantCheatOnly(entry.id(), meta);
                    if (!ItemFilter.shouldShowAccessLevel(meta.getOrDefault(SearchNodeKeys.ACCESS_LEVEL, ItemFilter.ACCESS_SURVIVAL))
                            && !isHiddenComponentDuplicateVariant(meta)) {
                        continue;
                    }
                    index.addNode(new SearchNode(entry.id(), NodeType.ITEM,
                            entry.displayName(), 0xFFFFFF, 0, meta));
                    subtypeNodes++;
                }
                subtypeNodeNs += System.nanoTime() - subtypeNodeStart;
                // Skip the plain base node — its subtypes represent the full item space.
                continue;
            }

            if (!ItemFilter.shouldShowAccessLevel(accessLevel)) continue;

            long basePreRecipeStart = System.nanoTime();
            long baseStageStart = System.nanoTime();
            ItemStack defaultStack = new ItemStack(item);
            String modId = id.getNamespace();
            String displayName = item.getName(defaultStack).getString();
            ItemFilter.firstCreativeStack(item, creativeStackMap).ifPresent(stack -> ItemIconRenderer.registerStack(id, stack));
            baseIdentityNs += System.nanoTime() - baseStageStart;

            baseStageStart = System.nanoTime();
            String variantGroup = GroupingEngine.classifyShape(item);
            String colorBucket = GroupingEngine.classifyColor(defaultStack);
            String materialGroup = GroupingEngine.classifyMaterialRoot(defaultStack);
            Optional<GroupingEngine.CollapsedFamily> collapsedFamily = GroupingEngine.classifyCollapsedFamily(id);
            baseGroupingNs += System.nanoTime() - baseStageStart;

            baseStageStart = System.nanoTime();
            int color = 0xFFFFFF;
            String tags = collectTags(item);
            String requiredTool = determineRequiredTool(item);
            baseTagsToolNs += System.nanoTime() - baseStageStart;

            baseStageStart = System.nanoTime();
            Optional<DpsMetricSniffer.DpsStats> dpsStats = DpsMetricSniffer.estimateStats(defaultStack);
            OptionalLong esmCapacity = StorageMetricSniffer.estimate(defaultStack, id, level);
            Optional<PowerStats> powerStats = powerMetricSniffer.sniff(defaultStack, id, level);
            Optional<FoodStats> foodStats = foodMetricSniffer.sniff(defaultStack);
            Optional<FluidStats> fluidStats = fluidMetricSniffer.sniff(defaultStack, id, level);
            Optional<ToolStats> toolStats = toolMetricSniffer.sniff(defaultStack);
            Optional<ArmorStats> armorStats = armorMetricSniffer.sniff(defaultStack);
            baseMetricsNs += System.nanoTime() - baseStageStart;

            baseStageStart = System.nanoTime();
            FacetProfile facetProfile = FacetIndexer.index(item, id, defaultStack);
            java.util.EnumSet<ItemFacet> resolvedFacets = facetProfile.facets().isEmpty()
                    ? java.util.EnumSet.noneOf(ItemFacet.class)
                    : java.util.EnumSet.copyOf(facetProfile.facets());
            boolean metricFacetAdded = false;
            if (powerStats.map(PowerStats::hasAny).orElse(false)) {
                metricFacetAdded |= resolvedFacets.add(ItemFacet.HAS_ENERGY);
            }
            if (fluidStats.map(FluidStats::hasAny).orElse(false)) {
                metricFacetAdded |= resolvedFacets.add(ItemFacet.FLUID_CONTAINER);
            }
            if (metricFacetAdded) {
                facetProfile = new FacetProfile(resolvedFacets, facetProfile.attributes());
            }
            baseFacetNs += System.nanoTime() - baseStageStart;

            baseStageStart = System.nanoTime();
            Map<String, String> meta = new HashMap<>(32);
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
                if (shouldDefaultCollapseExplicitFamily(family)) {
                    meta.put(SearchNodeKeys.VARIANT_COLLAPSE_MODE, "default_collapsed");
                }
            });
            if (collapsedFamily.isEmpty()) {
                GroupingEngine.classifyTintableGeneratedFamily(id, variantGroup, colorBucket, tags)
                        .or(() -> GroupingEngine.classifyLexicalGeneratedFamily(id, colorBucket))
                        .or(() -> GroupingEngine.classifyColorizedGeneratedFamily(id, displayName, colorBucket, tags, materialGroup))
                        .or(() -> GroupingEngine.classifyCompressedBlockFamily(id))
                        .ifPresent(family -> {
                            meta.put(SearchNodeKeys.COLLAPSE_FAMILY, family.key());
                            meta.put(SearchNodeKeys.COLLAPSE_LABEL, family.label());
                            meta.put(SearchNodeKeys.VARIANT_COLLAPSE_MODE, "default_collapsed");
                        });
            }
            addDurability(meta, defaultStack);
            dpsStats.ifPresent(stats -> {
                meta.put(SearchNodeKeys.ATTACK_DAMAGE, formatDps(stats.damage()));
                meta.put(SearchNodeKeys.DPS, formatDps(stats.dps()));
            });
            esmCapacity.ifPresent(value -> meta.put(SearchNodeKeys.ESM_CAPACITY, Long.toString(value)));
            powerStats.ifPresent(stats -> addPowerStats(meta, stats));
            foodStats.ifPresent(stats -> addFoodStats(meta, stats));
            fluidStats.ifPresent(stats -> addFluidStats(meta, stats));
            toolStats.ifPresent(stats -> addToolStats(meta, stats));
            armorStats.ifPresent(stats -> addArmorStats(meta, stats));
            long tooltipSearchStart = System.nanoTime();
            addTooltipSearchTokens(meta, defaultStack, level, id, displayName, modNameCache);
            tooltipSearchNs += System.nanoTime() - tooltipSearchStart;
            inferAmmoType(id, meta);
            ItemProviderCompatHooks.runCompatSafely("CompatFamilyDetector", () -> CompatFamilyDetector.detect(id, meta));
            if (!inCreative) {
                meta.put(SearchNodeKeys.VISIBILITY, "hidden");
            }
            basePreMetaNs += System.nanoTime() - baseStageStart;
            basePreRecipeNs += System.nanoTime() - basePreRecipeStart;

            long baseRecipeStart = System.nanoTime();
            RecipeMetadata recipeMetadata = computeRecipeMetadata(defaultStack);
            meta.put(SearchNodeKeys.OBTAINABILITY, recipeMetadata.obtainability());
            if (!recipeMetadata.recipeCategories().isEmpty()) {
                meta.put(SearchNodeKeys.RECIPE_CATEGORIES, recipeMetadata.recipeCategories());
            }
            if (!recipeMetadata.recipeUseCategories().isEmpty()) {
                meta.put(SearchNodeKeys.RECIPE_USE_CATEGORIES, recipeMetadata.recipeUseCategories());
            }
            if (recipeMetadata.recipeOutputCount() > 0) {
                meta.put(SearchNodeKeys.RECIPE_OUTPUT_COUNT, Integer.toString(recipeMetadata.recipeOutputCount()));
            }
            if (recipeMetadata.recipeUseCount() > 0) {
                meta.put(SearchNodeKeys.RECIPE_USE_COUNT, Integer.toString(recipeMetadata.recipeUseCount()));
            }
            baseRecipeNs += System.nanoTime() - baseRecipeStart;

            long basePostRecipeStart = System.nanoTime();
            long basePostStageStart = System.nanoTime();
            runFocusedCompatHooks(id, defaultStack, level, meta, true);
            addCheapPlainSearchTokens(meta, id, modNameCache);
            basePostCompatNs += System.nanoTime() - basePostStageStart;

            basePostStageStart = System.nanoTime();
            CategoryAssignment assignment = PrimaryCategoryResolver.resolve(id, facetProfile.facets(), meta);
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
            basePostCategoryNs += System.nanoTime() - basePostStageStart;

            basePostStageStart = System.nanoTime();
            index.addNode(new SearchNode(id, NodeType.ITEM, displayName, color, 0, meta));
            baseItemNodes++;
            basePostAddNodeNs += System.nanoTime() - basePostStageStart;
            basePostRecipeNs += System.nanoTime() - basePostRecipeStart;
        }
        long itemLoopMs = System.currentTimeMillis() - itemLoopStart;

        int heroNodes = 0;
        long heroMs = 0L;
        if (pass == ItemIndexPass.PRIMARY) {
            // Collect hero items from registered plugins (mods with infinite modular variants).
            progress.beginProgress("Indexing plugin variants");
            long heroStart = System.currentTimeMillis();
            heroNodes = indexHeroItems(index, registryAccess, creativeTabs, level);
            heroMs = System.currentTimeMillis() - heroStart;
        }
        AmiCore.LOGGER.info(
                "AMI indexing: ItemProvider pass={} setup={}ms creativeTabs={}ms items={}ms heroes={}ms total={}ms scanned={} deferredSkipped={} baseNodes={} subtypeNodes={} fastFacadeNodes={} creativeVariantCandidates={} suppressedCreativeVariants={} heroNodes={} breakdown=subtypeExpand:{}ms subtypeNodes:{}ms basePreRecipe:{}ms baseRecipe:{}ms basePostRecipe:{}ms tooltipSearch:{}ms detail=baseIdentity:{}ms baseGrouping:{}ms baseTagsTool:{}ms baseMetrics:{}ms baseFacet:{}ms basePreMeta:{}ms basePostCompat:{}ms basePostCategory:{}ms basePostAddNode:{}ms",
                pass.label, setupMs, creativeMs, itemLoopMs, heroMs, System.currentTimeMillis() - started,
                scannedItems, deferredSkipped, baseItemNodes, subtypeNodes, fastFacadeNodes, creativeVariantCandidates,
                suppressedCreativeVariants, heroNodes,
                nanosToMillis(subtypeExpandNs), nanosToMillis(subtypeNodeNs), nanosToMillis(basePreRecipeNs),
                nanosToMillis(baseRecipeNs), nanosToMillis(basePostRecipeNs), nanosToMillis(tooltipSearchNs),
                nanosToMillis(baseIdentityNs), nanosToMillis(baseGroupingNs), nanosToMillis(baseTagsToolNs),
                nanosToMillis(baseMetricsNs), nanosToMillis(baseFacetNs), nanosToMillis(basePreMetaNs),
                nanosToMillis(basePostCompatNs), nanosToMillis(basePostCategoryNs), nanosToMillis(basePostAddNodeNs));
    }

    private static long nanosToMillis(long nanos) {
        return java.util.concurrent.TimeUnit.NANOSECONDS.toMillis(nanos);
    }

    private static boolean hasMultipleCreativeStacks(@Nullable List<ItemFilter.CreativeStackInfo> stacks) {
        return stacks != null && stacks.size() > 1;
    }

    private static List<Item> orderedItemsForIndexing() {
        List<Item> regular = new ArrayList<>();
        List<Item> deferred = new ArrayList<>();
        for (Item item : BuiltInRegistries.ITEM) {
            ResourceLocation id = BuiltInRegistries.ITEM.getKey(item);
            if (IndexingHotItemPolicy.shouldDeferUntilTail(id)) {
                deferred.add(item);
            } else {
                regular.add(item);
            }
        }
        regular.addAll(deferred);
        return regular;
    }

    private void indexFastFacadeItem(GlobalIndex index, Item item, ResourceLocation id,
                                     Map<Item, List<ItemFilter.CreativeStackInfo>> creativeStackMap,
                                     @Nullable ItemFilter.CreativeTabInfo creativeTab,
                                     String accessLevel, boolean inCreative) {
        ItemStack defaultStack = new ItemStack(item);
        ItemStack iconStack = ItemFilter.firstCreativeStack(item, creativeStackMap).orElse(defaultStack);
        if (!iconStack.isEmpty()) {
            ItemIconRenderer.registerStack(id, iconStack);
        }

        Map<String, String> meta = new HashMap<>(16);
        meta.put(SearchNodeKeys.MOD_ID, id.getNamespace());
        meta.put(SearchNodeKeys.ACCESS_LEVEL, accessLevel);
        meta.put(SearchNodeKeys.VARIANT_GROUP, "facade");
        meta.put(SearchNodeKeys.MATERIAL_GROUP, id.toString());
        meta.put(SearchNodeKeys.SEARCH_TOKENS, "facade facades cover covers");
        meta.put(SearchNodeKeys.OBTAINABILITY, "no_recipe");
        meta.put(SearchNodeKeys.ONTOLOGY_CATEGORY, "tech");
        meta.put(SearchNodeKeys.ONTOLOGY_SUBCATEGORY, "components");
        applyCreativeTabMeta(meta, creativeTab);
        if (!inCreative) {
            meta.put(SearchNodeKeys.VISIBILITY, "hidden");
        }
        if ("ae2".equals(id.getNamespace()) || "appliedenergistics2".equals(id.getNamespace())) {
            meta.put(SearchNodeKeys.PRIMARY_COMPAT_FAMILY, "ae2");
            meta.put(SearchNodeKeys.COMPAT_FAMILIES, "ae2");
            meta.put(SearchNodeKeys.AE2_ITEM_KIND, "facade");
            meta.put(SearchNodeKeys.AE2_FACTS, "facade");
        }

        String displayName = item.getName(defaultStack).getString();
        index.addNode(new SearchNode(id, NodeType.ITEM, displayName, 0xFFFFFF, 0, meta));
    }

    private static boolean isHiddenComponentDuplicateVariant(Map<String, String> meta) {
        return "hidden_component_duplicate".equals(meta.get("variantAccessReason"));
    }

    private int indexHeroItems(GlobalIndex index, @Nullable RegistryAccess registryAccess,
                                Map<Item, ItemFilter.CreativeTabInfo> creativeTabs,
                                @Nullable Level level) {
        if (!ItemFilter.shouldShowAccessLevel(ItemFilter.ACCESS_CHEAT)) {
            return 0;
        }
        int emittedTotal = 0;
        Map<String, String> modNameCache = new HashMap<>();
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
            Set<String> seenHeroStackKeys = new HashSet<>();
            Set<ResourceLocation> emittedHeroIds = new HashSet<>();
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

                String identityHash = CreativeStackVariantExpander.stackIdentityHash(baseId, stack, level);
                String stackKey = baseId + "|" + identityHash;
                if (!seenHeroStackKeys.add(stackKey)) {
                    continue;
                }
                ResourceLocation syntheticId = Services.PLATFORM.rl(
                        "ami",
                        "hero/" + pluginKey + "/" + baseId.getNamespace() + "/" + baseId.getPath() + "_" + identityHash
                );
                int collisionOrdinal = 1;
                while (!emittedHeroIds.add(syntheticId)) {
                    syntheticId = Services.PLATFORM.rl("ami", syntheticId.getPath() + "_" + collisionOrdinal++);
                }
                ItemIconRenderer.registerStack(syntheticId, stack);

                Map<String, String> meta = buildSubtypeMeta(baseId, stack, extractColorBucket(baseId), creativeTabs.get(stack.getItem()), level, modNameCache);
                meta.put(SearchNodeKeys.ACCESS_LEVEL, ItemFilter.ACCESS_CHEAT);
                meta.put(SearchNodeKeys.VARIANT_SOURCE, "plugin_hero_stack");
                meta.put("variantAccessReason", "modular_generated_stack");
                foodMetricSniffer.sniff(stack).ifPresent(stats -> addFoodStats(meta, stats));
                powerMetricSniffer.sniff(stack, syntheticId, level).ifPresent(stats -> addPowerStats(meta, stats));
                fluidMetricSniffer.sniff(stack, syntheticId, level).ifPresent(stats -> addFluidStats(meta, stats));
                toolMetricSniffer.sniff(stack).ifPresent(stats -> addToolStats(meta, stats));
                armorMetricSniffer.sniff(stack).ifPresent(stats -> addArmorStats(meta, stats));
                addTooltipSearchTokens(meta, stack, level, syntheticId, stack.getHoverName().getString(), modNameCache);
                inferAmmoType(baseId, meta);
                index.addNode(new SearchNode(syntheticId, NodeType.ITEM,
                        stack.getHoverName().getString(), 0xFFFFFF, 0, meta));
                count++;
                emittedTotal++;
            }
        }
        return emittedTotal;
    }

    private static void markGeneratedModularGearVariantCheatOnly(ResourceLocation syntheticId, Map<String, String> meta) {
        if (syntheticId == null || meta == null || !syntheticId.getPath().contains("/variant/")) {
            return;
        }
        boolean modularGear = meta.getOrDefault(SearchNodeKeys.COMPAT_FAMILIES, "").contains(CompatFamilyDetector.MODULAR_GEAR)
                || !meta.getOrDefault(SearchNodeKeys.MODULAR_GEAR_FAMILY, "").isBlank();
        boolean assembledVariant = !meta.getOrDefault(SearchNodeKeys.MODULAR_GEAR_RUNTIME_TRAITS, "").isBlank()
                || !meta.getOrDefault(SearchNodeKeys.MODULAR_GEAR_RUNTIME_MATERIALS, "").isBlank();
        if (!modularGear || !assembledVariant) {
            return;
        }
        meta.put(SearchNodeKeys.ACCESS_LEVEL, ItemFilter.ACCESS_CHEAT);
        meta.put("variantAccessReason", "modular_generated_stack");
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
