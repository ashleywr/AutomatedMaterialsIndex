package net.minecraft.network.chat;

public interface Component {
    static MutableComponent translatable(String key) {
        return new DummyComponent(key);
    }

    static MutableComponent literal(String s) {
        return new DummyComponent(s);
    }

    static MutableComponent translatable(String key, Object... args) {
        return new DummyComponent(String.format(key, args));
    }

    static MutableComponent empty() {
        return new DummyComponent("");
    }

    String getString();

    default Style getStyle() {
        return Style.EMPTY;
    }

    class DummyComponent implements MutableComponent {
        private final String text;

        public DummyComponent(String text) {
            this.text = text;
        }

        @Override
        public String getString() {
            return text;
        }

        @Override
        public String toString() {
            return text;
        }

        @Override
        public MutableComponent copy() {
            return new DummyComponent(text);
        }

        @Override
        public Style getStyle() {
            return Style.EMPTY;
        }

        @Override
        public MutableComponent withStyle(java.util.function.UnaryOperator<Style> style) {
            return this;
        }
    }
}
