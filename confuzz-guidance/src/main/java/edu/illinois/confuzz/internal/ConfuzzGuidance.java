package edu.illinois.confuzz.internal;

import com.squareup.moshi.Moshi;
import com.squareup.moshi.Types;
import edu.berkeley.cs.jqf.fuzz.guidance.GuidanceException;
import edu.berkeley.cs.jqf.fuzz.guidance.Result;
import edu.berkeley.cs.jqf.fuzz.guidance.TimeoutException;
import edu.berkeley.cs.jqf.fuzz.util.Coverage;
import edu.berkeley.cs.jqf.instrument.tracing.events.TraceEvent;
import edu.illinois.confuzz.internal.junit.TestRunner;
import edu.illinois.confuzz.internal.types.AbstractConfigType;
import edu.illinois.confuzz.internal.types.ConfigType;
import okio.BufferedSink;
import okio.Okio;
import org.junit.runners.model.FrameworkMethod;
import org.junit.runners.model.TestClass;

import java.io.*;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class ConfuzzGuidance extends JQFZestGuidance {
    /**
     * The default fuzzMode is {@link FuzzMode#CONFUZZ}.
     */
    protected FuzzMode fuzzMode;

    /**
     * {@code true} if the input that is currently running has a parent.
     */
    private boolean hasParent = false;

    /**
     * The mutated parameters by the current input.
     */
    protected Set<String> runMutatedParams = new HashSet<>();

    /**
     * The mutated parameters by the parent input.
     */
    protected Set<String> parentMutatedParams = new HashSet<>();

    /**
     * Whether choose mutated parameter set based on parent information.
     */
    protected final boolean CONSIDER_PARENT_INFO = Boolean.getBoolean("confuzz.PARENT_INFO");

    /**
     * The file that stores the initial, min, max size of exercised configuration parameters are stored.
     */
    private String metricSummary = "summary.txt";

    private boolean firstEnterFlag = true;
    private int initCoverage = 0;

    /**
     * List of configs that are ignored. TODO: Might consider making this a running arg later.
     */
    public static Set<String> ignoredConfigs = new HashSet<>(Arrays.asList("fs.permissions.umask-mode",
            "dfs.journalnode.edits.dir.perm", "dfs.namenode.storage.dir.perm", "dfs.datanode.data.dir.perm",
            "yarn.nodemanager.default-container-executor.log-dirs.permissions",
            "mapreduce.jobhistory.intermediate-user-done-dir.permissions",
            "yarn.nodemanager.default-container-executor.log-dirs.permissions",
            "alluxio.security.authorization.permission.umask",
            "hive.scratch.dir.permission"));

    public final static String needClassMethod = "NEEDCLASSMETHOD";

    public ConfuzzGuidance(String testName, Duration duration, File outputDirectory, long seed) throws IOException {
        super(testName, duration, null, outputDirectory, new Random(seed));
        System.setProperty(needClassMethod, "false");
        setFuzzMode(System.getProperty("fuzzMode"));
    }

    /**
     * Set the fuzzing mutation mode in this ConfuzzGuidance.
     * @param mode
     */
    private void setFuzzMode(String mode) {
        switch (mode.toLowerCase()) {
            case "random":
                fuzzMode = FuzzMode.RANDOM;
                break;
            case "jqf":
                fuzzMode = FuzzMode.JQF;
                break;
            case "confuzz":
                fuzzMode = FuzzMode.CONFUZZ;
                break;
            default:
                throw new IllegalArgumentException("Does not support fuzzing mode: " + mode + ". " +
                        "Please choose from random, jqf, and confuzz.");
        }
    }

    @Override
    public void run(TestClass testClass, FrameworkMethod method, Object[] args) throws Throwable {
        new TestRunner(testClass.getJavaClass(), method, args).run();
    }

    @Override
    public boolean hasInput() {
        boolean hasConfig = ConfigTracker.getTotalConfigMap().keySet().stream().filter(
                e -> !ConfigTracker.getSetConfigs().contains(e)
        ).collect(Collectors.toSet()).size() > 0;
        if (!hasConfig) {
            console.printf("No config to fuzz!");
            throw new IllegalArgumentException("No configuration parameter tracked from test. ");
        }
        return super.hasInput();
    }

    @Override
    protected void displayStats(boolean force) {
        Date now = new Date();
        long intervalMilliseconds = now.getTime() - lastRefreshTime.getTime();
        if (intervalMilliseconds < STATS_REFRESH_TIME_PERIOD && !force) {
            return;
        }
        long interlvalTrials = numTrials - lastNumTrials;
        long intervalExecsPerSec = interlvalTrials * 1000L / intervalMilliseconds;
        double intervalExecsPerSecDouble = interlvalTrials * 1000.0 / intervalMilliseconds;
        lastRefreshTime = now;
        lastNumTrials = numTrials;
        long elapsedMilliseconds = now.getTime() - startTime.getTime();
        long execsPerSec = numTrials * 1000L / elapsedMilliseconds;

        String currentParentInputDesc;
        if (seedInputs.size() > 0 || savedInputs.isEmpty()) {
            currentParentInputDesc = "<seed>";
        } else {
            Input currentParentInput = savedInputs.get(currentParentInputIdx);
            currentParentInputDesc = currentParentInputIdx + " ";
            currentParentInputDesc += currentParentInput.isFavored() ? "(favored)" : "(not favored)";
            currentParentInputDesc += " {" + numChildrenGeneratedForCurrentParentInput +
                    "/" + getTargetChildrenForParent(currentParentInput) + " mutations}";
        }

        int nonZeroCount = totalCoverage.getNonZeroCount();
        double nonZeroFraction = nonZeroCount * 100.0 / totalCoverage.size();
        int nonZeroValidCount = validCoverage.getNonZeroCount();
        double nonZeroValidFraction = nonZeroValidCount * 100.0 / validCoverage.size();

        if (console != null) {
            if (LIBFUZZER_COMPAT_OUTPUT) {
                console.printf("#%,d\tNEW\tcov: %,d exec/s: %,d L: %,d\n", numTrials, nonZeroValidCount, intervalExecsPerSec, currentInput.size());
            } else if (!QUIET_MODE) {
                console.printf("\033[2J");
                console.printf("\033[H");
                console.printf(this.getTitle() + "\n");
                if (this.testName != null) {
                    console.printf("Test name:            %s\n", this.testName);
                }

                console.printf("Results directory:    %s\n", this.outputDirectory.getAbsolutePath());
                console.printf("Elapsed time:         %s (%s)\n", millisToDuration(elapsedMilliseconds),
                        maxDurationMillis == Long.MAX_VALUE ? "no time limit" : ("max " + millisToDuration(maxDurationMillis)));
                console.printf("Number of executions: %,d (%s)\n", numTrials,
                        maxTrials == Long.MAX_VALUE ? "no trial limit" : ("max " + maxTrials));
                console.printf("Valid inputs:         %,d (%.2f%%)\n", numValid, numValid * 100.0 / numTrials);
                console.printf("Cycles completed:     %d\n", cyclesCompleted);
                console.printf("Unique failures:      %,d\n", uniqueFailures.size());
                console.printf("Queue size:           %,d (%,d favored last cycle)\n", savedInputs.size(), numFavoredLastCycle);
                console.printf("Current parent input: %s\n", currentParentInputDesc);
                console.printf("Execution speed:      %,d/sec now | %,d/sec overall\n", intervalExecsPerSec, execsPerSec);
                console.printf("Total coverage:       %,d branches (%.2f%% of map)\n", nonZeroCount, nonZeroFraction);
                console.printf("Valid coverage:       %,d branches (%.2f%% of map)\n", nonZeroValidCount, nonZeroValidFraction);
                console.printf("Total param coverage: %,d \n", totalParamCoverage.size());
                console.printf("Valid param coverage: %,d \n", validParamCoverage.size());
            }
        }

        String plotData = String.format("%d, %d, %d, %d, %d, %d, %.2f%%, %d, %d, %d, %.2f, %d, %d, %.2f%%, %d, %d, %d, %d",
                TimeUnit.MILLISECONDS.toSeconds(now.getTime()), cyclesCompleted, currentParentInputIdx,
                numSavedInputs, 0, 0, nonZeroFraction, uniqueFailures.size(), 0, 0, intervalExecsPerSecDouble,
                numValid, numTrials-numValid, nonZeroValidFraction, nonZeroCount, nonZeroValidCount,
                totalParamCoverage.size(), validParamCoverage.size());
        appendLineToFile(statsFile, plotData);
    }


    protected void writeDebugDataToFile(File debugFile, Throwable error) throws IOException {
        DebugData.writeDebugData(debugFile, error, ((ConfuzzInput) currentInput).injectedConfig,
                currentDiffConfig());
    }

    protected Map<String, Object> currentDiffConfig() {
        if (hasParent) {
            return ConfigUtils.childDiffParent(((ConfuzzInput) currentInput).injectedConfig,
                    ((ConfuzzInput) savedInputs.get(currentParentInputIdx)).injectedConfig);
        }
        return ((ConfuzzInput) currentInput).injectedConfig;
    }

    protected void writeCurrentConfigToFile(File configFile) throws IOException {
        List<Map<String, String>> maps = new LinkedList<>();
        if (hasParent) {
            maps.add(ConfigUtils.convertFromObjectToStringMap(
                    ((ConfuzzInput) savedInputs.get(currentParentInputIdx)).injectedConfig));
        }
        maps.add(ConfigUtils.convertFromObjectToStringMap(
                ((ConfuzzInput) currentInput).injectedConfig));
        Moshi MOSHI = new Moshi.Builder().build();
        try (BufferedSink sink = Okio.buffer(Okio.sink(configFile))) {
            MOSHI.adapter(Types.newParameterizedType(List.class, Map.class))
                 .indent("    ")
                 .toJson(sink, maps);
        }
    }


    /**
     * Update the summary file with the current configuration context.
     * Update info:
     *  - minConfigSize
     *  - maxConfigSize
     *  - num_of_executed_rounds
     *  - num_of_valid_rounds
     *  - unique_failures
     *  - init_coverage
     *  - total_coverage
     *  - total_fuzzing_time
     * @param outputDir
     */
    private void updateSummaryInfoToFile(File outputDir) throws GuidanceException {
        int branchCoverageCount = totalCoverage.getNonZeroCount();
        if (firstEnterFlag && branchCoverageCount > 0) {
            firstEnterFlag = false;
            initCoverage = branchCoverageCount;
        }
        // Write the following three variables to the file
        int maxConfigSize = ConfigTracker.getMaxConfigMapSize();
        int minConfigSize = ConfigTracker.getMinConfigMapSize();
        try {
            // Create the file to write the config size info
            File summaryFile = new File(outputDir, metricSummary);
            FileWriter writer = new FileWriter(summaryFile, true);
            // writer.write("minConfigSize,maxConfigSize,num_of_executed_rounds,num_of_valid_rounds,unique_failures,init_coverage,total_coverage,total_param_coverage,fuzzing_time,fuzzing_mode\n");
            writer.write(minConfigSize + "," + maxConfigSize + "," + numTrials + "," + numValid + "," + uniqueFailures.size() + "," + initCoverage + ","
                    + branchCoverageCount + "," + totalParamCoverage.size() + "," + millisToDuration(new Date().getTime() - startTime.getTime()) + "," + fuzzMode + "\n");
            writer.close();
        } catch (IOException e) {
            e.printStackTrace();
            throw new GuidanceException("Failed to append summary info to file");
        }
    }

    @Override
    protected String getTitle() {
        switch (fuzzMode) {
            case CONFUZZ:
                return "Semantic Fuzzing Configuration Parameter With Confuzz\n" +
                        "-----------------------------------------------------\n";
            case JQF:
                return "Semantic Fuzzing Configuration Parameter With JQF\n" +
                        "-----------------------------------------------------\n";
            case RANDOM:
                return "Semantic Fuzzing Configuration Parameter With Random\n" +
                        "-----------------------------------------------------\n";
            default:
                return "Semantic Fuzzing Configuration Parameter With " + fuzzMode +
                        "\n-----------------------------------------------------\n";
        }
    }


    // ========= Structural Fuzzing on Configuration Parameter level =========

    /** Parameter Coverage statistics for a single run -- which parameter are covered by this run */
    protected ParamCoverage runParamCoverage = new ParamCoverage();

    /** Cumulative parameter coverage statistics -- which parameter are covered by previous rounds */
    protected ParamCoverage totalParamCoverage = new ParamCoverage();

    /** Cumulative parameter coverage statistics -- which parameter are covered by previous valid rounds */
    protected ParamCoverage validParamCoverage = new ParamCoverage();

    /** The maximum number of parameter covered by any single input found so far. */
    protected int maxParamCoverage = 0;

    /** A mapping of coverage keys to inputs that are responsible for them. */
    protected Map<Object, Input> paramResponsibleInputs = new HashMap<>(totalParamCoverage.size());

    // TODO: The logic of protected Set<List<StackTraceElement>> uniqueFailures must also be changed here


    // TODO: Low-priority: improve the fuzz.log so that all coverage information is correctly updated and later for evaluation
    //  it is inside  this.logFile = new File(outputDirectory, "fuzz.log"); and this.coverageFile = new File(outputDirectory, "coverage_hash");

    @Override
    protected void updateCoverageFile() {
        try {
            PrintWriter pw = new PrintWriter(coverageFile);
            pw.println("Code Coverage:");
            pw.println(getTotalCoverage().toString());
            pw.println("Hash code: " + getTotalCoverage().hashCode());
            // TODO: Here also need to update the parameter coverage
            pw.println();
            pw.println("Parameter Coverage:");
            pw.println(totalParamCoverage.toString());


            pw.close();
        } catch (FileNotFoundException ignore) {
            throw new GuidanceException(ignore);
        }
    }

    @Override
    protected void handleEvent(TraceEvent e) {
        conditionallySynchronize(multiThreaded, () -> {
            // Collect totalCoverage
            runCoverage.handleEvent(e);
            // Check for possible timeouts every so often
            if (this.singleRunTimeoutMillis > 0 &&
                    this.runStart != null && (++this.branchCount) % 10_000 == 0) {
                long elapsed = new Date().getTime() - runStart.getTime();
                if (elapsed > this.singleRunTimeoutMillis) {
                    throw new TimeoutException(elapsed, this.singleRunTimeoutMillis);
                }
            }
        });
    }

    @Override
    protected int getTargetChildrenForParent(Input parentInput) {
        // Baseline is a constant
        int target = NUM_CHILDREN_BASELINE;

        // We like inputs that cover many things, so scale with fraction of max branch coverage and max parameter coverage
        if (maxCoverage > 0 && maxParamCoverage > 0) {
            double cov_fraction = parentInput.nonZeroCoverage / (double) maxCoverage;
            double param_fraction = parentInput.paramCoverage.size() / (double) maxParamCoverage;
            target = (int) (NUM_CHILDREN_BASELINE * cov_fraction * param_fraction);
        }

        // We absolutely love favored inputs, so fuzz them more
        if (parentInput.isFavored()) {
            target = target * NUM_CHILDREN_MULTIPLIER_FAVORED;
        }

        return target;
    }

    @Override
    /** Handles the end of fuzzing cycle (i.e., having gone through the entire queue) */
        protected void completeCycle() {
        // Increment cycle count
        cyclesCompleted++;
        infoLog("\n# Cycle " + cyclesCompleted + " completed.");

        // Go over all inputs and do a sanity check (plus log)
        infoLog("Here is a list of favored inputs:");
        int sumResponsibilities = 0;
        numFavoredLastCycle = 0;
        for (Input input : savedInputs) {
            if (input.isFavored()) {
                int responsibleFor = input.responsibilities.size();
                infoLog("Input %d is responsible for %d branches", input.id, responsibleFor);
                sumResponsibilities += responsibleFor;
                numFavoredLastCycle++;
            }
        }
        int totalCoverageCount = totalCoverage.getNonZeroCount();
        int validCoverageCount = validCoverage.getNonZeroCount();
        infoLog("Total %d branches covered", totalCoverageCount);
        infoLog("Valid %d branches covered", validCoverageCount);
        infoLog("Total %d parameter covered", totalParamCoverage.size());
        infoLog("Valid %d parameter covered", validParamCoverage.size());
        // Here because we only save input for valid ones, so the responsibility should be equal to the
        // valid branch coverage + valid parameter coverage
        if (sumResponsibilities != validCoverageCount + validParamCoverage.size()) {
            if (multiThreaded) {
                infoLog("Warning: other threads are adding coverage between test executions");
            } else {
                throw new AssertionError("Responsibilty mismatch");
            }
        }

        // Break log after cycle
        infoLog("\n\n\n");
    }


    @Override
    public synchronized InputStream getInput() throws GuidanceException {
        conditionallySynchronize(multiThreaded, () -> {
            // Seeds and randomly generated inputs have no parent
            hasParent = seedInputs.isEmpty() && !savedInputs.isEmpty();

            // Clear coverage stats for this run
            runCoverage.clear();
            runParamCoverage.clear();

            // Choose an input to execute based on state of queues
            if (!seedInputs.isEmpty()) {
                // First, if we have some specific seeds, use those
                currentInput = seedInputs.removeFirst();
                parentMutatedParams = currentInput.mutatedParams;
                // Hopefully, the seeds will lead to new coverage and be added to saved inputs

            } else if (savedInputs.isEmpty()) {
                // If no seeds given try to start with something random
                if (!blind && numTrials > 100_000) {
                    throw new GuidanceException("Too many trials without coverage; " +
                            "likely all assumption violations");
                }

                // Make fresh input using either list or maps
                // infoLog("Spawning new input from thin air");
                currentInput = createFreshInput();
                parentMutatedParams = null;
            } else {
                // TODO: change this to non-sequential ?
                // The number of children to produce is determined by how much of the coverage
                // pool this parent input hits
                Input currentParentInput = savedInputs.get(currentParentInputIdx);
                int targetNumChildren = getTargetChildrenForParent(currentParentInput);
                if (numChildrenGeneratedForCurrentParentInput >= targetNumChildren) {
                    // Select the next saved input to fuzz
                    currentParentInputIdx = (currentParentInputIdx + 1) % savedInputs.size();

                    // Count cycles
                    if (currentParentInputIdx == 0) {
                        completeCycle();
                    }

                    numChildrenGeneratedForCurrentParentInput = 0;
                }
                Input parent = savedInputs.get(currentParentInputIdx);
                parentMutatedParams = parent.mutatedParams;

                // Fuzz it to get a new input
                // infoLog("Mutating input: %s", parent.desc);
                currentInput = parent.fuzz(random);
                numChildrenGeneratedForCurrentParentInput++;

                // Write it to disk for debugging
                try {
                    writeCurrentInputToFile(currentInputFile);
                } catch (IOException ignore) {
                }

                // Start time-counting for timeout handling
                this.runStart = new Date();
                this.branchCount = 0;
            }
        });

        return createParameterStream();

    }

    protected void updateResponsibility(Set<Object> responsibilities) {
        // Fourth, assume responsibility for branches
        currentInput.responsibilities = responsibilities;
        for (Object b : responsibilities) {
            // If there is an old input that is responsible,
            // subsume it
            Input oldResponsible = responsibleInputs.get(b);
            if (oldResponsible != null) {
                oldResponsible.responsibilities.remove(b);
                // infoLog("-- Stealing responsibility for %s from input %d", b, oldResponsible.id);
            } else {
                // infoLog("-- Assuming new responsibility for %s", b);
            }
            // We are now responsible
            responsibleInputs.put(b, currentInput);
        }
    }

    @Override
    protected void saveCurrentInput(Set<Object> responsibilities, String why) throws IOException {
        // TODO: logic for easier reuse other tests' interesting input? If we want to do this, we probably need to not only
        //  record the index, but we also need to store the parameter-byte mapping when we store the seed into file, so that
        //  later we can restore the parameter-byte mapping with the file.

        // TODO: How to update runParamCoverage here since there is no such field in Input?
        // First, save to disk (note: we issue IDs to everyone, but only write to disk if valid)
        int newInputIdx = numSavedInputs++;
        String saveFileName = String.format("id_%06d", newInputIdx);
        String how = currentInput.desc;
        File saveFile = new File(savedCorpusDirectory, saveFileName);
        writeCurrentInputToFile(saveFile);
        infoLog("Saved - %s %s %s", saveFile.getPath(), how, why);

        // If not using guidance, do nothing else
        if (blind) {
            return;
        }

        // Second, save to queue
        savedInputs.add(currentInput);

        // Third, store basic book-keeping data
        currentInput.id = newInputIdx;
        currentInput.saveFile = saveFile;
        currentInput.coverage = new Coverage(runCoverage);
        currentInput.paramCoverage = new ParamCoverage(runParamCoverage);
        currentInput.nonZeroCoverage = runCoverage.getNonZeroCount();
        currentInput.offspring = 0;
        currentInput.mutatedParams = new HashSet<>();
        currentInput.mutatedParams.addAll(runMutatedParams);
        savedInputs.get(currentParentInputIdx).offspring += 1;

        // Fourth, assume responsibility for branches
        updateResponsibility(responsibilities);
    }


    @Override
    public void handleResult(Result result, Throwable error) throws GuidanceException {
        conditionallySynchronize(multiThreaded, () -> {
            updateSummaryInfoToFile(outputDirectory);
            // Stop timeout handling
            this.runStart = null;

            // Increment run count
            this.numTrials++;

            boolean valid = result == Result.SUCCESS;

            if (valid) {
                // Increment valid counter
                numValid++;
            }

            // Only need to collect parameter coverage after the execution is finished
            runParamCoverage.collectParamCoverage();
            for (Map.Entry<String, Object> entry: ConfigTracker.getTotalConfigMap().entrySet()) {
                if (ConfigTracker.getSetConfigs().contains(entry.getKey()) && ParamTree.contains(entry.getKey())) {
                    ParamTree.remove(entry.getKey());
                } else {
                    ParamTree.insert(entry.getKey());
                }
            }

            if (result == Result.SUCCESS || (result == Result.INVALID && !SAVE_ONLY_VALID)) {

                // Compute a list of keys for which this input can assume responsibility.
                // Newly covered branches are always included.
                // Existing branches *may* be included, depending on the heuristics used.
                // A valid input will steal responsibility from invalid inputs
                Set<Object> responsibilities = computeResponsibilities(valid);

                // Determine if this input should be saved
                List<String> savingCriteriaSatisfied = checkSavingCriteriaSatisfied(result);
                boolean toSave = savingCriteriaSatisfied.size() > 0;

                if (toSave) {
                    // If a new parameter is accessed by the current input, we must extend the input to include it
                    if (savingCriteriaSatisfied.contains("+new_param")) {
                        // Extend the input to include the new parameter
                        Set<String> currentSet = ((ConfuzzInput) currentInput).paramByteIndexMap.keySet();
                        for (Map.Entry<String, Object> entry: ConfigTracker.getTotalConfigMap().entrySet()) {
                            if (!currentSet.contains(entry.getKey())) {
                                // entry.getKey is a new one
                                ConfigType type = ConfParamGenerator.register(entry.getKey(), entry.getValue());
                                ((ConfuzzInput) currentInput).extendInput(((AbstractConfigType) type).byteNum(),
                                        entry.getKey());
                            }
                        }

                    }

                    String why = String.join(" ", savingCriteriaSatisfied);

                    ((ConfuzzInput) currentInput).updateAccessedParam();

                    // Trim input (remove unused keys)
                    currentInput.gc();

                    // It must still be non-empty
                    assert (currentInput.size() > 0) : String.format("Empty input encountered");

                    // libFuzzerCompat stats are only displayed when they hit new coverage
                    if (LIBFUZZER_COMPAT_OUTPUT) {
                        displayStats(false);
                    }

                    infoLog("Saving new input (at run %d): " +
                                    "input #%d " +
                                    "of size %d; " +
                                    "reason = %s",
                            numTrials,
                            savedInputs.size(),
                            currentInput.size(),
                            why);

                    // Save input to queue and to disk
                    final String reason = why;

                    File saveFile = CoverageInput.getSaveFile(savedCorpusDirectory, numSavedInputs);
                    // numSavedInputs increase by 1 in saveCurrentInput
                    GuidanceException.wrap(() -> saveCurrentInput(responsibilities, reason));
                    GuidanceException.wrap(() -> CoverageInput.writeCoverageInput(saveFile, currentInput,
                            ConfuzzGenerator.getInjectedConfigMap(), numTrials));

                    // Update coverage information
                    updateCoverageFile();
                }
            } else if (result == Result.FAILURE || result == Result.TIMEOUT) {
                String msg = error.getMessage();

                // We should also increase the totalCoverage but not validCoverage
                checkSavingCriteriaSatisfied(result);
                // TODO: if we do not save the failed input, there is no need to update responsibility
                // Set<Object> responsibilities = computeResponsibilities(valid);
                // updateResponsibility(responsibilities);

                // Get the root cause of the failure
                Throwable rootCause = error;
                while (rootCause.getCause() != null) {
                    rootCause = rootCause.getCause();
                }

                rootCause = trimThrowableStacktrace(rootCause);

                // Attempt to add this to the set of unique failures
                if (uniqueFailures.add(Arrays.asList(rootCause.getStackTrace()))) {

                    // Trim input (remove unused keys)
                    currentInput.gc();

                    // It must still be non-empty
                    assert (currentInput.size() > 0) : String.format("Empty input encountered");

                    // Save crash to disk
                    int crashIdx = uniqueFailures.size() - 1;
                    String saveFileName = String.format("id_%06d", crashIdx);
                    File saveFile = new File(savedFailuresDirectory, saveFileName);
                    GuidanceException.wrap(() -> writeCurrentInputToFile(saveFile));

                    // Save debug data and JSON config to disk
                    String configFileName = String.format("config_%06d.json", crashIdx);
                    File configFile = new File(savedFailuresDirectory, configFileName);
                    GuidanceException.wrap(() -> writeCurrentConfigToFile(configFile));

                    File debugFile = DebugData.getDebugFile(savedFailuresDirectory, crashIdx);
                    GuidanceException.wrap(() -> writeDebugDataToFile(debugFile, error));

                    // Save coverage input to disk
                    File coverageInputFile = CoverageInput.getSaveFile(savedFailuresDirectory, crashIdx);
                    GuidanceException.wrap(() -> CoverageInput.writeCoverageInput(coverageInputFile, currentInput,
                            ConfuzzGenerator.getInjectedConfigMap(), numTrials));

                    infoLog("%s", "Found crash: " + error.getClass() + " - " + (msg != null ? msg : ""));
                    String why = result == Result.FAILURE ? "+crash" : "+hang";
                    infoLog("Saved - %s %s", saveFile.getPath(), why);

                    if (EXACT_CRASH_PATH != null && !EXACT_CRASH_PATH.equals("")) {
                        File exactCrashFile = new File(EXACT_CRASH_PATH);
                        GuidanceException.wrap(() -> writeCurrentInputToFile(exactCrashFile));
                    }

                    // libFuzzerCompat stats are only displayed when they hit new coverage or crashes
                    if (LIBFUZZER_COMPAT_OUTPUT) {
                        displayStats(false);
                    }
                }
            }

            // displaying stats on every interval is only enabled for AFL-like stats screen
            if (!LIBFUZZER_COMPAT_OUTPUT) {
                displayStats(false);
            }

            // Save input unconditionally if such a setting is enabled
            if (LOG_ALL_INPUTS && (SAVE_ONLY_VALID ? valid : true)) {
                File logDirectory = new File(allInputsDirectory, result.toString().toLowerCase());
                String saveFileName = String.format("id_%09d", numTrials);
                File saveFile = new File(logDirectory, saveFileName);
                GuidanceException.wrap(() -> writeCurrentInputToFile(saveFile));
            }
        });
    }

    /**
     * Remove stack trace elements that has "jdk.internal.reflect" in it
     * This is a hacky fix for sometimes "jdk.internal.reflect" shows up in the stack trace differently.
     */
    public static Throwable trimThrowableStacktrace(Throwable e) {
        e.setStackTrace(trimThrowableStacktrace(e.getStackTrace()).toArray(new StackTraceElement[0]));
        return e;
    }
    public static List<StackTraceElement> trimThrowableStacktrace(StackTraceElement[] stackTrace) {
        List<StackTraceElement> stackTraceList = new ArrayList<>();
        for (StackTraceElement stackTraceElement : stackTrace) {
            if (!stackTraceElement.getClassName().contains("jdk.internal.reflect")) {
                stackTraceList.add(stackTraceElement);
            }
        }
        return stackTraceList;
    }

    /**
     * Records 1)injected configuration map for debug goal; 2)generated configuration object for coverage goal.
     */
    @Override
    public void observeGeneratedArgs(Object[] args) {
        ((ConfuzzInput) currentInput).injectedConfig.putAll(ConfuzzGenerator.getInjectedConfig());
        ((ConfuzzInput) currentInput).configObjectForCoverage = ConfuzzGenerator.getInjectedConfigMap();
    }

    @Override
    protected List<String> checkSavingCriteriaSatisfied(Result result) {
        List<String> reasonsToSave = new ArrayList<>();

        // Update total parameter coverage
        boolean paramCoverageUpdated = totalParamCoverage.updateParamCoverage(runParamCoverage);
        if (result == Result.SUCCESS) {
            validParamCoverage.updateParamCoverage(runParamCoverage);
        }

        // Update max param coverage
        if (totalParamCoverage.size() > maxParamCoverage) {
            maxParamCoverage = totalParamCoverage.size();
        }

        // save input due to new param coverage
        if (paramCoverageUpdated) {
            reasonsToSave.add("+new_param");
        }

        reasonsToSave.addAll(super.checkSavingCriteriaSatisfied(result));
        return reasonsToSave;
    }

    @Override
    protected Set<Object> computeResponsibilities(boolean valid) {
        // TODO: Not only for branch coverage, but also need to compute the parameter responsibility
        Set<Object> result = new HashSet<>();

        // This input is responsible for all new coverage
        Collection<?> newParamCoverage = runParamCoverage.computeNewParamCoverage(totalParamCoverage);
        if (newParamCoverage.size() > 0) {
            result.addAll(newParamCoverage);
        }

        // If valid, this input is responsible for all new valid coverage
        if (valid) {
            Collection<?> newValidParamCoverage = runParamCoverage.computeNewParamCoverage(validParamCoverage);
            if (newValidParamCoverage.size() > 0) {
                result.addAll(newValidParamCoverage);
            }
        }

        result.addAll(super.computeResponsibilities(valid));
        return result;
    }

    /** ConfuzzInput maintains:
     *  1) a map of parameter name and its byte indexes;
     *  2) a map of parameter name and its byte values.
     *  The two maps are useful for structural fuzzing on configuration parameter level
     *  since we can control which specific parameter to mutate and what value to mutate to.
     */
    public class ConfuzzInput extends LinearInput {
        /** A map that contains parameter name and its byte indexes */
        protected Map<String, ArrayList<Integer>> paramByteIndexMap;
        /** A string that contains the byte values of the input */
        protected String debugByteString = "";
        protected final Map<String, Object> injectedConfig = new LinkedHashMap<>();
        /** A list that contains the names of the configuration files that are accessed
         *  Accessed means get but not set. */
        protected final List<String> accessedConfigs = new ArrayList<>();
        protected final boolean defaultFlag;

        /** The config object that is used for later coverage computation */
        protected Object configObjectForCoverage = null;

        public ConfuzzInput() {
            super();
            this.paramByteIndexMap = new LinkedHashMap<>();
            this.defaultFlag = false;
        }

        // This is used by fuzz() when bytes are either mutated or reused
        public ConfuzzInput(ConfuzzInput other) {
            super(other);
            this.debugByteString = "";
            this.paramByteIndexMap = deepCopyMap(other.paramByteIndexMap);
            this.defaultFlag = false;
        }
        public ConfuzzInput(boolean flag) {
            super();
            this.paramByteIndexMap = new LinkedHashMap<>();
            this.defaultFlag = flag;
        }

        @Override
        public int getOrGenerateFresh(Integer Index, Random random) {
            int size = values.size();
            int val = super.getOrGenerateFresh(Index, random);
            // Record the parameter name and its byte index and value
            String curGenParamName = ConfParamGenerator.getCurGenConfParam();
            // if values.size() > size, it means that there is a new byte generated
            if (values.size() > size) {
                recordIndexForParam(curGenParamName, Index);
                if (defaultFlag) {
                    values.set(values.size() - 1, 0);
                    val = 0;
                }
            }
            this.debugByteString += val + " ";
            return val;
        }

        public void extendInput(int n, String param) {
            if (paramByteIndexMap.containsKey(param)) {
                throw new GuidanceException("Cannot extend input for the same parameter twice");
            }
            ArrayList<Integer> indexes = new ArrayList<>();
            for (int i = 0; i < n; i++) {
                indexes.add(values.size());
                values.add(0);
            }
            paramByteIndexMap.put(param, indexes);
            requested += n;
        }

        public String getParamKeyByIndex(int index) {
            for (Map.Entry<String, ArrayList<Integer>> entry : paramByteIndexMap.entrySet()) {
                if (entry.getValue().contains(index)) {
                    return entry.getKey();
                }
            }
            return null;
        }

        // Deep copy the Map<String, ArrayList<Integer>>
        private Map<String, ArrayList<Integer>> deepCopyMap(Map<String, ArrayList<Integer>> map) {
            Map<String, ArrayList<Integer>> newMap = new LinkedHashMap<>();
            for (Map.Entry<String, ArrayList<Integer>> entry : map.entrySet()) {
                // deep copy the ArrayList<Integer>
                ArrayList<Integer> newList = new ArrayList<>();
                newList.addAll(entry.getValue());
                newMap.put(entry.getKey(), newList);
            }
            return newMap;
        }

        private Set<String> getFuzzMutationParamSet(ConfuzzInput newInput) {
            runMutatedParams.clear();

            // TODO: change the policy of how many parameters to mutate
            //  For now I hardcode the value with 2 to make sure everytime it only mutate one parameter for testing
            // We set here the number of parameters to mutate to be 2 since we observe that almost all the dependencies are 2
            int numParamMutations = sampleGeometric(random, Math.max(2,
                    ((ConfuzzInput) savedInputs.get(currentParentInputIdx)).accessedConfigs.size() * 0.1));
            //maxParamMutations = 5;

            Set<String> mutatedParams = new HashSet<>();
            if (CONSIDER_PARENT_INFO && parentMutatedParams != null && !parentMutatedParams.isEmpty()){
                // TODO: Logic to mutate based on the information from parent
                // TODO: select the subset from the parent to mutate instead of based on the whole set
            } else {
                Set<String> paramNameSet = new LinkedHashSet<>(newInput.paramByteIndexMap.keySet());
                paramNameSet.removeAll(ConfigTracker.getSetConfigs());
                paramNameSet.removeAll(ConfParamGenerator.getDullConfigs());
                List<String> paramNames = new ArrayList<>(paramNameSet);
                // TODO: if no stuff to mutate throw an error later
                // TODO: can optimize the efficiency here by ejecting or selecting
                if (numParamMutations > 1) {
                    if (numParamMutations > paramNames.size()) {
                        numParamMutations = paramNames.size();
                    }
                    List<Integer> range = IntStream.range(0, paramNames.size())
                            .boxed().collect(Collectors.toList());
                    Collections.shuffle(range, random);
                    for (int i = 0; i < numParamMutations; i++) {
                        mutatedParams.add(paramNames.get(range.get(i)));
                    }
                } else {
                    mutatedParams.add(paramNames.get(random.nextInt(paramNames.size())));
                }
            }

//            else {
//                // print out for debug
//                String mutatedParamsStr = "Mutate: ";
//                for (String param : mutatedParams) {
//                    mutatedParamsStr += param + " ";
//                }
//                LogUtils.println(mutatedParamsStr + "\n");
//            }
            runMutatedParams.addAll(mutatedParams);
            mutatedParams.addAll(ParamTree.sample(random, mutatedParams, accessedConfigs));
            if (mutatedParams.isEmpty()) {
                LogUtils.println("No parameter to mutate");
            } else {
                if (LogUtils.print) {
                    // print out for debug
                    StringBuilder sb = new StringBuilder().append("Mutate: ");
                    for (String param : mutatedParams) {
                        sb.append(param).append(" ");
                    }
                    LogUtils.println(sb.append("\n").toString());
                }
            }
            return mutatedParams;
        }

        public void updateAccessedParam() {
            accessedConfigs.addAll(ConfigTracker.getCurConfigMap().keySet());
        }

        /**
         * Fuzz the input with different mutation strategy
         * @param random
         * @return
         */
        @Override
        public Input fuzz(Random random) {
            // Clone this input to create initial version of new child
            ConfuzzInput newInput = new ConfuzzInput(this);
            switch (fuzzMode) {
                case CONFUZZ:
                    LogUtils.println("Fuzz with Confuzz fuzzing strategy\n");
                    if (random.nextInt(10) == 0) {// one out of 10 times
                        fuzzWithRandom(newInput);
                    } else {
                        fuzzWithConfuzz(newInput);
                    }
                    break;
                case RANDOM:
                    LogUtils.println("Fuzz with Random fuzzing strategy\n");
                    fuzzWithRandom(newInput);
                    break;
                case JQF:
                    LogUtils.println("Fuzz with JQF fuzzing strategy\n");
                    fuzzWithJQF(newInput);
                    break;
                default:
                    throw new RuntimeException("Unknown fuzz mode: " + fuzzMode);
            }
            return newInput;
        }

        /**
         * Random byte mutation strategy: randomly mutate the byte values
         * @param newInput
         */
        private void fuzzWithRandom(ConfuzzInput newInput) {
            for (int i = 0; i < newInput.values.size(); i++) {
                newInput.values.set(i, random.nextInt(256));
            }
        }

        /**
         * Confuzz parameter-level mutation strategy
         * @param newInput
         */
        private void fuzzWithConfuzz(ConfuzzInput newInput) {
            int paramNum = newInput.paramByteIndexMap.size();

            boolean setToZero = random.nextDouble() < 0.1; // one out of 10 times
            Set<String> mutatedParams = getFuzzMutationParamSet(newInput);

            // Stack a bunch of mutations
            newInput.desc += ",havoc:" + mutatedParams.size();

            for (String paramToBeMutated : mutatedParams) {
                ArrayList<Integer> paramByteIndexes = newInput.paramByteIndexMap.get(paramToBeMutated);
                int paramStartByteIndex = paramByteIndexes.get(0);
                int paramEndByteIndex = paramByteIndexes.get(paramByteIndexes.size() - 1);
                int paramByteSize = paramEndByteIndex - paramStartByteIndex + 1;
                // Mutate a random set of bits
                // TODO: may change this mean_mutation_size * 8 here
                int mutationSize = Math.min(sampleGeometric(random, MEAN_MUTATION_SIZE * 8),
                        paramByteSize * 8);
                byte[] randomness = new byte[(mutationSize / 8) + 1];
                random.nextBytes(randomness);

                // TODO: Maybe cache this somewhere
                List<Integer> range = IntStream.range(0, paramByteSize * 8)
                        .boxed().collect(Collectors.toList());
                Collections.shuffle(range, random);

                for (int i = 0; i < mutationSize; i++) {
                    int bitIndex = range.get(i);
                    int byteIndex = bitIndex / 8;
                    int bitOffset = bitIndex % 8;
                    int mask = 1 << bitOffset;
                    int mutatedBit = setToZero ? 0 : randomness[i / 8] & mask;
                    int mutatedValue = (newInput.values.get(paramStartByteIndex + byteIndex) & ~mask) | mutatedBit;
                    newInput.values.set(paramStartByteIndex + byteIndex, mutatedValue);
                }
            }

            // update the debug string
            /*newInput.debugByteString = newInput.values.toString();
            for (int i = 0; i < newInput.values.size(); i++) {
                newInput.debugByteString += newInput.values.get(i) + " ";
            }*/
        }

        /**
         * JQF byte-level mutation strategy
         * @param newInput
         */
        private void fuzzWithJQF(ConfuzzInput newInput) {
            // Stack a bunch of mutations
            int numMutations = sampleGeometric(random, MEAN_MUTATION_COUNT);
            newInput.desc += ",havoc:"+numMutations;

            boolean setToZero = random.nextDouble() < 0.1; // one out of 10 times

            for (int mutation = 1; mutation <= numMutations; mutation++) {

                // Select a random offset and size
                int offset = random.nextInt(newInput.values.size());
                int mutationSize = sampleGeometric(random, MEAN_MUTATION_SIZE);

                // desc += String.format(":%d@%d", mutationSize, idx);

                // Mutate a contiguous set of bytes from offset
                for (int i = offset; i < offset + mutationSize; i++) {
                    // Don't go past end of list
                    if (i >= newInput.values.size()) {
                        break;
                    }

                    // Otherwise, apply a random mutation
                    int mutatedValue = setToZero ? 0 : random.nextInt(256);
                    newInput.values.set(i, mutatedValue);
                }
            }
        }

        private void recordIndexForParam(String paramName, Integer Index) {
            if (paramByteIndexMap.containsKey(paramName)) {
                paramByteIndexMap.get(paramName).add(Index);
            } else {
                ArrayList<Integer> indexList = new ArrayList<>();
                indexList.add(Index);
                paramByteIndexMap.put(paramName, indexList);
            }
        }

        public Map<String, ArrayList<Integer>> getPramByteIndexMap() {
            return this.paramByteIndexMap;
        }

        private Map<String, ArrayList<Integer>> copyOnlyKey(Map<String, ArrayList<Integer>> map) {
            Map<String, ArrayList<Integer>> newMap = new LinkedHashMap<>();
            for (Map.Entry<String, ArrayList<Integer>> entry : map.entrySet()) {
                newMap.put(entry.getKey(), new ArrayList<>());
            }
            return newMap;
        }

    }

    /** Spawns a new input from thin air (i.e., actually random) */
    @Override
    protected Input<?> createFreshInput() {
        if (fuzzMode == FuzzMode.CONFUZZ) {
            return new ConfuzzInput(true);
        } else {
            return new ConfuzzInput();
        }
    }


    /**
     * Returns an InputStream that delivers parameters to the generators.
     *
     * Note: The variable `currentInput` has been set to point to the input
     * to mutate.
     *
     * @return an InputStream that delivers parameters to the generators
     */
    protected InputStream createParameterStream() {
        // Return an input stream that reads bytes from a linear array
        return new InputStream() {
            int bytesRead = 0;

            @Override
            public int read() throws IOException {
                assert currentInput instanceof ConfuzzInput : "ZestGuidance should only mutate ConfuzzInput(s)";

                // For linear inputs, get with key = bytesRead (which is then incremented)
                ConfuzzInput confuzzInput = (ConfuzzInput) currentInput;
                // Attempt to get a value from the list, or else generate a random value
                int ret = confuzzInput.getOrGenerateFresh(bytesRead++, random);
                // infoLog("read(%d) = %d", bytesRead, ret);
                return ret;
            }
        };
    }

    // log data for debugging


    @Override
    protected String getStatNames() {
        return super.getStatNames() + ", all_param_covered_probes, valid_param_covered_probes";
    }
}