package com.sanhiruzu.ami.client.recipe;

import com.sanhiruzu.ami.platform.Services;
import com.sanhiruzu.ami.recipe.special.*;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.crafting.ShapedRecipe;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

public final class RecipeDisplayHelper {
    private static final int CRAFTING_GRID_ORIGIN_X = 4;
    private static final int CRAFTING_GRID_ORIGIN_Y = 4;
    private static final int CRAFTING_GRID_SIZE = 3;

    private RecipeDisplayHelper() {
    }

    public static RecipeLayout getLayout(com.sanhiruzu.ami.util.AmiRecipeHolder<?> entry, net.minecraft.core.RegistryAccess registryAccess) {
        Recipe<?> recipe = entry.value();
        RecipeType<?> type = recipe.getType();
        String categoryName = getCategoryName(type);
        ItemStack output = getResultSafe(recipe, registryAccess);
        ItemStack categoryIcon = getCategoryIcon(recipe, registryAccess);

        List<SlotPosition> inputs = new ArrayList<>();
        int gridW, gridH;
        boolean shapeless = false;
        int outputX, outputY;
        int arrowX, arrowY;

        ResourceLocation backgroundTexture = null;
        int bgX = 0, bgY = 0, bgW = 0, bgH = 0;
        int bgRenderX = 0, bgRenderY = 0;
        boolean drawSlotBackground = true;

        if (type == RecipeType.CRAFTING) {
            shapeless = !(recipe instanceof ShapedRecipe);
            // No vanilla texture — use AMI-styled slots for visual consistency.
            // drawSlotBackground stays true (default).
            List<Ingredient> ingredients = recipe.getIngredients();
            List<List<ItemStack>> ingredientAlternatives = new ArrayList<>(ingredients.size());
            int recipeWidth = 0;
            int recipeHeight = 0;
            if (recipe instanceof ShapedRecipe shaped) {
                recipeWidth = shaped.getWidth();
                recipeHeight = shaped.getHeight();
            }
            for (Ingredient ingredient : ingredients) {
                ingredientAlternatives.add(ingredient.isEmpty() ? List.of() : List.of(ingredient.getItems()));
            }
            inputs.addAll(createCraftingGridSlots(ingredientAlternatives, recipeWidth, recipeHeight));

            gridW = CRAFTING_GRID_SIZE;
            gridH = CRAFTING_GRID_SIZE;
            int gridEndX = CRAFTING_GRID_ORIGIN_X + CRAFTING_GRID_SIZE * 18;
            int gridMidY = CRAFTING_GRID_ORIGIN_Y + CRAFTING_GRID_SIZE * 9 - 9;
            arrowX = gridEndX + 4;
            arrowY = gridMidY;
            outputX = arrowX + 26;
            outputY = gridMidY;

        } else if (isFurnaceType(type)) {
            backgroundTexture = Services.PLATFORM.rl("minecraft", "textures/gui/container/furnace.png");
            bgX = 55;
            bgY = 16;
            bgW = 82;
            bgH = 54;
            bgRenderX = 36;
            bgRenderY = 4;
            drawSlotBackground = false;

            List<Ingredient> ingredients = recipe.getIngredients();
            if (!ingredients.isEmpty()) {
                inputs.add(new SlotPosition(bgRenderX, bgRenderY, List.of(ingredients.get(0).getItems())));
            } else {
                inputs.add(new SlotPosition(bgRenderX, bgRenderY, List.of()));
            }
            inputs.add(new SlotPosition(bgRenderX, bgRenderY + 36, List.of()));

            gridW = 1;
            gridH = 2;
            arrowX = bgRenderX + 24;
            arrowY = bgRenderY + 18;
            outputX = bgRenderX + 61;
            outputY = bgRenderY + 19;

        } else if (type == RecipeType.SMITHING) {
            backgroundTexture = Services.PLATFORM.rl("minecraft", "textures/gui/container/smithing.png");
            bgX = 7;
            bgY = 47;
            bgW = 116;
            bgH = 26;
            bgRenderX = 20;
            bgRenderY = 18;
            drawSlotBackground = false;

            List<Ingredient> ingredients = recipe.getIngredients();
            for (int i = 0; i < 3; i++) {
                Ingredient ing = (i < ingredients.size()) ? ingredients.get(i) : Ingredient.EMPTY;
                inputs.add(new SlotPosition(bgRenderX + i * 18, bgRenderY, ing.isEmpty() ? List.of() : List.of(ing.getItems())));
            }

            gridW = 3;
            gridH = 1;
            arrowX = bgRenderX + 61;
            arrowY = bgRenderY;
            outputX = bgRenderX + 91;
            outputY = bgRenderY;

        } else if (type == RecipeType.STONECUTTING) {
            backgroundTexture = Services.PLATFORM.rl("minecraft", "textures/gui/container/stonecutter.png");
            bgX = 19;
            bgY = 32;
            bgW = 142;
            bgH = 20;
            bgRenderX = 10;
            bgRenderY = 18;
            drawSlotBackground = false;

            List<Ingredient> ingredients = recipe.getIngredients();
            if (!ingredients.isEmpty()) {
                inputs.add(new SlotPosition(bgRenderX, bgRenderY, List.of(ingredients.get(0).getItems())));
            }

            gridW = 1;
            gridH = 1;
            arrowX = bgRenderX + 68;
            arrowY = bgRenderY;
            outputX = bgRenderX + 124;
            outputY = bgRenderY;

        } else if (type.toString().equals("ami:brewing")) {
            backgroundTexture = Services.PLATFORM.rl("minecraft", "textures/gui/container/brewing_stand.png");
            bgX = 16;
            bgY = 14;
            bgW = 103;
            bgH = 61;
            bgRenderX = 28;
            bgRenderY = 4;
            drawSlotBackground = false;

            if (recipe instanceof PotionBrewingRecipeView pbr) {
                inputs.add(new SlotPosition(bgRenderX + 39, bgRenderY + 36, List.of(pbr.getInput())));
                inputs.add(new SlotPosition(bgRenderX + 62, bgRenderY + 2, List.of(pbr.getIngredient().getItems())));
            }
            inputs.add(new SlotPosition(bgRenderX, bgRenderY + 2, List.of()));

            gridW = 1;
            gridH = 1;
            arrowX = bgRenderX + 44;
            arrowY = bgRenderY + 30;
            outputX = bgRenderX + 85;
            outputY = bgRenderY + 36;

        } else if (type.toString().equals("ami:grinding")) {
            backgroundTexture = Services.PLATFORM.rl("minecraft", "textures/gui/container/grindstone.png");
            bgX = 30;
            bgY = 15;
            bgW = 116;
            bgH = 56;
            bgRenderX = 20;
            bgRenderY = 4;
            drawSlotBackground = false;

            if (recipe instanceof GrindstoneRepairRecipeView grr) {
                inputs.add(new SlotPosition(bgRenderX + 18, bgRenderY + 3, List.of(grr.getTool1())));
                inputs.add(new SlotPosition(bgRenderX + 18, bgRenderY + 24, List.of(grr.getTool2())));
            }

            gridW = 1;
            gridH = 2;
            arrowX = bgRenderX + 50;
            arrowY = bgRenderY + 18;
            outputX = bgRenderX + 98;
            outputY = bgRenderY + 18;

        } else if (type.toString().equals("ami:anvil_repairing")) {
            backgroundTexture = Services.PLATFORM.rl("minecraft", "textures/gui/container/anvil.png");
            bgX = 26;
            bgY = 46;
            bgW = 126;
            bgH = 20;
            bgRenderX = 16;
            bgRenderY = 18;
            drawSlotBackground = false;

            if (recipe instanceof AnvilRepairRecipeView arr) {
                inputs.add(new SlotPosition(bgRenderX, bgRenderY, List.of(arr.getTool())));
                inputs.add(new SlotPosition(bgRenderX + 49, bgRenderY, List.of(arr.getMaterial().getItems())));
            }

            gridW = 2;
            gridH = 1;
            arrowX = bgRenderX + 76;
            arrowY = bgRenderY;
            outputX = bgRenderX + 108;
            outputY = bgRenderY;

        } else if (type.toString().equals("ami:composting")) {
            drawSlotBackground = true;
            if (recipe instanceof CompostingRecipeView cr) {
                inputs.add(new SlotPosition(20, 18, List.of(cr.getStack())));
            }

            gridW = 1;
            gridH = 1;
            arrowX = 56;
            arrowY = 18;
            outputX = 92;
            outputY = 14;

        } else if (type.toString().equals("ami:fuel")) {
            drawSlotBackground = true;
            if (recipe instanceof FuelRecipeView fr) {
                inputs.add(new SlotPosition(36, 18, List.of(fr.getStack())));
            }

            gridW = 1;
            gridH = 1;
            arrowX = 36; // Keep the same to avoid offsets
            arrowY = 18;
            outputX = 36;
            outputY = 18;

        } else if (type.toString().equals("ami:disenchanting")) {
            backgroundTexture = Services.PLATFORM.rl("minecraft", "textures/gui/container/grindstone.png");
            bgX = 30;
            bgY = 15;
            bgW = 116;
            bgH = 56;
            bgRenderX = 20;
            bgRenderY = 4;
            drawSlotBackground = false;

            if (recipe instanceof GrindstoneDisenchantingRecipeView gdr) {
                inputs.add(new SlotPosition(bgRenderX + 18, bgRenderY + 3, List.of(gdr.getEnchanted())));
            }

            gridW = 1;
            gridH = 1;
            arrowX = bgRenderX + 50;
            arrowY = bgRenderY + 18;
            outputX = bgRenderX + 98;
            outputY = bgRenderY + 18;

        } else if (type.toString().equals("ami:enchanting")) {
            backgroundTexture = Services.PLATFORM.rl("minecraft", "textures/gui/container/anvil.png");
            bgX = 26;
            bgY = 46;
            bgW = 126;
            bgH = 20;
            bgRenderX = 16;
            bgRenderY = 18;
            drawSlotBackground = false;

            if (recipe instanceof AnvilEnchantingRecipeView aer) {
                inputs.add(new SlotPosition(bgRenderX, bgRenderY, List.of(aer.getTool())));
                inputs.add(new SlotPosition(bgRenderX + 49, bgRenderY, List.of(aer.getBook())));
            }

            gridW = 2;
            gridH = 1;
            arrowX = bgRenderX + 76;
            arrowY = bgRenderY;
            outputX = bgRenderX + 108;
            outputY = bgRenderY;

        } else {
            // Generic fallback for unknown/mod recipe types.
            GenericFallbackLayout fallbackLayout = createGenericFallbackLayout(
                    recipe.getIngredients().stream()
                            .map(ingredient -> List.of(ingredient.getItems()))
                            .toList());
            inputs.addAll(fallbackLayout.inputs());
            gridW = fallbackLayout.gridWidth();
            gridH = fallbackLayout.gridHeight();
            outputX = fallbackLayout.outputX();
            outputY = fallbackLayout.outputY();
            arrowX = fallbackLayout.arrowX();
            arrowY = fallbackLayout.arrowY();
        }

        return new RecipeLayout(
                entry.getId(), categoryIcon, categoryName, inputs, output, gridW, gridH, shapeless,
                outputX, outputY, arrowX, arrowY,
                backgroundTexture, bgX, bgY, bgW, bgH, bgRenderX, bgRenderY, drawSlotBackground
        );
    }

