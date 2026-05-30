package com.sanhiruzu.ami.benchmark;

import com.sanhiruzu.ami.index.*;
import com.sanhiruzu.ami.neoforge.AMI;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import java.io.IOException;
import java.util.List;
import java.util.Map;

@PrefixGameTestTemplate(false)
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

    @GameTest(templateNamespace = AMI.MODID, template = "ami_benchmark_empty", setupTicks = 1L, timeoutTicks = 6000)
    public static void benchmarkSearchRegistry(GameTestHelper helper) {
        AmiIndexerService indexer = AmiIndexerService.getInstance();
        indexer.rebuild(helper.getLevel());
        helper.succeedWhen(() -> {
            failIfRebuildFailed(helper, indexer);
            helper.assertTrue(indexer.isReady(), "AMI index rebuild is still running");
            try {
                runBenchmark(indexer);
            } catch (Throwable t) {
                AMI.LOGGER.error("AMI benchmark GameTest failed", t);
                if (t instanceof Error error) {
                    throw error;
                }
                helper.fail("AMI benchmark failed: " + t.getMessage());
            }
        });
    }

    private static void failIfRebuildFailed(GameTestHelper helper, AmiIndexerService indexer) {
        Throwable failure = indexer.getLastRebuildFailure();
        if (failure != null) {
            AMI.LOGGER.error("AMI benchmark GameTest failed during index rebuild", failure);
            helper.fail("AMI index rebuild failed: " + failure.getMessage());
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
