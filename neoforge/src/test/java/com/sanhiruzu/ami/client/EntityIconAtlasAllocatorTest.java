package com.sanhiruzu.ami.client;

import net.minecraft.resources.ResourceLocation;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class EntityIconAtlasAllocatorTest {
    @Test
    void allocatesDeterministicSlotsByInsertionOrder() {
        EntityIconAtlasAllocator allocator = new EntityIconAtlasAllocator(32, 16);

        EntityIconAtlasAllocator.AtlasEntry first = allocator.allocate(ResourceLocation.parse("minecraft:zombie"));
        EntityIconAtlasAllocator.AtlasEntry second = allocator.allocate(ResourceLocation.parse("minecraft:skeleton"));
        EntityIconAtlasAllocator.AtlasEntry third = allocator.allocate(ResourceLocation.parse("minecraft:creeper"));

        assertEquals(new EntityIconAtlasAllocator.AtlasEntry(0, 0), first);
        assertEquals(new EntityIconAtlasAllocator.AtlasEntry(16, 0), second);
        assertEquals(new EntityIconAtlasAllocator.AtlasEntry(0, 16), third);
        assertEquals(3, allocator.entryCount());
    }

    @Test
    void duplicateAllocationReturnsExistingEntry() {
        EntityIconAtlasAllocator allocator = new EntityIconAtlasAllocator(32, 16);
        ResourceLocation id = ResourceLocation.parse("minecraft:zombie");

        EntityIconAtlasAllocator.AtlasEntry first = allocator.allocate(id);
        EntityIconAtlasAllocator.AtlasEntry second = allocator.allocate(id);

        assertSame(first, second);
        assertEquals(1, allocator.entryCount());
    }

    @Test
    void returnsNullWhenAtlasIsFull() {
        EntityIconAtlasAllocator allocator = new EntityIconAtlasAllocator(32, 16);

        assertNotNull(allocator.allocate(ResourceLocation.parse("minecraft:a")));
        assertNotNull(allocator.allocate(ResourceLocation.parse("minecraft:b")));
        assertNotNull(allocator.allocate(ResourceLocation.parse("minecraft:c")));
        assertNotNull(allocator.allocate(ResourceLocation.parse("minecraft:d")));

        assertNull(allocator.allocate(ResourceLocation.parse("minecraft:e")));
    }
}
