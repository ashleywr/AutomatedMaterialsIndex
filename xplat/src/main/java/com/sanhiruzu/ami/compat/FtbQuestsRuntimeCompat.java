package com.sanhiruzu.ami.compat;

import com.sanhiruzu.ami.AmiCore;
import com.sanhiruzu.ami.api.AmiQuestDocument;
import com.sanhiruzu.ami.api.AmiQuestTaskDocument;
import com.sanhiruzu.ami.api.AmiQuestsApi;
import com.sanhiruzu.ami.config.AmiConfig;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;

/**
 * Runtime-only bridge for FTB Quests client data.
 * <p>
 * This intentionally uses reflection so AMI can compile without depending on
 * FTB Quests on every loader/version, while still mirroring the exact runtime
 * quest file when the mod is present.
 */
public final class FtbQuestsRuntimeCompat {
    private static final String SOURCE_ID = "ftbquests";
    private static final int REFRESH_INTERVAL_TICKS = 40;
    private static final int FILTER_ITEM_CAP = 32;
    private static final ResourceLocation FTB_SMART_FILTER_ITEM =
            ResourceLocation.fromNamespaceAndPath("ftbfiltersystem", "smart_filter");
    private static final java.util.regex.Pattern FTB_FILTER_ITEM_PATTERN =
            java.util.regex.Pattern.compile("(?<![a-zA-Z0-9_:-])item\\(([a-z0-9_.-]+:[a-z0-9_./-]+)\\)");
    private static final java.util.regex.Pattern FTB_FILTER_TAG_PATTERN =
            java.util.regex.Pattern.compile("item_tag\\(([a-z0-9_.-]+:[a-z0-9_./-]+)\\)");
    private static volatile boolean modLoaded;
    private static volatile boolean warnedUnavailable;
    private static int ticksUntilRefresh;
    private static String lastSignature = "";

    private FtbQuestsRuntimeCompat() {
    }

    public static void setModLoaded(boolean loaded) {
        modLoaded = loaded;
        ticksUntilRefresh = 0;
        warnedUnavailable = false;
        if (!loaded) {
            clear();
        }
    }

    public static void clientTick() {
        if (!modLoaded) {
            return;
        }
        if (!AmiConfig.searchIncludeQuests) {
            clearIfNeeded();
            return;
        }
        if (ticksUntilRefresh++ < REFRESH_INTERVAL_TICKS) {
            return;
        }
        ticksUntilRefresh = 0;
        refresh();
    }

    public static void refreshNow() {
        if (!modLoaded) {
            clearIfNeeded();
            return;
        }
        if (!AmiConfig.searchIncludeQuests) {
            clearIfNeeded();
            return;
        }
        ticksUntilRefresh = 0;
        refresh();
    }

    public static void clear() {
        lastSignature = "";
        AmiQuestsApi.replaceQuestDocumentsFromSource(SOURCE_ID, List.of());
    }

    public static boolean openQuestObject(long id) {
        try {
            Class<?> clientQuestFileClass = Class.forName("dev.ftb.mods.ftbquests.client.ClientQuestFile");
            Method open = clientQuestFileClass.getMethod("openBookToQuestObject", long.class);
            open.invoke(null, id);
            return true;
        } catch (ReflectiveOperationException | LinkageError e) {
            AmiCore.LOGGER.warn("AMI: Failed to open FTB quest object {}", id, e);
            return false;
        }
    }

    private static void refresh() {
        if (!AmiConfig.searchIncludeQuests) {
            clearIfNeeded();
            return;
        }
        try {
            Reflection reflection = Reflection.load();
            if (!reflection.exists()) {
                clearIfNeeded();
                return;
            }
            Object file = reflection.clientQuestFileInstance();
            if (file == null) {
                clearIfNeeded();
                return;
            }
            Object teamData = reflection.selfTeamData(file);
            List<AmiQuestDocument> documents = mirrorQuestFile(reflection, file, teamData);
            String signature = signature(documents);
            if (signature.equals(lastSignature)) {
                return;
            }
            lastSignature = signature;
            AmiQuestsApi.replaceQuestDocumentsFromSource(SOURCE_ID, documents);
            AmiCore.LOGGER.debug("AMI: Mirrored {} FTB quest documents", documents.size());
        } catch (ReflectiveOperationException | RuntimeException | LinkageError e) {
            clearIfNeeded();
            if (!warnedUnavailable) {
                warnedUnavailable = true;
                AmiCore.LOGGER.debug("AMI: FTB Quests runtime bridge unavailable", e);
            }
        }
    }

