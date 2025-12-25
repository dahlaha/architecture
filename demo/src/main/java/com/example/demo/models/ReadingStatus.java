package com.example.demo.models;

public enum ReadingStatus {
    WANT_TO_READ("В планах"),
    READING("Читаю"),
    FINISHED("Прочитано"),
    DROPPED("Брошено");

    private final String displayName;

    ReadingStatus(String displayName) {
        this.displayName = displayName;
    }


    public String getDisplayName() {
        return displayName;
    }

}