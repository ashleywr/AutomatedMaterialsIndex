package com.sanhiruzu.ami.client.results;

import com.sanhiruzu.ami.config.AmiConfig;
import com.sanhiruzu.ami.index.AmiOntology;
import com.sanhiruzu.ami.index.FacetCodec;
import com.sanhiruzu.ami.index.SearchNode;
import com.sanhiruzu.ami.index.SearchNodeKeys;
import net.minecraft.ChatFormatting;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Builds an AMI-internal debug tooltip shown when Left Control is held while hovering an entry.
 * Reveals: registry ID, ontology classification, classification routing, curated metadata, and tags.
 */
public final class DebugTooltip {

    private static final int MAX_TAGS_SHOWN = 12;
    private static final int MAX_TRACE_LINES_SHOWN = 44;

    private DebugTooltip() {
    }

    public static List<Component> build(SearchNode entry) {
        if (!AmiConfig.devMode) {
            return List.of(Component.literal(entry.displayName()));
        }

        List<Component> lines = new ArrayList<>();

        // Header
        lines.add(Component.translatable("ami.debug.header")
                .withStyle(ChatFormatting.YELLOW, ChatFormatting.BOLD));
        lines.add(Component.literal(entry.displayName())
                .withStyle(ChatFormatting.WHITE));
        lines.add(Component.empty()
                .append(Component.literal(entry.id().toString()).withStyle(ChatFormatting.GRAY))
                .append(Component.literal("  "))
                .append(Component.translatable("ami.debug.type").withStyle(ChatFormatting.DARK_GRAY))
                .append(Component.literal(entry.type().name()).withStyle(ChatFormatting.DARK_GRAY)));

        // Ontology
        AmiOntology.Category classified = AmiOntology.classifyNode(entry);
        String precat = entry.meta(SearchNodeKeys.ONTOLOGY_CATEGORY, "");
        String presub = entry.meta(SearchNodeKeys.ONTOLOGY_SUBCATEGORY, "");

        lines.add(Component.empty());
        lines.add(Component.translatable("ami.debug.ontology").withStyle(ChatFormatting.GOLD));
        lines.add(Component.empty()
                .append(Component.literal("  "))
                .append(Component.translatable("ami.debug.classified").withStyle(ChatFormatting.GRAY))
                .append(Component.literal("  "))
                .append(Component.literal(classified.displayName().getString()).withStyle(ChatFormatting.WHITE))
                .append(Component.literal(" (" + classified.id + ")").withStyle(ChatFormatting.DARK_GRAY)));
        if (!precat.isEmpty() || !presub.isEmpty()) {
            String both = precat.isEmpty() ? presub : precat + (presub.isEmpty() ? "" : " / " + presub);
            lines.add(Component.empty()
                    .append(Component.literal("  "))
                    .append(Component.translatable("ami.debug.precomputed").withStyle(ChatFormatting.GRAY))
                    .append(Component.literal(" "))
                    .append(Component.literal(both).withStyle(ChatFormatting.GREEN)));
        } else {
            lines.add(Component.empty()
                    .append(Component.literal("  "))
                    .append(Component.translatable("ami.debug.precomputed").withStyle(ChatFormatting.GRAY))
                    .append(Component.literal(" "))
                    .append(Component.translatable("ami.debug.runtime_only").withStyle(ChatFormatting.RED)));
        }

        appendRouting(lines, entry, precat, presub);

        // Other metadata (sorted, excludes ontology/routing/facets/tags shown separately)
        List<Map.Entry<String, String>> otherMeta = entry.metadata().entrySet().stream()
                .filter(e -> !isRenderedElsewhere(e.getKey()))
                .sorted(Map.Entry.comparingByKey())
                .toList();

        if (!otherMeta.isEmpty()) {
            lines.add(Component.empty());
            lines.add(Component.translatable("ami.debug.metadata").withStyle(ChatFormatting.GOLD));
            for (var kv : otherMeta) {
                lines.add(Component.empty()
                        .append(Component.literal("  ").withStyle(ChatFormatting.GRAY))
                        .append(Component.literal(kv.getKey()).withStyle(ChatFormatting.GRAY))
                        .append(Component.literal(" "))
                        .append(Component.literal(kv.getValue()).withStyle(ChatFormatting.WHITE)));
            }
        }

        String encodedFacets = entry.meta(SearchNodeKeys.FACETS, "");
        var facets = FacetCodec.decode(encodedFacets);
        if (!facets.isEmpty()) {
            lines.add(Component.empty());
            lines.add(Component.translatable("ami.debug.facets").withStyle(ChatFormatting.GOLD)
                    .append(Component.literal(" (" + facets.size() + ")").withStyle(ChatFormatting.DARK_GRAY)));
            for (var facet : facets) {
                lines.add(Component.empty()
                        .append(Component.literal("  "))
                        .append(Component.literal(facet.id()).withStyle(ChatFormatting.GRAY)));
            }
        }

        // Tags
        String tags = entry.meta(SearchNodeKeys.TAGS, "");
        if (!tags.isEmpty()) {
            String[] tagArr = tags.split(",");
            lines.add(Component.empty());
            lines.add(Component.translatable("ami.debug.tags").withStyle(ChatFormatting.GOLD)
                    .append(Component.literal(" (" + tagArr.length + ")").withStyle(ChatFormatting.DARK_GRAY)));
            int shown = Math.min(tagArr.length, MAX_TAGS_SHOWN);
            for (int i = 0; i < shown; i++) {
                lines.add(Component.empty()
                        .append(Component.literal("  "))
                        .append(Component.literal(tagArr[i].trim()).withStyle(ChatFormatting.GRAY)));
            }
            if (tagArr.length > MAX_TAGS_SHOWN) {
                int remaining = tagArr.length - MAX_TAGS_SHOWN;
                lines.add(Component.literal("  ")
                        .append(Component.translatable("ami.debug.more", remaining).withStyle(ChatFormatting.DARK_GRAY)));
            }
        } else {
            lines.add(Component.empty());
            lines.add(Component.translatable("ami.debug.no_tags").withStyle(ChatFormatting.DARK_GRAY));
        }

        // Block tags (for BlockItem entries, separate from item tags shown above)
        List<String> blockTags = collectBlockTags(entry);
        if (!blockTags.isEmpty()) {
            lines.add(Component.empty());
            lines.add(Component.translatable("ami.debug.block_tags").withStyle(ChatFormatting.GOLD)
                    .append(Component.literal(" (" + blockTags.size() + ")").withStyle(ChatFormatting.DARK_GRAY)));
            int shown = Math.min(blockTags.size(), MAX_TAGS_SHOWN);
            for (int i = 0; i < shown; i++) {
                lines.add(Component.empty()
                        .append(Component.literal("  "))
                        .append(Component.literal(blockTags.get(i)).withStyle(ChatFormatting.GRAY)));
            }
            if (blockTags.size() > MAX_TAGS_SHOWN) {
                int remaining = blockTags.size() - MAX_TAGS_SHOWN;
                lines.add(Component.literal("  ")
                        .append(Component.translatable("ami.debug.more", remaining).withStyle(ChatFormatting.DARK_GRAY)));
            }
        }

        return lines;
    }

