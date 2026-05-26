package com.sanhiruzu.ami.client.results;

import com.sanhiruzu.ami.index.AmiOntology;
import com.sanhiruzu.ami.index.SearchNode;
import com.sanhiruzu.ami.index.SearchNodeKeys;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class DashboardBrowse {
    private DashboardBrowse() {
    }

    public static List<TreeNode> buildCategoryNodes(List<AmiOntology.Category> categories,
                                                    java.util.function.Function<String, List<SearchNode>> categoryLookup) {
        List<TreeNode> dashboard = new ArrayList<>();

        for (AmiOntology.Category cat : categories) {
            List<SearchNode> categoryNodes = categoryLookup.apply(cat.id);
            if (categoryNodes == null || categoryNodes.isEmpty()) {
                continue;
            }

            TreeNode catNode = new TreeNode(cat.id, cat.displayName());
            catNode.setExpanded(true);
            catNode.setItemCountOverride(categoryNodes.size());

            for (TreeNode subNode : buildSubcategoryPlaceholders(cat, categoryNodes)) {
                catNode.addChild(subNode);
            }

            dashboard.add(catNode);
        }

        return dashboard;
    }

    public static boolean isSubcategoryKey(String key) {
        return key != null && key.contains("/");
    }

    public static String[] splitSubcategoryKey(String key) {
        int slash = key == null ? -1 : key.indexOf('/');
        if (slash <= 0 || slash + 1 >= key.length()) {
            return null;
        }
        return new String[]{key.substring(0, slash), key.substring(slash + 1)};
    }

    public static List<SearchNode> filterSubcategoryNodes(List<SearchNode> categoryNodes, String subcategoryId) {
        String target = subcategoryId == null ? "" : subcategoryId;
        List<SearchNode> filtered = new ArrayList<>();
        for (SearchNode node : categoryNodes) {
            String actual = node.meta(SearchNodeKeys.ONTOLOGY_SUBCATEGORY, "");
            if (target.equals(actual)) {
                filtered.add(node);
            }
        }
        return filtered;
    }

    private static List<TreeNode> buildSubcategoryPlaceholders(AmiOntology.Category category, List<SearchNode> categoryNodes) {
        Map<String, List<SearchNode>> subMap = new LinkedHashMap<>();
        for (SearchNode node : categoryNodes) {
            String subId = node.meta(SearchNodeKeys.ONTOLOGY_SUBCATEGORY, "");
            subMap.computeIfAbsent(subId, k -> new ArrayList<>()).add(node);
        }

        List<TreeNode> children = new ArrayList<>();
        for (AmiOntology.SubCategory subCategory : category.subCategories) {
            List<SearchNode> members = subMap.remove(subCategory.id());
            if (members == null || members.isEmpty()) {
                continue;
            }

            TreeNode subNode = new TreeNode(category.id + "/" + subCategory.id(), subCategory.displayName());
            subNode.setExpanded(true);
            subNode.setItemCountOverride(members.size());
            children.add(subNode);
        }

        List<SearchNode> miscMembers = subMap.remove("");
        if (miscMembers != null && !miscMembers.isEmpty()) {
            TreeNode miscNode = new TreeNode(category.id + "/none", Component.translatable("ami.group.misc"));
            miscNode.setExpanded(true);
            miscNode.setItemCountOverride(miscMembers.size());
            children.add(miscNode);
        }

        for (var extra : subMap.entrySet()) {
            if (extra.getValue().isEmpty()) {
                continue;
            }
            TreeNode extraNode = new TreeNode(category.id + "/" + extra.getKey(),
                    Component.literal(formatSubcategoryLabel(extra.getKey())));
            extraNode.setExpanded(true);
            extraNode.setItemCountOverride(extra.getValue().size());
            children.add(extraNode);
        }

        return children;
    }

    private static String formatSubcategoryLabel(String key) {
        String[] words = key.replace('_', ' ').trim().split("\\s+");
        StringBuilder out = new StringBuilder(key.length());
        for (String word : words) {
            if (word.isEmpty()) continue;
            if (out.length() > 0) out.append(' ');
            out.append(Character.toUpperCase(word.charAt(0)));
            if (word.length() > 1) out.append(word.substring(1));
        }
        return out.toString();
    }
}
