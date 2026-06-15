package com.sanhiruzu.ami.recipe;

import com.sanhiruzu.ami.fabric.AmiFabric;
import com.sanhiruzu.ami.fabric.recipe.special.AnvilEnchantingRecipe;
import com.sanhiruzu.ami.fabric.recipe.special.AnvilRepairRecipe;
import com.sanhiruzu.ami.fabric.recipe.special.CompostingRecipe;
import com.sanhiruzu.ami.fabric.recipe.special.FuelRecipe;
import com.sanhiruzu.ami.fabric.recipe.special.GrindstoneDisenchantingRecipe;
import com.sanhiruzu.ami.fabric.recipe.special.GrindstoneRepairRecipe;
import com.sanhiruzu.ami.fabric.recipe.special.PotionBrewingRecipe;
import com.sanhiruzu.ami.util.AmiRecipeHolder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.TieredItem;
import net.minecraft.world.item.alchemy.PotionContents;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.ComposterBlock;
import net.minecraft.world.level.block.entity.AbstractFurnaceBlockEntity;
import net.minecraft.tags.ItemTags;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Dedicated recipe cache indexed by input and output Item.
 * Independent of EMI/JEI — queries vanilla RecipeManager directly.
 * Fabric port of NeoForge AmiRecipeIndex.
 */
public final class AmiRecipeIndex extends AmiRecipeIndexBase {

    public static final RecipeType<PotionBrewingRecipe> BREWING = new RecipeType<>() {
        @Override
        public String toString() {
            return "ami:brewing";
        }
    };
    public static final RecipeType<GrindstoneRepairRecipe> GRINDING = new RecipeType<>() {
        @Override
        public String toString() {
            return "ami:grinding";
        }
    };
    public static final RecipeType<AnvilRepairRecipe> ANVIL_REPAIRING = new RecipeType<>() {
        @Override
        public String toString() {
            return "ami:anvil_repairing";
        }
    };
    public static final RecipeType<CompostingRecipe> COMPOSTING = new RecipeType<>() {
        @Override
        public String toString() {
            return "ami:composting";
        }
    };
    public static final RecipeType<FuelRecipe> FUEL = new RecipeType<>() {
        @Override
        public String toString() {
            return "ami:fuel";
        }
    };

    static {
        try {
            net.minecraft.core.Registry.register(net.minecraft.core.registries.BuiltInRegistries.RECIPE_TYPE,
                    ResourceLocation.fromNamespaceAndPath("ami", "brewing"), BREWING);
            net.minecraft.core.Registry.register(net.minecraft.core.registries.BuiltInRegistries.RECIPE_TYPE,
                    ResourceLocation.fromNamespaceAndPath("ami", "grinding"), GRINDING);
            net.minecraft.core.Registry.register(net.minecraft.core.registries.BuiltInRegistries.RECIPE_TYPE,
                    ResourceLocation.fromNamespaceAndPath("ami", "anvil_repairing"), ANVIL_REPAIRING);
            net.minecraft.core.Registry.register(net.minecraft.core.registries.BuiltInRegistries.RECIPE_TYPE,
                    ResourceLocation.fromNamespaceAndPath("ami", "composting"), COMPOSTING);
            net.minecraft.core.Registry.register(net.minecraft.core.registries.BuiltInRegistries.RECIPE_TYPE,
                    ResourceLocation.fromNamespaceAndPath("ami", "fuel"), FUEL);
        } catch (Exception ignored) {
        }
    }

    private static final AmiRecipeIndex INSTANCE = new AmiRecipeIndex();

    private AmiRecipeIndex() {
    }

