package com.sanhiruzu.ami.index;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

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
            if ((Files.exists(current.resolve("settings.gradle")) && Files.exists(current.resolve("gradle.properties")))) {
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

    @Test
    void majorCompatCategoriesExistAsTopLevelBuckets() {
        Map<String, AmiTaxonomyCatalog.CategoryEntry> categories = AmiTaxonomyCatalog.categories().stream()
                .collect(Collectors.toMap(AmiTaxonomyCatalog.CategoryEntry::id, category -> category));

        for (String id : Set.of(
                "cobblemon", "create", "ae2", "mekanism", "gregtech", "minecolonies",
                "apotheosis", "botania", "sophisticated", "mapping")) {
            assertTrue(categories.containsKey(id), "Missing compat category: " + id);
            assertFalse(categories.get(id).subcategories().isEmpty(), "Missing subcategories for " + id);
        }

        assertTrue(hasSubcategory(categories, "environment", "waypoints"));
        assertFalse(hasSubcategory(categories, "mapping", "waypoints"));
        assertTrue(hasSubcategory(categories, "mapping", "claims"));
        assertTrue(hasSubcategory(categories, "create", "kinetics"));
        assertTrue(hasSubcategory(categories, "ae2", "storage"));
        assertTrue(hasSubcategory(categories, "mekanism", "chemicals"));
        assertTrue(hasSubcategory(categories, "gregtech", "multiblocks"));
        assertTrue(hasSubcategory(categories, "sophisticated", "backpacks"));
    }

    private static boolean hasSubcategory(Map<String, AmiTaxonomyCatalog.CategoryEntry> categories,
                                          String categoryId,
                                          String subcategoryId) {
        AmiTaxonomyCatalog.CategoryEntry category = categories.get(categoryId);
        return category != null && category.subcategories().stream()
                .anyMatch(subcategory -> subcategory.id().equals(subcategoryId));
    }
}
