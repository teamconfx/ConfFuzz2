package edu.illinois.confuzz.internal;

public enum ReproStatus {
    REPRODUCIBLE,
    DIFFERENT,
    PASS,
    POLLUTED,
    FLAKY,
    TIMEOUT
}
