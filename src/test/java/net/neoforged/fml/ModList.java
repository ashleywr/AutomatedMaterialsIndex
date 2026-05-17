package net.neoforged.fml;

public final class ModList {
    private static final ModList INSTANCE = new ModList();
    public static ModList get() { return INSTANCE; }
    public boolean isLoaded(String modId) { return false; }
}
