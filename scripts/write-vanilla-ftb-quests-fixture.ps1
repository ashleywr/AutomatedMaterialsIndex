param(
    [string]$AmiRepo = (Resolve-Path (Join-Path $PSScriptRoot '..')).Path,
    [string]$RunName = 'neoforge-emi',
    [switch]$IncludeSmartFilters,
    [switch]$CleanGenerated
)

$ErrorActionPreference = 'Stop'

$runDir = Join-Path $AmiRepo "run\$RunName"
$questsDir = Join-Path $runDir 'config\ftbquests\quests'
$chaptersDir = Join-Path $questsDir 'chapters'

New-Item -ItemType Directory -Force -Path $chaptersDir | Out-Null

$utf8NoBom = New-Object System.Text.UTF8Encoding($false)

function Write-Utf8NoBom {
    param(
        [Parameter(Mandatory = $true)][string]$Path,
        [Parameter(Mandatory = $true)][string]$Value
    )

    [System.IO.File]::WriteAllText($Path, $Value, $utf8NoBom)
}

if ($CleanGenerated) {
    Get-ChildItem -Path $chaptersDir -Filter 'ami_*.snbt' -File -ErrorAction SilentlyContinue |
        Remove-Item -Force
}

$data = @'
{
	default_autoclaim_rewards: "disabled"
	default_consume_items: false
	default_quest_disable_jei: false
	default_quest_shape: "circle"
	default_reward_team: false
	detection_delay: 20
	disable_gui: false
	drop_loot_crates: false
	emergency_items_cooldown: 300
	grid_scale: 0.5d
	icon: {
		id: "minecraft:book"
	}
	lock_message: ""
	pause_game: false
	progression_mode: "flexible"
	show_lock_icons: true
	version: 13
}
'@

$chapter = @'
{
	default_hide_dependency_lines: false
	default_quest_shape: "circle"
	filename: "ami_vanilla_progression"
	icon: {
		id: "minecraft:crafting_table"
	}
	id: "414D4956414E0001"
	order_index: 0
	quest_links: [ ]
	quests: [
		{
			description: ["Start with logs, then craft a table."]
			id: "414D4956414E0101"
			rewards: [{
				count: 4
				id: "414D4956414E0103"
				item: { count: 4, id: "minecraft:oak_planks" }
				type: "item"
			}]
			tasks: [{
				count: 4L
				id: "414D4956414E0102"
				item: { count: 1, id: "minecraft:oak_log" }
				type: "item"
			}]
			title: "Gather Logs"
			x: -3.0d
			y: 0.0d
		}
		{
			description: ["Crafting tables are referenced by multiple AMI fixture quests."]
			id: "414D4956414E0201"
			rewards: [{
				count: 8
				id: "414D4956414E0203"
				item: { count: 8, id: "minecraft:stick" }
				type: "item"
			}]
			tasks: [{
				count: 1L
				id: "414D4956414E0202"
				item: { count: 1, id: "minecraft:crafting_table" }
				type: "item"
			}]
			title: "Make a Crafting Table"
			x: 0.0d
			y: 0.0d
		}
		{
			description: ["Get cobblestone and make a furnace."]
			id: "414D4956414E0301"
			rewards: [{
				count: 4
				id: "414D4956414E0304"
				item: { count: 4, id: "minecraft:coal" }
				type: "item"
			}]
			tasks: [
				{
					count: 16L
					id: "414D4956414E0302"
					item: { count: 1, id: "minecraft:cobblestone" }
					type: "item"
				}
				{
					count: 1L
					id: "414D4956414E0303"
					item: { count: 1, id: "minecraft:furnace" }
					type: "item"
				}
			]
			title: "Stone Age"
			x: 3.0d
			y: 0.0d
		}
		{
			description: ["Iron unlocks the first real machines in many packs."]
			id: "414D4956414E0401"
			rewards: [{
				count: 1
				id: "414D4956414E0403"
				item: { count: 1, id: "minecraft:bucket" }
				type: "item"
			}]
			tasks: [{
				count: 8L
				id: "414D4956414E0402"
				item: { count: 1, id: "minecraft:iron_ingot" }
				type: "item"
			}]
			title: "Iron Tools"
			x: 6.0d
			y: 0.0d
		}
		{
			description: ["Redstone appears in requirements and rewards to test item evidence counts."]
			id: "414D4956414E0501"
			rewards: [{
				count: 4
				id: "414D4956414E0503"
				item: { count: 4, id: "minecraft:redstone" }
				type: "item"
			}]
			tasks: [{
				count: 12L
				id: "414D4956414E0502"
				item: { count: 1, id: "minecraft:redstone" }
				type: "item"
			}]
			title: "Redstone Power"
			x: 9.0d
			y: 0.0d
		}
		{
			description: ["Diamonds are a late vanilla checkpoint."]
			id: "414D4956414E0601"
			rewards: [{
				count: 1
				id: "414D4956414E0603"
				item: { count: 1, id: "minecraft:enchanting_table" }
				type: "item"
			}]
			tasks: [{
				count: 3L
				id: "414D4956414E0602"
				item: { count: 1, id: "minecraft:diamond" }
				type: "item"
			}]
			title: "Diamonds"
			x: 12.0d
			y: 0.0d
		}
		{
			description: ["A second crafting table reference verifies grouped quest evidence."]
			id: "414D4956414E0701"
			tasks: [{
				count: 1L
				id: "414D4956414E0702"
				item: { count: 1, id: "minecraft:crafting_table" }
				type: "item"
			}]
			title: "Workspace Check"
			x: 0.0d
			y: 2.0d
		}
	]
}
'@

