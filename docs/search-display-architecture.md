# Search Index → Search Object → Display Architecture

## Current Design (1.4.0)

```
Index Layer (GlobalIndex, RuntimeSearchProviders)
    ↓ SearchNode list
Search Service Layer (SearchService, query resolution)
    ↓ Filtered SearchNode list
Universal Constraints Layer (DeletedSearchNodesTracker)
    ↓ Constrained SearchNode list
Results Processing Layer (ResultsProcessor, TreeNormalizer)
    ↓ TreeNode hierarchy
Display Projection Layer (ResultsViewProjector)
    ↓ Context-specific rendering (Favorites, Sidebar, Main)
```

## Key Contracts

1. **SearchNode** - Immutable data from index
   - Has ID, type, display name, metadata
   - No notion of visibility or deletion state
   - Index-agnostic

2. **Universal Constraints** - Applied uniformly before processing
   - DeletedSearchNodesTracker (instant hiding without reindex)
   - VisibilityFilter (hidden items, access levels)
   - Applied once at ResultsViewProjector entry point

3. **ResultsProcessor** - Organizes SearchNodes into hierarchy
   - Groups by category, mod, type
   - Builds navigation trees
   - Context-agnostic (works for favorites, search, sidebar)

4. **ResultsViewProjector** - Routes to display context
   - Selects view mode (grid, list, compact)
   - Handles favorites panel vs. main panel rendering
   - Entry point for universal constraints

## Design Principles

- **Layering**: Each layer has clear input/output contracts
- **Reusability**: ProcessingResults work across all display contexts
- **Testability**: Each layer can be unit tested independently
- **Extensibility**: New filters/processors don't require architecture changes

## Future Considerations

When deletion/editing becomes a major feature:
- Formalize UniversalConstraintsFilter registry
- Add constraint composability
- Consider caching strategies per layer
- May want DisplayDriver abstraction for context-specific rendering

**Current status**: Not needed yet. Keep simple.