    private static void clearIfNeeded() {
        if (!lastSignature.isEmpty()) {
            clear();
        }
    }

    private static List<AmiQuestDocument> mirrorQuestFile(Reflection reflection, Object file, Object teamData)
            throws ReflectiveOperationException {
        List<Object> quests = new ArrayList<>();
        Consumer<Object> collector = quests::add;
        reflection.forAllQuests.invoke(file, collector);

        List<AmiQuestDocument> documents = new ArrayList<>(quests.size());
        for (Object quest : quests) {
            AmiQuestDocument document = mirrorQuest(reflection, quest, teamData);
            if (document != null) {
                documents.add(document);
            }
        }
        documents.sort((a, b) -> a.id().compareTo(b.id()));
        return documents;
    }

    private static AmiQuestDocument mirrorQuest(Reflection reflection, Object quest, Object teamData)
            throws ReflectiveOperationException {
        long questId = longId(reflection, quest);
        String questCode = codeString(reflection, quest, questId);
        String documentId = SOURCE_ID + ":" + questCode;
        Object chapter = invoke(reflection.getChapter, quest);
        long chapterId = chapter == null ? 0L : longId(reflection, chapter);
        String title = fallback(componentString(invoke(reflection.getTitle, quest)), questCode);
        String chapterTitle = chapter == null ? "" : componentString(invoke(reflection.getTitle, chapter));
        String description = rawDescription(reflection, quest);

        AmiQuestDocument.Builder builder = AmiQuestDocument.builder(documentId, SOURCE_ID, title)
                .sourceId(SOURCE_ID)
                .chapterId(chapter == null ? "" : SOURCE_ID + ":" + codeString(reflection, chapter, chapterId))
                .chapterTitle(chapterTitle)
                .description(description)
                .status(status(reflection, quest, teamData))
                .openAction(() -> openQuestObject(questId));

        for (Object task : collection(invoke(reflection.getTasks, quest))) {
            if (reflection.isItemTask(task)) {
                AmiQuestTaskDocument taskDocument = mirrorItemTask(reflection, documentId, task, teamData);
                if (taskDocument != null) {
                    builder.task(taskDocument);
                }
            }
        }

        for (Object reward : collection(invoke(reflection.getRewards, quest))) {
            if (reflection.isItemReward(reward)) {
                AmiQuestTaskDocument rewardDocument = mirrorItemReward(reflection, documentId, reward);
                if (rewardDocument != null) {
                    builder.task(rewardDocument);
                }
            }
        }

        return builder.build();
    }

    private static AmiQuestTaskDocument mirrorItemTask(Reflection reflection, String questDocumentId, Object task,
                                                       Object teamData) throws ReflectiveOperationException {
        ItemStack stack = asStack(invoke(reflection.itemTaskGetItemStack, task));
        if (stack.isEmpty()) {
            return null;
        }
        long taskId = longId(reflection, task);
        ResourceLocation itemId = BuiltInRegistries.ITEM.getKey(stack.getItem());
        FilterItems filterItems = filterItems(stack, itemId);
        AmiQuestTaskDocument.Builder builder = AmiQuestTaskDocument
                .builder(questDocumentId + "/task/" + codeString(reflection, task, taskId),
                        questDocumentId,
                        AmiQuestTaskDocument.Role.REQUIREMENT)
                .taskType("ftb:item")
                .title(componentString(invoke(reflection.getTitle, task)))
                .itemIds(filterItems.itemIds())
                .requiredCount(number(invoke(reflection.getMaxProgress, task)))
                .progress(teamData == null ? 0L : reflection.progress(teamData, task))
                .consumesItems(booleanValue(invoke(reflection.itemTaskConsumesResources, task)))
                .craftingOnly(booleanValue(invoke(reflection.itemTaskIsOnlyFromCrafting, task)))
                .highCardinality(filterItems.highCardinality());

        if (booleanValue(invoke(reflection.itemTaskIsTaskScreenOnly, task))) {
            builder.tag("task_screen_only");
        }
        for (String tag : filterItems.tags()) {
            builder.tag(tag);
        }
        return builder.build();
    }

