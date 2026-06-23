package com.sanhiruzu.ami.client.entitydetails;

public record EntityDetailsRow(
        EntityDetailsSection section,
        String text,
        EntityDetailsLink link,
        String detail,
        EntityDetailsStatKind statKind
) {
    public EntityDetailsRow {
        if (text == null) text = "";
        if (detail == null) detail = "";
        if (statKind == null) statKind = EntityDetailsStatKind.NONE;
    }

    public EntityDetailsRow(EntityDetailsSection section, String text, EntityDetailsLink link, String detail) {
        this(section, text, link, detail, EntityDetailsStatKind.NONE);
    }

    public EntityDetailsRow(EntityDetailsSection section, String text, EntityDetailsLink link) {
        this(section, text, link, "", EntityDetailsStatKind.NONE);
    }

    public static EntityDetailsRow stat(String text, EntityDetailsStatKind statKind) {
        return new EntityDetailsRow(EntityDetailsSection.STATS, text, null, "", statKind);
    }
}
