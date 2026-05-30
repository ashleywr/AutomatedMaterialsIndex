package com.sanhiruzu.ami.util.tooltip;

import com.sanhiruzu.ami.index.SearchNode;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.List;

public final class AmiTooltipFacts {
    private static final List<AmiTooltipFact> FACTS = List.of(
            new RequiredToolTooltipFact(),
            new StorageTooltipFact(),
            new EnergyTooltipFact(),
            new DurabilityTooltipFact(),
            new CombatTooltipFact()
    );

    private AmiTooltipFacts() {
    }

    public static List<Component> build(SearchNode entry) {
        List<Component> lines = new ArrayList<>();
        for (AmiTooltipFact fact : FACTS) {
            lines.addAll(fact.build(entry));
        }
        return lines;
    }
}
