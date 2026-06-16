package com.sanhiruzu.ami.client.icon;

import com.sanhiruzu.ami.index.NodeType;
import com.sanhiruzu.ami.index.SearchNode;
import com.sanhiruzu.ami.index.SearchNodeKeys;
import com.sanhiruzu.ami.platform.Services;
import com.sanhiruzu.ami.util.AmiWorldTooltipComposer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Renders biomes and structures as a "hero block" item over a dimension-tinted
 * pastel gradient background — Cherry Sapling on green = Cherry Grove, not a block.
 * Unknown biomes try to resolve a signature block from the biome id before falling
 * back to a dimension-appropriate default item.
 */
public class ProxyBlockRenderer implements IIconRenderer {

    // String keys so we don't allocate ResourceLocations at class-load time
    private static final Map<String, String> PROXY_MAP = new HashMap<>();
    private static final Set<String> BIOME_STOP_WORDS = Set.of(
            "biome", "biomes", "old", "growth", "deep", "dense", "sparse", "windswept", "wooded",
            "forest", "forests", "woods", "woodland", "grove", "plains", "plateau", "hills", "hill",
            "mountain", "mountains", "peaks", "peak", "slopes", "slope", "shore", "beach", "river",
            "ocean", "island", "islands", "valley", "caves", "cave", "fields", "field", "wastes",
            "barrens", "midlands", "highlands", "end", "nether"
    );
    private static final Map<String, List<String>> BIOME_SIGNATURES = Map.ofEntries(
            Map.entry("cherry", List.of("cherry_sapling", "cherry_leaves")),
            Map.entry("jacaranda", List.of("jacaranda_sapling", "jacaranda_leaves", "jacaranda_log")),
            Map.entry("redwood", List.of("redwood_sapling", "redwood_leaves", "redwood_log")),
            Map.entry("maple", List.of("maple_sapling", "maple_leaves", "maple_log")),
            Map.entry("willow", List.of("willow_sapling", "willow_leaves", "willow_log")),
            Map.entry("pine", List.of("pine_sapling", "pine_leaves", "pine_log", "spruce_sapling")),
            Map.entry("fir", List.of("fir_sapling", "fir_leaves", "fir_log", "spruce_sapling")),
            Map.entry("mahogany", List.of("mahogany_sapling", "mahogany_leaves", "mahogany_log")),
            Map.entry("palm", List.of("palm_sapling", "palm_leaves", "palm_log")),
            Map.entry("lavender", List.of("lavender", "lavender_flower", "allium")),
            Map.entry("flower", List.of("wildflowers", "flowering_oak_leaves", "poppy", "dandelion")),
            Map.entry("mushroom", List.of("red_mushroom_block", "brown_mushroom_block", "red_mushroom")),
            Map.entry("fungal", List.of("red_mushroom_block", "brown_mushroom_block", "warped_fungus")),
            Map.entry("glow", List.of("glowshroom", "glow_lichen", "shroomlight")),
            Map.entry("lush", List.of("azalea_leaves", "flowering_azalea_leaves", "moss_block")),
            Map.entry("dripstone", List.of("pointed_dripstone", "dripstone_block")),
            Map.entry("crag", List.of("stone", "calcite", "andesite")),
            Map.entry("volcanic", List.of("basalt", "magma_block", "blackstone")),
            Map.entry("basalt", List.of("basalt", "smooth_basalt")),
            Map.entry("sculk", List.of("sculk", "sculk_shrieker")),
            Map.entry("dead", List.of("dead_bush", "dead_leaves", "dead_log")),
            Map.entry("snow", List.of("snow_block", "powder_snow_bucket", "ice")),
            Map.entry("frozen", List.of("blue_ice", "packed_ice", "ice")),
            Map.entry("ice", List.of("blue_ice", "packed_ice", "ice")),
            Map.entry("badlands", List.of("red_sand", "terracotta")),
            Map.entry("desert", List.of("cactus", "sandstone", "sand")),
            Map.entry("swamp", List.of("lily_pad", "mangrove_propagule", "mangrove_roots")),
            Map.entry("mangrove", List.of("mangrove_propagule", "mangrove_roots", "mangrove_log")),
            Map.entry("bamboo", List.of("bamboo", "bamboo_sapling")),
            Map.entry("coral", List.of("brain_coral_block", "tube_coral_block", "sea_pickle")),
            Map.entry("kelp", List.of("kelp", "dried_kelp_block"))
    );
    private static final Set<String> STRUCTURE_STOP_WORDS = Set.of(
            "structure", "structures", "large", "small", "big", "old", "ancient", "ruined", "abandoned",
            "underground", "surface", "overworld", "nether", "end", "desert", "jungle", "snowy",
            "taiga", "savanna", "plains", "mountain", "ocean", "warm", "cold"
    );
    private static final Map<String, List<String>> STRUCTURE_SIGNATURES = Map.ofEntries(
            Map.entry("tower", List.of("stone_bricks", "cobblestone", "deepslate_bricks")),
            Map.entry("castle", List.of("stone_bricks", "chiseled_stone_bricks", "deepslate_bricks")),
            Map.entry("citadel", List.of("stone_bricks", "deepslate_bricks", "chiseled_stone_bricks")),
            Map.entry("fortress", List.of("nether_bricks", "cracked_nether_bricks", "stone_bricks")),
            Map.entry("bastion", List.of("gilded_blackstone", "polished_blackstone_bricks", "blackstone")),
            Map.entry("dungeon", List.of("spawner", "mossy_cobblestone", "cobblestone")),
            Map.entry("mineshaft", List.of("rail", "oak_planks", "chain")),
            Map.entry("village", List.of("bell", "oak_planks", "hay_block")),
            Map.entry("temple", List.of("chiseled_stone_bricks", "mossy_cobblestone", "sandstone")),
            Map.entry("pyramid", List.of("chiseled_sandstone", "sandstone", "chiseled_red_sandstone")),
            Map.entry("outpost", List.of("crossbow", "dark_oak_planks", "cobblestone")),
            Map.entry("mansion", List.of("dark_oak_planks", "dark_oak_log", "bookshelf")),
            Map.entry("monument", List.of("prismarine", "sea_lantern", "dark_prismarine")),
            Map.entry("ruin", List.of("mossy_stone_bricks", "cracked_stone_bricks", "suspicious_gravel")),
            Map.entry("ship", List.of("oak_planks", "chest", "spruce_planks")),
            Map.entry("wreck", List.of("oak_planks", "chest", "spruce_planks")),
            Map.entry("portal", List.of("obsidian", "crying_obsidian", "gold_block")),
            Map.entry("city", List.of("sculk", "reinforced_deepslate", "purpur_block")),
            Map.entry("trail", List.of("suspicious_gravel", "decorated_pot", "terracotta")),
            Map.entry("chamber", List.of("trial_spawner", "copper_grate", "tuff_bricks")),
            Map.entry("grave", List.of("chiseled_stone_bricks", "stone_bricks", "bone_block")),
            Map.entry("crypt", List.of("chiseled_stone_bricks", "soul_lantern", "bone_block")),
            Map.entry("labyrinth", List.of("mossy_stone_bricks", "cracked_stone_bricks", "stone_bricks")),
            Map.entry("maze", List.of("mossy_stone_bricks", "cracked_stone_bricks", "stone_bricks")),
            Map.entry("shrine", List.of("chiseled_stone_bricks", "lantern", "gold_block")),
            Map.entry("hut", List.of("mushroom_stem", "spruce_planks", "cauldron"))
    );
    private static final Map<Identifier, ItemStack> stackCache = new HashMap<>();
    private static final Map<Identifier, Integer> backgroundCache = new HashMap<>();

