package com.sanhiruzu.ami.emi;

import java.util.*;
import java.util.stream.Collectors;

import dev.emi.emi.api.EmiEntrypoint;
import dev.emi.emi.api.EmiPlugin;
import dev.emi.emi.api.recipe.EmiRecipe;
import dev.emi.emi.api.recipe.EmiRecipeCategory;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

/**
 * Bridge that loads EMI plugins into AMI's capture system when EMI is absent.
 */
public final class AmiEmiBridge {
    private static boolean loaded;
    private static boolean realEmiPresent;

    private static final Map<ResourceLocation, EmiRecipeCategory> categories = new LinkedHashMap<>();
    private static final Map<ResourceLocation, List<AmiCapturedRecipe>> recipesByCategory = new LinkedHashMap<>();

    // Lookup maps
    private static final Map<Item, List<AmiCapturedRecipe>> recipesByOutput = new HashMap<>();
    private static final Map<Item, List<AmiCapturedRecipe>> recipesByInput = new HashMap<>();

    private AmiEmiBridge() {}

    public static boolean isRealEmiLoaded() {
        if (!loaded) detectEmi();
        return realEmiPresent;
    }

    private static void detectEmi() {
        try {
            Class.forName("dev.emi.emi.EmiPort");
            realEmiPresent = true;
        } catch (ClassNotFoundException e) {
            realEmiPresent = false;
        }
        loaded = true;
    }

    public static void loadPlugins() {
        if (isRealEmiLoaded()) return;
        if (!loaded) loaded = true;
        if (!categories.isEmpty()) return; // already loaded

        List<Class<?>> pluginClasses = discoverPlugins();
        if (pluginClasses.isEmpty()) return;

        AmiCaptureRegistry registry = new AmiCaptureRegistry();

        for (Class<?> clazz : pluginClasses) {
            try {
                Object instance = clazz.getDeclaredConstructor().newInstance();
                if (instance instanceof EmiPlugin plugin) {
                    plugin.register(registry);
                }
            } catch (Exception e) {
                com.sanhiruzu.ami.AMI.LOGGER.warn("AMI/EMI bridge: failed to load plugin {}", clazz.getName(), e);
            }
        }

        // Process deferred recipes (plugins that depend on other plugins)
        registry.processDeferred();

        // Copy captured data into lookup maps
        Map<ResourceLocation, List<AmiCapturedRecipe>> rbc = registry.getRecipesByCategory();
        recipesByCategory.putAll(rbc);
        categories.putAll(registry.getCategories());

        for (List<AmiCapturedRecipe> list : rbc.values()) {
            for (AmiCapturedRecipe recipe : list) {
                for (AmiCapturedRecipe.CapturedSlot slot : recipe.slots()) {
                    if (slot.isOutput()) {
                        for (ItemStack is : slot.alternatives()) {
                            if (!is.isEmpty()) {
                                recipesByOutput.computeIfAbsent(is.getItem(), k -> new ArrayList<>()).add(recipe);
                            }
                        }
                    } else {
                        for (ItemStack is : slot.alternatives()) {
                            if (!is.isEmpty()) {
                                recipesByInput.computeIfAbsent(is.getItem(), k -> new ArrayList<>()).add(recipe);
                            }
                        }
                    }
                }
            }
        }

        com.sanhiruzu.ami.AMI.LOGGER.info("AMI/EMI bridge: loaded {} plugins, {} categories, {} recipes",
            pluginClasses.size(), categories.size(),
            rbc.values().stream().mapToInt(List::size).sum());
    }

    private static List<Class<?>> discoverPlugins() {
        List<Class<?>> found = new ArrayList<>();
        // Scan for @EmiEntrypoint annotated classes via the mod classloader
        // In NeoForge, we scan the ModList for classes annotated with @EmiEntrypoint
        try {
            var mods = net.neoforged.fml.ModList.get().getAllScanData();
            for (var scanData : mods) {
                for (var ad : scanData.getAnnotations()) {
                    if (ad.annotationType().getClassName().equals("dev.emi.emi.api.EmiEntrypoint")) {
                        try {
                            Class<?> clazz = Class.forName(ad.memberName());
                            if (EmiPlugin.class.isAssignableFrom(clazz)) {
                                found.add(clazz);
                            }
                        } catch (Exception ignored) {}
                    }
                }
            }
        } catch (Exception e) {
            com.sanhiruzu.ami.AMI.LOGGER.debug("AMI/EMI bridge: annotation scanning failed, trying service loader", e);
            try {
                var loader = java.util.ServiceLoader.load(EmiPlugin.class);
                for (EmiPlugin plugin : loader) {
                    found.add(plugin.getClass());
                }
            } catch (Exception ignored) {}
        }
        return found;
    }

    @Nullable
    public static List<AmiCapturedRecipe> getRecipesForOutput(ItemStack stack) {
        if (stack.isEmpty()) return List.of();
        loadPlugins();
        return recipesByOutput.getOrDefault(stack.getItem(), List.of());
    }

    @Nullable
    public static List<AmiCapturedRecipe> getRecipesForInput(ItemStack stack) {
        if (stack.isEmpty()) return List.of();
        loadPlugins();
        return recipesByInput.getOrDefault(stack.getItem(), List.of());
    }

    public static boolean hasCapturedRecipes() {
        loadPlugins();
        return !recipesByCategory.isEmpty();
    }

    public static Map<ResourceLocation, List<AmiCapturedRecipe>> getRecipesByCategory() {
        loadPlugins();
        return Collections.unmodifiableMap(recipesByCategory);
    }

    public static Map<ResourceLocation, EmiRecipeCategory> getCategories() {
        loadPlugins();
        return Collections.unmodifiableMap(categories);
    }
}
