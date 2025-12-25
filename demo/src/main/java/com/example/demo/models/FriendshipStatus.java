package com.example.demo.models;

public enum FriendshipStatus {
    PENDING("Ожидает подтверждения"),
    ACCEPTED("Принята"),
    REJECTED("Отклонена"),
    BLOCKED("Заблокирована");

    private final String displayName;

    FriendshipStatus(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }

    // Дополнительные методы при необходимости
    public static FriendshipStatus fromString(String text) {
        for (FriendshipStatus status : FriendshipStatus.values()) {
            if (status.name().equalsIgnoreCase(text)) {
                return status;
            }
        }
        throw new IllegalArgumentException("No constant with text " + text + " found");
    }
}
