package com.sanhiruzu.ami.api;

import net.minecraft.resources.Identifier;

public record AmiQuestItemMatch(
        AmiQuestDocument quest,
        AmiQuestTaskDocument task,
        Identifier itemId
) {
}
