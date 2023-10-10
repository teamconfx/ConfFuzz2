package edu.illinois.confuzz;

import edu.neu.ccs.prl.meringue.FuzzingMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.annotations.ResolutionScope;
import edu.illinois.confuzz.internal.ConfuzzFramework;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Map;

@Mojo(name = "fuzz", defaultPhase = LifecyclePhase.POST_INTEGRATION_TEST,
        requiresDependencyResolution = ResolutionScope.TEST)
public class ConfigurationFuzzingMojo extends FuzzingMojo {
    /**
     * Make the required super class "framework" parameter optional; it is not used.
     */
    @Parameter(readonly = true)
    @SuppressWarnings("unused")
    private String framework;

    @Parameter(property = "regexFile", required = true, defaultValue = "regex")
    private File regexFile;

    @Parameter(property = "fuzzMode", defaultValue = "confuzz")
    private String fuzzMode;

    @Parameter(property = "logEnabled", defaultValue = "false")
    private boolean logEnabled;

    @Parameter(property = "onlyCheckDefault", defaultValue = "false")
    private boolean onlyCheckDefault;

    /**
     * Output directory that named by the test class and method
     */
    private File testOutputDir;
    static {
        System.setProperty("confuzz.goal", "fuzz");
    }

    @Override
    public void execute() {
        try {
            // Record the start time of the fuzzing
            long startTime = System.currentTimeMillis();
            super.execute();
            // Record the end time of the fuzzing
            long endTime = System.currentTimeMillis();
            // write the time of the fuzzing to a file
            ConfigurationMojoHelper.writeTimeToFile(getOutputDirectory(), "fuzz", startTime, endTime);
        } catch (Exception e) {
            if (onlyCheckDefault) {
                if (e.getMessage().contains("Campaign process terminated unexpectedly")) {
                    return;
                }
            }
            getLog().error(e);
            throw new RuntimeException(e);
        }
    }

    @Override
    public String getFrameworkClassName() {
        return ConfuzzFramework.class.getName();
    }

    @Override
    public List<String> getJavaOptions() throws MojoExecutionException {
        return ConfigurationMojoHelper.getJavaOptions(this, super.getJavaOptions(), getProject(),
                                                      getTemporaryDirectory(), regexFile, fuzzMode, logEnabled, onlyCheckDefault);
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
}
