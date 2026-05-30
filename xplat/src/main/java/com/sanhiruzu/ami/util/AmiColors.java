package com.sanhiruzu.ami.util;

public final class AmiColors {
    // Query token colors
    public static final int MOD_COLOR = 0xFF5555FF; // Blue  — mod namespaces and @mod queries
    public static final int TAG_COLOR = 0xFFFFAA00; // Gold  — tags and #tag queries
    public static final int EXCLUDE_COLOR = 0xFFFF5555; // Red   — excluded/negated tokens
    public static final int TEXT_DEFAULT = 0xFFFFFFFF; // White — default foreground text
    public static final int TEXT_SUBTLE = 0xFF555555; // Dark grey — metadata, counts, hints
    // Query token prefix colors
    public static final int TOKEN_ENV = 0xFF44BB44; // Green   — &environment
    public static final int TOKEN_PROP = 0xFFBBBB44; // Yellow  — ?property
    public static final int TOKEN_ESSENTIAL = 0xFFBB44BB; // Magenta — !essential
    public static final int TOKEN_ESM = 0xFFBB8844; // Orange  — >/<,= comparisons
    public static final int TOKEN_META = 0xFF44CCCC; // Cyan    — ~meta
    public static final int TOKEN_PLAIN = 0xFFCCCCCC; // Light grey — bare words
    public static final int TOKEN_OR = 0xFFFFFFFF;    // White   — | OR separator
    // Registry category colors (shared with index providers)
    public static final int CATEGORY_MONSTER = 0xFFCC4444;
    public static final int CATEGORY_CREATURE = 0xFF44AA44;
    public static final int CATEGORY_AMBIENT = 0xFFAAAA44;
    public static final int CATEGORY_AQUATIC = 0xFF4488CC;
    public static final int CATEGORY_DEFAULT = 0xFF888888;
    // Registry dimension colors (shared with index providers)
    public static final int DIM_OVERWORLD = 0xFF66BB6A;
    public static final int DIM_NETHER = 0xFFCC4444;
    public static final int DIM_END = 0xFF7A51A6;
    // Player name tint (used in index resolvers)
    public static final int PLAYER_NAME_COLOR = 0xFF4488FF;

    private AmiColors() {
    }
}
