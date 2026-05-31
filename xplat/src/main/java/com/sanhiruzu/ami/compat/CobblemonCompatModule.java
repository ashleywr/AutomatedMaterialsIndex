package com.sanhiruzu.ami.compat;

import com.sanhiruzu.ami.client.icon.CobblemonPokemonIconRenderer;
import com.sanhiruzu.ami.index.SearchNode;

public final class CobblemonCompatModule implements ModCompatModule {
    @Override
    public String modId() {
        return "cobblemon";
    }

    @Override
    public String probeClassName() {
        return "com.cobblemon.mod.common.api.pokemon.PokemonSpecies";
    }

    @Override
    public boolean handleResultClick(SearchNode node, int button) {
        return button == 0 && CobblemonPokedexOpener.handlePrimaryClick(node);
    }

    @Override
    public void invalidateCaches() {
        CobblemonPokedexOpener.invalidateCaches();
        CobblemonPokemonIconRenderer.invalidate();
    }
}