    // ── Mod-item inference ────────────────────────────────────────────────────
    // Suffix priority: earlier = preferred. Saplings/flowers are the most
    // distinctive biome icon; bricks/blocks the most distinctive for structures.
    private static final List<String> BIOME_ITEM_SUFFIXES = List.of(
            "_sapling", "_seedling",
            "_flower", "_blossom", "_petal",
            "_mushroom", "_fungus", "_spore",
            "_grass", "_fern", "_plant", "_bush", "_reed",
            "_leaves", "_frond", "_needle",
            "_log", "_stem", "_wood",
            "_block"
    );
    private static final List<String> STRUCTURE_ITEM_SUFFIXES = List.of(
            "_brick", "_bricks",
            "_block", "_stone",
            "_tile", "_tiles",
            "_pillar", "_column",
            "_planks", "_log"
    );

    // Namespace → all registered item IDs in that namespace (built once, never cleared).
    private static final Map<String, List<Identifier>> NAMESPACE_ITEM_CACHE = new HashMap<>();
    private static volatile boolean namespaceCacheReady = false;
    private static final List<String> LAST_RESORT_PROXIES = List.of(
            "minecraft:stone_bricks",
            "minecraft:grass_block",
            "minecraft:netherrack",
            "minecraft:end_stone"
    );