    static GenericFallbackLayout createGenericFallbackLayout(List<List<ItemStack>> ingredientAlternatives) {
        List<List<ItemStack>> nonEmpty = new ArrayList<>();
        for (List<ItemStack> alternatives : ingredientAlternatives) {
            if (alternatives != null && !alternatives.isEmpty()) {
                nonEmpty.add(alternatives);
            }
        }

        if (nonEmpty.isEmpty()) {
            return new GenericFallbackLayout(List.of(), 0, 0, 44, 0, 20, 4);
        }

        int inputCount = nonEmpty.size();
        int cols = switch (inputCount) {
            case 1 -> 1;
            case 2, 3 -> inputCount;
            case 4 -> 2;
            default -> 3;
        };
        int rows = (int) Math.ceil((double) inputCount / cols);

        List<SlotPosition> inputs = new ArrayList<>();
        for (int i = 0; i < inputCount; i++) {
            int col = i % cols;
            int row = i / cols;
            inputs.add(new SlotPosition(col * 18, row * 18, nonEmpty.get(i)));
        }

        int inputAreaWidth = cols * 18;
        int inputAreaHeight = rows * 18;
        int arrowX = inputAreaWidth + 4;
        int arrowY = Math.max(0, (inputAreaHeight - 16) / 2);
        int outputX = arrowX + 26;
        int outputY = Math.max(0, (inputAreaHeight - 18) / 2);

        return new GenericFallbackLayout(inputs, cols, rows, outputX, outputY, arrowX, arrowY);
    }

