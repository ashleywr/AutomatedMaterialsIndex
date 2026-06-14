package com.sanhiruzu.ami.client.results;

import com.sanhiruzu.ami.index.AmiRegistryDocumentIndex;
import com.sanhiruzu.ami.index.RegistryDocument;
import com.sanhiruzu.ami.index.RegistryDocumentKind;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public final class RegistryDocumentResultsProjector {
    private RegistryDocumentResultsProjector() {
    }

    public static List<RegistryDocumentRow> project(String query,
                                                     Set<RegistryDocumentKind> enabledKinds,
                                                     AmiRegistryDocumentIndex index) {
        if (index == null || query == null || query.isBlank() || enabledKinds.isEmpty()) {
            return List.of();
        }
        List<RegistryDocument> hits = index.query(query, enabledKinds);
        List<RegistryDocumentRow> rows = new ArrayList<>();
        for (RegistryDocument doc : hits) {
            rows.add(new RegistryDocumentRow(
                    doc,
                    doc.displayName(),
                    doc.description(),
                    doc.sourceMod()
            ));
        }
        return List.copyOf(rows);
    }
}
