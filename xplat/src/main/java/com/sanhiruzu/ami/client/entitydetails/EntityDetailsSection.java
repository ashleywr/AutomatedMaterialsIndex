package com.sanhiruzu.ami.client.entitydetails;

public enum EntityDetailsSection {
    STATS("Stats"),
    SPAWNS("Spawns"),
    DROPS("Drops"),
    EXTERNAL_INFO("External Info");

    private final String label;

    EntityDetailsSection(String label) {
        this.label = label;
    }

    public String label() {
        return label;
    }
}
