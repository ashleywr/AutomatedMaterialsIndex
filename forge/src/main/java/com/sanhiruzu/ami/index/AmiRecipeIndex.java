package com.sanhiruzu.ami.index;

import com.sanhiruzu.ami.forge.AMI;
import com.sanhiruzu.ami.compat.EmiIntegrationLoader;
import com.sanhiruzu.ami.util.AmiRecipeHolder;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.TieredItem;
import net.minecraft.world.item.PotionItem;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.alchemy.Potion;
import net.minecraft.world.item.alchemy.PotionUtils;
import net.minecraft.world.item.alchemy.PotionBrewing;
import net.minecraft.world.item.alchemy.Potions;
import net.minecraft.world.level.Level;
import net.minecraftforge.common.brewing.BrewingRecipe;
import net.minecraftforge.common.brewing.BrewingRecipeRegistry;
import net.minecraftforge.common.brewing.IBrewingRecipe;
import net.minecraftforge.common.brewing.VanillaBrewingRecipe;
import net.minecraftforge.registries.ForgeRegistries;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public final class AmiRecipeIndex {
    private static final AmiRecipeIndex INSTANCE = new AmiRecipeIndex();

    public static final RecipeType<com.sanhiruzu.ami.index.special.PotionBrewingRecipe> BREWING = new RecipeType<>() {
        @Override
        public String toString() { return "ami:brewing"; }
    };
    public static final RecipeType<com.sanhiruzu.ami.index.special.GrindstoneRepairRecipe> GRINDING = new RecipeType<>() {
        @Override
        public String toString() { return "ami:grinding"; }
    };
    public static final RecipeType<com.sanhiruzu.ami.index.special.AnvilRepairRecipe> ANVIL_REPAIRING = new RecipeType<>() {
        @Override
        public String toString() { return "ami:anvil_repairing"; }
    };
    public static final RecipeType<com.sanhiruzu.ami.index.special.CompostingRecipe> COMPOSTING = new RecipeType<>() {
        @Override
        public String toString() { return "ami:composting"; }
    };
    public static final RecipeType<com.sanhiruzu.ami.index.special.FuelRecipe> FUEL = new RecipeType<>() {
        @Override
        public String toString() { return "ami:fuel"; }
    };

    private static final Map<ResourceLocation, RecipeType<?>> EMI_CATEGORY_TYPES = new LinkedHashMap<>();
    private static final Map<RecipeType<?>, String> EMI_CATEGORY_NAMES = new HashMap<>();
    private static final Map<RecipeType<?>, ItemStack> EMI_CATEGORY_ICONS = new HashMap<>();

    public static RecipeType<?> getEmiCategoryType(ResourceLocation categoryId, String categoryName) {
        return EMI_CATEGORY_TYPES.computeIfAbsent(categoryId, id -> {
            var type = new RecipeType<>() {
                @Override
                public String toString() { return "emi:" + id.toString(); }
            };
            EMI_CATEGORY_NAMES.put(type, categoryName);
            return type;
        });
    }

    public static void setEmiCategoryIcon(RecipeType<?> type, ItemStack icon) {
        if (!icon.isEmpty()) EMI_CATEGORY_ICONS.put(type, icon.copy());
    }

    public static boolean isEmiCategoryType(RecipeType<?> type) {
        return EMI_CATEGORY_NAMES.containsKey(type);
    }

    public static String getEmiCategoryName(RecipeType<?> type) {
        return EMI_CATEGORY_NAMES.get(type);
    }

    public static ItemStack getEmiCategoryIcon(RecipeType<?> type) {
        return EMI_CATEGORY_ICONS.getOrDefault(type, ItemStack.EMPTY);
    }

    private final ConcurrentMap<Item, List<AmiRecipeHolder<?>>> recipesByOutput = new ConcurrentHashMap<>();
    private final ConcurrentMap<Item, List<AmiRecipeHolder<?>>> recipesByInput = new ConcurrentHashMap<>();
    private volatile boolean built;

    private AmiRecipeIndex() {}

    public static AmiRecipeIndex getInstance() { return INSTANCE; }

    public boolean isBuilt() { return built; }

    public void rebuild(@Nullable Level level) {
        if (level == null) return;
        recipesByOutput.clear();
        recipesByInput.clear();

        try {
            // In 1.20.1 Forge, getRecipes() might return recipes directly or we can use the internal map
            for (Recipe<?> recipe : level.getRecipeManager().getRecipes()) {
                ResourceLocation id = recipe.getId();
                AmiRecipeHolder<?> holder = new AmiRecipeHolder<>(id, recipe);
                
                ItemStack result = recipe.getResultItem(level.registryAccess());
                if (!result.isEmpty()) {
                    recipesByOutput.computeIfAbsent(result.getItem(), k -> new ArrayList<>()).add(holder);
                }

                for (Ingredient ing : recipe.getIngredients()) {
                    for (ItemStack stack : ing.getItems()) {
                        if (!stack.isEmpty()) {
                            recipesByInput.computeIfAbsent(stack.getItem(), k -> new ArrayList<>()).add(holder);
                        }
                    }
                }
            }

            indexBrewing(level);
            indexRepairs();
            indexComposting();
            indexFuels();
        } catch (Exception e) {
            AMI.LOGGER.warn("AmiRecipeIndex rebuild failed", e);
        }
        built = true;
    }

    private void indexBrewing(Level level) {
        try {
            int idCounter = 0;

            for (IBrewingRecipe brewingRecipe : BrewingRecipeRegistry.getRecipes()) {
                if (brewingRecipe instanceof BrewingRecipe recipe) {
                    ItemStack outputStack = recipe.getOutput();
                    if (outputStack.isEmpty()) continue;

                    ItemStack[] inputStacks = recipe.getInput().getItems();
                    if (inputStacks.length == 0) continue;

                    for (ItemStack inputStack : inputStacks) {
                        if (inputStack.isEmpty()) continue;
                        idCounter = indexBrewingRecipe(idCounter, inputStack, recipe.getIngredient(), outputStack);
                    }
                } else if (brewingRecipe instanceof VanillaBrewingRecipe) {
                    idCounter = indexVanillaBrewingRecipes(idCounter);
                }
            }
        } catch (Exception e) {
            AMI.LOGGER.warn("AmiRecipeIndex: Failed to index brewing recipes", e);
        }
    }

    private int indexVanillaBrewingRecipes(int idCounter) {
        List<ItemStack> knownPotions = new ArrayList<>();
        for (Item container : List.of(Items.POTION, Items.SPLASH_POTION, Items.LINGERING_POTION)) {
            knownPotions.add(new ItemStack(container));
            for (Potion potion : ForgeRegistries.POTIONS) {
                if (potion == Potions.EMPTY) continue;
                knownPotions.add(PotionUtils.setPotion(new ItemStack(container), potion));
            }
        }

        Set<String> seen = new HashSet<>();
        for (ItemStack inputStack : knownPotions) {
            for (Item reagent : ForgeRegistries.ITEMS) {
                ItemStack reagentStack = reagent.getDefaultInstance();
                if (reagentStack.isEmpty() || !PotionBrewing.isIngredient(reagentStack)) continue;

                ItemStack inputCandidate = inputStack.copy();
                ItemStack outputStack = PotionBrewing.mix(reagentStack, inputCandidate);
                if (outputStack == inputCandidate || outputStack.isEmpty()) continue;
                if (inputStack.getItem() instanceof PotionItem
                        && outputStack.getItem() instanceof PotionItem
                        && PotionUtils.getPotion(outputStack) == Potions.WATER) {
                    continue;
                }

                String key = brewingStackKey(inputStack) + "|" + ForgeRegistries.ITEMS.getKey(reagent) + "|" + brewingStackKey(outputStack);
                if (!seen.add(key)) continue;

                idCounter = indexBrewingRecipe(idCounter, inputStack, Ingredient.of(reagentStack), outputStack);
            }
        }

        return idCounter;
    }

    private int indexBrewingRecipe(int idCounter, ItemStack inputStack, Ingredient ingredient, ItemStack outputStack) {
        ResourceLocation id = new ResourceLocation("ami", "brewing/" + idCounter++);
        var recipe = new com.sanhiruzu.ami.index.special.PotionBrewingRecipe(
                id,
                inputStack.copy(),
                ingredient,
                outputStack.copy(),
                BREWING
        );
        AmiRecipeHolder<com.sanhiruzu.ami.index.special.PotionBrewingRecipe> holder = new AmiRecipeHolder<>(id, recipe);

        recipesByOutput.computeIfAbsent(outputStack.getItem(), k -> new ArrayList<>()).add(holder);
        recipesByInput.computeIfAbsent(inputStack.getItem(), k -> new ArrayList<>()).add(holder);
        for (ItemStack ingStack : ingredient.getItems()) {
            if (!ingStack.isEmpty()) {
                recipesByInput.computeIfAbsent(ingStack.getItem(), k -> new ArrayList<>()).add(holder);
            }
        }
        return idCounter;
    }

    private static String brewingStackKey(ItemStack stack) {
        ResourceLocation itemId = ForgeRegistries.ITEMS.getKey(stack.getItem());
        if (stack.getItem() instanceof PotionItem) {
            ResourceLocation potionId = ForgeRegistries.POTIONS.getKey(PotionUtils.getPotion(stack));
            return itemId + "@" + potionId;
        }
        return String.valueOf(itemId);
    }

    private void indexRepairs() {
        int idCounter = 0;
        for (Item item : ForgeRegistries.ITEMS) {
            if (!item.canBeDepleted()) continue;
            
            ItemStack stack = item.getDefaultInstance();
            for (Item materialItem : ForgeRegistries.ITEMS) {
                ItemStack materialStack = materialItem.getDefaultInstance();
                if (isValidRepairItem(item, stack, materialStack)) {
                    ResourceLocation id = new ResourceLocation("ami", "repair/" + idCounter++);
                    Ingredient ingredient = Ingredient.of(materialStack);
                    ItemStack result = stack.copy();
                    result.setDamageValue(0);
                    
                    var recipe = new com.sanhiruzu.ami.index.special.AnvilRepairRecipe(id, stack, ingredient, result, ANVIL_REPAIRING);
                    AmiRecipeHolder<com.sanhiruzu.ami.index.special.AnvilRepairRecipe> holder = new AmiRecipeHolder<>(id, recipe);
                    
                    recipesByOutput.computeIfAbsent(result.getItem(), k -> new ArrayList<>()).add(holder);
                    recipesByInput.computeIfAbsent(item, k -> new ArrayList<>()).add(holder);
                    recipesByInput.computeIfAbsent(materialItem, k -> new ArrayList<>()).add(holder);
                }
            }
        }
    }

    private static boolean isValidRepairItem(Item item, ItemStack stack, ItemStack materialStack) {
        if (materialStack.isEmpty()) return false;
        try {
            return item.isValidRepairItem(stack, materialStack);
        } catch (RuntimeException | LinkageError ignored) {
            // Some modded armor materials return a null repair ingredient; skip those invalid probes.
            return false;
        }
    }

    private void indexComposting() {
        // ComposterBlock.COMPOSTABLES
    }

    private void indexFuels() {
        // ForgeHooks.getBurnTime
    }

    public List<AmiRecipeHolder<?>> getRecipesFor(ItemStack stack) {
        return recipesByOutput.getOrDefault(stack.getItem(), List.of());
    }

    public List<AmiRecipeHolder<?>> getUsesFor(ItemStack stack) {
        return recipesByInput.getOrDefault(stack.getItem(), List.of());
    }

    public int recipeCount() {
        return recipesByOutput.values().stream().mapToInt(List::size).sum();
    }

    public boolean hasRecipe(Item item) {
        return recipesByOutput.containsKey(item);
    }

    public Set<Item> getAllOutputItems() {
        return recipesByOutput.keySet();
    }

    @SuppressWarnings("unchecked")
    public <T extends Recipe<?>> List<AmiRecipeHolder<T>> getAllRecipesOfType(RecipeType<T> type) {
        List<AmiRecipeHolder<T>> result = new ArrayList<>();
        for (List<AmiRecipeHolder<?>> list : recipesByOutput.values()) {
            for (AmiRecipeHolder<?> holder : list) {
                if (holder.value().getType() == type) {
                    result.add((AmiRecipeHolder<T>) holder);
                }
            }
        }
        return deduplicateByType(result);
    }

    public List<AmiRecipeHolder<?>> getRecipesByType(ItemStack stack) {
        return deduplicateByType(getRecipesFor(stack));
    }

    public List<AmiRecipeHolder<?>> getUsesByType(ItemStack stack) {
        return deduplicateByType(getUsesFor(stack));
    }

    private <T extends AmiRecipeHolder<?>> List<T> deduplicateByType(List<T> recipes) {
        if (recipes.size() <= 1) return recipes;
        Map<RecipeType<?>, T> unique = new LinkedHashMap<>();
        for (T holder : recipes) {
            unique.putIfAbsent(holder.value().getType(), holder);
        }
        return new ArrayList<>(unique.values());
    }
}
