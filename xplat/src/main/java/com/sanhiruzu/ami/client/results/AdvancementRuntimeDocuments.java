package com.sanhiruzu.ami.client.results;

import com.sanhiruzu.ami.api.AmiAdvancementDocument;
import com.sanhiruzu.ami.compat.RecipeViewerBridge;
import com.sanhiruzu.ami.index.AmiAdvancementSearchIndex;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * Mirrors the current client advancement tree into a lightweight search index.
 */
public final class AdvancementRuntimeDocuments {
    private static final ConcurrentMap<MethodKey, Optional<Method>> METHOD_CACHE = new ConcurrentHashMap<>();
    private static final ConcurrentMap<Class<?>, List<Method>> PROGRESS_METHODS_CACHE = new ConcurrentHashMap<>();

    private AdvancementRuntimeDocuments() {
    }

    public static AmiAdvancementSearchIndex searchIndex() {
        return new AmiAdvancementSearchIndex(documents());
    }

    static List<AmiAdvancementDocument> documents() {
        Minecraft minecraft = Minecraft.getInstance();
        ClientPacketListener connection = minecraft.getConnection();
        if (connection == null) {
            return List.of();
        }

        Object advancements = connection.getAdvancements();
        List<AmiAdvancementDocument> documents = new ArrayList<>();
        for (Object node : advancementNodes(advancements)) {
            Object advancement = advancement(node);
            Optional<Object> display = display(advancement);
            if (display.isEmpty() || isHidden(display.get())) {
                continue;
            }
            documents.add(document(advancements, node, display.get()));
        }
        return List.copyOf(documents);
    }

    private static AmiAdvancementDocument document(Object advancements, Object node, Object display) {
        ResourceLocation id = advancementId(node);
        String tabTitle = rootDisplay(node)
                .map(AdvancementRuntimeDocuments::displayTitle)
                .orElse("");
        ItemStack icon = invokeNoArg(display, "getIcon")
                .filter(ItemStack.class::isInstance)
                .map(ItemStack.class::cast)
                .orElse(ItemStack.EMPTY);
        ResourceLocation iconItemId = icon.isEmpty() ? null : BuiltInRegistries.ITEM.getKey(icon.getItem());
        String title = displayTitle(display);

        return AmiAdvancementDocument.builder(id, title)
                .sourceId(id.getNamespace())
                .tabTitle(tabTitle)
                .description(displayDescription(display))
                .type(displayType(display))
                .progressStatus(progressStatus(advancements, node))
                .iconItemId(iconItemId)
                .openAction(() -> openAdvancementTab(advancements, node))
                .build();
    }

    private static List<Object> advancementNodes(Object advancements) {
        Optional<Object> tree = invokeNoArg(advancements, "getTree");
        if (tree.isPresent()) {
            Optional<Object> nodes = invokeNoArg(tree.get(), "nodes");
            if (nodes.isPresent()) {
                return elements(nodes.get());
            }
        }
        return invokeNoArg(advancements, "getAdvancements")
                .map(AdvancementRuntimeDocuments::elements)
                .orElse(List.of());
    }

    private static Object advancement(Object node) {
        return invokeNoArg(node, "advancement")
                .or(() -> invokeNoArg(node, "value"))
                .or(() -> invokeNoArg(node, "getAdvancement"))
                .orElse(node);
    }

    private static ResourceLocation advancementId(Object node) {
        return invokeNoArg(node, "holder")
                .flatMap(holder -> invokeNoArg(holder, "id"))
                .filter(ResourceLocation.class::isInstance)
                .map(ResourceLocation.class::cast)
                .or(() -> invokeNoArg(node, "id")
                        .filter(ResourceLocation.class::isInstance)
                        .map(ResourceLocation.class::cast))
                .or(() -> invokeNoArg(node, "getId")
                        .filter(ResourceLocation.class::isInstance)
                        .map(ResourceLocation.class::cast))
                .orElseGet(() -> ResourceLocation.fromNamespaceAndPath("ami", "unknown_advancement"));
    }

    private static Optional<Object> rootDisplay(Object node) {
        return invokeNoArg(node, "root")
                .map(AdvancementRuntimeDocuments::advancement)
                .flatMap(AdvancementRuntimeDocuments::display);
    }

    private static Optional<Object> display(Object advancement) {
        return invokeNoArg(advancement, "display")
                .flatMap(AdvancementRuntimeDocuments::unwrapOptional)
                .or(() -> invokeNoArg(advancement, "getDisplay").flatMap(AdvancementRuntimeDocuments::unwrapOptional));
    }

