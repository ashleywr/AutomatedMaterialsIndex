package com.sanhiruzu.ami.client.favorites;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.sanhiruzu.ami.AMI;
import com.sanhiruzu.ami.client.icon.ItemIconRenderer;
import com.sanhiruzu.ami.index.GlobalIndex;
import com.sanhiruzu.ami.index.NodeType;
import com.sanhiruzu.ami.index.SearchNode;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.neoforged.fml.ModList;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Handles favorite synchronization between AMI and other recipe viewers (primarily EMI).
 * Maintains a local list of favorite IDs to ensure AMI-specific nodes (like entities/biomes)
 * can be favorited even if the external viewer doesn't support them.
 */
public class AmiFavoritesHandler {
    private static final AmiFavoritesHandler INSTANCE = new AmiFavoritesHandler();
    private static Path favoritesFile;

    private static Path getFavoritesFile() {
        if (favoritesFile == null) {
            try {
                Class<?> fmlPaths = Class.forName("net.neoforged.fml.loading.FMLPaths");
                Object configDir = fmlPaths.getField("CONFIGDIR").get(null);
                Path base = (Path) configDir.getClass().getMethod("get").invoke(configDir);
                favoritesFile = base.resolve("ami_favorites.json");
            } catch (Throwable e) {
                favoritesFile = Path.of("config", "ami_favorites.json");
            }
        }
        return favoritesFile;
    }

    private final Set<ResourceLocation> localFavorites = new HashSet<>();
    private boolean loaded;
    private Runnable onChange;

    private AmiFavoritesHandler() {}

    public static AmiFavoritesHandler getInstance() {
        INSTANCE.load();
        return INSTANCE;
    }

    public void setOnChange(Runnable onChange) {
        this.onChange = onChange;
    }

    private void notifyChange() {
        if (onChange != null) onChange.run();
    }

    private void load() {
        if (loaded) return;
        loaded = true;
        Path file = getFavoritesFile();
        if (file == null || !Files.exists(file)) return;
        try {
            JsonObject root = JsonParser.parseString(Files.readString(file, StandardCharsets.UTF_8)).getAsJsonObject();
            if (root.has("favorites")) {
                for (JsonElement e : root.getAsJsonArray("favorites")) {
                    localFavorites.add(ResourceLocation.parse(e.getAsString()));
                }
            }
        } catch (Exception e) {
            AMI.LOGGER.warn("AMI: Failed to load favorites: {}", e.getMessage());
        }
    }

    private void save() {
        Path file = getFavoritesFile();
        if (file == null) return;
        try {
            Files.createDirectories(file.getParent());
            JsonObject root = new JsonObject();
            JsonArray arr = new JsonArray();
            for (ResourceLocation id : localFavorites) {
                arr.add(id.toString());
            }
            root.add("favorites", arr);
            Files.writeString(file, root.toString(), StandardCharsets.UTF_8);
        } catch (Exception e) {
            AMI.LOGGER.warn("AMI: Failed to save favorites: {}", e.getMessage());
        }
    }

    public void toggleFavorite(SearchNode node) {
        if (node == null) return;
        if (isFavorite(node)) {
            removeFavorite(node);
        } else {
            addFavorite(node);
        }
        notifyChange();
    }

    public boolean isFavorite(SearchNode node) {
        if (localFavorites.contains(node.id())) return true;
        
        if (ModList.get().isLoaded("emi")) {
            return isEmiFavorite(node);
        }
        return false;
    }

    private boolean isEmiFavorite(SearchNode node) {
        try {
            Class<?> bridgeClass = Class.forName("com.sanhiruzu.ami.compat.EmiFavoritesBridge");
            Object result = bridgeClass.getMethod("isFavorite", ResourceLocation.class)
                .invoke(null, node.id());
            return (boolean) result;
        } catch (Exception e) {
            return false;
        }
    }

