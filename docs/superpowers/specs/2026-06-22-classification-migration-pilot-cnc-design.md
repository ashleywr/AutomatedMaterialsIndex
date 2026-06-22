# Classification Migration Pilot (Cnc) — Design

**Date:** 2026-06-22
**Status:** Approved (design)
**Predecessors:** Plan 2a (override consumer, merged), Plan 3a (offline curation tool, merged). This is the first slice of **Plan 3b**.

## Goal

Prove the end-to-end migration loop on one real plugin: extend the override
system so a `modPattern` can add facets, author override data that reproduces
`CncCompat`, prove equivalence and behavior-neutrality, then delete the Java
plugin. Establishes the pattern (and the trustworthy gate) that the full
Plan 3b sweep will reuse.

## Background: why the original premise changed

The Plan 3 spec assumed ~30 pure string-match compat plugins **plus** pure
path-token per-mod blocks inside `PrimaryCategoryResolver` ready to delete.
The refactored code is different:

- The in-resolver per-mod blocks (`resolveBotaniaIdentity`,
  `resolveWaystonesIdentity`, `resolveSpectrumIdentity`, …) now sit behind
  capability/class/tag guards (`*_ITEM_KIND`, `itemClass`, `blockClass`,
  block tags) and mix path-token matching with class checks. None are pure
  path-token matchers.
- Of ~40 classification compat plugins, only a handful (`HpmCompat`,
  `CncCompat`) are genuinely pure path-token. Most read `itemClass`/tags/
  recipe data or extract a capability — those stay as code.
- The pure-string-match plugins that exist mostly **add facets** by path
  token (then let the evidence classifier decide the category), which the
  current override schema cannot express and the replay gate does not check.

So Plan 3b is a per-rule triage, not a clean 30-plugin delete. This pilot
takes the smallest realistic slice to validate the loop and close the two
gaps (schema + gate) that the realistic case needs.

## Scope

**In scope (pilot):**

- Extend the override consumer so `modPatterns` carry `addFacets`/`removeFacets`.
- Extend the replay-diff gate to diff facets, not just category/subcategory.
- Author override data reproducing `CncCompat`'s facet effects.
- A deterministic equivalence test proving the override matches the plugin.
- Delete `CncCompat` and its classification registration.

**Out of scope:**

- Any other compat plugin or in-resolver block.
- Search tokens (`cnc_organic_material`, `cnc_artifact`) and the private
  `cncItemKind` meta key — no production consumer reads them (only a unit
  test asserts `cncItemKind`, removed with the plugin). Their loss is an
  accepted, documented delta outside classification scope.
- `CrittersCrawlersGuideSource` and any guide registration for `cnc` — these
  are the mod's guidebook integration, unrelated to classification; they stay.
- promoteMod / dynamic top-level categories (Plan 2b).

## Success criteria

1. `CncCompat.java` and its `ItemProvider` classification hook are deleted.
2. `cnc` items keep identical `category`/`subcategory` AND facets
   (`INGREDIENT_ORGANIC`, `MAGIC_ARTIFACT`) sourced from override data.
3. The extended replay gate is green (0 unexplained category/facet changes)
   over the full Synesthesia dump.
4. The deterministic equivalence test passes with the plugin deleted.
5. Full Java + Python suites green.

## What CncCompat does today

`xplat/src/main/java/com/sanhiruzu/ami/compat/CncCompat.java`, invoked from
`ItemProvider.runFocusedCompatHooks()` (`ItemProvider.java:475`):

```java
if (containsAny(path, "buckskin","raw_turkey","mandible","antler","tusk","wishbone"))
    facts.add("organic_material");           // -> ItemFacet.INGREDIENT_ORGANIC
if (containsAny(path, "potofmouse","kill_stick"))
    facts.add("artifact");                   // -> ItemFacet.MAGIC_ARTIFACT  (takes priority)
// plus: meta "cncItemKind" = kind; search token "cnc_" + kind   (both dropped)
```

It assigns **no category** — only facets, which feed the evidence classifier
downstream.

## Component 1 — Override consumer extension (Java)

Files: `ModPatternRule.java`, `ClassificationOverrides.java`,
`PrimaryCategoryResolver.java` (all under `xplat/.../index/`).

### ModPatternRule

Add two facet sets; category/subcategory become optionally empty:

```java
public record ModPatternRule(String modId, Set<String> pathTokens,
                             EnumSet<ItemFacet> addFacets, EnumSet<ItemFacet> removeFacets,
                             String category, String subcategory) {
    public boolean hasCategory() { return category != null && !category.isBlank(); }
}
```

### ClassificationOverrides.parsePatterns

Parse `addFacets`/`removeFacets` arrays via the existing `parseFacets` helper.
Absent arrays → empty `EnumSet`. `category`/`subcategory` already optional.

### PrimaryCategoryResolver.resolve()

The per-item override applies facets at lines 511–519, **before** the evidence
profile (`routedProfile`, `candidateSummary`) is built at 525–528. The
`modPattern` lookup currently happens at 538 and unconditionally terminates
with a category. Change:

1. Move `Optional<ModPatternRule> patternRule = ClassificationOverrides.patternFor(modId, path);`
   up beside the per-item block (≈511).
2. Apply `patternRule`'s `addFacets`/`removeFacets` to `facets` and re-encode
   `SearchNodeKeys.FACETS` in the same place the per-item facets are applied
   (so pattern facets influence the evidence profile built at 525).
3. Keep the terminal finish for a **category-bearing** pattern (today's
   behavior), guarded by `r.hasCategory()`. A **facet-only** pattern (no
   category) applies facets and falls through: `route.skipped(
   "classification_override", "mod_pattern facets-only")`.

