package com.sanhiruzu.ami.compat;

import com.sanhiruzu.ami.config.AmiConfig;
import com.sanhiruzu.ami.client.recipe.RecipeTransferHandler;
import com.sanhiruzu.ami.index.NodeType;
import com.sanhiruzu.ami.index.SearchNode;
import com.sanhiruzu.ami.platform.Services;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import java.lang.ref.WeakReference;
import java.util.Optional;

public class RecipeViewerBridge {

    private static boolean recipeViewActive = false;

    /**
     * True when EMI/JEI is currently displaying a recipe view triggered by AMI.
     */
    public static boolean isRecipeViewActive() {
        return recipeViewActive;
    }

    /**
     * Called to notify that the recipe view has been dismissed.
     */
    public static void clearRecipeView() {
        recipeViewActive = false;
    }

    private static void markRecipeViewActive() {
        recipeViewActive = true;
    }

    public static boolean isAvailable() {
        return isEmiSelectedExternalViewer() || isJeiSelectedExternalViewer() || isReiSelectedExternalViewer();
    }

    /**
     * REI (Fabric-only) is treated as a present external viewer when neither EMI nor JEI is selected.
     * The actual open is routed through the {@code openExternalRecipeView} platform seam so xplat never
     * references {@code me.shedaniel.rei.*}.
     */
    public static boolean isReiSelectedExternalViewer() {
        return !isEmiSelectedExternalViewer()
                && !isJeiSelectedExternalViewer()
                && Services.PLATFORM.isModLoaded("roughlyenoughitems");
    }

    public static boolean shouldUseNativeViewer() {
        return RecipeViewerBridgeCommon.shouldUseNativeViewer(isAvailable());
    }

    public static boolean isEmiSelectedExternalViewer() {
        return Services.PLATFORM.isModLoaded("emi");
    }

    public static boolean isJeiSelectedExternalViewer() {
        return !isEmiSelectedExternalViewer() && Services.PLATFORM.isModLoaded("jei");
    }

    public static boolean isJeiLoaded() {
        return Services.PLATFORM.isModLoaded("jei");
    }

    public static boolean openJustEnoughAdvancement(ResourceLocation advancementId) {
        if (advancementId == null || !Services.PLATFORM.isModLoaded("jei") || !Services.PLATFORM.isModLoaded("jea")) {
            return false;
        }
        boolean opened = JeiRecipeBridge.openJustEnoughAdvancement(advancementId);
        if (opened) {
            markRecipeViewActive();
        }
        return opened;
    }

    public static boolean hasRecipes(ItemStack stack) {
        if (stack == null || stack.isEmpty()) return false;
        if (isEmiSelectedExternalViewer()) {
            return EmiRecipeBridge.hasRecipes(stack);
        }
        if (isJeiSelectedExternalViewer()) {
            return JeiRecipeBridge.hasRecipes(stack);
        }
        return RecipeViewerBridgeCommon.shouldUseNativeViewer(false)
                && Services.PLATFORM.isRecipeIndexBuilt()
                && Services.PLATFORM.hasRecipesFor(stack);
    }

    public static boolean hasUses(ItemStack stack) {
        if (stack == null || stack.isEmpty()) return false;
        if (isEmiSelectedExternalViewer()) {
            return EmiRecipeBridge.hasUses(stack);
        }
        if (isJeiSelectedExternalViewer()) {
            return JeiRecipeBridge.hasUses(stack);
        }
        return RecipeViewerBridgeCommon.shouldUseNativeViewer(false)
                && Services.PLATFORM.isRecipeIndexBuilt()
                && Services.PLATFORM.hasUsesFor(stack);
    }

    public static boolean hasRecipes(SearchNode node) {
        if (node == null) return false;
        if (node.type() == NodeType.ITEM) {
            return hasRecipes(com.sanhiruzu.ami.client.favorites.AmiFavoritesHandler.resolveStack(node));
        }
        if (isJeiSelectedExternalViewer()) {
            return JeiRecipeBridge.hasRecipes(node);
        }
        return false;
    }

