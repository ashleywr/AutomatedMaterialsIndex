package com.sanhiruzu.ami.index.providers;

import com.sanhiruzu.ami.AmiCore;
import com.sanhiruzu.ami.index.SearchNodeKeys;
import com.sanhiruzu.ami.platform.IPlatformHelper.SubtypeStack;
import com.sanhiruzu.ami.platform.Services;
import net.minecraft.core.Holder;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.*;
import net.minecraft.world.item.alchemy.Potion;
import net.minecraft.world.item.enchantment.Enchantment;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Generates representative ItemStack variants for items whose single registry entry
 * covers many visual or functional subtypes.
 */
public final class SubtypeExpander {

    public static final int HARD_CAP = 150;
    static final int ENCHANTED_BOOK_CAP = 600;

    private SubtypeExpander() {
    }

    public static List<SubtypeEntry> expand(ResourceLocation baseId,
                                            RegistryAccess registryAccess) {
        if (!baseId.getNamespace().equals("minecraft")) return List.of();

        return switch (baseId.getPath()) {
            case "potion" -> expandPotions(Items.POTION, "potion");
            case "splash_potion" -> expandPotions(Items.SPLASH_POTION, "splash_potion");
            case "lingering_potion" -> expandPotions(Items.LINGERING_POTION, "lingering_potion");
            case "tipped_arrow" -> expandPotions(Items.TIPPED_ARROW, "tipped_arrow");
            case "enchanted_book" -> expandEnchantedBooks(registryAccess);
            case "suspicious_stew" -> expandSuspiciousStew();
            case "firework_rocket" -> expandFireworkRockets();
            case "goat_horn" -> expandGoatHorns(registryAccess);
            case "spawn_egg" -> expandSpawnEggs();
            default -> List.of();
        };
    }

    private static List<SubtypeEntry> expandPotions(Item potionItem, String itemPath) {
        List<SubtypeEntry> result = new ArrayList<>();
        for (Holder.Reference<Potion> potionRef : BuiltInRegistries.POTION.holders().toList()) {
            if (isAtCap(result, itemPath)) return result;

            ResourceLocation potionId = potionRef.key().location();
            if (shouldSkipPotionSubtype(potionId)) continue;

            ItemStack stack = Services.PLATFORM.createPotionSubtypeStack(potionItem, potionRef);
            if (stack.isEmpty()) continue;

            ResourceLocation syntheticId = syntheticId(itemPath, potionId.getNamespace(), potionId.getPath());
            java.util.Map<String, String> extra = java.util.Map.of(SearchNodeKeys.POTION_EFFECT, potionId.toString());
            result.add(new SubtypeEntry(syntheticId, stack, stack.getHoverName().getString(), extra));
        }
        return result;
    }

    static boolean shouldSkipPotionSubtype(ResourceLocation potionId) {
        return potionId == null || "empty".equals(potionId.getPath());
    }

    private static List<SubtypeEntry> expandEnchantedBooks(RegistryAccess registryAccess) {
        if (registryAccess == null) return List.of();
        var enchantmentRegistry = registryAccess.registry(Registries.ENCHANTMENT).orElse(null);
        if (enchantmentRegistry == null) return List.of();

        List<SubtypeEntry> result = new ArrayList<>();
        for (Holder.Reference<Enchantment> enchantmentRef : enchantmentRegistry.holders().toList()) {
            Enchantment enchantment = enchantmentRef.value();
            ResourceLocation enchantmentId = enchantmentRef.key().location();
            int maxLevel = enchantment.getMaxLevel();

            for (int level = 1; level <= maxLevel; level++) {
                if (isAtCap(result, "enchanted_book")) return result;

                ItemStack stack = Services.PLATFORM.createEnchantedBookSubtypeStack(enchantmentRef, level);
                if (stack.isEmpty()) continue;

                String suffix = maxLevel > 1 ? "_" + level : "";
                ResourceLocation syntheticId = syntheticId("enchanted_book",
                        enchantmentId.getNamespace(), enchantmentId.getPath() + suffix);
                result.add(new SubtypeEntry(syntheticId, stack, stack.getHoverName().getString()));
            }
        }
        return result;
    }

