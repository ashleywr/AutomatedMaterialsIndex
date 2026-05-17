package com.sanhiruzu.ami.compat;

class JeiSearchSyncBridge {
    // JEI 19.27 does not expose search text sync through the public API
    // This can be enhanced if JEI adds support in future versions
    static String getSearchText() {
        return "";
    }

    static void setSearchText(String text) {
        // Not supported in JEI 19.27 public API
    }
}
