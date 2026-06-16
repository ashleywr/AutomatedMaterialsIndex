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
import net.minecraft.resources.Identifier;
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
    private static final Set<String> CREATIVE_STACK_PARITY_ITEMS = Set.of(
            "enchanted_book",
            "suspicious_stew",
            "firework_rocket"
    );

    private SubtypeExpander() {
    }

    public static List<SubtypeEntry> expand(Identifier baseId,
                                            RegistryAccess registryAccess) {
        if (!baseId.getNamespace().equals("minecraft")) return List.of();

        return switch (baseId.getPath()) {
            case "potion" -> expandPotions(Items.POTION, "potion", registryAccess);
            case "splash_potion" -> expandPotions(Items.SPLASH_POTION, "splash_potion", registryAccess);
            case "lingering_potion" -> expandPotions(Items.LINGERING_POTION, "lingering_potion", registryAccess);
            case "tipped_arrow" -> expandPotions(Items.TIPPED_ARROW, "tipped_arrow", registryAccess);
            case "enchanted_book" -> expandEnchantedBooks(registryAccess);
            case "suspicious_stew" -> expandSuspiciousStew();
            case "firework_rocket" -> expandFireworkRockets();
            case "goat_horn" -> expandGoatHorns(registryAccess);
            case "spawn_egg" -> expandSpawnEggs();
            default -> List.of();
        };
    }

    private static List<SubtypeEntry> expandPotions(Item potionItem, String itemPath, RegistryAccess registryAccess) {
        if (registryAccess == null) return List.of();
        List<SubtypeEntry> result = new ArrayList<>();
        registryAccess.lookup(Registries.POTION).ifPresent(potionReg ->
            potionReg.listElements().forEach(potionRef -> {
                if (isAtCap(result, itemPath)) return;

                Identifier potionId = potionRef.key().identifier();
                if (shouldSkipPotionSubtype(potionId)) return;

                ItemStack stack = Services.PLATFORM.createPotionSubtypeStack(potionItem, potionRef);
                if (stack.isEmpty()) return;

                Identifier syntheticId = syntheticId(itemPath, potionId.getNamespace(), potionId.getPath());
                java.util.Map<String, String> extra = java.util.Map.of(SearchNodeKeys.POTION_EFFECT, potionId.toString());
                result.add(new SubtypeEntry(syntheticId, stack, stack.getHoverName().getString(), extra));
            })
        );
        return result;
    }

    static boolean shouldSkipPotionSubtype(Identifier potionId) {
        return potionId == null || "empty".equals(potionId.getPath());
    }

    private static List<SubtypeEntry> expandEnchantedBooks(RegistryAccess registryAccess) {
        if (registryAccess == null) return List.of();
        var enchantmentRegistry = registryAccess.lookup(Registries.ENCHANTMENT).orElse(null);
        if (enchantmentRegistry == null) return List.of();

        List<SubtypeEntry> result = new ArrayList<>();
        for (Holder.Reference<Enchantment> enchantmentRef : enchantmentRegistry.listElements().toList()) {
            Enchantment enchantment = enchantmentRef.value();
            Identifier enchantmentId = enchantmentRef.key().identifier();
            int maxLevel = enchantment.getMaxLevel();

            for (int level = 1; level <= maxLevel; level++) {
                if (isAtCap(result, "enchanted_book")) return result;

                ItemStack stack = Services.PLATFORM.createEnchantedBookSubtypeStack(enchantmentRef, level);
                if (stack.isEmpty()) continue;

                String suffix = maxLevel > 1 ? "_" + level : "";
                Identifier syntheticId = syntheticId("enchanted_book",
                        enchantmentId.getNamespace(), enchantmentId.getPath() + suffix);
                result.add(new SubtypeEntry(syntheticId, stack, stack.getHoverName().getString()));
            }
        }
        return result;
    }

    private static List<SubtypeEntry> expandSuspiciousStew() {
        List<SubtypeEntry> result = new ArrayList<>();
        Set<Identifier> seenEffectIds = new HashSet<>();

        for (SubtypeStack subtype : Services.PLATFORM.createSuspiciousStewSubtypeStacks()) {
            Identifier effectId = subtype.subtypeId();
            if (effectId == null || !seenEffectIds.add(effectId)) continue;
            if (isAtCap(result, "suspicious_stew")) return result;

            Identifier syntheticId = syntheticId("suspicious_stew", effectId.getNamespace(), effectId.getPath());
            result.add(new SubtypeEntry(syntheticId, subtype.stack(), subtype.stack().getHoverName().getString()));
        }
        return result;
    }

    private static List<SubtypeEntry> expandFireworkRockets() {
        List<SubtypeEntry> result = new ArrayList<>();

        for (SubtypeStack subtype : Services.PLATFORM.createFireworkRocketSubtypeStacks()) {
            Identifier shapeId = subtype.subtypeId();
            if (shapeId == null) continue;
            if (isAtCap(result, "firework_rocket")) return result;

            String shapeName = shapeId.getPath();
            Identifier syntheticId = syntheticId("firework_rocket", shapeId.getNamespace(), shapeName);
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
        var instrumentRegistry = registryAccess.lookup(Registries.INSTRUMENT).orElse(null);
        if (instrumentRegistry == null) return List.of();

        List<SubtypeEntry> result = new ArrayList<>();
        for (Holder.Reference<Instrument> instrumentRef : instrumentRegistry.listElements().toList()) {
            if (isAtCap(result, "goat_horn")) return result;

            ItemStack stack = Services.PLATFORM.createGoatHornSubtypeStack(instrumentRef);
            if (stack.isEmpty()) continue;

            Identifier instrumentId = instrumentRef.key().identifier();
            Identifier syntheticId = syntheticId("goat_horn", instrumentId.getNamespace(), instrumentId.getPath());
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
            Identifier entityId = Services.PLATFORM.getSpawnEggEntityTypeId(egg, stack);
            if (entityId == null) continue;

            Identifier syntheticId = syntheticId("spawn_egg", entityId.getNamespace(), entityId.getPath());
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

    public static boolean shouldPreferCreativeStackParity(Identifier baseId) {
        return baseId != null
                && "minecraft".equals(baseId.getNamespace())
                && CREATIVE_STACK_PARITY_ITEMS.contains(baseId.getPath());
    }

    private static Identifier syntheticId(String itemPath, String subNs, String subPath) {
        return Services.PLATFORM.rl("minecraft", itemPath + "/" + subNs + "/" + subPath);
    }

    public record SubtypeEntry(Identifier id, ItemStack stack, String displayName,
                               java.util.Map<String, String> extraMeta) {
        public SubtypeEntry(Identifier id, ItemStack stack, String displayName) {
            this(id, stack, displayName, java.util.Map.of());
        }
    }
}
