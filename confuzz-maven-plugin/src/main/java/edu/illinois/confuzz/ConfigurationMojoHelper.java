package edu.illinois.confuzz;

import edu.illinois.confuzz.internal.ConfuzzAgent;
import edu.illinois.confuzz.internal.DebugForkMain;
import edu.illinois.confuzz.internal.FuzzForkMain;
import edu.neu.ccs.prl.meringue.CampaignValues;
import edu.neu.ccs.prl.meringue.FileUtil;
import edu.neu.ccs.prl.meringue.JvmLauncher;
import edu.neu.ccs.prl.meringue.SystemPropertyUtil;
import edu.neu.ccs.prl.pomelo.JvmConfiguration;
import org.apache.maven.model.Plugin;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.util.xml.Xpp3Dom;

import java.io.*;
import java.util.*;

public final class ConfigurationMojoHelper {
    private static final boolean DEBUG = Boolean.getBoolean("confuzz.debug");

    private static final String GENERATOR_CLASS_NAME = System.getProperty("confuzz.generator", null);


    private ConfigurationMojoHelper() {
        throw new AssertionError();
    }

    public static void writeTimeToFile(File file, String mojoName, long startTime, long endTime) throws IOException {
        // write the time of the fuzzing to file
        File timeFile = new File(file, mojoName + ".time");
        FileWriter fileWriter = new FileWriter(timeFile);
        fileWriter.write("Time: " + (endTime - startTime) + "ms");
        fileWriter.close();
    }

    static Properties getSystemPropertiesFromSurefire(MavenProject project)throws MojoExecutionException {
        Properties systemProperties = new Properties();
        for (Plugin p : project.getBuildPlugins()) {
            if (p.getArtifactId().contains("maven-surefire-plugin")) {
                Xpp3Dom configuration = (Xpp3Dom) p.getConfiguration();
                if (configuration != null) {
                    Xpp3Dom variables = configuration.getChild("systemPropertyVariables");
                    if (variables != null) {
                        for (Xpp3Dom child : variables.getChildren()) {
                            if (child.getChildCount() == 0) {
                                systemProperties.put(child.getName(), expandProperty(child.getValue()));
                            } else if (child.getName().equals("property")) {
                                systemProperties.put(child.getChild("name").getValue(), 
                                        expandProperty(child.getChild("value").getValue()));
                            } else {
                                throw new MojoExecutionException("Unable to parse surefire system property");
                            }
                        }
                    }
                }
                break;
            }
        }
        return systemProperties;
    }

    // Also account for:
    // 1. interpolated maven variable expressions (e.g., ${name})
    // 2. interpolated surefire property expressions (e.g., @{name}) -- this is not supported yet
    // 3. plugin execution configuration
    // 4. surefire thread number placeholders (i.e., ${surefire.threadNumber})
    // 5. surefire fork number placeholders (i.e., ${surefire.forkNumber})
    private static String expandProperty(String value) throws MojoExecutionException {
        value = nullProof(value);
        if (value.indexOf('@') != -1) {
            throw new MojoExecutionException("Cannot handle surefire property with @{}");
        }
        while (value.indexOf('$') != -1) {
            int startIndex = value.indexOf("$");
            int endIndex = value.indexOf("}");
            String key = value.substring(startIndex + 2, endIndex);
            String replacement = nullProof(System.getProperty(key));
            value = value.replace("${" + key + "}", replacement);
        }
        return value;
    }

    private static String nullProof(String str) {
        if (str == null) {
            return "";
        }
        return str;
    }

    /** This is for fuzzing mode, so we need to instrument the code for coverage and fuzzing. */
    static List<String> getJavaOptions(CampaignValues values, List<String> javaOptions, MavenProject project,
                                       File temporaryDirectory, File regexFile, String fuzzMode,
                                       boolean logEnable, boolean onlyCheckDefault) throws MojoExecutionException {
        try {
            List<String> options = new ArrayList<>(javaOptions);
            File systemPropertiesFile = generatePropertyFile(temporaryDirectory, project);
            options.add(String.format("-D%s=%s", FuzzForkMain.PROPERTIES_KEY, systemPropertiesFile.getAbsolutePath()));
            options.add("-Dregex.file=" + regexFile.getAbsolutePath());
            options.add("-Dclass=" + values.getTestClassName());
            options.add("-Dmethod=" + values.getTestMethodName());
            options.add("-Dlog4j.configuration="); // disable log4j
            options.add("-DfuzzMode=" + fuzzMode);
            options.add("-Dconfuzz.log=" + logEnable);
            options.add("-DonlyCheckDefault=" + onlyCheckDefault);
            File agentJar = FileUtil.getClassPathElement(ConfuzzAgent.class);
            getGeneratorClass();
            options.add(String.format("-javaagent:%s=%s,%s,%s", agentJar.getAbsolutePath(), values.getTestClassName(),
                                      values.getTestMethodName(), GENERATOR_CLASS_NAME));
            if (DEBUG) {
                options.add(JvmLauncher.DEBUG_OPT + "5005");
            }
            options.add(
                    "-Djanala.excludes=java/,com/sun/proxy/,edu/berkeley/cs/jqf/,edu/illinois/confuzz/internal/,org/junit/");
            return options;
        } catch (IOException e) {
            throw new MojoExecutionException("Failed to write system properties to file", e);
        }
    }

