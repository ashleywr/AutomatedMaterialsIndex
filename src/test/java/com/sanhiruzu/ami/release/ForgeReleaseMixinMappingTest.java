package com.sanhiruzu.ami.release;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Assumptions;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.AnnotationNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodNode;

import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import static org.junit.jupiter.api.Assertions.assertTrue;

class ForgeReleaseMixinMappingTest {
    private static final String MIXIN_DESC = "Lorg/spongepowered/asm/mixin/Mixin;";
    private static final String SHADOW_DESC = "Lorg/spongepowered/asm/mixin/Shadow;";

    @Test
    void forgeMinecraftShadowsHaveProductionSafeNames() throws Exception {
        Path releaseJar = optionalPath("ami.forge.releaseJar");
        Path namedToIntermediate = optionalPath("ami.forge.namedToIntermediateTsrg");
        Assumptions.assumeTrue(releaseJar != null && namedToIntermediate != null,
                "Forge release verification properties are not configured for this build.");

        assertTrue(Files.exists(releaseJar), "Forge release jar not found: " + releaseJar);
        assertTrue(Files.exists(namedToIntermediate), "Forge named->SRG mapping not found: " + namedToIntermediate);

        Map<String, Map<MethodSignature, String>> mappings = parseNamedToIntermediateMappings(namedToIntermediate);
        List<String> failures = new ArrayList<>();

        try (ZipFile zip = new ZipFile(releaseJar.toFile())) {
            JsonObject mixinConfig = readJson(zip, "ami.mixins.json");
            String mixinPackagePath = mixinConfig.get("package").getAsString().replace('.', '/');

            for (String mixinName : mixinClassNames(mixinConfig)) {
                String mixinPath = mixinPackagePath + "/" + mixinName.replace('.', '/') + ".class";
                ZipEntry entry = zip.getEntry(mixinPath);
                if (entry == null) {
                    failures.add("Missing mixin class in Forge release jar: " + mixinPath);
                    continue;
                }

                ClassNode mixinClass = readClass(zip, entry);
                AnnotationNode mixinAnnotation = findAnnotation(mixinClass.visibleAnnotations, MIXIN_DESC);
                if (mixinAnnotation == null) {
                    mixinAnnotation = findAnnotation(mixinClass.invisibleAnnotations, MIXIN_DESC);
                }
                if (mixinAnnotation == null) {
                    continue;
                }

                Map<String, Object> mixinValues = annotationValues(mixinAnnotation);
                if (!Boolean.FALSE.equals(mixinValues.get("remap"))) {
                    continue;
                }

                List<String> minecraftTargets = extractMixinTargets(mixinValues).stream()
                        .filter(target -> target.startsWith("net/minecraft/"))
                        .toList();
                if (minecraftTargets.isEmpty()) {
                    continue;
                }

                for (MethodNode method : mixinClass.methods) {
                    AnnotationNode shadowAnnotation = findAnnotation(method.visibleAnnotations, SHADOW_DESC);
                    if (shadowAnnotation == null) {
                        shadowAnnotation = findAnnotation(method.invisibleAnnotations, SHADOW_DESC);
                    }
                    if (shadowAnnotation == null) {
                        continue;
                    }

                    Map<String, Object> shadowValues = annotationValues(shadowAnnotation);
                    Set<String> aliases = new LinkedHashSet<>(stringList(shadowValues.get("aliases")));
                    List<String> targetFailures = new ArrayList<>();
                    boolean checkedAtLeastOneTarget = false;

                    for (String target : minecraftTargets) {
                        String srgName = lookupMethodMapping(mappings, target, method.name, method.desc);
                        if (srgName == null) {
                            continue;
                        }
                        checkedAtLeastOneTarget = true;
                        if (method.name.equals(srgName) || aliases.contains(srgName)) {
                            continue;
                        }
                        targetFailures.add(target + " -> " + srgName);
                    }

                    if (checkedAtLeastOneTarget && !targetFailures.isEmpty()) {
                        failures.add(mixinClass.name + "#" + method.name + method.desc
                                + " is missing Forge production aliases for " + String.join(", ", targetFailures));
                    }
                }
            }
        }

        assertTrue(failures.isEmpty(),
                "Forge release mixin shadows are not production-safe:\n" + String.join("\n", failures));
    }

    private static Path optionalPath(String propertyName) {
        String value = System.getProperty(propertyName);
        if (value == null || value.isBlank()) {
            return null;
        }
        return Path.of(value);
    }

