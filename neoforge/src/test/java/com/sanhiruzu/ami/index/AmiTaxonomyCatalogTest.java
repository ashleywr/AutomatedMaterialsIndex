package com.sanhiruzu.ami.index;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AmiTaxonomyCatalogTest {
    private static void assertReadableLabel(String label, String key) {
        assertFalse(label == null || label.isBlank(), "Blank taxonomy label for " + key);
        assertFalse(label.startsWith("ami."), "Unresolved translation key for " + key + ": " + label);
        assertFalse(label.contains("Other"), "Taxonomy label should avoid visible catch-all wording for " + key + ": " + label);
    }

    private static Path repoRoot() {
        Path current = Path.of("").toAbsolutePath();
        while (current != null) {
            if (Files.exists(current.resolve("AGENTS.md"))) {
                return current;
            }
            current = current.getParent();
        }
        throw new IllegalStateException("Could not locate repository root");
    }

    @Test
    void taxonomyCatalogHasReadableUniqueEntries() {
        Set<String> categories = new HashSet<>();
        Set<String> subcategories = new HashSet<>();
        Set<String> kinds = new HashSet<>();

        for (AmiTaxonomyCatalog.CategoryEntry category : AmiTaxonomyCatalog.categories()) {
            assertTrue(categories.add(category.id()), "Duplicate category id: " + category.id());
            assertReadableLabel(category.label(), category.id());

            for (AmiTaxonomyCatalog.SubcategoryEntry subcategory : category.subcategories()) {
                String subcategoryKey = category.id() + "/" + subcategory.id();
                assertTrue(subcategories.add(subcategoryKey), "Duplicate subcategory id: " + subcategoryKey);
                assertReadableLabel(subcategory.label(), subcategoryKey);

                for (AmiTaxonomyCatalog.KindEntry kind : subcategory.kinds()) {
                    String kindKey = subcategoryKey + "/" + kind.id();
                    assertTrue(kinds.add(kindKey), "Duplicate kind id: " + kindKey);
                    assertReadableLabel(kind.label(), kindKey);
                }
            }
        }
    }

    @Test
    void writesTaxonomyCatalogReport() throws IOException {
        Path reportPath = repoRoot().resolve(Path.of("neoforge", "build", "reports", "ami-result-shapes", "taxonomy-catalog.md"));
        Files.createDirectories(reportPath.getParent());
        Files.writeString(reportPath, AmiTaxonomyCatalog.toMarkdown());
        assertTrue(Files.exists(reportPath), "Expected taxonomy report at " + reportPath.toAbsolutePath());
    }
}