    static List<SlotPosition> createCraftingGridSlots(List<List<ItemStack>> ingredientAlternatives, int width, int height) {
        List<List<ItemStack>> alternativesBySlot = new ArrayList<>(CRAFTING_GRID_SIZE * CRAFTING_GRID_SIZE);
        for (int i = 0; i < CRAFTING_GRID_SIZE * CRAFTING_GRID_SIZE; i++) {
            alternativesBySlot.add(List.of());
        }

        for (int i = 0; i < ingredientAlternatives.size(); i++) {
            int slotIndex = craftingGridIndex(i, width, height);
            if (slotIndex < 0 || slotIndex >= alternativesBySlot.size()) {
                continue;
            }
            List<ItemStack> alternatives = ingredientAlternatives.get(i);
            alternativesBySlot.set(slotIndex, alternatives == null ? List.of() : alternatives);
        }

        List<SlotPosition> slots = new ArrayList<>(alternativesBySlot.size());
        for (int i = 0; i < alternativesBySlot.size(); i++) {
            int col = i % CRAFTING_GRID_SIZE;
            int row = i / CRAFTING_GRID_SIZE;
            slots.add(new SlotPosition(
                    CRAFTING_GRID_ORIGIN_X + col * 18,
                    CRAFTING_GRID_ORIGIN_Y + row * 18,
                    alternativesBySlot.get(i)));
        }
        return slots;
    }

