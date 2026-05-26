package com.sanhiruzu.ami.compat;

import dev.emi.emi.api.EmiApi;

class EmiSearchSyncBridge {
    static boolean isAvailable() {
        return true;
    }

    static String getSearchText() {
        return EmiApi.getSearchText();
    }

    static void setSearchText(String text) {
        EmiApi.setSearchText(text);
    }
}
