package com.sanhiruzu.ami.api;

import java.util.function.Consumer;

/**
 * Optional provider for searchable guide documents.
 * <p>
 * Implementations should be client-side safe and should not open guide screens
 * while registering documents. Opening belongs in per-document callbacks.
 */
public interface AmiGuideSource {
    String id();

    void registerGuideDocuments(Consumer<AmiGuideDocument> documents);
}
