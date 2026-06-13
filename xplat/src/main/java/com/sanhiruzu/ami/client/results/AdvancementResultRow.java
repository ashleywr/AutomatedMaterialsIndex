package com.sanhiruzu.ami.client.results;

import com.sanhiruzu.ami.api.AmiAdvancementDocument;

import java.util.List;

/**
 * UI-ready row data for an advancement hit.
 */
public record AdvancementResultRow(
        AmiAdvancementDocument document,
        String title,
        String sourceLine,
        String provenanceLine,
        List<MatchEvidence> evidence
) {
    public AdvancementResultRow {
        title = title == null ? "" : title;
        sourceLine = sourceLine == null ? "" : sourceLine;
        provenanceLine = provenanceLine == null ? "" : provenanceLine;
        evidence = evidence == null ? List.of() : List.copyOf(evidence);
    }
}
