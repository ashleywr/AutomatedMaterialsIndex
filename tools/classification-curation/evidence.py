"""Stage 2: assemble recipe-graph + tab + tag evidence for each candidate."""
import sys

import schema


def build_recipe_index(recipes_path):
    index = {}
    for recipe in schema.read_jsonl(recipes_path):
        inputs = []
        seen = set()
        for slot in recipe.get("inputs", []) or []:
            for alt in slot or []:
                item = alt.get("itemId")
                if item and item not in seen:
                    seen.add(item)
                    inputs.append(item)
        output = (recipe.get("output") or {}).get("itemId") or None
        index[recipe["id"]] = {"inputs": inputs, "output": output}
    return index


def build_node_index(nodes_path):
    info_by_id = {}
    edges_by_id = {}
    meta_by_id = {}
    for node in schema.read_jsonl(nodes_path):
        if node.get("type") != "ITEM":
            continue
        rid = node["id"]
        meta = node.get("metadata", {})
        info_by_id[rid] = {
            "name": node.get("displayName", ""),
            "category": (meta.get("ontologyCategory") or "").strip(),
            "subcategory": (meta.get("ontologySubcategory") or "").strip(),
        }
        edges = node.get("unresolvedEdges", {}) or {}
        edges_by_id[rid] = {
            "OUTPUT_OF": list(edges.get("OUTPUT_OF", []) or []),
            "USED_IN": list(edges.get("USED_IN", []) or []),
        }
        meta_by_id[rid] = meta
    return info_by_id, edges_by_id, meta_by_id


def _resolve(neighbor_id, info_by_id):
    info = info_by_id.get(neighbor_id)
    if info is None:
        return {"id": neighbor_id, "name": "", "category": ""}
    return {"id": neighbor_id, "name": info["name"], "category": info["category"]}


def _neighbors(ids, info_by_id, limit):
    out = []
    seen = set()
    for nid in ids:
        if nid in seen:
            continue
        seen.add(nid)
        out.append(_resolve(nid, info_by_id))
        if len(out) >= limit:
            break
    return out


def evidence_record(candidate, info_by_id, edges_by_id, recipe_index, limit=12):
    rid = candidate["id"]
    edges = edges_by_id.get(rid, {"OUTPUT_OF": [], "USED_IN": []})

    crafted_ids = []
    for recipe_id in edges["OUTPUT_OF"]:
        for item in recipe_index.get(recipe_id, {}).get("inputs", []):
            if item != rid:
                crafted_ids.append(item)

    used_ids = []
    for recipe_id in edges["USED_IN"]:
        output = recipe_index.get(recipe_id, {}).get("output")
        if output and output != rid:
            used_ids.append(output)

    info = info_by_id.get(rid, {})
    return {
        "id": rid,
        "displayName": info.get("name", ""),
        "mod": candidate["mod"],
        "path": candidate["path"],
        "current": {
            "category": candidate["category"],
            "subcategory": candidate["subcategory"],
            "routePhase": candidate["routePhase"],
            "routeRule": candidate["routeRule"],
        },
        "facets": candidate["facets"],
        "tab": {"label": candidate["tab"], "id": candidate["tabId"]},
        "tags": schema.csv_list(candidate.get("tagsCsv")),
        "detectors": candidate["detectors"],
        "runnerUp": candidate.get("runnerUp", ""),
        "craftedFrom": _neighbors(crafted_ids, info_by_id, limit),
        "usedToMake": _neighbors(used_ids, info_by_id, limit),
    }


def run(candidates_path, nodes_path, recipes_path, out_path, limit=12):
    info_by_id, edges_by_id, meta_by_id = build_node_index(nodes_path)
    recipe_index = build_recipe_index(recipes_path)
    rows = []
    for candidate in schema.read_jsonl(candidates_path):
        meta = meta_by_id.get(candidate["id"], {})
        candidate = dict(candidate)
        candidate["tagsCsv"] = meta.get("tags")
        candidate["runnerUp"] = "; ".join(schema.csv_list(meta.get("classificationCandidates")))
        rows.append(evidence_record(candidate, info_by_id, edges_by_id, recipe_index, limit))
    schema.write_jsonl(out_path, rows)
    return len(rows)


if __name__ == "__main__":
    n = run(sys.argv[1], sys.argv[2], sys.argv[3], sys.argv[4])
    print(f"wrote {n} evidence rows to {sys.argv[4]}")
