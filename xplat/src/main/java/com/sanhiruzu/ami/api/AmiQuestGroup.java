package com.sanhiruzu.ami.api;

import net.minecraft.network.chat.Component;

import java.util.List;

/**
 * A group of quest requirements registered by an external mod (e.g. FTB Quests).
 * Each group appears as a collapsible section in the Quests sidebar panel.
 *
 * @param id       unique identifier for this group (e.g. "ftbquests:chapter_3_basic_circuits")
 * @param label    display name shown as the group header
 * @param entries  the item requirements in this group
 * @param priority sort order; lower values appear first (default 0)
 */
public record AmiQuestGroup(String id, Component label, List<AmiQuestEntry> entries, int priority) {
    public AmiQuestGroup {
        if (id == null || id.isBlank()) {
            throw new IllegalArgumentException("Quest group id must not be blank");
        }
        label = label == null ? Component.literal(id) : label;
        entries = entries == null ? List.of() : List.copyOf(entries);
    }

    public AmiQuestGroup(String id, Component label, List<AmiQuestEntry> entries) {
        this(id, label, entries, 0);
    }
}
