package edu.illinois.confuzz.internal;

import com.google.gson.Gson;
import edu.berkeley.cs.jqf.fuzz.guidance.Guidance;
import edu.berkeley.cs.jqf.fuzz.junit.GuidedFuzzing;
import edu.neu.ccs.prl.meringue.SystemPropertyUtil;
import org.apache.commons.io.FileUtils;
import org.junit.runner.JUnitCore;
import org.junit.runner.Request;
import org.junit.runner.Result;
import java.io.File;
import java.io.IOException;
import java.util.Map;

public final class FuzzForkMain {
    public static final String TARGET_SUFFIX = "$$CONFUZZ";
    public static final String PROPERTIES_KEY = "confuzz.properties";

    private FuzzForkMain() {
        throw new AssertionError();
    }

    public static void main(String[] args) throws Throwable {
        // Usage: testClassName testMethodName outputDirectory
        try {

            // Add JVM shutdown hook
            Runtime.getRuntime().addShutdownHook(new Thread() {
                @Override
                public void run() {
                    // Save the config map to a file with ConfuzzGenerator.getInjectedConfig();
                    Map<String, Object> configMap = ConfuzzGenerator.getInjectedConfig();
                    // Write the configMap to a json file
                    try {
                        Gson gson = new Gson();
                        String json = gson.toJson(configMap);
                        FileUtils.writeStringToFile(new File(args[2], "crash_configMap.json"), json, "UTF-8");
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            });

            Utils.setMavenGoal("fuzz");
            String testClassName = args[0];
            String testMethodName = args[1] + TARGET_SUFFIX;
            File outputDirectory = new File(args[2]);
            // Note: must set system properties before loading the test class
            SystemPropertyUtil.loadSystemProperties(PROPERTIES_KEY);
            // Init ConfigConstraints
            ConfigConstraints.init();
            // Load the test class
            Class<?> testClass = Class.forName(testClassName, true, FuzzForkMain.class.getClassLoader());
            // Run preliminary round to get default configuration
            runPreliminary(testClass, testMethodName, outputDirectory);
            if (Utils.onlyCheckDefault()) {
                System.exit(0);
            }
            // Run the main campaign
            long seed = System.currentTimeMillis();
            Guidance guidance = new ConfuzzGuidance(testClassName + "#" + testMethodName, null,
                                                    outputDirectory, seed);
            FileUtils.writeStringToFile(new File(outputDirectory, "seed"), String.valueOf(seed), "UTF-8");
            GuidedFuzzing.run(testClass, testMethodName, guidance, System.out);
        } catch (Throwable t) {
            t.printStackTrace();
            System.exit(-1);
        }
    }

    private static void runPreliminary(Class<?> testClass, String testMethodName, File outputDirectory) throws IOException {

        Guidance guidance = new DefConfCollectionGuidance(null);
        try {
            GuidedFuzzing.setGuidance(guidance);
            // Create a JUnit Request
            // Calculate the time used for preliminary round
            long startTime = System.currentTimeMillis();
            // Try the best to make sure (1) default configuration can pass the test; (2) the test is not flaky
            Result res1 =  new JUnitCore().run(Request.method(testClass, testMethodName));
            Result res2 = new JUnitCore().run(Request.method(testClass, testMethodName));
            // Check if both results are successful
            if (!res1.wasSuccessful()) {
                String failureMsg = "First pre round failed.\n";
                if (res1.getFailureCount() > 0) {
                    org.junit.runner.notification.Failure f = res1.getFailures().get(0);
                    failureMsg += f.getMessage() + f.getTrace();
                }
                FileUtils.writeStringToFile(new File(outputDirectory, "preRoundFailure"), failureMsg, "UTF-8");
                throw new RuntimeException("First pre round failed, this test is buggy!");
            }
            if (!res2.wasSuccessful()) {
                String failureMsg = "Second pre round failed.\n";
                if (res2.getFailureCount() > 0) {
                    org.junit.runner.notification.Failure f = res2.getFailures().get(0);
                    failureMsg += f.getMessage() + f.getTrace();
                }
                FileUtils.writeStringToFile(new File(outputDirectory, "preRoundFailure"), failureMsg, "UTF-8");
                throw new RuntimeException("Second pre round failed, this test is flaky!");
            }
            long endTime = System.currentTimeMillis();
            // Calculate the time used for preliminary round in seconds
            long timeUsed = (endTime - startTime) / 1000;
            // write the time used for preliminary round to a file called "preroundtime"
            FileUtils.writeStringToFile(new File(outputDirectory, "preroundtime"), String.valueOf(timeUsed), "UTF-8");
        } finally {
            GuidedFuzzing.unsetGuidance();
        }
    }
}
