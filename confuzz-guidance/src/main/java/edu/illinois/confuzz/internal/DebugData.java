package edu.illinois.confuzz.internal;

import java.io.*;
import java.nio.file.Files;
import java.util.*;

public final class DebugData implements Serializable {
    public static final String PREFIX = "debug_";
    private static final long serialVersionUID = -6570908799876448036L;
    private final Failure error;
    private final Map<String, Object> generatedConfig;
    private final Map<String, Object> diffConfig;

    // WARNING: Note that here generatedConfig must be equal to the config injected or there would be a bug
    public DebugData(Throwable error, Map<String, Object> generatedConfig, Map<String, Object> diffConfig) {
        this.error = new Failure(getRootCause(error));
        this.generatedConfig = new LinkedHashMap<>(generatedConfig);
        this.diffConfig = new LinkedHashMap<>(diffConfig);
    }

    public Failure getError() {
        return error;
    }

    public Map<String, Object> getGeneratedConfig() {
        return Collections.unmodifiableMap(generatedConfig);
    }

    public Map<String, Object> getDiffConfig() {
        return Collections.unmodifiableMap(diffConfig);
    }

    public static List<DebugData> getDebugDataFromFiles(List<File> debugFiles) throws IOException {
        List<DebugData> ret = new LinkedList<>();
        for (File debugFile : debugFiles) {
            try {
                ret.add(getDebugDataFromFile(debugFile));
            } catch (IOException | ClassNotFoundException e) {
                throw new IOException("Failed to read debug data from " + debugFile, e);
            }
        }
        return ret;
    }

    public static DebugData getDebugDataFromFile(File debugFile) throws IOException, ClassNotFoundException {
        try (ObjectInputStream in = new ObjectInputStream(Files.newInputStream(debugFile.toPath()))) {
            return (DebugData) in.readObject();
        }
    }

    public static File getDebugFile(File failureDirectory, int trialIdx) {
        String saveFileName = String.format(PREFIX + "%06d", trialIdx);
        return new File(failureDirectory, saveFileName);
    }

    public static void writeDebugData(File debugFile, Throwable error, Map<String, Object> generatedConfig, Map<String, Object> diffConfig) throws IOException {
        try (ObjectOutputStream out = new ObjectOutputStream(Files.newOutputStream(debugFile.toPath()))) {
            out.writeObject(new DebugData(error, generatedConfig, diffConfig));
        }
    }

    private static File toDebugFile(File saveFile) {
        return new File(saveFile.getParentFile(), PREFIX + saveFile.getName());
    }
    //TODO: (1) Compare parent and failure round configuration diff; (2) delta-debugging to find the minimum set

    public static Throwable getRootCause(Throwable e) {
        while (e.getCause() != null) {
            e = e.getCause();
        }
        return e;
    }
}
