package com.sanhiruzu.ami.compat;

import com.sanhiruzu.ami.index.SearchNode;
import com.sanhiruzu.ami.index.providers.IngredientIndexProvider;
import mezz.jei.api.gui.IRecipeLayoutDrawable;
import mezz.jei.api.constants.RecipeTypes;
import mezz.jei.api.ingredients.IIngredientType;
import mezz.jei.api.recipe.IFocusGroup;
import mezz.jei.api.recipe.IFocus;
import mezz.jei.api.recipe.IFocusFactory;
import mezz.jei.api.recipe.RecipeIngredientRole;
import mezz.jei.api.recipe.category.IRecipeCategory;
import mezz.jei.common.transfer.RecipeTransferUtil;
import mezz.jei.api.runtime.IJeiRuntime;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Recipe;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.List;
import java.util.Optional;

/**
 * Direct JEI API calls — only referenced behind a ModList.isLoaded("jei") guard
 * so this class is never loaded when JEI is absent.
 */
class JeiRecipeBridge {
    private static ItemStack draggedStack = ItemStack.EMPTY;

    static void openRecipes(ItemStack stack) {
        JeiRuntimeAccessor.withRuntime(runtime -> {
            ItemStack lookup = firstFocusStack(runtime, stack, RecipeIngredientRole.OUTPUT);
            show(runtime, lookup, RecipeIngredientRole.OUTPUT);
        });
    }

    static void openUses(ItemStack stack) {
        JeiRuntimeAccessor.withRuntime(runtime -> {
            ItemStack lookup = firstFocusStack(runtime, stack, RecipeIngredientRole.INPUT);
            show(runtime, lookup, RecipeIngredientRole.INPUT);
        });
    }

    static void openRecipes(SearchNode node) {
        JeiRuntimeAccessor.withRuntime(runtime -> show(node, runtime, RecipeIngredientRole.OUTPUT));
    }

    static void openUses(SearchNode node) {
        JeiRuntimeAccessor.withRuntime(runtime -> show(node, runtime, RecipeIngredientRole.INPUT));
    }

    static boolean hasRecipes(ItemStack stack) {
        if (stack == null || stack.isEmpty()) {
            return false;
        }
        return JeiRuntimeAccessor.withRuntime(runtime -> hasFocus(runtime, stack, RecipeIngredientRole.OUTPUT), false);
    }

    static boolean hasUses(ItemStack stack) {
        if (stack == null || stack.isEmpty()) {
            return false;
        }
        return JeiRuntimeAccessor.withRuntime(runtime -> hasFocus(runtime, stack, RecipeIngredientRole.INPUT), false);
    }

    static boolean hasRecipes(SearchNode node) {
        if (node == null) {
            return false;
        }
        return JeiRuntimeAccessor.withRuntime(runtime -> hasFocus(runtime, node, RecipeIngredientRole.OUTPUT), false);
    }

    static boolean hasUses(SearchNode node) {
        if (node == null) {
            return false;
        }
        return JeiRuntimeAccessor.withRuntime(runtime -> hasFocus(runtime, node, RecipeIngredientRole.INPUT), false);
    }

    static void startDrag(ItemStack stack) {
        draggedStack = stack.copy();
    }

    static ItemStack getDraggedStack() {
        return draggedStack;
    }

    static boolean isDragging() {
        return !draggedStack.isEmpty();
    }

    static void stopDrag() {
        draggedStack = ItemStack.EMPTY;
    }

    static boolean handleDrop(Screen screen, double mouseX, double mouseY) {
        if (draggedStack.isEmpty()) {
            return false;
        }
        draggedStack = ItemStack.EMPTY;
        return false;
    }

    static boolean canStartDrag(Screen screen, ItemStack stack) {
        if (screen == null) return false;
        if (stack == null || stack.isEmpty()) return false;
        return false;
    }

    static void handleShiftClick(ItemStack stack) {
        openRecipes(stack);
    }

