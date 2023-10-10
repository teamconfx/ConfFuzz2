package edu.illinois.confuzz;

import edu.illinois.confuzz.internal.*;
import edu.illinois.confuzz.internal.Failure;
import edu.neu.ccs.prl.meringue.*;
import jdk.jfr.StackTrace;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.annotations.ResolutionScope;
import org.apache.velocity.runtime.Runtime;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.TransformerException;
import java.io.*;
import java.sql.Time;
import java.util.*;

//TODO: Later remove ConfigurationAnalysisMojo and merge it with this class
@Mojo(name = "debug", defaultPhase = LifecyclePhase.POST_INTEGRATION_TEST,
        requiresDependencyResolution = ResolutionScope.TEST)
public class ConfigurationDebugNewJvmMojo extends ConfigurationAnalysisMojo {
    private JvmLauncher launcher;
    protected File debugFrameworkJar;
    private String resultFileName = "failures.json";
    private int curChecked = 0;
    protected int totalLength = 0;
    private DebugUtil debugger;
    private String CSV_SEPARATOR = "@;@";
    private Failure pollutionError = null;
    private Queue<DebugEntry> debugQueue = new LinkedList<>();
    @Parameter(property = "injectConfigFile", required = true)
    private File injectConfigFile;

    @Parameter(property = "localDebug", defaultValue = "false")
    private boolean localDebug;

    @Parameter(property = "debugBatchSize", defaultValue = "10")
    protected int debugBatchSize;

    @Parameter(property = "addBinaryDiff", defaultValue = "false")
    protected boolean addBinaryDiff;

    /** debugEntryMap is a map with key-value pair of (FailureType, DebugEntry) */
    protected Map<String, DebugEntry> debugEntryMap = new LinkedHashMap<>();
    public static String actualTestName = null;

    /** Max depth of entry map that can be extended with new binary search failue */
    public static int maxEntryMapSize = 2;

    @Override
    public void execute() {
        try {
            // Record the start time of the fuzzing
            long startTime = System.currentTimeMillis();
            // TODO: first check if there are multiple ctests
            // We must keep only one ctest.xml
            init();
            // If addBinaryDiff is true, we will add all debug left-right sub-diff to the debug queue
            if (addBinaryDiff) {
                debugQueue.addAll(debugEntryMap.values());
                while (!debugQueue.isEmpty()) {
                    DebugEntry failure = debugQueue.poll();
                    deltaDebug(failure);
                }
            } else {
                for (Map.Entry<String, DebugEntry> entry : debugEntryMap.entrySet()) {
                    DebugEntry failure = entry.getValue();
                    deltaDebug(failure);
                }
            }

            /** Write root cause configuration set to csv file */
            File resultFile = new File(getOutputDirectory().getAbsolutePath(), this.resultFileName);
            DebugResult.dumpResult(resultFile, getTestClassName(), getTestMethodName(), new LinkedList<>(debugEntryMap.values()));
            injectConfigFile.delete();
            long endTime = System.currentTimeMillis();
            ConfigurationMojoHelper.writeTimeToFile(getOutputDirectory(), "debug", startTime, endTime);
        } catch (Exception e) {
            getLog().error(e);
            throw new RuntimeException(e);
        }
    }

    protected void init() throws MojoExecutionException, IOException {
        injectConfigFile.createNewFile();
        initDebug();
        /** Run failure inspection */
        initDebugEntryMap();
        // Open binary diff mode if there are less than 5 failures
        if (debugEntryMap.size() < 5) {
            System.setProperty("addBinaryDiff", String.valueOf(addBinaryDiff));
            maxEntryMapSize = debugEntryMap.size() * 2;
        }
        if (addBinaryDiff) {
            getLog().warn("Turn on new failure debugging mode");
        } else {
            getLog().warn("Turn off new failure debugging mode");
        }
    }

    protected void initDebug() throws MojoExecutionException, IOException {
        /** Initialize the launcher */
        FuzzFramework framework = this.createFrameworkBuilder()
                .build(this.createCampaignConfiguration());
        this.debugFrameworkJar = framework.getRequiredClassPathElements().iterator().next();
        init(createCampaignConfiguration());
        /** Check configFile Exists */
        if (!ConfigUtils.checkConfigFile(injectConfigFile)) {
            throw new MojoExecutionException("Configuration file does not exist");
        }
        /** Read original configuration from configFile if exists */
        ConfigUtils.initializeInjectionConfigFile(injectConfigFile);
    }

