package net.minecraft.resources;

public final class Identifier implements Comparable<Identifier> {
    private final String namespace;
    private final String path;

    public Identifier(String location) {
        int i = location.indexOf(':');
        if (i < 0) {
            this.namespace = "minecraft";
            this.path = location;
        } else {
            this.namespace = location.substring(0, i);
            this.path = location.substring(i + 1);
        }
    }

    public Identifier(String namespace, String path) {
        this.namespace = namespace;
        this.path = path;
    }

    public static Identifier of(String namespace, String path) {
        return new Identifier(namespace, path);
    }

    public static Identifier fromNamespaceAndPath(String namespace, String path) {
        return new Identifier(namespace, path);
    }

    public static Identifier parse(String s) {
        int i = s.indexOf(':');
        if (i < 0) return new Identifier("minecraft", s);
        return new Identifier(s.substring(0, i), s.substring(i + 1));
    }

    public static Identifier tryParse(String s) {
        try {
            return parse(s);
        } catch (Exception e) {
            return null;
        }
    }

    public static Identifier withDefaultNamespace(String path) {
        return new Identifier("minecraft", path);
    }

    public String getNamespace() {
        return namespace;
    }

    public String getPath() {
        return path;
    }

    @Override
    public String toString() {
        return namespace + ":" + path;
    }

    @Override
    public int compareTo(Identifier o) {
        int i = this.namespace.compareTo(o.namespace);
        return i != 0 ? i : this.path.compareTo(o.path);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Identifier that)) return false;
        return namespace.equals(that.namespace) && path.equals(that.path);
    }

    @Override
    public int hashCode() {
        return 31 * namespace.hashCode() + path.hashCode();
    }
}
