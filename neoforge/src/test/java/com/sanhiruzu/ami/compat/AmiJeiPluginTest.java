package com.sanhiruzu.ami.compat;

import org.junit.jupiter.api.Test;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.tree.ClassNode;

import java.io.InputStream;
import java.lang.reflect.Method;

import static org.junit.jupiter.api.Assertions.*;

public class AmiJeiPluginTest {

    private static final String CLASS_NAME = "com.sanhiruzu.ami.compat.AmiJeiPlugin";

    private ClassNode readClassNode() throws Exception {
        String path = CLASS_NAME.replace('.', '/') + ".class";
        try (InputStream in = getClass().getClassLoader().getResourceAsStream(path)) {
            assertNotNull(in, "Could not find " + path + " on classpath");
            ClassNode node = new ClassNode();
            new ClassReader(in).accept(node, ClassReader.SKIP_CODE | ClassReader.SKIP_DEBUG);
            return node;
        }
    }

    @Test
    void classHasJeiPluginAnnotation() throws Exception {
        ClassNode node = readClassNode();
        boolean found = node.invisibleAnnotations != null
                && node.invisibleAnnotations.stream().anyMatch(
                a -> a.desc.equals("Lmezz/jei/api/JeiPlugin;"));
        assertTrue(found, "AmiJeiPlugin must have @JeiPlugin annotation");
    }

    @Test
    void classImplementsIModPlugin() throws Exception {
        ClassNode node = readClassNode();
        boolean found = node.interfaces != null
                && node.interfaces.contains("mezz/jei/api/IModPlugin");
        assertTrue(found, "AmiJeiPlugin must implement IModPlugin");
    }

    @Test
    void getPluginUidReturnsCorrectNamespaceAndPath() throws Exception {
        Class<?> clazz = Class.forName(CLASS_NAME);
        Object plugin = clazz.getConstructor().newInstance();
        Method getPluginUid = clazz.getMethod("getPluginUid");
        Object uid = getPluginUid.invoke(plugin);
        assertNotNull(uid);

        assertEquals("ami", uid.getClass().getMethod("getNamespace").invoke(uid),
                "Plugin UID namespace must be 'ami'");
        assertEquals("plugin", uid.getClass().getMethod("getPath").invoke(uid),
                "Plugin UID path must be 'plugin'");
    }

    @Test
    void getPluginUidIsConsistent() throws Exception {
        Class<?> clazz = Class.forName(CLASS_NAME);
        Object plugin = clazz.getConstructor().newInstance();
        Method getPluginUid = clazz.getMethod("getPluginUid");
        Object uid1 = getPluginUid.invoke(plugin);
        Object uid2 = getPluginUid.invoke(plugin);
        assertEquals(uid1, uid2);
        assertEquals(uid1.hashCode(), uid2.hashCode());
    }
}