    public static boolean hasUses(SearchNode node) {
        if (node == null) return false;
        if (node.type() == NodeType.ITEM) {
            return hasUses(com.sanhiruzu.ami.client.favorites.AmiFavoritesHandler.resolveStack(node));
        }
        if (isJeiSelectedExternalViewer()) {
            return JeiRecipeBridge.hasUses(node);
        }
        return false;
    }

    public static boolean hasEntityInfo(SearchNode node) {
        return node != null
                && node.type() == NodeType.ENTITY
                && (hasRecipes(node) || hasUses(node));
    }

    public static boolean openEntityInfo(SearchNode node) {
        if (!hasEntityInfo(node)) {
            return false;
        }
        if (hasRecipes(node)) {
            openRecipes(node);
        } else {
            openUses(node);
        }
        return true;
    }

    public static boolean supportsSearchSync() {
        if (isEmiSelectedExternalViewer()) return EmiSearchSyncBridge.isAvailable();
        if (isJeiSelectedExternalViewer()) return JeiSearchSyncBridge.isAvailable();
        return false;
    }

    /**
     * Returns the current search text from the active recipe viewer, or "" if none loaded.
     */
    public static String getSearchText() {
        if (isEmiSelectedExternalViewer() && EmiSearchSyncBridge.isAvailable())
            return EmiSearchSyncBridge.getSearchText();
        if (isJeiSelectedExternalViewer() && JeiSearchSyncBridge.isAvailable())
            return JeiSearchSyncBridge.getSearchText();
        return "";
    }

    /**
     * Pushes a search string into the active recipe viewer's search bar.
     */
    public static void setSearchText(String text) {
        if (isEmiSelectedExternalViewer() && EmiSearchSyncBridge.isAvailable())
            EmiSearchSyncBridge.setSearchText(text);
        if (isJeiSelectedExternalViewer() && JeiSearchSyncBridge.isAvailable())
            JeiSearchSyncBridge.setSearchText(text);
    }

    /**
     * Open the recipe viewer for the item's crafting recipes (what produces it).
     */
    public static void openRecipes(ItemStack stack) {
        if (stack == null || stack.isEmpty()) return;
        markRecipeViewActive();
        if (RecipeViewerBridgeCommon.shouldUseNativeViewer(isAvailable())) {
            RecipeViewerBridgeCommon.recordLookup(stack);
            RecipeViewerBridgeCommon.openNative(stack, true);
            return;
        }
        if (isEmiSelectedExternalViewer()) {
            EmiRecipeBridge.openRecipes(stack);
        } else if (isJeiSelectedExternalViewer()) {
            JeiRecipeBridge.openRecipes(stack);
        } else {
            // Loader-specific external viewers (REI on Fabric) route through the platform seam.
            Services.PLATFORM.openExternalRecipeView(stack, false);
        }
        RecipeViewerBridgeCommon.recordLookup(stack);
    }

    public static void openRecipes(SearchNode node) {
        if (node == null) return;
        if (node.type() == NodeType.ITEM) {
            openRecipes(com.sanhiruzu.ami.client.favorites.AmiFavoritesHandler.resolveStack(node));
            return;
        }
        if (!isJeiSelectedExternalViewer()) {
            return;
        }
        markRecipeViewActive();
        JeiRecipeBridge.openRecipes(node);
    }

    /**
     * Open the recipe viewer for uses of the item (what consumes it).
     */
    public static void openUses(ItemStack stack) {
        if (stack == null || stack.isEmpty()) return;
        markRecipeViewActive();
        if (RecipeViewerBridgeCommon.shouldUseNativeViewer(isAvailable())) {
            RecipeViewerBridgeCommon.recordLookup(stack);
            RecipeViewerBridgeCommon.openNative(stack, false);
            return;
        }
        if (isEmiSelectedExternalViewer()) {
            EmiRecipeBridge.openUses(stack);
        } else if (isJeiSelectedExternalViewer()) {
            JeiRecipeBridge.openUses(stack);
        } else {
            // Loader-specific external viewers (REI on Fabric) route through the platform seam.
            Services.PLATFORM.openExternalRecipeView(stack, true);
        }
        RecipeViewerBridgeCommon.recordLookup(stack);
    }

