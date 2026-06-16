package com.sanhiruzu.ami.compat;

import dev.latvian.mods.kubejs.event.KubeEvent;
import net.minecraft.resources.Identifier;

import java.util.Set;

public class KubeJSMarkDevOnlyEvent implements KubeEvent {
    private final Set<Identifier> marked;

    KubeJSMarkDevOnlyEvent(Set<Identifier> marked) {
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
        Identifier loc = Identifier.tryParse(id);
        if (loc != null) {
            marked.add(loc);
        }
    }
}
