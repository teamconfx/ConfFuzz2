package edu.illinois.confuzz;

import com.google.gson.*;
import edu.illinois.confuzz.internal.DebugEntry;
import edu.illinois.confuzz.internal.Failure;
import edu.illinois.confuzz.internal.ReproStatus;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

public class DebugResult {

    private String testClass;
    private String testMethod;
    private List<DebugSubResult> debugResults;

    public DebugResult(String testClass, String testMethod) {
        this.testClass = testClass;
        this.testMethod = testMethod;
        this.debugResults = new LinkedList<>();
    }

    private static class DebugSubResult {
        private final String failure;
        private final String errorMessage;
        private final String stackTrace;
        private final ReproStatus reproStatus;
        private final String replayedFailure;
        private final String replayedErrorMessage;
        private final String replayedStackTrace;
        private final String replayedFile;
        private final Map<String, String> minConfig;
        private final List<String> debugFiles = new LinkedList<>();
        public DebugSubResult(DebugEntry entry) {
            Failure error = entry.getFailure();
            this.failure = error.getFailure();
            this.errorMessage = error.getErrorMessage();
            this.stackTrace = DebugUtil.stackTraceToString(error.getStackTrace());
            this.minConfig = entry.getMinConfig();
            for (File debugFile: entry.getDebugFiles()) {
                this.debugFiles.add(debugFile.getName());
            }
            this.reproStatus = entry.getStatus();
            if (entry.getReplayedFile() != null) {
                this.replayedFile = entry.getReplayedFile().getPath();
            } else {
                this.replayedFile = null;
            }
            if (this.reproStatus != ReproStatus.REPRODUCIBLE) {
                Failure replayedError = entry.getReplayedFailure();
                if (replayedError != null) {
                    this.replayedFailure = replayedError.getFailure();
                    this.replayedErrorMessage = replayedError.getErrorMessage();
                    this.replayedStackTrace = DebugUtil.stackTraceToString(replayedError.getStackTrace());
                    return;
                }
            }
            this.replayedFailure = null;
            this.replayedErrorMessage = null;
            this.replayedStackTrace = null;
        }
    }

    public static void dumpResult(File jsonFile, String testClz, String testMethod, List<DebugEntry> failures)
            throws IOException {
        DebugResult result = new DebugResult(testClz, testMethod);
        for (DebugEntry failure: failures) {
            result.debugResults.add(new DebugSubResult(failure));
        }

        Gson gson = new GsonBuilder().setPrettyPrinting().create();
        try (FileWriter out = new FileWriter(jsonFile, false)){
            gson.toJson(result, out);
        }
    }
}
