package com.sanhiruzu.ami.release;

import org.junit.jupiter.api.Test;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ForgeDropdownPopupAbiContractTest {
    private static final String DROPDOWN_POPUP = "com/sanhiruzu/ami/client/widget/AmiDropdownPopup.class";
    private static final String DROPDOWN_CONTROLLER = "com/sanhiruzu/ami/client/widget/AmiDropdownPopupController.class";
    private static final String ENUM_DROPDOWN = "com/sanhiruzu/ami/client/widget/AmiEnumDropdownWidget.class";
    private static final String PANEL_DROPDOWN = "com/sanhiruzu/ami/client/widget/AmiPanelContentDropdownWidget.class";
    private static final String POPUP_CLICK_METHOD = "handlePopupClick";
    private static final String POPUP_CLICK_DESC = "(DDI)Z";

    @Test
    void forgeReleaseJarKeepsDropdownPopupClickAbiStable() throws Exception {
        Path releaseJar = requiredPath("ami.forge.releaseJar");
        assertTrue(Files.exists(releaseJar), "Forge release jar not found: " + releaseJar);

        try (ZipFile zip = new ZipFile(releaseJar.toFile())) {
            assertTrue(declaresMethod(zip, DROPDOWN_POPUP, POPUP_CLICK_METHOD, POPUP_CLICK_DESC),
                    "AmiDropdownPopup must declare " + POPUP_CLICK_METHOD + POPUP_CLICK_DESC + " in the Forge release jar.");
            assertTrue(declaresMethod(zip, ENUM_DROPDOWN, POPUP_CLICK_METHOD, POPUP_CLICK_DESC),
                    "AmiEnumDropdownWidget must implement " + POPUP_CLICK_METHOD + POPUP_CLICK_DESC + " in the Forge release jar.");
            assertTrue(declaresMethod(zip, PANEL_DROPDOWN, POPUP_CLICK_METHOD, POPUP_CLICK_DESC),
                    "AmiPanelContentDropdownWidget must implement " + POPUP_CLICK_METHOD + POPUP_CLICK_DESC + " in the Forge release jar.");

            Set<String> controllerCalls = invokedInterfaceMethods(zip, DROPDOWN_CONTROLLER, "com/sanhiruzu/ami/client/widget/AmiDropdownPopup");
            assertTrue(controllerCalls.contains(POPUP_CLICK_METHOD + POPUP_CLICK_DESC),
                    "AmiDropdownPopupController must call AmiDropdownPopup." + POPUP_CLICK_METHOD + POPUP_CLICK_DESC + " in the Forge release jar.");
            assertFalse(controllerCalls.contains("m_6375_" + POPUP_CLICK_DESC),
                    "AmiDropdownPopupController must not call remapped Minecraft mouseClicked names through AmiDropdownPopup in the Forge release jar.");
        }
    }

    private static Path requiredPath(String propertyName) {
        String value = System.getProperty(propertyName);
        if (value == null || value.isBlank()) {
            throw new IllegalStateException("Missing required system property: " + propertyName);
        }
        return Path.of(value);
    }

    private static boolean declaresMethod(ZipFile zip, String entryName, String methodName, String descriptor) throws IOException {
        ZipEntry entry = requiredEntry(zip, entryName);
        byte[] bytes;
        try (var stream = zip.getInputStream(entry)) {
            bytes = stream.readAllBytes();
        }

        final boolean[] found = {false};
        new ClassReader(bytes).accept(new ClassVisitor(Opcodes.ASM9) {
            @Override
            public MethodVisitor visitMethod(int access, String name, String desc, String signature, String[] exceptions) {
                if (name.equals(methodName) && desc.equals(descriptor)) {
                    found[0] = true;
                }
                return null;
            }
        }, ClassReader.SKIP_DEBUG | ClassReader.SKIP_FRAMES | ClassReader.SKIP_CODE);
        return found[0];
    }

    private static Set<String> invokedInterfaceMethods(ZipFile zip, String entryName, String owner) throws IOException {
        ZipEntry entry = requiredEntry(zip, entryName);
        byte[] bytes;
        try (var stream = zip.getInputStream(entry)) {
            bytes = stream.readAllBytes();
        }

        Set<String> invoked = new HashSet<>();
        new ClassReader(bytes).accept(new ClassVisitor(Opcodes.ASM9) {
            @Override
            public MethodVisitor visitMethod(int access, String name, String descriptor, String signature, String[] exceptions) {
                return new MethodVisitor(Opcodes.ASM9) {
                    @Override
                    public void visitMethodInsn(int opcode, String methodOwner, String methodName, String methodDesc, boolean isInterface) {
                        if (isInterface && methodOwner.equals(owner)) {
                            invoked.add(methodName + methodDesc);
                        }
                    }
                };
            }
        }, ClassReader.SKIP_DEBUG | ClassReader.SKIP_FRAMES);
        return invoked;
    }

    private static ZipEntry requiredEntry(ZipFile zip, String entryName) throws IOException {
        ZipEntry entry = zip.getEntry(entryName);
        if (entry == null) {
            throw new IOException("Missing jar entry: " + entryName);
        }
        return entry;
    }
}
