package com.sanhiruzu.ami.client.results;

import java.lang.reflect.Method;
import java.util.List;
import java.util.stream.Collectors;

final class ResultsGridDump {
    private ResultsGridDump() {
    }

    static String dump(List<TreeNode> roots, int cols) {
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
                            .append(ResultsDumpLabels.label(node))
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
                                    .map(item -> item.getLabel().getString())
                                    .map(ResultsDumpLabels::label)
                                    .collect(Collectors.joining(" | ")))
                            .append('\n');
                }
            }
            return out.toString();
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException("Unable to dump ItemGridView rows", e);
        }
    }

    private static Object invoke(Object target, String methodName) throws ReflectiveOperationException {
        Method method = target.getClass().getDeclaredMethod(methodName);
        method.setAccessible(true);
        return method.invoke(target);
    }
}
