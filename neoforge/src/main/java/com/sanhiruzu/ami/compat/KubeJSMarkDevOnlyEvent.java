package com.sanhiruzu.ami.compat;

import dev.latvian.mods.kubejs.event.KubeEvent;
import net.minecraft.resources.ResourceLocation;

import java.util.Set;

public class KubeJSMarkDevOnlyEvent implements KubeEvent {
    private final Set<ResourceLocation> marked;

    KubeJSMarkDevOnlyEvent(Set<ResourceLocation> marked) {
        this.marked = marked;
    }

    /**
     * Mark an item as dev-only in AMI's index. AMI will treat it as if it has
     * {@code accessLevel = "dev"}, hiding it from normal play but showing it
     * when dev-mode is enabled.
     *
     * @param id the item's registry ID, e.g. {@code "kubejs:my_special_item"}
     */
    public void mark(String id) {
        ResourceLocation loc = ResourceLocation.tryParse(id);
        if (loc != null) {
            marked.add(loc);
        }
    }
}