    private static boolean isHidden(Object display) {
        return invokeNoArg(display, "isHidden")
                .filter(Boolean.class::isInstance)
                .map(Boolean.class::cast)
                .orElse(false);
    }

    private static String displayTitle(Object display) {
        return invokeNoArg(display, "getTitle")
                .map(AdvancementRuntimeDocuments::componentString)
                .orElse("");
    }

    private static String displayDescription(Object display) {
        return invokeNoArg(display, "getDescription")
                .map(AdvancementRuntimeDocuments::componentString)
                .orElse("");
    }

    private static String displayType(Object display) {
        return invokeNoArg(display, "getType")
                .flatMap(type -> invokeNoArg(type, "getSerializedName")
                        .map(Object::toString)
                        .or(() -> Optional.of(type.toString().toLowerCase())))
                .orElse("");
    }

    private static AmiAdvancementDocument.ProgressStatus progressStatus(Object advancements, Object node) {
        Optional<Object> progress = advancementProgress(advancements, node);
        if (progress.isEmpty()) {
            return AmiAdvancementDocument.ProgressStatus.NOT_STARTED;
        }
        if (invokeNoArg(progress.get(), "isDone")
                .filter(Boolean.class::isInstance)
                .map(Boolean.class::cast)
                .orElse(false)) {
            return AmiAdvancementDocument.ProgressStatus.COMPLETED;
        }
        if (invokeNoArg(progress.get(), "hasProgress")
                .filter(Boolean.class::isInstance)
                .map(Boolean.class::cast)
                .orElse(false)) {
            return AmiAdvancementDocument.ProgressStatus.IN_PROGRESS;
        }
        if (invokeNoArg(progress.get(), "getCompletedCriteria")
                .map(AdvancementRuntimeDocuments::elements)
                .map(criteria -> !criteria.isEmpty())
                .orElse(false)) {
            return AmiAdvancementDocument.ProgressStatus.IN_PROGRESS;
        }
        return AmiAdvancementDocument.ProgressStatus.NOT_STARTED;
    }

    private static Optional<Object> advancementProgress(Object advancements, Object node) {
        List<Object> candidates = new ArrayList<>();
        invokeNoArg(node, "holder").ifPresent(candidates::add);
        candidates.add(advancement(node));
        candidates.add(node);
        ResourceLocation id = advancementId(node);
        invokeOneArg(advancements, "get", ResourceLocation.class, id).ifPresent(candidates::add);

        for (Method method : progressMethods(advancements.getClass())) {
            Class<?> parameterType = method.getParameterTypes()[0];
            for (Object candidate : candidates) {
                if (candidate == null || !parameterType.isAssignableFrom(candidate.getClass())) {
                    continue;
                }
                try {
                    return Optional.ofNullable(method.invoke(advancements, candidate));
                } catch (IllegalAccessException | InvocationTargetException ignored) {
                    break;
                }
            }
        }
        return Optional.empty();
    }

    private static String componentString(Object value) {
        if (value instanceof Component component) {
            return component.getString();
        }
        return String.valueOf(value);
    }

    private static void openAdvancementTab(Object advancements, Object node) {
        ResourceLocation id = advancementId(node);
        if (RecipeViewerBridge.openJustEnoughAdvancement(id)) {
            return;
        }
        Minecraft minecraft = Minecraft.getInstance();
        minecraft.execute(() -> {
            selectedTabObject(advancements, node).ifPresent(tab -> invokeSetSelectedTab(advancements, tab));
            createAdvancementsScreen(advancements, minecraft.screen).ifPresent(minecraft::setScreen);
        });
    }

    private static Optional<Object> selectedTabObject(Object advancements, Object node) {
        Optional<Object> root = invokeNoArg(node, "root");
        Optional<Object> rootHolder = root.flatMap(value -> invokeNoArg(value, "holder"));
        if (rootHolder.isPresent()) {
            return rootHolder;
        }

        ResourceLocation id = advancementId(node);
        Optional<Object> advancement = invokeOneArg(advancements, "get", ResourceLocation.class, id);
        if (advancement.isPresent()) {
            return advancement;
        }
        return Optional.of(node);
    }

    private static void invokeSetSelectedTab(Object advancements, Object selectedTab) {
        for (Method method : advancements.getClass().getMethods()) {
            if (!method.getName().equals("setSelectedTab") || method.getParameterCount() != 2) {
                continue;
            }
            if (method.getParameterTypes()[1] != boolean.class && method.getParameterTypes()[1] != Boolean.class) {
                continue;
            }
            Class<?> tabType = method.getParameterTypes()[0];
            if (selectedTab != null && !tabType.isAssignableFrom(selectedTab.getClass())) {
                continue;
            }
            try {
                method.invoke(advancements, selectedTab, true);
                return;
            } catch (IllegalAccessException | InvocationTargetException ignored) {
                return;
            }
        }
    }

