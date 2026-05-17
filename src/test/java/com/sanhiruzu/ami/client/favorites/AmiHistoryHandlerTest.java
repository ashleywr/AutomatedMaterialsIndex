package com.sanhiruzu.ami.client.favorites;

import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import org.junit.jupiter.api.Test;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

public class AmiHistoryHandlerTest {

    @Test
    void testRecordLookup() {
        AmiHistoryHandler handler = AmiHistoryHandler.getInstance();
        
        Item ironItem = new Item();
        Item goldItem = new Item();
        
        ItemStack iron = new ItemStack(ironItem);
        ItemStack gold = new ItemStack(goldItem);
        
        handler.recordLookup(iron);
        handler.recordLookup(gold);
        
        List<ItemStack> history = handler.getLookupHistory();
        assertEquals(2, history.size());
        assertEquals(goldItem, history.get(0).getItem()); // Gold should be most recent
        assertEquals(ironItem, history.get(1).getItem());
        
        // Re-record iron
        handler.recordLookup(iron);
        history = handler.getLookupHistory();
        assertEquals(2, history.size());
        assertEquals(ironItem, history.get(0).getItem()); // Iron should move to top
        assertEquals(goldItem, history.get(1).getItem());
    }
}
