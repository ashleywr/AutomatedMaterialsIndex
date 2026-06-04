package com.sanhiruzu.ami.index;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertTrue;

class ClassificationReplayDiffReportTest {
    private static Path locateDump() {
        String configured = System.getProperty("ami.searchNodesDump");
        if (configured == null || configured.isBlank()) {
            configured = System.getenv("AMI_SEARCH_NODES_DUMP");
        }
        if (configured != null && !configured.isBlank()) {
            return Path.of(configured);
        }
        return repoRoot().resolve(Path.of("run", "neoforge-emi", "ami_dumps", "search_nodes.jsonl"));
    }

    private static Path repoRoot() {
        Path current = Path.of("").toAbsolutePath();
        while (current != null) {
            if ((Files.exists(current.resolve("settings.gradle")) && Files.exists(current.resolve("gradle.properties")))) {
                return current;
            }
            current = current.getParent();
        }
        throw new IllegalStateException("Could not locate repository root");
    }

    @Test
    void writesClassificationReplayDiffReportWhenSearchNodeDumpExists() throws IOException {
        Path dump = locateDump();
        Path reportPath = repoRoot().resolve(Path.of(
                "neoforge", "build", "reports", "ami-classification", "replay-diff.md"));
        Files.createDirectories(reportPath.getParent());

        if (!Files.exists(dump)) {
            Files.writeString(reportPath, "# AMI Classification Replay Diff\n\n"
                    + "No runtime search-node dump found at `" + dump + "`.\n");
            assertTrue(Files.exists(reportPath));
            return;
        }

        List<SearchNode> runtime = SearchNodeMirrorDump.readJsonl(dump).stream()
                .filter(node -> node.type() == NodeType.ITEM)
                .toList();
        List<SearchNode> replay = SearchNodeMirrorDump.reclassifyItemOntology(runtime);

        Map<ResourceKey, SearchNode> replayByKey = replay.stream()
                .collect(Collectors.toMap(ResourceKey::from, node -> node, (first, second) -> first, LinkedHashMap::new));
        List<Pair> pairs = new ArrayList<>();
        for (SearchNode oldNode : runtime) {
            SearchNode newNode = replayByKey.get(ResourceKey.from(oldNode));
            if (newNode != null) {
                pairs.add(new Pair(oldNode, newNode));
            }
        }

        StringBuilder report = new StringBuilder();
        report.append("# AMI Classification Replay Diff\n\n");
        report.append("Source dump: `").append(dump).append("`\n\n");
        appendSummary(report, pairs);
        appendCounts(report, "Runtime Categories", categoryCounts(pairs, true), 40);
        appendCounts(report, "Replay Categories", categoryCounts(pairs, false), 40);
        appendCounts(report, "Runtime Route Rules", routeCounts(pairs, true), 40);
        appendCounts(report, "Replay Route Rules", routeCounts(pairs, false), 40);
        appendCounts(report, "Old Remaining Placeables Replayed To", oldRemainingPlaceablesDestinations(pairs), 80);
        appendCounts(report, "Replay Remaining Placeables By Mod And Block Class", replayRemainingPlaceablesByModAndClass(pairs), 80);
        appendExamples(report, "Old Remaining Placeables Changed", changedOldRemainingPlaceables(pairs), 120);
        appendCounts(report, "Old Masonry Full Blocks Replayed To", oldMasonryFullBlockDestinations(pairs), 80);
        appendCounts(report, "Replay Masonry Full Blocks By Mod And Block Class", replayMasonryFullBlocksByModAndClass(pairs), 80);
        appendExamples(report, "Old Masonry Full Blocks Changed", changedOldMasonryFullBlocks(pairs), 120);

        Files.writeString(reportPath, report.toString());
        assertTrue(Files.exists(reportPath), "Expected replay diff report at " + reportPath.toAbsolutePath());
    }

