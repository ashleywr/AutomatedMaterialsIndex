package com.sanhiruzu.ami.client.overlay;

/**
 * Fabric implementation of SearchBarWidget.
 * Extends AbstractSearchBarWidget (xplat) and supplies the platform-specific
 * cursor-movement overrides that NeoForge's EditBox patching provides.
 * Full key/mouse behaviour is handled by the inherited xplat AbstractSearchBarWidget.
 */
public class SearchBarWidget extends AbstractSearchBarWidget {

    public SearchBarWidget(AbstractSearchBarWidget.Listener listener) {
        super(listener);
    }

    @Override
    protected void doMoveCursorTo(int pos, boolean select) {
        moveCursorTo(pos, select);
    }

    @Override
    protected void doMoveCursorToEnd() {
        moveCursorToEnd(false);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        return false;
    }
}
