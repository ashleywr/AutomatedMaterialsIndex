package com.sanhiruzu.ami.index;

import com.sanhiruzu.ami.config.AmiConfig;
import net.minecraft.resources.ResourceLocation;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public class ItemFilterTest {

    @BeforeEach
    void setup() {
        AmiConfig.resetToDefaults();
    }

    @Test
    void testClassifyAccessLevel() {
        // Survival items
        assertEquals(ItemFilter.ACCESS_SURVIVAL, ItemFilter.classifyAccessLevel(ResourceLocation.parse("minecraft:iron_ingot"), true));
        
        // Creative/Cheat items
        assertEquals(ItemFilter.ACCESS_CREATIVE, ItemFilter.classifyAccessLevel(ResourceLocation.parse("minecraft:zombie_spawn_egg"), true));
        assertEquals(ItemFilter.ACCESS_CHEAT, ItemFilter.classifyAccessLevel(ResourceLocation.parse("minecraft:command_block"), true));
        
        // Hidden items (not in creative)
        assertEquals(ItemFilter.ACCESS_DEV, ItemFilter.classifyAccessLevel(ResourceLocation.parse("minecraft:barrier"), false));
    }

    @Test
    void testShouldShowAccessLevel() {
        // Default survival should always show
        assertTrue(ItemFilter.shouldShowAccessLevel(ItemFilter.ACCESS_SURVIVAL));
        
        // Cheat mode off - shouldn't show cheat items
        assertFalse(ItemFilter.shouldShowAccessLevel(ItemFilter.ACCESS_CHEAT));
        
        // Cheat mode on
        AmiConfig.cheatMode = true;
        assertTrue(ItemFilter.shouldShowAccessLevel(ItemFilter.ACCESS_CHEAT));
        assertTrue(ItemFilter.shouldShowAccessLevel(ItemFilter.ACCESS_CREATIVE));
        
        // Dev mode on
        AmiConfig.cheatMode = false;
        AmiConfig.devMode = true;
        assertTrue(ItemFilter.shouldShowAccessLevel(ItemFilter.ACCESS_DEV));
    }
}
