package com.sanhiruzu.ami.compat;

import com.sanhiruzu.ami.index.SearchNodeKeys;
import net.minecraft.resources.ResourceLocation;

import java.util.Locale;
import java.util.Map;

public final class SilentGemsCompat {
    private static final String MOD_ID = "silentgems";

    private SilentGemsCompat() {
    }

    public static void enrichItem(ResourceLocation id, Map<String, String> meta) {
        if (id == null || meta == null || !MOD_ID.equals(id.getNamespace())) {
            return;
        }

        GemShape shape = gemShape(id.getPath().toLowerCase(Locale.ROOT));
        if (shape == null) {
            return;
        }

        meta.put(SearchNodeKeys.MATERIAL_GROUP, MOD_ID + ":" + shape.familyKey);
        meta.put(SearchNodeKeys.COLLAPSE_FAMILY, MOD_ID + ":" + shape.familyKey);
        meta.put(SearchNodeKeys.COLLAPSE_LABEL, shape.label);
        meta.put(SearchNodeKeys.VARIANT_COLLAPSE_MODE, "default_collapsed");
        meta.put(SearchNodeKeys.COLOR_BUCKET, gemToken(id.getPath().toLowerCase(Locale.ROOT), shape));
    }

    private static GemShape gemShape(String path) {
        if (path.startsWith("smooth_") && !gemToken(path, GemShape.SMOOTH).isBlank()) return GemShape.SMOOTH;
        if (path.startsWith("polished_") && !gemToken(path, GemShape.POLISHED).isBlank()) return GemShape.POLISHED;
        if (path.endsWith("_small_bricks") && !gemToken(path, GemShape.SMALL_BRICKS).isBlank()) return GemShape.SMALL_BRICKS;
        if (path.endsWith("_bricks") && !gemToken(path, GemShape.BRICKS).isBlank()) return GemShape.BRICKS;
        if (path.endsWith("_tiles") && !gemToken(path, GemShape.TILES).isBlank()) return GemShape.TILES;
        if (path.endsWith("_glass") && !gemToken(path, GemShape.GLASS).isBlank()) return GemShape.GLASS;
        if (path.endsWith("_lamp_inverted_on") && !gemToken(path, GemShape.INVERTED_LAMP).isBlank()) return GemShape.INVERTED_LAMP;
        if (path.endsWith("_lamp") && !gemToken(path, GemShape.LAMP).isBlank()) return GemShape.LAMP;
        if (path.endsWith("_redstone_teleporter") && !gemToken(path, GemShape.REDSTONE_TELEPORTER).isBlank()) {
            return GemShape.REDSTONE_TELEPORTER;
        }
        if (path.endsWith("_teleporter") && !gemToken(path, GemShape.TELEPORTER).isBlank()) return GemShape.TELEPORTER;
        if (path.endsWith("_block") && !gemToken(path, GemShape.GEM_BLOCK).isBlank()) return GemShape.GEM_BLOCK;
        return null;
    }

    private static String gemToken(String path, GemShape shape) {
        String token = path;
        if (!shape.prefix.isBlank()) {
            if (!token.startsWith(shape.prefix)) return "";
            token = token.substring(shape.prefix.length());
        }
        if (!shape.suffix.isBlank()) {
            if (!token.endsWith(shape.suffix)) return "";
            token = token.substring(0, token.length() - shape.suffix.length());
        }
        return isGeneratedGemToken(token) ? token : "";
    }

    private static boolean isGeneratedGemToken(String token) {
        return !token.isBlank()
                && !"chaos_essence".equals(token)
                && !"silver".equals(token)
                && !"teleporter_anchor".equals(token);
    }

    private enum GemShape {
        SMOOTH("smooth_", "", "smooth_stone", "Smooth Gem Stones"),
        POLISHED("polished_", "", "polished_stone", "Polished Gem Stones"),
        BRICKS("", "_bricks", "bricks", "Gem Bricks"),
        SMALL_BRICKS("", "_small_bricks", "small_bricks", "Small Gem Bricks"),
        TILES("", "_tiles", "tiles", "Gem Tiles"),
        GLASS("", "_glass", "glass", "Gem Glass"),
        LAMP("", "_lamp", "lamps", "Gem Lamps"),
        INVERTED_LAMP("", "_lamp_inverted_on", "inverted_lamps", "Inverted Gem Lamps"),
        TELEPORTER("", "_teleporter", "teleporters", "Gem Teleporters"),
        REDSTONE_TELEPORTER("", "_redstone_teleporter", "redstone_teleporters", "Gem Redstone Teleporters"),
        GEM_BLOCK("", "_block", "gem_blocks", "Blocks of Gems");

        final String prefix;
        final String suffix;
        final String familyKey;
        final String label;

        GemShape(String prefix, String suffix, String familyKey, String label) {
            this.prefix = prefix;
            this.suffix = suffix;
            this.familyKey = familyKey;
            this.label = label;
        }
    }
}