    private void initDebugEntryMap() throws MojoExecutionException, IOException {
        List<File> debugFiles = new LinkedList<>();
        File failureDirectory = new File(getOutputDirectory().getAbsolutePath(), "campaign/failures");
        if (!failureDirectory.exists()) {
            throw new MojoExecutionException("Failure directory does not exist, please run fuzzing first");
        }

        for (File f: failureDirectory.listFiles()) {
            if (f.isFile() && f.getName().startsWith(DebugData.PREFIX)) {
                System.out.println(f.getName());
                debugFiles.add(f);
            }
        }
        // Vanilla run to check that the state is not polluted
        checkPollution();

        List<DebugData> debugDatas = DebugData.getDebugDataFromFiles(debugFiles);
        /*
         * Sometimes the name of the test class is not the same as the name of the class that contains the test method.
         * We need to set the actual class name to make the identifier calculation right.
         */
        for (DebugData debugData: debugDatas) {
            StackTraceElement[] stackTrace = debugData.getError().getStackTrace();
            for (int i = 1; i < stackTrace.length; i ++) {
                if (stackTrace[i].getMethodName().endsWith("$$CONFUZZ")) {
                    actualTestName = stackTrace[i-1].getClassName();
                    break;
                }
            }
            if (actualTestName != null) {
                break;
            }
        }
        for (int idx = 0; idx < debugFiles.size(); idx++) {
            DebugData debugData = debugDatas.get(idx);
            Failure error = debugData.getError();
            String identifier = DebugUtil.hashThrowable(error, getTestClassName());
            if (!debugEntryMap.containsKey(identifier)) {
                debugEntryMap.put(identifier, new DebugEntry(error));
            }
            DebugEntry debugEntry = debugEntryMap.get(identifier);
            debugEntry.addDebugFile(debugFiles.get(idx));
            debugEntry.addDiffConfig(debugData.getDiffConfig());
            debugEntry.addBugConfig(debugData.getGeneratedConfig());
        }
    }

    private void checkPollution() throws MojoExecutionException, IOException {
        debugger = new DebugUtil(launcher, injectConfigFile, getLog(), new Failure(),
                new LinkedHashMap<>(), getOutputDirectory(), getTestClassName(), getTestMethodName());
        try {
            pollutionError = debugger.runWithNewJVM();
        } catch(InterruptedException e) {
            // test should not timeout on its own
            pollutionError = new Failure(e);
        }

        if (isPolluted()) {
            getLog().error("Test failed on its own due to pollution. Skip debugging all failures.");
        }
    }

    public boolean isPolluted() {
        return pollutionError != null;
    }

