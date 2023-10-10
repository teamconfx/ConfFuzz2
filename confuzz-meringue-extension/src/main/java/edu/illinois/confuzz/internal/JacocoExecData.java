package edu.illinois.confuzz.internal;

import java.io.*;

public class JacocoExecData implements Serializable {
    private byte[] execData;
    private long time;
    public static String SUFFIX = ".jed";  // Jacoco Exec Data

    public JacocoExecData() {
        throw new IllegalArgumentException("JacocoExecData should not be empty");
    }

    public JacocoExecData(byte[] execData, long time) {
        this.execData = execData;
        this.time = time;
    }

    public long getTime() {
        return time;
    }

    public byte[] getExecData() {
        return execData;
    }

    public static JacocoExecData readJacocoExecDataFromFile(File file) throws IOException {
        try (ObjectInputStream in = new ObjectInputStream(new FileInputStream(file))) {
            return (JacocoExecData) in.readObject();
        } catch (ClassNotFoundException e) {
            throw new IOException(e);
        }
    }

    /**
     * Merge two JacocoExecData that have the same time
     */
    public static JacocoExecData merge(JacocoExecData execData1, JacocoExecData execData2) throws IOException {
        if (execData1.getTime() != execData2.getTime()) {
            throw new IOException("execData1 and execData2 should have the same time");
        }
        byte[] execData = ConfuzzCoverageReport.mergeExecData(execData1.getExecData(), execData2.getExecData());
        return new JacocoExecData(execData, execData1.getTime());
    }
}
