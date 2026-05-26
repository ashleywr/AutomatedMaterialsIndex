package com.sanhiruzu.ami.util;

import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Shared logic for formatting and copying tooltip text to the system clipboard.
 */
public final class AmiClipboardHelper {

    private AmiClipboardHelper() {
    }

    /**
     * Extracts text from a list of Components, strips Minecraft formatting codes,
     * and copies the resulting plain text to the clipboard.
     */
    public static void copyComponentsToClipboard(List<Component> lines) {
        if (lines == null || lines.isEmpty()) return;

        String text = lines.stream()
                .map(Component::getString)
                .map(ChatFormatting::stripFormatting)
                .collect(Collectors.joining("\n"));

        copyToClipboard(text);
    }

    /**
     * Resolves the full tooltip for an ItemStack, strips formatting, and copies it to the clipboard.
     */
    public static void copyItemTooltipToClipboard(ItemStack stack) {
        if (stack == null || stack.isEmpty()) return;

        List<Component> lines = Screen.getTooltipFromItem(Minecraft.getInstance(), stack);
        copyComponentsToClipboard(lines);
    }

    /**
     * Low-level helper to push a string to the system clipboard via Minecraft's keyboard handler.
     */
    public static void copyToClipboard(String text) {
        if (text == null || text.isEmpty()) return;
        Minecraft.getInstance().keyboardHandler.setClipboard(text);
    }
}
