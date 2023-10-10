package edu.illinois.confuzz.internal;

import edu.neu.ccs.prl.meringue.*;
import edu.neu.ccs.prl.meringue.report.FailureReport;
import org.apache.maven.plugin.MojoExecutionException;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.Comparator;
import java.util.LinkedList;
import java.util.List;

public class CoverageRunner {
    private final AnalysisValues values;

    public CoverageRunner(AnalysisValues values) {
        if (values == null) {
            throw new NullPointerException();
        }
        this.values = values;
    }

    public void run() throws MojoExecutionException {
        try {
            values.initialize();
            CampaignConfiguration configuration = values.createCampaignConfiguration();
            FuzzFramework framework = values.createFrameworkBuilder().build(configuration);
            values.getLog().info("Running analysis for: " + values.getTestDescription());
            run(framework, configuration);
        } catch (IOException | ReflectiveOperationException e) {
            throw new MojoExecutionException("Failed to analyze fuzzing campaign", e);
        }
    }

    private void run(FuzzFramework framework, CampaignConfiguration configuration)
            throws MojoExecutionException, IOException, ReflectiveOperationException {
        framework.startingAnalysis();
        CoverageCalculator calculator = values.createCoverageCalculator();
        JvmLauncher launcher =
                values.createAnalysisLauncher(calculator.getFilter().getJacocoOption(), configuration, framework);
        File[] inputFiles = collectInputFiles(framework);
        long firstTimestamp = inputFiles.length == 0 ? 0 : inputFiles[0].lastModified();
        ConfuzzCoverageReport coverageReport = new ConfuzzCoverageReport(calculator, firstTimestamp, new File(values.getOutputDirectory(), "coverage"));
        FailureReport failureReport = new FailureReport(firstTimestamp);
        analyze(inputFiles, launcher, coverageReport, failureReport);

        writeCoverageIncreasingTrendCSV(coverageReport);
        //writeJacocoReports(configuration, coverageReport);
    }

    private void analyze(File[] inputFiles, JvmLauncher launcher, ConfuzzCoverageReport coverageReport,
                         FailureReport failureReport) throws IOException, MojoExecutionException {
        if (inputFiles.length == 0) {
            values.getLog().info("No input files were found for analysis");
            return;
        }
        try (CoverageAnalyzer analyzer = new CoverageAnalyzer(launcher, coverageReport, failureReport,
                values.getTimeout())) {
            for (int i = 0; i < inputFiles.length; i++) {
                if ((i + 1) % 100 == 1) {
                    values.getLog().info(String.format("Analyzing %d/%d input files", i + 1, inputFiles.length));
                }
                analyzer.analyze(inputFiles[i]);
            }
        }
    }

    private void writeJacocoReports(CampaignConfiguration configuration, ConfuzzCoverageReport report)
            throws MojoExecutionException, IOException {
        File directory = new File(values.getOutputDirectory(), "jacoco");
        FileUtil.ensureEmptyDirectory(directory);
        values.getLog().info("Writing JaCoCo reports to: " + directory);
        report.writeJacocoReports(configuration.getTestDescription(), directory, values.getJacocoFormats());
    }

    private void writeCoverageIncreasingTrendCSV(ConfuzzCoverageReport report, File directory) throws MojoExecutionException, IOException {
        File file = new File(directory, "coverage_trend.csv");
        values.getLog().info("Writing coverage trend to: " + file);
        report.write(file);
    }

    private void writeCoverageIncreasingTrendCSV(ConfuzzCoverageReport report) throws MojoExecutionException, IOException {
        writeCoverageIncreasingTrendCSV(report, values.getOutputDirectory());
    }

    private static File[] collectInputFiles(FuzzFramework framework) throws IOException {
        List<File> files = new LinkedList<>(Arrays.asList(framework.getCorpusFiles()));
        files.addAll(Arrays.asList(framework.getFailureFiles()));
        files.sort(Comparator.comparingLong(File::lastModified));
        return files.toArray(new File[0]);
    }
}
