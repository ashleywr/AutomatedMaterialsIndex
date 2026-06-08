# Mod Compat Smoke Checklist

This is the minimum runtime bar for AMI mod compat work before merging into `feature/1.4.0-integration`.

Goal:

- prove the client still boots with the target recipe viewer
- prove AMI can search the compat content
- prove clicking a compat result does not crash or wedge the UI
- keep AI usage near zero when everything is healthy

Use deterministic JVM tests first. Only use runtime smoke for loader, UI, recipe-viewer, or runtime action contracts.

## Default Policy

1. Run focused JVM compat tests first.
2. Smoke the target compat in both recipe viewer modes:
   - EMI
   - JEI
3. Smoke both loaders when the compat is intended to work on both:
   - NeoForge
   - Forge
4. Only involve AI when:
   - the client crashes
   - expected results are missing
   - clicking a result behaves incorrectly
   - logs contain a real AMI stack trace or repeated error spam

## Runtime Matrix

Use this default matrix for cross-loader compat work:

| Loader | Viewer | Minimum runtime check |
| --- | --- | --- |
| NeoForge | EMI | Required |
| NeoForge | JEI | Required |
| Forge | EMI | Required |
| Forge | JEI | Required |

If a compat change is explicitly loader-specific, document the reduced scope in the branch notes or PR.

## Waystones Sentinel

Waystones is the default sentinel compat for validating the smoke workflow because this branch already has focused
classification coverage and a recorded NeoForge support ledger entry.

Suggested search probes:

- `waystone`
- `warp stone`
- `warp dust`

Minimum pass conditions:

- the client boots and reaches the title screen or a world
- opening inventory does not crash
- AMI search returns the expected Waystones items
- left-clicking a result does not crash
- right-clicking or opening the result's normal AMI interaction path does not crash
- no obvious AMI error spam appears in the log

Soft fail:

- results exist but are misgrouped, mislabeled, or awkward to use

Hard fail:

- crash
- missing expected search results
- result click breaks the screen flow
- repeated AMI exception or recipe-viewer exception tied to the compat path

## Cheapest Validation Order

1. Run the narrowest relevant JVM test class.
2. Build and deploy the AMI jars to the local Prism instances.
3. Boot the required loader/viewer combinations.
4. Perform the fixed search-and-click probes.
5. Stop there if everything passes.

Use AI only for failed logs, screenshots, or dump inspection.

## Current Local Hooks

Targeted test example:

```powershell
.\gradlew.bat :neoforge:test --tests com.sanhiruzu.ami.index.WaystonesCompatTest
```

Prism jar deploy example:

```powershell
.\gradlew.bat deployPrismCompatJars
```

Current Prism tasks update:

- `AMICompat` for NeoForge
- `AMICompatForge` for Forge

Current AutoMine coverage is NeoForge-oriented. Prefer it for repeatable NeoForge UI smoke. Forge can remain manual until
the repeated manual path becomes expensive enough to justify automation.

Repo entry point for the default smoke flow:

```powershell
.\scripts\smoke-mod-compat.ps1
```

Launch a single exact run from the matrix:

```powershell
.\scripts\smoke-mod-compat.ps1 -Loader neoforge -Viewer emi -Launch
```

Stop only the AMI smoke clients launched by the script for this repo:

```powershell
.\scripts\smoke-mod-compat.ps1 -StopManagedClients
```

Launch all four loader/viewer combinations only when you explicitly want parallel clients:

```powershell
.\scripts\smoke-mod-compat.ps1 -LaunchAll -AllowParallelLaunches -StopExistingClients
```

## Recording Exact Support

When a concrete upstream mod build was actually booted and checked, update
`docs/compat-support-matrix.md` in the same branch so release claims stay tied to real tested versions.
