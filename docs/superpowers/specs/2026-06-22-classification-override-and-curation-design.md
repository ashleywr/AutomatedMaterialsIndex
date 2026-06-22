# Classification Override Layer & Offline Curation Design

Date: 2026-06-22
Status: Draft for review

## Goal

Reach near-perfect item categorization for a large modpack (Synesthesia, ~21,489 indexed items) and ship those improvements to **all** AMI users as built-in defaults.

The work splits into two complementary halves (hybrid):

- **Generic runtime improvements** to the live classifier that help every AMI user automatically, with zero per-pack curation.
- **An offline-authored override layer** — bundled in the AMI mod — that corrects the genuinely hard, context-free tail that no runtime heuristic can recover.

A guiding principle constrains scope: **mod compat stays as code only when it extracts a capability nothing else exposes** (e.g. Create Stress Units, Mekanism chemicals). Compat that is merely `containsAny(path/class/tag, …) → addFacet` is data wearing a code costume and migrates into the override layer.

Out of scope for this spec:

- Rewriting the core ontology/category set itself (the planned WORKFLOW-dimension / `TECH → Craft & Industry` work is a separate effort; this design *reuses* that category registry rather than forking it).
- Runtime recipe-graph processing (all recipe analysis happens offline against the dump; runtime never builds the graph).
- Changing the capability-extracting compat plugins (Create, Mekanism, GregTech, AE2, SilentGears, Modular*, Cobblemon).

## Current State

Facets and classification are produced by a runtime pipeline:

- `xplat/src/main/java/com/sanhiruzu/ami/index/FacetIndexer.java` — `apply*Facts` passes over class/path/components/tags/block-state.
- `xplat/src/main/java/com/sanhiruzu/ami/index/EvidenceCollector.java`, `PrimaryCategoryResolver.java`, `CategoryScorer.java` — evidence weighting and category routing.
- `xplat/src/main/java/com/sanhiruzu/ami/index/providers/ItemProvider.java` — orchestration; runs metric sniffers and compat hooks after `FacetIndexer`.
- `xplat/src/main/java/com/sanhiruzu/ami/compat/*Compat.java` — 74 files; a subset assign facets via `CompatMetaUtil.addFacet`.

Each search node already records its own classification trace: `classificationRoutePhase`, `classificationRoute`, `classificationRouteRule`, `ontologyCategory`, `ontologySubcategory`, plus `creativeTabLabel`/`creativeTabId`, all tags, and the recipe graph (`unresolvedEdges.OUTPUT_OF` / `USED_IN`). The Synesthesia dump lives at:

`C:\Users\ashle\AppData\Roaming\PrismLauncher\instances\Synesthesia\minecraft\ami_dumps`
(`search/search_nodes.jsonl`, `recipes/recipes_runtime.jsonl`, `loot_tables/`, `recipe_viewer/`, `guides/`).

### Confidence profile (measured from the current dump, 21,489 items)

| Route phase | Count | Disposition |
| --- | ---: | --- |
| `hard_identity` + `primary_rule` + `evidence_strong` | 19,661 (91%) | confident — leave alone |
| `fallback` + `evidence_fallback` + `compat_fallback` | 1,137 | weak rule — review |
| `unknown` rule / `<none>` category | 706 | gap — review |

Additional review signals: **1,365 items have empty facets**; ~2,000 items sit in **mod-name pseudo-categories** (`sophisticated` 680, `swem` 478, `create` 337, `halcyon` 213, `modular_gear` 213, `malum` 101, `pastel` 40, `cataclysm` 11) — accidental flat dumps from string-matching compat. Union of all detectors ≈ 3–4k candidates, not 21k.

### Compat audit (facet-assigning plugins)

**String-matchers → migrate to override data (~13 plugins, ~137 facet rules):**
`AlexsCavesCompat` (17), `PastelCompat` (15), `DatanessenceCompat` (14), `SwemCompat` (14), `MnaCompat` (13), `BornInChaosCompat` (12), `MalumCompat` (11), `AlexsMobsCompat` (10), `NaturesAuraCompat` (9), `CataclysmCompat` (9), `ArsNouveauCompat` (6), `SpectrumCompat` (5), `TaczCompat` (2).

