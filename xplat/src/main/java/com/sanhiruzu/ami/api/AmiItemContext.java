package com.sanhiruzu.ami.api;

import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

import java.util.Map;
import java.util.function.Consumer;

/**
 * Stable item-result context passed to AMI plugin menu contributors.
 *
 * <p>Most fields are self-explanatory. Two deserve special attention:
 * <ul>
 *   <li>{@link #cheatEnabled()} — gate cheat/give actions behind this so they
 *       appear and disappear consistently with AMI's own cheat-mode actions.</li>
 *   <li>{@link #tokenInject()} — use this to build "filter by X" actions that
 *       narrow the search results in place. Call {@code tokenInject().accept("@mymod")}
 *       or {@code tokenInject().accept("?type:fire")} and AMI will rewrite the
 *       search bar and rerun the query. May be {@code null} if the current panel
 *       does not support search injection; always null-check before using.</li>
 * </ul>
 */
public record AmiItemContext(
        ResourceLocation id,
        String type,
        Component displayName,
        ItemStack stack,
        Map<String, String> metadata,
        boolean cheatEnabled,
        Consumer<String> tokenInject
) {
    public AmiItemContext {
        stack = stack == null ? ItemStack.EMPTY : stack.copy();
        metadata = metadata == null ? Map.of() : Map.copyOf(metadata);
    }

    public boolean isItem() {
        return "ITEM".equals(type);
    }

    public String meta(String key, String fallback) {
        if (key == null) {
            return fallback;
        }
        return metadata.getOrDefault(key, fallback);
    }
}
