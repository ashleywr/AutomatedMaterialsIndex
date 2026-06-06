# Shared Compat Provider Spec

AMI's preferred long-term compat path is mod-authored, viewer-neutral providers. AMI consumes these providers, but the
contracts avoid AMI result-row, tree, and context-menu types so other viewers can consume the same integration data.

## Provider Types

### Searchable Guides

Service file:

```text
META-INF/services/com.sanhiruzu.searchableguides.api.SearchableGuideProvider
```

Use for guide pages, tutorial pages, in-game manuals, and exact page openers.

Main API:

- `SearchableGuideProvider`
- `SearchableGuideDocument`
- `SearchableGuideProviders`

### Searchable Items

Service file:

```text
META-INF/services/com.sanhiruzu.searchableitems.api.SearchableItemProvider
```

Use for item metadata enrichment and representative generated stacks. This is the portable replacement for AMI-only
`IAmiPlugin#enrichItemMeta` and `IAmiPlugin#getHeroItems`.

Main API:

- `SearchableItemProvider#enrichItemMetadata`
- `SearchableItemProvider#getRepresentativeItems`
- `SearchableItemProviders`

### Item Result Actions

Service file:

```text
META-INF/services/com.sanhiruzu.searchableitems.api.SearchableItemActionProvider
```

Use for optional actions that a viewer can attach to an item result. AMI renders them in the item context menu; another
viewer may expose them elsewhere.

Main API:

- `SearchableItemActionProvider`
- `SearchableItemActionContext`
- `SearchableItemAction`
- `SearchableItemActionProviders`

## AMI Compatibility Layer

AMI still supports `IAmiPlugin` for existing integrations and AMI-specific features:

- `getExclusionZones(Screen)` remains AMI overlay-specific.
- `getHeroItems()` is adapted only by AMI.
- `enrichItemMeta(...)` is adapted only by AMI.
- `addItemContextMenuActions(...)` is adapted only by AMI.
- `addGuideDocuments(...)` is adapted only by AMI.

New mod-owned integrations should prefer the shared provider APIs when the data could be useful outside AMI. AMI-specific
APIs remain appropriate for overlay layout, AMI-only UI behavior, or migration compatibility.

## Direct Registration

Mods can use service loader files, or register directly when AMI is present:

```java
AmiApi.registerSearchableGuideProvider(new ExampleGuideProvider());
AmiApi.registerSearchableItemProvider(new ExampleItemProvider());
AmiApi.registerSearchableItemActionProvider(new ExampleActionProvider());
```

Service-loader providers are preferred for integrations that should also be visible to other viewers without calling
AMI APIs directly.
