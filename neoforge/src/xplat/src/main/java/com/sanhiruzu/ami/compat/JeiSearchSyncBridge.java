package com.sanhiruzu.ami.compat;

class JeiSearchSyncBridge {
    static boolean isAvailable() {
        return JeiRuntimeAccessor.withRuntime(runtime -> runtime.getIngredientFilter() != null, false);
    }

    static String getSearchText() {
        return JeiRuntimeAccessor.withRuntime(runtime -> runtime.getIngredientFilter().getFilterText(), "");
    }

    static void setSearchText(String text) {
        JeiRuntimeAccessor.withRuntime(runtime -> runtime.getIngredientFilter().setFilterText(text));
    }
}
