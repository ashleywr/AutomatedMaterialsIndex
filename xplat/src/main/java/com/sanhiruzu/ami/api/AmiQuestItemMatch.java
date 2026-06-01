package com.sanhiruzu.ami.api;

import net.minecraft.resources.ResourceLocation;

public record AmiQuestItemMatch(
        AmiQuestDocument quest,
        AmiQuestTaskDocument task,
        ResourceLocation itemId
) {
}
