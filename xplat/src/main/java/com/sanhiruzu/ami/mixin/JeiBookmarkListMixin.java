package com.sanhiruzu.ami.mixin;

import mezz.jei.gui.bookmarks.BookmarkList;
import mezz.jei.gui.bookmarks.IBookmark;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Pseudo
@Mixin(BookmarkList.class)
public class JeiBookmarkListMixin {
    @Inject(method = {"add", "remove"}, at = @At("RETURN"), remap = false)
    private void ami$bookmarksChanged(IBookmark bookmark, CallbackInfoReturnable<Boolean> cir) {
        if (cir.getReturnValueZ()) {
            ami$notifyBookmarksChanged();
        }
    }

    private static void ami$notifyBookmarksChanged() {
        try {
            Class.forName("com.sanhiruzu.ami.compat.RecipeViewerStateSync")
                    .getMethod("favoritesChanged")
                    .invoke(null);
        } catch (ReflectiveOperationException ignored) {
            // Keep JEI's bookmark list usable even if AMI's optional sync bridge is unavailable.
        }
    }
}
