"""Stage 5: fold approved proposals into the bundled classification_overrides.json."""
import json
import os
import sys

import schema
import validate_proposals


def load_overrides(path):
    if not os.path.exists(path):
        return {"items": {}, "modPatterns": []}
    with open(path, "r", encoding="utf-8") as f:
        data = json.load(f)
    data.setdefault("items", {})
    data.setdefault("modPatterns", [])
    return data


def _item_entry(override):
    entry = {}
    category = override.get("category")
    if category and category.strip():
        entry["category"] = category
    subcategory = override.get("subcategory")
    if subcategory and subcategory.strip():
        entry["subcategory"] = subcategory
    if override.get("addFacets"):
        entry["addFacets"] = list(override["addFacets"])
    if override.get("removeFacets"):
        entry["removeFacets"] = list(override["removeFacets"])
    return entry


def _pattern_key(pattern):
    return (pattern["mod"],
            tuple(sorted(pattern.get("pathTokens", []))),
            tuple(sorted(pattern.get("classTokens", []))),
            pattern.get("category", ""), pattern.get("subcategory", ""),
            tuple(sorted(pattern.get("addFacets", []))),
            tuple(sorted(pattern.get("removeFacets", []))))


def merge(overrides, proposals):
    overrides = {"items": dict(overrides.get("items", {})),
                 "modPatterns": list(overrides.get("modPatterns", []))}
    reject_rows = []
    existing_pattern_keys = {_pattern_key(p) for p in overrides["modPatterns"]}

    for row in proposals:
        decision = row.get("decision")
        if decision not in ("approve", "reject"):
            continue
        if decision == "reject":
            reject_rows.append({"id": row["id"], "scope": row.get("scope", ""), "key": row["id"]})
            continue
        errors = validate_proposals.validate_proposal(row)
        if errors:
            raise ValueError(f"approved proposal {row.get('id')} invalid: {errors}")
        override = row["override"]
        if row["scope"] == "item":
            overrides["items"][row["id"]] = _item_entry(override)
        else:
            pattern = {"mod": override["mod"]}
            if override.get("pathTokens"):
                pattern["pathTokens"] = list(override["pathTokens"])
            if override.get("classTokens"):
                pattern["classTokens"] = list(override["classTokens"])
            category = override.get("category")
            if category and category.strip():
                pattern["category"] = category
            subcategory = override.get("subcategory")
            if subcategory and subcategory.strip():
                pattern["subcategory"] = subcategory
            if override.get("addFacets"):
                pattern["addFacets"] = list(override["addFacets"])
            if override.get("removeFacets"):
                pattern["removeFacets"] = list(override["removeFacets"])
            key = _pattern_key(pattern)
            if key not in existing_pattern_keys:
                existing_pattern_keys.add(key)
                overrides["modPatterns"].append(pattern)
    return overrides, reject_rows


def _write_overrides(path, overrides):
    items = dict(sorted(overrides["items"].items()))
    patterns = sorted(overrides["modPatterns"],
                      key=lambda p: (p["mod"], p.get("category", ""), p.get("subcategory", "")))
    payload = {"items": items, "modPatterns": patterns}
    with open(path, "w", encoding="utf-8") as f:
        json.dump(payload, f, ensure_ascii=False, indent=2)
        f.write("\n")


def _append_rejects(path, reject_rows):
    if not reject_rows:
        return
    with open(path, "a", encoding="utf-8") as f:
        for row in reject_rows:
            f.write(json.dumps(row, ensure_ascii=False))
            f.write("\n")


def run(proposals_path, overrides_path, reject_path):
    proposals = schema.read_jsonl(proposals_path)
    overrides = load_overrides(overrides_path)
    updated, reject_rows = merge(overrides, proposals)
    _write_overrides(overrides_path, updated)
    _append_rejects(reject_path, reject_rows)
    approved = sum(1 for r in proposals if r.get("decision") == "approve")
    return approved, len(reject_rows)


if __name__ == "__main__":
    approved, rejected = run(sys.argv[1], sys.argv[2], sys.argv[3])
    print(f"applied {approved} approved, recorded {rejected} rejected")
