package edu.illinois.confuzz.internal;

public class TestLogger {
    private String message;
    public void info(String mes) {
        message = mes;
    }

    public String getMessage() {
        return message;
    }
}
