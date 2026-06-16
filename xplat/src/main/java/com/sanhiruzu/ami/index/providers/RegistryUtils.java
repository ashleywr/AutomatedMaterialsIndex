package com.sanhiruzu.ami.index.providers;

import com.sanhiruzu.ami.index.SearchNode;
import com.sanhiruzu.ami.util.AmiColors;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.item.ItemStack;

import java.util.Arrays;
import java.util.Comparator;
import java.util.stream.Collectors;

/**
 * Shared utilities for all data providers.
 */
public final class RegistryUtils {
    /**
     * Vanilla-first comparator matching the former WorldAtlasIndexer.ENTRY_ORDER.
     * Sorts: minecraft namespace first, then alphabetically by namespace, then by displayName.
     */
    public static final Comparator<SearchNode> ENTRY_ORDER =
            Comparator.comparing((SearchNode n) -> n.id().getNamespace().equals("minecraft") ? 0 : 1)
                    .thenComparing(n -> n.id().getNamespace())
                    .thenComparing(SearchNode::displayName);

    private RegistryUtils() {
    }

    /**
     * "dark_forest" → "Dark Forest"
     */
    public static String formatPath(String path) {
        return Arrays.stream(path.split("_"))
                .map(w -> w.isEmpty() ? w : Character.toUpperCase(w.charAt(0)) + w.substring(1))
                .collect(Collectors.joining(" "));
    }

    /**
     * "dark_forest" + "Biome" -> "Dark Forest Biome"
     */
    public static String formatPathWithSuffix(String path, String suffix) {
        return formatPath(path) + " " + suffix;
    }

    /**
     * Namespace → human-readable mod name, falling back to formatPath(namespace).
     */
    public static String modDisplayName(String namespace) {
        try {
            return com.sanhiruzu.ami.platform.Services.PLATFORM.getModName(namespace)
                    .orElse(formatPath(namespace));
        } catch (LinkageError | RuntimeException ignored) {
            return formatPath(namespace);
        }
    }

    public static int namespaceColor(String namespace) {
        int hash = namespace.hashCode();
        int r = 128 + ((hash >> 16) & 0x7F);
        int g = 128 + ((hash >> 8) & 0x7F);
        int b = 128 + (hash & 0x7F);
        return 0xFF000000 | (r << 16) | (g << 8) | b;
    }

    public static int categoryColor(MobCategory category) {
        return switch (category) {
            case MONSTER -> AmiColors.CATEGORY_MONSTER;
            case CREATURE -> AmiColors.CATEGORY_CREATURE;
            case AMBIENT -> AmiColors.CATEGORY_AMBIENT;
            case WATER_CREATURE, WATER_AMBIENT, UNDERGROUND_WATER_CREATURE -> AmiColors.CATEGORY_AQUATIC;
            default -> AmiColors.CATEGORY_DEFAULT;
        };
    }

    public static int dimensionColor(Identifier id) {
        return switch (id.toString()) {
            case "minecraft:overworld" -> AmiColors.DIM_OVERWORLD;
            case "minecraft:the_nether" -> AmiColors.DIM_NETHER;
            case "minecraft:the_end" -> AmiColors.DIM_END;
            default -> namespaceColor(id.getNamespace());
        };
    }

    public static int getAverageColor(ItemStack stack) {
        if (stack.isEmpty()) return 0xFF808080;
        Identifier id = BuiltInRegistries.ITEM.getKey(stack.getItem());
        int hash = id != null ? id.toString().hashCode() : stack.getHoverName().getString().hashCode();
        int r = 64 + ((hash >> 16) & 0xBF);
        int g = 64 + ((hash >> 8) & 0xBF);
        int b = 64 + (hash & 0xBF);
        return 0xFF000000 | (r << 16) | (g << 8) | b;
    }
}
