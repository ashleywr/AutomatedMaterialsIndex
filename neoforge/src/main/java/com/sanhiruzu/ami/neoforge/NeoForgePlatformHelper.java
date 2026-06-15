package com.sanhiruzu.ami.neoforge;

import com.mojang.authlib.GameProfile;
import com.mojang.blaze3d.platform.InputConstants;
import com.sanhiruzu.ami.index.GlobalIndexCache;
import com.sanhiruzu.ami.neoforge.client.AMIKeyMappings;
import com.sanhiruzu.ami.platform.IAmiKeyMappings;
import com.sanhiruzu.ami.platform.IPlatformHelper;
import com.sanhiruzu.ami.recipe.AmiRecipeIndex;
import com.sanhiruzu.ami.util.AmiRecipeHolder;
import it.unimi.dsi.fastutil.ints.IntList;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderSet;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.inventory.tooltip.TooltipComponent;
import net.minecraft.world.item.*;
import net.minecraft.world.item.alchemy.PotionContents;
import net.minecraft.world.item.component.*;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.ItemEnchantments;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.FlowerBlock;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.fml.ModList;
import net.neoforged.fml.loading.FMLPaths;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.energy.IEnergyStorage;
import net.neoforged.neoforge.fluids.capability.IFluidHandlerItem;
import net.neoforged.neoforge.items.IItemHandler;
import org.objectweb.asm.Type;

import java.nio.file.Path;
import java.util.*;

public class NeoForgePlatformHelper implements IPlatformHelper {
    private static final IAmiKeyMappings KEY_MAPPINGS = new IAmiKeyMappings() {
        @Override
        public KeyMapping favorite() {
            return AMIKeyMappings.FAVORITE;
        }

        @Override
        public KeyMapping toggleViewer() {
            return AMIKeyMappings.TOGGLE_VIEWER;
        }

        @Override
        public KeyMapping showRecipes() {
            return AMIKeyMappings.SHOW_RECIPES;
        }

        @Override
        public KeyMapping showUses() {
            return AMIKeyMappings.SHOW_USES;
        }

        @Override
        public KeyMapping cheatGiveStack() {
            return AMIKeyMappings.CHEAT_GIVE_STACK;
        }

        @Override
        public KeyMapping cheatGiveOne() {
            return AMIKeyMappings.CHEAT_GIVE_ONE;
        }

        @Override
        public KeyMapping debugTooltips() {
            return AMIKeyMappings.DEBUG_TOOLTIPS;
        }

        @Override
        public KeyMapping recipeBack() {
            return AMIKeyMappings.RECIPE_BACK;
        }

        @Override
        public KeyMapping[] all() {
            return new KeyMapping[]{
                    AMIKeyMappings.FAVORITE, AMIKeyMappings.TOGGLE_VIEWER, AMIKeyMappings.SHOW_RECIPES,
                    AMIKeyMappings.SHOW_USES, AMIKeyMappings.CHEAT_GIVE_STACK, AMIKeyMappings.CHEAT_GIVE_ONE,
                    AMIKeyMappings.DEBUG_TOOLTIPS, AMIKeyMappings.RECIPE_BACK
            };
        }
    };

    private static ItemAttributeKind attackAttributeKind(Holder<Attribute> attribute) {
        if (sameAttribute(attribute, Attributes.ATTACK_DAMAGE)) return ItemAttributeKind.ATTACK_DAMAGE;
        if (sameAttribute(attribute, Attributes.ATTACK_SPEED)) return ItemAttributeKind.ATTACK_SPEED;
        return null;
    }

    private static boolean sameAttribute(Holder<Attribute> left, Holder<Attribute> right) {
        return left == right || left.equals(right);
    }

    private static ItemAttributeOperation attributeOperation(AttributeModifier.Operation operation) {
        if (operation == AttributeModifier.Operation.ADD_VALUE) return ItemAttributeOperation.ADD_VALUE;
        if (operation == AttributeModifier.Operation.ADD_MULTIPLIED_BASE)
            return ItemAttributeOperation.ADD_MULTIPLIED_BASE;
        if (operation == AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL)
            return ItemAttributeOperation.ADD_MULTIPLIED_TOTAL;
        return null;
    }

    private static long itemHandlerCapacity(IItemHandler handler) {
        if (handler == null || handler.getSlots() <= 0) return 0L;
        long capacity = 0L;
        for (int slot = 0; slot < handler.getSlots(); slot++) {
            capacity += Math.max(0, handler.getSlotLimit(slot));
        }
        return capacity;
    }

