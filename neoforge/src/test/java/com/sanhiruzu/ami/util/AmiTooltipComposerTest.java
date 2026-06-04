package com.sanhiruzu.ami.util;

import net.minecraft.network.chat.Component;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AmiTooltipComposerTest {

    @Test
    void modNameDetectionIgnoresFormattingAndWhitespace() {
        assertTrue(TooltipLineMatcher.containsLine(
                List.of(Component.literal("\u00A79\u00A7o  Create  ")),
                "Create"
        ));
    }

    @Test
    void modNameDetectionRequiresWholeLineMatch() {
        assertFalse(TooltipLineMatcher.containsLine(
                List.of(Component.literal("Created item")),
                "Create"
        ));
    }
}
