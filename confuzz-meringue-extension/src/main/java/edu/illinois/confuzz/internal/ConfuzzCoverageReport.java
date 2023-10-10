package edu.illinois.confuzz.internal;

import edu.neu.ccs.prl.meringue.CoverageCalculator;
import edu.neu.ccs.prl.meringue.JacocoReportFormat;
import org.apache.commons.io.FileUtils;
import org.jacoco.core.tools.ExecFileLoader;

import java.io.*;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;

public class ConfuzzCoverageReport {
    private final CoverageCalculator calculator;
    private final List<long[]> rows = new ArrayList<>();
    private final long firstTimestamp;
    private byte[] lastExecData = null;
    private File directory;

    public ConfuzzCoverageReport(CoverageCalculator calculator, long firstTimestamp, File directory) throws IOException {
        this.calculator = calculator;
        this.firstTimestamp = firstTimestamp;
        this.directory = directory;
        // Delete the directory if it exists and create a new one
        FileUtils.deleteDirectory(directory);
        Files.createDirectory(directory.toPath());
    }

    public ConfuzzCoverageReport(CoverageCalculator calculator) throws IOException {
        this(calculator, 0, null);
    }

    public void write(File file) throws IOException {
        try (PrintStream out = new PrintStream(new BufferedOutputStream(Files.newOutputStream(file.toPath())))) {
            out.printf("time, covered_branches%n");
            for (long[] row : rows) {
                out.printf("%d, %d%n", row[0], row[1]);
            }
        }
    }

    public void writeJacocoReports(String testDescription, File directory, Iterable<JacocoReportFormat> formats)
            throws IOException {
        for (JacocoReportFormat format : formats) {
            calculator.createReport(lastExecData, testDescription, format, directory);
        }
    }

    public void writeJacocoExecData(JacocoExecData jacocoExecData, File inputFile) throws IOException {
        File execFile = new File(directory, inputFile.getParentFile().getName() + "_" + inputFile.getName() + jacocoExecData.SUFFIX);
        try (ObjectOutputStream out = new ObjectOutputStream(new FileOutputStream(execFile))) {
            out.writeObject(jacocoExecData);
        }
    }

    public JacocoExecData record(File inputFile, byte[] execData) throws IOException {
        long time = inputFile.lastModified() - firstTimestamp;
        lastExecData = (lastExecData == null) ? execData : mergeExecData(lastExecData, execData);
        long coverage = calculator.calculate(lastExecData);
        if (rows.isEmpty() || coverage > rows.get(rows.size() - 1)[1]) {
            rows.add(new long[]{time, coverage});
        }
        return new JacocoExecData(lastExecData, time);
    }

    public static byte[] mergeExecData(byte[] execData1, byte[] execData2) throws IOException {
        ExecFileLoader loader = new ExecFileLoader();
        loader.load(new ByteArrayInputStream(execData1));
        loader.load(new ByteArrayInputStream(execData2));
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        loader.save(out);
        return out.toByteArray();
    }

    public File getDirectory() {
        return directory;
    }
}