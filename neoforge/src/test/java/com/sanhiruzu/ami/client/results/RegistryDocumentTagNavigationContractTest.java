package com.sanhiruzu.ami.client.results;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertTrue;

class RegistryDocumentTagNavigationContractTest {
    @Test
    void clickingTagRegistryDocumentReplacesQueryWithCanonicalTagSearch() throws Exception {
        String source = Files.readString(Path.of("../xplat/src/main/java/com/sanhiruzu/ami/client/UniversalResultsPanel.java"));
        int registryClick = source.indexOf("RegistryDocumentRow registryDocumentRow = registryDocumentRowAt(mouseX, mouseY);");
        int dashboardSection = source.indexOf("// Handle Dashboard Atlas lazy loading", registryClick);

        assertTrue(registryClick >= 0 && dashboardSection > registryClick,
                "Expected to locate the registry-document click block in UniversalResultsPanel.");

        String body = source.substring(registryClick, dashboardSection);
        assertTrue(body.contains("registryDocumentRow.document().kind() == RegistryDocumentKind.TAG"),
                "Tag registry document clicks should be recognized explicitly.");
        assertTrue(body.contains("replaceQuery(\"#\" + registryDocumentRow.document().id())"),
                "Tag registry document clicks should navigate to the canonical #namespace:path query.");
    }
}
