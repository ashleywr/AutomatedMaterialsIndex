package com.sanhiruzu.ami.client;

import net.minecraft.resources.ResourceLocation;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class EntityIconSlowKeysTest {

    @AfterEach
    void cleanup() {
        EntityIconSlowKeys.clear();
    }

    @Test
    void entityNotSlowByDefault() {
        assertFalse(EntityIconSlowKeys.isKnownSlow(ResourceLocation.parse("minecraft:zombie")));
    }

    @Test
    void entityMarkedSlowWhenBakeExceedsThreshold() {
        ResourceLocation id = ResourceLocation.parse("test:heavy_mob");
        EntityIconSlowKeys.recordBakeElapsed(id, EntityIconSlowKeys.SLOW_ENTITY_THRESHOLD_NANOS + 1);
        assertTrue(EntityIconSlowKeys.isKnownSlow(id));
    }

    @Test
    void entityNotMarkedSlowWhenBakeEqualsThreshold() {
        ResourceLocation id = ResourceLocation.parse("test:borderline_mob");
        EntityIconSlowKeys.recordBakeElapsed(id, EntityIconSlowKeys.SLOW_ENTITY_THRESHOLD_NANOS);
        assertFalse(EntityIconSlowKeys.isKnownSlow(id));
    }

    @Test
    void entityNotMarkedSlowWhenBakeBelowThreshold() {
        ResourceLocation id = ResourceLocation.parse("test:fast_mob");
        EntityIconSlowKeys.recordBakeElapsed(id, EntityIconSlowKeys.SLOW_ENTITY_THRESHOLD_NANOS - 1);
        assertFalse(EntityIconSlowKeys.isKnownSlow(id));
    }
}
