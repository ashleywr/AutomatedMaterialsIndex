package com.sanhiruzu.ami.compat;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public final class ReflectiveCompat {
    private static final ConcurrentMap<String, Optional<Class<?>>> CLASS_CACHE = new ConcurrentHashMap<>();
    private static final ConcurrentMap<String, Optional<Method>> METHOD_CACHE = new ConcurrentHashMap<>();
    private static final ConcurrentMap<String, Optional<Constructor<?>>> CTOR_CACHE = new ConcurrentHashMap<>();

    private ReflectiveCompat() {
    }

    public static boolean classExists(String className) {
        return !className.isBlank() && findClass(className).isPresent();
    }

    public static Optional<Class<?>> findClass(String className) {
        if (className == null || className.isBlank()) {
            return Optional.empty();
        }
        return CLASS_CACHE.computeIfAbsent(className, ReflectiveCompat::loadClass);
    }

    public static Optional<Method> findMethod(Class<?> owner, String name, Class<?>... parameterTypes) {
        if (owner == null || name == null || name.isBlank()) {
            return Optional.empty();
        }
        String key = owner.getName() + "#" + name + descriptor(parameterTypes);
        return METHOD_CACHE.computeIfAbsent(key, ignored -> {
            try {
                return Optional.of(owner.getMethod(name, parameterTypes));
            } catch (ReflectiveOperationException e) {
                return Optional.empty();
            }
        });
    }

    public static Optional<Constructor<?>> findConstructor(Class<?> owner, Class<?>... parameterTypes) {
        if (owner == null) {
            return Optional.empty();
        }
        String key = owner.getName() + "#<init>" + descriptor(parameterTypes);
        return CTOR_CACHE.computeIfAbsent(key, ignored -> {
            try {
                return Optional.of(owner.getConstructor(parameterTypes));
            } catch (ReflectiveOperationException e) {
                return Optional.empty();
            }
        });
    }

    public static Optional<Object> invoke(Method method, Object receiver, Object... args) {
        if (method == null) {
            return Optional.empty();
        }
        try {
            return Optional.ofNullable(method.invoke(receiver, args));
        } catch (ReflectiveOperationException | RuntimeException e) {
            return Optional.empty();
        }
    }

    public static Optional<Object> construct(Constructor<?> constructor, Object... args) {
        if (constructor == null) {
            return Optional.empty();
        }
        try {
            return Optional.ofNullable(constructor.newInstance(args));
        } catch (ReflectiveOperationException | RuntimeException e) {
            return Optional.empty();
        }
    }

    public static void invokeStatic(String className, String methodName) {
        findClass(className)
                .flatMap(clazz -> findMethod(clazz, methodName))
                .ifPresent(method -> invoke(method, null));
    }

    private static Optional<Class<?>> loadClass(String className) {
        try {
            return Optional.of(Class.forName(className));
        } catch (ClassNotFoundException | LinkageError e) {
            return Optional.empty();
        }
    }

    private static String descriptor(Class<?>... parameterTypes) {
        StringBuilder out = new StringBuilder("(");
        for (Class<?> parameterType : parameterTypes) {
            out.append(parameterType == null ? "null" : parameterType.getName()).append(';');
        }
        return out.append(')').toString();
    }
}
