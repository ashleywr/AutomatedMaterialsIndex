package com.sanhiruzu.ami.client;

import com.sanhiruzu.ami.index.ClassificationOverride;
import com.sanhiruzu.ami.index.ClassificationOverrides;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class ClassificationOverrideTooltipTest {

    @AfterEach
    void cleanUp() { ClassificationOverrides.clear(); }

    @Test
    void appendsLinesWhenOverridePresent() {
        var stack = new ItemStack(BuiltInRegistries.ITEM.get(ResourceLocation.parse("minecraft:stone")));
        ClassificationOverrides.install(
                Map.of("minecraft:stone",
                    new ClassificationOverride(
                        EnumSet.noneOf(com.sanhiruzu.ami.index.ItemFacet.class),
                        EnumSet.noneOf(com.sanhiruzu.ami.index.ItemFacet.class),
                        EnumSet.noneOf(com.sanhiruzu.ami.index.SemanticVerb.class),
                        EnumSet.noneOf(com.sanhiruzu.ami.index.SemanticVerb.class),
                        null, null, List.of("Hello", "World"))),
                Map.of());

        List<Component> tooltip = new ArrayList<>();
        ClassificationOverrideTooltipAppender.appendTo(stack, tooltip);

        assertEquals(2, tooltip.size());
        assertEquals("Hello", tooltip.get(0).getString());
        assertEquals("World", tooltip.get(1).getString());
    }

    @Test
    void noOpWhenOverrideAbsent() {
        var stack = new ItemStack(BuiltInRegistries.ITEM.get(ResourceLocation.parse("minecraft:stone")));
        List<Component> tooltip = new ArrayList<>();
        ClassificationOverrideTooltipAppender.appendTo(stack, tooltip);
        assertEquals(0, tooltip.size());
    }
}
