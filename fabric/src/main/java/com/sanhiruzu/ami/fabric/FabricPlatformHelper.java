package com.sanhiruzu.ami.fabric;

import com.mojang.blaze3d.platform.InputConstants;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.sanhiruzu.ami.fabric.client.FabricAmiKeyMappings;
import com.sanhiruzu.ami.platform.IAmiKeyMappings;
import com.sanhiruzu.ami.platform.IPlatformHelper;
import com.sanhiruzu.ami.recipe.AmiRecipeIndex;
import com.sanhiruzu.ami.util.AmiRecipeHolder;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.inventory.tooltip.TooltipComponent;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.biome.Biome;

import java.nio.file.Path;
import java.util.List;
import java.util.Optional;

public class FabricPlatformHelper implements IPlatformHelper {

    private static final IAmiKeyMappings KEY_MAPPINGS = new FabricAmiKeyMappings();

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
}
