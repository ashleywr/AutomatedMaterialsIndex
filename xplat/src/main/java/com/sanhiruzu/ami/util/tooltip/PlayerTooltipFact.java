package com.sanhiruzu.ami.util.tooltip;

import com.sanhiruzu.ami.index.NodeType;
import com.sanhiruzu.ami.index.SearchNode;
import com.sanhiruzu.ami.index.SearchNodeKeys;
import com.sanhiruzu.ami.index.providers.RegistryUtils;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

public final class PlayerTooltipFact implements AmiTooltipFact {
    private static final int MAX_PROVIDER_PREVIEW = 3;

    private static boolean isSocialPlayerNode(SearchNode entry) {
        return entry.type() == NodeType.PLAYER || !entry.meta(SearchNodeKeys.PLAYER_HEAD_NAME, "").isBlank();
    }

    private static void appendStatus(List<Component> lines, SearchNode entry) {
        String status;
        if (isOnline(entry)) {
            status = Component.translatable("ami.tooltip.player_status_online").getString();
        } else if (!entry.meta(SearchNodeKeys.PLAYER_HEAD_NAME, "").isBlank()) {
            status = Component.translatable("ami.tooltip.player_status_offline").getString();
        } else {
            return;
        }
        lines.addAll(TooltipFactSupport.line("ami.tooltip.player_status", status));
    }

    private static void appendSource(List<Component> lines, SearchNode entry) {
        if (entry.type() != NodeType.ITEM) {
            return;
        }
        String source = entry.meta(SearchNodeKeys.PLAYER_HEAD_SOURCE, "");
        if (source.isBlank()) return;

        String label = switch (source) {
            case "online_player" -> "ami.tooltip.player_head_source_online";
            case "typed_or_history" -> "ami.tooltip.player_head_source_history";
            default -> "";
        };
        if (!label.isBlank()) {
            lines.addAll(TooltipFactSupport.line("ami.tooltip.player_head_source", Component.translatable(label).getString()));
        }
    }

    private static void appendLocation(List<Component> lines, SearchNode entry) {
        String x = entry.meta(SearchNodeKeys.PLAYER_X, "");
        String y = entry.meta(SearchNodeKeys.PLAYER_Y, "");
        String z = entry.meta(SearchNodeKeys.PLAYER_Z, "");
        if (x.isBlank() || y.isBlank() || z.isBlank()) {
            return;
        }
        String dimension = prettyDimension(entry.meta(SearchNodeKeys.PLAYER_DIMENSION, ""));
        String location = dimension.isBlank()
                ? String.format(Locale.ROOT, "%s, %s, %s", x, y, z)
                : dimension + " @ " + String.format(Locale.ROOT, "%s, %s, %s", x, y, z);
        lines.addAll(TooltipFactSupport.line("ami.tooltip.player_position", location));
    }

    private static void appendWaypointProviders(List<Component> lines, SearchNode entry) {
        String rawProviders = entry.meta(SearchNodeKeys.PLAYER_WAYPOINT_PROVIDER_LABELS, "");
        if (rawProviders.isBlank()) return;

        List<String> providers = splitCsv(rawProviders);
        if (providers.isEmpty()) return;

        String providerValue = formatProviderList(providers);
        lines.addAll(TooltipFactSupport.line("ami.tooltip.player_waypoint_providers", providerValue));
    }

    private static String formatProviderList(List<String> providers) {
        if (Screen.hasShiftDown() || providers.size() <= MAX_PROVIDER_PREVIEW) {
            return String.join(", ", providers);
        }
        List<String> preview = providers.subList(0, MAX_PROVIDER_PREVIEW);
        int remaining = providers.size() - MAX_PROVIDER_PREVIEW;
        return String.join(", ", preview)
                + ", "
                + Component.translatable("ami.tooltip.more_entries", remaining).getString();
    }

    private static boolean isOnline(SearchNode entry) {
        return "true".equals(entry.meta(SearchNodeKeys.PLAYER_ONLINE, ""));
    }

    private static String prettyDimension(String rawDimension) {
        ResourceLocation parsed = ResourceLocation.tryParse(rawDimension);
        if (parsed == null) {
            return rawDimension;
        }
        if ("minecraft".equals(parsed.getNamespace())) {
            return switch (parsed.getPath()) {
                case "overworld" -> Component.translatable("ami.dimension.overworld").getString();
                case "the_nether" -> Component.translatable("ami.dimension.nether").getString();
                case "the_end" -> Component.translatable("ami.dimension.end").getString();
                default -> RegistryUtils.formatPath(parsed.getPath());
            };
        }
        return RegistryUtils.formatPath(parsed.getPath()) + " (" + parsed.getNamespace() + ")";
    }

    private static List<String> splitCsv(String raw) {
        if (raw == null || raw.isBlank()) {
            return List.of();
        }
        return Arrays.stream(raw.split(","))
                .map(String::trim)
                .filter(value -> !value.isBlank())
                .distinct()
                .toList();
    }

    @Override
    public List<Component> build(SearchNode entry) {
        if (entry == null || !isSocialPlayerNode(entry)) {
            return List.of();
        }

        List<Component> lines = new ArrayList<>();
        appendStatus(lines, entry);
        appendSource(lines, entry);
        appendLocation(lines, entry);
        appendWaypointProviders(lines, entry);
        return lines;
    }
}
