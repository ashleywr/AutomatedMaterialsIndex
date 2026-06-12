package com.sanhiruzu.ami.recipe;

import com.sanhiruzu.ami.util.AmiRecipeHolder;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeHolder;
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

    static {
        try {
            net.minecraft.core.Registry.register(net.minecraft.core.registries.BuiltInRegistries.RECIPE_TYPE,
                    net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("ami", "brewing"), BREWING);
            net.minecraft.core.Registry.register(net.minecraft.core.registries.BuiltInRegistries.RECIPE_TYPE,
                    net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("ami", "grinding"), GRINDING);
            net.minecraft.core.Registry.register(net.minecraft.core.registries.BuiltInRegistries.RECIPE_TYPE,
                    net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("ami", "anvil_repairing"), ANVIL_REPAIRING);
            net.minecraft.core.Registry.register(net.minecraft.core.registries.BuiltInRegistries.RECIPE_TYPE,
                    net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("ami", "composting"), COMPOSTING);
            net.minecraft.core.Registry.register(net.minecraft.core.registries.BuiltInRegistries.RECIPE_TYPE,
                    net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("ami", "fuel"), FUEL);
        } catch (Exception ignored) {
        }
    }

    private AmiRecipeIndex() {
    }

    public static RecipeType<?> getEmiCategoryType(ResourceLocation categoryId, String categoryName) {
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

    @SuppressWarnings({"rawtypes", "unchecked"})
    private static AmiRecipeHolder<?> wrap(RecipeHolder<?> holder) {
        return new AmiRecipeHolder(holder.id(), holder.value());
    }

    public void rebuild(@Nullable Level level) {
        if (level == null) return;
        beginRebuild();

        try {
            Map<Item, Set<ResourceLocation>> seenInputs = new HashMap<>();
            Map<Item, Set<ResourceLocation>> seenOutputs = new HashMap<>();

            for (RecipeHolder<?> entry : level.getRecipeManager().getRecipes()) {
                ResourceLocation id = entry.id();
                AmiRecipeHolder<?> holder = wrap(entry);
                try {
                    ItemStack result = entry.value().getResultItem(level.registryAccess());
                    if (!result.isEmpty()) {
                        Item key = result.getItem();
                        if (seenOutputs.computeIfAbsent(key, k -> new HashSet<>()).add(id)) {
                            addOutput(result, holder);
                        }
                    }
                } catch (Exception ignored) {
                }

                try {
                    for (var ingredient : entry.value().getIngredients()) {
                        for (ItemStack stack : ingredient.getItems()) {
                            if (!stack.isEmpty()) {
                                Item key = stack.getItem();
                                if (seenInputs.computeIfAbsent(key, k -> new HashSet<>()).add(id)) {
                                    addInput(key, holder);
                                }
                            }
                        }
                    }
                } catch (Exception ignored) {
                }
            }

            // Index special recipe types
            indexBrewing(level);
            indexRepairs(level);
            indexComposting();
            indexFuels();
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

                        var recipe = new com.sanhiruzu.ami.neoforge.recipe.special.PotionBrewingRecipe(inStack, ingredient, outStack, BREWING);
                        AmiRecipeHolder<?> holder = wrap(new RecipeHolder<>(
                                net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("ami", "brewing_potion_" + idCounter++),
                                recipe
                        ));

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

                    for (net.minecraft.core.Holder.Reference<net.minecraft.world.item.alchemy.Potion> potionRef : net.minecraft.core.registries.BuiltInRegistries.POTION.holders().toList()) {
                        ItemStack inStack = net.minecraft.world.item.alchemy.PotionContents.createItemStack(fromItem, potionRef);
                        ItemStack outStack = net.minecraft.world.item.alchemy.PotionContents.createItemStack(toItem, potionRef);

                        var recipe = new com.sanhiruzu.ami.neoforge.recipe.special.PotionBrewingRecipe(inStack, ingredient, outStack, BREWING);
                        AmiRecipeHolder<?> holder = wrap(new RecipeHolder<>(
                                net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("ami", "brewing_container_" + idCounter++),
                                recipe
                        ));

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

                // 1. Material-based Anvil Repair
                Ingredient repairMaterial = null;
                if (item instanceof net.minecraft.world.item.ArmorItem ai) {
                    if (ai.getMaterial() != null && ai.getMaterial().value().repairIngredient() != null) {
                        repairMaterial = ai.getMaterial().value().repairIngredient().get();
                    }
                } else if (item instanceof net.minecraft.world.item.TieredItem ti) {
                    if (ti.getTier() != null && ti.getTier().getRepairIngredient() != null) {
                        repairMaterial = ti.getTier().getRepairIngredient();
                    }
                } else if (item == net.minecraft.world.item.Items.ELYTRA) {
                    repairMaterial = Ingredient.of(net.minecraft.world.item.Items.PHANTOM_MEMBRANE);
                } else if (item == net.minecraft.world.item.Items.SHIELD) {
                    repairMaterial = Ingredient.of(net.minecraft.tags.ItemTags.PLANKS);
                }

                if (repairMaterial != null && !repairMaterial.isEmpty()) {
                    ItemStack damagedInput = defaultStack.copy();
                    damagedInput.setDamageValue((int) (maxDamage * 0.7));

                    ItemStack repairedOutput = defaultStack.copy();
                    repairedOutput.setDamageValue((int) (maxDamage * 0.2));

                    var anvilRecipe = new com.sanhiruzu.ami.neoforge.recipe.special.AnvilRepairRecipe(damagedInput, repairMaterial, repairedOutput, ANVIL_REPAIRING);
                    AmiRecipeHolder<?> holder = wrap(new RecipeHolder<>(
                            net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("ami", "anvil_material_repair_" + idCounter++),
                            anvilRecipe
                    ));

                    addOutput(item, holder);
                    addInput(item, holder);
                    addIngredientInputs(repairMaterial, holder);
                }

                // 2. Tool combining (Anvil and Grindstone)
                ItemStack damaged1 = defaultStack.copy();
                damaged1.setDamageValue((int) (maxDamage * 0.7));

                ItemStack damaged2 = defaultStack.copy();
                damaged2.setDamageValue((int) (maxDamage * 0.5));

                ItemStack combinedRepaired = defaultStack.copy();
                combinedRepaired.setDamageValue((int) (maxDamage * 0.2));

                var anvilToolRecipe = new com.sanhiruzu.ami.neoforge.recipe.special.AnvilRepairRecipe(damaged1, Ingredient.of(damaged2), combinedRepaired, ANVIL_REPAIRING);
                AmiRecipeHolder<?> anvilHolder = wrap(new RecipeHolder<>(
                        net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("ami", "anvil_tool_repair_" + idCounter++),
                        anvilToolRecipe
                ));

                var grindstoneRecipe = new com.sanhiruzu.ami.neoforge.recipe.special.GrindstoneRepairRecipe(damaged1, damaged2, combinedRepaired, GRINDING);
                AmiRecipeHolder<?> grindstoneHolder = wrap(new RecipeHolder<>(
                        net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("ami", "grindstone_tool_repair_" + idCounter++),
                        grindstoneRecipe
                ));

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
                var recipe = new com.sanhiruzu.ami.neoforge.recipe.special.CompostingRecipe(stack, chance, COMPOSTING);
                AmiRecipeHolder<?> holder = wrap(new RecipeHolder<>(
                        net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("ami", "composting_" + idCounter++),
                        recipe
                ));

                addOutput(net.minecraft.world.item.Items.BONE_MEAL, holder);
                addInput(item, holder);
            }
        } catch (Exception e) {
            com.sanhiruzu.ami.neoforge.AMI.LOGGER.warn("AmiRecipeIndex indexComposting failed: {}", e.getMessage());
        }
    }

    private void indexFuels() {
        try {
            int idCounter = 0;

            for (Item item : net.minecraft.core.registries.BuiltInRegistries.ITEM) {
                ItemStack stack = item.getDefaultInstance();
                int burnTime = 0;
                try {
                    burnTime = stack.getBurnTime(RecipeType.SMELTING);
                } catch (Throwable ignored) {
                    if (item == net.minecraft.world.item.Items.COAL || item == net.minecraft.world.item.Items.CHARCOAL) {
                        burnTime = 1600;
                    } else if (item == net.minecraft.world.item.Items.LAVA_BUCKET) {
                        burnTime = 20000;
                    }
                }

                if (burnTime > 0) {
                    var recipe = new com.sanhiruzu.ami.neoforge.recipe.special.FuelRecipe(stack, burnTime, FUEL);
                    AmiRecipeHolder<?> holder = wrap(new RecipeHolder<>(
                            net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("ami", "fuel_" + idCounter++),
                            recipe
                    ));

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
            var enchantmentRegistry = level.registryAccess().registryOrThrow(net.minecraft.core.registries.Registries.ENCHANTMENT);
            var firstEnchantOpt = enchantmentRegistry.holders().findFirst();

            for (Item item : net.minecraft.core.registries.BuiltInRegistries.ITEM) {
                ItemStack defaultStack = item.getDefaultInstance();
                int maxDamage = defaultStack.getMaxDamage();
                if (maxDamage <= 0) continue;

                ItemStack enchantedInput = defaultStack.copy();
                if (firstEnchantOpt.isPresent()) {
                    enchantedInput.enchant(firstEnchantOpt.get(), 1);
                }

                var recipe = new com.sanhiruzu.ami.neoforge.recipe.special.GrindstoneDisenchantingRecipe(enchantedInput, defaultStack, GRINDING);
                AmiRecipeHolder<?> holder = wrap(new RecipeHolder<>(
                        net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("ami", "disenchanting_tool_" + idCounter++),
                        recipe
                ));

                addOutput(item, holder);
                addInput(item, holder);
            }

            ItemStack enchantedBook = new ItemStack(net.minecraft.world.item.Items.ENCHANTED_BOOK);
            if (firstEnchantOpt.isPresent()) {
                enchantedBook.enchant(firstEnchantOpt.get(), 1);
            }
            ItemStack normalBook = new ItemStack(net.minecraft.world.item.Items.BOOK);

            var bookRecipe = new com.sanhiruzu.ami.neoforge.recipe.special.GrindstoneDisenchantingRecipe(enchantedBook, normalBook, GRINDING);
            AmiRecipeHolder<?> bookHolder = wrap(new RecipeHolder<>(
                    net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("ami", "disenchanting_book_" + idCounter++),
                    bookRecipe
            ));

            addOutput(net.minecraft.world.item.Items.BOOK, bookHolder);
            addInput(net.minecraft.world.item.Items.ENCHANTED_BOOK, bookHolder);

        } catch (Exception e) {
            com.sanhiruzu.ami.neoforge.AMI.LOGGER.warn("AmiRecipeIndex indexDisenchanting failed: {}", e.getMessage());
        }
    }

    private void indexEnchanting(Level level) {
        try {
            int idCounter = 0;
            var enchantmentRegistry = level.registryAccess().registryOrThrow(net.minecraft.core.registries.Registries.ENCHANTMENT);

            for (Item item : net.minecraft.core.registries.BuiltInRegistries.ITEM) {
                ItemStack toolStack = item.getDefaultInstance();
                if (!toolStack.isEnchantable() && toolStack.getMaxDamage() <= 0) continue;

                for (net.minecraft.core.Holder.Reference<net.minecraft.world.item.enchantment.Enchantment> enchantRef : enchantmentRegistry.holders().toList()) {
                    net.minecraft.world.item.enchantment.Enchantment enchantment = enchantRef.value();

                    if (enchantment.canEnchant(toolStack)) {
                        int maxLevel = enchantment.getMaxLevel();

                        ItemStack enchantedBook = new ItemStack(net.minecraft.world.item.Items.ENCHANTED_BOOK);
                        enchantedBook.enchant(enchantRef, maxLevel);

                        ItemStack enchantedTool = toolStack.copy();
                        enchantedTool.enchant(enchantRef, maxLevel);

                        var recipe = new com.sanhiruzu.ami.neoforge.recipe.special.AnvilEnchantingRecipe(toolStack, enchantedBook, enchantedTool, ANVIL_REPAIRING);
                        AmiRecipeHolder<?> holder = wrap(new RecipeHolder<>(
                                net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("ami", "enchanting_" + idCounter++),
                                recipe
                        ));

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
