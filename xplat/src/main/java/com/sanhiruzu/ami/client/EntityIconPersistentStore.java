package com.sanhiruzu.ami.client;

import com.mojang.blaze3d.platform.NativeImage;
import net.minecraft.resources.Identifier;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;

final class EntityIconPersistentStore {
    private static final int MAX_PENDING_WRITES =
            Math.max(0, Integer.getInteger("ami.entityIconAtlasPersistQueueLimit", 1024));

    private final Path root;
    private final String fingerprint;
    private final ExecutorService writer;
    private final Map<Integer, LinkedHashMap<Identifier, String>> manifests = new LinkedHashMap<>();
    private int pendingWrites = 0;
    private long droppedWrites = 0L;

    EntityIconPersistentStore(Path root, String fingerprint, ExecutorService writer) {
        this.root = root;
        this.fingerprint = fingerprint;
        this.writer = writer;
    }

    NativeImage loadIcon(Identifier id, int size) {
        LinkedHashMap<Identifier, String> manifest = manifest(size);
        String fileName = manifest.get(id);
        if (fileName == null) {
            return null;
        }
        NativeImage image = readIconImage(iconDirectory(size).resolve(fileName));
        if (image == null) {
            return null;
        }
        if (image.getWidth() != size || image.getHeight() != size) {
            image.close();
            return null;
        }
        return image;
    }

    void enqueueWrite(Identifier id, int size, NativeImage source) {
        Path iconFile;
        synchronized (this) {
            if (pendingWrites >= MAX_PENDING_WRITES) {
                droppedWrites++;
                return;
            }
            LinkedHashMap<Identifier, String> manifest = manifest(size);
            manifest.put(id, EntityIconCacheKey.iconFileName(id.toString()));
            iconFile = iconDirectory(size).resolve(manifest.get(id));
            pendingWrites++;
        }

        NativeImage snapshot = copyIcon(size, source);
        writer.execute(() -> {
            try {
                writeIconAndManifest(size, iconFile, snapshot);
            } finally {
                snapshot.close();
                synchronized (this) {
                    pendingWrites--;
                }
            }
        });
    }

    synchronized StoreStats stats() {
        return new StoreStats(pendingWrites, droppedWrites);
    }

    private LinkedHashMap<Identifier, String> manifest(int size) {
        return manifests.computeIfAbsent(size, this::readManifest);
    }

    private LinkedHashMap<Identifier, String> readManifest(int size) {
        Path manifestFile = manifestFile(size);
        if (!Files.isRegularFile(manifestFile)) {
            return new LinkedHashMap<>();
        }
        try {
            return parseManifestLines(Files.readAllLines(manifestFile, StandardCharsets.UTF_8));
        } catch (IOException | RuntimeException ignored) {
            return new LinkedHashMap<>();
        }
    }

    static LinkedHashMap<Identifier, String> parseManifestLines(List<String> lines) {
        LinkedHashMap<Identifier, String> manifest = new LinkedHashMap<>();
        for (String line : lines) {
            if (line.isBlank()) {
                continue;
            }
            String[] parts = line.split("\t");
            if (parts.length != 2 || !EntityIconCacheKey.isIconFileName(parts[1])) {
                continue;
            }
            try {
                manifest.put(Identifier.parse(parts[0]), parts[1]);
            } catch (RuntimeException ignored) {
                // Ignore corrupt manifest rows.
            }
        }
        return manifest;
    }

    private List<String> manifestLines(LinkedHashMap<Identifier, String> manifest) {
        List<String> lines = new ArrayList<>(manifest.size());
        for (Map.Entry<Identifier, String> entry : manifest.entrySet()) {
            lines.add(entry.getKey() + "\t" + entry.getValue());
        }
        lines.sort(String::compareTo);
        return lines;
    }

    private void writeIconAndManifest(int size, Path iconFile, NativeImage snapshot) {
        try {
            Files.createDirectories(iconDirectory(size));
            snapshot.writeToFile(iconFile);
            List<String> manifestLines;
            synchronized (this) {
                manifestLines = manifestLines(manifest(size));
            }
            writeManifestAtomically(manifestFile(size), manifestLines);
        } catch (IOException ignored) {
            // Persistent cache is best-effort; the runtime atlas remains valid.
        }
    }

    private void writeManifestAtomically(Path manifestFile, List<String> manifestLines) throws IOException {
        Files.createDirectories(manifestFile.getParent());
        Path temp = manifestFile.resolveSibling(manifestFile.getFileName() + ".tmp");
        Files.write(temp, manifestLines, StandardCharsets.UTF_8);
        try {
            Files.move(temp, manifestFile, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
        } catch (IOException atomicMoveFailed) {
            Files.move(temp, manifestFile, StandardCopyOption.REPLACE_EXISTING);
        }
    }

    private NativeImage copyIcon(int size, NativeImage source) {
        NativeImage snapshot = new NativeImage(size, size, true);
        for (int y = 0; y < size; y++) {
            for (int x = 0; x < size; x++) {
                snapshot.setPixel(x, y, source.getPixel(x, y));
            }
        }
        return snapshot;
    }

    private Path cacheDirectory(int size) {
        return root.resolve(fingerprint).resolve(Integer.toString(size));
    }

    private Path iconDirectory(int size) {
        return cacheDirectory(size).resolve("icons");
    }

    private Path manifestFile(int size) {
        return cacheDirectory(size).resolve("icons.tsv");
    }

    private static NativeImage readIconImage(Path file) {
        if (!Files.isRegularFile(file)) {
            return null;
        }
        try (InputStream in = Files.newInputStream(file)) {
            return NativeImage.read(in);
        } catch (IOException ignored) {
            return null;
        }
    }

    record StoreStats(int pendingWrites, long droppedWrites) {
    }
}
