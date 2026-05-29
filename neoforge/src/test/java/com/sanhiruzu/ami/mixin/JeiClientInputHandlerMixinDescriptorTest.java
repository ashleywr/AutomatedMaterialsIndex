package com.sanhiruzu.ami.mixin;

import org.junit.jupiter.api.Test;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodNode;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class JeiClientInputHandlerMixinDescriptorTest {
    private static final String MIXIN_CLASS_FILE = "com/sanhiruzu/ami/mixin/JeiClientInputHandlerMixin.class";
    private static final String INJECTOR_NAME = "suppressMouseScroll";

    @Test
    void forgeMouseScrollInjectorMatchesJei15Signature() throws Exception {
        assertVendorSourceContains(
                "../vendor-sources/resolved/jei/forge-1.20.1/runtime/mezz/jei/gui/input/ClientInputHandler.java",
                "public boolean onGuiMouseScroll(double mouseX, double mouseY, double scrollDelta)");

        assertInjectorDescriptor(
                "../forge/build/classes/java/main/" + MIXIN_CLASS_FILE,
                "(DDDLorg/spongepowered/asm/mixin/injection/callback/CallbackInfoReturnable;)V");
    }

    @Test
    void neoForgeMouseScrollInjectorMatchesJei19Signature() throws Exception {
        assertVendorSourceContains(
                "../vendor-sources/resolved/jei/neoforge-1.21.1/runtime/mezz/jei/gui/input/ClientInputHandler.java",
                "public boolean onGuiMouseScroll(double mouseX, double mouseY, double scrollDeltaX, double scrollDeltaY)");

        assertInjectorDescriptor(
                "../neoforge/build/classes/java/main/" + MIXIN_CLASS_FILE,
                "(DDDDLorg/spongepowered/asm/mixin/injection/callback/CallbackInfoReturnable;)V");
    }

    @Test
    void mixinPackageDoesNotContainDirectlyReferencedSupportClasses() throws Exception {
        Path mixinPackage = Paths.get("../xplat/src/main/java/com/sanhiruzu/ami/mixin");
        try (Stream<Path> files = Files.list(mixinPackage)) {
            assertTrue(files.noneMatch(path -> path.getFileName().toString().endsWith("Support.java")),
                    "Support classes called by mixins must live outside com.sanhiruzu.ami.mixin; " +
                            "Mixin rejects direct references to classes in an owned mixin package.");
        }
    }

    private static void assertVendorSourceContains(String sourcePath, String expectedSignature) throws Exception {
        Path path = Paths.get(sourcePath);
        assertTrue(Files.exists(path), "Refresh recipe viewer sources; missing " + path);
        String source = Files.readString(path, StandardCharsets.UTF_8);
        assertTrue(source.contains(expectedSignature),
                "Unexpected JEI ClientInputHandler mouse scroll signature in " + path);
    }

    private static void assertInjectorDescriptor(String classPath, String expectedDescriptor) throws Exception {
        ClassNode node = readClassNode(Paths.get(classPath));
        MethodNode method = node.methods.stream()
                .filter(candidate -> INJECTOR_NAME.equals(candidate.name))
                .findFirst()
                .orElse(null);
        assertNotNull(method, "Missing " + INJECTOR_NAME + " in " + classPath);
        assertEquals(expectedDescriptor, method.desc,
                "Mixin injector descriptor must match the platform JEI ClientInputHandler.onGuiMouseScroll signature");
    }

    private static ClassNode readClassNode(Path path) throws Exception {
        assertTrue(Files.exists(path), "Missing compiled mixin class at " + path);
        try (InputStream in = Files.newInputStream(path)) {
            ClassNode node = new ClassNode();
            new ClassReader(in).accept(node, ClassReader.SKIP_CODE | ClassReader.SKIP_DEBUG);
            return node;
        }
    }
}
