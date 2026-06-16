package com.sanhiruzu.ami.client;

import net.minecraft.resources.Identifier;
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
        assertFalse(EntityIconSlowKeys.isKnownSlow(Identifier.parse("minecraft:zombie")));
    }

    @Test
    void entityMarkedSlowWhenBakeExceedsThreshold() {
        Identifier id = Identifier.parse("test:heavy_mob");
        EntityIconSlowKeys.recordBakeElapsed(id, EntityIconSlowKeys.SLOW_ENTITY_THRESHOLD_NANOS + 1);
        assertTrue(EntityIconSlowKeys.isKnownSlow(id));
    }

    @Test
    void entityNotMarkedSlowWhenBakeEqualsThreshold() {
        Identifier id = Identifier.parse("test:borderline_mob");
        EntityIconSlowKeys.recordBakeElapsed(id, EntityIconSlowKeys.SLOW_ENTITY_THRESHOLD_NANOS);
        assertFalse(EntityIconSlowKeys.isKnownSlow(id));
    }

    @Test
    void entityNotMarkedSlowWhenBakeBelowThreshold() {
        Identifier id = Identifier.parse("test:fast_mob");
        EntityIconSlowKeys.recordBakeElapsed(id, EntityIconSlowKeys.SLOW_ENTITY_THRESHOLD_NANOS - 1);
        assertFalse(EntityIconSlowKeys.isKnownSlow(id));
    }
}
