package com.sanhiruzu.ami.index;

import java.util.Set;

/** Per-mod path-token rule: if an item from {@code modId} has any of {@code pathTokens}, route to category/subcategory. */
public record ModPatternRule(String modId, Set<String> pathTokens, String category, String subcategory) {
}
