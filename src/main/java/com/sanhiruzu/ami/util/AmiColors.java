package com.sanhiruzu.ami.util;

public final class AmiColors {
    private AmiColors() {}

    public static final int MOD_COLOR     = 0xFF5555FF; // Blue  — mod namespaces and @mod queries
    public static final int TAG_COLOR     = 0xFFFFAA00; // Gold  — tags and #tag queries
    public static final int EXCLUDE_COLOR = 0xFFFF5555; // Red   — excluded/negated tokens
    public static final int TEXT_DEFAULT  = 0xFFFFFFFF; // White — default foreground text
    public static final int TEXT_SUBTLE   = 0xFF555555; // Dark grey — metadata, counts, hints
}
