package net.minecraft.network.chat;

public class MutableComponent extends Component {
    public MutableComponent(String s) { super(s); }
    public MutableComponent copy() { return new MutableComponent(this.key); }
}
