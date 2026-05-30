package com.sanhiruzu.ami.client.favorites;

import com.sanhiruzu.ami.client.icon.ItemIconRenderer;
import com.sanhiruzu.ami.index.NodeType;
import com.sanhiruzu.ami.index.SearchNode;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.LinkedHashMap;
import java.util.Map;

public record FavoriteEntry(String key, ResourceLocation itemId, ItemStack stack, ResourceLocation recipeId, String source) {
    public static final String META_KIND = "ami.favorite.kind";
    public static final String META_BASE_ID = "ami.favorite.base_id";
    public static final String META_RECIPE_ID = "ami.favorite.recipe_id";
    public static final String META_SOURCE = "ami.favorite.source";
    private static final String LOCAL_SOURCE = "ami";

    public static FavoriteEntry item(ItemStack stack, String source) {
        return create(stack, null, source);
    }

    public static FavoriteEntry recipe(ItemStack stack, ResourceLocation recipeId, String source) {
        return create(stack, recipeId, source);
    }

    public static FavoriteEntry localItem(ItemStack stack) {
        return item(stack, LOCAL_SOURCE);
    }

    public static FavoriteEntry localRecipe(ItemStack stack, ResourceLocation recipeId) {
        return recipe(stack, recipeId, LOCAL_SOURCE);
    }

    private static FavoriteEntry create(ItemStack stack, ResourceLocation recipeId, String source) {
        if (stack == null || stack.isEmpty()) {
            return null;
        }
        ResourceLocation itemId = BuiltInRegistries.ITEM.getKey(stack.getItem());
        if (itemId == null) {
            return null;
        }
        String stackKey = stackIdentity(stack);
        String key = recipeId == null ? "item|" + stackKey : "recipe|" + recipeId + "|" + stackKey;
        return new FavoriteEntry(key, itemId, stack.copy(), recipeId, source == null ? "" : source);
    }

    public boolean isRecipeFavorite() {
        return recipeId != null;
    }

    public SearchNode toNode() {
        ResourceLocation nodeId = nodeId();
        ItemIconRenderer.registerStack(nodeId, stack);

        Map<String, String> metadata = new LinkedHashMap<>();
        metadata.put(META_KIND, isRecipeFavorite() ? "recipe" : "item");
        metadata.put(META_BASE_ID, itemId.toString());
        metadata.put(META_SOURCE, source);
        if (recipeId != null) {
            metadata.put(META_RECIPE_ID, recipeId.toString());
        }

        return new SearchNode(nodeId, NodeType.ITEM, stack.getHoverName().getString(), 0, 0, metadata);
    }

    public ResourceLocation nodeId() {
        String path = "favorite/" + (isRecipeFavorite() ? "recipe/" : "item/") + sha1(key);
        ResourceLocation id = ResourceLocation.tryParse("ami:" + path);
        return id == null ? itemId : id;
    }

    public static String stackKey(ItemStack stack) {
        return stack == null || stack.isEmpty() ? "" : stackIdentity(stack);
    }

    private static String stackIdentity(ItemStack stack) {
        ResourceLocation itemId = BuiltInRegistries.ITEM.getKey(stack.getItem());
        StringBuilder sb = new StringBuilder(itemId == null ? "unknown" : itemId.toString());
        if (Boolean.TRUE.equals(invokeNoArg(stack, "isDamageableItem"))) {
            Object damage = invokeNoArg(stack, "getDamageValue");
            if (damage != null) {
                sb.append("|damage=").append(damage);
            }
        }
        sb.append("|data=").append(stackData(stack));
        return sb.toString();
    }

    private static String stackData(ItemStack stack) {
        Object result = invokeNoArg(stack, "getComponentsPatch");
        if (result == null) {
            result = invokeNoArg(stack, "getComponents");
        }
        if (result == null) {
            result = invokeNoArg(stack, "getTag");
        }
        return result == null ? "" : result.toString();
    }

    private static Object invokeNoArg(Object target, String methodName) {
        try {
            Method method = target.getClass().getMethod(methodName);
            method.setAccessible(true);
            return method.invoke(target);
        } catch (ReflectiveOperationException | RuntimeException ignored) {
            return null;
        }
    }

    private static String sha1(String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-1");
            byte[] bytes = digest.digest(value.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder(bytes.length * 2);
            for (byte b : bytes) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (NoSuchAlgorithmException e) {
            return Integer.toHexString(value.hashCode());
        }
    }
}
