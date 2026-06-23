package com.sanhiruzu.ami.client.sources;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertTrue;

class ItemSourceOverlayRouteContractTest {
    @Test
    void pastedSourcesRoutesBypassSearchDebounceOnEveryLoader() throws Exception {
        for (Path manager : overlayManagers()) {
            String source = Files.readString(manager);
            int trigger = source.indexOf("private void triggerSearch(String query)");
            assertTrue(trigger >= 0, manager + " should own search-bar query dispatch.");
            int immediateApply = source.indexOf("ItemSourceQuery.isRoute(query)", trigger);
            int pendingAssignment = source.indexOf("pendingSearchQuery = query", trigger);
            assertTrue(immediateApply > trigger && immediateApply < pendingAssignment,
                    manager + " should apply pasted source routes immediately instead of debouncing them as normal search text.");
        }
    }

    private static List<Path> overlayManagers() {
        return List.of(
                Path.of("../neoforge/src/main/java/com/sanhiruzu/ami/client/overlay/OverlayWidgetManager.java"),
                Path.of("../forge/src/main/java/com/sanhiruzu/ami/client/overlay/OverlayWidgetManager.java"),
                Path.of("../fabric/src/main/java/com/sanhiruzu/ami/client/overlay/OverlayWidgetManager.java")
        );
    }
}