    static FilterItems filterItems(ItemStack stack, ResourceLocation itemId) {
        if (itemId == null || !FTB_SMART_FILTER_ITEM.equals(itemId)) {
            return new FilterItems(itemId == null ? List.of() : List.of(itemId), false, List.of());
        }

        String filterString = smartFilterString(stack);
        if (filterString.isBlank()) {
            return new FilterItems(List.of(itemId), true, List.of("ftb_filter_empty"));
        }
        return parseFtbFilterString(filterString, itemId);
    }

    static FilterItems parseFtbFilterString(String filterString, ResourceLocation fallbackItemId) {
        if (filterString == null || filterString.isBlank()) {
            return new FilterItems(fallbackItemId == null ? List.of() : List.of(fallbackItemId), true, List.of("ftb_filter_empty"));
        }

        List<ResourceLocation> itemIds = new ArrayList<>();
        java.util.regex.Matcher itemMatcher = FTB_FILTER_ITEM_PATTERN.matcher(filterString);
        int totalItems = 0;
        while (itemMatcher.find()) {
            totalItems++;
            if (itemIds.size() >= FILTER_ITEM_CAP) {
                continue;
            }
            ResourceLocation parsed = ResourceLocation.tryParse(itemMatcher.group(1));
            if (parsed != null && !itemIds.contains(parsed)) {
                itemIds.add(parsed);
            }
        }

        List<String> tags = new ArrayList<>();
        java.util.regex.Matcher tagMatcher = FTB_FILTER_TAG_PATTERN.matcher(filterString);
        while (tagMatcher.find()) {
            String tag = "ftb_filter_tag:" + tagMatcher.group(1);
            if (!tags.contains(tag)) {
                tags.add(tag);
            }
        }

        boolean highCardinality = totalItems > FILTER_ITEM_CAP || !tags.isEmpty();
        if (itemIds.isEmpty() && fallbackItemId != null) {
            itemIds.add(fallbackItemId);
            highCardinality = true;
        }
        if (totalItems > FILTER_ITEM_CAP) {
            tags.add("ftb_filter_capped:" + totalItems);
        }
        return new FilterItems(itemIds, highCardinality, tags);
    }

    private static String smartFilterString(ItemStack stack) {
        try {
            Class<?> smartFilterItem = Class.forName("dev.ftb.mods.ftbfiltersystem.registry.item.SmartFilterItem");
            Object value = smartFilterItem.getMethod("getFilterString", ItemStack.class).invoke(null, stack);
            return value instanceof String string ? string : "";
        } catch (ClassNotFoundException ignored) {
            return "";
        } catch (ReflectiveOperationException | LinkageError | RuntimeException e) {
            return "";
        }
    }

    private static AmiQuestTaskDocument mirrorItemReward(Reflection reflection, String questDocumentId, Object reward)
            throws ReflectiveOperationException {
        ItemStack stack = asStack(invoke(reflection.itemRewardGetItem, reward));
        if (stack.isEmpty()) {
            return null;
        }
        long rewardId = longId(reflection, reward);
        ResourceLocation itemId = BuiltInRegistries.ITEM.getKey(stack.getItem());
        return AmiQuestTaskDocument
                .builder(questDocumentId + "/reward/" + codeString(reflection, reward, rewardId),
                        questDocumentId,
                        AmiQuestTaskDocument.Role.REWARD)
                .taskType("ftb:item_reward")
                .title(componentString(invoke(reflection.getTitle, reward)))
                .itemId(itemId)
                .requiredCount(number(invoke(reflection.itemRewardGetCount, reward)))
                .build();
    }