    static {
        // ── Overworld biomes ──────────────────────────────────────────────────
        p("minecraft:plains", "minecraft:grass_block");
        p("minecraft:sunflower_plains", "minecraft:sunflower");
        p("minecraft:forest", "minecraft:oak_log");
        p("minecraft:flower_forest", "minecraft:poppy");
        p("minecraft:birch_forest", "minecraft:birch_log");
        p("minecraft:old_growth_birch_forest", "minecraft:birch_log");
        p("minecraft:dark_forest", "minecraft:dark_oak_log");
        p("minecraft:jungle", "minecraft:jungle_log");
        p("minecraft:sparse_jungle", "minecraft:jungle_sapling");
        p("minecraft:bamboo_jungle", "minecraft:bamboo");
        p("minecraft:savanna", "minecraft:acacia_log");
        p("minecraft:savanna_plateau", "minecraft:acacia_log");
        p("minecraft:windswept_savanna", "minecraft:acacia_log");
        p("minecraft:desert", "minecraft:sand");
        p("minecraft:beach", "minecraft:sand");
        p("minecraft:snowy_beach", "minecraft:snow_block");
        p("minecraft:swamp", "minecraft:lily_pad");
        p("minecraft:mangrove_swamp", "minecraft:mangrove_log");
        p("minecraft:flower_forest", "minecraft:poppy");
        p("minecraft:cherry_grove", "minecraft:cherry_sapling");
        p("minecraft:meadow", "minecraft:dandelion");
        p("minecraft:snowy_plains", "minecraft:snow_block");
        p("minecraft:ice_spikes", "minecraft:packed_ice");
        p("minecraft:taiga", "minecraft:spruce_log");
        p("minecraft:snowy_taiga", "minecraft:spruce_log");
        p("minecraft:old_growth_pine_taiga", "minecraft:spruce_log");
        p("minecraft:old_growth_spruce_taiga", "minecraft:spruce_log");
        p("minecraft:grove", "minecraft:spruce_sapling");
        p("minecraft:snowy_slopes", "minecraft:powder_snow_bucket");
        p("minecraft:frozen_peaks", "minecraft:packed_ice");
        p("minecraft:jagged_peaks", "minecraft:stone");
        p("minecraft:stony_peaks", "minecraft:calcite");
        p("minecraft:stony_shore", "minecraft:cobblestone");
        p("minecraft:windswept_hills", "minecraft:cobblestone");
        p("minecraft:windswept_forest", "minecraft:oak_log");
        p("minecraft:windswept_gravelly_hills", "minecraft:gravel");
        p("minecraft:badlands", "minecraft:red_sand");
        p("minecraft:wooded_badlands", "minecraft:terracotta");
        p("minecraft:eroded_badlands", "minecraft:red_sandstone");
        p("minecraft:mushroom_fields", "minecraft:red_mushroom_block");
        p("minecraft:ocean", "minecraft:water_bucket");
        p("minecraft:deep_ocean", "minecraft:prismarine");
        p("minecraft:frozen_ocean", "minecraft:packed_ice");
        p("minecraft:deep_frozen_ocean", "minecraft:blue_ice");
        p("minecraft:cold_ocean", "minecraft:kelp");
        p("minecraft:deep_cold_ocean", "minecraft:prismarine");
        p("minecraft:lukewarm_ocean", "minecraft:sea_pickle");
        p("minecraft:deep_lukewarm_ocean", "minecraft:tube_coral_block");
        p("minecraft:warm_ocean", "minecraft:brain_coral_block");
        p("minecraft:river", "minecraft:gravel");
        p("minecraft:frozen_river", "minecraft:ice");
        p("minecraft:deep_dark", "minecraft:sculk");
        p("minecraft:dripstone_caves", "minecraft:pointed_dripstone");
        p("minecraft:lush_caves", "minecraft:azalea_leaves");
        // ── Nether biomes ─────────────────────────────────────────────────────
        p("minecraft:nether_wastes", "minecraft:netherrack");
        p("minecraft:soul_sand_valley", "minecraft:soul_sand");
        p("minecraft:crimson_forest", "minecraft:crimson_stem");
        p("minecraft:warped_forest", "minecraft:warped_stem");
        p("minecraft:basalt_deltas", "minecraft:basalt");
        // ── End biomes ────────────────────────────────────────────────────────
        p("minecraft:the_end", "minecraft:end_stone");
        p("minecraft:end_highlands", "minecraft:end_stone_bricks");
        p("minecraft:end_midlands", "minecraft:end_stone");
        p("minecraft:small_end_islands", "minecraft:end_stone");
        p("minecraft:end_barrens", "minecraft:end_stone");
        // ── Structures ────────────────────────────────────────────────────────
        p("minecraft:village_plains", "minecraft:oak_planks");
        p("minecraft:village_desert", "minecraft:sandstone");
        p("minecraft:village_savanna", "minecraft:acacia_planks");
        p("minecraft:village_snowy", "minecraft:spruce_planks");
        p("minecraft:village_taiga", "minecraft:spruce_planks");
        p("minecraft:stronghold", "minecraft:stone_bricks");
        p("minecraft:mineshaft", "minecraft:rail");
        p("minecraft:mineshaft_mesa", "minecraft:powered_rail");
        p("minecraft:desert_pyramid", "minecraft:sandstone");
        p("minecraft:jungle_pyramid", "minecraft:mossy_cobblestone");
        p("minecraft:igloo", "minecraft:snow_block");
        p("minecraft:swamp_hut", "minecraft:mushroom_stem");
        p("minecraft:monument", "minecraft:prismarine");
        p("minecraft:ocean_monument", "minecraft:prismarine");
        p("minecraft:ocean_ruin_cold", "minecraft:mossy_stone_bricks");
        p("minecraft:ocean_ruin_warm", "minecraft:sandstone");
        p("minecraft:pillager_outpost", "minecraft:dark_oak_planks");
        p("minecraft:mansion", "minecraft:dark_oak_planks");
        p("minecraft:woodland_mansion", "minecraft:dark_oak_planks");
        p("minecraft:buried_treasure", "minecraft:chest");
        p("minecraft:shipwreck", "minecraft:oak_planks");
        p("minecraft:shipwreck_beached", "minecraft:oak_planks");
        p("minecraft:bastion_remnant", "minecraft:gilded_blackstone");
        p("minecraft:fortress", "minecraft:nether_bricks");
        p("minecraft:nether_fossil", "minecraft:bone_block");
        p("minecraft:ruined_portal", "minecraft:obsidian");
        p("minecraft:ruined_portal_desert", "minecraft:obsidian");
        p("minecraft:ruined_portal_jungle", "minecraft:obsidian");
        p("minecraft:ruined_portal_mountain", "minecraft:obsidian");
        p("minecraft:ruined_portal_nether", "minecraft:obsidian");
        p("minecraft:ruined_portal_ocean", "minecraft:obsidian");
        p("minecraft:ruined_portal_swamp", "minecraft:obsidian");
        p("minecraft:end_city", "minecraft:purpur_block");
        p("minecraft:ancient_city", "minecraft:sculk");
        p("minecraft:trail_ruins", "minecraft:suspicious_gravel");
        p("minecraft:trial_chambers", "minecraft:copper_grate");
    }

