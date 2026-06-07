package com.sanhiruzu.ami.player;

public record PlayerWaypointAction(String id, String label, char mnemonic, Runnable action) {
    public boolean isUsable() {
        return id != null && !id.isBlank()
                && label != null && !label.isBlank()
                && action != null;
    }
}
