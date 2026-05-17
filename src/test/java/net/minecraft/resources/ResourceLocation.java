package net.minecraft.resources;

public final class ResourceLocation implements Comparable<ResourceLocation> {
    private final String namespace;
    private final String path;

    public ResourceLocation(String namespace, String path) {
        this.namespace = namespace;
        this.path = path;
    }

    public static ResourceLocation fromNamespaceAndPath(String namespace, String path) {
        return new ResourceLocation(namespace, path);
    }

    public static ResourceLocation parse(String s) {
        int i = s.indexOf(':');
        if (i < 0) return new ResourceLocation("minecraft", s);
        return new ResourceLocation(s.substring(0, i), s.substring(i + 1));
    }

    public static ResourceLocation tryParse(String s) {
        try { return parse(s); } catch (Exception e) { return null; }
    }

    public static ResourceLocation withDefaultNamespace(String path) {
        return new ResourceLocation("minecraft", path);
    }

    public String getNamespace() { return namespace; }
    public String getPath() { return path; }

    @Override
    public String toString() { return namespace + ":" + path; }

    @Override
    public int compareTo(ResourceLocation o) {
        int i = this.namespace.compareTo(o.namespace);
        return i != 0 ? i : this.path.compareTo(o.path);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof ResourceLocation that)) return false;
        return namespace.equals(that.namespace) && path.equals(that.path);
    }

    @Override
    public int hashCode() {
        return 31 * namespace.hashCode() + path.hashCode();
    }
}
