package com.sanhiruzu.ami.client.results;

import com.sanhiruzu.ami.config.AmiConfig;
import com.sanhiruzu.ami.index.AmiOntology;
import com.sanhiruzu.ami.index.ItemFilter;
import com.sanhiruzu.ami.index.SearchNode;
import com.sanhiruzu.ami.index.SearchNodeKeys;

import java.util.List;
import java.util.stream.Collectors;

final class ResultsFilter {
    private final ResultsPresentationOptions options;

    ResultsFilter(ResultsPresentationOptions options) {
        this.options = options;
    }

    List<SearchNode> filter(List<SearchNode> results) {
        return results.stream()
                .filter(n -> options.selectedMods().isEmpty() || options.selectedMods().contains(n.id().getNamespace()))
                .filter(this::matchesFacets)
                .filter(this::matchesAccessLevel)
                .filter(this::matchesVisibility)
                .collect(Collectors.toList());
    }

    private boolean matchesVisibility(SearchNode node) {
        if (AmiConfig.devMode) return true;
        return !"hidden".equals(node.meta(SearchNodeKeys.VISIBILITY, ""));
    }

    private boolean matchesFacets(SearchNode node) {
        if (options.activeFacets().isEmpty()) return true;
        return options.activeFacets().contains(AmiOntology.classifyNode(node).id);
    }

    private boolean matchesAccessLevel(SearchNode node) {
        String level = node.meta(SearchNodeKeys.ACCESS_LEVEL, "");
        if (ItemFilter.ACCESS_DEV.equals(level)) return AmiConfig.devMode;
        if (ItemFilter.ACCESS_CHEAT.equals(level)) return AmiConfig.cheatMode || AmiConfig.devMode;

        if (ItemFilter.ACCESS_CREATIVE.equals(level)) {
            if (AmiConfig.cheatMode || AmiConfig.devMode) {
                return true;
            }
            net.minecraft.client.multiplayer.MultiPlayerGameMode gameMode = net.minecraft.client.Minecraft.getInstance().gameMode;
            if (gameMode != null && gameMode.getPlayerMode() == net.minecraft.world.level.GameType.SURVIVAL) {
                return false;
            }
        }

        return true;
    }
}
