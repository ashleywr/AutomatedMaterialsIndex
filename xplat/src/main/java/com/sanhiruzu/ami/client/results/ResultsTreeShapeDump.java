package com.sanhiruzu.ami.client.results;

import com.sanhiruzu.ami.index.SearchNode;

import java.lang.reflect.Method;
import java.util.List;
import java.util.stream.Collectors;

public final class ResultsTreeShapeDump {
    private ResultsTreeShapeDump() {
    }

    public static String dumpTree(List<TreeNode> nodes) {
        StringBuilder out = new StringBuilder();
        for (TreeNode node : nodes) {
            appendTree(out, node, 0);
        }
        return out.toString();
    }

    public static String dumpGrid(List<TreeNode> roots, int cols) {
        try {
            ItemGridView gridView = new ItemGridView(0, 0, 200, 200);
            gridView.setRootNodes(roots);

            Method buildVirtualRows = ItemGridView.class.getDeclaredMethod("buildVirtualRows", int.class);
            buildVirtualRows.setAccessible(true);
            @SuppressWarnings("unchecked")
            List<Object> rows = (List<Object>) buildVirtualRows.invoke(gridView, cols);

            StringBuilder out = new StringBuilder();
            out.append("Grid cols=").append(cols).append('\n');
            for (Object row : rows) {
                if ("HeaderRow".equals(row.getClass().getSimpleName())) {
                    TreeNode node = (TreeNode) invoke(row, "node");
                    int depth = (int) invoke(row, "depth");
                    int itemCount = (int) invoke(row, "itemCount");
                    out.append("  ".repeat(depth))
                            .append("Header: ")
                            .append(label(node))
                            .append(" (").append(itemCount).append(")")
                            .append(node.isExpanded() ? " [expanded]" : " [collapsed]");
                    if (node.isHighCardinality()) {
                        out.append(" [cardinality]");
                    }
                    out.append('\n');
                } else {
                    @SuppressWarnings("unchecked")
                    List<TreeNode> items = (List<TreeNode>) invoke(row, "items");
                    out.append("Row: ")
                            .append(items.stream()
                                    .map(ResultsTreeShapeDump::label)
                                    .collect(Collectors.joining(" | ")))
                            .append('\n');
                }
            }
            return out.toString();
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException("Unable to dump ItemGridView rows", e);
        }
    }

    private static void appendTree(StringBuilder out, TreeNode node, int depth) {
        out.append("  ".repeat(depth));
        out.append(label(node));
        if (!node.isLeaf()) {
            out.append(node.isExpanded() ? " [expanded]" : " [collapsed]");
            if (node.isHighCardinality()) {
                out.append(" [cardinality]");
            }
        }
        out.append('\n');

        for (TreeNode child : node.getChildren()) {
            appendTree(out, child, depth + 1);
        }
    }

    private static Object invoke(Object target, String methodName) throws ReflectiveOperationException {
        Method method = target.getClass().getDeclaredMethod(methodName);
        method.setAccessible(true);
        return method.invoke(target);
    }

    private static String label(TreeNode node) {
        String label = label(node.getLabel().getString());
        SearchNode entry = node.getEntry();
        if (entry != null) {
            return label + " [" + entry.id() + "]";
        }
        if (node.getKey() != null) {
            return label + " <" + node.getKey() + ">";
        }
        return label;
    }

    private static String label(String raw) {
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
