package edu.illinois.confuzz;


import edu.illinois.confuzz.internal.ConfuzzFramework;
import edu.illinois.confuzz.internal.CoverageForkMain;
import edu.illinois.confuzz.internal.CoverageRunner;
import edu.neu.ccs.prl.meringue.*;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.annotations.ResolutionScope;

import java.io.File;
import java.io.IOException;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

@Mojo(name = "coverage", defaultPhase = LifecyclePhase.POST_INTEGRATION_TEST,
        requiresDependencyResolution = ResolutionScope.TEST)
public class ConfuzzCoverageMojo extends AnalysisMojo {
    @Parameter(readonly = true)
    @SuppressWarnings("unused")
    private String framework;
    @Parameter(property = "logEnabled", defaultValue = "false")
    private boolean logEnabled;
    /**
     * Output directory that named by the test class and method
     */
    protected File testOutputDir;

    @Override
    public void execute() {
        try {
            long startTime = System.currentTimeMillis();
            super.execute();
            long endTime = System.currentTimeMillis();
            ConfigurationMojoHelper.writeTimeToFile(getOutputDirectory(), "coverage", startTime, endTime);
        } catch (Exception e) {
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
                getTemporaryDirectory(), logEnabled);
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

    @Override
    public void analyze() throws MojoExecutionException {
        new CoverageRunner(this).run();
    }

    @Override
    public JvmLauncher createAnalysisLauncher(String jacocoOption, CampaignConfiguration configuration,
                                       FuzzFramework framework)
            throws MojoExecutionException, ReflectiveOperationException {
        List<String> options = new LinkedList<>(configuration.getJavaOptions());
        // Set property to indicate that the analysis phase is running
        options.add("-Dmeringue.analysis=true");
        // Prevent stack traces from being omitted
        options.add("-XX:-OmitStackTraceInFastThrow");
        options.add("-Dconfuzz.goal=coverage");
//        if (debug) {
//            options.add(JvmLauncher.DEBUG_OPT + "5005");
//        }
        options.add("-cp");
        options.add(CampaignUtil.buildClassPath(
                createAnalysisJar(),
                configuration.getTestClasspathJar(),
                createAnalysisFrameworkJar(framework)
        ));
        options.add(jacocoOption);
        options.addAll(framework.getAnalysisJavaOptions());
        String[] arguments = new String[]{
                configuration.getTestClassName(),
                configuration.getTestMethodName(),
                framework.getReplayerClass().getName(),
                String.valueOf(getMaxTraceSize())
        };
        return JvmLauncher.fromMain(
                configuration.getJavaExecutable(),
                CoverageForkMain.class.getName(),
                options.toArray(new String[0]),
                Boolean.getBoolean("confuzz.debug") || logEnabled,
                arguments,
                configuration.getWorkingDirectory(),
                configuration.getEnvironment()
        );
    }

}
