export function createGrid(host, { onSelect, onSelectionChange, onBuilt }) {
  const table = new Tabulator(host, {
    data: [],
    height: "100%",
    layout: "fitDataFill",
    renderVertical: "virtual",
    selectable: true,
    tableBuilt() { if (onBuilt) onBuilt(); },
    rowFormatter(row) {
      const d = row.getData();
      const el = row.getElement();
      el.classList.toggle("row-dirty",   !!d._dirty);
      el.classList.toggle("row-missing", !!d._missing);
    },
    rowClick(e, row) {
      if (e.target.type === "checkbox") return;
      onSelect(row.getData()._id);
    },
    rowSelectionChanged(_data, _rows) {
      onSelectionChange();
    },
    columns: [
      {
        formatter: "rowSelection",
        titleFormatter: "rowSelection",
        hozAlign: "center",
        headerSort: false,
        width: 40,
      },
      { title: "ID",          field: "id",               sorter: "string", width: 220 },
      { title: "Name",        field: "displayName",      sorter: "string", width: 180 },
      { title: "Mod",         field: "mod",              sorter: "string", width: 100 },
      { title: "Category",    field: "editedCategory",   sorter: "string", width: 130 },
      { title: "Subcategory", field: "editedSubcategory",sorter: "string", width: 130 },
      { title: "Facets",      field: "editedFacets",     sorter: "string", minWidth: 180 },
      { title: "Tips",        field: "tooltipCount",     sorter: "number", width: 55, hozAlign: "center" },
    ],
  });
  return { table };
}

export function toRow(item) {
  return {
    _id:              item.id,
    _dirty:           item.dirty,
    _missing:         item.missingFromDump,
    id:               item.id,
    displayName:      item.displayName,
    mod:              item.mod,
    editedCategory:   item.edited.category    ?? "",
    editedSubcategory:item.edited.subcategory ?? "",
    editedFacets:     (item.edited.facets ?? []).join(", "),
    tooltipCount:     (item.edited.tooltipLines ?? []).length,
  };
}
