package net.minecraft.resources;

import java.util.Objects;

public final class ResourceLocation {
    private final String namespace;
    private final String path;

    public ResourceLocation(String namespace, String path) {
        this.namespace = namespace;
        this.path = path;
    }

    public static ResourceLocation parse(String s) {
        if (s == null) return null;
        int i = s.indexOf(':');
        if (i == -1) return new ResourceLocation("minecraft", s);
        return new ResourceLocation(s.substring(0, i), s.substring(i + 1));
    }

    public static ResourceLocation fromNamespaceAndPath(String namespace, String path) {
        return new ResourceLocation(namespace, path);
    }

    public String getNamespace() { return namespace; }
    public String getPath() { return path; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof ResourceLocation)) return false;
        ResourceLocation that = (ResourceLocation) o;
        return Objects.equals(namespace, that.namespace) && Objects.equals(path, that.path);
    }

    @Override
    public int hashCode() {
        return Objects.hash(namespace, path);
    }

    @Override
    public String toString() {
        return namespace + ':' + path;
    }
}
