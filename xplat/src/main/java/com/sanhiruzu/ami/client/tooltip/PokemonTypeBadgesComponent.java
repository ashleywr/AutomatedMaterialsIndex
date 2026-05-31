package com.sanhiruzu.ami.client.tooltip;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipComponent;
import net.minecraft.network.chat.Component;
import net.minecraft.world.inventory.tooltip.TooltipComponent;

import java.util.List;
import java.util.Locale;

/**
 * Renders colored type pill-badges inline in a tooltip (e.g. [Grass] [Poison]).
 * Type names are looked up via translatable components so they follow the game locale.
 * Register with an identity factory: event.register(class, c -> c).
 */
public final class PokemonTypeBadgesComponent implements TooltipComponent, ClientTooltipComponent {

    // BADGE_H must exceed font.lineHeight (9) to leave room for vertical centering.
    // With BADGE_H=12 and the +1 formula, text sits 2px from top and 2px from bottom.
    private static final int BADGE_H   = 12;
    private static final int PAD_X     = 4;
    private static final int BADGE_GAP = 3;
    private static final int TOP_PAD   = 3;

    private final List<String> types;

    public PokemonTypeBadgesComponent(List<String> types) {
        this.types = List.copyOf(types);
    }

    @Override
    public int getHeight() {
        return TOP_PAD + BADGE_H;
    }

    @Override
    public int getWidth(Font font) {
        int total = 0;
        for (int i = 0; i < types.size(); i++) {
            if (i > 0) total += BADGE_GAP;
            total += font.width(typeName(types.get(i))) + PAD_X * 2;
        }
        return total;
    }

    @Override
    public void renderImage(Font font, int x, int y, GuiGraphics g) {
        int cx = x;
        int by = y + TOP_PAD;
        for (String type : types) {
            String label = typeName(type);
            int    color = color(type);
            int    bw    = font.width(label) + PAD_X * 2;

            g.fill(cx, by, cx + bw, by + BADGE_H, 0xFF000000 | color);

            // +1 accounts for the 1px line-gap included in font.lineHeight
            int textY = by + (BADGE_H - font.lineHeight + 1) / 2;
            g.drawString(font, label, cx + PAD_X, textY, 0xFFFFFFFF, false);

            cx += bw + BADGE_GAP;
        }
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    /**
     * Returns the localized display name for a type token (e.g. "fire" → "Fire").
     * Falls back to title-casing the raw token if the lang key is missing.
     */
    private static String typeName(String type) {
        String key = "ami.pokemon_type." + type.toLowerCase(Locale.ROOT);
        String resolved = Component.translatable(key).getString();
        if (resolved.equals(key)) {
            // Key not present — title-case the raw token as a fallback
            String lc = type.toLowerCase(Locale.ROOT);
            return lc.isEmpty() ? lc : Character.toUpperCase(lc.charAt(0)) + lc.substring(1);
        }
        return resolved;
    }

    public static int color(String type) {
        return switch (type.toLowerCase(Locale.ROOT)) {
            case "normal"   -> 0x706060;
            case "fire"     -> 0xC03020;
            case "water"    -> 0x3060C0;
            case "electric" -> 0xB09000;
            case "grass"    -> 0x30A030;
            case "ice"      -> 0x40A0A0;
            case "fighting" -> 0x902020;
            case "poison"   -> 0x803090;
            case "ground"   -> 0xA08030;
            case "flying"   -> 0x6060C0;
            case "psychic"  -> 0xC03060;
            case "bug"      -> 0x709000;
            case "rock"     -> 0x908030;
            case "ghost"    -> 0x504080;
            case "dragon"   -> 0x4020C0;
            case "dark"     -> 0x504040;
            case "steel"    -> 0x708080;
            case "fairy"    -> 0xB06080;
            default         -> 0x404050;
        };
    }
}
