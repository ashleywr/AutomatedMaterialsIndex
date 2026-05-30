package com.sanhiruzu.ami.client.results;

import com.sanhiruzu.ami.index.GlobalIndex;
import com.sanhiruzu.ami.index.AmiOntologyKinds;
import com.sanhiruzu.ami.index.SearchNode;
import com.sanhiruzu.ami.index.SearchNodeMirrorDump;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertTrue;

public class RuntimeMirrorOntologyCoverageTest {
    private static final List<Path> DEFAULT_DUMPS = List.of(
            Path.of("C:", "Users", "ashle", "AppData", "Roaming", "PrismLauncher", "instances", "Society- Sunlit Valley", "minecraft", "ami_dumps", "search_nodes.jsonl"),
            Path.of("C:", "Users", "ashle", "AppData", "Roaming", "PrismLauncher", "instances", "Ashley Modpack", "minecraft", "ami_dumps", "search_nodes.jsonl"),
            Path.of("C:", "Users", "ashle", "AppData", "Roaming", "PrismLauncher", "instances", "DeceasedCraft - Urban Zombie Apocalypse", "minecraft", "ami_dumps", "search_nodes.jsonl")
    );

    @BeforeEach
    void setUp() {
        GlobalIndex.getInstance().clear();
        GlobalIndex.getInstance().markIndexReady();
    }

    @Test
    void writesOntologyCoverageReportForAvailableRuntimeDumps() throws IOException {
        Path reportPath = repoRoot().resolve(Path.of("neoforge", "build", "reports", "ami-result-shapes", "ontology-unclassified-coverage.md"));
        Files.createDirectories(reportPath.getParent());

        StringBuilder report = new StringBuilder();
        report.append("# AMI Ontology Unclassified Coverage\n\n");
        report.append("Diagnostic report for Category view. Direct leaves under subcategories with curated kinds are ontology misses to audit.\n\n");

        int dumpsRead = 0;
        for (Path dump : configuredDumps()) {
            if (!Files.exists(dump)) {
                report.append("## Missing Dump\n\n`").append(dump).append("`\n\n");
                continue;
            }
            dumpsRead++;
            List<SearchNode> fixture = SearchNodeMirrorDump.reclassifyItemOntology(SearchNodeMirrorDump.readJsonl(dump));
            SearchState state = new SearchState();
            state.setGroupBy(ResultsProcessor.GroupBy.CATEGORY);
            state.setSortField(ResultsProcessor.SortField.ALPHABETICAL);
            state.setViewMode(ResultsToolbar.ViewMode.LIST);
            List<TreeNode> roots = ResultsViewProjector.project(fixture, state, null, false, false).roots();

            Map<String, CoverageBucket> buckets = new LinkedHashMap<>();
            collectUnclassifiedBuckets(roots, "", buckets);

            long unclassifiedLeaves = buckets.values().stream().mapToLong(bucket -> bucket.count).sum();
            long totalLeaves = countLeaves(roots);
            report.append("## ").append(dump.getParent().getParent().getFileName()).append("\n\n");
            report.append("- Source: `").append(dump).append("`\n");
            report.append("- Leaves: ").append(totalLeaves).append("\n");
            report.append("- Unclassified leaves: ").append(unclassifiedLeaves).append("\n");
            report.append("- Unclassified share: ").append(totalLeaves == 0 ? "0.00" : String.format(java.util.Locale.ROOT, "%.2f", unclassifiedLeaves * 100.0 / totalLeaves)).append("%\n\n");

            buckets.entrySet().stream()
                    .sorted((a, b) -> Long.compare(b.getValue().count, a.getValue().count))
                    .limit(50)
                    .forEach(entry -> {
                        CoverageBucket bucket = entry.getValue();
                        report.append("- ").append(entry.getKey()).append(": ").append(bucket.count).append("\n");
                        for (String example : bucket.examples) {
                            report.append("  - ").append(example).append("\n");
                        }
                    });
            report.append("\n");
        }

        if (dumpsRead == 0) {
            report.append("No configured runtime dumps were found on this machine.\n");
        }

        Files.writeString(reportPath, report.toString());
        assertTrue(Files.exists(reportPath), "Expected diagnostic report at " + reportPath.toAbsolutePath());
    }

    private static List<Path> configuredDumps() {
        String configured = System.getProperty("ami.coverageDumps");
        if (configured == null || configured.isBlank()) {
            configured = System.getenv("AMI_COVERAGE_DUMPS");
        }
        if (configured == null || configured.isBlank()) {
            return DEFAULT_DUMPS;
        }
        List<Path> paths = new ArrayList<>();
        for (String raw : configured.split(java.io.File.pathSeparator)) {
            if (!raw.isBlank()) paths.add(Path.of(raw));
        }
        return paths;
    }

    private static void collectUnclassifiedBuckets(List<TreeNode> nodes, String path, Map<String, CoverageBucket> buckets) {
        for (TreeNode node : nodes) {
            String label = node.getLabel().getString();
            String nextPath = path.isEmpty() ? label : path + " > " + label;

            if (!node.isLeaf()) {
                String key = node.getKey();
                if (key != null) {
                    String[] parts = key.split("/");
                    if (parts.length == 2 && !AmiOntologyKinds.kindsFor(parts[0], parts[1]).isEmpty()) {
                        CoverageBucket bucket = buckets.computeIfAbsent(nextPath, ignored -> new CoverageBucket());
                        for (TreeNode child : node.getChildren()) {
                            if (child.isLeaf()) {
                                addExample(bucket, child);
                            }
                        }
                    }
                }
                collectUnclassifiedBuckets(node.getChildren(), nextPath, buckets);
            }
        }
    }

    private static void addExample(CoverageBucket bucket, TreeNode node) {
        bucket.count++;
        if (bucket.examples.size() < 8) {
            bucket.examples.add(node.getLabel().getString() + " [" + node.getEntry().id() + "]");
        }
    }

    private static long countLeaves(List<TreeNode> nodes) {
        long count = 0;
        for (TreeNode node : nodes) {
            count += node.isLeaf() ? 1 : countLeaves(node.getChildren());
        }
        return count;
    }

    private static Path repoRoot() {
        Path current = Path.of("").toAbsolutePath();
        while (current != null) {
            if (Files.exists(current.resolve("AGENTS.md"))) {
                return current;
            }
            current = current.getParent();
        }
        throw new IllegalStateException("Could not locate repository root");
    }

    private static final class CoverageBucket {
        long count;
        final List<String> examples = new ArrayList<>();
    }
}
