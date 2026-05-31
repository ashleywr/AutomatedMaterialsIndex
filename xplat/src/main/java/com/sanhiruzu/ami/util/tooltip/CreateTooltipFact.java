package com.sanhiruzu.ami.util.tooltip;

import com.sanhiruzu.ami.index.SearchNode;
import com.sanhiruzu.ami.index.SearchNodeKeys;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * Shows Create mod recipe roles ("Used in: Mixer, Press") and a ponder badge
 * when the item has Ponder tutorial content.
 */
public final class CreateTooltipFact implements AmiTooltipFact {

    @Override
    public List<Component> build(SearchNode entry) {
        List<Component> lines = new ArrayList<>();

        String rolesRaw = entry.meta(SearchNodeKeys.CREATE_RECIPE_ROLES, "");
        if (!rolesRaw.isBlank()) {
            Set<String> inputs  = new LinkedHashSet<>();
            Set<String> outputs = new LinkedHashSet<>();

            for (String token : rolesRaw.split(",")) {
                token = token.trim();
                if (token.endsWith("_input")) {
                    String machine = machineName(token.substring(0, token.length() - 6));
                    if (!machine.isBlank()) inputs.add(machine);
                } else if (token.endsWith("_output")) {
                    String machine = machineName(token.substring(0, token.length() - 7));
                    if (!machine.isBlank()) outputs.add(machine);
                }
            }

            if (!inputs.isEmpty()) {
                lines.addAll(TooltipFactSupport.line(
                        "ami.tooltip.create_used_in", String.join(", ", inputs)));
            }
            if (!outputs.isEmpty()) {
                lines.addAll(TooltipFactSupport.line(
                        "ami.tooltip.create_made_by", String.join(", ", outputs)));
            }
        }

        if ("true".equals(entry.meta(SearchNodeKeys.CREATE_HAS_PONDER, ""))) {
            lines.addAll(TooltipFactSupport.message("ami.tooltip.create_has_ponder"));
        }

        return lines;
    }

    private static String machineName(String role) {
        return switch (role.toLowerCase(Locale.ROOT)) {
            case "pressing"             -> "Press";
            case "crushing"             -> "Crusher";
            case "mixing", "basin"      -> "Mixer";
            case "milling"              -> "Millstone";
            case "cutting", "sawing"    -> "Saw";
            case "compacting"           -> "Compactor";
            case "deploying"            -> "Deployer";
            case "item_application"     -> "Deployer";
            case "fan_washing",
                 "washing"             -> "Fan Washing";
            case "fan_blasting",
                 "blasting"            -> "Fan Blasting";
            case "fan_smoking",
                 "smoking"             -> "Fan Smoking";
            case "fan_haunting",
                 "haunting"            -> "Fan Haunting";
            case "mechanical_crafting"  -> "Mech. Crafting";
            case "sequenced_assembly"   -> "Sequenced Assembly";
            default                     -> "";
        };
    }
}