    public static AmiRecipeIndex getInstance() {
        return INSTANCE;
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
            AmiFabric.LOGGER.warn("AmiRecipeIndex rebuild failed: {}", e.getMessage());
            return;
        }
        finishRebuild();
    }

    private void indexBrewing(Level level) {
        try {
            var brewing = level.potionBrewing();
            if (brewing == null) return;

            // Direct field/accessor access (the lists + the package-private Mix record are
            // access-widened in ami.accesswidener). Mojmap-name reflection would fail on
            // Fabric's intermediary runtime; direct access is remapped by Loom.
            Item[] containers = {
                    Items.POTION,
                    Items.SPLASH_POTION,
                    Items.LINGERING_POTION
            };

            int idCounter = 0;

            // 1. Process Potion Contents Mixes (input potion -> output potion via ingredient)
            for (var mix : brewing.potionMixes) {
                var fromHolder = mix.from();
                var ingredient = mix.ingredient();
                var toHolder = mix.to();
                if (fromHolder == null || toHolder == null || ingredient == null) continue;

                for (Item container : containers) {
                    ItemStack inStack = PotionContents.createItemStack(container, fromHolder);
                    ItemStack outStack = PotionContents.createItemStack(container, toHolder);

                    var recipe = new PotionBrewingRecipe(inStack, ingredient, outStack, BREWING);
                    AmiRecipeHolder<?> holder = wrap(new RecipeHolder<>(
                            ResourceLocation.fromNamespaceAndPath("ami", "brewing_potion_" + idCounter++),
                            recipe
                    ));

                    addOutput(outStack, holder);
                    addInput(inStack, holder);
                    addIngredientInputs(ingredient, holder);
                }
            }

            // 2. Process Container Mixes (input container -> output container, across all potions)
            for (var mix : brewing.containerMixes) {
                var fromHolder = mix.from();
                var ingredient = mix.ingredient();
                var toHolder = mix.to();
                if (fromHolder == null || toHolder == null || ingredient == null) continue;

                Item fromItem = fromHolder.value();
                Item toItem = toHolder.value();

                for (var potionRef : BuiltInRegistries.POTION.holders().toList()) {
                    ItemStack inStack = PotionContents.createItemStack(fromItem, potionRef);
                    ItemStack outStack = PotionContents.createItemStack(toItem, potionRef);

                    var recipe = new PotionBrewingRecipe(inStack, ingredient, outStack, BREWING);
                    AmiRecipeHolder<?> holder = wrap(new RecipeHolder<>(
                            ResourceLocation.fromNamespaceAndPath("ami", "brewing_container_" + idCounter++),
                            recipe
                    ));

                    addOutput(outStack, holder);
                    addInput(inStack, holder);
                    addIngredientInputs(ingredient, holder);
                }
            }

        } catch (Exception e) {
            AmiFabric.LOGGER.warn("AmiRecipeIndex indexBrewing failed: {}", e.getMessage());
        }
    }

    private void indexRepairs(Level level) {
        try {
            int idCounter = 0;

            for (Item item : BuiltInRegistries.ITEM) {
                ItemStack defaultStack = item.getDefaultInstance();
                int maxDamage = defaultStack.getMaxDamage();
                if (maxDamage <= 0) continue;

                // 1. Material-based Anvil Repair
                Ingredient repairMaterial = null;
                if (item instanceof ArmorItem ai) {
                    if (ai.getMaterial() != null && ai.getMaterial().value().repairIngredient() != null) {
                        repairMaterial = ai.getMaterial().value().repairIngredient().get();
                    }
                } else if (item instanceof TieredItem ti) {
                    if (ti.getTier() != null && ti.getTier().getRepairIngredient() != null) {
                        repairMaterial = ti.getTier().getRepairIngredient();
                    }
                } else if (item == Items.ELYTRA) {
                    repairMaterial = Ingredient.of(Items.PHANTOM_MEMBRANE);
                } else if (item == Items.SHIELD) {
                    repairMaterial = Ingredient.of(ItemTags.PLANKS);
                }

                if (repairMaterial != null && !repairMaterial.isEmpty()) {
                    ItemStack damagedInput = defaultStack.copy();
                    damagedInput.setDamageValue((int) (maxDamage * 0.7));

                    ItemStack repairedOutput = defaultStack.copy();
                    repairedOutput.setDamageValue((int) (maxDamage * 0.2));

                    var anvilRecipe = new AnvilRepairRecipe(damagedInput, repairMaterial, repairedOutput, ANVIL_REPAIRING);
                    AmiRecipeHolder<?> holder = wrap(new RecipeHolder<>(
                            ResourceLocation.fromNamespaceAndPath("ami", "anvil_material_repair_" + idCounter++),
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

                var anvilToolRecipe = new AnvilRepairRecipe(damaged1, Ingredient.of(damaged2), combinedRepaired, ANVIL_REPAIRING);
                AmiRecipeHolder<?> anvilHolder = wrap(new RecipeHolder<>(
                        ResourceLocation.fromNamespaceAndPath("ami", "anvil_tool_repair_" + idCounter++),
                        anvilToolRecipe
                ));

                var grindstoneRecipe = new GrindstoneRepairRecipe(damaged1, damaged2, combinedRepaired, GRINDING);
                AmiRecipeHolder<?> grindstoneHolder = wrap(new RecipeHolder<>(
                        ResourceLocation.fromNamespaceAndPath("ami", "grindstone_tool_repair_" + idCounter++),
                        grindstoneRecipe
                ));

                addOutput(item, anvilHolder);
                addInput(item, anvilHolder);

                addOutput(item, grindstoneHolder);
                addInput(item, grindstoneHolder);
            }
        } catch (Exception e) {
            AmiFabric.LOGGER.warn("AmiRecipeIndex indexRepairs failed: {}", e.getMessage());
        }
    }

    private void indexComposting() {
        try {
            int idCounter = 0;

            for (Map.Entry<net.minecraft.world.level.ItemLike, Float> entry : ComposterBlock.COMPOSTABLES.entrySet()) {
                Item item = entry.getKey().asItem();
                float chance = entry.getValue();

                ItemStack stack = item.getDefaultInstance();
                var recipe = new CompostingRecipe(stack, chance, COMPOSTING);
                AmiRecipeHolder<?> holder = wrap(new RecipeHolder<>(
                        ResourceLocation.fromNamespaceAndPath("ami", "composting_" + idCounter++),
                        recipe
                ));

                addOutput(Items.BONE_MEAL, holder);
                addInput(item, holder);
            }
        } catch (Exception e) {
            AmiFabric.LOGGER.warn("AmiRecipeIndex indexComposting failed: {}", e.getMessage());
        }
    }

    private void indexFuels() {
        try {
            int idCounter = 0;

            // On Fabric/vanilla, use AbstractFurnaceBlockEntity.getFuel() which returns
            // a Map<Item, Integer> of item → burn time in ticks. This is the authoritative
            // vanilla fuel source and is equivalent to what NeoForge's getBurnTime() reads.
            Map<Item, Integer> fuelMap = AbstractFurnaceBlockEntity.getFuel();

            for (Map.Entry<Item, Integer> entry : fuelMap.entrySet()) {
                Item item = entry.getKey();
                int burnTime = entry.getValue();
                if (burnTime <= 0) continue;

                ItemStack stack = item.getDefaultInstance();
                var recipe = new FuelRecipe(stack, burnTime, FUEL);
                AmiRecipeHolder<?> holder = wrap(new RecipeHolder<>(
                        ResourceLocation.fromNamespaceAndPath("ami", "fuel_" + idCounter++),
                        recipe
                ));

                addInput(item, holder);
            }
        } catch (Exception e) {
            AmiFabric.LOGGER.warn("AmiRecipeIndex indexFuels failed: {}", e.getMessage());
        }
    }

    private void indexDisenchanting(Level level) {
        try {
            int idCounter = 0;
            var enchantmentRegistry = level.registryAccess().registryOrThrow(net.minecraft.core.registries.Registries.ENCHANTMENT);
            var firstEnchantOpt = enchantmentRegistry.holders().findFirst();

            for (Item item : BuiltInRegistries.ITEM) {
                ItemStack defaultStack = item.getDefaultInstance();
                int maxDamage = defaultStack.getMaxDamage();
                if (maxDamage <= 0) continue;

                ItemStack enchantedInput = defaultStack.copy();
                if (firstEnchantOpt.isPresent()) {
                    enchantedInput.enchant(firstEnchantOpt.get(), 1);
                }

                var recipe = new GrindstoneDisenchantingRecipe(enchantedInput, defaultStack, GRINDING);
                AmiRecipeHolder<?> holder = wrap(new RecipeHolder<>(
                        ResourceLocation.fromNamespaceAndPath("ami", "disenchanting_tool_" + idCounter++),
                        recipe
                ));

                addOutput(item, holder);
                addInput(item, holder);
            }

            ItemStack enchantedBook = new ItemStack(Items.ENCHANTED_BOOK);
            if (firstEnchantOpt.isPresent()) {
                enchantedBook.enchant(firstEnchantOpt.get(), 1);
            }
            ItemStack normalBook = new ItemStack(Items.BOOK);

            var bookRecipe = new GrindstoneDisenchantingRecipe(enchantedBook, normalBook, GRINDING);
            AmiRecipeHolder<?> bookHolder = wrap(new RecipeHolder<>(
                    ResourceLocation.fromNamespaceAndPath("ami", "disenchanting_book_" + idCounter++),
                    bookRecipe
            ));

            addOutput(Items.BOOK, bookHolder);
            addInput(Items.ENCHANTED_BOOK, bookHolder);

        } catch (Exception e) {
            AmiFabric.LOGGER.warn("AmiRecipeIndex indexDisenchanting failed: {}", e.getMessage());
        }
    }

    private void indexEnchanting(Level level) {
        try {
            int idCounter = 0;
            var enchantmentRegistry = level.registryAccess().registryOrThrow(net.minecraft.core.registries.Registries.ENCHANTMENT);

            for (Item item : BuiltInRegistries.ITEM) {
                ItemStack toolStack = item.getDefaultInstance();
                if (!toolStack.isEnchantable() && toolStack.getMaxDamage() <= 0) continue;

                for (net.minecraft.core.Holder.Reference<net.minecraft.world.item.enchantment.Enchantment> enchantRef : enchantmentRegistry.holders().toList()) {
                    net.minecraft.world.item.enchantment.Enchantment enchantment = enchantRef.value();

                    if (enchantment.canEnchant(toolStack)) {
                        int maxLevel = enchantment.getMaxLevel();

                        ItemStack enchantedBook = new ItemStack(Items.ENCHANTED_BOOK);
                        enchantedBook.enchant(enchantRef, maxLevel);

                        ItemStack enchantedTool = toolStack.copy();
                        enchantedTool.enchant(enchantRef, maxLevel);

                        var recipe = new AnvilEnchantingRecipe(toolStack, enchantedBook, enchantedTool, ANVIL_REPAIRING);
                        AmiRecipeHolder<?> holder = wrap(new RecipeHolder<>(
                                ResourceLocation.fromNamespaceAndPath("ami", "enchanting_" + idCounter++),
                                recipe
                        ));

                        addOutput(item, holder);
                        addInput(item, holder);
                        addInput(Items.ENCHANTED_BOOK, holder);
                    }
                }
            }
        } catch (Exception e) {
            AmiFabric.LOGGER.warn("AmiRecipeIndex indexEnchanting failed: {}", e.getMessage());
        }
    }
}
