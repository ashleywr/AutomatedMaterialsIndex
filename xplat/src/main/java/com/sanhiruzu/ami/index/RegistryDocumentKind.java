package com.sanhiruzu.ami.index;

import java.util.Locale;

public enum RegistryDocumentKind {
    ENCHANTMENT,
    MOB_EFFECT,
    TAG,
    GAME_RULE,
    PAINTING;

    /** Token added to every document of this kind so `$enchantment` etc. works. */
    public String categoryToken() {
        return name().toLowerCase(Locale.ROOT).replace('_', ' ');
        // yields: "enchantment", "mob effect", "tag", "game rule", "painting"
    }
}
