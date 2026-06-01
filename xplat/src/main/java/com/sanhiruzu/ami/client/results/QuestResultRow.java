package com.sanhiruzu.ami.client.results;

import com.sanhiruzu.ami.api.AmiQuestDocument;

import java.util.List;

/**
 * UI-ready row data for a quest document hit.
 */
public record QuestResultRow(
        AmiQuestDocument document,
        String title,
        String sourceLine,
        String provenanceLine,
        int requirementCount,
        int rewardCount,
        List<MatchEvidence> evidence
) {
    public QuestResultRow {
        title = title == null ? "" : title;
        sourceLine = sourceLine == null ? "" : sourceLine;
        provenanceLine = provenanceLine == null ? "" : provenanceLine;
        evidence = evidence == null ? List.of() : List.copyOf(evidence);
    }
}
