package com.sanhiruzu.ami.client.results;

import com.sanhiruzu.ami.index.NodeType;
import com.sanhiruzu.ami.index.SearchNode;
import com.sanhiruzu.ami.index.SearchNodeKeys;
import com.sanhiruzu.ami.index.SearchNodeMirrorDump;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertTrue;

public class RuntimeMirrorLexicalEvidenceReportTest {
    private static final List<Path> DEFAULT_DUMPS = List.of(
            Path.of("C:", "Users", "ashle", "AppData", "Roaming", "PrismLauncher", "instances", "Society- Sunlit Valley", "minecraft", "ami_dumps", "search", "search_nodes.jsonl"),
            Path.of("C:", "Users", "ashle", "AppData", "Roaming", "PrismLauncher", "instances", "Ashley Modpack", "minecraft", "ami_dumps", "search", "search_nodes.jsonl"),
            Path.of("C:", "Users", "ashle", "AppData", "Roaming", "PrismLauncher", "instances", "DeceasedCraft - Urban Zombie Apocalypse", "minecraft", "ami_dumps", "search", "search_nodes.jsonl")
    );
    private static final Set<String> STOP_WORDS = Set.of(
            "a", "an", "and", "block", "blocks", "item", "items", "of", "the", "with",
            "white", "orange", "magenta", "light", "blue", "yellow", "lime", "pink", "gray",
            "grey", "cyan", "purple", "brown", "green", "red", "black", "dark"
    );

