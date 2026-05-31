package com.sanhiruzu.ami.util.tooltip;

import com.sanhiruzu.ami.client.icon.EntityIconTooltipSupport;
import com.sanhiruzu.ami.index.SearchNode;
import com.sanhiruzu.ami.index.SearchNodeKeys;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.List;

public final class PokemonTooltipFact implements AmiTooltipFact {
    @Override
    public List<Component> build(SearchNode entry) {
        List<Component> lines = new ArrayList<>();

        String ballTier   = EntityIconTooltipSupport.formatToken(entry.meta(SearchNodeKeys.POKEMON_BALL_TIER, ""));
        String ballFamily = EntityIconTooltipSupport.formatToken(entry.meta(SearchNodeKeys.POKEMON_BALL_FAMILY, ""));
        if (!ballTier.isBlank()) {
            String detail = ballFamily.isBlank() ? ballTier : ballFamily + " / " + ballTier;
            lines.addAll(TooltipFactSupport.line("ami.tooltip.pokemon_ball", detail));
        }

        String healing = entry.meta(SearchNodeKeys.POKEMON_HEALING, "");
        if (!healing.isBlank()) {
            lines.addAll(TooltipFactSupport.line("ami.tooltip.pokemon_healing", healing + " HP"));
        }

        String medicineKind = EntityIconTooltipSupport.formatToken(entry.meta(SearchNodeKeys.POKEMON_MEDICINE_KIND, ""));
        if (!medicineKind.isBlank()) {
            lines.addAll(TooltipFactSupport.line("ami.tooltip.pokemon_medicine", medicineKind));
        }

        String statusCure = EntityIconTooltipSupport.formatTokenList(entry.meta(SearchNodeKeys.POKEMON_STATUS_CURE, ""));
        if (!statusCure.isBlank()) {
            lines.addAll(TooltipFactSupport.line("ami.tooltip.pokemon_status_cure", statusCure));
        }

        String heldRole = EntityIconTooltipSupport.formatToken(entry.meta(SearchNodeKeys.POKEMON_HELD_ITEM_ROLE, ""));
        if (!heldRole.isBlank()) {
            lines.addAll(TooltipFactSupport.line("ami.tooltip.pokemon_held_role", heldRole));
        }

        String evolutionTrigger = EntityIconTooltipSupport.formatToken(entry.meta(SearchNodeKeys.POKEMON_EVOLUTION_TRIGGER, ""));
        if (!evolutionTrigger.isBlank()) {
            lines.addAll(TooltipFactSupport.line("ami.tooltip.pokemon_evolution_trigger", evolutionTrigger));
        }

        return lines;
    }
}
