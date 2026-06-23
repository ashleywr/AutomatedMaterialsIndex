package com.sanhiruzu.ami.client.entitydetails;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertTrue;

class EntityDetailsPanelRouteContractTest {
    @Test
    void entityDetailsRoutesAreHandledBeforeNormalResultProjection() throws Exception {
        String source = panelSource();
        int refreshTree = source.indexOf("private void refreshTree(boolean incrementalUpdate)");
        int routeOpen = source.indexOf("openEntityDetailsRoute(state.getQuery())", refreshTree);
        int projectResults = source.indexOf("projectResults()", refreshTree);

        assertTrue(refreshTree >= 0, "UniversalResultsPanel should own refreshTree route dispatch.");
        assertTrue(routeOpen > refreshTree && routeOpen < projectResults,
                "Entity detail routes should open the dedicated view before normal result projection.");
    }

    @Test
    void sourceAndEntityDetailRoutesCloseEachOther() throws Exception {
        String source = panelSource();
        int openSourceRoute = source.indexOf("private boolean openSourceRoute(String query)");
        int closeEntityDetails = source.indexOf("closeEntityDetailsView()", openSourceRoute);
        int sourceReport = source.indexOf("activeSourceReport =", openSourceRoute);
        int openEntityDetailsRoute = source.indexOf("private boolean openEntityDetailsRoute(String query)");
        int closeSource = source.indexOf("closeSourceView()", openEntityDetailsRoute);
        int entityReport = source.indexOf("activeEntityDetailsReport =", openEntityDetailsRoute);

        assertTrue(closeEntityDetails > openSourceRoute && closeEntityDetails < sourceReport,
                "Opening a Sources route should leave any Mob Info view before building the source report.");
        assertTrue(closeSource > openEntityDetailsRoute && closeSource < entityReport,
                "Opening a Mob Info route should leave any Sources view before building the entity report.");
    }

    @Test
    void entityDetailsMouseHandlingBypassesNormalResultSelection() throws Exception {
        String source = panelSource();
        int mouseClicked = source.indexOf("public boolean mouseClicked(double mouseX, double mouseY, int button)");
        int entityDetailsBranch = source.indexOf("activeEntityDetailsReport != null", mouseClicked);
        int pressedNodeReset = source.indexOf("pressedNode = null", mouseClicked);

        assertTrue(mouseClicked >= 0, "UniversalResultsPanel should own mouse click dispatch.");
        assertTrue(entityDetailsBranch > mouseClicked && entityDetailsBranch < pressedNodeReset,
                "Clicks in the Mob Info view should dispatch to the dedicated list before normal result selection.");
    }

    private static String panelSource() throws Exception {
        return Files.readString(Path.of("../xplat/src/main/java/com/sanhiruzu/ami/client/UniversalResultsPanel.java"));
    }
}
