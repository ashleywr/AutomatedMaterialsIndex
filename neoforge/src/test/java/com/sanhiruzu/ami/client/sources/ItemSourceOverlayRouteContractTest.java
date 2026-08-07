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
            int immediateEntityApply = source.indexOf("EntityDetailsQuery.isRoute(query)", trigger);
            int pendingAssignment = source.indexOf("pendingSearchQuery = query", trigger);
            assertTrue(immediateApply > trigger && immediateApply < pendingAssignment,
                    manager + " should apply pasted source routes immediately instead of debouncing them as normal search text.");
            assertTrue(immediateEntityApply > trigger && immediateEntityApply < pendingAssignment,
                    manager + " should apply pasted entity detail routes immediately instead of debouncing them as normal search text.");
        }
    }

    @Test
    void sourceAndEntityRoutesDoNotSyncIntoRecipeViewerSearchOnEveryLoader() throws Exception {
        for (Path manager : overlayManagers()) {
            String source = Files.readString(manager);
            int applySearch = source.indexOf("private void applySearchQuery(String query)");
            assertTrue(applySearch >= 0, manager + " should own recipe-viewer search sync.");
            int sourceRouteGuard = source.indexOf("!ItemSourceQuery.isRoute(query)", applySearch);
            int entityRouteGuard = source.indexOf("!EntityDetailsQuery.isRoute(query)", applySearch);
            int syncAssignment = source.indexOf("lastSyncedQuery = query", applySearch);
            assertTrue(sourceRouteGuard > applySearch && sourceRouteGuard < syncAssignment,
                    manager + " should avoid syncing source route text into the recipe viewer search box.");
            assertTrue(entityRouteGuard > applySearch && entityRouteGuard < syncAssignment,
                    manager + " should avoid syncing entity route text into the recipe viewer search box.");
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
