param(
    [string]$AmiRepo = (Resolve-Path (Join-Path $PSScriptRoot '..')).Path,
    [string]$Compat = 'waystones',
    [ValidateSet('all', 'neoforge', 'forge')]
    [string]$Loader = 'all',
    [ValidateSet('all', 'emi', 'jei')]
    [string]$Viewer = 'all',
    [switch]$SkipTest,
    [switch]$SkipDeploy,
    [switch]$Launch,
    [switch]$LaunchAll,
    [switch]$StopExistingClients,
    [switch]$StopManagedClients,
    [switch]$AllowParallelLaunches,
    [switch]$Json,
    [int]$LaunchDelaySeconds = 3
)

$ErrorActionPreference = 'Stop'

$compatProfiles = @{
    waystones = @{
        TestClass = 'com.sanhiruzu.ami.index.WaystonesCompatTest'
        SearchTerms = @('waystone', 'warp stone', 'warp dust')
        Notes = 'Search Waystones content, then left-click and right-click at least one result without crash.'
    }
}

function Get-LaunchStatePath {
    return Join-Path $AmiRepo 'run\smoke-mod-compat-launches.json'
}

function Clear-StaleAutomationState {
    param([string]$RunDirectory)

    $automationDir = Join-Path $RunDirectory 'automine_testing'
    if (-not (Test-Path -LiteralPath $automationDir)) {
        return
    }

    @('command.json', 'command.bad.json') | ForEach-Object {
        $path = Join-Path $automationDir $_
        if (Test-Path -LiteralPath $path) {
            Remove-Item -LiteralPath $path -Force -ErrorAction SilentlyContinue
        }
    }
}

function Get-RepoTiedAmiProcesses {
    Get-CimInstance Win32_Process |
        Where-Object {
            ($_.Name -match '^java(w)?$|^gradle$|^cmd$') -and
            $_.CommandLine -and
            $_.CommandLine -like "*$AmiRepo*" -and
            ($_.CommandLine -like '*runClientEmi*' -or
             $_.CommandLine -like '*runClientJei*' -or
             $_.CommandLine -like '*run\neoforge-*' -or
             $_.CommandLine -like '*run\forge-*')
        }
}

function Get-ProcessDescendants {
    param(
        [int]$ProcessId,
        [System.Collections.Generic.HashSet[int]]$Visited = $(New-Object 'System.Collections.Generic.HashSet[int]')
    )

    if (-not $Visited.Add($ProcessId)) {
        return @()
    }

    $children = @(Get-CimInstance Win32_Process -Filter "ParentProcessId = $ProcessId" -ErrorAction SilentlyContinue)
    $all = @($children)
    foreach ($child in $children) {
        $all += Get-ProcessDescendants -ProcessId $child.ProcessId -Visited $Visited
    }
    return $all
}

function Read-LaunchState {
    $path = Get-LaunchStatePath
    if (-not (Test-Path -LiteralPath $path)) {
        return $null
    }

    try {
        return Get-Content -LiteralPath $path -Raw | ConvertFrom-Json
    } catch {
        Remove-Item -LiteralPath $path -Force -ErrorAction SilentlyContinue
        return $null
    }
}

function Write-LaunchState {
    param([object[]]$Launches)

    $path = Get-LaunchStatePath
    $dir = Split-Path -Parent $path
    New-Item -ItemType Directory -Force -Path $dir | Out-Null
    [pscustomobject]@{
        AmiRepo = $AmiRepo
        UpdatedAt = (Get-Date).ToString('o')
        Launches = $Launches
    } | ConvertTo-Json -Depth 6 | Set-Content -LiteralPath $path -Encoding UTF8
}

function Clear-LaunchState {
    $path = Get-LaunchStatePath
    if (Test-Path -LiteralPath $path) {
        Remove-Item -LiteralPath $path -Force -ErrorAction SilentlyContinue
    }
}

function Get-ActiveManagedLaunches {
    $state = Read-LaunchState
    if (-not $state -or -not $state.Launches) {
        return @()
    }

    $active = @()
    foreach ($launch in @($state.Launches)) {
        $proc = Get-Process -Id $launch.WrapperPid -ErrorAction SilentlyContinue
        if ($proc) {
            $active += $launch
        }
    }
    if ($active.Count -eq 0) {
        Clear-LaunchState
    }
    return $active
}

function Get-CompatProfile {
    param([string]$CompatName)

    $key = $CompatName.ToLowerInvariant()
    if (-not $compatProfiles.ContainsKey($key)) {
        $known = ($compatProfiles.Keys | Sort-Object) -join ', '
        throw "Unknown compat '$CompatName'. Known compat profiles: $known"
    }
    return $compatProfiles[$key]
}

