<#
.SYNOPSIS
Exports a release-ready AMI regression benchmark report from a profiler summary.

.DESCRIPTION
Reads a summary.json produced by tools/profile-synesthesia-ami.ps1 and writes a
compact Markdown report plus a machine-readable JSON payload suitable for GitHub
release assets. When a baseline summary is supplied, the report includes
regression deltas for the metrics that matter most for large modpacks.

.EXAMPLE
.\tools\export-ami-benchmark-release.ps1 `
  -Summary .\profile-runs\synesthesia-ami-20260613-120000\summary.json `
  -BaselineSummary .\profile-runs\synesthesia-ami-20260612-180000\summary.json

.EXAMPLE
.\tools\export-ami-benchmark-release.ps1 `
  -Summary .\profile-runs\synesthesia-ami-20260613-120000\summary.json `
  -BaselineSummary .\profile-runs\synesthesia-ami-20260612-180000\summary.json `
  -UploadToGitHubRelease
#>
param(
    [Parameter(Mandatory = $true)]
    [string]$Summary,
    [string]$BaselineSummary = "",
    [string]$Version = "",
    [string]$OutputDir = "release-assets/benchmarks",
    [string]$Tag = "",
    [switch]$UploadToGitHubRelease
)

Set-StrictMode -Version Latest
$ErrorActionPreference = "Stop"

$repo = Split-Path $PSScriptRoot -Parent

function Resolve-RepoPath([string]$Path) {
    if ([System.IO.Path]::IsPathRooted($Path)) {
        return $Path
    }
    return Join-Path $repo $Path
}

function Read-JsonFile([string]$Path) {
    $resolved = Resolve-RepoPath $Path
    if (-not (Test-Path $resolved)) {
        throw "File not found: $Path"
    }
    return Get-Content -Raw -Path $resolved | ConvertFrom-Json
}

function Read-ModVersion {
    $properties = Join-Path $repo "gradle.properties"
    $line = Get-Content $properties | Where-Object { $_ -like "mod_version=*" } | Select-Object -First 1
    if (-not $line) {
        throw "Could not read mod_version from gradle.properties."
    }
    return $line.Substring("mod_version=".Length).Trim()
}

function Format-Duration([object]$Millis) {
    if ($null -eq $Millis -or $Millis -eq "") {
        return "n/a"
    }
    $value = [double]$Millis
    if ($value -lt 1000) {
        return ("{0:N0} ms" -f $value)
    }
    if ($value -lt 60000) {
        return ("{0:N1} s" -f ($value / 1000.0))
    }
    return ("{0:N1} min" -f ($value / 60000.0))
}

function Format-Bytes([object]$Bytes) {
    if ($null -eq $Bytes -or $Bytes -eq "") {
        return "n/a"
    }
    $value = [double]$Bytes
    $units = @("B", "KiB", "MiB", "GiB")
    $unit = 0
    while ($value -ge 1024 -and $unit -lt ($units.Count - 1)) {
        $value = $value / 1024
        $unit++
    }
    if ($unit -eq 0) {
        return ("{0:N0} {1}" -f $value, $units[$unit])
    }
    return ("{0:N1} {1}" -f $value, $units[$unit])
}

function Format-Number([object]$Value, [int]$Decimals = 1) {
    if ($null -eq $Value -or $Value -eq "") {
        return "n/a"
    }
    return ("{0:N$Decimals}" -f ([double]$Value))
}

function Get-PropertyOrNull([object]$Object, [string]$Name) {
    if ($null -eq $Object) {
        return $null
    }
    $property = $Object.PSObject.Properties[$Name]
    if ($null -eq $property) {
        return $null
    }
    return $property.Value
}

function Add-Comparison([System.Collections.Generic.List[object]]$Rows,
                        [string]$Name,
                        [object]$Current,
                        [object]$Baseline,
                        [string]$Unit,
                        [bool]$LowerIsBetter) {
    $deltaPercent = $null
    $direction = "n/a"
    if ($null -ne $Current -and $null -ne $Baseline -and [double]$Baseline -ne 0.0) {
        $deltaPercent = (([double]$Current - [double]$Baseline) / [double]$Baseline) * 100.0
        if (-not $LowerIsBetter) {
            $deltaPercent = -1.0 * $deltaPercent
        }
        if ($deltaPercent -gt 0.0) {
            $direction = "worse"
        } elseif ($deltaPercent -lt 0.0) {
            $direction = "better"
        } else {
            $direction = "same"
        }
    }
    $Rows.Add([ordered]@{
        name = $Name
        current = $Current
        baseline = $Baseline
        unit = $Unit
        lowerIsBetter = $LowerIsBetter
        deltaPercent = $deltaPercent
        direction = $direction
    }) | Out-Null
}

function Get-NestedPropertyOrNull([object]$Object, [string[]]$Path) {
    $current = $Object
    foreach ($part in $Path) {
        $current = Get-PropertyOrNull $current $part
        if ($null -eq $current) {
            return $null
        }
    }
    return $current
}

function Format-ComparisonValue([object]$Value, [string]$Unit) {
    switch ($Unit) {
        "ms" { return Format-Duration $Value }
        "bytes" { return Format-Bytes $Value }
        "fps" { return Format-Number $Value 1 }
        default { return Format-Number $Value 1 }
    }
}

function Format-Delta([object]$DeltaPercent, [string]$Direction) {
    if ($null -eq $DeltaPercent) {
        return "n/a"
    }
    return ("{0:N1}% {1}" -f [math]::Abs([double]$DeltaPercent), $Direction)
}

$summary = Read-JsonFile $Summary
$baseline = if ($BaselineSummary) { Read-JsonFile $BaselineSummary } else { $null }

if (-not $Version) {
    $Version = Read-ModVersion
}
if (-not $Tag) {
    $Tag = "v$Version"
}

$resolvedOutputDir = Resolve-RepoPath $OutputDir
New-Item -ItemType Directory -Force -Path $resolvedOutputDir | Out-Null

$safeVersion = $Version -replace '[^0-9A-Za-z._-]', '-'
$reportPath = Join-Path $resolvedOutputDir "ami-regression-benchmark-$safeVersion.md"
$payloadPath = Join-Path $resolvedOutputDir "ami-regression-benchmark-$safeVersion.json"

$comparisonRows = New-Object System.Collections.Generic.List[object]
if ($baseline) {
    Add-Comparison $comparisonRows "Index wait" (Get-NestedPropertyOrNull $summary @("measuredIndexWaitMs")) (Get-NestedPropertyOrNull $baseline @("measuredIndexWaitMs")) "ms" $true
    Add-Comparison $comparisonRows "Reported index build" (Get-NestedPropertyOrNull $summary @("reportedIndexBuildTimeMs")) (Get-NestedPropertyOrNull $baseline @("reportedIndexBuildTimeMs")) "ms" $true
    Add-Comparison $comparisonRows "Entity icon warmup idle" (Get-NestedPropertyOrNull $summary @("entityWarmupIdleMs")) (Get-NestedPropertyOrNull $baseline @("entityWarmupIdleMs")) "ms" $true
    Add-Comparison $comparisonRows "Peak heap used" (Get-NestedPropertyOrNull $summary @("memoryPeaks", "heapUsedBytes")) (Get-NestedPropertyOrNull $baseline @("memoryPeaks", "heapUsedBytes")) "bytes" $true
    Add-Comparison $comparisonRows "Peak private memory" (Get-NestedPropertyOrNull $summary @("memoryPeaks", "privateMemoryBytes")) (Get-NestedPropertyOrNull $baseline @("memoryPeaks", "privateMemoryBytes")) "bytes" $true
    Add-Comparison $comparisonRows "Minimum estimated FPS" (Get-NestedPropertyOrNull $summary @("frameAndTick", "minEstimatedFps")) (Get-NestedPropertyOrNull $baseline @("frameAndTick", "minEstimatedFps")) "fps" $false
    Add-Comparison $comparisonRows "Max AMI tick" (Get-NestedPropertyOrNull $summary @("frameAndTick", "maxAmiTickMs")) (Get-NestedPropertyOrNull $baseline @("frameAndTick", "maxAmiTickMs")) "ms" $true
}

$nodeCounts = Get-PropertyOrNull $summary "nodeTypeCounts"
$entityIcons = Get-PropertyOrNull $summary "entityIcons"
$warmup = Get-PropertyOrNull $entityIcons "warmup"
$cache = Get-PropertyOrNull $entityIcons "cache"

$lines = New-Object System.Collections.Generic.List[string]
$lines.Add("# AMI Regression Benchmark $Version") | Out-Null
$lines.Add("") | Out-Null
$lines.Add("Pack: Synesthesia") | Out-Null
$lines.Add("Run: $(Get-NestedPropertyOrNull $summary @("runId"))") | Out-Null
$lines.Add("Forced reindex: $(Get-NestedPropertyOrNull $summary @("forcedReindex"))") | Out-Null
$lines.Add("Samples: $(Get-NestedPropertyOrNull $summary @("sampleCount")) over $(Format-Duration (Get-NestedPropertyOrNull $summary @("sampleDurationMs")))") | Out-Null
$lines.Add("") | Out-Null
$lines.Add("## Key Metrics") | Out-Null
$lines.Add("") | Out-Null
$lines.Add("| Metric | Value |") | Out-Null
$lines.Add("| --- | ---: |") | Out-Null
$lines.Add("| Index wait | $(Format-Duration (Get-NestedPropertyOrNull $summary @("measuredIndexWaitMs"))) |") | Out-Null
$lines.Add("| Reported index build | $(Format-Duration (Get-NestedPropertyOrNull $summary @("reportedIndexBuildTimeMs"))) |") | Out-Null
$lines.Add("| Entity icon warmup idle | $(Format-Duration (Get-NestedPropertyOrNull $summary @("entityWarmupIdleMs"))) |") | Out-Null
$lines.Add("| Peak heap used | $(Format-Bytes (Get-NestedPropertyOrNull $summary @("memoryPeaks", "heapUsedBytes"))) |") | Out-Null
$lines.Add("| Peak private memory | $(Format-Bytes (Get-NestedPropertyOrNull $summary @("memoryPeaks", "privateMemoryBytes"))) |") | Out-Null
$lines.Add("| Minimum estimated FPS | $(Format-Number (Get-NestedPropertyOrNull $summary @("frameAndTick", "minEstimatedFps")) 1) |") | Out-Null
$lines.Add("| Final estimated FPS | $(Format-Number (Get-NestedPropertyOrNull $summary @("frameAndTick", "finalEstimatedFps")) 1) |") | Out-Null
$lines.Add("| Max AMI tick | $(Format-Duration (Get-NestedPropertyOrNull $summary @("frameAndTick", "maxAmiTickMs"))) |") | Out-Null
$lines.Add("") | Out-Null

if ($comparisonRows.Count -gt 0) {
    $lines.Add("## Baseline Comparison") | Out-Null
    $lines.Add("") | Out-Null
    $lines.Add("| Metric | Current | Baseline | Delta |") | Out-Null
    $lines.Add("| --- | ---: | ---: | ---: |") | Out-Null
    foreach ($row in $comparisonRows) {
        $lines.Add("| $($row.name) | $(Format-ComparisonValue $row.current $row.unit) | $(Format-ComparisonValue $row.baseline $row.unit) | $(Format-Delta $row.deltaPercent $row.direction) |") | Out-Null
    }
    $lines.Add("") | Out-Null
}

if ($nodeCounts) {
    $lines.Add("## Indexed Content") | Out-Null
    $lines.Add("") | Out-Null
    $lines.Add("| Node Type | Count |") | Out-Null
    $lines.Add("| --- | ---: |") | Out-Null
    foreach ($property in ($nodeCounts.PSObject.Properties | Sort-Object Name)) {
        $lines.Add("| $($property.Name) | $($property.Value) |") | Out-Null
    }
    $lines.Add("") | Out-Null
}

if ($entityIcons) {
    $lines.Add("## Entity Icons") | Out-Null
    $lines.Add("") | Out-Null
    $lines.Add("| Metric | Value |") | Out-Null
    $lines.Add("| --- | ---: |") | Out-Null
    if ($warmup) {
        $lines.Add("| Warmup total | $($warmup.total) |") | Out-Null
        $lines.Add("| Warmup visited | $($warmup.visited) |") | Out-Null
        $lines.Add("| Warmup queued or cached | $($warmup.queuedOrCached) |") | Out-Null
        $lines.Add("| Warmup skipped | $($warmup.skipped) |") | Out-Null
        $lines.Add("| Warmup render failures | $($warmup.renderFailures) |") | Out-Null
    }
    if ($cache) {
        $lines.Add("| Pending bake count | $($cache.pendingBakeCount) |") | Out-Null
        $lines.Add("| Failed key count | $($cache.failedKeyCount) |") | Out-Null
        $lines.Add("| Queued bake requests | $($cache.queuedBakeRequests) |") | Out-Null
        $lines.Add("| Dropped bake requests | $($cache.droppedBakeRequests) |") | Out-Null
        $lines.Add("| Rendered bake count | $($cache.renderedBakeCount) |") | Out-Null
        $lines.Add("| Persistent load count | $($cache.persistentLoadCount) |") | Out-Null
        $lines.Add("| Failed bake count | $($cache.failedBakeCount) |") | Out-Null
    }
    $lines.Add("") | Out-Null
}

$payload = [ordered]@{
    version = $Version
    tag = $Tag
    summary = $summary
    baseline = $baseline
    comparisons = $comparisonRows
    report = $reportPath
}

$lines | Set-Content -Path $reportPath
$payload | ConvertTo-Json -Depth 100 | Set-Content -Path $payloadPath

Write-Host "Benchmark report written: $reportPath"
Write-Host "Benchmark payload written: $payloadPath"

if ($UploadToGitHubRelease) {
    $gh = Get-Command gh.exe -ErrorAction SilentlyContinue
    if (-not $gh) {
        $gh = Get-Command gh -ErrorAction SilentlyContinue
    }
    if (-not $gh) {
        throw "GitHub CLI not found. Install gh or rerun without -UploadToGitHubRelease."
    }
    & $gh.Source release upload $Tag --clobber $reportPath $payloadPath
    if ($LASTEXITCODE -ne 0) {
        throw "gh release upload failed for $Tag."
    }
}
