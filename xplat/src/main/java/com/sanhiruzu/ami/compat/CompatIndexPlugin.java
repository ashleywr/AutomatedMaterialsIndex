package com.sanhiruzu.ami.compat;

import com.sanhiruzu.ami.api.AmiGuideDocument;
import com.sanhiruzu.ami.index.GlobalIndex;

import java.util.function.Consumer;

public interface CompatIndexPlugin {
    String modId();

    default void applyToIndex(GlobalIndex index) {}

    default void registerGuideDocuments(Consumer<AmiGuideDocument> registry) {}
}