    private static void appendSummary(StringBuilder report, List<Pair> pairs) {
        long categoryChanged = pairs.stream().filter(pair -> !categoryKey(pair.oldNode()).equals(categoryKey(pair.newNode()))).count();
        long routeChanged = pairs.stream().filter(pair -> !routeKey(pair.oldNode()).equals(routeKey(pair.newNode()))).count();
        long oldRemaining = pairs.stream().filter(pair -> "remaining placeables".equals(rule(pair.oldNode()))).count();
        long replayRemaining = pairs.stream().filter(pair -> "remaining placeables".equals(rule(pair.newNode()))).count();
        long oldMasonryFull = pairs.stream().filter(pair -> "masonry/full_block".equals(categoryKey(pair.oldNode()))).count();
        long replayMasonryFull = pairs.stream().filter(pair -> "masonry/full_block".equals(categoryKey(pair.newNode()))).count();

        report.append("- Items compared: ").append(pairs.size()).append("\n");
        report.append("- Category/subcategory changed after replay: ").append(categoryChanged).append("\n");
        report.append("- Route changed after replay: ").append(routeChanged).append("\n");
        report.append("- Runtime `remaining placeables`: ").append(oldRemaining).append("\n");
        report.append("- Replay `remaining placeables`: ").append(replayRemaining).append("\n");
        report.append("- Runtime `masonry/full_block`: ").append(oldMasonryFull).append("\n");
        report.append("- Replay `masonry/full_block`: ").append(replayMasonryFull).append("\n\n");
    }

    private static Map<String, Long> categoryCounts(List<Pair> pairs, boolean old) {
        return countBy(pairs, pair -> categoryKey(old ? pair.oldNode() : pair.newNode()));
    }

    private static Map<String, Long> routeCounts(List<Pair> pairs, boolean old) {
        return countBy(pairs, pair -> routeKey(old ? pair.oldNode() : pair.newNode()));
    }

    private static Map<String, Long> oldRemainingPlaceablesDestinations(List<Pair> pairs) {
        return countBy(pairs.stream()
                        .filter(pair -> "remaining placeables".equals(rule(pair.oldNode())))
                        .toList(),
                pair -> categoryKey(pair.newNode()) + " via " + routeKey(pair.newNode()));
    }

    private static Map<String, Long> oldMasonryFullBlockDestinations(List<Pair> pairs) {
        return countBy(pairs.stream()
                        .filter(pair -> "masonry/full_block".equals(categoryKey(pair.oldNode())))
                        .toList(),
                pair -> categoryKey(pair.newNode()) + " via " + routeKey(pair.newNode()));
    }

    private static Map<String, Long> replayRemainingPlaceablesByModAndClass(List<Pair> pairs) {
        return countBy(pairs.stream()
                        .filter(pair -> "remaining placeables".equals(rule(pair.newNode())))
                        .toList(),
                pair -> modAndClass(pair.newNode()));
    }

    private static Map<String, Long> replayMasonryFullBlocksByModAndClass(List<Pair> pairs) {
        return countBy(pairs.stream()
                        .filter(pair -> "masonry/full_block".equals(categoryKey(pair.newNode())))
                        .toList(),
                pair -> modAndClass(pair.newNode()));
    }

    private static List<Pair> changedOldRemainingPlaceables(List<Pair> pairs) {
        return pairs.stream()
                .filter(pair -> "remaining placeables".equals(rule(pair.oldNode())))
                .filter(pair -> !categoryKey(pair.oldNode()).equals(categoryKey(pair.newNode()))
                        || !routeKey(pair.oldNode()).equals(routeKey(pair.newNode())))
                .sorted(Comparator.comparing(pair -> pair.oldNode().id().toString()))
                .toList();
    }

    private static List<Pair> changedOldMasonryFullBlocks(List<Pair> pairs) {
        return pairs.stream()
                .filter(pair -> "masonry/full_block".equals(categoryKey(pair.oldNode())))
                .filter(pair -> !categoryKey(pair.oldNode()).equals(categoryKey(pair.newNode()))
                        || !routeKey(pair.oldNode()).equals(routeKey(pair.newNode())))
                .sorted(Comparator.comparing(pair -> pair.oldNode().id().toString()))
                .toList();
    }

