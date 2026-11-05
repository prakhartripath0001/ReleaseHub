package com.yourapp.catalog.software.event;

public record SoftwareIndexEvent(String softwareId, Action action) {

    public enum Action {
        UPSERT,
        DELETE
    }
}
