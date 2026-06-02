package com.sanhiruzu.ami.index;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PathTokensTest {
    @Test
    void tokenMatchingDoesNotUseRawSubstringContains() {
        PathTokens tokens = PathTokens.of("copper_gearbox");

        assertTrue(tokens.contains("gearbox"));
        assertFalse(tokens.contains("gear"));
    }

    @Test
    void phraseMatchingSupportsRegistryStyleSeparators() {
        PathTokens tokens = PathTokens.of("oak_pressure_plate");

        assertTrue(tokens.contains("pressure_plate"));
        assertTrue(tokens.endsWith("pressure_plate"));
        assertFalse(tokens.contains("sure_plate"));
    }

    @Test
    void tagsAndPathsNormalizeSeparatorsConsistently() {
        PathTokens tokens = PathTokens.of("minecraft:mineable/pickaxe,c:ingots/iron");

        assertTrue(tokens.contains("mineable"));
        assertTrue(tokens.contains("pickaxe"));
        assertTrue(tokens.contains("ingots"));
        assertTrue(tokens.contains("iron"));
        assertFalse(tokens.contains("able"));
    }
}
