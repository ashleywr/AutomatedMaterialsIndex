package com.sanhiruzu.ami.compat;

import com.sanhiruzu.ami.api.IAmiPlugin;
import com.sanhiruzu.ami.index.ItemFilter;
import com.sanhiruzu.ami.index.SearchNodeKeys;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

import java.util.Map;

public class KubeJSAmiPlugin implements IAmiPlugin {
    @Override
    public void enrichItemMeta(Identifier id, ItemStack stack, Level level, Map<String, String> metadata) {
        if (AmiKubeJSPlugin.getDevOnlyItems().contains(id)) {
            metadata.put(SearchNodeKeys.ACCESS_LEVEL, ItemFilter.ACCESS_DEV);
        }
    }
}
