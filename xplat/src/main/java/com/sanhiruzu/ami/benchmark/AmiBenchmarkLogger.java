package com.sanhiruzu.ami.benchmark;

import net.minecraftforge.fml.loading.FMLPaths;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

public final class AmiBenchmarkLogger {
    private static final Path HISTORY_FILE = FMLPaths.CONFIGDIR.get().resolve("ami_benchmark_history.jsonl");

    private AmiBenchmarkLogger() {
    }

    public static BenchmarkRun createRun(String suiteName, int indexedItemCount) {
        return new BenchmarkRun(suiteName, indexedItemCount);
    }

    public static void append(BenchmarkRun run) throws IOException {
        Files.createDirectories(HISTORY_FILE.getParent());
        Files.writeString(
                HISTORY_FILE,
                run.toJsonLine() + System.lineSeparator(),
                StandardCharsets.UTF_8,
                StandardOpenOption.CREATE,
                StandardOpenOption.APPEND
        );
    }

    public static final class BenchmarkRun {
        private final String suiteName;
        private final int indexedItemCount;
        private final long startedAtNanos;
        private final String timestamp;
        private final List<Long> latenciesNanos = new ArrayList<>();
        private int queryExecutions;
        private int resultCount;
        private int skippedAnomalies;

        private BenchmarkRun(String suiteName, int indexedItemCount) {
            this.suiteName = suiteName;
            this.indexedItemCount = indexedItemCount;
            this.startedAtNanos = System.nanoTime();
            this.timestamp = Instant.now().toString();
        }

        public void recordQuery(long latencyNanos, int results) {
            latenciesNanos.add(latencyNanos);
            queryExecutions++;
            resultCount += results;
        }

        public void recordSkippedAnomaly() {
            skippedAnomalies++;
        }

        public int queryExecutions() {
            return queryExecutions;
        }

        private String toJsonLine() {
            long totalNanos = System.nanoTime() - startedAtNanos;
            double averageMs = latenciesNanos.isEmpty()
                    ? 0.0D
                    : latenciesNanos.stream().mapToLong(Long::longValue).average().orElse(0.0D) / 1_000_000.0D;
            double p99Ms = percentile99Nanos(latenciesNanos) / 1_000_000.0D;

            return "{"
                    + "\"timestamp\":\"" + escape(timestamp) + "\","
                    + "\"suite\":\"" + escape(suiteName) + "\","
                    + "\"indexed_items\":" + indexedItemCount + ","
                    + "\"query_executions\":" + queryExecutions + ","
                    + "\"result_count\":" + resultCount + ","
                    + "\"skipped_anomalies\":" + skippedAnomalies + ","
                    + "\"total_execution_ms\":" + formatMillis(totalNanos) + ","
                    + "\"average_search_latency_ms\":" + formatDouble(averageMs) + ","
                    + "\"p99_search_latency_ms\":" + formatDouble(p99Ms)
                    + "}";
        }

        private static long percentile99Nanos(List<Long> values) {
            if (values.isEmpty()) {
                return 0L;
            }
            List<Long> sorted = new ArrayList<>(values);
            Collections.sort(sorted);
            int index = (int) Math.ceil(sorted.size() * 0.99D) - 1;
            return sorted.get(Math.max(0, Math.min(index, sorted.size() - 1)));
        }

        private static String formatMillis(long nanos) {
            return formatDouble(nanos / 1_000_000.0D);
        }

        private static String formatDouble(double value) {
            return String.format(Locale.ROOT, "%.6f", value);
        }

        private static String escape(String value) {
            StringBuilder escaped = new StringBuilder(value.length() + 8);
            for (int i = 0; i < value.length(); i++) {
                char c = value.charAt(i);
                switch (c) {
                    case '"' -> escaped.append("\\\"");
                    case '\\' -> escaped.append("\\\\");
                    case '\b' -> escaped.append("\\b");
                    case '\f' -> escaped.append("\\f");
                    case '\n' -> escaped.append("\\n");
                    case '\r' -> escaped.append("\\r");
                    case '\t' -> escaped.append("\\t");
                    default -> {
                        if (c < 0x20) {
                            escaped.append(String.format(Locale.ROOT, "\\u%04x", (int) c));
                        } else {
                            escaped.append(c);
                        }
                    }
                }
            }
            return escaped.toString();
        }
    }
}
