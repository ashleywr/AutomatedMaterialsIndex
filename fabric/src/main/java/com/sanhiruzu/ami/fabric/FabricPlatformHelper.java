package com.sanhiruzu.ami.fabric;

import com.mojang.blaze3d.platform.InputConstants;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.sanhiruzu.ami.fabric.client.FabricAmiKeyMappings;
import com.sanhiruzu.ami.index.metrics.FoodStats;
import com.sanhiruzu.ami.platform.IAmiKeyMappings;
import com.sanhiruzu.ami.platform.IPlatformHelper;
import com.sanhiruzu.ami.recipe.AmiRecipeIndex;
import com.sanhiruzu.ami.util.AmiRecipeHolder;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.client.resources.PlayerSkin;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.inventory.tooltip.TooltipComponent;
import net.minecraft.world.item.Equipable;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.ItemContainerContents;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.Biome;

import java.nio.file.Path;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.OptionalLong;
import java.util.Set;

public class FabricPlatformHelper implements IPlatformHelper {

    private static final IAmiKeyMappings KEY_MAPPINGS = new FabricAmiKeyMappings();

    /**
     * Maps the {@code DataComponents} field-name string literals that AMI's xplat code passes to
     * {@link #hasDefaultItemComponent}, {@link #hasStackComponent}, {@link #getDefaultItemComponentNames}
     * and {@link #getStackComponentNames} to direct {@code DataComponents.*} references. Direct references
     * are compiled against Mojmap and remapped to intermediary by Loom, so they resolve at runtime on the
     * intermediary-named Fabric production runtime (reflection by Mojmap string literal would silently fail).
     * The key set is the union of every literal AMI actually passes (see FacetIndexer + OntologyClassifier).
     */
    private static final Map<String, DataComponentType<?>> COMPONENT_TYPES_BY_FIELD_NAME = Map.ofEntries(
            Map.entry("FOOD", DataComponents.FOOD),
            Map.entry("CONTAINER", DataComponents.CONTAINER),
            Map.entry("POTION_CONTENTS", DataComponents.POTION_CONTENTS),
            Map.entry("STORED_ENCHANTMENTS", DataComponents.STORED_ENCHANTMENTS),
            Map.entry("TOOL", DataComponents.TOOL),
            Map.entry("MAX_DAMAGE", DataComponents.MAX_DAMAGE),
            Map.entry("DAMAGE", DataComponents.DAMAGE),
            Map.entry("BUNDLE_CONTENTS", DataComponents.BUNDLE_CONTENTS),
            Map.entry("CUSTOM_DATA", DataComponents.CUSTOM_DATA),
            Map.entry("ENTITY_DATA", DataComponents.ENTITY_DATA),
            Map.entry("BUCKET_ENTITY_DATA", DataComponents.BUCKET_ENTITY_DATA),
            Map.entry("BLOCK_ENTITY_DATA", DataComponents.BLOCK_ENTITY_DATA)
    );

    // -------------------------------------------------------------------------
    // Platform identity
    // -------------------------------------------------------------------------

    @Override
    public boolean isClient() {
        return FabricLoader.getInstance().getEnvironmentType()
                == net.fabricmc.api.EnvType.CLIENT;
    }

    // -------------------------------------------------------------------------
    // Mod metadata
    // -------------------------------------------------------------------------

    @Override
    public Optional<String> getModName(String modId) {
        return FabricLoader.getInstance().getModContainer(modId)
                .map(c -> c.getMetadata().getName());
    }

    @Override
    public Optional<String> getModVersion(String modId) {
        return FabricLoader.getInstance().getModContainer(modId)
                .map(c -> c.getMetadata().getVersion().getFriendlyString());
    }

    @Override
    public boolean isModLoaded(String modId) {
        return FabricLoader.getInstance().isModLoaded(modId);
    }

    @Override
    public List<String> getLoadedModIds() {
        List<String> result = new java.util.ArrayList<>();
        for (var container : FabricLoader.getInstance().getAllMods()) {
            result.add(container.getMetadata().getId());
        }
        return result;
    }

