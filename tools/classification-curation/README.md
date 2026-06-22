# Classification Curation Tool

Dev-time pipeline that turns an `ami_dumps` capture into reviewed entries in
`xplat/src/main/resources/assets/ami/classification_overrides.json`. The runtime
never reads the dump — only the committed override JSON.

## Stages

| # | Stage | Command | Output |
|---|-------|---------|--------|
| 1 | detect | `python detect.py <dump>/search/search_nodes.jsonl candidates.jsonl reject_ledger.jsonl` | `candidates.jsonl` |
| 2 | evidence | `python evidence.py candidates.jsonl <dump>/search/search_nodes.jsonl <dump>/recipes/recipes_runtime.jsonl evidence_batch.jsonl` | `evidence_batch.jsonl` |
| 3 | propose | Claude Code agent reading `evidence_batch.jsonl` per `prompts/propose.md` | `proposals.jsonl` |
| 4 | review | Human edits each line's `decision` to `approve`/`reject` | `proposals.jsonl` |
| 5 | apply | `python validate_proposals.py proposals.jsonl && python apply.py proposals.jsonl ../../xplat/src/main/resources/assets/ami/classification_overrides.json reject_ledger.jsonl` | override JSON + `reject_ledger.jsonl` |
| 6 | verify | `./gradlew neoforge:test --tests "com.sanhiruzu.ami.index.ClassificationOverrideReplayGateTest"` (add `-Dami.overrideGateStrict=true` to fail on unexplained changes) | `neoforge/build/reports/ami-classification/override-replay-gate.md` |

Run all commands from `tools/classification-curation/`. Set `<dump>` to your
`ami_dumps` directory (e.g. the Synesthesia instance).

## Reject ledger

`reject_ledger.jsonl` is persistent curation memory and **is committed**.
`detect.py` reads it and skips any id already rejected, so a rejected item is
not re-surfaced on later rounds. Rejection is by id (coarse): re-considering a
rejected item means removing its line from the ledger.

## Loop until dry

Re-run stages 1–6 each round. The reject ledger plus the growing override file
shrink the candidate pool until `detect.py` reports zero (or only items you have
deliberately deferred).

## Tests

`python -m unittest discover -s tests -p "test_*.py" -v`
