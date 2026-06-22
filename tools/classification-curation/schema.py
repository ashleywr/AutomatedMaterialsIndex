"""Shared record IO and parsing helpers for the classification-curation pipeline."""
import json

WEAK_PHASES = {"fallback", "evidence_fallback", "compat_fallback", "unknown"}

# Curator-extensible: when an item sits in a creative tab listed here but its
# classified category is not in the allowed set, detect.py flags a contradiction.
# Seeded only with the relationship verified from the dump (Combat -> armor/tools).
TAB_EXPECTED_CATEGORIES = {
    "Combat": {"armor", "tools"},
}


def read_jsonl(path):
    rows = []
    with open(path, "r", encoding="utf-8") as f:
        for line in f:
            line = line.strip()
            if not line:
                continue
            rows.append(json.loads(line))
    return rows


def write_jsonl(path, rows):
    with open(path, "w", encoding="utf-8") as f:
        for row in rows:
            f.write(json.dumps(row, ensure_ascii=False))
            f.write("\n")


def split_id(rid):
    if ":" in rid:
        namespace, path = rid.split(":", 1)
        return namespace, path
    return "minecraft", rid


def csv_list(value):
    if not value:
        return []
    if value == "None":
        return []
    return [part for part in value.split(",") if part]
