// Initial implementation renders all rows; if performance is poor with 20k items,
// swap renderAll for windowed rendering (track scrollTop, only render visible slice).
export function renderGrid(host, items, { onSelect }) {
  const tbody = document.createElement("tbody");
  for (const it of items) {
    const tr = document.createElement("tr");
    tr.dataset.id = it.id;
    if (it.dirty) tr.classList.add("dirty");
    if (it.missingFromDump) tr.classList.add("missing-dump");
    tr.innerHTML = `
      <td><input type="checkbox" class="select" ${it._selected ? "checked" : ""}></td>
      <td>${escapeHtml(it.id)}</td>
      <td>${escapeHtml(it.displayName)}</td>
      <td>${escapeHtml(it.mod)}</td>
      <td>${escapeHtml(it.edited.category ?? "")}</td>
      <td>${escapeHtml(it.edited.subcategory ?? "")}</td>
      <td>${escapeHtml((it.edited.facets ?? []).join(", "))}</td>
      <td>${(it.edited.tooltipLines ?? []).length}</td>`;
    tr.querySelector(".select").addEventListener("change", e => {
      it._selected = e.target.checked;
    });
    tr.addEventListener("click", e => {
      if (e.target.tagName !== "INPUT") onSelect(it);
    });
    tbody.appendChild(tr);
  }
  host.innerHTML = `<table>
    <thead><tr><th></th><th>id</th><th>name</th><th>mod</th>
      <th>category</th><th>subcategory</th><th>facets</th><th>tooltipLines</th></tr></thead></table>`;
  host.querySelector("table").appendChild(tbody);
}

function escapeHtml(s) {
  return String(s ?? "").replace(/[&<>"]/g, c =>
    ({ "&": "&amp;", "<": "&lt;", ">": "&gt;", '"': "&quot;" }[c]));
}
