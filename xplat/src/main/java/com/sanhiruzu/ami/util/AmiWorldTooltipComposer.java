package com.sanhiruzu.ami.util;

import com.sanhiruzu.ami.client.AMITheme;
import com.sanhiruzu.ami.index.SearchNode;
import com.sanhiruzu.ami.index.SearchNodeKeys;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.BiomeSpecialEffects;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Shared tooltip builder for world entries (biomes, dimensions, structures).
 * Keeps the text path consistent across the results panel and palette views.
 */
public final class AmiWorldTooltipComposer {
    // Lazy cache: biome ID → list of structure display names that generate there
    private static Map<ResourceLocation, List<String>> biomeStructureCache = null;

    private AmiWorldTooltipComposer() {
    }

    public static void invalidateCache() {
        biomeStructureCache = null;
    }

    private static Map<ResourceLocation, List<String>> getBiomeStructureMap() {
        if (biomeStructureCache != null) return biomeStructureCache;
        biomeStructureCache = new HashMap<>();
        var mc = Minecraft.getInstance();
        if (mc.level == null) return biomeStructureCache;

        mc.level.registryAccess().registry(Registries.STRUCTURE).ifPresent(structReg -> {
            for (var entry : structReg.entrySet()) {
                var structure = entry.getValue();
                var biomes = structure.biomes();
                String structName = entry.getKey().location().toString();
                for (Holder<Biome> biomeHolder : biomes) {
                    biomeHolder.unwrapKey().ifPresent(key ->
                            biomeStructureCache.computeIfAbsent(key.location(), k -> new ArrayList<>()).add(structName));
                }
            }
        });
        return biomeStructureCache;
    }

    public static List<Component> buildBody(SearchNode entry) {
        List<Component> lines = new ArrayList<>();
        switch (entry.type()) {
            case BIOME -> appendBiome(lines, entry);
            case STRUCTURE -> appendStructure(lines, entry);
            case DIMENSION -> appendDimension(lines, entry);
            default -> {
            }
        }
        return lines;
    }

    private static void appendBiome(List<Component> lines, SearchNode entry) {
        var mc = Minecraft.getInstance();

        // Always show metadata-driven fields (available even without registry)
        float temp = parseFloat(entry.meta(SearchNodeKeys.TEMPERATURE, "0"));
        float downfall = parseFloat(entry.meta(SearchNodeKeys.DOWNFALL, "0"));

        lines.add(Component.translatable("ami.tooltip.temperature")
                .append(Component.literal(": " + String.format(java.util.Locale.ROOT, "%.2f", temp))
                        .withStyle(s -> s.withColor(tempColor(temp)))));
        lines.add(Component.translatable("ami.tooltip.downfall")
                .append(Component.literal(": " + String.format(java.util.Locale.ROOT, "%.2f", downfall))
                        .withStyle(s -> s.withColor(AMITheme.ACCENT_BLUE))));

        if (mc.level == null) {
            addShiftHint(lines);
            return;
        }

        var biomeKey = ResourceKey.create(Registries.BIOME, entry.id());
        mc.level.registryAccess().registry(Registries.BIOME).ifPresent(reg ->
                reg.getHolder(biomeKey).ifPresent(holder -> {
                    Biome biome = holder.value();
                    BiomeSpecialEffects effects = biome.getSpecialEffects();
                    Component precip = !biome.hasPrecipitation()
                            ? Component.translatable("ami.precipitation.none")
                            : temp < 0.15f ? Component.translatable("ami.precipitation.snow")
                              : Component.translatable("ami.precipitation.rain");

                    lines.add(Component.translatable("ami.tooltip.precipitation")
                            .append(Component.literal(": ").append(precip)
                                    .withStyle(s -> s.withColor(AMITheme.ACCENT_BLUE))));
                    lines.add(Component.translatable("ami.tooltip.water_color")
                            .append(colorSwatch(effects.getWaterColor())));

                    if (Screen.hasShiftDown()) {
                        lines.add(Component.translatable("ami.tooltip.sky_color")
                                .append(colorSwatch(effects.getSkyColor())));

                        int fogColor = effects.getFogColor();
                        lines.add(Component.translatable("ami.tooltip.fog_color")
                                .append(colorSwatch(fogColor)));

                        String foliageStr = entry.meta(SearchNodeKeys.FOLIAGE_COLOR, "");
                        if (!foliageStr.isBlank()) {
                            lines.add(Component.translatable("ami.tooltip.foliage_color")
                                    .append(colorSwatch(Integer.parseInt(foliageStr))));
                        }
                        String grassStr = entry.meta(SearchNodeKeys.GRASS_COLOR, "");
                        if (!grassStr.isBlank()) {
                            lines.add(Component.translatable("ami.tooltip.grass_color")
                                    .append(colorSwatch(Integer.parseInt(grassStr))));
                        }
                        String tempMod = entry.meta(SearchNodeKeys.TEMPERATURE_MODIFIER, "");
                        if ("frozen".equals(tempMod)) {
                            lines.add(Component.translatable("ami.tooltip.temperature_modifier")
                                    .append(Component.literal(": ")
                                            .append(Component.translatable("ami.temperature_modifier.frozen"))
                                            .withStyle(s -> s.withColor(AMITheme.ACCENT_BLUE))));
                        }

                        // Structures that generate in this biome
                        var structuresHere = getBiomeStructureMap().get(entry.id());
                        if (structuresHere != null && !structuresHere.isEmpty()) {
                            List<String> sorted = structuresHere.stream().sorted().toList();
                            int maxShow = 8;
                            int total = sorted.size();
                            lines.add(Component.empty());
                            lines.add(Component.translatable("ami.tooltip.structures_here",
                                    total).withStyle(s -> s.withColor(AMITheme.ACCENT_BLUE)));
                            for (int i = 0; i < Math.min(total, maxShow); i++) {
                                String structId = sorted.get(i);
                                ResourceLocation sl = ResourceLocation.tryParse(structId);
                                String display = sl != null
                                        ? com.sanhiruzu.ami.index.providers.RegistryUtils.formatPath(sl.getPath())
                                        : structId;
                                lines.add(Component.translatable("ami.tooltip.tag_prefix", display)
                                        .withStyle(s -> s.withColor(AMITheme.POSITIVE)));
                            }
                            if (total > maxShow) {
                                lines.add(Component.translatable("ami.tooltip.more_entries", total - maxShow)
                                        .withStyle(s -> s.withColor(AMITheme.TEXT_SUBTLE)));
                            }
                        }

                        var tags = holder.tags()
                                .sorted((a, b) -> {
                                    boolean aIs = a.location().getPath().startsWith("is_");
                                    boolean bIs = b.location().getPath().startsWith("is_");
                                    if (aIs != bIs) return aIs ? -1 : 1;
                                    return a.location().toString().compareTo(b.location().toString());
                                })
                                .limit(6)
                                .toList();
                        if (!tags.isEmpty()) {
                            lines.add(Component.translatable("ami.tooltip.tags")
                                    .append(Component.literal(":").withStyle(s -> s.withColor(AMITheme.TEXT_SUBTLE))));
                            for (var tag : tags) {
                                lines.add(Component.translatable("ami.tooltip.tag_prefix", tag.location().toString())
                                        .withStyle(s -> s.withColor(AMITheme.POSITIVE)));
                            }
                        }
                    } else {
                        addShiftHint(lines);
                    }
                })
        );
    }