    private static AmiQuestDocument.Status status(Reflection reflection, Object quest, Object teamData) {
        if (teamData == null) {
            return AmiQuestDocument.Status.UNKNOWN;
        }
        try {
            if (reflection.isCompleted(teamData, quest)) {
                return AmiQuestDocument.Status.COMPLETED;
            }
            if (reflection.isStarted(teamData, quest)) {
                return AmiQuestDocument.Status.STARTED;
            }
            if (reflection.canStartTasks(teamData, quest)) {
                return AmiQuestDocument.Status.AVAILABLE;
            }
            return AmiQuestDocument.Status.LOCKED;
        } catch (ReflectiveOperationException | RuntimeException e) {
            return AmiQuestDocument.Status.UNKNOWN;
        }
    }

    private static String rawDescription(Reflection reflection, Object quest) throws ReflectiveOperationException {
        List<String> lines = new ArrayList<>();
        for (Object line : collection(invoke(reflection.getRawDescription, quest))) {
            String text = Objects.toString(line, "").trim();
            if (!text.isEmpty()) {
                lines.add(text);
            }
        }
        return String.join("\n", lines);
    }

    private static String signature(List<AmiQuestDocument> documents) {
        StringBuilder builder = new StringBuilder();
        for (AmiQuestDocument document : documents) {
            builder.append(document.id()).append('|')
                    .append(document.title()).append('|')
                    .append(document.description().hashCode()).append('|')
                    .append(document.status()).append('|');
            for (AmiQuestTaskDocument task : document.tasks()) {
                builder.append(task.id()).append(',')
                        .append(task.role()).append(',')
                        .append(task.taskType()).append(',')
                        .append(task.itemIds()).append(',')
                        .append(task.requiredCount()).append(',')
                        .append(task.progress()).append(',')
                        .append(task.consumesItems()).append(',')
                        .append(task.craftingOnly()).append(',')
                        .append(task.tags()).append(';');
            }
            builder.append('\n');
        }
        return builder.toString();
    }

    private static long longId(Reflection reflection, Object object) throws ReflectiveOperationException {
        Object id = invoke(reflection.getId, object);
        return number(id);
    }

    private static String codeString(Reflection reflection, Object object, long id) {
        try {
            return Objects.toString(invoke(reflection.getCodeString, object), Long.toUnsignedString(id)).trim();
        } catch (ReflectiveOperationException | RuntimeException e) {
            return Long.toUnsignedString(id);
        }
    }

    private static Object invoke(Method method, Object target, Object... args) throws ReflectiveOperationException {
        try {
            return method.invoke(target, args);
        } catch (InvocationTargetException e) {
            Throwable cause = e.getCause();
            if (cause instanceof RuntimeException runtimeException) {
                throw runtimeException;
            }
            if (cause instanceof Error error) {
                throw error;
            }
            throw e;
        }
    }

    private static Collection<?> collection(Object value) {
        if (value instanceof Collection<?> collection) {
            return collection;
        }
        return List.of();
    }

    private static ItemStack asStack(Object value) {
        return value instanceof ItemStack stack ? stack : ItemStack.EMPTY;
    }

    private static String componentString(Object value) {
        if (value instanceof Component component) {
            return component.getString().trim();
        }
        return Objects.toString(value, "").trim();
    }

    private static long number(Object value) {
        return value instanceof Number number ? number.longValue() : 0L;
    }

    private static boolean booleanValue(Object value) {
        return value instanceof Boolean bool && bool;
    }

    private static String fallback(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value;
    }

    record FilterItems(List<ResourceLocation> itemIds, boolean highCardinality, List<String> tags) {
        FilterItems {
            itemIds = itemIds == null ? List.of() : List.copyOf(itemIds);
            tags = tags == null ? List.of() : List.copyOf(tags);
        }
    }

    private static final class Reflection {
        private final Class<?> clientQuestFileClass;
        private final Class<?> itemTaskClass;
        private final Class<?> itemRewardClass;
        private final Method exists;
        private final Method forAllQuests;
        private final Method getId;
        private final Method getCodeString;
        private final Method getTitle;
        private final Method getChapter;
        private final Method getTasks;
        private final Method getRewards;
        private final Method getRawDescription;
        private final Method getMaxProgress;
        private final Method itemTaskGetItemStack;
        private final Method itemTaskConsumesResources;
        private final Method itemTaskIsOnlyFromCrafting;
        private final Method itemTaskIsTaskScreenOnly;
        private final Method itemRewardGetItem;
        private final Method itemRewardGetCount;
        private final Method teamDataGetProgress;
        private final Method teamDataIsCompleted;
        private final Method teamDataIsStarted;
        private final Method teamDataCanStartTasks;
        private final Field instance;
        private final Field selfTeamData;

