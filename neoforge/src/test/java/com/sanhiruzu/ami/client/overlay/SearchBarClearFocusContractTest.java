package com.sanhiruzu.ami.client.overlay;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertTrue;

class SearchBarClearFocusContractTest {
    @Test
    void clearAndFocusDismissesSuggestionsAfterRefocusing() throws Exception {
        String source = Files.readString(Path.of("../xplat/src/main/java/com/sanhiruzu/ami/client/overlay/AbstractSearchBarWidget.java"));
        int clearAndFocus = source.indexOf("public void clearAndFocus()");
        int nextMethod = source.indexOf("public void toggleToken", clearAndFocus);

        assertTrue(clearAndFocus >= 0 && nextMethod > clearAndFocus,
                "Expected to locate clearAndFocus() in AbstractSearchBarWidget.");

        String body = source.substring(clearAndFocus, nextMethod);
        int focusForInput = body.indexOf("focusForInput();");
        int dismissSuggestions = body.indexOf("dismissSuggestionsPopup();");

        assertTrue(focusForInput >= 0, "clearAndFocus() should keep the search bar focused.");
        assertTrue(dismissSuggestions > focusForInput,
                "clearAndFocus() should dismiss the suggestion popup after refocusing so Up recalls query history instead of stale empty-query suggestions.");
    }
}
