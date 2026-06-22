package com.sanhiruzu.ami.index;

import net.minecraft.resources.ResourceLocation;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;

class ClassificationOverridesLoadTest {

    @AfterEach
    void reset() {
        ClassificationOverrides.clear();
    }

    @Test
    void loadingBundledDefaultsDoesNotThrowAndLeavesRegistryQueryable() {
        ClassificationOverrides.loadBundledDefaults();
        // The shipped default file is empty, so no override should match, and no exception should be thrown.
        assertTrue(ClassificationOverrides.forItem(new ResourceLocation("examplemod:widget")).isEmpty());
        assertTrue(ClassificationOverrides.patternFor("botania", "mana_spreader").isEmpty());
    }
}