    @Override
    public List<String> getLoadedModFingerprintEntries() {
        List<String> result = new java.util.ArrayList<>();
        for (var container : FabricLoader.getInstance().getAllMods()) {
            var meta = container.getMetadata();
            result.add(meta.getId() + ":" + meta.getVersion().getFriendlyString());
        }
        return result;
    }

    // -------------------------------------------------------------------------
    // Paths
    // -------------------------------------------------------------------------

    @Override
    public Path getConfigDir() {
        return FabricLoader.getInstance().getConfigDir();
    }

    @Override
    public Path getGameDir() {
        return FabricLoader.getInstance().getGameDir();
    }

    // -------------------------------------------------------------------------
    // Resource locations
    // -------------------------------------------------------------------------

    @Override
    public ResourceLocation rl(String namespace, String path) {
        return ResourceLocation.fromNamespaceAndPath(namespace, path);
    }

    // -------------------------------------------------------------------------
    // Key mappings
    // -------------------------------------------------------------------------

    @Override
    public IAmiKeyMappings keyMappings() {
        return KEY_MAPPINGS;
    }

    /**
     * Vanilla equivalent of NeoForge's {@code KeyMapping.isActiveAndMatches(key)}.
     * NeoForge patches in conflict-context awareness; on Fabric we approximate with
     * the vanilla {@code matchesMouse} / {@code matches} methods on the KeyMapping.
     */
    @Override
    public boolean keyActiveAndMatches(KeyMapping mapping, InputConstants.Key key) {
        if (mapping.isUnbound()) {
            return false;
        }
        if (key.getType() == InputConstants.Type.MOUSE) {
            return mapping.matchesMouse(key.getValue());
        }
        return mapping.matches(key.getValue(), InputConstants.UNKNOWN.getValue());
    }

    // -------------------------------------------------------------------------
    // GUI / screen field access (access-widened via ami.accesswidener)
    // -------------------------------------------------------------------------

    @Override
    public Slot getHoveredSlot(AbstractContainerScreen<?> screen) {
        return screen.hoveredSlot;
    }

    @Override
    public int getGuiLeft(AbstractContainerScreen<?> screen) {
        return screen.leftPos;
    }

    @Override
    public int getGuiTop(AbstractContainerScreen<?> screen) {
        return screen.topPos;
    }

    // -------------------------------------------------------------------------
    // Biome climate (access-widened climateSettings field)
    // -------------------------------------------------------------------------

    /**
     * Returns downfall from the biome's vanilla climateSettings record.
     * Equivalent to NeoForge's {@code biome.getModifiedClimateSettings().downfall()}.
     */
    @Override
    public float getBiomeDownfall(Biome biome) {
        return biome.climateSettings.downfall();
    }

    /**
     * Returns whether the biome's temperature modifier is FROZEN.
     * Equivalent to NeoForge's {@code biome.getModifiedClimateSettings().temperatureModifier() == FROZEN}.
     */
    @Override
    public boolean isBiomeTemperatureFrozen(Biome biome) {
        return biome.climateSettings.temperatureModifier() == Biome.TemperatureModifier.FROZEN;
    }

    // -------------------------------------------------------------------------
    // Tooltip rendering
    // -------------------------------------------------------------------------

    /**
     * Fabric/vanilla tooltip render: drops the ItemStack argument (NeoForge-only decorator).
     * Calls the 5-arg vanilla {@code GuiGraphics.renderTooltip}.
     */
    @Override
    public void renderItemTooltip(GuiGraphics g, Font font, List<Component> lines,
                                  Optional<TooltipComponent> image, ItemStack stack, int x, int y) {
        g.renderTooltip(font, lines, image, x, y);
    }

    // -------------------------------------------------------------------------
    // GUI quad batch rendering (1.21.1 vertex-buffer API; direct calls so Loom
    // remaps the Mojang names to intermediary — reflection by name would not).
    // -------------------------------------------------------------------------

