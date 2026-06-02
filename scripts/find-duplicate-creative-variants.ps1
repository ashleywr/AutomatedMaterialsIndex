param(
    [Parameter(Mandatory = $true)]
    [string]$DumpPath,

    [int]$Limit = 80,

    [switch]$IncludeMinecraft
)

if (-not (Test-Path -LiteralPath $DumpPath)) {
    Write-Error "Dump not found: $DumpPath"
    exit 1
}

function Get-MetaValue {
    param(
        [object]$Metadata,
        [string]$Key
    )

    if ($null -eq $Metadata) {
        return ""
    }
    $property = $Metadata.PSObject.Properties[$Key]
    if ($null -eq $property -or $null -eq $property.Value) {
        return ""
    }
    return [string]$property.Value
}

function New-CandidateKey {
    param([object]$Node)

    $metadata = $Node.metadata
    return @(
        $Node.displayName
        (Get-MetaValue $metadata "subtypeOf")
        (Get-MetaValue $metadata "materialGroup")
        (Get-MetaValue $metadata "creativeTabId")
        (Get-MetaValue $metadata "itemClass")
        (Get-MetaValue $metadata "facets")
        (Get-MetaValue $metadata "tags")
        (Get-MetaValue $metadata "ontologyCategory")
        (Get-MetaValue $metadata "ontologySubcategory")
        (Get-MetaValue $metadata "accessLevel")
    ) -join "`u{1f}"
}

$groups = @{}
$lineNumber = 0
Get-Content -LiteralPath $DumpPath | ForEach-Object {
    $lineNumber++
    if ([string]::IsNullOrWhiteSpace($_)) {
        return
    }

    try {
        $node = $_ | ConvertFrom-Json
    } catch {
        Write-Warning "Skipping invalid JSON at line $lineNumber"
        return
    }

    if ($node.type -ne "ITEM") {
        return
    }
    if (-not ([string]$node.id).Contains("/variant/")) {
        return
    }
    if (-not $IncludeMinecraft -and ([string]$node.id).StartsWith("minecraft:")) {
        return
    }
    if ((Get-MetaValue $node.metadata "variantSource") -ne "creative_tab") {
        return
    }

    $key = New-CandidateKey $node
    if (-not $groups.ContainsKey($key)) {
        $groups[$key] = [System.Collections.Generic.List[object]]::new()
    }
    $groups[$key].Add($node)
}

$duplicates = $groups.Values |
    Where-Object { $_.Count -gt 1 } |
    Sort-Object -Property Count -Descending

if (-not $duplicates) {
    Write-Output "No duplicate-looking creative-tab variants found."
    exit 0
}

$shown = 0
foreach ($group in $duplicates) {
    if ($shown -ge $Limit) {
        break
    }
    $first = $group[0]
    $metadata = $first.metadata
    Write-Output ""
    Write-Output ("{0} copies: {1}" -f $group.Count, $first.displayName)
    Write-Output ("  base: {0}" -f (Get-MetaValue $metadata "subtypeOf"))
    Write-Output ("  tab:  {0} ({1})" -f (Get-MetaValue $metadata "creativeTabLabel"), (Get-MetaValue $metadata "creativeTabId"))
    Write-Output ("  class: {0}" -f (Get-MetaValue $metadata "itemClass"))
    foreach ($node in $group) {
        Write-Output ("    {0}" -f $node.id)
    }
    $shown++
}

if ($duplicates.Count -gt $shown) {
    Write-Output ""
    Write-Output ("... {0} more duplicate groups not shown. Raise -Limit to see more." -f ($duplicates.Count - $shown))
}
