package com.sanhiruzu.ami.client.results;

import com.sanhiruzu.ami.index.SearchNode;

import java.util.ArrayList;
import java.util.List;

public class TreeNode {
    private final String label;
    private final SearchNode entry; // null for group nodes
    private final List<TreeNode> children = new ArrayList<>();
    private boolean expanded = false;

    public TreeNode(String label) {
        this.label = label;
        this.entry = null;
    }

    public TreeNode(String label, SearchNode entry) {
        this.label = label;
        this.entry = entry;
    }

    public void addChild(TreeNode child) {
        children.add(child);
    }

    public String getLabel() { return label; }
    public SearchNode getEntry() { return entry; }
    public List<TreeNode> getChildren() { return children; }
    public boolean isLeaf() { return entry != null; }
    public boolean isExpanded() { return expanded; }
    public void setExpanded(boolean expanded) { this.expanded = expanded; }
    public int getChildCount() { return children.size(); }
}
