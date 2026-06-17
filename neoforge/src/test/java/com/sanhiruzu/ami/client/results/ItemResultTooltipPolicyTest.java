package com.sanhiruzu.ami.client.results;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ItemResultTooltipPolicyTest {
    @Test
    void gridItemResultsRenderComponentAwareItemTooltip() throws Exception {
        String source = readXplat("client", "results", "ItemGridView.java");

        assertTrue(source.contains("AmiTooltipRenderer.renderResultItemTooltip"));
        assertFalse(source.contains("AmiTooltipHandler.renderWithResultTooltipContext"));
        assertTrue(source.contains("} else if (entry.type() == com.sanhiruzu.ami.index.NodeType.ITEM)"));
        assertTrue(source.contains("pendingTextTooltip = null;"));
        assertTrue(source.contains("pendingTooltipImage = Optional.empty();"));
    }

    @Test
    void treeItemResultsRenderComponentAwareItemTooltip() throws Exception {
        String source = readXplat("client", "results", "ResultsTreeView.java");

        assertTrue(source.contains("AmiTooltipRenderer.renderResultItemTooltip"));
        assertFalse(source.contains("AmiTooltipHandler.renderWithResultTooltipContext"));
        assertTrue(source.contains("if (entry.type() == NodeType.ITEM)"));
        assertTrue(source.contains("pendingTooltipLines = null;"));
        assertTrue(source.contains("pendingTooltipImage = Optional.empty();"));
    }

    @Test
    void resultTooltipHandlerDoesNotMutateItemTooltipEventForResultFooter() throws Exception {
        String source = readXplat("client", "AmiTooltipHandler.java");

        assertFalse(source.contains("RESULT_TOOLTIP_CONTEXT"));
        assertFalse(source.contains("renderWithResultTooltipContext"));
        assertFalse(source.contains("buildItemTooltipFooter(context)"));
        assertFalse(source.contains("appendModNameIfMissing(lines, context)"));
    }

    @Test
    void resultTooltipRendererKeepsTooltipComponentsAsElements() throws Exception {
        String source = readXplat("client", "tooltip", "AmiResultTooltipElements.java");
        String renderer = readXplat("client", "tooltip", "AmiTooltipRenderer.java");
        String platform = readXplat("platform", "IPlatformHelper.java");

        assertTrue(source.contains("List<Either<FormattedText, TooltipComponent>>"));
        assertTrue(source.contains("stack.getTooltipImage()"));
        assertTrue(source.contains("AmiTooltipComposer.buildItemTooltipFooter"));
        assertTrue(source.contains("AmiTooltipComposer.appendModNameIfMissing"));
        assertTrue(renderer.contains("renderResultItemTooltip"));
        assertTrue(renderer.contains("Services.PLATFORM.renderTooltipElements"));
        assertTrue(platform.contains("renderTooltipElements"));
    }

    private static String readXplat(String... path) throws Exception {
        Path root = repoRoot().resolve(Path.of("xplat", "src", "main", "java", "com", "sanhiruzu", "ami"));
        return Files.readString(root.resolve(Path.of("", path)));
    }

    private static Path repoRoot() {
        Path current = Path.of("").toAbsolutePath();
        if (Files.exists(current.resolve("settings.gradle"))) {
            return current;
        }
        return current.getParent();
    }
}
