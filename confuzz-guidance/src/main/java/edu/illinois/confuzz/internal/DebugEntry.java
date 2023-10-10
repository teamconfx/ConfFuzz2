package edu.illinois.confuzz.internal;

import java.io.File;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.LinkedList;
import java.util.Map;

public class DebugEntry {
    private ReproStatus status;
    private Failure failure;
    private Failure replayedFailure;
    private File replayedFile;
    private List<Map<String, String>> bugConfigs;
    private List<Map<String, String>> diffConfigs;
    private Map<String, String> minConfig;
    private List<File> debugFiles;

    public DebugEntry(Throwable failure) {
        this(new Failure(failure));
    }

    public DebugEntry(Failure failure) {
        this.status = ReproStatus.PASS;
        this.failure = failure;
        this.replayedFailure = null;
        this.bugConfigs = new LinkedList<>();
        this.diffConfigs = new LinkedList<>();
        this.minConfig = new LinkedHashMap<>();
        this.debugFiles = new LinkedList<>();
    }

    public Failure getFailure() {
        return this.failure;
    }

    public List<Map<String, String>> getBugConfigs() {
        return Collections.unmodifiableList(this.bugConfigs);
    }

    public List<Map<String, String>> getDiffConfig() {
        return Collections.unmodifiableList(this.diffConfigs);
    }

    public void addBugConfig(Map<String, Object> config) {
        this.bugConfigs.add(ConfigUtils.convertFromObjectToStringMap(config));
    }

    public void addDiffConfig(Map<String, Object> config) {
        this.diffConfigs.add(ConfigUtils.convertFromObjectToStringMap(config));
    }

    public void addDebugFile(File debugFile) {
        debugFiles.add(debugFile);
    }

    public void setMinConfig(Map<String, String> config) {
        minConfig = new LinkedHashMap<>(config);
    }

    public void setReplayedFailure(Failure error) {
        replayedFailure = error;
    }

    public void setReplayedFile(File f) {
        replayedFile = new File(f.getPath());
    }

    public void setStatus(ReproStatus s) {
        status = s;
    }

    public ReproStatus getStatus() {
        return status;
    }

    public File getReplayedFile() {
        if (replayedFile == null) {
            return null;
        }
        return new File(replayedFile.getPath());
    }

    public Failure getReplayedFailure() {
        return replayedFailure;
    }

    public Map<String, String> getMinConfig() {
        if (minConfig == null) {
            return null;
        }
        return Collections.unmodifiableMap(minConfig);
    }

    public List<File> getDebugFiles() {
        return Collections.unmodifiableList(debugFiles);
    }

    @Override
    public String toString() {
        return this.failure.toString();
    }
}
