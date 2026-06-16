package com.sanhiruzu.ami.index;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.minecraft.resources.Identifier;
import org.junit.jupiter.api.Test;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertTrue;

class ClassificationGoldSetEvaluationTest {
    private static final String GOLD_SET_RESOURCE = "ami/classification_gold_set.jsonl";
    private static final String VANILLA_GOLD_SET_RESOURCE = "ami/vanilla_classification_gold_set.jsonl";

    private static Evaluation evaluate(List<GoldLabel> labels, List<SearchNode> nodes) {
        Map<Identifier, SearchNode> byId = nodes.stream()
                .filter(node -> node.type() == NodeType.ITEM)
                .collect(Collectors.toMap(SearchNode::id, node -> node, (first, second) -> first, LinkedHashMap::new));

        List<Missing> missing = new ArrayList<>();
        List<Mismatch> mismatches = new ArrayList<>();
        List<Match> matches = new ArrayList<>();
        Map<String, Integer> confusion = new TreeMap<>();
        Map<String, LabelStats> statsByExpected = new TreeMap<>();
        Set<String> expectedKeys = labels.stream()
                .map(label -> label.category() + "/" + label.subcategory())
                .collect(Collectors.toSet());

        for (GoldLabel label : labels) {
            SearchNode node = byId.get(label.id());
            if (node == null) {
                missing.add(new Missing(label));
                continue;
            }

            String actualCategory = node.meta(SearchNodeKeys.ONTOLOGY_CATEGORY, "misc");
            String actualSubcategory = node.meta(SearchNodeKeys.ONTOLOGY_SUBCATEGORY, "unknown");
            String expectedKey = label.category() + "/" + label.subcategory();
            String actualKey = actualCategory + "/" + actualSubcategory;
            statsByExpected.computeIfAbsent(expectedKey, ignored -> new LabelStats()).seen++;
            if (expectedKeys.contains(actualKey)) {
                statsByExpected.computeIfAbsent(actualKey, ignored -> new LabelStats()).predicted++;
            }

            if (expectedKey.equals(actualKey)) {
                matches.add(new Match(label, node));
                statsByExpected.get(expectedKey).truePositive++;
            } else {
                mismatches.add(new Mismatch(label, node, actualCategory, actualSubcategory));
                confusion.merge(expectedKey + " -> " + actualKey, 1, Integer::sum);
            }
        }

        return new Evaluation(labels.size(), matches, mismatches, missing, confusion, statsByExpected);
    }