**Capability extractors → stay as code:** `CreateCompat`, `MekanismCompat`, `GregTechCompat`, `AE2Compat`, `SilentGemsCompat`/trait index, `ModularGearCompat`, `ModularGolemsCompat`, `CobblemonCompat`. JEI/EMI/FTB/guide bridges are infrastructure, not classification.

## Architecture

```
OFFLINE (dev machine, repo tooling)            RUNTIME (bundled in AMI mod, all users)
┌─────────────────────────────┐               ┌──────────────────────────────────┐
│ ami_dumps → detectors →     │   generates   │ 1. Generic classifier (improved) │
│ evidence assembly → LLM     │ ────────────► │ 2. Capability compat (Create…)   │
│ proposals → human approve   │  override     │ 3. Override consumer  ◄── LAST    │
│ → bundled override JSON     │   .json       │    (per-item > mod-promo > above) │
└─────────────────────────────┘               └──────────────────────────────────┘
```

The offline pipeline is a **dev-time authoring tool**; its only product is committed override data. Runtime reads the small bundled JSON, never the dump — so there is no load-time recipe processing.

## Component 1 — Runtime generic improvements

Ships to every AMI user; lands first to shrink the review queue before curation begins.

- **Creative-tab weighting:** raise the weight of `creativeTabLabel`/`creativeTabId` evidence in `EvidenceCollector`/`CategoryScorer`. The author's own tab grouping is the strongest available prior for overloaded names.
- **`minecraft:enchantable/*` tag evidence:** map the vanilla 1.21 functional tags (`enchantable/sharp_weapon`, `/armor`, `/bow`, `/crossbow`, `/mining`, `/fishing`, `/trident`, `/durability`) to weapon/armor/tool facets in `applyTagFacts`.
- **Attribute-modifier evidence:** items granting `attack_damage` → weapon, `armor`/`armor_toughness` → armor, independent of class/interface. Reuses signals `DpsMetricSniffer` already reads.
- **Richer decision trace:** each item records its confidence tier (exists today), plus the runner-up category and which evidence won. Consumed by the offline detectors and used to ground LLM proposals.

## Component 2 — Override layer (runtime)

- **Bundled resource:** `data/ami/classification_overrides/<mod>.json`, one file per mod, loaded by a new `ClassificationOverrides` consumer.
- **Insertion point — TOP of `PrimaryCategoryResolver.resolve()`** (not the bottom). To actually win over the runtime gates, the override is checked *before* `compat_route`/`hard_identity`, in this order: (1) apply per-item `add`/`remove` to the facet set; (2) per-item `forceCategory`/`forceSubcategory` → short-circuit return; (3) `promoteMod` match → return promoted category + derived subcategory; (4) per-mod pattern rule match → return; else fall through to the existing gates. (A bottom insertion would only catch fall-throughs and could not override a confident wrong classification.)
- **Three scopes:**
  - **Per-item:** `add` / `remove` facets, `forceCategory`, `forceSubcategory`.
  - **Mod promotion:** `promoteMod: <id> → <newTopLevelCategory>`. The mod's items default into the new category, but their **subcategory is still derived** from facets/recipe-role (Create → kinetics / logistics / materials / decoration) — promoted as a domain, never flattened into a heap. Optional `subcategoryHints` for items where derivation is wrong. Per-item overrides can pull individual items back out.
  - **Per-mod pattern rule:** `mod + pathToken set [+ optional itemClass/tag guards] → category/subcategory`. This is the data form that replaces the pure-path-token per-mod blocks currently hardcoded in `PrimaryCategoryResolver` (`resolveBotaniaIdentity`, `resolveWaystonesIdentity`, `resolveGregTechIdentity`, `resolveApotheosisIdentity`, the `BOTANIA_*`/`GREGTECH_*`/`WAYSTONES_*`/`APOTHEOSIS_*` token `Set`s, etc.).
