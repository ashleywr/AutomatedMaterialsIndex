package com.sanhiruzu.ami.compat;

import com.sanhiruzu.ami.client.icon.CobblemonPokemonIconRenderer;

public final class CobblemonCompatModule implements ModCompatModule {
    @Override
    public String modId() {
        return "cobblemon";
    }

    @Override
    public String probeClassName() {
        return "com.cobblemon.mod.common.api.pokemon.PokemonSpecies";
    }

    // Primary click intentionally does NOT open the Pokedex anymore — it falls through to AMI's
    // normal item-click handling, which opens the recipe viewer (JEI/EMI) for the clicked
    // species, matching every other result type. Opening the Pokedex is still available via the
    // "Open Pokedex" right-click context menu action (ResultContextMenuActionBuilder).

    @Override
    public void invalidateCaches() {
        CobblemonPokedexOpener.invalidateCaches();
        CobblemonPokemonIconRenderer.invalidate();
    }
}
