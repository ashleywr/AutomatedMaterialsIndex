package com.sanhiruzu.ami.client.overlay;

public class SearchBarWidget extends AbstractSearchBarWidget {

    public SearchBarWidget(Listener listener) {
        super(listener);
    }

    @Override
    protected void doMoveCursorTo(int pos, boolean select) {
        moveCursorTo(pos);
    }

    @Override
    protected void doMoveCursorToEnd() {
        moveCursorToEnd();
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollDelta) {
        return false;
    }
}
