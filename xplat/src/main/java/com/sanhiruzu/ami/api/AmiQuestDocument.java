package com.sanhiruzu.ami.api;

import java.util.ArrayList;
import java.util.List;

/**
 * Source-agnostic searchable quest document.
 */
public record AmiQuestDocument(
        String id,
        String sourceType,
        String sourceId,
        String chapterId,
        String chapterTitle,
        String title,
        String description,
        Status status,
        List<AmiQuestTaskDocument> tasks,
        Runnable openAction
) {
    public AmiQuestDocument {
        id = clean(id);
        if (id.isEmpty()) {
            throw new IllegalArgumentException("Quest id must not be blank");
        }
        sourceType = clean(sourceType);
        sourceId = clean(sourceId);
        chapterId = clean(chapterId);
        chapterTitle = clean(chapterTitle);
        title = clean(title);
        description = description == null ? "" : description.trim();
        status = status == null ? Status.UNKNOWN : status;
        tasks = tasks == null ? List.of() : List.copyOf(tasks);
    }

    public boolean canOpen() {
        return openAction != null;
    }

    public void open() {
        if (openAction != null) {
            openAction.run();
        }
    }

    public static Builder builder(String id, String sourceType, String title) {
        return new Builder(id, sourceType, title);
    }

    private static String clean(String value) {
        return value == null ? "" : value.trim();
    }

    public enum Status {
        UNKNOWN,
        LOCKED,
        AVAILABLE,
        STARTED,
        COMPLETED
    }

    public static final class Builder {
        private final String id;
        private final String sourceType;
        private final String title;
        private String sourceId = "";
        private String chapterId = "";
        private String chapterTitle = "";
        private String description = "";
        private Status status = Status.UNKNOWN;
        private final List<AmiQuestTaskDocument> tasks = new ArrayList<>();
        private Runnable openAction;

        private Builder(String id, String sourceType, String title) {
            this.id = id;
            this.sourceType = sourceType;
            this.title = title;
        }

        public Builder sourceId(String sourceId) {
            this.sourceId = sourceId;
            return this;
        }

        public Builder chapterId(String chapterId) {
            this.chapterId = chapterId;
            return this;
        }

        public Builder chapterTitle(String chapterTitle) {
            this.chapterTitle = chapterTitle;
            return this;
        }

        public Builder description(String description) {
            this.description = description;
            return this;
        }

        public Builder status(Status status) {
            this.status = status;
            return this;
        }

        public Builder task(AmiQuestTaskDocument task) {
            if (task != null) {
                tasks.add(task);
            }
            return this;
        }

        public Builder tasks(List<AmiQuestTaskDocument> tasks) {
            if (tasks != null) {
                for (AmiQuestTaskDocument task : tasks) {
                    task(task);
                }
            }
            return this;
        }

        public Builder openAction(Runnable openAction) {
            this.openAction = openAction;
            return this;
        }

        public AmiQuestDocument build() {
            return new AmiQuestDocument(
                    id,
                    sourceType,
                    sourceId,
                    chapterId,
                    chapterTitle,
                    title,
                    description,
                    status,
                    tasks,
                    openAction
            );
        }
    }
}
