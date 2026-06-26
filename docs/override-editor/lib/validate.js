import { KNOWN_FACETS, SCHEMA_VERSION } from "./constants.js";

export function validate(overrides, dump) {
  const issues = [];
  if ((overrides.schemaVersion ?? 1) > SCHEMA_VERSION) {
    issues.push({ severity: "error",
      message: `overrides schemaVersion ${overrides.schemaVersion} > supported ${SCHEMA_VERSION}` });
  }
  const dumpIds = dump ? new Set((dump.items ?? []).map(r => r.id)) : null;
  const known = new Set(KNOWN_FACETS);
  for (const [id, entry] of Object.entries(overrides.items ?? {})) {
    if (dumpIds && !dumpIds.has(id)) {
      issues.push({ severity: "warn", itemId: id, message: `item ${id} not present in loaded registry dump` });
    }
    for (const f of [...(entry.addFacets ?? []), ...(entry.removeFacets ?? [])]) {
      if (!known.has(f)) {
        issues.push({ severity: "warn", itemId: id, message: `unknown facet "${f}"` });
      }
    }
  }
  return issues;
}
