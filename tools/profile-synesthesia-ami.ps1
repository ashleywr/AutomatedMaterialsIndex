<#
.SYNOPSIS
Runs a repeatable AMI profiling pass against the local Synesthesia Prism instance.

.DESCRIPTION
Builds and syncs AMI, installs AutoMine Testing, launches or attaches to Synesthesia,
opens the configured world and inventory, optionally forces AMI reindexing, then samples
AMI runtime telemetry and JVM process memory until entity icon warmup is idle and the
minimum sample window has elapsed.

.EXAMPLE
.\tools\profile-synesthesia-ami.ps1 -StartJfr

.EXAMPLE
.\tools\profile-synesthesia-ami.ps1 -BaselineSummary .\profile-runs\synesthesia-ami-20260613-120000\summary.json -FailOnRegression
#>
param(
    [string]$GameDir = "C:\Users\ashle\AppData\Roaming\PrismLauncher\instances\Synesthesia\minecraft",
    [string]$InstanceName = "Synesthesia",
    [string]$PrismExe = "C:\Users\ashle\AppData\Local\Programs\PrismLauncher\prismlauncher.exe",
    [string]$World = "New World",
    [int]$Port = 47321,
    [int]$AmiReadyTimeoutSec = 600,
    [int]$WarmupTimeoutSec = 600,
    [int]$MinSampleSec = 120,
    [int]$SampleIntervalMs = 1000,
    [string]$OutputRoot = "profile-runs",
    [string]$BaselineSummary = "",
    [double]$MaxIndexRegressionPercent = 25.0,
    [double]$MaxWarmupRegressionPercent = 25.0,
    [double]$MaxHeapRegressionPercent = 20.0,
    [double]$MaxPrivateMemoryRegressionPercent = 20.0,
    [double]$MaxFpsDropPercent = 20.0,
    [switch]$AttachOnly,
    [switch]$UseCache,
    [switch]$SkipBuildAndSync,
    [switch]$SkipAutoMineInstall,
    [switch]$StartJfr,
    [switch]$FailOnRegression,
    [switch]$StopAfter
)

Set-StrictMode -Version Latest
$ErrorActionPreference = "Stop"

$repo = Split-Path $PSScriptRoot -Parent
$runId = Get-Date -Format "yyyyMMdd-HHmmss"
$outDir = Join-Path $repo (Join-Path $OutputRoot "synesthesia-ami-$runId")
$rawSamples = Join-Path $outDir "samples.jsonl"
$csvSamples = Join-Path $outDir "samples.csv"
$summaryPath = Join-Path $outDir "summary.json"
$baseUri = "http://127.0.0.1:$Port"
$launchedProcess = $null
$jfrFile = Join-Path $outDir "ami-profile.jfr"

New-Item -ItemType Directory -Force -Path $outDir | Out-Null

function Write-Step([string]$Message) {
    Write-Host "[$(Get-Date -Format HH:mm:ss)] $Message"
}

function Invoke-AutoMineJson([string]$Method, [string]$Path, [object]$Body = $null, [int]$TimeoutSec = 30) {
    $params = @{
        Method = $Method
        Uri = "$baseUri$Path"
        TimeoutSec = $TimeoutSec
    }
    if ($null -ne $Body) {
        $params.ContentType = "application/json"
        $params.Body = ($Body | ConvertTo-Json -Depth 64)
    }
    Invoke-RestMethod @params
}

function Wait-AutoMine([int]$TimeoutSec) {
    $deadline = (Get-Date).AddSeconds($TimeoutSec)
    do {
        try {
            return Invoke-AutoMineJson -Method Get -Path "/status" -TimeoutSec 5
        } catch {
            Start-Sleep -Milliseconds 500
        }
    } while ((Get-Date) -lt $deadline)
    throw "Timed out waiting for AutoMine HTTP on $baseUri."
}

