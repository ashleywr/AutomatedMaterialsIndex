package dev.emi.emi.api.widget;

public record Bounds(int x, int y, int width, int height) {
    public static final Bounds EMPTY = new Bounds(0, 0, 0, 0);

    public int left() { return x; }
    public int right() { return x + width; }
    public int top() { return y; }
    public int bottom() { return y + height; }

    public boolean contains(int px, int py) {
        return px >= x && px < x + width && py >= y && py < y + height;
    }

    public boolean empty() { return width <= 0 || height <= 0; }
}
