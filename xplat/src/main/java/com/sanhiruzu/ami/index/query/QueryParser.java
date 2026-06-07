package com.sanhiruzu.ami.index.query;

import java.util.ArrayList;
import java.util.List;

public final class QueryParser {
    private QueryParser() {
    }

    /**
     * Parse a raw query string into typed tokens.
     * Supports:
     * - Plain text: "creeper" → INCLUDE
     * - Broad metadata: "~ingot" → META
     * - Negation: "-creeper", "-#ore", "-~ingot" → EXCLUDE with type inherited from suffix
     * - Tags: "#storage" → TAG
     * - Environment: "&nether" → ENV
     * - Properties: "?precipitation:rain" → PROP
     * - Players: "^name" → PLAYER
     * - Essential: "!storage" → ESSENTIAL
     * - Numeric metrics: ">storage:100", ">dps:8", "<storage:4096" → ESM
     * <p>
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

        SearchSyntax.ParsedPrefix parsedPrefix = SearchSyntax.parsePrefixedToken(part)
                .orElse(new SearchSyntax.ParsedPrefix(TokenType.INCLUDE, part));
        TokenType type = parsedPrefix.type();
        String value = parsedPrefix.value();

        if (value.isEmpty()) return null;

        if (isExclude) {
            value = SearchSyntax.exclusionValue(type, value);
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

    public enum TokenType {
        INCLUDE,     // Plain text or -less
        META,        // ~text broad metadata search
        EXCLUDE,     // Negation: -word
        TAG,         // #tag
        MOD,         // @modid
        ENV,         // &env
        PROP,        // ?property
        PLAYER,      // ^player
        ESSENTIAL,   // !curated
        ESM,         // >capacity:value
        CATEGORY     // $categoryId  (AMI ontology category)
    }

    public record QueryToken(TokenType type, String value) {
    }

    public record ParsedQuery(List<QueryToken> tokens) {
        public ParsedQuery(List<QueryToken> tokens) {
            this.tokens = List.copyOf(tokens);
        }
    }
}
