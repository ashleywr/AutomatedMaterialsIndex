package com.sanhiruzu.ami.index;

import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.StringJoiner;

public final class SemanticVerbCodec {
    private SemanticVerbCodec() {
    }

    public static Set<SemanticVerb> read(Map<String, String> meta) {
        Set<SemanticVerb> result = new LinkedHashSet<>();
        if (meta == null) {
            return result;
        }
        String encoded = meta.getOrDefault(SearchNodeKeys.SEMANTIC_VERBS, "");
        for (String value : encoded.split(",")) {
            SemanticVerb verb = SemanticVerb.byId(value);
            if (verb != null) {
                result.add(verb);
            }
        }
        return result;
    }

    public static boolean has(Map<String, String> meta, SemanticVerb verb) {
        return verb != null && read(meta).contains(verb);
    }

    public static void add(Map<String, String> meta, SemanticVerb verb, String evidence) {
        if (meta == null || verb == null) {
            return;
        }
        LinkedHashSet<String> ids = encodedIds(meta);
        if (ids.add(verb.id())) {
            meta.put(SearchNodeKeys.SEMANTIC_VERBS, String.join(",", ids));
        }
        if (evidence != null && !evidence.isBlank()) {
            Map<String, String> evidenceMap = evidenceMap(meta);
            evidenceMap.putIfAbsent(verb.id(), sanitizeEvidence(evidence));
            writeEvidence(meta, evidenceMap);
        }
    }

    public static void remove(Map<String, String> meta, SemanticVerb verb) {
        if (meta == null || verb == null) {
            return;
        }
        LinkedHashSet<String> ids = encodedIds(meta);
        if (ids.remove(verb.id())) {
            if (ids.isEmpty()) {
                meta.remove(SearchNodeKeys.SEMANTIC_VERBS);
            } else {
                meta.put(SearchNodeKeys.SEMANTIC_VERBS, String.join(",", ids));
            }
        }
        Map<String, String> evidenceMap = evidenceMap(meta);
        if (evidenceMap.remove(verb.id()) != null) {
            writeEvidence(meta, evidenceMap);
        }
    }

    private static LinkedHashSet<String> encodedIds(Map<String, String> meta) {
        LinkedHashSet<String> ids = new LinkedHashSet<>();
        String encoded = meta.getOrDefault(SearchNodeKeys.SEMANTIC_VERBS, "");
        for (String raw : encoded.split(",")) {
            SemanticVerb verb = SemanticVerb.byId(raw);
            if (verb != null) {
                ids.add(verb.id());
            }
        }
        return ids;
    }

    private static Map<String, String> evidenceMap(Map<String, String> meta) {
        Map<String, String> out = new LinkedHashMap<>();
        String encoded = meta.getOrDefault(SearchNodeKeys.SEMANTIC_VERB_EVIDENCE, "");
        for (String part : encoded.split("\\|")) {
            int separator = part.indexOf('=');
            if (separator <= 0) {
                continue;
            }
            SemanticVerb verb = SemanticVerb.byId(part.substring(0, separator));
            if (verb != null) {
                String value = part.substring(separator + 1).trim();
                if (!value.isBlank()) {
                    out.put(verb.id(), sanitizeEvidence(value));
                }
            }
        }
        return out;
    }

    private static void writeEvidence(Map<String, String> meta, Map<String, String> evidenceMap) {
        if (evidenceMap.isEmpty()) {
            meta.remove(SearchNodeKeys.SEMANTIC_VERB_EVIDENCE);
            return;
        }
        StringJoiner joiner = new StringJoiner("|");
        for (Map.Entry<String, String> entry : evidenceMap.entrySet()) {
            joiner.add(entry.getKey() + "=" + entry.getValue());
        }
        meta.put(SearchNodeKeys.SEMANTIC_VERB_EVIDENCE, joiner.toString());
    }

    private static String sanitizeEvidence(String raw) {
        return raw.trim()
                .replace(',', ';')
                .replace('|', ';')
                .replace('=', ':');
    }
}
