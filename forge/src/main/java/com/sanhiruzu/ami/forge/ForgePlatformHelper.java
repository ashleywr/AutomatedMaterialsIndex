package com.sanhiruzu.ami.forge;

import com.mojang.authlib.GameProfile;
import com.mojang.authlib.properties.Property;
import com.mojang.authlib.properties.PropertyMap;
import com.mojang.blaze3d.platform.InputConstants;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.sanhiruzu.ami.forge.client.AMIKeyMappings;
import com.sanhiruzu.ami.index.GlobalIndexCache;
import com.sanhiruzu.ami.platform.IAmiKeyMappings;
import com.sanhiruzu.ami.platform.IPlatformHelper;
import com.sanhiruzu.ami.recipe.AmiRecipeIndex;
import com.sanhiruzu.ami.util.AmiRecipeHolder;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderSet;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.inventory.tooltip.TooltipComponent;
import net.minecraft.world.item.*;
import net.minecraft.world.item.alchemy.Potion;
import net.minecraft.world.item.alchemy.PotionUtils;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentInstance;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.FlowerBlock;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.energy.IEnergyStorage;
import net.minecraftforge.fluids.capability.IFluidHandlerItem;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.loading.FMLEnvironment;
import net.minecraftforge.fml.loading.FMLLoader;
import net.minecraftforge.fml.loading.FMLPaths;
import net.minecraftforge.items.IItemHandler;

import java.nio.file.Path;
import java.util.*;

public class ForgePlatformHelper implements IPlatformHelper {
    private static final int SHULKER_SLOTS = 27;

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

    private static ItemAttributeKind attackAttributeKind(Attribute attribute) {
        if (attribute == Attributes.ATTACK_DAMAGE) return ItemAttributeKind.ATTACK_DAMAGE;
        if (attribute == Attributes.ATTACK_SPEED) return ItemAttributeKind.ATTACK_SPEED;
        return null;
    }

