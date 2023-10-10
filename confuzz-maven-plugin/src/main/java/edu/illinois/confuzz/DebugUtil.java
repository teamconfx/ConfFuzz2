package edu.illinois.confuzz;

import edu.illinois.confuzz.internal.*;
import edu.illinois.confuzz.internal.Failure;
import edu.neu.ccs.prl.meringue.*;
import org.apache.commons.io.FileUtils;
import org.apache.maven.plugin.logging.Log;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.TransformerException;
import java.io.*;
import java.nio.file.Files;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

public class DebugUtil {

    private static final int MAX_ITERATION = 100;
    public static final File FAILURE_FILE = new File("confuzz-failure.temp");
    private static Process process = null;
    private JvmLauncher launcher;
    private File injectConfigFile;
    private Log logger;
    private Failure expectedFailure;
    private Map<String, String> configMap;
    private List<String> configKeys;
    private int repeatRecursionCounter;
    private int debugIteration;
    private int timeout = 60;
    private final String className;
    private final String methodName;
    public DebugUtil(JvmLauncher launcher, File configFile, Log logger, Failure expectedFailure,
                     Map<String, String> configMap, File outputDirectory, String className, String methodName) {
        this.launcher = launcher;
        this.injectConfigFile = configFile;
        this.logger = logger;
        this.expectedFailure = expectedFailure;
        this.configMap = configMap;
        this.configKeys = new LinkedList<>(configMap.keySet());
        this.debugIteration = 0;
        this.repeatRecursionCounter = 0;
        this.className = className;
        this.methodName = methodName;
        setDebugTimeout(outputDirectory);
    }

    public void setExpectedFailure(Failure error) {
        expectedFailure = error;
    }

    public ReproStatus getReproduceStatus()
            throws IOException, ParserConfigurationException, InterruptedException, TransformerException {
        Failure result = getReproduceResult(configMap);
        return checkReproduceStatus(result, false, null);
    }

    public ReproStatus getReproduceStatus(DebugEntry failure)
            throws IOException, ParserConfigurationException, InterruptedException, TransformerException {
        Failure result = getReproduceResult(configMap);
        return checkReproduceStatus(result, true, failure);
    }

    private ReproStatus getReproduceStatus(Map<String, String> configMap, boolean log)
            throws IOException, ParserConfigurationException, TransformerException, InterruptedException {
        Failure result = getReproduceResult(configMap);
        return checkReproduceStatus(result, log, null);
    }

    public Failure getReproduceResult(Map<String, String> configMap)
            throws IOException, ParserConfigurationException, TransformerException, InterruptedException {
        ConfigUtils.injectConfig(injectConfigFile, configMap, System.out, false);
        Failure result = runWithNewJVM();
        return result;
    }

    /**
     * This method is used to determine whether the failure is reproducible with whole configuration injection.
     * @return
     */
    private ReproStatus checkReproduceStatus(Failure result, boolean log, DebugEntry failure) {
        // If test pass, then the failure is not reproducible
        if (log) {
            logger.info("Expected: " + expectedFailure);
        }
        if (result == null) {
            return ReproStatus.PASS;
        }
        // Compare other properties
        if (hashThrowable(expectedFailure).equals(hashThrowable(result))) {
            return ReproStatus.REPRODUCIBLE;
        }
        if (failure != null) {
            failure.setReplayedFailure(result);
        }
        if (log) {
            logger.info("Replayed: " + result);
        }
        return ReproStatus.DIFFERENT;
    }

    /**
     * This method is used to set the timeout for debug.
     * @param timeoutFilePath
     */
    private void setDebugTimeout(File timeoutFilePath) {
        File timeoutFile = new File(timeoutFilePath, "campaign/preroundtime");
        if (timeoutFile.exists()) {
            try {
                int time = Integer.valueOf(FileUtils.readFileToString(timeoutFile, "UTF-8"));
                this.timeout = time + 60;
            } catch (IOException e) {
                // Do nothing
            }
        }
    }

    public boolean testPass() throws IOException, InterruptedException {
        return runWithNewJVM() == null;
    }

    public class FakeException extends Throwable {
        public FakeException () {
            super();
        }

        public FakeException (Throwable cause) {
            super(cause);
        }
    }

    /**
     * Initialize the JVM launcher to run a single debug session.
     *
     * @return true if the test pass in this round, else false
     * @throws IOException
     * @throws InterruptedException
     */
    public Failure runWithNewJVM() throws IOException, InterruptedException {
        if (process != null) {
            process.destroy();
        }
        process = launcher.launch();
        // We dynamically set the timeout here based on the time of vanilla run (pre-round in fuzzing)
        boolean notTimeout = process.waitFor(this.timeout, TimeUnit.SECONDS);
        if (!notTimeout) {
            logger.info("Exit with Timeout, Test Fail!");
            return new Failure(new TimeoutException("TIMEOUT"));
        }
        try {
            return getResultFromFile();
        } catch (ClassNotFoundException e) {
            return new Failure(new FakeException(e));
        }
    }