    private static JsonObject readJson(ZipFile zip, String entryName) throws IOException {
        ZipEntry entry = zip.getEntry(entryName);
        if (entry == null) {
            throw new IOException("Missing jar entry: " + entryName);
        }
        try (InputStreamReader reader = new InputStreamReader(zip.getInputStream(entry), StandardCharsets.UTF_8)) {
            return JsonParser.parseReader(reader).getAsJsonObject();
        }
    }

    private static ClassNode readClass(ZipFile zip, ZipEntry entry) throws IOException {
        ClassNode node = new ClassNode();
        try (var stream = zip.getInputStream(entry)) {
            new ClassReader(stream).accept(node, ClassReader.SKIP_DEBUG | ClassReader.SKIP_FRAMES);
        }
        return node;
    }

    private static List<String> mixinClassNames(JsonObject mixinConfig) {
        List<String> classNames = new ArrayList<>();
        addMixinArray(classNames, mixinConfig.getAsJsonArray("mixins"));
        addMixinArray(classNames, mixinConfig.getAsJsonArray("client"));
        addMixinArray(classNames, mixinConfig.getAsJsonArray("server"));
        return classNames;
    }

    private static void addMixinArray(List<String> classNames, JsonArray array) {
        if (array == null) {
            return;
        }
        for (JsonElement element : array) {
            classNames.add(element.getAsString());
        }
    }

    private static Map<String, Map<MethodSignature, String>> parseNamedToIntermediateMappings(Path mappingPath) throws IOException {
        Map<String, Map<MethodSignature, String>> classMappings = new HashMap<>();
        String currentClass = null;

        for (String rawLine : Files.readAllLines(mappingPath, StandardCharsets.UTF_8)) {
            if (rawLine.isBlank() || rawLine.startsWith("#")) {
                continue;
            }

            if (!Character.isWhitespace(rawLine.charAt(0))) {
                String[] parts = rawLine.trim().split("\\s+");
                if (parts.length >= 2) {
                    currentClass = parts[0];
                    classMappings.putIfAbsent(currentClass, new HashMap<>());
                }
                continue;
            }

            if (currentClass == null) {
                continue;
            }

            String[] parts = rawLine.trim().split("\\s+");
            if (parts.length == 3 && parts[1].startsWith("(")) {
                classMappings.get(currentClass).put(new MethodSignature(parts[0], parts[1]), parts[2]);
            }
        }

        return classMappings;
    }

    private static AnnotationNode findAnnotation(List<AnnotationNode> annotations, String descriptor) {
        if (annotations == null) {
            return null;
        }
        for (AnnotationNode annotation : annotations) {
            if (descriptor.equals(annotation.desc)) {
                return annotation;
            }
        }
        return null;
    }

    private static Map<String, Object> annotationValues(AnnotationNode annotation) {
        if (annotation.values == null || annotation.values.isEmpty()) {
            return Collections.emptyMap();
        }

        Map<String, Object> values = new HashMap<>();
        for (int i = 0; i < annotation.values.size(); i += 2) {
            values.put((String) annotation.values.get(i), annotation.values.get(i + 1));
        }
        return values;
    }

    private static List<String> extractMixinTargets(Map<String, Object> values) {
        List<String> targets = new ArrayList<>();

        Object valueTargets = values.get("value");
        if (valueTargets instanceof List<?> types) {
            for (Object type : types) {
                if (type instanceof Type asmType) {
                    targets.add(asmType.getInternalName());
                }
            }
        }

        Object stringTargets = values.get("targets");
        if (stringTargets instanceof List<?> names) {
            for (Object name : names) {
                targets.add(Objects.toString(name).replace('.', '/'));
            }
        }

        return targets;
    }

    private static List<String> stringList(Object value) {
        if (!(value instanceof List<?> values)) {
            return List.of();
        }
        List<String> strings = new ArrayList<>(values.size());
        for (Object element : values) {
            strings.add(Objects.toString(element));
        }
        return strings;
    }

    private static String lookupMethodMapping(Map<String, Map<MethodSignature, String>> mappings,
                                              String owner,
                                              String methodName,
                                              String descriptor) {
        Map<MethodSignature, String> classMappings = mappings.get(owner);
        if (classMappings == null) {
            return null;
        }
        return classMappings.get(new MethodSignature(methodName, descriptor));
    }

    private record MethodSignature(String name, String descriptor) {
    }
}
