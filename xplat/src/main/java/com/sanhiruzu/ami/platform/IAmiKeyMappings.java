package com.sanhiruzu.ami.platform;

import net.minecraft.client.KeyMapping;

public interface IAmiKeyMappings {
    KeyMapping favorite();
    KeyMapping toggleViewer();
    KeyMapping showRecipes();
    KeyMapping showUses();
    KeyMapping cheatGiveStack();
    KeyMapping cheatGiveOne();
    KeyMapping debugTooltips();
    KeyMapping recipeBack();
    KeyMapping[] all();
}