    public static void openUses(SearchNode node) {
        if (node == null) return;
        if (node.type() == NodeType.ITEM) {
            openUses(com.sanhiruzu.ami.client.favorites.AmiFavoritesHandler.resolveStack(node));
            return;
        }
        if (!isJeiSelectedExternalViewer()) {
            return;
        }
        markRecipeViewActive();
        JeiRecipeBridge.openUses(node);
    }

    public static void startDrag(ItemStack stack) {
        if (isEmiSelectedExternalViewer()) {
            EmiRecipeBridge.startDrag(stack);
        } else if (isJeiSelectedExternalViewer()) {
            JeiRecipeBridge.startDrag(stack);
        }
    }

    public static boolean canStartDrag(ItemStack stack) {
        if (stack == null || stack.isEmpty()) return false;

        Screen screen = Minecraft.getInstance().screen;
        if (isEmiSelectedExternalViewer() && EmiRecipeBridge.canStartDrag(screen, stack)) return true;
        if (isJeiSelectedExternalViewer() && JeiRecipeBridge.canStartDrag(screen, stack)) return true;

        try {
            var manager = com.sanhiruzu.ami.client.InventoryOverlayHandler.getManager();
            return manager != null && manager.hasVisibleFavoritesPanel();
        } catch (RuntimeException | LinkageError ignored) {
            return false;
        }
    }

    public static ItemStack getDraggedStack() {
        if (isEmiSelectedExternalViewer()) return EmiRecipeBridge.getDraggedStack();
        if (isJeiSelectedExternalViewer()) return JeiRecipeBridge.getDraggedStack();
        return ItemStack.EMPTY;
    }

    public static boolean isDragging() {
        if (isEmiSelectedExternalViewer()) return EmiRecipeBridge.isDragging();
        if (isJeiSelectedExternalViewer()) return JeiRecipeBridge.isDragging();
        return false;
    }

    public static boolean isEmiRecipeScreenActive() {
        var screen = net.minecraft.client.Minecraft.getInstance().screen;
        return isScreenClass(screen, "dev.emi.emi.screen.RecipeScreen", "RecipeScreen");
    }

    public static RecipeViewerBounds getActiveRecipeViewerBounds() {
        var screen = Minecraft.getInstance().screen;
        if (screen == null) {
            return RecipeViewerBounds.EMPTY;
        }

        RecipeViewerBounds emiBounds = readEmiRecipeScreenBounds(screen);
        if (emiBounds.isValid()) {
            return emiBounds;
        }

        RecipeViewerBounds jeiBounds = readJeiRecipeScreenBounds(screen);
        if (jeiBounds.isValid()) {
            return jeiBounds;
        }

        return readNativeRecipeViewerBounds();
    }

    /**
     * Structured access to active viewer geometry for overlay layout.
     */
    public static final class RecipeViewerBounds {
        public static final RecipeViewerBounds EMPTY = new RecipeViewerBounds(-1, -1, 0, 0);
        public final int x;
        public final int y;
        public final int width;
        public final int height;

        public RecipeViewerBounds(int x, int y, int width, int height) {
            this.x = x;
            this.y = y;
            this.width = width;
            this.height = height;
        }

        public int left() {
            return x;
        }

        public int top() {
            return y;
        }

        public int right() {
            return x + width;
        }

        public int bottom() {
            return y + height;
        }

        public boolean isValid() {
            return width > 0 && height > 0;
        }
    }

    private static RecipeViewerBounds readEmiRecipeScreenBounds(Screen screen) {
        if (!isEmiRecipeScreenActive()) {
            return RecipeViewerBounds.EMPTY;
        }

        try {
            Object recipeScreen = screen;
            Object bounds = readMember(recipeScreen, "getBounds", "bounds");
            return readBoundsLike(bounds, "x", "y", "width", "height");
        } catch (RuntimeException ignored) {
            return RecipeViewerBounds.EMPTY;
        }
    }

