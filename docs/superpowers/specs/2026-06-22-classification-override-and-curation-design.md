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
- **Applied as the final classification step**, after the runtime classifier and capability compat.
- **Two scopes:**
  - **Per-item:** `add` / `remove` facets, `forceCategory`, `forceSubcategory`.
  - **Mod promotion:** `promoteMod: <id> → <newTopLevelCategory>`. The mod's items default into the new category, but their **subcategory is still derived** from facets/recipe-role (Create → kinetics / logistics / materials / decoration) — promoted as a domain, never flattened into a heap. Per-item overrides can pull individual items back out.
- **Precedence (most specific wins):** per-item → mod-promotion → runtime classifier.
- **Dynamic categories:** override data can introduce **new top-level categories**. The consumer and the category-tree UI must accept dynamically-declared nodes, reusing the same category registry as the WORKFLOW-dimension ontology work.

The accidental-vs-intentional distinction is the crux: a mod-name bucket is a *smell* only when it is a fallback flat dump. A deliberate `promoteMod` with preserved subcategories is the correct treatment for a large, cohesive mod.

## Component 3 — Offline curation pipeline

- **Input:** `search_nodes.jsonl` + `recipes_runtime.jsonl`.
- **Detectors** flag candidates:
  - route phase in `fallback`/`evidence_fallback`/`compat_fallback`/`unknown`/`<none>`;
  - empty facets;
  - cross-signal contradiction (tab vs category, recipe-role vs category, facet vs category);
  - mod-name pseudo-categories.
- **Evidence assembly:** per candidate, gather recipe-graph neighbors (`OUTPUT_OF` / `USED_IN`), creative tab, tags, current route + runner-up.
- **LLM proposal:** grounded in that evidence, proposes either a per-item correction or a mod-promotion.
- **Review queue:** proposals land in an editable artifact; the human bulk-approves/rejects. Approved entries are written into the bundled JSON. Rejections are remembered so they are not re-proposed.
- **Seed batch:** the 13 string-matching plugins' rules become the first proposals — each a per-mod *disperse vs promote* decision.

## Component 4 — Plugin migration

- Lift the **facet-assignment** rules out of the 13 string-matchers into override data; **delete** those plugins.
- **Preserve** their non-classification behaviors (display-name fixes, family detection, search tokens) — keep in code or relocate; not dropped.
- **Keep** capability-extracting compat untouched.

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

## Open sub-decisions (resolve during planning)

- Offline tool language/runtime: standalone Python script vs Gradle/Java task.
- Review-queue artifact format (editable JSONL, generated Markdown, or minimal TUI).
- Exact override JSON schema shape and how `promoteMod` declares seed subcategories vs deriving them.
