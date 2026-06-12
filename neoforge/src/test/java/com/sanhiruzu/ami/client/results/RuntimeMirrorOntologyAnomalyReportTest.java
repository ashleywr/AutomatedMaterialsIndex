package com.sanhiruzu.ami.client.results;

import com.sanhiruzu.ami.index.*;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import static org.junit.jupiter.api.Assertions.assertTrue;

public class RuntimeMirrorOntologyAnomalyReportTest {
    private static final List<Path> DEFAULT_DUMPS = List.of(
            Path.of("C:", "Users", "ashle", "AppData", "Roaming", "PrismLauncher", "instances", "Society- Sunlit Valley", "minecraft", "ami_dumps", "search", "search_nodes.jsonl"),
            Path.of("C:", "Users", "ashle", "AppData", "Roaming", "PrismLauncher", "instances", "Ashley Modpack", "minecraft", "ami_dumps", "search", "search_nodes.jsonl"),
            Path.of("C:", "Users", "ashle", "AppData", "Roaming", "PrismLauncher", "instances", "DeceasedCraft - Urban Zombie Apocalypse", "minecraft", "ami_dumps", "search", "search_nodes.jsonl")
    );

    private static List<Path> configuredDumps() {
        String configured = System.getProperty("ami.ontologyAuditDumps");
        if (configured == null || configured.isBlank()) {
            configured = System.getenv("AMI_ONTOLOGY_AUDIT_DUMPS");
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
    void writesOntologyAnomalyReportForAvailableRuntimeDumps() throws IOException {
        Path reportPath = repoRoot().resolve(Path.of("neoforge", "build", "reports", "ami-result-shapes", "ontology-anomalies.md"));
        Files.createDirectories(reportPath.getParent());

        StringBuilder report = new StringBuilder();
        report.append("# AMI Ontology Anomaly Report\n\n");
        report.append("Heuristic audit for runtime dumps. `Runtime` is the dumped category; `Replay` is the current resolver reapplied to dumped metadata.\n\n");

        int dumpsRead = 0;
        for (Path dump : configuredDumps()) {
            if (!Files.exists(dump)) {
                report.append("## Missing Dump\n\n`").append(dump).append("`\n\n");
                continue;
            }
            dumpsRead++;
            List<SearchNode> runtime = SearchNodeMirrorDump.readJsonl(dump);
            List<SearchNode> replayed = SearchNodeMirrorDump.reclassifyItemOntology(runtime);

            List<String> runtimeAnomalies = anomalies(runtime);
            List<String> replayAnomalies = anomalies(replayed);

            report.append("## ").append(dump.getParent().getParent().getFileName()).append("\n\n");
            report.append("- Source: `").append(dump).append("`\n");
            report.append("- Runtime anomalies: ").append(runtimeAnomalies.size()).append("\n");
            report.append("- Remaining after replay: ").append(replayAnomalies.size()).append("\n\n");

            appendExamples(report, "Runtime", runtimeAnomalies);
            appendExamples(report, "Replay", replayAnomalies);
        }

        if (dumpsRead == 0) {
            report.append("No configured runtime dumps were found on this machine.\n");
        }

        Files.writeString(reportPath, report.toString());
        assertTrue(Files.exists(reportPath), "Expected diagnostic report at " + reportPath.toAbsolutePath());
    }

    private static void appendExamples(StringBuilder report, String title, List<String> rows) {
        report.append("### ").append(title).append("\n\n");
        if (rows.isEmpty()) {
            report.append("No anomalies found.\n\n");
            return;
        }
        rows.stream().limit(200).forEach(row -> report.append("- ").append(row).append("\n"));
        if (rows.size() > 200) {
            report.append("- ... ").append(rows.size() - 200).append(" more\n");
        }
        report.append("\n");
    }

    private static List<String> anomalies(List<SearchNode> nodes) {
        List<String> rows = new ArrayList<>();
        for (SearchNode node : nodes) {
            if (node.type() != NodeType.ITEM) {
                continue;
            }
            String actual = categoryKey(node);
            for (ExpectedPlacement expected : expectedPlacements(node)) {
                if (!expected.matches(actual)) {
                    rows.add(expected.reason() + ": " + node.displayName() + " [" + node.id() + "] "
                            + "expected " + expected.category() + "/" + expected.subcategory()
                            + ", got " + actual);
                }
            }
        }
        rows.sort(String::compareTo);
        return rows;
    }

    private static List<ExpectedPlacement> expectedPlacements(SearchNode node) {
        List<ExpectedPlacement> expected = new ArrayList<>();
        String path = node.id().getPath().toLowerCase(Locale.ROOT);
        String displayName = node.displayName().toLowerCase(Locale.ROOT);
        String facets = node.meta(SearchNodeKeys.FACETS, "");

        if (isSapling(node)) {
            expected.add(new ExpectedPlacement("sapling evidence", "nature", "seeds"));
        } else if (hasPathToken(path, "seed", "seeds") || hasDisplayToken(displayName, "seed", "seeds")
                || hasCsvToken(facets, ItemFacet.SEED.id())) {
            expected.add(new ExpectedPlacement("seed evidence", "nature", "seeds"));
        }
        if (hasCsvToken(facets, ItemFacet.FLOWER.id())) {
            expected.add(new ExpectedPlacement("flower evidence", "nature", "flora"));
        }
        if (hasCsvToken(facets, ItemFacet.LEAVES.id())
                || hasCsvToken(node.meta(SearchNodeKeys.TAGS, ""), "minecraft:leaves")
                || hasCsvToken(node.meta(SearchNodeKeys.BLOCK_TAGS, ""), "minecraft:leaves")) {
            expected.add(new ExpectedPlacement("leaves evidence", "nature", "flora"));
        }
        if (hasCsvToken(facets, ItemFacet.SPAWN_EGG.id())) {
            expected.add(new ExpectedPlacement("spawn egg evidence", "bestiary", null));
        }
        return expected;
    }

    private static boolean isSapling(SearchNode node) {
        String path = node.id().getPath().toLowerCase(Locale.ROOT);
        String blockClass = node.meta(SearchNodeKeys.BLOCK_CLASS, "");
        return hasPathToken(path, "sapling", "saplings")
                || hasCsvToken(node.meta(SearchNodeKeys.TAGS, ""), "minecraft:saplings")
                || hasCsvToken(node.meta(SearchNodeKeys.BLOCK_TAGS, ""), "minecraft:saplings")
                || blockClass.endsWith("SaplingBlock")
                || blockClass.contains(".SaplingBlock");
    }

    private static boolean hasPathToken(String path, String... expected) {
        for (String token : path.split("[_/]")) {
            for (String match : expected) {
                if (match.equals(token)) {
                    return true;
                }
            }
        }
        return false;
    }

    private static boolean hasDisplayToken(String displayName, String... expected) {
        for (String token : displayName.split("[\\s_-]+")) {
            for (String match : expected) {
                if (match.equals(token)) {
                    return true;
                }
            }
        }
        return false;
    }

    private static boolean hasCsvToken(String encoded, String expected) {
        if (encoded == null || encoded.isBlank()) {
            return false;
        }
        for (String token : encoded.split(",")) {
            if (expected.equals(token.trim())) {
                return true;
            }
        }
        return false;
    }

    private static String categoryKey(SearchNode node) {
        return node.meta(SearchNodeKeys.ONTOLOGY_CATEGORY, "")
                + "/"
                + node.meta(SearchNodeKeys.ONTOLOGY_SUBCATEGORY, "");
    }

    private record ExpectedPlacement(String reason, String category, String subcategory) {
        boolean matches(String actual) {
            String prefix = category + "/";
            return subcategory == null
                    ? actual.startsWith(prefix)
                    : actual.equals(prefix + subcategory);
        }
    }
}