- **Precedence (most specific wins):** per-item → mod-promotion → per-mod pattern rule → runtime classifier.
- **Dynamic categories:** override data can introduce **new top-level categories**. The consumer and the category-tree UI must accept dynamically-declared nodes, reusing the same category registry as the WORKFLOW-dimension ontology work.

The accidental-vs-intentional distinction is the crux: a mod-name bucket is a *smell* only when it is a fallback flat dump. A deliberate `promoteMod` with preserved subcategories is the correct treatment for a large, cohesive mod.

## Component 3 — Offline curation pipeline

A **dev-time authoring tool** in `tools/classification-curation/`. Six file-connected stages: Python does the deterministic plumbing, a Claude Code agent does the judgment, a Java replay-diff is the correctness gate. All handoffs are diffable JSONL/JSON files — no stage holds another's data in memory.

```
ami_dumps
   │
1. detect.py     search_nodes.jsonl → candidates.jsonl
   │              (fallback/unknown phase · empty facets ·
   │               cross-signal contradiction · mod-name pseudo-categories)
2. evidence.py   candidates + recipe edges (OUTPUT_OF/USED_IN) + tab + tags
   │              + current route + runner-up  → evidence_batch.jsonl
3. PROPOSE       Claude Code agent reads evidence_batch.jsonl, proposes a
   │  (Claude)    per-item or per-mod-pattern override + rationale
   │              → proposals.jsonl  (each: decision="pending")
4. REVIEW (human) flip decision → approve/reject in proposals.jsonl (bulk);
   │              rejects appended to a reject ledger → never re-proposed
5. apply.py      fold approvals → bundled classification_overrides.json
   │
6. VERIFY (Java) replay-diff: load override JSON + dump item-facts, re-run the
                  REAL resolver, diff vs baseline → targeted items change as
                  intended, zero unintended regressions in the confident ~17k
```

