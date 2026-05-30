package net.neoforged.fml.loading;

public final class FMLLoader {
    private static Dist dist = Dist.CLIENT;

    public static Dist getDist() {
        return dist;
    }

    public static void setDist(Dist d) {
        dist = d;
    }

    public enum Dist {
        CLIENT, DEDICATED_SERVER;

        public boolean isClient() {
            return this == CLIENT;
        }
    }
}
