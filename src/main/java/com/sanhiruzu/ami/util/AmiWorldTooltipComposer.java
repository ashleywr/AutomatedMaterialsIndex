package com.sanhiruzu.ami.util;

import com.sanhiruzu.ami.client.AMITheme;
import com.sanhiruzu.ami.index.SearchNode;
import com.sanhiruzu.ami.index.SearchNodeKeys;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.BiomeTags;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.BiomeSpecialEffects;
import net.minecraft.world.level.levelgen.structure.Structure;

import java.util.ArrayList;
import java.util.List;

/**
 * Shared tooltip builder for world entries (biomes, dimensions, structures).
 * Keeps the text path consistent across the results panel and palette views.
 */
public final class AmiWorldTooltipComposer {
    private AmiWorldTooltipComposer() {}

    public static List<Component> build(SearchNode entry) {
        List<Component> lines = new ArrayList<>();
        lines.add(Component.literal(entry.displayName()));
        lines.add(Component.translatable(typeKey(entry))
                .withStyle(s -> s.withColor(AMITheme.TEXT_SUBTLE)));
        lines.add(Component.literal(entry.id().toString())
                .withStyle(s -> s.withColor(AMITheme.TEXT_SUBTLE)));

        switch (entry.type()) {
            case BIOME -> appendBiome(lines, entry);
            case STRUCTURE -> appendStructure(lines, entry);
            case DIMENSION -> appendDimension(lines, entry);
            default -> {}
        }

        return lines;
    }

    private static void appendBiome(List<Component> lines, SearchNode entry) {
        var mc = Minecraft.getInstance();
        if (mc.level == null) {
            addShiftHint(lines);
            return;
        }

        var biomeKey = ResourceKey.create(Registries.BIOME, entry.id());
        mc.level.registryAccess().registry(Registries.BIOME).ifPresent(reg ->
            reg.getHolder(biomeKey).ifPresent(holder -> {
                Biome biome = holder.value();
                BiomeSpecialEffects effects = biome.getSpecialEffects();
                float temp = biome.getBaseTemperature();
                Component precip = !biome.hasPrecipitation()
                        ? Component.translatable("ami.precipitation.none")
                        : temp < 0.15f ? Component.translatable("ami.precipitation.snow")
                        : Component.translatable("ami.precipitation.rain");

                lines.add(Component.translatable("ami.tooltip.temperature")
                        .append(Component.literal(": " + String.format(java.util.Locale.ROOT, "%.2f", temp))
                                .withStyle(s -> s.withColor(tempColor(temp)))));
                lines.add(Component.translatable("ami.tooltip.precipitation")
                        .append(Component.literal(": ").append(precip)
                                .withStyle(s -> s.withColor(AMITheme.ACCENT_BLUE))));
                lines.add(Component.translatable("ami.tooltip.water_color")
                        .append(colorSwatch(effects.getWaterColor())));

                if (Screen.hasShiftDown()) {
                    lines.add(Component.translatable("ami.tooltip.sky_color")
                            .append(colorSwatch(effects.getSkyColor())));

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
                            lines.add(Component.translatable("ami.tooltip.tag_prefix", tag.location())
                                    .withStyle(s -> s.withColor(AMITheme.POSITIVE)));
                        }
                    }
                } else {
                    addShiftHint(lines);
                }
            })
        );
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
                            lines.add(Component.translatable("ami.tooltip.tag_prefix", tag.location())
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

    private static String typeKey(SearchNode entry) {
        return switch (entry.type()) {
            case BIOME -> "ami.tooltip.biome";
            case STRUCTURE -> "ami.tooltip.structure";
            case DIMENSION -> "ami.tooltip.dimension";
            default -> "ami.tooltip.world";
        };
    }
}