    private static int craftingGridIndex(int ingredientIndex, int width, int height) {
        if (width <= 0 || height <= 0) {
            return ingredientIndex;
        }
        if (width == 1) {
            if (height == 3) {
                return ingredientIndex * 3 + 1;
            }
            if (height == 2) {
                return ingredientIndex * 3 + 1;
            }
            return 4;
        }
        if (height == 1) {
            return ingredientIndex + 3;
        }
        if (width == 2) {
            int index = ingredientIndex;
            if (ingredientIndex > 1) {
                index++;
                if (ingredientIndex > 3) {
                    index++;
                }
            }
            return index;
        }
        if (height == 2) {
            return ingredientIndex + 3;
        }
        return ingredientIndex;
    }

    /** Returns true for recipe types that have a dedicated bespoke renderer (no generic fallback). */
    public static boolean hasDedicatedLayout(RecipeType<?> type) {
        if (type == RecipeType.CRAFTING || type == RecipeType.SMITHING || type == RecipeType.STONECUTTING)
            return true;
        if (isFurnaceType(type)) return true;
        String t = type.toString();
        return t.equals("ami:brewing") || t.equals("ami:grinding") || t.equals("ami:anvil_repairing")
                || t.equals("ami:composting") || t.equals("ami:fuel") || t.equals("ami:disenchanting")
                || t.equals("ami:enchanting");
    }

    public static boolean isFurnaceType(RecipeType<?> type) {
        return type == RecipeType.SMELTING
                || type == RecipeType.BLASTING
                || type == RecipeType.SMOKING
                || type == RecipeType.CAMPFIRE_COOKING;
    }

