package com.sanhiruzu.ami.api;

import com.sanhiruzu.ami.AmiCore;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public final class ItemProviderCompatHooks {
    private static final Set<String> DISABLED_COMPATS = Collections.synchronizedSet(new HashSet<>());

    private ItemProviderCompatHooks() {
    }

    public static void clearDisabledCompatHooks() {
        DISABLED_COMPATS.clear();
    }

    public static Set<String> getDisabledCompatHooks() {
        return Set.copyOf(DISABLED_COMPATS);
    }

    public static void runCompatSafely(String compatName, Runnable compatAction) {
        if (compatName == null || compatName.isBlank()) {
            return;
        }
        if (DISABLED_COMPATS.contains(compatName)) {
            return;
        }
        try {
            compatAction.run();
        } catch (Throwable t) {
            DISABLED_COMPATS.add(compatName);
            AmiCore.LOGGER.warn("AMI compat '{}' failed and has been disabled for the remainder of this index pass.", compatName, t);
        }
    }

    public static void runPluginItemCompatHooks(ResourceLocation id, ItemStack stack, @Nullable Level level,
                                         Map<String, String> metadata) {
        if (id == null || stack == null || metadata == null) {
            return;
        }
        for (IAmiPlugin plugin : AmiPluginRegistry.getPlugins()) {
            String compatName = "IAmiPlugin." + plugin.getClass().getName();
            runCompatSafely(compatName, () -> plugin.enrichItemMeta(id, stack, level, metadata));
        }
    }
}
