import { parseRegistryDump, parseOverrides } from "./lib/load.js";
import { mergeForEditing } from "./lib/merge.js";
import { computeSparsePatch } from "./lib/diff.js";
import { validate } from "./lib/validate.js";
import { createGrid, toRow } from "./lib/grid.js";
import { setTooltipLines } from "./lib/edit.js";
import { KNOWN_FACETS } from "./lib/constants.js";

const state = {
  dump: null,
  overrides: { schemaVersion: 1, items: new Map(), modPatterns: [] },
  items: [],
  // null = show all
  // { cat, sub: undefined } = category (all subcategories within it)
  // { cat, sub }            = specific subcategory (sub may be null for items with no subcategory)
  view: null,
  textFilter: "",
  dirtyOnly: false,
};

const el = id => document.getElementById(id);
const setStatus = m => el("status").textContent = m;

// ── Grid ──────────────────────────────────────────────────
let table = null;
let tableReady = false;

function initGrid() {
  ({ table } = createGrid(el("grid-host"), {
    onSelectionChange: updateActionBar,
    onEditItem: id => openItemEdit(state.items.find(i => i.id === id)),
    onCellEdited(id, field, value) {
      const item = state.items.find(i => i.id === id);
      if (!item) return;
      item.edited[field] = value;
      item.dirty = computeDirty(item);
      buildTree();
      // Update only this row's dirty flag without full reload (preserves scroll + selection)
      table.updateData([toRow(item)]);
      // If the item's category changed and we're viewing a specific category,
      // it may no longer match the filter — do a full refresh in that case.
      if (field === "category" && state.view !== null) refreshGrid();
      refreshIssues();
    },
    getOptions: {
      categories: () => allCategories(),
      subcategories: () => allSubcategories(null),
    },
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
    setStatus(`Overrides: ${state.overrides.items.size} items.`);
    rebuild();
  } catch (err) {
    setStatus(`Error loading overrides: ${err.message}`);
  }
});

function rebuild() {
  if (!state.dump) return;
  state.view = null;
  state.items = mergeForEditing(state.dump, state.overrides);
  if (!table) initGrid();
  buildTree();
  refreshGrid();
  refreshIssues();
  el("download").disabled = false;
}

// ── Classification tree ───────────────────────────────────
function buildTree() {
  // Group: category (null=uncategorized) → subcategory (null=none) → count
  const tree = new Map();
  for (const item of state.items) {
    const cat = item.edited.category    ?? null;
    const sub = item.edited.subcategory ?? null;
    if (!tree.has(cat)) tree.set(cat, new Map());
    const subs = tree.get(cat);
    subs.set(sub, (subs.get(sub) || 0) + 1);
  }

  const panel = el("tree-panel");
  panel.innerHTML = "";

  // "All items" row
  const allRow = mkNode("div", "tree-all" + (state.view === null ? " active" : ""),
    "All items", state.items.length);
  allRow.addEventListener("click", () => setView(null));
  panel.appendChild(allRow);

  const cats = [...tree.keys()].sort((a, b) => {
    if (a === null) return 1;
    if (b === null) return -1;
    return a.localeCompare(b);
  });

  for (const cat of cats) {
    const subs = tree.get(cat);
    const catTotal = [...subs.values()].reduce((a, b) => a + b, 0);
    const isCatView = state.view !== null && state.view.cat === cat;
    const isCatOnly = isCatView && state.view.sub === undefined;

    const details = document.createElement("details");
    if (isCatView) details.open = true;

    const summary = mkNode("summary", "tree-cat" + (isCatOnly ? " active" : ""),
      cat ?? "(uncategorized)", catTotal);
    summary.addEventListener("click", e => {
      e.preventDefault();
      details.open = !details.open;
      setView({ cat, sub: undefined });
    });
    details.appendChild(summary);

    const subsSorted = [...subs.keys()].sort((a, b) => {
      if (a === null) return 1;
      if (b === null) return -1;
      return a.localeCompare(b);
    });

    for (const sub of subsSorted) {
      const count = subs.get(sub);
      const isSubActive = isCatView && state.view.sub === sub;
      const div = mkNode("div", "tree-sub" + (isSubActive ? " active" : ""),
        sub ?? "(none)", count);
      div.addEventListener("click", e => {
        e.stopPropagation();
        setView({ cat, sub });
      });
      details.appendChild(div);
    }

    panel.appendChild(details);
  }
}

function mkNode(tag, className, label, count) {
  const node = document.createElement(tag);
  node.className = className;
  node.innerHTML =
    `<span class="tree-label">${escapeHtml(label)}</span>` +
    `<span class="tree-count">${count}</span>`;
  return node;
}

function setView(view) {
  state.view = view;
  buildTree();
  refreshGrid();
}

// ── Filters / Grid ────────────────────────────────────────
function applyFilters() {
  const lower = state.textFilter.toLowerCase();
  return state.items.filter(i => {
    if (state.view !== null) {
      const ic = i.edited.category    ?? null;
      const is = i.edited.subcategory ?? null;
      if (ic !== state.view.cat) return false;
      if (state.view.sub !== undefined && is !== state.view.sub) return false;
    }
    if (state.dirtyOnly && !i.dirty) return false;
    if (lower && !i.id.toLowerCase().includes(lower) &&
        !i.displayName.toLowerCase().includes(lower) &&
        !i.mod.toLowerCase().includes(lower)) return false;
    return true;
  });
}

function refreshGrid() {
  if (!table || !tableReady) return;
  table.setData(applyFilters().map(toRow));
}

function refreshIssues() {
  const patch = computeSparsePatch(state.items, state.overrides);
  const issues = validate(patch, state.dump);
  el("issues").innerHTML = issues.map(i =>
    `<span class="issue-${i.severity}">[${i.severity}] ${escapeHtml(i.itemId ?? "")} ${escapeHtml(i.message)}</span>`
  ).join("");
}

// ── Toolbar filters ───────────────────────────────────────
el("filter-text").addEventListener("input", e => {
  state.textFilter = e.target.value;
  refreshGrid();
});
el("filter-dirty-only").addEventListener("change", e => {
  state.dirtyOnly = e.target.checked;
  refreshGrid();
});

// ── Action bar (bulk move for checked rows) ───────────────
let tsMoveCat = null;
let tsMoveSub = null;

function initMoveTomSelect() {
  if (tsMoveCat) return;
  tsMoveCat = new TomSelect("#move-cat", {
    options: allCategories().map(v => ({ value: v, text: v })),
    create: true,
    allowEmptyOption: true,
    placeholder: "category…",
    maxOptions: 600,
    onChange(val) {
      if (!tsMoveSub) return;
      tsMoveSub.clearOptions();
      allSubcategories(val || null).forEach(v =>
        tsMoveSub.addOption({ value: v, text: v }));
      tsMoveSub.refreshOptions(false);
    },
  });
  tsMoveSub = new TomSelect("#move-sub", {
    options: [],
    create: true,
    allowEmptyOption: true,
    placeholder: "subcategory…",
    maxOptions: 600,
  });
}

function allCategories() {
  return [...new Set(state.items.map(i => i.edited.category).filter(Boolean))].sort();
}

function allSubcategories(cat) {
  return [...new Set(
    state.items
      .filter(i => cat === null ? true : (i.edited.category ?? null) === cat)
      .map(i => i.edited.subcategory)
      .filter(Boolean)
  )].sort();
}

function updateActionBar() {
  const selected = table ? table.getSelectedData() : [];
  const bar = el("action-bar");
  if (selected.length === 0) {
    bar.hidden = true;
    return;
  }
  bar.hidden = false;
  el("sel-count").textContent =
    `${selected.length} item${selected.length !== 1 ? "s" : ""} selected`;
  initMoveTomSelect();

  // Pre-populate from the selection when all selected items share a category
  if (selected.length === 1) {
    const item = state.items.find(i => i.id === selected[0]._id);
    if (item) {
      tsMoveCat.setValue(item.edited.category ?? "", true);
      tsMoveSub.setValue(item.edited.subcategory ?? "", true);
    }
  } else {
    const cats = [...new Set(selected.map(r => r.editedCategory))];
    if (cats.length === 1) {
      tsMoveCat.setValue(cats[0] ?? "", true);
      const subs = [...new Set(selected.map(r => r.editedSubcategory))];
      if (subs.length === 1) tsMoveSub.setValue(subs[0] ?? "", true);
    }
  }
}

el("move-apply").addEventListener("click", () => {
  if (!table) return;
  const ids = new Set(table.getSelectedData().map(r => r._id));
  const newCat = tsMoveCat?.getValue() || null;
  const newSub = tsMoveSub?.getValue() || null;
  if (!newCat && !newSub) return;

  for (const item of state.items) {
    if (!ids.has(item.id)) continue;
    if (newCat !== null) item.edited.category    = newCat;
    if (newSub !== null) item.edited.subcategory = newSub;
    item.dirty = computeDirty(item);
  }
  table.deselectRow();
  buildTree();
  refreshGrid();
  refreshIssues();
});

el("sel-clear").addEventListener("click", () => {
  table?.deselectRow();
  el("action-bar").hidden = true;
});

// ── Per-item edit dialog (facets + tooltip only) ──────────
let tsFacets = null;
let currentEditItem = null;
const dialogPos = { left: null, top: null };
const dialog   = el("item-edit");
const titlebar = el("ie-titlebar");

function openItemEdit(item) {
  if (!item) return;
  currentEditItem = item;
  el("ie-title").textContent = `${item.displayName} — ${item.id}`;

  if (dialogPos.left !== null) {
    dialog.style.transform = "none";
    dialog.style.left = dialogPos.left + "px";
    dialog.style.top  = dialogPos.top  + "px";
  }
  dialog.hidden = false;

  if (!tsFacets) {
    tsFacets = new TomSelect("#ie-facets", {
      options: KNOWN_FACETS.map(v => ({ value: v, text: v })),
      create: true,
      plugins: ["remove_button"],
      maxOptions: 600,
    });
  }
  tsFacets.setValue(item.edited.facets ?? [], true);
  el("ie-tooltip").value = (item.edited.tooltipLines ?? []).join("\n");
}

// Dialog drag
titlebar.addEventListener("mousedown", e => {
  if (e.target === el("ie-close")) return;
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

el("ie-close").addEventListener("click", () => { dialog.hidden = true; });

el("ie-apply").addEventListener("click", () => {
  if (!currentEditItem) return;
  if (tsFacets) currentEditItem.edited.facets = tsFacets.getValue();
  setTooltipLines(currentEditItem, el("ie-tooltip").value);
  currentEditItem.dirty = computeDirty(currentEditItem);
  table?.updateData([toRow(currentEditItem)]);
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

// ── Helpers ───────────────────────────────────────────────
function computeDirty(item) {
  const b = item.baseline, e = item.edited;
  const bFacets = [...(b.facets ?? [])].sort().join("\0");
  const eFacets = [...(e.facets ?? [])].sort().join("\0");
  return (b.category    ?? null) !== (e.category    ?? null) ||
         (b.subcategory ?? null) !== (e.subcategory ?? null) ||
         bFacets !== eFacets ||
         JSON.stringify(b.tooltipLines ?? []) !== JSON.stringify(e.tooltipLines ?? []);
}

function escapeHtml(s) {
  return String(s ?? "").replace(/[&<>"]/g, c =>
    ({ "&": "&amp;", "<": "&lt;", ">": "&gt;", '"': "&quot;" }[c]));
}
