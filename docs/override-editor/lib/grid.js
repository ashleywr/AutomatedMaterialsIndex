export function createGrid(host, { onSelectionChange, onEditItem, onCellEdited, getOptions, onBuilt }) {
  const table = new Tabulator(host, {
    data: [],
    index: "_id",
    height: "100%",
    layout: "fitDataFill",
    renderVertical: "virtual",
    selectable: true,
    rowFormatter(row) {
      const d = row.getData();
      const el = row.getElement();
      el.classList.toggle("row-dirty",   !!d._dirty);
      el.classList.toggle("row-missing", !!d._missing);
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
        width: 36,
        cellClick(e) { e.stopPropagation(); },
      },
      { title: "ID",    field: "id",          sorter: "string", width: 195 },
      { title: "Name",  field: "displayName", sorter: "string", width: 145 },
      { title: "Mod",   field: "mod",         sorter: "string", width: 80  },
      {
        title: "Category",
        field: "editedCategory",
        sorter: "string",
        width: 125,
        editor: "list",
        editorParams: {
          values: () => getOptions.categories(),
          autocomplete: true,
          clearable: true,
          listOnEmpty: true,
          emptyValue: "",
          allowEmpty: true,
          defaultValue: "",
          placeholderEmpty: "(none)",
          placeholderLoading: "",
        },
        cellEdited(cell) {
          onCellEdited(cell.getData()._id, "category", cell.getValue() || null);
        },
      },
      {
        title: "Subcategory",
        field: "editedSubcategory",
        sorter: "string",
        width: 125,
        editor: "list",
        editorParams: {
          values: () => getOptions.subcategories(),
          autocomplete: true,
          clearable: true,
          listOnEmpty: true,
          emptyValue: "",
          allowEmpty: true,
          defaultValue: "",
          placeholderEmpty: "(none)",
          placeholderLoading: "",
        },
        cellEdited(cell) {
          onCellEdited(cell.getData()._id, "subcategory", cell.getValue() || null);
        },
      },
      { title: "Facets", field: "editedFacets", sorter: "string", minWidth: 120 },
      { title: "Tips",   field: "tooltipCount", sorter: "number", width: 44, hozAlign: "center" },
      {
        title: "", field: "_id", width: 34, headerSort: false, hozAlign: "center",
        formatter: () => `<span class="row-edit-btn" title="Edit facets &amp; tooltip">✎</span>`,
        cellClick(_e, cell) { onEditItem(cell.getData()._id); },
      },
    ],
  });
  if (onBuilt) table.on("tableBuilt", onBuilt);
  return { table };
}

export function toRow(item) {
  return {
    _id:               item.id,
    _dirty:            item.dirty,
    _missing:          item.missingFromDump,
    id:                item.id,
    displayName:       item.displayName,
    mod:               item.mod,
    editedCategory:    item.edited.category    ?? "",
    editedSubcategory: item.edited.subcategory ?? "",
    editedFacets:      (item.edited.facets ?? []).join(", "),
    tooltipCount:      (item.edited.tooltipLines ?? []).length,
  };
}
