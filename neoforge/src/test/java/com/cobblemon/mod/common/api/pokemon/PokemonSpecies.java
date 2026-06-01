package com.cobblemon.mod.common.api.pokemon;

import com.cobblemon.mod.common.pokemon.Species;

import java.util.Collection;
import java.util.List;

public final class PokemonSpecies {
    public static final PokemonSpecies INSTANCE = new PokemonSpecies();

    private PokemonSpecies() {
    }

    public Collection<Species> getSpecies() {
        return List.of(new Species());
    }
}
