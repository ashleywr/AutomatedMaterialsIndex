package com.sanhiruzu.ami.index.providers;

import com.sanhiruzu.ami.AMI;
import net.minecraft.core.Holder;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.*;
import net.minecraft.world.item.alchemy.Potion;
import net.minecraft.world.item.alchemy.PotionContents;
import net.minecraft.world.item.component.FireworkExplosion;
import net.minecraft.world.item.component.Fireworks;
import net.minecraft.world.item.component.SuspiciousStewEffects;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.ItemEnchantments;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SuspiciousEffectHolder;
import it.unimi.dsi.fastutil.ints.IntList;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Generates representative ItemStack variants for items whose single registry entry
 * covers many visual/functional subtypes (potions, enchanted books, etc.).
 *
 * Each item gets at most HARD_CAP variants. If generation would exceed this, we stop
 * early and log a warning — this prevents poorly-coded mods from producing thousands
 * of entries that crash the indexer.
 */
public final class SubtypeExpander {

    public static final int HARD_CAP = 150;

    private SubtypeExpander() {}

    /**
     * Returned from each expansion: a synthetic ResourceLocation (safe to use as a
     * SearchNode id), the pre-built ItemStack to render, and the display name.
     */
    public record SubtypeEntry(ResourceLocation id, ItemStack stack, String displayName) {}

    /**
     * Expand {@code item} into its visual subtypes.
     *
     * @param item          the item from the registry
     * @param baseId        its registry key (e.g. {@code minecraft:potion})
     * @param registryAccess live registry access for data-driven registries (enchantments, instruments)
     * @return an ordered list of subtypes, or an empty list if no expansion is defined
     */
    public static List<SubtypeEntry> expand(ResourceLocation baseId,
                                            RegistryAccess registryAccess) {
        if (!baseId.getNamespace().equals("minecraft")) return List.of();

        return switch (baseId.getPath()) {
            case "potion"          -> expandPotions(Items.POTION,          "potion");
            case "splash_potion"   -> expandPotions(Items.SPLASH_POTION,   "splash_potion");
            case "lingering_potion" -> expandPotions(Items.LINGERING_POTION, "lingering_potion");
            case "tipped_arrow"    -> expandPotions(Items.TIPPED_ARROW,    "tipped_arrow");
            case "enchanted_book"  -> expandEnchantedBooks(registryAccess);
            case "suspicious_stew" -> expandSuspiciousStew();
            case "firework_rocket" -> expandFireworkRockets();
            case "goat_horn"       -> expandGoatHorns(registryAccess);
            default -> List.of();
        };
    }

    // ── Potions (and tipped arrows which share PotionContents) ───────────────

    private static List<SubtypeEntry> expandPotions(Item potionItem, String itemPath) {
        List<SubtypeEntry> result = new ArrayList<>();
        for (Holder.Reference<Potion> potionRef : BuiltInRegistries.POTION.holders().toList()) {
            if (result.size() >= HARD_CAP) {
                AMI.LOGGER.warn("SubtypeExpander: hit HARD_CAP for {}", itemPath);
                break;
            }
            ResourceLocation potionId = potionRef.key().location();
            if (potionId.getPath().equals("empty")) continue;

            ItemStack stack = new ItemStack(potionItem);
            stack.set(DataComponents.POTION_CONTENTS, new PotionContents(potionRef));

            ResourceLocation syntheticId = syntheticId(itemPath,
                    potionId.getNamespace(), potionId.getPath());
            result.add(new SubtypeEntry(syntheticId, stack, stack.getHoverName().getString()));
        }
        return result;
    }

    // ── Enchanted books ──────────────────────────────────────────────────────

    private static List<SubtypeEntry> expandEnchantedBooks(RegistryAccess registryAccess) {
        if (registryAccess == null) return List.of();
        var enchantmentRegistry = registryAccess.registry(Registries.ENCHANTMENT).orElse(null);
        if (enchantmentRegistry == null) return List.of();

        List<SubtypeEntry> result = new ArrayList<>();
        for (Holder.Reference<Enchantment> enchRef : enchantmentRegistry.holders().toList()) {
            if (result.size() >= HARD_CAP) {
                AMI.LOGGER.warn("SubtypeExpander: hit HARD_CAP for enchanted_book");
                break;
            }
            Enchantment enchantment = enchRef.value();
            ResourceLocation enchId = enchRef.key().location();
            int maxLevel = enchantment.getMaxLevel();

            for (int level = 1; level <= maxLevel; level++) {
                if (result.size() >= HARD_CAP) break;

                ItemStack stack = new ItemStack(Items.ENCHANTED_BOOK);
                ItemEnchantments.Mutable mutable = new ItemEnchantments.Mutable(ItemEnchantments.EMPTY);
                mutable.set(enchRef, level);
                stack.set(DataComponents.STORED_ENCHANTMENTS, mutable.toImmutable());

                String suffix = maxLevel > 1 ? "_" + level : "";
                ResourceLocation syntheticId = syntheticId("enchanted_book",
                        enchId.getNamespace(), enchId.getPath() + suffix);
                result.add(new SubtypeEntry(syntheticId, stack, stack.getHoverName().getString()));
            }
        }
        return result;
    }

