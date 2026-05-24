package com.sanhiruzu.ami.client.widget;

import com.sanhiruzu.ami.config.AmiConfig;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;

import static org.junit.jupiter.api.Assertions.assertTrue;

public class AmiWidgetFactoryTest {

    @Test
    void testCreateBooleanWidget() throws NoSuchFieldException {
        Field field = AmiConfig.class.getField("cheatMode");
        var widget = AmiWidgetFactory.createWidget(field, o -> {
        });
        assertTrue(widget instanceof Button, "Boolean field should create a Button widget");
    }

    @Test
    void testCreateColorWidget() throws NoSuchFieldException {
        Field field = AmiConfig.class.getField("accentColor");
        var widget = AmiWidgetFactory.createWidget(field, o -> {
        });
        assertTrue(widget instanceof EditBox, "Color field should create an EditBox widget");
    }

    @Test
    void testCreateEnumWidget() throws NoSuchFieldException {
        Field field = AmiConfig.class.getField("mode");
        var widget = AmiWidgetFactory.createWidget(field, o -> {
        });
        assertTrue(widget instanceof Button, "Enum field should create a Button widget");
    }

    @Test
    void testCreateIntWidget() throws NoSuchFieldException {
        Field field = AmiConfig.class.getField("leftPanelWidth");
        var widget = AmiWidgetFactory.createWidget(field, o -> {
        });
        assertTrue(widget instanceof EditBox, "Int field should create an EditBox widget");
    }
}
