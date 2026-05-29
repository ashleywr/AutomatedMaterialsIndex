package com.sanhiruzu.ami.client.results;

final class ResultsGroupLabels {
    private ResultsGroupLabels() {
    }

    static String formatGroupKey(String value, boolean compactResourceIds) {
        String key = value;
        if (compactResourceIds) {
            int sep = key.indexOf(':');
            if (sep >= 0 && sep + 1 < key.length()) key = key.substring(sep + 1);
        }
        return key.replace('_', ' ').trim();
    }

    static String formatGroupLabel(String key) {
        String[] words = key.split("\\s+");
        StringBuilder out = new StringBuilder(key.length());
        for (String word : words) {
            if (word.isEmpty()) continue;
            if (out.length() > 0) out.append(' ');
            out.append(Character.toUpperCase(word.charAt(0)));
            if (word.length() > 1) out.append(word.substring(1));
        }
        return out.toString();
    }

    static String colorGroupLabel(String materialGroup) {
        int sep = materialGroup.indexOf(':');
        if (sep < 0) return formatGroupLabel(materialGroup.replace('_', ' '));
        String namespace = materialGroup.substring(0, sep);
        String path = materialGroup.substring(sep + 1);
        String base = formatGroupLabel(path.replace('_', ' '));
        if ("minecraft".equals(namespace)) return base;
        String modLabel = com.sanhiruzu.ami.index.providers.RegistryUtils.modDisplayName(namespace);
        return base + " — " + modLabel;
    }
}