    private static void p(String key, String value) {
        PROXY_MAP.put(key, value);
    }

    // ── Rendering ─────────────────────────────────────────────────────────────

    private static ItemStack resolveProxy(SearchNode node) {
        Identifier blockId = resolveProxyId(node);
        ItemStack stack = resolveStack(blockId);
        if (!stack.isEmpty()) {
            return stack;
        }

        Identifier defaultId = dimensionDefaultBlock(node);
        if (!defaultId.equals(blockId)) {
            stack = resolveStack(defaultId);
            if (!stack.isEmpty()) {
                return stack;
            }
        }

        for (String fallback : LAST_RESORT_PROXIES) {
            Identifier fallbackId = Services.PLATFORM.rl(fallback);
            if (fallbackId.equals(blockId) || fallbackId.equals(defaultId)) {
                continue;
            }
            stack = resolveStack(fallbackId);
            if (!stack.isEmpty()) {
                return stack;
            }
        }

        return ItemStack.EMPTY;
    }

    private static ItemStack resolveStack(Identifier id) {
        ItemStack cached = stackCache.get(id);
        if (cached != null) {
            return cached;
        }

        ItemStack stack = BuiltInRegistries.ITEM.getOptional(id).map(ItemStack::new).orElse(ItemStack.EMPTY);
        if (!stack.isEmpty()) {
            stackCache.put(id, stack);
        }
        return stack;
    }

