package com.sanhiruzu.ami.neoforge;

import com.sanhiruzu.ami.index.GlobalIndexCache;
import com.sanhiruzu.ami.neoforge.client.AMIKeyMappings;
import com.sanhiruzu.ami.platform.IAmiKeyMappings;
import com.sanhiruzu.ami.platform.IPlatformHelper;
import com.sanhiruzu.ami.recipe.AmiRecipeIndex;
import com.sanhiruzu.ami.util.AmiRecipeHolder;
import it.unimi.dsi.fastutil.ints.IntList;
import net.minecraft.client.KeyMapping;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.*;
import net.minecraft.world.item.alchemy.PotionContents;
import net.minecraft.world.item.component.FireworkExplosion;
import net.minecraft.world.item.component.Fireworks;
import net.minecraft.world.item.component.ItemAttributeModifiers;
import net.minecraft.world.item.component.SuspiciousStewEffects;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.ItemEnchantments;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.SuspiciousEffectHolder;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.fml.ModList;
import net.neoforged.fml.loading.FMLLoader;
import net.neoforged.fml.loading.FMLPaths;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.energy.IEnergyStorage;
import net.neoforged.neoforge.fluids.capability.IFluidHandlerItem;
import net.neoforged.neoforge.items.IItemHandler;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.OptionalLong;

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

    @Override
    public boolean isClient() {
        return FMLLoader.getDist().isClient();
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
        if (stack == null || stack.isEmpty() || level == null || !(stack.getItem() instanceof BlockItem blockItem)) {
            return OptionalLong.empty();
        }
        Block block = blockItem.getBlock();
        if (!(block instanceof EntityBlock entityBlock)) {
            return OptionalLong.empty();
        }
        try {
            BlockPos pos = BlockPos.ZERO;
            BlockState state = block.defaultBlockState();
            BlockEntity blockEntity = entityBlock.newBlockEntity(pos, state);
            if (blockEntity == null) return OptionalLong.empty();
            blockEntity.setLevel(level);

            long capacity = 0L;
            IItemHandler unsided = level.getCapability(Capabilities.ItemHandler.BLOCK, pos, state, blockEntity, null);
            capacity = Math.max(capacity, itemHandlerCapacity(unsided));
            for (Direction direction : Direction.values()) {
                IItemHandler sided = level.getCapability(Capabilities.ItemHandler.BLOCK, pos, state, blockEntity, direction);
                capacity = Math.max(capacity, itemHandlerCapacity(sided));
            }
            return capacity > 0 ? OptionalLong.of(capacity) : OptionalLong.empty();
        } catch (RuntimeException | LinkageError ignored) {
            return OptionalLong.empty();
        }
    }

    private static long itemHandlerCapacity(IItemHandler handler) {
        if (handler == null || handler.getSlots() <= 0) return 0L;
        long capacity = 0L;
        for (int slot = 0; slot < handler.getSlots(); slot++) {
            capacity += Math.max(0, handler.getSlotLimit(slot));
        }
        return capacity;
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
        for (Block block : BuiltInRegistries.BLOCK) {
            SuspiciousEffectHolder holder = SuspiciousEffectHolder.tryGet(block);
            if (holder == null) continue;

            SuspiciousStewEffects effects = holder.getSuspiciousEffects();
            if (effects.effects().isEmpty()) continue;

            SuspiciousStewEffects.Entry firstEntry = effects.effects().get(0);
            ResourceLocation effectId = BuiltInRegistries.MOB_EFFECT.getKey(firstEntry.effect().value());
            if (effectId == null) continue;

            ItemStack stack = new ItemStack(Items.SUSPICIOUS_STEW);
            stack.set(DataComponents.SUSPICIOUS_STEW_EFFECTS, effects);
            result.add(new SubtypeStack(effectId, stack));
        }
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