    private static RecipeViewerBounds readJeiRecipeScreenBounds(Screen screen) {
        if (!isJeiRecipeScreenActive(screen)) {
            return RecipeViewerBounds.EMPTY;
        }

        RecipeViewerBounds fromProperties = readJeiRecipeScreenBoundsViaProperties(screen);
        if (fromProperties.isValid()) {
            return fromProperties;
        }
        return readJeiRecipeScreenBoundsViaArea(screen);
    }

    private static RecipeViewerBounds readJeiRecipeScreenBoundsViaProperties(Screen screen) {
        try {
            Object properties = readMember(screen, "getProperties", "properties");
            return readBoundsLike(properties, "guiLeft", "guiTop", "guiXSize", "guiYSize");
        } catch (RuntimeException ignored) {
            return RecipeViewerBounds.EMPTY;
        }
    }

    private static RecipeViewerBounds readJeiRecipeScreenBoundsViaArea(Screen screen) {
        try {
            Object area = readMember(screen, "getArea", "area");
            return readBoundsLike(area, "x", "y", "width", "height");
        } catch (RuntimeException ignored) {
            return RecipeViewerBounds.EMPTY;
        }
    }

    private static boolean isJeiRecipeScreenActive(Screen screen) {
        return isScreenClass(screen, "mezz.jei.gui.recipes.RecipesGui", "RecipesGui");
    }

    private static boolean isScreenClass(Screen screen, String... classNames) {
        if (screen == null) {
            return false;
        }

        String screenClass = screen.getClass().getName();
        for (String className : classNames) {
            if (screenClass.equals(className)) {
                return true;
            }
            if (screenClass.endsWith("." + className) || screenClass.endsWith("$" + className)) {
                return true;
            }
        }
        return false;
    }

    private static RecipeViewerBounds readNativeRecipeViewerBounds() {
        Screen screen = Minecraft.getInstance().screen;
        if (!(screen instanceof com.sanhiruzu.ami.client.RecipeViewerScreen rvs)) {
            return RecipeViewerBounds.EMPTY;
        }
        return rvs.getViewerBounds();
    }

    private static RecipeViewerBounds readBoundsLike(Object bounds, String xName, String yName, String widthName, String heightName) {
        if (bounds == null) {
            return RecipeViewerBounds.EMPTY;
        }
        Optional<Integer> x = callInt(bounds, xName, "left", "getLeft");
        Optional<Integer> y = callInt(bounds, yName, "top", "getTop");
        Optional<Integer> width = callInt(bounds, widthName, "w", "getW", "getWidth");
        Optional<Integer> height = callInt(bounds, heightName, "h", "getH", "getHeight");
        if (x.isEmpty() || y.isEmpty() || width.isEmpty() || height.isEmpty()) {
            return RecipeViewerBounds.EMPTY;
        }
        return new RecipeViewerBounds(x.get(), y.get(), width.get(), height.get());
    }

    private static Object readMember(Object obj, String... memberNames) {
        if (obj == null || memberNames == null) {
            return null;
        }
        Class<?> type = obj.getClass();
        for (String memberName : memberNames) {
            if (memberName == null || memberName.isBlank()) {
                continue;
            }
            try {
                return type.getMethod(memberName).invoke(obj);
            } catch (ReflectiveOperationException ignored) {
            } catch (RuntimeException ignored) {
            }

            try {
                var field = type.getField(memberName);
                return field.get(obj);
            } catch (ReflectiveOperationException ignored) {
            } catch (RuntimeException ignored) {
            }

            try {
                var field = type.getDeclaredField(memberName);
                if (!field.canAccess(obj)) {
                    field.setAccessible(true);
                }
                return field.get(obj);
            } catch (ReflectiveOperationException ignored) {
            } catch (RuntimeException ignored) {
            }
        }
        return null;
    }

