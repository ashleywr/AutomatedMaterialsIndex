package com.sanhiruzu.ami.recipe;

import com.sanhiruzu.ami.util.AmiRecipeHolder;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeInput;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

import java.util.*;

/**
 * Dedicated recipe cache indexed by input and output Item.
 * Independent of EMI/JEI — queries vanilla RecipeManager directly.
 */
public final class AmiRecipeIndex extends AmiRecipeIndexBase {
    public static final RecipeType<com.sanhiruzu.ami.neoforge.recipe.special.PotionBrewingRecipe> BREWING = new RecipeType<>() {
        @Override
        public String toString() {
            return "ami:brewing";
        }
    };
    public static final RecipeType<com.sanhiruzu.ami.neoforge.recipe.special.GrindstoneRepairRecipe> GRINDING = new RecipeType<>() {
        @Override
        public String toString() {
            return "ami:grinding";
        }
    };
    public static final RecipeType<com.sanhiruzu.ami.neoforge.recipe.special.AnvilRepairRecipe> ANVIL_REPAIRING = new RecipeType<>() {
        @Override
        public String toString() {
            return "ami:anvil_repairing";
        }
    };
    public static final RecipeType<com.sanhiruzu.ami.neoforge.recipe.special.CompostingRecipe> COMPOSTING = new RecipeType<>() {
        @Override
        public String toString() {
            return "ami:composting";
        }
    };
    public static final RecipeType<com.sanhiruzu.ami.neoforge.recipe.special.FuelRecipe> FUEL = new RecipeType<>() {
        @Override
        public String toString() {
            return "ami:fuel";
        }
    };
    private static final AmiRecipeIndex INSTANCE = new AmiRecipeIndex();

    private static final RecipeInput EMPTY_RECIPE_INPUT = new RecipeInput() {
        @Override public ItemStack getItem(int index) { return ItemStack.EMPTY; }
        @Override public int size() { return 0; }
    };

    static {
        try {
            net.minecraft.core.Registry.register(net.minecraft.core.registries.BuiltInRegistries.RECIPE_TYPE,
                    net.minecraft.resources.Identifier.fromNamespaceAndPath("ami", "brewing"), BREWING);
            net.minecraft.core.Registry.register(net.minecraft.core.registries.BuiltInRegistries.RECIPE_TYPE,
                    net.minecraft.resources.Identifier.fromNamespaceAndPath("ami", "grinding"), GRINDING);
            net.minecraft.core.Registry.register(net.minecraft.core.registries.BuiltInRegistries.RECIPE_TYPE,
                    net.minecraft.resources.Identifier.fromNamespaceAndPath("ami", "anvil_repairing"), ANVIL_REPAIRING);
            net.minecraft.core.Registry.register(net.minecraft.core.registries.BuiltInRegistries.RECIPE_TYPE,
                    net.minecraft.resources.Identifier.fromNamespaceAndPath("ami", "composting"), COMPOSTING);
            net.minecraft.core.Registry.register(net.minecraft.core.registries.BuiltInRegistries.RECIPE_TYPE,
                    net.minecraft.resources.Identifier.fromNamespaceAndPath("ami", "fuel"), FUEL);
        } catch (Exception ignored) {
        }
    }

    private AmiRecipeIndex() {
    }

