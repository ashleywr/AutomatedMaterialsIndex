package com.sanhiruzu.ami.recipe;

import com.sanhiruzu.ami.forge.AMI;
import com.sanhiruzu.ami.util.AmiRecipeHolder;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.PotionItem;
import net.minecraft.world.item.alchemy.Potion;
import net.minecraft.world.item.alchemy.PotionBrewing;
import net.minecraft.world.item.alchemy.PotionUtils;
import net.minecraft.world.item.alchemy.Potions;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeType;
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
    public static final RecipeType<com.sanhiruzu.ami.forge.recipe.special.PotionBrewingRecipe> BREWING = new RecipeType<>() {
        @Override
        public String toString() {
            return "ami:brewing";
        }
    };
    public static final RecipeType<com.sanhiruzu.ami.forge.recipe.special.GrindstoneRepairRecipe> GRINDING = new RecipeType<>() {
        @Override
        public String toString() {
            return "ami:grinding";
        }
    };
    public static final RecipeType<com.sanhiruzu.ami.forge.recipe.special.AnvilRepairRecipe> ANVIL_REPAIRING = new RecipeType<>() {
        @Override
        public String toString() {
            return "ami:anvil_repairing";
        }
    };
    public static final RecipeType<com.sanhiruzu.ami.forge.recipe.special.CompostingRecipe> COMPOSTING = new RecipeType<>() {
        @Override
        public String toString() {
            return "ami:composting";
        }
    };
    public static final RecipeType<com.sanhiruzu.ami.forge.recipe.special.FuelRecipe> FUEL = new RecipeType<>() {
        @Override
        public String toString() {
            return "ami:fuel";
        }
    };
    private static final AmiRecipeIndex INSTANCE = new AmiRecipeIndex();
    private final ConcurrentMap<Item, List<AmiRecipeHolder<?>>> recipesByOutput = new ConcurrentHashMap<>();
    private final ConcurrentMap<Item, List<AmiRecipeHolder<?>>> recipesByInput = new ConcurrentHashMap<>();
    private volatile boolean built;

    private AmiRecipeIndex() {
    }

    public static RecipeType<?> getEmiCategoryType(ResourceLocation categoryId, String categoryName) {
        return AmiRecipeCategoryRegistry.getEmiCategoryType(categoryId, categoryName, id -> "emi:" + id);
    }

    public static void setEmiCategoryIcon(RecipeType<?> type, ItemStack icon) {
        AmiRecipeCategoryRegistry.setEmiCategoryIcon(type, icon);
    }

    public static boolean isEmiCategoryType(RecipeType<?> type) {
        return AmiRecipeCategoryRegistry.isEmiCategoryType(type);
    }

    public static String getEmiCategoryName(RecipeType<?> type) {
        return AmiRecipeCategoryRegistry.getEmiCategoryName(type);
    }

    public static ItemStack getEmiCategoryIcon(RecipeType<?> type) {
        return AmiRecipeCategoryRegistry.getEmiCategoryIcon(type);
    }

    public static AmiRecipeIndex getInstance() {
        return INSTANCE;
    }

    private static String brewingStackKey(ItemStack stack) {
        ResourceLocation itemId = ForgeRegistries.ITEMS.getKey(stack.getItem());
        if (stack.getItem() instanceof PotionItem) {
            ResourceLocation potionId = ForgeRegistries.POTIONS.getKey(PotionUtils.getPotion(stack));
            return itemId + "@" + potionId;
        }
        return String.valueOf(itemId);
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

    public boolean isBuilt() {
        return built;
    }

    private void beginRebuild() {
        recipesByOutput.clear();
        recipesByInput.clear();
        built = false;
    }

    private void finishRebuild() {
        built = true;
    }

    private void addOutput(Item item, AmiRecipeHolder<?> holder) {
        recipesByOutput.computeIfAbsent(item, ignored -> Collections.synchronizedList(new ArrayList<>())).add(holder);
    }

    private void addInput(Item item, AmiRecipeHolder<?> holder) {
        recipesByInput.computeIfAbsent(item, ignored -> Collections.synchronizedList(new ArrayList<>())).add(holder);
    }

    private void addOutput(ItemStack stack, AmiRecipeHolder<?> holder) {
        if (!stack.isEmpty()) addOutput(stack.getItem(), holder);
    }

    private void addInput(ItemStack stack, AmiRecipeHolder<?> holder) {
        if (!stack.isEmpty()) addInput(stack.getItem(), holder);
    }

    private void addIngredientInputs(Ingredient ingredient, AmiRecipeHolder<?> holder) {
        for (ItemStack stack : ingredient.getItems()) {
            addInput(stack, holder);
        }
    }

    public List<AmiRecipeHolder<?>> getRecipesFor(ItemStack stack) {
        if (stack == null || stack.isEmpty()) return List.of();
        List<AmiRecipeHolder<?>> list = recipesByOutput.get(stack.getItem());
        return list == null ? List.of() : List.copyOf(list);
    }

    public boolean hasRecipesFor(ItemStack stack) {
        if (stack == null || stack.isEmpty()) return false;
        List<AmiRecipeHolder<?>> list = recipesByOutput.get(stack.getItem());
        return list != null && !list.isEmpty();
    }

    public List<AmiRecipeHolder<?>> getUsesFor(ItemStack stack) {
        if (stack == null || stack.isEmpty()) return List.of();
        List<AmiRecipeHolder<?>> list = recipesByInput.get(stack.getItem());
        return list == null ? List.of() : List.copyOf(list);
    }

    public boolean hasUsesFor(ItemStack stack) {
        if (stack == null || stack.isEmpty()) return false;
        List<AmiRecipeHolder<?>> list = recipesByInput.get(stack.getItem());
        return list != null && !list.isEmpty();
    }

    public int recipeCount() {
        return recipesByOutput.values().stream().mapToInt(List::size).sum();
    }

    public boolean hasRecipe(Item item) {
        return recipesByOutput.containsKey(item);
    }

    public Set<Item> getAllOutputItems() {
        return Set.copyOf(recipesByOutput.keySet());
    }

    public Set<Item> getAllInputItems() {
        return Set.copyOf(recipesByInput.keySet());
    }

    public List<AmiRecipeHolder<?>> getAllRecipes() {
        List<AmiRecipeHolder<?>> result = new ArrayList<>();
        Set<ResourceLocation> seen = new HashSet<>();
        for (List<AmiRecipeHolder<?>> holders : recipesByOutput.values()) {
            for (AmiRecipeHolder<?> holder : holders) {
                if (holder != null && seen.add(holder.id())) {
                    result.add(holder);
                }
            }
        }
        return result;
    }

    @SuppressWarnings("unchecked")
    public <T extends Recipe<?>> List<AmiRecipeHolder<T>> getAllRecipesOfType(RecipeType<T> type) {
        List<AmiRecipeHolder<T>> result = new ArrayList<>();
        Set<ResourceLocation> seen = new HashSet<>();
        for (List<AmiRecipeHolder<?>> holders : recipesByOutput.values()) {
            for (AmiRecipeHolder<?> holder : holders) {
                if (holder.value().getType() == type && seen.add(holder.id())) {
                    result.add((AmiRecipeHolder<T>) holder);
                }
            }
        }
        return result;
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

    public void rebuild(@Nullable Level level) {
        if (level == null) return;
        beginRebuild();

        try {
            // In 1.20.1 Forge, getRecipes() might return recipes directly or we can use the internal map
            for (Recipe<?> recipe : level.getRecipeManager().getRecipes()) {
                ResourceLocation id = recipe.getId();
                AmiRecipeHolder<?> holder = new AmiRecipeHolder<>(id, recipe);

                ItemStack result = recipe.getResultItem(level.registryAccess());
                if (!result.isEmpty()) {
                    addOutput(result, holder);
                }

                for (Ingredient ing : recipe.getIngredients()) {
                    for (ItemStack stack : ing.getItems()) {
                        if (!stack.isEmpty()) {
                            addInput(stack, holder);
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
        finishRebuild();
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
        ResourceLocation id = ResourceLocation.fromNamespaceAndPath("ami", "brewing/" + idCounter++);
        var recipe = new com.sanhiruzu.ami.forge.recipe.special.PotionBrewingRecipe(
                id,
                inputStack.copy(),
                ingredient,
                outputStack.copy(),
                BREWING
        );
        AmiRecipeHolder<com.sanhiruzu.ami.forge.recipe.special.PotionBrewingRecipe> holder = new AmiRecipeHolder<>(id, recipe);

        addOutput(outputStack, holder);
        addInput(inputStack, holder);
        addIngredientInputs(ingredient, holder);
        return idCounter;
    }

    private void indexRepairs() {
        int idCounter = 0;
        for (Item item : ForgeRegistries.ITEMS) {
            if (!item.canBeDepleted()) continue;

            ItemStack stack = item.getDefaultInstance();
            for (Item materialItem : ForgeRegistries.ITEMS) {
                ItemStack materialStack = materialItem.getDefaultInstance();
                if (isValidRepairItem(item, stack, materialStack)) {
                    ResourceLocation id = ResourceLocation.fromNamespaceAndPath("ami", "repair/" + idCounter++);
                    Ingredient ingredient = Ingredient.of(materialStack);
                    ItemStack result = stack.copy();
                    result.setDamageValue(0);

                    var recipe = new com.sanhiruzu.ami.forge.recipe.special.AnvilRepairRecipe(id, stack, ingredient, result, ANVIL_REPAIRING);
                    AmiRecipeHolder<com.sanhiruzu.ami.forge.recipe.special.AnvilRepairRecipe> holder = new AmiRecipeHolder<>(id, recipe);

                    addOutput(result, holder);
                    addInput(item, holder);
                    addInput(materialItem, holder);
                }
            }
        }
    }

    private void indexComposting() {
        // ComposterBlock.COMPOSTABLES
    }

    private void indexFuels() {
        // ForgeHooks.getBurnTime
    }

}
