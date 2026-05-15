package com.sanhiruzu.ami.index.providers;

import com.sanhiruzu.ami.index.SearchNode;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.MobCategory;
import net.neoforged.fml.ModList;

import java.util.Arrays;
import java.util.Comparator;
import java.util.stream.Collectors;

/**
 * Shared utilities for all data providers.
 */
public final class RegistryUtils {
    private RegistryUtils() {}

    /**
     * Vanilla-first comparator matching the former WorldAtlasIndexer.ENTRY_ORDER.
     * Sorts: minecraft namespace first, then alphabetically by namespace, then by displayName.
     */
    public static final Comparator<SearchNode> ENTRY_ORDER =
            Comparator.comparing((SearchNode n) -> n.id().getNamespace().equals("minecraft") ? 0 : 1)
                      .thenComparing(n -> n.id().getNamespace())
                      .thenComparing(SearchNode::displayName);

    /**
     * "dark_forest" → "Dark Forest"
     */
    public static String formatPath(String path) {
        return Arrays.stream(path.split("_"))
                .map(w -> w.isEmpty() ? w : Character.toUpperCase(w.charAt(0)) + w.substring(1))
                .collect(Collectors.joining(" "));
    }

    /**
     * Namespace → human-readable mod name, falling back to formatPath(namespace).
     */
    public static String modDisplayName(String namespace) {
        return ModList.get().getModContainerById(namespace)
                .map(mc -> mc.getModInfo().getDisplayName())
                .orElse(formatPath(namespace));
    }

    public static int namespaceColor(String namespace) {
        int hash = namespace.hashCode();
        int r = 128 + ((hash >> 16) & 0x7F);
        int g = 128 + ((hash >> 8)  & 0x7F);
        int b = 128 + (hash         & 0x7F);
        return 0xFF000000 | (r << 16) | (g << 8) | b;
    }

    public static int categoryColor(MobCategory category) {
        return switch (category) {
            case MONSTER -> 0xFFCC4444;
            case CREATURE -> 0xFF44AA44;
            case AMBIENT -> 0xFFAAAA44;
            case WATER_CREATURE, WATER_AMBIENT, UNDERGROUND_WATER_CREATURE -> 0xFF4488CC;
            default -> 0xFF888888;
        };
    }

    public static int dimensionColor(ResourceLocation id) {
        return switch (id.toString()) {
            case "minecraft:overworld" -> 0xFF66BB6A;
            case "minecraft:the_nether" -> 0xFFCC4444;
            case "minecraft:the_end" -> 0xFF7A51A6;
            default -> namespaceColor(id.getNamespace());
        };
    }
}
