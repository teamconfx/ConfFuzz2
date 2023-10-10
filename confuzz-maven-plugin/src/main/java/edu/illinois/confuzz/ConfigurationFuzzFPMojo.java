package edu.illinois.confuzz;

import com.google.gson.*;
import com.google.gson.reflect.TypeToken;
import com.pholser.junit.quickcheck.random.SourceOfRandomness;
import edu.illinois.confuzz.internal.*;
import edu.neu.ccs.prl.meringue.CampaignConfiguration;
import edu.neu.ccs.prl.meringue.FileUtil;
import edu.neu.ccs.prl.meringue.FuzzFramework;
import edu.neu.ccs.prl.meringue.JvmLauncher;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.annotations.ResolutionScope;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.reflect.Type;
import java.nio.file.Files;
import java.util.*;
import java.util.concurrent.TimeoutException;

@Mojo(name = "fuzz-fp", defaultPhase = LifecyclePhase.POST_INTEGRATION_TEST,
        requiresDependencyResolution = ResolutionScope.TEST)
public class ConfigurationFuzzFPMojo extends ConfigurationAnalysisMojo {
    private JvmLauncher launcher;
    protected File debugFrameworkJar;
    private String resultFileName = "fuzzDebugFailures.json";
    private DebugUtil debugger;
    private final Map<String, Object> minConfig = new HashMap<>();

    @Parameter(property = "regexFile", required = true, defaultValue = "regex")
    private File regexFile;

    @Parameter(property = "minConfigFile", required = true, defaultValue = "config.json")
    private File minConfigFile;

    @Parameter(property = "injectConfigFile", required = true)
    private File injectConfigFile;

    /** debugEntryMap is a map with key-value pair of (FailureType, DebugEntry) */
    protected Map<String, DebugEntry> debugEntryMap = new LinkedHashMap<>();
    public static final int FUZZ_DEBUG_LIMIT = 10;

    /** Fuzz Debug Output File **/
    private File resultFile;

    @Override
    public void execute() {
        this.resultFileName = String.format("fuzzDebugFailures_%s.json", minConfigFile.getName().split("\\.")[0]);
        try {
            File workingDirectory = new File(getOutputDirectory(), "campaign");
            if (!workingDirectory.exists() && !workingDirectory.mkdirs()) {
                getLog().warn("Failed to create output directory");
                throw new RuntimeException("Failed to create output directory");
            }
            init();
            // TODO: not done
            List<FuzzResult> result = fuzzDebug(FUZZ_DEBUG_LIMIT);
            assert result.size() == FUZZ_DEBUG_LIMIT;

            GsonBuilder gsonBuilder = new GsonBuilder();
            JsonSerializer<FuzzResult> serializer = (src, typeOfSrc, context) -> {
                JsonObject jsonFailure=  new JsonObject();
                jsonFailure.addProperty("failure", src.failure.getFailure());
                jsonFailure.addProperty("errorMessage", src.failure.getErrorMessage());
                JsonArray stackTrace = new JsonArray();
                for (StackTraceElement trace: src.failure.getStackTrace()) {
                    stackTrace.add(trace.toString());
                }
                jsonFailure.add("stackTrace", stackTrace);
                JsonObject object = new JsonObject();
                for (Map.Entry<String, String> entry: src.config.entrySet()) {
                    object.addProperty(entry.getKey(), entry.getValue());
                }
                jsonFailure.add("config", object);
                return jsonFailure;
            };
            gsonBuilder.registerTypeAdapter(FuzzResult.class, serializer);

            Gson gson = gsonBuilder.create();
            try (FileWriter out = new FileWriter(resultFile, false)){
                gson.toJson(result, out);
            }
            injectConfigFile.delete();
        } catch (Exception e) {
            getLog().error(e);
            throw new RuntimeException(e);
        }
    }

    protected void init() throws MojoExecutionException, IOException, TimeoutException {
        injectConfigFile.createNewFile();
        resultFile = new File(getOutputDirectory(), this.resultFileName);
        if (!resultFile.exists() && !resultFile.createNewFile()) {
            getLog().warn("Failed to create output directory");
            throw new RuntimeException("Failed to create output directory");
        }
        initMinConfig();
        initDebug();
        initConfParamGenerater();
    }

    protected void initMinConfig() throws IOException {
        // Convert JSON to a Map
        Gson gson = new Gson();
        Type type = new TypeToken<Map<String, String>>(){}.getType () ;
        minConfig.putAll(gson.fromJson(Files.readString(minConfigFile.toPath()), type));
    }

    /**
     * Register the minConfig to the ConfParamGenerator
     */
    protected void initConfParamGenerater() throws IOException, TimeoutException {
        // Init ConfigConstraints
        System.setProperty("regex.file", regexFile.getAbsolutePath());
        ConfigConstraints.init();
        for (Map.Entry<String, Object> entry: minConfig.entrySet()) {
            ConfParamGenerator.register(entry.getKey(), entry.getValue());
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

        // init debugger
        debugger = new DebugUtil(launcher, injectConfigFile, getLog(), new Failure(),
                new HashMap<> (), getOutputDirectory(), getTestClassName(), getTestMethodName());
    }

    @Override
    public List<String> getJavaOptions() throws MojoExecutionException {
        return ConfigurationMojoHelper.getJavaOptions(this, new LinkedList<>(), getProject(),
                                                      getTemporaryDirectory());
    }

    /**
     * Fuzz the test few more times
     * @param fuzzCount number of times to fuzz
     * @return a list of failures encountered during fuzzing. Size should be exactly equal to fuzzCount
     */
    private List<FuzzResult> fuzzDebug(int fuzzCount) {
        List<FuzzResult> ret = new LinkedList<>();
        for (int i=0; i<fuzzCount; i++) {
            Map<String, String> config = new HashMap<>();
            for (Map.Entry<String, Object> entry: this.minConfig.entrySet()) {
                Object generatedObject = ConfParamGenerator.generate(entry.getKey(), new SourceOfRandomness(new Random()));
                // Skip if generatedObject is null, the configuration parameter value is roll back to default
                if (generatedObject == null) {
                    continue;
                }
                config.put(entry.getKey(), generatedObject.toString());
            }
            Failure result;
            try {
                result = debugger.getReproduceResult(config);
            } catch (Exception e) {
                result = new Failure(new MojoExecutionException(e));
            }
            // Null means no failure found
            if (result == null) {
                result = new Failure();
            }
            ret.add(new FuzzResult(result, config));
        }
        return ret;
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
