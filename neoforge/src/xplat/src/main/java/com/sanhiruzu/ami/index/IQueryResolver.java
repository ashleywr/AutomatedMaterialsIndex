package com.sanhiruzu.ami.index;

import java.util.List;
import java.util.Map;

/**
 * A pluggable resolver contributing results to a search query.
 * Each resolver is called with the raw query string and returns
 * a map of NodeType → matched SearchNodes. Results from all
 * resolvers are merged by SearchService.
 */
public interface IQueryResolver {
    /**
     * @param query the user's raw search string (already trimmed, never null/empty)
     * @return results grouped by NodeType; may be empty but never null
     */
    Map<NodeType, List<SearchNode>> resolve(String query);
}
