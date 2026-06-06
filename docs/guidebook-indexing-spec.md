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

## Viewer-Neutral Provider Path

The preferred long-term compat path is a mod-provided searchable-guide provider, not an AMI hardcoded adapter.
AMI consumes this provider, and the same provider contract is small enough for EMI, JEI, or another viewer to consume
without inheriting AMI UI assumptions.

Mods can expose a `SearchableGuideProvider` implementation through Java's service loader:

```text
META-INF/services/com.sanhiruzu.searchableguides.api.SearchableGuideProvider
```

The service file contains the implementation class name:

```text
com.examplemod.compat.searchableguides.ExampleGuideProvider
```

The provider contributes guide documents during viewer indexing:

```java
package com.examplemod.compat.searchableguides;

import com.sanhiruzu.searchableguides.api.SearchableGuideDocument;
import com.sanhiruzu.searchableguides.api.SearchableGuideProvider;
import net.minecraft.resources.ResourceLocation;

import java.util.function.Consumer;

public final class ExampleGuideProvider implements SearchableGuideProvider {
    @Override
    public String id() {
        return "examplemod:manual";
    }

    @Override
    public void addGuideDocuments(Consumer<SearchableGuideDocument> documents) {
        ResourceLocation bookId = ResourceLocation.fromNamespaceAndPath("examplemod", "manual");
        documents.accept(SearchableGuideDocument.builder(
                        ResourceLocation.fromNamespaceAndPath("examplemod", "ami_guide/manual/steel_press"),
                        "examplemod_manual",
                        "examplemod",
                        "Steel Press")
                .bookId(bookId)
                .iconItemId(bookId)
                .pageId("machines/steel_press")
                .chapter("Machines")
                .referencedItem(ResourceLocation.fromNamespaceAndPath("examplemod", "steel_press"))
                .tag("machine")
                .tag("automation")
                .summaryText("The Steel Press forms plates and gears from heated ingots.")
                .openAction(() -> ExampleManualScreens.open("machines/steel_press"))
                .build());
    }
}
```

AMI also keeps `IAmiPlugin#addGuideDocuments` and `AmiApi.registerGuideSource` for AMI-specific integrations, but those
should be treated as compatibility wrappers. A mod author who wants their guide support to be portable should prefer
`SearchableGuideProvider`, or register one directly with `AmiApi.registerSearchableGuideProvider`.

Authoring rules:

- Keep document ids stable across reloads and mod versions when the guide page is the same concept.
- Use the source-native page id in `pageId`; viewers can display/copy it, and the provider's opener can use it directly.
- Put the guidebook item id in `iconItemId` so guide rows show the correct book stack.
- Prefer concise `summaryText`; viewers may cap text before indexing, and full guide rendering remains the owning mod's job.
- Make `openAction` client-only and defensive. It should quietly do nothing if the page is locked, unavailable, or the screen cannot open yet.
- Use `referencedItems` for pages that teach an item. This lets viewers connect item searches to guide matches without duplicating guide text into item metadata.

AMI's built-in adapters should be treated as examples and broad coverage for common book systems. New mods should prefer
the shared provider route so exact page ids, unlock rules, and custom screen navigation stay owned by the mod that knows them.

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
