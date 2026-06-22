"""Guard: verify proposals.jsonl conforms to the proposal schema before apply."""
import sys

import schema

SCOPES = {"item", "modPattern"}
DECISIONS = {"pending", "approve", "reject"}


def _is_str_list(value):
    return isinstance(value, list) and all(isinstance(v, str) for v in value)


def validate_proposal(row):
    errors = []
    rid = row.get("id")
    if not isinstance(rid, str) or ":" not in rid:
        errors.append("id must be a namespaced string")
    scope = row.get("scope")
    if scope not in SCOPES:
        errors.append(f"scope must be one of {sorted(SCOPES)}")
    if row.get("decision") not in DECISIONS:
        errors.append(f"decision must be one of {sorted(DECISIONS)}")
    override = row.get("override")
    if not isinstance(override, dict):
        errors.append("override must be an object")
        return errors

    if scope == "item":
        category = override.get("category")
        if category is not None and (not isinstance(category, str) or not category.strip()):
            errors.append("item category, when present, must be a non-empty string")
        add = override.get("addFacets", [])
        rem = override.get("removeFacets", [])
        if not _is_str_list(add) or not _is_str_list(rem):
            errors.append("addFacets/removeFacets must be string arrays")
        if not (category and category.strip()) and not add and not rem:
            errors.append("item override must change category, addFacets, or removeFacets")
    elif scope == "modPattern":
        mod = override.get("mod")
        if not isinstance(mod, str) or not mod.strip():
            errors.append("modPattern mod must be a non-empty string")
        path_tokens = override.get("pathTokens", [])
        class_tokens = override.get("classTokens", [])
        if not _is_str_list(path_tokens):
            errors.append("pathTokens must be a string array")
        else:
            for token in path_tokens:
                if "_" in token or "/" in token:
                    errors.append(f"pathToken '{token}' must be a single token (no '_' or '/')")
        if not _is_str_list(class_tokens):
            errors.append("classTokens must be a string array")
        if not path_tokens and not class_tokens:
            errors.append("modPattern must have at least one pathToken or classToken")
        category = override.get("category")
        add = override.get("addFacets", [])
        rem = override.get("removeFacets", [])
        if not _is_str_list(add) or not _is_str_list(rem):
            errors.append("addFacets/removeFacets must be string arrays")
        has_category = isinstance(category, str) and bool(category.strip())
        if category is not None and not has_category:
            errors.append("modPattern category, when present, must be a non-empty string")
        if not has_category and not add and not rem:
            errors.append("modPattern must set category, addFacets, or removeFacets")
    return errors


def validate_file(path):
    all_errors = []
    for index, row in enumerate(schema.read_jsonl(path), start=1):
        for error in validate_proposal(row):
            all_errors.append(f"line {index} ({row.get('id', '?')}): {error}")
    return all_errors


if __name__ == "__main__":
    errors = validate_file(sys.argv[1])
    for error in errors:
        print(error)
    if errors:
        print(f"{len(errors)} error(s)")
        sys.exit(1)
    print("proposals valid")