    @Override
    public Object beginGuiQuadBatch(boolean textured) {
        return Tesselator.getInstance().begin(VertexFormat.Mode.QUADS,
                textured ? DefaultVertexFormat.POSITION_TEX_COLOR : DefaultVertexFormat.POSITION_COLOR);
    }

    @Override
    public void guiQuadVertex(Object buffer, org.joml.Matrix4f matrix, float x, float y, float u, float v,
                              float r, float g, float b, float a, boolean textured) {
        VertexConsumer vertex = ((BufferBuilder) buffer).addVertex(matrix, x, y, 0.0f);
        if (textured) {
            vertex.setUv(u, v);
        }
        vertex.setColor(r, g, b, a);
    }

    @Override
    public void endAndDrawGuiQuadBatch(Object buffer) {
        BufferUploader.drawWithShader(((BufferBuilder) buffer).buildOrThrow());
    }

    // -------------------------------------------------------------------------
    // Recipe index
    // -------------------------------------------------------------------------

    @Override
    public boolean isRecipeIndexBuilt() {
        return AmiRecipeIndex.getInstance().isBuilt();
    }

    @Override
    public List<AmiRecipeHolder<?>> getRecipesFor(ItemStack target) {
        return AmiRecipeIndex.getInstance().getRecipesFor(target);
    }

    @Override
    public boolean hasRecipesFor(ItemStack target) {
        return AmiRecipeIndex.getInstance().hasRecipesFor(target);
    }

    @Override
    public List<AmiRecipeHolder<?>> getUsesFor(ItemStack target) {
        return AmiRecipeIndex.getInstance().getUsesFor(target);
    }

    @Override
    public boolean hasUsesFor(ItemStack target) {
        return AmiRecipeIndex.getInstance().hasUsesFor(target);
    }

    @Override
    @SuppressWarnings({"unchecked", "rawtypes"})
    public List<AmiRecipeHolder<?>> getAllRecipesOfType(RecipeType<?> type) {
        return (List<AmiRecipeHolder<?>>) (List<?>) AmiRecipeIndex.getInstance().getAllRecipesOfType((RecipeType) type);
    }

    // -------------------------------------------------------------------------
    // Item metadata extraction (direct 1.21.1 API calls; the xplat defaults use
    // reflection-by-Mojmap-name which silently fails on Fabric's intermediary
    // runtime, so each of these is overridden with a remappable direct call).
    // -------------------------------------------------------------------------

    @Override
    public List<Component> getTooltipLines(ItemStack stack, Level level) {
        if (stack == null || stack.isEmpty()) {
            return List.of();
        }
        Item.TooltipContext context = level == null ? Item.TooltipContext.EMPTY : Item.TooltipContext.of(level);
        return stack.getTooltipLines(context, null, TooltipFlag.Default.NORMAL);
    }

    @Override
    public Optional<FoodStats> getFoodStats(ItemStack stack) {
        if (stack == null || stack.isEmpty()) {
            return Optional.empty();
        }
        FoodProperties food = stack.get(DataComponents.FOOD);
        if (food == null) {
            return Optional.empty();
        }
        return Optional.of(new FoodStats(food.nutrition(), food.saturation()));
    }

    @Override
    public boolean hasFood(ItemStack stack) {
        if (stack == null || stack.isEmpty()) {
            return false;
        }
        return stack.has(DataComponents.FOOD);
    }

    @Override
    public boolean sameItemSameComponents(ItemStack first, ItemStack second) {
        return ItemStack.isSameItemSameComponents(first, second);
    }

    @Override
    public Optional<String> getEquipmentSlotName(ItemStack stack) {
        if (stack == null || stack.isEmpty()) {
            return Optional.empty();
        }
        // MC 1.21.1 has no Equippable DataComponent (that arrived in 1.21.2); equip info is exposed
        // via the vanilla Equipable interface, resolved per-stack by Equipable.get(stack).
        Equipable equipable = Equipable.get(stack);
        if (equipable == null) {
            return Optional.empty();
        }
        EquipmentSlot slot = equipable.getEquipmentSlot();
        if (slot == null) {
            return Optional.empty();
        }
        return Optional.of(slot.toString().toLowerCase(Locale.ROOT));
    }

