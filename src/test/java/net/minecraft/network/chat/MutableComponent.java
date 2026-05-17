package net.minecraft.network.chat;

public interface MutableComponent extends Component {
    MutableComponent copy();
    MutableComponent withStyle(java.util.function.UnaryOperator<Style> style);
}
