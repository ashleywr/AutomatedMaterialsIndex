package com.sanhiruzu.ami.compat;

final class EmiCraftablesProvider implements CraftablesProvider {
    @Override
    public Result getCraftables() {
        if (!RecipeViewerBridge.isEmiSelectedExternalViewer()) {
            return Result.unhandled();
        }
        return Result.handled(EmiRecipeBridge.getCraftables());
    }
}