    private static Optional<Integer> callInt(Object obj, String... memberNames) {
        if (obj == null) {
            return Optional.empty();
        }
        for (String memberName : memberNames) {
            if (memberName == null || memberName.isBlank()) {
                continue;
            }
            Object value = readMember(obj, memberName);
            if (value instanceof Number n) {
                return Optional.of(n.intValue());
            }
        }
        return Optional.empty();
    }

    public static void stopDrag() {
        if (isEmiSelectedExternalViewer()) {
            EmiRecipeBridge.stopDrag();
        } else if (isJeiSelectedExternalViewer()) {
            JeiRecipeBridge.stopDrag();
        }
    }

    public static boolean handleDrop(double mouseX, double mouseY) {
        if (isEmiSelectedExternalViewer()) {
            return EmiRecipeBridge.handleDrop(net.minecraft.client.Minecraft.getInstance().screen, mouseX, mouseY);
        }
        if (isJeiSelectedExternalViewer()) {
            return JeiRecipeBridge.handleDrop(net.minecraft.client.Minecraft.getInstance().screen, mouseX, mouseY);
        }
        return false;
    }

    /**
     * Dispatch a click on an item. Left-click crafts one when the current screen
     * can accept a recipe transfer, and shift-left crafts as many as possible.
     * button: 0 = left, 1 = right.
     */
    public static void handleItemClick(ItemStack stack, int button, boolean shiftDown) {
        handleItemClick(stack, button, shiftDown, false);
    }

    public static void handleItemClick(ItemStack stack, int button, boolean shiftDown, boolean controlDown) {
        if (stack == null || stack.isEmpty()) return;

        if (button == 0 && (shiftDown || controlDown || canTransferStack(stack))) {
            handleTransferClick(stack, shiftDown, false);
            return;
        }

        if (button == 1) {
            // Right-click always opens uses regardless of config
            openUses(stack);
            return;
        }
        switch (AmiConfig.itemClickAction) {
            case RECIPES -> {
                openRecipes(stack);
            }
            case USES -> {
                openUses(stack);
            }
            case NONE -> {
            }
        }
    }

    private static void handleTransferClick(ItemStack stack, boolean maxTransfer, boolean toCursor) {
        if (tryTransferRecipe(stack, maxTransfer, toCursor)) {
            return;
        }
        if (isEmiSelectedExternalViewer()) {
            EmiRecipeBridge.handleShiftClick(stack);
        } else if (isJeiSelectedExternalViewer()) {
            JeiRecipeBridge.handleShiftClick(stack);
        }
    }

    private static boolean tryTransferRecipe(ItemStack stack, boolean maxTransfer, boolean toCursor) {
        Minecraft mc = Minecraft.getInstance();
        Screen screen = mc.screen;
        if (stack == null || stack.isEmpty() || screen == null) {
            return false;
        }
        if (!toCursor && Services.PLATFORM.isModLoaded("emi")
                && EmiRecipeBridge.transferStack(stack, screen, maxTransfer)) {
            RecipeViewerBridgeCommon.recordCraft(stack);
            return true;
        }
        if (!toCursor && Services.PLATFORM.isModLoaded("jei")
                && JeiRecipeBridge.transferStack(stack, screen, maxTransfer)) {
            RecipeViewerBridgeCommon.recordCraft(stack);
            return true;
        }
        if (!Services.PLATFORM.isRecipeIndexBuilt()) {
            return false;
        }
        for (var recipe : Services.PLATFORM.getRecipesFor(stack)) {
            if (transferRecipe(recipe, screen, mc, maxTransfer, toCursor)) {
                RecipeViewerBridgeCommon.recordCraft(stack);
                return true;
            }
        }
        return false;
    }

    private static ItemStack lastTransferStack = ItemStack.EMPTY;
    private static WeakReference<Screen> lastTransferScreenRef = new WeakReference<>(null);
    private static boolean lastTransferResult = false;
    private static long lastTransferTime = 0;

