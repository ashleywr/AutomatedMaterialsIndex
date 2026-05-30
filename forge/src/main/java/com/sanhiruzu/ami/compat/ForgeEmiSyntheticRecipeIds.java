package com.sanhiruzu.ami.compat;

import dev.emi.emi.EmiPort;
import net.minecraft.resources.ResourceLocation;

public final class ForgeEmiSyntheticRecipeIds {
    private ForgeEmiSyntheticRecipeIds() {
    }

    public static ResourceLocation normalize(String id) {
        ResourceLocation parsed = EmiPort.id(id);
        String normalizedPath = normalizePath(parsed.getNamespace(), parsed.getPath());
        if (normalizedPath.equals(parsed.getPath())) {
            return parsed;
        }
        return EmiPort.id(parsed.getNamespace(), normalizedPath);
    }

    public static String normalizePath(String namespace, String path) {
        if ("emi".equals(namespace) && (path.startsWith("brewing/item/") || path.startsWith("brewing/forge/"))) {
            return "/" + path;
        }
        return path;
    }
}
