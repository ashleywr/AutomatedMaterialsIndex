package com.sanhiruzu.ami.client.results;

import com.sanhiruzu.ami.index.GlobalIndex;
import com.sanhiruzu.ami.index.SearchNode;
import com.sanhiruzu.ami.index.SearchNodeKeys;
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

public class RuntimeMirrorResultsShapeExplorerTest {
    private static long countReclassified(List<SearchNode> before, List<SearchNode> after) {
        long changed = 0;
        int size = Math.min(before.size(), after.size());
        for (int i = 0; i < size; i++) {
            SearchNode previous = before.get(i);
            SearchNode current = after.get(i);
            if (!previous.id().equals(current.id())) {
                continue;
            }
            String previousCategory = previous.meta(SearchNodeKeys.ONTOLOGY_CATEGORY, "");
            String previousSubcategory = previous.meta(SearchNodeKeys.ONTOLOGY_SUBCATEGORY, "");
            String currentCategory = current.meta(SearchNodeKeys.ONTOLOGY_CATEGORY, "");
            String currentSubcategory = current.meta(SearchNodeKeys.ONTOLOGY_SUBCATEGORY, "");
            if (!previousCategory.equals(currentCategory) || !previousSubcategory.equals(currentSubcategory)) {
                changed++;
            }
        }
        return changed;
    }

    private static String categoryTransitionSummary(List<SearchNode> before, List<SearchNode> after) {
        Map<String, List<String>> examplesByTransition = new LinkedHashMap<>();
        Map<String, Integer> countsByTransition = new LinkedHashMap<>();
        int size = Math.min(before.size(), after.size());
        for (int i = 0; i < size; i++) {
            SearchNode previous = before.get(i);
            SearchNode current = after.get(i);
            if (!previous.id().equals(current.id())) {
                continue;
            }

            String previousKey = categoryKey(previous);
            String currentKey = categoryKey(current);
            if (previousKey.equals(currentKey)) {
                continue;
            }

            String transition = previousKey + " -> " + currentKey;
            countsByTransition.merge(transition, 1, Integer::sum);
            examplesByTransition.computeIfAbsent(transition, ignored -> new ArrayList<>());
            List<String> examples = examplesByTransition.get(transition);
            if (examples.size() < 5) {
                examples.add(current.displayName() + " (" + current.id() + ")");
            }
        }

        StringBuilder out = new StringBuilder();
        out.append("## Ontology Category Changes\n\n");
        countsByTransition.entrySet().stream()
                .sorted((a, b) -> Integer.compare(b.getValue(), a.getValue()))
                .limit(25)
                .forEach(entry -> {
                    out.append("- ").append(entry.getKey()).append(": ").append(entry.getValue()).append("\n");
                    for (String example : examplesByTransition.getOrDefault(entry.getKey(), List.of())) {
                        out.append("  - ").append(example).append("\n");
                    }
                });
        out.append("\n");
        return out.toString();
    }

    private static String categoryKey(SearchNode node) {
        return node.meta(SearchNodeKeys.ONTOLOGY_CATEGORY, "")
                + "/"
                + node.meta(SearchNodeKeys.ONTOLOGY_SUBCATEGORY, "");
    }

    private static SearchState state(ResultsProcessor.SortField sortField,
                                     ResultsProcessor.GroupBy groupBy,
                                     ResultsToolbar.ViewMode viewMode) {
        SearchState state = new SearchState();
        state.setSortField(sortField);
        state.setGroupBy(groupBy);
        state.setViewMode(viewMode);
        return state;
    }

    private static Path locateMirrorDump() {
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
            if (Files.exists(current.resolve("AGENTS.md"))) {
                return current;
            }
            current = current.getParent();
        }
        throw new IllegalStateException("Could not locate repository root");
    }

    @BeforeEach
    void setUp() {
        GlobalIndex.getInstance().clear();
        GlobalIndex.getInstance().markIndexReady();
    }

    @Test
    void writesRuntimeMirrorShapeReportWhenSearchNodeDumpExists() throws IOException {
        Path reportPath = repoRoot().resolve(Path.of("neoforge", "build", "reports", "ami-result-shapes", "runtime-mirror-result-shapes.md"));
        Files.createDirectories(reportPath.getParent());

        Path mirrorPath = locateMirrorDump();
        if (!Files.exists(mirrorPath)) {
            Files.writeString(reportPath, "# AMI Runtime Mirror Result Shape Exploration\n\n"
                    + "No runtime mirror dump found at `" + mirrorPath + "`.\n\n"
                    + "Run `/ami dump-search-nodes` in the client after indexing to generate it.\n");
            assertTrue(Files.exists(reportPath), "Expected diagnostic report at " + reportPath.toAbsolutePath());
            return;
        }

        List<SearchNode> runtimeFixture = SearchNodeMirrorDump.readJsonl(mirrorPath);
        List<SearchNode> fixture = SearchNodeMirrorDump.reclassifyItemOntology(runtimeFixture);
        StringBuilder report = new StringBuilder();
        report.append("# AMI Runtime Mirror Result Shape Exploration\n\n");
        report.append("Source dump: `").append(mirrorPath).append("`\n\n");
        report.append("Nodes: ").append(fixture.size()).append("\n\n");
        report.append("Ontology replay: current resolver reapplied from dumped item facets.\n\n");
        report.append("Reclassified items: ").append(countReclassified(runtimeFixture, fixture)).append("\n\n");
        report.append(categoryTransitionSummary(runtimeFixture, fixture));

        for (ResultsProcessor.GroupBy groupBy : List.of(
                ResultsProcessor.GroupBy.CATEGORY,
                ResultsProcessor.GroupBy.FAMILY,
                ResultsProcessor.GroupBy.MATERIAL
        )) {
            for (ResultsProcessor.SortField sortField : List.of(
                    ResultsProcessor.SortField.REGISTRY,
                    ResultsProcessor.SortField.ALPHABETICAL,
                    ResultsProcessor.SortField.COUNT
            )) {
                try {
                    SearchState listState = state(sortField, groupBy, ResultsToolbar.ViewMode.LIST);
                    SearchState compactState = state(sortField, groupBy, ResultsToolbar.ViewMode.GRID);
                    List<TreeNode> tree = ResultsViewProjector.project(fixture, listState, null, false, false).roots();
                    List<TreeNode> compact = ResultsViewProjector.project(fixture, compactState, null, true, false).roots();
                    report.append(ResultsShapeSnapshot.capture(groupBy, sortField, tree, compact, 9).toMarkdown());
                } catch (Throwable t) {
                    report.append("## group=").append(groupBy.name())
                            .append(" sort=").append(sortField.name())
                            .append("\n\n");
                    report.append("Unavailable in runtime mirror explorer: ")
                            .append(t.getClass().getSimpleName())
                            .append(": ")
                            .append(t.getMessage())
                            .append("\n\n");
                }
            }
        }

        Files.writeString(reportPath, report.toString());
        assertTrue(Files.exists(reportPath), "Expected diagnostic report at " + reportPath.toAbsolutePath());
    }
}
