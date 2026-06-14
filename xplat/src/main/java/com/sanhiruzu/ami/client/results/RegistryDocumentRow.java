package com.sanhiruzu.ami.client.results;

import com.sanhiruzu.ami.index.RegistryDocument;

public record RegistryDocumentRow(
        RegistryDocument document,
        String title,
        String subtitleLine,
        String sourceMod
) {
    public RegistryDocumentRow {
        title = title == null ? "" : title;
        subtitleLine = subtitleLine == null ? "" : subtitleLine;
        sourceMod = sourceMod == null ? "" : sourceMod;
    }
}
