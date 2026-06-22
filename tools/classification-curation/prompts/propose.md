# Classification Proposal Agent

You are proposing classification overrides for Minecraft mod items that the
runtime classifier got wrong or left weak. You are given one JSON object per
line from `evidence_batch.jsonl`. For each, decide whether an override would
improve it and emit at most one proposal object.

## Input (per line)
`id`, `displayName`, `mod`, `path`, `current` (category/subcategory/route),
`facets`, `tab`, `tags`, `detectors`, `runnerUp`, `craftedFrom[]`, `usedToMake[]`.

## Output (one JSON object per line, to `proposals.jsonl`)
- `id`: the item id from the input.
- `scope`: `"item"` for a single item; `"modPattern"` when a whole family of a
  mod's items shares a path token and should route together.
- `override`:
  - item: `{ "category": "...", "subcategory": "...", "addFacets": [], "removeFacets": [] }`
  - modPattern: `{ "mod": "...", "pathTokens": ["spreader"], "category": "...", "subcategory": "..." }`
- `rationale`: one sentence grounded in the evidence (recipe neighbors, tab, tags).
- `decision`: always `"pending"` — the human sets approve/reject.

## Rules
- Use existing category ids seen in `current`/`craftedFrom` (e.g. `armor`,
  `tools`, `masonry`, `nature`, `decoration`, `magic`, `ingredients`,
  `geology`, `utility`); do not invent new top-level categories in this tool.
- `pathTokens` must be single tokens (no `_` or `/`). `"mana_spreader"` →
  token `"spreader"`.
- If the current classification already looks correct, emit no line for that id.
- Ground every proposal in the evidence; never guess from the name alone.
