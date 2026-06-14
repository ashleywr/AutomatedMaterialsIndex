package com.sanhiruzu.ami.index;

import net.minecraft.resources.ResourceLocation;

import java.util.List;
import java.util.Objects;

public record RegistryDocument(
        RegistryDocumentKind kind,
        ResourceLocation id,
        String displayName,
        String description,
        String sourceMod,
        List<String> searchTokens
) {
    public RegistryDocument {
        Objects.requireNonNull(kind, "kind");
        Objects.requireNonNull(id, "id");
        displayName = displayName == null ? "" : displayName;
        description = description == null ? "" : description;
        sourceMod = sourceMod == null ? id.getNamespace() : sourceMod;
        searchTokens = searchTokens == null ? List.of() : List.copyOf(searchTokens);
    }
}