    private static ItemAttributeOperation attributeOperation(AttributeModifier.Operation operation) {
        if (operation == AttributeModifier.Operation.ADDITION) return ItemAttributeOperation.ADD_VALUE;
        if (operation == AttributeModifier.Operation.MULTIPLY_BASE) return ItemAttributeOperation.ADD_MULTIPLIED_BASE;
        if (operation == AttributeModifier.Operation.MULTIPLY_TOTAL) return ItemAttributeOperation.ADD_MULTIPLIED_TOTAL;
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

    private static int[] uuidToIntArray(UUID uuid) {
        long most = uuid.getMostSignificantBits();
        long least = uuid.getLeastSignificantBits();
        return new int[]{(int) (most >> 32), (int) most, (int) (least >> 32), (int) least};
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
        g.fill(x, thumbY, x + width, thumbY + thumbHeight, -8355712);
        g.fill(x, thumbY, x + width - 1, thumbY + thumbHeight - 1, -4144960);
    }

    @Override
    public Object beginGuiQuadBatch(boolean textured) {
        BufferBuilder builder = Tesselator.getInstance().getBuilder();
        builder.begin(VertexFormat.Mode.QUADS,
                textured ? DefaultVertexFormat.POSITION_TEX_COLOR : DefaultVertexFormat.POSITION_COLOR);
        return builder;
    }

    @Override
    public void guiQuadVertex(Object buffer, org.joml.Matrix4f matrix, float x, float y, float u, float v,
                              float r, float g, float b, float a, boolean textured) {
        VertexConsumer vertex = ((BufferBuilder) buffer).vertex(matrix, x, y, 0.0f);
        if (textured) {
            vertex = vertex.uv(u, v);
        }
        vertex.color(r, g, b, a).endVertex();
    }

    @Override
    public void endAndDrawGuiQuadBatch(Object buffer) {
        BufferUploader.drawWithShader(((BufferBuilder) buffer).end());
    }

    @Override
    public Optional<Integer> getItemEnergyCapacity(ItemStack stack) {
        IEnergyStorage energyStorage = stack.getCapability(ForgeCapabilities.ENERGY).orElse(null);
        if (energyStorage == null) return Optional.empty();
        int capacity = energyStorage.getMaxEnergyStored();
        return capacity > 0 ? Optional.of(capacity) : Optional.empty();
    }

    @Override
    public Optional<Integer> getItemEnergyStored(ItemStack stack) {
        IEnergyStorage energyStorage = stack.getCapability(ForgeCapabilities.ENERGY).orElse(null);
        if (energyStorage == null) return Optional.empty();
        int stored = energyStorage.getEnergyStored();
        return stored > 0 ? Optional.of(stored) : Optional.empty();
    }

    @Override
    public net.minecraft.network.chat.Component getFluidDisplayName(net.minecraft.world.level.material.Fluid fluid) {
        return new net.minecraftforge.fluids.FluidStack(fluid, 1000).getDisplayName();
    }

    @Override
    public net.minecraft.resources.ResourceLocation getFluidStillTexture(net.minecraft.world.level.material.Fluid fluid) {
        try {
            return net.minecraftforge.client.extensions.common.IClientFluidTypeExtensions.of(fluid).getStillTexture();
        } catch (Exception e) {
            return null;
        }
    }

    @Override
    public int getFluidTintColor(net.minecraft.world.level.material.Fluid fluid) {
        try {
            return net.minecraftforge.client.extensions.common.IClientFluidTypeExtensions.of(fluid).getTintColor();
        } catch (Exception e) {
            return 0xFFFFFFFF;
        }
    }

    @Override
    public OptionalLong getItemFluidCapacity(ItemStack stack) {
        IFluidHandlerItem handler = stack.getCapability(ForgeCapabilities.FLUID_HANDLER_ITEM).orElse(null);
        if (handler == null || handler.getTanks() <= 0) return OptionalLong.empty();
        long capacity = 0L;
        for (int tank = 0; tank < handler.getTanks(); tank++) {
            capacity += Math.max(0, handler.getTankCapacity(tank));
        }
        return capacity > 0 ? OptionalLong.of(capacity) : OptionalLong.empty();
    }

    @Override
    public OptionalLong getItemFluidAmount(ItemStack stack) {
        IFluidHandlerItem handler = stack.getCapability(ForgeCapabilities.FLUID_HANDLER_ITEM).orElse(null);
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
            IItemHandler handler = stack.getCapability(ForgeCapabilities.ITEM_HANDLER).orElse(null);
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
        // Several Forge mods lazily transform capability classes from a thread context where
        // Forge's eventbus class hierarchy scanner cannot resolve their parent chain.
        return OptionalLong.empty();
    }

    @Override
    public List<ItemAttributeModifier> getMainHandAttackModifiers(ItemStack stack) {
        if (stack == null || stack.isEmpty()) return List.of();
        List<ItemAttributeModifier> result = new ArrayList<>();
        stack.getAttributeModifiers(EquipmentSlot.MAINHAND).forEach((attribute, modifier) -> {
            ItemAttributeKind kind = attackAttributeKind(attribute);
            ItemAttributeOperation operation = attributeOperation(modifier.getOperation());
            if (kind != null && operation != null) {
                result.add(new ItemAttributeModifier(kind, modifier.getAmount(), operation));
            }
        });
        return result;
    }

    @Override
    @SuppressWarnings("unchecked")
    public ItemStack createPotionSubtypeStack(Item potionItem, Object potionHolder) {
        if (!(potionHolder instanceof Holder<?> holder) || !(holder.value() instanceof Potion potion)) {
            return ItemStack.EMPTY;
        }
        ItemStack stack = new ItemStack(potionItem);
        PotionUtils.setPotion(stack, potion);
        return stack;
    }

    @Override
    @SuppressWarnings("unchecked")
    public ItemStack createEnchantedBookSubtypeStack(Object enchantmentHolder, int level) {
        if (!(enchantmentHolder instanceof Holder<?> holder) || !(holder.value() instanceof Enchantment enchantment)) {
            return ItemStack.EMPTY;
        }
        ItemStack stack = new ItemStack(Items.ENCHANTED_BOOK);
        EnchantedBookItem.addEnchantment(stack, new EnchantmentInstance(enchantment, level));
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
                    MobEffect effect = flowerBlock.getSuspiciousEffect();
                    ResourceLocation effectId = BuiltInRegistries.MOB_EFFECT.getKey(effect);
                    if (effectId == null) return;

                    ItemStack stack = new ItemStack(Items.SUSPICIOUS_STEW);
                    CompoundTag tag = stack.getOrCreateTag();
                    ListTag list = new ListTag();
                    CompoundTag effectTag = new CompoundTag();
                    effectTag.putInt("EffectId", MobEffect.getId(effect));
                    effectTag.putInt("EffectDuration", flowerBlock.getEffectDuration());
                    list.add(effectTag);
                    tag.put("Effects", list);
                    result.add(new SubtypeStack(effectId, stack));
                });
        return result;
    }

    @Override
    public List<SubtypeStack> createFireworkRocketSubtypeStacks() {
        List<SubtypeStack> result = new ArrayList<>();
        int[] repColors = {0xFF0000, 0x00AA00, 0x5555FF, 0xFFFF55, 0xFFFFFF};
        String[] shapeNames = {"small_ball", "large_ball", "star", "creeper", "burst"};

        for (int i = 0; i < Math.min(shapeNames.length, repColors.length); i++) {
            ItemStack stack = new ItemStack(Items.FIREWORK_ROCKET);
            CompoundTag fireworksTag = stack.getOrCreateTagElement("Fireworks");
            ListTag explosionsList = new ListTag();
            CompoundTag explosionTag = new CompoundTag();
            explosionTag.putByte("Type", (byte) i);
            explosionTag.putIntArray("Colors", new int[]{repColors[i]});
            explosionsList.add(explosionTag);
            fireworksTag.put("Explosions", explosionsList);
            fireworksTag.putByte("Flight", (byte) 1);
            result.add(new SubtypeStack(rl("minecraft", shapeNames[i]), stack));
        }
        return result;
    }

    @Override
    public ItemStack createGoatHornSubtypeStack(Object instrumentHolder) {
        if (!(instrumentHolder instanceof Holder<?> holder) || !(holder.value() instanceof Instrument)) {
            return ItemStack.EMPTY;
        }
        ItemStack stack = new ItemStack(Items.GOAT_HORN);
        stack.getOrCreateTag().putString("instrument", holder.unwrapKey()
                .map(key -> key.location().toString())
                .orElse(""));
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
        if (uuid == null) {
            stack.getOrCreateTag().putString("SkullOwner", name);
            return stack;
        }
        CompoundTag skullOwner = new CompoundTag();
        skullOwner.putString("Name", name);
        skullOwner.putIntArray("Id", uuidToIntArray(uuid));
        stack.getOrCreateTag().put("SkullOwner", skullOwner);
        return stack;
    }

    @Override
    public ItemStack createPlayerHeadStack(GameProfile profile) {
        if (profile == null || profile.getName() == null || profile.getName().isBlank()) return ItemStack.EMPTY;
        ItemStack stack = createPlayerHeadStack(profile.getName(), profile.getId());
        CompoundTag skullOwner = stack.getOrCreateTag().getCompound("SkullOwner");
        PropertyMap properties = profile.getProperties();
        if (!properties.isEmpty()) {
            CompoundTag propsTag = new CompoundTag();
            for (String key : properties.keySet()) {
                ListTag propList = new ListTag();
                for (Property property : properties.get(key)) {
                    CompoundTag entry = new CompoundTag();
                    entry.putString("Value", property.getValue());
                    if (property.getSignature() != null) {
                        entry.putString("Signature", property.getSignature());
                    }
                    propList.add(entry);
                }
                propsTag.put(key, propList);
            }
            skullOwner.put("Properties", propsTag);
            stack.getOrCreateTag().put("SkullOwner", skullOwner);
        }
        return stack;
    }

    @Override
    public String playerHeadGiveCommand(String name) {
        if (name == null || name.isBlank()) {
            return "give @s minecraft:player_head 1";
        }
        return "give @s minecraft:player_head{SkullOwner:\"" + escapeCommandString(name) + "\"} 1";
    }

    @Override
    public ResourceLocation getSpawnEggEntityTypeId(SpawnEggItem egg, ItemStack stack) {
        EntityType<?> entityType = egg.getType(stack.getTag());
        return entityType == null ? null : BuiltInRegistries.ENTITY_TYPE.getKey(entityType);
    }

    @Override
    public OptionalLong getContainerComponentCapacity(ItemStack stack, long defaultStackSize) {
        OptionalLong componentCapacity = IPlatformHelper.super.getContainerComponentCapacity(stack, defaultStackSize);
        if (componentCapacity.isPresent()) return componentCapacity;

        net.minecraft.nbt.CompoundTag tag = stack.getTagElement("BlockEntityTag");
        if (tag == null || !tag.contains("Items", 9)) return OptionalLong.empty();
        return OptionalLong.of(SHULKER_SLOTS * defaultStackSize);
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
}