    private void deltaDebug(DebugEntry failure) throws ParserConfigurationException, TransformerException,
            IOException, InterruptedException, AssertionError, MojoExecutionException {
        // Skip debugging timeout failures
        if (failure.getFailure().getFailure().contains("TimeoutException")) {
            failure.setStatus(ReproStatus.TIMEOUT);
            failure.setMinConfig(failure.getBugConfigs().get(0));
            return;
        }

        if (isPolluted()) {
            failure.setStatus(ReproStatus.POLLUTED);
            if (failure.getDebugFiles().size() > 0) {
                failure.setReplayedFile(failure.getDebugFiles().get(0));
            }
            failure.setMinConfig(failure.getBugConfigs().get(0));
            return;
        }

        if (failure.getDebugFiles().size() > 0) {
            getLog().info("Currently debugging: " + failure.getDebugFiles().get(0).getName());
        } else {
            getLog().info("Currently debugging: new failure encountered during debugging");
        }

        // Read parameter from configuration file
        // TODO: To make delta debug more robust, we may need to read multiple bug configs
        //  and stop debugging at the first one that can reproduce the bug
        Map<String, String> diffConfig = failure.getDiffConfig().get(0);
        Map<String, String> failedConfig = failure.getBugConfigs().get(0);
        if (diffConfig.size() < failedConfig.size()) {
            // First check the configuration diff between child and parent
            if (diffConfig.size() > 0) {
                getLog().info("Debugging parent-child different configuration first: diffConfig Size = " +
                        diffConfig.size() + " whole config size = " + failure.getBugConfigs().size());
                debugger = new DebugUtil(launcher, injectConfigFile, getLog(), failure.getFailure(),
                        diffConfig, getOutputDirectory(), getTestClassName(), getTestMethodName());
                ReproStatus status = debugger.getReproduceStatus(failure);
                if (failure.getDebugFiles().size() > 0) {
                    failure.setReplayedFile(failure.getDebugFiles().get(0));
                }

                // Only return if the diff can reproduce the failure, otherwise debug with the whole config
                if (status == ReproStatus.REPRODUCIBLE) {
                    failure.setStatus(status);
                    Map<String, String> res = debugger.searchBuggyConfig(debugQueue, debugEntryMap, failure);
                    // print the result
                    getLog().info("Iterate " + debugger.getDebugIteration() + " times with binary search.");
                    getLog().info("Debugging result: ");
                    for (Map.Entry<String, String> entry : res.entrySet()) {
                        getLog().info(entry.getKey() + " = " + entry.getValue());
                    }
                    failure.setMinConfig(res);
                    return;
                }
            } else {
                getLog().error("No parent-child different configuration found!");
            }
        }

        getLog().info("Debugging with the whole configuration.");
        Failure error = failure.getFailure();

        debugger = new DebugUtil(launcher, injectConfigFile, getLog(), error,
                failedConfig, getOutputDirectory(), getTestClassName(), getTestMethodName());
        ReproStatus status = debugger.getReproduceStatus(failure);
        failure.setStatus(status);
        if (failure.getDebugFiles().size() > 0) {
            failure.setReplayedFile(failure.getDebugFiles().get(0));
        }

        if (status == ReproStatus.PASS) {
            getLog().error("Test passed. Skip debugging this failure.");
            failure.setMinConfig(failedConfig);
            return;
        } else if (status == ReproStatus.DIFFERENT) {
            getLog().error("Test triggered different failure. Debugging the new failure instead.");
            if (failure.getReplayedFailure().getFailure().contains("TimeoutException")) {
                failure.setStatus(ReproStatus.TIMEOUT);
                failure.setMinConfig(failure.getBugConfigs().get(0));
                return;
            }
            debugger.setExpectedFailure(failure.getReplayedFailure());
        }

        Map<String, String> res = debugger.searchBuggyConfig(debugQueue, debugEntryMap, failure);
        // print the result
        getLog().info("Iterate " + debugger.getDebugIteration() + " times with binary search");
        getLog().info("Debugging result: ");
        for (Map.Entry<String, String> entry : res.entrySet()) {
            getLog().info(entry.getKey() + " = " + entry.getValue());
        }
        failure.setMinConfig(res);
    }

    protected Map<String, String> runDebugOneByOne(Map<String, String> config)
            throws IOException, ParserConfigurationException, TransformerException, InterruptedException {
        LinkedHashMap<String, String> res = new LinkedHashMap<>();
        for (Map.Entry<String, String> entry : config.entrySet()) {
            getLog().info("Checking parameter: " + entry.getKey() + " = " + entry.getValue());
            ConfigUtils.cleanConfig(injectConfigFile, System.out);
            Map<String, String> inject = new HashMap<>();
            inject.put(entry.getKey(), entry.getValue());
            ConfigUtils.injectConfig(injectConfigFile, inject, System.out, localDebug);

            // If the test fail, then this parameter is included in the return value
            if (!debugger.testPass()) {
                res.put(entry.getKey(), entry.getValue());
            }
            curChecked += 1;
            getLog().info("Checked: " + curChecked + " / " + totalLength);
        }

        // Clean injected ctest file after debugging
        ConfigUtils.cleanConfig(injectConfigFile, System.out);
        return res;
    }