Write-Utf8NoBom -Path (Join-Path $questsDir 'data.snbt') -Value $data
Write-Utf8NoBom -Path (Join-Path $chaptersDir 'ami_vanilla_progression.snbt') -Value $chapter

if ($IncludeSmartFilters) {
    $filterChapter = @'
{
	default_hide_dependency_lines: false
	default_quest_shape: "circle"
	filename: "ami_vanilla_filters"
	icon: {
		id: "minecraft:redstone_torch"
	}
	id: "414D4946494C0001"
	order_index: 1
	quest_links: [ ]
	quests: [
		{
			description: ["Requires any vanilla light source accepted by a smart filter."]
			id: "414D4946494C0101"
			tasks: [{
				count: 1L
				id: "414D4946494C0102"
				item: {
					components: {
						"ftbfiltersystem:filter": "or(item(minecraft:torch)item(minecraft:soul_torch)item(minecraft:redstone_torch)item(minecraft:lantern)item(minecraft:soul_lantern)item(minecraft:glowstone)item(minecraft:sea_lantern))"
					}
					count: 1
					id: "ftbfiltersystem:smart_filter"
				}
				type: "item"
			}]
			title: "Any Light Source"
			x: 0.0d
			y: 0.0d
		}
		{
			description: ["Tag filters should stay high-cardinality instead of expanding every stack."]
			id: "414D4946494C0201"
			tasks: [{
				count: 1L
				id: "414D4946494C0202"
				item: {
					components: {
						"ftbfiltersystem:filter": "ftbfiltersystem:item_tag(minecraft:logs)"
					}
					count: 1
					id: "ftbfiltersystem:smart_filter"
				}
				type: "item"
			}]
			title: "Any Log"
			x: 3.0d
			y: 0.0d
		}
	]
}
'@
    Write-Utf8NoBom -Path (Join-Path $chaptersDir 'ami_vanilla_filters.snbt') -Value $filterChapter
}

$written = @(
    Join-Path $questsDir 'data.snbt'
    Join-Path $chaptersDir 'ami_vanilla_progression.snbt'
)
if ($IncludeSmartFilters) {
    $written += Join-Path $chaptersDir 'ami_vanilla_filters.snbt'
}

[pscustomobject]@{
    RunDirectory = $runDir
    QuestDirectory = $questsDir
    IncludeSmartFilters = [bool]$IncludeSmartFilters
    WrittenFiles = $written
} | ConvertTo-Json -Depth 4
