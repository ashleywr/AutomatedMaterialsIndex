package com.sanhiruzu.ami.index;

public enum RegistryDocumentKind {
    ENCHANTMENT,
    MOB_EFFECT,
    TAG,
    GAME_RULE,
    PAINTING;

    /** Token added to every document of this kind so `$enchantment` etc. works. */
    public String categoryToken() {
        return name().toLowerCase(java.util.Locale.ROOT).replace('_', ' ');
        // yields: "enchantment", "mob effect", "tag", "game rule", "painting"
    }
}