    protected Map<String, String> runBatchTogether(Map<String, String> config)
            throws IOException, ParserConfigurationException, TransformerException, InterruptedException {
        LinkedHashMap<String, String> res = new LinkedHashMap<>();
        ConfigUtils.cleanConfig(injectConfigFile, System.out);

        // Inject all the parameters and run test together
        ConfigUtils.injectConfig(injectConfigFile, config, System.out, localDebug);
        curChecked += config.size();
        // If test failed, then debug one by one
        if (!debugger.testPass()) {
            curChecked -= config.size();
            res.putAll(runDebugOneByOne(config));
        }
        // Clean injected ctest file after debugging
        ConfigUtils.cleanConfig(injectConfigFile, System.out);
        getLog().info("Checked: " + curChecked + " / " + totalLength);
        return res;
    }

    protected Map<String, String> runDebugAll(Map<String, String> configAll, int batchSize)
            throws IOException, ParserConfigurationException, InterruptedException, TransformerException {
        if (batchSize <= 0) {
            throw new IllegalArgumentException("Batch size should be positive");
        }
        LinkedHashMap<String, String> res = new LinkedHashMap<>();
        curChecked = 0;
        totalLength = configAll.size();
        // Split config into several maps that each map contains ten elements
        ArrayList<Map<String, String>> configs = new ArrayList<>();
        int i = 0;
        for (Map.Entry<String, String> entry : configAll.entrySet()) {
            if (i % batchSize == 0) {
                configs.add(new LinkedHashMap<>());
            }
            configs.get(i / batchSize).put(entry.getKey(), entry.getValue());
            i++;
        }
        // for all in configs, run runAllTogether
        for (Map<String, String> config : configs) {
            res.putAll(runBatchTogether(config));
        }
        return res;
    }

    @Deprecated
    private Map<String, String> runDebug(Map<String, String> failedConfig)
            throws ParserConfigurationException, TransformerException, IOException, InterruptedException {
        LinkedHashMap<String, String> res = new LinkedHashMap<>(failedConfig);

        // Set a while loop, write configuration into file for debugging
        // TODO: Delta-Debugging can be applied here to speed up
        Iterator<Map.Entry<String, String>> it = res.entrySet().iterator();
        while (it.hasNext()) {
            // Clean injected ctest file before each round
            ConfigUtils.cleanConfig(injectConfigFile, System.out);

            Map.Entry<String, String> cur = it.next();

            // For now each step delete one parameter for debugging
            String curRemoved = cur.getKey();
            Map<String, String> inject = new HashMap<>();
            inject.putAll(res);
            inject.remove(curRemoved);
            ConfigUtils.injectConfig(injectConfigFile, inject, System.out, localDebug);

            // If the test still fail, remove the parameter from the root cause configuration
            if (!debugger.testPass()) {
                it.remove();
            }
        }

        // Clean injected ctest file after debugging
        ConfigUtils.cleanConfig(injectConfigFile, System.out);
        return res;
    }

    @Override
    public List<String> getJavaOptions() throws MojoExecutionException {
        return ConfigurationMojoHelper.getJavaOptions(this, new LinkedList<>(), getProject(),
                                                      getTemporaryDirectory());
    }

    /** Initialize the JVM launcher for debugging. */
    private void init(CampaignConfiguration configuration) throws IOException {
        File outputDirectory = configuration.getOutputDirectory();
        FileUtil.ensureDirectory(outputDirectory);
        List<String> javaOptions = new ArrayList(configuration.getJavaOptions());
        javaOptions.add("-cp");
        String classPath = configuration.getTestClasspathJar().getAbsolutePath() + File.pathSeparator + this.debugFrameworkJar.getAbsolutePath();
        javaOptions.add(classPath);
        String[] arguments = new String[]{configuration.getTestClassName(), configuration.getTestMethodName(), outputDirectory.getAbsolutePath()};
        String DEBUG = System.getProperty("binary.debug");
        if (DEBUG != null && DEBUG.toLowerCase().equals("true")) {
            javaOptions.add(JvmLauncher.DEBUG_OPT + "5005");
        }
        this.launcher = JvmLauncher.fromMain(configuration.getJavaExecutable(), DebugForkMain.class.getName(), (String[])javaOptions.toArray(new String[0]), true, arguments, configuration.getWorkingDirectory(), configuration.getEnvironment());
    }

}
