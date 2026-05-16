package com.sanhiruzu.ami.util;

public class ColorUtils {
    public static int parseHexColor(String hex) {
        if (hex.startsWith("#")) {
            hex = hex.substring(1);
        } else if (hex.startsWith("0x")) {
            hex = hex.substring(2);
        }
        return (int) Long.parseLong(hex, 16);
    }
}