    private static String getCategoryName(RecipeType<?> type) {
        if (com.sanhiruzu.ami.recipe.AmiRecipeIndex.isEmiCategoryType(type)) {
            String name = com.sanhiruzu.ami.recipe.AmiRecipeIndex.getEmiCategoryName(type);
            return name != null ? name : type.toString();
        }
        ResourceLocation key = BuiltInRegistries.RECIPE_TYPE.getKey(type);
        if (key == null) return type.toString().toLowerCase();
        return "minecraft".equals(key.getNamespace()) ? key.getPath() : key.toString();
    }

    private static ItemStack getResultSafe(Recipe<?> recipe, net.minecraft.core.RegistryAccess registryAccess) {
        try {
            return recipe.getResultItem(registryAccess);
        } catch (Exception e) {
            return ItemStack.EMPTY;
        }
    }

    private static ItemStack getCategoryIcon(Recipe<?> recipe, net.minecraft.core.RegistryAccess registryAccess) {
        try {
            ItemStack icon = recipe.getToastSymbol();
            if (icon != null && !icon.isEmpty()) return icon;
        } catch (Exception ignored) {
        }
        return ItemStack.EMPTY;
    }

    public static Component tabComponent(RecipeType<?> type) {
        if (com.sanhiruzu.ami.recipe.AmiRecipeIndex.isEmiCategoryType(type)) {
            String name = com.sanhiruzu.ami.recipe.AmiRecipeIndex.getEmiCategoryName(type);
            return name != null ? Component.literal(name) : Component.literal(type.toString());
        }
        ResourceLocation key = BuiltInRegistries.RECIPE_TYPE.getKey(type);
        if (key == null) {
            String raw = type.toString();
            if (raw.startsWith("ami:")) {
                return Component.translatable("ami.recipe_viewer.tab." + raw.substring(4));
            }
            return Component.literal(raw);
        }
        String langKey = "ami.recipe_viewer.tab." + key.getPath();
        if ("minecraft".equals(key.getNamespace()) || "ami".equals(key.getNamespace())) {
            return Component.translatable(langKey);
        }
        return Component.literal(key.toString());
    }

    public static String tabShortLabel(RecipeType<?> type) {
        if (com.sanhiruzu.ami.recipe.AmiRecipeIndex.isEmiCategoryType(type)) {
            String name = com.sanhiruzu.ami.recipe.AmiRecipeIndex.getEmiCategoryName(type);
            if (name != null) return name.length() > 8 ? name.substring(0, 8) : name;
        }
        ResourceLocation key = BuiltInRegistries.RECIPE_TYPE.getKey(type);
        if (key == null) {
            String raw = type.toString();
            if (raw.startsWith("ami:")) {
                return Component.translatable("ami.recipe_viewer.tab.short." + raw.substring(4)).getString();
            }
            return raw.substring(0, Math.min(6, raw.length()));
        }
        if ("minecraft".equals(key.getNamespace()) || "ami".equals(key.getNamespace())) {
            return Component.translatable("ami.recipe_viewer.tab.short." + key.getPath()).getString();
        }
        String path = key.getPath();
        int slash = path.lastIndexOf('/');
        if (slash >= 0) path = path.substring(slash + 1);
        return path.length() > 8 ? path.substring(0, 8) : path;
    }

    public static ItemStack getRepresentativeWorkstation(RecipeType<?> type) {
        if (com.sanhiruzu.ami.recipe.AmiRecipeIndex.isEmiCategoryType(type)) {
            return com.sanhiruzu.ami.recipe.AmiRecipeIndex.getEmiCategoryIcon(type);
        }
        if (type == RecipeType.CRAFTING) return new ItemStack(net.minecraft.world.item.Items.CRAFTING_TABLE);
        if (type == RecipeType.SMELTING) return new ItemStack(net.minecraft.world.item.Items.FURNACE);
        if (type == RecipeType.BLASTING) return new ItemStack(net.minecraft.world.item.Items.BLAST_FURNACE);
        if (type == RecipeType.SMOKING) return new ItemStack(net.minecraft.world.item.Items.SMOKER);
        if (type == RecipeType.CAMPFIRE_COOKING) return new ItemStack(net.minecraft.world.item.Items.CAMPFIRE);
        if (type == RecipeType.STONECUTTING) return new ItemStack(net.minecraft.world.item.Items.STONECUTTER);
        if (type == RecipeType.SMITHING) return new ItemStack(net.minecraft.world.item.Items.SMITHING_TABLE);

        String tStr = type.toString();
        if (tStr.equals("ami:brewing")) return new ItemStack(net.minecraft.world.item.Items.BREWING_STAND);
        if (tStr.equals("ami:grinding") || tStr.equals("ami:disenchanting"))
            return new ItemStack(net.minecraft.world.item.Items.GRINDSTONE);
        if (tStr.equals("ami:anvil_repairing") || tStr.equals("ami:enchanting"))
            return new ItemStack(net.minecraft.world.item.Items.ANVIL);
        if (tStr.equals("ami:composting")) return new ItemStack(net.minecraft.world.item.Items.COMPOSTER);
        if (tStr.equals("ami:fuel")) return new ItemStack(net.minecraft.world.item.Items.COAL);

        return ItemStack.EMPTY;
    }

