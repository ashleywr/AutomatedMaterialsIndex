package com.sanhiruzu.ami.compat;

import dev.emi.emi.EmiPort;
import net.minecraft.resources.ResourceLocation;

public final class ForgeEmiSyntheticRecipeIds {
    private ForgeEmiSyntheticRecipeIds() {
    }

    public static ResourceLocation normalize(String id) {
        ResourceLocation parsed = EmiPort.id(id);
        String parsedId = parsed.toString();
        int separator = parsedId.indexOf(':');
        String namespace = separator >= 0 ? parsedId.substring(0, separator) : "minecraft";
        String path = separator >= 0 ? parsedId.substring(separator + 1) : parsedId;
        String normalizedPath = normalizePath(namespace, path);
        if (normalizedPath.equals(path)) {
            return parsed;
        }
        return EmiPort.id(namespace, normalizedPath);
    }

    public static String normalizePath(String namespace, String path) {
        if ("emi".equals(namespace) && (path.startsWith("brewing/item/") || path.startsWith("brewing/forge/"))) {
            return "/" + path;
        }
        return path;
    }
}
