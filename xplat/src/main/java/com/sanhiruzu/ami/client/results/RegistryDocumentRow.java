package com.sanhiruzu.ami.client.results;

import com.sanhiruzu.ami.index.RegistryDocument;

import java.util.Objects;

public record RegistryDocumentRow(
        RegistryDocument document,
        String title,
        String subtitleLine,
        String sourceMod
) {
    public RegistryDocumentRow {
        Objects.requireNonNull(document, "document");
        title = title == null ? "" : title;
        subtitleLine = subtitleLine == null ? "" : subtitleLine;
        sourceMod = sourceMod == null ? "" : sourceMod;
    }
}
