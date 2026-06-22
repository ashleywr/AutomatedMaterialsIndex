package com.sanhiruzu.ami.index;

import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PrimaryCategoryTextMatchersTest {
    @Test
    void metadataTokenMatchesTrimmedCommaSeparatedValuesExactly() {
        Map<String, String> attributes = Map.of("tags", "minecraft:logs, c:crops ,forge:crops");

        assertTrue(PrimaryCategoryTextMatchers.hasMetadataToken(attributes, "tags", "c:crops"));
        assertFalse(PrimaryCategoryTextMatchers.hasMetadataToken(attributes, "tags", "c:crop"));
        assertFalse(PrimaryCategoryTextMatchers.hasMetadataToken(Map.of(), "tags", "c:crops"));
    }

    @Test
    void csvTokenHandlesNullBlankAndWhitespace() {
        assertFalse(PrimaryCategoryTextMatchers.hasCsvToken(null, "moisture"));
        assertFalse(PrimaryCategoryTextMatchers.hasCsvToken("  ", "moisture"));
        assertTrue(PrimaryCategoryTextMatchers.hasCsvToken("axis, moisture , waterlogged", "moisture"));
        assertFalse(PrimaryCategoryTextMatchers.hasCsvToken("axis,moisture_level", "moisture"));
    }

    @Test
    void pathTokenMatchingUsesTokenBoundariesAndPhrases() {
        assertTrue(PrimaryCategoryTextMatchers.containsPathToken("oak_wall_sign", Set.of("wall_sign")));
        assertTrue(PrimaryCategoryTextMatchers.endsWithPathToken("potato_cannon", "cannon"));
        assertFalse(PrimaryCategoryTextMatchers.containsPathToken("gearbox", Set.of("gear")));
        assertFalse(PrimaryCategoryTextMatchers.endsWithPathToken("cannon_mount", "cannon"));
    }

    @Test
    void substringMatchingIsCaseInsensitiveButRejectsBlankInput() {
        assertTrue(PrimaryCategoryTextMatchers.containsAnyIgnoreCase("com.example.PowerBottleBlock", "powerbottle"));
        assertFalse(PrimaryCategoryTextMatchers.containsAnyIgnoreCase("", "powerbottle"));
        assertFalse(PrimaryCategoryTextMatchers.containsAnyIgnoreCase(null, "powerbottle"));
    }
}
