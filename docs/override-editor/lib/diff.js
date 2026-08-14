import { SCHEMA_VERSION } from "./constants.js";

export function computeSparsePatch(editableItems, originalOverrides) {
  const items = {};
  for (const it of editableItems) {
    const delta = {};
    const b = it.baseline, e = it.edited;
    if ((b.category ?? null) !== (e.category ?? null) && e.category != null) delta.category = e.category;
    if ((b.subcategory ?? null) !== (e.subcategory ?? null) && e.subcategory != null) delta.subcategory = e.subcategory;

    // Facet diff is computed against runtimeFacets, NOT against baseline.facets, so the saved
    // override layer reconstructs the desired final set from the bare runtime. This is what
    // makes round-tripping (load → re-save) preserve previously-added override facets.
    const runtime = new Set(it.runtimeFacets ?? []);
    const edFacets = new Set(e.facets ?? []);
    const added   = [...edFacets].filter(f => !runtime.has(f));
    const removed = [...runtime].filter(f => !edFacets.has(f));
    if (added.length)   delta.addFacets    = added;
    if (removed.length) delta.removeFacets = removed;

    const tb = b.tooltipLines ?? [], te = e.tooltipLines ?? [];
    if (JSON.stringify(tb) !== JSON.stringify(te)) delta.tooltipLines = te;

    if ((b.accessLevel ?? null) !== null || (e.accessLevel ?? null) !== null) {
      if ((b.accessLevel ?? null) !== (e.accessLevel ?? null)) {
        if (e.accessLevel != null) delta.accessLevel = e.accessLevel;
      } else if (e.accessLevel != null) {
        delta.accessLevel = e.accessLevel;
      }
    }
    if ((b.visibility ?? null) !== null || (e.visibility ?? null) !== null) {
      if ((b.visibility ?? null) !== (e.visibility ?? null)) {
        if (e.visibility != null) delta.visibility = e.visibility;
      } else if (e.visibility != null) {
        delta.visibility = e.visibility;
      }
    }

    if (Object.keys(delta).length > 0) items[it.id] = delta;
  }
  return {
    schemaVersion: SCHEMA_VERSION,
    items,
    modPatterns: originalOverrides.modPatterns ?? [],
  };
}
