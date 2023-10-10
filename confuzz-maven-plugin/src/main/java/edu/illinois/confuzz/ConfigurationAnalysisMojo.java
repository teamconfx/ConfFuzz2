package edu.illinois.confuzz;

import edu.neu.ccs.prl.meringue.AnalysisMojo;
import edu.neu.ccs.prl.meringue.JacocoReportFormat;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.annotations.ResolutionScope;
import edu.illinois.confuzz.internal.ConfuzzFramework;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Deprecated
@Mojo(name = "analyze", defaultPhase = LifecyclePhase.POST_INTEGRATION_TEST,
        requiresDependencyResolution = ResolutionScope.TEST)
public class ConfigurationAnalysisMojo extends AnalysisMojo {
    /**
     * Make the required super class "framework" parameter optional; it is not used.
     */
    @Parameter(readonly = true)
    @SuppressWarnings("unused")
    private String framework;

    @Parameter(property = "regexFile", required = true, defaultValue = "regex")
    private File regexFile;

    @Parameter(property = "analyzeOpt", defaultValue = "true")
    private boolean analyzeOptimization;

    /**
     * Output directory that named by the test class and method
     */
    protected File testOutputDir;

    @Override
    public String getFrameworkClassName() {
        if (analyzeOptimization) {
            getLog().warn("Using optimized analysis framework -- non-failed inputs are ignored");
            return ConfuzzFramework.class.getName();
        }
        getLog().info("Using unoptimized analysis framework -- all inputs are considered");
        return ConfuzzFramework.class.getName();
    }

    @Override
    public List<String> getJavaOptions() throws MojoExecutionException {
        return ConfigurationMojoHelper.getJavaOptions(this, super.getJavaOptions(), getProject(),
                                                      getTemporaryDirectory(), regexFile, "analyze", false, false);
    }

    @Override
    public Map<String, String> getEnvironment() throws MojoExecutionException {
        return ConfigurationMojoHelper.getEnvironment(getProject());
    }

    @Override
    public File getOutputDirectory() throws MojoExecutionException {
        if (testOutputDir == null) {
            testOutputDir =  new File(super.getOutputDirectory(),
                    getTestClassName() + File.separator + getTestMethodName());
        }
        return testOutputDir;
    }


    /** Do not generate jacoco report in analyze mode */
    @Override
    public List<JacocoReportFormat> getJacocoFormats() {
        getLog().info("Jacoco reports are disabled in analyze mode");
        return new ArrayList<>();
    }
}