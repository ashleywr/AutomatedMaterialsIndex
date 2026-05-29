package com.sanhiruzu.ami.client.results;

final class ResultsDumpLabels {
    private ResultsDumpLabels() {
    }

    static String label(TreeNode node) {
        return label(node.getLabel().getString());
    }

    static String label(String raw) {
        if (raw.startsWith("ami.category.")) {
            return title(raw.substring("ami.category.".length()));
        }
        if (raw.startsWith("ami.subcategory.")) {
            if ("ami.subcategory.ingredients.dyes".equals(raw)) return "Dyes & Pigments";
            if ("ami.subcategory.food.proteins".equals(raw)) return "Raw Proteins";
            if ("ami.subcategory.nature.fungi".equals(raw)) return "Fungi & Forage";
            if ("ami.subcategory.nature.flora".equals(raw)) return "Flora & Foliage";
            if ("ami.subcategory.nature.wood".equals(raw)) return "Wood & Logs";
            int lastDot = raw.lastIndexOf('.');
            return title(lastDot >= 0 ? raw.substring(lastDot + 1) : raw);
        }
        if (raw.startsWith("ami.group.unknown_")) {
            return "Unknown " + title(raw.substring("ami.group.unknown_".length()));
        }
        return switch (raw) {
            case "ami.gui.items" -> "Items";
            case "ami.group.misc" -> "Misc";
            case "ami.group.unknown_material" -> "Unknown Material";
            case "ami.group.unknown_family" -> "Unknown Family";
            default -> raw;
        };
    }

    private static String title(String value) {
        String[] parts = value.replace('_', ' ').split("\\s+");
        StringBuilder out = new StringBuilder(value.length());
        for (String part : parts) {
            if (part.isEmpty()) continue;
            if (out.length() > 0) out.append(' ');
            out.append(Character.toUpperCase(part.charAt(0)));
            if (part.length() > 1) out.append(part.substring(1));
        }
        return out.toString();
    }
}
