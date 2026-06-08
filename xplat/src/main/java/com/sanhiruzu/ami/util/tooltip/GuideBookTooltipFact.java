package com.sanhiruzu.ami.util.tooltip;

import com.sanhiruzu.ami.index.*;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

import java.util.List;
import java.util.function.Supplier;

public final class GuideBookTooltipFact implements AmiTooltipFact {
    private final Supplier<AmiGuideSearchIndex> guideIndexSupplier;

    public GuideBookTooltipFact() {
        this(() -> AmiIndexerService.getInstance().getGuideSearchIndex());
    }

    GuideBookTooltipFact(Supplier<AmiGuideSearchIndex> guideIndexSupplier) {
        this.guideIndexSupplier = guideIndexSupplier;
    }

    private static boolean isGuideBookCandidate(SearchNode entry) {
        return "true".equalsIgnoreCase(entry.meta(SearchNodeKeys.GUIDE_BOOK_CANDIDATE, ""))
                || containsFacet(entry, "guide_book");
    }

    private static boolean containsFacet(SearchNode entry, String facet) {
        String facets = entry.meta(SearchNodeKeys.FACETS, "");
        if (facets.isBlank()) {
            return false;
        }
        for (String part : facets.split(",")) {
            if (facet.equals(part.trim())) {
                return true;
            }
        }
        return false;
    }

    @Override
    public List<Component> build(SearchNode entry) {
        if (entry.type() != NodeType.ITEM || !isGuideBookCandidate(entry)) {
            return List.of();
        }

        int indexedPages = indexedPageCount(entry);
        Component value = indexedPages > 0
                ? Component.translatable("ami.tooltip.guide_index.indexed", indexedPages)
                : Component.translatable("ami.tooltip.guide_index.not_indexed");
        ChatFormatting formatting = indexedPages > 0 ? ChatFormatting.GREEN : ChatFormatting.GRAY;
        return TooltipFactSupport.line("ami.tooltip.guide_index", value, formatting);
    }

    int indexedPageCount(SearchNode entry) {
        ResourceLocation guideBookId = ResourceLocation.tryParse(entry.meta(SearchNodeKeys.GUIDE_BOOK_ID, ""));
        if (guideBookId == null) {
            return 0;
        }
        AmiGuideSearchIndex guideIndex = guideIndexSupplier.get();
        return guideIndex == null ? 0 : guideIndex.indexedPageCountForBook(guideBookId);
    }
}
