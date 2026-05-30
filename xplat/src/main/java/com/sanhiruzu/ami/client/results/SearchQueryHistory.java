package com.sanhiruzu.ami.client.results;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;

public final class SearchQueryHistory {
    private static final int MAX_SIZE = 50;

    private final ArrayDeque<String> entries = new ArrayDeque<>();
    private int position = -1; // -1 = at live buffer
    private String liveBuffer = "";

    public void submit(String query) {
        if (query == null || query.isBlank()) return;
        entries.remove(query);
        entries.addFirst(query);
        while (entries.size() > MAX_SIZE) entries.removeLast();
        resetNavigation();
    }

    public String navigateUp(String current) {
        if (position == -1) liveBuffer = current;
        List<String> list = asList();
        int next = position + 1;
        if (next < list.size()) {
            position = next;
            return list.get(position);
        }
        return current;
    }

    public String navigateDown(String current) {
        if (position <= 0) {
            resetNavigation();
            return liveBuffer;
        }
        List<String> list = asList();
        position--;
        return list.get(position);
    }

    public void resetNavigation() {
        position = -1;
        liveBuffer = "";
    }

    public boolean isNavigating() {
        return position != -1;
    }

    private List<String> asList() {
        return new ArrayList<>(entries);
    }
}