    // ── Suspicious stew ──────────────────────────────────────────────────────

    private static List<SubtypeEntry> expandSuspiciousStew() {
        // Derive stew variants from flower blocks that implement SuspiciousEffectHolder.
        // This picks up modded flowers automatically without any hardcoded effect list.
        List<SubtypeEntry> result = new ArrayList<>();
        Set<ResourceLocation> seenEffectIds = new HashSet<>();

        for (Block block : BuiltInRegistries.BLOCK) {
            if (result.size() >= HARD_CAP) {
                AMI.LOGGER.warn("SubtypeExpander: hit HARD_CAP for suspicious_stew");
                break;
            }
            SuspiciousEffectHolder holder = SuspiciousEffectHolder.tryGet(block);
            if (holder == null) continue;

            SuspiciousStewEffects effects = holder.getSuspiciousEffects();
            if (effects.effects().isEmpty()) continue;

            // Deduplicate — multiple flower variants can share the same effect
            SuspiciousStewEffects.Entry firstEntry = effects.effects().get(0);
            ResourceLocation effectId = BuiltInRegistries.MOB_EFFECT
                    .getKey(firstEntry.effect().value());
            if (effectId == null || !seenEffectIds.add(effectId)) continue;

            ItemStack stack = new ItemStack(Items.SUSPICIOUS_STEW);
            stack.set(DataComponents.SUSPICIOUS_STEW_EFFECTS, effects);

            ResourceLocation syntheticId = syntheticId("suspicious_stew",
                    effectId.getNamespace(), effectId.getPath());
            result.add(new SubtypeEntry(syntheticId, stack, stack.getHoverName().getString()));
        }
        return result;
    }

    // ── Firework rockets ─────────────────────────────────────────────────────

    private static List<SubtypeEntry> expandFireworkRockets() {
        // Representative set: one entry per explosion shape × a standard red colour.
        // Keeping this short (5 entries) — full colour × shape combinations would be 5 × 16 = 80,
        // which is under the cap but clutters the UI for minimal gain.
        List<SubtypeEntry> result = new ArrayList<>();

        int[] repColors = { 0xFF0000, 0x00AA00, 0x5555FF, 0xFFFF55, 0xFFFFFF };
        FireworkExplosion.Shape[] shapes = FireworkExplosion.Shape.values();

        for (int i = 0; i < Math.min(shapes.length, repColors.length); i++) {
            FireworkExplosion.Shape shape = shapes[i];
            int color = repColors[i];

            FireworkExplosion explosion = new FireworkExplosion(
                    shape, IntList.of(color), IntList.of(), false, false);
            Fireworks fireworks = new Fireworks(1, List.of(explosion));

            ItemStack stack = new ItemStack(Items.FIREWORK_ROCKET);
            stack.set(DataComponents.FIREWORKS, fireworks);

            ResourceLocation syntheticId = syntheticId("firework_rocket", "minecraft", shape.name().toLowerCase());
            result.add(new SubtypeEntry(syntheticId, stack, "Firework Rocket (" + friendlyShape(shape) + ")"));
        }
        return result;
    }

    private static String friendlyShape(FireworkExplosion.Shape shape) {
        return switch (shape) {
            case SMALL_BALL  -> "Small Ball";
            case LARGE_BALL  -> "Large Ball";
            case STAR        -> "Star";
            case CREEPER     -> "Creeper";
            case BURST       -> "Burst";
        };
    }

    // ── Goat horns ───────────────────────────────────────────────────────────

    private static List<SubtypeEntry> expandGoatHorns(RegistryAccess registryAccess) {
        if (registryAccess == null) return List.of();
        var instrumentRegistry = registryAccess.registry(Registries.INSTRUMENT).orElse(null);
        if (instrumentRegistry == null) return List.of();

        List<SubtypeEntry> result = new ArrayList<>();
        for (Holder.Reference<Instrument> instrRef : instrumentRegistry.holders().toList()) {
            if (result.size() >= HARD_CAP) {
                AMI.LOGGER.warn("SubtypeExpander: hit HARD_CAP for goat_horn");
                break;
            }

            ItemStack stack = new ItemStack(Items.GOAT_HORN);
            stack.set(DataComponents.INSTRUMENT, instrRef);

            ResourceLocation instrId = instrRef.key().location();
            ResourceLocation syntheticId = syntheticId("goat_horn",
                    instrId.getNamespace(), instrId.getPath());
            result.add(new SubtypeEntry(syntheticId, stack, stack.getHoverName().getString()));
        }
        return result;
    }

    // ── Utilities ────────────────────────────────────────────────────────────

    /**
     * Build a synthetic ResourceLocation for a subtype node.
     * Format: {@code minecraft:itemPath/ns/subPath} (e.g. {@code minecraft:potion/minecraft/regeneration}).
     * Using the item's own namespace as the synthetic ID namespace keeps IDs clean and
     * avoids collisions with real registry entries.
     */
    private static ResourceLocation syntheticId(String itemPath, String subNs, String subPath) {
        return ResourceLocation.fromNamespaceAndPath("minecraft",
                itemPath + "/" + subNs + "/" + subPath);
    }
}
