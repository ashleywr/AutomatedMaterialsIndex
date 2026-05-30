# AMI Lexical Evidence Dictionary Draft

This is a review draft for replacing scattered substring checks with token-based lexical evidence.

Rules for using this dictionary:

- Match whole normalized tokens only. `egg` must not match `leggings`; `map` must not match `maple`; `arrow` must not match `narrow`.
- Tokens produce evidence, not final categories.
- Evidence should be scored with tags, classes, item type, recipe data, and block facts.
- Ambiguous tokens require context gates or lower weight.
- This draft is seeded from `neoforge/build/reports/ami-result-shapes/lexical-evidence.md` plus the bad in-game examples reviewed so far.

Machine-readable draft: `neoforge/src/test/resources/ami/lexical_evidence_dictionary.draft.json`

## Strong Token Groups

| Evidence | Tokens | Suggested facets | Category hint | Confidence | Notes |
| --- | --- | --- | --- | --- | --- |
| decorative_furniture | chair, table, desk, couch, sofa, bench, stool, cabinet, cabinetry, shelf, rack, counter, nightstand, wardrobe, dresser, bookcase, bookshelves, postbox | decorative_block | decoration/furniture | strong | Useful as primary evidence when not a machine/storage block. |
| decorative_lighting | lamp, lantern, chandelier, sconce, brazier, candelabra, candle, torch, glowstone, shroomlight, froglight, beacon | decorative_block, light_source | decoration/lighting | strong | `light_source` alone should not be enough; these tokens make lighting primary. |
| decorative_textiles | curtain, curtains, blinds, shutter, shutters, rug, carpet, pillow, cushion, blanket, sheet, banner, tapestry, canvas | decorative_block | decoration/textiles | medium | `banner` and `canvas` may also be utility/template depending on tags. |
| rail_transport | rail, rails, track, tracks, train, tram, monorail, coupler, conductor, semaphore, handcar, locomotive | rail, transport | tech/transport | strong | Fixes Railways items without relying on Create-family bias alone. |
| tech_machine | machine, generator, crusher, mixer, press, millstone, pump, fan, engine, motor, charger, crafter, assembler, fabricator, refinery, compressor, chamber | machine | tech/machines | medium | Some are ambiguous: `press`, `fan`, `crafter`, `charger`. Boost with block entity, energy, recipe, or known tech tags. |
| tech_redstone | redstone, piston, observer, comparator, repeater, button, lever, detector, pressure, relay, transmitter, receiver, sensor, target | redstone_logic, redstone_signal | tech/redstone | strong | `pressure` needs `plate` context for vanilla-style redstone. |
| tech_cables | cable, cables, wire, wires, pipe, pipes, tube, tubes, conduit, duct | cable, tech_component | tech/cables | strong | Gate natural false positives like coral, cobweb, frogspawn, dead_bush. |
| tech_circuits | circuit, circuits, processor, chip, chipset, logic, calculation, engineering | tech_component | tech/circuits | strong | Good candidate for integrated circuits and processor parts. |
| mechanical_component | gear, gears, gearbox, cog, cogs, cogwheel, cogwheels, shaft, shafts, belt, belts, flywheel | mechanical_component, tech_component | tech/parts | strong | Avoid matching unrelated substrings. |
| template | blueprint, schematic, template, mold | template | tech/templates | strong | Should outrank incidental ingredient signals. |
| wood_block | log, logs, planks, stem, stems, hyphae, stripped | log, wood_block | nature/wood | strong | Best when path suffix or block tags agree. |
| wood_material_variant | pine, palm, maple, cherry, willow, mahogany, rosewood, runewood, oak, spruce, birch, jungle, acacia, mangrove, crimson, warped | none | none | weak | Material/family evidence only. Do not use as category evidence by itself. |
| flora | sapling, saplings, leaves, leaf, flower, flowers, bush, shrub, sprouts, roots, vine, vines, moss, coral, seagrass, kelp, cactus, fern, clover | flower, leaves, nature_misc | nature/flora | strong | `leaf` is weaker than `leaves`; enchanted-book names can contain leaf effects. |
| seeds | seed, seeds | seed | nature/seeds | medium | Gate out pouches, buckets, oils, crystal seeds, machines, and technical seeds. |
| crops | crop, crops, wheat, carrot, potato, beetroot, tomato, cabbage, onion, pepper, cucumber, grape, berry, berries, rice | crop | nature/crops | medium | Many are also food tokens; placeable/block class/tags should decide crops vs snacks. |
| fungi | mushroom, mushrooms, fungus, fungi, mycelium, nylium | fungi | nature/fungi | strong | Mushroom building variants may still be masonry/decor. |
| food_meal | soup, stew, sandwich, burger, pizza, pasta, noodle, noodles, rice, kebab, salad, dumpling, dumplings, casserole, lasagna, quiche, plate, bowl, meal | edible, food_meal | nature/meals | strong | `bowl` as item is utility/container unless food evidence exists. |
| food_snack | cake, pie, cookie, bread, tart, pudding, icecream, preserve, preserves, jam, jelly, candy, chocolate, berry, berries, fruit, apple | edible | nature/snacks | medium | Placeable cakes/pies should stay food/nature, not Full Blocks. |
| food_drink | juice, soda, beer, wine, coffee, tea, cider, milk, bottle, drink, smoothie | food_drink | nature/drinks | medium | `bottle` is ambiguous; needs food/drink context. |
| food_protein | beef, chicken, pork, porkchop, cod, salmon, rabbit, mutton, fish, meat, bacon, ham, sausage, ribs, wing, wings, venison | food_protein | nature/proteins | strong | Entity/body-part mod items may need ingredient/mob-drop evidence instead. |
| organic_ingredient | string, feather, feathers, bone, bones, leather, hide, scute, honeycomb, wool, egg, eggs | ingredient_organic | ingredients/organic | strong | Exact tokens only. |
| natural_shell | shell, shells | ingredient_organic | ingredients/organic | weak | Only if no ammo/firearm context. Natural shells should not become projectile. |
| mineral_ingredient | flint, clay, shard, crystal, crystals, prismarine, pottery, sherd, brick, bricks | ingredient_mineral | ingredients/mineral | medium | `crystal`, `shard`, and `brick` are often category-dependent. |
| dyes | dye, dyes, pigment, pigments, ink | ingredient_dye | ingredients/dyes | strong | `dye` should be token/exact, not substring. |
| melee_weapon | sword, swords, dagger, daggers, spear, spears, mace, club, katana | melee_weapon | tools/melee | medium | Some weapon words also appear in decorative names. |
| ranged_weapon | bow, bows, crossbow, crossbows, gun, rifle, pistol, musket, cannon | ranged_weapon | tools/ranged | medium | `cannon` can also mean machine/transport part. |
| ammo_projectile | arrow, arrows, bolt, bolts, bullet, bullets, round, rounds, cartridge, cartridges, grenade, rocket | projectile | tools/ammo | strong | Exact tokens only. |
| ammo_shell | shell, shells | projectile | tools/ammo | contextual | Require ammo, gun, shotgun, cannon, autocannon, artillery, mortar, munition, or weapon context. |
| harvest_tool | pickaxe, pickaxes, shovel, shovels, axe, axes, hoe, hoes, sickle, scythe, shears | harvest_tool | tools/harvest | strong | `axe` should be token only. |
| utility_tool | wrench, hammer, brush, fishing, rod, flint, steel | utility_tool | tools/utility | medium | `hammer`, `flint`, and `steel` are ambiguous without tag/item-type evidence. |
| armor | helmet, helmets, chestplate, chestplates, leggings, boots, armor, elytra | armor_head/chest/legs/feet | armor/* | strong | Should not imply organic ingredient due to `egg` inside `leggings`. |
| animal_armor | horse, harness, saddle | armor_animal | armor/animal | medium | `saddle` may be utility unless wearable/entity equipment facts exist. |
| navigation | compass, map, maps, clock, spyglass | utility_navigation | utility/navigation | strong | Exact token only. |
| medical | bandage, medkit, syringe, morphine, adrenaline, splint | utility_medical | utility/medical | strong | Good lexical evidence. |
| currency | coin, coins, cash, money, credit, card | utility_currency | utility/currency | medium | `card` can be tech/card/module. |
| magic_artifact | spell, wand, staff, scroll, totem, charm, relic | magic_artifact | magic/artifacts | medium | `charm` may also be curio. |
| magic_reagent | rune, runes, essence, pearl, blaze, ghast, phantom, membrane, wart, slime | magic_reagent | magic/reagents | medium | `rune` token only; do not match `runewood`. `essence` can be very broad. |
| spawn_egg | spawn, egg | spawn_egg | bestiary/* | contextual | Prefer actual `SpawnEggItem`; lexical fallback only for full `spawn_egg` pattern. |

## Ambiguous Or Review-Only Tokens

These should not be standalone category evidence:

| Token | Why |
| --- | --- |
| light | Color adjective and light-source trait; not primary lighting evidence. |
| shell | Natural shells and ammunition share the word. Needs context. |
| pattern | Can be banner pattern, smithing template, masonry pattern, or decorative variant. |
| fan | Machine fan, ceiling fan, decoration. Needs block/mod/facet context. |
| press | Machine press, wine press, printed press names. Needs context. |
| controller | Tech controller, handheld controller, UI/control items. |
| module | Tech part, upgrade, magic module, generic component. |
| core | Magic reagent, machine core, mob drop, material. |
| seed | Plant seed, crystal seed, seed oil, seed pouch. |
| crystal | Mineral, magic reagent, tech seed/component. |
| gem | Material evidence, not necessarily Tech. |
| leaf | Flora in many cases, but also enchant/effect names. |
| wood-family names | Pine, maple, runewood, etc. should influence family/material grouping, not primary category. |
| colors | Color names should almost never decide category. |