        private Reflection() throws ReflectiveOperationException {
            clientQuestFileClass = Class.forName("dev.ftb.mods.ftbquests.client.ClientQuestFile");
            Class<?> questFileClass = Class.forName("dev.ftb.mods.ftbquests.quest.BaseQuestFile");
            Class<?> questObjectBaseClass = Class.forName("dev.ftb.mods.ftbquests.quest.QuestObjectBase");
            Class<?> questObjectClass = Class.forName("dev.ftb.mods.ftbquests.quest.QuestObject");
            Class<?> questClass = Class.forName("dev.ftb.mods.ftbquests.quest.Quest");
            Class<?> taskClass = Class.forName("dev.ftb.mods.ftbquests.quest.task.Task");
            Class<?> teamDataClass = Class.forName("dev.ftb.mods.ftbquests.quest.TeamData");
            itemTaskClass = Class.forName("dev.ftb.mods.ftbquests.quest.task.ItemTask");
            itemRewardClass = Class.forName("dev.ftb.mods.ftbquests.quest.reward.ItemReward");

            exists = clientQuestFileClass.getMethod("exists");
            forAllQuests = questFileClass.getMethod("forAllQuests", Consumer.class);
            getId = questObjectBaseClass.getMethod("getId");
            getCodeString = questObjectBaseClass.getMethod("getCodeString");
            getTitle = questObjectBaseClass.getMethod("getTitle");
            getChapter = questClass.getMethod("getChapter");
            getTasks = questClass.getMethod("getTasks");
            getRewards = questClass.getMethod("getRewards");
            getRawDescription = questClass.getMethod("getRawDescription");
            getMaxProgress = taskClass.getMethod("getMaxProgress");
            itemTaskGetItemStack = itemTaskClass.getMethod("getItemStack");
            itemTaskConsumesResources = itemTaskClass.getMethod("consumesResources");
            itemTaskIsOnlyFromCrafting = itemTaskClass.getMethod("isOnlyFromCrafting");
            itemTaskIsTaskScreenOnly = itemTaskClass.getMethod("isTaskScreenOnly");
            itemRewardGetItem = itemRewardClass.getMethod("getItem");
            itemRewardGetCount = itemRewardClass.getMethod("getCount");
            teamDataGetProgress = teamDataClass.getMethod("getProgress", taskClass);
            teamDataIsCompleted = teamDataClass.getMethod("isCompleted", questObjectClass);
            teamDataIsStarted = teamDataClass.getMethod("isStarted", questObjectClass);
            teamDataCanStartTasks = teamDataClass.getMethod("canStartTasks", questClass);
            instance = clientQuestFileClass.getField("INSTANCE");
            selfTeamData = clientQuestFileClass.getField("selfTeamData");
        }

        static Reflection load() throws ReflectiveOperationException {
            return new Reflection();
        }

        boolean exists() throws ReflectiveOperationException {
            return booleanValue(invoke(exists, null));
        }

        Object clientQuestFileInstance() throws IllegalAccessException {
            return instance.get(null);
        }

        Object selfTeamData(Object file) throws IllegalAccessException {
            return selfTeamData.get(file);
        }

        boolean isItemTask(Object task) {
            return itemTaskClass.isInstance(task);
        }

        boolean isItemReward(Object reward) {
            return itemRewardClass.isInstance(reward);
        }

        long progress(Object teamData, Object task) throws ReflectiveOperationException {
            return number(invoke(teamDataGetProgress, teamData, task));
        }

        boolean isCompleted(Object teamData, Object quest) throws ReflectiveOperationException {
            return booleanValue(invoke(teamDataIsCompleted, teamData, quest));
        }

        boolean isStarted(Object teamData, Object quest) throws ReflectiveOperationException {
            return booleanValue(invoke(teamDataIsStarted, teamData, quest));
        }

        boolean canStartTasks(Object teamData, Object quest) throws ReflectiveOperationException {
            return booleanValue(invoke(teamDataCanStartTasks, teamData, quest));
        }
    }
}