    private static String escapeCommandString(String value) {
        return value == null ? "" : value.replace("\\", "\\\\").replace("\"", "\\\"");
    }

    @Override
    public boolean isClient() {
        return FMLEnvironment.dist.isClient();
    }

    @Override
    public boolean supportsItemIconCache() {
        return true;
    }

    @Override
    public Optional<String> getModName(String modId) {
        if (ModList.get() != null) {
            for (var info : ModList.get().getMods()) {
                if (info.getModId().equals(modId)) {
                    return Optional.ofNullable(info.getDisplayName());
                }
            }
        }
        return Optional.empty();
    }

    @Override
    public Optional<String> getModVersion(String modId) {
        if (ModList.get() != null) {
            for (var info : ModList.get().getMods()) {
                if (info.getModId().equals(modId)) {
                    return Optional.ofNullable(info.getVersion().toString());
                }
            }
        }
        return Optional.empty();
    }

    @Override
    public Optional<String> getModMetadataText(String modId) {
        if (ModList.get() != null) {
            for (var info : ModList.get().getMods()) {
                if (info.getModId().equals(modId)) {
                    return Optional.of((info.getDisplayName() + " " + info.getDescription()).trim());
                }
            }
        }
        return Optional.empty();
    }

    @Override
    public boolean isModLoaded(String modId) {
        return ModList.get() != null && ModList.get().isLoaded(modId);
    }

    @Override
    public List<String> getLoadedModIds() {
        if (ModList.get() == null) return List.of();
        List<String> result = new ArrayList<>();
        for (var info : ModList.get().getMods()) {
            result.add(info.getModId());
        }
        return result;
    }

    @Override
    public List<String> getLoadedModFingerprintEntries() {
        if (ModList.get() == null) return List.of();
        List<String> result = new ArrayList<>();
        for (var info : ModList.get().getMods()) {
            result.add(info.getModId() + ":" + info.getVersion());
        }
        return result;
    }

    @Override
    public Path getConfigDir() {
        return FMLPaths.CONFIGDIR.get();
    }

    @Override
    public Path getGameDir() {
        return FMLPaths.GAMEDIR.get();
    }

    @Override
    public ResourceLocation rl(String namespace, String path) {
        return ResourceLocation.fromNamespaceAndPath(namespace, path);
    }

    @Override
    public IAmiKeyMappings keyMappings() {
        return KEY_MAPPINGS;
    }

    @Override
    public boolean keyActiveAndMatches(KeyMapping mapping, InputConstants.Key key) {
        return mapping.isActiveAndMatches(key);
    }

    @Override
    public Slot getHoveredSlot(AbstractContainerScreen<?> screen) {
        return screen.getSlotUnderMouse();
    }

    @Override
    public int getGuiLeft(AbstractContainerScreen<?> screen) {
        return screen.getGuiLeft();
    }

    @Override
    public int getGuiTop(AbstractContainerScreen<?> screen) {
        return screen.getGuiTop();
    }

    @Override
    public float getBiomeDownfall(Biome biome) {
        return biome.getModifiedClimateSettings().downfall();
    }

    @Override
    public boolean isBiomeTemperatureFrozen(Biome biome) {
        return biome.getModifiedClimateSettings().temperatureModifier()
                == Biome.TemperatureModifier.FROZEN;
    }

    @Override
    public void renderItemTooltip(GuiGraphics g, Font font, java.util.List<net.minecraft.network.chat.Component> lines,
                                  java.util.Optional<TooltipComponent> image, ItemStack stack, int x, int y) {
        g.renderTooltip(font, lines, image, stack, x, y);
    }

    @Override
    public void renderVanillaScrollbar(Object guiGraphics, ResourceLocation scroller, ResourceLocation scrollerBackground,
                                       int x, int y, int width, int height, int thumbY, int thumbHeight) {
        GuiGraphics g = (GuiGraphics) guiGraphics;
        g.blitSprite(scroller, x, thumbY, width, thumbHeight);
    }

    @Override
    public Optional<Integer> getItemEnergyCapacity(ItemStack stack) {
        IEnergyStorage energyStorage = stack.getCapability(Capabilities.EnergyStorage.ITEM);
        if (energyStorage == null) return Optional.empty();
        int capacity = energyStorage.getMaxEnergyStored();
        return capacity > 0 ? Optional.of(capacity) : Optional.empty();
    }

