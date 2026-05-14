package com.sanhiruzu.ami.client;

/**
 * Central palette for all AMI UI colours.
 * All rendering code reads from here so the entire look can be changed in one place.
 * Fields are non-final so a future config screen can overwrite them at startup.
 */
public final class AMITheme {
    private AMITheme() {}

    // Panel chrome
    public static int PANEL_BG       = 0xCC000000;
    public static int PANEL_INNER    = 0xFF2A2A2A;

    // Header bar (mode label row at the top of every panel)
    public static int HEADER_BG      = 0xFF1A3A1A;
    public static int HEADER_SEP     = 0xFF4A6A4A;
    public static int HEADER_TEXT    = 0xFF88FF88;

    // Namespace group headers in atlas lists
    public static int GROUP_BG       = 0xFF1E3A1E;
    public static int GROUP_BG_HOVER = 0xFF2A4A2A;
    public static int GROUP_TEXT     = 0xFF99DD99;

    // Atlas entry rows
    public static int ENTRY_HOVER    = 0xFF3A5A3A;
    public static int ENTRY_TEXT     = 0xFFCCCCCC;

    // Scrollbar
    public static int SCROLL_TRACK   = 0xFF333333;
    public static int SCROLL_THUMB   = 0xFF88AA88;

    // Dimension badges
    public static int DIM_NETHER     = 0xFFCC4444;
    public static int DIM_END        = 0xFF9944CC;

    // Item grid
    public static int SLOT_BG        = 0xFF555555;
    public static int SLOT_HOVER     = 0xFFAAAAAA;
}
