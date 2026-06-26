# AMI Override Editor

Browser-based editor for the `overrides.json` file that AMI loads from a modpack's `config/ami/` directory.

## Use it

1. In-game: run `/ami dump`. Look in `<instance>/ami_dumps/registry/registry-dump.json`.
2. Open https://<your-github-username>.github.io/<repo>/override-editor/.
3. Drop in `registry-dump.json`. Optionally drop in an existing `overrides.json` to keep editing.
4. Filter / select / bulk-edit. Per-item edits via row click.
5. Click **Download overrides.json**, place it in `<instance>/config/ami/overrides.json`.
6. In-game: `/ami reindex` (or restart) to apply.

## Notes

- Pack overrides win over mod-shipped defaults — your file is the highest-priority layer.
- Tool refuses to load files with a `schemaVersion` newer than it supports.
- Stale-id warnings show which override entries reference items not in your current dump.
- v1 lets you edit per-item category / subcategory / facets / custom tooltip lines. `modPatterns` are passed through unchanged.
