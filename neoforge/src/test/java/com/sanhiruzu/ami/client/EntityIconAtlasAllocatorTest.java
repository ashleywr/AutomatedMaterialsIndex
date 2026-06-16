package com.sanhiruzu.ami.client;

import net.minecraft.resources.Identifier;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class EntityIconAtlasAllocatorTest {
    @Test
    void allocatesDeterministicSlotsByInsertionOrder() {
        EntityIconAtlasAllocator allocator = new EntityIconAtlasAllocator(32, 16);

        EntityIconAtlasAllocator.AtlasEntry first = allocator.allocate(Identifier.parse("minecraft:zombie"));
        EntityIconAtlasAllocator.AtlasEntry second = allocator.allocate(Identifier.parse("minecraft:skeleton"));
        EntityIconAtlasAllocator.AtlasEntry third = allocator.allocate(Identifier.parse("minecraft:creeper"));

        assertEquals(new EntityIconAtlasAllocator.AtlasEntry(0, 0), first);
        assertEquals(new EntityIconAtlasAllocator.AtlasEntry(16, 0), second);
        assertEquals(new EntityIconAtlasAllocator.AtlasEntry(0, 16), third);
        assertEquals(3, allocator.entryCount());
    }

    @Test
    void duplicateAllocationReturnsExistingEntry() {
        EntityIconAtlasAllocator allocator = new EntityIconAtlasAllocator(32, 16);
        Identifier id = Identifier.parse("minecraft:zombie");

        EntityIconAtlasAllocator.AtlasEntry first = allocator.allocate(id);
        EntityIconAtlasAllocator.AtlasEntry second = allocator.allocate(id);

        assertSame(first, second);
        assertEquals(1, allocator.entryCount());
    }

    @Test
    void returnsNullWhenAtlasIsFull() {
        EntityIconAtlasAllocator allocator = new EntityIconAtlasAllocator(32, 16);

        assertNotNull(allocator.allocate(Identifier.parse("minecraft:a")));
        assertNotNull(allocator.allocate(Identifier.parse("minecraft:b")));
        assertNotNull(allocator.allocate(Identifier.parse("minecraft:c")));
        assertNotNull(allocator.allocate(Identifier.parse("minecraft:d")));

        assertNull(allocator.allocate(Identifier.parse("minecraft:e")));
    }
}
