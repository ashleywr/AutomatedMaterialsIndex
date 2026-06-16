package com.sanhiruzu.ami.index.providers;

import net.minecraft.resources.Identifier;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SubtypeExpanderTest {
    @Test
    void enchantedBooksUseHigherParityCap() {
        assertEquals(SubtypeExpander.ENCHANTED_BOOK_CAP, SubtypeExpander.capFor("enchanted_book"));
        assertEquals(SubtypeExpander.HARD_CAP, SubtypeExpander.capFor("potion"));
    }

    @Test
    void creativeParityFamiliesPreferCreativeStacks() {
        assertTrue(SubtypeExpander.shouldPreferCreativeStackParity(new Identifier("minecraft", "enchanted_book")));
        assertTrue(SubtypeExpander.shouldPreferCreativeStackParity(new Identifier("minecraft", "suspicious_stew")));
        assertTrue(SubtypeExpander.shouldPreferCreativeStackParity(new Identifier("minecraft", "firework_rocket")));
        assertFalse(SubtypeExpander.shouldPreferCreativeStackParity(new Identifier("minecraft", "potion")));
        assertFalse(SubtypeExpander.shouldPreferCreativeStackParity(new Identifier("apotheosis", "enchanted_book")));
    }

    @Test
    void potionParityKeepsLongAndStrongVariants() {
        assertFalse(SubtypeExpander.shouldSkipPotionSubtype(new Identifier("minecraft", "long_fire_resistance")));
        assertFalse(SubtypeExpander.shouldSkipPotionSubtype(new Identifier("minecraft", "strong_swiftness")));
        assertFalse(SubtypeExpander.shouldSkipPotionSubtype(new Identifier("apotheosis", "extra_long_flying")));
        assertFalse(SubtypeExpander.shouldSkipPotionSubtype(new Identifier("minecraft", "awkward")));
    }

    @Test
    void potionParityStillSkipsEmptySentinel() {
        assertTrue(SubtypeExpander.shouldSkipPotionSubtype(new Identifier("minecraft", "empty")));
        assertTrue(SubtypeExpander.shouldSkipPotionSubtype(null));
    }
}
