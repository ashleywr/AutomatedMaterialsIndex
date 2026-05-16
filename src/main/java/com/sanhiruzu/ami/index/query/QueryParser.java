package com.sanhiruzu.ami.index.query;

import java.util.ArrayList;
import java.util.List;

public final class QueryParser {
    private QueryParser() {}

    public enum TokenType {
        INCLUDE,     // Plain text or -less
        EXCLUDE,     // Negation: -word
        TAG,         // #tag
        MOD,         // @modid
        ENV,         // &env
        PROP,        // ?property
        ESSENTIAL,   // !curated
        ESM          // >capacity:value
    }

    public record QueryToken(TokenType type, String value) {}

    public record ParsedQuery(List<QueryToken> tokens) {
        public ParsedQuery(List<QueryToken> tokens) {
            this.tokens = List.copyOf(tokens);
        }
    }

    /**
     * Parse a raw query string into typed tokens.
     * Supports:
     * - Plain text: "creeper" → INCLUDE
     * - Negation: "-creeper", "-#ore" → EXCLUDE with type inherited from suffix
     * - Tags: "#storage" → TAG
     * - Environment: "&nether" → ENV
     * - Properties: "?precipitation:rain" → PROP
     * - Essential: "!storage" → ESSENTIAL
     * - Numeric metrics: ">storage:100", ">dps:8", "<storage:4096" → ESM
     *
     * Tokens are space-separated. Quoted strings ("iron chest") treated as single token.
     * Leading "-" on any prefix inverts it to EXCLUDE.
     */
    public static ParsedQuery parse(String queryText) {
        if (queryText == null || queryText.isBlank()) {
            return new ParsedQuery(List.of());
        }

        List<QueryToken> tokens = new ArrayList<>();
        String text = queryText.trim();

        // Tokenize: split by spaces, but respect quoted strings
        List<String> parts = tokenizeWithQuotes(text);

        for (String part : parts) {
            if (part.isEmpty()) continue;

            QueryToken token = parseToken(part);
            if (token != null) {
                tokens.add(token);
            }
        }

        return new ParsedQuery(tokens);
    }

    private static QueryToken parseToken(String part) {
        boolean isExclude = false;

        // Strip leading "-" if present
        if (part.startsWith("-")) {
            isExclude = true;
            part = part.substring(1);
        }

        if (part.isEmpty()) return null;

        // Detect prefix and extract value
        TokenType type;
        String value;

        if (part.startsWith("#")) {
            type = TokenType.TAG;
            value = part.substring(1);
        } else if (part.startsWith("@")) {
            type = TokenType.MOD;
            value = part.substring(1);
        } else if (part.startsWith("&")) {
            type = TokenType.ENV;
            value = part.substring(1);
        } else if (part.startsWith("?")) {
            type = TokenType.PROP;
            value = part.substring(1);
        } else if (part.startsWith("!")) {
            type = TokenType.ESSENTIAL;
            value = part.substring(1);
        } else if (part.startsWith(">") || part.startsWith("<") || part.startsWith("=")) {
            type = TokenType.ESM;
            value = part;
        } else {
            type = TokenType.INCLUDE;
            value = part;
        }

        if (value.isEmpty()) return null;

        if (isExclude) {
            value = switch (type) {
                case TAG -> "#" + value;
                case MOD -> "@" + value;
                case ENV -> "&" + value;
                case PROP -> "?" + value;
                case ESSENTIAL -> "!" + value;
                default -> value;
            };
            type = TokenType.EXCLUDE;
        }

        return new QueryToken(type, value);
    }

    /**
     * Tokenize by spaces, but respect double-quoted strings.
     * "iron chest" becomes one token.
     */
    private static List<String> tokenizeWithQuotes(String text) {
        List<String> result = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        boolean inQuotes = false;

        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);

            if (c == '"') {
                inQuotes = !inQuotes;
            } else if (c == ' ' && !inQuotes) {
                if (current.length() > 0) {
                    result.add(current.toString());
                    current = new StringBuilder();
                }
            } else {
                current.append(c);
            }
        }

        if (current.length() > 0) {
            result.add(current.toString());
        }

        return result;
    }
}
