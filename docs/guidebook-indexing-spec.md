# Guidebook Indexing Spec

## Goals

- Surface in-game documentation as first-class AMI search results.
- Make guide-derived matches explainable in the result row, tooltip, and context menu.
- Keep guide content optional, memory-bounded, and isolated from the main item index.
- Give other mods a public API to register searchable guide documents and open actions without AMI hard-depending on their internals.

## Non-Goals

- Do not add semantic embedding search in the first implementation.
- Do not fetch or index remote web documentation by default.
- Do not require Patchouli, GuideME, Create, or any other guide system as a hard dependency.
- Do not store full guide body text in `SearchNode.metadata`.
- Do not make guide body text outrank exact item, registry id, recipe, or structured metadata matches.

## Supported Sources

### V1

- Public AMI guide document registration API.
- Source-agnostic guide document model.
- Lightweight guide index over title, chapter, book id, mod id, referenced items, tags, and optional capped text.
- Deterministic JVM tests for registration, duplicate handling, search fields, and failure isolation.

### V1.5

- Patchouli resource adapter:
  - Index book/category/page titles.
  - Index page text with caps.
  - Extract item references when pages declare stacks or recipe/item widgets.
  - Add `Open Guide Page` when Patchouli is loaded and the page opener is available.
- AE2 GuideME adapter:
  - Keep the existing reflective `Open ME Guide` behavior.
  - Add page-level documents if GuideME exposes stable page ids/open APIs.

### V2

- Create Ponder scene adapter:
  - Treat Ponder scenes as guide/tutorial documents.
  - Connect `CREATE_HAS_PONDER` item metadata to related guide hits.
- Optional body-text indexing modes and richer snippets.

## Data Model

Guide documents are not normal item nodes. They live in a separate guide index and can later be projected into result rows.

Core fields:

- `id`: stable document id, for example `patchouli:botania/lexicon/basics/mana_spreaders`.
- `sourceType`: `patchouli`, `guideme`, `ponder`, `plugin`, or another stable adapter id.
- `modId`: owning content mod, for example `botania`.
- `bookId`: guide/book id when available.
- `pageId`: source-native page id when available.
- `title`: row label.
- `chapter`: optional chapter/category label.
- `referencedItems`: registry ids mentioned or displayed by the page.
- `tags`: stable search tags such as `mana`, `automation`, `network`.
- `summaryText`: optional capped text for search/snippets.
- `openAction`: optional client callback owned by the source adapter/plugin.

Full body text should be loaded lazily by the owning adapter when possible. `summaryText` is a bounded search field, not a full document store.

## Public API Contract

AMI exposes a client-side guide registry. Mods or AMI compat adapters can register documents during client setup, resource reload, or index rebuild.

Rules:

- Document ids must be stable.
- Duplicate ids replace previous documents deterministically.
- Registration failures must not break AMI indexing or search.
- `openAction` callbacks must be optional and may fail quietly.
- AMI may cap text length before indexing.
- AMI may clear and rebuild all guide docs on resource reload.

The public API owns document registration. AMI owns presentation, result ranking, and filtering.

## UX Rules

### Guide Page Result

A guide page result should be visually distinct from item results:

```text
[book icon] Mana Spreaders
Lexica Botania > Basics · Guide Page · mentions Mana Spreader
```

Context menu actions:

- `Open Guide Page`
- `Show Referenced Items`
- `Copy Page Title`
- `Copy Guide Id`
- `Filter Mod`

### Item Result With Guide Evidence

If guide evidence materially contributes to an item result, the item row gets one compact provenance line:

```text
Mana Pool
Botania · Guide match: Lexica Botania > Mana Spreaders
```

Guide snippets should not be shown in every item row by default. They belong in hover/expanded details.

### Expanded Detail / Tooltip

Tooltip or expanded row detail should show match provenance:

```text
Matched Guide Page
Lexica Botania > Basics > Mana Spreaders

Matched text:
"point a Mana Spreader at a Mana Pool"

Referenced items:
Mana Spreader, Mana Pool, Wand of the Forest
```

## Search And Ranking

Ranking priority should be:

1. Exact item/display-name/id matches.
2. Structured metadata and explicit filters.
3. Guide title/chapter matches.
4. Guide referenced-item matches.
5. Guide summary/body-text matches.

Guide body text should only influence item ranking when guide result types are enabled or the query looks explanatory, such as `how`, `why`, `setup`, `guide`, `use`, or `tutorial`.

Structured filters remain hard constraints. Semantic or guide evidence must not bypass `@mod`, category, capability, or numeric predicates.

## Memory And Lifecycle

Indexing modes:

- `off`: no guide indexing.
- `titles`: title/chapter/book/source metadata and referenced items only.
- `summary`: titles plus capped page text.
- `full`: reserved for future use; still bounded and optional.

Initial default should be `titles` or `summary` with a conservative per-document text cap.

Implementation rules:

- Intern or deduplicate common strings where practical.
- Store item references as `ResourceLocation` ids.
- Generate snippets lazily for visible results.
- Do not duplicate full text into `SearchNode`.
- Clear/rebuild guide documents on resource reload or source adapter refresh.

## Test Plan

Deterministic JVM tests:

- Guide document registration stores the expected fields.
- Duplicate guide ids replace previous documents deterministically.
- Plugin/adapter exceptions do not prevent other documents from indexing.
- Title, chapter, tag, mod id, book id, and referenced item queries find expected docs.
- Summary/body text is capped.
- `off` indexing mode removes guide results.
- Referenced item evidence can be represented without modifying item metadata.
- Context-menu/open callbacks are optional and not invoked during indexing.

Fixture tests:

- Patchouli-style JSON fixture indexes title/category/item references.
- GuideME-style fixture maps stable page ids if an adapter is added.
- Ponder-style fixture maps scene id and referenced item ids if an adapter is added.

Runtime smoke, after JVM coverage:

- Search for a Patchouli page title and open the page.
- Search for an AE2 item and verify `Open ME Guide` still works.
- Search for a Create item with Ponder content and open the Ponder scene.
- Verify guide result rows and tooltips do not overlap existing AMI panels.

