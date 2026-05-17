package com.sanhiruzu.ami.index.query;

public final class QueryUtils {
    private QueryUtils() {}

    /**
     * Toggles a token in a space-separated query string.
     * If the token exists, it is removed. If it doesn't, it is appended.
     * Handles cleaning up extra spaces.
     */
    public static String toggleToken(String current, String token) {
        if (current == null) current = "";
        if (token == null || token.isEmpty()) return current;

        String[] parts = current.trim().split("\\s+");
        java.util.List<String> tokens = new java.util.ArrayList<>(java.util.Arrays.asList(parts));
        if (tokens.size() == 1 && tokens.get(0).isEmpty()) tokens.clear();

        if (tokens.contains(token)) {
            tokens.remove(token);
        } else {
            tokens.add(token);
        }

        return String.join(" ", tokens).trim();
    }
}