    public static List<ItemStack> getWorkstations(RecipeType<?> type) {
        if (type == RecipeType.CRAFTING) return List.of(new ItemStack(net.minecraft.world.item.Items.CRAFTING_TABLE));
        if (type == RecipeType.SMELTING) return List.of(new ItemStack(net.minecraft.world.item.Items.FURNACE));
        if (type == RecipeType.BLASTING) return List.of(new ItemStack(net.minecraft.world.item.Items.BLAST_FURNACE));
        if (type == RecipeType.SMOKING) return List.of(new ItemStack(net.minecraft.world.item.Items.SMOKER));
        if (type == RecipeType.CAMPFIRE_COOKING) return List.of(
                new ItemStack(net.minecraft.world.item.Items.CAMPFIRE),
                new ItemStack(net.minecraft.world.item.Items.SOUL_CAMPFIRE)
        );
        if (type == RecipeType.STONECUTTING) return List.of(new ItemStack(net.minecraft.world.item.Items.STONECUTTER));
        if (type == RecipeType.SMITHING) return List.of(new ItemStack(net.minecraft.world.item.Items.SMITHING_TABLE));

        String tStr = type.toString();
        if (tStr.equals("ami:brewing")) return List.of(new ItemStack(net.minecraft.world.item.Items.BREWING_STAND));
        if (tStr.equals("ami:grinding") || tStr.equals("ami:disenchanting"))
            return List.of(new ItemStack(net.minecraft.world.item.Items.GRINDSTONE));
        if (tStr.equals("ami:anvil_repairing") || tStr.equals("ami:enchanting")) return List.of(
                new ItemStack(net.minecraft.world.item.Items.ANVIL),
                new ItemStack(net.minecraft.world.item.Items.CHIPPED_ANVIL),
                new ItemStack(net.minecraft.world.item.Items.DAMAGED_ANVIL)
        );
        if (tStr.equals("ami:composting")) return List.of(new ItemStack(net.minecraft.world.item.Items.COMPOSTER));
        if (tStr.equals("ami:fuel")) return List.of(
                new ItemStack(net.minecraft.world.item.Items.FURNACE),
                new ItemStack(net.minecraft.world.item.Items.BLAST_FURNACE),
                new ItemStack(net.minecraft.world.item.Items.SMOKER)
        );

        return List.of();
    }

    public record SlotPosition(int x, int y, List<ItemStack> alternatives) {
    }

    record GenericFallbackLayout(
            List<SlotPosition> inputs,
            int gridWidth,
            int gridHeight,
            int outputX,
            int outputY,
            int arrowX,
            int arrowY
    ) {
    }

    public record RecipeLayout(
            ResourceLocation recipeId,
            ItemStack categoryIcon,
            String categoryName,
            List<SlotPosition> inputs,
            ItemStack output,
            int gridWidth,
            int gridHeight,
            boolean shapeless,
            int outputX,
            int outputY,
            int arrowX,
            int arrowY,
            @Nullable ResourceLocation backgroundTexture,
            int bgX,
            int bgY,
            int bgW,
            int bgH,
            int bgRenderX,
            int bgRenderY,
            boolean drawSlotBackground
    ) {
    }
}