    public static boolean canTransferStack(ItemStack stack) {
        Minecraft mc = Minecraft.getInstance();
        Screen screen = mc.screen;
        if (stack == null || stack.isEmpty() || screen == null) {
            return false;
        }

        // 1-element cache for tooltips
        long now = System.currentTimeMillis();
        if (Services.PLATFORM.sameItemSameComponents(stack, lastTransferStack) 
                && lastTransferScreenRef.get() == screen 
                && (now - lastTransferTime) < 500) {
            return lastTransferResult;
        }

        boolean result = computeCanTransferStack(stack, screen);
        
        lastTransferStack = stack.copy();
        lastTransferScreenRef = new WeakReference<>(screen);
        lastTransferResult = result;
        lastTransferTime = now;
        
        return result;
    }

    private static boolean computeCanTransferStack(ItemStack stack, Screen screen) {
        try {
            if (Services.PLATFORM.isModLoaded("emi") && EmiRecipeBridge.canTransferStack(stack, screen)) {
                return true;
            }
            if (Services.PLATFORM.isModLoaded("jei") && JeiRecipeBridge.canTransferStack(stack, screen)) {
                return true;
            }
            if (!Services.PLATFORM.isRecipeIndexBuilt()) {
                return false;
            }
            for (var recipe : Services.PLATFORM.getRecipesFor(stack)) {
                if (canTransferRecipe(recipe, screen)) {
                    return true;
                }
            }
        } catch (Exception e) {
            // Log once per item maybe? For now just ignore to prevent crashes
            return false;
        }
        return false;
    }

    public static boolean transferRecipe(com.sanhiruzu.ami.util.AmiRecipeHolder<?> recipe, Screen screen, Minecraft mc,
                                         boolean maxTransfer, boolean toCursor) {
        if (recipe == null || screen == null || mc == null) {
            return false;
        }
        if (Services.PLATFORM.isModLoaded("emi") && EmiRecipeBridge.transfer(recipe, screen, maxTransfer, toCursor)) {
            return true;
        }
        if (!toCursor && Services.PLATFORM.isModLoaded("jei") && JeiRecipeBridge.transfer(recipe, screen, maxTransfer, false)) {
            return true;
        }
        return !toCursor
                && RecipeTransferHandler.canTransfer(recipe, screen)
                && RecipeTransferHandler.transfer(recipe, screen, mc, maxTransfer);
    }

    public static boolean canTransferRecipe(com.sanhiruzu.ami.util.AmiRecipeHolder<?> recipe, Screen screen) {
        if (recipe == null || screen == null) {
            return false;
        }
        if (Services.PLATFORM.isModLoaded("emi") && EmiRecipeBridge.canTransfer(recipe, screen)) {
            return true;
        }
        if (Services.PLATFORM.isModLoaded("jei") && JeiRecipeBridge.canTransfer(recipe, screen)) {
            return true;
        }
        return RecipeTransferHandler.canTransfer(recipe, screen);
    }

    /**
     * Overload for backward compatibility with code not passing shift state.
     */
    public static void handleItemClick(ItemStack stack, int button) {
        handleItemClick(stack, button, false);
    }

    public static java.util.List<ItemStack> getCraftables() {
        return VanillaCraftablesService.getCraftables();
    }

    public static java.util.List<ItemStack> getLookupHistory() {
        if (Services.PLATFORM.isModLoaded("emi")) {
            var emiHistory = EmiRecipeBridge.getLookupHistory();
            if (!emiHistory.isEmpty()) return emiHistory;
        }
        return com.sanhiruzu.ami.client.favorites.AmiHistoryHandler.getInstance().getLookupHistory();
    }

    public static java.util.List<ItemStack> getCraftHistory() {
        if (Services.PLATFORM.isModLoaded("emi")) return EmiRecipeBridge.getCraftHistory();
        return com.sanhiruzu.ami.client.favorites.AmiHistoryHandler.getInstance().getCraftHistory();
    }
}
