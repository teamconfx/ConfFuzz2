package edu.illinois.confuzz.internal;


import org.jetbrains.annotations.NotNull;

import java.io.*;
import java.nio.file.Files;
import java.util.*;

/**
 * Created by Shuai Wang.
 * This is the saved input files for later coverage analysis.
 */
public class CoverageInput implements Serializable {
    public static final String PREFIX = "cov_";

    /** Key-value map for the configuration */
    private final Map<String, Object> configMap;

    /** Key-object map for the configuration that the parameter name is indexed by object but not string*/
    //private final Map<String, Object> configKeyMap;

    /** It is necessary to keep track of the sequence of the fuzzed input to try the best to prevent pollution. */
    private final long trialNum;

    /** Just store the bytes to launch the fuzzing, but not really use it */
    private final byte[] input;

    public CoverageInput(byte[] input, Map<String, Object> configMap, long curTrialNum) {
        this.input = input.clone();
        if (configMap == null) {
            throw new RuntimeException("Config object can't be null in CoverageInput.");
        }
        this.configMap = configMap;
        //this.configKeyMap = configKeyMap;
        this.trialNum = curTrialNum;
    }

    public Map<String, Object> getConfigMap() {
        if (configMap == null) {
            throw new RuntimeException("Coverage input gets null config key-value map.");
        }
        return configMap;
    }

//    public Map<String, Object> getConfigKeyMap() {
//        if (configKeyMap == null) {
//            throw new RuntimeException("Coverage input gets null config key-object map.");
//        }
//        return configKeyMap;
//    }

    /**
     * Read the coverage input from cov_* file.
     */
    public static CoverageInput readCoverageInput(File saveFile) throws IOException, ClassNotFoundException {
        File covFile = toCoverageFile(saveFile);
        try (ObjectInputStream in = new ObjectInputStream(Files.newInputStream(covFile.toPath()))) {
            return (CoverageInput) in.readObject();
        }
    }

    /**
     * Write the coverage input to cov_* file.
     */
    public static void writeCoverageInput(File saveFile, JQFZestGuidance.Input<?> input,
                                          Map<String, Object> configMap, long curTrialNum) throws IOException {
        File covFile = toCoverageFile(saveFile);
        try (ObjectOutputStream out = new ObjectOutputStream(Files.newOutputStream(covFile.toPath()))) {
            out.writeObject(new CoverageInput(convert(input), configMap, curTrialNum));
        }
    }

    /**
     * Get the cov file based on the name of the save file.
     */
    public static File toCoverageFile(File saveFile) {
        return new File(saveFile.getParentFile(), PREFIX + saveFile.getName());
    }

    public static File getSaveFile(File covDirectory, int trialIdx) {
        String saveFileName = String.format("id_%06d", trialIdx);
        return new File(covDirectory, saveFileName);
    }

    public long getTrialNum() {
        return trialNum;
    }

    public byte[] getInput() {
        return input;
    }

    private static byte[] convert(JQFZestGuidance.Input<?> input) {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        for (int b : input) {
            assert (b >= 0 && b < 256);
            out.write(b);
        }
        return out.toByteArray();
    }
}
