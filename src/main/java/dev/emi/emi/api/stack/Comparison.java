package dev.emi.emi.api.stack;

public class Comparison {
    public static final Comparison DEFAULT_COMPARISON = new Comparison();

    public boolean compare(EmiStack a, EmiStack b) {
        return a.getKey().equals(b.getKey());
    }
}