    private static void appendRouting(List<Component> lines, SearchNode entry, String category, String subcategory) {
        String trace = entry.meta(SearchNodeKeys.CLASSIFICATION_TRACE, "");
        String route = entry.meta(SearchNodeKeys.CLASSIFICATION_ROUTE, "");
        String phase = entry.meta(SearchNodeKeys.CLASSIFICATION_ROUTE_PHASE, "");
        String rule = entry.meta(SearchNodeKeys.CLASSIFICATION_ROUTE_RULE, "");
        String mode = entry.meta("classificationMode", "");
        String score = entry.meta("classificationScore", "");
        String evidence = entry.meta("classificationEvidence", "");
        String scores = entry.meta("classificationScores", "");
        String winnerLabel = phase.isBlank() || rule.isBlank()
                ? ""
                : phase + ":" + rule;

        if (trace.isBlank() && route.isBlank() && phase.isBlank() && rule.isBlank()
                && mode.isBlank() && score.isBlank() && evidence.isBlank() && scores.isBlank()) {
            return;
        }

        lines.add(Component.empty());
        lines.add(Component.translatable("ami.debug.routing").withStyle(ChatFormatting.GOLD));
        String destination = category.isBlank() ? subcategory : category + (subcategory.isBlank() ? "" : " / " + subcategory);
        if (!destination.isBlank()) {
            appendDebugLine(lines, "result", destination, ChatFormatting.GREEN);
        }
        if (!winnerLabel.isBlank()) {
            appendDebugLine(lines, "winner", phase + (rule.isBlank() ? "" : ":" + rule), ChatFormatting.WHITE);
        }
        if (!mode.isBlank()) {
            appendDebugLine(lines, "mode", mode, ChatFormatting.WHITE);
        }
        if (!score.isBlank()) {
            appendDebugLine(lines, "score", score, ChatFormatting.WHITE);
        }
        if (!evidence.isBlank()) {
            appendDebugLine(lines, "evidence", evidence, ChatFormatting.WHITE);
        }
        if (!scores.isBlank()) {
            appendDebugLine(lines, "scores", scores, ChatFormatting.WHITE);
        }

        List<String> rawSteps = traceSteps(trace, route);
        rawSteps = dedupeAndCompactTrace(rawSteps, winnerLabel);
        if (rawSteps.isEmpty() && !winnerLabel.isBlank()) {
            rawSteps = List.of("winner:" + winnerLabel);
        }
        int shown = Math.min(rawSteps.size(), MAX_TRACE_LINES_SHOWN);
        for (int i = 0; i < shown; i++) {
            String step = rawSteps.get(i);
            if (!step.isBlank()) {
                lines.add(Component.empty()
                        .append(Component.literal("  "))
                        .append(Component.literal(step).withStyle(traceStyle(step))));
            }
        }
        if (rawSteps.size() > MAX_TRACE_LINES_SHOWN) {
            lines.add(Component.literal("  ")
                            .append(Component.translatable("ami.debug.more", rawSteps.size() - MAX_TRACE_LINES_SHOWN)
                                    .withStyle(ChatFormatting.DARK_GRAY)));
        }
    }

