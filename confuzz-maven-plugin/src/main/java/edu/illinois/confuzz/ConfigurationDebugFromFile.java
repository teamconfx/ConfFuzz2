package edu.illinois.confuzz;

import com.squareup.moshi.Moshi;
import com.squareup.moshi.Types;
import edu.illinois.confuzz.internal.ConfigUtils;
import okio.BufferedSink;
import okio.Okio;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.annotations.ResolutionScope;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.TransformerException;
import java.io.File;
import java.io.IOException;
import java.util.*;

@Mojo(name = "debug-file", defaultPhase = LifecyclePhase.POST_INTEGRATION_TEST,
        requiresDependencyResolution = ResolutionScope.TEST)
public class ConfigurationDebugFromFile extends ConfigurationDebugNewJvmMojo {
    @Parameter(property = "sourceConfigFile", required = true)
    private File sourceConfigFile;

    /** Flag to control whether to debug from sourceConfigFile or not*/
    @Parameter(property = "debug", defaultValue = "true")
    private boolean debug;
    /** Flag to control whether to show buggy configuration exceptions or not*/
    @Parameter(property = "debugOutput", defaultValue = "false")
    private boolean debugOutput;

    private static final String OUTPUT_CONFIG_FILE = "buggyConfigFile.json";
    @Override
    public void execute() {
        try {
            initDebug();
            if (debug) {
                debugFromFile();
            }
            if (debugOutput) {
                runSingleCTest();
            }
        } catch (Exception e) {
            getLog().error(e);
            throw new RuntimeException(e);
        }
    }

    @Override
    protected void initDebug() throws MojoExecutionException, IOException {
        File outputDir = new File(getOutputDirectory(), "campaign");
        if (!outputDir.exists()) {
            outputDir.mkdirs();
        }
        super.initDebug();
    }

    private void debugFromFile()
            throws IOException, ParserConfigurationException, InterruptedException, TransformerException {
        ArrayList<Map> maps = ConfigUtils.getConfigMapsFromJSON(sourceConfigFile);
        int mapSize = maps.size();
        if(maps == null || mapSize <= 0) {
            throw new IOException("Can't get configuration from JSON file "
                    + sourceConfigFile.getAbsolutePath());
        }
        Map<String, String> failedConfig;
        Map<String, String> parentConfig;
        if (mapSize == 2) {
            parentConfig = maps.get(0);
            failedConfig = maps.get(1);
        } else if(mapSize == 1) {
            parentConfig = new LinkedHashMap<>();
            failedConfig = maps.get(0);
        } else {
            throw new IOException("Incorrect configuration number get from JSON file: "
                    + sourceConfigFile.getAbsolutePath());
        }
        File outputFile = new File(sourceConfigFile.getParentFile(), OUTPUT_CONFIG_FILE);
        List<Map> mapList = new LinkedList<>();
        mapList.add(runDebugAll(failedConfig, debugBatchSize));
        Moshi MOSHI = new Moshi.Builder().build();
        try (BufferedSink sink = Okio.buffer(Okio.sink(outputFile))) {
            MOSHI.adapter(Types.newParameterizedType(List.class, Map.class))
                    .indent("    ")
                    .toJson(sink, mapList);
        }
    }

    /**
     * Run test with all configurations one by one in the given map to see the exceptions
     * @throws IOException
     * @throws ParserConfigurationException
     * @throws InterruptedException
     * @throws TransformerException
     */
    private void runSingleCTest() throws IOException, ParserConfigurationException, InterruptedException, TransformerException {
        // Read the configuration from the file OUTPUT_CONFIG_FILE
        Map<String, String> configs = ConfigUtils.getConfigMapsFromJSON(new File(sourceConfigFile.getParentFile(), OUTPUT_CONFIG_FILE)).get(0);
        totalLength = configs.size();
        runDebugOneByOne(configs);
    }
}
