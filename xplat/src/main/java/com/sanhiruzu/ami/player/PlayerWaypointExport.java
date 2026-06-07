package com.sanhiruzu.ami.player;

public record PlayerWaypointExport(String providerId, String label, String payload) {
    public PlayerWaypointExport {
        providerId = providerId == null ? "" : providerId;
        label = label == null ? providerId : label;
        payload = payload == null ? "" : payload;
    }

    public boolean isUsable() {
        return !providerId.isBlank() && !label.isBlank() && !payload.isBlank();
    }
}