    private static List<SubtypeEntry> expandSuspiciousStew() {
        List<SubtypeEntry> result = new ArrayList<>();
        Set<ResourceLocation> seenEffectIds = new HashSet<>();

        for (SubtypeStack subtype : Services.PLATFORM.createSuspiciousStewSubtypeStacks()) {
            ResourceLocation effectId = subtype.subtypeId();
            if (effectId == null || !seenEffectIds.add(effectId)) continue;
            if (isAtCap(result, "suspicious_stew")) return result;

            ResourceLocation syntheticId = syntheticId("suspicious_stew", effectId.getNamespace(), effectId.getPath());
            result.add(new SubtypeEntry(syntheticId, subtype.stack(), subtype.stack().getHoverName().getString()));
        }
        return result;
    }

    private static List<SubtypeEntry> expandFireworkRockets() {
        List<SubtypeEntry> result = new ArrayList<>();

        for (SubtypeStack subtype : Services.PLATFORM.createFireworkRocketSubtypeStacks()) {
            ResourceLocation shapeId = subtype.subtypeId();
            if (shapeId == null) continue;
            if (isAtCap(result, "firework_rocket")) return result;

            String shapeName = shapeId.getPath();
            ResourceLocation syntheticId = syntheticId("firework_rocket", shapeId.getNamespace(), shapeName);
            result.add(new SubtypeEntry(syntheticId, subtype.stack(),
                    Component.translatable("ami.subtype.firework_rocket", friendlyShape(shapeName)).getString()));
        }
        return result;
    }

    private static String friendlyShape(String shapeName) {
        return Component.translatable("ami.subtype.shape." + shapeName).getString();
    }

    private static List<SubtypeEntry> expandGoatHorns(RegistryAccess registryAccess) {
        if (registryAccess == null) return List.of();
        var instrumentRegistry = registryAccess.registry(Registries.INSTRUMENT).orElse(null);
        if (instrumentRegistry == null) return List.of();

        List<SubtypeEntry> result = new ArrayList<>();
        for (Holder.Reference<Instrument> instrumentRef : instrumentRegistry.holders().toList()) {
            if (isAtCap(result, "goat_horn")) return result;

            ItemStack stack = Services.PLATFORM.createGoatHornSubtypeStack(instrumentRef);
            if (stack.isEmpty()) continue;

            ResourceLocation instrumentId = instrumentRef.key().location();
            ResourceLocation syntheticId = syntheticId("goat_horn", instrumentId.getNamespace(), instrumentId.getPath());
            result.add(new SubtypeEntry(syntheticId, stack, stack.getHoverName().getString()));
        }
        return result;
    }

    private static List<SubtypeEntry> expandSpawnEggs() {
        List<SubtypeEntry> result = new ArrayList<>();
        for (Item item : BuiltInRegistries.ITEM) {
            if (!(item instanceof SpawnEggItem egg)) continue;
            if (isAtCap(result, "spawn_egg")) return result;

            ItemStack stack = new ItemStack(egg);
            ResourceLocation entityId = Services.PLATFORM.getSpawnEggEntityTypeId(egg, stack);
            if (entityId == null) continue;

            ResourceLocation syntheticId = syntheticId("spawn_egg", entityId.getNamespace(), entityId.getPath());
            result.add(new SubtypeEntry(syntheticId, stack, stack.getHoverName().getString()));
        }
        return result;
    }

    private static boolean isAtCap(List<SubtypeEntry> result, String itemPath) {
        int cap = capFor(itemPath);
        if (result.size() < cap) return false;
        AmiCore.LOGGER.debug("SubtypeExpander: hit HARD_CAP for {}; truncating expansion to {} entries.",
                itemPath, cap);
        return true;
    }

    static int capFor(String itemPath) {
        return "enchanted_book".equals(itemPath) ? ENCHANTED_BOOK_CAP : HARD_CAP;
    }

    private static ResourceLocation syntheticId(String itemPath, String subNs, String subPath) {
        return Services.PLATFORM.rl("minecraft", itemPath + "/" + subNs + "/" + subPath);
    }

    public record SubtypeEntry(ResourceLocation id, ItemStack stack, String displayName,
                               java.util.Map<String, String> extraMeta) {
        public SubtypeEntry(ResourceLocation id, ItemStack stack, String displayName) {
            this(id, stack, displayName, java.util.Map.of());
        }
    }
}
