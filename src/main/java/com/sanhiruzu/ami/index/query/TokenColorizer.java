package com.sanhiruzu.ami.index.query;

import com.sanhiruzu.ami.util.AmiColors;

import java.util.ArrayList;
import java.util.List;

public final class TokenColorizer {
    private TokenColorizer() {
    }

    public record ColorSpan(int startIndex, int endIndex, int argbColor) {
    }

    static final int COLOR_TAG = AmiColors.TAG_COLOR;
    static final int COLOR_MOD = AmiColors.MOD_COLOR;
    static final int COLOR_EXCLUDE = AmiColors.EXCLUDE_COLOR;
    static final int COLOR_ENV = AmiColors.TOKEN_ENV;
    static final int COLOR_PROP = AmiColors.TOKEN_PROP;
    static final int COLOR_ESSENTIAL = AmiColors.TOKEN_ESSENTIAL;
    static final int COLOR_ESM = AmiColors.TOKEN_ESM;
    static final int COLOR_META = AmiColors.TOKEN_META;
    static final int COLOR_PLAIN = AmiColors.TOKEN_PLAIN;

    /**
     * Colorize a query string, returning color spans for each token.
     * Handles prefixes (~, #, &, ?, !, >, <, =) and negation (-).
     */
    public static List<ColorSpan> colorize(String queryText) {
        if (queryText == null || queryText.isEmpty()) {
            return List.of();
        }

        List<ColorSpan> spans = new ArrayList<>();
        int pos = 0;
        boolean inQuotes = false;

        while (pos < queryText.length()) {
            // Skip spaces
            if (!inQuotes && queryText.charAt(pos) == ' ') {
                pos++;
                continue;
            }

            // Handle quotes
            if (queryText.charAt(pos) == '"') {
                inQuotes = !inQuotes;
                pos++;
                continue;
            }

            // Find end of current token
            int tokenStart = pos;
            while (pos < queryText.length()) {
                char c = queryText.charAt(pos);
                if (!inQuotes && c == ' ') break;
                if (!inQuotes && c == '"') break;
                pos++;
            }

            String token = queryText.substring(tokenStart, pos);
            int color = colorForToken(token);
            spans.add(new ColorSpan(tokenStart, pos, color));
        }

        return spans;
    }

    private static int colorForToken(String token) {
        if (token.isEmpty()) return COLOR_PLAIN;

        boolean isExclude = token.startsWith("-");
        String stripped = isExclude ? token.substring(1) : token;

        if (stripped.isEmpty()) return COLOR_PLAIN;

        char prefix = stripped.charAt(0);
        int color = switch (prefix) {
            case '~' -> COLOR_META;
            case '#' -> COLOR_TAG;
            case '@' -> COLOR_MOD;
            case '&' -> COLOR_ENV;
            case '?' -> COLOR_PROP;
            case '!' -> COLOR_ESSENTIAL;
            case '>', '<', '=' -> COLOR_ESM;
            default -> COLOR_PLAIN;
        };

        // Bare negation (no recognised prefix) → exclude color
        if (isExclude && prefix != '~' && prefix != '#' && prefix != '@' && prefix != '&'
                && prefix != '?' && prefix != '!' && prefix != '>' && prefix != '<' && prefix != '=') {
            color = COLOR_EXCLUDE;
        }

        return color;
    }
}