    public static RecipeType<?> getEmiCategoryType(Identifier categoryId, String categoryName) {
        return AmiRecipeCategoryRegistry.getEmiCategoryType(
                categoryId,
                categoryName,
                id -> "emi:" + id.getNamespace() + "/" + id.getPath());
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

    private static ResourceKey<net.minecraft.world.item.crafting.Recipe<?>> recipeKey(Identifier id) {
        return ResourceKey.create(net.minecraft.core.registries.Registries.RECIPE, id);
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private static AmiRecipeHolder<?> wrap(RecipeHolder<?> holder) {
        return new AmiRecipeHolder(holder.id().identifier(), holder.value());
    }

    private static <T extends net.minecraft.world.item.crafting.Recipe<RecipeInput>>
    AmiRecipeHolder<T> wrapSpecial(Identifier id, T recipe) {
        @SuppressWarnings("unchecked")
        AmiRecipeHolder<T> holder = new AmiRecipeHolder(id, recipe);
        return holder;
    }

    public void rebuild(@Nullable Level level) {
        if (level == null) return;
        if (!(level instanceof net.minecraft.server.level.ServerLevel)) return;
        var recipeManager = ((net.minecraft.server.level.ServerLevel) level).getServer().getRecipeManager();
        beginRebuild();

        try {
            Map<Item, Set<Identifier>> seenInputs = new HashMap<>();
            Map<Item, Set<Identifier>> seenOutputs = new HashMap<>();

            for (RecipeHolder<?> entry : recipeManager.getRecipes()) {
                Identifier id = entry.id().identifier();
                AmiRecipeHolder<?> holder = wrap(entry);
                try {
                    @SuppressWarnings("unchecked")
                    ItemStack result = ((net.minecraft.world.item.crafting.Recipe<RecipeInput>)(Object)entry.value()).assemble(EMPTY_RECIPE_INPUT);
                    if (!result.isEmpty()) {
                        Item key = result.getItem();
                        if (seenOutputs.computeIfAbsent(key, k -> new HashSet<>()).add(id)) {
                            addOutput(result, holder);
                        }
                    }
                } catch (Exception ignored) {
                }

                try {
                    for (var ingredient : entry.value().placementInfo().ingredients()) {
                        ingredient.items().forEach(itemHolder -> {
                            Item key = itemHolder.value();
                            if (seenInputs.computeIfAbsent(key, k -> new HashSet<>()).add(id)) {
                                addInput(key, holder);
                            }
                        });
                    }
                } catch (Exception ignored) {
                }
            }

            // Index special recipe types
            indexBrewing(level);
            indexRepairs(level);
            indexComposting();
            indexFuels(level);
            indexDisenchanting(level);
            indexEnchanting(level);

        } catch (Exception e) {
            com.sanhiruzu.ami.neoforge.AMI.LOGGER.warn("AmiRecipeIndex rebuild failed: {}", e.getMessage());
            return;
        }
        finishRebuild();
    }

    private void indexBrewing(Level level) {
        try {
            net.minecraft.world.item.alchemy.PotionBrewing brewing = level.potionBrewing();
            if (brewing == null) return;

            java.lang.reflect.Field potionMixesField = net.minecraft.world.item.alchemy.PotionBrewing.class.getDeclaredField("potionMixes");
            potionMixesField.setAccessible(true);
            List<?> potionMixes = (List<?>) potionMixesField.get(brewing);

            java.lang.reflect.Field containerMixesField = net.minecraft.world.item.alchemy.PotionBrewing.class.getDeclaredField("containerMixes");
            containerMixesField.setAccessible(true);
            List<?> containerMixes = (List<?>) containerMixesField.get(brewing);

            Item[] containers = {
                    net.minecraft.world.item.Items.POTION,
                    net.minecraft.world.item.Items.SPLASH_POTION,
                    net.minecraft.world.item.Items.LINGERING_POTION
            };

            int idCounter = 0;

            // 1. Process Potion Contents Mixes
            for (Object mix : potionMixes) {
                java.lang.reflect.Field fromField = mix.getClass().getDeclaredField("from");
                fromField.setAccessible(true);
                net.minecraft.core.Holder<?> fromHolder = (net.minecraft.core.Holder<?>) fromField.get(mix);

                java.lang.reflect.Field ingredientField = mix.getClass().getDeclaredField("ingredient");
                ingredientField.setAccessible(true);
                net.minecraft.world.item.crafting.Ingredient ingredient = (net.minecraft.world.item.crafting.Ingredient) ingredientField.get(mix);

                java.lang.reflect.Field toField = mix.getClass().getDeclaredField("to");
                toField.setAccessible(true);
                net.minecraft.core.Holder<?> toHolder = (net.minecraft.core.Holder<?>) toField.get(mix);

                if (fromHolder != null && toHolder != null && ingredient != null) {
                    for (Item container : containers) {
                        @SuppressWarnings("unchecked")
                        ItemStack inStack = net.minecraft.world.item.alchemy.PotionContents.createItemStack(container, (net.minecraft.core.Holder<net.minecraft.world.item.alchemy.Potion>) fromHolder);
                        @SuppressWarnings("unchecked")
                        ItemStack outStack = net.minecraft.world.item.alchemy.PotionContents.createItemStack(container, (net.minecraft.core.Holder<net.minecraft.world.item.alchemy.Potion>) toHolder);

                        Identifier id = Identifier.fromNamespaceAndPath("ami", "brewing_potion_" + idCounter++);
                        var recipe = new com.sanhiruzu.ami.neoforge.recipe.special.PotionBrewingRecipe(inStack, ingredient, outStack, BREWING);
                        AmiRecipeHolder<?> holder = wrapSpecial(id, recipe);

                        addOutput(outStack, holder);
                        addInput(inStack, holder);
                        addIngredientInputs(ingredient, holder);
                    }
                }
            }

            // 2. Process Container Mixes
            for (Object mix : containerMixes) {
                java.lang.reflect.Field fromField = mix.getClass().getDeclaredField("from");
                fromField.setAccessible(true);
                net.minecraft.core.Holder<?> fromHolder = (net.minecraft.core.Holder<?>) fromField.get(mix);

                java.lang.reflect.Field ingredientField = mix.getClass().getDeclaredField("ingredient");
                ingredientField.setAccessible(true);
                net.minecraft.world.item.crafting.Ingredient ingredient = (net.minecraft.world.item.crafting.Ingredient) ingredientField.get(mix);

                java.lang.reflect.Field toField = mix.getClass().getDeclaredField("to");
                toField.setAccessible(true);
                net.minecraft.core.Holder<?> toHolder = (net.minecraft.core.Holder<?>) toField.get(mix);

                if (fromHolder != null && toHolder != null && ingredient != null) {
                    Item fromItem = (Item) fromHolder.value();
                    Item toItem = (Item) toHolder.value();

                    for (net.minecraft.core.Holder.Reference<net.minecraft.world.item.alchemy.Potion> potionRef : net.minecraft.core.registries.BuiltInRegistries.POTION.listElements().toList()) {
                        ItemStack inStack = net.minecraft.world.item.alchemy.PotionContents.createItemStack(fromItem, potionRef);
                        ItemStack outStack = net.minecraft.world.item.alchemy.PotionContents.createItemStack(toItem, potionRef);

                        Identifier id = Identifier.fromNamespaceAndPath("ami", "brewing_container_" + idCounter++);
                        var recipe = new com.sanhiruzu.ami.neoforge.recipe.special.PotionBrewingRecipe(inStack, ingredient, outStack, BREWING);
                        AmiRecipeHolder<?> holder = wrapSpecial(id, recipe);

                        addOutput(outStack, holder);
                        addInput(inStack, holder);
                        addIngredientInputs(ingredient, holder);
                    }
                }
            }

        } catch (Exception e) {
            com.sanhiruzu.ami.neoforge.AMI.LOGGER.warn("AmiRecipeIndex indexBrewing failed: {}", e.getMessage());
        }
    }

    private void indexRepairs(Level level) {
        try {
            int idCounter = 0;

            for (Item item : net.minecraft.core.registries.BuiltInRegistries.ITEM) {
                ItemStack defaultStack = item.getDefaultInstance();
                int maxDamage = defaultStack.getMaxDamage();
                if (maxDamage <= 0) continue;

                // Use REPAIRABLE component to find repair materials (replaces ArmorItem/TieredItem checks)
                Ingredient repairMaterial = null;
                net.minecraft.world.item.enchantment.Repairable repairable = defaultStack.get(net.minecraft.core.component.DataComponents.REPAIRABLE);
                if (repairable != null && repairable.items().size() > 0) {
                    repairMaterial = Ingredient.of(repairable.items());
                }

                if (repairMaterial != null && !repairMaterial.isEmpty()) {
                    ItemStack damagedInput = defaultStack.copy();
                    damagedInput.setDamageValue((int) (maxDamage * 0.7));

                    ItemStack repairedOutput = defaultStack.copy();
                    repairedOutput.setDamageValue((int) (maxDamage * 0.2));

                    Identifier id = Identifier.fromNamespaceAndPath("ami", "anvil_material_repair_" + idCounter++);
                    var anvilRecipe = new com.sanhiruzu.ami.neoforge.recipe.special.AnvilRepairRecipe(damagedInput, repairMaterial, repairedOutput, ANVIL_REPAIRING);
                    AmiRecipeHolder<?> holder = wrapSpecial(id, anvilRecipe);

                    addOutput(item, holder);
                    addInput(item, holder);
                    addIngredientInputs(repairMaterial, holder);
                }

                // Tool combining (Anvil and Grindstone)
                ItemStack damaged1 = defaultStack.copy();
                damaged1.setDamageValue((int) (maxDamage * 0.7));

                ItemStack damaged2 = defaultStack.copy();
                damaged2.setDamageValue((int) (maxDamage * 0.5));

                ItemStack combinedRepaired = defaultStack.copy();
                combinedRepaired.setDamageValue((int) (maxDamage * 0.2));

                Identifier anvilId = Identifier.fromNamespaceAndPath("ami", "anvil_tool_repair_" + idCounter++);
                var anvilToolRecipe = new com.sanhiruzu.ami.neoforge.recipe.special.AnvilRepairRecipe(damaged1, Ingredient.of(item), combinedRepaired, ANVIL_REPAIRING);
                AmiRecipeHolder<?> anvilHolder = wrapSpecial(anvilId, anvilToolRecipe);

                Identifier grindId = Identifier.fromNamespaceAndPath("ami", "grindstone_tool_repair_" + idCounter++);
                var grindstoneRecipe = new com.sanhiruzu.ami.neoforge.recipe.special.GrindstoneRepairRecipe(damaged1, damaged2, combinedRepaired, GRINDING);
                AmiRecipeHolder<?> grindstoneHolder = wrapSpecial(grindId, grindstoneRecipe);

                addOutput(item, anvilHolder);
                addInput(item, anvilHolder);

                addOutput(item, grindstoneHolder);
                addInput(item, grindstoneHolder);
            }
        } catch (Exception e) {
            com.sanhiruzu.ami.neoforge.AMI.LOGGER.warn("AmiRecipeIndex indexRepairs failed: {}", e.getMessage());
        }
    }

    private void indexComposting() {
        try {
            int idCounter = 0;

            for (Map.Entry<net.minecraft.world.level.ItemLike, Float> entry : net.minecraft.world.level.block.ComposterBlock.COMPOSTABLES.entrySet()) {
                Item item = entry.getKey().asItem();
                float chance = entry.getValue();

                ItemStack stack = item.getDefaultInstance();
                Identifier id = Identifier.fromNamespaceAndPath("ami", "composting_" + idCounter++);
                var recipe = new com.sanhiruzu.ami.neoforge.recipe.special.CompostingRecipe(stack, chance, COMPOSTING);
                AmiRecipeHolder<?> holder = wrapSpecial(id, recipe);

                addOutput(net.minecraft.world.item.Items.BONE_MEAL, holder);
                addInput(item, holder);
            }
        } catch (Exception e) {
            com.sanhiruzu.ami.neoforge.AMI.LOGGER.warn("AmiRecipeIndex indexComposting failed: {}", e.getMessage());
        }
    }

    private void indexFuels(Level level) {
        try {
            int idCounter = 0;
            net.minecraft.world.level.block.entity.FuelValues fuelValues = level.fuelValues();

            for (Item item : net.minecraft.core.registries.BuiltInRegistries.ITEM) {
                ItemStack stack = item.getDefaultInstance();
                int burnTime = fuelValues.burnDuration(stack);

                if (burnTime > 0) {
                    Identifier id = Identifier.fromNamespaceAndPath("ami", "fuel_" + idCounter++);
                    var recipe = new com.sanhiruzu.ami.neoforge.recipe.special.FuelRecipe(stack, burnTime, FUEL);
                    AmiRecipeHolder<?> holder = wrapSpecial(id, recipe);

                    addInput(item, holder);
                }
            }
        } catch (Exception e) {
            com.sanhiruzu.ami.neoforge.AMI.LOGGER.warn("AmiRecipeIndex indexFuels failed: {}", e.getMessage());
        }
    }

    private void indexDisenchanting(Level level) {
        try {
            int idCounter = 0;
            var enchantmentRegistry = level.registryAccess().lookupOrThrow(net.minecraft.core.registries.Registries.ENCHANTMENT);
            var firstEnchantOpt = enchantmentRegistry.listElements().findFirst();

            for (Item item : net.minecraft.core.registries.BuiltInRegistries.ITEM) {
                ItemStack defaultStack = item.getDefaultInstance();
                int maxDamage = defaultStack.getMaxDamage();
                if (maxDamage <= 0) continue;

                ItemStack enchantedInput = defaultStack.copy();
                if (firstEnchantOpt.isPresent()) {
                    enchantedInput.enchant(firstEnchantOpt.get(), 1);
                }

                Identifier id = Identifier.fromNamespaceAndPath("ami", "disenchanting_tool_" + idCounter++);
                var recipe = new com.sanhiruzu.ami.neoforge.recipe.special.GrindstoneDisenchantingRecipe(enchantedInput, defaultStack, GRINDING);
                AmiRecipeHolder<?> holder = wrapSpecial(id, recipe);

                addOutput(item, holder);
                addInput(item, holder);
            }

            ItemStack enchantedBook = new ItemStack(net.minecraft.world.item.Items.ENCHANTED_BOOK);
            if (firstEnchantOpt.isPresent()) {
                enchantedBook.enchant(firstEnchantOpt.get(), 1);
            }
            ItemStack normalBook = new ItemStack(net.minecraft.world.item.Items.BOOK);

            Identifier bookId = Identifier.fromNamespaceAndPath("ami", "disenchanting_book_" + idCounter++);
            var bookRecipe = new com.sanhiruzu.ami.neoforge.recipe.special.GrindstoneDisenchantingRecipe(enchantedBook, normalBook, GRINDING);
            AmiRecipeHolder<?> bookHolder = wrapSpecial(bookId, bookRecipe);

            addOutput(net.minecraft.world.item.Items.BOOK, bookHolder);
            addInput(net.minecraft.world.item.Items.ENCHANTED_BOOK, bookHolder);

        } catch (Exception e) {
            com.sanhiruzu.ami.neoforge.AMI.LOGGER.warn("AmiRecipeIndex indexDisenchanting failed: {}", e.getMessage());
        }
    }

    private void indexEnchanting(Level level) {
        try {
            int idCounter = 0;
            var enchantmentRegistry = level.registryAccess().lookupOrThrow(net.minecraft.core.registries.Registries.ENCHANTMENT);

            for (Item item : net.minecraft.core.registries.BuiltInRegistries.ITEM) {
                ItemStack toolStack = item.getDefaultInstance();
                if (!toolStack.isEnchantable() && toolStack.getMaxDamage() <= 0) continue;

                for (net.minecraft.core.Holder.Reference<net.minecraft.world.item.enchantment.Enchantment> enchantRef : enchantmentRegistry.listElements().toList()) {
                    net.minecraft.world.item.enchantment.Enchantment enchantment = enchantRef.value();

                    if (enchantment.canEnchant(toolStack)) {
                        int maxLevel = enchantment.getMaxLevel();

                        ItemStack enchantedBook = new ItemStack(net.minecraft.world.item.Items.ENCHANTED_BOOK);
                        enchantedBook.enchant(enchantRef, maxLevel);

                        ItemStack enchantedTool = toolStack.copy();
                        enchantedTool.enchant(enchantRef, maxLevel);

                        Identifier id = Identifier.fromNamespaceAndPath("ami", "enchanting_" + idCounter++);
                        var recipe = new com.sanhiruzu.ami.neoforge.recipe.special.AnvilEnchantingRecipe(toolStack, enchantedBook, enchantedTool, ANVIL_REPAIRING);
                        AmiRecipeHolder<?> holder = wrapSpecial(id, recipe);

                        addOutput(item, holder);
                        addInput(item, holder);
                        addInput(net.minecraft.world.item.Items.ENCHANTED_BOOK, holder);
                    }
                }
            }
        } catch (Exception e) {
            com.sanhiruzu.ami.neoforge.AMI.LOGGER.warn("AmiRecipeIndex indexEnchanting failed: {}", e.getMessage());
        }
    }

}