    private static Map<String, Long> countBy(List<Pair> pairs, java.util.function.Function<Pair, String> classifier) {
        return pairs.stream().collect(Collectors.groupingBy(classifier, LinkedHashMap::new, Collectors.counting()))
                .entrySet().stream()
                .sorted(Map.Entry.<String, Long>comparingByValue(Comparator.reverseOrder()).thenComparing(Map.Entry.comparingByKey()))
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue, (a, b) -> a, LinkedHashMap::new));
    }

    private static void appendCounts(StringBuilder report, String title, Map<String, Long> counts, int limit) {
        report.append("## ").append(title).append("\n\n");
        if (counts.isEmpty()) {
            report.append("No rows.\n\n");
            return;
        }
        counts.entrySet().stream().limit(limit).forEach(entry ->
                report.append("- `").append(entry.getKey()).append("`: ").append(entry.getValue()).append("\n"));
        if (counts.size() > limit) {
            report.append("- ... ").append(counts.size() - limit).append(" more\n");
        }
        report.append("\n");
    }

    private static void appendExamples(StringBuilder report, String title, List<Pair> pairs, int limit) {
        report.append("## ").append(title).append("\n\n");
        if (pairs.isEmpty()) {
            report.append("No changed rows.\n\n");
            return;
        }
        pairs.stream().limit(limit).forEach(pair -> {
            SearchNode oldNode = pair.oldNode();
            SearchNode newNode = pair.newNode();
            report.append("- `").append(oldNode.id()).append("` ")
                    .append(oldNode.displayName()).append(": ")
                    .append(categoryKey(oldNode)).append(" / ").append(routeKey(oldNode))
                    .append(" -> ")
                    .append(categoryKey(newNode)).append(" / ").append(routeKey(newNode))
                    .append(" [").append(evidence(newNode)).append("]\n");
        });
        if (pairs.size() > limit) {
            report.append("- ... ").append(pairs.size() - limit).append(" more\n");
        }
        report.append("\n");
    }

    private static String evidence(SearchNode node) {
        List<String> parts = new ArrayList<>();
        addEvidence(parts, "facets", node.meta(SearchNodeKeys.FACETS, ""));
        addEvidence(parts, "shape", node.meta("blockShape", ""));
        addEvidence(parts, "blockClass", node.meta(SearchNodeKeys.BLOCK_CLASS, ""));
        addEvidence(parts, "props", node.meta(SearchNodeKeys.BLOCK_STATE_PROPERTIES, ""));
        return String.join("; ", parts);
    }

    private static void addEvidence(List<String> parts, String label, String value) {
        if (value != null && !value.isBlank()) {
            parts.add(label + "=" + value);
        }
    }

    private static String categoryKey(SearchNode node) {
        return node.meta(SearchNodeKeys.ONTOLOGY_CATEGORY, "") + "/" + node.meta(SearchNodeKeys.ONTOLOGY_SUBCATEGORY, "");
    }

    private static String routeKey(SearchNode node) {
        String phase = node.meta(SearchNodeKeys.CLASSIFICATION_ROUTE_PHASE, "");
        String rule = rule(node);
        if (phase.isBlank()) {
            return rule;
        }
        return rule.isBlank() ? phase : phase + ":" + rule;
    }

    private static String rule(SearchNode node) {
        return node.meta(SearchNodeKeys.CLASSIFICATION_ROUTE_RULE, "");
    }

    private static String modAndClass(SearchNode node) {
        String blockClass = node.meta(SearchNodeKeys.BLOCK_CLASS, "");
        if (blockClass.isBlank()) {
            blockClass = node.meta(SearchNodeKeys.ITEM_CLASS, "");
        }
        return node.id().getNamespace() + " / " + simpleClassName(blockClass);
    }

    private static String simpleClassName(String className) {
        if (className == null || className.isBlank()) {
            return "(no class)";
        }
        int lastDot = className.lastIndexOf('.');
        return lastDot >= 0 && lastDot + 1 < className.length() ? className.substring(lastDot + 1) : className;
    }

    private record Pair(SearchNode oldNode, SearchNode newNode) {
    }

    private record ResourceKey(String id, NodeType type) {
        static ResourceKey from(SearchNode node) {
            return new ResourceKey(node.id().toString(), node.type());
        }
    }
}
