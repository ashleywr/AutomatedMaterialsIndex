package net.minecraft.client.gui.components;

import net.minecraft.network.chat.Component;

public class Button extends AbstractWidget {
    public Button(int x, int y, int w, int h, Component c) { super(x,y,w,h,c); }
    public static Builder builder(Component c, OnPress p) { return new Builder(c, p); }
    public void setMessage(Component c) {}

    public interface OnPress { void onPress(Button b); }
    public static class Builder {
        public Builder(Component c, OnPress p) {}
        public Builder bounds(int x, int y, int w, int h) { return this; }
        public Button build() { return new Button(0,0,0,0, null); }
    }
}