    /** This is for coverage goal, there is no need to have regex file, fuzz mode and log enable. */
    static List<String> getJavaOptions(CampaignValues values, List<String> javaOptions, MavenProject project,
                                       File temporaryDirectory, boolean logEnable) throws MojoExecutionException {
        try {
            List<String> options = new ArrayList<>(javaOptions);
            File systemPropertiesFile = generatePropertyFile(temporaryDirectory, project);
            options.add(String.format("-D%s=%s", FuzzForkMain.PROPERTIES_KEY, systemPropertiesFile.getAbsolutePath()));
            options.add("-Dclass=" + values.getTestClassName());
            options.add("-Dmethod=" + values.getTestMethodName());
            options.add("-Dlog4j.configuration="); // disable log4j
            options.add("-Dconfuzz.log=" + logEnable);
            File agentJar = FileUtil.getClassPathElement(ConfuzzAgent.class);
            getGeneratorClass();
            options.add(String.format("-javaagent:%s=%s,%s,%s", agentJar.getAbsolutePath(), values.getTestClassName(),
                                      values.getTestMethodName(), GENERATOR_CLASS_NAME));
            if (DEBUG) {
                options.add(JvmLauncher.DEBUG_OPT + "5005");
            }
            options.add(
                    "-Djanala.excludes=java/,com/sun/proxy/,edu/berkeley/cs/jqf/,edu/illinois/confuzz/internal/,org/junit/");
            return options;
        } catch (IOException e) {
            throw new MojoExecutionException("Failed to write system properties to file", e);
        }
    }


    /** This is for debug mode, so we don't need to instrument the code for coverage and fuzzing. */
    static List<String> getJavaOptions(CampaignValues values, List<String> javaOptions, MavenProject project,
                                       File temporaryDirectory) throws MojoExecutionException {
        try {
            List<String> options = new ArrayList<>(javaOptions);
            File systemPropertiesFile = generatePropertyFile(temporaryDirectory, project);
            options.add(String.format("-D%s=%s", DebugForkMain.PROPERTIES_KEY, systemPropertiesFile.getAbsolutePath()));
            options.add("-Dlog4j.configuration="); // disable log4j
            options.add("-Dclass=" + values.getTestClassName());
            options.add("-Dmethod=" + values.getTestMethodName());
            if (DEBUG) {
                options.add(JvmLauncher.DEBUG_OPT + "5005");
            }
            return options;
        } catch (IOException e) {
            throw new MojoExecutionException("Failed to write system properties to file", e);
        }
    }

    static File generatePropertyFile(File outputDir, MavenProject project) throws IOException {
        try {
            if (!outputDir.exists()) {
                outputDir.mkdirs();
            }
            File systemPropertiesFile = new File(outputDir, "confuzz.properties");
            SystemPropertyUtil.store(systemPropertiesFile, "", getSystemPropertiesFromSurefire(project));
            return systemPropertiesFile;
        } catch (IOException | MojoExecutionException e) {
            throw new IOException("Failed to write system properties to file", e);
        }
    }

    static Map<String, String> getEnvironment(MavenProject project) {
        Map<String, String> environment = new HashMap<>();
        for (Plugin p : project.getBuildPlugins()) {
            if (p.getArtifactId().contains("maven-surefire-plugin")) {
                Xpp3Dom configuration = (Xpp3Dom) p.getConfiguration();
                if (configuration != null) {
                    Xpp3Dom variables = configuration.getChild("environmentVariables");
                    if (variables != null) {
                        for (Xpp3Dom child : variables.getChildren()) {
                            environment.put(child.getName(), child.getValue());
                        }
                    }
                }
            }
        }
        return JvmConfiguration.createEnvironment(environment, new String[0]);
    }

    static String getGeneratorClass() {
        String currentGoal = System.getProperty("confuzz.goal", null);
        if (Objects.equals(currentGoal, "fuzz") || Objects.equals(currentGoal, "analyze")) {
            if (GENERATOR_CLASS_NAME == null) {
                throw new RuntimeException("Must specify your configuration generator with -Dconfuzz.generator");
            }
        }
        return GENERATOR_CLASS_NAME;
    }
}
