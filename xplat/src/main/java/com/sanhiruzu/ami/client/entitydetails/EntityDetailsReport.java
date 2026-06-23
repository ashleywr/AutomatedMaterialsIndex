package com.sanhiruzu.ami.client.entitydetails;

import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

public final class EntityDetailsReport {
    private static final List<EntityDetailsSection> DISPLAY_ORDER = List.of(
            EntityDetailsSection.STATS,
            EntityDetailsSection.SPAWNS,
            EntityDetailsSection.DROPS,
            EntityDetailsSection.EXTERNAL_INFO
    );

    private final Component title;
    private final Map<EntityDetailsSection, List<EntityDetailsRow>> rowsBySection;
    private final boolean loading;
    private final List<Component> diagnostics;

    public EntityDetailsReport(Component title, List<EntityDetailsRow> rows) {
        this(title, rows, false, List.of());
    }

    public EntityDetailsReport(Component title, List<EntityDetailsRow> rows, boolean loading,
                               List<Component> diagnostics) {
        this.title = title == null ? Component.empty() : title;
        this.loading = loading;
        this.diagnostics = diagnostics == null ? List.of() : diagnostics.stream()
                .filter(component -> component != null && !component.getString().isBlank())
                .toList();
        this.rowsBySection = new EnumMap<>(EntityDetailsSection.class);
        if (rows != null) {
            for (EntityDetailsRow row : rows) {
                if (row == null || row.section() == null || row.text().isBlank()) continue;
                rowsBySection.computeIfAbsent(row.section(), ignored -> new ArrayList<>()).add(row);
            }
        }
    }

    public Component title() {
        return title;
    }

    public boolean loading() {
        return loading;
    }

    public List<Component> diagnostics() {
        return diagnostics;
    }

    public EntityDetailsReport withState(boolean loading, List<Component> diagnostics) {
        List<EntityDetailsRow> rows = new ArrayList<>();
        for (EntityDetailsSection section : DISPLAY_ORDER) {
            rows.addAll(rows(section));
        }
        return new EntityDetailsReport(title, rows, loading, diagnostics);
    }

    public List<EntityDetailsSection> groupOrder() {
        List<EntityDetailsSection> out = new ArrayList<>();
        for (EntityDetailsSection section : DISPLAY_ORDER) {
            List<EntityDetailsRow> rows = rowsBySection.get(section);
            if (rows != null && !rows.isEmpty()) out.add(section);
        }
        return out;
    }

    public List<EntityDetailsRow> rows(EntityDetailsSection section) {
        List<EntityDetailsRow> rows = rowsBySection.get(section);
        return rows == null ? List.of() : List.copyOf(rows);
    }
}
