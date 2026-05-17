package com.sanhiruzu.ami.client.results;

import com.sanhiruzu.ami.index.SearchNode;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.List;

public class TreeNode {
    private final String key;      // stable dedup ID for group nodes; null for leaves
    private final Component label;
    private final SearchNode entry; // null for group nodes
    private final List<TreeNode> children = new ArrayList<>();
    private boolean expanded = false;
    private boolean modGroup = false; // true when this group represents a mod namespace
    private boolean highCardinality = false; // true for "collapsed" groups like enchanted books
    private int itemCountOverride = -1;

    /** Group node constructor. key is a stable ID used for deduplication. */
    public TreeNode(String key, Component label) {
        this.key = key;
        this.label = label;
        this.entry = null;
    }

    /** Leaf node constructor. */
    public TreeNode(Component label, SearchNode entry) {
        this.key = null;
        this.label = label;
        this.entry = entry;
    }

    public void addChild(TreeNode child) {
        children.add(child);
    }

    public String getKey() { return key; }
    public Component getLabel() { return label; }
    public SearchNode getEntry() { return entry; }
    public List<TreeNode> getChildren() { return children; }
    public boolean isLeaf() { return entry != null; }
    public boolean isExpanded() { return expanded; }
    public void setExpanded(boolean expanded) { this.expanded = expanded; }
    public int getChildCount() { return children.size(); }
    public boolean isModGroup() { return modGroup; }
    public void setModGroup(boolean modGroup) { this.modGroup = modGroup; }
    public boolean isHighCardinality() { return highCardinality; }
    public void setHighCardinality(boolean highCardinality) { this.highCardinality = highCardinality; }
    public int getItemCountOverride() { return itemCountOverride; }
    public void setItemCountOverride(int itemCountOverride) { this.itemCountOverride = itemCountOverride; }
}