    private static void show(IJeiRuntime runtime, ItemStack stack, RecipeIngredientRole role) {
        IIngredientType<ItemStack> itemType = mezz.jei.api.constants.VanillaTypes.ITEM_STACK;
        if (itemType == null) return;
        IFocusFactory focusFactory = runtime.getJeiHelpers().getFocusFactory();
        IFocus<ItemStack> focus = focusFactory.createFocus(role, itemType, stack);
        runtime.getRecipesGui().show(focus);
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private static void show(SearchNode node, IJeiRuntime runtime, RecipeIngredientRole role) {
        Optional<FocusTarget> focusTarget = resolveFocusTarget(runtime, node);
        if (focusTarget.isEmpty()) {
            return;
        }
        IFocusFactory focusFactory = runtime.getJeiHelpers().getFocusFactory();
        FocusTarget target = focusTarget.get();
        IFocus<?> focus = focusFactory.createFocus(role, (IIngredientType) target.type(), target.ingredient());
        runtime.getRecipesGui().show(List.of(focus));
    }

    private static ItemStack firstFocusStack(IJeiRuntime runtime, ItemStack requested, RecipeIngredientRole role) {
        for (ItemStack candidate : RecipeLookupStackResolver.candidates(requested)) {
            if (hasDirectFocus(runtime, candidate, role)) {
                return candidate;
            }
        }
        return requested;
    }

    private static boolean hasFocus(IJeiRuntime runtime, ItemStack requested, RecipeIngredientRole role) {
        for (ItemStack candidate : RecipeLookupStackResolver.candidates(requested)) {
            if (hasDirectFocus(runtime, candidate, role)) {
                return true;
            }
        }
        return false;
    }

    private static boolean hasFocus(IJeiRuntime runtime, SearchNode node, RecipeIngredientRole role) {
        return resolveFocusTarget(runtime, node)
                .map(target -> hasDirectFocus(runtime, target, role))
                .orElse(false);
    }

    private static boolean hasDirectFocus(IJeiRuntime runtime, ItemStack stack, RecipeIngredientRole role) {
        IIngredientType<ItemStack> itemType = mezz.jei.api.constants.VanillaTypes.ITEM_STACK;
        if (itemType == null) {
            return false;
        }
        IFocusFactory focusFactory = runtime.getJeiHelpers().getFocusFactory();
        IFocus<ItemStack> focus = focusFactory.createFocus(role, itemType, stack);
        IFocusGroup focusGroup = focusFactory.createFocusGroup(List.of(focus));
        return runtime.getRecipeManager()
                .createRecipeCategoryLookup()
                .limitFocus(focusGroup.getAllFocuses())
                .get()
                .findAny()
                .isPresent();
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private static boolean hasDirectFocus(IJeiRuntime runtime, FocusTarget target, RecipeIngredientRole role) {
        IFocusFactory focusFactory = runtime.getJeiHelpers().getFocusFactory();
        IFocus<?> focus = focusFactory.createFocus(role, (IIngredientType) target.type(), target.ingredient());
        IFocusGroup focusGroup = focusFactory.createFocusGroup(List.of(focus));
        return runtime.getRecipeManager()
                .createRecipeCategoryLookup()
                .limitFocus(focusGroup.getAllFocuses())
                .get()
                .findAny()
                .isPresent();
    }

    @SuppressWarnings({"rawtypes", "unchecked", "removal"})
    private static Optional<FocusTarget> resolveFocusTarget(IJeiRuntime runtime, SearchNode node) {
        if (node == null) {
            return Optional.empty();
        }
        String typeUid = node.meta(IngredientIndexProvider.TYPE_UID_KEY, "");
        String ingredientUid = node.meta(IngredientIndexProvider.INGREDIENT_UID_KEY, "");
        if (typeUid.isBlank() || ingredientUid.isBlank()) {
            return Optional.empty();
        }
        return runtime.getIngredientManager()
                .getIngredientTypeForUid(typeUid)
                .flatMap(type -> ((IIngredientType) type) == null
                        ? Optional.empty()
                        : runtime.getIngredientManager()
                                .getIngredientByUid((IIngredientType) type, ingredientUid)
                                .map(ingredient -> new FocusTarget((IIngredientType<Object>) type, ingredient)));
    }

    static boolean transferStack(ItemStack stack, Screen screen, boolean maxTransfer) {
        if (stack == null || stack.isEmpty() || !(screen instanceof AbstractContainerScreen<?> containerScreen)) {
            return false;
        }
        return JeiRuntimeAccessor.withRuntime(runtime -> {
            Player player = net.minecraft.client.Minecraft.getInstance().player;
            if (player == null) return false;
            IFocusFactory focusFactory = runtime.getJeiHelpers().getFocusFactory();
            IFocus<ItemStack> focus = focusFactory.createFocus(
                    RecipeIngredientRole.OUTPUT,
                    mezz.jei.api.constants.VanillaTypes.ITEM_STACK,
                    stack);
            IFocusGroup focusGroup = focusFactory.createFocusGroup(List.of(focus));
            List<IRecipeCategory<?>> categories = runtime.getRecipeManager()
                    .createRecipeCategoryLookup()
                    .limitFocus(focusGroup.getAllFocuses())
                    .get()
                    .toList();
            for (IRecipeCategory<?> category : categories) {
                if (transferFirstMatching(runtime, category, focusGroup, containerScreen, player, maxTransfer)) {
                    return true;
                }
            }
            return false;
        }, false);
    }

    static boolean canTransferStack(ItemStack stack, Screen screen) {
        if (stack == null || stack.isEmpty() || !(screen instanceof AbstractContainerScreen<?> containerScreen)) {
            return false;
        }
        return JeiRuntimeAccessor.withRuntime(runtime -> {
            Player player = net.minecraft.client.Minecraft.getInstance().player;
            if (player == null) return false;
            IFocusFactory focusFactory = runtime.getJeiHelpers().getFocusFactory();
            IFocus<ItemStack> focus = focusFactory.createFocus(
                    RecipeIngredientRole.OUTPUT,
                    mezz.jei.api.constants.VanillaTypes.ITEM_STACK,
                    stack);
            IFocusGroup focusGroup = focusFactory.createFocusGroup(List.of(focus));
            List<IRecipeCategory<?>> categories = runtime.getRecipeManager()
                    .createRecipeCategoryLookup()
                    .limitFocus(focusGroup.getAllFocuses())
                    .get()
                    .toList();
            for (IRecipeCategory<?> category : categories) {
                if (canTransferAny(runtime, category, focusGroup, containerScreen, player)) {
                    return true;
                }
            }
            return false;
        }, false);
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private static boolean transferFirstMatching(IJeiRuntime runtime, IRecipeCategory category, IFocusGroup focusGroup,
                                                 AbstractContainerScreen<?> containerScreen, Player player,
                                                 boolean maxTransfer) {
        var recipeManager = runtime.getRecipeManager();
        List<?> recipes = recipeManager.createRecipeLookup(category.getRecipeType())
                .limitFocus(focusGroup.getAllFocuses())
                .get()
                .toList();
        for (Object recipe : recipes) {
            if (!isHandled(category, recipe)) {
                continue;
            }
            try {
                Optional<IRecipeLayoutDrawable<?>> layout = (Optional) recipeManager.createRecipeLayoutDrawable(
                        category,
                        recipe,
                        focusGroup);
                if (layout.filter(recipeLayout -> RecipeTransferUtil.transferRecipe(
                        runtime.getRecipeTransferManager(),
                        containerScreen.getMenu(),
                        recipeLayout,
                        player,
                        maxTransfer)).isPresent()) {
                    return true;
                }
            } catch (RuntimeException | LinkageError ignored) {
            }
        }
        return false;
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private static boolean canTransferAny(IJeiRuntime runtime, IRecipeCategory category, IFocusGroup focusGroup,
                                          AbstractContainerScreen<?> containerScreen, Player player) {
        var recipeManager = runtime.getRecipeManager();
        List<?> recipes = recipeManager.createRecipeLookup(category.getRecipeType())
                .limitFocus(focusGroup.getAllFocuses())
                .get()
                .toList();
        for (Object recipe : recipes) {
            if (!isHandled(category, recipe)) {
                continue;
            }
            try {
                Optional<IRecipeLayoutDrawable<?>> layout = (Optional) recipeManager.createRecipeLayoutDrawable(
                        category,
                        recipe,
                        focusGroup);
                if (layout.filter(recipeLayout -> RecipeTransferUtil.getTransferRecipeError(
                        runtime.getRecipeTransferManager(),
                        containerScreen.getMenu(),
                        recipeLayout,
                        player).map(error -> error.getType().allowsTransfer).orElse(true)).isPresent()) {
                    return true;
                }
            } catch (RuntimeException | LinkageError ignored) {
            }
        }
        return false;
    }

    static boolean transfer(com.sanhiruzu.ami.util.AmiRecipeHolder<?> recipe, Screen screen, boolean maxTransfer,
                            boolean toCursor) {
        if (toCursor || !(screen instanceof AbstractContainerScreen<?> containerScreen)) {
            return false;
        }
        return JeiRuntimeAccessor.withRuntime(runtime -> {
            Player player = net.minecraft.client.Minecraft.getInstance().player;
            if (player == null) return false;
            Optional<IRecipeLayoutDrawable<?>> layout = createLayout(runtime, recipe);
            return layout.filter(recipeLayout -> RecipeTransferUtil.transferRecipe(
                    runtime.getRecipeTransferManager(),
                    containerScreen.getMenu(),
                    recipeLayout,
                    player,
                    maxTransfer)).isPresent();
        }, false);
    }

    static boolean canTransfer(com.sanhiruzu.ami.util.AmiRecipeHolder<?> recipe, Screen screen) {
        if (!(screen instanceof AbstractContainerScreen<?> containerScreen)) {
            return false;
        }
        return JeiRuntimeAccessor.withRuntime(runtime -> {
            Player player = net.minecraft.client.Minecraft.getInstance().player;
            if (player == null) return false;
            Optional<IRecipeLayoutDrawable<?>> layout = createLayout(runtime, recipe);
            return layout.filter(recipeLayout -> RecipeTransferUtil.getTransferRecipeError(
                    runtime.getRecipeTransferManager(),
                    containerScreen.getMenu(),
                    recipeLayout,
                    player).map(error -> error.getType().allowsTransfer).orElse(true)).isPresent();
        }, false);
    }

    private static Optional<IRecipeLayoutDrawable<?>> createLayout(mezz.jei.api.runtime.IJeiRuntime runtime,
                                                                  com.sanhiruzu.ami.util.AmiRecipeHolder<?> recipe) {
        net.minecraft.world.item.crafting.RecipeType<?> vanillaType = recipe.value().getType();
        net.minecraft.resources.ResourceLocation typeId = BuiltInRegistries.RECIPE_TYPE.getKey(vanillaType);
        if (typeId == null) {
            return Optional.empty();
        }
        try {
            RecipeTypeAndRecipe jeiRecipe = createJeiRecipe(typeId, recipe);
            return createLayoutUnchecked(runtime, jeiRecipe.type(), jeiRecipe.recipe());
        } catch (RuntimeException ignored) {
            return Optional.empty();
        }
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private static <R> Optional<IRecipeLayoutDrawable<?>> createLayoutUnchecked(
            mezz.jei.api.runtime.IJeiRuntime runtime,
            mezz.jei.api.recipe.RecipeType<?> jeiType,
            Object recipe) {
        IRecipeCategory<R> category = runtime.getRecipeManager().getRecipeCategory((mezz.jei.api.recipe.RecipeType<R>) jeiType);
        if (!isHandled(category, recipe)) {
            return Optional.empty();
        }
        IFocusGroup focusGroup = runtime.getJeiHelpers().getFocusFactory().getEmptyFocusGroup();
        return (Optional) runtime.getRecipeManager().createRecipeLayoutDrawable(category, (R) recipe, focusGroup);
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private static boolean isHandled(IRecipeCategory category, Object recipe) {
        if (category == null || recipe == null) {
            return false;
        }
        try {
            return category.isHandled(recipe);
        } catch (RuntimeException | LinkageError ignored) {
            return false;
        }
    }

    private static RecipeTypeAndRecipe createJeiRecipe(
            net.minecraft.resources.ResourceLocation typeId,
            com.sanhiruzu.ami.util.AmiRecipeHolder<?> recipe) {
        Optional<RecipeTypeAndRecipe> holderRecipe = createHolderRecipe(typeId, recipe);
        if (holderRecipe.isPresent()) {
            return holderRecipe.get();
        }

        mezz.jei.api.recipe.RecipeType<?> legacyType = legacyRecipeType(typeId, recipe.value());
        return new RecipeTypeAndRecipe(legacyType, recipe.value());
    }

    private static Optional<RecipeTypeAndRecipe> createHolderRecipe(
            net.minecraft.resources.ResourceLocation typeId,
            com.sanhiruzu.ami.util.AmiRecipeHolder<?> recipe) {
        try {
            Method createHolderType = mezz.jei.api.recipe.RecipeType.class
                    .getMethod("createRecipeHolderType", net.minecraft.resources.ResourceLocation.class);
            Object jeiType = createHolderType.invoke(null, typeId);
            Class<?> holderClass = Class.forName("net.minecraft.world.item.crafting.RecipeHolder");
            Constructor<?> constructor = holderClass.getConstructor(
                    net.minecraft.resources.ResourceLocation.class, Recipe.class);
            Object holder = constructor.newInstance(recipe.id(), recipe.value());
            return Optional.of(new RecipeTypeAndRecipe((mezz.jei.api.recipe.RecipeType<?>) jeiType, holder));
        } catch (ReflectiveOperationException | RuntimeException ignored) {
            return Optional.empty();
        }
    }

    private static mezz.jei.api.recipe.RecipeType<?> legacyRecipeType(
            net.minecraft.resources.ResourceLocation typeId,
            Recipe<?> recipe) {
        net.minecraft.world.item.crafting.RecipeType<?> type = recipe.getType();
        if (type == net.minecraft.world.item.crafting.RecipeType.CRAFTING) return RecipeTypes.CRAFTING;
        if (type == net.minecraft.world.item.crafting.RecipeType.STONECUTTING) return RecipeTypes.STONECUTTING;
        if (type == net.minecraft.world.item.crafting.RecipeType.SMELTING) return RecipeTypes.SMELTING;
        if (type == net.minecraft.world.item.crafting.RecipeType.SMOKING) return RecipeTypes.SMOKING;
        if (type == net.minecraft.world.item.crafting.RecipeType.BLASTING) return RecipeTypes.BLASTING;
        if (type == net.minecraft.world.item.crafting.RecipeType.CAMPFIRE_COOKING) return RecipeTypes.CAMPFIRE_COOKING;
        if (type == net.minecraft.world.item.crafting.RecipeType.SMITHING) return RecipeTypes.SMITHING;
        return mezz.jei.api.recipe.RecipeType.create(
                typeId.getNamespace(), typeId.getPath(), (Class) recipe.getClass());
    }

    private record RecipeTypeAndRecipe(mezz.jei.api.recipe.RecipeType<?> type, Object recipe) {
    }

    private record FocusTarget(IIngredientType<Object> type, Object ingredient) {
    }
}