    private Failure getResultFromFile() throws IOException, ClassNotFoundException {
        Failure result = null;
        if (FAILURE_FILE.exists()) {
            try (ObjectInputStream in = new ObjectInputStream(Files.newInputStream(FAILURE_FILE.toPath()))) {
                result = (Failure) in.readObject();
            }
            FAILURE_FILE.delete();
        }
        return result;
    }

    protected Map<String, String> runDebugOneByOne(Map<String, String> config)
            throws IOException, ParserConfigurationException, TransformerException, InterruptedException {
        LinkedHashMap<String, String> res = new LinkedHashMap<>();
        //res.putAll(config);
        for (Map.Entry<String, String> entry : config.entrySet()) {
            Map<String, String> inject = new HashMap<>();
            inject.putAll(config);
            inject.remove(entry.getKey());

            // If the test pass or cause a different exception, then this parameter is included in the return value
            if (getReproduceStatus(inject, true) != ReproStatus.REPRODUCIBLE) {
                res.put(entry.getKey(), entry.getValue());
            }
        }

        return res;
    }

    /** ================== Binary Search to find buggy configuration below ==================*/

    /**
     * Use binary search to find the potential bug configuration.
     * @return the potential bug configuration in the format of a map.
     */
    public Map<String, String> searchBuggyConfig(Queue<DebugEntry> entryQueue, Map<String, DebugEntry> entryMap, DebugEntry failure)
            throws IOException, ParserConfigurationException, InterruptedException, TransformerException, AssertionError {
        ReproStatus status = getReproduceStatus();
        if (status == ReproStatus.PASS) {
            // If status is pass then this test is flaky, directly return the whole map
            failure.setStatus(ReproStatus.FLAKY);
            return configMap;
        } else if (status == ReproStatus.DIFFERENT) {
            // Should not reach here because we already override the expected failure.
            failure.setStatus(ReproStatus.DIFFERENT);
        }
        // Reach here means the failure is reproducible or different.
        return searchBuggyConfig(entryQueue, entryMap);
    }

    public Map<String, String> searchBuggyConfig(Queue<DebugEntry> entryQueue, Map<String, DebugEntry> entryMap)
            throws IOException, ParserConfigurationException, InterruptedException, TransformerException, AssertionError{
        Map<String, String> config = binarySearch(entryQueue, entryMap);
        // We may not find the exact configuration that causes the bug when there are multiple parameters.
        if (config.size() == 1) {
            return config;
        }
        // We do not update debugQueue in one by one mode for better performance
        return runDebugOneByOne(config);
    }

    /**
     * Search for the bug configuration using binary search.
     * @return a set of config that may not be precise
     */
    private Map<String, String> binarySearch(Queue<DebugEntry> entryQueue, Map<String, DebugEntry> entryMap)
            throws IOException, ParserConfigurationException, TransformerException, InterruptedException {
        debugIteration = 0;
        repeatRecursionCounter = 0;
        int leftIndex = 0, rightIndex = configKeys.size();
        Map<String, String> resultConfig = configMap;
        while(rightIndex - leftIndex > 1) {
            if (debugIteration++ > MAX_ITERATION || repeatRecursionCounter > 7 ||
                    (repeatRecursionCounter > 3 && rightIndex - leftIndex <= 3)) {
                break;
            }
            // Construct the left and right configuration
            int midIndex = (leftIndex + rightIndex) / 2;
            //System.out.println("LEFT:");
            Map<String, String> leftConfig = new LinkedHashMap<>();
            for (int i = leftIndex; i < midIndex; i++) {
                //System.out.println(configKeys.get(i) + " " + configMap.get(configKeys.get(i)));
                leftConfig.put(configKeys.get(i), configMap.get(configKeys.get(i)));
            }
            //System.out.println("RIGHT:");
            Map<String, String> rightConfig = new LinkedHashMap<>();
            for (int i = midIndex; i < rightIndex; i++) {
                //System.out.println(configKeys.get(i) + " " + configMap.get(configKeys.get(i)));
                rightConfig.put(configKeys.get(i), configMap.get(configKeys.get(i)));
            }


            // Check whether the left and right configuration cause new failure
            ReproStatus leftStatus = checkNewFailure(entryQueue, entryMap, leftConfig);
            ReproStatus rightStatus = checkNewFailure(entryQueue, entryMap, rightConfig);

            // TODO: If both config causes the same failure, there might be some weird stuff involved
            // TODO: is summing up the recursing counter better?
            if (leftStatus == ReproStatus.REPRODUCIBLE) {
                repeatRecursionCounter = 0;
                resultConfig = leftConfig;
                rightIndex = midIndex;
            } else if (rightStatus == ReproStatus.REPRODUCIBLE) {
                repeatRecursionCounter = 0;
                resultConfig = rightConfig;
                leftIndex = midIndex;
            } else {
                // both branch succeeded
                // shuffle the configKeys and retry
                repeatRecursionCounter++;
                shuffleConfigKeys(leftIndex, rightIndex);
            }
        }
        if (rightIndex - leftIndex > 1) {
            logger.warn("Reach max iteration, return the current stage: Iteration times: "
                    + debugIteration + " Buggy size = " + resultConfig.size());
        }
        return resultConfig;
    }

