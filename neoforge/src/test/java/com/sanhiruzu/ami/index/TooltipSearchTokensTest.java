package com.sanhiruzu.ami.index;

import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class TooltipSearchTokensTest {
    @Test
    public void extractsFilteredTooltipWordsForPlainSearch() {
        String tokens = TooltipSearchTokens.extract(
                List.of(
                        Component.literal("Poke Ball"),
                        Component.literal("Can catch wild Pok\u00E9mon in battle"),
                        Component.literal("Hold Shift for details"),
                        Component.literal("Cobblemon")
                ),
                "Poke Ball",
                new ResourceLocation("cobblemon:poke_ball"),
                "Cobblemon"
        );

        Set<String> parts = split(tokens);
        assertTrue(parts.contains("catch"));
        assertTrue(parts.contains("wild"));
        assertTrue(parts.contains("pokemon"));
        assertTrue(parts.contains("battle"));
        assertFalse(parts.contains("poke"));
        assertFalse(parts.contains("ball"));
        assertFalse(parts.contains("shift"));
        assertFalse(parts.contains("cobblemon"));
    }

    @Test
    public void ignoresDiagnosticTooltipLinesForPlainSearch() {
        String tokens = TooltipSearchTokens.extract(
                List.of(
                        Component.literal("Redstone Dust"),
                        Component.literal("Si(FeS2)5(CrAl2O3)Hg3"),
                        Component.literal("ID: minecraft:redstone"),
                        Component.literal("#cobblemon:pokedex_screen"),
                        Component.literal("Comp: minecraft:lore"),
                        Component.literal("+6 more (hold Shift)"),
                        Component.literal("AMI Group: Default"),
                        Component.literal("[Middle Button] give one")
                ),
                "Redstone Dust",
                new ResourceLocation("minecraft:redstone"),
                "Minecraft"
        );

        Set<String> parts = split(tokens);
        assertFalse(parts.contains("cobblemon"));
        assertFalse(parts.contains("pokedex"));
        assertFalse(parts.contains("screen"));
        assertFalse(parts.contains("minecraft"));
        assertFalse(parts.contains("lore"));
        assertFalse(parts.contains("default"));
    }

    private static Set<String> split(String tokens) {
        return Arrays.stream(tokens.split("\\s+"))
                .filter(part -> !part.isBlank())
                .collect(Collectors.toSet());
    }
}