- **Stage 1 — `detect.py`:** detectors over `search_nodes.jsonl` flag candidates — route phase in `fallback`/`evidence_fallback`/`compat_fallback`/`unknown`/`<none>`; empty facets; cross-signal contradiction (tab vs category, recipe-role vs category, facet vs category); mod-name pseudo-categories. Output `candidates.jsonl` (one record per flagged item: id, current category/route, facets, tab, tags).
- **Stage 2 — `evidence.py`:** per candidate, join recipe-graph neighbors (`OUTPUT_OF`/`USED_IN` from the node's `unresolvedEdges`, ingredient detail from `recipes_runtime.jsonl`), creative tab, tags, current route + runner-up candidates. Output `evidence_batch.jsonl` (candidate + its evidence bundle).
- **Stage 3 — proposal (Claude Code agent/skill):** reads `evidence_batch.jsonl`; for each candidate proposes an override grounded in the evidence — per-item `add`/`remove`/`forceCategory`/`forceSubcategory`, a per-mod pattern rule, or a mod-promotion. Output `proposals.jsonl` (candidate id + proposed override + rationale + `decision: "pending"`).
- **Stage 4 — review (human):** edit `proposals.jsonl`, bulk-flipping `decision` to `approve`/`reject`. Rejected proposals are appended to a reject ledger so the detectors/proposer skip them on later rounds.
- **Stage 5 — `apply.py`:** fold approved proposals into the bundled `classification_overrides.json`, merging per-item + per-mod-pattern entries (dedup; reject-ledger honored).
- **Stage 6 — verification (Java replay-diff):** extend the existing `ClassificationReplayDiffReportTest`/`ClassificationGoldSetEvaluationTest` infra to load the updated override JSON plus the dump's captured item facts, re-run the **real** `PrimaryCategoryResolver`, and diff resolved categories against the pre-override baseline. Gate before committing data: every confident item keeps its category; flagged items change only as proposed. This same gate later protects code deletion in Component 4.
- **Loop:** detect → … → verify, iterating; the reject ledger plus already-applied overrides shrink the candidate pool each round (loop-until-dry over the ~3–4k).
- **Seed batch:** the string-matching plugins' rules become the first proposals — each a per-mod *disperse vs promote* decision.

## Component 4 — Plugin migration

Migration targets **two code sites** (both pure string-matching):

1. **Compat plugins** (`compat/*Compat.java`): lift the **facet-assignment** rules out into override data; **delete** those plugins. Scope grew during design — as of 2026-06-22 the modpack carries **~30** string-matching plugins (the original ~13 plus 17 added in-flight: Cgs, Cnc, DoggyTalents, EnigmaticLegacyPlus, EternalStarlight, ForbiddenArcanus, Hexalia, Hexerei, Hpm, McTradePost, Minecolonies, MowziesMobs, Ntgl, PowerGrid, Tide, Witchery, ZenColony). All audited as cap=0 → all migrate.
2. **In-resolver per-mod blocks** (`PrimaryCategoryResolver`, ~3,478 lines): the pure-path-token identity resolvers (`resolveBotaniaIdentity`, `resolveWaystonesIdentity`, `resolveGregTechIdentity`, `resolveApotheosisIdentity`, `resolveSpectrum/NaturesAura/AlexsMobs/AlexsCaves/Tacz/Mna/ArsNouveau`) and their token `Set`s become **per-mod pattern rules** in override data; delete the blocks. This shrinks the resolver toward pure facet/capability logic.

- **Preserve** non-classification behaviors (display-name fixes, family detection, search tokens) — keep in code or relocate; not dropped.
- **Keep** capability-extracting compat and the capability-backed identity resolvers (`*_ITEM_KIND`: Create, Cobblemon, Mekanism, Sophisticated, modular-gear) untouched.

## Data flow & sequencing

1. **Phase 1 — Runtime wins + trace.** Tab weighting, enchantable tags, attribute evidence, richer decision trace. Ships to everyone; reclassifies part of the 3–4k automatically.
2. **Phase 2 — Override consumer + schema.** New `ClassificationOverrides` consumer, JSON schema, dynamic-category support, bundled (initially empty) data; applied last.
3. **Phase 3 — Offline pipeline + plugin migration.** Detectors, evidence assembly, LLM proposals, review queue; migrate the 13 plugins as the seed batch.
4. **Phase 4 — Curate the residual tail.** Work the remaining flagged items through the review queue.

## Testing

- **Regression replay:** run the dump through the new pipeline; assert the ~17k confident items keep their category (no regressions) and flagged items change only as intended.
- **Override resolution** unit tests: precedence ordering (per-item > mod-promotion > runtime); mod-promotion preserves derived subcategories; new-top-level-category registration.
- **Migrated-plugin tests:** convert existing `*CompatTest`s from "plugin asserts facet" to "override data asserts facet" fixtures.
- **Schema guardrail:** every override id resolves to a real item/mod; JSON validates.

## Resolved sub-decisions

- **Offline tool runtime:** Python in `tools/classification-curation/` for the deterministic stages (detect, evidence assembly, apply); a Claude Code agent/skill for the proposal stage; Java for the replay-diff verification. Not a Gradle/Java task — the deterministic plumbing is text-munging over JSONL, better suited to Python, and the judgment stage needs an LLM, not a build task.
- **Proposal step:** runs **inside Claude Code** (agent/workflow reading `evidence_batch.jsonl`), not an external API call. Keeps the human, the evidence, and the proposer in one loop.
- **Verification:** **Java replay-diff over the dump**, reusing the existing replay/golden-set test infra to re-run the real classifier and diff before/after. This is the correctness gate for both new override data and (in Component 4) code deletion.
- **Review-queue format:** editable `proposals.jsonl` with a `decision` field, plus a persistent reject ledger.
- **`promoteMod` subcategories:** derived by default from facets/recipe-role, with optional `subcategoryHints` for items where derivation is wrong (see Component 2).
