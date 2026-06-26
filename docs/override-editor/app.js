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
let tableReady = false;

function initGrid() {
  ({ table } = createGrid(el("grid-host"), {
    onSelect: id => openSingleEdit(state.items.find(i => i.id === id)),
    onSelectionChange: () => {},
    onBuilt: () => { tableReady = true; refreshGrid(); },
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
  initTomSelect();
  refreshTomSelectOptions();
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
  if (!table || !tableReady) return;
  table.setData(applyFilters().map(toRow));
}

function refreshIssues() {
  const patch = computeSparsePatch(state.items, state.overrides);
  const issues = validate({ ...patch, items: patch.items }, state.dump);
  el("issues").innerHTML = issues.map(i =>
    `<div class="issue-${i.severity}">[${i.severity}] ${escapeHtml(i.itemId ?? "")} ${escapeHtml(i.message)}</div>`
  ).join("");
}

// ── Single-edit dialog ────────────────────────────────────
let currentEditItem = null;

function openSingleEdit(item) {
  if (!item) return;
  currentEditItem = item;
  el("se-title").textContent = `${item.displayName} — ${item.id}`;

  const dialog = el("single-edit");
  if (dialogPos.left !== null) {
    dialog.style.transform = "none";
    dialog.style.left = dialogPos.left + "px";
    dialog.style.top  = dialogPos.top  + "px";
  }
  dialog.hidden = false;

  // Tom Select needs the element visible before init
  initDialogTomSelect();

  tsCat.setValue(item.edited.category    ?? "", true);
  tsSub.setValue(item.edited.subcategory ?? "", true);
  tsFacets.setValue(item.edited.facets   ?? [], true);
  el("se-tooltip").value = (item.edited.tooltipLines ?? []).join("\n");
}

// ── Filters ───────────────────────────────────────────────
el("filter-text").addEventListener("input",  e => { state.filter.text      = e.target.value;   refreshGrid(); });
el("filter-mod").addEventListener("change",  e => { state.filter.mod       = e.target.value;   refreshGrid(); });
el("filter-category").addEventListener("change", e => { state.filter.category = e.target.value; refreshGrid(); });
el("filter-dirty-only").addEventListener("change", e => { state.filter.dirtyOnly = e.target.checked; refreshGrid(); });

// ── Bulk edit ─────────────────────────────────────────────
el("bulk-apply").addEventListener("click", () => {
  const selected = table ? table.getSelectedData().map(r => r._id) : [];
  applyBulkEdit(state.items, selected, {
    category:    tsBulkCat         ? (tsBulkCat.getValue()         || null) : null,
    subcategory: tsBulkSub         ? (tsBulkSub.getValue()         || null) : null,
    addFacet:    tsBulkAddFacet    ? (tsBulkAddFacet.getValue()    || null) : null,
    removeFacet: tsBulkRemoveFacet ? (tsBulkRemoveFacet.getValue() || null) : null,
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

// ── Tom Select helpers ────────────────────────────────────
function makeSingleTS(id, opts = []) {
  return new TomSelect(`#${id}`, {
    options: opts.map(v => ({ value: v, text: v })),
    create: true,
    allowEmptyOption: true,
    maxOptions: 600,
  });
}

function makeMultiTS(id, opts = []) {
  return new TomSelect(`#${id}`, {
    options: opts.map(v => ({ value: v, text: v })),
    create: true,
    plugins: ["remove_button"],
    maxOptions: 600,
  });
}

let tsCat = null, tsSub = null, tsFacets = null;
let tsBulkCat = null, tsBulkSub = null, tsBulkAddFacet = null, tsBulkRemoveFacet = null;

function dumpCategories()    { return [...new Set(state.items.map(i => i.edited.category).filter(Boolean))].sort(); }
function dumpSubcategories() { return [...new Set(state.items.map(i => i.edited.subcategory).filter(Boolean))].sort(); }

function initTomSelect() {
  if (tsBulkCat) return;
  tsBulkCat         = makeSingleTS("bulk-category",     dumpCategories());
  tsBulkSub         = makeSingleTS("bulk-subcategory",  dumpSubcategories());
  tsBulkAddFacet    = makeSingleTS("bulk-add-facet",    KNOWN_FACETS);
  tsBulkRemoveFacet = makeSingleTS("bulk-remove-facet", KNOWN_FACETS);
}

function initDialogTomSelect() {
  if (tsCat) return;
  tsCat    = makeSingleTS("se-cat",    dumpCategories());
  tsSub    = makeSingleTS("se-sub",    dumpSubcategories());
  tsFacets = makeMultiTS("se-facets",  KNOWN_FACETS);
}

function refreshTomSelectOptions() {
  if (!tsBulkCat) return;
  const cats = dumpCategories();
  const subs = dumpSubcategories();
  [tsBulkCat, ...(tsCat ? [tsCat] : [])].forEach(ts => {
    ts.clearOptions();
    cats.forEach(v => ts.addOption({ value: v, text: v }));
  });
  [tsBulkSub, ...(tsSub ? [tsSub] : [])].forEach(ts => {
    ts.clearOptions();
    subs.forEach(v => ts.addOption({ value: v, text: v }));
  });
}

// ── Dialog drag ───────────────────────────────────────────
const dialogPos = { left: null, top: null };

function initDialog() {
  const dialog   = el("single-edit");
  const titlebar = el("se-titlebar");

  titlebar.addEventListener("mousedown", e => {
    if (e.target === el("se-close")) return;
    e.preventDefault();
    const rect = dialog.getBoundingClientRect();
    const ox = e.clientX - rect.left;
    const oy = e.clientY - rect.top;
    dialog.style.transform = "none";

    function onMove(ev) {
      let left = ev.clientX - ox;
      let top  = ev.clientY - oy;
      left = Math.max(0, Math.min(left, window.innerWidth  - dialog.offsetWidth));
      top  = Math.max(0, Math.min(top,  window.innerHeight - dialog.offsetHeight));
      dialog.style.left = left + "px";
      dialog.style.top  = top  + "px";
      dialogPos.left = left;
      dialogPos.top  = top;
    }

    function onUp() {
      document.removeEventListener("mousemove", onMove);
      document.removeEventListener("mouseup",   onUp);
    }

    document.addEventListener("mousemove", onMove);
    document.addEventListener("mouseup",   onUp);
  });

  el("se-close").addEventListener("click", () => { dialog.hidden = true; });

  el("se-apply").addEventListener("click", () => {
    if (!currentEditItem) return;
    currentEditItem.edited.category    = tsCat.getValue()    || null;
    currentEditItem.edited.subcategory = tsSub.getValue()    || null;
    currentEditItem.edited.facets      = tsFacets.getValue();
    setTooltipLines(currentEditItem, el("se-tooltip").value);
    const b = currentEditItem.baseline, e = currentEditItem.edited;
    const bFacets = [...(b.facets ?? [])].sort().join("\0");
    const eFacets = [...(e.facets ?? [])].sort().join("\0");
    currentEditItem.dirty =
      (b.category ?? null) !== (e.category ?? null) ||
      (b.subcategory ?? null) !== (e.subcategory ?? null) ||
      bFacets !== eFacets ||
      JSON.stringify(b.tooltipLines ?? []) !== JSON.stringify(e.tooltipLines ?? []);
    refreshGrid();
    refreshIssues();
  });
}

initDialog();
