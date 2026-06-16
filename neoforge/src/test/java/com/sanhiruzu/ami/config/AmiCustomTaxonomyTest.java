package com.sanhiruzu.ami.config;

import com.sanhiruzu.ami.index.AmiOntology;
import com.sanhiruzu.ami.index.NodeType;
import com.sanhiruzu.ami.index.SearchNode;
import com.sanhiruzu.ami.index.SearchNodeKeys;
import net.minecraft.resources.Identifier;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AmiCustomTaxonomyTest {
    @TempDir
    Path tempDir;

    @Test
    void readsCustomCategoryDefinitionsAndRules() throws Exception {
        Path file = tempDir.resolve("taxonomy.json");
        Files.writeString(file, """
                {
                  "replaceDefaults": true,
                  "categories": {
                    "automation": {
                      "label": "Automation",
                      "subcategories": {
                        "diagnostics": "Diagnostics"
                      }
                    }
                  },
                  "rules": [
                    {
                      "match": {
                        "mods": ["example"],
                        "pathContains": ["scanner"]
                      },
                      "category": "automation",
                      "subcategory": "diagnostics",
                      "metadata": {
                        "collapseLabel": "Scanners"
                      }
                    }
                  ]
                }
                """, StandardCharsets.UTF_8);

        AmiCustomTaxonomy.ParsedProfile profile = AmiCustomTaxonomy.read(file, "test");

        assertTrue(profile.replaceDefaults());
        assertTrue(profile.categories().containsKey("automation"));
        assertEquals("Automation", profile.categories().get("automation").label());
        assertEquals("Diagnostics", profile.categories().get("automation").subcategories().get("diagnostics").label());
        assertEquals(1, profile.rules().size());
        assertEquals("automation", profile.rules().get(0).metadata().get(SearchNodeKeys.ONTOLOGY_CATEGORY));
        assertEquals("diagnostics", profile.rules().get(0).metadata().get(SearchNodeKeys.ONTOLOGY_SUBCATEGORY));
        assertEquals("Scanners", profile.rules().get(0).metadata().get(SearchNodeKeys.COLLAPSE_LABEL));
    }

    @Test
    void customSubcategoryLabelsFlowThroughOntologyRecords() {
        AmiCustomTaxonomy.SubcategoryDefinition subcategory = new AmiCustomTaxonomy.SubcategoryDefinition(
                "diagnostics",
                "Diagnostics"
        );

        AmiOntology.SubCategory ontologySubcategory = subcategory.toOntologySubcategory();

        assertEquals("Diagnostics", ontologySubcategory.displayName().getString());
    }

    @Test
    void ruleMatcherCanTargetModAndTags() {
        SearchNode node = new SearchNode(
                Identifier.parse("example:scanner"),
                NodeType.ITEM,
                "Scanner",
                0,
                0,
                Map.of(
                        SearchNodeKeys.MOD_ID, "example",
                        SearchNodeKeys.TAGS, "c:gadgets,minecraft:tools"
                )
        );

        AmiCustomTaxonomy.RuleMatcher matcher = new AmiCustomTaxonomy.RuleMatcher(
                NodeType.ITEM,
                java.util.Set.of(),
                java.util.Set.of(),
                java.util.Set.of("example"),
                java.util.Set.of(),
                java.util.Set.of(),
                java.util.Set.of(),
                java.util.Set.of("scan"),
                java.util.Set.of(),
                java.util.Set.of("c:gadgets"),
                java.util.Set.of(),
                Map.of()
        );

        assertTrue(matcher.matches(node, node.metadata()));
    }
}
