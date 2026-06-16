package com.sanhiruzu.ami.benchmark;

import com.sanhiruzu.ami.index.*;
import com.sanhiruzu.ami.neoforge.AMI;

import java.io.IOException;
import java.util.List;
import java.util.Map;

public final class AmiBenchmarkGameTests {
    private static final String SUITE_NAME = "ami_search_registry_benchmark";
    private static final int DEFAULT_ITERATIONS = 120;

    private static final BenchmarkCase[] CASES = {
            new BenchmarkCase("iron", BenchmarkMode.SEARCH_ONLY, null),
            new BenchmarkCase("oak_planks", BenchmarkMode.SEARCH_ONLY, null),
            new BenchmarkCase("sword", BenchmarkMode.SEARCH_ONLY, null),
            new BenchmarkCase("#minecraft:planks", BenchmarkMode.SEARCH_ONLY, null),
            new BenchmarkCase("#minecraft:swords", BenchmarkMode.SEARCH_ONLY, null),
            new BenchmarkCase("oak", BenchmarkMode.GROUP_RESULTS, SearchNodeKeys.VARIANT_GROUP),
            new BenchmarkCase("stone", BenchmarkMode.GROUP_RESULTS, SearchNodeKeys.ONTOLOGY_SUBCATEGORY),
            new BenchmarkCase("red", BenchmarkMode.GROUP_RESULTS, SearchNodeKeys.COLOR_BUCKET)
    };

    private AmiBenchmarkGameTests() {
    }

    public static void benchmarkSearchRegistry(Runnable onReady, java.util.function.Consumer<String> onFail) {
        AmiIndexerService indexer = AmiIndexerService.getInstance();
        try {
            runBenchmark(indexer);
            onReady.run();
        } catch (Throwable t) {
            AMI.LOGGER.error("AMI benchmark failed", t);
            onFail.accept(t.getMessage());
        }
    }

    private static void runBenchmark(AmiIndexerService indexer) throws IOException {
        SearchService searchService = indexer.getOrBuildSearchService();
        AmiBenchmarkLogger.BenchmarkRun run = AmiBenchmarkLogger.createRun(SUITE_NAME, indexer.indexedItemCount());

        int iterations = Integer.getInteger("ami.benchmark.iterations", DEFAULT_ITERATIONS);
        for (int i = 0; i < iterations; i++) {
            for (BenchmarkCase benchmarkCase : CASES) {
                executeCase(searchService, benchmarkCase, run);
            }
        }

        if (run.queryExecutions() == 0) {
            throw new IllegalStateException("AMI benchmark produced no successful query executions");
        }

        AmiBenchmarkLogger.append(run);
        AMI.LOGGER.info(
                "AMI benchmark appended {} executions across {} indexed items to config/ami_benchmark_history.jsonl",
                run.queryExecutions(),
                indexer.indexedItemCount()
        );
    }

    private static void executeCase(SearchService searchService, BenchmarkCase benchmarkCase, AmiBenchmarkLogger.BenchmarkRun run) {
        try {
            long started = System.nanoTime();
            Map<NodeType, List<SearchNode>> results = searchService.query(benchmarkCase.query());
            if (benchmarkCase.mode() == BenchmarkMode.GROUP_RESULTS) {
                groupResults(results, benchmarkCase.groupKey());
            }
            long elapsed = System.nanoTime() - started;
            run.recordQuery(elapsed, countResults(results));
        } catch (Throwable t) {
            if (isNonRecoverable(t)) {
                throw t;
            }
            run.recordSkippedAnomaly();
            AMI.LOGGER.warn("Skipping anomalous AMI benchmark query '{}'", benchmarkCase.query(), t);
        }
    }

    private static Map<String, List<SearchNode>> groupResults(Map<NodeType, List<SearchNode>> results, String metadataKey) {
        List<SearchNode> itemResults = results.getOrDefault(NodeType.ITEM, List.of());
        if (itemResults.isEmpty()) {
            return GlobalIndex.getInstance().getGrouped(NodeType.ITEM, metadataKey);
        }
        return itemResults.stream().collect(java.util.stream.Collectors.groupingBy(
                node -> node.meta(metadataKey, "unknown"),
                java.util.LinkedHashMap::new,
                java.util.stream.Collectors.toList()
        ));
    }

    private static int countResults(Map<NodeType, List<SearchNode>> results) {
        int count = 0;
        for (List<SearchNode> nodes : results.values()) {
            count += nodes.size();
        }
        return count;
    }

    private static boolean isNonRecoverable(Throwable t) {
        return t instanceof VirtualMachineError || t instanceof LinkageError;
    }

    private enum BenchmarkMode {
        SEARCH_ONLY,
        GROUP_RESULTS
    }

    private record BenchmarkCase(String query, BenchmarkMode mode, String groupKey) {
    }
}
