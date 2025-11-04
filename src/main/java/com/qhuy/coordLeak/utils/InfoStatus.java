package com.qhuy.coordLeak.utils;

public enum InfoStatus {
    START("<green>Starting") {
        @Override
        public boolean getStatus() {
            return true;
        }
    },
    STOP("<red>Stopping"),
    RESTART("<yellow>Restarting");

    private final String message;

    InfoStatus(String msg) {
        this.message = msg;
    }

    public String getMessage() {
        return message;
    }

    public boolean getStatus() {
        return false;
    }
}