    private static float parseFloat(String s) {
        try {
            return Float.parseFloat(s);
        } catch (NumberFormatException e) {
            return 0f;
        }
    }

    private static void appendStructure(List<Component> lines, SearchNode entry) {
        var mc = Minecraft.getInstance();
        if (mc.level == null) {
            addShiftHint(lines);
            return;
        }

        var key = ResourceKey.create(Registries.STRUCTURE, entry.id());
        mc.level.registryAccess().registry(Registries.STRUCTURE).ifPresent(reg ->
                reg.getHolder(key).ifPresent(holder -> {
                    if (Screen.hasShiftDown()) {
                        var tags = holder.tags().limit(6).toList();
                        if (!tags.isEmpty()) {
                            lines.add(Component.translatable("ami.tooltip.tags")
                                    .append(Component.literal(":").withStyle(s -> s.withColor(AMITheme.TEXT_SUBTLE))));
                            for (var tag : tags) {
                                lines.add(Component.translatable("ami.tooltip.tag_prefix", tag.location().toString())
                                        .withStyle(s -> s.withColor(AMITheme.POSITIVE)));
                            }
                        }
                    } else {
                        addShiftHint(lines);
                    }
                })
        );
    }

    private static void appendDimension(List<Component> lines, SearchNode entry) {
        String dim = entry.id().toString();
        String label = switch (dim) {
            case "minecraft:overworld" -> Component.translatable("ami.dimension.overworld").getString();
            case "minecraft:the_nether" -> Component.translatable("ami.dimension.nether").getString();
            case "minecraft:the_end" -> Component.translatable("ami.dimension.end").getString();
            default -> null;
        };
        if (label != null) {
            lines.add(Component.literal(label).withStyle(s -> s.withColor(AMITheme.TEXT_SUBTLE)));
        }
        if (!Screen.hasShiftDown()) {
            addShiftHint(lines);
        }
    }

    private static void addShiftHint(List<Component> lines) {
        lines.add(Component.translatable("ami.tooltip.shift_for_details")
                .withStyle(s -> s.withColor(AMITheme.TEXT_SUBTLE)));
    }

    private static MutableComponent colorSwatch(int rgb) {
        int opaque = 0xFF000000 | rgb;
        return Component.literal(" #" + String.format("%06X", rgb & 0xFFFFFF))
                .withStyle(s -> s.withColor(opaque));
    }

    private static int tempColor(float temp) {
        if (temp <= 0.0f) return AMITheme.ACCENT_BLUE;
        if (temp < 0.3f) return AMITheme.TEMP_COOL;
        if (temp < 0.6f) return AMITheme.POSITIVE;
        if (temp < 1.0f) return AMITheme.TEMP_WARM;
        return AMITheme.TEMP_HOT;
    }
}