    @Override
    public ItemStack getRecipeResultItem(Object recipeOrHolder, RegistryAccess registryAccess) {
        Recipe<?> recipe = unwrapRecipe(recipeOrHolder);
        if (recipe == null) {
            return ItemStack.EMPTY;
        }
        ItemStack result = recipe.getResultItem(registryAccess);
        return result == null ? ItemStack.EMPTY : result;
    }

    @Override
    public List<Ingredient> getRecipeIngredients(Object recipeOrHolder) {
        Recipe<?> recipe = unwrapRecipe(recipeOrHolder);
        if (recipe == null) {
            return List.of();
        }
        List<Ingredient> ingredients = recipe.getIngredients();
        return ingredients == null ? List.of() : ingredients;
    }

    @Override
    public OptionalLong getContainerComponentCapacity(ItemStack stack, long defaultStackSize) {
        if (stack == null || stack.isEmpty()) {
            return OptionalLong.empty();
        }
        ItemContainerContents contents = stack.get(DataComponents.CONTAINER);
        if (contents == null) {
            return OptionalLong.empty();
        }
        // The xplat default counts slots (NeoForge's patched ItemContainerContents.getSlots()). In vanilla
        // Mojmap, stream() yields one entry per slot (including empty ones), so its count is the slot count.
        long slots = contents.stream().count();
        return slots > 0 ? OptionalLong.of(slots * defaultStackSize) : OptionalLong.empty();
    }

    @Override
    public ResourceLocation getPlayerSkinTexture(Object playerInfo) {
        if (!(playerInfo instanceof PlayerInfo info)) {
            return null;
        }
        PlayerSkin skin = info.getSkin();
        return skin == null ? null : skin.texture();
    }

    @Override
    public boolean hasDefaultItemComponent(Object item, String componentFieldName) {
        if (!(item instanceof Item actualItem)) {
            return false;
        }
        DataComponentType<?> type = COMPONENT_TYPES_BY_FIELD_NAME.get(componentFieldName);
        return type != null && actualItem.components().has(type);
    }

    @Override
    public boolean hasStackComponent(ItemStack stack, String componentFieldName) {
        if (stack == null || stack.isEmpty()) {
            return false;
        }
        DataComponentType<?> type = COMPONENT_TYPES_BY_FIELD_NAME.get(componentFieldName);
        return type != null && stack.has(type);
    }

    @Override
    public Set<String> getDefaultItemComponentNames(Object item, Collection<String> componentFieldNames) {
        if (!(item instanceof Item actualItem) || componentFieldNames == null || componentFieldNames.isEmpty()) {
            return Set.of();
        }
        Set<String> result = new HashSet<>();
        for (String fieldName : componentFieldNames) {
            DataComponentType<?> type = COMPONENT_TYPES_BY_FIELD_NAME.get(fieldName);
            if (type != null && actualItem.components().has(type)) {
                result.add(fieldName);
            }
        }
        return result;
    }

    @Override
    public Set<String> getStackComponentNames(ItemStack stack, Collection<String> componentFieldNames) {
        if (stack == null || stack.isEmpty() || componentFieldNames == null || componentFieldNames.isEmpty()) {
            return Set.of();
        }
        Set<String> result = new HashSet<>();
        for (String fieldName : componentFieldNames) {
            DataComponentType<?> type = COMPONENT_TYPES_BY_FIELD_NAME.get(fieldName);
            if (type != null && stack.has(type)) {
                result.add(fieldName);
            }
        }
        return result;
    }

    private static Recipe<?> unwrapRecipe(Object recipeOrHolder) {
        if (recipeOrHolder instanceof RecipeHolder<?> holder) {
            return holder.value();
        }
        if (recipeOrHolder instanceof Recipe<?> recipe) {
            return recipe;
        }
        return null;
    }
}