    @Override
    public Optional<Integer> getItemEnergyStored(ItemStack stack) {
        IEnergyStorage energyStorage = stack.getCapability(Capabilities.EnergyStorage.ITEM);
        if (energyStorage == null) return Optional.empty();
        int stored = energyStorage.getEnergyStored();
        return stored > 0 ? Optional.of(stored) : Optional.empty();
    }

    @Override
    public net.minecraft.network.chat.Component getFluidDisplayName(net.minecraft.world.level.material.Fluid fluid) {
        return new net.neoforged.neoforge.fluids.FluidStack(fluid, net.neoforged.neoforge.fluids.FluidType.BUCKET_VOLUME).getHoverName();
    }

    @Override
    public net.minecraft.resources.ResourceLocation getFluidStillTexture(net.minecraft.world.level.material.Fluid fluid) {
        try {
            return net.neoforged.neoforge.client.extensions.common.IClientFluidTypeExtensions.of(fluid).getStillTexture();
        } catch (Exception e) {
            return null;
        }
    }

    @Override
    public int getFluidTintColor(net.minecraft.world.level.material.Fluid fluid) {
        try {
            return net.neoforged.neoforge.client.extensions.common.IClientFluidTypeExtensions.of(fluid).getTintColor();
        } catch (Exception e) {
            return 0xFFFFFFFF;
        }
    }

    @Override
    public void renderFluidSprite(net.minecraft.client.gui.GuiGraphics g,
                                  net.minecraft.client.renderer.texture.TextureAtlasSprite sprite,
                                  int tintColor, int x, int y, int size) {
        int alphaInt = (tintColor >> 24) & 0xFF;
        float a = alphaInt == 0 ? 1.0f : alphaInt / 255.0f;
        float r = ((tintColor >> 16) & 0xFF) / 255.0f;
        float gv = ((tintColor >> 8) & 0xFF) / 255.0f;
        float b = (tintColor & 0xFF) / 255.0f;
        com.mojang.blaze3d.systems.RenderSystem.enableBlend();
        com.mojang.blaze3d.systems.RenderSystem.setShaderTexture(0, net.minecraft.world.inventory.InventoryMenu.BLOCK_ATLAS);
        com.mojang.blaze3d.systems.RenderSystem.setShader(net.minecraft.client.renderer.GameRenderer::getPositionTexShader);
        com.mojang.blaze3d.systems.RenderSystem.setShaderColor(r, gv, b, a);
        org.joml.Matrix4f matrix = g.pose().last().pose();
        com.mojang.blaze3d.vertex.Tesselator tesselator = com.mojang.blaze3d.vertex.Tesselator.getInstance();
        com.mojang.blaze3d.vertex.BufferBuilder buf = tesselator.begin(com.mojang.blaze3d.vertex.VertexFormat.Mode.QUADS, com.mojang.blaze3d.vertex.DefaultVertexFormat.POSITION_TEX);
        float u0 = sprite.getU0(), u1 = sprite.getU1();
        float v0 = sprite.getV0(), v1 = sprite.getV1();
        buf.addVertex(matrix, x,        y + size, 100).setUv(u0, v1);
        buf.addVertex(matrix, x + size, y + size, 100).setUv(u1, v1);
        buf.addVertex(matrix, x + size, y,        100).setUv(u1, v0);
        buf.addVertex(matrix, x,        y,        100).setUv(u0, v0);
        com.mojang.blaze3d.vertex.BufferUploader.drawWithShader(buf.buildOrThrow());
        com.mojang.blaze3d.systems.RenderSystem.setShaderColor(1f, 1f, 1f, 1f);
        com.mojang.blaze3d.systems.RenderSystem.disableBlend();
    }

    @Override
    public OptionalLong getItemFluidCapacity(ItemStack stack) {
        IFluidHandlerItem handler = stack.getCapability(Capabilities.FluidHandler.ITEM);
        if (handler == null || handler.getTanks() <= 0) return OptionalLong.empty();
        long capacity = 0L;
        for (int tank = 0; tank < handler.getTanks(); tank++) {
            capacity += Math.max(0, handler.getTankCapacity(tank));
        }
        return capacity > 0 ? OptionalLong.of(capacity) : OptionalLong.empty();
    }

