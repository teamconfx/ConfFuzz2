package edu.illinois.confuzz;

import edu.illinois.confuzz.internal.Failure;

import java.util.Map;

public class FuzzResult {
    protected Failure failure;
    protected Map<String, String> config;

    public FuzzResult(Failure failure, Map<String, String> config) {
        this.failure = failure;
        this.config = config;
    }
}
