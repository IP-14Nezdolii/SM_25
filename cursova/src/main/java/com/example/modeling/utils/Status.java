package com.example.modeling.utils;

public enum Status {
    BUSY,
    DONE,
    READY;

    public boolean isBusy() {
        return this == BUSY;
    }

    public boolean isDone() {
        return this == DONE;
    }

    public boolean isReady() {
        return this == READY;
    }
}
