package com.sanhiruzu.ami.client.icon;

import com.sanhiruzu.ami.client.tooltip.CompositeTooltipComponent;
import com.sanhiruzu.ami.client.tooltip.PokemonStatBarsComponent;
import com.sanhiruzu.ami.client.tooltip.PokemonTypeBadgesComponent;
import com.sanhiruzu.ami.index.SearchNode;
import com.sanhiruzu.ami.index.SearchNodeKeys;
import com.sanhiruzu.ami.util.tooltip.TooltipFactSupport;
import net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipComponent;
import net.minecraft.network.chat.Component;
import net.minecraft.world.inventory.tooltip.TooltipComponent;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

/**
 * Shared tooltip helpers for entity icon renderers.
 * Extracted here to avoid duplicating identical logic across the Forge and NeoForge modules.
 */
public final class EntityIconTooltipSupport {
    private EntityIconTooltipSupport() {}

    // ── Entity classification ─────────────────────────────────────────────────

    public static boolean isPokemonSpecies(SearchNode node) {
        return "pokemon_species".equals(node.meta(SearchNodeKeys.ENTITY_CATEGORY, ""));
    }

    // ── Category / traits text formatting ────────────────────────────────────

    public static Component formatCategoryComponent(String raw) {
        return switch (raw.toUpperCase(Locale.ROOT)) {
            case "MONSTER"                     -> Component.translatable("ami.entity_category.hostile");
            case "CREATURE"                    -> Component.translatable("ami.entity_category.passive");
            case "AMBIENT"                     -> Component.translatable("ami.entity_category.ambient");
            case "WATER_CREATURE", "WATER_AMBIENT" -> Component.translatable("ami.entity_category.aquatic");
            case "MISC"                        -> Component.translatable("ami.entity_category.misc");
            case "POKEMON_SPECIES"             -> Component.translatable("ami.entity_category.pokemon");
            default                            -> Component.literal(raw);
        };
    }

    public static String formatTraits(String raw) {
        StringBuilder sb = new StringBuilder();
        for (String token : raw.split(" ")) {
            if (token.startsWith("#")) {
                if (sb.length() > 0) sb.append("  ");
                sb.append(token.substring(1));
            }
        }
        return sb.toString();
    }

    // ── Token formatting (title-cases underscore/hyphen separated tokens) ────

    public static String formatTokenList(String raw) {
        if (raw == null || raw.isBlank()) return "";
        List<String> parts = new ArrayList<>();
        for (String token : raw.split("[,\\s]+")) {
            if (!token.isBlank()) parts.add(formatToken(token));
        }
        return String.join(", ", parts);
    }

    public static String formatToken(String raw) {
        String normalized = raw.replace('_', ' ').replace('-', ' ').trim();
        if (normalized.isBlank()) return "";
        StringBuilder out = new StringBuilder();
        for (String word : normalized.split("\\s+")) {
            if (word.isBlank()) continue;
            if (out.length() > 0) out.append(' ');
            out.append(Character.toUpperCase(word.charAt(0)));
            if (word.length() > 1) out.append(word.substring(1).toLowerCase(Locale.ROOT));
        }
        return out.toString();
    }

    // ── Pokémon species tooltip ───────────────────────────────────────────────

    /** Appends dex number and ability text lines for a Pokémon species node. */
    public static void appendPokemonTextLines(List<Component> lines, SearchNode node) {
        String dex = node.meta(SearchNodeKeys.POKEMON_DEX_NUMBER, "");
        if (!dex.isBlank()) {
            lines.addAll(TooltipFactSupport.line("ami.tooltip.pokemon_dex", "#" + dex, 0xFFE95B5B));
        }
        // Type and BST are shown as visual components via getTooltipImage — not duplicated here.
        String abilities = formatTokenList(node.meta(SearchNodeKeys.POKEMON_ABILITIES, ""));
        if (!abilities.isBlank()) {
            lines.addAll(TooltipFactSupport.line("ami.tooltip.pokemon_abilities", abilities, 0xFFBDE86B));
        }
    }

    /**
     * Builds the visual tooltip image for a Pokémon species node:
     * type-badge row followed by a base-stat bar chart.
     */
    public static Optional<TooltipComponent> buildPokemonVisuals(SearchNode node) {
        List<ClientTooltipComponent> parts = new ArrayList<>();

        String typeRaw = node.meta(SearchNodeKeys.POKEMON_TYPE, "");
        if (!typeRaw.isBlank()) {
            List<String> types = new ArrayList<>();
            for (String t : typeRaw.split("[,\\s]+")) {
                if (!t.isBlank()) types.add(t.toLowerCase(Locale.ROOT));
            }
            if (!types.isEmpty()) parts.add(new PokemonTypeBadgesComponent(types));
        }

        int hp  = parseStat(node.meta(SearchNodeKeys.POKEMON_BASE_HP, ""));
        int atk = parseStat(node.meta(SearchNodeKeys.POKEMON_BASE_ATTACK, ""));
        int def = parseStat(node.meta(SearchNodeKeys.POKEMON_BASE_DEFENSE, ""));
        int spa = parseStat(node.meta(SearchNodeKeys.POKEMON_BASE_SPECIAL_ATTACK, ""));
        int spd = parseStat(node.meta(SearchNodeKeys.POKEMON_BASE_SPECIAL_DEFENSE, ""));
        int spe = parseStat(node.meta(SearchNodeKeys.POKEMON_BASE_SPEED, ""));
        if (hp + atk + def + spa + spd + spe > 0) {
            parts.add(new PokemonStatBarsComponent(hp, atk, def, spa, spd, spe));
        }

        if (parts.isEmpty()) return Optional.empty();
        if (parts.size() == 1) return Optional.of((TooltipComponent) parts.get(0));
        return Optional.of(new CompositeTooltipComponent(parts));
    }

    // ── Internal utilities ────────────────────────────────────────────────────

    private static int parseStat(String s) {
        if (s == null || s.isBlank()) return 0;
        try { return Integer.parseInt(s.trim()); }
        catch (NumberFormatException ignored) { return 0; }
    }
}
