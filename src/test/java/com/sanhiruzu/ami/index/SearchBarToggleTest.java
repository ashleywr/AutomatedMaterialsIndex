package com.sanhiruzu.ami.index;

import com.sanhiruzu.ami.index.query.QueryUtils;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;

public class SearchBarToggleTest {

    @Test
    public void testToggleToken() {
        // Test adding
        assertEquals("@minecraft", QueryUtils.toggleToken("", "@minecraft"));
        assertEquals("boat @minecraft", QueryUtils.toggleToken("boat", "@minecraft"));
        
        // Test removing from end
        assertEquals("boat", QueryUtils.toggleToken("boat @minecraft", "@minecraft"));
        
        // Test removing from front
        assertEquals("boat", QueryUtils.toggleToken("@minecraft boat", "@minecraft"));
        
        // Test removing from middle
        assertEquals("iron boat", QueryUtils.toggleToken("iron @minecraft boat", "@minecraft"));
        
        // Test exact match removal
        assertEquals("", QueryUtils.toggleToken("@minecraft", "@minecraft"));
        
        // Test that it doesn't remove partial matches
        assertEquals("@minecraft @mine", QueryUtils.toggleToken("@minecraft", "@mine"));
        assertEquals("@minecraft @mine @other", QueryUtils.toggleToken("@minecraft @mine", "@other"));
        
        // Test complex toggle as requested: boat -> @minecraft boat -> boat
        String s = "boat";
        s = QueryUtils.toggleToken(s, "@minecraft");
        assertEquals("boat @minecraft", s); // appended
        s = QueryUtils.toggleToken(s, "@minecraft");
        assertEquals("boat", s); // toggled off
    }
}