    /**
     * Check whether the left and right config causes different failure.
     * @param entryQueue
     * @param entryMap
     * @param config
     * @return the reproduction status
     * @throws IOException
     * @throws ParserConfigurationException
     * @throws TransformerException
     * @throws InterruptedException
     */
    private ReproStatus checkNewFailure(Queue<DebugEntry> entryQueue,
                                        Map<String, DebugEntry> entryMap, Map<String, String> config)
            throws IOException, ParserConfigurationException, TransformerException, InterruptedException {
        Failure result = getReproduceResult(config);
        ReproStatus status = checkReproduceStatus(result, false, null);
        updateDebugEntry(entryQueue, entryMap, config, result, status);
        return status;
    }

    /**
     * Update the debug entry when left or right config causes different failure.
     * @param entryQueue
     * @param entryMap
     * @param config
     * @param result
     * @param status
     */
    private void updateDebugEntry(Queue<DebugEntry> entryQueue, Map<String, DebugEntry> entryMap,
                                  Map<String, String> config, Failure result, ReproStatus status) {
        if (Boolean.getBoolean("addBinaryDiff") && status == ReproStatus.DIFFERENT &&
                entryMap.size() < ConfigurationDebugNewJvmMojo.maxEntryMapSize &&
                !result.getClass().getName().contains("IllegalArgumentException")) {
            // Check if the entry already exists
            String identifier = hashThrowable(result);
            if (entryMap.containsKey(identifier)) {
                entryMap.get(identifier).addBugConfig(new HashMap<>(config));
            } else {
                // A new failure. Add it to the queue
                DebugEntry debugEntry = new DebugEntry(result);
                // note that here we only set the bug config
                debugEntry.addBugConfig(new HashMap<>(config));
                entryMap.put(identifier, debugEntry);
                entryQueue.add(debugEntry);
            }
        }
    }

    /**
     * Shuffle the config keys when binary search does not successfully find the bug configuration.
     */
    private void shuffleConfigKeys(int leftIndex, int rightIndex) {
        Collections.shuffle(configKeys.subList(leftIndex, rightIndex));
    }

    public int getDebugIteration() {
        return debugIteration;
    }

    /** ================== Printer methods below ==================*/

    public static String stackTraceToString(StackTraceElement[] stackTrace) {
        StringWriter sw = new StringWriter();
        for (StackTraceElement element : stackTrace) {
            sw.append(element.toString());
            sw.append(",");
        }
        return sw.toString();
    }

    public static String getErrorMessage(Throwable e) {
        String message = e.getMessage();
        if (message == null) {
            message = "No message";
        }
        return message.replaceAll("\n", ",").replaceAll("\t", "");
    }

    public static Throwable getRootCause(Throwable e) {
        while (e.getCause() != null) {
            e = e.getCause();
        }
        return e;
    }

    public String hashThrowable(Throwable e) {
        return hashThrowable(e, className);
    }

    public String hashThrowable(Failure e) {
        return hashThrowable(e, className);
    }

    /**
     * Hash a throwable
     * @param e the throwable
     * @param className name of the test class
     * @return unique identifier of a throwable of the form Type + Stacktrace for each cause
     */
    public static String hashThrowable(Throwable e, String className) {
        return hashThrowable(new Failure(e), className);
    }
    public static String hashThrowable(Failure f, String className) {
        /*
         * Corner situations
         * 1. If the throwable is thrown in another thread
         * 2. If the test method is not in the current test class.
         */
        if (f.getFailure().contains("FakeException")) {
            return f.getFailure();
        }
        StringBuilder identifier = new StringBuilder();
        String failureType = f.getFailure();
        identifier.append(failureType).append("\n");
        List<StackTraceElement> stacktrace = ConfuzzGuidance.trimThrowableStacktrace(f.getStackTrace());
        String actualTestName = ConfigurationDebugNewJvmMojo.actualTestName == null ?
                className : ConfigurationDebugNewJvmMojo.actualTestName;
        int bound = stacktrace.size();
        // Find the last line where the test class appears
        while (bound > 0) {
            StackTraceElement trace = stacktrace.get(bound - 1);
            if (trace.getClassName().equals(actualTestName) && !trace.getMethodName().endsWith("$$CONFUZZ")) {
                break;
            }
            bound -= 1;
        }
        // If assertion error, the line containing the test name might be useful
        // If not assertion error, the line can also be useful if it is the on the top of the stack trace
        if (!failureType.equals("java.lang.AssertionError") && bound > 1) {
            bound -= 1;
        }
        // If the test name is still not found, since maybe failure in thread
        if (bound == 0) {
            bound = stacktrace.size();
        }
        for (int i = 0; i < bound; i ++) {
            String desc = stacktrace.get(i).toString();
            int index = desc.indexOf('(');
            if (index != -1 && desc.indexOf('$') != -1) {
                String desc1 = desc.substring(0, index).replaceAll("\\d", "");
                String desc2 = desc.substring(index);
                identifier.append(desc1).append(desc2).append("\n");
            } else {
                identifier.append(desc).append("\n");
            }
        }
        return identifier.toString();
    }
}