    private static Optional<Screen> createAdvancementsScreen(Object advancements, Screen lastScreen) {
        try {
            // AdvancementsScreen is an always-present client class; its constructor shape differs
            // across MC 1.20.1/1.21.1 (handled by the constructor probing below), but the class
            // reference itself is stable. A direct reference is remapped by Loom for the
            // intermediary-named Fabric runtime, where Class.forName(Mojmap-name) failed.
            Class<?> screenClass = net.minecraft.client.gui.screens.advancements.AdvancementsScreen.class;
            for (Constructor<?> constructor : screenClass.getConstructors()) {
                Class<?>[] parameters = constructor.getParameterTypes();
                if (parameters.length == 2
                        && parameters[0].isAssignableFrom(advancements.getClass())
                        && Screen.class.isAssignableFrom(parameters[1])) {
                    Object screen = constructor.newInstance(advancements, lastScreen);
                    return screen instanceof Screen value ? Optional.of(value) : Optional.empty();
                }
            }
            for (Constructor<?> constructor : screenClass.getConstructors()) {
                Class<?>[] parameters = constructor.getParameterTypes();
                if (parameters.length == 1 && parameters[0].isAssignableFrom(advancements.getClass())) {
                    Object screen = constructor.newInstance(advancements);
                    return screen instanceof Screen value ? Optional.of(value) : Optional.empty();
                }
            }
        } catch (InstantiationException | IllegalAccessException | InvocationTargetException ignored) {
            return Optional.empty();
        }
        return Optional.empty();
    }

    private static Optional<Object> invokeNoArg(Object target, String methodName) {
        if (target == null) {
            return Optional.empty();
        }
        Optional<Method> method = method(target.getClass(), methodName);
        if (method.isEmpty()) {
            return Optional.empty();
        }
        try {
            return Optional.ofNullable(method.get().invoke(target));
        } catch (IllegalAccessException | InvocationTargetException ignored) {
            return Optional.empty();
        }
    }

    private static Optional<Object> invokeOneArg(Object target, String methodName, Class<?> parameterType, Object argument) {
        if (target == null) {
            return Optional.empty();
        }
        Optional<Method> method = method(target.getClass(), methodName, parameterType);
        if (method.isEmpty()) {
            return Optional.empty();
        }
        try {
            return Optional.ofNullable(method.get().invoke(target, argument));
        } catch (IllegalAccessException | InvocationTargetException ignored) {
            return Optional.empty();
        }
    }

    private static Optional<Method> method(Class<?> owner, String methodName, Class<?>... parameterTypes) {
        MethodKey key = new MethodKey(owner, methodName, List.of(parameterTypes));
        return METHOD_CACHE.computeIfAbsent(key, ignored -> {
            try {
                Method method = owner.getMethod(methodName, parameterTypes);
                method.setAccessible(true);
                return Optional.of(method);
            } catch (NoSuchMethodException | SecurityException ignoredException) {
                return Optional.empty();
            }
        });
    }

    private static List<Method> progressMethods(Class<?> owner) {
        return PROGRESS_METHODS_CACHE.computeIfAbsent(owner, ignored -> Arrays.stream(owner.getMethods())
                .filter(method -> (method.getName().equals("getProgress") || method.getName().equals("getOrStartProgress"))
                        && method.getParameterCount() == 1)
                .peek(method -> {
                    try {
                        method.setAccessible(true);
                    } catch (SecurityException ignoredException) {
                    }
                })
                .toList());
    }

    private static Optional<Object> unwrapOptional(Object value) {
        if (value instanceof Optional<?> optional) {
            return optional.map(Object.class::cast);
        }
        return Optional.ofNullable(value);
    }

    private static List<Object> elements(Object value) {
        if (value instanceof Map<?, ?> map) {
            return new ArrayList<>(map.values());
        }
        if (value instanceof Collection<?> collection) {
            return new ArrayList<>(collection);
        }
        if (value instanceof Iterable<?> iterable) {
            List<Object> elements = new ArrayList<>();
            for (Object element : iterable) {
                elements.add(element);
            }
            return elements;
        }
        return List.of();
    }

    private record MethodKey(Class<?> owner, String name, List<Class<?>> parameterTypes) {
    }
}
