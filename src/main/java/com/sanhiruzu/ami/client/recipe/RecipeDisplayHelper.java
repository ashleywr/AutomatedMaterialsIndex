package com.sanhiruzu.ami.client.recipe;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.*;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

public final class RecipeDisplayHelper {
    private RecipeDisplayHelper() {}

    public record SlotPosition(int x, int y, List<ItemStack> alternatives) {}

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
    ) {}

    public static RecipeLayout getLayout(RecipeHolder<?> entry, net.minecraft.core.RegistryAccess registryAccess) {
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
            backgroundTexture = ResourceLocation.fromNamespaceAndPath("minecraft", "textures/gui/container/crafting_table.png");
            bgX = 29; bgY = 16; bgW = 116; bgH = 54;
            bgRenderX = 20; bgRenderY = 4;
            drawSlotBackground = false;

            List<Ingredient> ingredients = recipe.getIngredients();
            List<Ingredient> padded = new ArrayList<>();
            if (recipe instanceof ShapedRecipe shaped) {
                int w = shaped.getWidth();
                int h = shaped.getHeight();
                for (int y = 0; y < 3; y++) {
                    for (int x = 0; x < 3; x++) {
                        if (x < w && y < h) {
                            padded.add(ingredients.get(y * w + x));
                        } else {
                            padded.add(Ingredient.EMPTY);
                        }
                    }
                }
            } else {
                for (int i = 0; i < 9; i++) {
                    if (i < ingredients.size()) {
                        padded.add(ingredients.get(i));
                    } else {
                        padded.add(Ingredient.EMPTY);
                    }
                }
            }

            for (int i = 0; i < 9; i++) {
                int col = i % 3;
                int row = i / 3;
                Ingredient ing = padded.get(i);
                List<ItemStack> alternatives = ing.isEmpty() ? List.of() : List.of(ing.getItems());
                inputs.add(new SlotPosition(bgRenderX + col * 18 + 1, bgRenderY + row * 18 + 1, alternatives));
            }

            gridW = 3; gridH = 3;
            arrowX = bgRenderX + 61;
            arrowY = bgRenderY + 19;
            outputX = bgRenderX + 95;
            outputY = bgRenderY + 19;

        } else if (isFurnaceType(type)) {
            backgroundTexture = ResourceLocation.fromNamespaceAndPath("minecraft", "textures/gui/container/furnace.png");
            bgX = 55; bgY = 16; bgW = 82; bgH = 54;
            bgRenderX = 36; bgRenderY = 4;
            drawSlotBackground = false;

            List<Ingredient> ingredients = recipe.getIngredients();
            if (!ingredients.isEmpty()) {
                inputs.add(new SlotPosition(bgRenderX + 1, bgRenderY + 1, List.of(ingredients.get(0).getItems())));
            } else {
                inputs.add(new SlotPosition(bgRenderX + 1, bgRenderY + 1, List.of()));
            }
            inputs.add(new SlotPosition(bgRenderX + 1, bgRenderY + 37, List.of()));

            gridW = 1; gridH = 2;
            arrowX = bgRenderX + 24;
            arrowY = bgRenderY + 18;
            outputX = bgRenderX + 61;
            outputY = bgRenderY + 19;

        } else if (type == RecipeType.SMITHING) {
            backgroundTexture = ResourceLocation.fromNamespaceAndPath("minecraft", "textures/gui/container/smithing.png");
            bgX = 7; bgY = 47; bgW = 116; bgH = 26;
            bgRenderX = 20; bgRenderY = 18;
            drawSlotBackground = false;

            List<Ingredient> ingredients = recipe.getIngredients();
            for (int i = 0; i < 3; i++) {
                Ingredient ing = (i < ingredients.size()) ? ingredients.get(i) : Ingredient.EMPTY;
                inputs.add(new SlotPosition(bgRenderX + i * 18 + 1, bgRenderY + 1, ing.isEmpty() ? List.of() : List.of(ing.getItems())));
            }

            gridW = 3; gridH = 1;
            arrowX = bgRenderX + 61;
            arrowY = bgRenderY;
            outputX = bgRenderX + 91;
            outputY = bgRenderY + 1;

        } else if (type == RecipeType.STONECUTTING) {
            backgroundTexture = ResourceLocation.fromNamespaceAndPath("minecraft", "textures/gui/container/stonecutter.png");
            bgX = 19; bgY = 32; bgW = 142; bgH = 20;
            bgRenderX = 10; bgRenderY = 18;
            drawSlotBackground = false;

            List<Ingredient> ingredients = recipe.getIngredients();
            if (!ingredients.isEmpty()) {
                inputs.add(new SlotPosition(bgRenderX + 1, bgRenderY + 1, List.of(ingredients.get(0).getItems())));
            }

            gridW = 1; gridH = 1;
            arrowX = bgRenderX + 68;
            arrowY = bgRenderY + 1;
            outputX = bgRenderX + 124;
            outputY = bgRenderY + 1;

        } else if (type.toString().equals("ami:brewing")) {
            backgroundTexture = ResourceLocation.fromNamespaceAndPath("minecraft", "textures/gui/container/brewing_stand.png");
            bgX = 16; bgY = 14; bgW = 103; bgH = 61;
            bgRenderX = 28; bgRenderY = 4;
            drawSlotBackground = false;

            if (recipe instanceof com.sanhiruzu.ami.index.special.PotionBrewingRecipe pbr) {
                inputs.add(new SlotPosition(bgRenderX + 39, bgRenderY + 36, List.of(pbr.getInput())));
                inputs.add(new SlotPosition(bgRenderX + 62, bgRenderY + 2, List.of(pbr.getIngredient().getItems())));
            }
            inputs.add(new SlotPosition(bgRenderX, bgRenderY + 2, List.of()));

            gridW = 1; gridH = 1;
            arrowX = bgRenderX + 44;
            arrowY = bgRenderY + 30;
            outputX = bgRenderX + 85;
            outputY = bgRenderY + 36;

        } else if (type.toString().equals("ami:grinding")) {
            backgroundTexture = ResourceLocation.fromNamespaceAndPath("minecraft", "textures/gui/container/grindstone.png");
            bgX = 30; bgY = 15; bgW = 116; bgH = 56;
            bgRenderX = 20; bgRenderY = 4;
            drawSlotBackground = false;

            if (recipe instanceof com.sanhiruzu.ami.index.special.GrindstoneRepairRecipe grr) {
                inputs.add(new SlotPosition(bgRenderX + 18, bgRenderY + 3, List.of(grr.getTool1())));
                inputs.add(new SlotPosition(bgRenderX + 18, bgRenderY + 24, List.of(grr.getTool2())));
            }

            gridW = 1; gridH = 2;
            arrowX = bgRenderX + 50;
            arrowY = bgRenderY + 18;
            outputX = bgRenderX + 98;
            outputY = bgRenderY + 18;

        } else if (type.toString().equals("ami:anvil_repairing")) {
            backgroundTexture = ResourceLocation.fromNamespaceAndPath("minecraft", "textures/gui/container/anvil.png");
            bgX = 26; bgY = 46; bgW = 126; bgH = 20;
            bgRenderX = 16; bgRenderY = 18;
            drawSlotBackground = false;

            if (recipe instanceof com.sanhiruzu.ami.index.special.AnvilRepairRecipe arr) {
                inputs.add(new SlotPosition(bgRenderX + 1, bgRenderY + 1, List.of(arr.getTool())));
                inputs.add(new SlotPosition(bgRenderX + 50, bgRenderY + 1, List.of(arr.getMaterial().getItems())));
            }

            gridW = 2; gridH = 1;
            arrowX = bgRenderX + 76;
            arrowY = bgRenderY;
            outputX = bgRenderX + 108;
            outputY = bgRenderY + 1;

        } else if (type.toString().equals("ami:composting")) {
            drawSlotBackground = true;
            if (recipe instanceof com.sanhiruzu.ami.index.special.CompostingRecipe cr) {
                inputs.add(new SlotPosition(20, 18, List.of(cr.getStack())));
            }

            gridW = 1; gridH = 1;
            arrowX = 56;
            arrowY = 18;
            outputX = 92;
            outputY = 14;

        } else if (type.toString().equals("ami:fuel")) {
            drawSlotBackground = true;
            if (recipe instanceof com.sanhiruzu.ami.index.special.FuelRecipe fr) {
                inputs.add(new SlotPosition(36, 18, List.of(fr.getStack())));
            }

            gridW = 1; gridH = 1;
            arrowX = 36; // Keep the same to avoid offsets
            arrowY = 18;
            outputX = 36;
            outputY = 18;

        } else if (type.toString().equals("ami:disenchanting")) {
            backgroundTexture = ResourceLocation.fromNamespaceAndPath("minecraft", "textures/gui/container/grindstone.png");
            bgX = 30; bgY = 15; bgW = 116; bgH = 56;
            bgRenderX = 20; bgRenderY = 4;
            drawSlotBackground = false;

            if (recipe instanceof com.sanhiruzu.ami.index.special.GrindstoneDisenchantingRecipe gdr) {
                inputs.add(new SlotPosition(bgRenderX + 18, bgRenderY + 3, List.of(gdr.getEnchanted())));
            }

            gridW = 1; gridH = 1;
            arrowX = bgRenderX + 50;
            arrowY = bgRenderY + 18;
            outputX = bgRenderX + 98;
            outputY = bgRenderY + 18;

        } else if (type.toString().equals("ami:enchanting")) {
            backgroundTexture = ResourceLocation.fromNamespaceAndPath("minecraft", "textures/gui/container/anvil.png");
            bgX = 26; bgY = 46; bgW = 126; bgH = 20;
            bgRenderX = 16; bgRenderY = 18;
            drawSlotBackground = false;

            if (recipe instanceof com.sanhiruzu.ami.index.special.AnvilEnchantingRecipe aer) {
                inputs.add(new SlotPosition(bgRenderX + 1, bgRenderY + 1, List.of(aer.getTool())));
                inputs.add(new SlotPosition(bgRenderX + 50, bgRenderY + 1, List.of(aer.getBook())));
            }

            gridW = 2; gridH = 1;
            arrowX = bgRenderX + 76;
            arrowY = bgRenderY;
            outputX = bgRenderX + 108;
            outputY = bgRenderY + 1;

        } /* EMI integration temporarily disabled to resolve Java 21 module conflicts
        else if (recipe instanceof com.sanhiruzu.ami.emi.CapturedRecipe cr) {
            var captured = cr.getCaptured();
            drawSlotBackground = true;
            int minX = Integer.MAX_VALUE, maxX = 0;
            int minY = Integer.MAX_VALUE, maxY = 0;
            outputX = 92; outputY = 14; // defaults

            for (var slot : captured.slots()) {
                if (!slot.isOutput()) {
                    var stacks = new ArrayList<>(slot.alternatives());
                    inputs.add(new SlotPosition(slot.x(), slot.y(), stacks));
                    minX = Math.min(minX, slot.x()); maxX = Math.max(maxX, slot.x() + 18);
                    minY = Math.min(minY, slot.y()); maxY = Math.max(maxY, slot.y() + 18);
                }
            }

            for (var slot : captured.slots()) {
                if (slot.isOutput()) {
                    var first = slot.alternatives().isEmpty() ? ItemStack.EMPTY : slot.alternatives().get(0);
                    output = first;
                    outputX = slot.x();
                    outputY = slot.y();
                    minX = Math.min(minX, slot.x()); maxX = Math.max(maxX, slot.x() + 26);
                    break;
                }
            }

            gridW = Math.max(1, (maxX - minX) / 18);
            gridH = Math.max(1, (maxY - minY) / 18);
            arrowX = maxX + 2;
            arrowY = (minY + maxY) / 2 - 4;
            int offsetX = 20 - minX;
            int offsetY = 18 - minY;
            List<SlotPosition> adjusted = new ArrayList<>();
            for (var sp : inputs) {
                adjusted.add(new SlotPosition(sp.x() + offsetX, sp.y() + offsetY, sp.alternatives()));
            }
            inputs.clear();
            inputs.addAll(adjusted);
            arrowX += offsetX;
            arrowY += offsetY;
            outputX += offsetX;
            outputY += offsetY;

        }*/ else {
            List<Ingredient> ingredients = recipe.getIngredients();
            int cols = Math.min(ingredients.size(), 3);
            int rows = ingredients.isEmpty() ? 0 : (int) Math.ceil((double) ingredients.size() / cols);
            int gridPixelW = cols * 18;
            int startX = (134 - gridPixelW) / 2;
            int startY = 14;

            for (int i = 0; i < ingredients.size(); i++) {
                Ingredient ing = ingredients.get(i);
                if (ing.isEmpty()) continue;
                int col = i % cols;
                int row = i / cols;
                inputs.add(new SlotPosition(startX + col * 18, startY + row * 18, List.of(ing.getItems())));
            }

            gridW = cols;
            gridH = rows;
            arrowX = 92;
            arrowY = 14;
            outputX = 92;
            outputY = 14;
        }

        return new RecipeLayout(
                entry.id(), categoryIcon, categoryName, inputs, output, gridW, gridH, shapeless,
                outputX, outputY, arrowX, arrowY,
                backgroundTexture, bgX, bgY, bgW, bgH, bgRenderX, bgRenderY, drawSlotBackground
        );
    }

    public static boolean isFurnaceType(RecipeType<?> type) {
        return type == RecipeType.SMELTING
                || type == RecipeType.BLASTING
                || type == RecipeType.SMOKING
                || type == RecipeType.CAMPFIRE_COOKING;
    }

    private static String getCategoryName(RecipeType<?> type) {
        if (com.sanhiruzu.ami.index.AmiRecipeIndex.isEmiCategoryType(type)) {
            String name = com.sanhiruzu.ami.index.AmiRecipeIndex.getEmiCategoryName(type);
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
        } catch (Exception ignored) {}
        return ItemStack.EMPTY;
    }

    public static Component tabComponent(RecipeType<?> type) {
        if (com.sanhiruzu.ami.index.AmiRecipeIndex.isEmiCategoryType(type)) {
            String name = com.sanhiruzu.ami.index.AmiRecipeIndex.getEmiCategoryName(type);
            return name != null ? Component.literal(name) : Component.literal(type.toString());
        }
        ResourceLocation key = BuiltInRegistries.RECIPE_TYPE.getKey(type);
        if (key == null) return Component.literal(type.toString());
        String langKey = "ami.recipe_viewer.tab." + key.getPath();
        if ("minecraft".equals(key.getNamespace()) || "ami".equals(key.getNamespace())) {
            return Component.translatable(langKey);
        }
        return Component.literal(key.toString());
    }

    public static String tabShortLabel(RecipeType<?> type) {
        if (com.sanhiruzu.ami.index.AmiRecipeIndex.isEmiCategoryType(type)) {
            String name = com.sanhiruzu.ami.index.AmiRecipeIndex.getEmiCategoryName(type);
            if (name != null) return name.length() > 8 ? name.substring(0, 8) : name;
        }
        ResourceLocation key = BuiltInRegistries.RECIPE_TYPE.getKey(type);
        if (key == null) return type.toString().substring(0, Math.min(6, type.toString().length()));
        if ("minecraft".equals(key.getNamespace()) || "ami".equals(key.getNamespace())) {
            return Component.translatable("ami.recipe_viewer.tab.short." + key.getPath()).getString();
        }
        String path = key.getPath();
        int slash = path.lastIndexOf('/');
        if (slash >= 0) path = path.substring(slash + 1);
        return path.length() > 8 ? path.substring(0, 8) : path;
    }

    public static ItemStack getRepresentativeWorkstation(RecipeType<?> type) {
        if (com.sanhiruzu.ami.index.AmiRecipeIndex.isEmiCategoryType(type)) {
            return com.sanhiruzu.ami.index.AmiRecipeIndex.getEmiCategoryIcon(type);
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
        if (tStr.equals("ami:grinding") || tStr.equals("ami:disenchanting")) return new ItemStack(net.minecraft.world.item.Items.GRINDSTONE);
        if (tStr.equals("ami:anvil_repairing") || tStr.equals("ami:enchanting")) return new ItemStack(net.minecraft.world.item.Items.ANVIL);
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
        if (tStr.equals("ami:grinding") || tStr.equals("ami:disenchanting")) return List.of(new ItemStack(net.minecraft.world.item.Items.GRINDSTONE));
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
}
