package net.minecraft.world.level.block.state.properties;

public class Property<T extends Comparable<T>> {
    private final String name;

    public Property(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }
}
