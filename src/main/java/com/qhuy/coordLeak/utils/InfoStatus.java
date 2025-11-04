package com.qhuy.coordLeak.utils;

public enum InfoStatus {
    START("Starting") {
        @Override
        public boolean getStatus() {
            return true;
        }
    },
    STOP("Stopping"),
    RESTART("Restarting");

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
