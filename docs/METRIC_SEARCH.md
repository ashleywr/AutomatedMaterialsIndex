# Metric Search

AMI indexes some item metrics as plain `SearchNode` metadata so the frontend can display, sort, and filter them without recalculating values while the player types.

## Indexed Fields

| Metadata key | Meaning | Unit | Source |
| --- | --- | --- | --- |
| `dps` | Estimated baseline damage per second | damage/second | Main-hand attack damage and attack speed attributes |
| `emsCapacity` | Equivalent Stack Metric storage capacity | item units | Container components, item-handler capabilities, vanilla rules, explicit adapters |

## Query Syntax

Metric filters use numeric UQL tokens:

| Query | Meaning |
| --- | --- |
| `>dps:10` | Items with estimated DPS at least 10 |
| `<dps:6` | Items with estimated DPS at most 6 |
| `=dps:8` | Items with estimated DPS equal to 8 |
| `>storage:10000` | Storage items with capacity at least 10,000 item units |
| `<storage:4096` | Storage items with capacity at most 4,096 item units |
| `>esm:10000` | Same as `>storage:10000` |
| `>10000` | Shorthand for `>storage:10000` |
| `sword >dps:8` | Normal text search refined by a DPS filter |

Comparisons are inclusive: `>dps:10` means `dps >= 10`, and `<storage:4096` means `emsCapacity <= 4096`.

## Frontend Usage

The frontend should read metrics from `SearchNode.meta(...)`:

- DPS: `node.meta(SearchNodeKeys.DPS, "")`
- Storage capacity: `node.meta(SearchNodeKeys.ESM_CAPACITY, "")`

Empty string means AMI did not have enough confidence to index that metric. Treat it as unknown, not zero.

The results toolbar exposes these sort fields:

- `ResultsProcessor.SortField.DPS`
- `ResultsProcessor.SortField.STORAGE_CAPACITY`

Rows can display the same data through `RowField.DPS` and `RowField.STORAGE_CAPACITY`.

## Player-Facing Wording

Suggested tooltip/wiki wording:

**Estimated DPS** is a baseline comparison from the item's attack damage and attack speed attributes. It does not include enchantments, conditional mod effects, target-specific bonuses, or scripted procs.

**Storage** is shown as item capacity. A normal chest is `1728 items` because it has 27 slots and each slot normally holds 64 items.

## Adapter Policy

Storage adapters should only write `emsCapacity` when the number is defensible. Unknown storage systems should leave the field absent. Good adapter candidates:

- item-handler capability slot limits
- vanilla shulker boxes, chests, barrels
- Sophisticated Backpacks tiers
- Functional Storage drawers
- Applied Energistics 2 and Refined Storage cells, using explicit bytes/types conversion rules

Avoid executing arbitrary mod behavior during query time. Metrics should be computed during indexing and stored as metadata.

## Compatibility Roadmap

Storage compatibility should live behind `StorageMetricAdapter` implementations, registered through `StorageMetricAdapters`. Keep each mod's conversion rules isolated so a bad assumption for one storage system does not affect generic containers.

Planned adapters:

| Mod/system | Adapter target | Conversion notes |
| --- | --- | --- |
| Functional Storage | `FunctionalStorageMetricAdapter` | Convert drawer slot count, per-slot stack multiplier, and upgrades into total item units. |
| Applied Energistics 2 | `AppliedEnergisticsStorageMetricAdapter` | Convert storage-cell bytes and type limits into an estimated item-unit capacity. Document the bytes-per-item assumption next to the adapter. |
| Refined Storage | `RefinedStorageMetricAdapter` | Convert disk capacity to item units directly when the disk exposes a fixed item count. |
| Sophisticated Backpacks | `SophisticatedBackpacksStorageMetricAdapter` | Currently has conservative tier defaults; replace with API/config-backed values when available. |

Adapter ordering matters. Prefer specific mod adapters before generic capability rules when the generic `IItemHandler` capacity only describes currently exposed slots and misses upgrades, bytes, types, or nested storage.