    private static List<GoldLabel> readGoldLabels(String resource) throws IOException {
        var stream = ClassificationGoldSetEvaluationTest.class.getClassLoader().getResourceAsStream(resource);
        if (stream == null) {
            throw new IOException("Missing test resource: " + resource);
        }

        List<GoldLabel> labels = new ArrayList<>();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(stream, StandardCharsets.UTF_8))) {
            String line;
            int lineNumber = 0;
            while ((line = reader.readLine()) != null) {
                lineNumber++;
                if (line.isBlank()) continue;
                JsonObject object = JsonParser.parseString(line).getAsJsonObject();
                Identifier id = Identifier.tryParse(required(object, "id", lineNumber));
                if (id == null) {
                    throw new IOException("Invalid id in gold set line " + lineNumber + ": " + object.get("id"));
                }
                labels.add(new GoldLabel(
                        id,
                        required(object, "category", lineNumber),
                        required(object, "subcategory", lineNumber),
                        optional(object, "confidence").isBlank() ? "certain" : optional(object, "confidence"),
                        optional(object, "notes")
                ));
            }
        }
        return labels;
    }

    private static String required(JsonObject object, String key, int lineNumber) throws IOException {
        if (!object.has(key) || object.get(key).getAsString().isBlank()) {
            throw new IOException("Missing `" + key + "` in gold set line " + lineNumber);
        }
        return object.get(key).getAsString();
    }

    private static String optional(JsonObject object, String key) {
        if (!object.has(key)) return "";
        return object.get(key).getAsString();
    }

    private static Path locateDump() {
        String configured = System.getProperty("ami.searchNodesDump");
        if (configured == null || configured.isBlank()) {
            configured = System.getenv("AMI_SEARCH_NODES_DUMP");
        }
        if (configured != null && !configured.isBlank()) {
            return Path.of(configured);
        }

        Path deceasedCraft = Path.of(
                System.getProperty("user.home"),
                "AppData", "Roaming", "PrismLauncher", "instances",
                "DeceasedCraft - Urban Zombie Apocalypse",
                "minecraft", "ami_dumps", "search", "search_nodes.jsonl");
        if (Files.exists(deceasedCraft)) {
            return deceasedCraft;
        }

        return repoRoot().resolve(Path.of("run", "neoforge-emi", "ami_dumps", "search", "search_nodes.jsonl"));
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
    void writesClassificationGoldSetReportWhenSearchNodeDumpExists() throws IOException {
        writeClassificationGoldSetReport(GOLD_SET_RESOURCE, "gold-set.md");
    }

    @Test
    void writesVanillaClassificationGoldSetReportWhenSearchNodeDumpExists() throws IOException {
        writeClassificationGoldSetReport(VANILLA_GOLD_SET_RESOURCE, "vanilla-gold-set.md");
    }

    private static void writeClassificationGoldSetReport(String resource, String reportName) throws IOException {
        Path reportPath = repoRoot().resolve(Path.of(
                "neoforge", "build", "reports", "ami-classification", reportName));
        Files.createDirectories(reportPath.getParent());

        List<GoldLabel> labels = readGoldLabels(resource);
        Path dumpPath = locateDump();
        if (!Files.exists(dumpPath)) {
            Files.writeString(reportPath, "# AMI Classification Gold Set\n\n"
                    + "No runtime search-node dump found at `" + dumpPath + "`.\n\n"
                    + "Generate one with `/ami dump-search-nodes`, or set `AMI_SEARCH_NODES_DUMP` / "
                    + "`-Dami.searchNodesDump=...`.\n\n"
                    + "Gold labels available: " + labels.size() + "\n");
            assertTrue(Files.exists(reportPath), "Expected classification report at " + reportPath.toAbsolutePath());
            return;
        }

        List<SearchNode> nodes = SearchNodeMirrorDump.reclassifyItemOntology(SearchNodeMirrorDump.readJsonl(dumpPath));
        Evaluation evaluation = evaluate(labels, nodes);
        Files.writeString(reportPath, evaluation.toMarkdown(dumpPath));

        if (Boolean.getBoolean("ami.classificationGoldStrict")) {
            assertTrue(evaluation.mismatches().isEmpty(), () ->
                    "Classification gold-set mismatches. See " + reportPath.toAbsolutePath());
            assertTrue(evaluation.missing().isEmpty(), () ->
                    "Classification gold-set labels missing from dump. See " + reportPath.toAbsolutePath());
        }

        assertTrue(Files.exists(reportPath), "Expected classification report at " + reportPath.toAbsolutePath());
    }

    private record GoldLabel(Identifier id, String category, String subcategory, String confidence, String notes) {
    }

    private record Match(GoldLabel expected, SearchNode actual) {
    }

    private record Missing(GoldLabel expected) {
    }

    private record Mismatch(GoldLabel expected, SearchNode actual, String actualCategory, String actualSubcategory) {
    }

    private static final class LabelStats {
        int seen;
        int predicted;
        int truePositive;

        double precision() {
            return predicted == 0 ? 0.0D : (double) truePositive / predicted;
        }

        double recall() {
            return seen == 0 ? 0.0D : (double) truePositive / seen;
        }

        double f1() {
            double precision = precision();
            double recall = recall();
            return precision + recall == 0.0D ? 0.0D : 2.0D * precision * recall / (precision + recall);
        }
    }

    private record Evaluation(
            int totalLabels,
            List<Match> matches,
            List<Mismatch> mismatches,
            List<Missing> missing,
            Map<String, Integer> confusion,
            Map<String, LabelStats> statsByExpected
    ) {
        private static String evidence(SearchNode node) {
            List<String> parts = new ArrayList<>();
            addEvidence(parts, "facets", node.meta(SearchNodeKeys.FACETS, ""));
            addEvidence(parts, "tags", node.meta(SearchNodeKeys.TAGS, ""));
            addEvidence(parts, "blockTags", node.meta(SearchNodeKeys.BLOCK_TAGS, ""));
            addEvidence(parts, "blockShape", node.meta("blockShape", ""));
            addEvidence(parts, "blockClass", node.meta(SearchNodeKeys.BLOCK_CLASS, ""));
            addEvidence(parts, "stateProps", node.meta(SearchNodeKeys.BLOCK_STATE_PROPERTIES, ""));
            addEvidence(parts, "route", winningRoute(node));
            addEvidence(parts, "recipes", node.meta(SearchNodeKeys.RECIPE_CATEGORIES, ""));
            addEvidence(parts, "uses", node.meta(SearchNodeKeys.RECIPE_USE_CATEGORIES, ""));
            if (parts.isEmpty()) return "";
            return " [" + String.join("; ", parts) + "]";
        }

        private static String winningRoute(SearchNode node) {
            String phase = node.meta(SearchNodeKeys.CLASSIFICATION_ROUTE_PHASE, "");
            String rule = node.meta(SearchNodeKeys.CLASSIFICATION_ROUTE_RULE, "");
            if (phase.isBlank()) {
                return rule;
            }
            return rule.isBlank() ? phase : phase + ":" + rule;
        }

        private static void addEvidence(List<String> parts, String label, String value) {
            if (value == null || value.isBlank()) return;
            parts.add(label + "=" + value);
        }

        private static String format(double value) {
            return String.format(java.util.Locale.ROOT, "%.3f", value);
        }

        String toMarkdown(Path dumpPath) {
            int evaluated = matches.size() + mismatches.size();
            double accuracy = evaluated == 0 ? 0.0D : (double) matches.size() / evaluated;
            double macroF1 = statsByExpected.values().stream()
                    .mapToDouble(LabelStats::f1)
                    .average()
                    .orElse(0.0D);

            StringBuilder out = new StringBuilder();
            out.append("# AMI Classification Gold Set\n\n");
            out.append("Source dump: `").append(dumpPath).append("`\n\n");
            out.append("- Labels: ").append(totalLabels).append("\n");
            appendConfidenceSummary(out);
            out.append("- Evaluated: ").append(evaluated).append("\n");
            out.append("- Missing from dump: ").append(missing.size()).append("\n");
            out.append("- Correct: ").append(matches.size()).append("\n");
            out.append("- Mismatched: ").append(mismatches.size()).append("\n");
            out.append("- Accuracy: ").append(format(accuracy)).append("\n");
            out.append("- Macro F1: ").append(format(macroF1)).append("\n\n");

            appendConfusion(out);
            appendMismatches(out);
            appendMissing(out);
            return out.toString();
        }

        private void appendConfusion(StringBuilder out) {
            out.append("## Confusion\n\n");
            if (confusion.isEmpty()) {
                out.append("No mismatched labels.\n\n");
                return;
            }
            confusion.entrySet().stream()
                    .sorted(Map.Entry.<String, Integer>comparingByValue(Comparator.reverseOrder()))
                    .forEach(entry -> out.append("- ")
                            .append(entry.getKey())
                            .append(": ")
                            .append(entry.getValue())
                            .append("\n"));
            out.append("\n");
        }

        private void appendMismatches(StringBuilder out) {
            out.append("## Mismatches\n\n");
            if (mismatches.isEmpty()) {
                out.append("No mismatches.\n\n");
                return;
            }
            for (Mismatch mismatch : mismatches) {
                GoldLabel expected = mismatch.expected();
                SearchNode actual = mismatch.actual();
                out.append("- `").append(expected.id()).append("` ")
                        .append(actual.displayName())
                        .append(": expected ")
                        .append(expected.category()).append("/").append(expected.subcategory())
                        .append(", got ")
                        .append(mismatch.actualCategory()).append("/").append(mismatch.actualSubcategory())
                        .append(" (").append(expected.confidence()).append(")")
                        .append(evidence(actual))
                        .append("\n");
                if (!expected.notes().isBlank()) {
                    out.append("  - notes: ").append(expected.notes()).append("\n");
                }
                appendTrace(out, actual);
            }
            out.append("\n");
        }

        private static void appendTrace(StringBuilder out, SearchNode node) {
            List<String> steps = traceSteps(node);
            if (steps.isEmpty()) {
                return;
            }
            out.append("  - trace:\n");
            int maxSteps = Math.min(steps.size(), 80);
            for (int i = 0; i < maxSteps; i++) {
                out.append("    - ").append(escapeMarkdownListText(steps.get(i))).append("\n");
            }
            if (steps.size() > maxSteps) {
                out.append("    - +").append(steps.size() - maxSteps).append(" more trace steps\n");
            }
        }

        private static List<String> traceSteps(SearchNode node) {
            String trace = node.meta(SearchNodeKeys.CLASSIFICATION_TRACE, "");
            if (!trace.isBlank()) {
                return splitTrace(trace, "\\|");
            }
            String route = node.meta(SearchNodeKeys.CLASSIFICATION_ROUTE, "");
            if (!route.isBlank()) {
                return splitTrace(route, " -> ");
            }
            return List.of();
        }

        private static List<String> splitTrace(String value, String separatorRegex) {
            return Arrays.stream(value.split(separatorRegex))
                    .map(String::trim)
                    .filter(step -> !step.isBlank())
                    .toList();
        }

        private static String escapeMarkdownListText(String value) {
            return value.replace("\\", "\\\\");
        }

        private void appendMissing(StringBuilder out) {
            out.append("## Missing Labels\n\n");
            if (missing.isEmpty()) {
                out.append("No labels missing from dump.\n\n");
                return;
            }
            for (Missing missingLabel : missing) {
                out.append("- `").append(missingLabel.expected().id()).append("`: expected ")
                        .append(missingLabel.expected().category()).append("/")
                        .append(missingLabel.expected().subcategory())
                        .append(" (").append(missingLabel.expected().confidence()).append(")")
                        .append("\n");
            }
            out.append("\n");
        }

        private void appendConfidenceSummary(StringBuilder out) {
            Map<String, Long> counts = new TreeMap<>();
            for (Match match : matches) {
                counts.merge(match.expected().confidence(), 1L, Long::sum);
            }
            for (Mismatch mismatch : mismatches) {
                counts.merge(mismatch.expected().confidence(), 1L, Long::sum);
            }
            for (Missing missingLabel : missing) {
                counts.merge(missingLabel.expected().confidence(), 1L, Long::sum);
            }
            for (Map.Entry<String, Long> entry : counts.entrySet()) {
                out.append("- Labels `").append(entry.getKey()).append("`: ").append(entry.getValue()).append("\n");
            }
        }
    }
}
