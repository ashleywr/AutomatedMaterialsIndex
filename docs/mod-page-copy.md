# AMI Mod Page Copy

Use this as the long description for Modrinth and CurseForge.

## Short Summary

Search, recipe browsing, and content indexing for large modded Minecraft packs.

## Long Description

Automated Materials Index, or AMI, is a client-side search and browsing mod for large Minecraft modpacks. It builds an index of items, entities, biomes, structures, and recipe data, then gives you a focused way to search and browse that content in game.

AMI is built for packs where the item list is too large to scan by hand. It groups related blocks and items, shows useful metadata, and connects its panels with JEI or EMI when those recipe viewers are installed.

## What AMI Adds

- Search across items, entities, biomes, and structures from one AMI interface.
- Group related block variants such as stairs, slabs, walls, colors, and material families.
- Filter by mod, tag, category, property, and numeric stats.
- Find useful item details such as durability, damage, armor, food values, storage, fluid capacity, and energy capacity when AMI can detect them.
- Browse entity entries with health, damage, category, spawn egg support, and traits such as mountable or tamable when available.
- Use AMI panels for favorites, lookup history, crafting history, and craftable entries.
- Transfer recipes through JEI or EMI when the current screen supports recipe transfer.

## Basic Controls

Default keybinds can be changed in Minecraft's Controls menu.

Inventory and AMI panels:

- Open or hide AMI overlay: V
- Show recipes for a hovered item: R
- Show uses for a hovered item: U
- Add or remove a hovered entry from favorites: A
- Select the search bar: Control + F
- Go back in AMI's built-in recipe viewer: Backspace or Escape

Result entries:

- Left-click: primary action for the current AMI setting
- Right-click: open the context menu
- Control + Right-click: filter by group when hovering a group header
- Shift: show extra item details when available

## Search Examples

AMI supports plain text search and structured filters.

```text
iron gear
@create
#forge:ingots
>energy:50000
>damage:15
>dps:8
```

## Recipe Viewer Support

AMI can use its built-in recipe viewer. If JEI or EMI is installed, AMI can also open recipes and uses through the installed viewer.

Recipe transfer works from AMI panels when the current screen accepts the transfer. This includes common crafting actions from favorites, lookup history, crafting history, and craftable side panels.

## Pack Author Features

AMI includes optional pack-author tools for copying item reports, category information, quest helper text, and recipe starter snippets. These tools are intended for pack development and can be left disabled during normal play.

## Supported Loaders and Versions

- NeoForge 1.21.1
- Forge 1.20.1

JEI and EMI are optional. AMI works best with one of them installed if you want full recipe viewer integration.

## FAQ

### Why use AMI if I already have JEI or EMI?

JEI and EMI are excellent recipe viewers. AMI is for the parts of a large pack that are harder to handle with a recipe list alone: finding related items across mods, grouping block families, searching entities, biomes, and structures, comparing item stats, and keeping useful panels beside your inventory.

You can keep the recipe viewer you already like. AMI can send recipe and use lookups to JEI or EMI while adding its own search, grouping, metadata, history, favorites, and pack-author tools around it.

### Can I use AMI with JEI or EMI?

Yes. AMI can open recipes and uses through JEI or EMI when one is installed. It can also use its own built-in recipe viewer.

### Does AMI replace JEI or EMI?

For most players, AMI works best alongside a recipe viewer. AMI focuses on search, grouping, metadata, panels, and pack-scale browsing. JEI and EMI provide broad recipe viewer support across the mod ecosystem.

### Can AMI run client-only?

AMI is primarily client-side. Some actions, such as cheat actions or recipe transfer behavior, depend on the current loader, server, installed mods, and screen support.

### Why are some items uncategorized or missing metadata?

AMI reads what it can from registries, tags, recipes, item data, and compatible mods. Some generated items, custom recipe systems, or unusual runtime-only entries may not expose enough information for AMI to classify perfectly.

## Current Status

AMI 1.0 is the first public release. Large modpacks vary widely, so edge cases can happen with unusual items, generated variants, or custom recipe systems. Reports with the modpack name, loader, Minecraft version, and a short reproduction path are the most useful.

AMI also welcomes mod authors who want to own more of their integration story. If a mod wants to expose searchable
guides, enriched item metadata, representative generated stacks, or item actions from its own codebase, AMI is built to
consume shared compat providers instead of forcing every long-term integration to live downstream in AMI.