function Invoke-Gradle {
    param(
        [string[]]$Arguments,
        [switch]$PassThru
    )

    $gradle = Join-Path $AmiRepo 'gradlew.bat'
    if (-not (Test-Path -LiteralPath $gradle)) {
        throw "Could not find gradlew.bat under $AmiRepo"
    }

    if ($PassThru) {
        & $gradle @Arguments
    } else {
        & $gradle @Arguments | Out-Host
    }

    if ($LASTEXITCODE -ne 0) {
        throw "Gradle command failed: gradlew.bat $($Arguments -join ' ')"
    }
}

function Get-SmokeMatrix {
    param(
        [string]$LoaderFilter,
        [string]$ViewerFilter
    )

    $rows = @(
        [pscustomobject]@{
            Loader = 'NeoForge'
            Viewer = 'EMI'
            Task = ':neoforge:runClientEmi'
            RunName = 'neoforge-emi'
            RunDirectory = 'run\neoforge-emi'
        }
        [pscustomobject]@{
            Loader = 'NeoForge'
            Viewer = 'JEI'
            Task = ':neoforge:runClientJei'
            RunName = 'neoforge-jei'
            RunDirectory = 'run\neoforge-jei'
        }
        [pscustomobject]@{
            Loader = 'Forge'
            Viewer = 'EMI'
            Task = ':forge:runClientEmi'
            RunName = 'forge-emi'
            RunDirectory = 'run\forge-emi'
        }
        [pscustomobject]@{
            Loader = 'Forge'
            Viewer = 'JEI'
            Task = ':forge:runClientJei'
            RunName = 'forge-jei'
            RunDirectory = 'run\forge-jei'
        }
    )

    return $rows | Where-Object {
        ($LoaderFilter -eq 'all' -or $_.Loader.ToLowerInvariant() -eq $LoaderFilter) -and
        ($ViewerFilter -eq 'all' -or $_.Viewer.ToLowerInvariant() -eq $ViewerFilter)
    }
}

function Stop-AmiClients {
    foreach ($proc in @(Get-RepoTiedAmiProcesses)) {
        foreach ($descendant in @(Get-ProcessDescendants -ProcessId $proc.ProcessId)) {
            Stop-Process -Id $descendant.ProcessId -Force -ErrorAction SilentlyContinue
        }
        Stop-Process -Id $proc.ProcessId -Force -ErrorAction SilentlyContinue
    }
    Clear-LaunchState
}

function Start-SmokeLaunch {
    param(
        [pscustomobject]$Entry,
        [int]$DelaySeconds
    )

    $gradle = Join-Path $AmiRepo 'gradlew.bat'
    $runDir = Join-Path $AmiRepo $Entry.RunDirectory
    $logDir = Join-Path $runDir 'logs'
    Clear-StaleAutomationState -RunDirectory $runDir
    New-Item -ItemType Directory -Force -Path $logDir | Out-Null

    $stamp = Get-Date -Format 'yyyyMMdd-HHmmss'
    $stdout = Join-Path $logDir "ami-$($Entry.RunName)-smoke-$stamp.out.log"
    $stderr = Join-Path $logDir "ami-$($Entry.RunName)-smoke-$stamp.err.log"

    $proc = Start-Process -FilePath $gradle `
        -ArgumentList $Entry.Task, '--console=plain' `
        -WorkingDirectory $AmiRepo `
        -RedirectStandardOutput $stdout `
        -RedirectStandardError $stderr `
        -WindowStyle Hidden `
        -PassThru

    Start-Sleep -Seconds $DelaySeconds

    return [pscustomobject]@{
        Loader = $Entry.Loader
        Viewer = $Entry.Viewer
        Task = $Entry.Task
        RunDirectory = $runDir
        StdoutLog = $stdout
        StderrLog = $stderr
        WrapperPid = $proc.Id
        StartedAt = (Get-Date).ToString('o')
    }
}

