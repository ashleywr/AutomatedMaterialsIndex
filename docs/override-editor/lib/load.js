import { SCHEMA_VERSION } from "./constants.js";

export function parseRegistryDump(text) {
  const doc = JSON.parse(text);
  const version = doc.schemaVersion ?? 1;
  if (version > SCHEMA_VERSION) {
    throw new Error(`registry-dump schemaVersion ${version} is newer than tool (${SCHEMA_VERSION})`);
  }
  return { schemaVersion: version, items: Array.isArray(doc.items) ? doc.items : [] };
}

export function parseOverrides(text) {
  const doc = JSON.parse(text);
  const version = doc.schemaVersion ?? 1;
  if (version > SCHEMA_VERSION) {
    throw new Error(`overrides schemaVersion ${version} is newer than tool (${SCHEMA_VERSION})`);
  }
  const items = new Map();
  if (doc.items && typeof doc.items === "object") {
    for (const [id, entry] of Object.entries(doc.items)) {
      items.set(id, {
        category: entry.category ?? null,
        subcategory: entry.subcategory ?? null,
        addFacets: entry.addFacets ?? [],
        removeFacets: entry.removeFacets ?? [],
        tooltipLines: entry.tooltipLines ?? [],
        accessLevel: entry.accessLevel ?? null,
        visibility: entry.visibility ?? null,
      });
    }
  }
  const modPatterns = Array.isArray(doc.modPatterns) ? doc.modPatterns : [];
  return { schemaVersion: version, items, modPatterns };
}
