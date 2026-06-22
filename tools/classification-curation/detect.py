"""Stage 1: flag weak / contradictory items from a search_nodes.jsonl dump."""
import sys

import schema


def detectors_for(node, reject_ids):
    if node.get("type") != "ITEM":
        return []
    rid = node["id"]
    if rid in reject_ids:
        return []
    meta = node.get("metadata", {})
    namespace, _ = schema.split_id(rid)
    category = (meta.get("ontologyCategory") or "").strip()
    phase = (meta.get("classificationRoutePhase") or "").strip()
    tab = (meta.get("creativeTabLabel") or "").strip()

    found = set()
    if phase in schema.WEAK_PHASES or not category:
        found.add("weak_route")
    if not schema.csv_list(meta.get("facets")):
        found.add("empty_facets")
    if category and category == namespace:
        found.add("mod_pseudo_category")
    expected = schema.TAB_EXPECTED_CATEGORIES.get(tab)
    if expected is not None and category and category not in expected:
        found.add("tab_contradiction")
    return sorted(found)


def candidate_record(node, detectors):
    meta = node.get("metadata", {})
    mod, path = schema.split_id(node["id"])
    return {
        "id": node["id"],
        "mod": mod,
        "path": path,
        "category": (meta.get("ontologyCategory") or "").strip(),
        "subcategory": (meta.get("ontologySubcategory") or "").strip(),
        "routePhase": (meta.get("classificationRoutePhase") or "").strip(),
        "routeRule": (meta.get("classificationRouteRule") or "").strip(),
        "facets": schema.csv_list(meta.get("facets")),
        "tab": (meta.get("creativeTabLabel") or "").strip(),
        "tabId": (meta.get("creativeTabId") or "").strip(),
        "detectors": detectors,
    }


def load_reject_ids(path):
    if not path:
        return set()
    try:
        return {row["id"] for row in schema.read_jsonl(path) if "id" in row}
    except FileNotFoundError:
        return set()


def run(nodes_path, out_path, reject_path):
    reject_ids = load_reject_ids(reject_path)
    rows = []
    for node in schema.read_jsonl(nodes_path):
        detectors = detectors_for(node, reject_ids)
        if detectors:
            rows.append(candidate_record(node, detectors))
    schema.write_jsonl(out_path, rows)
    return len(rows)


if __name__ == "__main__":
    nodes = sys.argv[1]
    out = sys.argv[2]
    reject = sys.argv[3] if len(sys.argv) > 3 else None
    n = run(nodes, out, reject)
    print(f"wrote {n} candidates to {out}")