    static Identifier resolveProxyId(SearchNode node) {
        String mapped = PROXY_MAP.get(node.id().toString());
        if (mapped != null) {
            return Services.PLATFORM.rl(mapped);
        }
        if (node.type() == NodeType.BIOME) {
            return inferBiomeProxyId(node).orElseGet(() -> dimensionDefaultBlock(node));
        }
        if (node.type() == NodeType.STRUCTURE) {
            return inferStructureProxyId(node).orElseGet(() -> dimensionDefaultBlock(node));
        }
        return dimensionDefaultBlock(node);
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private static Optional<Identifier> inferBiomeProxyId(SearchNode node) {
        String namespace = node.id().getNamespace();
        String path = node.id().getPath().toLowerCase(Locale.ROOT);
        List<Identifier> candidates = new ArrayList<>();
        Set<Identifier> seen = new HashSet<>();

        addCandidate(candidates, seen, namespace, path);
        addCandidate(candidates, seen, namespace, path + "_sapling");
        addCandidate(candidates, seen, namespace, path + "_leaves");
        addCandidate(candidates, seen, namespace, path + "_log");
        addCandidate(candidates, seen, namespace, path + "_block");

        for (var entry : BIOME_SIGNATURES.entrySet()) {
            if (path.contains(entry.getKey())) {
                for (String itemPath : entry.getValue()) {
                    addCandidate(candidates, seen, namespace, itemPath);
                    addCandidate(candidates, seen, "minecraft", itemPath);
                }
            }
        }

        for (String token : path.split("[_/]+")) {
            if (token.isBlank() || BIOME_STOP_WORDS.contains(token)) continue;
            addTokenCandidates(candidates, seen, namespace, token);
            addTokenCandidates(candidates, seen, "minecraft", token);
        }

        Optional<Identifier> found = candidates.stream().filter(ProxyBlockRenderer::itemExists).findFirst();
        if (found.isPresent()) return found;
        return findBestModItem(namespace, path, BIOME_STOP_WORDS, BIOME_ITEM_SUFFIXES);
    }

    private static Optional<Identifier> inferStructureProxyId(SearchNode node) {
        String namespace = node.id().getNamespace();
        String path = node.id().getPath().toLowerCase(Locale.ROOT);
        List<Identifier> candidates = new ArrayList<>();
        Set<Identifier> seen = new HashSet<>();

        addCandidate(candidates, seen, namespace, path);
        addCandidate(candidates, seen, namespace, path + "_block");
        addCandidate(candidates, seen, namespace, path + "_bricks");
        addCandidate(candidates, seen, namespace, path + "_stone");
        addCandidate(candidates, seen, namespace, path + "_planks");

        for (var entry : STRUCTURE_SIGNATURES.entrySet()) {
            if (path.contains(entry.getKey())) {
                for (String itemPath : entry.getValue()) {
                    addCandidate(candidates, seen, namespace, itemPath);
                    addCandidate(candidates, seen, "minecraft", itemPath);
                }
            }
        }

        for (String token : path.split("[_/]+")) {
            if (token.isBlank() || STRUCTURE_STOP_WORDS.contains(token)) continue;
            addStructureTokenCandidates(candidates, seen, namespace, token);
            addStructureTokenCandidates(candidates, seen, "minecraft", token);
        }

        Optional<Identifier> found = candidates.stream().filter(ProxyBlockRenderer::itemExists).findFirst();
        if (found.isPresent()) return found;
        return findBestModItem(namespace, path, STRUCTURE_STOP_WORDS, STRUCTURE_ITEM_SUFFIXES);
    }

    private static void addTokenCandidates(List<Identifier> candidates, Set<Identifier> seen,
                                           String namespace, String token) {
        addCandidate(candidates, seen, namespace, token + "_sapling");
        addCandidate(candidates, seen, namespace, token + "_leaves");
        addCandidate(candidates, seen, namespace, token + "_log");
        addCandidate(candidates, seen, namespace, token + "_stem");
        addCandidate(candidates, seen, namespace, token + "_mushroom");
        addCandidate(candidates, seen, namespace, token + "_block");
        addCandidate(candidates, seen, namespace, token + "_grass");
        addCandidate(candidates, seen, namespace, token + "_flower");
        addCandidate(candidates, seen, namespace, token);
    }

    private static void addStructureTokenCandidates(List<Identifier> candidates, Set<Identifier> seen,
                                                    String namespace, String token) {
        addCandidate(candidates, seen, namespace, token + "_block");
        addCandidate(candidates, seen, namespace, token + "_bricks");
        addCandidate(candidates, seen, namespace, token + "_stone");
        addCandidate(candidates, seen, namespace, token + "_planks");
        addCandidate(candidates, seen, namespace, token + "_brick");
        addCandidate(candidates, seen, namespace, token + "_pillar");
        addCandidate(candidates, seen, namespace, token + "_lantern");
        addCandidate(candidates, seen, namespace, token);
    }

    private static void addCandidate(List<Identifier> candidates, Set<Identifier> seen,
                                     String namespace, String path) {
        Identifier id = Services.PLATFORM.rl(namespace, path);
        if (seen.add(id)) {
            candidates.add(id);
        }
    }

    private static boolean itemExists(Identifier id) {
        return BuiltInRegistries.ITEM.getOptional(id).isPresent();
    }

    private static Identifier dimensionDefaultBlock(SearchNode node) {
        String dim = node.meta(SearchNodeKeys.DIMENSION, "overworld");
        if (node.type() == NodeType.STRUCTURE) {
            return Services.PLATFORM.rl("minecraft:stone_bricks");
        }
        return switch (dim) {
            case "nether" -> Services.PLATFORM.rl("minecraft:netherrack");
            case "the_end",
                 "end" -> Services.PLATFORM.rl("minecraft:end_stone");
            default -> Services.PLATFORM.rl("minecraft:grass_block");
        };
    }

    @Override
    public void render(GuiGraphicsExtractor g, SearchNode node, int x, int y, int size, boolean hovered) {
        ItemStack proxy = resolveProxy(node);
        if (proxy.isEmpty()) {
            FallbackTextRenderer.renderFallback(g, node, x, y, size);
            return;
        }
        g.fill(x, y, x + size, y + size, resolveBackground(node));

        var poses = g.pose();
        poses.pushMatrix();
        if (size == 16) {
            g.item(proxy, x, y);
        } else {
            poses.translate(x, y);
            float s = size / 16f;
            poses.scale(s, s);
            g.item(proxy, 0, 0);
        }
        poses.popMatrix();
    }

    // ── Background color ──────────────────────────────────────────────────────

    private static int resolveBackground(SearchNode node) {
        return backgroundCache.computeIfAbsent(node.id(), __ -> computeBackground(node));
    }

    private static int computeBackground(SearchNode node) {
        if (node.type() == NodeType.BIOME) {
            Minecraft mc = Minecraft.getInstance();
            if (mc.level != null) {
                var reg = mc.level.registryAccess().lookupOrThrow(Registries.BIOME);
                var holder = reg.get(net.minecraft.resources.ResourceKey.create(Registries.BIOME, node.id()));
                if (holder.isPresent()) {
                    return darkenRgb(holder.get().value().getSpecialEffects().waterColor(), 0.28f);
                }
            }
            return hashToArgb(node.id().toString(), 0.40f, 0.22f);
        }
        // Structures: dimension-family base hue + per-ID perturbation
        String dim = node.meta(SearchNodeKeys.DIMENSION, "overworld");
        float baseHue = switch (dim) {
            case "nether"               -> 0.03f;
            case "the_end", "end"       -> 0.75f;
            default                     -> 0.27f;
        };
        float hueNoise = ((node.id().toString().hashCode() & 0x3FF) / 1024f - 0.5f) * 0.15f;
        return hsbToArgb(baseHue + hueNoise, 0.38f, 0.20f);
    }

    private static int darkenRgb(int rgb, float brightness) {
        int r = (int)(((rgb >> 16) & 0xFF) * brightness);
        int g = (int)(((rgb >> 8)  & 0xFF) * brightness);
        int b = (int)( (rgb        & 0xFF) * brightness);
        return 0xFF000000 | (r << 16) | (g << 8) | b;
    }

    private static int hashToArgb(String key, float saturation, float brightness) {
        float hue = Math.abs(key.hashCode() % 360) / 360f;
        return hsbToArgb(hue, saturation, brightness);
    }

    private static int hsbToArgb(float h, float s, float b) {
        // Inline HSB→RGB to avoid java.awt dependency
        if (s == 0f) {
            int v = (int)(b * 255);
            return 0xFF000000 | (v << 16) | (v << 8) | v;
        }
        float hh = (h - (float) Math.floor(h)) * 6f;
        int   i  = (int) hh;
        float f  = hh - i;
        float p  = b * (1f - s);
        float q  = b * (1f - s * f);
        float t  = b * (1f - s * (1f - f));
        float r, g, bv;
        switch (i) {
            case 0  -> { r = b;  g = t;  bv = p; }
            case 1  -> { r = q;  g = b;  bv = p; }
            case 2  -> { r = p;  g = b;  bv = t; }
            case 3  -> { r = p;  g = q;  bv = b; }
            case 4  -> { r = t;  g = p;  bv = b; }
            default -> { r = b;  g = p;  bv = q; }
        }
        return 0xFF000000 | ((int)(r * 255) << 16) | ((int)(g * 255) << 8) | (int)(bv * 255);
    }

    // ── Mod-namespace item search ─────────────────────────────────────────────

    private static synchronized void buildNamespaceCache() {
        if (namespaceCacheReady) return;
        Map<String, List<Identifier>> map = new HashMap<>();
        for (Identifier id : BuiltInRegistries.ITEM.keySet()) {
            if (!"minecraft".equals(id.getNamespace())) {
                map.computeIfAbsent(id.getNamespace(), k -> new ArrayList<>()).add(id);
            }
        }
        NAMESPACE_ITEM_CACHE.putAll(map);
        namespaceCacheReady = true;
    }

    /**
     * Scans all items registered under {@code namespace} for the best match to
     * {@code path}. Scores by token overlap then breaks ties with {@code suffixes}
     * priority (earlier = preferred). Returns empty for minecraft: namespace since
     * the existing candidate lists already cover vanilla exhaustively.
     */
    private static Optional<Identifier> findBestModItem(
            String namespace, String path, Set<String> stopWords, List<String> suffixes) {
        if ("minecraft".equals(namespace)) return Optional.empty();
        if (!namespaceCacheReady) buildNamespaceCache();
        List<Identifier> modItems = NAMESPACE_ITEM_CACHE.get(namespace);
        if (modItems == null || modItems.isEmpty()) return Optional.empty();

        Set<String> tokens = java.util.Arrays.stream(path.split("[_/]+"))
                .filter(t -> !t.isBlank() && !stopWords.contains(t))
                .collect(Collectors.toSet());
        if (tokens.isEmpty()) return Optional.empty();

        return modItems.stream()
                .filter(ProxyBlockRenderer::itemExists)
                .filter(id -> tokens.stream().anyMatch(id.getPath()::contains))
                .max(Comparator.comparingInt(id -> scoreModItem(id.getPath(), tokens, suffixes)));
    }

    private static int scoreModItem(String itemPath, Set<String> tokens, List<String> suffixes) {
        int score = (int) tokens.stream().filter(itemPath::contains).count() * 10;
        for (int i = 0; i < suffixes.size(); i++) {
            if (itemPath.endsWith(suffixes.get(i))) {
                return score + suffixes.size() - i;
            }
        }
        return score;
    }

    @Override
    public List<Component> getTooltip(SearchNode node) {
        return AmiWorldTooltipComposer.buildBody(node);
    }

    @Override
    public void invalidate() {
        stackCache.clear();
        backgroundCache.clear();
    }
}
