package edu.illinois.confuzz;

import edu.illinois.confuzz.internal.JacocoExecData;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.jacoco.core.tools.ExecFileLoader;

import java.io.ByteArrayInputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Created by Shuai Wang on 07/24/23.
 * This Maven Goal is used to aggregate the jacoco coverage exec data from all the test cases for a single project
 */
@Mojo(name = "dumpCov")
public class CoverageAggregationMojo extends AbstractMojo {
    @Parameter(property="data.dir", required = true, readonly = true)
    private File jacocoExecDataDir;

    @Parameter(property="output.dir", required = true, readonly = true)
    private File outputDir;

    @Override
    public void execute() {
        try {
            long startTime = System.currentTimeMillis();
            init();
            dumpJacocoExec();
            long endTime = System.currentTimeMillis();
            ConfigurationMojoHelper.writeTimeToFile(outputDir, "dumpCov", startTime, endTime);
        } catch (Exception e) {
            getLog().error(e);
            throw new RuntimeException(e);
        }
    }

    private void init() {
        if (!jacocoExecDataDir.exists()) {
            throw new RuntimeException("The jacoco exec data directory does not exist");
        }
        if (!outputDir.exists()) {
            outputDir.mkdirs();
        }
    }

    private void generateJacocoReportFromExecData(long time, ExecFileLoader loader) throws IOException {
        String fileName = time + ".exec";
        File outputFile = new File(outputDir, fileName);
        if (outputFile.exists()) {
            throw new IOException("The output file " + outputFile.getName() + " already exists");
        }
        loader.save(outputFile, false);
    }

    private void dumpJacocoExec() throws IOException {
        // merge coverage exec data from all the test cases
        mergeExecData(getExecDataListFromDir(jacocoExecDataDir));
    }

    /**
     * Merge all the exec data from all test cases
     */
    private void mergeExecData(List<JacocoExecData> execDataListByTime) throws IOException {
        ExecFileLoader loader = new ExecFileLoader();
        // start time from the first exec data and add the first one to the result
        long curTime = 0;
        for (JacocoExecData jed: execDataListByTime) {
            if ((jed.getTime() + 999) / 1000 > curTime) {
                // save
                generateJacocoReportFromExecData(curTime, loader);
            } else if ((jed.getTime() + 999) / 1000 < curTime) {
                throw new IOException("The exec data list is not sorted by time");
            }
            // load
            loader.load(new ByteArrayInputStream(jed.getExecData()));
            curTime = (jed.getTime() + 999) / 1000;
        }
        if (curTime == 0) {
            generateJacocoReportFromExecData(curTime, loader);
        }
    }

    /**
     * Get all the coverage exec data from the directory
     * @param dir
     * @return
     */
    private List<JacocoExecData> getExecDataListFromDir(File dir) throws IOException {
        if (dir.isDirectory()) {
            // iterate the directory and find all files that ends with JacocoExecData.SUFFIX
            // read the file and store the data into a list
            try (Stream<Path> walk = Files.walk(dir.toPath())) {
                return walk
                        .filter(p -> !Files.isDirectory(p))
                        .filter(p -> p.getFileName().toString().endsWith(JacocoExecData.SUFFIX))
                        .map(p -> {
                            // read JacocoExecData from the file
                            try {
                                return JacocoExecData.readJacocoExecDataFromFile(p.toFile());
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                            return null;
                        })
                        .sorted(Comparator.comparingLong(JacocoExecData::getTime))
                        .collect(Collectors.toList());
            } catch (IOException e) {
                throw new IOException("Failed to read the coverage exec data from the directory: " + dir.getAbsolutePath(), e);
            }
        } else {
            List<JacocoExecData> ret = new ArrayList<>();
            try (BufferedReader br = new BufferedReader(new FileReader(dir))) {
                for (String line = br.readLine();line != null; line = br.readLine()) {
                    ret.add(JacocoExecData.readJacocoExecDataFromFile(new File(line)));
                }
            }
            ret.sort(Comparator.comparingLong(JacocoExecData::getTime));
            return ret;
        }
    }
}
