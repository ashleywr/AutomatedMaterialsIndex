package com.sanhiruzu.ami.index;

import com.sanhiruzu.ami.index.resolvers.TagResolver;
import net.minecraft.resources.Identifier;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertTrue;

class TagResolverTest {
    @Test
    void resolvesItemAndBlockTags() {
        SearchNode cable = new SearchNode(
                new Identifier("example", "cable"),
                NodeType.ITEM,
                "Cable",
                0,
                0,
                Map.of(
                        SearchNodeKeys.TAGS, "c:wires",
                        SearchNodeKeys.BLOCK_TAGS, "minecraft:mineable/pickaxe"
                )
        );
        TagResolver resolver = new TagResolver();
        resolver.addNode(cable);

        assertTrue(resolver.resolve("wires").get(NodeType.ITEM).contains(cable));
        assertTrue(resolver.resolve("mineable/pick").get(NodeType.ITEM).contains(cable));
    }
}