    @Override
    public OptionalLong getItemFluidAmount(ItemStack stack) {
        IFluidHandlerItem handler = stack.getCapability(Capabilities.FluidHandler.ITEM);
        if (handler == null || handler.getTanks() <= 0) return OptionalLong.empty();
        long amount = 0L;
        for (int tank = 0; tank < handler.getTanks(); tank++) {
            amount += Math.max(0, handler.getFluidInTank(tank).getAmount());
        }
        return amount > 0 ? OptionalLong.of(amount) : OptionalLong.empty();
    }

    @Override
    public OptionalLong getItemHandlerCapacity(ItemStack stack) {
        try {
            IItemHandler handler = stack.getCapability(Capabilities.ItemHandler.ITEM);
            if (handler == null || handler.getSlots() <= 0) return OptionalLong.empty();
            long capacity = itemHandlerCapacity(handler);
            return capacity > 0 ? OptionalLong.of(capacity) : OptionalLong.empty();
        } catch (RuntimeException | LinkageError ignored) {
            return OptionalLong.empty();
        }
    }

    @Override
    public OptionalLong getBlockItemHandlerCapacity(ItemStack stack, Level level) {
        // Avoid constructing arbitrary modded BlockEntity instances during background indexing.
        // Item/component capabilities are still probed above; block capability capacity can be
        // added back through focused compat where it is known to be cheap and side-effect-free.
        return OptionalLong.empty();
    }

    @Override
    public List<ItemAttributeModifier> getMainHandAttackModifiers(ItemStack stack) {
        if (stack == null || stack.isEmpty()) return List.of();
        List<ItemAttributeModifier> result = new ArrayList<>();
        ItemAttributeModifiers modifiers = stack.getAttributeModifiers();
        modifiers.forEach(EquipmentSlot.MAINHAND, (attribute, modifier) -> {
            ItemAttributeKind kind = attackAttributeKind(attribute);
            ItemAttributeOperation operation = attributeOperation(modifier.operation());
            if (kind != null && operation != null) {
                result.add(new ItemAttributeModifier(kind, modifier.amount(), operation));
            }
        });
        return result;
    }

    @Override
    public ItemStack createPotionSubtypeStack(Item potionItem, Object potionHolder) {
        if (!(potionHolder instanceof Holder<?> holder)) return ItemStack.EMPTY;
        ItemStack stack = new ItemStack(potionItem);
        stack.set(DataComponents.POTION_CONTENTS, new PotionContents((Holder) holder));
        return stack;
    }

    @Override
    public ItemStack createEnchantedBookSubtypeStack(Object enchantmentHolder, int level) {
        if (!(enchantmentHolder instanceof Holder<?> holder) || !(holder.value() instanceof Enchantment)) {
            return ItemStack.EMPTY;
        }
        ItemStack stack = new ItemStack(Items.ENCHANTED_BOOK);
        ItemEnchantments.Mutable mutable = new ItemEnchantments.Mutable(ItemEnchantments.EMPTY);
        mutable.set((Holder) holder, level);
        stack.set(DataComponents.STORED_ENCHANTMENTS, mutable.toImmutable());
        return stack;
    }

    @Override
    public List<SubtypeStack> createSuspiciousStewSubtypeStacks() {
        List<SubtypeStack> result = new ArrayList<>();
        BuiltInRegistries.ITEM.getTag(ItemTags.SMALL_FLOWERS)
                .stream()
                .flatMap(HolderSet.ListBacked::stream)
                .map(Holder::value)
                .filter(BlockItem.class::isInstance)
                .map(BlockItem.class::cast)
                .map(BlockItem::getBlock)
                .filter(FlowerBlock.class::isInstance)
                .map(FlowerBlock.class::cast)
                .forEach(flowerBlock -> {
                    SuspiciousStewEffects effects = flowerBlock.getSuspiciousEffects();
                    if (effects.effects().isEmpty()) return;

                    SuspiciousStewEffects.Entry firstEntry = effects.effects().get(0);
                    ResourceLocation effectId = BuiltInRegistries.MOB_EFFECT.getKey(firstEntry.effect().value());
                    if (effectId == null) return;

                    ItemStack stack = new ItemStack(Items.SUSPICIOUS_STEW);
                    stack.set(DataComponents.SUSPICIOUS_STEW_EFFECTS, effects);
                    result.add(new SubtypeStack(effectId, stack));
                });
        return result;
    }

