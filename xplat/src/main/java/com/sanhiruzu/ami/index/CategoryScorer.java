package com.sanhiruzu.ami.index;

import net.minecraft.resources.Identifier;

import java.util.*;

final class CategoryScorer {
    private static final int MIN_FALLBACK_SCORE = 40;
    private static final int MIN_STRONG_SCORE = 80;

    private CategoryScorer() {
    }

    static Optional<CategoryAssignment> resolve(Identifier id, FacetProfile profile) {
        return resolve(id, profile, MIN_FALLBACK_SCORE);
    }

    static Optional<CategoryAssignment> resolveStrong(Identifier id, FacetProfile profile) {
        return resolve(id, profile, MIN_STRONG_SCORE);
    }

    private static Optional<CategoryAssignment> resolve(Identifier id, FacetProfile profile, int minScore) {
        List<ClassificationEvidence> evidence = EvidenceCollector.collect(id, profile);
        if (evidence.isEmpty()) {
            return Optional.empty();
        }

        Map<String, Score> scores = new LinkedHashMap<>();
        for (ClassificationEvidence item : evidence) {
            String key = item.categoryKey();
            scores.computeIfAbsent(key, ignored -> new Score(item.categoryId(), item.subcategoryId()))
                    .add(item);
        }

        Score best = scores.values().stream()
                .max(Comparator
                        .comparingInt(Score::total)
                        .thenComparingInt(Score::strongest)
                        .thenComparing(Score::key, Comparator.reverseOrder()))
                .orElse(null);
        if (best == null || best.total() < minScore) {
            return Optional.empty();
        }

        Map<String, String> attributes = new LinkedHashMap<>(profile.attributes());
        attributes.put("classificationMode", "evidence_scoring");
        attributes.put("classificationScore", Integer.toString(best.total()));
        attributes.put("classificationEvidence", best.reasonSummary());
        attributes.put("classificationScores", scoreSummary(scores.values()));
        return Optional.of(new CategoryAssignment(best.categoryId, best.subcategoryId, attributes));
    }

    private static String scoreSummary(Iterable<Score> scores) {
        List<Score> sorted = new ArrayList<>();
        scores.forEach(sorted::add);
        sorted.sort(Comparator.comparingInt(Score::total).reversed().thenComparing(Score::key));
        StringJoiner joiner = new StringJoiner("; ");
        for (Score score : sorted.stream().limit(6).toList()) {
            joiner.add(score.key() + "=" + score.total());
        }
        return joiner.toString();
    }

    private static final class Score {
        private final String categoryId;
        private final String subcategoryId;
        private final List<ClassificationEvidence> evidence = new ArrayList<>();
        private int total;
        private int strongest;

        private Score(String categoryId, String subcategoryId) {
            this.categoryId = categoryId;
            this.subcategoryId = subcategoryId;
        }

        private void add(ClassificationEvidence item) {
            evidence.add(item);
            total += item.weight();
            strongest = Math.max(strongest, item.weight());
        }

        private int total() {
            return total;
        }

        private int strongest() {
            return strongest;
        }

        private String key() {
            return categoryId + "/" + subcategoryId;
        }

        private String reasonSummary() {
            evidence.sort(Comparator.comparingInt(ClassificationEvidence::weight).reversed());
            StringJoiner joiner = new StringJoiner("; ");
            for (ClassificationEvidence item : evidence.stream().limit(5).toList()) {
                joiner.add("+" + item.weight() + " " + item.id() + "[" + item.reason() + "]");
            }
            return joiner.toString();
        }
    }
}
