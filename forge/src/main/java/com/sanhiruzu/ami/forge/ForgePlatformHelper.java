package com.sanhiruzu.ami.forge;

import com.sanhiruzu.ami.forge.client.AMIKeyMappings;
import com.sanhiruzu.ami.index.GlobalIndexCache;
import com.sanhiruzu.ami.platform.IAmiKeyMappings;
import com.sanhiruzu.ami.platform.IPlatformHelper;
import com.sanhiruzu.ami.recipe.AmiRecipeIndex;
import com.sanhiruzu.ami.util.AmiRecipeHolder;
import net.minecraft.client.KeyMapping;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.*;
import net.minecraft.world.item.alchemy.Potion;
import net.minecraft.world.item.alchemy.PotionUtils;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentInstance;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SuspiciousEffectHolder;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.energy.IEnergyStorage;
import net.minecraftforge.fluids.capability.IFluidHandlerItem;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.loading.FMLLoader;
import net.minecraftforge.fml.loading.FMLPaths;
import net.minecraftforge.items.IItemHandler;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.OptionalLong;

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

    @Override
    public boolean isClient() {
        return FMLLoader.getDist().isClient();
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
        return new ResourceLocation(namespace, path);
    }

    @Override
    public IAmiKeyMappings keyMappings() {
        return KEY_MAPPINGS;
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
        IItemHandler handler = stack.getCapability(ForgeCapabilities.ITEM_HANDLER).orElse(null);
        if (handler == null || handler.getSlots() <= 0) return OptionalLong.empty();
        long capacity = 0L;
        for (int slot = 0; slot < handler.getSlots(); slot++) {
            capacity += Math.max(0, handler.getSlotLimit(slot));
        }
        return capacity > 0 ? OptionalLong.of(capacity) : OptionalLong.empty();
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
        for (Block block : BuiltInRegistries.BLOCK) {
            SuspiciousEffectHolder holder = SuspiciousEffectHolder.tryGet(block);
            if (holder == null) continue;

            MobEffect effect = holder.getSuspiciousEffect();
            ResourceLocation effectId = BuiltInRegistries.MOB_EFFECT.getKey(effect);
            if (effectId == null) continue;

            ItemStack stack = new ItemStack(Items.SUSPICIOUS_STEW);
            CompoundTag tag = stack.getOrCreateTag();
            ListTag list = new ListTag();
            CompoundTag effectTag = new CompoundTag();
            effectTag.putInt("EffectId", MobEffect.getId(effect));
            effectTag.putInt("EffectDuration", holder.getEffectDuration());
            list.add(effectTag);
            tag.put("Effects", list);
            result.add(new SubtypeStack(effectId, stack));
        }
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
    public List<AmiRecipeHolder<?>> getUsesFor(ItemStack target) {
        return AmiRecipeIndex.getInstance().getUsesFor(target);
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
