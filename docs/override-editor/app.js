import { parseRegistryDump, parseOverrides } from "./lib/load.js";
import { mergeForEditing } from "./lib/merge.js";
import { computeSparsePatch } from "./lib/diff.js";
import { validate } from "./lib/validate.js";
import { createGrid, toRow } from "./lib/grid.js";
import { applyBulkEdit, setTooltipLines } from "./lib/edit.js";
import { KNOWN_FACETS } from "./lib/constants.js";

const state = {
  dump: null,
  overrides: { schemaVersion: 1, items: new Map(), modPatterns: [] },
  items: [],
  filter: { text: "", mod: "", category: "", dirtyOnly: false },
};

const el = id => document.getElementById(id);
const setStatus = m => el("status").textContent = m;

// ── Grid ──────────────────────────────────────────────────
let table = null;

function initGrid() {
  ({ table } = createGrid(el("grid-host"), {
    onSelect: id => openSingleEdit(state.items.find(i => i.id === id)),
    onSelectionChange: () => {},
  }));
}

// ── File loading ──────────────────────────────────────────
async function readFile(input) {
  const f = input.files[0];
  if (!f) return null;
  return f.text();
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
  if (!table) initGrid();
  populateFilterOptions();
  refreshGrid();
  refreshIssues();
  el("download").disabled = false;
}

function populateFilterOptions() {
  const mods = [...new Set(state.items.map(i => i.mod))].sort();
  const cats = [...new Set(state.items.map(i => i.edited.category).filter(Boolean))].sort();
  el("filter-mod").innerHTML = `<option value="">(all mods)</option>` +
    mods.map(m => `<option>${escapeHtml(m)}</option>`).join("");
  el("filter-category").innerHTML = `<option value="">(all categories)</option>` +
    cats.map(c => `<option>${escapeHtml(c)}</option>`).join("");
}

function applyFilters() {
  const { text, mod, category, dirtyOnly } = state.filter;
  const lower = text.toLowerCase();
  return state.items.filter(i =>
    (!mod      || i.mod === mod) &&
    (!category || i.edited.category === category) &&
    (!dirtyOnly || i.dirty) &&
    (!text ||
      i.id.toLowerCase().includes(lower) ||
      i.displayName.toLowerCase().includes(lower) ||
      i.mod.toLowerCase().includes(lower))
  );
}

function refreshGrid() {
  if (!table) return;
  table.setData(applyFilters().map(toRow));
}

function refreshIssues() {
  const patch = computeSparsePatch(state.items, state.overrides);
  const issues = validate({ ...patch, items: patch.items }, state.dump);
  el("issues").innerHTML = issues.map(i =>
    `<div class="issue-${i.severity}">[${i.severity}] ${escapeHtml(i.itemId ?? "")} ${escapeHtml(i.message)}</div>`
  ).join("");
}

// ── Single-edit dialog (stub — Tom Select wired in Task 6) ─
let currentEditItem = null;

function openSingleEdit(item) {
  if (!item) return;
  currentEditItem = item;
  el("se-title").textContent = `${item.displayName} — ${item.id}`;
  el("single-edit").hidden = false;
}

// ── Filters ───────────────────────────────────────────────
el("filter-text").addEventListener("input",  e => { state.filter.text      = e.target.value;   refreshGrid(); });
el("filter-mod").addEventListener("change",  e => { state.filter.mod       = e.target.value;   refreshGrid(); });
el("filter-category").addEventListener("change", e => { state.filter.category = e.target.value; refreshGrid(); });
el("filter-dirty-only").addEventListener("change", e => { state.filter.dirtyOnly = e.target.checked; refreshGrid(); });

// ── Bulk edit (Tom Select values wired in Task 6) ─────────
el("bulk-apply").addEventListener("click", () => {
  const selected = table ? table.getSelectedData().map(r => r._id) : [];
  applyBulkEdit(state.items, selected, {
    category:    el("bulk-category").value    || null,
    subcategory: el("bulk-subcategory").value || null,
    addFacet:    el("bulk-add-facet").value   || null,
    removeFacet: el("bulk-remove-facet").value || null,
  });
  refreshGrid();
  refreshIssues();
});

// ── Download ──────────────────────────────────────────────
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
