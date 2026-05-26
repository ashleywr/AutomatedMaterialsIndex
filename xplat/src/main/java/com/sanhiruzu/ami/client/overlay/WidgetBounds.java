package com.sanhiruzu.ami.client.overlay;

public record WidgetBounds(int x, int y, int width, int height) {
    public boolean contains(double mx, double my) {
        return mx >= x && mx < x + width && my >= y && my < y + height;
    }
}