    @Override
    public List<SubtypeStack> createFireworkRocketSubtypeStacks() {
        List<SubtypeStack> result = new ArrayList<>();
        int[] repColors = {0xFF0000, 0x00AA00, 0x5555FF, 0xFFFF55, 0xFFFFFF};
        FireworkExplosion.Shape[] shapes = FireworkExplosion.Shape.values();

        for (int i = 0; i < Math.min(shapes.length, repColors.length); i++) {
            FireworkExplosion.Shape shape = shapes[i];
            FireworkExplosion explosion = new FireworkExplosion(
                    shape, IntList.of(repColors[i]), IntList.of(), false, false);
            ItemStack stack = new ItemStack(Items.FIREWORK_ROCKET);
            stack.set(DataComponents.FIREWORKS, new Fireworks(1, List.of(explosion)));
            result.add(new SubtypeStack(rl("minecraft", shape.name().toLowerCase()), stack));
        }
        return result;
    }

    @Override
    @SuppressWarnings("unchecked")
    public ItemStack createGoatHornSubtypeStack(Object instrumentHolder) {
        if (!(instrumentHolder instanceof Holder<?> holder) || !(holder.value() instanceof Instrument)) {
            return ItemStack.EMPTY;
        }
        Holder<Instrument> instrument = (Holder<Instrument>) holder;
        ItemStack stack = new ItemStack(Items.GOAT_HORN);
        stack.set(DataComponents.INSTRUMENT, instrument);
        return stack;
    }

    @Override
    public ItemStack createPlayerHeadStack(String name) {
        return createPlayerHeadStack(name, null);
    }

    @Override
    public ItemStack createPlayerHeadStack(String name, UUID uuid) {
        if (name == null || name.isBlank()) return ItemStack.EMPTY;
        ItemStack stack = new ItemStack(Items.PLAYER_HEAD);
        stack.set(DataComponents.PROFILE, new ResolvableProfile(
                Optional.of(name),
                uuid == null ? Optional.empty() : Optional.of(uuid),
                new com.mojang.authlib.properties.PropertyMap()));
        return stack;
    }

    @Override
    public ItemStack createPlayerHeadStack(GameProfile profile) {
        if (profile == null || profile.getName() == null || profile.getName().isBlank()) return ItemStack.EMPTY;
        ItemStack stack = new ItemStack(Items.PLAYER_HEAD);
        stack.set(DataComponents.PROFILE, new ResolvableProfile(
                Optional.of(profile.getName()),
                profile.getId() == null ? Optional.empty() : Optional.of(profile.getId()),
                profile.getProperties()));
        return stack;
    }

    @Override
    public String playerHeadGiveCommand(String name) {
        if (name == null || name.isBlank()) {
            return "give @s minecraft:player_head 1";
        }
        return "give @s minecraft:player_head[minecraft:profile={name:\"" + escapeCommandString(name) + "\"}] 1";
    }

    @Override
    public ResourceLocation getSpawnEggEntityTypeId(SpawnEggItem egg, ItemStack stack) {
        EntityType<?> entityType = egg.getType(stack);
        return entityType == null ? null : BuiltInRegistries.ENTITY_TYPE.getKey(entityType);
    }

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

    @Override
    public boolean tryLoadGlobalIndexCache() {
        return GlobalIndexCache.tryLoad();
    }

    @Override
    public void saveGlobalIndexCache() {
        GlobalIndexCache.save();
    }

    @Override
    public <T> List<T> discoverAnnotatedPlugins(Class<?> annotationClass, Class<T> pluginClass) {
        List<T> result = new ArrayList<>();
        if (ModList.get() == null) return result;
        var annotationType = Type.getType(annotationClass);
        for (var scanData : ModList.get().getAllScanData()) {
            for (var annotation : scanData.getAnnotations()) {
                if (!Objects.equals(annotation.annotationType(), annotationType)) continue;
                String className = annotation.memberName();
                try {
                    Class<?> cls = Class.forName(className, false, NeoForgePlatformHelper.class.getClassLoader());
                    if (!pluginClass.isAssignableFrom(cls)) continue;
                    @SuppressWarnings("unchecked")
                    T plugin = (T) cls.getDeclaredConstructor().newInstance();
                    result.add(plugin);
                } catch (Throwable t) {
                    com.sanhiruzu.ami.AmiCore.LOGGER.warn("Failed to instantiate @{} plugin class {}", annotationClass.getSimpleName(), className, t);
                }
            }
        }
        return result;
    }
}
