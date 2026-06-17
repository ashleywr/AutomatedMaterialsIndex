package com.sanhiruzu.ami.client;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertTrue;

class ItemTooltipEventPolicyTest {
    @Test
    void forgeItemTooltipHandlerRunsAfterOtherTooltipMods() throws Exception {
        String source = Files.readString(Path.of("..", "forge", "src", "main", "java", "com", "sanhiruzu",
                "ami", "forge", "AMIClient.java"));

        assertTrue(source.contains("import net.minecraftforge.eventbus.api.EventPriority;"));
        assertTrue(source.contains("@SubscribeEvent(priority = EventPriority.LOWEST)\r\n        public static void onItemTooltip")
                || source.contains("@SubscribeEvent(priority = EventPriority.LOWEST)\n        public static void onItemTooltip"));
    }

    @Test
    void neoforgeItemTooltipHandlerRunsAfterOtherTooltipMods() throws Exception {
        String source = Files.readString(Path.of("..", "neoforge", "src", "main", "java", "com", "sanhiruzu",
                "ami", "neoforge", "AMIClient.java"));

        assertTrue(source.contains("import net.neoforged.bus.api.EventPriority;"));
        assertTrue(source.contains("@SubscribeEvent(priority = EventPriority.LOWEST)\r\n    static void onItemTooltip")
                || source.contains("@SubscribeEvent(priority = EventPriority.LOWEST)\n    static void onItemTooltip"));
    }
}
