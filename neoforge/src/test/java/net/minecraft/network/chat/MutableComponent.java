package net.minecraft.network.chat;

public abstract class MutableComponent implements Component {
    public abstract MutableComponent copy();

    public abstract MutableComponent withStyle(java.util.function.UnaryOperator<Style> style);

    public MutableComponent withStyle(net.minecraft.ChatFormatting... formats) {
        return this;
    }

    public MutableComponent withStyle(net.minecraft.ChatFormatting format) {
        return this;
    }

    public MutableComponent withStyle(Style style) {
        return this;
    }
}
