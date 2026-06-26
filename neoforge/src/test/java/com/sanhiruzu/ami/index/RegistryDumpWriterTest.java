package com.sanhiruzu.ami.index;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class RegistryDumpWriterTest {

    @Test
    void writeJson_emitsVersionedDocumentWithExpectedRows(@org.junit.jupiter.api.io.TempDir Path tmp) throws Exception {
        Path out = tmp.resolve("registry-dump.json");
        var rows = List.of(
            new RegistryDumpWriter.Row(
                "minecraft:diamond_sword", "minecraft",
                "net.minecraft.world.item.SwordItem", "Diamond Sword",
                List.of("Combat"), "weapons", "melee",
                List.of("melee_weapon"))
        );

        int count = RegistryDumpWriter.writeJson(out, rows);

        assertEquals(1, count);
        JsonObject doc = JsonParser.parseString(Files.readString(out)).getAsJsonObject();
        assertEquals(1, doc.get("schemaVersion").getAsInt());
        assertEquals(1, doc.getAsJsonArray("items").size());
        JsonObject item = doc.getAsJsonArray("items").get(0).getAsJsonObject();
        assertEquals("minecraft:diamond_sword", item.get("id").getAsString());
        assertEquals("minecraft", item.get("mod").getAsString());
        assertEquals("net.minecraft.world.item.SwordItem", item.get("className").getAsString());
        assertEquals("Diamond Sword", item.get("displayName").getAsString());
        assertEquals("Combat", item.getAsJsonArray("creativeTabs").get(0).getAsString());
        assertEquals("weapons", item.get("currentCategory").getAsString());
        assertEquals("melee", item.get("currentSubcategory").getAsString());
        assertEquals("melee_weapon", item.getAsJsonArray("currentFacets").get(0).getAsString());
    }

    @Test
    void writeJson_emptyRows_writesEmptyArrayNotNull(@org.junit.jupiter.api.io.TempDir Path tmp) throws Exception {
        Path out = tmp.resolve("registry-dump.json");
        int count = RegistryDumpWriter.writeJson(out, List.of());
        assertEquals(0, count);
        JsonObject doc = JsonParser.parseString(Files.readString(out)).getAsJsonObject();
        assertTrue(doc.has("items"));
        assertEquals(0, doc.getAsJsonArray("items").size());
    }
}
