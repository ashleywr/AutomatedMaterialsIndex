package com.sanhiruzu.ami.compat;

class JeiSearchSyncBridge {
    static final int MAX_SYNC_SEARCH_TEXT_LENGTH = 64;

    static boolean isAvailable() {
        return JeiRuntimeAccessor.withRuntime(runtime -> runtime.getIngredientFilter() != null, false);
    }

    static String getSearchText() {
        return JeiRuntimeAccessor.withRuntime(runtime -> runtime.getIngredientFilter().getFilterText(), "");
    }

    static void setSearchText(String text) {
        String safeText = sanitizeSearchText(text);
        try {
            JeiRuntimeAccessor.withRuntime(runtime -> runtime.getIngredientFilter().setFilterText(safeText));
        } catch (StackOverflowError ignored) {
            // JEI 19.x can re-enter its search callbacks for long external updates.
        }
    }

    static String sanitizeSearchText(String text) {
        if (text == null) {
            return "";
        }
        if (text.length() <= MAX_SYNC_SEARCH_TEXT_LENGTH) {
            return text;
        }
        return text.substring(0, MAX_SYNC_SEARCH_TEXT_LENGTH);
    }
}