try {
    $profile = Get-CompatProfile -CompatName $Compat
    $matrix = @(Get-SmokeMatrix -LoaderFilter $Loader -ViewerFilter $Viewer)

    if (-not $matrix.Count) {
        throw "No smoke matrix entries matched loader=$Loader viewer=$Viewer"
    }

    if ($LaunchAll -and -not $AllowParallelLaunches) {
        throw 'LaunchAll is blocked by default because it can strand multiple Minecraft clients. Re-run with -AllowParallelLaunches only if you really want all matrix entries at once.'
    }

    if ($StopManagedClients) {
        Stop-AmiClients
        $result = [pscustomobject]@{
            State = 'stopped'
            AmiRepo = $AmiRepo
        }
        if ($Json) {
            $result | ConvertTo-Json -Depth 5
            exit 0
        }
        Write-Host 'Stopped smoke-managed AMI client processes for this repo.'
        exit 0
    }

    if (-not $SkipTest) {
        Invoke-Gradle -Arguments @(':neoforge:test', '--tests', $profile.TestClass)
    }

    if (-not $SkipDeploy) {
        Invoke-Gradle -Arguments @('deployPrismCompatJars')
    }

    if ($StopExistingClients) {
        Stop-AmiClients
    }

    $activeManaged = @(Get-ActiveManagedLaunches)
    if (($Launch -or $LaunchAll) -and $activeManaged.Count -gt 0) {
        $activeList = $activeManaged | ForEach-Object { "$($_.Loader)/$($_.Viewer) pid=$($_.WrapperPid)" }
        throw "Existing smoke-managed launch is still running: $($activeList -join ', '). Stop it with -StopManagedClients or reuse that client."
    }

    $activeRepoProcesses = @(Get-RepoTiedAmiProcesses)
    if (($Launch -or $LaunchAll) -and $activeRepoProcesses.Count -gt 0) {
        throw "Repo-tied Minecraft/Gradle processes are already running for $AmiRepo. Use -StopExistingClients before launching another smoke client."
    }

    $launches = @()
    if ($LaunchAll) {
        foreach ($entry in $matrix) {
            $launches += Start-SmokeLaunch -Entry $entry -DelaySeconds $LaunchDelaySeconds
        }
    } elseif ($Launch) {
        $launches += Start-SmokeLaunch -Entry $matrix[0] -DelaySeconds $LaunchDelaySeconds
    }

    if ($launches.Count -gt 0) {
        Write-LaunchState -Launches $launches
    }

    $result = [pscustomobject]@{
        State = 'ready'
        Compat = $Compat.ToLowerInvariant()
        TestClass = $profile.TestClass
        SearchTerms = $profile.SearchTerms
        Notes = $profile.Notes
        Matrix = $matrix | ForEach-Object {
            [pscustomobject]@{
                Loader = $_.Loader
                Viewer = $_.Viewer
                Task = $_.Task
                RunDirectory = Join-Path $AmiRepo $_.RunDirectory
            }
        }
        Launches = $launches
        PassChecklist = @(
            'Client boots to title screen or a world.',
            'Open inventory without crash.',
            "Search these terms: $($profile.SearchTerms -join ', ').",
            'Confirm expected compat results appear.',
            'Left-click at least one compat result without crash.',
            'Right-click or open the normal AMI interaction path without crash.',
            'Check the log only for real AMI or recipe-viewer errors.'
        )
    }

    if ($Json) {
        $result | ConvertTo-Json -Depth 5
        exit 0
    }

    Write-Host ''
    Write-Host "Compat smoke is ready for '$($result.Compat)'."
} catch {
    Write-Error $_
    exit 1
}
Write-Host ''
Write-Host 'Matrix:'
foreach ($entry in $result.Matrix) {
    Write-Host "  [$($entry.Loader)][$($entry.Viewer)] $($entry.Task)"
    Write-Host "    Run dir: $($entry.RunDirectory)"
}

if ($launches.Count -gt 0) {
    Write-Host ''
    Write-Host 'Launched runs:'
    foreach ($launch in $launches) {
        Write-Host "  [$($launch.Loader)][$($launch.Viewer)] $($launch.Task)"
        Write-Host "    stdout: $($launch.StdoutLog)"
        Write-Host "    stderr: $($launch.StderrLog)"
    }
}

Write-Host ''
Write-Host 'Pass checklist:'
foreach ($line in $result.PassChecklist) {
    Write-Host "  - $line"
}

Write-Host ''
Write-Host "Notes: $($result.Notes)"
Write-Host ''
Write-Host 'Typical uses:'
Write-Host '  .\scripts\smoke-mod-compat.ps1'
Write-Host '  .\scripts\smoke-mod-compat.ps1 -Loader neoforge -Viewer emi -Launch'
Write-Host '  .\scripts\smoke-mod-compat.ps1 -StopManagedClients'
Write-Host '  .\scripts\smoke-mod-compat.ps1 -LaunchAll -AllowParallelLaunches -StopExistingClients'
exit 0
