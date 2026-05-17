package net.minecraft.client.gui.components;

import net.minecraft.network.chat.Component;

public class EditBox extends AbstractWidget {
    public EditBox(net.minecraft.client.gui.Font f, int x, int y, int w, int h, Component c) { super(x,y,w,h,c); }
    public void setValue(String s) {}
    public void setResponder(java.util.function.Consumer<String> r) {}
}