function Get-JavaProcessForGameDir {
    $escaped = $GameDir.Replace('\', '\\')
    $candidates = Get-CimInstance Win32_Process |
            Where-Object {
                ($_.Name -eq "java.exe" -or $_.Name -eq "javaw.exe") -and
                ($_.CommandLine -like "*$GameDir*" -or $_.CommandLine -like "*$escaped*" -or $_.CommandLine -like "*$InstanceName*")
            } |
            Sort-Object CreationDate -Descending
    if ($candidates) {
        return $candidates[0]
    }
    return $null
}

function Get-JcmdPath {
    $instanceCfg = Join-Path (Split-Path $GameDir -Parent) "instance.cfg"
    if (Test-Path $instanceCfg) {
        $javaPathLine = Get-Content $instanceCfg | Where-Object { $_ -like "JavaPath=*" } | Select-Object -First 1
        if ($javaPathLine) {
            $javaPath = $javaPathLine.Substring("JavaPath=".Length)
            $candidate = Join-Path (Split-Path $javaPath -Parent) "jcmd.exe"
            if (Test-Path $candidate) {
                return $candidate
            }
        }
    }
    $cmd = Get-Command jcmd.exe -ErrorAction SilentlyContinue
    if ($cmd) {
        return $cmd.Source
    }
    return $null
}

function Invoke-JcmdText([int]$ProcessId, [string[]]$CommandArgs) {
    $jcmd = Get-JcmdPath
    if (-not $jcmd) {
        return ""
    }
    try {
        & $jcmd $ProcessId @CommandArgs 2>&1 | Out-String
    } catch {
        return "jcmd failed: $($_.Exception.Message)"
    }
}

function Install-AutoMine {
    $jar = "C:\WorkDir\AutoMineTesting\build\libs\automine_testing-neoforge-1.21.1-0.1.0.jar"
    if (-not (Test-Path $jar)) {
        Write-Step "Building AutoMine Testing jar"
        & "C:\WorkDir\AutoMineTesting\gradlew.bat" -p "C:\WorkDir\AutoMineTesting" jar
    }
    if (-not (Test-Path $jar)) {
        throw "AutoMine jar not found: $jar"
    }
    $mods = Join-Path $GameDir "mods"
    New-Item -ItemType Directory -Force -Path $mods | Out-Null
    Copy-Item -LiteralPath $jar -Destination (Join-Path $mods (Split-Path $jar -Leaf)) -Force
}

function Copy-IfExists([string]$Source, [string]$DestinationName) {
    if (Test-Path $Source) {
        Copy-Item -LiteralPath $Source -Destination (Join-Path $outDir $DestinationName) -Force
    }
}

function Copy-LogArtifacts {
    Copy-IfExists (Join-Path $GameDir "logs\latest.log") "latest.log"
    Copy-IfExists (Join-Path $GameDir "logs\debug.log") "debug.log"
    Get-ChildItem (Join-Path $GameDir "logs") -Filter "gc.log*" -ErrorAction SilentlyContinue |
            ForEach-Object { Copy-Item -LiteralPath $_.FullName -Destination (Join-Path $outDir $_.Name) -Force }
    Get-ChildItem (Join-Path $GameDir "crash-reports") -Filter "*.txt" -ErrorAction SilentlyContinue |
            Sort-Object LastWriteTime -Descending |
            Select-Object -First 3 |
            ForEach-Object { Copy-Item -LiteralPath $_.FullName -Destination (Join-Path $outDir $_.Name) -Force }
}

function Get-ProcessSample([int]$ProcessId, [object]$Previous, [double]$IntervalSeconds) {
    $proc = Get-Process -Id $ProcessId -ErrorAction Stop
    $cpuSeconds = if ($null -ne $proc.CPU) { [double]$proc.CPU } else { 0.0 }
    $cpuPercent = $null
    if ($null -ne $Previous -and $IntervalSeconds -gt 0) {
        $deltaCpu = $cpuSeconds - [double]$Previous.cpuSeconds
        $cpuPercent = [math]::Max(0.0, ($deltaCpu / $IntervalSeconds / [Environment]::ProcessorCount) * 100.0)
    }
    [pscustomobject]@{
        pid = $ProcessId
        workingSetBytes = [int64]$proc.WorkingSet64
        privateMemoryBytes = [int64]$proc.PrivateMemorySize64
        pagedMemoryBytes = [int64]$proc.PagedMemorySize64
        handleCount = [int]$proc.HandleCount
        threadCount = [int]$proc.Threads.Count
        cpuSeconds = $cpuSeconds
        cpuPercent = $cpuPercent
    }
}

function Get-JsonProperty([object]$Object, [string]$Name, [object]$Default = $null) {
    if ($null -eq $Object) {
        return $Default
    }
    $property = $Object.PSObject.Properties[$Name]
    if ($null -eq $property) {
        return $Default
    }
    return $property.Value
}

function Flatten-Sample([object]$Sample) {
    $ami = $Sample.ami
    $icons = $ami.entityIcons
    $warmup = $icons.warmup
    $mem = $ami.memory
    $tick = $ami.clientTick
    $proc = $Sample.process
    [pscustomobject]@{
        elapsedMs = $Sample.elapsedMs
        ready = $ami.ready
        busy = $ami.busy
        progressPhase = $ami.progress.phase
        progressMessage = $ami.progress.message
        heapUsedBytes = $mem.heapUsedBytes
        heapCommittedBytes = $mem.heapCommittedBytes
        processWorkingSetBytes = $proc.workingSetBytes
        processPrivateMemoryBytes = $proc.privateMemoryBytes
        processCpuPercent = $proc.cpuPercent
        estimatedFps = $tick.estimatedFps
        averageAmiTickMs = $tick.averageTickMs
        maxAmiTickMs = $tick.maxTickMs
        averageFrameIntervalMs = $tick.averageFrameIntervalMs
        maxFrameIntervalMs = $tick.maxFrameIntervalMs
        entityWarmupDone = $warmup.done
        entityWarmupVisited = $warmup.visited
        entityWarmupTotal = $warmup.total
        pendingBakeCount = $icons.pendingBakeCount
        renderedBakeCount = $icons.renderedBakeCount
        persistentLoadCount = $icons.persistentLoadCount
        failedBakeCount = $icons.failedBakeCount
        failedKeyCount = $icons.failedKeyCount
        droppedBakeRequests = $icons.droppedBakeRequests
        pendingPersistentWrites = $icons.pendingPersistentWrites
        droppedPersistentWrites = $icons.droppedPersistentWrites
    }
}

function Add-RegressionCheck(
    [System.Collections.Generic.List[object]]$Checks,
    [string]$Name,
    [object]$BaselineValue,
    [object]$CurrentValue,
    [double]$LimitPercent,
    [bool]$LowerIsBetter
) {
    if ($null -eq $BaselineValue -or $null -eq $CurrentValue) {
        return
    }
    $baselineDouble = [double]$BaselineValue
    $currentDouble = [double]$CurrentValue
    if ($baselineDouble -le 0.0) {
        return
    }
    $percentChange = (($currentDouble - $baselineDouble) / $baselineDouble) * 100.0
    $regressed = if ($LowerIsBetter) {
        $percentChange -gt $LimitPercent
    } else {
        (-$percentChange) -gt $LimitPercent
    }
    $Checks.Add([pscustomobject]@{
        name = $Name
        baseline = $baselineDouble
        current = $currentDouble
        percentChange = [math]::Round($percentChange, 2)
        limitPercent = $LimitPercent
        lowerIsBetter = $LowerIsBetter
        regressed = $regressed
    })
}

try {
    Write-Step "Output: $outDir"

    if (-not $SkipBuildAndSync) {
        Write-Step "Building and syncing AMI into Synesthesia"
        & (Join-Path $repo "gradlew.bat") -p $repo syncSynesthesia
    }

    if (-not $SkipAutoMineInstall) {
        Write-Step "Installing AutoMine Testing into Synesthesia"
        Install-AutoMine
    }

    if (-not $AttachOnly) {
        if (-not (Test-Path $PrismExe)) {
            throw "Prism launcher not found: $PrismExe. Re-run with -AttachOnly after launching the instance manually."
        }
        Write-Step "Launching Prism instance $InstanceName"
        $launchedProcess = Start-Process -FilePath $PrismExe -ArgumentList @("--launch", $InstanceName) -PassThru
    } else {
        Write-Step "Attach-only mode; expecting Synesthesia to already be running"
    }

    Wait-AutoMine -TimeoutSec 300 | ConvertTo-Json -Depth 32 | Set-Content (Join-Path $outDir "automine-initial-status.json")

    $javaProcess = $null
    $deadline = (Get-Date).AddSeconds(300)
    do {
        $javaProcess = Get-JavaProcessForGameDir
        if ($javaProcess) { break }
        Start-Sleep -Seconds 1
    } while ((Get-Date) -lt $deadline)
    if (-not $javaProcess) {
        throw "Could not find Java process for $GameDir."
    }
    $javaPid = [int]$javaProcess.ProcessId
    Write-Step "Attached to Java process $javaPid"

    if ($StartJfr) {
        Write-Step "Starting JFR"
        Invoke-JcmdText -ProcessId $javaPid -CommandArgs @("JFR.start", "name=AMIRegression", "settings=profile", "filename=$jfrFile", "disk=true") |
                Set-Content (Join-Path $outDir "jfr-start.txt")
    }

    Invoke-JcmdText -ProcessId $javaPid -CommandArgs @("VM.command_line") | Set-Content (Join-Path $outDir "vm-command-line.txt")
    Invoke-JcmdText -ProcessId $javaPid -CommandArgs @("GC.heap_info") | Set-Content (Join-Path $outDir "heap-start.txt")

    Write-Step "Opening world $World"
    Invoke-AutoMineJson -Method Post -Path "/world/open" -Body @{ world = $World } -TimeoutSec 30 |
            ConvertTo-Json -Depth 32 | Set-Content (Join-Path $outDir "world-open.json")

    Write-Step "Waiting for AMI status endpoint"
    $amiStatus = $null
    $deadline = (Get-Date).AddSeconds($AmiReadyTimeoutSec)
    do {
        try {
            $amiStatus = Invoke-AutoMineJson -Method Get -Path "/ami/status" -TimeoutSec 10
            break
        } catch {
            Start-Sleep -Seconds 1
        }
    } while ((Get-Date) -lt $deadline)
    if (-not $amiStatus) {
        throw "Timed out waiting for /ami/status."
    }

    $indexStopwatch = [System.Diagnostics.Stopwatch]::StartNew()
    if (-not $UseCache) {
        Write-Step "Requesting forced AMI reindex"
        Invoke-AutoMineJson -Method Post -Path "/ami/reindex" -Body @{ forceProviderRebuild = $true; worldTimeoutMs = 180000 } -TimeoutSec 30 |
                ConvertTo-Json -Depth 64 | Set-Content (Join-Path $outDir "ami-reindex-request.json")
    } else {
        Write-Step "Using existing AMI index/cache state"
    }

    Write-Step "Waiting for AMI indexing to become ready"
    $deadline = (Get-Date).AddSeconds($AmiReadyTimeoutSec)
    do {
        $amiStatus = Invoke-AutoMineJson -Method Get -Path "/ami/status" -TimeoutSec 10
        $lastRebuildFailure = Get-JsonProperty $amiStatus "lastRebuildFailure"
        if ($lastRebuildFailure) {
            throw "AMI indexing failed: $lastRebuildFailure"
        }
        if ((Get-JsonProperty $amiStatus "ready" $false) -and -not (Get-JsonProperty $amiStatus "busy" $false)) {
            break
        }
        Start-Sleep -Milliseconds 500
    } while ((Get-Date) -lt $deadline)
    $indexStopwatch.Stop()
    if (-not ((Get-JsonProperty $amiStatus "ready" $false) -and -not (Get-JsonProperty $amiStatus "busy" $false))) {
        throw "Timed out waiting for AMI indexing to become ready."
    }

    Write-Step "Opening inventory"
    Invoke-AutoMineJson -Method Post -Path "/inventory/open" -Body @{ worldTimeoutMs = 180000 } -TimeoutSec 30 |
            ConvertTo-Json -Depth 32 | Set-Content (Join-Path $outDir "inventory-open.json")

    $sampleRows = New-Object System.Collections.Generic.List[object]
    $rawRows = New-Object System.Collections.Generic.List[object]
    $runStopwatch = [System.Diagnostics.Stopwatch]::StartNew()
    $entityDoneAtMs = $null
    $previousProcess = $null
    $previousSampleAt = Get-Date
    $deadline = (Get-Date).AddSeconds($WarmupTimeoutSec)
    Write-Step "Sampling inventory/entity warmup"
    do {
        $now = Get-Date
        $intervalSeconds = [math]::Max(0.001, ($now - $previousSampleAt).TotalSeconds)
        $processSample = Get-ProcessSample -ProcessId $javaPid -Previous $previousProcess -IntervalSeconds $intervalSeconds
        $previousProcess = $processSample
        $previousSampleAt = $now

        $amiStatus = Invoke-AutoMineJson -Method Get -Path "/ami/status" -TimeoutSec 10
        $automineStatus = Invoke-AutoMineJson -Method Get -Path "/status" -TimeoutSec 10
        $sample = [pscustomobject]@{
            time = $now.ToString("o")
            elapsedMs = [int64]$runStopwatch.ElapsedMilliseconds
            process = $processSample
            ami = $amiStatus
            automine = $automineStatus
        }
        $rawRows.Add($sample)
        ($sample | ConvertTo-Json -Compress -Depth 100) | Add-Content -Path $rawSamples
        $flat = Flatten-Sample $sample
        $sampleRows.Add($flat)

        $warmupDone = [bool]$amiStatus.entityIcons.warmup.done
        $bakesDone = ([int]$amiStatus.entityIcons.pendingBakeCount -eq 0)
        $writesDone = ([int]$amiStatus.entityIcons.pendingPersistentWrites -eq 0)
        if ($null -eq $entityDoneAtMs -and $warmupDone -and $bakesDone -and $writesDone) {
            $entityDoneAtMs = [int64]$runStopwatch.ElapsedMilliseconds
            Write-Step "Entity warmup/cache work is idle at ${entityDoneAtMs}ms"
        }

        $minElapsed = $runStopwatch.Elapsed.TotalSeconds -ge $MinSampleSec
        if ($minElapsed -and $null -ne $entityDoneAtMs) {
            break
        }
        Start-Sleep -Milliseconds $SampleIntervalMs
    } while ((Get-Date) -lt $deadline)
    $runStopwatch.Stop()

    $sampleRows | Export-Csv -NoTypeInformation -Path $csvSamples

    Write-Step "Capturing final inventory screenshot"
    $screenshotName = "ami-profile-$runId.png"
    Invoke-AutoMineJson -Method Post -Path "/inventory/screenshot" -Body @{
        screenshot = $screenshotName
        delayTicks = 5
        worldTimeoutMs = 180000
    } -TimeoutSec 30 | ConvertTo-Json -Depth 32 | Set-Content (Join-Path $outDir "screenshot-request.json")
    Start-Sleep -Seconds 2
    Copy-IfExists (Join-Path $GameDir "screenshots\$screenshotName") $screenshotName

    Invoke-JcmdText -ProcessId $javaPid -CommandArgs @("GC.heap_info") | Set-Content (Join-Path $outDir "heap-end.txt")
    if ($StartJfr) {
        Write-Step "Dumping/stopping JFR"
        Invoke-JcmdText -ProcessId $javaPid -CommandArgs @("JFR.dump", "name=AMIRegression", "filename=$jfrFile") |
                Set-Content (Join-Path $outDir "jfr-dump.txt")
        Invoke-JcmdText -ProcessId $javaPid -CommandArgs @("JFR.stop", "name=AMIRegression") |
                Set-Content (Join-Path $outDir "jfr-stop.txt")
    }
    Copy-LogArtifacts

    $final = $rawRows[$rawRows.Count - 1]
    $maxHeap = ($sampleRows | Measure-Object heapUsedBytes -Maximum).Maximum
    $maxWorkingSet = ($sampleRows | Measure-Object processWorkingSetBytes -Maximum).Maximum
    $maxPrivate = ($sampleRows | Measure-Object processPrivateMemoryBytes -Maximum).Maximum
    $minFps = ($sampleRows | Where-Object { $_.estimatedFps -gt 0 } | Measure-Object estimatedFps -Minimum).Minimum
    $maxAmiTick = ($sampleRows | Measure-Object maxAmiTickMs -Maximum).Maximum
    $summary = [ordered]@{
        runId = $runId
        gameDir = $GameDir
        instanceName = $InstanceName
        pid = $javaPid
        startedAt = (Get-Date).ToString("o")
        forcedReindex = -not [bool]$UseCache
        measuredIndexWaitMs = [int64]$indexStopwatch.ElapsedMilliseconds
        reportedIndexBuildTimeMs = $final.ami.indexBuildTimeMs
        entityWarmupIdleMs = $entityDoneAtMs
        sampleDurationMs = [int64]$runStopwatch.ElapsedMilliseconds
        sampleCount = $sampleRows.Count
        nodeTypeCounts = $final.ami.nodeTypeCounts
        entityIcons = $final.ami.entityIcons
        memoryPeaks = [ordered]@{
            heapUsedBytes = [int64]$maxHeap
            workingSetBytes = [int64]$maxWorkingSet
            privateMemoryBytes = [int64]$maxPrivate
        }
        frameAndTick = [ordered]@{
            minEstimatedFps = $minFps
            finalEstimatedFps = $final.ami.clientTick.estimatedFps
            maxAmiTickMs = $maxAmiTick
            finalAverageAmiTickMs = $final.ami.clientTick.averageTickMs
            finalMaxFrameIntervalMs = $final.ami.clientTick.maxFrameIntervalMs
        }
        artifacts = [ordered]@{
            samplesJsonl = $rawSamples
            samplesCsv = $csvSamples
            summaryJson = $summaryPath
            screenshot = (Join-Path $outDir $screenshotName)
            jfr = if ($StartJfr) { $jfrFile } else { $null }
        }
    }
    $summary | ConvertTo-Json -Depth 100 | Set-Content $summaryPath
    if ($BaselineSummary) {
        if (-not (Test-Path $BaselineSummary)) {
            throw "Baseline summary not found: $BaselineSummary"
        }
        $baseline = Get-Content -Raw -Path $BaselineSummary | ConvertFrom-Json
        $checks = New-Object System.Collections.Generic.List[object]
        Add-RegressionCheck $checks "measuredIndexWaitMs" $baseline.measuredIndexWaitMs $summary.measuredIndexWaitMs $MaxIndexRegressionPercent $true
        Add-RegressionCheck $checks "entityWarmupIdleMs" $baseline.entityWarmupIdleMs $summary.entityWarmupIdleMs $MaxWarmupRegressionPercent $true
        Add-RegressionCheck $checks "peakHeapUsedBytes" $baseline.memoryPeaks.heapUsedBytes $summary.memoryPeaks.heapUsedBytes $MaxHeapRegressionPercent $true
        Add-RegressionCheck $checks "peakPrivateMemoryBytes" $baseline.memoryPeaks.privateMemoryBytes $summary.memoryPeaks.privateMemoryBytes $MaxPrivateMemoryRegressionPercent $true
        Add-RegressionCheck $checks "minEstimatedFps" $baseline.frameAndTick.minEstimatedFps $summary.frameAndTick.minEstimatedFps $MaxFpsDropPercent $false
        $regressions = @($checks | Where-Object { $_.regressed })
        $comparison = [ordered]@{
            baselineSummary = (Resolve-Path $BaselineSummary).Path
            currentSummary = $summaryPath
            failed = $regressions.Count -gt 0
            checks = $checks
        }
        $comparisonPath = Join-Path $outDir "regression-comparison.json"
        $comparison | ConvertTo-Json -Depth 64 | Set-Content $comparisonPath
        Write-Step "Regression comparison written: $comparisonPath"
        if ($FailOnRegression -and $regressions.Count -gt 0) {
            $names = ($regressions | ForEach-Object { $_.name }) -join ", "
            throw "Profiling regression threshold exceeded: $names"
        }
    }
    Write-Step "Summary written: $summaryPath"

    if ($StopAfter -and $javaPid) {
        Write-Step "Stopping Java process $javaPid"
        Stop-Process -Id $javaPid -Force
    }
} finally {
    Copy-LogArtifacts
}
