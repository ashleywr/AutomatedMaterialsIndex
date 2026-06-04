# Custom Taxonomy Profiles

AMI now supports external taxonomy profiles for both pack authors and players.

## Files

- Pack-author profiles: `run/<instance>/ami/taxonomy/*.json`
- Player profile: `config/ami/taxonomy.json`

Pack profiles load first in filename order. The player profile loads last and can override category definitions or append later rules.

## In-Game Export

In AMI Pack Author Mode or dev mode, right-click a result group and use `Create Pack Files`.
AMI writes three ready-to-drop files for that group:

- `ami/ami-export-<group>-README.md`
- `ami/taxonomy/ami-export-<group>.json`
- `kubejs/server_scripts/ami/ami-export-<group>.js`

Start with the README. It explains what each file does, what to edit first, and which items were included in the export.
Use this flow when you want the simplest workflow: create files first, then review and edit them in place.

In AMI Pack Author Mode or dev mode, right-click a result group and use `Create Category Rule`.
AMI copies a conservative starter JSON profile for the selected group:

- exact item IDs by default
- inferred category and subcategory when AMI already has them
- common collapse/grouping metadata when the selected group already shares it

The export is intentionally narrow so it is safe to paste first, then widen to `mods`, `pathContains`, or `tags` once reviewed.

The same group menu also offers `Create KubeJS Removal Script`.
That copies a ready-to-paste KubeJS file fragment with one `event.remove({ output: ... })` line per item in the selected group.
It is meant as a simple starting point for hiding or replacing recipes before you move on to broader rules.

## What profiles can do

- Define custom categories and subcategories.
- Re-route items into those categories with rule-based matching.
- Optionally enable `replaceDefaults` to start from an uncategorized bucket instead of AMI's built-in item taxonomy.
- Override extra grouping metadata such as `collapseFamily`, `collapseLabel`, or `materialGroup`.
- Remove inherited grouping metadata with `removeMetadata`.

## Example

```json
{
  "replaceDefaults": true,
  "categories": {
    "automation": {
      "label": "Automation",
      "iconItem": "minecraft:comparator",
      "subcategories": {
        "diagnostics": "Diagnostics",
        "sensors": "Sensors"
      }
    }
  },
  "rules": [
    {
      "match": {
        "mods": ["create"],
        "pathContains": ["gauge"]
      },
      "category": "automation",
      "subcategory": "diagnostics"
    },
    {
      "match": {
        "tags": ["c:wrenches"]
      },
      "metadata": {
        "ontologyCategory": "automation",
        "ontologySubcategory": "sensors",
        "collapseFamily": "automation:wrenches",
        "collapseLabel": "Wrenches"
      },
      "removeMetadata": ["materialGroup"]
    }
  ]
}
```

## Match fields

Rules support these `match` fields:

- `type`
- `ids`
- `idPrefixes`
- `mods` or `modIds`
- `paths`
- `pathPrefixes`
- `pathContains`
- `displayNameContains`
- `tags`
- `facets`
- `metadata`

List fields use "match any" semantics inside that field. Different fields combine with "match all" semantics.

## Notes

- Rules run in file order after core indexing and before `fixes.json` item-specific overrides.
- `replaceDefaults` only affects item nodes. Non-item atlas nodes keep their normal AMI grouping.
- If a rule omits `match`, it applies to every item it reaches. That is useful for broad cleanup rules.