Precedence (unchanged ordering, facets added): per-item facets → pattern
facets → per-item forced category → pattern category → existing gates.

## Component 2 — Replay-diff gate extension (Java test)

File: `neoforge/src/test/java/.../ClassificationOverrideReplayGateTest.java`.

`categoryById` currently maps id → `category + "/" + subcategory`. Extend the
per-item signature to also include **sorted facets** read from
`SearchNodeKeys.FACETS`, e.g. id → `category/subcategory|facetA,facetB`. A
facet-only change now registers as a diff and must be "explained" by
`forItem`/`patternFor` (the `explained()` predicate is unchanged). This makes
the gate trustworthy for facet-adding migrations.

Add a synthetic unit test alongside the existing two: install a facet-only
modPattern for `minecraft:dirt` (adds one facet, no category), replay over the
dump, assert dirt's facet set changed and is explained while every other
item's signature is unchanged. State restored via `loadBundledDefaults()` in
`finally`, matching the existing tests.

## Component 3 — Curation tool: facets on modPattern proposals (Python)

Files: `tools/classification-curation/validate_proposals.py`, `apply.py`.

- `validate_proposal`: for a `modPattern`-scope row, accept optional
  `addFacets`/`removeFacets` lists (lowercase facet ids). A modPattern is
  valid if it has `mod` + non-empty single-token `pathTokens` + at least one
  of `category` (non-empty) / `addFacets` / `removeFacets`.
- `apply.merge`: emit `addFacets`/`removeFacets` into the modPattern JSON when
  present and non-empty; keep them out when empty. Dedup key extends to
  include the sorted facet lists.

The dump's `cnc` items must surface as candidates — `detect.py` flags them
(`weak_route`/`empty_facets` as applicable) — so `evidence.py` produces rows
for the proposal step. The pilot may author the two modPatterns by hand into
`proposals.jsonl` (the curation flow is the same either way).

## Component 4 — The cnc override data

Two `modPatterns` (one per facet group). Tokens match single `[_/]`-split
components of the item path:

```json
{
  "items": {},
  "modPatterns": [
    {"mod": "cnc", "pathTokens": ["potofmouse", "kill", "stick"],
     "addFacets": ["magic_artifact"]},
    {"mod": "cnc", "pathTokens": ["buckskin", "turkey", "mandible", "antler", "tusk", "wishbone"],
     "addFacets": ["ingredient_organic"]}
  ]
}
```

Notes:
- `raw_turkey` → tokens `raw`,`turkey`; the rule uses `turkey`. `kill_stick`
  → `kill`,`stick`. `potofmouse` is one token.
- The **artifact rule is listed first** so it wins for any item that could
  match both groups, matching `CncCompat`'s `artifact`-takes-priority order
  (`patternFor` returns the first matching rule).
- Rules are mod-scoped to `cnc`, so generic tokens (`raw`, `stick`) cannot
  bleed into other mods.

This data is appended to the bundled
`xplat/src/main/resources/assets/ami/classification_overrides.json` (currently
empty). It becomes the first real entries in that file.

## Component 5 — Equivalence proof (acceptance test)

The replay gate proves **consistency** (the override causes no *unexplained*
drift in the current dump) but not equivalence — the dump already carries
Cnc's facets, so installing the same facets is idempotent. The real proof is a
deterministic JUnit test, written **before** deletion, that compares the two
mechanisms directly:

For representative items (`cnc:buckskin`, `cnc:wishbone`, `cnc:potofmouse`,
plus one negative `cnc` item that matches neither group):

1. Compute the facet set `CncCompat.enrichItem` produces from a bare profile.
2. Compute the facet set `resolve()` produces for the same bare profile with
   the cnc override installed.
3. Assert they are equal.

Because step 2 does not depend on `CncCompat`, the test still passes after the
plugin is deleted — that is the equivalence guarantee. This is deterministic
and in-repo (no dump regeneration), matching the project's preference for
deterministic tests over runtime checks.

## Sequencing

1. **Consumer + gate extension** (Components 1–2) with their unit tests.
2. **Curation facet support** (Component 3) with unit tests.
3. **Author cnc data + equivalence test** (Components 4–5), with `CncCompat`
   still present; gate + equivalence both green.
4. **Delete** `CncCompat.java`, the `ItemProvider.java:475` classification
   hook, the `cnc` classification entry in `CompatIndexRegistry` (verify it is
   the classification registration, not the guide source), and the `cnc`
   assertions in `SynesthesiaCompatTest`. Preserve `CrittersCrawlersGuideSource`.
5. **Verify**: re-run the extended gate over the dump and the full Java +
   Python suites.

## Testing summary

| Layer | Test | Proves |
|---|---|---|
| Consumer | facet-only + category-bearing modPattern unit tests | resolver applies pattern facets; facet-only falls through |
| Curation | validate/apply facet round-trip | tool emits modPattern facets correctly |
| Gate | synthetic facet-only replay test | gate detects + explains facet changes |
| Equivalence | CncCompat vs override facet equality | override reproduces the plugin (survives deletion) |
| Regression | extended gate over full dump; full suites | no unexplained category/facet drift; nothing else breaks |

## Risks

- **Token over-match:** a generic token (`stick`) matching an unintended cnc
  item. Mitigated by mod-scoping and the full-dump gate, which would flag any
  unexpected cnc item gaining a facet.
- **`CompatIndexRegistry` entanglement:** the `cnc` entry must be confirmed as
  the classification registration before deletion; the guide source stays.
  Verified during step 4, not assumed.
- **Gate remains necessary-but-not-sufficient** for facet migrations in
  general; the equivalence test is the per-plugin sufficiency proof. The full
  3b sweep inherits both.
