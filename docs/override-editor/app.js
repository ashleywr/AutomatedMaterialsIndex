import { parseRegistryDump, parseOverrides } from "./lib/load.js";
import { mergeForEditing } from "./lib/merge.js";
import { computeSparsePatch } from "./lib/diff.js";
import { validate } from "./lib/validate.js";
import { renderGrid } from "./lib/grid.js";
import { applyBulkEdit, setTooltipLines } from "./lib/edit.js";

const state = {
  dump: null,
  overrides: { schemaVersion: 1, items: new Map(), modPatterns: [] },
  items: [],
  filter: { text: "", mod: "", category: "", dirtyOnly: false },
};

const el = id => document.getElementById(id);
const setStatus = m => el("status").textContent = m;

async function readFile(input) {
  const f = input.files[0];
  if (!f) return null;
  return await f.text();
}

el("dump-input").addEventListener("change", async e => {
  const text = await readFile(e.target);
  if (!text) return;
  try {
    state.dump = parseRegistryDump(text);
    setStatus(`Dump: ${state.dump.items.length} items loaded.`);
    rebuild();
  } catch (err) {
    setStatus(`Error loading dump: ${err.message}`);
  }
});

el("overrides-input").addEventListener("change", async e => {
  const text = await readFile(e.target);
  if (!text) return;
  try {
    state.overrides = parseOverrides(text);
    setStatus(`Overrides: ${state.overrides.items.size} items + ${state.overrides.modPatterns.length} patterns.`);
    rebuild();
  } catch (err) {
    setStatus(`Error loading overrides: ${err.message}`);
  }
});

function rebuild() {
  if (!state.dump) return;
  state.items = mergeForEditing(state.dump, state.overrides);
  populateFilterOptions();
  refreshGrid();
  refreshIssues();
  el("download").disabled = false;
}

function populateFilterOptions() {
  const mods = new Set(state.items.map(i => i.mod));
  const cats = new Set(state.items.map(i => i.edited.category).filter(Boolean));
  el("filter-mod").innerHTML = `<option value="">(all mods)</option>` +
    [...mods].sort().map(m => `<option>${m}</option>`).join("");
  el("filter-category").innerHTML = `<option value="">(all categories)</option>` +
    [...cats].sort().map(c => `<option>${c}</option>`).join("");
}

function applyFilters() {
  const { text, mod, category, dirtyOnly } = state.filter;
  const lower = text.toLowerCase();
  return state.items.filter(i =>
    (!mod || i.mod === mod) &&
    (!category || i.edited.category === category) &&
    (!dirtyOnly || i.dirty) &&
    (!text ||
      i.id.toLowerCase().includes(lower) ||
      i.displayName.toLowerCase().includes(lower) ||
      i.mod.toLowerCase().includes(lower))
  );
}

function refreshGrid() {
  renderGrid(el("grid-host"), applyFilters(), { onSelect: openSingleEdit });
}

function refreshIssues() {
  const patch = computeSparsePatch(state.items, state.overrides);
  const issues = validate({ ...patch, items: patch.items }, state.dump);
  el("issues").innerHTML = issues.map(i =>
    `<div class="issue-${i.severity}">[${i.severity}] ${escapeHtml(i.itemId ?? "")} ${escapeHtml(i.message)}</div>`
  ).join("");
}

function openSingleEdit(item) {
  const host = el("single-edit");
  host.hidden = false;
  el("single-edit-body").innerHTML = `
    <div><strong>${escapeHtml(item.id)}</strong> (${escapeHtml(item.displayName)})</div>
    <label>Category <input id="se-cat" value="${escapeHtml(item.edited.category ?? "")}"></label>
    <label>Subcategory <input id="se-sub" value="${escapeHtml(item.edited.subcategory ?? "")}"></label>
    <label>Tooltip lines (one per line):<br>
      <textarea id="se-tooltip" rows="4" cols="60">${escapeHtml((item.edited.tooltipLines ?? []).join("\n"))}</textarea></label>
    <button id="se-apply">Apply</button>`;
  el("se-apply").addEventListener("click", () => {
    item.edited.category = el("se-cat").value || null;
    item.edited.subcategory = el("se-sub").value || null;
    setTooltipLines(item, el("se-tooltip").value);
    item.dirty = JSON.stringify(item.baseline) !== JSON.stringify(item.edited);
    refreshGrid();
    refreshIssues();
  });
}

el("filter-text").addEventListener("input", e => { state.filter.text = e.target.value; refreshGrid(); });
el("filter-mod").addEventListener("change", e => { state.filter.mod = e.target.value; refreshGrid(); });
el("filter-category").addEventListener("change", e => { state.filter.category = e.target.value; refreshGrid(); });
el("filter-dirty-only").addEventListener("change", e => { state.filter.dirtyOnly = e.target.checked; refreshGrid(); });

el("bulk-apply").addEventListener("click", () => {
  const selected = state.items.filter(i => i._selected).map(i => i.id);
  applyBulkEdit(state.items, selected, {
    category: el("bulk-category").value,
    subcategory: el("bulk-subcategory").value,
    addFacet: el("bulk-add-facet").value,
    removeFacet: el("bulk-remove-facet").value,
  });
  refreshGrid();
  refreshIssues();
});

el("download").addEventListener("click", () => {
  const patch = computeSparsePatch(state.items, state.overrides);
  const blob = new Blob([JSON.stringify(patch, null, 2)], { type: "application/json" });
  const a = document.createElement("a");
  a.href = URL.createObjectURL(blob);
  a.download = "overrides.json";
  a.click();
  URL.revokeObjectURL(a.href);
});

function escapeHtml(s) {
  return String(s ?? "").replace(/[&<>"]/g, c =>
    ({ "&": "&amp;", "<": "&lt;", ">": "&gt;", '"': "&quot;" }[c]));
}
