package edu.illinois.confuzz.internal;

import edu.berkeley.cs.jqf.fuzz.guidance.Guidance;
import edu.berkeley.cs.jqf.fuzz.guidance.GuidanceException;
import edu.berkeley.cs.jqf.fuzz.guidance.Result;
import edu.berkeley.cs.jqf.instrument.tracing.events.TraceEvent;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.Map;
import java.util.function.Consumer;

public class CoverageGuidance implements Guidance {
    /** The real config key-value pair that is used by ConfuzzGenerator to in the coverage goal */
    public static Map<String, Object> curConfigMap;
    /** The config key-object pair that is used by ConfuzzGenerator */
    //public static Map<String, Object> configKeyMap;
    private final CoverageInput coverageInput;
    private boolean consumed = false;
    private static long lastTrialNum = -1;
    private Throwable failure = null;

    public CoverageGuidance(File inputFile) throws IOException, ClassNotFoundException {
        this.coverageInput = CoverageInput.readCoverageInput(inputFile);
        long curTrialNum = coverageInput.getTrialNum();
        if (curTrialNum < lastTrialNum) {
            throw new GuidanceException("Coverage input file is not in order: " + CoverageInput.toCoverageFile(inputFile).getAbsolutePath());
        }
        curConfigMap = coverageInput.getConfigMap();
        //configKeyMap = coverageInput.getConfigKeyMap();
        lastTrialNum = curTrialNum;
    }

    @Override
    public InputStream getInput() throws IllegalStateException, GuidanceException {
        return new ByteArrayInputStream(coverageInput.getInput());
    }

    @Override
    public boolean hasInput() {
        boolean result = !consumed;
        consumed = true;
        return result;
    }

    @Override
    public void handleResult(Result result, Throwable error) throws GuidanceException {
        if (result == Result.FAILURE) {
            this.failure = error;
        }
    }


    @Override
    public Consumer<TraceEvent> generateCallBack(Thread thread) {
        return e -> {
        };
    }

    public Throwable getFailure() {
        return failure;
    }
}
