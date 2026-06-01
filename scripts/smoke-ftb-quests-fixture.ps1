param(
    [string]$AmiRepo = (Resolve-Path (Join-Path $PSScriptRoot '..')).Path,
    [string]$RunName = 'neoforge-emi',
    [string]$World = 'New World',
    [switch]$IncludeSmartFilters,
    [string]$FtbFilterSystemJar = '',
    [switch]$NoLaunch,
    [switch]$RestartClient,
    [switch]$StopAfter,
    [int]$TimeoutSeconds = 180
)

$ErrorActionPreference = 'Stop'

$runDir = Join-Path $AmiRepo "run\$RunName"
$gameDir = $runDir
$automineCommand = 'C:\WorkDir\AutoMineTesting\scripts\send-automine-command.ps1'
$automineInstall = 'C:\WorkDir\AutoMineTesting\scripts\install-into-ami-run.ps1'

function Stop-AmiClient {
    Get-CimInstance Win32_Process |
        Where-Object {
            ($_.Name -match 'java|javaw|cmd|gradle') -and
            ($_.CommandLine -like '*runClientEmi*' -or $_.CommandLine -like '*run\neoforge-emi*')
        } |
        ForEach-Object { Stop-Process -Id $_.ProcessId -Force -ErrorAction SilentlyContinue }
}

function Resolve-FtbFilterSystemJar {
    param([string]$ExplicitPath)

    if ($ExplicitPath -and (Test-Path -LiteralPath $ExplicitPath)) {
        return (Resolve-Path -LiteralPath $ExplicitPath).Path
    }

    $roots = @(
        "$env:APPDATA\PrismLauncher\instances",
        "$env:LOCALAPPDATA\PrismLauncher\instances",
        "$env:USERPROFILE\PrismLauncher\instances"
    ) | Where-Object { Test-Path -LiteralPath $_ } | Select-Object -Unique

    foreach ($root in $roots) {
        $jar = Get-ChildItem -Path $root -Recurse -File -Filter 'ftb-filter-system-*.jar' -ErrorAction SilentlyContinue |
            Sort-Object FullName |
            Select-Object -First 1
        if ($jar) {
            return $jar.FullName
        }
    }

    throw 'Could not find ftb-filter-system jar. Pass -FtbFilterSystemJar or install a Prism instance containing it.'
}

function Wait-AutoMineStatus {
    param([int]$Timeout)

    $deadline = (Get-Date).AddSeconds($Timeout)
    while ((Get-Date) -lt $deadline) {
        try {
            $raw = & $automineCommand -GameDir $gameDir -Action status -TimeoutSeconds 5 2>$null
            if ($LASTEXITCODE -eq 0 -and $raw) {
                return $raw | ConvertFrom-Json
            }
        } catch {
            Start-Sleep -Seconds 2
        }
    }
    throw "AutoMine status did not become available within $Timeout seconds."
}

function Invoke-AutoMineJson {
    param(
        [string]$Action,
        [string]$Item = '',
        [int]$Timeout = 30
    )

    if ($Item) {
        $raw = & $automineCommand -GameDir $gameDir -Action $Action -Item $Item -TimeoutSeconds $Timeout
    } else {
        $raw = & $automineCommand -GameDir $gameDir -Action $Action -TimeoutSeconds $Timeout
    }
    return $raw | ConvertFrom-Json
}

function Assert-QuestItem {
    param(
        [string]$Item,
        [int]$ExpectedMatches,
        [int]$ExpectedDocuments,
        [string]$ExpectedFirstTitle = '',
        [int]$Timeout = 45
    )

    $deadline = (Get-Date).AddSeconds($Timeout)
    $status = $null
    do {
        Start-Sleep -Milliseconds 500
        $status = Invoke-AutoMineJson -Action 'ami_quest_status' -Item $Item -Timeout 40
        $documentsMatch = $status.amiQuestDocuments -eq $ExpectedDocuments
        $matchesMatch = $status.amiQuestMatches -eq $ExpectedMatches
        $titleMatch = -not $ExpectedFirstTitle -or $status.amiFirstQuestTitle -eq $ExpectedFirstTitle
        if ($documentsMatch -and $matchesMatch -and $titleMatch) {
            return $status
        }
    } while ((Get-Date) -lt $deadline)

    if ($status -eq $null) {
        throw "No quest status returned for $Item."
    }
    throw "Expected $ExpectedDocuments docs/$ExpectedMatches matches/$ExpectedFirstTitle for $Item, got $($status.amiQuestDocuments) docs/$($status.amiQuestMatches) matches/$($status.amiFirstQuestTitle)."
}

