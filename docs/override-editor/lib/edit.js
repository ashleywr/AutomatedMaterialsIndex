export function applyBulkEdit(items, ids, edits) {
  const idSet = new Set(ids);
  for (const it of items) {
    if (!idSet.has(it.id)) continue;
    if (edits.category != null && edits.category !== "") it.edited.category = edits.category;
    if (edits.subcategory != null && edits.subcategory !== "") it.edited.subcategory = edits.subcategory;
    if (edits.addFacet) {
      const set = new Set(it.edited.facets ?? []);
      set.add(edits.addFacet);
      it.edited.facets = [...set];
    }
    if (edits.removeFacet) {
      it.edited.facets = (it.edited.facets ?? []).filter(f => f !== edits.removeFacet);
    }
    it.dirty = !isEqual(it.baseline, it.edited);
  }
}

export function setTooltipLines(item, linesText) {
  item.edited.tooltipLines = linesText.split("\n").map(l => l.trim()).filter(l => l.length > 0);
  item.dirty = !isEqual(item.baseline, item.edited);
}

function isEqual(a, b) {
  return JSON.stringify(a) === JSON.stringify(b);
}
