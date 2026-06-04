package com.sanhiruzu.ami.index;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ClassificationArchitectureGuardrailTest {

    @Test
    void pathOnlyCableWordsStayLexicalNotConcreteFacets() throws IOException {
        String source = Files.readString(repoRoot().resolve(Path.of(
                "xplat", "src", "main", "java", "com", "sanhiruzu", "ami", "index", "FacetIndexer.java"
        )));

        String pathFacts = methodBody(source, "applyPathFacts");
        assertFalse(pathFacts.contains("ItemFacet.CABLE"),
                "Path-only cable words must stay lexical evidence, not concrete ItemFacet.CABLE facts");
    }

    @Test
    void concreteCableFacetSourcesStillExist() throws IOException {
        String source = Files.readString(repoRoot().resolve(Path.of(
                "xplat", "src", "main", "java", "com", "sanhiruzu", "ami", "index", "FacetIndexer.java"
        )));

        assertTrue(source.contains("facets.add(ItemFacet.CABLE)"),
                "Concrete cable facets should still be available from tags/classes/capabilities");
    }

    @Test
    void createCompatCableSubcategoriesRequireCableFacets() throws IOException {
        String source = Files.readString(repoRoot().resolve(Path.of(
                "xplat", "src", "main", "java", "com", "sanhiruzu", "ami", "index", "PrimaryCategoryResolver.java"
        )));

        String createTech = methodBody(source, "classifyCreateFamilyTechSubcategory");
        assertFalse(createTech.contains("isCablePath(path)"),
                "Create compat subcategory routing should use ItemFacet.CABLE, not path-only cable words");
        assertTrue(createTech.contains("facets.contains(ItemFacet.CABLE)"),
                "Create compat cable subcategory routing still needs a concrete cable facet path");
    }

    private static String methodBody(String source, String methodName) {
        String marker = " " + methodName + "(";
        int signature = source.indexOf(marker);
        while (signature >= 0 && !isMethodDeclaration(source, signature)) {
            signature = source.indexOf(marker, signature + marker.length());
        }
        if (signature < 0) {
            throw new AssertionError("Missing method " + methodName);
        }
        int open = source.indexOf('{', signature);
        if (open < 0) {
            throw new AssertionError("Missing method body for " + methodName);
        }

        int depth = 0;
        for (int i = open; i < source.length(); i++) {
            char current = source.charAt(i);
            if (current == '{') {
                depth++;
            } else if (current == '}') {
                depth--;
                if (depth == 0) {
                    return source.substring(open + 1, i);
                }
            }
        }
        throw new AssertionError("Unclosed method body for " + methodName);
    }

    private static boolean isMethodDeclaration(String source, int markerIndex) {
        int lineStart = source.lastIndexOf('\n', markerIndex);
        String prefix = source.substring(lineStart < 0 ? 0 : lineStart + 1, markerIndex);
        return prefix.contains("private ")
                || prefix.contains("protected ")
                || prefix.contains("public ")
                || prefix.contains("static ");
    }

    private static Path repoRoot() {
        Path current = Path.of("").toAbsolutePath();
        while (current != null) {
            if ((Files.exists(current.resolve("settings.gradle")) && Files.exists(current.resolve("gradle.properties")))) {
                return current;
            }
            current = current.getParent();
        }
        throw new IllegalStateException("Could not locate repository root");
    }
}