if ($IncludeSmartFilters) {
    & (Join-Path $PSScriptRoot 'write-vanilla-ftb-quests-fixture.ps1') `
        -AmiRepo $AmiRepo `
        -RunName $RunName `
        -CleanGenerated `
        -IncludeSmartFilters | Out-Null
} else {
    & (Join-Path $PSScriptRoot 'write-vanilla-ftb-quests-fixture.ps1') `
        -AmiRepo $AmiRepo `
        -RunName $RunName `
        -CleanGenerated | Out-Null
}

if ($IncludeSmartFilters) {
    $jar = Resolve-FtbFilterSystemJar -ExplicitPath $FtbFilterSystemJar
    $modsDir = Join-Path $runDir 'mods'
    New-Item -ItemType Directory -Force -Path $modsDir | Out-Null
    Copy-Item -LiteralPath $jar -Destination (Join-Path $modsDir (Split-Path $jar -Leaf)) -Force
}

if (Test-Path -LiteralPath $automineInstall) {
    & $automineInstall -AmiRepo $AmiRepo -RunName $RunName | Out-Null
}

if ($RestartClient) {
    Stop-AmiClient
    Start-Sleep -Seconds 2
}

if (-not $NoLaunch) {
    $logDir = Join-Path $runDir 'logs'
    New-Item -ItemType Directory -Force -Path $logDir | Out-Null
    $stamp = Get-Date -Format 'yyyyMMdd-HHmmss'
    $out = Join-Path $logDir "ami-runClientEmi-smoke-ftbquests-$stamp.out.log"
    $err = Join-Path $logDir "ami-runClientEmi-smoke-ftbquests-$stamp.err.log"
    Start-Process -FilePath (Join-Path $AmiRepo 'gradlew.bat') `
        -ArgumentList ':neoforge:runClientEmi','--console=plain' `
        -WorkingDirectory $AmiRepo `
        -RedirectStandardOutput $out `
        -RedirectStandardError $err `
        -WindowStyle Hidden | Out-Null
}

$ready = Wait-AutoMineStatus -Timeout $TimeoutSeconds
if (-not $ready.inWorld) {
    & $automineCommand -GameDir $gameDir -Action open_world -World $World -TimeoutSeconds $TimeoutSeconds | Out-Null
    Start-Sleep -Seconds 3
}

$expectedDocuments = 7
if ($IncludeSmartFilters) {
    $expectedDocuments = 9
}
$checks = @()
$checks += Assert-QuestItem -Item 'minecraft:redstone' -ExpectedMatches 2 -ExpectedDocuments $expectedDocuments -ExpectedFirstTitle 'Redstone Power'
$checks += Assert-QuestItem -Item 'minecraft:crafting_table' -ExpectedMatches 2 -ExpectedDocuments $expectedDocuments -ExpectedFirstTitle 'Make a Crafting Table'
$checks += Assert-QuestItem -Item 'minecraft:diamond' -ExpectedMatches 1 -ExpectedDocuments $expectedDocuments -ExpectedFirstTitle 'Diamonds'
$checks += Assert-QuestItem -Item 'minecraft:oak_log' -ExpectedMatches 1 -ExpectedDocuments $expectedDocuments -ExpectedFirstTitle 'Gather Logs'

if ($IncludeSmartFilters) {
    $checks += Assert-QuestItem -Item 'minecraft:torch' -ExpectedMatches 1 -ExpectedDocuments $expectedDocuments -ExpectedFirstTitle 'Any Light Source'
    $checks += Assert-QuestItem -Item 'minecraft:redstone_torch' -ExpectedMatches 1 -ExpectedDocuments $expectedDocuments -ExpectedFirstTitle 'Any Light Source'
    $checks += Assert-QuestItem -Item 'ftbfiltersystem:smart_filter' -ExpectedMatches 1 -ExpectedDocuments $expectedDocuments -ExpectedFirstTitle 'Any Log'
}

if ($StopAfter) {
    Stop-AmiClient
}

[pscustomobject]@{
    State = 'ok'
    RunDirectory = $runDir
    IncludeSmartFilters = [bool]$IncludeSmartFilters
    CheckedItems = $checks | ForEach-Object {
        [pscustomobject]@{
            Item = $_.amiQuestItem
            Documents = $_.amiQuestDocuments
            Matches = $_.amiQuestMatches
            FirstQuestTitle = $_.amiFirstQuestTitle
        }
    }
} | ConvertTo-Json -Depth 5
