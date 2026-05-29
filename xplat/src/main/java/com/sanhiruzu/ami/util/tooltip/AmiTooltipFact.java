package com.sanhiruzu.ami.util.tooltip;

import com.sanhiruzu.ami.index.SearchNode;
import net.minecraft.network.chat.Component;

import java.util.List;

public interface AmiTooltipFact {
    List<Component> build(SearchNode entry);
}
