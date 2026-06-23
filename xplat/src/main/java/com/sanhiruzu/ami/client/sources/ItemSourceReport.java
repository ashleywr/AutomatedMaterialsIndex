package com.sanhiruzu.ami.client.sources;

import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

public final class ItemSourceReport {
    private static final List<ItemSourceType> DISPLAY_ORDER = List.of(
            ItemSourceType.MOB_DROP,
            ItemSourceType.RECIPE,
            ItemSourceType.PROCESSING,
            ItemSourceType.INDIRECT_SOURCE,
            ItemSourceType.SALVAGE,
            ItemSourceType.STRUCTURE_LOOT,
            ItemSourceType.TRADE
    );

    private final Component title;
    private final Map<ItemSourceType, List<ItemSourceRow>> rowsByType;
    private final boolean loading;
    private final List<Component> diagnostics;

    public ItemSourceReport(Component title, List<ItemSourceRow> rows) {
        this(title, rows, false);
    }

    public ItemSourceReport(Component title, List<ItemSourceRow> rows, boolean loading) {
        this(title, rows, loading, List.of());
    }

    public ItemSourceReport(Component title, List<ItemSourceRow> rows, boolean loading, List<Component> diagnostics) {
        this.title = title == null ? Component.empty() : title;
        this.loading = loading;
        this.diagnostics = diagnostics == null ? List.of() : diagnostics.stream()
                .filter(component -> component != null && !component.getString().isBlank())
                .toList();
        this.rowsByType = new EnumMap<>(ItemSourceType.class);
        if (rows != null) {
            for (ItemSourceRow row : rows) {
                if (row == null || row.type() == null || row.text().isBlank()) continue;
                rowsByType.computeIfAbsent(row.type(), ignored -> new ArrayList<>()).add(row);
            }
        }
    }

    public Component title() {
        return title;
    }

    public boolean loading() {
        return loading;
    }

    public ItemSourceReport withLoading(boolean loading) {
        return withState(loading, diagnostics);
    }

    public ItemSourceReport withDiagnostics(List<Component> diagnostics) {
        return withState(loading, diagnostics);
    }

    public ItemSourceReport withState(boolean loading, List<Component> diagnostics) {
        List<ItemSourceRow> rows = new ArrayList<>();
        for (ItemSourceType type : DISPLAY_ORDER) {
            rows.addAll(rows(type));
        }
        return new ItemSourceReport(title, rows, loading, diagnostics);
    }

    public List<Component> diagnostics() {
        return diagnostics;
    }

    public List<ItemSourceType> groupOrder() {
        List<ItemSourceType> out = new ArrayList<>();
        for (ItemSourceType type : DISPLAY_ORDER) {
            List<ItemSourceRow> rows = rowsByType.get(type);
            if (rows != null && !rows.isEmpty()) out.add(type);
        }
        return out;
    }

    public List<ItemSourceRow> rows(ItemSourceType type) {
        List<ItemSourceRow> rows = rowsByType.get(type);
        return rows == null ? List.of() : List.copyOf(rows);
    }
}
