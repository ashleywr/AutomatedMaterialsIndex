# Changelog

All notable user-facing changes should be recorded here.

## Unreleased

### Added
- Headless AMI search benchmark suite backed by the live Minecraft item registry.
- Local benchmark history written to `run/config/ami_benchmark_history.jsonl`.
- Gradle `check` now runs unit tests and the AMI benchmark suite.

### Performance
- Record indexed item count, query executions, average search latency, P99 latency, skipped anomalies, and total benchmark duration for each benchmark run.

