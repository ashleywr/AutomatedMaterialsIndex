package com.sanhiruzu.ami.index;

import com.google.gson.Gson;
import com.sanhiruzu.ami.api.AmiGuideDocument;
import net.minecraft.resources.ResourceLocation;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public final class GuideDocumentMirrorDump {
    private static final Gson GSON = new Gson();

    private GuideDocumentMirrorDump() {
    }

    public static int writeJsonl(Path path, List<AmiGuideDocument> documents) throws IOException {
        Files.createDirectories(path.getParent());
        List<String> lines = new ArrayList<>(documents.size());
        for (AmiGuideDocument document : documents) {
            lines.add(GSON.toJson(Snapshot.from(document)));
        }
        Files.write(path, lines, StandardCharsets.UTF_8);
        return lines.size();
    }

    private record Snapshot(
            String id,
            String sourceType,
            String modId,
            String bookId,
            String iconItemId,
            String pageId,
            String title,
            String chapter,
            List<String> referencedItems,
            List<String> tags,
            String summaryText,
            boolean canOpen
    ) {
        static Snapshot from(AmiGuideDocument document) {
            List<String> items = document.referencedItems().stream()
                    .map(ResourceLocation::toString)
                    .sorted()
                    .toList();
            return new Snapshot(
                    document.id().toString(),
                    document.sourceType(),
                    document.modId(),
                    document.bookId() == null ? "" : document.bookId().toString(),
                    document.iconItemId() == null ? "" : document.iconItemId().toString(),
                    document.pageId(),
                    document.title(),
                    document.chapter(),
                    items,
                    document.tags(),
                    document.summaryText(),
                    document.canOpen()
            );
        }
    }
}
