package com.sanhiruzu.ami.index;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.minecraft.core.Registry;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;

import java.io.IOException;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Dumps the biome, structure and dimension registries, with their tags.
 *
 * <p>Why this exists at all: these are not in the item registry dump, and they cannot be
 * recovered reliably by reading jars. Modded biomes and structures are datapack JSON and
 * <em>can</em> be scanned that way, but vanilla's are registered in code, so a data scan
 * silently misses {@code minecraft:deep_ocean} and every other vanilla entry. Taking them
 * from the live {@link RegistryAccess} is the only way to get one complete list.
 *
 * <p>Tags are included because they are usually what you actually want to reference -
 * {@code #minecraft:is_mountain} rather than enumerating twelve biome ids by hand.
 *
 * <p>Deliberately server-side and free of any client type: the registries are populated
 * from the server's datapacks, so a dedicated server has the authoritative copy.
 */
public final class WorldgenDumpWriter {

    public static final int SCHEMA_VERSION = 1;

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    private WorldgenDumpWriter() {
    }

    /** One registry entry: its id, plus every tag it belongs to. */
    public record Entry(String id, List<String> tags) {
    }

    public record Dump(int schemaVersion, List<Entry> biomes, List<Entry> structures, List<String> dimensions) {
    }

    public record Counts(int biomes, int structures, int dimensions) {
    }

    public static Counts writeJson(Path out, RegistryAccess access) throws IOException {
        Dump dump = collect(access);
        Files.createDirectories(out.getParent());
        try (Writer writer = Files.newBufferedWriter(out, StandardCharsets.UTF_8)) {
            GSON.toJson(dump, writer);
        }
        return new Counts(dump.biomes().size(), dump.structures().size(), dump.dimensions().size());
    }

    public static Dump collect(RegistryAccess access) {
        return new Dump(
                SCHEMA_VERSION,
                collectRegistry(access, Registries.BIOME),
                collectRegistry(access, Registries.STRUCTURE),
                collectDimensions(access));
    }

    // Signatures taken from javap against the 1.21.1 server jar, not from memory:
    //   RegistryAccess.registry(ResourceKey<? extends Registry<? extends E>>) -> Optional<Registry<E>>
    //   Registry.holders() -> Stream<Holder.Reference<T>>          (a Stream, not an Iterable)
    //   Holder.tags()      -> Stream<TagKey<T>>
    private static <T> List<Entry> collectRegistry(
            RegistryAccess access, ResourceKey<? extends Registry<? extends T>> key) {
        List<Entry> rows = new ArrayList<>();
        access.registry(key).ifPresent(registry -> registry.holders().forEach(holder -> {
            ResourceLocation id = holder.key().location();
            List<String> tags = new ArrayList<>();
            holder.tags().forEach(tag -> tags.add("#" + tag.location()));
            tags.sort(Comparator.naturalOrder());
            rows.add(new Entry(id.toString(), tags));
        }));
        rows.sort(Comparator.comparing(Entry::id));
        return rows;
    }

    /**
     * Dimension <em>types</em> come from a datapack registry; the dimensions a world
     * actually has are a level-stem list. LEVEL_STEM is the one that matches what a
     * player can be in, which is what a dimension-visit task needs.
     */
    private static List<String> collectDimensions(RegistryAccess access) {
        List<String> rows = new ArrayList<>();
        access.registry(Registries.LEVEL_STEM).ifPresent(registry -> {
            for (ResourceLocation id : registry.keySet()) {
                rows.add(id.toString());
            }
        });
        rows.sort(Comparator.naturalOrder());
        return rows;
    }
}