    private static List<String> dedupeAndCompactTrace(List<String> rawSteps, String winnerLabel) {
        if (rawSteps == null || rawSteps.isEmpty()) {
            return List.of();
        }
        String winnerRulePrefix = (winnerLabel == null || winnerLabel.isBlank()) ? "" : winnerLabel;
        List<String> compacted = new java.util.ArrayList<>();
        String previous = null;
        for (String step : rawSteps) {
            if (step == null || step.isBlank()) {
                continue;
            }
            String normalized = step.trim();
            if (!winnerRulePrefix.isBlank()
                    && normalized.startsWith(winnerRulePrefix) && normalized.contains(": matched")) {
                continue;
            }
            if (!normalized.equals(previous)) {
                compacted.add(normalized);
                previous = normalized;
            }
        }
        return compacted;
    }

    static List<String> traceSteps(String trace, String route) {
        String renderedTrace = trace == null || trace.isBlank() ? route : trace;
        if (renderedTrace == null || renderedTrace.isBlank()) {
            return List.of();
        }
        String separator = trace == null || trace.isBlank() ? " -> " : "\\|";
        return java.util.Arrays.stream(renderedTrace.split(separator))
                .map(String::trim)
                .filter(step -> !step.isBlank())
                .toList();
    }

    private static void appendDebugLine(List<Component> lines, String label, String value, ChatFormatting valueStyle) {
        lines.add(Component.empty()
                .append(Component.literal("  ").withStyle(ChatFormatting.GRAY))
                .append(Component.literal(label).withStyle(ChatFormatting.GRAY))
                .append(Component.literal(" "))
                .append(Component.literal(value).withStyle(valueStyle)));
    }

    private static ChatFormatting traceStyle(String step) {
        if (step.contains(": matched")) {
            return ChatFormatting.GREEN;
        }
        if (step.contains(": skip")) {
            return ChatFormatting.DARK_GRAY;
        }
        if (step.startsWith("facts[")) {
            return ChatFormatting.AQUA;
        }
        return ChatFormatting.GRAY;
    }

    static boolean isRenderedElsewhere(String key) {
        return key.equals(SearchNodeKeys.TAGS)
                || key.equals(SearchNodeKeys.BLOCK_TAGS)
                || key.equals(SearchNodeKeys.FACETS)
                || key.equals(SearchNodeKeys.ONTOLOGY_CATEGORY)
                || key.equals(SearchNodeKeys.ONTOLOGY_SUBCATEGORY)
                || key.equals(SearchNodeKeys.CLASSIFICATION_ROUTE)
                || key.equals(SearchNodeKeys.CLASSIFICATION_TRACE)
                || key.equals(SearchNodeKeys.CLASSIFICATION_ROUTE_PHASE)
                || key.equals(SearchNodeKeys.CLASSIFICATION_ROUTE_RULE)
                || key.equals("classificationMode")
                || key.equals("classificationScore")
                || key.equals("classificationEvidence")
                || key.equals("classificationScores");
    }

    private static List<String> collectBlockTags(SearchNode entry) {
        String indexedBlockTags = entry.meta(SearchNodeKeys.BLOCK_TAGS, "");
        if (!indexedBlockTags.isBlank()) {
            return parseCsvTags(indexedBlockTags);
        }

        Item item = BuiltInRegistries.ITEM.get(entry.id());
        if (!(item instanceof BlockItem blockItem)) {
            return List.of();
        }

        @SuppressWarnings("deprecation")
        var holder = blockItem.getBlock().builtInRegistryHolder();
        return holder.tags()
                .map(tagKey -> {
                    ResourceLocation loc = tagKey.location();
                    return "#" + loc.getNamespace() + ":" + loc.getPath();
                })
                .sorted()
                .toList();
    }

    private static List<String> parseCsvTags(String tags) {
        if (tags == null || tags.isBlank()) {
            return List.of();
        }
        return java.util.Arrays.stream(tags.split(","))
                .map(String::trim)
                .filter(tag -> !tag.isBlank())
                .map(tag -> tag.startsWith("#") ? tag : "#" + tag)
                .distinct()
                .sorted()
                .toList();
    }
}