    public static ItemStack resolveStack(SearchNode node) {
        ItemStack stack = ItemIconRenderer.resolveStack(node.id());
        if (stack.isEmpty() && node.type() == NodeType.ENTITY) {
            ResourceLocation eggId = ResourceLocation.withDefaultNamespace(node.id().getPath() + "_spawn_egg");
            stack = BuiltInRegistries.ITEM.getOptional(eggId).map(ItemStack::new).orElse(ItemStack.EMPTY);
        }
        return stack;
    }

    public void addFavorite(SearchNode node) {
        localFavorites.add(node.id());

        if (ModList.get().isLoaded("emi") && node.type() == NodeType.ITEM) {
            addFavorite(resolveStack(node));
        }
        save();
        notifyChange();
    }

    public void addFavorite(ItemStack stack) {
        if (stack == null || stack.isEmpty()) return;

        ResourceLocation id = BuiltInRegistries.ITEM.getKey(stack.getItem());
        localFavorites.add(id);

        if (ModList.get().isLoaded("emi")) {
            callEmiVoidMethod("addFavorite", ItemStack.class, stack);
        }
        save();
        notifyChange();
    }

    public void addFavoriteAt(ItemStack stack, int index) {
        if (stack == null || stack.isEmpty()) return;

        ResourceLocation id = BuiltInRegistries.ITEM.getKey(stack.getItem());
        localFavorites.add(id);

        if (ModList.get().isLoaded("emi")) {
            callEmiVoidMethod("addFavoriteAt", new Class<?>[] {ItemStack.class, int.class}, stack, index);
        }
        save();
        notifyChange();
    }

    public void removeFavorite(SearchNode node) {
        localFavorites.remove(node.id());

        if (ModList.get().isLoaded("emi")) {
            removeFavorite(resolveStack(node));
        }
        save();
        notifyChange();
    }

    public void removeFavorite(ItemStack stack) {
        if (stack == null || stack.isEmpty()) return;

        ResourceLocation id = BuiltInRegistries.ITEM.getKey(stack.getItem());
        localFavorites.remove(id);

        if (ModList.get().isLoaded("emi")) {
            callEmiVoidMethod("removeFavorite", ItemStack.class, stack);
        }
        save();
        notifyChange();
    }

    public List<SearchNode> getFavorites() {
        List<SearchNode> result = new ArrayList<>();
        Set<ResourceLocation> seen = new HashSet<>();

        // 1. Add EMI favorites first to maintain their order
        if (ModList.get().isLoaded("emi")) {
            Collection<ResourceLocation> emiFavorites = getEmiFavoriteIds();
            for (ResourceLocation id : emiFavorites) {
                GlobalIndex.getInstance().getNode(id).ifPresent(node -> {
                    result.add(node);
                    seen.add(id);
                });
            }
        }

        // 2. Add local favorites that weren't in EMI (e.g. entities, biomes)
        for (ResourceLocation id : localFavorites) {
            if (!seen.contains(id)) {
                GlobalIndex.getInstance().getNode(id).ifPresent(result::add);
            }
        }

        return result;
    }

    @SuppressWarnings("unchecked")
    private Collection<ResourceLocation> getEmiFavoriteIds() {
        try {
            Class<?> bridgeClass = Class.forName("com.sanhiruzu.ami.compat.EmiFavoritesBridge");
            Object result = bridgeClass.getMethod("getFavoriteIds").invoke(null);
            return (Collection<ResourceLocation>) result;
        } catch (Exception e) {
            return new ArrayList<>();
        }
    }

    private void callEmiVoidMethod(String methodName, Class<?> paramType, Object param) {
        try {
            Class<?> bridgeClass = Class.forName("com.sanhiruzu.ami.compat.EmiFavoritesBridge");
            bridgeClass.getMethod(methodName, paramType).invoke(null, param);
        } catch (Exception e) {
            // EMI integration silently fails if the bridge class isn't available
        }
    }

    private void callEmiVoidMethod(String methodName, Class<?>[] paramTypes, Object... params) {
        try {
            Class<?> bridgeClass = Class.forName("com.sanhiruzu.ami.compat.EmiFavoritesBridge");
            bridgeClass.getMethod(methodName, paramTypes).invoke(null, params);
        } catch (Exception e) {
            // EMI integration silently fails if the bridge class isn't available
        }
    }
}
