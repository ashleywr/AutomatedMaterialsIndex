export function mergeForEditing(dump, overrides) {
  const merged = [];
  const seen = new Set();

  for (const row of dump.items) {
    seen.add(row.id);
    const ov = overrides.items.get(row.id);
    const runtimeFacets = [...(row.currentFacets ?? [])];
    const baseline = {
      category:     ov?.category    ?? row.currentCategory    ?? null,
      subcategory:  ov?.subcategory ?? row.currentSubcategory ?? null,
      facets:       Array.from(new Set([...runtimeFacets, ...(ov?.addFacets ?? [])]))
                       .filter(f => !(ov?.removeFacets ?? []).includes(f)),
      tooltipLines: ov?.tooltipLines ?? [],
      accessLevel:  ov?.accessLevel ?? null,
      visibility:   ov?.visibility ?? null,
    };
    merged.push({
      id: row.id, mod: row.mod, className: row.className,
      displayName: row.displayName, creativeTabs: row.creativeTabs ?? [],
      runtimeFacets,
      baseline, edited: structuredClone(baseline),
      dirty: false, missingFromDump: false,
    });
  }

  for (const [id, ov] of overrides.items) {
    if (seen.has(id)) continue;
    const baseline = {
      category: ov.category ?? null, subcategory: ov.subcategory ?? null,
      facets: [...(ov.addFacets ?? [])], tooltipLines: ov.tooltipLines ?? [],
      accessLevel: ov.accessLevel ?? null, visibility: ov.visibility ?? null,
    };
    merged.push({
      id, mod: id.split(":")[0], className: "", displayName: id, creativeTabs: [],
      runtimeFacets: [],
      baseline, edited: structuredClone(baseline), dirty: false, missingFromDump: true,
    });
  }

  return merged;
}
