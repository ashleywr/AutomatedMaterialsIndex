package com.sanhiruzu.ami.client.results;

import com.sanhiruzu.ami.api.AmiGuideDocument;

import java.util.List;

/**
 * UI-ready row data for a guide document hit.
 */
public record GuideResultRow(
        AmiGuideDocument document,
        String title,
        String sourceLine,
        String provenanceLine,
        int referencedItemCount,
        List<MatchEvidence> evidence
) {
    public GuideResultRow {
        title = title == null ? "" : title;
        sourceLine = sourceLine == null ? "" : sourceLine;
        provenanceLine = provenanceLine == null ? "" : provenanceLine;
        evidence = evidence == null ? List.of() : List.copyOf(evidence);
    }
}
