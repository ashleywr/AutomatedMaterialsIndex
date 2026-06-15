package com.sanhiruzu.ami.platform;

import com.mojang.authlib.GameProfile;
import com.mojang.blaze3d.platform.InputConstants;
import com.sanhiruzu.ami.index.metrics.FoodStats;
import com.sanhiruzu.ami.util.AmiRecipeHolder;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.core.RegistryAccess;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.inventory.tooltip.TooltipComponent;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.SpawnEggItem;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.Level;
import org.joml.Matrix4f;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.nio.file.Path;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.OptionalLong;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public interface IPlatformHelper {
    private static Object unwrapRecipe(Object recipeOrHolder) {
        if (recipeOrHolder == null) return null;
        Object value = invokeNoArg(recipeOrHolder, "value");
        return value == null ? recipeOrHolder : value;
    }

    private static Object createTooltipContext(Level level) {
        try {
            Class<?> contextClass = Class.forName("net.minecraft.world.item.Item$TooltipContext");
            for (Method method : contextClass.getMethods()) {
                if (!method.getName().equals("of") || method.getParameterTypes().length != 1) continue;
                Class<?> parameterType = method.getParameterTypes()[0];
                if (level != null && parameterType.isAssignableFrom(level.getClass())) {
                    return method.invoke(null, level);
                }
            }
        } catch (ReflectiveOperationException | RuntimeException ignored) {
        }
        return null;
    }

    private static Object getFoodComponent(ItemStack stack) {
        Object foodComponentType = getDataComponentType("FOOD");
        return foodComponentType == null ? null : invokeOneArg(stack, "get", foodComponentType);
    }

    private static Object getDataComponentType(String fieldName) {
        return DataComponentTypeCache.get(fieldName);
    }

    private static Boolean invokeStaticItemStackComparison(String methodName, ItemStack first, ItemStack second) {
        try {
            Method method = ItemStack.class.getMethod(methodName, ItemStack.class, ItemStack.class);
            Object result = method.invoke(null, first, second);
            return result instanceof Boolean bool ? bool : null;
        } catch (ReflectiveOperationException | RuntimeException ignored) {
            return null;
        }
    }

    private static Object invokeNoArg(Object target, String methodName) {
        try {
            Method method = target.getClass().getMethod(methodName);
            method.setAccessible(true);
            return method.invoke(target);
        } catch (ReflectiveOperationException | RuntimeException ignored) {
            return null;
        }
    }

    private static Object invokeOneArg(Object target, String methodName, Object firstArgument, Object secondArgument) {
        if (target == null) return null;
        for (Method method : target.getClass().getMethods()) {
            Class<?>[] parameterTypes = method.getParameterTypes();
            if (!method.getName().equals(methodName) || parameterTypes.length != 2) continue;
            if (firstArgument != null && !parameterTypes[0].isAssignableFrom(firstArgument.getClass())) continue;
            if (secondArgument != null && !parameterTypes[1].isAssignableFrom(secondArgument.getClass())) continue;
            try {
                method.setAccessible(true);
                return method.invoke(target, firstArgument, secondArgument);
            } catch (ReflectiveOperationException | RuntimeException ignored) {
                return null;
            }
        }
        return null;
    }

    private static Object invokeOneArg(Object target, String methodName, Object argument) {
        if (target == null || argument == null) return null;
        for (Method method : target.getClass().getMethods()) {
            Class<?>[] parameterTypes = method.getParameterTypes();
            if (!method.getName().equals(methodName) || parameterTypes.length != 1) continue;
            if (!parameterTypes[0].isAssignableFrom(argument.getClass())) continue;
            try {
                method.setAccessible(true);
                return method.invoke(target, argument);
            } catch (ReflectiveOperationException | RuntimeException ignored) {
                return null;
            }
        }
        return null;
    }

    private static Integer extractInt(Object target, String... methodNames) {
        for (String methodName : methodNames) {
            Object value = invokeNoArg(target, methodName);
            if (value instanceof Number number) return number.intValue();
        }
        return null;
    }

    private static Float extractFloat(Object target, String... methodNames) {
        for (String methodName : methodNames) {
            Object value = invokeNoArg(target, methodName);
            if (value instanceof Number number) return number.floatValue();
        }
        return null;
    }

    boolean isClient();

    Optional<String> getModName(String modId);

    default Optional<String> getModVersion(String modId) {
        return Optional.empty();
    }

    default Optional<String> getModMetadataText(String modId) {
        return getModName(modId);
    }

    /**
     * Loaded mod IDs for the current runtime, used for lightweight cache/config invalidation.
     */
    default List<String> getLoadedModIds() {
        return List.of();
    }

    default List<String> getLoadedModFingerprintEntries() {
        return getLoadedModIds();
    }

    /**
     * The currently selected Minecraft language code, normalized for cache key derivation.
     */
    default String getClientLanguageCode() {
        try {
            String language = Minecraft.getInstance().getLanguageManager().getSelected();
            if (language != null && !language.isBlank()) {
                return language;
            }
        } catch (RuntimeException ignored) {
        }
        return "en_us";
    }

    Path getConfigDir();

    Path getGameDir();

    default boolean isModLoaded(String modId) {
        return getModName(modId).isPresent();
    }

    ResourceLocation rl(String namespace, String path);

    IAmiKeyMappings keyMappings();

    /**
     * Returns true if the given KeyMapping is active and matches the given key.
     * Delegates to {@code KeyMapping.isActiveAndMatches()} which is patched in by Forge/NeoForge
     * and does not exist in vanilla/Fabric.
     */
    boolean keyActiveAndMatches(KeyMapping mapping, InputConstants.Key key);

    /**
     * Returns the slot currently under the mouse in an AbstractContainerScreen, or null.
     * Delegates to {@code AbstractContainerScreen.getSlotUnderMouse()} which is Forge/NeoForge-patched.
     */
    Slot getHoveredSlot(AbstractContainerScreen<?> screen);

    /**
     * Returns the screen-space X origin of the container GUI (left edge).
     * Delegates to {@code AbstractContainerScreen.getGuiLeft()} which is Forge/NeoForge-patched.
     */
    int getGuiLeft(AbstractContainerScreen<?> screen);

    /**
     * Returns the screen-space Y origin of the container GUI (top edge).
     * Delegates to {@code AbstractContainerScreen.getGuiTop()} which is Forge/NeoForge-patched.
     */
    int getGuiTop(AbstractContainerScreen<?> screen);

    /**
     * Returns the biome downfall value from the modified climate settings.
     * Delegates to {@code Biome.getModifiedClimateSettings().downfall()} which is Forge/NeoForge-patched.
     */
    float getBiomeDownfall(net.minecraft.world.level.biome.Biome biome);

    /**
     * Returns whether this biome has the FROZEN temperature modifier.
     * Delegates to {@code Biome.getModifiedClimateSettings().temperatureModifier()} which is Forge/NeoForge-patched.
     */
    boolean isBiomeTemperatureFrozen(net.minecraft.world.level.biome.Biome biome);

    /**
     * Returns the painting's {@code {width, height}} in tiles. The accessor names diverge across
     * versions: MC 1.20.1 exposes {@code getWidth()}/{@code getHeight()} on the {@code PaintingVariant}
     * class, while MC 1.21.1 exposes the record accessors {@code width()}/{@code height()}. Implemented
     * per-loader so each calls the correct direct (remappable) accessor instead of name reflection.
     */
    int[] getPaintingSize(net.minecraft.world.entity.decoration.PaintingVariant variant);

    /**
     * Wraps {@code recipe} in a {@code net.minecraft.world.item.crafting.RecipeHolder} for JEI's
     * recipe-holder display path. {@code RecipeHolder} was introduced in MC 1.21 and does NOT exist in
     * MC 1.20.1, so the type cannot be referenced from xplat (which compiles against the 1.20.1 API).
     * Implemented per-loader: Forge (1.20.1) returns {@code null} (no such type — caller falls back to
     * the legacy recipe path); NeoForge/Fabric (1.21.1) return a real {@code RecipeHolder} via a direct,
     * Loom-remappable reference. Returns {@link Object} because the type is absent at xplat compile time.
     */
    default Object createRecipeHolder(ResourceLocation id, net.minecraft.world.item.crafting.Recipe<?> recipe) {
        return null;
    }

    /**
     * Opens the vanilla chat screen pre-filled with {@code text}. {@code ChatScreen} is a client-only
     * class; referencing it directly from xplat would force the verifier to load it on the
     * client-stripped unit-test classpath, and {@code Class.forName(Mojmap-name)} failed on Fabric's
     * intermediary runtime. Implemented per-loader with direct (Loom-remappable) calls. No-op by default.
     */
    default void openChatDraftScreen(String text) {
        // Default no-op; loader helpers provide the real client implementation.
    }

    /**
     * Opens an external recipe-viewer view (recipes or uses) for {@code stack} using a viewer that
     * only exists on one loader. Currently this is REI, which ships <b>only</b> on Fabric; referencing
     * {@code me.shedaniel.rei.*} from xplat (or neoforge/forge) would break those builds since REI is
     * not on their classpath. The Fabric helper overrides this to route to {@code ReiRecipeBridge} when
     * {@code roughlyenoughitems} is loaded; neoforge/forge inherit this no-op default.
     *
     * @param stack the stack to look up
     * @param uses  {@code true} to show usages (what consumes it), {@code false} to show recipes
     * @return {@code true} if an external view was opened
     */
    default boolean openExternalRecipeView(ItemStack stack, boolean uses) {
        return false;
    }

    /**
     * Returns the vanilla {@link ItemStack} currently under the cursor in a loader-only external recipe
     * viewer's window, or {@link ItemStack#EMPTY} if none. Used so AMI's favorite key can favorite the
     * hovered viewer item into AMI's own favorites. Currently this is REI, which ships <b>only</b> on
     * Fabric; referencing {@code me.shedaniel.rei.*} from xplat (or neoforge/forge) would break those
     * builds. The Fabric helper overrides this to read REI's focused stack when {@code roughlyenoughitems}
     * is loaded; neoforge/forge inherit this no-op default (EMI/JEI are handled by their own bridges).
     */
    default ItemStack getHoveredExternalViewerStack() {
        return ItemStack.EMPTY;
    }

    /**
     * Renders a tooltip with an associated ItemStack for decoration/positioning purposes.
     * Delegates to the 6-arg {@code GuiGraphics.renderTooltip(Font, List, Optional, ItemStack, int, int)}
     * overload added by Forge/NeoForge.
     */
    void renderItemTooltip(GuiGraphics g, Font font, List<Component> lines,
                           Optional<TooltipComponent> image, ItemStack stack, int x, int y);

    /**
     * Begins a GUI quad batch on the shared {@code Tesselator}, returning the loader's buffer object.
     * xplat cannot call this API directly because the vertex-buffer API differs between MC 1.20.1
     * (Tesselator.getBuilder + BufferBuilder.begin/vertex/uv/color/endVertex/end) and 1.21.1
     * (Tesselator.begin + BufferBuilder.addVertex/setUv/setColor/buildOrThrow), and reflection by
     * Mojang/SRG name fails on Fabric's intermediary runtime. Each loader implements these three
     * methods with direct calls so Loom's remap (and NeoForge/Forge's native names) resolve correctly.
     *
     * @param textured {@code true} for POSITION_TEX_COLOR (uv used), {@code false} for POSITION_COLOR.
     */
    Object beginGuiQuadBatch(boolean textured);

    /** Appends one vertex to a batch started by {@link #beginGuiQuadBatch(boolean)}. */
    void guiQuadVertex(Object buffer, Matrix4f matrix, float x, float y, float u, float v,
                       float r, float g, float b, float a, boolean textured);

    /** Builds and draws (via {@code BufferUploader.drawWithShader}) a batch started by {@link #beginGuiQuadBatch(boolean)}. */
    void endAndDrawGuiQuadBatch(Object buffer);

    default void renderVanillaScrollbar(Object guiGraphics, ResourceLocation scroller, ResourceLocation scrollerBackground,
                                        int x, int y, int width, int height, int thumbY, int thumbHeight) {
        throw new UnsupportedOperationException("Vanilla scrollbar rendering is client-only");
    }

    /** Returns true if the off-screen framebuffer item icon cache is stable on this platform/MC version. */
    default boolean supportsItemIconCache() {
        return false;
    }

    default ResourceLocation rl(String namespaceAndPath) {
        int colon = namespaceAndPath.indexOf(':');
        return colon >= 0
                ? rl(namespaceAndPath.substring(0, colon), namespaceAndPath.substring(colon + 1))
                : rl("minecraft", namespaceAndPath);
    }

    default boolean sameItemSameComponents(ItemStack first, ItemStack second) {
        Boolean componentResult = invokeStaticItemStackComparison("isSameItemSameComponents", first, second);
        if (componentResult != null) return componentResult;
        Boolean tagResult = invokeStaticItemStackComparison("isSameItemSameTags", first, second);
        return tagResult != null && tagResult;
    }

    default ResourceLocation getPlayerSkinTexture(Object playerInfo) {
        if (playerInfo == null) return null;
        Object skin = invokeNoArg(playerInfo, "getSkin");
        if (skin != null) {
            Object texture = invokeNoArg(skin, "texture");
            if (texture instanceof ResourceLocation location) return location;
        }
        Object location = invokeNoArg(playerInfo, "getSkinLocation");
        return location instanceof ResourceLocation resourceLocation ? resourceLocation : null;
    }

    default ItemStack getRecipeResultItem(Object recipeOrHolder, RegistryAccess registryAccess) {
        Object recipe = unwrapRecipe(recipeOrHolder);
        Object result = invokeOneArg(recipe, "getResultItem", registryAccess);
        return result instanceof ItemStack stack ? stack : ItemStack.EMPTY;
    }

    @SuppressWarnings("unchecked")
    default List<Ingredient> getRecipeIngredients(Object recipeOrHolder) {
        Object recipe = unwrapRecipe(recipeOrHolder);
        Object result = invokeNoArg(recipe, "getIngredients");
        return result instanceof List<?> list ? (List<Ingredient>) list : List.of();
    }

    @SuppressWarnings("unchecked")
    default List<Component> getTooltipLines(ItemStack stack, Level level) {
        if (stack == null || stack.isEmpty()) return List.of();
        Object context = createTooltipContext(level);
        for (Method method : stack.getClass().getMethods()) {
            if (!method.getName().equals("getTooltipLines")) continue;
            Class<?>[] parameterTypes = method.getParameterTypes();
            try {
                Object result = null;
                if (parameterTypes.length == 2 && parameterTypes[1].isAssignableFrom(TooltipFlag.class)) {
                    result = method.invoke(stack, null, TooltipFlag.Default.NORMAL);
                } else if (parameterTypes.length == 3 && context != null
                        && parameterTypes[0].isAssignableFrom(context.getClass())
                        && parameterTypes[2].isAssignableFrom(TooltipFlag.class)) {
                    result = method.invoke(stack, context, null, TooltipFlag.Default.NORMAL);
                }
                if (result instanceof List<?> list) {
                    return (List<Component>) list;
                }
            } catch (ReflectiveOperationException | RuntimeException ignored) {
                return List.of();
            }
        }
        return List.of();
    }

    default Optional<FoodStats> getFoodStats(ItemStack stack) {
        if (stack == null || stack.isEmpty()) return Optional.empty();
        Object food = invokeOneArg(stack.getItem(), "getFoodProperties", stack, null);
        if (food == null) {
            food = getFoodComponent(stack);
        }
        if (food == null) return Optional.empty();

        Integer nutrition = extractInt(food, "nutrition", "getNutrition");
        Float saturation = extractFloat(food, "saturation");
        if (saturation == null) {
            Float saturationModifier = extractFloat(food, "getSaturationModifier");
            if (nutrition != null && saturationModifier != null) {
                saturation = nutrition * saturationModifier * 2.0F;
            }
        }
        return nutrition == null || saturation == null
                ? Optional.empty()
                : Optional.of(new FoodStats(nutrition, saturation));
    }

    default boolean hasFood(ItemStack stack) {
        if (getFoodStats(stack).isPresent()) return true;
        if (stack == null || stack.isEmpty()) return false;
        Object edible = invokeNoArg(stack.getItem(), "isEdible");
        if (edible instanceof Boolean bool && bool) return true;
        Object foodComponentType = getDataComponentType("FOOD");
        if (foodComponentType == null) return false;
        Object itemComponents = invokeNoArg(stack.getItem(), "components");
        Object hasFood = invokeOneArg(itemComponents, "has", foodComponentType);
        return hasFood instanceof Boolean bool && bool;
    }

    default boolean hasDefaultItemComponent(Object item, String componentFieldName) {
        if (item == null) return false;
        Object componentType = getDataComponentType(componentFieldName);
        if (componentType == null) return false;
        Object itemComponents = invokeNoArg(item, "components");
        Object hasComponent = invokeOneArg(itemComponents, "has", componentType);
        return hasComponent instanceof Boolean bool && bool;
    }

    default boolean hasStackComponent(ItemStack stack, String componentFieldName) {
        if (stack == null || stack.isEmpty()) return false;
        Object componentType = getDataComponentType(componentFieldName);
        if (componentType == null) return false;
        Object hasComponent = invokeOneArg(stack, "has", componentType);
        return hasComponent instanceof Boolean bool && bool;
    }

    default Set<String> getDefaultItemComponentNames(Object item, Collection<String> componentFieldNames) {
        if (item == null || componentFieldNames == null || componentFieldNames.isEmpty()) {
            return Set.of();
        }
        Object itemComponents = invokeNoArg(item, "components");
        if (itemComponents == null) {
            return Set.of();
        }
        Set<String> result = new HashSet<>();
        for (String componentFieldName : componentFieldNames) {
            Object componentType = getDataComponentType(componentFieldName);
            if (componentType == null) continue;
            Object hasComponent = invokeOneArg(itemComponents, "has", componentType);
            if (hasComponent instanceof Boolean bool && bool) {
                result.add(componentFieldName);
            }
        }
        return result;
    }

    default Set<String> getStackComponentNames(ItemStack stack, Collection<String> componentFieldNames) {
        if (stack == null || stack.isEmpty() || componentFieldNames == null || componentFieldNames.isEmpty()) {
            return Set.of();
        }
        Set<String> result = new HashSet<>();
        for (String componentFieldName : componentFieldNames) {
            Object componentType = getDataComponentType(componentFieldName);
            if (componentType == null) continue;
            Object hasComponent = invokeOneArg(stack, "has", componentType);
            if (hasComponent instanceof Boolean bool && bool) {
                result.add(componentFieldName);
            }
        }
        return result;
    }

    default Optional<String> getEquipmentSlotName(ItemStack stack) {
        if (stack == null || stack.isEmpty()) return Optional.empty();
        Object slot = invokeNoArg(stack, "getEquipmentSlot");
        if (slot == null) {
            slot = invokeNoArg(stack.getItem(), "getEquipmentSlot");
        }
        if (slot == null) {
            Object block = invokeNoArg(stack.getItem(), "getBlock");
            slot = invokeNoArg(block, "getEquipmentSlot");
        }
        return slot == null
                ? Optional.empty()
                : Optional.of(slot.toString().toLowerCase(java.util.Locale.ROOT));
    }

    default Optional<Integer> getItemEnergyCapacity(ItemStack stack) {
        return Optional.empty();
    }

    default Optional<Integer> getItemEnergyStored(ItemStack stack) {
        return Optional.empty();
    }

    default net.minecraft.network.chat.Component getFluidDisplayName(net.minecraft.world.level.material.Fluid fluid) {
        return net.minecraft.network.chat.Component.empty();
    }

    @org.jetbrains.annotations.Nullable
    default ResourceLocation getFluidStillTexture(net.minecraft.world.level.material.Fluid fluid) {
        return null;
    }

    default int getFluidTintColor(net.minecraft.world.level.material.Fluid fluid) {
        return 0xFFFFFFFF;
    }

    /**
     * Renders a fluid still sprite with tint. Default uses GuiGraphics.blit() + setShaderColor()
     * which works correctly in MC 1.20.x. NeoForge 1.21.x overrides with the BufferBuilder API
     * because GuiGraphics.blit() hard-codes color=-1 in that version.
     */
    default void renderFluidSprite(net.minecraft.client.gui.GuiGraphics g,
                                   net.minecraft.client.renderer.texture.TextureAtlasSprite sprite,
                                   int tintColor, int x, int y, int size) {
        int alphaInt = (tintColor >> 24) & 0xFF;
        float a = alphaInt == 0 ? 1.0f : alphaInt / 255.0f;
        float r = ((tintColor >> 16) & 0xFF) / 255.0f;
        float gv = ((tintColor >> 8) & 0xFF) / 255.0f;
        float b = (tintColor & 0xFF) / 255.0f;
        com.mojang.blaze3d.systems.RenderSystem.enableBlend();
        com.mojang.blaze3d.systems.RenderSystem.setShaderColor(r, gv, b, a);
        g.blit(x, y, 100, size, size, sprite);
        com.mojang.blaze3d.systems.RenderSystem.setShaderColor(1f, 1f, 1f, 1f);
        com.mojang.blaze3d.systems.RenderSystem.disableBlend();
    }

    default OptionalLong getItemFluidCapacity(ItemStack stack) {
        return OptionalLong.empty();
    }

    default OptionalLong getItemFluidAmount(ItemStack stack) {
        return OptionalLong.empty();
    }

    default OptionalLong getItemHandlerCapacity(ItemStack stack) {
        return OptionalLong.empty();
    }

    default OptionalLong getBlockItemHandlerCapacity(ItemStack stack, Level level) {
        return OptionalLong.empty();
    }

    default boolean isInstanceOf(Object target, String className) {
        if (target == null) return false;
        try {
            return Class.forName(className).isInstance(target);
        } catch (ClassNotFoundException ignored) {
            return false;
        }
    }

    default List<ItemAttributeModifier> getMainHandAttackModifiers(ItemStack stack) {
        return List.of();
    }

    default ItemStack createPotionSubtypeStack(Item potionItem, Object potionHolder) {
        return ItemStack.EMPTY;
    }

    default ItemStack createEnchantedBookSubtypeStack(Object enchantmentHolder, int level) {
        return ItemStack.EMPTY;
    }

    default List<SubtypeStack> createSuspiciousStewSubtypeStacks() {
        return List.of();
    }

    default List<SubtypeStack> createFireworkRocketSubtypeStacks() {
        return List.of();
    }

    default ItemStack createGoatHornSubtypeStack(Object instrumentHolder) {
        return ItemStack.EMPTY;
    }

    default ItemStack createPlayerHeadStack(String name) {
        return ItemStack.EMPTY;
    }

    default ItemStack createPlayerHeadStack(String name, java.util.UUID uuid) {
        return createPlayerHeadStack(name);
    }

    default ItemStack createPlayerHeadStack(GameProfile profile) {
        if (profile == null) return ItemStack.EMPTY;
        return createPlayerHeadStack(profile.getName(), profile.getId());
    }

    default String playerHeadGiveCommand(String name) {
        return "give @s minecraft:player_head 1";
    }

    default ResourceLocation getSpawnEggEntityTypeId(SpawnEggItem egg, ItemStack stack) {
        return null;
    }

    default OptionalLong getContainerComponentCapacity(ItemStack stack, long defaultStackSize) {
        if (stack == null || stack.isEmpty()) return OptionalLong.empty();
        Object containerType = getDataComponentType("CONTAINER");
        if (containerType == null) return OptionalLong.empty();
        Object contents = invokeOneArg(stack, "get", containerType);
        Object slots = invokeNoArg(contents, "getSlots");
        return slots instanceof Number number && number.intValue() > 0
                ? OptionalLong.of(number.longValue() * defaultStackSize)
                : OptionalLong.empty();
    }

    /**
     * Discovers all classes annotated with {@code annotationClass} that implement {@code pluginClass},
     * using the platform's mod scan data (e.g. NeoForge ModList).
     */
    default <T> List<T> discoverAnnotatedPlugins(Class<?> annotationClass, Class<T> pluginClass) {
        return List.of();
    }

    boolean isRecipeIndexBuilt();

    List<AmiRecipeHolder<?>> getRecipesFor(ItemStack target);

    boolean hasRecipesFor(ItemStack target);

    List<AmiRecipeHolder<?>> getUsesFor(ItemStack target);

    boolean hasUsesFor(ItemStack target);

    List<AmiRecipeHolder<?>> getAllRecipesOfType(RecipeType<?> type);

    default boolean tryLoadGlobalIndexCache() {
        return false;
    }

    default void saveGlobalIndexCache() {
    }

    enum ItemAttributeKind {
        ATTACK_DAMAGE,
        ATTACK_SPEED
    }

    enum ItemAttributeOperation {
        ADD_VALUE,
        ADD_MULTIPLIED_BASE,
        ADD_MULTIPLIED_TOTAL
    }

    record ItemAttributeModifier(ItemAttributeKind kind, double amount, ItemAttributeOperation operation) {
    }

    record SubtypeStack(ResourceLocation subtypeId, ItemStack stack) {
    }

    final class DataComponentTypeCache {
        private static final Object MISSING = new Object();
        private static final ConcurrentMap<String, Object> VALUES = new ConcurrentHashMap<>();

        private DataComponentTypeCache() {
        }

        static Object get(String fieldName) {
            if (fieldName == null || fieldName.isBlank()) {
                return null;
            }
            Object value = VALUES.computeIfAbsent(fieldName, DataComponentTypeCache::lookup);
            return value == MISSING ? null : value;
        }

        private static Object lookup(String fieldName) {
            try {
                Class<?> dataComponentsClass = Class.forName("net.minecraft.core.component.DataComponents");
                Field field = dataComponentsClass.getField(fieldName);
                return field.get(null);
            } catch (ReflectiveOperationException | RuntimeException ignored) {
                return MISSING;
            }
        }
    }
}
