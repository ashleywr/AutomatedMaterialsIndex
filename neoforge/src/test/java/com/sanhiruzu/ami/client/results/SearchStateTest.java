package com.sanhiruzu.ami.client.results;

import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;

class SearchStateTest {
    @Test
    void setQueryDoesNotNotifyWhenQueryIsUnchanged() {
        SearchState state = new SearchState();
        AtomicInteger notifications = new AtomicInteger();
        state.addListener(ignored -> notifications.incrementAndGet());

        state.setQuery("");
        state.setQuery(null);

        assertEquals(0, notifications.get());

        state.setQuery("ingot");
        state.setQuery("ingot");

        assertEquals(1, notifications.get());

        state.setQuery(null);

        assertEquals(2, notifications.get());
        assertEquals("", state.getQuery());
    }
}
