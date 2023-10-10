package edu.illinois.confuzz.internal;

import java.io.Serializable;
import java.util.Arrays;

public class Failure implements Serializable {
    private final String failure;
    private final String errorMessage;
    private final StackTraceElement[] stackTrace;

    public Failure() {
        this.failure = "";
        this.errorMessage = "";
        this.stackTrace = new StackTraceElement[0];
    }

    public Failure(Throwable error) {
        this.failure = error.getClass().getName();
        this.errorMessage = error.getMessage();
        this.stackTrace = error.getStackTrace();
    }

    @Override
    public String toString() {
        return failure + ": " + errorMessage + Arrays.stream(stackTrace).map(StackTraceElement::toString)
                .map(e -> "at " + e + ",").reduce("", (a, b) -> a + "\n" + b);
    }

    public String getFailure() {
        return failure;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public StackTraceElement[] getStackTrace() {
        return stackTrace;
    }
}
