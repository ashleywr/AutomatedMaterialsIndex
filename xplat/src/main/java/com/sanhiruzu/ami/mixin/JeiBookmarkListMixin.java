package com.sanhiruzu.ami.mixin;

import com.sanhiruzu.ami.compat.JeiBookmarkListMixinSupport;
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
            JeiBookmarkListMixinSupport.bookmarksChanged();
        }
    }
}
