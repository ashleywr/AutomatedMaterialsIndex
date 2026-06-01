package com.cobblemon.mod.common.pokemon;

import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

public final class Species {
    public ResourceLocation getResourceIdentifier() {
        return ResourceLocation.fromNamespaceAndPath("cobblemon", "bulbasaur");
    }

    public Component getTranslatedName() {
        return Component.literal("Bulbasaur");
    }

    public int getNationalPokedexNumber() {
        return 1;
    }

    public boolean getImplemented() {
        return true;
    }

    public Object getPrimaryType() {
        return new NamedValue("grass");
    }

    public Object getSecondaryType() {
        return new NamedValue("poison");
    }

    public Map<Object, Integer> getBaseStats() {
        Map<Object, Integer> stats = new HashMap<>();
        stats.put(new StatValue("hp"), 45);
        stats.put(new StatValue("attack"), 49);
        return stats;
    }

    public HashSet<Object> getEggGroups() {
        return new HashSet<>(List.of(new EggGroupValue("monster")));
    }

    public List<Object> getAbilities() {
        return List.of(new PotentialAbilityValue("overgrow"));
    }

    public Object getMoves() {
        return new LegacyLearnset();
    }

    public float getHeight() {
        return 0.7f;
    }

    public float getWeight() {
        return 6.9f;
    }

    private record NamedValue(String name) {
        public String getName() {
            return name;
        }
    }

    private record StatValue(String showdownId) {
        public String getShowdownId() {
            return showdownId;
        }
    }

    private record EggGroupValue(String showdownId) {
        public String getShowdownID$common() {
            return showdownId;
        }
    }

    private record PotentialAbilityValue(String name) {
        public Object getTemplate() {
            return new NamedValue(name);
        }
    }

    private static final class LegacyLearnset {
        public List<Object> getTmMoves() {
            return List.of(new NamedValue("solar-beam"));
        }

        public List<Object> getEggMoves() {
            return List.of(new NamedValue("skull bash"));
        }

        public List<Object> getTutorMoves() {
            return List.of();
        }

        public Map<Integer, List<Object>> getLevelUpMoves() {
            return Map.of(1, List.of(new NamedValue("tackle")));
        }
    }
}
