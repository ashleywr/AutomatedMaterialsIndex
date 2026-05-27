package com.sanhiruzu.ami.client.icon;

import com.sanhiruzu.ami.client.AMITheme;
import com.sanhiruzu.ami.index.NodeType;
import com.sanhiruzu.ami.platform.Services;
import com.sanhiruzu.ami.index.SearchNode;
import com.sanhiruzu.ami.index.SearchNodeKeys;
import com.sanhiruzu.ami.util.AmiWorldTooltipComposer;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Renders biomes and structures as a "hero block" item over a dimension-tinted
 * pastel gradient background — Cherry Sapling on green = Cherry Grove, not a block.
 * Unknown biomes/structures fall back to a dimension-appropriate default item.
 */
public class ProxyBlockRenderer implements IIconRenderer {

    // String keys so we don't allocate ResourceLocations at class-load time
    private static final Map<String, String> PROXY_MAP = new HashMap<>();
    private static final Map<ResourceLocation, ItemStack> stackCache = new HashMap<>();

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
        p("minecraft:mineshaft_mesa", "minecraft:golden_rail");
        p("minecraft:desert_pyramid", "minecraft:sandstone");
        p("minecraft:jungle_pyramid", "minecraft:mossy_cobblestone");
        p("minecraft:igloo", "minecraft:snow_block");
        p("minecraft:swamp_hut", "minecraft:mushroom_stem");
        p("minecraft:ocean_monument", "minecraft:prismarine");
        p("minecraft:ocean_ruin_cold", "minecraft:mossy_stone_bricks");
        p("minecraft:ocean_ruin_warm", "minecraft:sandstone");
        p("minecraft:pillager_outpost", "minecraft:dark_oak_planks");
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
    }

    private static void p(String key, String value) {
        PROXY_MAP.put(key, value);
    }

    // ── Rendering ─────────────────────────────────────────────────────────────

    @Override
    public void render(GuiGraphics g, SearchNode node, int x, int y, int size, boolean hovered) {
        int bg = dimensionColor(node);

        // Two-tone gradient: top half slightly lighter
        int bgLight = lighten(bg, 20);
        g.fill(x, y, x + size, y + size / 2, bgLight);
        g.fill(x, y + size / 2, x + size, y + size, bg);

        // Subtle 1px border to show it's a "badge" rather than a real item
        int borderColor = lighten(bg, 40);
        g.fill(x, y, x + size, y + 1, borderColor); // Top
        g.fill(x, y + size - 1, x + size, y + size, borderColor); // Bottom
        g.fill(x, y + 1, x + 1, y + size - 1, borderColor); // Left
        g.fill(x + size - 1, y + 1, x + size, y + size - 1, borderColor); // Right

        ItemStack proxy = resolveProxy(node);
        if (proxy.isEmpty()) return;

        var poses = g.pose();
        poses.pushPose();
        poses.translate(x, y, 0);
        float s = size / 16f;
        poses.scale(s, s, 1f);
        g.renderItem(proxy, 0, 0);
        poses.popPose();
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private static ItemStack resolveProxy(SearchNode node) {
        String mapped = PROXY_MAP.get(node.id().toString());
        ResourceLocation blockId;
        if (mapped != null) {
            blockId = Services.PLATFORM.rl(mapped);
        } else {
            blockId = dimensionDefaultBlock(node);
        }
        return stackCache.computeIfAbsent(blockId,
                id -> BuiltInRegistries.ITEM.getOptional(id).map(ItemStack::new).orElse(ItemStack.EMPTY));
    }

    private static ResourceLocation dimensionDefaultBlock(SearchNode node) {
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

    private static int dimensionColor(SearchNode node) {
        if (node.type() == NodeType.STRUCTURE) return AMITheme.DIM_ICON_BG_STRUCTURE;
        String dim = node.meta(SearchNodeKeys.DIMENSION, "overworld");
        return switch (dim) {
            case "nether" -> AMITheme.DIM_ICON_BG_NETHER;
            case "the_end",
                 "end" -> AMITheme.DIM_ICON_BG_END;
            default -> AMITheme.DIM_ICON_BG_OVERWORLD;
        };
    }

    private static int lighten(int argb, int amount) {
        int a = (argb >> 24) & 0xFF;
        int r = Math.min(255, ((argb >> 16) & 0xFF) + amount);
        int gv = Math.min(255, ((argb >> 8) & 0xFF) + amount);
        int b = Math.min(255, (argb & 0xFF) + amount);
        return (a << 24) | (r << 16) | (gv << 8) | b;
    }

    @Override
    public List<Component> getTooltip(SearchNode node) {
        return AmiWorldTooltipComposer.buildBody(node);
    }

    @Override
    public void invalidate() {
        stackCache.clear();
    }
}
