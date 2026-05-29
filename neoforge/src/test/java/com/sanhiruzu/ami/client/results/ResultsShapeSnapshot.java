package com.sanhiruzu.ami.client.results;

import java.util.List;

record ResultsShapeSnapshot(
        ResultsProcessor.GroupBy groupBy,
        ResultsProcessor.SortField sortField,
        List<String> warnings,
        String treeDump,
        String gridDump,
        List<String> compactWarnings,
        String compactTreeDump,
        String compactGridDump
) {
    static ResultsShapeSnapshot capture(ResultsProcessor.GroupBy groupBy,
                                        ResultsProcessor.SortField sortField,
                                        List<TreeNode> tree,
                                        int gridColumns) {
        return capture(groupBy, sortField, tree, List.of(), gridColumns);
    }

    static ResultsShapeSnapshot capture(ResultsProcessor.GroupBy groupBy,
                                        ResultsProcessor.SortField sortField,
                                        List<TreeNode> tree,
                                        List<TreeNode> compact,
                                        int gridColumns) {
        return new ResultsShapeSnapshot(
                groupBy,
                sortField,
                ResultsShapeDiagnostics.warnings(tree),
                ResultsTreeDump.dump(tree),
                ResultsGridDump.dump(tree, gridColumns),
                ResultsShapeDiagnostics.warnings(compact),
                ResultsTreeDump.dump(compact),
                ResultsGridDump.dump(compact, gridColumns)
        );
    }

    String toMarkdown() {
        StringBuilder report = new StringBuilder();
        report.append("## group=").append(groupBy.name())
                .append(" sort=").append(sortField.name())
                .append("\n\n");

        if (warnings.isEmpty()) {
            report.append("Warnings: none\n\n");
        } else {
            report.append("Warnings:\n");
            for (String warning : warnings) {
                report.append("- ").append(warning).append('\n');
            }
            report.append('\n');
        }

        report.append("Tree:\n\n```text\n")
                .append(treeDump)
                .append("```\n\n");
        report.append("Grid:\n\n```text\n")
                .append(gridDump)
                .append("```\n\n");
        report.append("Compact:\n\n");
        if (compactWarnings.isEmpty()) {
            report.append("Warnings: none\n\n");
        } else {
            report.append("Warnings:\n");
            for (String warning : compactWarnings) {
                report.append("- ").append(warning).append('\n');
            }
            report.append('\n');
        }
        report.append("```text\n")
                .append(compactTreeDump)
                .append("```\n\n");
        report.append("Compact Grid:\n\n```text\n")
                .append(compactGridDump)
                .append("```\n\n");
        return report.toString();
    }
}