    private static List<Path> configuredDumps() {
        String configured = System.getProperty("ami.lexicalEvidenceDumps");
        if (configured == null || configured.isBlank()) {
            configured = System.getenv("AMI_LEXICAL_EVIDENCE_DUMPS");
        }
        if (configured == null || configured.isBlank()) {
            configured = System.getProperty("ami.searchNodesDump");
        }
        if (configured == null || configured.isBlank()) {
            configured = System.getenv("AMI_SEARCH_NODES_DUMP");
        }
        if (configured == null || configured.isBlank()) {
            return DEFAULT_DUMPS;
        }
        List<Path> paths = new ArrayList<>();
        for (String raw : configured.split(java.io.File.pathSeparator)) {
            if (!raw.isBlank()) {
                paths.add(Path.of(raw));
            }
        }
        return paths;
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
    void writesLexicalEvidenceReportForAvailableRuntimeDumps() throws IOException {
        Path reportPath = repoRoot().resolve(Path.of("neoforge", "build", "reports", "ami-result-shapes", "lexical-evidence.md"));
        Files.createDirectories(reportPath.getParent());

        StringBuilder report = new StringBuilder();
        report.append("# AMI Lexical Evidence Report\n\n");
        report.append("Token lift/TF-IDF style audit over runtime dumps. Use this to seed a curated token-to-facet dictionary; do not copy tokens blindly into resolver rules.\n\n");

        int dumpsRead = 0;
        for (Path dump : configuredDumps()) {
            if (!Files.exists(dump)) {
                report.append("## Missing Dump\n\n`").append(dump).append("`\n\n");
                continue;
            }
            dumpsRead++;
            List<SearchNode> nodes = SearchNodeMirrorDump.reclassifyItemOntology(SearchNodeMirrorDump.readJsonl(dump));
            Corpus corpus = Corpus.from(nodes);

            report.append("## ").append(dump.getParent().getParent().getFileName()).append("\n\n");
            report.append("- Source: `").append(dump).append("`\n");
            report.append("- Item nodes: ").append(corpus.totalItems).append("\n");
            report.append("- Buckets: ").append(corpus.bucketCounts.size()).append("\n\n");

            appendBucketTerms(report, corpus);
            appendAmbiguousTerms(report, corpus);
        }

        if (dumpsRead == 0) {
            report.append("No configured runtime dumps were found on this machine.\n");
        }

        Files.writeString(reportPath, report.toString());
        assertTrue(Files.exists(reportPath), "Expected diagnostic report at " + reportPath.toAbsolutePath());
    }

    private static void appendBucketTerms(StringBuilder report, Corpus corpus) {
        report.append("### Strong Terms By Bucket\n\n");
        corpus.bucketCounts.entrySet().stream()
                .sorted(Map.Entry.<String, Integer>comparingByValue().reversed())
                .limit(40)
                .forEach(bucket -> {
                    List<TokenScore> scores = corpus.topTokensForBucket(bucket.getKey(), 12);
                    if (scores.isEmpty()) {
                        return;
                    }
                    report.append("#### `").append(bucket.getKey()).append("` (").append(bucket.getValue()).append(" items)\n\n");
                    for (TokenScore score : scores) {
                        report.append("- `").append(score.token()).append("` score=")
                                .append(format(score.score()))
                                .append(" bucketDf=").append(score.bucketDf())
                                .append(" globalDf=").append(score.globalDf())
                                .append(" examples=").append(String.join("; ", score.examples()))
                                .append("\n");
                    }
                    report.append("\n");
                });
    }

    private static void appendAmbiguousTerms(StringBuilder report, Corpus corpus) {
        report.append("### Ambiguous High-Frequency Terms\n\n");
        List<AmbiguousToken> ambiguous = corpus.ambiguousTokens();
        if (ambiguous.isEmpty()) {
            report.append("No ambiguous terms found.\n\n");
            return;
        }
        ambiguous.stream().limit(80).forEach(token -> {
            report.append("- `").append(token.token()).append("` globalDf=").append(token.globalDf()).append("\n");
            token.bucketCounts().entrySet().stream()
                    .sorted(Map.Entry.<String, Integer>comparingByValue().reversed())
                    .limit(6)
                    .forEach(entry -> report.append("  - ").append(entry.getKey()).append(": ").append(entry.getValue()).append("\n"));
        });
        report.append("\n");
    }

    private static Set<String> tokens(SearchNode node) {
        Set<String> tokens = new HashSet<>();
        addTokens(tokens, node.id().getPath());
        addTokens(tokens, node.displayName());
        return tokens;
    }

    private static void addTokens(Set<String> out, String value) {
        for (String raw : value.toLowerCase(Locale.ROOT).split("[^a-z0-9]+")) {
            if (raw.length() < 3 || STOP_WORDS.contains(raw)) {
                continue;
            }
            if (raw.chars().allMatch(Character::isDigit)) {
                continue;
            }
            out.add(raw);
        }
    }

    private static String bucket(SearchNode node) {
        return node.meta(SearchNodeKeys.ONTOLOGY_CATEGORY, "misc")
                + "/"
                + node.meta(SearchNodeKeys.ONTOLOGY_SUBCATEGORY, "");
    }

    private static String format(double value) {
        return String.format(Locale.ROOT, "%.2f", value);
    }

    private record Corpus(
            int totalItems,
            Map<String, Integer> bucketCounts,
            Map<String, Integer> tokenDocumentFrequency,
            Map<String, Map<String, Integer>> tokenBucketFrequency,
            Map<String, Map<String, List<String>>> tokenBucketExamples
    ) {
        static Corpus from(List<SearchNode> nodes) {
            int total = 0;
            Map<String, Integer> bucketCounts = new LinkedHashMap<>();
            Map<String, Integer> tokenDocumentFrequency = new HashMap<>();
            Map<String, Map<String, Integer>> tokenBucketFrequency = new HashMap<>();
            Map<String, Map<String, List<String>>> examples = new HashMap<>();

            for (SearchNode node : nodes) {
                if (node.type() != NodeType.ITEM) {
                    continue;
                }
                total++;
                String bucket = bucket(node);
                bucketCounts.merge(bucket, 1, Integer::sum);
                for (String token : tokens(node)) {
                    tokenDocumentFrequency.merge(token, 1, Integer::sum);
                    tokenBucketFrequency.computeIfAbsent(token, ignored -> new HashMap<>()).merge(bucket, 1, Integer::sum);
                    examples.computeIfAbsent(token, ignored -> new HashMap<>())
                            .computeIfAbsent(bucket, ignored -> new ArrayList<>());
                    List<String> bucketExamples = examples.get(token).get(bucket);
                    if (bucketExamples.size() < 3) {
                        bucketExamples.add(node.displayName() + " [" + node.id() + "]");
                    }
                }
            }

            return new Corpus(total, bucketCounts, tokenDocumentFrequency, tokenBucketFrequency, examples);
        }

        List<TokenScore> topTokensForBucket(String bucket, int limit) {
            int bucketSize = bucketCounts.getOrDefault(bucket, 0);
            if (bucketSize == 0) {
                return List.of();
            }
            List<TokenScore> scores = new ArrayList<>();
            for (var tokenEntry : tokenBucketFrequency.entrySet()) {
                int bucketDf = tokenEntry.getValue().getOrDefault(bucket, 0);
                int globalDf = tokenDocumentFrequency.getOrDefault(tokenEntry.getKey(), 0);
                if (bucketDf < 3 || globalDf < 3) {
                    continue;
                }
                double precision = bucketDf / (double) globalDf;
                double recall = bucketDf / (double) bucketSize;
                double idf = Math.log((totalItems + 1.0) / (globalDf + 1.0));
                double score = precision * Math.sqrt(recall) * idf;
                if (score < 0.10 || precision < 0.35) {
                    continue;
                }
                List<String> examples = tokenBucketExamples
                        .getOrDefault(tokenEntry.getKey(), Map.of())
                        .getOrDefault(bucket, List.of());
                scores.add(new TokenScore(tokenEntry.getKey(), score, bucketDf, globalDf, examples));
            }
            scores.sort(Comparator.comparingDouble(TokenScore::score).reversed().thenComparing(TokenScore::token));
            return scores.stream().limit(limit).toList();
        }

        List<AmbiguousToken> ambiguousTokens() {
            List<AmbiguousToken> rows = new ArrayList<>();
            for (var tokenEntry : tokenBucketFrequency.entrySet()) {
                int globalDf = tokenDocumentFrequency.getOrDefault(tokenEntry.getKey(), 0);
                if (globalDf < 20 || tokenEntry.getValue().size() < 4) {
                    continue;
                }
                int top = tokenEntry.getValue().values().stream().mapToInt(Integer::intValue).max().orElse(0);
                if (top / (double) globalDf > 0.70) {
                    continue;
                }
                rows.add(new AmbiguousToken(tokenEntry.getKey(), globalDf, tokenEntry.getValue()));
            }
            rows.sort(Comparator.comparingInt(AmbiguousToken::globalDf).reversed().thenComparing(AmbiguousToken::token));
            return rows;
        }
    }

    private record TokenScore(String token, double score, int bucketDf, int globalDf, List<String> examples) {
    }

    private record AmbiguousToken(String token, int globalDf, Map<String, Integer> bucketCounts) {
    }
}
